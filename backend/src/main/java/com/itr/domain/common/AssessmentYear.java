package com.itr.domain.common;

import java.time.LocalDate;
import java.util.Map;

/**
 * AssessmentYear — SINGLE SOURCE OF TRUTH for all AY 2026-27 tax constants.
 * <p>
 * Every service, calculator, and validator reads from here.
 * When Finance Act changes: update this file ONLY. Commit with tag "AY-2027-28-BUDGET-UPDATE".
 * <p>
 * All monetary values are in paise (Indian smallest unit: 1 rupee = 100 paise).
 * Rates are in basis points (bps: 1 bps = 0.01%).
 */
public final class AssessmentYear {

    private AssessmentYear() {}

    // ═════════════════════════════════════════════════════════════════════
    // IDENTIFICATION
    // ═════════════════════════════════════════════════════════════════════
    public static final String AY_2026_27 = "2026-27";
    public static final String FY_2025_26 = "2025-26";
    public static final String FINANCE_ACT = "Finance Act 2025 (Budget Feb 1, 2025)";

    // ═════════════════════════════════════════════════════════════════════
    // NEW REGIME SLABS (default regime) — Finance Act 2025, Section 115BAC
    // ═════════════════════════════════════════════════════════════════════
    // Each slab: {from_paise, to_paise, rate_bps}
    // 500 bps = 5%, 1000 bps = 10%, etc.
    public static final long[][] NEW_REGIME_SLABS = {
        {0L,             4_00_00000L,    0},   // Nil
        {4_00_00001L,    8_00_00000L,  500},   // 5%
        {8_00_00001L,   12_00_00000L, 1000},   // 10%
        {12_00_00001L,  16_00_00000L, 1500},   // 15%
        {16_00_00001L,  20_00_00000L, 2000},   // 20%
        {20_00_00001L,  24_00_00000L, 2500},   // 25%
        {24_00_00001L,  Long.MAX_VALUE, 3000}   // 30%
    };

    public static final long NEW_REGIME_REBATE_INCOME_LIMIT  = 12_00_00000L;
    public static final long NEW_REGIME_REBATE_TAX_AMOUNT    = 60_00000L;
    public static final long NEW_REGIME_STANDARD_DEDUCTION   = 75_00000L;
    public static final long NEW_REGIME_FAMILY_PENSION_DEDUCTION = 25_00000L;

    // ═════════════════════════════════════════════════════════════════════
    // OLD REGIME SLABS — Finance Act 2017 as amended
    // ═════════════════════════════════════════════════════════════════════
    public static final long[][] OLD_REGIME_SLABS_BELOW60 = {
        {0L,            2_50_00000L,    0},
        {2_50_00001L,   5_00_00000L,  500},
        {5_00_00001L,  10_00_00000L, 2000},
        {10_00_00001L, Long.MAX_VALUE, 3000}
    };

    public static final long[][] OLD_REGIME_SLABS_SENIOR = {
        {0L,            3_00_00000L,    0},
        {3_00_00001L,   5_00_00000L,  500},
        {5_00_00001L,  10_00_00000L, 2000},
        {10_00_00001L, Long.MAX_VALUE, 3000}
    };

    public static final long[][] OLD_REGIME_SLABS_SUPER_SENIOR = {
        {0L,            5_00_00000L,    0},
        {5_00_00001L,  10_00_00000L, 2000},
        {10_00_00001L, Long.MAX_VALUE, 3000}
    };

    public static final long OLD_REGIME_REBATE_INCOME_LIMIT  = 5_00_00000L;
    public static final long OLD_REGIME_REBATE_MAX_AMOUNT    = 12_50000L;
    public static final long OLD_REGIME_STANDARD_DEDUCTION   = 50_00000L;

    // ═════════════════════════════════════════════════════════════════════
    // SURCHARGE
    // ═════════════════════════════════════════════════════════════════════
    // {income_threshold_from_paise, income_threshold_to_paise, old_regime_bps, new_regime_bps}
    public static final long[][] SURCHARGE_SLABS = {
        {0L,             50_00_00000L,     0,    0},
        {50_00_00001L,   1_00_00_00000L, 1000, 1000},
        {1_00_00_00001L, 2_00_00_00000L, 1500, 1500},
        {2_00_00_00001L, 5_00_00_00000L, 2500, 1500},
        {5_00_00_00001L, Long.MAX_VALUE,  3700, 1500},
    };

    public static final int SURCHARGE_CAP_CAPITAL_GAINS_BPS = 1500;

    // ═════════════════════════════════════════════════════════════════════
    // CESS
    // ═════════════════════════════════════════════════════════════════════
    public static final int CESS_RATE_BPS = 400; // 4%

