package com.itr.domain.salary;

/**
 * Leave Encashment Exemption under Section 10(10AA).
 *
 * GOVERNMENT EMPLOYEES:
 *   Entire leave encashment is FULLY EXEMPT. No monetary cap.
 *
 * NON-GOVERNMENT EMPLOYEES:
 *   Least of:
 *   (a) Actual leave encashment received
 *   (b) Rs 25,00,000 (raised by Budget 2023)
 *   (c) Average monthly salary × unavailed leave days / 30 × number of years
 *       (subject to 30 days per year of earned leave)
 *
 * All amounts in PAISE.
 */
public final class LeaveEncashmentExemption {

    // Rs 25,00,000 in paise — raised by Finance Act 2023 (Budget 2023)
    public static final long NON_GOVT_CAP = 25_00_00000L;
    // Maximum 30 days leave per year for calculation
    private static final int MAX_LEAVE_DAYS_PER_YEAR = 30;

    private LeaveEncashmentExemption() {}

    /**
     * Compute leave encashment exemption.
     *
     * @param actualReceived         Actual leave encashment received (paise)
     * @param averageMonthlySalary   Average monthly salary (paise)
     * @param unavailedLeaveDays     Unavailed earned leave days
     * @param yearsOfService         Completed years of service
     * @param isGovernmentEmployee   Is employee a govt/local authority/statutory corp?
     * @return Exempt amount in paise
     */
    public static long compute(
            long actualReceived,
            long averageMonthlySalary,
            int unavailedLeaveDays,
            int yearsOfService,
            boolean isGovernmentEmployee
    ) {
        if (actualReceived <= 0) return 0L;

        if (isGovernmentEmployee) {
            // Government employees: fully exempt, no cap
            return actualReceived;
        }

        // Non-government: formula-based
        // Cap unavailed leave at 30 days per year of service
        int cappedLeaveDays = Math.min(unavailedLeaveDays, MAX_LEAVE_DAYS_PER_YEAR * yearsOfService);

        // Daily rate = average monthly salary / 30
        long calculated = (averageMonthlySalary / 30L) * cappedLeaveDays;

        return Math.min(actualReceived, Math.min(NON_GOVT_CAP, calculated));
    }

    /**
     * Convenience overload for non-government employees.
     */
    public static long computeNonGovernment(
            long actualReceived,
            long averageMonthlySalary,
            int unavailedLeaveDays,
            int yearsOfService
    ) {
        return compute(actualReceived, averageMonthlySalary, unavailedLeaveDays, yearsOfService, false);
    }
}
