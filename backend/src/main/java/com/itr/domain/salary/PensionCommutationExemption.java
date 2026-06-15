package com.itr.domain.salary;

/**
 * Pension Commutation Exemption under Section 10(10A).
 *
 * GOVERNMENT EMPLOYEES (Central/State govt, local authorities, statutory corporations):
 *   Entire commuted pension is FULLY EXEMPT. No limit.
 *
 * NON-GOVERNMENT EMPLOYEES:
 *   (a) Employee also received gratuity from same employer → 1/3 of commuted pension exempt
 *   (b) Employee did NOT receive gratuity → 1/2 of commuted pension exempt
 *
 *   Taxable = Commuted pension received − Exempt portion
 *
 * Note: There is no monetary cap on the exempt amount.
 * The remaining taxable amount is reported under Section 17(1) as salary.
 *
 * All amounts in PAISE.
 */
public final class PensionCommutationExemption {

    private PensionCommutationExemption() {}

    /**
     * Compute pension commutation exemption.
     *
     * @param commutedPensionReceived  Lump sum commuted pension received (paise)
     * @param isGovernmentEmployee     Is employee a govt/local authority/statutory corp?
     * @param gratuityAlsoReceived     Did employee also receive gratuity from this employer?
     * @return Exempt amount in paise
     */
    public static long compute(
            long commutedPensionReceived,
            boolean isGovernmentEmployee,
            boolean gratuityAlsoReceived
    ) {
        if (commutedPensionReceived <= 0) return 0L;

        if (isGovernmentEmployee) {
            // Government employees: fully exempt — no cap
            return commutedPensionReceived;
        }

        // Non-government: fraction depends on gratuity receipt
        if (gratuityAlsoReceived) {
            return commutedPensionReceived / 3; // 1/3 exempt
        } else {
            return commutedPensionReceived / 2; // 1/2 exempt
        }
    }

    /**
     * Compute taxable portion of commuted pension.
     */
    public static long computeTaxable(
            long commutedPensionReceived,
            boolean isGovernmentEmployee,
            boolean gratuityAlsoReceived
    ) {
        long exempt = compute(commutedPensionReceived, isGovernmentEmployee, gratuityAlsoReceived);
        return commutedPensionReceived - exempt;
    }
}
