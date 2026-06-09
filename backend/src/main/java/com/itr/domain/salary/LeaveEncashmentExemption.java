package com.itr.domain.salary;

/**
 * LeaveEncashmentExemption under Section 10(10AA).
 * <p>
 * Least of:
 * 1. Actual leave encashment received
 * 2. Statutory limit: ₹25,00,000 (Budget 2023)
 * 3. 10 months average salary × unavailed leave (max 30 days per year)
 */
public final class LeaveEncashmentExemption {

    private LeaveEncashmentExemption() {}

    public static long compute(long actualReceived, long averageMonthlySalary,
                                int unavailedLeaveDays) {
        long calculated = (averageMonthlySalary / 30L) * unavailedLeaveDays;
        long cap = 25_00_00000L; // ₹25L per Budget 2023
        return Math.min(actualReceived, Math.min(cap, calculated));
    }
}
