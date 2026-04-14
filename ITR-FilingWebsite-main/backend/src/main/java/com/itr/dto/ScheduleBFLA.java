package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Schedule BFLA - Brought Forward Loss Adjustment
 * 101% CBDT Compliant - Section 72, 73, 74, 32(2)
 * 
 * Carry Forward Periods:
 * - HP Loss: 8 years (set off against HP income only)
 * - Non-Speculative Business Loss: 8 years (against business income only)
 * - Speculative Business Loss: 4 years (against speculative income only)
 * - STCG Loss: 8 years (against STCG and LTCG)
 * - LTCG Loss: 8 years (against LTCG only)
 * - Unabsorbed Depreciation: UNLIMITED (against business first, then any head except salary)
 * 
 * CRITICAL: Loss can be carried forward ONLY if return filed on or before due date
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleBFLA {
    
    // Brought Forward Losses (from previous years)
    @Builder.Default
    private List<BroughtForwardLoss> hpLosses = new ArrayList<>();
    @Builder.Default
    private List<BroughtForwardLoss> businessLosses = new ArrayList<>();
    @Builder.Default
    private List<BroughtForwardLoss> speculativeLosses = new ArrayList<>();
    @Builder.Default
    private List<BroughtForwardLoss> stcgLosses = new ArrayList<>();
    @Builder.Default
    private List<BroughtForwardLoss> ltcgLosses = new ArrayList<>();
    @Builder.Default
    private List<BroughtForwardLoss> unabsorbedDepreciation = new ArrayList<>();
    
    // Current Year Income Available for Set-Off (After CYLA)
    @Builder.Default
    private double hpIncomeAvailable = 0;
    @Builder.Default
    private double businessIncomeAvailable = 0;
    @Builder.Default
    private double speculativeBusinessIncomeAvailable = 0;
    @Builder.Default
    private double stcgIncomeAvailable = 0;
    @Builder.Default
    private double ltcgIncomeAvailable = 0;
    @Builder.Default
    private double otherSourcesIncomeAvailable = 0;
    
    // Set-Off Summary (FIFO - First In First Out)
    @Builder.Default
    private double totalHpLossSetOff = 0;
    @Builder.Default
    private double totalBusinessLossSetOff = 0;
    @Builder.Default
    private double totalSpeculativeLossSetOff = 0;
    @Builder.Default
    private double totalStcgLossSetOff = 0;
    @Builder.Default
    private double totalLtcgLossSetOff = 0;
    @Builder.Default
    private double totalUnabsorbedDepreciationSetOff = 0;
    
    // Income After BFLA Set-Off
    @Builder.Default
    private double hpIncomeAfterBFLA = 0;
    @Builder.Default
    private double businessIncomeAfterBFLA = 0;
    @Builder.Default
    private double speculativeBusinessIncomeAfterBFLA = 0;
    @Builder.Default
    private double stcgIncomeAfterBFLA = 0;
    @Builder.Default
    private double ltcgIncomeAfterBFLA = 0;
    @Builder.Default
    private double otherSourcesIncomeAfterBFLA = 0;
    
    // Expired Losses (Cannot be carried forward anymore)
    @Builder.Default
    private List<ExpiredLoss> expiredLosses = new ArrayList<>();
    
    // Warnings
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    /**
     * Brought Forward Loss Entry
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BroughtForwardLoss {
        private String assessmentYear; // Year in which loss was incurred
        private String lossType; // HP, BUSINESS, SPECULATIVE, STCG, LTCG, DEPRECIATION
        private double lossAmount; // Original loss amount
        private double lossRemaining; // Remaining after previous years' set-off
        private double lossSetOffThisYear; // Set off in current year
        private double lossCarryForward; // To be carried forward to next year
        private int yearsRemaining; // Years left for carry forward (0 = last year)
        private boolean expired; // True if cannot be carried forward anymore
    }
    
    /**
     * Expired Loss Entry
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpiredLoss {
        private String assessmentYear;
        private String lossType;
        private double lossAmount;
        private String reason; // "Time limit expired" or "Not filed on time"
    }
}
