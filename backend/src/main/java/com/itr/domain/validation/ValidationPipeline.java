package com.itr.domain.validation;

import com.itr.entity.ClientYearData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * ValidationPipeline - executes all registered validators in stage order.
 * Document 1 §6.2
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationPipeline {
    
    private final List<Validator> validators;
    
    public ValidationReport validate(ClientYearData draft) {
        log.info("Running validation pipeline for client {} AY {}", 
            draft.getClient().getId(), draft.getAssessmentYear());
        
        ValidationReport.ValidationReportBuilder reportBuilder = ValidationReport.builder()
            .clientId(draft.getClient().getId())
            .assessmentYear(draft.getAssessmentYear())
            .validatedAt(Instant.now());
        
        // Execute validators in stage order
        validators.stream()
            .sorted(Comparator.comparing(Validator::stage))
            .forEach(validator -> {
                try {
                    ValidationResult result = validator.validate(draft);
                    reportBuilder.result(result);
                    
                    if (!result.isPassed() && result.getSeverity() == ValidationSeverity.BLOCKING) {
                        log.warn("BLOCKING validation failure: {} - {}", 
                            validator.getClass().getSimpleName(), result.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Validator {} threw exception", validator.getClass().getSimpleName(), e);
                    reportBuilder.result(ValidationResult.builder()
                        .passed(false)
                        .severity(ValidationSeverity.WARNING)
                        .validatorName(validator.getClass().getSimpleName())
                        .message("Validator error: " + e.getMessage())
                        .build());
                }
            });
        
        return reportBuilder.build();
    }
}
