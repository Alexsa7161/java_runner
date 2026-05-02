package org.example.adaptive.execution;

public class ExecutionResult {

    private final double executionTimeMs;
    private final boolean success;

    public ExecutionResult(double executionTimeMs, boolean success) {
        this.executionTimeMs = executionTimeMs;
        this.success = success;
    }

    public double getExecutionTimeMs() {
        return executionTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }
}