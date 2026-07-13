package com.sentinel.monitor;

import com.sentinel.config.ServiceConfig;
import com.sentinel.model.CheckResult;
import com.sentinel.utils.CommandExecutor;

public class HttpHealthChecker implements HealthChecker {

    @Override
    public CheckResult check(ServiceConfig config) {
        long startTime = System.currentTimeMillis();
        String url = config.getTarget();
        int timeoutSec = Math.max(1, config.getTimeoutMs() / 1000);

        String os = System.getProperty("os.name").toLowerCase();
        String discardOutput = os.contains("win") ? "NUL" : "/dev/null";
        
        // Construct curl command to output only the http status code
        String command = String.format("curl -s -o %s -w \"%%{http_code}\" --connect-timeout %d -m %d \"%s\"", 
                discardOutput, timeoutSec, timeoutSec + 2, url);

        try {
            CommandExecutor.CommandResult result = CommandExecutor.execute(command, config.getTimeoutMs() + 3000);
            long latencyMs = System.currentTimeMillis() - startTime;

            if (result.isSuccess()) {
                String stdout = result.getStdout().trim();
                stdout = stdout.replace("\"", "");
                try {
                    int statusCode = Integer.parseInt(stdout);
                    if (statusCode >= 200 && statusCode < 400) {
                        return CheckResult.success(latencyMs, "HTTP " + statusCode + " - OK (via curl)");
                    } else {
                        return CheckResult.failure(latencyMs, "HTTP status code: " + statusCode + " (via curl)");
                    }
                } catch (NumberFormatException e) {
                    return CheckResult.failure(latencyMs, "Unexpected output from curl: '" + stdout + "'");
                }
            } else {
                String err = result.getStderr().isEmpty() ? "curl execution failed" : result.getStderr();
                if (os.contains("win") && result.getException() != null) {
                    // Fallback to java HttpClient on Windows to facilitate local desktop run
                    return checkFallback(config, startTime);
                }
                return CheckResult.failure(latencyMs, "curl error: " + err);
            }
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            return CheckResult.failure(latencyMs, "HTTP check exception: " + e.getMessage());
        }
    }

    private CheckResult checkFallback(ServiceConfig config, long startTime) {
        try {
            java.net.URI uri = java.net.URI.create(config.getTarget());
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofMillis(config.getTimeoutMs()))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(java.time.Duration.ofMillis(config.getTimeoutMs()))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            long latencyMs = System.currentTimeMillis() - startTime;
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 400) {
                return CheckResult.success(latencyMs, "HTTP " + statusCode + " - OK (java fallback)");
            } else {
                return CheckResult.failure(latencyMs, "HTTP Status Code " + statusCode + " (java fallback)");
            }
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            return CheckResult.failure(latencyMs, "Fallback error: " + e.getMessage());
        }
    }
}
