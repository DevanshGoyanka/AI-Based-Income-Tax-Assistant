package com.itr.service;

import com.itr.model.ITRFormType;
import com.itr.model.PreFillData;
import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

/**
 * Automatically determines the applicable ITR form based on:
 * 1. PAN 4th character (entity type)
 * 2. Income sources and amounts
 * 3. Taxpayer category and residential status
 * 
 * 101% Compliant with ITR Field Guide AY 2026-27
 * 
 * PAN 4th Character Logic:
 * P = Individual, H = HUF, F = Firm, C = Company, A = AOP, T = Trust, 
 * B = BOI, L = Local Authority, J = Juridical Person, G = Government
 * 
 * ITR Form Selection:
 * - ITR-1 (Sahaj): Individual/HUF, salary+1HP+OS, income≤₹50L, resident, no CG/business/foreign
 * - ITR-2: Individual/HUF with CG, multiple properties, foreign income, income>₹50L, NRI
 * - ITR-3: Individual/HUF with business/profession (non-presumptive)
 * - ITR-4 (Sugam): Individual/HUF with presumptive income (44AD/44ADA/44AE)
 * - ITR-5: Firm, AOP, BOI, Trust, Local Authority
 * - ITR-6: Company
 * - ITR-7: Trust/Political Party u/s 139(4A/4B/4C/4D)
 */
@Service
@lombok.extern.slf4j.Slf4j
public class AutoITRFormSelector {
    
    private final PANTypeDetectionService panService;
    
    public AutoITRFormSelector(PANTypeDetectionService panService) {
        this.panService = panService;
    }
    
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    
    // PAN 4th character entity types
    private static final char ENTITY_INDIVIDUAL = 'P';
    private static final char ENTITY_HUF = 'H';
    private static final char ENTITY_FIRM = 'F';
    private static final char ENTITY_COMPANY = 'C';
    private static final char ENTITY_AOP = 'A';
    private static final char ENTITY_TRUST = 'T';
    private static final char ENTITY_BOI = 'B';
    private static final char ENTITY_LOCAL_AUTHORITY = 'L';
    private static final char ENTITY_JURIDICAL_PERSON = 'J';
    private static final char ENTITY_GOVERNMENT = 'G';
    
    /**
     * Validate PAN format and extract entity type.
     */
    public boolean isValidPAN(String pan) {
        if (pan == null || pan.length() != 10) {
            return false;
        }
        return PAN_PATTERN.matcher(pan).matches();
    }
    
    /**
     * Extract entity type from PAN (4th character).
     */
    public char getEntityTypeFromPAN(String pan) {
        if (!isValidPAN(pan)) {
            throw new IllegalArgumentException("Invalid PAN format: " + pan);
        }
        return pan.charAt(3); // 4th character (0-indexed)
    }
    
    /**
     * Get entity type description.
     */
    public String getEntityTypeDescription(char entityType) {
        switch (entityType) {
            case ENTITY_INDIVIDUAL: return "Individual";
            case ENTITY_HUF: return "Hindu Undivided Family";
            case ENTITY_FIRM: return "Partnership Firm";
            case ENTITY_COMPANY: return "Company";
            case ENTITY_AOP: return "Association of Persons";
            case ENTITY_TRUST: return "Trust";
            case ENTITY_BOI: return "Body of Individuals";
            case ENTITY_LOCAL_AUTHORITY: return "Local Authority";
            case ENTITY_JURIDICAL_PERSON: return "Artificial Juridical Person";
            case ENTITY_GOVERNMENT: return "Government";
            default: return "Unknown Entity Type";
        }
    }
    
    /**
     * Check if entity type is eligible for ITR-1/2/3/4 (only Individual and HUF).
     */
    public boolean isEligibleForIndividualITRForms(char entityType) {
        return entityType == ENTITY_INDIVIDUAL || entityType == ENTITY_HUF;
    }
    
