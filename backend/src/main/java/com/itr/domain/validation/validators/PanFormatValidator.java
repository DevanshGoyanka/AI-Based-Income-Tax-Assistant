package com.itr.domain.validation.validators;

import com.itr.domain.validation.*;
import com.itr.entity.ClientYearData;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * PanFormatValidator - validates PAN format per Document 2 §E.1
 */
@Component
public class PanFormatValidator implements Validator {
    
    private static final Pattern PAN_PATTERN = Pattern.compile("[A-Z]{3}[PCHFATBLJG][A-Z][0-9]{4}[A-Z]");
    
    @Override
    public ValidationStage stage() {
        return ValidationStage.IDENTITY;
    }
    
    @Override
    public ValidationSeverity severity() {
        return ValidationSeverity.BLOCKING;
    }
    
    @Override
    public ValidationResult validate(ClientYearData draft) {
        String pan = draft.getClient().getPan();
        
        if (pan == null || pan.trim().isEmpty()) {
            return ValidationResult.builder()
                .passed(false)
                .severity(severity())
                .validatorName("PanFormatValidator")
                .fieldReference("client.pan")
                .message("PAN is required")
                .itdErrorCode("PAN-001")
                .build();
        }
        
        if (!PAN_PATTERN.matcher(pan.toUpperCase()).matches()) {
            return ValidationResult.builder()
                .passed(false)
                .severity(severity())
                .validatorName("PanFormatValidator")
                .fieldReference("client.pan")
                .message("Invalid PAN format: " + pan)
                .itdErrorCode("PAN-002")
                .build();
        }
        
        return ValidationResult.builder()
            .passed(true)
            .severity(severity())
            .validatorName("PanFormatValidator")
            .fieldReference("client.pan")
            .message("PAN format valid")
            .build();
    }
}
