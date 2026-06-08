package com.example.alarm.tools;

import com.example.alarm.mock.OpsMockDataStore;
import com.example.alarm.model.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolsTest {

    private OpsMockDataStore dataStore;
    private ServiceStatusTool serviceStatusTool;
    private ErrorLogTool errorLogTool;
    private DeployRecordTool deployRecordTool;
    private DependencyTool dependencyTool;
    private ResourceMetricsTool resourceMetricsTool;

    @BeforeEach
    void setUp() {
        dataStore = new OpsMockDataStore();
        serviceStatusTool = new ServiceStatusTool(dataStore);
        errorLogTool = new ErrorLogTool(dataStore);
        deployRecordTool = new DeployRecordTool(dataStore);
        dependencyTool = new DependencyTool(dataStore);
        resourceMetricsTool = new ResourceMetricsTool(dataStore);
    }

    @Test
    void testServiceStatusToolSuccess() {
        ToolResult result = serviceStatusTool.queryStatus("order-service");
        assertTrue(result.isSuccess());
        assertTrue(result.getData().contains("healthy"));
    }

    @Test
    void testServiceStatusToolNotFound() {
        ToolResult result = serviceStatusTool.queryStatus("nonexistent");
        assertTrue(result.isSuccess());
        assertTrue(result.getData().contains("unknown"));
    }

    @Test
    void testErrorLogToolReturnsLogs() {
        ToolResult result = errorLogTool.queryErrors("order-service", "??");
        assertTrue(result.isSuccess());
        assertTrue(result.getData().length() > 10);
    }

    @Test
    void testErrorLogToolEmptyForUnknown() {
        ToolResult result = errorLogTool.queryErrors("nonexistent", "test");
        assertTrue(result.isSuccess());
    }

    @Test
    void testDeployRecordToolSuccess() {
        ToolResult result = deployRecordTool.queryDeploys("order-service");
        assertTrue(result.isSuccess());
        assertTrue(result.getData().contains("v2.3.1"));
    }

    @Test
    void testDependencyToolSuccess() {
        ToolResult result = dependencyTool.queryDependencies("order-service");
        assertTrue(result.isSuccess());
        assertTrue(result.getData().contains("payment-service") || result.getData().contains("user-service"));
    }

    @Test
    void testResourceMetricsToolSuccess() {
        ToolResult result = resourceMetricsTool.queryMetrics("order-service");
        assertTrue(result.isSuccess());
        assertTrue(result.getData().contains("CPU"));
    }
}
