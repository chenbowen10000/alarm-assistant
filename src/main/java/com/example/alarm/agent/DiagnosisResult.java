package com.example.alarm.agent;

import java.util.LinkedHashMap;
import java.util.Map;

public class DiagnosisResult {
    private String rootCause;
    private String riskLevel;
    private double confidence;
    private Map<String, String> evidenceSummary = new LinkedHashMap<>();
    private boolean needsEscalation;
    private String impactDescription;

    public DiagnosisResult() {}

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public Map<String, String> getEvidenceSummary() { return evidenceSummary; }
    public void setEvidenceSummary(Map<String, String> evidenceSummary) { this.evidenceSummary = evidenceSummary; }

    public boolean isNeedsEscalation() { return needsEscalation; }
    public void setNeedsEscalation(boolean needsEscalation) { this.needsEscalation = needsEscalation; }

    public String getImpactDescription() { return impactDescription; }
    public void setImpactDescription(String impactDescription) { this.impactDescription = impactDescription; }
}
