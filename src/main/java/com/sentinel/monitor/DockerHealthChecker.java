package com.sentinel.monitor;

import com.sentinel.config.ServiceConfig;
import com.sentinel.model.CheckResult;
import com.sentinel.utils.CommandExecutor;

public class DockerHealthChecker implements HealthChecker {
    private static Boolean dockerAvailable = null;

    private synchronized boolean isDockerAvailable() {
        if (dockerAvailable == null) {
            CommandExecutor.CommandResult result = CommandExecutor.execute("docker --version", 2000);
            dockerAvailable = result.isSuccess();
            if (!dockerAvailable) {
                MockContainerRegistry.setUseMock(true);
                System.out.println("[Sentinel] Docker CLI not found/running. Falling back to in-memory container simulation.");
            }
        }
        return dockerAvailable;
    }

    @Override
    public CheckResult check(ServiceConfig config) {
        long startTime = System.currentTimeMillis();
        String containerName = config.getTarget();

        if (!isDockerAvailable() || MockContainerRegistry.isUseMock()) {
            boolean running = MockContainerRegistry.isContainerRunning(containerName);
            long latencyMs = System.currentTimeMillis() - startTime;
            if (running) {
                return CheckResult.success(latencyMs, "Docker (MOCK) - Container '" + containerName + "' running");
            } else {
                return CheckResult.failure(latencyMs, "Docker (MOCK) - Container '" + containerName + "' stopped");
            }
        }

        String command = "docker inspect --format=\"{{.State.Running}}\" " + containerName;
        try {
            CommandExecutor.CommandResult result = CommandExecutor.execute(command, config.getTimeoutMs());
            long latencyMs = System.currentTimeMillis() - startTime;

            if (result.isSuccess() && "true".equalsIgnoreCase(result.getStdout().trim())) {
                return CheckResult.success(latencyMs, "Container running");
            } else {
                String error = result.getStderr().isEmpty() ? "Container State.Running is not true" : result.getStderr();
                return CheckResult.failure(latencyMs, "Docker inspect failed: " + error);
            }
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            return CheckResult.failure(latencyMs, "Docker command error: " + e.getMessage());
        }
    }
}
