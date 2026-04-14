package com.itr.service;

import com.itr.dto.Itr2FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ITR2ValidationService {

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern TAN_PATTERN = Pattern.compile("^[A-Z]{4}[0-9]{5}[A-Z]$");
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

    public ITR1ValidationService.ValidationResult validateITR2(Itr2FormData formData) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validatePartA(formData, errors, warnings);
        validateHouseProperties(formData, errors, warnings);
        validateCapitalGains(formData, errors, warnings);
        validateVDA(formData, errors, warnings);
        validateAL(formData, errors, warnings);
        validateFA(formData, errors, warnings);
        validateDeductions(formData, errors, warnings);

        return ITR1ValidationService.ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    private void validatePartA(Itr2FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getPartA() == null) {
            errors.add("Part A is mandatory");
            return;
        }
        var partA = formData.getPartA();
        if (partA.getPan() == null || !PAN_PATTERN.matcher(partA.getPan()).matches())
            errors.add("Invalid PAN format");
        if (partA.getBankAccountNo() != null && !partA.getBankAccountNo().isEmpty()
                && (partA.getBankIFSC() == null || !IFSC_PATTERN.matcher(partA.getBankIFSC()).matches()))
            errors.add("Invalid IFSC format");
        if (!partA.isPanAadhaarLinked())
            warnings.add("PAN-Aadhaar linkage not verified");
    }

    private void validateHouseProperties(Itr2FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getHouseProperties() == null) return;
        
        long selfOccupied = formData.getHouseProperties().stream()
                .filter(hp -> "SELF_OCCUPIED".equals(hp.getPropertyType())).count();
        if (selfOccupied > 2) {
            warnings.add("More than 2 self-occupied properties: third property onwards treated as deemed let-out");
        }
        
        for (var hp : formData.getHouseProperties()) {
            if ("SELF_OCCUPIED".equals(hp.getPropertyType()) && hp.getInterestOnLoan() > 200000) {
                errors.add("Interest on home loan for self-occupied property cannot exceed ₹2,00,000");
            }
            if ("LET_OUT".equals(hp.getPropertyType()) && hp.getGrossRent() > 600000
                    && (hp.getTenantPAN() == null || hp.getTenantPAN().isEmpty())) {
                warnings.add("Tenant PAN recommended if annual rent exceeds ₹50,000/month");
            }
        }
    }

    private void validateCapitalGains(Itr2FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getScheduleCG() == null) return;
        var cg = formData.getScheduleCG();
        
        if (cg.getLtcg112ATotal() > 125000 && cg.getLtcg112AExemption() > 125000) {
            errors.add("LTCG 112A annual exemption cannot exceed ₹1,25,000");
        }
        
        if (cg.getExemptions() != null) {
            for (var exemption : cg.getExemptions()) {
                if ("54EC".equals(exemption.getSection()) && exemption.getExemptionAmount() > 5000000) {
                    errors.add("Section 54EC exemption cannot exceed ₹50,00,000 in a financial year");
                }
            }
        }
    }

    private void validateVDA(Itr2FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getScheduleVDA() == null) return;
        if (formData.getScheduleVDA().getTotalVDAIncome() < 0) {
            errors.add("VDA loss cannot be set off against any other income or carried forward");
        }
    }

    private void validateAL(Itr2FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getComputation() == null) return;
        double gti = formData.getComputation().getGrossTotalIncome();
        if (gti > 5000000 && formData.getScheduleAL() == null) {
            errors.add("Schedule AL (Assets & Liabilities) is mandatory if total income exceeds ₹50 lakh");
        }
    }

    private void validateFA(Itr2FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getScheduleFA() == null) return;
        var fa = formData.getScheduleFA();
        boolean hasForeignAssets = (fa.getForeignBankAccounts() != null && !fa.getForeignBankAccounts().isEmpty()) ||
                                   (fa.getForeignEquityDebt() != null && !fa.getForeignEquityDebt().isEmpty());
        if (hasForeignAssets && !fa.isBlackMoneyActAcknowledged()) {
            warnings.add("Black Money Act: Non-disclosure of foreign assets attracts penalty of ₹10,00,000 per asset");
        }
    }

    private void validateDeductions(Itr2FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getDeductions() == null) return;
        var ded = formData.getDeductions();
        
        if (ded.getDeduction80C() + ded.getDeduction80CCC() + ded.getDeduction80CCD1() > 150000)
            errors.add("80C + 80CCC + 80CCD(1) aggregate cannot exceed ₹1,50,000");
        if (ded.getDeduction80CCD1B() > 50000)
            errors.add("80CCD(1B) cannot exceed ₹50,000");
    }
}
