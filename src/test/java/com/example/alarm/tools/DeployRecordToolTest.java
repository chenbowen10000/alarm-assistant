package com.example.alarm.tools;

import com.example.alarm.mock.OpsMockDataStore;
import com.example.alarm.model.ToolResult;
import com.example.alarm.model.mock.DeployRecord;
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
class DeployRecordToolTest {

    @Mock
    private OpsMockDataStore dataStore;

    @InjectMocks
    private DeployRecordTool tool;

    @Test
    void queryDeploys_knownService_returnsRecordsWithVersionAndDeployTime() {
        List<DeployRecord> records = Arrays.asList(
                new DeployRecord("order-service", "v2.3.1", "2026-06-06T19:45:00", "completed",
                        "重构订单创建流程"),
                new DeployRecord("order-service", "v2.3.0", "2026-06-05T14:00:00", "completed",
                        "修复订单状态更新bug")
        );
        when(dataStore.getDeployRecords("order-service")).thenReturn(records);

        ToolResult result = tool.queryDeploys("order-service");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getToolName()).isEqualTo("DeployRecordTool");
        assertThat(result.getServiceName()).isEqualTo("order-service");
        assertThat(result.getData()).contains("v2.3.1");
        assertThat(result.getData()).contains("2026-06-06T19:45:00");
        assertThat(result.getData()).contains("completed");
    }

    @Test
    void queryDeploys_unknownService_returnsNoDeployRecordsFound() {
        when(dataStore.getDeployRecords("unknown-svc")).thenReturn(Collections.emptyList());

        ToolResult result = tool.queryDeploys("unknown-svc");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("No deploy records found");
    }

    @Test
    void queryDeploys_dataStoreThrows_returnsErrorResult() {
        when(dataStore.getDeployRecords("bad-svc")).thenThrow(new RuntimeException("DB unavailable"));

        ToolResult result = tool.queryDeploys("bad-svc");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("DB unavailable");
    }
}
