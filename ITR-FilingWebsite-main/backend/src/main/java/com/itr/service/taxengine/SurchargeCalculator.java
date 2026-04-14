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
 * Surcharge Calculator - Handles surcharge computation with marginal relief
 * 4-tier surcharge: 10% (₹50L), 15% (₹1Cr), 25% (₹2Cr), 37% (₹5Cr)
 * Special: 15% cap on surcharge for 111A/112A income
 * Marginal relief at all thresholds
 */
@Slf4j
@Service
public class SurchargeCalculator {

    // Surcharge thresholds
    private static final BigDecimal THRESHOLD_50L = new BigDecimal("5000000");
    private static final BigDecimal THRESHOLD_1CR = new BigDecimal("10000000");
    private static final BigDecimal THRESHOLD_2CR = new BigDecimal("20000000");
    private static final BigDecimal THRESHOLD_5CR = new BigDecimal("50000000");

    // Surcharge rates
    private static final BigDecimal RATE_10_PERCENT = new BigDecimal("0.10");
    private static final BigDecimal RATE_15_PERCENT = new BigDecimal("0.15");
    private static final BigDecimal RATE_25_PERCENT = new BigDecimal("0.25");
    private static final BigDecimal RATE_37_PERCENT = new BigDecimal("0.37");

    // New regime max surcharge
    private static final BigDecimal NEW_REGIME_MAX_SURCHARGE = new BigDecimal("0.25");

    /**
     * Calculate surcharge with marginal relief
     */
    public SurchargeResult calculateSurcharge(
            BigDecimal totalIncome,
            BigDecimal taxBeforeSurcharge,
            BigDecimal specialRateIncomeTax,
            String regime) {

        if (totalIncome.compareTo(THRESHOLD_50L) < 0) {
            return SurchargeResult.builder()
                    .surchargeRate(BigDecimal.ZERO)
                    .surchargeAmount(BigDecimal.ZERO)
                    .marginalReliefApplied(false)
                    .marginalReliefAmount(BigDecimal.ZERO)
                    .effectiveSurcharge(BigDecimal.ZERO)
                    .build();
        }

        // Determine base surcharge rate
        BigDecimal baseSurchargeRate = determineSurchargeRate(totalIncome, regime);

        // Calculate surcharge on normal income
        BigDecimal normalIncomeTax = taxBeforeSurcharge.subtract(specialRateIncomeTax);
        BigDecimal surchargeOnNormal = normalIncomeTax.multiply(baseSurchargeRate);

        // Calculate surcharge on special rate income (15% cap)
        BigDecimal cappedRate = baseSurchargeRate.min(RATE_15_PERCENT);
        BigDecimal surchargeOnSpecial = specialRateIncomeTax.multiply(cappedRate);

        // Total surcharge before marginal relief
        BigDecimal totalSurcharge = surchargeOnNormal.add(surchargeOnSpecial);

        // Apply marginal relief
        MarginalReliefResult reliefResult = applyMarginalRelief(
                totalIncome,
                taxBeforeSurcharge,
                totalSurcharge,
                baseSurchargeRate);

        return SurchargeResult.builder()
                .surchargeRate(baseSurchargeRate)
                .surchargeAmount(reliefResult.getFinalSurcharge())
                .marginalReliefApplied(reliefResult.isReliefApplied())
                .marginalReliefAmount(reliefResult.getReliefAmount())
                .effectiveSurcharge(reliefResult.getFinalSurcharge())
                .surchargeOnNormalIncome(surchargeOnNormal)
                .surchargeOnSpecialRateIncome(surchargeOnSpecial)
                .cappedRateForSpecialIncome(cappedRate)
                .build();
    }