    // ═════════════════════════════════════════════════════════════════════
    // CHAPTER VI-A DEDUCTION LIMITS (OLD REGIME ONLY — new regime allows only 80CCD(2), 80CCH(2), 80JJAA)
    // ═════════════════════════════════════════════════════════════════════
    public static final long LIMIT_80C_COMBINED         = 1_50_00000L;
    public static final long LIMIT_80CCD_1B             =    50_00000L;
    public static final int  LIMIT_80CCD_2_GOVT_PCT_BPS =          1400; // 14%
    public static final int  LIMIT_80CCD_2_PVTCO_PCT_BPS =         1000; // 10%
    public static final long LIMIT_80D_SELF_BELOW60     =    25_00000L;
    public static final long LIMIT_80D_SELF_ABOVE60     =    50_00000L;
    public static final long LIMIT_80D_PARENTS_BELOW60  =    25_00000L;
    public static final long LIMIT_80D_PARENTS_ABOVE60  =    50_00000L;
    public static final long LIMIT_80D_PREVENTIVE_HCU   =     5_00000L;
    public static final long LIMIT_80DD_NORMAL          =    75_00000L;
    public static final long LIMIT_80DD_SEVERE          = 1_25_00000L;
    public static final long LIMIT_80DDB_BELOW60        =    40_00000L;
    public static final long LIMIT_80DDB_ABOVE60        = 1_00_00000L;
    public static final long LIMIT_80E_INTEREST         = Long.MAX_VALUE;
    public static final long LIMIT_80EEA                = 1_50_00000L;
    public static final long LIMIT_80EEB                = 1_50_00000L;
    public static final long LIMIT_80GG_MONTHLY         =     5_00000L;
    public static final long LIMIT_80TTA                =    10_00000L;
    public static final long LIMIT_80TTB                =    50_00000L;
    public static final long LIMIT_80U_NORMAL           =    75_00000L;
    public static final long LIMIT_80U_SEVERE           = 1_25_00000L;

    // ═════════════════════════════════════════════════════════════════════
    // SALARY EXEMPTIONS
    // ═════════════════════════════════════════════════════════════════════
    public static final long GRATUITY_EXEMPTION_MAX     = 20_00_00000L;
    public static final long LEAVE_ENCASHMENT_MAX       = 25_00_00000L;
    public static final long VRS_EXEMPTION_MAX          = 5_00_00000L;
    public static final long COMMUTED_PENSION_GOVT_PCT  = Long.MAX_VALUE;
    public static final long FAMILY_PENSION_STD_DED_MAX  = 15_00000L;
    // Rs 2,500 professional tax per state — per CBDT validation rule S-04
    public static final long PROFESSIONAL_TAX_MAX        = 2_50_00000L;

    // ═════════════════════════════════════════════════════════════════════
    // HOUSE PROPERTY
    // ═════════════════════════════════════════════════════════════════════
    public static final long HP_INTEREST_SELF_OCC_LIMIT = 2_00_00000L;
    public static final int  HP_STANDARD_DED_RATE_BPS   = 3000;
    public static final long HP_INTER_HEAD_LOSS_LIMIT   = 2_00_00000L;

    // ═════════════════════════════════════════════════════════════════════
    // CAPITAL GAINS — Finance Act 2024 (effective FY 2024-25 / AY 2025-26)
    // ═════════════════════════════════════════════════════════════════════
    public static final int  STCG_111A_RATE_BPS           = 2000;
    public static final int  LTCG_112A_RATE_BPS           = 1250;
    public static final long LTCG_112A_EXEMPTION          = 1_25_00000L;
    public static final int  LTCG_112_WITH_INDEXATION     = 2000;
    public static final int  LTCG_112_WITHOUT_INDEXATION  = 1250;
    public static final long PROPERTY_INDEXATION_CUTOFF   = 20240723L;
    public static final int  VDA_RATE_BPS                 = 3000;
    public static final int  LOTTERY_RATE_BPS             = 3000;
    public static final int  STCG_OTHER_RATE_BPS          = -1;

    // CII Table (Cost Inflation Index)
    public static final Map<Integer, Integer> CII_TABLE = Map.ofEntries(
        Map.entry(2001, 100), Map.entry(2002, 105), Map.entry(2003, 109),
        Map.entry(2004, 113), Map.entry(2005, 117), Map.entry(2006, 122),
        Map.entry(2007, 129), Map.entry(2008, 137), Map.entry(2009, 148),
        Map.entry(2010, 167), Map.entry(2011, 184), Map.entry(2012, 200),
        Map.entry(2013, 220), Map.entry(2014, 240), Map.entry(2015, 254),
        Map.entry(2016, 264), Map.entry(2017, 272), Map.entry(2018, 280),
        Map.entry(2019, 289), Map.entry(2020, 301), Map.entry(2021, 317),
        Map.entry(2022, 331), Map.entry(2023, 348), Map.entry(2024, 363)
    );

