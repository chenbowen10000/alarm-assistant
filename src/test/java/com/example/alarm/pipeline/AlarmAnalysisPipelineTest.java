package com.example.alarm.pipeline;

import com.example.alarm.mock.OpsMockDataStore;
import com.example.alarm.model.ToolResult;
import com.example.alarm.pipeline.AlarmAnalysisPipeline.PipelineResult;
import com.example.alarm.pipeline.ModelFallbackHandler.ModelResult;
import com.example.alarm.tools.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlarmAnalysisPipelineTest {

    @Mock private ModelFallbackHandler modelFallback;
    @Mock private ServiceStatusTool serviceStatusTool;
    @Mock private ErrorLogTool errorLogTool;
    @Mock private DeployRecordTool deployRecordTool;
    @Mock private DependencyTool dependencyTool;
    @Mock private ResourceMetricsTool resourceMetricsTool;
    @Mock private OpsMockDataStore dataStore;

    @InjectMocks
    private AlarmAnalysisPipeline pipeline;

    private static final String PARSE_JSON = "{\"serviceName\":\"order-service\",\"alarmType\":\"接口超时\"," +
            "\"keyMetrics\":{\"P99延迟\":\"5200ms\"},\"riskLevel\":\"P1\",\"userImpact\":true," +
            "\"needsEscalation\":false,\"confidence\":0.85}";

    private static final String RCA_TEXT = "风险等级：P1\n根因：order-service在v2.3.1发布后数据库连接池配置不当导致接口超时\n" +
            "用户影响：用户下单失败\n建议：建议回滚至v2.3.0并调整连接池配置";

    // ======== Test: Full pipeline success ========
    @Test
    void analyze_validAlarm_fullPipelineSucceeds() {
        when(modelFallback.callWithFallback(contains("运维告警解析专家"), contains("order-service")))
                .thenReturn(new ModelResult(PARSE_JSON, "deepseek-chat", false));
        when(modelFallback.callWithFallback(contains("资深运维工程师"), anyString()))
                .thenReturn(new ModelResult(RCA_TEXT, "deepseek-chat", false));
        when(serviceStatusTool.queryStatus("order-service"))
                .thenReturn(ToolResult.success("ServiceStatusTool", "order-service", "status=healthy", 1));
        when(resourceMetricsTool.queryMetrics("order-service"))
                .thenReturn(ToolResult.success("ResourceMetricsTool", "order-service", "CPU=72.5%", 1));
        when(errorLogTool.queryErrors("order-service", "接口超时"))
                .thenReturn(ToolResult.success("ErrorLogTool", "order-service", "timeout errors found", 2));
        when(deployRecordTool.queryDeploys("order-service"))
                .thenReturn(ToolResult.success("DeployRecordTool", "order-service", "v2.3.1 at 2026-06-06T19:45:00", 1));
        when(dependencyTool.queryDependencies("order-service"))
                .thenReturn(ToolResult.success("DependencyTool", "order-service", "dep data", 1));

        PipelineResult result = pipeline.analyze("order-service接口超时，P99延迟5200ms，影响用户下单，刚发布了v2.3.1版本");

        assertThat(result.getAnalysisId()).isNotNull();
        assertThat(result.getReport()).isNotNull();
        assertThat(result.getReport().getAnalysisStatus()).isEqualTo("SUCCESS");
        assertThat(result.getReport().getServiceName()).isEqualTo("order-service");
        assertThat(result.getReport().getRiskLevel()).isEqualTo("P1");
        assertThat(result.getReport().getAlarmType()).isEqualTo("接口超时");
        assertThat(result.getReport().getConfidence()).isGreaterThan(0.0);
        assertThat(result.getReport().getRecommendedActions()).isNotEmpty();
        assertThat(result.getReport().getEvidence()).isNotEmpty();
        assertThat(result.getReport().isShouldRollback()).isTrue();
        assertThat(result.getToolResults()).hasSize(5);
        assertThat(result.getPipelineLatency()).containsKeys("parse", "toolInvocation", "evidenceAggregate",
                "rootCauseAnalysis", "reportGeneration", "total");
        assertThat(result.isFallbackActivated()).isFalse();
        assertThat(result.getModelUsed()).isEqualTo("deepseek-chat");
    }

    // ======== Test: No alarm signal → parse failed ========
    @Test
    void analyze_noAlarmSignal_returnsParseFailed() {
        PipelineResult result = pipeline.analyze("今天天气真好");

        assertThat(result.getReport().getAnalysisStatus()).isEqualTo("PARSE_FAILED");
        assertThat(result.getReport().getServiceName()).isEqualTo("unknown");
        assertThat(result.getReport().getConfidence()).isEqualTo(0.0);
        assertThat(result.getReport().getRiskLevel()).isEqualTo("N/A");
        assertThat(result.getToolResults()).isEmpty();
        assertThat(result.isFallbackActivated()).isTrue();

        verify(modelFallback, never()).callWithFallback(anyString(), anyString());
    }

    // ======== Test: Empty alarm text ========
    @Test
    void analyze_emptyText_returnsParseFailed() {
        PipelineResult result = pipeline.analyze("");

        assertThat(result.getReport().getAnalysisStatus()).isEqualTo("PARSE_FAILED");
    }

    // ======== Test: Null alarm text ========
    @Test
    void analyze_nullText_returnsParseFailed() {
        PipelineResult result = pipeline.analyze(null);

        assertThat(result.getReport().getAnalysisStatus()).isEqualTo("PARSE_FAILED");
    }

    @Test
    void analyze_unknownParsedService_returnsParseFailedAndSkipsTools() {
        String unknownServiceJson = "{\"serviceName\":\"billing-service\",\"alarmType\":\"接口超时\","
                + "\"keyMetrics\":{\"P99延迟\":\"5200ms\"},\"riskLevel\":\"P1\",\"userImpact\":true,"
                + "\"needsEscalation\":true,\"confidence\":0.9}";
        when(modelFallback.callWithFallback(anyString(), contains("billing-service")))
                .thenReturn(new ModelResult(unknownServiceJson, "deepseek-chat", false));

        PipelineResult result = pipeline.analyze("billing-service接口超时，P99延迟5200ms，影响用户");

        assertThat(result.getReport().getAnalysisStatus()).isEqualTo("PARSE_FAILED");
        assertThat(result.getToolResults()).isEmpty();
        verify(serviceStatusTool, never()).queryStatus(anyString());
        verify(resourceMetricsTool, never()).queryMetrics(anyString());
        verify(errorLogTool, never()).queryErrors(anyString(), anyString());
        verify(deployRecordTool, never()).queryDeploys(anyString());
        verify(dependencyTool, never()).queryDependencies(anyString());
    }

    // ======== Test: Fallback model used ========
    @Test
    void analyze_fallbackActivated_flagSetInResult() {
        when(modelFallback.callWithFallback(contains("运维告警解析专家"), contains("payment")))
                .thenReturn(new ModelResult(PARSE_JSON.replace("order-service", "payment-service")
                        .replace("接口超时", "数据库异常"), "qwen-turbo", true));
        when(modelFallback.callWithFallback(contains("资深运维工程师"), anyString()))
                .thenReturn(new ModelResult(RCA_TEXT, "qwen-turbo", true));
        when(serviceStatusTool.queryStatus("payment-service"))
                .thenReturn(ToolResult.success("ServiceStatusTool", "payment-service", "status=degraded", 1));
        when(resourceMetricsTool.queryMetrics("payment-service"))
                .thenReturn(ToolResult.success("ResourceMetricsTool", "payment-service", "CPU=85%", 1));
        when(errorLogTool.queryErrors("payment-service", "数据库异常"))
                .thenReturn(ToolResult.success("ErrorLogTool", "payment-service", "db errors", 1));
        when(deployRecordTool.queryDeploys("payment-service"))
                .thenReturn(ToolResult.success("DeployRecordTool", "payment-service", "no records", 0));
        when(dependencyTool.queryDependencies("payment-service"))
                .thenReturn(ToolResult.success("DependencyTool", "payment-service", "dep data", 1));

        PipelineResult result = pipeline.analyze("payment-service数据库连接池耗尽，大量支付失败");

        assertThat(result.isFallbackActivated()).isTrue();
        assertThat(result.getModelUsed()).isEqualTo("qwen-turbo");
    }

    // ======== Test: Tool failures don''t block pipeline ========
    @Test
    void analyze_toolFailure_doesNotBlockPipeline() {
        String toolFailureRca = "风险等级：P2\n根因：order-service部分超时";
        when(modelFallback.callWithFallback(contains("运维告警解析专家"), contains("order-service")))
                .thenReturn(new ModelResult(PARSE_JSON, "deepseek-chat", false));
        when(modelFallback.callWithFallback(contains("资深运维工程师"), anyString()))
                .thenReturn(new ModelResult(toolFailureRca, "deepseek-chat", false));
        when(serviceStatusTool.queryStatus("order-service"))
                .thenReturn(ToolResult.error("ServiceStatusTool", "order-service", "timeout"));
        when(resourceMetricsTool.queryMetrics("order-service"))
                .thenReturn(ToolResult.error("ResourceMetricsTool", "order-service", "timeout"));
        when(errorLogTool.queryErrors("order-service", "接口超时"))
                .thenReturn(ToolResult.error("ErrorLogTool", "order-service", "timeout"));
        when(deployRecordTool.queryDeploys("order-service"))
                .thenReturn(ToolResult.error("DeployRecordTool", "order-service", "timeout"));
        when(dependencyTool.queryDependencies("order-service"))
                .thenReturn(ToolResult.error("DependencyTool", "order-service", "timeout"));

        PipelineResult result = pipeline.analyze("order-service接口超时严重");

        assertThat(result.getReport().getAnalysisStatus()).isEqualTo("SUCCESS");
        assertThat(result.getToolResults()).hasSize(5);
        assertThat(result.getToolResults().stream().allMatch(r -> !r.isSuccess())).isTrue();
    }

    // ======== Test: inventory-service alarm ========
    @Test
    void analyze_inventoryServiceCPU_parsesCorrectly() {
        String invJson = "{\"serviceName\":\"inventory-service\",\"alarmType\":\"CPU异常\"," +
                "\"keyMetrics\":{\"CPU\":\"100%\"},\"riskLevel\":\"P0\",\"userImpact\":false," +
                "\"needsEscalation\":true,\"confidence\":0.9}";
        String invRca = "风险等级：P0\n根因：inventory-service批量查询未做性能测试导致CPU飙升";

        when(modelFallback.callWithFallback(contains("运维告警解析专家"), anyString()))
                .thenReturn(new ModelResult(invJson, "deepseek-chat", false));
        when(modelFallback.callWithFallback(contains("资深运维工程师"), anyString()))
                .thenReturn(new ModelResult(invRca, "deepseek-chat", false));
        when(serviceStatusTool.queryStatus("inventory-service"))
                .thenReturn(ToolResult.success("ServiceStatusTool", "inventory-service", "status=degraded", 1));
        when(resourceMetricsTool.queryMetrics("inventory-service"))
                .thenReturn(ToolResult.success("ResourceMetricsTool", "inventory-service", "CPU=100%", 1));
        when(errorLogTool.queryErrors("inventory-service", "CPU异常"))
                .thenReturn(ToolResult.success("ErrorLogTool", "inventory-service", "cpu errors", 1));
        when(deployRecordTool.queryDeploys("inventory-service"))
                .thenReturn(ToolResult.success("DeployRecordTool", "inventory-service", "v1.5.2", 1));
        when(dependencyTool.queryDependencies("inventory-service"))
                .thenReturn(ToolResult.success("DependencyTool", "inventory-service", "deps", 1));

        PipelineResult result = pipeline.analyze("inventory-service CPU使用率100%，系统负载高");

        assertThat(result.getReport().getServiceName()).isEqualTo("inventory-service");
        assertThat(result.getReport().getAlarmType()).isEqualTo("CPU异常");
        assertThat(result.getReport().getRiskLevel()).isEqualTo("P0");
    }

    // ======== Test: user-service alarm ========
    @Test
    void analyze_userServiceDatabase_parsesCorrectly() {
        String userJson = "{\"serviceName\":\"user-service\",\"alarmType\":\"数据库异常\"," +
                "\"keyMetrics\":{\"错误数\":\"大量\"},\"riskLevel\":\"P1\",\"userImpact\":true," +
                "\"needsEscalation\":false,\"confidence\":0.8}";

        when(modelFallback.callWithFallback(contains("运维告警解析专家"), anyString()))
                .thenReturn(new ModelResult(userJson, "deepseek-chat", false));
        when(modelFallback.callWithFallback(contains("资深运维工程师"), anyString()))
                .thenReturn(new ModelResult(RCA_TEXT, "deepseek-chat", false));
        when(serviceStatusTool.queryStatus("user-service"))
                .thenReturn(ToolResult.success("ServiceStatusTool", "user-service", "status=healthy", 1));
        when(resourceMetricsTool.queryMetrics("user-service"))
                .thenReturn(ToolResult.success("ResourceMetricsTool", "user-service", "db=error", 1));
        when(errorLogTool.queryErrors("user-service", "数据库异常"))
                .thenReturn(ToolResult.success("ErrorLogTool", "user-service", "db errors", 1));
        when(deployRecordTool.queryDeploys("user-service"))
                .thenReturn(ToolResult.success("DeployRecordTool", "user-service", "v4.0.1", 1));
        when(dependencyTool.queryDependencies("user-service"))
                .thenReturn(ToolResult.success("DependencyTool", "user-service", "deps", 1));

        PipelineResult result = pipeline.analyze("user-service数据库连接异常，出现大量Connection refused");

        assertThat(result.getReport().getServiceName()).isEqualTo("user-service");
        assertThat(result.getReport().getAlarmType()).isEqualTo("数据库异常");
        assertThat(result.getReport().getFollowUpMetrics()).isNotEmpty();
        assertThat(result.getReport().getFollowUpMetrics()).contains("user-service服务健康状态");
    }

    // ======== Test: Heuristic parse fallback (when JSON parse fails) ========
    @Test
    void analyze_jsonParseFails_usesHeuristicParse() {
        // Return invalid JSON that will cause parseJsonResult to throw → triggers heuristicParse
        String badJson = "{\n  bad: invalid json <<<\n}";
        when(modelFallback.callWithFallback(contains("运维告警解析专家"), contains("order-service")))
                .thenReturn(new ModelResult(badJson, "deepseek-chat", false));
        when(modelFallback.callWithFallback(contains("资深运维工程师"), anyString()))
                .thenReturn(new ModelResult(RCA_TEXT, "deepseek-chat", false));
        when(serviceStatusTool.queryStatus("order-service"))
                .thenReturn(ToolResult.success("ServiceStatusTool", "order-service", "healthy", 1));
        when(resourceMetricsTool.queryMetrics("order-service"))
                .thenReturn(ToolResult.success("ResourceMetricsTool", "order-service", "ok", 1));
        when(errorLogTool.queryErrors("order-service", "接口超时"))
                .thenReturn(ToolResult.success("ErrorLogTool", "order-service", "logs", 1));
        when(deployRecordTool.queryDeploys("order-service"))
                .thenReturn(ToolResult.success("DeployRecordTool", "order-service", "deploys", 1));
        when(dependencyTool.queryDependencies("order-service"))
                .thenReturn(ToolResult.success("DependencyTool", "order-service", "deps", 1));

        PipelineResult result = pipeline.analyze("order-service接口超时，P99延迟超过5秒");

        assertThat(result.getReport().getAnalysisStatus()).isEqualTo("SUCCESS");
        assertThat(result.getReport().getServiceName()).isEqualTo("order-service");
        // Heuristic parse sets confidence to 0.3
        assertThat(result.getReport().getConfidence()).isGreaterThanOrEqualTo(0.3);
    }

    // ======== Test: P0 risk level ========
    @Test
    void analyze_P0RiskLevel_setsEscalation() {
        String p0Json = "{\n" +
                "  \"serviceName\": \"order-service\",\n" +
                "  \"alarmType\": \"综合告警\",\n" +
                "  \"keyMetrics\": {},\n" +
                "  \"riskLevel\": \"P0\",\n" +
                "  \"userImpact\": true,\n" +
                "  \"needsEscalation\": true,\n" +
                "  \"confidence\": 0.95\n" +
                "}";
        String p0Rca = "风险等级：P0\n根因：order-service全面宕机\n用户影响：所有用户无法下单";

        when(modelFallback.callWithFallback(contains("运维告警解析专家"), anyString()))
                .thenReturn(new ModelResult(p0Json, "deepseek-chat", false));
        when(modelFallback.callWithFallback(contains("资深运维工程师"), anyString()))
                .thenReturn(new ModelResult(p0Rca, "deepseek-chat", false));
        when(serviceStatusTool.queryStatus("order-service"))
                .thenReturn(ToolResult.success("ServiceStatusTool", "order-service", "down", 1));
        when(resourceMetricsTool.queryMetrics("order-service"))
                .thenReturn(ToolResult.success("ResourceMetricsTool", "order-service", "N/A", 1));
        when(errorLogTool.queryErrors("order-service", "综合告警"))
                .thenReturn(ToolResult.success("ErrorLogTool", "order-service", "critical", 1));
        when(deployRecordTool.queryDeploys("order-service"))
                .thenReturn(ToolResult.success("DeployRecordTool", "order-service", "v2.3.1", 1));
        when(dependencyTool.queryDependencies("order-service"))
                .thenReturn(ToolResult.success("DependencyTool", "order-service", "deps", 1));

        PipelineResult result = pipeline.analyze("order-service宕机，所有用户无法下单，全面崩溃");

        assertThat(result.getReport().getRiskLevel()).isEqualTo("P0");
        assertThat(result.getReport().isNeedsEscalation()).isTrue();
        assertThat(result.getReport().getSeverity()).isEqualTo("紧急");
    }
}
