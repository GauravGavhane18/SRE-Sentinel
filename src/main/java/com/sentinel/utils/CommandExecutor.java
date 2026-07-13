package com.sentinel.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class CommandExecutor {

    public static class CommandResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;
        private final long durationMs;
        private final Exception exception;

        public CommandResult(int exitCode, String stdout, String stderr, long durationMs, Exception exception) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.durationMs = durationMs;
            this.exception = exception;
        }

        public boolean isSuccess() {
            return exception == null && exitCode == 0;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public Exception getException() {
            return exception;
        }
    }

    public static CommandResult execute(String command, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        String os = System.getProperty("os.name").toLowerCase();
        String[] shellCommand;

        if (os.contains("win")) {
            shellCommand = new String[]{"cmd.exe", "/c", command};
        } else {
            shellCommand = new String[]{"/bin/sh", "-c", command};
        }

        try {
            Process process = new ProcessBuilder(shellCommand).start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            // Asynchronously read stdout and stderr to avoid process deadlock
            Thread stdoutThread = new Thread(() -> readStream(process.getInputStream(), stdout));
            Thread stderrThread = new Thread(() -> readStream(process.getErrorStream(), stderr));

            stdoutThread.start();
            stderrThread.start();

            boolean completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            long durationMs = System.currentTimeMillis() - startTime;

            if (!completed) {
                process.destroyForcibly();
                stdoutThread.interrupt();
                stderrThread.interrupt();
                return new CommandResult(-1, stdout.toString(), "Command timed out after " + timeoutMs + "ms", durationMs, new java.util.concurrent.TimeoutException("Command timed out"));
            }

            stdoutThread.join(1000);
            stderrThread.join(1000);

            return new CommandResult(process.exitValue(), stdout.toString().trim(), stderr.toString().trim(), durationMs, null);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            return new CommandResult(-1, "", "", durationMs, e);
        }
    }

    private static void readStream(java.io.InputStream is, StringBuilder sb) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (Exception ignored) {
        }
    }
}
