package com.sentinel.model;

public enum HealthStatus {
    HEALTHY,      // Service is UP and normal
    DEGRADED,     // Service failed check, currently undergoing backoff retries
    UNHEALTHY,    // Service failed all retries, recovery is being attempted/failed
    UNKNOWN       // Service status not yet verified
}
