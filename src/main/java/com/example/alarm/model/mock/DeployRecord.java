package com.example.alarm.model.mock;

public class DeployRecord {
    private String serviceName;
    private String version;
    private String deployTime;
    private String status;
    private String changeLog;

    public DeployRecord() {}

    public DeployRecord(String serviceName, String version, String deployTime, String status, String changeLog) {
        this.serviceName = serviceName;
        this.version = version;
        this.deployTime = deployTime;
        this.status = status;
        this.changeLog = changeLog;
    }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDeployTime() { return deployTime; }
    public void setDeployTime(String deployTime) { this.deployTime = deployTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getChangeLog() { return changeLog; }
    public void setChangeLog(String changeLog) { this.changeLog = changeLog; }
}
