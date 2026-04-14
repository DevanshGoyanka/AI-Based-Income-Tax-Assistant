package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Schedule CFL - Carry Forward Losses
 * 101% CBDT Compliant
 * 
 * Tracks losses to be carried forward to future assessment years
 * 
 * CRITICAL: Loss can be carried forward ONLY if return filed on or before due date
 * Belated return → NO carry forward (except unabsorbed depreciation and BF losses from prior years)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCFL {
    
    // Assessment Year for which this CFL is being prepared
    private String currentAssessmentYear;
    
    // Filing Status
    private boolean filedOnTime; // True if filed on or before due date
    private String filingDate; // Actual filing date
    private String dueDate; // Due date for this return
    
    // Losses to Carry Forward
    @Builder.Default
    private List<LossCarryForward> lossesToCarryForward = new ArrayList<>();
    
    // Summary
    @Builder.Default
    private double totalHpLossCarryForward = 0;
    @Builder.Default
    private double totalBusinessLossCarryForward = 0;
    @Builder.Default
    private double totalSpeculativeLossCarryForward = 0;
    @Builder.Default
    private double totalStcgLossCarryForward = 0;
    @Builder.Default
    private double totalLtcgLossCarryForward = 0;
    @Builder.Default
    private double totalUnabsorbedDepreciationCarryForward = 0;
    
    // Warnings
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    /**
     * Loss Carry Forward Entry
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LossCarryForward {
        private String lossType; // HP, BUSINESS, SPECULATIVE, STCG, LTCG, DEPRECIATION
        private String assessmentYearOfLoss; // Year in which loss was incurred
        private double lossAmount; // Amount to carry forward
        private int carryForwardPeriod; // Years allowed (8, 4, or UNLIMITED)
        private int yearsRemaining; // Years left for carry forward
        private String expiryAssessmentYear; // Year after which loss expires
        private String setOffAgainst; // Which income heads can this be set off against
        private boolean eligible; // True if eligible for carry forward
        private String ineligibilityReason; // Reason if not eligible
    }
}
