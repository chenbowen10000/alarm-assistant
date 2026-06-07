package com.example.alarm.tools;

import com.example.alarm.mock.OpsMockDataStore;
import com.example.alarm.model.ToolResult;
import com.example.alarm.model.mock.ServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ServiceStatusTool {

    private static final Logger log = LoggerFactory.getLogger(ServiceStatusTool.class);
    private final OpsMockDataStore dataStore;

    public ServiceStatusTool(OpsMockDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public ToolResult queryStatus(String serviceName) {
        long start = System.currentTimeMillis();
        try {
            ServiceStatus status = dataStore.getServiceStatus(serviceName);
            String data = String.format("status=%s, uptime=%s, instances=%d, lastCheck=%s",
                    status.getStatus(), status.getUptime(), status.getInstanceCount(), status.getHealthCheckTime());
            long latency = System.currentTimeMillis() - start;
            log.info("[TOOL] ServiceStatusTool service={}, success=true, latency={}ms", serviceName, latency);
            return ToolResult.success("ServiceStatusTool", serviceName, data, latency);
        } catch (Exception e) {
            log.error("[TOOL] ServiceStatusTool service={}, success=false, error={}", serviceName, e.getMessage());
            return ToolResult.error("ServiceStatusTool", serviceName, e.getMessage());
        }
    }
}
