package com.sentinel.scheduler;

import com.sentinel.config.ServiceConfig;
import com.sentinel.metrics.MetricsCollector;
import com.sentinel.model.CheckResult;
import com.sentinel.model.HealthStatus;
import com.sentinel.monitor.*;
import com.sentinel.recovery.RecoveryManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthCheckScheduler {
    private final ScheduledExecutorService scheduler;
    private final RecoveryManager recoveryManager;
    private final MetricsCollector metricsCollector;
    private final Map<String, HealthStatus> serviceStatuses;
    private final Map<String, CheckResult> lastCheckResults;
    private final Map<String, HealthChecker> checkers;

    public HealthCheckScheduler(RecoveryManager recoveryManager, MetricsCollector metricsCollector) {
        this.scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        this.recoveryManager = recoveryManager;
        this.metricsCollector = metricsCollector;
        this.serviceStatuses = new ConcurrentHashMap<>();
        this.lastCheckResults = new ConcurrentHashMap<>();
        this.checkers = new ConcurrentHashMap<>();

        // Register check execution strategies
        checkers.put("HTTP", new HttpHealthChecker());
        checkers.put("TCP", new TcpHealthChecker());
        checkers.put("DOCKER", new DockerHealthChecker());
        checkers.put("PROCESS", new ProcessHealthChecker());
    }

    public void start(List<ServiceConfig> services) {
        for (ServiceConfig service : services) {
            String name = service.getName();
            serviceStatuses.put(name, HealthStatus.UNKNOWN);

            HealthChecker checker = checkers.get(service.getType().name());
            if (checker == null) {
                System.err.println("[Scheduler] Configuration error: Unknown check type for service " + name);
                continue;
            }

            Runnable checkTask = () -> {
                try {
                    // Skip regular scheduling if the service is undergoing retry/recovery
                    if (recoveryManager.isRecovering(name)) {
                        return;
                    }

                    metricsCollector.incrementTotalChecks();
                    CheckResult result = checker.check(service);
                    lastCheckResults.put(name, result);

                    if (result.isHealthy()) {
                        metricsCollector.incrementSuccessfulChecks();
                        serviceStatuses.put(name, HealthStatus.HEALTHY);
                    } else {
                        metricsCollector.incrementFailedChecks();
                        // Asynchronously trigger recovery
                        recoveryManager.handleFailure(service, checker, result, this::updateServiceStatus);
                    }
                } catch (Exception e) {
                    System.err.println("[Scheduler] Check exception for " + name + ": " + e.getMessage());
                }
            };

            // Run checks periodically based on configured interval
            scheduler.scheduleWithFixedDelay(checkTask, 0, service.getCheckIntervalMs(), TimeUnit.MILLISECONDS);
        }
    }

    public void updateServiceStatus(String serviceName, HealthStatus status) {
        serviceStatuses.put(serviceName, status);
    }

    public Map<String, HealthStatus> getServiceStatuses() {
        return serviceStatuses;
    }

    public Map<String, CheckResult> getLastCheckResults() {
        return lastCheckResults;
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