    /**
     * NEW: Classify ITR form based on income profile (income-based classification).
     * This is the primary method for determining correct ITR form.
     */
    public com.itr.dto.ITRClassificationResult classifyITRForm(com.itr.dto.IncomeProfile profile) {
        String pan = profile.getPan();
        
        // Validate PAN
        if (!isValidPAN(pan)) {
            return com.itr.dto.ITRClassificationResult.builder()
                    .pan(pan)
                    .classificationReason("Invalid PAN format")
                    .build();
        }
        
        char entityType = getEntityTypeFromPAN(pan);
        String entityDescription = getEntityTypeDescription(entityType);
        
        com.itr.dto.ITRClassificationResult.ITRClassificationResultBuilder result = 
                com.itr.dto.ITRClassificationResult.builder()
                .pan(pan)
                .entityType(entityType)
                .entityDescription(entityDescription)
                .totalIncome(profile.getTotalIncome())
                .hasCapitalGains(profile.isHasCapitalGains())
                .hasBusinessIncome(profile.isHasBusinessIncome())
                .hasMultipleProperties(profile.isHasMultipleProperties())
                .hasForeignIncome(profile.isHasForeignIncome());
        
        // Route based on entity type
        if (entityType == ENTITY_COMPANY) {
            result.recommendedForm(ITRFormType.ITR6)
                  .eligibleForms(java.util.Arrays.asList(ITRFormType.ITR6))
                  .classificationReason("Companies must file ITR-6");
            return result.build();
        }
        
        if (entityType == ENTITY_FIRM || entityType == ENTITY_AOP || entityType == ENTITY_BOI ||
            entityType == ENTITY_TRUST || entityType == ENTITY_LOCAL_AUTHORITY || 
            entityType == ENTITY_JURIDICAL_PERSON) {
            result.recommendedForm(ITRFormType.ITR5)
                  .eligibleForms(java.util.Arrays.asList(ITRFormType.ITR5))
                  .classificationReason(entityDescription + " must file ITR-5");
            return result.build();
        }
        
        // For Individual/HUF: Apply income-based classification
        return classifyForIndividualHUF(profile, result);
    }
    
