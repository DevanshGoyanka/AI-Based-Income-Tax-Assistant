package com.itr.domain.advancetax;

import static com.itr.domain.common.AssessmentYear.*;

/**
 * Section234FComputer — late filing fee under Section 234F.
 * <p>
 * Income > ₹5,00,000: ₹5,000 (if filed before Dec 31) / ₹10,000 (if filed after Dec 31)
 * Income ≤ ₹5,00,000: ₹1,000 maximum
 */
public final class Section234FComputer {

    private Section234FComputer() {}

    public static long compute(long totalIncome, boolean filedByDec31) {
        if (totalIncome <= 5_00_00000L) {
            return LATE_FEE_LOW_INCOME;
        }
        return filedByDec31 ? LATE_FEE_HIGH_INCOME : 10_00000L;
    }
}
