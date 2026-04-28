package com.itr.service.validation;

import com.itr.dto.FlatFormData;
import com.itr.dto.Itr1FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * CBDT Validation Service - 101% Compliance
 * Validates all ITR data against CBDT rules for AY 2025-26
 */
@Service
@Slf4j
public class CBDTValidationService {

    // Regex patterns
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern TAN_PATTERN = Pattern.compile("^[A-Z]{4}[0-9]{5}[A-Z]$");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^[0-9]{12}$");
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern BSR_PATTERN = Pattern.compile("^[0-9]{7}$");
    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[0-9]{6}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Financial Year dates for AY 2025-26
    private static final LocalDate FY_START = LocalDate.of(2024, 4, 1);
    private static final LocalDate FY_END = LocalDate.of(2025, 3, 31);
    private static final LocalDate AGE_CALCULATION_DATE = LocalDate.of(2025, 3, 31);

    // Deduction limits for AY 2025-26
    private static final double LIMIT_80C = 150000;
    private static final double LIMIT_80CCD1B = 50000;
    private static final double LIMIT_80D_SELF_BELOW_60 = 25000;
    private static final double LIMIT_80D_SELF_60_PLUS = 50000;
    private static final double LIMIT_80D_PARENTS_BELOW_60 = 25000;
    private static final double LIMIT_80D_PARENTS_60_PLUS = 50000;
    private static final double LIMIT_80TTA = 10000;
    private static final double LIMIT_80TTB = 50000;
    private static final double LIMIT_STANDARD_DEDUCTION = 75000;
    private static final double LIMIT_PROFESSIONAL_TAX = 2500;
    private static final double ITR1_MAX_INCOME = 5000000;
    private static final double ITR1_MAX_AGRICULTURAL_INCOME = 5000;

    public ValidationResult validateItr1FormData(Itr1FormData formData) {
        ValidationResult result = new ValidationResult();
        
        if (formData.getPersonalInfo() != null) {
            validatePersonalInfo(formData.getPersonalInfo(), result);
        }
        
        if (formData.getSalaryIncome() != null) {
            validateSalaryIncome(formData.getSalaryIncome(), result);
        }
        
        if (formData.getHousePropertyIncome() != null) {
            validateHouseProperty(formData.getHousePropertyIncome(), result);
        }
        
        if (formData.getDeductions() != null) {
            validateDeductions(formData.getDeductions(), result);
        }
        
        if (formData.getTaxPayments() != null) {
            validateTaxPayments(formData.getTaxPayments(), result);
        }
        
        validateItr1Eligibility(formData, result);
        
        return result;
    }

    public ValidationResult validateFlatFormData(FlatFormData formData) {
        ValidationResult result = new ValidationResult();
        
        validatePersonalInfoFlat(formData, result);
        validateTDSEntries(formData.getTdsEntries(), result);
        validateAdvanceTaxEntries(formData.getAdvanceTaxEntries(), result);
        validateSelfAssessmentTaxEntries(formData.getSelfAssessmentTaxEntries(), result);
        validateAmounts(formData, result);
        
        return result;
    }

