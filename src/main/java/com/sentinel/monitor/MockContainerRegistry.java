package com.sentinel.monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockContainerRegistry {
    private static final Map<String, Boolean> containerStates = new ConcurrentHashMap<>();
    private static boolean useMock = false;

    static {
        // Pre-register some mock containers
        containerStates.put("sentinel-nginx", true);
        containerStates.put("sentinel-redis", true);
        containerStates.put("sentinel-postgres", true);
        containerStates.put("backup-daemon", true);
    }

    public static boolean isUseMock() {
        return useMock;
    }

    public static void setUseMock(boolean mock) {
        useMock = mock;
    }

    public static boolean isContainerRunning(String containerName) {
        return containerStates.getOrDefault(containerName, false);
    }

    public static void setContainerStatus(String containerName, boolean running) {
        containerStates.put(containerName, running);
    }

    public static Map<String, Boolean> getContainerStates() {
        return containerStates;
    }
}
