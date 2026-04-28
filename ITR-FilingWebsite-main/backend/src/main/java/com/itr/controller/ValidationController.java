package com.itr.controller;

import com.itr.dto.FlatFormData;
import com.itr.dto.Itr1FormData;
import com.itr.service.validation.CBDTValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Validation Controller - CBDT Compliance Validation
 */
@RestController
@RequestMapping("/api/validation")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ValidationController {

    private final CBDTValidationService validationService;

    @PostMapping("/itr1")
    public ResponseEntity<ValidationResponse> validateItr1(@RequestBody Itr1FormData formData) {
        log.info("Validating ITR-1 form data");
        CBDTValidationService.ValidationResult result = validationService.validateItr1FormData(formData);
        
        return ResponseEntity.ok(ValidationResponse.builder()
                .valid(result.isValid())
                .errors(result.getErrors())
                .warnings(result.getWarnings())
                .errorCount(result.getErrorCount())
                .warningCount(result.getWarningCount())
                .build());
    }

    @PostMapping("/flat")
    public ResponseEntity<ValidationResponse> validateFlat(@RequestBody FlatFormData formData) {
        log.info("Validating flat form data");
        CBDTValidationService.ValidationResult result = validationService.validateFlatFormData(formData);
        
        return ResponseEntity.ok(ValidationResponse.builder()
                .valid(result.isValid())
                .errors(result.getErrors())
                .warnings(result.getWarnings())
                .errorCount(result.getErrorCount())
                .warningCount(result.getWarningCount())
                .build());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidationResponse {
        private boolean valid;
        private java.util.List<String> errors;
        private java.util.List<String> warnings;
        private int errorCount;
        private int warningCount;
    }
}
