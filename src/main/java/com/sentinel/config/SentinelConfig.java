package com.sentinel.config;

import java.util.List;

public class SentinelConfig {
    private int webServerPort;
    private List<ServiceConfig> services;

    public int getWebServerPort() {
        return webServerPort;
    }

    public void setWebServerPort(int webServerPort) {
        this.webServerPort = webServerPort;
    }

    public List<ServiceConfig> getServices() {
        return services;
    }

    public void setServices(List<ServiceConfig> services) {
        this.services = services;
    }
}
