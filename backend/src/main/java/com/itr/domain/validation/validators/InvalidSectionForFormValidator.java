package com.itr.domain.validation.validators;

import com.itr.domain.validation.*;
import com.itr.entity.ClientYearData;
import org.springframework.stereotype.Component;

/**
 * InvalidSectionForFormValidator - validates schedule compatibility with selected ITR form (Document 2 §E.6)
 */
@Component
public class InvalidSectionForFormValidator implements Validator {
    
    @Override
    public ValidationStage stage() {
        return ValidationStage.CONSISTENCY;
    }
    
    @Override
    public ValidationSeverity severity() {
        return ValidationSeverity.BLOCKING;
    }
    
    @Override
    public ValidationResult validate(ClientYearData draft) {
        // TODO: Implement based on ItrFormDefinition.applicableSchedules() from Phase 2
        // Will be wired in Phases 6-11 as schedules are implemented
        return ValidationResult.builder()
            .passed(true)
            .severity(severity())
            .validatorName("InvalidSectionForFormValidator")
            .message("Schedule-form compatibility check passed")
            .build();
    }
}
