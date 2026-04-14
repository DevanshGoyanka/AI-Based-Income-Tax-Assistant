package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Form 26AS Data DTO
 * 101% CBDT Compliant - Tax Credit Statement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Form26ASData {
    
    private String pan;
    private String assessmentYear;
    private String financialYear;
    
    // Part A - TDS on Salary
    private List<PartA> partA;
    
    // Part B - TDS on Other than Salary
    private List<PartB> partB;
    
    // Part C - TCS
    private List<PartC> partC;
    
    // Part D - Advance Tax and Self-Assessment Tax
    private List<PartD> partD;
    
    // Part E - Refund
    private List<PartE> partE;
    
    // Part F - AIR Transactions
    private List<PartF> partF;
    
    // Part G - TDS Defaults
    private List<PartG> partG;
    
    // Part H - Capital Gains Transactions
    private List<CapitalGainsTransaction> capitalGainsTransactions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartA {
        private String deductorName;
        private String deductorTAN;
        private Double totalAmountPaid;
        private Double totalTaxDeducted;
        private Double totalTaxDeposited;
        private List<QuarterlyBreakup> quarterlyBreakup;
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
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartB {
        private String deductorName;
        private String deductorTAN;
        private String section;
        private String transactionDate;
        private Double amountPaid;
        private Double taxDeducted;
        private String dateOfDeposit;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartC {
        private String collectorName;
        private String collectorTAN;
        private String section;
        private Double amountPaid;
        private Double taxCollected;
        private String dateOfCollection;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartD {
        private String bsrCode;
        private String dateOfDeposit;
        private String challanSerialNumber;
        private Double amount;
        private String taxType;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartE {
        private String assessmentYear;
        private Double refundAmount;
        private String dateOfReceipt;
        private String mode;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartF {
        private String transactionType;
        private Double amount;
        private String date;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartG {
        private String deductorTAN;
        private String section;
        private Double amount;
        private String defaultType;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapitalGainsTransaction {
        private String assetType;
        private String assetDescription;
        private String isin;
        private String dateOfAcquisition;
        private String dateOfSale;
        private Double purchasePrice;
        private Double salePrice;
        private Double expenditureOnTransfer;
        private String gainType;
        private String section;
        private Double sttPaid;
        private String brokerName;
        private String brokerPAN;
        private Integer quantity;
    }
}
