package com.itr.domain.advancetax;

/**
 * Section234BComputer — default in payment of advance tax.
 * <p>
 * If advance tax paid is less than 90% of assessed tax,
 * interest at 1% per month from April 1 to date of payment.
 */
public final class Section234BComputer {

    private static final int RATE_BPS_PER_MONTH = 100; // 1% per month

    private Section234BComputer() {}

    /**
     * Compute interest under Section 234B.
     *
     * @param assessedTax     Total assessed tax in paise
     * @param advanceTaxPaid  Total advance tax paid in paise
     * @param monthsFromApril Months from April 1 to payment (or filing if unpaid)
     * @return Interest in paise
     */
    public static long compute(long assessedTax, long advanceTaxPaid, int monthsFromApril) {
        if (assessedTax <= 0) return 0;
        // Shortfall: if advance tax < 90% of assessed tax
        long ninetyPercent = assessedTax * 90 / 100;
        if (advanceTaxPaid >= ninetyPercent) return 0;

        long shortfall = assessedTax - advanceTaxPaid;
        return shortfall * RATE_BPS_PER_MONTH * monthsFromApril / 10000;
    }
}
