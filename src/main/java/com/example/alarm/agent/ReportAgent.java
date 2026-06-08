package com.example.alarm.agent;

import com.example.alarm.model.AlarmReport;
import com.example.alarm.model.ParsedAlarm;
import com.example.alarm.model.ToolResult;
import com.example.alarm.pipeline.ModelFallbackHandler;
import com.example.alarm.pipeline.ModelFallbackHandler.ModelResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReportAgent {

    private static final Logger log = LoggerFactory.getLogger(ReportAgent.class);
    private final ModelFallbackHandler modelFallback;

    private static final String SYSTEM_PROMPT =
            "你是一个运维报告撰写专家。请根据告警解析、工具证据和诊断结果，生成处置建议。" +
            "用中文返回，格式：\n" +
            "建议动作:\n1. <动作1>\n2. <动作2>\n...\n是否回滚: true/false\n后续观察:\n- <指标1>\n- <指标2>";

    public ReportAgent(ModelFallbackHandler modelFallback) {
        this.modelFallback = modelFallback;
    }

    public void generate(AlarmReport report, ParsedAlarm parsedAlarm, DiagnosisResult diagnosis, List<ToolResult> toolResults) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("=== 告警 ===\n服务: ").append(parsedAlarm.getServiceName()).append(", 类型: ").append(parsedAlarm.getAlarmType()).append("\n");
        userPrompt.append("=== 诊断 ===\n根因: ").append(diagnosis.getRootCause()).append("\n风险: ").append(diagnosis.getRiskLevel()).append("\n");
        userPrompt.append("=== 证据 ===\n");
        for (ToolResult r : toolResults) {
            if (r.isSuccess()) userPrompt.append("- ").append(r.getToolName()).append(": ").append(r.getData()).append("\n");
        }

        ModelResult result = modelFallback.callWithFallback(SYSTEM_PROMPT, userPrompt.toString());
        log.info("[AGENT-REPORT] model={}, fallback={}", result.getModelUsed(), result.isFallbackActivated());

        parseActions(result.getContent(), report, parsedAlarm);
    }

    private void parseActions(String content, AlarmReport report, ParsedAlarm parsedAlarm) {
        List<String> actions = new ArrayList<>();
        boolean inActions = false;
        boolean inFollowUp = false;

        for (String line : content.split("\n")) {
            String l = line.trim();
            if (l.contains("建议动作") || l.contains("处置建议")) { inActions = true; inFollowUp = false; continue; }
            if (l.contains("后续观察") || l.contains("是否回滚")) { inActions = false; inFollowUp = true; }
            if (inActions && l.matches("^\\d+\\..*")) actions.add(l.replaceFirst("^\\d+\\.\\s*", ""));
            if (l.contains("是否回滚") && l.toLowerCase().contains("true")) report.setShouldRollback(true);
        }

        if (actions.isEmpty()) {
            actions.add("查看详细错误日志定位具体异常");
            actions.add("通知相关开发及运维人员关注");
        }
        report.setRecommendedActions(actions);
    }
}
