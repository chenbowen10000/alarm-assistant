package com.example.alarm.exception;

public class ToolInvocationException extends RuntimeException {
    private final String toolName;

    public ToolInvocationException(String toolName, String message) {
        super(message);
        this.toolName = toolName;
    }

    public String getToolName() { return toolName; }
}