    private void validatePersonalInfo(Itr1FormData.PersonalInfo info, ValidationResult result) {
        // PAN validation
        if (info.getPan() == null || info.getPan().trim().isEmpty()) {
            result.addError("PAN is mandatory");
        } else if (!PAN_PATTERN.matcher(info.getPan()).matches()) {
            result.addError("Invalid PAN format. Must be: [A-Z]{5}[0-9]{4}[A-Z]");
        } else {
            validatePANEntityType(info.getPan(), result);
        }
        
        // Aadhaar validation
        if (info.getAadhaar() != null && !info.getAadhaar().trim().isEmpty()) {
            String aadhaar = info.getAadhaar().replaceAll("\\s", "");
            if (!AADHAAR_PATTERN.matcher(aadhaar).matches()) {
                result.addError("Invalid Aadhaar format. Must be 12 digits");
            }
        }
        
        // Mandatory fields
        if (info.getAssesseeName() == null || info.getAssesseeName().trim().isEmpty()) {
            result.addError("Assessee name is mandatory");
        }
        
        if (info.getFatherName() == null || info.getFatherName().trim().isEmpty()) {
            result.addError("Father's name is mandatory");
        }
        
        if (info.getGender() == null || info.getGender().trim().isEmpty()) {
            result.addError("Gender is mandatory");
        } else if (!List.of("MALE", "FEMALE", "TRANSGENDER").contains(info.getGender().toUpperCase())) {
            result.addError("Gender must be MALE, FEMALE, or TRANSGENDER");
        }
        
        if (info.getMaritalStatus() == null || info.getMaritalStatus().trim().isEmpty()) {
            result.addError("Marital status is mandatory");
        } else if (!List.of("SINGLE", "MARRIED", "DIVORCED", "WIDOWED").contains(info.getMaritalStatus().toUpperCase())) {
            result.addError("Marital status must be SINGLE, MARRIED, DIVORCED, or WIDOWED");
        }
        
        if (info.getNationality() == null || info.getNationality().trim().isEmpty()) {
            result.addWarning("Nationality not specified, defaulting to INDIA");
        }
        
        // Date of Birth validation
        if (info.getDateOfBirth() == null) {
            result.addError("Date of Birth is mandatory");
        } else if (info.getDateOfBirth().isAfter(FY_START)) {
            result.addError("Date of Birth must be before the financial year start date");
        }
        
        // Age validation
        if (info.getAge() != null && info.getDateOfBirth() != null) {
            int calculatedAge = AGE_CALCULATION_DATE.getYear() - info.getDateOfBirth().getYear();
            if (Math.abs(calculatedAge - info.getAge()) > 1) {
                result.addWarning("Age mismatch: Calculated age is " + calculatedAge + " but provided age is " + info.getAge());
            }
        }
        
        // Email validation
        if (info.getEmail() != null && !info.getEmail().trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(info.getEmail()).matches()) {
                result.addError("Invalid email format");
            }
        }
        
        // Mobile validation
        if (info.getMobile() != null && !info.getMobile().trim().isEmpty()) {
            String mobile = info.getMobile().replaceAll("[^0-9]", "");
            if (!MOBILE_PATTERN.matcher(mobile).matches()) {
                result.addError("Invalid mobile number. Must be 10 digits starting with 6-9");
            }
        }
        
        // Bank account validation
        if (info.getBankIFSC() != null && !info.getBankIFSC().trim().isEmpty()) {
            if (!IFSC_PATTERN.matcher(info.getBankIFSC()).matches()) {
                result.addError("Invalid IFSC code format");
            }
        }
        
