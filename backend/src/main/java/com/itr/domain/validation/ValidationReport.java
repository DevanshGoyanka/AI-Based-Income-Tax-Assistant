package com.itr.domain.validation;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * ValidationReport - aggregated validation results.
 * Document 1 §6.2
 */
@Value
@Builder
public class ValidationReport {
    Long clientId;
    String assessmentYear;
    Instant validatedAt;
    
    @Singular
    List<ValidationResult> results;
    
    public boolean hasBlockingIssues() {
        return results.stream()
            .anyMatch(r -> !r.isPassed() && r.getSeverity() == ValidationSeverity.BLOCKING);
    }
    
    public long blockingCount() {
        return results.stream()
            .filter(r -> !r.isPassed() && r.getSeverity() == ValidationSeverity.BLOCKING)
            .count();
    }
    
    public long warningCount() {
        return results.stream()
            .filter(r -> !r.isPassed() && r.getSeverity() == ValidationSeverity.WARNING)
            .count();
    }
}
