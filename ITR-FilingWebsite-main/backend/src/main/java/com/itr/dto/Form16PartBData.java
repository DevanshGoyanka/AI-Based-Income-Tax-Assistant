package com.itr.dto;

import lombok.Data;

/**
 * Form 16 Part B Data - Salary computation by employer
 * Reference: ITR_Import_JSON_Validation.md Section 1
 */
@Data
public class Form16PartBData {
    // Salary components 17(1)
    private long basicSalary;
    private long dearnessAllowance;
    private long houseRentAllowance;
    private long leaveTravelAllowance;
    private long leaveEncashment;
    private long gratuity;
    private long bonus;
    private long commission;
    private long arrearsOfSalary;
    private long otherAllowances;
    private long totalSalary17_1;

    // Perquisites 17(2)
    private long totalPerquisites17_2;

    // Profits in lieu 17(3)
    private long totalProfitsInLieu17_3;

    // Gross salary
    private long grossSalary;

    // Exemptions u/s 10
    private long hraExemption10_13A;
    private long ltaExemption10_5;
    private long gratuityExemption10_10;
    private long leaveEncashmentExemption10_10AA;
    private long totalExemptionsUnder10;

    // Net salary
    private long netSalaryAfterExemptions;

    // Deductions u/s 16
    private long standardDeduction;
    private long professionalTax;
    private long entertainmentAllowance;

    // Taxable income
    private long taxableIncomeSalary;

    // Chapter VI-A deductions
    private long deduction80C;
    private long deduction80CCC;
    private long deduction80CCD_1;
    private long deduction80CCD_1B;
    private long deduction80CCD_2;
    private long deduction80D;
    private long deduction80E;
    private long deduction80G;
    private long totalDeductionsVIA;

    // Tax computation
    private long taxPayableOnSalary;
    private long reliefUnder89;
    private long netTaxPayable;
    private long tdsDeductedTotal;
}
