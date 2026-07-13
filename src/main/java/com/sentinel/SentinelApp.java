package com.sentinel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.config.SentinelConfig;
import com.sentinel.config.ServiceConfig;
import com.sentinel.logging.IncidentLogger;
import com.sentinel.metrics.MetricsCollector;
import com.sentinel.model.CheckResult;
import com.sentinel.model.HealthStatus;
import com.sentinel.model.IncidentReport;
import com.sentinel.recovery.RecoveryManager;
import com.sentinel.scheduler.HealthCheckScheduler;
import com.sentinel.utils.ANSI;
import com.sentinel.monitor.SystemHealthMonitor;

import java.io.File;
import java.util.List;
import java.util.Map;

public class SentinelApp {
    private static final String CONFIG_FILE_PATH = "config/sentinel-config.json";
    private static volatile boolean running = true;

    public static void main(String[] args) {
        System.out.println("[Sentinel] Initializing Self-Healing Infrastructure Monitoring System...");

        // Load configuration
        SentinelConfig config;
        try {
            ObjectMapper mapper = new ObjectMapper();
            File configFile = new File(CONFIG_FILE_PATH);
            if (!configFile.exists()) {
                System.err.println("[Sentinel] Configuration file not found at: " + configFile.getAbsolutePath());
                System.err.println("[Sentinel] Please ensure config/sentinel-config.json exists.");
                System.exit(1);
            }
            config = mapper.readValue(configFile, SentinelConfig.class);
            System.out.println("[Sentinel] Configuration loaded successfully. Monitored services: " + config.getServices().size());
        } catch (Exception e) {
            System.err.println("[Sentinel] Critical error loading configuration: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Initialize SRE modules
        MetricsCollector metricsCollector = new MetricsCollector();
        IncidentLogger incidentLogger = new IncidentLogger();
        RecoveryManager recoveryManager = new RecoveryManager(incidentLogger, metricsCollector);
        HealthCheckScheduler scheduler = new HealthCheckScheduler(recoveryManager, metricsCollector);

        // Register shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            System.out.print(ANSI.SHOW_CURSOR); // Restore terminal cursor
            System.out.println("\n[Sentinel] Shutdown signal received. Stopping schedulers and clean shutdown...");
            scheduler.shutdown();
            recoveryManager.shutdown();
            System.out.println("[Sentinel] System offline.");
        }));

        // Start periodic monitoring
        scheduler.start(config.getServices());

        // Hide terminal cursor for clean htop-style console UI
        System.out.print(ANSI.HIDE_CURSOR);

        // Run live console rendering loop
        try {
            while (running) {
                renderDashboard(config.getServices(), scheduler, metricsCollector, incidentLogger);
                Thread.sleep(1500); // Refresh UI every 1.5 seconds
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.print(ANSI.SHOW_CURSOR);
        }
    }

