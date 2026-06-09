package com.itr.service.validation;

import com.itr.dto.Itr1FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ITR-1 Eligibility Checker - CBDT Compliant
 * Validates if taxpayer is eligible to file ITR-1 (Sahaj)
 */
@Service
@Slf4j
public class ITR1EligibilityService {

    private static final double MAX_TOTAL_INCOME = 5000000; // ₹50 lakh
    private static final double MAX_AGRICULTURAL_INCOME = 5000; // ₹5,000

    /**
     * Check ITR-1 eligibility and return detailed result
     */
    public EligibilityResult checkEligibility(Itr1FormData formData) {
        EligibilityResult result = new EligibilityResult();
        
        // 1. Residential Status Check
        if (formData.getPersonalInfo() != null) {
            String residentialStatus = formData.getPersonalInfo().getResidentialStatus();
            if (!"ROR".equals(residentialStatus)) {
                result.addError("ITR-1 is only for Resident and Ordinarily Resident (ROR). " +
                        "Your status: " + residentialStatus + ". Use ITR-2.");
            }
        }
        
        // 2. Total Income Check
        if (formData.getTaxComputation() != null) {
            double totalIncome = formData.getTaxComputation().getGrossTotalIncome();
            if (totalIncome > MAX_TOTAL_INCOME) {
                result.addError("Total income exceeds ₹50,00,000 (₹" + totalIncome + "). Use ITR-2.");
            }
        }
        
        // 3. Agricultural Income Check
        if (formData.getPersonalInfo() != null) {
            double agriIncome = formData.getPersonalInfo().getAgriculturalIncome();
            if (agriIncome > MAX_AGRICULTURAL_INCOME) {
                result.addError("Agricultural income exceeds ₹5,000 (₹" + agriIncome + "). Use ITR-2.");
            }
        }
        
        // 4. Director Check
        if (formData.getPersonalInfo() != null && formData.getPersonalInfo().isDirector()) {
            result.addError("You are a director in a company. Directors must file ITR-2 or higher.");
        }
        
        // 5. Unlisted Shares Check
        if (formData.getPersonalInfo() != null && formData.getPersonalInfo().isHoldsUnlistedShares()) {
            result.addError("You hold unlisted equity shares. Must file ITR-2 or higher.");
        }
        
        // 6. Capital Gains Check - ITR-1 does not allow capital gains
        // This is enforced at form level, no field exists in ITR-1
        
        // 7. Business Income Check - ITR-1 does not allow business income
        // This is enforced at form level, no field exists in ITR-1
        
        // 8. Foreign Assets Check
        if (formData.getPersonalInfo() != null) {
            if (formData.getPersonalInfo().isBankAccountsOutsideIndia() ||
                formData.getPersonalInfo().isForeignAssets() ||
                formData.getPersonalInfo().isSigningAuthorityInForeignAccount()) {
                result.addError("You have foreign assets/accounts. Must file ITR-2 with Schedule FA.");
            }
        }
        
        // 9. Multiple House Properties Check
        if (formData.getHousePropertyIncome() != null) {
            // ITR-1 allows only ONE house property
            // Check if there are multiple properties (future enhancement)
            result.addWarning("ITR-1 allows only ONE house property. " +
                    "If you have multiple properties, use ITR-2.");
        }
        
        // 10. Brought Forward Loss Check - ITR-1 does not allow BF losses
        // This is enforced at form level
        
        return result;
    }

    /**
     * Get recommended ITR form based on income sources
     */
    public String getRecommendedITRForm(Itr1FormData formData) {
        EligibilityResult result = checkEligibility(formData);
        
        if (result.isEligible()) {
            return "ITR-1";
        }
        
        // Determine which ITR form to use
        if (formData.getPersonalInfo() != null && formData.getPersonalInfo().isDirector()) {
            return "ITR-2"; // Director
        }
        
        if (formData.getPersonalInfo() != null && 
            (formData.getPersonalInfo().isForeignAssets() || 
             formData.getPersonalInfo().isBankAccountsOutsideIndia())) {
            return "ITR-2"; // Foreign Assets
        }
        
        return "ITR-2"; // Default fallback
    }

    public static class EligibilityResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
            log.error("ITR-1 Eligibility Error: {}", error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
            log.warn("ITR-1 Eligibility Warning: {}", warning);
        }
        
        public boolean isEligible() {
            return errors.isEmpty();
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
        
        public int getErrorCount() {
            return errors.size();
        }
        
        public int getWarningCount() {
            return warnings.size();
        }
    }
}
