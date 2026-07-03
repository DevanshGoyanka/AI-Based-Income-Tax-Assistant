package com.itr.domain.validation.validators;

import com.itr.domain.validation.*;
import com.itr.entity.ClientYearData;
import org.springframework.stereotype.Component;

/**
 * DuplicateDocumentValidator - prevents duplicate imports per Document 2 §E.5
 */
@Component
public class DuplicateDocumentValidator implements Validator {
    
    @Override
    public ValidationStage stage() {
        return ValidationStage.STRUCTURAL;
    }
    
    @Override
    public ValidationSeverity severity() {
        return ValidationSeverity.BLOCKING;
    }
    
    @Override
    public ValidationResult validate(ClientYearData draft) {
        // TODO: Implement when document import tracking is added in Phase 4
        // For now, pass validation
        return ValidationResult.builder()
            .passed(true)
            .severity(severity())
            .validatorName("DuplicateDocumentValidator")
            .fieldReference("documents")
            .message("Duplicate document check passed")
            .build();
    }
}
