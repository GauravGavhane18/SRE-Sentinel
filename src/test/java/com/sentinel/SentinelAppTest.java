package com.sentinel;

import com.sentinel.config.RetryPolicyConfig;
import com.sentinel.config.ServiceConfig;
import com.sentinel.metrics.MetricsCollector;
import com.sentinel.model.CheckResult;
import com.sentinel.model.CheckType;
import com.sentinel.utils.ANSI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SentinelAppTest {

    @Test
    public void testMetricsCollector() {
        MetricsCollector collector = new MetricsCollector();
        assertEquals(0, collector.getTotalChecks());
        assertEquals(0, collector.getSuccessfulChecks());
        assertEquals(0, collector.getFailedChecks());

        collector.incrementTotalChecks();
        collector.incrementSuccessfulChecks();
        assertEquals(1, collector.getTotalChecks());
        assertEquals(1, collector.getSuccessfulChecks());

        collector.incrementFailedChecks();
        assertEquals(1, collector.getFailedChecks());
    }

    @Test
    public void testMetricsRecoveryStats() {
        MetricsCollector collector = new MetricsCollector();
        collector.recordRecovery(true, 1500);
        collector.recordRecovery(true, 2500);
        collector.recordRecovery(false, 3000);

        assertEquals(2, collector.getRecoverySuccess());
        assertEquals(1, collector.getRecoveryFailures());
        assertEquals(2000, collector.getAverageRecoveryTimeMs());
    }

    @Test
    public void testServiceConfig() {
        ServiceConfig config = new ServiceConfig();
        config.setName("Test-HTTP");
        config.setType(CheckType.HTTP);
        config.setTarget("http://localhost:8080/");
        config.setCheckIntervalMs(5000);
        config.setTimeoutMs(2000);
        config.setRecoveryCommand("docker restart test-container");

        assertEquals("Test-HTTP", config.getName());
        assertEquals(CheckType.HTTP, config.getType());
        assertEquals("http://localhost:8080/", config.getTarget());
        assertEquals(5000, config.getCheckIntervalMs());
        assertEquals(2000, config.getTimeoutMs());
        assertEquals("docker restart test-container", config.getRecoveryCommand());
    }

    @Test
    public void testRetryPolicyConfig() {
        RetryPolicyConfig retry = new RetryPolicyConfig();
        retry.setMaxRetries(3);
        retry.setInitialBackoffMs(1000);
        retry.setMultiplier(2.0);
        retry.setMaxBackoffMs(5000);

        assertEquals(3, retry.getMaxRetries());
        assertEquals(1000, retry.getInitialBackoffMs());
        assertEquals(2.0, retry.getMultiplier());
        assertEquals(5000, retry.getMaxBackoffMs());
    }

    @Test
    public void testCheckResult() {
        CheckResult success = CheckResult.success(120, "Connected");
        assertTrue(success.isHealthy());
        assertEquals(120, success.getLatencyMs());
        assertEquals("Connected", success.getDetails());
        assertNull(success.getErrorReason());

        CheckResult failure = CheckResult.failure(250, "Connection timeout");
        assertFalse(failure.isHealthy());
        assertEquals(250, failure.getLatencyMs());
        assertNull(failure.getDetails());
        assertEquals("Connection timeout", failure.getErrorReason());
    }

    @Test
    public void testANSIColorizer() {
        String coloredText = ANSI.colorize("OK", ANSI.GREEN);
        assertTrue(coloredText.contains("OK"));
        assertTrue(coloredText.contains("\u001B[32m"));
        assertTrue(coloredText.contains(ANSI.RESET));
    }
}
