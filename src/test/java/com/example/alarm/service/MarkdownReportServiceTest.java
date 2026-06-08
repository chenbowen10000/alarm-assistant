package com.example.alarm.service;

import com.example.alarm.model.AlarmReport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownReportServiceTest {

    @Test
    void testGenerateMarkdown() {
        MarkdownReportService service = new MarkdownReportService();
        AlarmReport report = new AlarmReport();
        report.setServiceName("order-service");
        report.setAlarmType("????");
        report.setRiskLevel("P1");
        report.setSeverity("??");
        report.setConfidence(0.85);
        report.setKeyMetrics(Map.of("???", "15%"));
        report.setPossibleRootCause("v2.3.1????????");
        report.setRecommendedActions(List.of("???v2.2.1", "??????"));
        report.setShouldRollback(true);
        report.setFollowUpMetrics(List.of("????", "???"));
        report.setNextCheckTime("5??");

        String md = service.generate("test-001", report);
        assertNotNull(md);
        assertTrue(md.contains("order-service"));
        assertTrue(md.contains("P1"));
        assertTrue(md.contains("??"));
        assertTrue(md.contains("test-001"));
    }
}
