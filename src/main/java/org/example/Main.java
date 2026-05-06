package org.example;

import org.example.adaptive.engine.AdaptiveQueryEngine;
import org.example.adaptive.experiment.*;
import org.example.adaptive.metrics.MetricsCollector;
import org.example.adaptive.execution.QueryExecutor;
import org.example.config.DataSourceFactory;

import javax.sql.DataSource;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        DataSource dataSource = DataSourceFactory.create();

        MetricsCollector metricsCollector = new MetricsCollector(dataSource);
        QueryExecutor queryExecutor = new QueryExecutor(dataSource);

        AdaptiveQueryEngine adaptiveEngine = new AdaptiveQueryEngine(
                metricsCollector,
                dataSource,
                0.001,
                queryExecutor,
                5
        );

        List<String> queries = QueryLoader.loadAllQueries("queries", 10);
        int repeats = 50;

        BaselineExecutor baselineExecutor = new BaselineExecutor(dataSource);

        LoadGenerator loadGenerator = new LoadGenerator(
                dataSource,
                QueryLoader.loadAllQueries("queries", 10)
        );

        ExperimentRunner runner = new ExperimentRunner(adaptiveEngine, baselineExecutor, loadGenerator);
        List<Long> res1 = runner.runBaseline(queries, repeats);
        List<Long> res2 = runner.runAdaptive(queries, repeats);
        List<Long> res3 = runner.runBaselineWithLoad(queries, repeats);
        List<Long> res4 = runner.runAdaptiveWithLoad(queries, repeats);
        runner.printComparisonTable();
    }
}