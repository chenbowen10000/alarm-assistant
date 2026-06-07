package com.example.alarm.model.mock;

public class ResourceMetrics {
    private String serviceName;
    private double cpuPercent;
    private double memoryPercent;
    private double p99LatencyMs;
    private double errorRatePercent;
    private int activeConnections;
    private String dbStatus;

    public ResourceMetrics() {}

    public ResourceMetrics(String serviceName, double cpuPercent, double memoryPercent,
                           double p99LatencyMs, double errorRatePercent, int activeConnections, String dbStatus) {
        this.serviceName = serviceName;
        this.cpuPercent = cpuPercent;
        this.memoryPercent = memoryPercent;
        this.p99LatencyMs = p99LatencyMs;
        this.errorRatePercent = errorRatePercent;
        this.activeConnections = activeConnections;
        this.dbStatus = dbStatus;
    }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public double getCpuPercent() { return cpuPercent; }
    public void setCpuPercent(double cpuPercent) { this.cpuPercent = cpuPercent; }

    public double getMemoryPercent() { return memoryPercent; }
    public void setMemoryPercent(double memoryPercent) { this.memoryPercent = memoryPercent; }

    public double getP99LatencyMs() { return p99LatencyMs; }
    public void setP99LatencyMs(double p99LatencyMs) { this.p99LatencyMs = p99LatencyMs; }

    public double getErrorRatePercent() { return errorRatePercent; }
    public void setErrorRatePercent(double errorRatePercent) { this.errorRatePercent = errorRatePercent; }

    public int getActiveConnections() { return activeConnections; }
    public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }

    public String getDbStatus() { return dbStatus; }
    public void setDbStatus(String dbStatus) { this.dbStatus = dbStatus; }
}
