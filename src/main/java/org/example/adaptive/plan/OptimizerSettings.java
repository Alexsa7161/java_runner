package org.example.adaptive.plan;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OptimizerSettings {

    private final Map<String, String> parameters = new HashMap<>();

    public void set(String name, String value) {
        parameters.put(name, value);
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(parameters);
    }

    public boolean isEmpty() {
        return parameters.isEmpty();
    }
}