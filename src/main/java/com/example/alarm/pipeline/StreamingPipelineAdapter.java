package com.example.alarm.pipeline;

import com.example.alarm.model.ToolResult;
import com.example.alarm.pipeline.AlarmAnalysisPipeline.PipelineResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class StreamingPipelineAdapter {

    private static final Logger log = LoggerFactory.getLogger(StreamingPipelineAdapter.class);
    private final AlarmAnalysisPipeline pipeline;

    public StreamingPipelineAdapter(AlarmAnalysisPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @FunctionalInterface
    public interface StepCallback {
        void onStep(String step, String status, Map<String, Object> data, long latencyMs) throws IOException;
    }

    @FunctionalInterface
    public interface ReportCallback {
        void onReport(Map<String, Object> reportData) throws IOException;
    }

    @FunctionalInterface
    public interface DoneCallback {
        void onDone(String analysisId, long totalLatencyMs, String modelUsed) throws IOException;
    }

    public static class StreamingContext {
        public StepCallback stepCallback;
        public ReportCallback reportCallback;
        public DoneCallback doneCallback;
    }

    public void analyze(String alarmText, StreamingContext ctx) throws IOException {
        String analysisId = UUID.randomUUID().toString();
        long totalStart = System.currentTimeMillis();

        ctx.stepCallback.onStep("parse", "running",
                Map.of("message", "正在解析告警文本..."), 0);

        PipelineResult result = pipeline.analyze(alarmText);

        ctx.stepCallback.onStep("parse", "done", new LinkedHashMap<>() {{
            put("serviceName", result.getReport().getServiceName());
            put("alarmType", result.getReport().getAlarmType());
            put("confidence", result.getReport().getConfidence());
        }}, result.getPipelineLatency().getOrDefault("parse", 0L));

        List<Map<String, Object>> toolList = new ArrayList<>();
        for (ToolResult tr : result.getToolResults()) {
            toolList.add(Map.of("name", tr.getToolName(),
                    "ok", tr.isSuccess(), "ms", tr.getLatencyMs()));
        }
        ctx.stepCallback.onStep("tools", "done",
                Map.of("tools", toolList),
                result.getPipelineLatency().getOrDefault("toolInvocation", 0L));

        ctx.stepCallback.onStep("evidence", "done",
                Map.of("count", result.getToolResults().size()),
                result.getPipelineLatency().getOrDefault("evidenceAggregate", 0L));

        ctx.stepCallback.onStep("rca", "done", new LinkedHashMap<>() {{
            put("riskLevel", result.getReport().getRiskLevel());
            put("rootCause", truncate(result.getReport().getPossibleRootCause(), 200));
        }}, result.getPipelineLatency().getOrDefault("rootCauseAnalysis", 0L));

        Map<String, Object> reportMap = new LinkedHashMap<>();
        reportMap.put("analysisId", result.getAnalysisId());
        reportMap.put("report", buildReportData(result));
        reportMap.put("modelUsed", result.getModelUsed());
        reportMap.put("fallbackActivated", result.isFallbackActivated());
        reportMap.put("pipelineLatency", result.getPipelineLatency());
        ctx.reportCallback.onReport(reportMap);

        long totalLatency = System.currentTimeMillis() - totalStart;
        ctx.doneCallback.onDone(analysisId, totalLatency, result.getModelUsed());
    }

    private Map<String, Object> buildReportData(PipelineResult result) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("serviceName", result.getReport().getServiceName());
        m.put("alarmType", result.getReport().getAlarmType());
        m.put("riskLevel", result.getReport().getRiskLevel());
        m.put("severity", result.getReport().getSeverity());
        m.put("keyMetrics", result.getReport().getKeyMetrics());
        m.put("userImpact", result.getReport().isUserImpact());
        m.put("possibleRootCause", result.getReport().getPossibleRootCause());
        m.put("confidence", result.getReport().getConfidence());
        m.put("recommendedActions", result.getReport().getRecommendedActions());
        m.put("shouldRollback", result.getReport().isShouldRollback());
        m.put("needsEscalation", result.getReport().isNeedsEscalation());
        m.put("evidence", result.getReport().getEvidence());
        m.put("followUpMetrics", result.getReport().getFollowUpMetrics());
        return m;
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
