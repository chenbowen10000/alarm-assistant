package com.example.alarm.tools;

import com.example.alarm.mock.OpsMockDataStore;
import com.example.alarm.model.ToolResult;
import com.example.alarm.model.mock.ServiceDependency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DependencyToolTest {

    @Mock
    private OpsMockDataStore dataStore;

    @InjectMocks
    private DependencyTool tool;

    @Test
    void queryDependencies_knownService_returnsUpstreamDownstreamInfo() {
        List<ServiceDependency> deps = Arrays.asList(
                new ServiceDependency("order-service", "user-service", "payment-service", "healthy"),
                new ServiceDependency("order-service", null, "inventory-service", "healthy")
        );
        when(dataStore.getDependencies("order-service")).thenReturn(deps);

        ToolResult result = tool.queryDependencies("order-service");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getToolName()).isEqualTo("DependencyTool");
        assertThat(result.getServiceName()).isEqualTo("order-service");
        assertThat(result.getData()).contains("upstream=user-service");
        assertThat(result.getData()).contains("downstream=inventory-service");
        assertThat(result.getData()).contains("downstream=inventory-service");
        assertThat(result.getData()).contains("[status=healthy]");
    }

    @Test
    void queryDependencies_unknownService_returnsNoDependencyData() {
        when(dataStore.getDependencies("unknown-svc")).thenReturn(Collections.emptyList());

        ToolResult result = tool.queryDependencies("unknown-svc");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("No dependency data");
    }

    @Test
    void queryDependencies_dataStoreThrows_returnsErrorResult() {
        when(dataStore.getDependencies("bad-svc")).thenThrow(new RuntimeException("Network error"));

        ToolResult result = tool.queryDependencies("bad-svc");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Network error");
    }
}
