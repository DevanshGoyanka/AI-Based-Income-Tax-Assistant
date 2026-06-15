package com.itr.domain.salary;

import com.itr.domain.common.TaxRegime;

/**
 * Entertainment Allowance Deduction under Section 16(ii).
 *
 * AVAILABLE: Government employees + Old Tax Regime ONLY.
 * NOT AVAILABLE: New Tax Regime, or non-government employees.
 *
 * Deduction = Minimum of:
 *   (a) Actual entertainment allowance received
 *   (b) Rs 5,000
 *   (c) 20% of basic salary
 *
 * Note: This is a DEDUCTION from gross salary, not an exemption.
 * It reduces the taxable salary, not the gross amount received.
 *
 * All amounts in PAISE.
 */
public final class EntertainmentAllowanceDeduction {

    // Rs 5,000 in paise
    private static final long MAX_DEDUCTION = 5_00000L;

    private EntertainmentAllowanceDeduction() {}

    /**
     * Compute entertainment allowance deduction.
     *
     * @param entertainmentAllowanceReceived  Entertainment allowance received (paise)
     * @param basicSalary                     Basic salary (paise)
     * @param isGovernmentEmployee            Is employee a government employee?
     * @param regime                          Tax regime
     * @return Deduction amount in paise
     */
    public static long compute(
            long entertainmentAllowanceReceived,
            long basicSalary,
            boolean isGovernmentEmployee,
            TaxRegime regime
    ) {
        // Only available for government employees under old regime
        if (!isGovernmentEmployee || regime == TaxRegime.NEW) {
            return 0L;
        }

        long twentyPctOfBasic = (basicSalary * 20) / 100;
        return Math.min(entertainmentAllowanceReceived,
               Math.min(MAX_DEDUCTION, twentyPctOfBasic));
    }
}
