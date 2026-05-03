//package org.example.adaptive.experiment;
//
//import org.example.adaptive.engine.AdaptiveQueryEngine;
//import org.example.adaptive.execution.ExecutionResult;
//import org.example.adaptive.plan.OptimizerSettings;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class ExperimentRunner {
//
//    private final AdaptiveQueryEngine adaptiveEngine;
//    private final BaselineExecutor baselineExecutor;
//    private final LoadGenerator loadGenerator;
//
//    public ExperimentRunner(
//            AdaptiveQueryEngine adaptiveEngine,
//            BaselineExecutor baselineExecutor,
//            LoadGenerator loadGenerator
//    ) {
//        this.adaptiveEngine = adaptiveEngine;
//        this.baselineExecutor = baselineExecutor;
//        this.loadGenerator = loadGenerator;
//    }
//
//    // =========================
//    // BASELINE
//    // =========================
//    public List<Long> runBaseline(List<String> queries, int repeats) {
//
//        System.out.println("\n=== BASELINE ===");
//
//        List<Long> results = new ArrayList<>();
//
//        for (String q : queries) {
//            for (int i = 0; i < repeats; i++) {
//                long t = baselineExecutor.execute(q);
//                results.add(t);
//            }
//        }
//
//        printStats("BASELINE", results);
//        return results;
//    }
//
//    // =========================
//    // ADAPTIVE
//    // =========================
//    public List<Long> runAdaptive(List<String> queries, OptimizerSettings settings, int repeats) {
//
//        System.out.println("\n=== ADAPTIVE ===");
//
//        List<Long> results = new ArrayList<>();
//
//        for (String q : queries) {
//            for (int i = 0; i < repeats; i++) {
//
//                ExecutionResult res = adaptiveEngine.execute(q, settings);
//
//                results.add((long) res.getExecutionTimeMs());
//            }
//        }
//
//        printStats("ADAPTIVE", results);
//        return results;
//    }
//
//    // =========================
//    // BASELINE + LOAD
//    // =========================
//    public List<Long> runBaselineWithLoad(List<String> queries, int repeats) {
//
//        System.out.println("\n=== BASELINE + LOAD ===");
//
//        List<Long> results = new ArrayList<>();
//
//        loadGenerator.start();
//
//        try {
//            for (String q : queries) {
//                for (int i = 0; i < repeats; i++) {
//                    long t = baselineExecutor.execute(q);
//                    results.add(t);
//                }
//            }
//        } finally {
//            loadGenerator.stop();
//        }
//
//        printStats("BASELINE + LOAD", results);
//        return results;
//    }
//
//    // =========================
//    // ADAPTIVE + LOAD
//    // =========================
//    public List<Long> runAdaptiveWithLoad(List<String> queries, OptimizerSettings settings, int repeats) {
//
//        System.out.println("\n=== ADAPTIVE + LOAD ===");
//
//        List<Long> results = new ArrayList<>();
//
//        loadGenerator.start();
//
//        try {
//            for (String q : queries) {
//                for (int i = 0; i < repeats; i++) {
//
//                    ExecutionResult res = adaptiveEngine.execute(q, settings);
//
//                    results.add((long) res.getExecutionTimeMs());
//                }
//            }
//        } finally {
//            loadGenerator.stop();
//        }
//
//        printStats("ADAPTIVE + LOAD", results);
//        return results;
//    }
//
//    // =========================
//    // STATISTICS
//    // =========================
//    private void printStats(String label, List<Long> times) {
//
//        if (times == null || times.isEmpty()) {
//            System.out.println("[" + label + "] No results");
//            return;
//        }
//
//        long sum = 0;
//        long min = Long.MAX_VALUE;
//        long max = Long.MIN_VALUE;
//
//        for (long t : times) {
//            sum += t;
//            min = Math.min(min, t);
//            max = Math.max(max, t);
//        }
//
//        double avg = sum / (double) times.size();
//
//        System.out.println("\n[" + label + "]");
//        System.out.println("Count: " + times.size());
//        System.out.println("Avg ms: " + avg);
//        System.out.println("Min ms: " + min);
//        System.out.println("Max ms: " + max);
//    }
//}