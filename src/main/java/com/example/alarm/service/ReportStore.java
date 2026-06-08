package com.example.alarm.service;

import com.example.alarm.model.AlarmReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReportStore {

    private static final Logger log = LoggerFactory.getLogger(ReportStore.class);
    private static final int TTL_MINUTES = 60;
    private final ConcurrentHashMap<String, StoredReport> reports = new ConcurrentHashMap<>();

    private static class StoredReport {
        final AlarmReport report;
        final LocalDateTime storedAt;
        StoredReport(AlarmReport report) { this.report = report; this.storedAt = LocalDateTime.now(); }
        boolean isExpired() { return storedAt.plusMinutes(TTL_MINUTES).isBefore(LocalDateTime.now()); }
    }

    public void store(String analysisId, AlarmReport report) {
        reports.put(analysisId, new StoredReport(report));
    }

    public AlarmReport get(String analysisId) {
        StoredReport sr = reports.get(analysisId);
        if (sr != null && !sr.isExpired()) return sr.report;
        reports.remove(analysisId);
        return null;
    }

    @Scheduled(fixedRate = 600000)
    public void cleanupExpired() {
        reports.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
