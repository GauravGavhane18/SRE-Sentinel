package com.sentinel.monitor;

import com.sentinel.utils.CommandExecutor;

public class SystemHealthMonitor {

    public static class SystemMetrics {
        public String cpuLoad = "n/a";
        public String memoryUsage = "n/a";
        public String diskUsage = "n/a";
        public String uptime = "n/a";
        public int runningProcesses = 0;
        public int zombieProcesses = 0;
    }

    public static SystemMetrics collect() {
        SystemMetrics metrics = new SystemMetrics();
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Mock data or simple Windows commands for developer local experience
            metrics.cpuLoad = "0.12, 0.25, 0.18 (MOCK Windows)";
            metrics.memoryUsage = "Used: 4096MB / Total: 16384MB (MOCK)";
            metrics.diskUsage = "Used: 65% (C:\\ 120GB free of 350GB)";
            metrics.uptime = "up 1 hour, 45 minutes";
            metrics.runningProcesses = 112;
            metrics.zombieProcesses = 0;
            return metrics;
        }

        // --- CPU Load ---
        CommandExecutor.CommandResult loadResult = CommandExecutor.execute("cat /proc/loadavg", 1000);
        if (loadResult.isSuccess()) {
            String[] parts = loadResult.getStdout().split("\\s+");
            if (parts.length >= 3) {
                metrics.cpuLoad = parts[0] + ", " + parts[1] + ", " + parts[2];
            }
        } else {
            CommandExecutor.CommandResult uptimeResult = CommandExecutor.execute("uptime", 1000);
            if (uptimeResult.isSuccess()) {
                String stdout = uptimeResult.getStdout();
                int loadIdx = stdout.indexOf("load average:");
                if (loadIdx != -1) {
                    metrics.cpuLoad = stdout.substring(loadIdx + 13).trim();
                }
            }
        }

        // --- Memory Usage ---
        CommandExecutor.CommandResult memResult = CommandExecutor.execute("free -m", 1000);
        if (memResult.isSuccess()) {
            String[] lines = memResult.getStdout().split("\n");
            for (String line : lines) {
                if (line.startsWith("Mem:")) {
                    String[] tokens = line.split("\\s+");
                    if (tokens.length >= 4) {
                        String total = tokens[1];
                        String used = tokens[2];
                        String free = tokens[3];
                        metrics.memoryUsage = "Used: " + used + "MB / Total: " + total + "MB (Free: " + free + "MB)";
                    }
                }
            }
        }

        // --- Disk Usage ---
        CommandExecutor.CommandResult diskResult = CommandExecutor.execute("df -h / | tail -n 1", 1000);
        if (diskResult.isSuccess()) {
            String[] tokens = diskResult.getStdout().split("\\s+");
            if (tokens.length >= 6) {
                String size = tokens[1];
                String used = tokens[2];
                String avail = tokens[3];
                String usePct = tokens[4];
                metrics.diskUsage = "Used: " + usePct + " (" + avail + " free of " + size + ")";
            }
        }

        // --- Uptime ---
        CommandExecutor.CommandResult uptimeRes = CommandExecutor.execute("uptime -p", 1000);
        if (uptimeRes.isSuccess()) {
            metrics.uptime = uptimeRes.getStdout().trim();
        } else {
            metrics.uptime = "n/a";
        }

        // --- Running & Zombie Processes ---
        CommandExecutor.CommandResult psResult = CommandExecutor.execute("ps -eo state", 2000);
        if (psResult.isSuccess()) {
            String[] states = psResult.getStdout().split("\n");
            int running = 0;
            int zombies = 0;
            for (String state : states) {
                state = state.trim();
                if (!state.isEmpty()) {
                    running++;
                    if ("Z".equals(state)) {
                        zombies++;
                    }
                }
            }
            metrics.runningProcesses = running;
            metrics.zombieProcesses = zombies;
        }

        return metrics;
    }
}
