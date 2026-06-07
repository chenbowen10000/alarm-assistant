package com.example.alarm.model;

public class ToolResult {

    private String toolName;
    private String serviceName;
    private boolean success;
    private String data;
    private long latencyMs;
    private String errorMessage;

    public ToolResult() {}

    public static ToolResult success(String toolName, String serviceName, String data, long latencyMs) {
        ToolResult r = new ToolResult();
        r.toolName = toolName;
        r.serviceName = serviceName;
        r.success = true;
        r.data = data;
        r.latencyMs = latencyMs;
        return r;
    }

    public static ToolResult error(String toolName, String serviceName, String errorMessage) {
        ToolResult r = new ToolResult();
        r.toolName = toolName;
        r.serviceName = serviceName;
        r.success = false;
        r.errorMessage = errorMessage;
        r.data = "null";
        return r;
    }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @Override
    public String toString() {
        if (success) {
            return String.format("[%s] %s: %s (%dms)", success ? "OK" : "FAIL", toolName, data, latencyMs);
        } else {
            return String.format("[FAIL] %s: %s", toolName, errorMessage);
        }
    }
}
