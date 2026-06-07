package com.example.alarm.mock;

import com.example.alarm.model.mock.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OpsMockDataStore {

    private static final Logger log = LoggerFactory.getLogger(OpsMockDataStore.class);

    private final Map<String, ServiceStatus> serviceStatuses = new LinkedHashMap<>();
    private final Map<String, List<ErrorLog>> errorLogs = new LinkedHashMap<>();
    private final Map<String, List<DeployRecord>> deployRecords = new LinkedHashMap<>();
    private final Map<String, List<ServiceDependency>> dependencies = new LinkedHashMap<>();
    private final Map<String, ResourceMetrics> resourceMetrics = new LinkedHashMap<>();

    public OpsMockDataStore() {
        initServiceStatuses();
        initErrorLogs();
        initDeployRecords();
        initDependencies();
        initResourceMetrics();
        log.info("OpsMockDataStore initialized with 4 services");
    }

    // ------- Service Status -------
    private void initServiceStatuses() {
        serviceStatuses.put("order-service",
                new ServiceStatus("order-service", "healthy", "2026-06-06T20:00:00", "99.8%", 4));
        serviceStatuses.put("payment-service",
                new ServiceStatus("payment-service", "degraded", "2026-06-06T20:00:00", "95.2%", 3));
        serviceStatuses.put("inventory-service",
                new ServiceStatus("inventory-service", "degraded", "2026-06-06T20:00:00", "88.7%", 2));
        serviceStatuses.put("user-service",
                new ServiceStatus("user-service", "healthy", "2026-06-06T20:00:00", "99.9%", 5));
    }

    // ------- Error Logs -------
    private void initErrorLogs() {
        // order-service errors
        List<ErrorLog> orderErrors = Arrays.asList(
                new ErrorLog("2026-06-06T19:55:00", "ERROR", "order-service",
                        "接口超时: POST /api/order/create 耗时5230ms, 阈值3000ms",
                        "java.util.concurrent.TimeoutException: Request timed out after 5000ms\n" +
                                "  at org.apache.http.impl.execchain.RetryExec.execute(RetryExec.java:89)\n" +
                                "  at com.example.order.service.OrderService.create(OrderService.java:42)"),
                new ErrorLog("2026-06-06T19:56:12", "ERROR", "order-service",
                        "接口超时: POST /api/order/create 耗时4890ms, 阈值3000ms",
                        "java.util.concurrent.TimeoutException: Request timed out"),
                new ErrorLog("2026-06-06T19:57:30", "WARN", "order-service",
                        "下游payment-service响应变慢, P99延迟3200ms",
                        null),
                new ErrorLog("2026-06-06T19:50:00", "INFO", "order-service",
                        "服务发布: v2.3.1 部署完成, 包含订单流程重构",
                        null),
                new ErrorLog("2026-06-06T19:52:00", "ERROR", "order-service",
                        "数据库连接池等待超时, active=50, max=50, pending=12",
                        "org.springframework.jdbc.CannotGetJdbcConnectionException: Failed to obtain JDBC Connection")
        );
        errorLogs.put("order-service", orderErrors);

        // payment-service errors
        List<ErrorLog> payErrors = Arrays.asList(
                new ErrorLog("2026-06-06T19:30:00", "ERROR", "payment-service",
                        "支付接口错误率8.2%, 5分钟内失败47次/总请求573次",
                        null),
                new ErrorLog("2026-06-06T19:30:15", "ERROR", "payment-service",
                        "数据库连接池耗尽: HikariPool-1 - Connection is not available, request timed out after 30000ms",
                        "com.zaxxer.hikari.pool.HikariPool$PoolEntryCreator.call(HikariPool.java:728)"),
                new ErrorLog("2026-06-06T19:31:00", "ERROR", "payment-service",
                        "调用inventory-service扣减库存超时: GET /api/inventory/deduct 耗时8230ms",
                        "java.net.SocketTimeoutException: Read timed out"),
                new ErrorLog("2026-06-06T19:32:00", "WARN", "payment-service",
                        "重试队列积压: pending=128, retryCount=3",
                        null)
        );
        errorLogs.put("payment-service", payErrors);

        // inventory-service errors
        List<ErrorLog> invErrors = Arrays.asList(
                new ErrorLog("2026-06-06T19:10:00", "ERROR", "inventory-service",
                        "CPU使用率达到100%, 系统负载load average: 8.5",
                        null),
                new ErrorLog("2026-06-06T19:10:30", "WARN", "inventory-service",
                        "内存使用率90%, 堆内存: 3.6GB/4GB, GC频繁",
                        "java.lang.OutOfMemoryError: GC overhead limit exceeded"),
                new ErrorLog("2026-06-06T19:11:00", "ERROR", "inventory-service",
                        "接口P99延迟3500ms, GET /api/inventory/query",
                        null)
        );
        errorLogs.put("inventory-service", invErrors);

        // user-service errors
        List<ErrorLog> userErrors = Arrays.asList(
                new ErrorLog("2026-06-06T19:40:00", "ERROR", "user-service",
                        "数据库连接异常: Connection refused to 10.0.1.50:3306",
                        "com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure\n" +
                                "  at com.mysql.cj.jdbc.ConnectionImpl.createNewIO(ConnectionImpl.java:836)"),
                new ErrorLog("2026-06-06T19:40:30", "ERROR", "user-service",
                        "认证服务超时: /api/auth/verify 耗时12000ms",
                        null),
                new ErrorLog("2026-06-06T19:41:00", "WARN", "user-service",
                        "上游order-service请求量突增300%, 疑似重试风暴",
                        null)
        );
        errorLogs.put("user-service", userErrors);
    }

    // ------- Deploy Records -------
    private void initDeployRecords() {
        deployRecords.put("order-service", Arrays.asList(
                new DeployRecord("order-service", "v2.3.1", "2026-06-06T19:45:00", "completed",
                        "重构订单创建流程; 新增批量下单功能; 优化数据库查询"),
                new DeployRecord("order-service", "v2.3.0", "2026-06-05T14:00:00", "completed",
                        "修复订单状态更新bug; 添加超时重试机制"),
                new DeployRecord("order-service", "v2.2.1", "2026-06-01T10:00:00", "completed",
                        "版本稳定运行5天, 无异常")
        ));
        deployRecords.put("payment-service", Arrays.asList(
                new DeployRecord("payment-service", "v3.1.0", "2026-06-06T18:00:00", "completed",
                        "切换支付渠道; 更新数据库连接池配置(max=50→max=40)"),
                new DeployRecord("payment-service", "v3.0.5", "2026-06-04T09:00:00", "completed",
                        "修复退款回调bug")
        ));
        deployRecords.put("inventory-service", Arrays.asList(
                new DeployRecord("inventory-service", "v1.5.2", "2026-06-06T16:00:00", "completed",
                        "添加批量库存查询; 未做性能测试")
        ));
        deployRecords.put("user-service", Arrays.asList(
                new DeployRecord("user-service", "v4.0.1", "2026-06-06T10:00:00", "completed",
                        "常规安全补丁更新")
        ));
    }

    // ------- Dependencies -------
    private void initDependencies() {
        dependencies.put("order-service", Arrays.asList(
                new ServiceDependency("order-service", "user-service", "payment-service", "healthy"),
                new ServiceDependency("order-service", null, "inventory-service", "healthy")
        ));
        dependencies.put("payment-service", Arrays.asList(
                new ServiceDependency("payment-service", "order-service", "inventory-service", "degraded"),
                new ServiceDependency("payment-service", null, "database", "exhausted")
        ));
        dependencies.put("inventory-service", Arrays.asList(
                new ServiceDependency("inventory-service", "order-service", null, "healthy"),
                new ServiceDependency("inventory-service", "payment-service", null, "healthy")
        ));
        dependencies.put("user-service", Arrays.asList(
                new ServiceDependency("user-service", "order-service", "database", "error"),
                new ServiceDependency("user-service", null, "auth-service", "timeout")
        ));
    }

    // ------- Resource Metrics -------
    private void initResourceMetrics() {
        resourceMetrics.put("order-service",
                new ResourceMetrics("order-service", 72.5, 65.0, 5200.0, 15.3, 50, "slow"));
        resourceMetrics.put("payment-service",
                new ResourceMetrics("payment-service", 85.0, 78.0, 3200.0, 8.2, 40, "exhausted"));
        resourceMetrics.put("inventory-service",
                new ResourceMetrics("inventory-service", 100.0, 90.0, 3500.0, 3.1, 20, "healthy"));
        resourceMetrics.put("user-service",
                new ResourceMetrics("user-service", 45.0, 55.0, 1200.0, 0.5, 30, "error"));
    }

    // ======== Public accessors ========

    public ServiceStatus getServiceStatus(String serviceName) {
        ServiceStatus status = serviceStatuses.get(serviceName);
        if (status != null) return status;
        log.warn("ServiceStatus not found for: {}, returning default", serviceName);
        return new ServiceStatus(serviceName, "unknown", "N/A", "N/A", 0);
    }

    public List<ErrorLog> getErrorLogs(String serviceName, String alarmType) {
        List<ErrorLog> all = errorLogs.getOrDefault(serviceName, Collections.emptyList());
        if (all.isEmpty()) {
            log.warn("ErrorLogs not found for: {}", serviceName);
            return all;
        }
        // filter by alarmType keyword match
        if (alarmType != null && !alarmType.isBlank()) {
            List<ErrorLog> filtered = new ArrayList<>();
            String lower = alarmType.toLowerCase();
            for (ErrorLog log : all) {
                if (log.getMessage() != null && log.getMessage().toLowerCase().contains(lower)) {
                    filtered.add(log);
                } else if (containsKeyword(log.getMessage(), lower)) {
                    filtered.add(log);
                }
            }
            return filtered.isEmpty() ? all.subList(0, Math.min(all.size(), 5)) : filtered;
        }
        return all.size() > 10 ? all.subList(0, 10) : all;
    }

    private boolean containsKeyword(String msg, String keyword) {
        if (msg == null) return false;
        Map<String, String[]> mapping = new LinkedHashMap<>();
        mapping.put("timeout", new String[]{"timeout", "超时", "延迟", "等待"});
        mapping.put("error", new String[]{"error", "失败", "错误", "异常", "exception"});
        mapping.put("cpu", new String[]{"cpu", "负载", "load"});
        mapping.put("database", new String[]{"database", "数据库", "连接池", "connection", "db", "jdbc"});
        mapping.put("memory", new String[]{"memory", "内存", "heap", "gc"});
        mapping.put("deploy", new String[]{"deploy", "发布", "部署", "版本"});

        String[] keywords = mapping.get(keyword);
        if (keywords == null) {
            return msg.toLowerCase().contains(keyword);
        }
        for (String kw : keywords) {
            if (msg.toLowerCase().contains(kw)) return true;
        }
        return false;
    }

    public List<DeployRecord> getDeployRecords(String serviceName) {
        return deployRecords.getOrDefault(serviceName, Collections.emptyList());
    }

    public List<ServiceDependency> getDependencies(String serviceName) {
        return dependencies.getOrDefault(serviceName, Collections.emptyList());
    }

    public ResourceMetrics getResourceMetrics(String serviceName) {
        ResourceMetrics metrics = resourceMetrics.get(serviceName);
        if (metrics != null) return metrics;
        log.warn("ResourceMetrics not found for: {}, returning default", serviceName);
        return new ResourceMetrics(serviceName, 0, 0, 0, 0, 0, "unknown");
    }

    public boolean serviceExists(String serviceName) {
        return serviceStatuses.containsKey(serviceName);
    }

    public Set<String> getAllServiceNames() {
        return serviceStatuses.keySet();
    }
}
