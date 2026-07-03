package com.itr.controller;

import com.itr.domain.validation.ValidationReport;
import com.itr.service.ValidationOrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ValidationController - on-demand validation endpoints.
 * Document 1 §6, Document 3 Phase 3
 */
@RestController
@RequestMapping("/api/v1/clients/{clientId}/validate")
@RequiredArgsConstructor
public class ValidationController {
    
    private final ValidationOrchestrationService validationService;
    
    @PostMapping("/{assessmentYear}")
    public ResponseEntity<ValidationReport> validate(
            @PathVariable Long clientId,
            @PathVariable String assessmentYear) {
        
        ValidationReport report = validationService.validateDraft(clientId, assessmentYear);
        return ResponseEntity.ok(report);
    }
}
