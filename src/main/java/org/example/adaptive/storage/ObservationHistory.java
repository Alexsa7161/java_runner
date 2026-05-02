package org.example.adaptive.storage;

import org.example.adaptive.model.Observation;

import java.util.ArrayDeque;
import java.util.Deque;

public class ObservationHistory {

    private final int maxSize;
    private final Deque<Observation> observations;

    public ObservationHistory(int maxSize) {
        this.maxSize = maxSize;
        this.observations = new ArrayDeque<>();
    }

    public void add(Observation observation) {
        if (observations.size() >= maxSize) {
            observations.removeFirst(); // удаляем самое старое
        }
        observations.addLast(observation);
    }

    public int size() {
        return observations.size();
    }

    public boolean isReady(int minSize) {
        return observations.size() >= minSize;
    }

    public double[][] getFeatureMatrix() {
        int m = observations.size();
        int n = observations.peekFirst().getFeatureSize();

        double[][] X = new double[m][n];

        int i = 0;
        for (Observation obs : observations) {
            X[i++] = obs.getFeatures();
        }

        return X;
    }

    public double[] getTargetVector() {
        int m = observations.size();
        double[] T = new double[m];

        int i = 0;
        for (Observation obs : observations) {
            T[i++] = obs.getExecutionTime();
        }

        return T;
    }

    public void clear() {
        observations.clear();
    }
}