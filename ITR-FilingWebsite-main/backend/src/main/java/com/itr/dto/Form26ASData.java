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
 * Form 26AS (TRACES) data structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Form26ASData {
    private List<TDSEntry26AS> partIEntries = new ArrayList<>();
    private List<PropertyTDS26AS> partIVEntries = new ArrayList<>();
    private List<RefundEntry26AS> partVIIEntries = new ArrayList<>();
    private List<DefaultEntry26AS> partXEntries = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TDSEntry26AS {
        private String deductorName;
        private String tan;
        private String section;
        private BigDecimal amountPaid;
        private BigDecimal taxDeducted;
        private BigDecimal taxDeposited;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyTDS26AS {
        private String acknowledgementNo;
        private String buyerName;
        private String buyerPAN;
        private LocalDate transactionDate;
        private BigDecimal transactionAmount;
        private BigDecimal tdsDeposited;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundEntry26AS {
        private String assessmentYear;
        private BigDecimal refundAmount;
        private BigDecimal interestAmount;
        private LocalDate refundDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefaultEntry26AS {
        private String deductorName;
        private String tan;
        private String section;
        private BigDecimal amount;
        private String remarks;
    }
}
