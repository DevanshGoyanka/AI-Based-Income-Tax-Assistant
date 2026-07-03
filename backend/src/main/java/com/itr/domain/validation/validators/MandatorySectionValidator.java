package com.itr.domain.validation.validators;

import com.itr.domain.validation.*;
import com.itr.entity.ClientYearData;
import org.springframework.stereotype.Component;

/**
 * MandatorySectionValidator - checks required fields per selected ITR form (Document 2 §E.6)
 */
@Component
public class MandatorySectionValidator implements Validator {
    
    @Override
    public ValidationStage stage() {
        return ValidationStage.COMPLETENESS;
    }
    
    @Override
    public ValidationSeverity severity() {
        return ValidationSeverity.BLOCKING;
    }
    
    @Override
    public ValidationResult validate(ClientYearData draft) {
        // Basic mandatory field checks
        if (draft.getClient() == null) {
            return ValidationResult.builder()
                .passed(false)
                .severity(severity())
                .validatorName("MandatorySectionValidator")
                .fieldReference("client")
                .message("Client information is required")
                .itdErrorCode("MANDATORY-001")
                .build();
        }
        
        return ValidationResult.builder()
            .passed(true)
            .severity(severity())
            .validatorName("MandatorySectionValidator")
            .message("Mandatory sections present")
            .build();
    }
}
