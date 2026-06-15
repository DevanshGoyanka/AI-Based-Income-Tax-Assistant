package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ITR Form Data DTO - represents the complete ITR form data for JSON export.
 * All monetary values are stored in paise (multiply by 100).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ITRFormData {

    private Long clientId;
    private String pan;
    private String assessmentYear;
    private String itrType;

    // Personal Information
    private String firstName;
    private String lastName;
    private String fatherName;
    private String dob;
    private String gender;
    private String mobile;
    private String email;
    private String residentialStatus;

    // Income - Salary
    private long salaryIncome;
    private long allowances;
    private long perquisites;
    private long profitsInSalary;
    private long standardDeduction;
    private long professionalTax;

    // Income - House Property
    private long housePropertyIncome;
    private long propertyTax;

    // Income - Capital Gains
    private long stcg111A;
    private long ltcg112A;
    private long ltcg112AVRD;
    private long stcgOnProperty;
    private long ltcgOnProperty;
    private long vdaGains;

    // Income - Business
    private long businessIncome;
    private long presumptiveBusinessIncome;

    // Income - Other Sources
    private long otherSourcesIncome;
    private long interestIncome;
    private long dividendIncome;
    private long familyPension;

    // Gross Total Income
    private long grossTotalIncome;

    // Deductions (Chapter VI-A) - in Paise
    private long section80C;
    private long section80CCC;
    private long section80CCD1;
    private long section80CCD1B;
    private long section80CCD2;
    private long section80D;
    private long section80DD;
    private long section80DDB;
    private long section80E;
    private long section80EE;
    private long section80EEA;
    private long section80G;
    private long section80GGC;
    private long section80GG;
    private long section80P;
    private long section80TTA;
    private long section80TTB;
    private long section80U;
    private long section80CCH2;
    private long section80JJAA;

    // Total Deductions
    private long totalDeductions;

    // Total Income
    private long totalIncome;

    // Tax Computation
    private long taxOnTotalIncome;
    private long taxOnSpecialRateIncome;
    private long grossTaxLiability;
    private long rebate87A;
    private long taxAfterRebate;
    private long surcharge;
    private long healthAndEducationCess;
    private long totalTaxLiability;
    private long relief89;

    // TDS
    private long tdsOnSalary;
    private long tdsOnOtherThanSalary;
    private long tcs;
    private long totalTds;

    // Tax Paid
    private long advanceTax;
    private long selfAssessmentTax;
    private long totalTaxPaid;

    // Final Tax Calculation
    private long balanceTaxPayable;
    private long refundAmount;

    // Interest
    private long interest234A;
    private long interest234B;
    private long interest234C;
    private long fee234F;
    private long totalInterestAndFees;

    // Regimes
    private String oldRegimeTotalIncome;
    private String newRegimeTotalIncome;
    private String recommendedRegime;

    // Verification
    private boolean verified;
    private String verificationDate;
    private String verfierName;
}
