package org.example.adaptive.engine;

import org.example.adaptive.execution.ExecutionResult;
import org.example.adaptive.execution.QueryExecutor;
import org.example.adaptive.metrics.MetricsCollector;
import org.example.adaptive.model.Observation;
import org.example.adaptive.model.RegressionModel;
import org.example.adaptive.plan.OptimizerApplier;
import org.example.adaptive.plan.OptimizerSettings;
import org.example.adaptive.storage.ObservationHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class AdaptiveQueryEngine {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveQueryEngine.class);

    private final MetricsCollector metricsCollector;
    private final RegressionModel model;
    private final QueryExecutor executor;
    private final OptimizerApplier optimizerApplier;
    private final ObservationHistory history;

    private final int minObservations;
    private final int retrainInterval;

    private int executionCounter = 0;

    public AdaptiveQueryEngine(
            MetricsCollector metricsCollector,
            RegressionModel model,
            QueryExecutor executor,
            OptimizerApplier optimizerApplier,
            ObservationHistory history,
            int minObservations,
            int retrainInterval
    ) {
        this.metricsCollector = metricsCollector;
        this.model = model;
        this.executor = executor;
        this.optimizerApplier = optimizerApplier;
        this.history = history;
        this.minObservations = minObservations;
        this.retrainInterval = retrainInterval;
    }

    public ExecutionResult execute(String sql, OptimizerSettings settings) {

        long start = System.currentTimeMillis();

        log.info("Executing query. sqlHash={}, settings={}",
                sql.hashCode(),
                settings.getAll());

        // 1. Features
        double[] features = metricsCollector.collectFeatures(sql);
        log.debug("Collected features: {}", Arrays.toString(features));

        // 2. Apply optimizer settings
        String sqlToExecute = optimizerApplier.applyHints(sql, settings);

        log.debug("SQL after optimizer applied: {}", sqlToExecute);

        // 3. Execute query
        ExecutionResult result = executor.execute(sqlToExecute);

        long execTime = (long) result.getExecutionTimeMs();

        log.info("Query executed. time={} ms", execTime);

        // 4. Build feature vector
        double[] extended = RegressionModel.buildExtendedVector(features);

        // 5. Save observation
        Observation observation = new Observation(extended, execTime);
        history.add(observation);

        log.debug("Observation stored. historySize={}", history.size());

        // 6. Retrain logic
        executionCounter++;

        if (history.isReady(minObservations) &&
                executionCounter % retrainInterval == 0) {

            log.info("Retraining model. observations={}, counter={}",
                    history.size(), executionCounter);

            retrainModel();

            log.info("Model retrained successfully");
        }

        log.info("Total execution time (engine): {} ms",
                (System.currentTimeMillis() - start));

        return result;
    }

    public void executeQueryStream(Iterable<String> queries, OptimizerSettings settings) {
        for (String sql : queries) {
            execute(sql, settings);
        }
    }

    private void retrainModel() {

        double[][] X = history.getFeatureMatrix();
        double[] T = history.getTargetVector();

        model.train(X, T);
    }
}