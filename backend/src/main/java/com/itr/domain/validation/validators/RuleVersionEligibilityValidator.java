package com.itr.domain.validation.validators;

import com.itr.domain.ruleengine.RuleLifecycleState;
import com.itr.domain.validation.*;
import com.itr.entity.ClientYearData;
import org.springframework.stereotype.Component;

/**
 * RuleVersionEligibilityValidator - blocks filing artifacts against DRAFT/ARCHIVED rules (Document 1 §5.3)
 */
@Component
public class RuleVersionEligibilityValidator implements Validator {
    
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
        // TODO: Wire to TaxYearRulesRegistry.forAY(ay).lifecycleState() from Phase 2
        // For now, assume SUPPORTED state for AY 2026-27
        RuleLifecycleState state = RuleLifecycleState.SUPPORTED;
        
        if (state == RuleLifecycleState.DRAFT) {
            return ValidationResult.builder()
                .passed(false)
                .severity(severity())
                .validatorName("RuleVersionEligibilityValidator")
                .fieldReference("assessmentYear")
                .message("Cannot file against DRAFT rule version - rules not yet verified")
                .itdErrorCode("RULE-001")
                .build();
        }
        
        if (state == RuleLifecycleState.ARCHIVED) {
            return ValidationResult.builder()
                .passed(false)
                .severity(severity())
                .validatorName("RuleVersionEligibilityValidator")
                .fieldReference("assessmentYear")
                .message("Cannot file against ARCHIVED rule version")
                .itdErrorCode("RULE-002")
                .build();
        }
        
        return ValidationResult.builder()
            .passed(true)
            .severity(severity())
            .validatorName("RuleVersionEligibilityValidator")
            .message("Rule version is SUPPORTED")
            .build();
    }
}
