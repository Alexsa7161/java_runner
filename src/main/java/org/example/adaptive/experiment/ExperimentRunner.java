package org.example.adaptive.experiment;

import org.example.adaptive.engine.AdaptiveQueryEngine;

import java.util.*;

public class ExperimentRunner {

    private final AdaptiveQueryEngine adaptiveEngine;
    private final BaselineExecutor baselineExecutor;
    private final LoadGenerator loadGenerator;

    // Хранение средних времён для итоговой таблицы
    private final List<Double> avgBaseline = new ArrayList<>();
    private final List<Double> avgAdaptive = new ArrayList<>();
    private final List<Double> avgBaselineLoad = new ArrayList<>();
    private final List<Double> avgAdaptiveLoad = new ArrayList<>();

    public ExperimentRunner(
            AdaptiveQueryEngine adaptiveEngine,
            BaselineExecutor baselineExecutor,
            LoadGenerator loadGenerator
    ) {
        this.adaptiveEngine = adaptiveEngine;
        this.baselineExecutor = baselineExecutor;
        this.loadGenerator = loadGenerator;
    }

    // ===================== BASELINE =====================
    public List<Long> runBaseline(List<String> queries, int repeats) {
        System.out.println("\n==============================");
        System.out.println("RUN: BASELINE");
        System.out.println("queries: " + queries.size() + ", repeats: " + repeats);
        System.out.println("==============================\n");

        List<Long> allResults = new ArrayList<>();
        avgBaseline.clear();  // сбрасываем перед новым прогоном

        for (int qIdx = 0; qIdx < queries.size(); qIdx++) {
            String q = queries.get(qIdx);
            List<Long> queryTimes = new ArrayList<>();

            System.out.println("\n[BASELINE] Query " + (qIdx + 1) + "/" + queries.size());
            for (int i = 0; i < repeats; i++) {
                long t = baselineExecutor.execute(q);
                queryTimes.add(t);
                allResults.add(t);
                System.out.println("  run " + (i + 1) + "/" + repeats + " = " + t + " ms");
            }

            double avg = queryTimes.stream().mapToLong(v -> v).average().orElse(0);
            avgBaseline.add(avg);
        }

        printStats("BASELINE", allResults);
        return allResults;
    }

    // ===================== ADAPTIVE =====================
    public List<Long> runAdaptive(List<String> queries, int repeats) {
        System.out.println("\n==============================");
        System.out.println("RUN: ADAPTIVE");
        System.out.println("==============================\n");

        List<Long> allResults = new ArrayList<>();
        avgAdaptive.clear();

        for (int qIdx = 0; qIdx < queries.size(); qIdx++) {
            String q = queries.get(qIdx);
            List<Long> queryTimes = new ArrayList<>();

            System.out.println("\n[ADAPTIVE] Query " + (qIdx + 1) + "/" + queries.size());
            for (int i = 0; i < repeats; i++) {
                double t = adaptiveEngine.execute(q);
                long tLong = Math.round(t);
                queryTimes.add(tLong);
                allResults.add(tLong);
                System.out.println("  run " + (i + 1) + "/" + repeats + " = " + tLong + " ms");
            }

            double avg = queryTimes.stream().mapToLong(v -> v).average().orElse(0);
            avgAdaptive.add(avg);
        }

        printStats("ADAPTIVE", allResults);
        return allResults;
    }

    // ===================== BASELINE + LOAD =====================
    public List<Long> runBaselineWithLoad(List<String> queries, int repeats) {
        System.out.println("\n==============================");
        System.out.println("RUN: BASELINE + LOAD");
        System.out.println("==============================\n");

        List<Long> allResults = new ArrayList<>();
        avgBaselineLoad.clear();

        loadGenerator.start();
        System.out.println("[LOAD] started");

        try {
            for (int qIdx = 0; qIdx < queries.size(); qIdx++) {
                String q = queries.get(qIdx);
                List<Long> queryTimes = new ArrayList<>();

                System.out.println("\n[BASELINE+LOAD] Query " + (qIdx + 1));
                for (int i = 0; i < repeats; i++) {
                    long t = baselineExecutor.execute(q);
                    queryTimes.add(t);
                    allResults.add(t);
                    System.out.println("  run " + (i + 1) + " = " + t + " ms");
                }

                double avg = queryTimes.stream().mapToLong(v -> v).average().orElse(0);
                avgBaselineLoad.add(avg);
            }
        } finally {
            loadGenerator.stop();
            System.out.println("[LOAD] stopped");
        }

        printStats("BASELINE + LOAD", allResults);
        return allResults;
    }

    // ===================== ADAPTIVE + LOAD =====================
    public List<Long> runAdaptiveWithLoad(List<String> queries, int repeats) {
        System.out.println("\n==============================");
        System.out.println("RUN: ADAPTIVE + LOAD");
        System.out.println("==============================\n");

        List<Long> allResults = new ArrayList<>();
        avgAdaptiveLoad.clear();

        loadGenerator.start();
        System.out.println("[LOAD] started");

        try {
            for (int qIdx = 0; qIdx < queries.size(); qIdx++) {
                String q = queries.get(qIdx);
                List<Long> queryTimes = new ArrayList<>();

                System.out.println("\n[ADAPTIVE+LOAD] Query " + (qIdx + 1));
                for (int i = 0; i < repeats; i++) {
                    double t = adaptiveEngine.execute(q);
                    long tLong = Math.round(t);
                    queryTimes.add(tLong);
                    allResults.add(tLong);
                    System.out.println("  run " + (i + 1) + " = " + tLong + " ms");
                }

                double avg = queryTimes.stream().mapToLong(v -> v).average().orElse(0);
                avgAdaptiveLoad.add(avg);
            }
        } finally {
            loadGenerator.stop();
            System.out.println("[LOAD] stopped");
        }

        printStats("ADAPTIVE + LOAD", allResults);
        return allResults;
    }

    // ===================== СВОДНАЯ ТАБЛИЦА =====================
    public void printComparisonTable() {
        int n = Math.min(avgBaseline.size(),
                Math.min(avgAdaptive.size(),
                        Math.min(avgBaselineLoad.size(), avgAdaptiveLoad.size())));

        if (n == 0) {
            System.out.println("No data for comparison table.");
            return;
        }

        System.out.println("\n====== FINAL COMPARISON TABLE ======");
        System.out.println("Query  |  avg_baseline  |  avg_adaptive  |  avg_baseline_load  |  avg_adaptive_load");
        System.out.println("-------------------------------------------------------------------------");
        for (int i = 0; i < n; i++) {
            System.out.printf("q%-6d| %-15.2f| %-15.2f| %-20.2f| %-20.2f\n",
                    i + 1,
                    avgBaseline.get(i),
                    avgAdaptive.get(i),
                    avgBaselineLoad.get(i),
                    avgAdaptiveLoad.get(i));
        }
        System.out.println("======================================\n");
    }

    // ===================== Вспомогательная статистика =====================
    private void printStats(String label, List<Long> times) {
        if (times.isEmpty()) return;

        long sum = 0, min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (long t : times) {
            sum += t;
            min = Math.min(min, t);
            max = Math.max(max, t);
        }
        double avg = sum / (double) times.size();
        System.out.println("\n--- " + label + " ---");
        System.out.printf("Count: %d | Avg: %.2f ms | Min: %d ms | Max: %d ms\n",
                times.size(), avg, min, max);
    }
}