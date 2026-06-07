package com.example.alarm.pipeline;

import com.example.alarm.mock.OpsMockDataStore;
import com.example.alarm.model.AlarmReport;
import com.example.alarm.model.ParsedAlarm;
import com.example.alarm.model.ToolResult;
import com.example.alarm.pipeline.ModelFallbackHandler.ModelResult;
import com.example.alarm.tools.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AlarmAnalysisPipeline {

    private static final Logger log = LoggerFactory.getLogger(AlarmAnalysisPipeline.class);

    private final ModelFallbackHandler modelFallback;
    private final ServiceStatusTool serviceStatusTool;
    private final ErrorLogTool errorLogTool;
    private final DeployRecordTool deployRecordTool;
    private final DependencyTool dependencyTool;
    private final ResourceMetricsTool resourceMetricsTool;
    private final OpsMockDataStore dataStore;
    private final ObjectMapper objectMapper;

    public AlarmAnalysisPipeline(ModelFallbackHandler modelFallback,
                                  ServiceStatusTool serviceStatusTool,
                                  ErrorLogTool errorLogTool,
                                  DeployRecordTool deployRecordTool,
                                  DependencyTool dependencyTool,
                                  ResourceMetricsTool resourceMetricsTool,
                                  OpsMockDataStore dataStore) {
        this.modelFallback = modelFallback;
        this.serviceStatusTool = serviceStatusTool;
        this.errorLogTool = errorLogTool;
        this.deployRecordTool = deployRecordTool;
        this.dependencyTool = dependencyTool;
        this.resourceMetricsTool = resourceMetricsTool;
        this.dataStore = dataStore;
        this.objectMapper = new ObjectMapper();
    }

    public static class PipelineResult {
        private final String analysisId;
        private final AlarmReport report;
        private final String modelUsed;
        private final boolean fallbackActivated;
        private final Map<String, Long> pipelineLatency;
        private final List<ToolResult> toolResults;

        public PipelineResult(String analysisId, AlarmReport report, String modelUsed,
                              boolean fallbackActivated, Map<String, Long> pipelineLatency,
                              List<ToolResult> toolResults) {
            this.analysisId = analysisId;
            this.report = report;
            this.modelUsed = modelUsed;
            this.fallbackActivated = fallbackActivated;
            this.pipelineLatency = pipelineLatency;
            this.toolResults = toolResults;
        }

        public String getAnalysisId() { return analysisId; }
        public AlarmReport getReport() { return report; }
        public String getModelUsed() { return modelUsed; }
        public boolean isFallbackActivated() { return fallbackActivated; }
        public Map<String, Long> getPipelineLatency() { return pipelineLatency; }
        public List<ToolResult> getToolResults() { return toolResults; }
    }

    public PipelineResult analyze(String alarmText) {
        String analysisId = UUID.randomUUID().toString();
        long totalStart = System.currentTimeMillis();
        log.info("[ANALYSIS-START] id={}, input=\"{}\"", analysisId, truncate(alarmText, 100));

        Map<String, Long> latencies = new LinkedHashMap<>();
        boolean fallbackActivated = false;
        String finalModel = "rule-engine";

        // ======== Step 1: Parse Alarm ========
        long stepStart = System.currentTimeMillis();
        ParsedAlarm parsedAlarm = parseAlarm(alarmText, analysisId);
        latencies.put("parse", System.currentTimeMillis() - stepStart);
        log.info("[STEP-1-PARSE] id={}, service={}, type={}, latency={}ms",
                analysisId, parsedAlarm.getServiceName(), parsedAlarm.getAlarmType(), latencies.get("parse"));

        if (!parsedAlarm.isParsed()) {
            return buildParseFailedResult(analysisId, alarmText, parsedAlarm, latencies, totalStart);
        }

        // ======== Step 2: Invoke Tools ========
        stepStart = System.currentTimeMillis();
        List<ToolResult> toolResults = invokeTools(parsedAlarm, analysisId);
        latencies.put("toolInvocation", System.currentTimeMillis() - stepStart);

        // ======== Step 3: Aggregate Evidence ========
        stepStart = System.currentTimeMillis();
        String evidenceContext = buildEvidenceContext(parsedAlarm, toolResults);
        latencies.put("evidenceAggregate", System.currentTimeMillis() - stepStart);
        log.info("[STEP-3-EVIDENCE] id={}, evidenceCount={}", analysisId, toolResults.size());

        // ======== Step 4: Root Cause Analysis ========
        stepStart = System.currentTimeMillis();
        ModelResult rcaResult = performRootCauseAnalysis(parsedAlarm, evidenceContext);
        latencies.put("rootCauseAnalysis", System.currentTimeMillis() - stepStart);
        if (rcaResult.isFallbackActivated()) fallbackActivated = true;
        finalModel = rcaResult.getModelUsed();
        log.info("[STEP-4-RCA] id={}, model={}, latency={}ms",
                analysisId, finalModel, latencies.get("rootCauseAnalysis"));

        // ======== Step 5: Generate Report ========
        stepStart = System.currentTimeMillis();
        AlarmReport report = generateReport(parsedAlarm, evidenceContext, rcaResult, toolResults);
        latencies.put("reportGeneration", System.currentTimeMillis() - stepStart);
        log.info("[STEP-5-REPORT] id={}, model={}, latency={}ms",
                analysisId, finalModel, latencies.get("reportGeneration"));

        long totalLatency = System.currentTimeMillis() - totalStart;
        latencies.put("total", totalLatency);
        log.info("[ANALYSIS-COMPLETE] id={}, totalLatency={}ms, riskLevel={}, modelUsed={}",
                analysisId, totalLatency, report.getRiskLevel(), finalModel);

        return new PipelineResult(analysisId, report, finalModel, fallbackActivated, latencies, toolResults);
    }

    // ======== Step 1: Parse Alarm ========
    private ParsedAlarm parseAlarm(String alarmText, String analysisId) {
        String systemPrompt = "你是一个运维告警解析专家。请从自然语言告警文本中提取结构化信息。" +
                "只返回JSON，不要任何其他文字。格式如下：\n" +
                "{\"serviceName\":\"服务名\",\"alarmType\":\"告警类型\",\"keyMetrics\":{\"指标名\":\"值\"}," +
                "\"riskLevel\":\"P0/P1/P2/P3\",\"userImpact\":true/false,\"needsEscalation\":true/false,\"confidence\":0.0-1.0}\n\n" +
                "已知服务: order-service(订单服务), payment-service(支付服务), inventory-service(库存服务), user-service(用户服务)\n" +
                "告警类型: 接口超时, 错误率上升, CPU异常, 内存异常, 数据库异常, 发布后异常, 下游服务异常, 综合告警\n" +
                "riskLevel: P0(核心功能不可用), P1(大面积异常), P2(部分异常), P3(轻微异常)";

        if (!hasAlarmSignal(alarmText)) {
            log.warn("[STEP-1-PARSE] id={}, no alarm signal detected in text", analysisId);
            return ParsedAlarm.failed();
        }
        ModelResult result = modelFallback.callWithFallback(systemPrompt, alarmText);
        log.info("[STEP-1-PARSE] id={}, model={}", analysisId, result.getModelUsed());

        try {
            return parseJsonResult(result.getContent());
        } catch (Exception e) {
            log.warn("[STEP-1-PARSE] id={}, parse failed, trying heuristic", analysisId);
            return heuristicParse(alarmText);
        }
    }

    private ParsedAlarm parseJsonResult(String content) throws JsonProcessingException {
        String json = extractJson(content);
        Map<String, Object> map = objectMapper.readValue(json, Map.class);

        ParsedAlarm alarm = new ParsedAlarm();
        alarm.setServiceName(getString(map, "serviceName"));
        alarm.setAlarmType(getString(map, "alarmType"));
        String rl = getString(map, "riskLevel");
        alarm.setRiskLevel(isValidRiskLevel(rl) ? rl : "P2");
        alarm.setUserImpact(getBoolean(map, "userImpact"));
        alarm.setNeedsEscalation(getBoolean(map, "needsEscalation"));
        alarm.setConfidence(getDouble(map, "confidence"));

        Object metricsObj = map.get("keyMetrics");
        if (metricsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> metrics = (Map<String, String>) metricsObj;
            alarm.setKeyMetrics(metrics);
        }

        return alarm;
    }

    private ParsedAlarm heuristicParse(String alarmText) {
        ParsedAlarm alarm = new ParsedAlarm();
        alarm.setConfidence(0.3);
        String lower = alarmText.toLowerCase();

        if (lower.contains("order") || lower.contains("订单")) alarm.setServiceName("order-service");
        else if (lower.contains("pay") || lower.contains("支付")) alarm.setServiceName("payment-service");
        else if (lower.contains("inventory") || lower.contains("库存")) alarm.setServiceName("inventory-service");
        else if (lower.contains("user") || lower.contains("用户")) alarm.setServiceName("user-service");
        else { alarm.setServiceName("unknown"); alarm.setParsed(false); }

        if (lower.contains("超时") || lower.contains("timeout")) alarm.setAlarmType("接口超时");
        else if (lower.contains("错误率") || lower.contains("error rate")) alarm.setAlarmType("错误率上升");
        else if (lower.contains("cpu")) alarm.setAlarmType("CPU异常");
        else if (lower.contains("内存") || lower.contains("memory")) alarm.setAlarmType("内存异常");
        else if (lower.contains("数据库") || lower.contains("database") || lower.contains("db")) alarm.setAlarmType("数据库异常");
        else if (lower.contains("发布") || lower.contains("deploy") || lower.contains("发版")) alarm.setAlarmType("发布后异常");
        else alarm.setAlarmType("综合告警");

        if (lower.contains("宕机") || lower.contains("不可用") || lower.contains("crash")) alarm.setRiskLevel("P0");
        else if (lower.contains("大量") || lower.contains("严重") || lower.contains("大面积")) alarm.setRiskLevel("P1");
        else if (lower.contains("部分") || lower.contains("偶尔")) alarm.setRiskLevel("P3");
        else alarm.setRiskLevel("P2");

        alarm.setUserImpact(lower.contains("用户") || lower.contains("下单") || lower.contains("影响"));
        alarm.setNeedsEscalation(alarm.getRiskLevel().equals("P0") || alarm.getRiskLevel().equals("P1"));

        return alarm;
    }

    // ======== Step 2: Invoke Tools ========
    private List<ToolResult> invokeTools(ParsedAlarm alarm, String analysisId) {
        List<ToolResult> results = new ArrayList<>();
        String serviceName = alarm.getServiceName();
        String alarmType = alarm.getAlarmType();

        // Always query service status
        results.add(safeInvoke(() -> serviceStatusTool.queryStatus(serviceName), analysisId));

        // Always query resource metrics
        results.add(safeInvoke(() -> resourceMetricsTool.queryMetrics(serviceName), analysisId));

        // Error logs
        results.add(safeInvoke(() -> errorLogTool.queryErrors(serviceName, alarmType), analysisId));

        // Deploy records
        results.add(safeInvoke(() -> deployRecordTool.queryDeploys(serviceName), analysisId));

        // Dependencies (for cascade analysis)
        results.add(safeInvoke(() -> dependencyTool.queryDependencies(serviceName), analysisId));

        return results;
    }

    @FunctionalInterface
    interface ToolInvoker {
        ToolResult invoke();
    }

    private ToolResult safeInvoke(ToolInvoker invoker, String analysisId) {
        try {
            return invoker.invoke();
        } catch (Exception e) {
            log.error("[STEP-2-TOOLS] id={}, tool invocation failed: {}", analysisId, e.getMessage());
            return ToolResult.error("Unknown", "unknown", e.getMessage());
        }
    }

    // ======== Step 3: Aggregate Evidence ========
    private String buildEvidenceContext(ParsedAlarm alarm, List<ToolResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 告警解析 ===\n");
        sb.append(String.format("服务名称: %s\n", alarm.getServiceName()));
        sb.append(String.format("告警类型: %s\n", alarm.getAlarmType()));
        sb.append(String.format("异常指标: %s\n", alarm.getKeyMetrics()));
        sb.append(String.format("预估风险等级: %s\n", alarm.getRiskLevel()));
        sb.append(String.format("影响用户: %s\n", alarm.isUserImpact()));
        sb.append(String.format("置信度: %.2f\n", alarm.getConfidence()));
        sb.append("\n=== 工具调用证据 ===\n");
        for (ToolResult r : results) {
            sb.append(String.format("- %s [%s]: %s\n",
                    r.getToolName(), r.isSuccess() ? "OK" : "FAIL", r.toString()));
        }
        return sb.toString();
    }

    // ======== Step 4: Root Cause Analysis ========
    private ModelResult performRootCauseAnalysis(ParsedAlarm alarm, String evidenceContext) {
        String systemPrompt = "你是一个资深运维工程师。请根据告警信息和工具调用证据，分析根因。" +
                "请用中文返回结构化分析，重点关注：\n" +
                "1. 最可能的根因是什么？（结合证据具体说明）\n" +
                "2. 风险等级应该是什么？（P0/P1/P2/P3）\n" +
                "3. 是否需要人工升级处理？\n" +
                "4. 用户是否受到影响及影响程度\n" +
                "5. 给出置信度(百分比)";

        return modelFallback.callWithFallback(systemPrompt, evidenceContext);
    }

    // ======== Step 5: Generate Report ========
    private AlarmReport generateReport(ParsedAlarm parsedAlarm, String evidenceContext,
                                        ModelResult rcaResult, List<ToolResult> toolResults) {
        // Try to extract structured info from RCA
        String rcaText = rcaResult.getContent();

        AlarmReport report = new AlarmReport();
        report.setAnalysisStatus("SUCCESS");
        report.setServiceName(parsedAlarm.getServiceName());
        report.setAlarmType(parsedAlarm.getAlarmType());

        // Extract risk level from RCA result
        String riskLevel = parseRiskLevel(rcaText, parsedAlarm.getRiskLevel());
        report.setRiskLevel(riskLevel);
        report.setSeverity(mapSeverity(riskLevel));
        report.setKeyMetrics(parsedAlarm.getKeyMetrics());
        report.setUserImpact(parsedAlarm.isUserImpact());
        String rawImpact = extractFromRca(rcaText, "用户影响", null);
        report.setImpactDescription(rawImpact != null && rawImpact.length() < 200 ? rawImpact : "待确认");
        report.setPossibleRootCause(parseRootCause(rcaText, parsedAlarm));
        report.setConfidence(parsedAlarm.getConfidence() > 0 ? parsedAlarm.getConfidence() : 0.7);

        // Evidence from tools
        List<String> evidence = new ArrayList<>();
        for (ToolResult r : toolResults) {
            if (r.isSuccess()) {
                evidence.add(String.format("[%s] ✓ %s (%dms)", r.getToolName(), r.getData(), r.getLatencyMs()));
            } else {
                evidence.add(String.format("[%s] ✗ %s", r.getToolName(), r.getErrorMessage()));
            }
        }
        report.setEvidence(evidence);

        // Recommendations
        List<String> actions = extractRecommendations(rcaText, parsedAlarm);
        report.setRecommendedActions(actions);
        report.setShouldRollback(rcaText.contains("回滚") || rcaText.contains("rollback") ||
                evidenceContext.contains("v2.3.1") || parsedAlarm.getAlarmType().contains("发布"));
        report.setNeedsEscalation(riskLevel.equals("P0") || riskLevel.equals("P1") || parsedAlarm.isNeedsEscalation());

        // Follow-up
        List<String> followUp = new ArrayList<>();
        followUp.add("接口P99延迟");
        followUp.add("错误率趋势");
        followUp.add("CPU/内存使用率");
        if (report.isShouldRollback()) followUp.add("回滚后服务稳定性");
        followUp.add(parsedAlarm.getServiceName() + "服务健康状态");
        report.setFollowUpMetrics(followUp);
        report.setNextCheckTime("持续监控，建议每5分钟检查一次");

        return report;
    }

    // ======== Parse-failed shortcut ========
    private PipelineResult buildParseFailedResult(String analysisId, String alarmText,
                                                     ParsedAlarm parsedAlarm,
                                                     Map<String, Long> latencies, long totalStart) {
        long totalLatency = System.currentTimeMillis() - totalStart;
        latencies.put("total", totalLatency);

        AlarmReport report = new AlarmReport();
        report.setAnalysisStatus("PARSE_FAILED");
        report.setServiceName("unknown");
        report.setAlarmType("未识别");
        report.setRiskLevel("N/A");
        report.setSeverity("N/A");
        report.setConfidence(0.0);
        report.setUserImpact(false);
        report.setImpactDescription("告警文本中未检测到可识别的服务名称或异常指标，无法进行自动化分析。请确认告警内容是否包含有效的服务名称（如 order-service、payment-service）和异常描述。");
        report.setPossibleRootCause("无法确定根因 —— 告警文本无法解析为有效的结构化信息");
        List<String> evidence = new ArrayList<>();
        evidence.add("告警解析失败：未识别到有效服务名或异常指标");
        report.setEvidence(evidence);
        report.setRecommendedActions(Arrays.asList(
                "请检查告警文本是否包含有效的服务名称",
                "请确认告警文本是否包含可量化的异常指标",
                "建议携带具体指标（如超时率、错误率、CPU使用率等）后重试"
        ));
        report.setShouldRollback(false);
        report.setNeedsEscalation(false);
        report.setFollowUpMetrics(Collections.emptyList());
        report.setNextCheckTime("N/A");

        log.info("[ANALYSIS-COMPLETE] id={}, status=PARSE_FAILED, totalLatency={}ms",
                analysisId, totalLatency);

        return new PipelineResult(analysisId, report, "rule-engine", true, latencies,
                Collections.emptyList());
    }

    private boolean isValidService(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) return false;
        return dataStore.serviceExists(serviceName);
    }

    private boolean hasAlarmSignal(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase();
        String[] services = {"order", "payment", "inventory", "user"};
        String[] cnServices = {"订单", "支付", "库存", "用户"};
        for (String s : services) if (lower.contains(s)) return true;
        for (String s : cnServices) if (text.contains(s)) return true;
        String[] indicators = {"timeout", "超时", "error", "错误", "cpu", "内存", "memory", "database", "数据库", "连接", "connection", "拒绝", "refused", "时间", "延迟", "发布", "deploy", "版本", "v2.", "v3.", "v1.", "down", "挂了", "宕机", "不可用"};
        for (String s : indicators) if (lower.contains(s) || text.contains(s)) return true;
        return text.length() >= 60;
    }

    private boolean isValidRiskLevel(String level) {
        if (level == null) return false;
        return level.equals("P0") || level.equals("P1") || level.equals("P2") || level.equals("P3");
    }

    private String parseRiskLevel(String rcaText, String fallback) {
        if (rcaText == null) return fallback;
        for (String line : rcaText.split("\n")) {
            if (line.contains("风险等级")) {
                if (line.contains("P0")) return "P0";
                if (line.contains("P1")) return "P1";
                if (line.contains("P2")) return "P2";
                if (line.contains("P3")) return "P3";
            }
        }
        return fallback;
    }

    private String parseRootCause(String rcaText, ParsedAlarm alarm) {
        if (rcaText == null) return buildDefaultRca(alarm);
        String[] lines = rcaText.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("根因") && (line.contains("：") || line.contains(":"))) {
                String after = line.split("根因")[1].replaceFirst("^[：:*\\s]+", "").trim();
                if (after.length() >= 10 && after.length() < 300) return after;
                if (i + 1 < lines.length && lines[i+1].trim().length() > 10) return lines[i+1].trim();
            }
        }
        return buildDefaultRca(alarm);
    }

    private String buildDefaultRca(ParsedAlarm alarm) {
        return String.format("服务: %s, 类型: %s, 置信度: %.0f%%",
                alarm.getServiceName(), alarm.getAlarmType(), alarm.getConfidence() * 100);
    }

    private String parseImpactDesc(String rcaText, ParsedAlarm alarm) {
        if (rcaText == null) return alarm.isUserImpact() ? "影响用户" : "未影响用户";
        for (String line : rcaText.split("\n")) {
            String lower = line.toLowerCase();
            if ((lower.contains("用户") || lower.contains("影响")) && (lower.contains("：") || lower.contains(":"))) {
                String after = line.split("[：:]", 2)[1].trim();
                if (after.length() >= 4 && after.length() < 200) return after;
            }
        }
        return alarm.isUserImpact() ? "影响用户" : "未影响用户";
    }

    // ======== Helper methods ========

    private String extractJson(String content) {
        content = content.trim();
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return "{}";
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private boolean getBoolean(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return val != null && val.toString().equalsIgnoreCase("true");
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0.0; }
    }

    private String extractFromRca(String rcaText, String label, String defaultVal) {
        if (rcaText == null) return defaultVal;
        for (String line : rcaText.split("\n")) {
            if (line.contains(label)) {
                String val = line.replaceAll(".*" + label + "[：:]*\\s*", "").trim();
                if (!val.isBlank()) return val;
            }
        }
        return defaultVal;
    }

    private String mapSeverity(String riskLevel) {
        switch (riskLevel) {
            case "P0": return "紧急";
            case "P1": return "严重";
            case "P2": return "一般";
            case "P3": return "轻微";
            default: return "未知";
        }
    }

    private List<String> extractRecommendations(String rcaText, ParsedAlarm alarm) {
        List<String> actions = new ArrayList<>();
        String lowerRca = rcaText.toLowerCase();

        if (lowerRca.contains("回滚") || lowerRca.contains("rollback") || alarm.getAlarmType().contains("发布")) {
            actions.add("建议立即回滚至上一个稳定版本");
        }
        if (lowerRca.contains("扩容") || lowerRca.contains("scale") || alarm.getAlarmType().contains("CPU") || alarm.getAlarmType().contains("内存")) {
            actions.add("建议扩容服务实例或增加资源配额");
        }
        if (lowerRca.contains("连接池") || lowerRca.contains("数据库") || lowerRca.contains("database")) {
            actions.add("检查并调大数据库连接池配置");
        }
        if (lowerRca.contains("超时") || lowerRca.contains("timeout")) {
            actions.add("调大接口超时阈值并检查下游服务响应时间");
        }
        if (lowerRca.contains("限流") || lowerRca.contains("rate limit") || lowerRca.contains("重试")) {
            actions.add("启用限流机制，避免重试风暴");
        }

        // Default actions
        actions.add("查看详细错误日志定位具体异常堆栈");
        actions.add("通知相关开发及运维人员关注");
        if (alarm.isUserImpact()) {
            actions.add("发布用户通知，告知服务异常");
        }

        return actions;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
