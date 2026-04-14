package com.itr.dto;

import com.itr.model.ITR1Result;

import java.util.List;

/**
 * DTOs for unified tax calculation API.
 * Note: ITR-2 support has been removed.
 */
public class TaxCalculationDtos {

    // ── Regime Comparison ─────────────────────────────────────────────────────
    
    public static class RegimeComparisonRequest {
        public String rawPrefillJson;
    }

    public static class RegimeComparisonResponse {
        public String recommendedRegime;
        public long taxSavings;
        public String recommendation;
        public OldRegimeDetails oldRegime;
        public NewRegimeDetails newRegime;

        public static class OldRegimeDetails {
            public long grossTotalIncome;
            public long totalDeductions;
            public long totalIncome;
            public long taxBeforeRebate;
            public long rebate87A;
            public long taxAfterRebate;
            public long educationCess;
            public long totalTaxLiability;
            public long sec80CTotal;
            public long sec80D;
            public long sec80E;
            public long otherDeductions;
        }

        public static class NewRegimeDetails {
            public long grossTotalIncome;
            public long totalDeductions;
            public long totalIncome;
            public long taxBeforeRebate;
            public long rebate87A;
            public long taxAfterRebate;
            public long educationCess;
            public long totalTaxLiability;
        }
    }

    // ── Form Selection ────────────────────────────────────────────────────────
    
    public static class FormSelectionRequest {
        public String rawPrefillJson;
        public long totalIncome;
        public boolean hasCapitalGains;
        public boolean hasMultipleProperties;
        public boolean hasBusinessIncome;
        public boolean hasForeignIncome;
    }

    public static class FormSelectionResponse {
        public String selectedForm;
        public String formDescription;
        public String selectionReason;
        public boolean eligibleForITR1;
        public boolean requiresITR2;
    }

    // ── ITR-1 Calculation ─────────────────────────────────────────────────────
    
    public static class ITR1CalculationRequest {
        public String rawPrefillJson;
        public String filingDate;
        public boolean autoSelectRegime;
        public DeductionOverrides deductionOverrides;
    }

    public static class ITR1CalculationResponse {
        public String taxRegime;
        public long grossSalary;
        public long incomeFromSalary;
        public long totalIncomeOfHP;
        public long netIncomeOthSrc;
        public long grossTotalIncome;
        public long totalDeductions;
        public long totalIncome;
        public long totalTaxPayable;
        public long rebate87A;
        public long taxAfterRebate;
        public long educationCess;
        public long grossTaxLiability;
        public long netTaxLiability;
        public long totalTaxFeeInterest;
        public long totalTaxesPaid;
        public long refundDue;
        public long balTaxPayable;
        public boolean canUpload;
        public List<ITR1Result.ValidationMessage> errors;
        public List<ITR1Result.ValidationMessage> warnings;
    }

    // ── Smart Calculation ─────────────────────────────────────────────────────
    // Note: ITR-2 fields removed but kept for backward compatibility
    
    public static class SmartCalculationRequest {
        public String rawPrefillJson;
        public String filingDate;
        public long estimatedTotalIncome;
        public boolean hasCapitalGains;
        public boolean hasMultipleProperties;
        public boolean hasBusinessIncome;
        public boolean hasForeignIncome;
    }

    public static class SmartCalculationResponse {
        public String selectedForm;
        public String selectedRegime;
        public long regimeSavings;
        public String regimeRecommendation;
        public long totalIncome;
        public long totalTaxLiability;
        public long refundDue;
        public long balTaxPayable;
        public boolean canUpload;
    }

    // ── Common DTOs ───────────────────────────────────────────────────────────
    
    public static class DeductionOverrides {
        public Long section80C;
        public Long section80CCC;
        public Long section80CCD_Employee;
        public Long section80CCD_1B;
        public Long section80CCD_Employer;
        public Long section80D;
        public Long section80DD;
        public Long section80DDB;
        public Long section80E;
        public Long section80EE;
        public Long section80EEA;
        public Long section80EEB;
        public Long section80G;
        public Long section80GG;
        public Long section80GGA;
        public Long section80GGC;
        public Long section80TTA;
        public Long section80TTB;
        public Long section80U;
        public Long reliefUs89;
    }
}
