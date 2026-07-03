package com.itr.domain.validation;

import lombok.Builder;
import lombok.Value;

/**
 * ValidationResult - single validation finding.
 * Document 1 §6.2
 */
@Value
@Builder
public class ValidationResult {
    boolean passed;
    ValidationSeverity severity;
    String validatorName;
    String fieldReference;
    String message;
    String itdErrorCode;  // CBDT error code where applicable
}
