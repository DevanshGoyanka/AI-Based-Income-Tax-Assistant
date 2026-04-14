package com.itr.service;

import com.itr.dto.Itr3FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ITR3ValidationService {

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    public ITR1ValidationService.ValidationResult validateITR3(Itr3FormData formData) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validatePartA(formData, errors, warnings);
        validateBusinessProfession(formData, errors, warnings);
        validateDepreciation(formData, errors, warnings);
        validateGST(formData, errors, warnings);
        validateAudit(formData, errors, warnings);

        return ITR1ValidationService.ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    private void validatePartA(Itr3FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getPartA() == null) {
            errors.add("Part A is mandatory");
            return;
        }
        var partA = formData.getPartA();
        if (partA.getPan() == null || !PAN_PATTERN.matcher(partA.getPan()).matches())
            errors.add("Invalid PAN format");
    }

    private void validateBusinessProfession(Itr3FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getScheduleBP() == null) return;
        var bp = formData.getScheduleBP();

        if (bp.isFandO() && bp.getFandOTurnover() > 10000000) {
            warnings.add("F&O turnover exceeds ₹1 crore: Tax audit may be required under Section 44AB");
        }

        if (bp.isIntraday() && bp.getIntradayTurnover() > 10000000) {
            warnings.add("Intraday turnover exceeds ₹1 crore: Tax audit may be required");
        }
    }

    private void validateDepreciation(Itr3FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getScheduleDPM() == null) return;
        for (var block : formData.getScheduleDPM().getBlocks()) {
            if ("Goodwill".equalsIgnoreCase(block.getAssetClass()) && block.getDepreciationRate() > 0) {
                errors.add("Goodwill is not eligible for depreciation from AY 2021-22 onwards");
            }
        }
    }

    private void validateGST(Itr3FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getScheduleGST() == null) return;
        var gst = formData.getScheduleGST();
        double diff = Math.abs(gst.getTurnoverAsPerBooks() - gst.getTurnoverAsPerGSTR1());
        if (diff > 100000 && (gst.getReasonForDifference() == null || gst.getReasonForDifference().isEmpty())) {
            warnings.add("GST turnover differs from books by ₹" + diff + ". Please provide reason.");
        }
    }

    private void validateAudit(Itr3FormData formData, List<String> errors, List<String> warnings) {
        if (formData.getAuditDetails() == null) return;
        var audit = formData.getAuditDetails();
        if (audit.isAuditRequired()) {
            if (audit.getAuditorPAN() == null || audit.getAuditorPAN().isEmpty())
                errors.add("Auditor PAN is mandatory for audit cases");
        }
    }
}
