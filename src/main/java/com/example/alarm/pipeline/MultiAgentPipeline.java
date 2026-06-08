package com.example.alarm.pipeline;

import com.example.alarm.agent.DiagnosisAgent;
import com.example.alarm.agent.DiagnosisResult;
import com.example.alarm.agent.ParserAgent;
import com.example.alarm.agent.ReportAgent;
import com.example.alarm.metrics.AlarmMetrics;
import com.example.alarm.model.*;
import com.example.alarm.service.ReportStore;
import com.example.alarm.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;

@Component
public class MultiAgentPipeline {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentPipeline.class);
    private final ParserAgent parserAgent;
    private final DiagnosisAgent diagnosisAgent;
    private final ReportAgent reportAgent;
    private final ServiceStatusTool serviceStatusTool;
    private final ErrorLogTool errorLogTool;
    private final DeployRecordTool deployRecordTool;
    private final DependencyTool dependencyTool;
    private final ResourceMetricsTool resourceMetricsTool;
    private final AlarmMetrics metrics;
    private final ReportStore reportStore;

    @Value("${alarm.pipeline.mode:single}")
    private String pipelineMode;

    public MultiAgentPipeline(ParserAgent parserAgent, DiagnosisAgent diagnosisAgent, ReportAgent reportAgent,
                               ServiceStatusTool sst, ErrorLogTool elt, DeployRecordTool drt,
                               DependencyTool dt, ResourceMetricsTool rmt, AlarmMetrics metrics,
                               ReportStore reportStore) {
        this.parserAgent = parserAgent;
        this.diagnosisAgent = diagnosisAgent;
        this.reportAgent = reportAgent;
        this.serviceStatusTool = sst;
        this.errorLogTool = elt;
        this.deployRecordTool = drt;
        this.dependencyTool = dt;
        this.resourceMetricsTool = rmt;
        this.metrics = metrics;
        this.reportStore = reportStore;
    }

    public boolean isMultiAgentMode() {
        return "multi-agent".equalsIgnoreCase(pipelineMode);
    }

    public AlarmAnalysisPipeline.PipelineResult analyze(String alarmText, ChatContext ctx) {
        String analysisId = UUID.randomUUID().toString();
        long totalStart = System.currentTimeMillis();
        Map<String, Long> latencies = new LinkedHashMap<>();

        String contextPrompt = ctx != null ? ctx.buildContextPrompt() : "";

        long stepStart = System.currentTimeMillis();
        ParsedAlarm parsed = parserAgent.parse(alarmText, contextPrompt);
        latencies.put("parse", System.currentTimeMillis() - stepStart);

        stepStart = System.currentTimeMillis();
        List<ToolResult> tools = invokeTools(parsed);
        latencies.put("toolInvocation", System.currentTimeMillis() - stepStart);

        stepStart = System.currentTimeMillis();
        DiagnosisResult diagnosis = diagnosisAgent.diagnose(parsed, tools);
        latencies.put("rootCauseAnalysis", System.currentTimeMillis() - stepStart);

        stepStart = System.currentTimeMillis();
        AlarmReport report = buildBaseReport(parsed, diagnosis, tools);
        reportAgent.generate(report, parsed, diagnosis, tools);
        latencies.put("reportGeneration", System.currentTimeMillis() - stepStart);

        long totalLatency = System.currentTimeMillis() - totalStart;
        latencies.put("total", totalLatency);

        reportStore.store(analysisId, report);
        metrics.recordAnalysis("success", "multi-agent");
        log.info("[MULTI-AGENT] id={}, total={}ms, risk={}", analysisId, totalLatency, report.getRiskLevel());

        return new AlarmAnalysisPipeline.PipelineResult(analysisId, report, "multi-agent", false, latencies, tools);
    }

    private List<ToolResult> invokeTools(ParsedAlarm alarm) {
        List<ToolResult> results = new ArrayList<>();
        String svc = alarm.getServiceName();
        String type = alarm.getAlarmType();
        results.add(safeInvoke(() -> serviceStatusTool.queryStatus(svc)));
        results.add(safeInvoke(() -> resourceMetricsTool.queryMetrics(svc)));
        results.add(safeInvoke(() -> errorLogTool.queryErrors(svc, type)));
        results.add(safeInvoke(() -> deployRecordTool.queryDeploys(svc)));
        results.add(safeInvoke(() -> dependencyTool.queryDependencies(svc)));
        return results;
    }

    private ToolResult safeInvoke(Supplier<ToolResult> fn) {
        try { return fn.get(); } catch (Exception e) { return ToolResult.error("Unknown", "unknown", e.getMessage()); }
    }

    private AlarmReport buildBaseReport(ParsedAlarm parsed, DiagnosisResult diagnosis, List<ToolResult> tools) {
        AlarmReport r = new AlarmReport();
        r.setServiceName(parsed.getServiceName());
        r.setAlarmType(parsed.getAlarmType());
        r.setRiskLevel(diagnosis.getRiskLevel());
        r.setSeverity(mapSeverity(diagnosis.getRiskLevel()));
        r.setKeyMetrics(parsed.getKeyMetrics());
        r.setUserImpact(parsed.isUserImpact());
        r.setImpactDescription(diagnosis.getImpactDescription());
        r.setPossibleRootCause(diagnosis.getRootCause());
        r.setConfidence(diagnosis.getConfidence());
        r.setNeedsEscalation(diagnosis.isNeedsEscalation());
        List<String> evidence = new ArrayList<>();
        for (ToolResult tr : tools) {
            evidence.add(tr.isSuccess()
                    ? "[" + tr.getToolName() + "] OK " + tr.getData() + " (" + tr.getLatencyMs() + "ms)"
                    : "[" + tr.getToolName() + "] FAIL " + tr.getErrorMessage());
        }
        r.setEvidence(evidence);
        r.setFollowUpMetrics(List.of("接口P99延迟", "错误率趋势", "CPU/内存使用率"));
        r.setNextCheckTime("持续监控，建议每5分钟检查一次");
        return r;
    }

    private String mapSeverity(String level) {
        switch (level) {
            case "P0": return "紧急";
            case "P1": return "严重";
            case "P2": return "一般";
            case "P3": return "轻微";
            default: return "未知";
        }
    }
}