    // ═════════════════════════════════════════════════════════════════════
    // ADVANCE TAX
    // ═════════════════════════════════════════════════════════════════════
    public static final long ADVANCE_TAX_THRESHOLD         = 10_00000L;
    public static final int  ADV_TAX_Q1_PCT_BPS           = 1500;  // 15% by Jun 15
    public static final int  ADV_TAX_Q2_PCT_BPS           = 4500;  // 45% by Sep 15
    public static final int  ADV_TAX_Q3_PCT_BPS           = 7500;  // 75% by Dec 15
    public static final int  ADV_TAX_Q4_PCT_BPS           = 10000; // 100% by Mar 15

    // ═════════════════════════════════════════════════════════════════════
    // INTEREST RATES (simple interest per month)
    // ═════════════════════════════════════════════════════════════════════
    public static final int INTEREST_234A_BPS  = 100;
    public static final int INTEREST_234B_BPS  = 100;
    public static final int INTEREST_234C_BPS  = 100;

    // ═════════════════════════════════════════════════════════════════════
    // LATE FILING FEE (Section 234F)
    // ═════════════════════════════════════════════════════════════════════
    public static final long LATE_FEE_HIGH_INCOME = 5_00000L;
    public static final long LATE_FEE_LOW_INCOME  = 1_00000L;

    // ═════════════════════════════════════════════════════════════════════
    // ITR DUE DATES (AY 2026-27 / FY 2025-26)
    // ═════════════════════════════════════════════════════════════════════
    public static final LocalDate DUE_DATE_NON_AUDIT   = LocalDate.of(2026, 7, 31);
    public static final LocalDate DUE_DATE_AUDIT        = LocalDate.of(2026, 10, 31);
    public static final LocalDate DUE_DATE_TRANSFER_PRICING = LocalDate.of(2026, 11, 30);
    public static final LocalDate DUE_DATE_BELATED      = LocalDate.of(2026, 12, 31);

    // ═════════════════════════════════════════════════════════════════════
    // PRESUMPTIVE TAXATION
    // ═════════════════════════════════════════════════════════════════════
    public static final long LIMIT_44AD_TURNOVER         = 2_00_00_00000L;
    public static final int  RATE_44AD_NON_DIGITAL_BPS   = 800;
    public static final int  RATE_44AD_DIGITAL_BPS       = 600;
    public static final long LIMIT_44ADA_RECEIPTS        = 50_00_00000L;
    public static final int  RATE_44ADA_BPS              = 5000;

    // ═════════════════════════════════════════════════════════════════════
    // AMT & MAT
    // ═════════════════════════════════════════════════════════════════════
    public static final int AMT_RATE_BPS  = 1850;
    public static final int MAT_RATE_BPS  = 1500;

    // ═════════════════════════════════════════════════════════════════════
    // LOSS CARRY-FORWARD PERIODS (in years)
    // ═════════════════════════════════════════════════════════════════════
    public static final int HP_LOSS_CARRY_FORWARD_YEARS        = 8;
    public static final int BUSINESS_LOSS_CARRY_FORWARD_YEARS  = 8;
    public static final int SPECULATIVE_LOSS_CF_YEARS          = 4;
    public static final int STCG_LOSS_CARRY_FORWARD_YEARS      = 8;
    public static final int LTCG_LOSS_CARRY_FORWARD_YEARS      = 8;
    public static final int DEPRECIATION_CARRY_FORWARD_YEARS   = -1;   // UNLIMITED
    public static final int SPECIFIED_BUSINESS_35AD_CF_YEARS   = 15;   // Section 35AD

    // ═════════════════════════════════════════════════════════════════════
    // HELPER: Get CII for a financial year
    // ═════════════════════════════════════════════════════════════════════
    public static int getCII(int financialYear) {
        return CII_TABLE.getOrDefault(financialYear, 0);
    }

    public static long[] getSlabsForOldRegime(AgeCategory ageCategory) {
        return switch (ageCategory) {
            case BELOW_60 -> flattenSlabs(OLD_REGIME_SLABS_BELOW60);
            case SENIOR -> flattenSlabs(OLD_REGIME_SLABS_SENIOR);
            case SUPER_SENIOR -> flattenSlabs(OLD_REGIME_SLABS_SUPER_SENIOR);
        };
    }

    public static long[] getNewRegimeRatesArray() {
        return flattenSlabs(NEW_REGIME_SLABS);
    }

    private static long[] flattenSlabs(long[][] slabs) {
        long[] result = new long[slabs.length * 3];
        int idx = 0;
        for (long[] slab : slabs) {
            result[idx++] = slab[0];
            result[idx++] = slab[1];
            result[idx++] = slab[2];
        }
        return result;
    }
}
