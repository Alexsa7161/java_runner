package org.example.adaptive.engine;

import org.example.adaptive.execution.ExecutionResult;
import org.example.adaptive.execution.QueryExecutor;
import org.example.adaptive.metrics.MetricsCollector;
import org.example.adaptive.model.RegressionModel;
import org.example.adaptive.plan.OptimizerApplier;
import org.example.adaptive.storage.Observation;
import org.example.adaptive.storage.ObservationHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AdaptiveQueryEngine {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveQueryEngine.class);

    private final MetricsCollector metricsCollector;
    private final double lambda;
    private final QueryExecutor executor;
    private final OptimizerApplier optimizerApplier;
    private final int minObservations;
    private final int retrainInterval;

    // Для каждого уникального текста запроса — своя история и модель
    private final Map<String, ObservationHistory> historyMap = new HashMap<>();
    private final Map<String, RegressionModel> modelMap = new HashMap<>();

    public AdaptiveQueryEngine(
            MetricsCollector metricsCollector,
            double lambda,
            QueryExecutor executor,
            OptimizerApplier optimizerApplier,
            int minObservations,
            int retrainInterval
    ) {
        this.metricsCollector = metricsCollector;
        this.lambda = lambda;
        this.executor = executor;
        this.optimizerApplier = optimizerApplier;
        this.minObservations = minObservations;
        this.retrainInterval = retrainInterval;
    }

    public double execute(String SQL_text) {
        // Получить или создать историю и модель для данного запроса
        ObservationHistory history = historyMap.computeIfAbsent(SQL_text, k -> new ObservationHistory());
        RegressionModel model = modelMap.computeIfAbsent(SQL_text, k -> new RegressionModel(lambda));

        // 1. Собрать текущие метрики среды
        Map<String, Double> features = metricsCollector.collectSessionParameters();

        // 2. Если модель уже предлагает оптимальные параметры – применить их
        double[] optimized = trainIfPossible(history, model);
        if (optimized != null) {
            Map<String, String> sessionParams = buildSessionParams(optimized);
            executor.applySessionSettings(sessionParams);
        }
        System.out.println("beta:" + (model.isTrained() ? Arrays.toString(model.getCoefficients()) : "null"));
        System.out.println("opt_param:" + (optimized != null ? Arrays.toString(optimized) : "null"));

        // 3. Выполнить запрос и замерить реальное время
        long start = System.nanoTime();
        ExecutionResult result = executor.execute(SQL_text);
        double executionTimeMs;
        if (result.isSuccess()) {
            long durationNs = System.nanoTime() - start;
            executionTimeMs = durationNs / 1_000_000.0;
        } else {
            executionTimeMs = -1;  // ошибка — можно обработать отдельно
        }

        // 4. Сохранить наблюдение в историю конкретного запроса
        history.add(new Observation(features, executionTimeMs));

        return executionTimeMs;
    }

    private Map<String, String> buildSessionParams(double[] optimized) {
        Map<String, String> params = new LinkedHashMap<>();
        // Только те параметры, которые можно менять в сессии
        for (int i = 0; i < 16; i++) {
            String key = FEATURE_KEYS[i];
            if (!ALLOWED_SESSION_KEYS.contains(key)) {
                continue;
            }
            double rawValue = optimized[i];
            String value = formatParameter(key, rawValue);
            params.put(key, value);
        }
        return params;
    }

    private static final Set<String> ALLOWED_SESSION_KEYS = Set.of(
            "optimizer_index_cost_adj",
            "optimizer_index_caching",
            "db_file_multiblock_read_count",
            "parallel_degree_limit",
            "parallel_min_time_threshold",
            "optimizer_dynamic_sampling",
            "hash_area_size",
            "sort_area_size",
            "result_cache_max_size"
    );

    private String formatParameter(String key, double value) {
        // Приводим к целому, если параметр целочисленный
        if (key.equals("db_file_multiblock_read_count") ||
                key.equals("parallel_degree_limit") ||
                key.equals("parallel_degree_policy") ||
                key.equals("parallel_min_time_threshold") ||
                key.equals("optimizer_dynamic_sampling") ||
                key.equals("hash_area_size") ||
                key.equals("sort_area_size") ||
                key.equals("workarea_size_policy") ||
                key.equals("plsql_optimize_level") ||
                key.equals("result_cache_max_size") ||
                key.equals("sga_target") ||
                key.equals("db_cache_size") ||
                key.equals("shared_pool_size") ||
                key.equals("pga_aggregate_target")
        ) {
            return String.valueOf((long) Math.round(value));
        }
        // Для дробных параметров (optimizer_index_cost_adj, optimizer_index_caching)
        return String.valueOf(value);
    }

    private double[] toVector(Map<String, Double> features) {
        double[] vector = new double[features.size()];
        int i = 0;
        for (Double value : features.values()) {
            vector[i++] = value;
        }
        return vector;
    }

    private double[] trainIfPossible(ObservationHistory history, RegressionModel model) {
        if (history.size() < minObservations) {
            return null;
        }

        int size = history.size();
        double[][] X = new double[size][];
        double[] Y = new double[size];

        int i = 0;
        for (Observation obs : history.getAll()) {
            X[i] = toVector(obs.getFeatures());
            Y[i] = obs.getExecutionTime();
            i++;
        }

        model.train(X, Y);

        // Маска adjustable (только управляемые числовые параметры)
        int featureCount = model.getMean().length;
        boolean[] adjustable = new boolean[featureCount];
        int[] adjustableIndices = {
                0,  // optimizer_index_cost_adj
                1,  // optimizer_index_caching
                2,  // db_file_multiblock_read_count
                3,  // parallel_degree_limit
                5,  // parallel_min_time_threshold
                6,  // optimizer_dynamic_sampling
                7,  // pga_aggregate_target
                8,  // sga_target
                9,  // db_cache_size
                10, // shared_pool_size
                11, // result_cache_max_size
                12, // hash_area_size
                13  // sort_area_size
        };
        for (int idx : adjustableIndices) {
            if (idx < featureCount) {
                adjustable[idx] = true;
            }
        }

        double[] currentFeatures = mapToArray(history.getLast().getFeatures());
        double[] newFeatures = model.optimizeParameters(currentFeatures, adjustable);

        log.info("Model trained on {} observations", size);
        return newFeatures;
    }

    private static double[] mapToArray(Map<String, Double> features) {
        double[] array = new double[FEATURE_KEYS.length];
        for (int i = 0; i < FEATURE_KEYS.length; i++) {
            Double value = features.get(FEATURE_KEYS[i]);
            array[i] = (value != null) ? value : 0.0;
        }
        return array;
    }

    public static final String[] FEATURE_KEYS = {
            "optimizer_index_cost_adj",
            "optimizer_index_caching",
            "db_file_multiblock_read_count",
            "parallel_degree_limit",
            "parallel_degree_policy",
            "parallel_min_time_threshold",
            "optimizer_dynamic_sampling",
            "pga_aggregate_target",
            "sga_target",
            "db_cache_size",
            "shared_pool_size",
            "result_cache_max_size",
            "hash_area_size",
            "sort_area_size",
            "workarea_size_policy",
            "plsql_optimize_level",
            "cpu_usage",
            "active_sessions",
            "logical_reads",
            "physical_reads",
            "physical_writes",
            "db_block_gets",
            "consistent_gets",
            "buffer_cache_hit_ratio",
            "library_cache_hit_ratio",
            "latch_waits",
            "enqueue_waits",
            "pga_used",
            "temp_used",
            "io_wait_time",
            "logons",
            "parse_calls"
    };
}