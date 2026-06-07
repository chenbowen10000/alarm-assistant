package com.example.alarm.model.mock;

public class ServiceStatus {
    private String serviceName;
    private String status;
    private String healthCheckTime;
    private String uptime;
    private int instanceCount;

    public ServiceStatus() {}

    public ServiceStatus(String serviceName, String status, String healthCheckTime, String uptime, int instanceCount) {
        this.serviceName = serviceName;
        this.status = status;
        this.healthCheckTime = healthCheckTime;
        this.uptime = uptime;
        this.instanceCount = instanceCount;
    }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHealthCheckTime() { return healthCheckTime; }
    public void setHealthCheckTime(String healthCheckTime) { this.healthCheckTime = healthCheckTime; }

    public String getUptime() { return uptime; }
    public void setUptime(String uptime) { this.uptime = uptime; }

    public int getInstanceCount() { return instanceCount; }
    public void setInstanceCount(int instanceCount) { this.instanceCount = instanceCount; }
}
