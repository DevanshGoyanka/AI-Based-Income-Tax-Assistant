package com.itr.domain.salary;

import com.itr.domain.common.TaxRegime;

/**
 * EmployerEntry — per-employer salary data for AY 2026-27.
 * All amounts in PAISE (1 rupee = 100 paise).
 */
public record EmployerEntry(
    // Core employer info
    String employerName,
    String tan,
    
    // Section 17(1) — Gross Salary
    long basicSalary,
    long da,
    long commission,
    long hraReceived,
    long ltaReceived,
    long transportAllowance,
    long childrenEducationAllowance,
    long hostelExpenditureAllowance,
    long uniformAllowance,
    long otherAllowances,
    long bonus,
    long arrearSalary,
    
    // Section 17(2) — Perquisites
    long perquisitesValue,
    
    // Section 17(3) — Profits in Lieu
    long profitsInLieu,
    
    // Additional perquisites
    long rentFreeAccomValue,
    long concessionalRentValue,
    long motorCarValue,
    long otherPerquisites,
    
    // HRA inputs
    long annualRentPaid,
    String city,
    long cityPopulation,
    boolean employerOwnedAccommodation,
    long actualRentByEmployer,
    
    // Taxable benefits
    long commutedPensionReceived,
    boolean gratuityAlsoReceived,
    long gratuityReceived,
    long leaveEncashmentReceived,
    long averageMonthlySalary,
    int unavailedLeaveDays,
    long uncommutedPensionMonthly,
    long actualLtaFare,
    int numberOfChildren,
    boolean isDomesticTravel,
    int journeysInBlock,
    int yearsOfService,
    
    // Employer details
    boolean isGovernmentEmployee,
    boolean isDisabledEmployee,
    
    // Deductions
    long professionalTax,
    long entertainmentAllowanceReceived,
    long employerNPSContribution,
    
    // TDS
    long tdsDeducted
) {
    /** Salary for HRA purposes: Basic + DA + Commission */
    public long salaryForHRA() {
        return basicSalary + da + commission;
    }

    /** Gross salary from this employer: Section 17(1+2+3) */
    public long grossSalary() {
        return basicSalary + da + commission + hraReceived + ltaReceived
             + transportAllowance + childrenEducationAllowance + hostelExpenditureAllowance
             + uniformAllowance + otherAllowances + bonus + arrearSalary
             + perquisitesValue + profitsInLieu;
    }

    /** Factory for a minimal employer entry */
    public static EmployerEntry minimal(
            String employerName, String tan,
            long basicSalary, long da, 
            long hra, long lta, long professionalTax, long tds,
            boolean isGovt, boolean isDisabled, String city
    ) {
        return new EmployerEntry(
            employerName, tan,
            // Section 17(1)
            basicSalary, da, 0L, hra, lta, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            // Section 17(2)
            0L,
            // Section 17(3)
            0L,
            // Perquisites
            0L, 0L, 0L, 0L,
            // HRA inputs
            0L, city, 0L, false, 0L,
            // Taxable benefits - all zeros
            0L, false, 0L, 0L, 0L, 0, 0L, 0L, 0, false, 0, 0,
            // Employer details
            isGovt, isDisabled,
            // Deductions
            professionalTax, 0L, 0L,
            // TDS
            tds
        );
    }
}
