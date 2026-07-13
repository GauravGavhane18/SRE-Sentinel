package com.sentinel.logging;

import com.sentinel.model.IncidentReport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class IncidentLogger {
    private static final String LOG_FILE_PATH = "logs/incidents.log";
    private final List<IncidentReport> incidentHistory;

    public IncidentLogger() {
        this.incidentHistory = new CopyOnWriteArrayList<>();
        initializeLogFile();
        loadIncidentHistory();
    }

    private void initializeLogFile() {
        File file = new File(LOG_FILE_PATH);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    private void loadIncidentHistory() {
        File file = new File(LOG_FILE_PATH);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (!line.startsWith("[")) continue; // Ignore diagnostic details lines for quick history load
                IncidentReport report = parseLogLine(line);
                if (report != null) {
                    incidentHistory.add(report);
                }
            }
        } catch (IOException e) {
            System.err.println("[Sentinel] Could not read incident logs: " + e.getMessage());
        }
    }

    private IncidentReport parseLogLine(String line) {
        try {
            int closeBracket = line.indexOf(']');
            if (closeBracket == -1) return null;
            String timestamp = line.substring(1, closeBracket);

            String rest = line.substring(closeBracket + 2);
            String[] tokens = rest.split(" \\| ");

            IncidentReport report = new IncidentReport();
            report.setTimestamp(timestamp);

            for (String token : tokens) {
                String[] kv = token.split("=");
                if (kv.length != 2) continue;
                String key = kv[0].trim();
                String val = kv[1].trim();

                switch (key) {
                    case "SERVICE":
                        report.setServiceName(val);
                        break;
                    case "TYPE":
                        report.setCheckType(com.sentinel.model.CheckType.valueOf(val));
                        break;
                    case "ATTEMPTS":
                        report.setRetryCount(Integer.parseInt(val));
                        break;
                    case "DURATION":
                        report.setRecoveryDurationMs(Long.parseLong(val.replace("ms", "")));
                        break;
                    case "STATUS":
                        report.setRecoveryStatus(val);
                        break;
                    case "RCA":
                        report.setRootCause(val.replace("\"", ""));
                        break;
                }
            }
            return report;
        } catch (Exception e) {
            return null;
        }
    }

    public synchronized void logIncident(IncidentReport report) {
        incidentHistory.add(report);

        // Standardized SRE log entry
        String logEntry = String.format("[%s] SERVICE=%s | TYPE=%s | ATTEMPTS=%d | DURATION=%dms | STATUS=%s | RCA=\"%s\"",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                report.getServiceName(),
                report.getCheckType().name(),
                report.getRetryCount(),
                report.getRecoveryDurationMs(),
                report.getRecoveryStatus(),
                report.getRootCause()
        );

        // Append to logs/incidents.log
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE_PATH, true))) {
            writer.println(logEntry);
            if (report.getDiagnostics() != null && !report.getDiagnostics().isEmpty()) {
                writer.println(report.getDiagnostics());
            }
            writer.println(); // Blank line delimiter
        } catch (IOException e) {
            System.err.println("[Sentinel] Failed to write incident to log: " + e.getMessage());
        }
    }

    public List<IncidentReport> getRecentIncidents(int count) {
        List<IncidentReport> all = new ArrayList<>(incidentHistory);
        if (all.size() <= count) {
            return all;
        }
        return all.subList(all.size() - count, all.size());
    }

    public List<IncidentReport> getIncidentHistory() {
        return new ArrayList<>(incidentHistory);
    }
}
