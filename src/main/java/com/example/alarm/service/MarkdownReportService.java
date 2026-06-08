package com.example.alarm.service;

import com.example.alarm.model.AlarmReport;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class MarkdownReportService {

    public String generate(String analysisId, AlarmReport report) {
        StringBuilder md = new StringBuilder();
        md.append("# 运维告警处置报告\n\n");
        md.append("**分析ID**: ").append(analysisId).append("  \n");
        md.append("**生成时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("  \n\n---\n\n");
        md.append("## 基本信息\n\n| 字段 | 值 |\n|------|------|\n");
        md.append("| 服务名称 | ").append(s(report.getServiceName())).append(" |\n");
        md.append("| 告警类型 | ").append(s(report.getAlarmType())).append(" |\n");
        md.append("| 风险等级 | **").append(s(report.getRiskLevel())).append(" · ").append(s(report.getSeverity())).append("** |\n");
        md.append("| 置信度 | ").append(String.format("%.0f%%", report.getConfidence() * 100)).append(" |\n\n");
        md.append("## 异常指标\n\n");
        if (report.getKeyMetrics() != null && !report.getKeyMetrics().isEmpty()) {
            md.append("| 指标 | 值 |\n|------|------|\n");
            report.getKeyMetrics().forEach((k, v) -> md.append("| ").append(k).append(" | ").append(v).append(" |\n"));
        } else md.append("无\n");
        md.append("\n## 用户影响\n\n- **是否影响用户**: ").append(report.isUserImpact() ? "是" : "否").append("\n");
        md.append("- **影响描述**: ").append(s(report.getImpactDescription())).append("\n\n");
        md.append("## 根因分析\n\n").append(s(report.getPossibleRootCause())).append("\n\n");
        md.append("## 工具调用证据\n\n");
        if (report.getEvidence() != null && !report.getEvidence().isEmpty()) {
            for (String e : report.getEvidence()) md.append("- ").append(e).append("\n");
        } else md.append("无\n");
        md.append("\n## 处置建议\n\n");
        if (report.getRecommendedActions() != null) {
            int i = 1;
            for (String a : report.getRecommendedActions()) md.append(i++).append(". ").append(a).append("\n");
        }
        md.append("\n- **是否建议回滚**: ").append(report.isShouldRollback() ? "⚠ 是" : "否").append("\n");
        md.append("- **是否需要人工升级**: ").append(report.isNeedsEscalation() ? "⚠ 是" : "否").append("\n\n");
        md.append("## 后续观察指标\n\n");
        if (report.getFollowUpMetrics() != null) {
            for (String m : report.getFollowUpMetrics()) md.append("- ").append(m).append("\n");
        }
        md.append("\n**下次检查**: ").append(s(report.getNextCheckTime())).append("\n");
        return md.toString();
    }

    private String s(String v) { return v == null || v.isBlank() ? "N/A" : v; }
}
