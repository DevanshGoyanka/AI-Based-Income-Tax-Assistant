package com.itr.service;

import com.itr.dto.Itr4FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ITR4ValidationService {

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    public ITR1ValidationService.ValidationResult validateITR4(Itr4FormData formData) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validatePartA(formData, errors, warnings);
        validatePresumptive(formData, errors, warnings);

        return ITR1ValidationService.ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    private void validatePartA(Itr4FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getPartA() == null) {
            errors.add("Part A is mandatory");
            return;
        }
        var partA = formData.getPartA();
        if (partA.getPan() == null || !PAN_PATTERN.matcher(partA.getPan()).matches())
            errors.add("Invalid PAN format");
    }

    private void validatePresumptive(Itr4FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getSchedulePresumptive() == null) return;
        var presumptive = formData.getSchedulePresumptive();

        if (presumptive.getBusiness44AD() != null && presumptive.getBusiness44AD().isApplicable()) {
            var business = presumptive.getBusiness44AD();
            double totalTurnover = business.getGrossTurnoverDigital() + business.getGrossTurnoverCash();
            
            if (totalTurnover > 200000000) {
                errors.add("Section 44AD not applicable if turnover exceeds ₹2 crore (₹3 crore for digital)");
            }
            
            double minPresumptive = business.getGrossTurnoverDigital() * 0.06 + business.getGrossTurnoverCash() * 0.08;
            if (business.getDeclaredIncome() < minPresumptive) {
                warnings.add("Declared income less than presumptive: 5-year lock-in and audit requirements apply");
            }
        }

        if (presumptive.getProfessional44ADA() != null && presumptive.getProfessional44ADA().isApplicable()) {
            var professional = presumptive.getProfessional44ADA();
            
            if (professional.getGrossReceipts() > 5000000) {
                errors.add("Section 44ADA not applicable if gross receipts exceed ₹50 lakh (₹75 lakh from AY 2024-25)");
            }
            
            if (professional.getDeclaredIncome() < professional.getGrossReceipts() * 0.50) {
                warnings.add("Declared income less than 50%: Regular books and audit required");
            }
        }
    }
}
