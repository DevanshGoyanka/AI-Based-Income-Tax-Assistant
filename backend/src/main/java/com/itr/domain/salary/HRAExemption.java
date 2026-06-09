package com.itr.domain.salary;

/**
 * HRAExemption computator — Section 10(13A) read with Rule 2A.
 * <p>
 * Exemption = minimum of three conditions:
 * 1. Actual HRA received
 * 2. Rent paid minus 10% of salary (basic + DA)
 * 3. 50% of salary (metro) or 40% (non-metro)
 */
public final class HRAExemption {

    private HRAExemption() {}

    /**
     * Compute HRA exemption.
     *
     * @param actualHRA      HRA actually received in paise
     * @param basicDA        Basic salary + Dearness Allowance (paise)
     * @param rentPaid       Actual rent paid (paise)
     * @param isMetro        Whether the employee lives in a metro city
     * @return HRA exemption amount in paise
     */
    public static long compute(long actualHRA, long basicDA, long rentPaid, boolean isMetro) {
        // Condition 1: Actual HRA
        long condition1 = Math.max(actualHRA, 0);

        // Condition 2: Rent paid - 10% of salary (basic + DA)
        long condition2 = Math.max(rentPaid - (basicDA * 10 / 100), 0);

        // Condition 3: 50% of salary (metro) or 40% (non-metro)
        long condition3 = isMetro ? (basicDA * 50 / 100) : (basicDA * 40 / 100);

        // Exemption = minimum of the three
        return Math.min(condition1, Math.min(condition2, condition3));
    }
}
