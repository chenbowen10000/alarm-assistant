package com.example.alarm.tools;

import com.example.alarm.mock.OpsMockDataStore;
import com.example.alarm.model.ToolResult;
import com.example.alarm.model.mock.ServiceStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceStatusToolTest {

    @Mock
    private OpsMockDataStore dataStore;

    @InjectMocks
    private ServiceStatusTool tool;

    @Test
    void queryStatus_knownService_returnsHealthyStatus() {
        ServiceStatus mockStatus = new ServiceStatus("order-service", "healthy", "2026-06-06T20:00:00", "99.8%", 4);
        when(dataStore.getServiceStatus("order-service")).thenReturn(mockStatus);

        ToolResult result = tool.queryStatus("order-service");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getToolName()).isEqualTo("ServiceStatusTool");
        assertThat(result.getServiceName()).isEqualTo("order-service");
        assertThat(result.getData()).contains("status=healthy");
        assertThat(result.getData()).contains("instances=4");
        assertThat(result.getLatencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void queryStatus_unknownService_returnsDefaultUnknownStatus() {
        ServiceStatus defaultStatus = new ServiceStatus("unknown-svc", "unknown", "N/A", "N/A", 0);
        when(dataStore.getServiceStatus("unknown-svc")).thenReturn(defaultStatus);

        ToolResult result = tool.queryStatus("unknown-svc");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).contains("status=unknown");
        assertThat(result.getData()).contains("instances=0");
    }

    @Test
    void queryStatus_dataStoreThrows_returnsErrorResult() {
        when(dataStore.getServiceStatus("bad-svc")).thenThrow(new RuntimeException("DB connection failed"));

        ToolResult result = tool.queryStatus("bad-svc");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getToolName()).isEqualTo("ServiceStatusTool");
        assertThat(result.getErrorMessage()).contains("DB connection failed");
    }
}
