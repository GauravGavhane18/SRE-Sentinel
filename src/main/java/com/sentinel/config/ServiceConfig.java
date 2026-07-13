package com.sentinel.config;

import com.sentinel.model.CheckType;

public class ServiceConfig {
    private String name;
    private CheckType type;
    private String target;
    private long checkIntervalMs;
    private int timeoutMs;
    private RetryPolicyConfig retryPolicy;
    private String recoveryCommand;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CheckType getType() {
        return type;
    }

    public void setType(CheckType type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public long getCheckIntervalMs() {
        return checkIntervalMs;
    }

    public void setCheckIntervalMs(long checkIntervalMs) {
        this.checkIntervalMs = checkIntervalMs;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public RetryPolicyConfig getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicyConfig retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public String getRecoveryCommand() {
        return recoveryCommand;
    }

    public void setRecoveryCommand(String recoveryCommand) {
        this.recoveryCommand = recoveryCommand;
    }
}
