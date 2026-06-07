package com.example.alarm.model.mock;

public class ErrorLog {
    private String timestamp;
    private String level;
    private String serviceName;
    private String message;
    private String stackTrace;

    public ErrorLog() {}

    public ErrorLog(String timestamp, String level, String serviceName, String message, String stackTrace) {
        this.timestamp = timestamp;
        this.level = level;
        this.serviceName = serviceName;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
}
