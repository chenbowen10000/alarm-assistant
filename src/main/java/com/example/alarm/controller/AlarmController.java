package com.example.alarm.controller;

import com.example.alarm.model.AlarmReport;
import com.example.alarm.model.ToolResult;
import com.example.alarm.pipeline.AlarmAnalysisPipeline;
import com.example.alarm.pipeline.AlarmAnalysisPipeline.PipelineResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/alarm")
public class AlarmController {

    private static final Logger log = LoggerFactory.getLogger(AlarmController.class);
    private final AlarmAnalysisPipeline pipeline;

    public AlarmController(AlarmAnalysisPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody Map<String, String> request) {
        String alarmText = request.get("alarmText");
        if (alarmText == null || alarmText.isBlank()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("message", "请输入告警文本");
            return ResponseEntity.badRequest().body(error);
        }

        log.info("[API] Received alarm analysis request, text length={}", alarmText.length());
        PipelineResult result = pipeline.analyze(alarmText);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("analysisId", result.getAnalysisId());
        response.put("report", buildReportMap(result.getReport()));
        response.put("modelUsed", result.getModelUsed());
        response.put("fallbackActivated", result.isFallbackActivated());
        response.put("pipelineLatency", result.getPipelineLatency());
        response.put("toolResults", buildToolResultList(result.getToolResults()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> listServices() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("services", Arrays.asList("order-service", "payment-service", "inventory-service", "user-service"));
        response.put("examples", Arrays.asList(
                "订单服务接口超时率达到15%，P99延迟超过5秒，影响用户下单，刚在10分钟前发布了v2.3.1版本",
                "支付服务错误率突然上升到8%，数据库连接池耗尽，大量支付失败",
                "库存服务CPU使用率持续100%，内存使用率90%，接口响应超时",
                "用户服务数据库连接异常，出现大量Connection refused，认证服务超时",
                "order-service数据库连接池耗尽，active=50已达上限，12个请求等待中，下游payment-service响应变慢",
                "payment-service调用inventory-service扣减库存超时8300ms，重试队列积压128条",
                "inventory-service内存使用率90%触发频繁GC，出现OutOfMemoryError: GC overhead limit exceeded",
                "user-service上游order-service请求量突增300%，疑似重试风暴引发连锁雪崩",
                "支付服务数据库连接池配置从50降到40，更新后大量支付请求排队超时",
                "这只是随便写写的异常"
        ));
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> buildReportMap(AlarmReport report) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("analysisStatus", report.getAnalysisStatus());
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

    private List<Map<String, Object>> buildToolResultList(List<ToolResult> toolResults) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ToolResult r : toolResults) {
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