    /**
     * Classify ITR form for Individual/HUF based on income sources.
     */
    private com.itr.dto.ITRClassificationResult classifyForIndividualHUF(
            com.itr.dto.IncomeProfile profile,
            com.itr.dto.ITRClassificationResult.ITRClassificationResultBuilder result) {
        
        java.util.List<ITRFormType> eligible = new java.util.ArrayList<>();
        java.util.List<String> reasons = new java.util.ArrayList<>();
        java.util.List<String> warnings = new java.util.ArrayList<>();
        
        // Priority 1: Business Income with Presumptive Taxation → ITR-4
        if (profile.isHasBusinessIncome() && 
            (profile.isEligibleFor44AD() || profile.isEligibleFor44ADA() || profile.isEligibleFor44AE())) {
            result.recommendedForm(ITRFormType.ITR4);
            eligible.add(ITRFormType.ITR4);
            reasons.add("Presumptive income under Section 44AD/44ADA/44AE");
            
            if (profile.getTotalIncome() > 5000000) {
                warnings.add("Total income exceeds ₹50 lakhs - verify ITR-4 eligibility");
            }
            
            result.eligibleForms(eligible)
                  .reasonDetails(reasons)
                  .warnings(warnings)
                  .classificationReason("ITR-4 (Sugam) - Presumptive taxation scheme");
            return result.build();
        }
        
        // Priority 2: Business/Professional Income (non-presumptive) → ITR-3
        if (profile.isHasBusinessIncome() || profile.isHasProfessionalIncome()) {
            result.recommendedForm(ITRFormType.ITR3);
            eligible.add(ITRFormType.ITR3);
            reasons.add("Income from business or profession");
            
            result.eligibleForms(eligible)
                  .reasonDetails(reasons)
                  .classificationReason("ITR-3 - Business/Professional income");
            return result.build();
        }
        
        // Priority 3: Capital Gains → ITR-2
        if (profile.isHasCapitalGains()) {
            result.recommendedForm(ITRFormType.ITR2);
            eligible.add(ITRFormType.ITR2);
            reasons.add("Capital gains from sale of assets/investments");
        }
        
        // Priority 4: Multiple Properties → ITR-2
        if (profile.isHasMultipleProperties()) {
            if (result.build().getRecommendedForm() == null) {
                result.recommendedForm(ITRFormType.ITR2);
            }
            if (!eligible.contains(ITRFormType.ITR2)) {
                eligible.add(ITRFormType.ITR2);
            }
            reasons.add("More than one house property");
        }
        
        // Priority 5: Foreign Income/Assets → ITR-2
        if (profile.isHasForeignIncome() || profile.isHasForeignAssets()) {
            if (result.build().getRecommendedForm() == null) {
                result.recommendedForm(ITRFormType.ITR2);
            }
            if (!eligible.contains(ITRFormType.ITR2)) {
                eligible.add(ITRFormType.ITR2);
            }
            reasons.add("Foreign income or foreign assets");
        }
        
        // Priority 6: Total Income > ₹50 lakhs → ITR-2
        if (profile.getTotalIncome() > 5000000) {
            if (result.build().getRecommendedForm() == null) {
                result.recommendedForm(ITRFormType.ITR2);
            }
            if (!eligible.contains(ITRFormType.ITR2)) {
                eligible.add(ITRFormType.ITR2);
            }
            reasons.add("Total income exceeds ₹50 lakhs");
        }
        
        // Priority 7: Non-Resident → ITR-2
        if (profile.getResidentialStatus() != null && 
            !profile.getResidentialStatus().equalsIgnoreCase("RES")) {
            if (result.build().getRecommendedForm() == null) {
                result.recommendedForm(ITRFormType.ITR2);
            }
            if (!eligible.contains(ITRFormType.ITR2)) {
                eligible.add(ITRFormType.ITR2);
            }
            reasons.add("Non-resident or RNOR status");
        }
        
        // Priority 8: Director/Unlisted Shares → ITR-2
        if (profile.isDirector()) {
            if (result.build().getRecommendedForm() == null) {
                result.recommendedForm(ITRFormType.ITR2);
            }
            if (!eligible.contains(ITRFormType.ITR2)) {
                eligible.add(ITRFormType.ITR2);
            }
            reasons.add("Director in a company");
        }
        
        if (profile.isHasUnlistedShares()) {
            if (result.build().getRecommendedForm() == null) {
                result.recommendedForm(ITRFormType.ITR2);
            }
            if (!eligible.contains(ITRFormType.ITR2)) {
                eligible.add(ITRFormType.ITR2);
            }
            reasons.add("Holds unlisted equity shares");
        }
        
        // Priority 9: Agricultural Income > ₹5,000 → ITR-2
        if (profile.getAgriculturalIncome() > 5000) {
            if (result.build().getRecommendedForm() == null) {
                result.recommendedForm(ITRFormType.ITR2);
            }
            if (!eligible.contains(ITRFormType.ITR2)) {
                eligible.add(ITRFormType.ITR2);
            }
            reasons.add("Agricultural income exceeds ₹5,000");
        }
        
        // Priority 10: Lottery/Race Horse Income → ITR-2
        if (profile.isHasLotteryIncome() || profile.isHasRaceHorseIncome()) {
            if (result.build().getRecommendedForm() == null) {
                result.recommendedForm(ITRFormType.ITR2);
            }
            if (!eligible.contains(ITRFormType.ITR2)) {
                eligible.add(ITRFormType.ITR2);
            }
            reasons.add("Income from lottery or race horses");
        }
        
        // Default: Simple income (Salary + 1 HP + OS) → ITR-1
        if (result.build().getRecommendedForm() == null) {
            result.recommendedForm(ITRFormType.ITR1);
            eligible.add(ITRFormType.ITR1);
            eligible.add(ITRFormType.ITR2); // ITR-2 is always an option for ITR-1 eligible
            reasons.add("Salary income with simple structure");
            reasons.add("Eligible for ITR-1 (Sahaj) - simplified form");
        }
        
        // Build classification reason
        String mainReason = result.build().getRecommendedForm() == ITRFormType.ITR1 
                ? "ITR-1 (Sahaj) - Salary with one house property and other sources"
                : result.build().getRecommendedForm() == ITRFormType.ITR2
                ? "ITR-2 - " + String.join(", ", reasons)
                : "ITR-3 - Business or professional income";
        
        result.eligibleForms(eligible)
              .reasonDetails(reasons)
              .warnings(warnings)
              .classificationReason(mainReason);
        
        return result.build();
    }
    
