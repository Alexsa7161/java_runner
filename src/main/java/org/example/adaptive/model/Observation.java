package org.example.adaptive.model;

import java.util.Arrays;

public class Observation {

    private final double[] features; // φ(x)
    private final double executionTime; // T

    public Observation(double[] features, double executionTime) {
        this.features = features;
        this.executionTime = executionTime;
    }

    public double[] getFeatures() {
        return features;
    }

    public double getExecutionTime() {
        return executionTime;
    }

    public int getFeatureSize() {
        return features.length;
    }

    @Override
    public String toString() {
        return "Observation{" +
                "features=" + Arrays.toString(features) +
                ", executionTime=" + executionTime +
                '}';
    }
}