    private static void renderDashboard(List<ServiceConfig> services,
                                       HealthCheckScheduler scheduler,
                                       MetricsCollector metrics,
                                       IncidentLogger logger) {
        Map<String, HealthStatus> statuses = scheduler.getServiceStatuses();
        Map<String, CheckResult> lastResults = scheduler.getLastCheckResults();

        // Collect host resource stats
        SystemHealthMonitor.SystemMetrics hostMetrics = SystemHealthMonitor.collect();

        // Clear screen and reset cursor
        StringBuilder sb = new StringBuilder();
        sb.append(ANSI.CLEAR_SCREEN);
        sb.append(ANSI.CURSOR_HOME);

        // Draw header
        sb.append(ANSI.colorize("========================================================================================\n", ANSI.BRIGHT_BLACK));
        sb.append(ANSI.colorize("       SENTINEL : SELF-HEALING INFRASTRUCTURE MONITORING DAEMON (SRE PANEL)             \n", ANSI.BOLD + ANSI.BRIGHT_CYAN));
        sb.append(ANSI.colorize("========================================================================================\n", ANSI.BRIGHT_BLACK));

        // Draw Host Resource Metrics Block
        sb.append(ANSI.colorize(" [HOST RESOURCES]\n", ANSI.BOLD + ANSI.BRIGHT_BLUE));
        sb.append(String.format("  CPU Load: %-26s | Host Uptime: %-37s\n",
                ANSI.colorize(hostMetrics.cpuLoad, ANSI.BRIGHT_GREEN),
                ANSI.colorize(hostMetrics.uptime, ANSI.BRIGHT_GREEN)
        ));
        sb.append(String.format("  Memory:   %-26s | Disk Space:  %-37s\n",
                ANSI.colorize(hostMetrics.memoryUsage, ANSI.BRIGHT_GREEN),
                ANSI.colorize(hostMetrics.diskUsage, ANSI.BRIGHT_GREEN)
        ));
        sb.append(String.format("  Processes: %-25s | Zombie Proc: %-37s\n",
                ANSI.colorize(String.valueOf(hostMetrics.runningProcesses), ANSI.BRIGHT_GREEN),
                ANSI.colorize(String.valueOf(hostMetrics.zombieProcesses), hostMetrics.zombieProcesses > 0 ? ANSI.RED : ANSI.BRIGHT_GREEN)
        ));
        sb.append(ANSI.colorize("----------------------------------------------------------------------------------------\n", ANSI.BRIGHT_BLACK));

        // Draw Daemon Metrics Block
        sb.append(ANSI.colorize(" [DAEMON METRICS]\n", ANSI.BOLD + ANSI.BRIGHT_BLUE));
        sb.append(String.format("  Uptime: %-24s | Total Checks: %-8d | Successful: %-8d\n",
                ANSI.colorize(metrics.getFormattedUptime(), ANSI.BRIGHT_GREEN),
                metrics.getTotalChecks(),
                metrics.getSuccessfulChecks()
        ));
        sb.append(String.format("  Failed Checks: %-17s | Recovery Success: %-5d | Avg Recovery Time: %-7s\n",
                ANSI.colorize(String.valueOf(metrics.getFailedChecks()), metrics.getFailedChecks() > 0 ? ANSI.RED : ANSI.WHITE),
                metrics.getRecoverySuccess(),
                ANSI.colorize(metrics.getAverageRecoveryTimeMs() + "ms", ANSI.BRIGHT_YELLOW)
        ));
        sb.append(ANSI.colorize("========================================================================================\n", ANSI.BRIGHT_BLACK));

        // Draw Table Header
        sb.append(String.format(" %-23s | %-12s | %-12s | %-8s | %-20s\n",
                ANSI.colorize("SERVICE NAME", ANSI.BOLD),
                ANSI.colorize("STATUS", ANSI.BOLD),
                ANSI.colorize("LATENCY", ANSI.BOLD),
                ANSI.colorize("TYPE", ANSI.BOLD),
                ANSI.colorize("DETAILS / MESSAGE", ANSI.BOLD)
        ));
        sb.append(ANSI.colorize("----------------------------------------------------------------------------------------\n", ANSI.BRIGHT_BLACK));

        // Draw Service Rows
        for (ServiceConfig sc : services) {
            String name = sc.getName();
            HealthStatus status = statuses.getOrDefault(name, HealthStatus.UNKNOWN);
            CheckResult res = lastResults.get(name);

            // Format status column with colors
            String statusText;
            switch (status) {
                case HEALTHY:
                    statusText = ANSI.colorize("● UP", ANSI.BOLD + ANSI.GREEN);
                    break;
                case DEGRADED:
                    statusText = ANSI.colorize("▲ RETRYING", ANSI.BOLD + ANSI.YELLOW);
                    break;
                case UNHEALTHY:
                    statusText = ANSI.colorize("■ DOWN", ANSI.BOLD + ANSI.RED);
                    break;
                default:
                    statusText = ANSI.colorize("○ UNKNOWN", ANSI.BRIGHT_BLACK);
                    break;
            }

            // Latency & Detail strings
            String latencyStr = (res != null) ? res.getLatencyMs() + " ms" : "n/a";
            String detailStr;
            if (res != null) {
                detailStr = res.isHealthy() ? res.getDetails() : res.getErrorReason();
            } else {
                detailStr = "Waiting first run...";
            }

            // Truncate detailStr if too long to prevent row wrapping
            if (detailStr.length() > 32) {
                detailStr = detailStr.substring(0, 29) + "...";
            }

            sb.append(String.format(" %-23s | %-21s | %-12s | %-8s | %-20s\n",
                    name,
                    statusText,
                    latencyStr,
                    sc.getType().name(),
                    detailStr
            ));
        }

        // Draw Incident Log Header
        sb.append(ANSI.colorize("========================================================================================\n", ANSI.BRIGHT_BLACK));
        sb.append(ANSI.colorize(" RECENT AUTOMATED INCIDENT RECOVERIES (Tail - 4 logs/incidents.log)\n", ANSI.BOLD + ANSI.BRIGHT_PURPLE));
        sb.append(ANSI.colorize("----------------------------------------------------------------------------------------\n", ANSI.BRIGHT_BLACK));

        // Display recent incidents
        List<IncidentReport> recentIncidents = logger.getRecentIncidents(4);
        if (recentIncidents.isEmpty()) {
            sb.append(" [No incidents logged. System is operating within normal parameters.]\n");
        } else {
            for (IncidentReport incident : recentIncidents) {
                String recStatus = "SUCCESS".equalsIgnoreCase(incident.getRecoveryStatus())
                        ? ANSI.colorize("SUCCESS", ANSI.GREEN)
                        : ANSI.colorize("FAILED", ANSI.RED);

                sb.append(String.format(" [%s] %s (%s) -> Recovery: %s (Duration: %dms) | RCA: %s\n",
                        incident.getTimestamp(),
                        incident.getServiceName(),
                        incident.getCheckType(),
                        recStatus,
                        incident.getRecoveryDurationMs(),
                        incident.getRootCause()
                ));
            }
        }
        sb.append(ANSI.colorize("========================================================================================\n", ANSI.BRIGHT_BLACK));

        // Flush buffer to console
        System.out.print(sb.toString());
    }
}
