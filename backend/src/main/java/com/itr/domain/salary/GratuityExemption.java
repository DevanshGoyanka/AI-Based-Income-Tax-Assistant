package com.itr.domain.salary;

/**
 * Gratuity Exemption under Section 10(10).
 *
 * GOVERNMENT EMPLOYEES (Central/State govt, local authorities, statutory corporations):
 *   Entire gratuity is FULLY EXEMPT. No monetary cap.
 *
 * NON-GOVERNMENT EMPLOYEES:
 *   Least of:
 *   (a) 15/26 × last drawn monthly salary × number of years of service
 *   (b) Rs 20,00,000 (notification limit)
 *   (c) Actual gratuity received
 *
 * Note: For employees covered under the Payment of Gratuity Act 1972,
 * the statutory cap is Rs 20,00,000 (raised from Rs 10,00,000 w.e.f 29-Mar-2024).
 *
 * All amounts in PAISE.
 */
public final class GratuityExemption {

    // Rs 20,00,000 in paise
    public static final long NON_GOVT_CAP = 20_00_00000L;

    private GratuityExemption() {}

    /**
     * Compute gratuity exemption.
     *
     * @param lastDrawnMonthlySalary Last drawn monthly salary (paise)
     * @param yearsOfService         Completed years of service
     * @param actualGratuityReceived  Actual gratuity received (paise)
     * @param isGovernmentEmployee    Is employee a govt/local authority/statutory corp?
     * @return Exempt amount in paise
     */
    public static long compute(
            long lastDrawnMonthlySalary,
            int yearsOfService,
            long actualGratuityReceived,
            boolean isGovernmentEmployee
    ) {
        if (actualGratuityReceived <= 0) return 0L;

        if (isGovernmentEmployee) {
            // Government employees: fully exempt, no cap
            return actualGratuityReceived;
        }

        // Non-government: 15/26 × monthly salary × years
        long calculated = (15L * lastDrawnMonthlySalary * yearsOfService) / 26;

        // Return minimum of calculated, statutory cap, actual received
        return Math.min(calculated, Math.min(NON_GOVT_CAP, actualGratuityReceived));
    }

    /**
     * Compute non-government gratuity exemption (convenience overload).
     */
    public static long computeNonGovernment(
            long lastDrawnMonthlySalary,
            int yearsOfService,
            long actualGratuityReceived
    ) {
        return compute(lastDrawnMonthlySalary, yearsOfService, actualGratuityReceived, false);
    }
}
