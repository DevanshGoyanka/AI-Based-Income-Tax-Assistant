package com.itr.domain.validation.validators;

import com.itr.domain.validation.*;
import com.itr.entity.ClientYearData;
import org.springframework.stereotype.Component;

/**
 * AssessmentYearConsistencyValidator - ensures AY consistency per Document 2 §E.2
 */
@Component
public class AssessmentYearConsistencyValidator implements Validator {
    
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
        String ay = draft.getAssessmentYear();
        
        if (ay == null || ay.trim().isEmpty()) {
            return ValidationResult.builder()
                .passed(false)
                .severity(severity())
                .validatorName("AssessmentYearConsistencyValidator")
                .fieldReference("assessmentYear")
                .message("Assessment Year is required")
                .itdErrorCode("AY-001")
                .build();
        }
        
        if (!ay.matches("\\d{4}-\\d{2}")) {
            return ValidationResult.builder()
                .passed(false)
                .severity(severity())
                .validatorName("AssessmentYearConsistencyValidator")
                .fieldReference("assessmentYear")
                .message("Assessment Year format must be YYYY-YY")
                .itdErrorCode("AY-002")
                .build();
        }
        
        return ValidationResult.builder()
            .passed(true)
            .severity(severity())
            .validatorName("AssessmentYearConsistencyValidator")
            .fieldReference("assessmentYear")
            .message("Assessment Year format valid")
            .build();
    }
}
