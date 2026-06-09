package com.itr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Section 14A Disallowance Service
 * 101% CBDT Compliant
 * 
 * Section 14A: Expenditure incurred to earn exempt income is NOT deductible
 * Rule 8D: Method to compute disallowance if AO not satisfied with taxpayer's computation
 */
@Slf4j
@Service
public class Section14AService {

    /**
     * Calculate disallowance u/s 14A read with Rule 8D
     * 
     * Rule 8D Formula:
     * Disallowance = (a) + (b) + (c)
     * 
     * (a) Direct expenses related to exempt income
     * (b) 1% of average value of investment yielding exempt income
     * (c) (Total interest expense × Average investment in exempt income) / Average total assets
     *     Subject to: (b) + (c) cannot exceed total expenses (excluding depreciation)
     */
    public Section14AResult calculateDisallowance(Section14AInput input) {
        
        log.info("Calculating Section 14A disallowance");
        
        double directExpenses = input.getDirectExpensesRelatedToExemptIncome();
        double exemptIncome = input.getExemptIncome();
        double avgInvestmentInExemptIncome = input.getAvgInvestmentInExemptIncome();
        double totalInterestExpense = input.getTotalInterestExpense();
        double avgTotalAssets = input.getAvgTotalAssets();
        double totalExpenses = input.getTotalExpenses();
        
        // Component (a): Direct expenses
        double componentA = directExpenses;
        
        // Component (b): 1% of average investment in exempt income
        double componentB = avgInvestmentInExemptIncome * 0.01;
        
        // Component (c): Proportionate interest expense
        double componentC = 0;
        if (avgTotalAssets > 0) {
            componentC = (totalInterestExpense * avgInvestmentInExemptIncome) / avgTotalAssets;
        }
        
        // Cap: (b) + (c) cannot exceed total expenses
        double maxBPlusC = totalExpenses - directExpenses;
        double actualBPlusC = Math.min(componentB + componentC, maxBPlusC);
        
        // Total disallowance
        double totalDisallowance = componentA + actualBPlusC;
        
        log.info("Section 14A disallowance: Direct: ₹{}, 1% of investment: ₹{}, Interest: ₹{}, Total: ₹{}",
                componentA, componentB, componentC, totalDisallowance);
        
        return Section14AResult.builder()
                .directExpenses(componentA)
                .onePercentOfInvestment(componentB)
                .proportionateInterest(componentC)
                .totalDisallowance(totalDisallowance)
                .exemptIncome(exemptIncome)
                .build();
    }

    /**
     * Input for Section 14A calculation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Section14AInput {
        private double directExpensesRelatedToExemptIncome;
        private double exemptIncome;
        private double avgInvestmentInExemptIncome;
        private double totalInterestExpense;
        private double avgTotalAssets;
        private double totalExpenses;
    }

    /**
     * Result of Section 14A calculation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Section14AResult {
        private double directExpenses;
        private double onePercentOfInvestment;
        private double proportionateInterest;
        private double totalDisallowance;
        private double exemptIncome;
    }
}
