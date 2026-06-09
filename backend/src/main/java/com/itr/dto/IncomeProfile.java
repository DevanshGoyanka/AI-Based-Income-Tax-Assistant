package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Income Profile for ITR Form Classification.
 * Contains all income sources and taxpayer characteristics needed to determine correct ITR form.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeProfile {
    
    // Basic Info
    private String pan;
    private String residentialStatus;  // RES / RNOR / NR
    
    // Income Amounts
    private double salaryIncome;
    private double housePropertyIncome;
    private double businessIncome;
    private double capitalGainsIncome;
    private double otherSourcesIncome;
    private double agriculturalIncome;
    private long totalIncome;
    
    // Income Characteristics
    @Builder.Default
    private boolean hasCapitalGains = false;
    @Builder.Default
    private boolean hasBusinessIncome = false;
    @Builder.Default
    private boolean hasProfessionalIncome = false;
    @Builder.Default
    private boolean hasMultipleProperties = false;
    @Builder.Default
    private boolean hasForeignIncome = false;
    @Builder.Default
    private boolean hasForeignAssets = false;
    
    // Taxpayer Characteristics
    @Builder.Default
    private boolean isDirector = false;
    @Builder.Default
    private boolean hasUnlistedShares = false;
    @Builder.Default
    private boolean hasLotteryIncome = false;
    @Builder.Default
    private boolean hasRaceHorseIncome = false;
    
    // Presumptive Income Eligibility
    @Builder.Default
    private boolean eligibleFor44AD = false;  // Business presumptive
    @Builder.Default
    private boolean eligibleFor44ADA = false; // Professional presumptive
    @Builder.Default
    private boolean eligibleFor44AE = false;  // Goods carriage
    
    // Business Details
    private Double businessTurnover;
    private Double professionalReceipts;
    private Integer numberOfGoodsVehicles;
    
    // Additional Flags
    @Builder.Default
    private boolean hasCarriedForwardLosses = false;
    @Builder.Default
    private boolean claimingForeignTaxCredit = false;
}
