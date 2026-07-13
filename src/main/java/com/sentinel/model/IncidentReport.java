package com.sentinel.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class IncidentReport {
    private String id;
    private String serviceName;
    private String timestamp;
    private CheckType checkType;
    private String failureReason;
    private long latencyMs;
    private int retryCount;
    private String recoveryAction;
    private String recoveryStatus;
    private long recoveryDurationMs;
    private String rootCause;
    private String diagnostics;

    public IncidentReport() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public IncidentReport(String serviceName, CheckType checkType, String failureReason, long latencyMs,
                          int retryCount, String recoveryAction, String recoveryStatus,
                          long recoveryDurationMs, String rootCause, String diagnostics) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.serviceName = serviceName;
        this.checkType = checkType;
        this.failureReason = failureReason;
        this.latencyMs = latencyMs;
        this.retryCount = retryCount;
        this.recoveryAction = recoveryAction;
        this.recoveryStatus = recoveryStatus;
        this.recoveryDurationMs = recoveryDurationMs;
        this.rootCause = rootCause;
        this.diagnostics = diagnostics;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public CheckType getCheckType() {
        return checkType;
    }

    public void setCheckType(CheckType checkType) {
        this.checkType = checkType;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getRecoveryAction() {
        return recoveryAction;
    }

    public void setRecoveryAction(String recoveryAction) {
        this.recoveryAction = recoveryAction;
    }

    public String getRecoveryStatus() {
        return recoveryStatus;
    }

    public void setRecoveryStatus(String recoveryStatus) {
        this.recoveryStatus = recoveryStatus;
    }

    public long getRecoveryDurationMs() {
        return recoveryDurationMs;
    }

    public void setRecoveryDurationMs(long recoveryDurationMs) {
        this.recoveryDurationMs = recoveryDurationMs;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(String diagnostics) {
        this.diagnostics = diagnostics;
    }
}
