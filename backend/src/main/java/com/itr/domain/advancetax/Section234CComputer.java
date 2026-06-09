package com.itr.domain.advancetax;

/**
 * Section234CComputer — deferment of advance tax installments.
 * <p>
 * If an installment is short, 1% per month on shortfall from due date.
 * <p>
 * Due dates:
 * - Q1: 15 June (15% of assessed tax)
 * - Q2: 15 September (45% cumulative)
 * - Q3: 15 December (75% cumulative)
 * - Q4: 15 March (100% cumulative)
 */
public final class Section234CComputer {

    private Section234CComputer() {}

    private static final int RATE_BPS_PER_MONTH = 100;
    private static final long[][] INSTALLMENTS = {
        {15, 1500},  // Q1: 15% by June 15
        {45, 4500},  // Q2: 45% by Sep 15
        {75, 7500},  // Q3: 75% by Dec 15
        {100, 10000} // Q4: 100% by Mar 15
    };

    public static long compute(long assessedTax, long[] advanceTaxPaidByQuarter, int[] monthsDelayed) {
        long totalInterest = 0;
        for (int i = 0; i < 4; i++) {
            long required = assessedTax * INSTALLMENTS[i][1] / 10000;
            long paid = advanceTaxPaidByQuarter[i];
            if (paid < required) {
                long shortfall = required - paid;
                totalInterest += shortfall * RATE_BPS_PER_MONTH * monthsDelayed[i] / 10000;
            }
        }
        return totalInterest;
    }
}
