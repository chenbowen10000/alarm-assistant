package com.example.alarm.model;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class AlarmReportTest {

    @Test
    void allFields_shouldBeSetAndGet() {
        AlarmReport r = new AlarmReport();
        r.setAnalysisStatus("SUCCESS");
        r.setServiceName("order-service");
        r.setAlarmType("接口超时");
        r.setRiskLevel("P1");
        r.setSeverity("严重");

        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("超时率", "15%");
        r.setKeyMetrics(metrics);

        r.setUserImpact(true);
        r.setImpactDescription("用户无法下单");
        r.setPossibleRootCause("v2.3.1发布");
        r.setEvidence(Arrays.asList("e1", "e2"));
        r.setConfidence(0.85);
        r.setRecommendedActions(Arrays.asList("回滚", "扩容"));
        r.setShouldRollback(true);
        r.setNeedsEscalation(true);
        r.setFollowUpMetrics(Arrays.asList("P99延迟", "错误率"));
        r.setNextCheckTime("5分钟后");

        assertThat(r.getAnalysisStatus()).isEqualTo("SUCCESS");
        assertThat(r.getServiceName()).isEqualTo("order-service");
        assertThat(r.getAlarmType()).isEqualTo("接口超时");
        assertThat(r.getRiskLevel()).isEqualTo("P1");
        assertThat(r.getSeverity()).isEqualTo("严重");
        assertThat(r.getKeyMetrics()).containsEntry("超时率", "15%");
        assertThat(r.isUserImpact()).isTrue();
        assertThat(r.getImpactDescription()).isEqualTo("用户无法下单");
        assertThat(r.getPossibleRootCause()).isEqualTo("v2.3.1发布");
        assertThat(r.getEvidence()).hasSize(2);
        assertThat(r.getConfidence()).isEqualTo(0.85);
        assertThat(r.getRecommendedActions()).contains("回滚");
        assertThat(r.isShouldRollback()).isTrue();
        assertThat(r.isNeedsEscalation()).isTrue();
        assertThat(r.getFollowUpMetrics()).hasSize(2);
        assertThat(r.getNextCheckTime()).isEqualTo("5分钟后");
    }

    @Test
    void defaultReport_shouldHaveEmptyCollections() {
        AlarmReport r = new AlarmReport();
        assertThat(r.getEvidence()).isEmpty();
        assertThat(r.getKeyMetrics()).isEmpty();
        assertThat(r.getRecommendedActions()).isEmpty();
        assertThat(r.getFollowUpMetrics()).isEmpty();
    }
}