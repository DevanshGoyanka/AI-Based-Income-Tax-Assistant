package com.itr.domain.advancetax;

/**
 * Section234AComputer — delay in filing return.
 * <p>
 * Interest = 1% per month (simple) on tax unpaid from due date to filing date.
 */
public final class Section234AComputer {

    private static final int RATE_BPS_PER_MONTH = 100; // 1% per month

    private Section234AComputer() {}

    /**
     * Compute interest under Section 234A.
     *
     * @param unpaidTax    Tax unpaid at due date in paise
     * @param monthsDelay  Number of months delay (part-month = full month)
     * @return Interest in paise
     */
    public static long compute(long unpaidTax, int monthsDelay) {
        if (unpaidTax <= 0 || monthsDelay <= 0) return 0;
        // Simple interest: unpaidTax × 1% × months
        return unpaidTax * RATE_BPS_PER_MONTH * monthsDelay / 10000;
    }
}
