package com.itr.domain.validation.validators;

import com.itr.domain.validation.*;
import com.itr.entity.ClientYearData;
import org.springframework.stereotype.Component;

/**
 * AadhaarFormatValidator - validates Aadhaar format with Verhoeff checksum per Document 2 §E.1
 */
@Component
public class AadhaarFormatValidator implements Validator {
    
    private static final int[][] MULTIPLICATION_TABLE = {
        {0,1,2,3,4,5,6,7,8,9}, {1,2,3,4,0,6,7,8,9,5},
        {2,3,4,0,1,7,8,9,5,6}, {3,4,0,1,2,8,9,5,6,7},
        {4,0,1,2,3,9,5,6,7,8}, {5,9,8,7,6,0,4,3,2,1},
        {6,5,9,8,7,1,0,4,3,2}, {7,6,5,9,8,2,1,0,4,3},
        {8,7,6,5,9,3,2,1,0,4}, {9,8,7,6,5,4,3,2,1,0}
    };
    
    private static final int[][] PERMUTATION_TABLE = {
        {0,1,2,3,4,5,6,7,8,9}, {1,5,7,6,2,8,3,0,9,4},
        {5,8,0,3,7,9,6,1,4,2}, {8,9,1,6,0,4,3,5,2,7},
        {9,4,5,3,1,2,6,8,7,0}, {4,2,8,6,5,7,3,9,0,1},
        {2,7,9,3,8,0,6,4,1,5}, {7,0,4,6,9,1,3,2,5,8}
    };
    
    @Override
    public ValidationStage stage() {
        return ValidationStage.IDENTITY;
    }
    
    @Override
    public ValidationSeverity severity() {
        return ValidationSeverity.WARNING;
    }
    
    @Override
    public ValidationResult validate(ClientYearData draft) {
        String aadhaar = draft.getClient().getAadhaar();
        
        if (aadhaar == null || aadhaar.trim().isEmpty()) {
            return ValidationResult.builder()
                .passed(true)
                .severity(severity())
                .validatorName("AadhaarFormatValidator")
                .fieldReference("client.aadhaar")
                .message("Aadhaar not provided (optional)")
                .build();
        }
        
        String cleaned = aadhaar.replaceAll("\\s+", "");
        
        if (!cleaned.matches("\\d{12}")) {
            return ValidationResult.builder()
                .passed(false)
                .severity(severity())
                .validatorName("AadhaarFormatValidator")
                .fieldReference("client.aadhaar")
                .message("Aadhaar must be 12 digits")
                .itdErrorCode("AADHAAR-001")
                .build();
        }
        
        if (!verhoeffCheck(cleaned)) {
            return ValidationResult.builder()
                .passed(false)
                .severity(severity())
                .validatorName("AadhaarFormatValidator")
                .fieldReference("client.aadhaar")
                .message("Invalid Aadhaar checksum")
                .itdErrorCode("AADHAAR-002")
                .build();
        }
        
        return ValidationResult.builder()
            .passed(true)
            .severity(severity())
            .validatorName("AadhaarFormatValidator")
            .fieldReference("client.aadhaar")
            .message("Aadhaar format valid")
            .build();
    }
    
    private boolean verhoeffCheck(String number) {
        int c = 0;
        int[] digits = number.chars().map(ch -> ch - '0').toArray();
        
        for (int i = digits.length - 1; i >= 0; i--) {
            c = MULTIPLICATION_TABLE[c][PERMUTATION_TABLE[(digits.length - i) % 8][digits[i]]];
        }
        
        return c == 0;
    }
}
