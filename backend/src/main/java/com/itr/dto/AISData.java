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
 * AIS (Annual Information Statement) Data DTO
 * Part 2: Comprehensive structure for AIS, 26AS, TIS import
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AISData {
    
    private String pan;
    private String assessmentYear;
    private String financialYear;
    
    // Part A: General Information
    private AISGeneralInfo generalInfo;
    
    // Part B1: TDS/TCS Information
    private AISPartB1Data partB1;
    
    // Part B2: Specified Financial Transactions
    private AISPartB2Data partB2;
    
    // Part B3: Tax Payments
    private List<TaxPaymentAIS> partB3;
    
    // Legacy fields for backward compatibility
    private List<TDSSalary> tdsSalary;
    private List<TDSOther> tdsOther;
    private List<TCSEntry> tcs;
    private List<TaxPayment> taxPayments;
    private SFTData sft;
    private List<DemandRefund> demandRefund;
    private List<AIRTransaction> airTransactions;
    private List<CapitalGainsTransaction> capitalGainsTransactions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AISGeneralInfo {
        private String pan;
        private String aadhaar;
        private String name;
        private LocalDate dob;
        private String mobile;
        private String email;
        private String address;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AISPartB1Data {
        private List<AISTDSEntry> tdsEntries = new ArrayList<>();
        private List<AISTransaction> tdsTransactions = new ArrayList<>();
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AISTDSEntry {
        private String section;
        private String deductorName;
        private String deductorTAN;
        private long totalAmountPaid;
        private long totalTDSDeducted;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AISTransaction {
        private String tsnId;
        private String quarter;
        private LocalDate transactionDate;
        private long amountPaid;
        private long tdsDeducted;
        private long tdsDeposited;
        private String status;
        private String section;
        private String deductorName;
        private String deductorTAN;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AISPartB2Data {
        private long dividendIncome;
        private List<SFTSaleEntry> securitiesSale = new ArrayList<>();
        private List<SFTSaleEntry> dividendEntries = new ArrayList<>();
        private long securitiesPurchaseAmount;
        private List<MFPurchase> mutualFundPurchase = new ArrayList<>();
        private long interestOnSecurities;

        // Enhanced fields for complete extraction
        private List<InterestEntry> savingsInterest;
        private List<InterestEntry> depositInterest;
        private List<SFTSaleEntry> propertySales;
        private List<SFTSaleEntry> mfSales;
        private List<DemandRefund> demandRefunds;
        private List<OtherInfoEntry> otherInfo;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestEntry {
        private String sourceName;
        private String sourceId;
        private long totalAmount;
        private int transactionCount;
        private String accountNumber;
        private String accountType;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SFTSaleEntry {
        private LocalDate transferDate;
        private String securityName;
        private String assetType;
        private BigDecimal quantity;
        private BigDecimal salePricePerUnit;
        private BigDecimal salesConsideration;
        private BigDecimal costOfAcquisition;
        private BigDecimal fmvPerUnit;
        private BigDecimal indexedCostOfAcquisition;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MFPurchase {
        private String amcName;
        private String clientId;
        private long totalPurchase;
        private long totalSales;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxPaymentAIS {
        private String financialYear;
        private String minorHead;
        private long taxAmount;
        private long surcharge;
        private long educationCess;
        private long totalAmount;
        private String bsrCode;
        private LocalDate depositDate;
        private String challanSerialNo;
    }
    
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
    public static class OtherInfoEntry {
        private String sourceName;
        private String sourceId;
        private String category;
        private String description;
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
        private Double indexedCost;
        private Double grandfatheredCost;
    }
}
