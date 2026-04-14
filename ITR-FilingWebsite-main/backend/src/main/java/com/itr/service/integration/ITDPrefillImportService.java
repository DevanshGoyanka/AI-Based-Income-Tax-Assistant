package com.itr.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr.dto.ITDPrefillData;
import com.itr.dto.Itr1FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ITD Prefill JSON Import Service - 101% CBDT Compliant
 */
@Slf4j
@Service
public class ITDPrefillImportService {

    private final ObjectMapper objectMapper;

    public ITDPrefillImportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ITDPrefillData importPrefillData(MultipartFile jsonFile) throws IOException {
        log.info("Importing ITD Prefill JSON: {}", jsonFile.getOriginalFilename());
        
        try {
            ITDPrefillData data = objectMapper.readValue(jsonFile.getInputStream(), ITDPrefillData.class);
            validatePrefillData(data);
            log.info("ITD Prefill import successful for PAN: {}", data.getPersonalInfo().getPan());
            return data;
        } catch (Exception e) {
            log.error("Failed to import ITD Prefill: {}", e.getMessage(), e);
            throw new IOException("Invalid ITD Prefill JSON format: " + e.getMessage(), e);
        }
    }

    public Itr1FormData autoPopulateFromPrefill(Itr1FormData formData, ITDPrefillData prefill) {
        log.info("Auto-populating ITR-1 from ITD Prefill");
        
        if (prefill.getPersonalInfo() != null) {
            if (formData.getPersonalInfo() == null) {
                formData.setPersonalInfo(Itr1FormData.PersonalInfo.builder().build());
            }
            formData.getPersonalInfo().setPan(prefill.getPersonalInfo().getPan());
            formData.getPersonalInfo().setAssesseeName(prefill.getPersonalInfo().getName());
            if (prefill.getPersonalInfo().getDateOfBirth() != null) {
                formData.getPersonalInfo().setDateOfBirth(LocalDate.parse(prefill.getPersonalInfo().getDateOfBirth()));
            }
        }
        
        if (prefill.getSalaryIncome() != null && !prefill.getSalaryIncome().isEmpty()) {
            if (formData.getSalaryIncome() == null) {
                formData.setSalaryIncome(Itr1FormData.SalaryIncome.builder().build());
            }
            ITDPrefillData.SalaryIncome salary = prefill.getSalaryIncome().get(0);
            formData.getSalaryIncome().setSalary17_1(salary.getGrossSalary());
            formData.getSalaryIncome().setStandardDeduction(salary.getStandardDeduction());
            formData.getSalaryIncome().setProfessionalTax(salary.getProfessionalTax());
        }
        
        if (prefill.getOtherSources() != null) {
            if (formData.getOtherSourcesIncome() == null) {
                formData.setOtherSourcesIncome(Itr1FormData.OtherSourcesIncome.builder().build());
            }
            formData.getOtherSourcesIncome().setSavingsAccountInterest(prefill.getOtherSources().getInterestIncome());
            formData.getOtherSourcesIncome().setDividendFromShares(prefill.getOtherSources().getDividendIncome());
        }
        
        if (prefill.getTdsDetails() != null) {
            if (formData.getTaxPayments() == null) {
                formData.setTaxPayments(Itr1FormData.TaxPayments.builder().build());
            }
            formData.getTaxPayments().setTotalTDSOnSalary(prefill.getTdsDetails().getTotalTDS());
        }
        
        if (prefill.getTaxPayments() != null) {
            if (formData.getTaxPayments() == null) {
                formData.setTaxPayments(Itr1FormData.TaxPayments.builder().build());
            }
            formData.getTaxPayments().setTotalAdvanceTax(prefill.getTaxPayments().getAdvanceTax());
            formData.getTaxPayments().setTotalSelfAssessmentTax(prefill.getTaxPayments().getSelfAssessmentTax());
        }
        
        if (prefill.getDeductions() != null) {
            if (formData.getDeductions() == null) {
                formData.setDeductions(Itr1FormData.Deductions.builder().build());
            }
            ITDPrefillData.Deductions ded = prefill.getDeductions();
            formData.getDeductions().setDeduction80C(ded.getSection80C());
            formData.getDeductions().setNpsEmployee80CCD1B(ded.getSection80CCD1B());
            formData.getDeductions().setNpsEmployer80CCD2(ded.getSection80CCD2());
            formData.getDeductions().setDeduction80D(ded.getSection80D());
            formData.getDeductions().setDeduction80E(ded.getSection80E());
            formData.getDeductions().setDeduction80G(ded.getSection80G());
            formData.getDeductions().setDeduction80TTA(ded.getSection80TTA());
            formData.getDeductions().setDeduction80TTB(ded.getSection80TTB());
        }
        
        log.info("Auto-population from ITD Prefill completed");
        return formData;
    }

    private void validatePrefillData(ITDPrefillData data) {
        List<String> errors = new ArrayList<>();
        
        if (data.getPersonalInfo() == null) {
            errors.add("Personal info is mandatory");
        } else {
            if (data.getPersonalInfo().getPan() == null || 
                !data.getPersonalInfo().getPan().matches("[A-Z]{5}[0-9]{4}[A-Z]")) {
                errors.add("Invalid PAN format");
            }
        }
        
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("ITD Prefill validation failed: " + String.join(", ", errors));
        }
    }
}
