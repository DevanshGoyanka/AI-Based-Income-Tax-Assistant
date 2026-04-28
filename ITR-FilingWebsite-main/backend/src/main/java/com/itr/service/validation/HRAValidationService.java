package com.itr.service.validation;

import com.itr.dto.Itr1FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * HRA Exemption Validation Service - CBDT Compliant
 * Validates HRA exemption calculation and landlord PAN requirements
 */
@Service
@Slf4j
public class HRAValidationService {

    private static final double LANDLORD_PAN_THRESHOLD = 100000; // ₹1,00,000 per year

    /**
     * Validates HRA exemption details
     */
    public ValidationResult validateHRAExemption(Itr1FormData.ExemptAllowanceDetail hraDetail, 
                                                  Itr1FormData.SalaryIncome salaryIncome) {
        ValidationResult result = new ValidationResult();
        
        if (!"HRA".equals(hraDetail.getAllowanceType())) {
            return result; // Not HRA, skip validation
        }
        
        // Check landlord PAN requirement
        if (hraDetail.getRentPaid() > LANDLORD_PAN_THRESHOLD) {
            if (hraDetail.getLandlordPAN() == null || hraDetail.getLandlordPAN().trim().isEmpty()) {
                result.addError("Landlord PAN is MANDATORY when annual rent exceeds ₹1,00,000. " +
                        "Rent paid: ₹" + hraDetail.getRentPaid());
            }
        }
        
        // Validate HRA exemption calculation
        if (hraDetail.getExemptAmount() > 0) {
            double basicSalary = salaryIncome.getBasicSalary() + salaryIncome.getDaAmount();
            double actualHRA = hraDetail.getAmountReceived();
            double rentPaid = hraDetail.getRentPaid();
            double rentMinus10Percent = rentPaid - (basicSalary * 0.10);
            
            // Metro city: 50% of basic, Non-metro: 40% of basic
            double percentageOfBasic = hraDetail.isMetroCity() ? 
                    (basicSalary * 0.50) : (basicSalary * 0.40);
            
            // HRA exemption = Minimum of:
            // 1. Actual HRA received
            // 2. Rent paid - 10% of salary
            // 3. 50% of salary (metro) or 40% of salary (non-metro)
            double calculatedExemption = Math.min(actualHRA, 
                    Math.min(rentMinus10Percent, percentageOfBasic));
            
            // Allow 1 rupee tolerance for rounding
            if (Math.abs(hraDetail.getExemptAmount() - calculatedExemption) > 1) {
                result.addWarning("HRA exemption calculation mismatch. " +
                        "Calculated: ₹" + calculatedExemption + 
                        ", Claimed: ₹" + hraDetail.getExemptAmount());
            }
        }
        
        // Validate landlord details
        if (hraDetail.getLandlordName() == null || hraDetail.getLandlordName().trim().isEmpty()) {
            result.addWarning("Landlord name is recommended for HRA claim");
        }
        
        if (hraDetail.getLandlordAddress() == null || hraDetail.getLandlordAddress().trim().isEmpty()) {
            result.addWarning("Landlord address is recommended for HRA claim");
        }
        
        return result;
    }

    public static class ValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> warnings = new java.util.ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
            log.error("HRA Validation Error: {}", error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
            log.warn("HRA Validation Warning: {}", warning);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public java.util.List<String> getErrors() {
            return errors;
        }
        
        public java.util.List<String> getWarnings() {
            return warnings;
        }
    }
}
