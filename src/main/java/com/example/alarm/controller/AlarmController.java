package com.example.alarm.controller;

import com.example.alarm.metrics.AlarmMetrics;
import com.example.alarm.model.AlarmReport;
import com.example.alarm.model.ChatContext;
import com.example.alarm.model.ChatSession;
import com.example.alarm.model.ToolResult;
import com.example.alarm.pipeline.AlarmAnalysisPipeline;
import com.example.alarm.pipeline.AlarmAnalysisPipeline.PipelineResult;
import com.example.alarm.pipeline.MultiAgentPipeline;
import com.example.alarm.pipeline.StreamingPipelineAdapter;
import com.example.alarm.pipeline.StreamingPipelineAdapter.StreamingContext;
import com.example.alarm.service.ChatMemoryService;
import com.example.alarm.service.MarkdownReportService;
import com.example.alarm.service.ReportStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/alarm")
public class AlarmController {

    private static final Logger log = LoggerFactory.getLogger(AlarmController.class);
    private final AlarmAnalysisPipeline pipeline;
    private final MultiAgentPipeline multiAgentPipeline;
    private final StreamingPipelineAdapter streamingAdapter;
    private final ChatMemoryService chatMemory;
    private final MarkdownReportService markdownService;
    private final ReportStore reportStore;
    private final AlarmMetrics metrics;
    private final ObjectMapper objectMapper;

