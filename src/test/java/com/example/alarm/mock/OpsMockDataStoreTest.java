package com.example.alarm.mock;

import com.example.alarm.model.mock.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OpsMockDataStoreTest {

    private OpsMockDataStore store;

    @BeforeEach
    void setUp() {
        store = new OpsMockDataStore();
    }

    // ======== serviceExists ========

    @Test
    void serviceExists_knownServices_returnsTrue() {
        assertTrue(store.serviceExists("order-service"));
        assertTrue(store.serviceExists("payment-service"));
        assertTrue(store.serviceExists("inventory-service"));
        assertTrue(store.serviceExists("user-service"));
    }

    @Test
    void serviceExists_unknownService_returnsFalse() {
        assertFalse(store.serviceExists("unknown-service"));
        assertFalse(store.serviceExists(""));
        assertFalse(store.serviceExists("audit-service"));
    }

    // ======== getServiceStatus ========

    @Test
    void getServiceStatus_knownService_returnsCorrectStatus() {
        ServiceStatus status = store.getServiceStatus("order-service");
        assertEquals("order-service", status.getServiceName());
        assertEquals("healthy", status.getStatus());
        assertEquals("99.8%", status.getUptime());
        assertEquals(4, status.getInstanceCount());
        assertNotNull(status.getHealthCheckTime());
    }

    @Test
    void getServiceStatus_degradedService_returnsDegradedStatus() {
        ServiceStatus status = store.getServiceStatus("payment-service");
        assertEquals("payment-service", status.getServiceName());
        assertEquals("degraded", status.getStatus());
    }

    @Test
    void getServiceStatus_unknownService_returnsDefault() {
        ServiceStatus status = store.getServiceStatus("nonexistent");
        assertEquals("nonexistent", status.getServiceName());
        assertEquals("unknown", status.getStatus());
        assertEquals(0, status.getInstanceCount());
    }

    // ======== getAllServiceNames ========

    @Test
    void getAllServiceNames_returnsFourServiceNames() {
        Set<String> names = store.getAllServiceNames();
        assertNotNull(names);
        assertEquals(4, names.size());
        assertTrue(names.contains("order-service"));
        assertTrue(names.contains("payment-service"));
        assertTrue(names.contains("inventory-service"));
        assertTrue(names.contains("user-service"));
    }

    // ======== getErrorLogs ========

    @Test
    void getErrorLogs_knownServiceNoFilter_returnsErrorLogs() {
        List<ErrorLog> logs = store.getErrorLogs("order-service", null);
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        // order-service has 5 logs, all under the 10 limit
        assertEquals(5, logs.size());
        for (ErrorLog log : logs) {
            assertEquals("order-service", log.getServiceName());
        }
    }

    @Test
    void getErrorLogs_filterByTimeout_returnsMatchingLogs() {
        List<ErrorLog> logs = store.getErrorLogs("order-service", "timeout");
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        for (ErrorLog log : logs) {
            String msg = log.getMessage();
            assertNotNull(msg);
            // Each log message should contain timeout-related text (Chinese or English)
            boolean containsTimeout = msg.toLowerCase().contains("timeout")
                    || msg.contains("超时")
                    || msg.contains("延迟")
                    || msg.contains("等待");
            assertTrue(containsTimeout,
                    "Expected timeout-related message but got: " + msg);
        }
    }

    @Test
    void getErrorLogs_filterByError_returnsMatchingLogs() {
        List<ErrorLog> logs = store.getErrorLogs("payment-service", "error");
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        for (ErrorLog log : logs) {
            String msg = log.getMessage();
            assertNotNull(msg);
        }
    }

    @Test
    void getErrorLogs_unknownService_returnsEmptyList() {
        List<ErrorLog> logs = store.getErrorLogs("nonexistent", null);
        assertNotNull(logs);
        assertTrue(logs.isEmpty());
    }

    @Test
    void getErrorLogs_filterByDeploy_returnsMatchingLogs() {
        List<ErrorLog> logs = store.getErrorLogs("order-service", "deploy");
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        for (ErrorLog log : logs) {
            String msg = log.getMessage();
            assertNotNull(msg);
        }
    }

    // ======== getDeployRecords ========

    @Test
    void getDeployRecords_knownService_returnsRecords() {
        List<DeployRecord> records = store.getDeployRecords("order-service");
        assertNotNull(records);
        assertFalse(records.isEmpty());
        // order-service has 3 deploy records
        assertEquals(3, records.size());
        for (DeployRecord record : records) {
            assertEquals("order-service", record.getServiceName());
            assertNotNull(record.getVersion());
            assertNotNull(record.getDeployTime());
            assertNotNull(record.getStatus());
        }
    }

    @Test
    void getDeployRecords_unknownService_returnsEmptyList() {
        List<DeployRecord> records = store.getDeployRecords("unknown-service");
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    // ======== getDependencies ========

    @Test
    void getDependencies_orderService_returnsUpstreamAndDownstream() {
        List<ServiceDependency> deps = store.getDependencies("order-service");
        assertNotNull(deps);
        assertEquals(2, deps.size());

        // First dependency: upstream=user-service, downstream=payment-service
        ServiceDependency dep1 = deps.get(0);
        assertEquals("order-service", dep1.getServiceName());
        assertEquals("user-service", dep1.getUpstream());
        assertEquals("payment-service", dep1.getDownstream());
        assertEquals("healthy", dep1.getDependencyStatus());

        // Second dependency: upstream=null, downstream=inventory-service
        ServiceDependency dep2 = deps.get(1);
        assertEquals("order-service", dep2.getServiceName());
        assertNull(dep2.getUpstream());
        assertEquals("inventory-service", dep2.getDownstream());
    }

    @Test
    void getDependencies_unknownService_returnsEmptyList() {
        List<ServiceDependency> deps = store.getDependencies("unknown-service");
        assertNotNull(deps);
        assertTrue(deps.isEmpty());
    }

    // ======== getResourceMetrics ========

    @Test
    void getResourceMetrics_inventoryService_returnsCorrectValues() {
        ResourceMetrics metrics = store.getResourceMetrics("inventory-service");
        assertNotNull(metrics);
        assertEquals("inventory-service", metrics.getServiceName());
        assertEquals(100.0, metrics.getCpuPercent(), 0.001);
        assertEquals(90.0, metrics.getMemoryPercent(), 0.001);
        assertEquals(3500.0, metrics.getP99LatencyMs(), 0.001);
        assertEquals(3.1, metrics.getErrorRatePercent(), 0.001);
        assertEquals(20, metrics.getActiveConnections());
        assertEquals("healthy", metrics.getDbStatus());
    }

    @Test
    void getResourceMetrics_healthyService_returnsLowCpu() {
        ResourceMetrics metrics = store.getResourceMetrics("user-service");
        assertNotNull(metrics);
        assertEquals(45.0, metrics.getCpuPercent(), 0.001);
        assertEquals(0.5, metrics.getErrorRatePercent(), 0.001);
    }

    @Test
    void getResourceMetrics_unknownService_returnsDefault() {
        ResourceMetrics metrics = store.getResourceMetrics("nonexistent");
        assertNotNull(metrics);
        assertEquals("nonexistent", metrics.getServiceName());
        assertEquals(0.0, metrics.getCpuPercent(), 0.001);
        assertEquals(0.0, metrics.getMemoryPercent(), 0.001);
        assertEquals(0, metrics.getActiveConnections());
        assertEquals("unknown", metrics.getDbStatus());
    }
}
