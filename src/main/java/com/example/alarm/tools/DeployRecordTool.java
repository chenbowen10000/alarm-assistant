package com.example.alarm.tools;

import com.example.alarm.mock.OpsMockDataStore;
import com.example.alarm.model.ToolResult;
import com.example.alarm.model.mock.DeployRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeployRecordTool {

    private static final Logger log = LoggerFactory.getLogger(DeployRecordTool.class);
    private final OpsMockDataStore dataStore;

    public DeployRecordTool(OpsMockDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public ToolResult queryDeploys(String serviceName) {
        long start = System.currentTimeMillis();
        try {
            List<DeployRecord> records = dataStore.getDeployRecords(serviceName);
            if (records.isEmpty()) {
                long latency = System.currentTimeMillis() - start;
                return ToolResult.success("DeployRecordTool", serviceName, "No deploy records found", latency);
            }
            String data = records.stream()
                    .map(r -> String.format("v%s at %s [%s] - %s", r.getVersion(), r.getDeployTime(), r.getStatus(), r.getChangeLog()))
                    .collect(Collectors.joining(" | "));
            long latency = System.currentTimeMillis() - start;
            log.info("[TOOL] DeployRecordTool service={}, success=true, count={}, latency={}ms", serviceName, records.size(), latency);
            return ToolResult.success("DeployRecordTool", serviceName, data, latency);
        } catch (Exception e) {
            log.error("[TOOL] DeployRecordTool service={}, success=false, error={}", serviceName, e.getMessage());
            return ToolResult.error("DeployRecordTool", serviceName, e.getMessage());
        }
    }
}
