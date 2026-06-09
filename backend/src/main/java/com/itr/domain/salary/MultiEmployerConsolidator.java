package com.itr.domain.salary;

import java.util.List;

/**
 * MultiEmployerConsolidator — merges multiple Form 16 entries from different employers.
 * <p>
 * Rules:
 * - Standard deduction applied only ONCE (use max value across all employers)
 * - Professional tax total capped at ₹2,500
 * - TDS total = sum of all employers' TDS
 * - Gross salary = sum of all employers' gross
 */
public final class MultiEmployerConsolidator {

    private MultiEmployerConsolidator() {}

    public static MultiEmployerResult consolidate(List<EmployerEntry> employers) {
        long totalGross = 0;
        long totalBasicDA = 0;
        long totalHRA = 0;
        long totalTDS = 0;
        long totalPT = 0;
        long totalNPS = 0;
        long maxStdDed = 0;

        for (EmployerEntry emp : employers) {
            totalGross += emp.grossSalary();
            totalBasicDA += emp.basicDA();
            totalHRA += emp.hraReceived();
            totalTDS += emp.tdsDeducted();
            totalPT += emp.professionalTax();
            totalNPS += emp.employerNPSContribution();
        }

        // Standard deduction: max across employers, applied once
        // (This is typically ₹50K/₹75K — set externally by regime)
        long profTaxDeduction = Math.min(totalPT, 2_50000L);

        return new MultiEmployerResult(
            totalGross, totalBasicDA, totalHRA,
            0, // standard deduction set externally
            profTaxDeduction, totalTDS, totalNPS,
            employers.size()
        );
    }

    public record MultiEmployerResult(
        long totalGrossSalary,
        long totalBasicDA,
        long totalHRA,
        long standardDeduction,
        long professionalTax,
        long totalTDS,
        long totalEmployerNPS,
        int employerCount
    ) {}
}
