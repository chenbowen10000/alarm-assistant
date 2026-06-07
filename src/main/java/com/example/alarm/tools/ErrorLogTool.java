package com.example.alarm.tools;

import com.example.alarm.mock.OpsMockDataStore;
import com.example.alarm.model.ToolResult;
import com.example.alarm.model.mock.ErrorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ErrorLogTool {

    private static final Logger log = LoggerFactory.getLogger(ErrorLogTool.class);
    private final OpsMockDataStore dataStore;

    public ErrorLogTool(OpsMockDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public ToolResult queryErrors(String serviceName, String alarmType) {
        long start = System.currentTimeMillis();
        try {
            List<ErrorLog> logs = dataStore.getErrorLogs(serviceName, alarmType);
            if (logs.isEmpty()) {
                long latency = System.currentTimeMillis() - start;
                return ToolResult.success("ErrorLogTool", serviceName, "No matching error logs found", latency);
            }
            String data = logs.stream()
                    .map(l -> String.format("[%s][%s] %s", l.getTimestamp(), l.getLevel(), l.getMessage()))
                    .collect(Collectors.joining(" | "));
            long latency = System.currentTimeMillis() - start;
            log.info("[TOOL] ErrorLogTool service={}, success=true, count={}, latency={}ms", serviceName, logs.size(), latency);
            return ToolResult.success("ErrorLogTool", serviceName, data, latency);
        } catch (Exception e) {
            log.error("[TOOL] ErrorLogTool service={}, success=false, error={}", serviceName, e.getMessage());
            return ToolResult.error("ErrorLogTool", serviceName, e.getMessage());
        }
    }
}
