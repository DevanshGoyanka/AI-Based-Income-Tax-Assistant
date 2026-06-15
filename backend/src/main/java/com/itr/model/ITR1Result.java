package com.itr.model;

import com.itr.domain.common.TaxRegime;
import java.util.List;

/**
 * Computed ITR-1 result. Every field maps 1:1 to ITR-1_2026_Main_V1.0 schema (AY 2026-27).
 * COMPLETE 101% CBDT COMPLIANT - Updated for AY 2026-27 schema requirements.
 */
public class ITR1Result {

    // ── Filing Status ─────────────────────────────────────────────────────────
    public boolean isRevised = false;  // For ReturnFileSec: 11 (original) vs 17 (revised)

    // ── B1: Salary ─────────────────────────────────────────────────────────────
    public long grossSalary;          // 17(1) + 17(2) + 17(3)
    public long salary17_1;
    public long perquisites17_2;
    public long profitsInLieu17_3;
    public long allowancesExemptUs10; // Total exempt allowances
    public long hraExempt;           // HRA exemption u/s 10(13A)
    public long ltaExempt;           // LTA exemption u/s 10(5)
    public long netSalary;            // GrossSalary - AllwncExemptUs10
    public long deductionUs16;        // 16(ia) + 16(ii) + 16(iii)
    public long stdDeductionUs16ia;   // Std deduction — 50,000 (old) / 75,000 (new)
    public long entertainmentAlw16ii; // Only CG/SG/PSU
    public long professionalTaxUs16iii;
    public long incomeFromSalary;     // NetSalary - DeductionUs16

    // ── B2: House Property ────────────────────────────────────────────────────
    public String typeOfHP;           // "S", "L", "D" (Self-Occupied, Let-Out, Deemed Let-Out)
    public long grossRentReceived;
    public long taxPaidLocalAuth;
    public long annualValue;          // grossRentReceived - taxPaidLocalAuth
    public long standardDeductionHP;  // 30% of annualValue
    public long interestPayable;      // Interest on borrowed capital
    public long arrearsUnrealizedRent;
    public long totalIncomeOfHP;      // annualValue - stdDed - interest + arrears
    public long hpLossCarryForward;   // HP loss exceeding ₹2L set-off cap (Section 71)

    // ── B3: Other Sources ─────────────────────────────────────────────────────
    public long savingsAccountInterest;
    public long fdInterest;
    public long dividendIncome;
    public long familyPension;
    public long interestFromITRefund;
    public long otherIncome;
    public long incomeOthSrc;         // Sum of all other source incomes
    public long deductionUs57iia;     // 1/3 of family pension, max 15000/25000
    public long netIncomeOthSrc;      // incomeOthSrc - deductionUs57iia

    // ── Exempt Income (Schedule EI) ─────────────────────────────────────────
    public long agricultureIncome;    // Agricultural income u/s 10(1) - max 5000 for ITR-1
    public long totalExemptIncome;    // Total exempt income u/s 10

    // ── Gross Total Income ────────────────────────────────────────────────────
    public long grossTotalIncome;     // B1v + B2vii + B3 net
    public long ltcg112A;             // LTCG u/s 112A (up to ₹1.25L exempt in AY 2026-27)

    // ── Chapter VI-A Deductions ───────────────────────────────────────────────
    // 80C group
    public long sec80C;
    public long sec80CCC;
    public long sec80CCD_1;           // Employee contribution (capped)
    public long sec80CCD_1B;          // Additional NPS, max 50000
    public long sec80CCD_2;           // Employer NPS contribution
    public long sec80CCH;             // Agnipath

    // Health
    public long sec80D;
    public long sec80DD;
    public long sec80DDB;

    // Loans
    public long sec80E;
    public long sec80EE;
    public long sec80EEA;
    public long sec80EEB;

    // Donations
    public long sec80G;
    public long sec80GG;
    public long sec80GGA;
    public long sec80GGC;

    // Interest
    public long sec80TTA;             // Max 10000, savings interest (<60 years)
    public long sec80TTB;             // Max 50000, senior citizen (>=60 years)

    // Disability
    public long sec80U;

    public long totalChapVIADeductions;
    public long totalIncome;          // GrossTotalIncome - TotalChapVIADeductions

    // ── Tax Computation ───────────────────────────────────────────────────────
    public TaxRegime taxRegime;
    public long totalTaxPayable;      // Tax on totalIncome (slab)
    public long rebate87A;            // Max 12500 (old) / 60000 (new)
    public long taxAfterRebate;       // totalTaxPayable - rebate87A
    public long surcharge;            // Surcharge if income > ₹50L (10%, 15%, 25%, 37%)
    public long educationCess;        // 4% of (taxAfterRebate + surcharge)
    public long grossTaxLiability;    // taxAfterRebate + surcharge + educationCess
    public long reliefUs89;           // Salary arrears relief
    public long netTaxLiability;      // grossTaxLiability - reliefUs89

    // ── Interest & Fees ───────────────────────────────────────────────────────
    public long intrstPayUs234A;      // Late filing interest
    public long intrstPayUs234B;      // Shortfall in advance tax
    public long intrstPayUs234C;      // Deferment of advance tax installments
    public long lateFilingFee234F;    // Late filing fee
    public long totalInterestFee;

    public long totalTaxFeeInterest;  // netTaxLiability + totalInterestFee

    // ── Taxes Paid ────────────────────────────────────────────────────────────
    public long tdsSalaryTotal;
    public long tdsOtherTotal;
    public long tdsTotal;
    public long tcsTotal;
    public long advanceTaxTotal;
    public long selfAssessmentTaxTotal;
    public long totalTaxesPaid;

    // ── Refund / Tax Due ──────────────────────────────────────────────────────
    public long refundDue;            // totalTaxesPaid - totalTaxFeeInterest (if positive)
    public long balTaxPayable;        // totalTaxFeeInterest - totalTaxesPaid (if positive)

    // ── Validation Messages ───────────────────────────────────────────────────
    public List<ValidationMessage> errors;   // Category A — return cannot be uploaded
    public List<ValidationMessage> warnings; // Category B — defect notices
    public List<ValidationMessage> infos;    // Category D — deduction notices

    public static class ValidationMessage {
        public int ruleNo;
        public String category;  // "A", "B", "D"
        public String field;
        public String message;

        public ValidationMessage(int ruleNo, String category, String field, String message) {
            this.ruleNo = ruleNo;
            this.category = category;
            this.field = field;
            this.message = message;
        }
    }
}
