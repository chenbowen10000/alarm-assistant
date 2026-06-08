package com.example.alarm.tools;

import com.example.alarm.mock.OpsMockDataStore;
import com.example.alarm.model.ToolResult;
import com.example.alarm.model.mock.ResourceMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceMetricsToolTest {

    @Mock
    private OpsMockDataStore dataStore;

    @InjectMocks
    private ResourceMetricsTool tool;

    @Test
    void queryMetrics_knownService_returnsCpuMemoryP99LatencyValues() {
        ResourceMetrics mockMetrics = new ResourceMetrics("order-service", 72.5, 65.0, 5200.0, 15.3, 50, "slow");
        when(dataStore.getResourceMetrics("order-service")).thenReturn(mockMetrics);

        ToolResult result = tool.queryMetrics("order-service");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getToolName()).isEqualTo("ResourceMetricsTool");
        assertThat(result.getServiceName()).isEqualTo("order-service");
        assertThat(result.getData()).contains("CPU=72.5%");
        assertThat(result.getData()).contains("Memory=65.0%");
        assertThat(result.getData()).contains("P99Latency=5200ms");
        assertThat(result.getData()).contains("DB=slow");
    }

    @Test
    void queryMetrics_unknownService_returnsDefaultZeros() {
        ResourceMetrics defaultMetrics = new ResourceMetrics("unknown-svc", 0, 0, 0, 0, 0, "unknown");
        when(dataStore.getResourceMetrics("unknown-svc")).thenReturn(defaultMetrics);

        ToolResult result = tool.queryMetrics("unknown-svc");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).contains("CPU=0.0%");
        assertThat(result.getData()).contains("Memory=0.0%");
        assertThat(result.getData()).contains("P99Latency=0ms");
        assertThat(result.getData()).contains("DB=unknown");
    }

    @Test
    void queryMetrics_dataStoreThrows_returnsErrorResult() {
        when(dataStore.getResourceMetrics("bad-svc")).thenThrow(new RuntimeException("Metrics collection failed"));

        ToolResult result = tool.queryMetrics("bad-svc");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Metrics collection failed");
    }
}
