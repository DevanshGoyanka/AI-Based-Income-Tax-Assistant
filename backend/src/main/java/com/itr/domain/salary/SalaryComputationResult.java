package com.itr.domain.salary;

import com.itr.domain.common.TaxRegime;

/**
 * SalaryScheduleComputer result — comprehensive computation of income from salaries.
 *
 * All amounts in PAISE (1 rupee = 100 paise).
 */
public record SalaryComputationResult(
    // ── Gross Salary Section 17 ──────────────────────────────────────
    long grossSalarySection17_1,       // Section 17(1): Salary + Allowances + Arrears
    long grossSalarySection17_2,       // Section 17(2): Perquisites
    long grossSalarySection17_3,       // Section 17(3): Profits in lieu of salary
    long grossSalaryTotal,             // 17(1) + 17(2) + 17(3)

    // ── Section 10 Exemptions ───────────────────────────────────────
    long hraExempt,                    // Section 10(13A)
    long ltaExempt,                   // Section 10(5)
    long gratuityExempt,               // Section 10(10)
    long leaveEncashmentExempt,        // Section 10(10AA)
    long pensionCommutationExempt,     // Section 10(10A)
    long transportAllowanceExempt,     // Section 10(14)(i)
    long childrenEducationExempt,      // Section 10(14)
    long hostelExpenditureExempt,     // Section 10(14)
    long uniformAllowanceExempt,      // Section 10(14)
    long totalSection10Exempt,        // Sum of all Section 10 exemptions

    // ── Section 16 Deductions ─────────────────────────────────────────
    long standardDeduction,           // Section 16(ia): Rs 75K new / Rs 50K old
    long entertainmentAllowanceDed,    // Section 16(ii): Govt + old reg only
    long professionalTaxDed,          // Section 16(iii): per-state limit
    long totalSection16Deductions,     // Sum of 16(ia) + 16(ii) + 16(iii)

    // ── Net Salary ───────────────────────────────────────────────────
    long netTaxableSalary,            // Gross − Exemptions − Sec16 deductions
    long employerCount,               // Number of employers
    TaxRegime regimeUsed,             // Which regime was used for computation
    String assessmentYear,             // AY e.g. "2026-27"

    // ── TDS & Credits ─────────────────────────────────────────────────
    long totalTDSDeducted,

    // ── HRA Debug (optional) ─────────────────────────────────────────
    long hraCondition1_Actual,        // Actual HRA received
    long hraCondition2_RentMinus10Pct, // Rent − 10% salary
    long hraCondition3_MetroPct,      // Metro/non-metro %
    boolean hraIsMetroCity,           // Was city classified as metro?
    String hraCityClassified          // City name used for classification
) {
    /** Empty result for no employers */
    public static SalaryComputationResult empty(String ay, TaxRegime regime) {
        return new SalaryComputationResult(
            0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L,
            0L,
            regime, ay, 0L,
            0L, 0L, 0L, false, ""
        );
    }
}
