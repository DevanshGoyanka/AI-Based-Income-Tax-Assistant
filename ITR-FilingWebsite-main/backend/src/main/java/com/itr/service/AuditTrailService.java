package com.itr.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Audit Trail Service - 101% CBDT Compliant
 * Tracks all data sources and computations
 */
@Slf4j
@Service
public class AuditTrailService {

    private List<AuditEntry> auditLog = new ArrayList<>();

    public void logDataSource(String source, String description, Object data) {
        AuditEntry entry = new AuditEntry();
        entry.setTimestamp(LocalDateTime.now());
        entry.setType("DATA_SOURCE");
        entry.setSource(source);
        entry.setDescription(description);
        entry.setData(data != null ? data.toString() : null);
        auditLog.add(entry);
        log.info("Audit: {} - {}", source, description);
    }

    public void logComputation(String computation, String input, String output) {
        AuditEntry entry = new AuditEntry();
        entry.setTimestamp(LocalDateTime.now());
        entry.setType("COMPUTATION");
        entry.setSource(computation);
        entry.setDescription("Input: " + input + " | Output: " + output);
        auditLog.add(entry);
        log.info("Audit: {} computed", computation);
    }

    public void logValidation(String rule, boolean passed, String message) {
        AuditEntry entry = new AuditEntry();
        entry.setTimestamp(LocalDateTime.now());
        entry.setType("VALIDATION");
        entry.setSource(rule);
        entry.setDescription(message);
        entry.setPassed(passed);
        auditLog.add(entry);
        log.info("Audit: {} - {}", rule, passed ? "PASS" : "FAIL");
    }

    public List<AuditEntry> getAuditLog() {
        return new ArrayList<>(auditLog);
    }

    public void clearAuditLog() {
        auditLog.clear();
    }

    @Data
    public static class AuditEntry {
        private LocalDateTime timestamp;
        private String type;
        private String source;
        private String description;
        private String data;
        private Boolean passed;
    }
}
