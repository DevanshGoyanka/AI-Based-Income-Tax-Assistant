package com.itr.dto.itd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * TIS (Tax Information Statement) Data DTO
 * TIS is similar to AIS but with additional tax computation details
 * TIS PDFs are password-protected with: {pan_lowercase}{ddmmyyyy}
 * 
 * Example: PAN=AAAAA1234A, DOB=21/01/1991 → Password: aaaaa1234a21011991
 * 
 * TIS = AIS + Tax Computation + Feedback mechanism
 * Use ITDPdfDecryptor utility to decrypt password-protected PDFs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TISData {
    
    private String pan;
    private String assessmentYear;
    private String financialYear;
    
    /**
     * If encrypted, store raw encrypted data
     */
    private String encryptedData;
    @Builder.Default
    private boolean isEncrypted = false;
    
    /**
     * Decrypted TIS data structure (similar to AIS but with additional fields)
     */
    private TISPartA partA; // TDS on Salary
    private TISPartB partB; // TDS on Other than Salary
    private TISPartC partC; // TCS
    private TISPartD partD; // Tax Payments
    private TISPartE partE; // SFT (Specified Financial Transactions)
    private TISPartF partF; // Demand and Refund
    private TISPartG partG; // AIR Transactions
    private TISPartH partH; // TDS Defaults (Mismatches)
    
    /**
     * TIS-specific: Taxpayer feedback on information
     */
    private List<TISFeedback> feedbacks;
    
    /**
     * TIS-specific: Computed tax liability based on information
     */
    private TISComputedTax computedTax;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TISPartA {
        private List<TDSSalaryEntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSSalaryEntry {
        private String deductorName;
        private String deductorTAN;
        private String deductorPAN;
        private Double totalAmountPaid;
        private Double totalTaxDeducted;
        private String financialYear;
        private List<QuarterlyDetail> quarterlyDetails;
        private String feedbackStatus; // ACCEPTED, REJECTED, PENDING
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuarterlyDetail {
        private String quarter;
        private String receiptNumber;
        private Double amountPaid;
        private Double taxDeducted;
        private String dateOfDeposit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TISPartB {
        private List<TDSOtherEntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSOtherEntry {
        private String deductorName;
        private String deductorTAN;
        private String section;
        private String transactionDate;
        private Double amountPaid;
        private Double taxDeducted;
        private String dateOfDeposit;
        private String feedbackStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TISPartC {
        private List<TCSEntry> entries;
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
        private String feedbackStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TISPartD {
        private List<TaxPaymentEntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxPaymentEntry {
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
    public static class TISPartE {
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
        private String feedbackStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TISPartF {
        private List<DemandRefundEntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemandRefundEntry {
        private String assessmentYear;
        private Double demandAmount;
        private Double refundAmount;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TISPartG {
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
    public static class TISPartH {
        private List<TDSDefaultEntry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSDefaultEntry {
        private String deductorTAN;
        private String section;
        private Double amount;
        private String defaultType;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TISFeedback {
        private String informationId;
        private String informationType; // TDS, TCS, SFT, etc.
        private String feedbackType; // ACCEPT, REJECT, MODIFY
        private String reason;
        private String submittedDate;
        private String status; // PENDING, PROCESSED, REJECTED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TISComputedTax {
        private Double totalIncome;
        private Double totalTaxDeducted;
        private Double totalTaxCollected;
        private Double totalTaxPaid;
        private Double estimatedTaxLiability;
        private Double balanceTaxPayable;
        private Double refundDue;
    }
}
