package org.example.adaptive.storage;

import java.util.ArrayList;
import java.util.List;

public class ObservationHistory {

    private final List<Observation> history = new ArrayList<>();

    // добавить запись
    public void add(Observation observation) {
        history.add(observation);
    }

    // получить всю историю
    public List<Observation> getAll() {
        return history;
    }

    // размер истории
    public int size() {
        return history.size();
    }

    // очистка (если нужно для экспериментов)
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