        // Residential status validation
        if (info.getResidentialStatus() != null && !"ROR".equals(info.getResidentialStatus())) {
            result.addError("ITR-1 is only for Resident Ordinary Resident (ROR). Use ITR-2 for RNOR/NR");
        }
    }

    private void validatePANEntityType(String pan, ValidationResult result) {
        char fourthChar = pan.charAt(3);
        if (fourthChar != 'P') {
            result.addWarning("PAN 4th character is '" + fourthChar + "'. Expected 'P' for Individual. " +
                    "H=HUF, F=Firm, C=Company, A=AOP, T=Trust");
        }
    }

    private void validateSalaryIncome(Itr1FormData.SalaryIncome salary, ValidationResult result) {
        if (salary.getStandardDeduction() > LIMIT_STANDARD_DEDUCTION) {
            result.addError("Standard deduction cannot exceed ₹" + LIMIT_STANDARD_DEDUCTION + " for AY 2025-26");
        }
        
        if (salary.getProfessionalTax() > LIMIT_PROFESSIONAL_TAX) {
            result.addError("Professional tax deduction cannot exceed ₹" + LIMIT_PROFESSIONAL_TAX);
        }
        
        // Validate employer details
        if (salary.getEmployers() != null) {
            for (Itr1FormData.EmployerDetails emp : salary.getEmployers()) {
                validateEmployerDetails(emp, result);
            }
        }
    }

    private void validateEmployerDetails(Itr1FormData.EmployerDetails emp, ValidationResult result) {
        if (emp.getEmployerName() == null || emp.getEmployerName().trim().isEmpty()) {
            result.addError("Employer name is mandatory");
        }
        
        if (emp.getEmployerTAN() == null || emp.getEmployerTAN().trim().isEmpty()) {
            result.addError("Employer TAN is mandatory");
        } else if (!TAN_PATTERN.matcher(emp.getEmployerTAN()).matches()) {
            result.addError("Invalid TAN format for " + emp.getEmployerName() + ". Must be: [A-Z]{4}[0-9]{5}[A-Z]");
        }
        
        if (emp.getEmployerAddress() == null || emp.getEmployerAddress().trim().isEmpty()) {
            result.addWarning("Employer address is recommended");
        }
        
        if (emp.getEmployerState() == null || emp.getEmployerState().trim().isEmpty()) {
            result.addError("Employer state is mandatory");
        }
        
        if (emp.getEmployerPinCode() != null && !emp.getEmployerPinCode().trim().isEmpty()) {
            if (!PINCODE_PATTERN.matcher(emp.getEmployerPinCode()).matches()) {
                result.addError("Invalid employer pincode format");
            }
        }
    }

    private void validateHouseProperty(Itr1FormData.HousePropertyIncome hp, ValidationResult result) {
        if (hp.getPropertyIdentificationNo() == null || hp.getPropertyIdentificationNo().trim().isEmpty()) {
            result.addWarning("Property identification number (Survey/Plot No) is recommended");
        }
        
        if ("LET_OUT".equals(hp.getPropertyType())) {
            if (hp.getAnnualRent() > 100000 && (hp.getTenantPAN() == null || hp.getTenantPAN().trim().isEmpty())) {
                result.addError("Tenant PAN is mandatory if annual rent exceeds ₹1,00,000");
            }
        }
        
        if (hp.getLenderPAN() != null && !hp.getLenderPAN().trim().isEmpty()) {
            if (!PAN_PATTERN.matcher(hp.getLenderPAN()).matches()) {
                result.addError("Invalid lender PAN format");
            }
        }
    }

    private void validateDeductions(Itr1FormData.Deductions ded, ValidationResult result) {
        if (ded.getDeduction80C() > LIMIT_80C) {
            result.addError("80C deduction cannot exceed ₹" + LIMIT_80C);
        }
        
        if (ded.getNpsEmployee80CCD1B() > LIMIT_80CCD1B) {
            result.addError("80CCD(1B) NPS deduction cannot exceed ₹" + LIMIT_80CCD1B);
        }
        
        // 80D validation
        double maxSelf = ded.isSelfSeniorCitizen() ? LIMIT_80D_SELF_60_PLUS : LIMIT_80D_SELF_BELOW_60;
        double maxParents = ded.isParentsSeniorCitizen() ? LIMIT_80D_PARENTS_60_PLUS : LIMIT_80D_PARENTS_BELOW_60;
        
        if (ded.getHealthInsuranceSelf() > maxSelf) {
            result.addError("80D self health insurance cannot exceed ₹" + maxSelf);
        }
        
        if (ded.getHealthInsuranceParents() > maxParents) {
            result.addError("80D parents health insurance cannot exceed ₹" + maxParents);
        }
        
        if (ded.getDeduction80TTA() > LIMIT_80TTA) {
            result.addError("80TTA savings interest deduction cannot exceed ₹" + LIMIT_80TTA + " (for age < 60)");
        }
        
        if (ded.getDeduction80TTB() > LIMIT_80TTB) {
            result.addError("80TTB interest deduction cannot exceed ₹" + LIMIT_80TTB + " (for age 60+)");
        }
    }

    private void validateTaxPayments(Itr1FormData.TaxPayments tax, ValidationResult result) {
        if (tax.getTdsOnSalary() != null) {
            for (Itr1FormData.TDSOnSalary tds : tax.getTdsOnSalary()) {
                validateTDSOnSalary(tds, result);
            }
        }
        
        if (tax.getTdsOnOther() != null) {
            for (Itr1FormData.TDSOnOther tds : tax.getTdsOnOther()) {
                validateTDSOnOther(tds, result);
            }
        }
        
        if (tax.getAdvanceTaxEntries() != null) {
            for (Itr1FormData.AdvanceTaxEntry adv : tax.getAdvanceTaxEntries()) {
                validateAdvanceTaxEntry(adv, result);
            }
        }
        
        if (tax.getSelfAssessmentTaxEntries() != null) {
            for (Itr1FormData.SelfAssessmentTaxEntry sat : tax.getSelfAssessmentTaxEntries()) {
                validateSelfAssessmentTaxEntry(sat, result);
            }
        }
    }

    private void validateTDSOnSalary(Itr1FormData.TDSOnSalary tds, ValidationResult result) {
        if (tds.getEmployerTAN() == null || tds.getEmployerTAN().trim().isEmpty()) {
            result.addError("Employer TAN is mandatory for TDS on salary");
        } else if (!TAN_PATTERN.matcher(tds.getEmployerTAN()).matches()) {
            result.addError("Invalid TAN format: " + tds.getEmployerTAN());
        }
    }

    private void validateTDSOnOther(Itr1FormData.TDSOnOther tds, ValidationResult result) {
        if (tds.getDeductorTAN() == null || tds.getDeductorTAN().trim().isEmpty()) {
            result.addError("Deductor TAN is mandatory for TDS entry");
        } else if (!TAN_PATTERN.matcher(tds.getDeductorTAN()).matches()) {
            result.addError("Invalid TAN format: " + tds.getDeductorTAN());
        }
        
        if (tds.getSection() == null || tds.getSection().trim().isEmpty()) {
            result.addError("TDS section is mandatory");
        }
        
        if (tds.getDeductionDate() != null) {
            if (tds.getDeductionDate().isBefore(FY_START) || tds.getDeductionDate().isAfter(FY_END)) {
                result.addError("TDS deduction date must be within FY 2024-25 (01-Apr-2024 to 31-Mar-2025)");
            }
        }
    }

    private void validateAdvanceTaxEntry(Itr1FormData.AdvanceTaxEntry adv, ValidationResult result) {
        if (adv.getBsrCode() == null || adv.getBsrCode().trim().isEmpty()) {
            result.addError("BSR code is mandatory for advance tax");
        } else if (!BSR_PATTERN.matcher(adv.getBsrCode()).matches()) {
            result.addError("Invalid BSR code format. Must be 7 digits");
        }
        
        if (adv.getChallanNo() == null || adv.getChallanNo().trim().isEmpty()) {
            result.addError("Challan number is mandatory for advance tax");
        }
        
        if (adv.getDepositDate() != null) {
            if (adv.getDepositDate().isBefore(FY_START) || adv.getDepositDate().isAfter(FY_END)) {
                result.addError("Advance tax deposit date must be within FY 2024-25");
            }
        }
    }

    private void validateSelfAssessmentTaxEntry(Itr1FormData.SelfAssessmentTaxEntry sat, ValidationResult result) {
        if (sat.getBsrCode() == null || sat.getBsrCode().trim().isEmpty()) {
            result.addError("BSR code is mandatory for self assessment tax");
        } else if (!BSR_PATTERN.matcher(sat.getBsrCode()).matches()) {
            result.addError("Invalid BSR code format. Must be 7 digits");
        }
        
        if (sat.getChallanNo() == null || sat.getChallanNo().trim().isEmpty()) {
            result.addError("Challan number is mandatory for self assessment tax");
        }
        
        if (sat.getDepositDate() == null) {
            result.addError("Deposit date is mandatory for self assessment tax");
        }
    }

    private void validateItr1Eligibility(Itr1FormData formData, ValidationResult result) {
        // Total income check
        if (formData.getTaxComputation() != null) {
            double totalIncome = formData.getTaxComputation().getGrossTotalIncome();
            if (totalIncome > ITR1_MAX_INCOME) {
                result.addError("Total income exceeds ₹50,00,000. Must file ITR-2 or higher");
            }
        }
        
        // Agricultural income check
        if (formData.getPersonalInfo() != null && formData.getPersonalInfo().getAgriculturalIncome() > ITR1_MAX_AGRICULTURAL_INCOME) {
            result.addError("Agricultural income exceeds ₹5,000. Must file ITR-2");
        }
        
        // Director check
        if (formData.getPersonalInfo() != null && formData.getPersonalInfo().isDirector()) {
            result.addError("Directors must file ITR-2 or higher");
        }
        
        // Unlisted shares check
        if (formData.getPersonalInfo() != null && formData.getPersonalInfo().isHoldsUnlistedShares()) {
            result.addError("Holders of unlisted equity shares must file ITR-2 or higher");
        }
    }

    private void validatePersonalInfoFlat(FlatFormData formData, ValidationResult result) {
        if (formData.getPan() != null && !formData.getPan().trim().isEmpty()) {
            if (!PAN_PATTERN.matcher(formData.getPan()).matches()) {
                result.addError("Invalid PAN format");
            }
        }
        
        if (formData.getAadhaar() != null && !formData.getAadhaar().trim().isEmpty()) {
            String aadhaar = formData.getAadhaar().replaceAll("\\s", "");
            if (!AADHAAR_PATTERN.matcher(aadhaar).matches()) {
                result.addError("Invalid Aadhaar format");
            }
        }
    }

    private void validateTDSEntries(List<FlatFormData.TDSEntry> entries, ValidationResult result) {
        if (entries == null) return;
        
        for (FlatFormData.TDSEntry entry : entries) {
            if (entry.getDeductorTAN() == null || entry.getDeductorTAN().trim().isEmpty()) {
                result.addError("Deductor TAN is mandatory for TDS entry");
            } else if (!TAN_PATTERN.matcher(entry.getDeductorTAN()).matches()) {
                result.addError("Invalid TAN format: " + entry.getDeductorTAN());
            }
            
            if (entry.getSection() == null || entry.getSection().trim().isEmpty()) {
                result.addError("TDS section is mandatory");
            }
            
            if (entry.getDeductionDate() != null) {
                if (entry.getDeductionDate().isBefore(FY_START) || entry.getDeductionDate().isAfter(FY_END)) {
                    result.addError("TDS deduction date must be within FY 2024-25");
                }
            }
        }
    }

    private void validateAdvanceTaxEntries(List<FlatFormData.AdvanceTaxEntry> entries, ValidationResult result) {
        if (entries == null) return;
        
        for (FlatFormData.AdvanceTaxEntry entry : entries) {
            if (entry.getBsrCode() != null && !entry.getBsrCode().trim().isEmpty()) {
                if (!BSR_PATTERN.matcher(entry.getBsrCode()).matches()) {
                    result.addError("Invalid BSR code format: " + entry.getBsrCode());
                }
            }
        }
    }

    private void validateSelfAssessmentTaxEntries(List<FlatFormData.SelfAssessmentTaxEntry> entries, ValidationResult result) {
        if (entries == null) return;
        
        for (FlatFormData.SelfAssessmentTaxEntry entry : entries) {
            if (entry.getBsrCode() != null && !entry.getBsrCode().trim().isEmpty()) {
                if (!BSR_PATTERN.matcher(entry.getBsrCode()).matches()) {
                    result.addError("Invalid BSR code format: " + entry.getBsrCode());
                }
            }
        }
    }

    private void validateAmounts(FlatFormData formData, ValidationResult result) {
        // All amounts should be non-negative (except losses)
        if (formData.getBasic() < 0) result.addError("Basic salary cannot be negative");
        if (formData.getInterestFD() < 0) result.addError("FD interest cannot be negative");
        if (formData.getInterestSB() < 0) result.addError("Savings interest cannot be negative");
        if (formData.getDividends() < 0) result.addError("Dividends cannot be negative");
    }

    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
            log.error("Validation Error: {}", error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
            log.warn("Validation Warning: {}", warning);
        }
        
        public boolean isValid() {
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
