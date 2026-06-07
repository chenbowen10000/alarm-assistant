package com.example.alarm.tools;

import com.example.alarm.mock.OpsMockDataStore;
import com.example.alarm.model.ToolResult;
import com.example.alarm.model.mock.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DependencyTool {

    private static final Logger log = LoggerFactory.getLogger(DependencyTool.class);
    private final OpsMockDataStore dataStore;

    public DependencyTool(OpsMockDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public ToolResult queryDependencies(String serviceName) {
        long start = System.currentTimeMillis();
        try {
            List<ServiceDependency> deps = dataStore.getDependencies(serviceName);
            if (deps.isEmpty()) {
                long latency = System.currentTimeMillis() - start;
                return ToolResult.success("DependencyTool", serviceName, "No dependency data", latency);
            }
            String data = deps.stream()
                    .map(d -> {
                        String rel = d.getUpstream() != null ? "upstream=" + d.getUpstream() : "downstream=" + d.getDownstream();
                        return rel + "[status=" + d.getDependencyStatus() + "]";
                    })
                    .collect(Collectors.joining(", "));
            long latency = System.currentTimeMillis() - start;
            log.info("[TOOL] DependencyTool service={}, success=true, count={}, latency={}ms", serviceName, deps.size(), latency);
            return ToolResult.success("DependencyTool", serviceName, data, latency);
        } catch (Exception e) {
            log.error("[TOOL] DependencyTool service={}, success=false, error={}", serviceName, e.getMessage());
            return ToolResult.error("DependencyTool", serviceName, e.getMessage());
        }
    }
}
