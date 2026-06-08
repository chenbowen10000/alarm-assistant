package com.example.alarm.tools;

import com.example.alarm.mock.OpsMockDataStore;
import com.example.alarm.model.ToolResult;
import com.example.alarm.model.mock.ErrorLog;
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
class ErrorLogToolTest {

    @Mock
    private OpsMockDataStore dataStore;

    @InjectMocks
    private ErrorLogTool tool;

    @Test
    void queryErrors_nullAlarmType_returnsAllLogs() {
        List<ErrorLog> allLogs = Arrays.asList(
                new ErrorLog("2026-06-06T19:55:00", "ERROR", "order-service",
                        "接口超时: POST /api/order/create 耗时5230ms", "stack1"),
                new ErrorLog("2026-06-06T19:57:30", "WARN", "order-service",
                        "下游payment-service响应变慢", null)
        );
        when(dataStore.getErrorLogs("order-service", null)).thenReturn(allLogs);

        ToolResult result = tool.queryErrors("order-service", null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getToolName()).isEqualTo("ErrorLogTool");
        assertThat(result.getServiceName()).isEqualTo("order-service");
        assertThat(result.getData()).contains("接口超时");
        assertThat(result.getData()).contains("下游payment-service响应变慢");
    }

    @Test
    void queryErrors_timeoutAlarmType_filtersCorrectly() {
        List<ErrorLog> timeoutLogs = Arrays.asList(
                new ErrorLog("2026-06-06T19:55:00", "ERROR", "order-service",
                        "接口超时: POST /api/order/create 耗时5230ms", "stack1"),
                new ErrorLog("2026-06-06T19:56:12", "ERROR", "order-service",
                        "接口超时: POST /api/order/create 耗时4890ms", "stack2")
        );
        when(dataStore.getErrorLogs("order-service", "timeout")).thenReturn(timeoutLogs);

        ToolResult result = tool.queryErrors("order-service", "timeout");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).contains("超时");
    }

    @Test
    void queryErrors_unknownService_returnsEmptyMessage() {
        when(dataStore.getErrorLogs("unknown-svc", null)).thenReturn(Collections.emptyList());

        ToolResult result = tool.queryErrors("unknown-svc", null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("No matching error logs found");
    }

    @Test
    void queryErrors_dataStoreThrows_returnsErrorResult() {
        when(dataStore.getErrorLogs("bad-svc", null)).thenThrow(new RuntimeException("Connection timeout"));

        ToolResult result = tool.queryErrors("bad-svc", null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Connection timeout");
    }
}
