package com.itr.dto.itd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Form 26AS Data DTO - Matches REAL ITD JSON structure
 * Updated to match actual nested structure from ITD portal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Form26ASDataNew {
    
    private String pan;
    private String assessmentYear;
    private String financialYear;
    
    // Part A - TDS on Salary
    private PartA partA;
    
    // Part B - TDS on Other than Salary
    private PartB partB;
    
    // Part C - TCS
    private PartC partC;
    
    // Part D - Advance Tax and Self-Assessment Tax
    private PartD partD;
    
    // Part E - Refund
    private PartE partE;
    
    // Part F - AIR Transactions
    private PartF partF;
    
    // Part G - TDS Defaults
    private PartG partG;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartA {
        private List<TDSSalaryEntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSSalaryEntry {
        private EmployerOrDeductorOrCollectDetl employerOrDeductorOrCollectDetl;
        private TaxDeductCreditDtls taxDeductCreditDtls;
        private List<QuarterlyBreakup> quarterlyBreakup;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployerOrDeductorOrCollectDetl {
        private String tan;
        private String employerOrDeductorOrCollecterName;
        private String branchOrDivisionName;
        private String address;
        private String pinCode;
        private String stateCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxDeductCreditDtls {
        private Double taxClaimedOwnHands;
        private Double taxDeductedOwnHands;
        private Double taxDepositedOwnHands;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuarterlyBreakup {
        private String quarter;
        private String receiptNumber;
        private Double amountPaid;
        private Double taxDeducted;
        private Double taxDeposited;
        private String dateOfDeposit;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartB {
        private List<TDSOtherEntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSOtherEntry {
        private EmployerOrDeductorOrCollectDetl employerOrDeductorOrCollectDetl;
        private TaxDeductCreditDtls taxDeductCreditDtls;
        private String sectionCode;
        private String transactionDate;
        private Double grossAmount;
        private String headOfIncome;
        private String certificateNumber;
        private String dateOfDeduction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartC {
        private List<TCSEntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TCSEntry {
        private EmployerOrDeductorOrCollectDetl employerOrDeductorOrCollectDetl;
        private String sectionCode;
        private Double amountPaid;
        private Double taxCollected;
        private String dateOfCollection;
        private String certificateNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartD {
        private List<TaxPaymentEntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxPaymentEntry {
        private String bsrCode;
        private NameOfBankAndBranch nameOfBankAndBranch;
        private String dateOfDeposit;
        private String challanSerialNumber;
        private Long serialNoOfChallan;
        private Double amount;
        private String taxType;
        private String minorHead;
        private String receiptNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NameOfBankAndBranch {
        private String nameOfBank;
        private String nameOfBranch;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartE {
        private List<RefundEntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundEntry {
        private String assessmentYear;
        private Double refundAmount;
        private String dateOfReceipt;
        private String mode;
        private String referenceNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartF {
        private List<AIREntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIREntry {
        private String transactionType;
        private Double amount;
        private String date;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartG {
        private List<TDSDefaultEntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSDefaultEntry {
        private String deductorTAN;
        private String deductorName;
        private String section;
        private Double amount;
        private String defaultType;
        private String reason;
    }
}
