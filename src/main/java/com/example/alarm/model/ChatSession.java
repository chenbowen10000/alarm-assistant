package com.example.alarm.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatSession {

    private String sessionId;
    private List<Exchange> history = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;

    public ChatSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
    }

    public static class Exchange {
        private String alarmText;
        private String reportJson;
        private String followUpQuestion;
        private String followUpAnswer;
        private LocalDateTime timestamp;

        public Exchange() { this.timestamp = LocalDateTime.now(); }

        public String getAlarmText() { return alarmText; }
        public void setAlarmText(String alarmText) { this.alarmText = alarmText; }

        public String getReportJson() { return reportJson; }
        public void setReportJson(String reportJson) { this.reportJson = reportJson; }

        public String getFollowUpQuestion() { return followUpQuestion; }
        public void setFollowUpQuestion(String followUpQuestion) { this.followUpQuestion = followUpQuestion; }

        public String getFollowUpAnswer() { return followUpAnswer; }
        public void setFollowUpAnswer(String followUpAnswer) { this.followUpAnswer = followUpAnswer; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public List<Exchange> getHistory() { return history; }
    public void setHistory(List<Exchange> history) { this.history = history; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public void touch() { this.lastActiveAt = LocalDateTime.now(); }

    public boolean isExpired(int ttlMinutes) {
        return lastActiveAt.plusMinutes(ttlMinutes).isBefore(LocalDateTime.now());
    }
}
