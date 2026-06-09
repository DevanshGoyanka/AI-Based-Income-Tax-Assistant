package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Business Income Calculation
 * Frontend only displays these values - all calculations done in backend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessIncomeResponse {
    
    private String scheme;
    private String assessmentYear;
    
    // Input values
    private Double grossTurnover;
    private Double declaredIncome;
    private Double netProfitPL;
    
    // Calculated values
    private Double taxableIncome;
    private Double adjustedTaxableIncome;
    private Double presumptiveRate;
    private String incomeType;
    private Boolean isLoss;
    private Double businessLoss;
    
    // Loss set-off info
    private LossSetOffInfo lossSetOffInfo;
    
    // Compliance notes for audit trail
    private List<String> complianceNotes;
    
    // Timestamp
    private String timestamp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LossSetOffInfo {
        private Double businessLossBroughtForward;
        private Double lossSetOffAdjusted;
        private Double remainingLossCarryForward;
    }
}
