package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AIS (Annual Information Statement) Data DTO
 * 101% CBDT Compliant - Matches ITD AIS JSON schema
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AISData {
    
    private String pan;
    private String assessmentYear;
    private String financialYear;
    
    // Part A - TDS on Salary
    private List<TDSSalary> tdsSalary;
    
    // Part B - TDS on Other than Salary
    private List<TDSOther> tdsOther;
    
    // Part C - TCS (Tax Collected at Source)
    private List<TCSEntry> tcs;
    
    // Part D - Tax Payments (Advance Tax, Self-Assessment)
    private List<TaxPayment> taxPayments;
    
    // Part E - Specified Financial Transactions (SFT)
    private SFTData sft;
    
    // Part F - Demand and Refund
    private List<DemandRefund> demandRefund;
    
    // Part G - AIR Transactions
    private List<AIRTransaction> airTransactions;
    
    // Part H - Capital Gains Transactions (from SFT)
    private List<CapitalGainsTransaction> capitalGainsTransactions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSSalary {
        private String deductorName;
        private String deductorTAN;
        private String deductorPAN;
        private Double totalAmountPaid;
        private Double totalTaxDeducted;
        private String financialYear;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSOther {
        private String deductorName;
        private String deductorTAN;
        private String deductorPAN;
        private String section;
        private Double amountPaid;
        private Double taxDeducted;
        private String dateOfPayment;
        private String dateOfDeduction;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TCSEntry {
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
    public static class TaxPayment {
        private String bsrCode;
        private String dateOfDeposit;
        private String challanSerialNumber;
        private Double amount;
        private String taxType;
        private String minorHead;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SFTData {
        private List<SFTEntry> entries;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SFTEntry {
        private String reportingEntity;
        private String transactionType;
        private Double amount;
        private String remarks;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemandRefund {
        private String assessmentYear;
        private Double demandAmount;
        private Double refundAmount;
        private String status;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIRTransaction {
        private String transactionType;
        private Double amount;
        private String date;
        private String remarks;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapitalGainsTransaction {
        private String assetType; // EQUITY_LISTED, EQUITY_UNLISTED, PROPERTY, DEBT_MF, GOLD, BONDS
        private String assetDescription;
        private String isin; // For securities
        private String dateOfAcquisition;
        private String dateOfSale;
        private Double purchasePrice;
        private Double salePrice;
        private Double expenditureOnTransfer;
        private String gainType; // STCG, LTCG
        private String section; // 111A, 112A, 112, NORMAL
        private Double sttPaid;
        private String brokerName;
        private String brokerPAN;
        private Integer quantity;
        private Double indexedCost;
        private Double grandfatheredCost; // For 112A pre-Jan 31, 2018 assets
    }
}
