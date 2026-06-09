package com.itr.service.taxengine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Special Rate Income Calculator
 * Handles: 111A (STCG equity), 112A (LTCG equity), 112 (LTCG property/unlisted),
 *          115BB (lottery/gambling), 115BBH (VDA), 115BBE (unexplained income)
 * Split-rate computation for AY 2025-26 (pre/post July 23, 2024)
 */
@Slf4j
@Service
public class SpecialRateIncomeCalculator {

    // Split date for AY 2025-26
    private static final LocalDate SPLIT_DATE = LocalDate.of(2024, 7, 23);

    // STCG 111A rates
    private static final BigDecimal STCG_111A_RATE_PRE_JULY = new BigDecimal("0.15");
    private static final BigDecimal STCG_111A_RATE_POST_JULY = new BigDecimal("0.20");

    // LTCG 112A rates and exemption
    private static final BigDecimal LTCG_112A_RATE_PRE_JULY = new BigDecimal("0.10");
    private static final BigDecimal LTCG_112A_RATE_POST_JULY = new BigDecimal("0.125");
    private static final BigDecimal LTCG_112A_EXEMPTION_PRE_JULY = new BigDecimal("100000");
    private static final BigDecimal LTCG_112A_EXEMPTION_POST_JULY = new BigDecimal("125000");

    // LTCG 112 rates
    private static final BigDecimal LTCG_112_RATE_WITH_INDEXATION = new BigDecimal("0.20");
    private static final BigDecimal LTCG_112_RATE_WITHOUT_INDEXATION = new BigDecimal("0.125");

    // Other special rates
    private static final BigDecimal RATE_115BB = new BigDecimal("0.30"); // Lottery/gambling
    private static final BigDecimal RATE_115BBH = new BigDecimal("0.30"); // VDA
    private static final BigDecimal RATE_115BBE = new BigDecimal("0.60"); // Unexplained income

