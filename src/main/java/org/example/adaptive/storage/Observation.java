package org.example.adaptive.storage;

import java.util.Map;

public class Observation {

    private final Map<String, Double> features;
    private final double executionTime;

    public Observation(Map<String, Double> features, double executionTime) {
        this.features = features;
        this.executionTime = executionTime;
    }

    public Map<String, Double> getFeatures() {
        return features;
    }

    public double getExecutionTime() {
        return executionTime;
    }
}