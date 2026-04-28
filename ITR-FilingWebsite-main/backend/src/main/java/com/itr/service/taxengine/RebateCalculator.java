package com.itr.service.taxengine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rebate Calculator - Section 87A rebate computation
 * Old Regime: ₹5L threshold, ₹12,500 max rebate
 * New Regime AY 2025-26: ₹7L threshold, ₹25,000 max rebate
 * New Regime AY 2026-27: ₹12L threshold, ₹60,000 max rebate
 * CRITICAL: Rebate applies ONLY to normal income tax, NOT special rate income
 */
@Slf4j
@Service
public class RebateCalculator {

    // Old regime
    private static final BigDecimal OLD_REGIME_THRESHOLD = new BigDecimal("500000");
    private static final BigDecimal OLD_REGIME_MAX_REBATE = new BigDecimal("12500");

    // New regime AY 2025-26 (CORRECTED per Finance Act 2024)
    private static final BigDecimal NEW_2025_26_THRESHOLD = new BigDecimal("700000");
    private static final BigDecimal NEW_2025_26_MAX_REBATE = new BigDecimal("25000");

    // New regime AY 2026-27
    private static final BigDecimal NEW_2026_27_THRESHOLD = new BigDecimal("1200000");
    private static final BigDecimal NEW_2026_27_MAX_REBATE = new BigDecimal("60000");

    /**
     * Calculate 87A rebate
     * @param totalIncome Total income (for threshold check)
     * @param taxOnNormalIncome Tax on normal income ONLY (excludes special rate income)
     * @param regime OLD or NEW
     * @param assessmentYear AY 2025-26 or AY 2026-27
     * @return Rebate result with amount and eligibility
     */
    public RebateResult calculate87ARebate(
            BigDecimal totalIncome,
            BigDecimal taxOnNormalIncome,
            String regime,
            String assessmentYear) {

        BigDecimal threshold;
        BigDecimal maxRebate;

        // Determine threshold and max rebate based on regime and AY
        if (TaxSlabEngine.OLD_REGIME.equals(regime)) {
            threshold = OLD_REGIME_THRESHOLD;
            maxRebate = OLD_REGIME_MAX_REBATE;
        } else if (TaxSlabEngine.NEW_REGIME.equals(regime)) {
            if (TaxSlabEngine.AY_2025_26.equals(assessmentYear)) {
                threshold = NEW_2025_26_THRESHOLD;
                maxRebate = NEW_2025_26_MAX_REBATE;
            } else if (TaxSlabEngine.AY_2026_27.equals(assessmentYear)) {
                threshold = NEW_2026_27_THRESHOLD;
                maxRebate = NEW_2026_27_MAX_REBATE;
            } else {
                return buildNoRebateResult("Invalid assessment year");
            }
        } else {
            return buildNoRebateResult("Invalid regime");
        }

        // Check eligibility
        if (totalIncome.compareTo(threshold) > 0) {
            return buildNoRebateResult("Income exceeds threshold of ₹" + threshold);
        }

        // Rebate = min(tax on normal income, max rebate)
        BigDecimal rebateAmount = taxOnNormalIncome.min(maxRebate);

        return RebateResult.builder()
                .eligible(true)
                .rebateAmount(rebateAmount.setScale(0, RoundingMode.HALF_UP))
                .threshold(threshold)
                .maxRebate(maxRebate)
                .taxOnNormalIncome(taxOnNormalIncome)
                .reason("Eligible for 87A rebate")
                .build();
    }

    /**
     * Calculate marginal relief for ₹12L cliff (AY 2026-27 new regime only)
     * If income slightly exceeds ₹12L, tax cannot exceed (income - ₹12L)
     */
    public MarginalReliefResult calculate12LCliffRelief(
            BigDecimal totalIncome,
            BigDecimal taxBeforeRebate,
            String regime,
            String assessmentYear) {

        // Only applicable for new regime AY 2026-27
        if (!TaxSlabEngine.NEW_REGIME.equals(regime) || 
            !TaxSlabEngine.AY_2026_27.equals(assessmentYear)) {
            return MarginalReliefResult.builder()
                    .applicable(false)
                    .reliefAmount(BigDecimal.ZERO)
                    .finalTax(taxBeforeRebate)
                    .build();
        }

        // Only applicable if income > ₹12L but within marginal relief zone
        if (totalIncome.compareTo(NEW_2026_27_THRESHOLD) <= 0) {
            return MarginalReliefResult.builder()
                    .applicable(false)
                    .reliefAmount(BigDecimal.ZERO)
                    .finalTax(taxBeforeRebate)
                    .build();
        }

        // Marginal relief zone: approximately ₹12L to ₹12.75L
        BigDecimal incomeAbove12L = totalIncome.subtract(NEW_2026_27_THRESHOLD);
        
        // If tax exceeds income above ₹12L, cap it
        if (taxBeforeRebate.compareTo(incomeAbove12L) > 0) {
            BigDecimal reliefAmount = taxBeforeRebate.subtract(incomeAbove12L);
            
            return MarginalReliefResult.builder()
                    .applicable(true)
                    .reliefAmount(reliefAmount.setScale(0, RoundingMode.HALF_UP))
                    .finalTax(incomeAbove12L.setScale(0, RoundingMode.HALF_UP))
                    .incomeAboveThreshold(incomeAbove12L)
                    .build();
        }

        return MarginalReliefResult.builder()
                .applicable(false)
                .reliefAmount(BigDecimal.ZERO)
                .finalTax(taxBeforeRebate)
                .build();
    }

    private RebateResult buildNoRebateResult(String reason) {
        return RebateResult.builder()
                .eligible(false)
                .rebateAmount(BigDecimal.ZERO)
                .reason(reason)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RebateResult {
        private boolean eligible;
        private BigDecimal rebateAmount;
        private BigDecimal threshold;
        private BigDecimal maxRebate;
        private BigDecimal taxOnNormalIncome;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarginalReliefResult {
        private boolean applicable;
        private BigDecimal reliefAmount;
        private BigDecimal finalTax;
        private BigDecimal incomeAboveThreshold;
    }
}
