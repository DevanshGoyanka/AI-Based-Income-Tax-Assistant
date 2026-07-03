package com.itr.domain.ruleengine;

import com.itr.domain.common.IndianAmount;

/**
 * RoundingRules - Sections 288A and 288B rounding per Document 2 §D.
 * Section 288A: Total Income rounded to nearest ₹10 (drop paise).
 * Section 288B: Tax payable rounded to nearest ₹10 (drop paise).
 */
public class RoundingRules {
    
    /**
     * Section 288A: Round Total Income to nearest ₹10.
     * Drop paise, round rupees to nearest multiple of 10.
     */
    public static IndianAmount roundTotalIncome(IndianAmount amount) {
        if (amount == null) return IndianAmount.ZERO;
        
        long rupees = amount.toRupees();
        long remainder = rupees % 10;
        
        if (remainder < 5) {
            return IndianAmount.fromRupees(rupees - remainder);
        } else {
            return IndianAmount.fromRupees(rupees + (10 - remainder));
        }
    }
    
    /**
     * Section 288B: Round tax payable to nearest ₹10.
     * Drop paise, round rupees to nearest multiple of 10.
     */
    public static IndianAmount roundTaxPayable(IndianAmount amount) {
        if (amount == null) return IndianAmount.ZERO;
        
        long rupees = amount.toRupees();
        long remainder = rupees % 10;
        
        if (remainder < 5) {
            return IndianAmount.fromRupees(rupees - remainder);
        } else {
            return IndianAmount.fromRupees(rupees + (10 - remainder));
        }
    }
}
