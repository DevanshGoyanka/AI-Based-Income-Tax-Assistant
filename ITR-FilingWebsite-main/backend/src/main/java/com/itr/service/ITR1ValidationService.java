package com.itr.service;

import com.itr.dto.Itr1FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ITR1ValidationService {

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^[0-9]{12}$");
    private static final Pattern TAN_PATTERN = Pattern.compile("^[A-Z]{4}[0-9]{5}[A-Z]$");
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^[0-9]{6}$");

    public ValidationResult validateITR1(Itr1FormData formData) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validatePersonalInfo(formData.getPersonalInfo(), errors, warnings);
        if (formData.getSalaryIncome() != null) validateSalaryIncome(formData.getSalaryIncome(), errors);
        if (formData.getHousePropertyIncome() != null) validateHousePropertyIncome(formData.getHousePropertyIncome(), errors, warnings);
        if (formData.getOtherSourcesIncome() != null) validateOtherSourcesIncome(formData.getOtherSourcesIncome(), errors);
        if (formData.getDeductions() != null) {
            validateDeductions(formData.getDeductions(), "NEW".equals(formData.getPersonalInfo().getRegime()), errors, warnings);
        }

        return ValidationResult.builder().valid(errors.isEmpty()).errors(errors).warnings(warnings).build();
    }

    private void validatePersonalInfo(Itr1FormData.PersonalInfo info, List<String> errors, List<String> warnings) {
        if (info == null) { errors.add("Personal Info is mandatory"); return; }
        if (info.getPan() == null || !PAN_PATTERN.matcher(info.getPan()).matches())
            errors.add("Invalid PAN format");
        if (info.getAadhaar() == null || !AADHAAR_PATTERN.matcher(info.getAadhaar()).matches())
            errors.add("Invalid Aadhaar format");
        if (!info.isPanAadhaarLinked())
            warnings.add("PAN-Aadhaar linkage not verified");
    }

    private void validateSalaryIncome(Itr1FormData.SalaryIncome salary, List<String> errors) {
        if (salary.getProfessionalTax() > 2500)
            errors.add("Professional tax cannot exceed ₹2,500");
    }

    private void validateHousePropertyIncome(Itr1FormData.HousePropertyIncome hp, List<String> errors, List<String> warnings) {
        if ("SELF_OCCUPIED".equals(hp.getPropertyType()) && hp.getInterestOnLoan() > 200000)
            errors.add("Interest on home loan for self-occupied property cannot exceed ₹2,00,000");
    }

    private void validateOtherSourcesIncome(Itr1FormData.OtherSourcesIncome os, List<String> errors) {
        if (os.getFamilyPensionReceived() > 0 && os.getFamilyPensionDeduction() > Math.min(15000, os.getFamilyPensionReceived() / 3.0))
            errors.add("Family pension deduction cannot exceed ₹15,000 or 1/3rd");
    }

    private void validateDeductions(Itr1FormData.Deductions d, boolean isNew, List<String> errors, List<String> warnings) {
        if (!isNew) {
            if (d.getDeduction80C() > 150000)
                errors.add("80C cannot exceed ₹1,50,000");
            if (d.getNpsEmployee80CCD1B() > 50000)
                errors.add("80CCD(1B) cannot exceed ₹50,000");
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
    }
}
