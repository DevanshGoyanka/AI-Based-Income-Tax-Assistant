package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Schedule CYLA - Current Year Loss Adjustment
 * 101% CBDT Compliant - Section 70, 71, 72, 73, 74
 * 
 * Order of Set-Off (CRITICAL):
 * 1. HP Loss → Set off against: Salary, Other Sources, CG (all types), Business income. Cap: ₹2,00,000
 * 2. Non-speculative business loss → Set off against: All heads EXCEPT salary
 * 3. Speculative business loss → ONLY against speculative business income
 * 4. STCG loss → Set off against STCG and LTCG of any type
 * 5. LTCG loss → Set off against LTCG only
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCYLA {
    
    // Current Year Losses (Before Set-Off)
    @Builder.Default
    private double hpLoss = 0;
    @Builder.Default
    private double nonSpeculativeBusinessLoss = 0;
    @Builder.Default
    private double speculativeBusinessLoss = 0;
    @Builder.Default
    private double stcgLoss = 0;
    @Builder.Default
    private double ltcgLoss = 0;
    
    // Current Year Positive Incomes (Available for Set-Off)
    @Builder.Default
    private double salaryIncome = 0;
    @Builder.Default
    private double hpIncome = 0;
    @Builder.Default
    private double businessIncome = 0;
    @Builder.Default
    private double speculativeBusinessIncome = 0;
    @Builder.Default
    private double stcgIncome = 0;
    @Builder.Default
    private double ltcgIncome = 0;
    @Builder.Default
    private double otherSourcesIncome = 0;
    
    // Step 1: HP Loss Set-Off (Max ₹2L inter-head)
    @Builder.Default
    private double hpLossSetOffAgainstSalary = 0;
    @Builder.Default
    private double hpLossSetOffAgainstOtherSources = 0;
    @Builder.Default
    private double hpLossSetOffAgainstBusiness = 0;
    @Builder.Default
    private double hpLossSetOffAgainstSTCG = 0;
    @Builder.Default
    private double hpLossSetOffAgainstLTCG = 0;
    @Builder.Default
    private double totalHpLossSetOff = 0;
    @Builder.Default
    private double hpLossUnabsorbed = 0;
    
    // Step 2: Non-Speculative Business Loss Set-Off (Cannot touch salary)
    @Builder.Default
    private double businessLossSetOffAgainstHP = 0;
    @Builder.Default
    private double businessLossSetOffAgainstOtherSources = 0;
    @Builder.Default
    private double businessLossSetOffAgainstSTCG = 0;
    @Builder.Default
    private double businessLossSetOffAgainstLTCG = 0;
    @Builder.Default
    private double totalBusinessLossSetOff = 0;
    @Builder.Default
    private double businessLossUnabsorbed = 0;
    
    // Step 3: Speculative Business Loss Set-Off (Only against speculative income)
    @Builder.Default
    private double speculativeLossSetOff = 0;
    @Builder.Default
    private double speculativeLossUnabsorbed = 0;
    
    // Step 4: STCG Loss Set-Off (Against STCG first, then LTCG)
    @Builder.Default
    private double stcgLossSetOffAgainstSTCG = 0;
    @Builder.Default
    private double stcgLossSetOffAgainstLTCG = 0;
    @Builder.Default
    private double totalStcgLossSetOff = 0;
    @Builder.Default
    private double stcgLossUnabsorbed = 0;
    
    // Step 5: LTCG Loss Set-Off (Only against LTCG)
    @Builder.Default
    private double ltcgLossSetOff = 0;
    @Builder.Default
    private double ltcgLossUnabsorbed = 0;
    
    // Final Income After Set-Off
    @Builder.Default
    private double salaryIncomeAfterSetOff = 0;
    @Builder.Default
    private double hpIncomeAfterSetOff = 0;
    @Builder.Default
    private double businessIncomeAfterSetOff = 0;
    @Builder.Default
    private double speculativeBusinessIncomeAfterSetOff = 0;
    @Builder.Default
    private double stcgIncomeAfterSetOff = 0;
    @Builder.Default
    private double ltcgIncomeAfterSetOff = 0;
    @Builder.Default
    private double otherSourcesIncomeAfterSetOff = 0;
    
    // Gross Total Income After CYLA
    @Builder.Default
    private double grossTotalIncomeAfterCYLA = 0;
    
    // Warnings
    @Builder.Default
    private String hpLossWarning = null;
    @Builder.Default
    private String businessLossWarning = null;
    @Builder.Default
    private String speculativeLossWarning = null;
}
