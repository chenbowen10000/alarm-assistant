package com.example.alarm.agent;

import com.example.alarm.model.ParsedAlarm;
import com.example.alarm.model.ToolResult;
import com.example.alarm.pipeline.ModelFallbackHandler;
import com.example.alarm.pipeline.ModelFallbackHandler.ModelResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiagnosisAgent {

    private static final Logger log = LoggerFactory.getLogger(DiagnosisAgent.class);
    private final ModelFallbackHandler modelFallback;

    private static final String SYSTEM_PROMPT =
            "你是一个资深运维工程师。请根据告警信息和工具调用证据，分析根因。" +
            "请用中文返回结构化分析，格式：\n" +
            "根因: <具体根因>\n风险等级: P0/P1/P2/P3\n置信度: 0.0-1.0\n用户影响: <影响描述>\n是否需要升级: true/false\n证据摘要: <关键证据>";

    public DiagnosisAgent(ModelFallbackHandler modelFallback) {
        this.modelFallback = modelFallback;
    }

    public DiagnosisResult diagnose(ParsedAlarm parsedAlarm, List<ToolResult> toolResults) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("=== 告警信息 ===\n");
        userPrompt.append("服务: ").append(parsedAlarm.getServiceName()).append("\n");
        userPrompt.append("类型: ").append(parsedAlarm.getAlarmType()).append("\n");
        userPrompt.append("指标: ").append(parsedAlarm.getKeyMetrics()).append("\n\n");
        userPrompt.append("=== 工具证据 ===\n");
        for (ToolResult r : toolResults) {
            userPrompt.append("- ").append(r.getToolName()).append(" [").append(r.isSuccess() ? "OK" : "FAIL").append("]: ").append(r.getData()).append("\n");
        }

        ModelResult result = modelFallback.callWithFallback(SYSTEM_PROMPT, userPrompt.toString());
        log.info("[AGENT-DIAGNOSIS] model={}, fallback={}", result.getModelUsed(), result.isFallbackActivated());

        return parseDiagnosis(result.getContent());
    }

    private DiagnosisResult parseDiagnosis(String content) {
        DiagnosisResult dr = new DiagnosisResult();
        String[] lines = content.split("\n");
        for (String line : lines) {
            String l = line.trim();
            if (l.startsWith("根因:") || l.startsWith("根因：")) dr.setRootCause(l.replaceFirst("根因[：:]\\s*", ""));
            else if (l.startsWith("风险等级:") || l.startsWith("风险等级：")) dr.setRiskLevel(l.replaceFirst("风险等级[：:]\\s*", "").trim());
            else if (l.startsWith("置信度:") || l.startsWith("置信度：")) {
                try { dr.setConfidence(Double.parseDouble(l.replaceFirst("置信度[：:]\\s*", "").trim())); } catch (Exception e) { dr.setConfidence(0.7); }
            }
            else if (l.startsWith("用户影响:") || l.startsWith("用户影响：")) dr.setImpactDescription(l.replaceFirst("用户影响[：:]\\s*", ""));
            else if (l.startsWith("是否需要升级:") || l.startsWith("是否需要升级：")) dr.setNeedsEscalation("true".equalsIgnoreCase(l.replaceFirst("是否需要升级[：:]\\s*", "").trim()));
        }
        if (dr.getRiskLevel() == null || dr.getRiskLevel().isBlank()) dr.setRiskLevel("P2");
        if (dr.getConfidence() == 0) dr.setConfidence(0.7);
        return dr;
    }
}
