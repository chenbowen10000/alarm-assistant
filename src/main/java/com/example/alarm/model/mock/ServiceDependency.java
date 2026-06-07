package com.example.alarm.model.mock;

public class ServiceDependency {
    private String serviceName;
    private String upstream;
    private String downstream;
    private String dependencyStatus;

    public ServiceDependency() {}

    public ServiceDependency(String serviceName, String upstream, String downstream, String dependencyStatus) {
        this.serviceName = serviceName;
        this.upstream = upstream;
        this.downstream = downstream;
        this.dependencyStatus = dependencyStatus;
    }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getUpstream() { return upstream; }
    public void setUpstream(String upstream) { this.upstream = upstream; }

    public String getDownstream() { return downstream; }
    public void setDownstream(String downstream) { this.downstream = downstream; }

    public String getDependencyStatus() { return dependencyStatus; }
    public void setDependencyStatus(String dependencyStatus) { this.dependencyStatus = dependencyStatus; }
}