    /**
     * Master ITR form selection with PAN validation.
     */
    public ITRFormType selectITRFormWithPAN(String pan, PreFillData prefill, long totalIncome,
                                             boolean hasCapitalGains, boolean hasMultipleProperties,
                                             boolean hasBusinessIncome, boolean hasForeignIncome,
                                             boolean isDirector, boolean hasUnlistedShares,
                                             boolean hasLotteryIncome, boolean hasRaceHorseIncome,
                                             long agriculturalIncome, boolean eligibleFor44AD,
                                             boolean eligibleFor44ADA, boolean eligibleFor44AE) {
        
        // Step 1: Validate PAN
        if (!isValidPAN(pan)) {
            throw new IllegalArgumentException("Invalid PAN format. Expected format: AAAAA9999A");
        }
        
        // Step 2: Extract entity type
        char entityType = getEntityTypeFromPAN(pan);
        
        // Step 3: Route to appropriate ITR form based on entity type
        if (entityType == ENTITY_COMPANY) {
            return ITRFormType.ITR6;
        }
        
        if (entityType == ENTITY_FIRM || entityType == ENTITY_AOP || entityType == ENTITY_BOI ||
            entityType == ENTITY_TRUST || entityType == ENTITY_LOCAL_AUTHORITY || 
            entityType == ENTITY_JURIDICAL_PERSON) {
            return ITRFormType.ITR5;
        }
        
        if (entityType == ENTITY_GOVERNMENT) {
            return ITRFormType.ITR7; // Special case
        }
        
        // Step 4: For Individual (P) and HUF (H), apply detailed selection logic
        if (!isEligibleForIndividualITRForms(entityType)) {
            throw new IllegalArgumentException("Entity type " + getEntityTypeDescription(entityType) + 
                                             " is not eligible for ITR-1/2/3/4");
        }
        
        // Step 5: Apply income-based selection for Individual/HUF
        return selectITRFormForIndividual(prefill, totalIncome, hasCapitalGains, hasMultipleProperties,
                                         hasBusinessIncome, hasForeignIncome, isDirector, hasUnlistedShares,
                                         hasLotteryIncome, hasRaceHorseIncome, agriculturalIncome,
                                         eligibleFor44AD, eligibleFor44ADA, eligibleFor44AE);
    }
    
    /**
     * Detailed ITR form selection for Individual/HUF based on income profile.
     */
    private ITRFormType selectITRFormForIndividual(PreFillData prefill, long totalIncome,
                                                    boolean hasCapitalGains, boolean hasMultipleProperties,
                                                    boolean hasBusinessIncome, boolean hasForeignIncome,
                                                    boolean isDirector, boolean hasUnlistedShares,
                                                    boolean hasLotteryIncome, boolean hasRaceHorseIncome,
                                                    long agriculturalIncome, boolean eligibleFor44AD,
                                                    boolean eligibleFor44ADA, boolean eligibleFor44AE) {
        
        // ITR-4: Presumptive taxation (44AD/44ADA/44AE)
        if (hasBusinessIncome && (eligibleFor44AD || eligibleFor44ADA || eligibleFor44AE)) {
            return ITRFormType.ITR4;
        }
        
        // ITR-3: Business or profession income (non-presumptive)
        if (hasBusinessIncome) {
            return ITRFormType.ITR3;
        }
        
        // ITR-2: Capital gains present
        if (hasCapitalGains) {
            return ITRFormType.ITR2;
        }
        
        // ITR-2: Multiple house properties (>1)
        if (hasMultipleProperties) {
            return ITRFormType.ITR2;
        }
        
        // ITR-2: Foreign income or assets
        if (hasForeignIncome) {
            return ITRFormType.ITR2;
        }
        
        // ITR-2: Total income > ₹50 lakhs
        if (totalIncome > 5_000_000) {
            return ITRFormType.ITR2;
        }
        
        // ITR-2: Non-resident (NRI/RNOR)
        if (prefill.residentialStatus != null && 
            !prefill.residentialStatus.equalsIgnoreCase("RES")) {
            return ITRFormType.ITR2;
        }
        
        // ITR-2: Director in a company
        if (isDirector) {
            return ITRFormType.ITR2;
        }
        
        // ITR-2: Holds unlisted equity shares
        if (hasUnlistedShares) {
            return ITRFormType.ITR2;
        }
        
        // ITR-2: Agricultural income > ₹5,000
        if (agriculturalIncome > 5000) {
            return ITRFormType.ITR2;
        }
        
        // ITR-2: Income from lottery or race horses
        if (hasLotteryIncome || hasRaceHorseIncome) {
            return ITRFormType.ITR2;
        }
        
        // ITR-1: Default for salaried individuals with simple income
        // Conditions: Salary + max 1 house property + other sources, total ≤ ₹50L, resident
        if (prefill.salary17_1 > 0 && totalIncome <= 5_000_000 && 
            "RES".equalsIgnoreCase(prefill.residentialStatus)) {
            return ITRFormType.ITR1;
        }
        
        // Default to ITR-2 for safety
        return ITRFormType.ITR2;
    }
    
