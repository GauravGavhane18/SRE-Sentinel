package com.sentinel.monitor;

import com.sentinel.config.ServiceConfig;
import com.sentinel.model.CheckResult;
import com.sentinel.utils.CommandExecutor;

public class TcpHealthChecker implements HealthChecker {

    @Override
    public CheckResult check(ServiceConfig config) {
        long startTime = System.currentTimeMillis();
        String target = config.getTarget();
        String[] parts = target.split(":");
        if (parts.length != 2) {
            return CheckResult.failure(0, "Invalid TCP target format. Expected 'host:port', got: " + target);
        }

        String host = parts[0];
        String portStr = parts[1];
        int timeoutSec = Math.max(1, config.getTimeoutMs() / 1000);

        String os = System.getProperty("os.name").toLowerCase();
        String command;
        
        if (os.contains("win")) {
            // Windows PowerShell equivalent
            command = String.format("powershell.exe -Command \"$t = Test-NetConnection -ComputerName %s -Port %s -WarningAction SilentlyContinue; exit [int](-not $t.TcpTestSucceeded)\"", host, portStr);
        } else {
            // Linux nc (netcat) port verification
            command = String.format("nc -z -w %d %s %s", timeoutSec, host, portStr);
        }

        try {
            CommandExecutor.CommandResult result = CommandExecutor.execute(command, config.getTimeoutMs() + 2000);
            long latencyMs = System.currentTimeMillis() - startTime;

            if (result.isSuccess()) {
                return CheckResult.success(latencyMs, "TCP Connection established to " + target + " (via nc)");
            } else {
                String err = result.getStderr().isEmpty() ? "Port closed or unreachable" : result.getStderr();
                if (os.contains("win") && result.getException() != null) {
                    // Fallback to java Socket connection on Windows developer machine
                    return checkFallback(host, portStr, config.getTimeoutMs(), startTime);
                }
                return CheckResult.failure(latencyMs, "TCP check failed: " + err + " (Exit code: " + result.getExitCode() + ")");
            }
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            return CheckResult.failure(latencyMs, "TCP check exception: " + e.getMessage());
        }
    }

    private CheckResult checkFallback(String host, String portStr, int timeoutMs, long startTime) {
        try {
            int port = Integer.parseInt(portStr);
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
                long latencyMs = System.currentTimeMillis() - startTime;
                return CheckResult.success(latencyMs, "TCP Connection established (java fallback)");
            }
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            return CheckResult.failure(latencyMs, "Fallback error: " + e.getMessage());
        }
    }
}
