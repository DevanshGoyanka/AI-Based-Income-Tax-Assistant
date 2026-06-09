package com.itr.domain.salary;

/**
 * GratuityExemption under Section 10(10).
 * <p>
 * Least of:
 * 1. 15/26 × last drawn salary × years of service
 * 2. ₹20,00,000 (Government notification)
 * 3. Actual gratuity received
 */
public final class GratuityExemption {

    private GratuityExemption() {}

    public static long compute(long lastDrawnMonthlySalary, int yearsOfService, long actualGratuityReceived) {
        long calculated = (15L * lastDrawnMonthlySalary * yearsOfService) / 26;
        return Math.min(calculated, Math.min(20_00_00000L, actualGratuityReceived));
    }
}
