package com.sentinel.model;

public class CheckResult {
    private final boolean healthy;
    private final long latencyMs;
    private final String details;
    private final String errorReason;

    public CheckResult(boolean healthy, long latencyMs, String details, String errorReason) {
        this.healthy = healthy;
        this.latencyMs = latencyMs;
        this.details = details;
        this.errorReason = errorReason;
    }

    public static CheckResult success(long latencyMs, String details) {
        return new CheckResult(true, latencyMs, details, null);
    }

    public static CheckResult failure(long latencyMs, String errorReason) {
        return new CheckResult(false, latencyMs, null, errorReason);
    }

    public boolean isHealthy() {
        return healthy;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public String getDetails() {
        return details;
    }

    public String getErrorReason() {
        return errorReason;
    }
}
