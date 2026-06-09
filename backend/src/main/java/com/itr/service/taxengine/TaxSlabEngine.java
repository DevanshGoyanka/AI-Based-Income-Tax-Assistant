package com.itr.service.taxengine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tax Slab Engine - Handles all tax slab calculations for AY 2025-26 and AY 2026-27
 * Supports: Old Regime, New Regime (both assessment years)
 * Age categories: Regular (<60), Senior Citizen (60-79), Super Senior Citizen (80+)
 */
@Slf4j
@Service
public class TaxSlabEngine {

    // Assessment Year constants
    public static final String AY_2025_26 = "2025-26";
    public static final String AY_2026_27 = "2026-27";
    
    // Regime constants
    public static final String OLD_REGIME = "OLD";
    public static final String NEW_REGIME = "NEW";
    
    // Age category constants
    public static final String AGE_REGULAR = "REGULAR";
    public static final String AGE_SENIOR = "SENIOR";
    public static final String AGE_SUPER_SENIOR = "SUPER_SENIOR";

    /**
     * Calculate tax on normal income (excluding special rate income)
     */
    public BigDecimal calculateTaxOnNormalIncome(
            BigDecimal taxableIncome,
            String regime,
            String assessmentYear,
            String ageCategory,
            boolean isHUF) {
        
        if (taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // HUF has no senior citizen benefit - always use regular slabs
        if (isHUF) {
            ageCategory = AGE_REGULAR;
        }

        if (OLD_REGIME.equals(regime)) {
            return calculateOldRegimeTax(taxableIncome, ageCategory);
        } else if (NEW_REGIME.equals(regime)) {
            if (AY_2025_26.equals(assessmentYear)) {
                return calculateNewRegimeTax_AY2025_26(taxableIncome);
            } else if (AY_2026_27.equals(assessmentYear)) {
                return calculateNewRegimeTax_AY2026_27(taxableIncome);
            }
        }

        throw new IllegalArgumentException("Invalid regime or assessment year");
    }

    /**
     * Old Regime Tax Calculation (Same for both AYs)
     * Regular: 0-2.5L (0%), 2.5L-5L (5%), 5L-10L (20%), >10L (30%)
     * Senior: 0-3L (0%), 3L-5L (5%), 5L-10L (20%), >10L (30%)
     * Super Senior: 0-5L (0%), 5L-10L (20%), >10L (30%)
     */
    private BigDecimal calculateOldRegimeTax(BigDecimal income, String ageCategory) {
        BigDecimal tax = BigDecimal.ZERO;

        if (AGE_SUPER_SENIOR.equals(ageCategory)) {
            // Super Senior Citizen (80+)
            if (income.compareTo(new BigDecimal("500000")) <= 0) {
                return BigDecimal.ZERO;
            }
            tax = tax.add(calculateSlabTax(income, new BigDecimal("500000"), new BigDecimal("1000000"), new BigDecimal("0.20")));
            tax = tax.add(calculateSlabTax(income, new BigDecimal("1000000"), null, new BigDecimal("0.30")));
            
        } else if (AGE_SENIOR.equals(ageCategory)) {
            // Senior Citizen (60-79)
            if (income.compareTo(new BigDecimal("300000")) <= 0) {
                return BigDecimal.ZERO;
            }
            tax = tax.add(calculateSlabTax(income, new BigDecimal("300000"), new BigDecimal("500000"), new BigDecimal("0.05")));
            tax = tax.add(calculateSlabTax(income, new BigDecimal("500000"), new BigDecimal("1000000"), new BigDecimal("0.20")));
            tax = tax.add(calculateSlabTax(income, new BigDecimal("1000000"), null, new BigDecimal("0.30")));
            
        } else {
            // Regular (<60)
            if (income.compareTo(new BigDecimal("250000")) <= 0) {
                return BigDecimal.ZERO;
            }
            tax = tax.add(calculateSlabTax(income, new BigDecimal("250000"), new BigDecimal("500000"), new BigDecimal("0.05")));
            tax = tax.add(calculateSlabTax(income, new BigDecimal("500000"), new BigDecimal("1000000"), new BigDecimal("0.20")));
            tax = tax.add(calculateSlabTax(income, new BigDecimal("1000000"), null, new BigDecimal("0.30")));
        }

        return tax.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * New Regime Tax Calculation - AY 2025-26 (Finance Act 2024)
     * 0-3L (0%), 3L-7L (5%), 7L-10L (10%), 10L-12L (15%), 12L-15L (20%), >15L (30%)
     * Rebate 87A: Income ≤ 7L → Full tax rebate (max ₹25,000)
     */
    private BigDecimal calculateNewRegimeTax_AY2025_26(BigDecimal income) {
        BigDecimal tax = BigDecimal.ZERO;

        if (income.compareTo(new BigDecimal("300000")) <= 0) {
            return BigDecimal.ZERO;
        }

        tax = tax.add(calculateSlabTax(income, new BigDecimal("300000"), new BigDecimal("700000"), new BigDecimal("0.05")));
        tax = tax.add(calculateSlabTax(income, new BigDecimal("700000"), new BigDecimal("1000000"), new BigDecimal("0.10")));
        tax = tax.add(calculateSlabTax(income, new BigDecimal("1000000"), new BigDecimal("1200000"), new BigDecimal("0.15")));
        tax = tax.add(calculateSlabTax(income, new BigDecimal("1200000"), new BigDecimal("1500000"), new BigDecimal("0.20")));
        tax = tax.add(calculateSlabTax(income, new BigDecimal("1500000"), null, new BigDecimal("0.30")));

        return tax.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * New Regime Tax Calculation - AY 2026-27
     * 0-4L (0%), 4L-8L (5%), 8L-12L (10%), 12L-16L (15%), 16L-20L (20%), 20L-24L (25%), >24L (30%)
     */
    private BigDecimal calculateNewRegimeTax_AY2026_27(BigDecimal income) {
        BigDecimal tax = BigDecimal.ZERO;

        if (income.compareTo(new BigDecimal("400000")) <= 0) {
            return BigDecimal.ZERO;
        }

        tax = tax.add(calculateSlabTax(income, new BigDecimal("400000"), new BigDecimal("800000"), new BigDecimal("0.05")));
        tax = tax.add(calculateSlabTax(income, new BigDecimal("800000"), new BigDecimal("1200000"), new BigDecimal("0.10")));
        tax = tax.add(calculateSlabTax(income, new BigDecimal("1200000"), new BigDecimal("1600000"), new BigDecimal("0.15")));
        tax = tax.add(calculateSlabTax(income, new BigDecimal("1600000"), new BigDecimal("2000000"), new BigDecimal("0.20")));
        tax = tax.add(calculateSlabTax(income, new BigDecimal("2000000"), new BigDecimal("2400000"), new BigDecimal("0.25")));
        tax = tax.add(calculateSlabTax(income, new BigDecimal("2400000"), null, new BigDecimal("0.30")));

        return tax.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Helper method to calculate tax for a specific slab
     * @param income Total income
     * @param slabStart Start of slab (inclusive)
     * @param slabEnd End of slab (exclusive), null for last slab
     * @param rate Tax rate for this slab
     */
    private BigDecimal calculateSlabTax(BigDecimal income, BigDecimal slabStart, BigDecimal slabEnd, BigDecimal rate) {
        if (income.compareTo(slabStart) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal taxableInSlab;
        if (slabEnd == null) {
            // Last slab - no upper limit
            taxableInSlab = income.subtract(slabStart);
        } else {
            if (income.compareTo(slabEnd) <= 0) {
                taxableInSlab = income.subtract(slabStart);
            } else {
                taxableInSlab = slabEnd.subtract(slabStart);
            }
        }

        return taxableInSlab.multiply(rate);
    }

    /**
     * Determine age category from age
     */
    public String determineAgeCategory(int age) {
        if (age >= 80) {
            return AGE_SUPER_SENIOR;
        } else if (age >= 60) {
            return AGE_SENIOR;
        } else {
            return AGE_REGULAR;
        }
    }

    /**
     * Get basic exemption limit based on regime, AY, and age
     */
    public BigDecimal getBasicExemptionLimit(String regime, String assessmentYear, String ageCategory, boolean isHUF) {
        if (isHUF) {
            ageCategory = AGE_REGULAR;
        }

        if (OLD_REGIME.equals(regime)) {
            if (AGE_SUPER_SENIOR.equals(ageCategory)) {
                return new BigDecimal("500000");
            } else if (AGE_SENIOR.equals(ageCategory)) {
                return new BigDecimal("300000");
            } else {
                return new BigDecimal("250000");
            }
        } else if (NEW_REGIME.equals(regime)) {
            if (AY_2025_26.equals(assessmentYear)) {
                return new BigDecimal("300000"); // correct: 3L for AY 2025-26
            } else if (AY_2026_27.equals(assessmentYear)) {
                return new BigDecimal("400000"); // correct: 4L for AY 2026-27
            }
        }

        return new BigDecimal("250000"); // Default
    }
    
    /**
     * Standard Deduction u/s 16(ia) - AY 2025-26
     * Both old and new regime: ₹75,000 for salaried/pensioners
     */
    public static final BigDecimal STANDARD_DEDUCTION_AY2526 = new BigDecimal("75000");
    
    /**
     * Family Pension Deduction u/s 57(iia)
     * Lower of ₹15,000 or 1/3rd of family pension
     */
    public BigDecimal calculateFamilyPensionDeduction(BigDecimal familyPensionAmount) {
        BigDecimal oneThird = familyPensionAmount.divide(new BigDecimal("3"), 0, RoundingMode.DOWN);
        return oneThird.min(new BigDecimal("15000"));
    }
}
