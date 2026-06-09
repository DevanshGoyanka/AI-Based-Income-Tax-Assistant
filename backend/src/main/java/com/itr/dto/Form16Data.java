package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Form 16 Extracted Data DTO
 * 101% CBDT Compliant - Matches Form 16 Part A & Part B structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Form16Data {
    
    private PartA partA;
    private PartB partB;
    private List<String> validationErrors;
    private List<String> validationWarnings;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartA {
        // Employer Details
        private String employerName;
        private String employerTAN;
        private String employerPAN;
        private String employerAddress;
        
        // Employee Details
        private String employeeName;
        private String employeePAN;
        private String employeeDesignation;
        
        // Period
        private String financialYear;
        private String assessmentYear;
        private String periodFrom;
        private String periodTo;
        
        // TDS Summary
        private Double totalTaxDeducted;
        private Double totalTaxDeposited;
        
        // Quarterly TDS Details
        private List<QuarterlyTDS> quarterlyTDS;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuarterlyTDS {
        private String quarter;
        private String receiptNumber;
        private Double amountDeducted;
        private Double amountDeposited;
        private String depositDate;
        private String bsrCode;
        private String challanSerialNumber;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartB {
        // Gross Salary - Section 17(1)
        private Double salary;
        private Double valueOfPerquisites;
        private Double profitsInLieuOfSalary;
        private Double totalGrossSalary;
        
        // Allowances - Section 10
        private Double hraReceived;
        private Double hraExempt;
        private Double ltaExempt;
        private Double otherAllowancesExempt;
        
        // Deductions from Salary - Section 16
        private Double standardDeduction;
        private Double entertainmentAllowance;
        private Double professionalTax;
        
        // Income from Salary
        private Double incomeFromSalary;
        
        // Other Income
        private Double incomeFromOtherSources;
        
        // Gross Total Income
        private Double grossTotalIncome;
        
        // Deductions - Chapter VI-A
        private Deductions deductions;
        
        // Total Income
        private Double totalIncome;
        
        // Tax Computation
        private Double taxOnTotalIncome;
        private Double rebateU87A;
        private Double surcharge;
        private Double healthEducationCess;
        private Double totalTaxPayable;
        private Double reliefU89;
        private Double netTaxPayable;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Deductions {
        private Double deduction80C;
        private Double deduction80CCC;
        private Double deduction80CCD1;
        private Double deduction80CCD1B;
        private Double deduction80CCD2;
        private Double deduction80D;
        private Double deduction80DD;
        private Double deduction80DDB;
        private Double deduction80E;
        private Double deduction80EE;
        private Double deduction80EEA;
        private Double deduction80EEB;
        private Double deduction80G;
        private Double deduction80GG;
        private Double deduction80GGA;
        private Double deduction80GGC;
        private Double deduction80IA;
        private Double deduction80IB;
        private Double deduction80JJAA;
        private Double deduction80TTA;
        private Double deduction80TTB;
        private Double deduction80U;
        private Double totalDeductions;
    }
}
