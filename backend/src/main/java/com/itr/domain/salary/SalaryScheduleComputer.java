package com.itr.domain.salary;

import com.itr.domain.common.AssessmentYear;
import com.itr.domain.common.TaxRegime;

import java.util.List;

/**
 * SalaryScheduleComputer — orchestrates all salary-related computations
 * for AY 2026-27 (FY 2025-26).
 *
 * Computes income under the head "Salaries" (Sections 15-17) end-to-end:
 *   - Aggregates multiple employer entries
 *   - Computes all Section 10 exemptions (HRA, LTA, Gratuity, Leave Encashment,
 *     Pension Commutation, Transport, Children Education, Hostel, Uniform)
 *   - Computes Section 16 deductions (Standard Deduction, Entertainment Allowance, Professional Tax)
 *   - Applies regime-awareness (old vs new tax regime)
 *
 * All monetary values are in PAISE (1 rupee = 100 paise).
 */
public final class SalaryScheduleComputer {

    private SalaryScheduleComputer() {}

    /**
     * Compute net taxable salary from multiple employers.
     *
     * @param employers    List of employer salary entries
     * @param standardDeductionPaise  Standard deduction amount in paise
     *                             (Rs 75,000 for new regime, Rs 50,000 for old regime)
     * @param regime      Tax regime (OLD or NEW)
     * @param ay          Assessment year e.g. "2026-27"
     * @return SalaryComputationResult with all computed fields
     */
    public static SalaryComputationResult compute(
            List<EmployerEntry> employers,
            long standardDeductionPaise,
            TaxRegime regime,
            String ay
    ) {
        if (employers == null || employers.isEmpty()) {
            return SalaryComputationResult.empty(ay, regime);
        }

        // ── Aggregate from all employers ────────────────────────────────────────
        long totalGrossSalary17_1 = 0L;
        long totalGrossSalary17_2 = 0L;
        long totalGrossSalary17_3 = 0L;
        long totalBasicDA = 0L;       // Basic + DA for HRA salary computation
        long totalHRAReceived = 0L;
        long totalLTAReceived = 0L;
        long totalTransportReceived = 0L;
        long totalCEAReceived = 0L;
        long totalHostelReceived = 0L;
        long totalUniformReceived = 0L;
        long totalOtherAllowances = 0L;
        long totalBonus = 0L;
        long totalArrearSalary = 0L;
        long totalCommutedPension = 0L;
        long totalGratuity = 0L;
        long totalLeaveEncashment = 0L;
        long totalUncommutedPension = 0L;
        long totalProfessionalTax = 0L;
        long totalEntertainmentReceived = 0L;
        long totalEmployerNPS = 0L;
        long totalTDS = 0L;
        long totalAnnualRentPaid = 0L;
        long totalActualLTAFare = 0L;
        int totalChildren = 0;
        boolean anyGovtEmployee = false;
        boolean anyDisabledEmployee = false;
        int totalJourneysInBlock = 0;
        boolean allDomesticTravel = true;

        // Aggregated fields needed for exemptions
        long totalBasicSalary = 0L;
        long totalDA = 0L;
        long totalCommission = 0L;
        long totalGratuityReceived = 0L;
        long totalLeaveEncashmentReceived = 0L;
        long totalAverageMonthlySalary = 0L;
        int totalUnavailedLeaveDays = 0;
        int totalYearsOfService = 0;
        int totalCommutedPensionCount = 0;
        boolean anyGratuityReceived = false;
        boolean anyCommutedPension = false;

        for (EmployerEntry emp : employers) {
            // Section 17(1)
            totalBasicSalary += emp.basicSalary();
            totalDA += emp.da();
            totalCommission += emp.commission();
            totalBasicDA += emp.basicSalary() + emp.da(); // salaryForHRA
            totalHRAReceived += emp.hraReceived();
            totalLTAReceived += emp.ltaReceived();
            totalTransportReceived += emp.transportAllowance();
            totalCEAReceived += emp.childrenEducationAllowance();
            totalHostelReceived += emp.hostelExpenditureAllowance();
            totalUniformReceived += emp.uniformAllowance();
            totalOtherAllowances += emp.otherAllowances();
            totalBonus += emp.bonus();
            totalArrearSalary += emp.arrearSalary();
            // Section 17(2)
            totalGrossSalary17_2 += emp.perquisitesValue();
            // Section 17(3)
            totalGrossSalary17_3 += emp.profitsInLieu();
            // Exemptions inputs
            totalAnnualRentPaid += emp.annualRentPaid();
            totalActualLTAFare += emp.actualLtaFare();
            totalChildren += emp.numberOfChildren();
            totalCommutedPension += emp.commutedPensionReceived();
            totalGratuityReceived += emp.gratuityReceived();
            totalLeaveEncashmentReceived += emp.leaveEncashmentReceived();
            totalAverageMonthlySalary += emp.averageMonthlySalary();
            totalUnavailedLeaveDays += emp.unavailedLeaveDays();
            totalYearsOfService += emp.yearsOfService();
            // Flags
            if (emp.isGovernmentEmployee()) anyGovtEmployee = true;
            if (emp.isDisabledEmployee()) anyDisabledEmployee = true;
            if (!emp.isDomesticTravel()) allDomesticTravel = false;
            totalJourneysInBlock += emp.journeysInBlock();
            if (emp.gratuityAlsoReceived()) anyGratuityReceived = true;
            if (emp.commutedPensionReceived() > 0) anyCommutedPension = true;
            // Deductions
            totalProfessionalTax += emp.professionalTax();
            totalEntertainmentReceived += emp.entertainmentAllowanceReceived();
            totalEmployerNPS += emp.employerNPSContribution();
            totalTDS += emp.tdsDeducted();
        }

        int employerCount = employers.size();

        // ── Section 10 Exemptions ───────────────────────────────────────────────

        // HRA: Use first employer's city (primary employer)
        String primaryCity = employers.get(0).city();
        boolean isGovtEmp = employers.get(0).isGovernmentEmployee();
        long hraExempt = HRAExemption.compute(
            totalHRAReceived,
            totalBasicSalary,
            totalDA,
            totalCommission,
            totalAnnualRentPaid,
            primaryCity,
            ay,
            regime,
            isGovtEmp
        );

        // LTA
        long ltaExempt = LTAExemption.compute(
            totalLTAReceived,
            totalActualLTAFare,
            allDomesticTravel,
            totalJourneysInBlock,
            ay,
            regime
        );

        // Gratuity
        long gratuityExempt = GratuityExemption.compute(
            employers.get(0).averageMonthlySalary(), // last drawn monthly salary
            totalYearsOfService,
            totalGratuityReceived,
            anyGovtEmployee
        );

        // Leave Encashment
        long leaveEncashmentExempt = LeaveEncashmentExemption.compute(
            totalLeaveEncashmentReceived,
            totalAverageMonthlySalary / Math.max(employerCount, 1),
            totalUnavailedLeaveDays,
            totalYearsOfService / Math.max(employerCount, 1),
            anyGovtEmployee
        );

        // Pension Commutation
        long pensionCommutationExempt = PensionCommutationExemption.compute(
            totalCommutedPension,
            anyGovtEmployee,
            anyGratuityReceived
        );

        // Transport Allowance (use first employer for isDisabled flag)
        long transportExempt = SpecialAllowanceExemption.computeTransportAllowance(
            totalTransportReceived,
            employers.get(0).isDisabledEmployee(),
            regime
        );

        // Children Education Allowance
        long ceaExempt = SpecialAllowanceExemption.computeChildrenEducation(
            totalCEAReceived,
            totalChildren,
            regime
        );

        // Hostel Expenditure Allowance
        long hostelExempt = SpecialAllowanceExemption.computeHostelExpenditure(
            totalHostelReceived,
            totalChildren,
            regime
        );

        // Uniform Allowance
        long uniformExempt = SpecialAllowanceExemption.computeUniformAllowance(
            totalUniformReceived,
            totalUniformReceived, // actual expenditure — in production, use actualExpenditure field
            regime
        );

        long totalSection10Exempt = hraExempt + ltaExempt + gratuityExempt
            + leaveEncashmentExempt + pensionCommutationExempt
            + transportExempt + ceaExempt + hostelExempt + uniformExempt;

        // ── Section 16 Deductions ───────────────────────────────────────────────

        // Standard Deduction: applied once, amount set by caller based on regime
        long stdDed = standardDeductionPaise;

        // Entertainment Allowance: govt + old regime only
        long entAllowanceDed = EntertainmentAllowanceDeduction.compute(
            totalEntertainmentReceived,
            totalBasicSalary,
            anyGovtEmployee,
            regime
        );

        // Professional Tax: capped at Rs 2,500 per state/employer
        // Total PT deduction capped at Rs 2,500 per state — sum across employers
        long profTaxDed = Math.min(totalProfessionalTax, AssessmentYear.PROFESSIONAL_TAX_MAX);

        long totalSection16Deductions = stdDed + entAllowanceDed + profTaxDed;

        // ── Gross Salary Total ──────────────────────────────────────────────────
        // Section 17(1) components
        totalGrossSalary17_1 = totalBasicSalary + totalDA + totalCommission
            + totalHRAReceived + totalLTAReceived + totalTransportReceived
            + totalCEAReceived + totalHostelReceived + totalUniformReceived
            + totalOtherAllowances + totalBonus + totalArrearSalary
            + totalUncommutedPension;

        long grossSalaryTotal = totalGrossSalary17_1 + totalGrossSalary17_2 + totalGrossSalary17_3;

        // ── Net Taxable Salary ──────────────────────────────────────────────────
        // Net = Gross Salary − Section 10 Exemptions − Section 16 Deductions
        long netTaxableSalary = grossSalaryTotal - totalSection10Exempt - totalSection16Deductions;
        netTaxableSalary = Math.max(0L, netTaxableSalary);

        // ── HRA debug conditions ────────────────────────────────────────────────
        long salaryForHRA = totalBasicSalary + totalDA + totalCommission;
        long hraCond1 = totalHRAReceived;
        long hraCond2 = Math.max(totalAnnualRentPaid - (salaryForHRA / 10), 0L);
        HRAMetroCity metroCity = HRAMetroCity.fromCity(primaryCity, ay);
        long hraCond3 = (salaryForHRA * metroCity.getPercentageOfSalary()) / 100;
        boolean hraIsMetro = metroCity == HRAMetroCity.METRO;

        return new SalaryComputationResult(
            totalGrossSalary17_1,
            totalGrossSalary17_2,
            totalGrossSalary17_3,
            grossSalaryTotal,
            hraExempt,
            ltaExempt,
            gratuityExempt,
            leaveEncashmentExempt,
            pensionCommutationExempt,
            transportExempt,
            ceaExempt,
            hostelExempt,
            uniformExempt,
            totalSection10Exempt,
            stdDed,
            entAllowanceDed,
            profTaxDed,
            totalSection16Deductions,
            netTaxableSalary,
            employerCount,
            regime,
            ay,
            totalTDS,
            hraCond1,
            hraCond2,
            hraCond3,
            hraIsMetro,
            primaryCity
        );
    }

    /**
     * Convenience overload using AY 2026-27 and regime-based standard deduction.
     */
    public static SalaryComputationResult compute(
            List<EmployerEntry> employers,
            TaxRegime regime
    ) {
        long stdDed = regime == TaxRegime.NEW
            ? AssessmentYear.NEW_REGIME_STANDARD_DEDUCTION
            : AssessmentYear.OLD_REGIME_STANDARD_DEDUCTION;
        return compute(employers, stdDed, regime, AssessmentYear.AY_2026_27);
    }

    /**
     * Convenience overload: single employer, AY 2026-27.
     */
    public static SalaryComputationResult compute(
            EmployerEntry employer,
            TaxRegime regime
    ) {
        return compute(List.of(employer), regime);
    }
}