    /**
     * Determine surcharge rate based on income and regime
     */
    private BigDecimal determineSurchargeRate(BigDecimal totalIncome, String regime) {
        // New regime has max 25% surcharge
        boolean isNewRegime = TaxSlabEngine.NEW_REGIME.equals(regime);

        if (totalIncome.compareTo(THRESHOLD_5CR) >= 0) {
            return isNewRegime ? NEW_REGIME_MAX_SURCHARGE : RATE_37_PERCENT;
        } else if (totalIncome.compareTo(THRESHOLD_2CR) >= 0) {
            return NEW_REGIME_MAX_SURCHARGE;
        } else if (totalIncome.compareTo(THRESHOLD_1CR) >= 0) {
            return RATE_15_PERCENT;
        } else if (totalIncome.compareTo(THRESHOLD_50L) >= 0) {
            return RATE_10_PERCENT;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Apply marginal relief at surcharge thresholds
     * Marginal relief ensures that tax increase doesn't exceed income increase
     */
    private MarginalReliefResult applyMarginalRelief(
            BigDecimal totalIncome,
            BigDecimal taxBeforeSurcharge,
            BigDecimal surcharge,
            BigDecimal surchargeRate) {

        BigDecimal threshold = null;
        BigDecimal lowerRate = null;

        // Identify which threshold was crossed
        if (totalIncome.compareTo(THRESHOLD_5CR) >= 0 && 
            totalIncome.compareTo(THRESHOLD_5CR.add(new BigDecimal("1000000"))) < 0) {
            threshold = THRESHOLD_5CR;
            lowerRate = NEW_REGIME_MAX_SURCHARGE;
        } else if (totalIncome.compareTo(THRESHOLD_2CR) >= 0 && 
                   totalIncome.compareTo(THRESHOLD_2CR.add(new BigDecimal("1000000"))) < 0) {
            threshold = THRESHOLD_2CR;
            lowerRate = RATE_15_PERCENT;
        } else if (totalIncome.compareTo(THRESHOLD_1CR) >= 0 && 
                   totalIncome.compareTo(THRESHOLD_1CR.add(new BigDecimal("500000"))) < 0) {
            threshold = THRESHOLD_1CR;
            lowerRate = RATE_10_PERCENT;
        } else if (totalIncome.compareTo(THRESHOLD_50L) >= 0 && 
                   totalIncome.compareTo(THRESHOLD_50L.add(new BigDecimal("500000"))) < 0) {
            threshold = THRESHOLD_50L;
            lowerRate = BigDecimal.ZERO;
        }

        // No marginal relief needed if not near threshold
        if (threshold == null) {
            return MarginalReliefResult.builder()
                    .reliefApplied(false)
                    .reliefAmount(BigDecimal.ZERO)
                    .finalSurcharge(surcharge.setScale(0, RoundingMode.HALF_UP))
                    .build();
        }

        // Calculate tax at threshold with lower rate
        BigDecimal taxAtThreshold = taxBeforeSurcharge; // Simplified - should recalculate
        BigDecimal surchargeAtThreshold = taxAtThreshold.multiply(lowerRate);
        BigDecimal totalTaxAtThreshold = taxAtThreshold.add(surchargeAtThreshold);

        // Calculate current total tax
        BigDecimal currentTotalTax = taxBeforeSurcharge.add(surcharge);

        // Income increase beyond threshold
        BigDecimal incomeIncrease = totalIncome.subtract(threshold);

        // Maximum allowed tax increase (marginal relief formula)
        BigDecimal maxAllowedTaxIncrease = incomeIncrease;

        // Actual tax increase
        BigDecimal actualTaxIncrease = currentTotalTax.subtract(totalTaxAtThreshold);

        // Apply marginal relief if actual increase exceeds allowed
        if (actualTaxIncrease.compareTo(maxAllowedTaxIncrease) > 0) {
            BigDecimal reliefAmount = actualTaxIncrease.subtract(maxAllowedTaxIncrease);
            BigDecimal finalSurcharge = surcharge.subtract(reliefAmount);

            return MarginalReliefResult.builder()
                    .reliefApplied(true)
                    .reliefAmount(reliefAmount.setScale(0, RoundingMode.HALF_UP))
                    .finalSurcharge(finalSurcharge.max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP))
                    .build();
        }

        return MarginalReliefResult.builder()
                .reliefApplied(false)
                .reliefAmount(BigDecimal.ZERO)
                .finalSurcharge(surcharge.setScale(0, RoundingMode.HALF_UP))
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SurchargeResult {
        private BigDecimal surchargeRate;
        private BigDecimal surchargeAmount;
        private boolean marginalReliefApplied;
        private BigDecimal marginalReliefAmount;
        private BigDecimal effectiveSurcharge;
        private BigDecimal surchargeOnNormalIncome;
        private BigDecimal surchargeOnSpecialRateIncome;
        private BigDecimal cappedRateForSpecialIncome;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class MarginalReliefResult {
        private boolean reliefApplied;
        private BigDecimal reliefAmount;
        private BigDecimal finalSurcharge;
    }
}