    public AlarmController(AlarmAnalysisPipeline pipeline, MultiAgentPipeline multiAgentPipeline,
                           StreamingPipelineAdapter streamingAdapter, ChatMemoryService chatMemory,
                           MarkdownReportService markdownService, ReportStore reportStore, AlarmMetrics metrics) {
        this.pipeline = pipeline;
        this.multiAgentPipeline = multiAgentPipeline;
        this.streamingAdapter = streamingAdapter;
        this.chatMemory = chatMemory;
        this.markdownService = markdownService;
        this.reportStore = reportStore;
        this.metrics = metrics;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody Map<String, String> request) {
        String alarmText = request.get("alarmText");
        if (alarmText == null || alarmText.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please input alarm text"));
        }
        String sessionId = request.get("sessionId");
        ChatContext ctx = null;
        if (sessionId != null && !sessionId.isBlank()) {
            ChatSession session = chatMemory.getSession(sessionId);
            if (session != null && !session.getHistory().isEmpty()) {
                ctx = new ChatContext();
                ctx.setSessionId(sessionId);
                ChatSession.Exchange last = session.getHistory().get(session.getHistory().size() - 1);
                ctx.setPreviousAnalysis(last.getReportJson());
                ctx.setQuestion(alarmText);
            }
        }
        log.info("[API] analyze text len={}, session={}", alarmText.length(), sessionId);
        PipelineResult result;
        if (multiAgentPipeline.isMultiAgentMode()) {
            result = multiAgentPipeline.analyze(alarmText, ctx);
        } else {
            result = pipeline.analyze(alarmText);
        }
        reportStore.store(result.getAnalysisId(), result.getReport());
        metrics.recordAnalysis("success", result.getModelUsed());
        if (sessionId != null && !sessionId.isBlank()) {
            ChatSession.Exchange ex = new ChatSession.Exchange();
            ex.setAlarmText(alarmText);
            try { ex.setReportJson(objectMapper.writeValueAsString(result.getReport())); } catch (Exception ignored) {}
            chatMemory.addExchange(sessionId, ex);
        }
        return ResponseEntity.ok(buildResponse(result, sessionId));
    }

    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeStream(@RequestBody Map<String, String> request) {
        String alarmText = request.get("alarmText");
        if (alarmText == null || alarmText.isBlank()) alarmText = "";
        final String finalAlarmText = alarmText;

        SseEmitter emitter = new SseEmitter(120000L);
        StreamingContext ctx = new StreamingContext();

        ctx.stepCallback = (step, status, data, latency) -> {
            try {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("step", step);
                event.put("status", status);
                if (data != null) event.putAll(data);
                event.put("latencyMs", latency);
                emitter.send(SseEmitter.event().name("step").data(objectMapper.writeValueAsString(event)));
            } catch (IOException e) { emitter.completeWithError(e); }
        };

        ctx.reportCallback = (reportData) -> {
            try {
                emitter.send(SseEmitter.event().name("report").data(objectMapper.writeValueAsString(reportData)));
            } catch (IOException e) { emitter.completeWithError(e); }
        };

        ctx.doneCallback = (analysisId, totalMs, model) -> {
            try {
                Map<String, Object> doneData = new LinkedHashMap<>();
                doneData.put("analysisId", analysisId);
                doneData.put("totalLatencyMs", totalMs);
                doneData.put("modelUsed", model);
                emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(doneData)));
                emitter.complete();
            } catch (IOException e) { emitter.completeWithError(e); }
        };

        new Thread(() -> {
            try { streamingAdapter.analyze(finalAlarmText, ctx); }
            catch (Exception e) { emitter.completeWithError(e); }
        }).start();

        emitter.onTimeout(emitter::complete);
        return emitter;
    }

    @PostMapping("/session/{sessionId}/followup")
    public ResponseEntity<Map<String, Object>> followup(@PathVariable String sessionId, @RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please input follow-up question"));
        }
        ChatSession session = chatMemory.getSession(sessionId);
        if (session == null || session.getHistory().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Session not found or expired"));
        }
        ChatSession.Exchange last = session.getHistory().get(session.getHistory().size() - 1);
        ChatContext ctx = new ChatContext();
        ctx.setSessionId(sessionId);
        ctx.setPreviousAnalysis(last.getReportJson());
        ctx.setPreviousServiceName(last.getAlarmText());
        ctx.setQuestion(question);

        PipelineResult result;
        if (multiAgentPipeline.isMultiAgentMode()) {
            result = multiAgentPipeline.analyze(question, ctx);
        } else {
            result = pipeline.analyze(question);
        }
        reportStore.store(result.getAnalysisId(), result.getReport());

        ChatSession.Exchange ex = new ChatSession.Exchange();
        ex.setFollowUpQuestion(question);
        try { ex.setFollowUpAnswer(objectMapper.writeValueAsString(result.getReport())); } catch (Exception ignored) {}
        chatMemory.addExchange(sessionId, ex);

        return ResponseEntity.ok(buildResponse(result, sessionId));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        ChatSession session = chatMemory.getSession(sessionId);
        if (session == null) return ResponseEntity.notFound().build();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("sessionId", session.getSessionId());
        resp.put("createdAt", session.getCreatedAt().toString());
        resp.put("lastActiveAt", session.getLastActiveAt().toString());
        resp.put("exchangeCount", session.getHistory().size());
        return ResponseEntity.ok(resp);
    }

    @GetMapping(value = "/report/{analysisId}/markdown", produces = "text/markdown;charset=UTF-8")
    public ResponseEntity<String> getMarkdownReport(@PathVariable String analysisId) {
        AlarmReport report = reportStore.get(analysisId);
        if (report == null) return ResponseEntity.notFound().build();
        String md = markdownService.generate(analysisId, report);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=alarm-report-" + analysisId + ".md")
                .body(md);
    }

    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> listServices() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("services", Arrays.asList("order-service", "payment-service", "inventory-service", "user-service"));
        response.put("examples", Arrays.asList(
                "order-service timeout rate 15%, P99 latency >5s, just released v2.3.1",
                "payment-service error rate 8%, DB connection pool exhausted",
                "inventory-service CPU 100%, memory 90%, query timeout",
                "user-service DB connection refused, auth timeout"
        ));
        response.put("pipelineMode", multiAgentPipeline.isMultiAgentMode() ? "multi-agent" : "single");
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> buildResponse(PipelineResult result, String sessionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("analysisId", result.getAnalysisId());
        response.put("report", buildReportMap(result.getReport()));
        response.put("modelUsed", result.getModelUsed());
        response.put("fallbackActivated", result.isFallbackActivated());
        response.put("pipelineLatency", result.getPipelineLatency());
        response.put("toolResults", buildToolResults(result.getToolResults()));
        if (sessionId != null) response.put("sessionId", sessionId);
        return response;
    }

    private Map<String, Object> buildReportMap(AlarmReport report) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("serviceName", report.getServiceName());
        map.put("alarmType", report.getAlarmType());
        map.put("riskLevel", report.getRiskLevel());
        map.put("severity", report.getSeverity());
        map.put("keyMetrics", report.getKeyMetrics());
        map.put("userImpact", report.isUserImpact());
        map.put("impactDescription", report.getImpactDescription());
        map.put("possibleRootCause", report.getPossibleRootCause());
        map.put("evidence", report.getEvidence());
        map.put("confidence", report.getConfidence());
        map.put("recommendedActions", report.getRecommendedActions());
        map.put("shouldRollback", report.isShouldRollback());
        map.put("needsEscalation", report.isNeedsEscalation());
        map.put("followUpMetrics", report.getFollowUpMetrics());
        map.put("nextCheckTime", report.getNextCheckTime());
        return map;
    }

    private List<Map<String, Object>> buildToolResults(List<ToolResult> results) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ToolResult r : results) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("toolName", r.getToolName());
            m.put("serviceName", r.getServiceName());
            m.put("success", r.isSuccess());
            m.put("data", r.getData());
            m.put("latencyMs", r.getLatencyMs());
            m.put("errorMessage", r.getErrorMessage());
            list.add(m);
        }
        return list;
    }
}
