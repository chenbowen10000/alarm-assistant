package com.example.alarm.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChatContext {
    private String sessionId;
    private String previousAnalysis;
    private String previousServiceName;
    private String question;
    private Map<String, String> extra = new LinkedHashMap<>();

    public ChatContext() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getPreviousAnalysis() { return previousAnalysis; }
    public void setPreviousAnalysis(String previousAnalysis) { this.previousAnalysis = previousAnalysis; }

    public String getPreviousServiceName() { return previousServiceName; }
    public void setPreviousServiceName(String previousServiceName) { this.previousServiceName = previousServiceName; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public Map<String, String> getExtra() { return extra; }
    public void setExtra(Map<String, String> extra) { this.extra = extra; }

    public boolean hasContext() {
        return previousAnalysis != null && !previousAnalysis.isBlank();
    }

    public String buildContextPrompt() {
        if (!hasContext()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("=== 前序上下文 ===\n");
        sb.append("上一轮分析结果: ").append(previousAnalysis).append("\n");
        if (previousServiceName != null) sb.append("涉及服务: ").append(previousServiceName).append("\n");
        sb.append("用户追问: ").append(question).append("\n");
        return sb.toString();
    }
}
