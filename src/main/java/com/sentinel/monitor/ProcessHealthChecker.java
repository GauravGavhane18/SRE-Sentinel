package com.sentinel.monitor;

import com.sentinel.config.ServiceConfig;
import com.sentinel.model.CheckResult;
import com.sentinel.utils.CommandExecutor;

public class ProcessHealthChecker implements HealthChecker {
    @Override
    public CheckResult check(ServiceConfig config) {
        long startTime = System.currentTimeMillis();
        String processName = config.getTarget();
        String os = System.getProperty("os.name").toLowerCase();

        String command;
        if (os.contains("win")) {
            // Check process on Windows
            command = "tasklist /NH /FI \"IMAGENAME eq " + processName + "\"";
        } else {
            // Check process on Linux using pgrep
            command = "pgrep -f " + processName;
        }

        try {
            CommandExecutor.CommandResult result = CommandExecutor.execute(command, config.getTimeoutMs());
            long latencyMs = System.currentTimeMillis() - startTime;

            if (os.contains("win")) {
                if (result.isSuccess() && result.getStdout().contains(processName)) {
                    return CheckResult.success(latencyMs, "Process '" + processName + "' running (Windows)");
                } else {
                    if (MockContainerRegistry.isUseMock()) {
                        boolean running = MockContainerRegistry.isContainerRunning(processName);
                        if (running) {
                            return CheckResult.success(latencyMs, "Process '" + processName + "' running (MOCK)");
                        }
                    }
                    return CheckResult.failure(latencyMs, "Process '" + processName + "' not running");
                }
            } else {
                // On Linux, pgrep returns exit code 0 if a match is found
                if (result.getExitCode() == 0) {
                    String pids = result.getStdout().replace("\n", ", ").trim();
                    return CheckResult.success(latencyMs, "Process active (PIDs: " + pids + ")");
                } else {
                    if (MockContainerRegistry.isUseMock()) {
                        boolean running = MockContainerRegistry.isContainerRunning(processName);
                        if (running) {
                            return CheckResult.success(latencyMs, "Process '" + processName + "' running (MOCK)");
                        }
                    }
                    return CheckResult.failure(latencyMs, "Process not found in process list");
                }
            }
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            return CheckResult.failure(latencyMs, "Process check error: " + e.getMessage());
        }
    }
}
