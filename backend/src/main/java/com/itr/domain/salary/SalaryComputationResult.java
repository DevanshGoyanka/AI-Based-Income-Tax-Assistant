package com.itr.domain.salary;

/** Result of salary computation for one or multiple employers. */
public record SalaryComputationResult(
    long totalGrossSalary,
    long totalExemptAllowances,
    long netSalary,
    long standardDeduction,
    long professionalTaxDeduction,
    long totalDeductionsU16,
    long totalEmployerNPS,
    long totalTDSDeducted,
    int employerCount
) {
    public static SalaryComputationResult empty() {
        return new SalaryComputationResult(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
