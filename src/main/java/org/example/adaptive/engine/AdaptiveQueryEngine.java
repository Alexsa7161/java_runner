package org.example.adaptive.engine;

import org.example.adaptive.execution.ExecutionResult;
import org.example.adaptive.execution.QueryExecutor;
import org.example.adaptive.metrics.MetricsCollector;
import org.example.adaptive.model.RegressionModel;
import org.example.adaptive.storage.Observation;
import org.example.adaptive.storage.ObservationHistory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;

public class AdaptiveQueryEngine {

    private final MetricsCollector metricsCollector;
    private final DataSource dataSource;
    private final double lambda;
    private final QueryExecutor executor;
    private final int minObservations;

    private final Map<String, ObservationHistory> historyMap = new HashMap<>();
    private final Map<String, RegressionModel> modelMap = new HashMap<>();

    // =========================
    // STATIC (16)
    // =========================
    private static final String[] STATIC_KEYS = {

            "optimizer_index_cost_adj",
            "optimizer_index_caching",
            "optimizer_dynamic_sampling",
            "db_file_multiblock_read_count",
            "optimizer_features_enable",
            "pga_aggregate_target",
            "workarea_size_policy",
            "query_rewrite_enabled",

            "parallel_degree",
            "parallel_index_degree",
            "dynamic_sampling_hint",
            "cardinality",
            "opt_estimate_table",
            "opt_estimate_join",
            "opt_estimate_index",
            "pq_distribute"
    };

    // =========================
    // DYNAMIC (16)
    // =========================
    private static final String[] DYNAMIC_KEYS = {

            "consistent_gets",
            "buffer_cache_hit_ratio",
            "parse_calls",
            "physical_reads",
            "cpu_usage",
            "pga_used",
            "temp_used",
            "logical_reads",

            "active_sessions",
            "db_block_gets",
            "hard_parses",
            "rows_processed",
            "table_scans",
            "join_operations",
            "index_scans",
            "io_wait_time"
    };

    public AdaptiveQueryEngine(
            MetricsCollector metricsCollector,
            DataSource dataSource,
            double lambda,
            QueryExecutor executor,
            int minObservations
    ) {
        this.metricsCollector = metricsCollector;
        this.dataSource = dataSource;
        this.lambda = lambda;
        this.executor = executor;
        this.minObservations = minObservations;
    }

    public double execute(String sql) {

        Map<String, Double> all = metricsCollector.collectAll();

        addDefaultHints(all);

        ObservationHistory history =
                historyMap.computeIfAbsent(sql, k -> new ObservationHistory());

        RegressionModel model =
                modelMap.computeIfAbsent(sql, k -> new RegressionModel(lambda));

        double[] dynamic = toVector(all, DYNAMIC_KEYS);
        double[] stat = toVector(all, STATIC_KEYS);

        double[] newStat = trainAndAdjust(history, model, dynamic, stat);

        try (Connection conn = dataSource.getConnection()) {

            if (newStat != null) {

                double[] delta = computeDelta(stat, newStat);

                logVectors(stat, newStat, delta);

                Map<String, String> sessionParams = extractSessionParams(stat, newStat);
                Map<String, Double> hintParams = extractHintParams(newStat);

                if (!sessionParams.isEmpty()) {
                    logSessionChanges(sessionParams);
                    executor.applySessionSettings(conn, sessionParams);
                }

                logHintChanges(hintParams);

                sql = HintBuilder.applyHints(sql, hintParams);
            }

            long start = System.nanoTime();

            ExecutionResult result = executor.execute(conn, sql);

            double time = (System.nanoTime() - start) / 1_000_000.0;

            System.out.println("  time = " + time + " ms");

            history.add(new Observation(all, time));

            return time;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================
    // CORE LOGIC
    // =========================
    private double[] trainAndAdjust(
            ObservationHistory history,
            RegressionModel model,
            double[] dynamic,
            double[] stat
    ) {

        if (history.size() < minObservations) return null;

        int size = history.size();

        double[][] X = new double[size][];
        double[] Y = new double[size];

        int i = 0;
        for (Observation o : history.getAll()) {
            X[i] = toVector(o.getFeatures(), DYNAMIC_KEYS);
            Y[i] = o.getExecutionTime();
            i++;
        }

        model.train(X, Y);

        double[] beta = model.getCoefficients();

        double[] result = Arrays.copyOf(stat, stat.length);

        double max = Arrays.stream(beta).map(Math::abs).max().orElse(1);

        double baseStep = 0.2;

        for (int j = 0; j < stat.length; j++) {

            double influence = beta[Math.min(j, beta.length - 1)] / max;

            double step = baseStep * (0.5 + Math.abs(influence));

            result[j] = stat[j] - Math.signum(influence) * step * (Math.abs(stat[j]) + 1);
        }

        return result;
    }

    // =========================
    // LOGGING (ТВОЙ ФОРМАТ)
    // =========================
    private void logVectors(double[] current, double[] adjusted, double[] delta) {

        System.out.println("\n=== PARAMETER UPDATE ===");

        for (int i = 0; i < STATIC_KEYS.length; i++) {

            System.out.printf(
                    "%-30s | old=%10.3f | new=%10.3f | Δ=%10.3f%n",
                    STATIC_KEYS[i],
                    current[i],
                    adjusted[i],
                    delta[i]
            );
        }

        System.out.println("========================\n");
    }

    private void logSessionChanges(Map<String, String> params) {

        System.out.println("=== SESSION PARAM CHANGES ===");

        for (var e : params.entrySet()) {
            System.out.println(e.getKey() + " -> " + e.getValue());
        }

        System.out.println("=============================");
    }

    private void logHintChanges(Map<String, Double> params) {

        System.out.println("=== HINT PARAMS ===");

        for (var e : params.entrySet()) {
            System.out.println(e.getKey() + " -> " + String.format("%.3f", e.getValue()));
        }

        System.out.println("===================");
    }

    // =========================
    // UTILS
    // =========================
    private double[] toVector(Map<String, Double> f, String[] keys) {

        double[] v = new double[keys.length];

        for (int i = 0; i < keys.length; i++) {
            v[i] = f.getOrDefault(keys[i], 0.0);
        }

        return v;
    }

    private double[] computeDelta(double[] a, double[] b) {

        double[] d = new double[a.length];

        for (int i = 0; i < a.length; i++) {
            d[i] = b[i] - a[i];
        }

        return d;
    }

    private Map<String, String> extractSessionParams(double[] current, double[] adjusted) {

        Map<String, String> res = new HashMap<>();

        for (int i = 0; i < 8; i++) {
            if (Math.abs(current[i] - adjusted[i]) > 1) {
                res.put(STATIC_KEYS[i], String.valueOf((long) adjusted[i]));
            }
        }

        return res;
    }

    private Map<String, Double> extractHintParams(double[] adjusted) {

        Map<String, Double> res = new HashMap<>();

        for (int i = 8; i < STATIC_KEYS.length; i++) {
            res.put(STATIC_KEYS[i], adjusted[i]);
        }

        return res;
    }

    private void addDefaultHints(Map<String, Double> f) {

        f.putIfAbsent("parallel_degree", 1.0);
        f.putIfAbsent("parallel_index_degree", 1.0);
        f.putIfAbsent("dynamic_sampling_hint", 2.0);
        f.putIfAbsent("cardinality", 1000.0);
        f.putIfAbsent("opt_estimate_table", 1.0);
        f.putIfAbsent("opt_estimate_join", 1.0);
        f.putIfAbsent("opt_estimate_index", 1.0);
        f.putIfAbsent("pq_distribute", 1.0);
    }
}