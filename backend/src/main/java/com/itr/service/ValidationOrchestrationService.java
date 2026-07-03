package com.itr.service;

import com.itr.domain.validation.ValidationPipeline;
import com.itr.domain.validation.ValidationReport;
import com.itr.entity.ClientYearData;
import com.itr.repository.ClientYearDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ValidationOrchestrationService - coordinates validation execution.
 * Document 1 §6
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ValidationOrchestrationService {
    
    private final ValidationPipeline validationPipeline;
    private final ClientYearDataRepository clientYearDataRepository;
    
    public ValidationReport validateDraft(Long clientId, String assessmentYear) {
        log.info("Validating draft for client {} AY {}", clientId, assessmentYear);
        
        ClientYearData draft = clientYearDataRepository
            .findByClientIdAndAssessmentYear(clientId, assessmentYear)
            .orElseThrow(() -> new RuntimeException("Draft not found"));
        
        return validationPipeline.validate(draft);
    }
}
