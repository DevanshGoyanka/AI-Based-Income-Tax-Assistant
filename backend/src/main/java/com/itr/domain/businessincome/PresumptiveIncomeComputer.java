package com.itr.domain.businessincome;

import static com.itr.domain.common.AssessmentYear.*;

/**
 * PresumptiveIncomeComputer — presumptive taxation under Sections 44AD, 44ADA, 44AE.
 */
public final class PresumptiveIncomeComputer {

    private PresumptiveIncomeComputer() {}

    /**
     * Compute presumptive income under Section 44AD.
     * @param turnover        Total turnover in paise
     * @param isDigitalReceipt If true, 6% of turnover; else 8%
     * @return Presumptive income in paise
     */
    public static long compute44AD(long turnover, boolean isDigitalReceipt) {
        if (turnover > LIMIT_44AD_TURNOVER) return 0; // Exceeds limit, not eligible
        int rate = isDigitalReceipt ? RATE_44AD_DIGITAL_BPS : RATE_44AD_NON_DIGITAL_BPS;
        return turnover * rate / 10000;
    }

    /**
     * Compute presumptive income under Section 44ADA (professionals).
     * @param grossReceipts Gross receipts in paise
     * @return Presumptive income in paise (50% of receipts)
     */
    public static long compute44ADA(long grossReceipts) {
        if (grossReceipts > LIMIT_44ADA_RECEIPTS) return 0;
        return grossReceipts * RATE_44ADA_BPS / 10000;
    }

    /**
     * Compute presumptive income under Section 44AE (transport).
     * @param numberOfVehicles Number of goods carriages
     * @param monthsUsed Months used during the year
     * @return Presumptive income in paise
     */
    public static long compute44AE(int numberOfVehicles, int monthsUsed) {
        // ₹7,500 per vehicle per month (heavy goods) or ₹1,000 per ton per month
        // Simplified: ₹7,500 per vehicle per month
        return numberOfVehicles * 7500_00L * monthsUsed;
    }

    public static boolean isEligible44AD(long turnover) {
        return turnover <= LIMIT_44AD_TURNOVER;
    }

    public static boolean isEligible44ADA(long grossReceipts) {
        return grossReceipts <= LIMIT_44ADA_RECEIPTS;
    }
}
