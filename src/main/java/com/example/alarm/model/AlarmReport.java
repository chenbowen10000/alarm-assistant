package com.example.alarm.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AlarmReport {

    private String analysisStatus;
    private String serviceName;
    private String alarmType;
    private String riskLevel;
    private String severity;
    private Map<String, String> keyMetrics = new LinkedHashMap<>();
    private boolean userImpact;
    private String impactDescription;
    private String possibleRootCause;
    private List<String> evidence = new ArrayList<>();
    private double confidence;
    private List<String> recommendedActions = new ArrayList<>();
    private boolean shouldRollback;
    private boolean needsEscalation;
    private List<String> followUpMetrics = new ArrayList<>();
    private String nextCheckTime;

    public AlarmReport() {}

    public String getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getAlarmType() { return alarmType; }
    public void setAlarmType(String alarmType) { this.alarmType = alarmType; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Map<String, String> getKeyMetrics() { return keyMetrics; }
    public void setKeyMetrics(Map<String, String> keyMetrics) { this.keyMetrics = keyMetrics; }

    public boolean isUserImpact() { return userImpact; }
    public void setUserImpact(boolean userImpact) { this.userImpact = userImpact; }

    public String getImpactDescription() { return impactDescription; }
    public void setImpactDescription(String impactDescription) { this.impactDescription = impactDescription; }

    public String getPossibleRootCause() { return possibleRootCause; }
    public void setPossibleRootCause(String possibleRootCause) { this.possibleRootCause = possibleRootCause; }

    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public List<String> getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(List<String> recommendedActions) { this.recommendedActions = recommendedActions; }

    public boolean isShouldRollback() { return shouldRollback; }
    public void setShouldRollback(boolean shouldRollback) { this.shouldRollback = shouldRollback; }

    public boolean isNeedsEscalation() { return needsEscalation; }
    public void setNeedsEscalation(boolean needsEscalation) { this.needsEscalation = needsEscalation; }

    public List<String> getFollowUpMetrics() { return followUpMetrics; }
    public void setFollowUpMetrics(List<String> followUpMetrics) { this.followUpMetrics = followUpMetrics; }

    public String getNextCheckTime() { return nextCheckTime; }
    public void setNextCheckTime(String nextCheckTime) { this.nextCheckTime = nextCheckTime; }
}
