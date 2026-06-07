package com.example.alarm.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParsedAlarm {

    private String serviceName;
    private String alarmType;
    private Map<String, String> keyMetrics = new LinkedHashMap<>();
    private String riskLevel;
    private boolean userImpact;
    private boolean needsEscalation;
    private double confidence;

    public ParsedAlarm() {}

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getAlarmType() { return alarmType; }
    public void setAlarmType(String alarmType) { this.alarmType = alarmType; }

    public Map<String, String> getKeyMetrics() { return keyMetrics; }
    public void setKeyMetrics(Map<String, String> keyMetrics) { this.keyMetrics = keyMetrics; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public boolean isUserImpact() { return userImpact; }
    public void setUserImpact(boolean userImpact) { this.userImpact = userImpact; }

    public boolean isNeedsEscalation() { return needsEscalation; }
    public void setNeedsEscalation(boolean needsEscalation) { this.needsEscalation = needsEscalation; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}