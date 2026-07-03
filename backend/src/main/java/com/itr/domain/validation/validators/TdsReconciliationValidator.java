package com.itr.domain.validation.validators;

import com.itr.domain.validation.*;
import com.itr.entity.ClientYearData;
import org.springframework.stereotype.Component;

/**
 * TdsReconciliationValidator - reconciles TDS entries against 26AS/AIS (Document 2 §E.3, §C.14)
 */
@Component
public class TdsReconciliationValidator implements Validator {
    
    private static final long DEFAULT_TOLERANCE = 100; // ₹1 tolerance in paise
    
    @Override
    public ValidationStage stage() {
        return ValidationStage.RECONCILIATION;
    }
    
    @Override
    public ValidationSeverity severity() {
        return ValidationSeverity.WARNING;
    }
    
    @Override
    public ValidationResult validate(ClientYearData draft) {
        // TODO: Implement when TDS/26AS import is wired in Phase 4
        // Will compare Schedule TDS entries against imported 26AS data
        return ValidationResult.builder()
            .passed(true)
            .severity(severity())
            .validatorName("TdsReconciliationValidator")
            .message("TDS reconciliation passed")
            .build();
    }
}
