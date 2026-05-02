package org.example;

import org.example.adaptive.engine.AdaptiveQueryEngine;
import org.example.adaptive.experiment.*;
import org.example.adaptive.metrics.MetricsCollector;
import org.example.adaptive.model.RegressionModel;
import org.example.adaptive.plan.OptimizerApplier;
import org.example.adaptive.plan.OptimizerSettings;
import org.example.adaptive.execution.QueryExecutor;
import org.example.adaptive.storage.ObservationHistory;
import org.example.config.DataSourceFactory;

import javax.sql.DataSource;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        // =========================
        // DataSource
        // =========================
        DataSource dataSource = DataSourceFactory.create();

        // =========================
        // Components
        // =========================
        MetricsCollector metricsCollector = new MetricsCollector(dataSource);

        RegressionModel model = new RegressionModel(0.1);

        ObservationHistory history = new ObservationHistory(100);

        QueryExecutor queryExecutor = new QueryExecutor(dataSource);

        OptimizerApplier optimizerApplier = new OptimizerApplier(dataSource);

        // =========================
        // Adaptive engine
        // =========================
        AdaptiveQueryEngine adaptiveEngine = new AdaptiveQueryEngine(
                metricsCollector,
                model,
                queryExecutor,
                optimizerApplier,
                history,
                20,
                5
        );

        // =========================
        // Baseline executor
        // =========================
        BaselineExecutor baselineExecutor = new BaselineExecutor(dataSource);

        // =========================
        // LOAD QUERIES (FIXED)
        // =========================
        List<String> queries = QueryLoader.loadAllQueries("queries", 10);

        System.out.println("Loaded queries: " + queries.size());

        // =========================
        // Load generator
        // =========================
        LoadGenerator loadGenerator = new LoadGenerator(dataSource, queries);

        // =========================
        // Experiment runner
        // =========================
        ExperimentRunner runner = new ExperimentRunner(
                adaptiveEngine,
                baselineExecutor,
                loadGenerator
        );

        // =========================
        // Optimizer settings
        // =========================
        OptimizerSettings settings = new OptimizerSettings();
        settings.set("optimizer_mode", "ALL_ROWS");
        settings.set("optimizer_index_cost_adj", "10");

        int repeats = 10;

        // =========================
        // RUN
        // =========================
        System.out.println("\n=== BASELINE ===");
        //runner.runBaseline(queries, repeats);

        System.out.println("\n=== ADAPTIVE ===");
        runner.runAdaptive(queries, settings, repeats);

        System.out.println("\n=== BASELINE + LOAD ===");
        //runner.runBaselineWithLoad(queries, repeats);

        System.out.println("\n=== ADAPTIVE + LOAD ===");
        //runner.runAdaptiveWithLoad(queries, settings, repeats);

        System.out.println("\n=== DONE ===");
    }
}