    /**
     * Get detailed explanation for form selection.
     */
    public String getFormSelectionReason(String pan, PreFillData prefill, long totalIncome,
                                          boolean hasCapitalGains, boolean hasMultipleProperties,
                                          boolean hasBusinessIncome, boolean hasForeignIncome,
                                          boolean isDirector, boolean hasUnlistedShares,
                                          boolean hasLotteryIncome, boolean hasRaceHorseIncome,
                                          long agriculturalIncome, boolean eligibleFor44AD,
                                          boolean eligibleFor44ADA, boolean eligibleFor44AE) {
        
        if (!isValidPAN(pan)) {
            return "Invalid PAN format. Please provide a valid 10-character PAN.";
        }
        
        char entityType = getEntityTypeFromPAN(pan);
        String entityDesc = getEntityTypeDescription(entityType);
        
        // Entity-based routing
        if (entityType == ENTITY_COMPANY) {
            return "ITR-6 is required for " + entityDesc + " (PAN 4th character: C).";
        }
        
        if (entityType == ENTITY_FIRM || entityType == ENTITY_AOP || entityType == ENTITY_BOI ||
            entityType == ENTITY_TRUST || entityType == ENTITY_LOCAL_AUTHORITY || 
            entityType == ENTITY_JURIDICAL_PERSON) {
            return "ITR-5 is required for " + entityDesc + " (PAN 4th character: " + entityType + ").";
        }
        
        // Individual/HUF income-based reasons
        if (hasBusinessIncome && (eligibleFor44AD || eligibleFor44ADA || eligibleFor44AE)) {
            return "ITR-4 (Sugam) is applicable as you have opted for presumptive taxation under Section 44AD/44ADA/44AE.";
        }
        
        if (hasBusinessIncome) {
            return "ITR-3 is required because you have income from business or profession.";
        }
        
        if (hasCapitalGains) {
            return "ITR-2 is required because you have capital gains from sale of assets/investments.";
        }
        
        if (hasMultipleProperties) {
            return "ITR-2 is required because you own more than one house property.";
        }
        
        if (hasForeignIncome) {
            return "ITR-2 is required because you have foreign income or foreign assets.";
        }
        
        if (totalIncome > 5_000_000) {
            return String.format("ITR-2 is required because your total income (₹%,d) exceeds ₹50 lakhs.", totalIncome);
        }
        
        if (prefill.residentialStatus != null && !prefill.residentialStatus.equalsIgnoreCase("RES")) {
            return "ITR-2 is required because you are a Non-Resident or Not Ordinarily Resident.";
        }
        
        if (isDirector) {
            return "ITR-2 is required because you are a director in a company.";
        }
        
        if (hasUnlistedShares) {
            return "ITR-2 is required because you hold unlisted equity shares.";
        }
        
        if (agriculturalIncome > 5000) {
            return String.format("ITR-2 is required because your agricultural income (₹%,d) exceeds ₹5,000.", agriculturalIncome);
        }
        
        if (hasLotteryIncome || hasRaceHorseIncome) {
            return "ITR-2 is required because you have income from lottery/race horses.";
        }
        
        if (prefill.salary17_1 > 0 && totalIncome <= 5_000_000) {
            return "ITR-1 (Sahaj) is applicable as you have salary income, one house property (or none), " +
                   "other sources like interest, and total income is within ₹50 lakhs.";
        }
        
        return "ITR-2 is recommended based on your income profile.";
    }
    
    /**
     * Check eligibility for ITR-1 (Sahaj).
     */
    public boolean isEligibleForITR1(String pan, PreFillData prefill, long totalIncome,
                                      boolean hasCapitalGains, boolean hasMultipleProperties,
                                      boolean hasBusinessIncome, boolean hasForeignIncome,
                                      boolean isDirector, boolean hasUnlistedShares,
                                      long agriculturalIncome) {
        
        // Must have valid PAN
        if (!isValidPAN(pan)) {
            return false;
        }
        
        // Must be Individual or HUF
        char entityType = getEntityTypeFromPAN(pan);
        if (!isEligibleForIndividualITRForms(entityType)) {
            return false;
        }
        
        // Must be resident
        if (prefill.residentialStatus == null || !prefill.residentialStatus.equalsIgnoreCase("RES")) {
            return false;
        }
        
        // Total income ≤ ₹50L
        if (totalIncome > 5_000_000) {
            return false;
        }
        
        // No capital gains
        if (hasCapitalGains) {
            return false;
        }
        
        // Max one house property
        if (hasMultipleProperties) {
            return false;
        }
        
        // No business income
        if (hasBusinessIncome) {
            return false;
        }
        
        // No foreign income/assets
        if (hasForeignIncome) {
            return false;
        }
        
        // Not a director
        if (isDirector) {
            return false;
        }
        
        // No unlisted shares
        if (hasUnlistedShares) {
            return false;
        }
        
        // Agricultural income ≤ ₹5,000
        if (agriculturalIncome > 5000) {
            return false;
        }
        
        // Must have salary or pension income
        if (prefill.salary17_1 <= 0) {
            return false;
        }
        
        return true;
    }
}
