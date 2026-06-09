package com.itr.service;

import com.itr.model.ITRFormType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * PAN Type Detection and Entity Classification Service.
 * Extracts entity type from PAN 4th character and determines eligible ITR forms.
 * 
 * PAN Format: AAAAA9999A
 * 4th Character Entity Types:
 * P = Individual, H = HUF, F = Firm, C = Company, A = AOP, T = Trust,
 * B = BOI, L = Local Authority, J = Juridical Person, G = Government
 */
@Service
@Slf4j
public class PANTypeDetectionService {
    
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    
    // Entity type constants
    public static final char ENTITY_INDIVIDUAL = 'P';
    public static final char ENTITY_HUF = 'H';
    public static final char ENTITY_FIRM = 'F';
    public static final char ENTITY_COMPANY = 'C';
    public static final char ENTITY_AOP = 'A';
    public static final char ENTITY_TRUST = 'T';
    public static final char ENTITY_BOI = 'B';
    public static final char ENTITY_LOCAL_AUTHORITY = 'L';
    public static final char ENTITY_JURIDICAL_PERSON = 'J';
    public static final char ENTITY_GOVERNMENT = 'G';
    
    /**
     * Validate PAN format (AAAAA9999A).
     */
    public boolean isValidPAN(String pan) {
        if (pan == null || pan.length() != 10) {
            return false;
        }
        return PAN_PATTERN.matcher(pan.trim().toUpperCase()).matches();
    }
    
    /**
     * Extract entity type from PAN (4th character, 0-indexed position 3).
     */
    public char getEntityTypeFromPAN(String pan) {
        if (!isValidPAN(pan)) {
            throw new IllegalArgumentException("Invalid PAN format: " + pan + ". Expected format: AAAAA9999A");
        }
        return pan.trim().toUpperCase().charAt(3);
    }
    
    /**
     * Get human-readable entity type description.
     */
    public String getEntityTypeDescription(char entityType) {
        switch (entityType) {
            case ENTITY_INDIVIDUAL: return "Individual";
            case ENTITY_HUF: return "Hindu Undivided Family (HUF)";
            case ENTITY_FIRM: return "Partnership Firm";
            case ENTITY_COMPANY: return "Company";
            case ENTITY_AOP: return "Association of Persons (AOP)";
            case ENTITY_TRUST: return "Trust";
            case ENTITY_BOI: return "Body of Individuals (BOI)";
            case ENTITY_LOCAL_AUTHORITY: return "Local Authority";
            case ENTITY_JURIDICAL_PERSON: return "Artificial Juridical Person";
            case ENTITY_GOVERNMENT: return "Government";
            default: return "Unknown Entity Type";
        }
    }
    
    /**
     * Check if entity is eligible for Individual ITR forms (ITR 1-4).
     * Only Individual (P) and HUF (H) can file ITR 1-4.
     */
    public boolean isEligibleForIndividualITRForms(char entityType) {
        return entityType == ENTITY_INDIVIDUAL || entityType == ENTITY_HUF;
    }
    
    /**
     * Get all eligible ITR forms based on PAN entity type.
     */
    public List<ITRFormType> getEligibleITRFormsByPAN(String pan) {
        if (!isValidPAN(pan)) {
            throw new IllegalArgumentException("Invalid PAN format: " + pan);
        }
        
        char entityType = getEntityTypeFromPAN(pan);
        List<ITRFormType> eligibleForms = new ArrayList<>();
        
        switch (entityType) {
            case ENTITY_INDIVIDUAL:
            case ENTITY_HUF:
                // Individual and HUF can file ITR 1-4
                eligibleForms.add(ITRFormType.ITR1);
                eligibleForms.add(ITRFormType.ITR2);
                eligibleForms.add(ITRFormType.ITR3);
                eligibleForms.add(ITRFormType.ITR4);
                break;
                
            case ENTITY_FIRM:
            case ENTITY_AOP:
            case ENTITY_BOI:
            case ENTITY_TRUST:
            case ENTITY_LOCAL_AUTHORITY:
            case ENTITY_JURIDICAL_PERSON:
                // These entities file ITR-5
                eligibleForms.add(ITRFormType.ITR5);
                break;
                
            case ENTITY_COMPANY:
                // Companies file ITR-6
                eligibleForms.add(ITRFormType.ITR6);
                break;
                
            case ENTITY_GOVERNMENT:
                // Government entities file ITR-7
                eligibleForms.add(ITRFormType.ITR7);
                break;
                
            default:
                log.warn("Unknown entity type '{}' for PAN: {}", entityType, pan);
                break;
        }
        
        return eligibleForms;
    }
    
    /**
     * Get detailed PAN analysis with entity info and eligible forms.
     */
    public PANAnalysis analyzePAN(String pan) {
        if (!isValidPAN(pan)) {
            return PANAnalysis.builder()
                    .pan(pan)
                    .valid(false)
                    .errorMessage("Invalid PAN format. Expected: AAAAA9999A")
                    .build();
        }
        
        char entityType = getEntityTypeFromPAN(pan);
        String entityDescription = getEntityTypeDescription(entityType);
        List<ITRFormType> eligibleForms = getEligibleITRFormsByPAN(pan);
        boolean isIndividualOrHUF = isEligibleForIndividualITRForms(entityType);
        
        return PANAnalysis.builder()
                .pan(pan.trim().toUpperCase())
                .valid(true)
                .entityType(entityType)
                .entityDescription(entityDescription)
                .eligibleITRForms(eligibleForms)
                .isIndividualOrHUF(isIndividualOrHUF)
                .build();
    }
    
    /**
     * PAN Analysis Result DTO.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PANAnalysis {
        private String pan;
        private boolean valid;
        private String errorMessage;
        private Character entityType;
        private String entityDescription;
        private List<ITRFormType> eligibleITRForms;
        private boolean isIndividualOrHUF;
    }
}