    /**
     * Calculate tax on STCG u/s 111A (listed equity with STT)
     */
    public BigDecimal calculateSTCG111A(BigDecimal stcgAmount, LocalDate saleDate, String assessmentYear) {
        if (stcgAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Split-rate only for AY 2025-26
        if (TaxSlabEngine.AY_2025_26.equals(assessmentYear) && saleDate != null) {
            if (saleDate.isBefore(SPLIT_DATE)) {
                return stcgAmount.multiply(STCG_111A_RATE_PRE_JULY).setScale(0, RoundingMode.HALF_UP);
            } else {
                return stcgAmount.multiply(STCG_111A_RATE_POST_JULY).setScale(0, RoundingMode.HALF_UP);
            }
        }

        // AY 2026-27: uniform 20% rate
        return stcgAmount.multiply(STCG_111A_RATE_POST_JULY).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Calculate tax on LTCG u/s 112A (listed equity with STT)
     * Includes ₹1L/₹1.25L annual exemption
     */
    public LTCG112AResult calculateLTCG112A(
            BigDecimal ltcgAmount,
            LocalDate saleDate,
            String assessmentYear,
            BigDecimal exemptionAlreadyUsed) {

        if (ltcgAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return LTCG112AResult.builder()
                    .taxableAmount(BigDecimal.ZERO)
                    .exemptionUsed(BigDecimal.ZERO)
                    .tax(BigDecimal.ZERO)
                    .rate(BigDecimal.ZERO)
                    .build();
        }

        BigDecimal exemptionLimit;
        BigDecimal rate;

        // Determine exemption and rate based on AY and sale date
        if (TaxSlabEngine.AY_2025_26.equals(assessmentYear) && saleDate != null) {
            if (saleDate.isBefore(SPLIT_DATE)) {
                exemptionLimit = LTCG_112A_EXEMPTION_PRE_JULY;
                rate = LTCG_112A_RATE_PRE_JULY;
            } else {
                exemptionLimit = LTCG_112A_EXEMPTION_POST_JULY;
                rate = LTCG_112A_RATE_POST_JULY;
            }
        } else {
            // AY 2026-27: uniform ₹1.25L exemption and 12.5% rate
            exemptionLimit = LTCG_112A_EXEMPTION_POST_JULY;
            rate = LTCG_112A_RATE_POST_JULY;
        }

        // Calculate available exemption
        BigDecimal availableExemption = exemptionLimit.subtract(exemptionAlreadyUsed).max(BigDecimal.ZERO);
        BigDecimal exemptionUsed = ltcgAmount.min(availableExemption);
        BigDecimal taxableAmount = ltcgAmount.subtract(exemptionUsed).max(BigDecimal.ZERO);
        BigDecimal tax = taxableAmount.multiply(rate).setScale(0, RoundingMode.HALF_UP);

        return LTCG112AResult.builder()
                .taxableAmount(taxableAmount)
                .exemptionUsed(exemptionUsed)
                .tax(tax)
                .rate(rate)
                .build();
    }

    /**
     * Calculate tax on LTCG u/s 112 (property/unlisted shares)
     * For pre-July 23, 2024 property: choice between 20% with indexation or 12.5% without
     */
    public LTCG112Result calculateLTCG112(
            BigDecimal ltcgWithIndexation,
            BigDecimal ltcgWithoutIndexation,
            LocalDate saleDate,
            String assessmentYear,
            boolean isProperty) {

        // For property sold before July 23, 2024 in AY 2025-26: offer choice
        if (isProperty && TaxSlabEngine.AY_2025_26.equals(assessmentYear) && 
            saleDate != null && saleDate.isBefore(SPLIT_DATE)) {
            
            BigDecimal taxWithIndexation = ltcgWithIndexation.multiply(LTCG_112_RATE_WITH_INDEXATION)
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal taxWithoutIndexation = ltcgWithoutIndexation.multiply(LTCG_112_RATE_WITHOUT_INDEXATION)
                    .setScale(0, RoundingMode.HALF_UP);

            // Choose lower tax
            if (taxWithIndexation.compareTo(taxWithoutIndexation) <= 0) {
                return LTCG112Result.builder()
                        .taxableAmount(ltcgWithIndexation)
                        .tax(taxWithIndexation)
                        .rate(LTCG_112_RATE_WITH_INDEXATION)
                        .indexationApplied(true)
                        .choiceAvailable(true)
                        .build();
            } else {
                return LTCG112Result.builder()
                        .taxableAmount(ltcgWithoutIndexation)
                        .tax(taxWithoutIndexation)
                        .rate(LTCG_112_RATE_WITHOUT_INDEXATION)
                        .indexationApplied(false)
                        .choiceAvailable(true)
                        .build();
            }
        }

        // Post July 23, 2024 or AY 2026-27: only 12.5% without indexation
        BigDecimal tax = ltcgWithoutIndexation.multiply(LTCG_112_RATE_WITHOUT_INDEXATION)
                .setScale(0, RoundingMode.HALF_UP);

        return LTCG112Result.builder()
                .taxableAmount(ltcgWithoutIndexation)
                .tax(tax)
                .rate(LTCG_112_RATE_WITHOUT_INDEXATION)
                .indexationApplied(false)
                .choiceAvailable(false)
                .build();
    }

    /**
     * Calculate tax on lottery/gambling income u/s 115BB
     */
    public BigDecimal calculate115BB(BigDecimal lotteryIncome) {
        if (lotteryIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return lotteryIncome.multiply(RATE_115BB).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Calculate tax on VDA income u/s 115BBH
     */
    public BigDecimal calculate115BBH(BigDecimal vdaIncome) {
        if (vdaIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return vdaIncome.multiply(RATE_115BBH).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Calculate tax on unexplained income u/s 115BBE
     * 60% + 25% surcharge + 4% cess = ~81% effective rate
     */
    public UnexplainedIncomeResult calculate115BBE(BigDecimal unexplainedIncome) {
        if (unexplainedIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return UnexplainedIncomeResult.builder()
                    .baseTax(BigDecimal.ZERO)
                    .surcharge(BigDecimal.ZERO)
                    .cess(BigDecimal.ZERO)
                    .totalTax(BigDecimal.ZERO)
                    .build();
        }

        BigDecimal baseTax = unexplainedIncome.multiply(RATE_115BBE);
        BigDecimal surcharge = baseTax.multiply(new BigDecimal("0.25"));
        BigDecimal taxPlusSurcharge = baseTax.add(surcharge);
        BigDecimal cess = taxPlusSurcharge.multiply(new BigDecimal("0.04"));
        BigDecimal totalTax = taxPlusSurcharge.add(cess);

        return UnexplainedIncomeResult.builder()
                .baseTax(baseTax.setScale(0, RoundingMode.HALF_UP))
                .surcharge(surcharge.setScale(0, RoundingMode.HALF_UP))
                .cess(cess.setScale(0, RoundingMode.HALF_UP))
                .totalTax(totalTax.setScale(0, RoundingMode.HALF_UP))
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LTCG112AResult {
        private BigDecimal taxableAmount;
        private BigDecimal exemptionUsed;
        private BigDecimal tax;
        private BigDecimal rate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LTCG112Result {
        private BigDecimal taxableAmount;
        private BigDecimal tax;
        private BigDecimal rate;
        private boolean indexationApplied;
        private boolean choiceAvailable;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnexplainedIncomeResult {
        private BigDecimal baseTax;
        private BigDecimal surcharge;
        private BigDecimal cess;
        private BigDecimal totalTax;
    }
}
