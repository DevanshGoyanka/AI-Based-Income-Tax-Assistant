package com.itr.domain.salary;

import com.itr.domain.common.AssessmentYear;
import java.util.List;

/**
 * SalaryScheduleComputer — orchestrates all salary-related computations.
 * <p>
 * Produces the net taxable salary from salary income under the head
 * "Salaries" (Section 15-17).
 */
public final class SalaryScheduleComputer {

    private SalaryScheduleComputer() {}

    /**
     * Compute net taxable salary from multiple employers.
     *
     * @param employers       List of employer entries
     * @param standardDeduction Standard deduction amount (75K new regime, 50K old)
     * @return SalaryComputationResult with all computed fields
     */
    public static SalaryComputationResult compute(List<EmployerEntry> employers, long standardDeduction) {
        if (employers == null || employers.isEmpty()) {
            return SalaryComputationResult.empty();
        }

        long totalGrossSalary = 0;
        long totalBasicDA = 0;
        long totalHRA = 0;
        long totalEmployerNPS = 0;
        long totalProfessionalTax = 0;
        long totalTDS = 0;

        for (EmployerEntry emp : employers) {
            totalGrossSalary += emp.grossSalary();
            totalBasicDA += emp.basicDA();
            totalHRA += emp.hraReceived();
            totalEmployerNPS += emp.employerNPSContribution();
            totalProfessionalTax += emp.professionalTax();
            totalTDS += emp.tdsDeducted();
        }

        // Compute exemptions
        long exemptAllowances = 0;
        // HRA not computed here — requires rent details from user input
        // LTA — requires travel proof; treated separately
        // Gratuity exemption: minimum of 15/26 × salary × years, ₹20L, actual
        // Leave encashment: minimum of calculated amount, ₹25L, actual

        long netSalary = totalGrossSalary - exemptAllowances;
        long profTaxDeduction = Math.min(totalProfessionalTax, 250000L); // paise version of ₹2,500

        long grossTotalSalary = totalGrossSalary;
        long totalDeductionsU16 = standardDeduction + profTaxDeduction;

        return new SalaryComputationResult(
            totalGrossSalary, exemptAllowances, netSalary,
            standardDeduction, profTaxDeduction, totalDeductionsU16,
            totalEmployerNPS, totalTDS, employers.size()
        );
    }
}
