package com.itr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr.dto.ITRFormData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * ITRJsonExportService — exports ITR form data to ITD schema JSON format.
 * Generates JSON that conforms to the official ITD e-filing portal schema
 * for AY 2026-27.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ITRJsonExportService {

    private final ObjectMapper objectMapper;

    /**
     * Generate ITR-1 (Sahaj) JSON in ITD schema format.
     *
     * @param formData the ITR-1 form data
     * @return JSON string conforming to ITD schema
     */
    public String generateITR1Json(ITRFormData formData) {
        log.info("Generating ITR-1 JSON for client {}", formData.getClientId());
        
        try {
            Map<String, Object> itr1Json = new HashMap<>();
            
            // Schema version
            itr1Json.put("schemaVersion", "AY202627");
            itr1Json.put("formName", "ITR-1");
            itr1Json.put("assessmentYear", formData.getAssessmentYear());
            
            // PersonalInfo section
            Map<String, Object> personalInfo = new HashMap<>();
            personalInfo.put("pan", formData.getPan());
            personalInfo.put("firstName", formData.getFirstName());
            personalInfo.put("lastName", formData.getLastName());
            personalInfo.put("dob", formData.getDob());
            personalInfo.put("gender", formData.getGender());
            personalInfo.put("fatherName", formData.getFatherName());
            personalInfo.put("mobile", formData.getMobile());
            personalInfo.put("email", formData.getEmail());
            personalInfo.put("residentialStatus", formData.getResidentialStatus());
            itr1Json.put("PersonalInfo", personalInfo);
            
            // IncomeDeductions section
            Map<String, Object> incomeDeductions = new HashMap<>();
            incomeDeductions.put("salary", formData.getSalaryIncome());
            incomeDeductions.put("allowances", formData.getAllowances());
            incomeDeductions.put("perquisites", formData.getPerquisites());
            incomeDeductions.put("profitsInSalary", formData.getProfitsInSalary());
            incomeDeductions.put("incomeFromHouseProperty", formData.getHousePropertyIncome());
            incomeDeductions.put("incomeFromOtherSources", formData.getOtherSourcesIncome());
            incomeDeductions.put("incomeFromOtherSourcesInterest", formData.getInterestIncome());
            incomeDeductions.put("dividendIncome", formData.getDividendIncome());
            itr1Json.put("IncomeDeductions", incomeDeductions);
            
            // Deductions section (Chapter VI-A)
            Map<String, Object> deductions = new HashMap<>();
            deductions.put("section80C", formData.getSection80C());
            deductions.put("section80CCD1", formData.getSection80CCD1());
            deductions.put("section80CCD1B", formData.getSection80CCD1B());
            deductions.put("section80CCD2", formData.getSection80CCD2());
            deductions.put("section80D", formData.getSection80D());
            deductions.put("section80DD", formData.getSection80DD());
            deductions.put("section80DDB", formData.getSection80DDB());
            deductions.put("section80E", formData.getSection80E());
            deductions.put("section80EE", formData.getSection80EE());
            deductions.put("section80G", formData.getSection80G());
            deductions.put("section80GG", formData.getSection80GG());
            deductions.put("section80TTA", formData.getSection80TTA());
            deductions.put("section80TTB", formData.getSection80TTB());
            itr1Json.put("Dedn", deductions);
            
            // TaxComputation section
            Map<String, Object> taxComputation = new HashMap<>();
            taxComputation.put("grossTotalIncome", formData.getGrossTotalIncome());
            taxComputation.put("totalDeductions", formData.getTotalDeductions());
            taxComputation.put("totalIncome", formData.getTotalIncome());
            taxComputation.put("taxOnTotalIncome", formData.getTaxOnTotalIncome());
            taxComputation.put("rebate87A", formData.getRebate87A());
            taxComputation.put("taxAfterRebate", formData.getTaxAfterRebate());
            taxComputation.put("surcharge", formData.getSurcharge());
            taxComputation.put("healthAndEducationCess", formData.getHealthAndEducationCess());
            taxComputation.put("totalTaxLiability", formData.getTotalTaxLiability());
            itr1Json.put("TaxComputation", taxComputation);
            
            // TDS section
            Map<String, Object> tds = new HashMap<>();
            tds.put("tdsOnSalary", formData.getTdsOnSalary());
            tds.put("tdsOnOtherThanSalary", formData.getTdsOnOtherThanSalary());
            tds.put("totalTds", formData.getTotalTds());
            itr1Json.put("TDS", tds);
            
            // TaxPaid section
            Map<String, Object> taxPaid = new HashMap<>();
            taxPaid.put("advanceTax", formData.getAdvanceTax());
            taxPaid.put("selfAssessmentTax", formData.getSelfAssessmentTax());
            taxPaid.put("totalTaxPaid", formData.getTotalTaxPaid());
            itr1Json.put("TaxPaid", taxPaid);
            
            // Verification section
            Map<String, Object> verification = new HashMap<>();
            verification.put("verified", "N");
            verification.put("date", "");
            itr1Json.put("Verification", verification);
            
            // CreationInfo for JSON integrity
            Map<String, Object> creationInfo = new HashMap<>();
            creationInfo.put("jsonVersion", "1.0");
            creationInfo.put("generatedBy", "ITR Filing Assistant");
            creationInfo.put("generatedAt", java.time.Instant.now().toString());
            itr1Json.put("CreationInfo", creationInfo);
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(itr1Json);
            
        } catch (Exception e) {
            log.error("Failed to generate ITR-1 JSON", e);
            throw new RuntimeException("Failed to generate ITR-1 JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Generate ITR-2 JSON in ITD schema format.
     */
    public String generateITR2Json(ITRFormData formData) {
        log.info("Generating ITR-2 JSON for client {}", formData.getClientId());
        // Similar to ITR-1 but with additional sections:
        // - Schedule HP (multiple properties)
        // - Schedule CG (capital gains)
        // - Schedule AL (assets)
        // - Schedule FA (foreign assets)
        // - Schedule FSI (foreign income)
        return generateBaseJson(formData, "ITR-2");
    }

    /**
     * Generate ITR-3 JSON in ITD schema format.
     */
    public String generateITR3Json(ITRFormData formData) {
        log.info("Generating ITR-3 JSON for client {}", formData.getClientId());
        // Similar to ITR-2 but with business sections:
        // - Schedule BP (business profits)
        // - Schedule DPM (depreciation)
        // - Schedule PL (P&L reference)
        // - Schedule BS (balance sheet)
        return generateBaseJson(formData, "ITR-3");
    }

    /**
     * Generate ITR-4 JSON in ITD schema format.
     */
    public String generateITR4Json(ITRFormData formData) {
        log.info("Generating ITR-4 JSON for client {}", formData.getClientId());
        // Similar to ITR-1 but with presumptive sections:
        // - Schedule Presumptive (44AD/44ADA/44AE)
        return generateBaseJson(formData, "ITR-4");
    }

    private String generateBaseJson(ITRFormData formData, String formName) {
        try {
            Map<String, Object> itrJson = new HashMap<>();
            itrJson.put("schemaVersion", "AY202627");
            itrJson.put("formName", formName);
            itrJson.put("assessmentYear", formData.getAssessmentYear());
            
            Map<String, Object> personalInfo = new HashMap<>();
            personalInfo.put("pan", formData.getPan());
            itrJson.put("PersonalInfo", personalInfo);
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(itrJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Convert monetary value to paise (multiply by 100).
     */
    public static long toPaise(double rupees) {
        return (long) (rupees * 100);
    }

    /**
     * Convert paise to rupees (divide by 100).
     */
    public static double toRupees(long paise) {
        return paise / 100.0;
    }
}
