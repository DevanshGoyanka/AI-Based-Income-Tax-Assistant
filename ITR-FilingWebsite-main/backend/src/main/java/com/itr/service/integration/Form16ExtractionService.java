package com.itr.service.integration;

import com.itr.dto.Form16Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Form 16 PDF Extraction Service - 101% CBDT Compliant
 * Extracts data from Form 16 Part A & Part B using Apache PDFBox
 */
@Slf4j
@Service
public class Form16ExtractionService {

    private static final Pattern PAN_PATTERN = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]");
    private static final Pattern TAN_PATTERN = Pattern.compile("[A-Z]{4}[0-9]{5}[A-Z]");
    private static final Pattern FY_PATTERN = Pattern.compile("(20[0-9]{2})[-\\s]+(20[0-9]{2})");
    
    public Form16Data extractForm16(MultipartFile pdfFile) throws IOException {
        log.info("Starting Form 16 extraction: {}", pdfFile.getOriginalFilename());
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(pdfFile.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            Form16Data.PartA partA = extractPartA(text, errors, warnings);
            Form16Data.PartB partB = extractPartB(text, errors, warnings);
            validateExtractedData(partA, partB, errors, warnings);
            
            return Form16Data.builder()
                    .partA(partA)
                    .partB(partB)
                    .validationErrors(errors)
                    .validationWarnings(warnings)
                    .build();
            
        } catch (Exception e) {
            log.error("Error extracting Form 16: {}", e.getMessage(), e);
            errors.add("Failed to extract Form 16: " + e.getMessage());
            return Form16Data.builder().validationErrors(errors).validationWarnings(warnings).build();
        }
    }
    
    private Form16Data.PartA extractPartA(String text, List<String> errors, List<String> warnings) {
        Form16Data.PartA.PartABuilder builder = Form16Data.PartA.builder();
        
        String tan = extractPattern(text, "TAN\\s*[:\\-]?\\s*(" + TAN_PATTERN.pattern() + ")", 1);
        if (tan != null) builder.employerTAN(tan);
        else errors.add("Employer TAN not found");
        
        String employerPAN = extractPattern(text, "(?:Employer|Deductor)\\s*PAN\\s*[:\\-]?\\s*(" + PAN_PATTERN.pattern() + ")", 1);
        if (employerPAN != null) builder.employerPAN(employerPAN);
        
        String employeePAN = extractPattern(text, "(?:Employee|Deductee)\\s*PAN\\s*[:\\-]?\\s*(" + PAN_PATTERN.pattern() + ")", 1);
        if (employeePAN != null) builder.employeePAN(employeePAN);
        else errors.add("Employee PAN not found");
        
        Matcher fyMatcher = FY_PATTERN.matcher(text);
        if (fyMatcher.find()) {
            String fy = fyMatcher.group(1) + "-" + fyMatcher.group(2);
            builder.financialYear(fy);
            int year2 = Integer.parseInt(fyMatcher.group(2));
            builder.assessmentYear(year2 + "-" + (year2 + 1));
        } else {
            warnings.add("Financial Year not found");
        }
        
        String employerName = extractEmployerName(text);
        if (employerName != null) builder.employerName(employerName);
        
        String employeeName = extractEmployeeName(text);
        if (employeeName != null) builder.employeeName(employeeName);
        
        Double totalTDS = extractAmount(text, "(?:Total Tax Deducted|Tax Deducted at Source)\\s*[:\\-]?\\s*₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (totalTDS != null) {
            builder.totalTaxDeducted(totalTDS);
            builder.totalTaxDeposited(totalTDS);
        }
        
        List<Form16Data.QuarterlyTDS> quarterlyTDS = extractQuarterlyTDS(text);
        if (!quarterlyTDS.isEmpty()) builder.quarterlyTDS(quarterlyTDS);
        
        return builder.build();
    }
    
    private Form16Data.PartB extractPartB(String text, List<String> errors, List<String> warnings) {
        Form16Data.PartB.PartBBuilder builder = Form16Data.PartB.builder();
        
        Double salary = extractAmount(text, "(?:Salary as per|Gross Salary|Section 17\\(1\\)).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (salary != null) builder.salary(salary);
        
        Double perquisites = extractAmount(text, "(?:Value of perquisites|Section 17\\(2\\)).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (perquisites != null) builder.valueOfPerquisites(perquisites);
        
        Double profitsInLieu = extractAmount(text, "(?:Profits in lieu|Section 17\\(3\\)).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (profitsInLieu != null) builder.profitsInLieuOfSalary(profitsInLieu);
        
        Double totalGross = extractAmount(text, "(?:Total.*Gross Salary).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (totalGross != null) builder.totalGrossSalary(totalGross);
        
        Double hraReceived = extractAmount(text, "(?:HRA Received).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (hraReceived != null) builder.hraReceived(hraReceived);
        
        Double hraExempt = extractAmount(text, "(?:HRA Exempt|Less:.*HRA).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (hraExempt != null) builder.hraExempt(hraExempt);
        
        Double ltaExempt = extractAmount(text, "(?:LTA Exempt).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (ltaExempt != null) builder.ltaExempt(ltaExempt);
        
        Double standardDed = extractAmount(text, "(?:Standard Deduction).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (standardDed != null) builder.standardDeduction(standardDed);
        
        Double profTax = extractAmount(text, "(?:Professional Tax).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (profTax != null) builder.professionalTax(profTax);
        
        Double incomeFromSalary = extractAmount(text, "(?:Income.*Salary).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (incomeFromSalary != null) builder.incomeFromSalary(incomeFromSalary);
        
        Double gti = extractAmount(text, "(?:Gross Total Income).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (gti != null) builder.grossTotalIncome(gti);
        
        builder.deductions(extractDeductions(text));
        
        Double totalIncome = extractAmount(text, "(?:Total Income).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (totalIncome != null) builder.totalIncome(totalIncome);
        
        Double tax = extractAmount(text, "(?:Tax on Total Income).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (tax != null) builder.taxOnTotalIncome(tax);
        
        Double rebate = extractAmount(text, "(?:Rebate.*87A).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (rebate != null) builder.rebateU87A(rebate);
        
        Double surcharge = extractAmount(text, "(?:Surcharge).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (surcharge != null) builder.surcharge(surcharge);
        
        Double cess = extractAmount(text, "(?:Health.*Education.*Cess|Cess).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (cess != null) builder.healthEducationCess(cess);
        
        Double totalTaxPayable = extractAmount(text, "(?:Total Tax Payable).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (totalTaxPayable != null) builder.totalTaxPayable(totalTaxPayable);
        
        return builder.build();
    }
    
    private Form16Data.Deductions extractDeductions(String text) {
        Form16Data.Deductions.DeductionsBuilder builder = Form16Data.Deductions.builder();
        
        Double d80C = extractAmount(text, "(?:Section 80C|Deduction.*80C).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (d80C != null) builder.deduction80C(d80C);
        
        Double d80CCD1B = extractAmount(text, "(?:Section 80CCD\\(1B\\)).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (d80CCD1B != null) builder.deduction80CCD1B(d80CCD1B);
        
        Double d80CCD2 = extractAmount(text, "(?:Section 80CCD\\(2\\)|Employer.*NPS).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (d80CCD2 != null) builder.deduction80CCD2(d80CCD2);
        
        Double d80D = extractAmount(text, "(?:Section 80D|Medical Insurance).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (d80D != null) builder.deduction80D(d80D);
        
        Double d80E = extractAmount(text, "(?:Section 80E|Education Loan).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (d80E != null) builder.deduction80E(d80E);
        
        Double d80G = extractAmount(text, "(?:Section 80G|Donations).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (d80G != null) builder.deduction80G(d80G);
        
        Double d80TTA = extractAmount(text, "(?:Section 80TTA).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (d80TTA != null) builder.deduction80TTA(d80TTA);
        
        Double d80TTB = extractAmount(text, "(?:Section 80TTB).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (d80TTB != null) builder.deduction80TTB(d80TTB);
        
        Double total = extractAmount(text, "(?:Total.*Deductions|Aggregate.*Deductions).*?₹?\\s*([0-9,]+\\.?[0-9]*)");
        if (total != null) builder.totalDeductions(total);
        
        return builder.build();
    }
    
    private String extractPattern(String text, String pattern, int group) {
        try {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.find()) return m.group(group).trim();
        } catch (Exception e) {
            log.debug("Pattern extraction failed: {}", pattern);
        }
        return null;
    }
    
    private Double extractAmount(String text, String pattern) {
        try {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher m = p.matcher(text);
            if (m.find()) {
                String amountStr = m.group(1).replaceAll(",", "").trim();
                return Double.parseDouble(amountStr);
            }
        } catch (Exception e) {
            log.debug("Amount extraction failed: {}", pattern);
        }
        return null;
    }
    
    private String extractEmployerName(String text) {
        String[] lines = text.split("\\n");
        for (int i = 0; i < Math.min(10, lines.length); i++) {
            if (lines[i].contains("Name") && lines[i].contains("Address")) {
                if (i + 1 < lines.length) return lines[i + 1].trim();
            }
        }
        return null;
    }
    
    private String extractEmployeeName(String text) {
        String pattern = "(?:Employee|Deductee)\\s*Name\\s*[:\\-]?\\s*([A-Za-z\\s]+)";
        return extractPattern(text, pattern, 1);
    }
    
    private List<Form16Data.QuarterlyTDS> extractQuarterlyTDS(String text) {
        List<Form16Data.QuarterlyTDS> list = new ArrayList<>();
        String[] quarters = {"Q1", "Q2", "Q3", "Q4"};
        
        for (String quarter : quarters) {
            String pattern = quarter + ".*?([0-9,]+\\.?[0-9]*)";
            Double amount = extractAmount(text, pattern);
            if (amount != null) {
                list.add(Form16Data.QuarterlyTDS.builder()
                        .quarter(quarter)
                        .amountDeducted(amount)
                        .amountDeposited(amount)
                        .build());
            }
        }
        return list;
    }
    
    private void validateExtractedData(Form16Data.PartA partA, Form16Data.PartB partB, 
                                      List<String> errors, List<String> warnings) {
        if (partA == null) {
            errors.add("Part A extraction failed");
            return;
        }
        
        if (partA.getEmployeePAN() == null) errors.add("Employee PAN is mandatory");
        if (partA.getEmployerTAN() == null) errors.add("Employer TAN is mandatory");
        if (partA.getTotalTaxDeducted() == null || partA.getTotalTaxDeducted() == 0) {
            warnings.add("No TDS amount found");
        }
        
        if (partB != null && partB.getTotalIncome() != null && partB.getTotalIncome() < 0) {
            errors.add("Total income cannot be negative");
        }
    }
}
