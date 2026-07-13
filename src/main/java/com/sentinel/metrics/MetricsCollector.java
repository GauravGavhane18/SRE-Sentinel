package com.sentinel.metrics;

import java.util.concurrent.atomic.AtomicLong;

public class MetricsCollector {
    private final long startTime;
    private final AtomicLong totalChecks;
    private final AtomicLong successfulChecks;
    private final AtomicLong failedChecks;
    private final AtomicLong recoverySuccess;
    private final AtomicLong recoveryFailures;
    private final AtomicLong totalRecoveryTimeMs;

    public MetricsCollector() {
        this.startTime = System.currentTimeMillis();
        this.totalChecks = new AtomicLong(0);
        this.successfulChecks = new AtomicLong(0);
        this.failedChecks = new AtomicLong(0);
        this.recoverySuccess = new AtomicLong(0);
        this.recoveryFailures = new AtomicLong(0);
        this.totalRecoveryTimeMs = new AtomicLong(0);
    }

    public void incrementTotalChecks() {
        totalChecks.incrementAndGet();
    }

    public void incrementSuccessfulChecks() {
        successfulChecks.incrementAndGet();
    }

    public void incrementFailedChecks() {
        failedChecks.incrementAndGet();
    }

    public void recordRecovery(boolean success, long durationMs) {
        if (success) {
            recoverySuccess.incrementAndGet();
            totalRecoveryTimeMs.addAndGet(durationMs);
        } else {
            recoveryFailures.incrementAndGet();
        }
    }

    public long getTotalChecks() {
        return totalChecks.get();
    }

    public long getSuccessfulChecks() {
        return successfulChecks.get();
    }

    public long getFailedChecks() {
        return failedChecks.get();
    }

    public long getRecoverySuccess() {
        return recoverySuccess.get();
    }

    public long getRecoveryFailures() {
        return recoveryFailures.get();
    }

    public long getAverageRecoveryTimeMs() {
        long successCount = recoverySuccess.get();
        if (successCount == 0) {
            return 0;
        }
        return totalRecoveryTimeMs.get() / successCount;
    }

    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    public String getFormattedUptime() {
        long uptimeSec = getUptimeSeconds();
        long hours = uptimeSec / 3600;
        long minutes = (uptimeSec % 3600) / 60;
        long seconds = uptimeSec % 60;
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }
}
