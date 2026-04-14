package com.itr.model;

/**
 * Result of comparing Old vs New tax regime.
 * Helps taxpayer choose the regime with lower tax liability.
 */
public class RegimeComparisonResult {
    
    public TaxRegime recommendedRegime;
    public long taxSavings;
    public String recommendation;
    
    public OldRegimeDetails oldRegime;
    public NewRegimeDetails newRegime;
    
    public static class OldRegimeDetails {
        public long grossTotalIncome;
        public long totalDeductions;
        public long totalIncome;
        public long taxBeforeRebate;
        public long rebate87A;
        public long taxAfterRebate;
        public long educationCess;
        public long totalTaxLiability;
        
        // Breakdown of deductions
        public long sec80CTotal;
        public long sec80D;
        public long sec80E;
        public long otherDeductions;
    }
    
    public static class NewRegimeDetails {
        public long grossTotalIncome;
        public long totalDeductions;      // Only 80CCD(2) + 80CCH
        public long totalIncome;
        public long taxBeforeRebate;
        public long rebate87A;
        public long taxAfterRebate;
        public long educationCess;
        public long totalTaxLiability;
    }
}
