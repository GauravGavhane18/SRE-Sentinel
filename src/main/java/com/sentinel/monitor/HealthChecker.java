package com.sentinel.monitor;

import com.sentinel.config.ServiceConfig;
import com.sentinel.model.CheckResult;

public interface HealthChecker {
    CheckResult check(ServiceConfig config);
}
