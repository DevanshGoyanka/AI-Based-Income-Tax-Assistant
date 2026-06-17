package com.itr.domain.salary;

import com.itr.domain.common.TaxRegime;

/**
 * Special Allowance Exemptions under Section 10(14).
 *
 * ALL Section 10(14) allowances are NOT available under the New Tax Regime.
 * Under Old Tax Regime:
 *   - Transport allowance: Rs 1,600/month (Rs 19,200/year) — regular employees
 *   - Transport allowance: Rs 3,200/month (Rs 38,400/year) — disabled employees (Section 10(14)(ii))
 *   - Children education: Rs 100/month/child (Rs 1,200/year/child, max 2 children)
 *   - Hostel expenditure: Rs 300/month/child (Rs 3,600/year/child, max 2 children)
 *   - Uniform allowance: Actual expenditure on uniform worn during duty
 *   - Conveyance allowance: Actual for official duty
 *
 * All amounts in PAISE.
 */
public final class SpecialAllowanceExemption {

    // Rs 1,600/month in paise (Rs 19,200/year)
    private static final long TRANSPORT_MONTHLY = 1_60000L;
    private static final long TRANSPORT_ANNUAL = 19_20000L;  // Rs 19,200/year
    // Rs 3,200/month in paise (disabled employees) (Rs 38,400/year)
    private static final long TRANSPORT_DISABLED_MONTHLY = 3_20000L;
    private static final long TRANSPORT_DISABLED_ANNUAL = 38_40000L;  // Rs 38,400/year
    // Rs 100/month per child in paise (Rs 1,200/year)
    private static final long CEA_MONTHLY_PER_CHILD = 100_000L;
    private static final long CEA_ANNUAL_PER_CHILD = 1_20000L;  // Rs 1,200/year
    // Rs 300/month per child in paise (Rs 3,600/year)
    private static final long HOSTEL_MONTHLY_PER_CHILD = 300_000L;
    private static final long HOSTEL_ANNUAL_PER_CHILD = 3_60000L;  // Rs 3,600/year

    private SpecialAllowanceExemption() {}

    /**
     * Transport Allowance u/s 10(14)(i).
     * Regular employees: Rs 1,600/month from AY 2018-19 onwards (not available earlier)
     * Disabled employees: Rs 3,200/month u/s 10(14)(ii)
     *
     * NOT AVAILABLE under New Tax Regime.
     */
    public static long computeTransportAllowance(
            long transportReceived,
            boolean isDisabledEmployee,
            TaxRegime regime
    ) {
        if (regime == TaxRegime.NEW) {
            return 0L;
        }
        long cap = isDisabledEmployee ? TRANSPORT_DISABLED_ANNUAL : TRANSPORT_ANNUAL;
        return Math.min(transportReceived, cap);
    }

    /**
     * Children Education Allowance u/s 10(14).
     * Rs 100/month per child, max 2 children.
     *
     * NOT AVAILABLE under New Tax Regime.
     */
    public static long computeChildrenEducation(
            long allowanceReceived,
            int numberOfChildren,
            TaxRegime regime
    ) {
        if (regime == TaxRegime.NEW) {
            return 0L;
        }
        int eligibleChildren = Math.min(numberOfChildren, 2);
        long cap = CEA_ANNUAL_PER_CHILD * eligibleChildren;
        return Math.min(allowanceReceived, cap);
    }

    /**
     * Hostel Expenditure Allowance u/s 10(14).
     * Rs 300/month per child, max 2 children.
     *
     * NOT AVAILABLE under New Tax Regime.
     */
    public static long computeHostelExpenditure(
            long allowanceReceived,
            int numberOfChildren,
            TaxRegime regime
    ) {
        if (regime == TaxRegime.NEW) {
            return 0L;
        }
        int eligibleChildren = Math.min(numberOfChildren, 2);
        long cap = HOSTEL_ANNUAL_PER_CHILD * eligibleChildren;
        return Math.min(allowanceReceived, cap);
    }

    /**
     * Uniform Allowance u/s 10(14).
     * Actual expenditure on uniform worn during duty.
     *
     * NOT AVAILABLE under New Tax Regime.
     */
    public static long computeUniformAllowance(
            long allowanceReceived,
            long actualExpenditure,
            TaxRegime regime
    ) {
        if (regime == TaxRegime.NEW) {
            return 0L;
        }
        return Math.min(allowanceReceived, actualExpenditure);
    }

    /**
     * Compute total Section 10(14) exemptions from an EmployerEntry.
     */
    public static long computeTotal(
            long transportReceived,
            long childrenEducationReceived,
            long hostelExpenditureReceived,
            long uniformAllowanceReceived,
            long actualUniformExpenditure,
            int numberOfChildren,
            boolean isDisabledEmployee,
            TaxRegime regime
    ) {
        return computeTransportAllowance(transportReceived, isDisabledEmployee, regime)
             + computeChildrenEducation(childrenEducationReceived, numberOfChildren, regime)
             + computeHostelExpenditure(hostelExpenditureReceived, numberOfChildren, regime)
             + computeUniformAllowance(uniformAllowanceReceived, actualUniformExpenditure, regime);
    }
}
