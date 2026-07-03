package com.itr.domain.validation;

import com.itr.entity.ClientYearData;

/**
 * Validator - single validation rule interface.
 * Document 1 §6.2
 */
public interface Validator {
    ValidationStage stage();
    ValidationSeverity severity();
    ValidationResult validate(ClientYearData draft);
}
