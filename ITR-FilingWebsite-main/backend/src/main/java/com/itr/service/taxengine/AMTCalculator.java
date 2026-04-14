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
 * AMT (Alternate Minimum Tax) Calculator - Section 115JC
 * Applicable when deductions like 10AA, 35AD, 80H-80RRB claimed
 * AMT = 18.5% of Adjusted Total Income (ATI)
 * AMT Credit u/s 115JD - carry forward 15 years
 */
@Slf4j
@Service
public class AMTCalculator {

    private static final BigDecimal AMT_RATE = new BigDecimal("0.185");
    private static final BigDecimal AMT_THRESHOLD = new BigDecimal("2000000"); // ₹20L
    private static final int AMT_CREDIT_CARRY_FORWARD_YEARS = 15;

    /**
     * Calculate AMT and determine if applicable
     */
    public AMTResult calculateAMT(AMTInput input) {
        
        // AMT not applicable if total income <= ₹20L
        if (input.getTotalIncome().compareTo(AMT_THRESHOLD) <= 0) {
            return AMTResult.builder()
                    .amtApplicable(false)
                    .adjustedTotalIncome(BigDecimal.ZERO)
                    .amtAmount(BigDecimal.ZERO)
                    .regularTax(input.getRegularTax())
                    .taxPayable(input.getRegularTax())
                    .amtCreditGenerated(BigDecimal.ZERO)
                    .reason("Total income ≤ ₹20L - AMT not applicable")
                    .build();
        }

        // Calculate Adjusted Total Income (ATI)
        BigDecimal ati = calculateATI(input);

        // Calculate AMT @ 18.5%
        BigDecimal amtAmount = ati.multiply(AMT_RATE).setScale(0, RoundingMode.HALF_UP);

        // Compare AMT with regular tax
        if (amtAmount.compareTo(input.getRegularTax()) <= 0) {
            // Regular tax is higher - AMT not applicable
            // But check if AMT credit can be utilized
            BigDecimal amtCreditUtilized = input.getRegularTax().subtract(amtAmount)
                    .min(input.getAmtCreditBroughtForward());

            return AMTResult.builder()
                    .amtApplicable(false)
                    .adjustedTotalIncome(ati)
                    .amtAmount(amtAmount)
                    .regularTax(input.getRegularTax())
                    .taxPayable(input.getRegularTax())
                    .amtCreditGenerated(BigDecimal.ZERO)
                    .amtCreditUtilized(amtCreditUtilized)
                    .amtCreditCarryForward(input.getAmtCreditBroughtForward().subtract(amtCreditUtilized))
                    .reason("Regular tax > AMT - Regular tax payable, AMT credit utilized")
                    .build();
        }

        // AMT is higher - AMT applicable
        BigDecimal amtCreditGenerated = amtAmount.subtract(input.getRegularTax());

        return AMTResult.builder()
                .amtApplicable(true)
                .adjustedTotalIncome(ati)
                .amtAmount(amtAmount)
                .regularTax(input.getRegularTax())
                .taxPayable(amtAmount)
                .amtCreditGenerated(amtCreditGenerated)
                .amtCreditUtilized(BigDecimal.ZERO)
                .amtCreditCarryForward(input.getAmtCreditBroughtForward().add(amtCreditGenerated))
                .reason("AMT > Regular tax - AMT payable, credit generated")
                .build();
    }

    /**
     * Calculate Adjusted Total Income (ATI)
     * ATI = Total Income + Add backs
     */
    private BigDecimal calculateATI(AMTInput input) {
        BigDecimal ati = input.getTotalIncome();

        // Add back: Deduction u/s 10AA (SEZ units)
        ati = ati.add(input.getDeduction10AA());

        // Add back: Deduction u/s 35AD (specified business)
        ati = ati.add(input.getDeduction35AD());

        // Add back: Deductions u/s 80H to 80RRB (except 80P)
        ati = ati.add(input.getDeduction80HTo80RRB());

        return ati;
    }

    /**
     * Track AMT credit for future years
     */
    public AMTCreditTracking trackAMTCredit(
            BigDecimal currentYearCredit,
            BigDecimal broughtForwardCredit,
            int yearsOld) {

        // AMT credit expires after 15 years
        if (yearsOld >= AMT_CREDIT_CARRY_FORWARD_YEARS) {
            return AMTCreditTracking.builder()
                    .creditExpired(broughtForwardCredit)
                    .creditAvailable(currentYearCredit)
                    .creditCarryForward(currentYearCredit)
                    .build();
        }

        BigDecimal totalCredit = broughtForwardCredit.add(currentYearCredit);

        return AMTCreditTracking.builder()
                .creditExpired(BigDecimal.ZERO)
                .creditAvailable(totalCredit)
                .creditCarryForward(totalCredit)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AMTInput {
        private BigDecimal totalIncome;
        private BigDecimal regularTax;
        
        // Add-backs for ATI calculation
        @Builder.Default
        private BigDecimal deduction10AA = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal deduction35AD = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal deduction80HTo80RRB = BigDecimal.ZERO;
        
        // AMT credit from previous years
        @Builder.Default
        private BigDecimal amtCreditBroughtForward = BigDecimal.ZERO;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AMTResult {
        private boolean amtApplicable;
        private BigDecimal adjustedTotalIncome;
        private BigDecimal amtAmount;
        private BigDecimal regularTax;
        private BigDecimal taxPayable;
        private BigDecimal amtCreditGenerated;
        private BigDecimal amtCreditUtilized;
        private BigDecimal amtCreditCarryForward;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AMTCreditTracking {
        private BigDecimal creditExpired;
        private BigDecimal creditAvailable;
        private BigDecimal creditCarryForward;
    }
}
