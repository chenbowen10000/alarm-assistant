package com.example.alarm.tools;

import com.example.alarm.mock.OpsMockDataStore;
import com.example.alarm.model.ToolResult;
import com.example.alarm.model.mock.ResourceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ResourceMetricsTool {

    private static final Logger log = LoggerFactory.getLogger(ResourceMetricsTool.class);
    private final OpsMockDataStore dataStore;

    public ResourceMetricsTool(OpsMockDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public ToolResult queryMetrics(String serviceName) {
        long start = System.currentTimeMillis();
        try {
            ResourceMetrics metrics = dataStore.getResourceMetrics(serviceName);
            String data = String.format("CPU=%.1f%%, Memory=%.1f%%, P99Latency=%.0fms, ErrorRate=%.2f%%, DB=%s, Connections=%d",
                    metrics.getCpuPercent(), metrics.getMemoryPercent(),
                    metrics.getP99LatencyMs(), metrics.getErrorRatePercent(),
                    metrics.getDbStatus(), metrics.getActiveConnections());
            long latency = System.currentTimeMillis() - start;
            log.info("[TOOL] ResourceMetricsTool service={}, success=true, latency={}ms", serviceName, latency);
            return ToolResult.success("ResourceMetricsTool", serviceName, data, latency);
        } catch (Exception e) {
            log.error("[TOOL] ResourceMetricsTool service={}, success=false, error={}", serviceName, e.getMessage());
            return ToolResult.error("ResourceMetricsTool", serviceName, e.getMessage());
        }
    }
}
