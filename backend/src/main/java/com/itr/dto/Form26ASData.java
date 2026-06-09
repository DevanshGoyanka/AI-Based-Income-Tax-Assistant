package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Form 26AS (TRACES) - Complete CBDT Compliant Structure
 * 
 * CBDT Standard Parts:
 * - PART-B: TDS on Salary (Section 192)
 * - PART-C: TDS Other than Salary (194A, 194C, 194J, 194H, 194I, 194D, 194IB, 194IA)
 * - PART-D: TCS (Section 206C)
 * - PART-E: Tax Paid (Advance Tax, Self-Assessment Tax, Regular Tax)
 * - PART-F: Refunds
 * - PART-X: High Value Transactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Form26ASData {
    
    // Verification
    private String assesseName;
    private String assessePAN;
    private String financialYear;
    private String assessmentYear;
    private String status;
    
    // PART-B: TDS on Salary
    @Builder.Default
    private List<TDSOnSalary> tdsOnSalary = new ArrayList<>();
    
    // PART-C: TDS on Other than Salary
    @Builder.Default
    private List<TDSOtherThanSalary> tdsOnInterest = new ArrayList<>();     // 194A
    @Builder.Default
    private List<TDSOtherThanSalary> tdsOnContractor = new ArrayList<>();   // 194C
    @Builder.Default
    private List<TDSOtherThanSalary> tdsOnProfessional = new ArrayList<>(); // 194J
    @Builder.Default
    private List<TDSOtherThanSalary> tdsOnCommission = new ArrayList<>();   // 194H
    @Builder.Default
    private List<TDSOtherThanSalary> tdsOnRent = new ArrayList<>();         // 194I
    @Builder.Default
    private List<TDSOtherThanSalary> tdsOnInsurance = new ArrayList<>();    // 194D
    @Builder.Default
    private List<TDSOtherThanSalary> tdsOnProperty = new ArrayList<>();     // 194IB, 194IA
    @Builder.Default
    private List<TDSOtherThanSalary> tdsOnOther = new ArrayList<>();
    
    // PART-D: TCS
    @Builder.Default
    private List<TCSEntry> tcsEntries = new ArrayList<>();
    
    // PART-E: Tax Payments (Self Assessment, Advance Tax, etc.)
    @Builder.Default
    private List<TaxPaymentEntry> taxPayments = new ArrayList<>();
    
    // PART-F: Refunds
    @Builder.Default
    private List<RefundEntry> refunds = new ArrayList<>();
    
    // PART-X: High Value Transactions
    @Builder.Default
    private List<HighValueTransaction> highValueTransactions = new ArrayList<>();
    
    // Computed totals for auto-population
    private BigDecimal totalTDSSalary;
    private BigDecimal totalTDSInterest;
    private BigDecimal totalTDSContractor;
    private BigDecimal totalTDSProfessional;
    private BigDecimal totalTDSOther;
    private BigDecimal totalTaxPaid;
    private BigDecimal totalRefund;
    
    // ==================== NESTED CLASSES ====================
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSOnSalary {
        private String deductorName;
        private String deductorTAN;
        private String section;        // 192, 192A
        private String quarter;        // Q1, Q2, Q3, Q4
        private BigDecimal amountPaid;
        private BigDecimal taxDeducted;
        private BigDecimal taxDeposited;
        private String date;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSOtherThanSalary {
        private String deductorName;
        private String deductorTAN;
        private String deductorPAN;
        private String section;        // 194A, 194C, 194J, etc.
        private String bsrCode;        // Bank code
        private String challanNo;
        private String depositDate;
        private BigDecimal amountPaid;
        private BigDecimal taxDeducted;
        private BigDecimal taxDeposited;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TCSEntry {
        private String collectorName;
        private String collectorTAN;
        private String section;        // 206C
        private String quarter;
        private BigDecimal amountCollected;
        private BigDecimal taxCollected;
        private BigDecimal taxDeposited;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxPaymentEntry {
        private String bsrCode;
        private String challanNo;
        private String date;
        private BigDecimal amount;
        private String type;           // SELF_ASSESSMENT, ADVANCE, REGULAR, TDS, OTHERS
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundEntry {
        private String assessmentYear;
        private String returnType;
        private BigDecimal refundAmount;
        private BigDecimal interestAmount;
        private String refundDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighValueTransaction {
        private String nature;
        private String name;
        private String accountNo;
        private BigDecimal amount;
    }
}
