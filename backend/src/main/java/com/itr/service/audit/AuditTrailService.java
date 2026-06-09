package com.itr.service.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Audit Trail Service - CBDT Compliant Logging
 * Tracks all data changes, computations, and submissions
 */
@Service
@Slf4j
public class AuditTrailService {

    private final List<AuditEntry> auditLog = new ArrayList<>();

    /**
     * Log data import event
     */
    public void logDataImport(String userId, String source, String documentType, boolean success) {
        AuditEntry entry = AuditEntry.builder()
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .eventType("DATA_IMPORT")
                .source(source)
                .details("Imported " + documentType)
                .success(success)
                .build();
        
        auditLog.add(entry);
        log.info("Audit: {} imported {} from {} - {}", userId, documentType, source, 
                success ? "SUCCESS" : "FAILED");
    }

    /**
     * Log field edit event
     */
    public void logFieldEdit(String userId, String fieldName, String oldValue, String newValue) {
        AuditEntry entry = AuditEntry.builder()
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .eventType("FIELD_EDIT")
                .fieldName(fieldName)
                .oldValue(maskSensitiveData(oldValue))
                .newValue(maskSensitiveData(newValue))
                .success(true)
                .build();
        
        auditLog.add(entry);
        log.info("Audit: {} edited {} from [MASKED] to [MASKED]", userId, fieldName);
    }

    /**
     * Log computation event
     */
    public void logComputation(String userId, String computationType, double inputAmount, 
                               double outputAmount, boolean success) {
        AuditEntry entry = AuditEntry.builder()
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .eventType("COMPUTATION")
                .details(computationType)
                .inputAmount(inputAmount)
                .outputAmount(outputAmount)
                .success(success)
                .build();
        
        auditLog.add(entry);
        log.info("Audit: {} computed {} - Input: ₹{}, Output: ₹{}", 
                userId, computationType, inputAmount, outputAmount);
    }

    /**
     * Log validation event
     */
    public void logValidation(String userId, String validationType, boolean passed, 
                              int errorCount, int warningCount) {
        AuditEntry entry = AuditEntry.builder()
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .eventType("VALIDATION")
                .details(validationType)
                .success(passed)
                .errorCount(errorCount)
                .warningCount(warningCount)
                .build();
        
        auditLog.add(entry);
        log.info("Audit: {} validation {} - Errors: {}, Warnings: {}", 
                userId, passed ? "PASSED" : "FAILED", errorCount, warningCount);
    }

    /**
     * Log ITR submission event
     */
    public void logSubmission(String userId, String itrType, String acknowledgementNo, 
                             boolean success) {
        AuditEntry entry = AuditEntry.builder()
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .eventType("ITR_SUBMISSION")
                .details(itrType)
                .acknowledgementNo(acknowledgementNo)
                .success(success)
                .build();
        
        auditLog.add(entry);
        log.info("Audit: {} submitted {} - Ack: {} - {}", 
                userId, itrType, acknowledgementNo, success ? "SUCCESS" : "FAILED");
    }

    /**
     * Log PDF generation event
     */
    public void logPDFGeneration(String userId, String documentType, String version, 
                                boolean success) {
        AuditEntry entry = AuditEntry.builder()
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .eventType("PDF_GENERATION")
                .details(documentType)
                .version(version)
                .success(success)
                .build();
        
        auditLog.add(entry);
        log.info("Audit: {} generated PDF {} v{} - {}", 
                userId, documentType, version, success ? "SUCCESS" : "FAILED");
    }

    /**
     * Get audit trail for user
     */
    public List<AuditEntry> getAuditTrail(String userId) {
        return auditLog.stream()
                .filter(e -> e.getUserId().equals(userId))
                .toList();
    }

    /**
     * Get audit trail for user within date range
     */
    public List<AuditEntry> getAuditTrail(String userId, LocalDateTime from, LocalDateTime to) {
        return auditLog.stream()
                .filter(e -> e.getUserId().equals(userId))
                .filter(e -> e.getTimestamp().isAfter(from) && e.getTimestamp().isBefore(to))
                .toList();
    }

    private String maskSensitiveData(String value) {
        if (value == null) return null;
        if (value.length() <= 4) return "****";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditEntry {
        private LocalDateTime timestamp;
        private String userId;
        private String eventType;
        private String source;
        private String fieldName;
        private String oldValue;
        private String newValue;
        private String details;
        private double inputAmount;
        private double outputAmount;
        private boolean success;
        private int errorCount;
        private int warningCount;
        private String acknowledgementNo;
        private String version;
    }
}
