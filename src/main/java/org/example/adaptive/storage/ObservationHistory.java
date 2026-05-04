package org.example.adaptive.storage;

import java.util.ArrayList;
import java.util.List;

public class ObservationHistory {

    private final List<Observation> history = new ArrayList<>();


    public void add(Observation observation) {
        history.add(observation);
    }


    public List<Observation> getAll() {
        return history;
    }


    public int size() {
        return history.size();
    }


    public void clear() {
        history.clear();
    }
    public Observation getLast() {
        if (history.isEmpty()) {
            return null;
        }
        return history.get(history.size() - 1);
    }
}