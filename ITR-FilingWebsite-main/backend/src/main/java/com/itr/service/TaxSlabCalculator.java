package com.itr.service;

import com.itr.model.TaxRegime;
import org.springframework.stereotype.Component;

/**
 * Computes income tax before cess/rebate for AY 2026-27 (Budget 2025/2026).
 *
 * NEW REGIME slabs (u/s 115BAC, AY 2026-27):
 *   0        –  4,00,000  → 0%
 *   4,00,001 –  8,00,000  → 5%
 *   8,00,001 – 12,00,000  → 10%
 *  12,00,001 – 16,00,000  → 15%
 *  16,00,001 – 20,00,000  → 20%
 *  20,00,001 – 24,00,000  → 25%
 *  > 24,00,000            → 30%
 *  Standard deduction: ₹75,000 (new regime)
 *  Rebate 87A: Total Income ≤ 12,00,000 → full tax rebate (max ₹60,000)
 *
 * OLD REGIME slabs:
 *   0        –  2,50,000  → 0%
 *   2,50,001 –  5,00,000  → 5%
 *   5,00,001 – 10,00,000  → 20%
 *  > 10,00,000            → 30%
 *  Standard deduction: ₹50,000 (old regime)
 *  Rebate 87A: Total Income ≤ 5,00,000 → full tax rebate (max ₹12,500)
 *
 * Senior citizen (>= 60): basic exemption 3,00,000 (old regime)
 * Super senior (>= 80):   basic exemption 5,00,000 (old regime)
 */
@Component
public class TaxSlabCalculator {

    // ── New Regime ─────────────────────────────────────────────────────────────

    /**
     * @param taxableIncome Income after all deductions (new regime — no Ch VI-A except 80CCD2)
     * @param ageYears      Assessee age as of April 1 of FY
     * @return Tax before cess (includes surcharge logic if needed)
     */
    public long computeNewRegimeTax(long taxableIncome, int ageYears) {
        if (taxableIncome <= 0) return 0;

        long tax = 0;

        if (taxableIncome <= 400_000) {
            tax = 0;
        } else if (taxableIncome <= 800_000) {
            tax = (taxableIncome - 400_000) * 5 / 100;
        } else if (taxableIncome <= 1_200_000) {
            tax = 20_000 + (taxableIncome - 800_000) * 10 / 100;
        } else if (taxableIncome <= 1_600_000) {
            tax = 60_000 + (taxableIncome - 1_200_000) * 15 / 100;
        } else if (taxableIncome <= 2_000_000) {
            tax = 120_000 + (taxableIncome - 1_600_000) * 20 / 100;
        } else if (taxableIncome <= 2_400_000) {
            tax = 200_000 + (taxableIncome - 2_000_000) * 25 / 100;
        } else {
            tax = 300_000 + (taxableIncome - 2_400_000) * 30 / 100;
        }

        return tax;
    }

    /**
     * Rebate u/s 87A for new regime (AY 2026-27, Budget 2025/2026):
     * - Total income ≤ ₹12,00,000: rebate = min(tax, ₹60,000)
     * - Total income > ₹12,00,000: NO rebate
     * 
     * Note: Marginal relief applies for income between ₹12,00,001 and ₹12,75,000
     * where tax cannot exceed (income - ₹12,00,000)
     */
    public long computeRebate87ANewRegime(long totalIncome, long taxBeforeRebate) {
        if (totalIncome <= 1_200_000) {
            return Math.min(taxBeforeRebate, 60_000); // Max ₹60,000 rebate
        }
        return 0; // No rebate if income > ₹12L
    }

    // ── Old Regime ─────────────────────────────────────────────────────────────

    /**
     * @param taxableIncome Income after all Ch VI-A deductions
     * @param ageYears      Assessee age as of April 1 of PY
     */
    public long computeOldRegimeTax(long taxableIncome, int ageYears) {
        if (taxableIncome <= 0) return 0;

        long exempt = basicExemptionOldRegime(ageYears);
        long tax    = 0;

        if (taxableIncome <= exempt) {
            return 0;
        }

        if (ageYears >= 80) {
            // Super senior: 0 – 5L = 0%, 5L–10L = 20%, >10L = 30%
            if (taxableIncome <= 500_000) {
                tax = 0;
            } else if (taxableIncome <= 1_000_000) {
                tax = (taxableIncome - 500_000) * 20 / 100;
            } else {
                tax = 100_000 + (taxableIncome - 1_000_000) * 30 / 100;
            }
        } else if (ageYears >= 60) {
            // Senior: 0 – 3L = 0%, 3L–5L = 5%, 5L–10L = 20%, >10L = 30%
            if (taxableIncome <= 300_000) {
                tax = 0;
            } else if (taxableIncome <= 500_000) {
                tax = (taxableIncome - 300_000) * 5 / 100;
            } else if (taxableIncome <= 1_000_000) {
                tax = 10_000 + (taxableIncome - 500_000) * 20 / 100;
            } else {
                tax = 110_000 + (taxableIncome - 1_000_000) * 30 / 100;
            }
        } else {
            // Regular: 0 – 2.5L = 0%, 2.5L–5L = 5%, 5L–10L = 20%, >10L = 30%
            if (taxableIncome <= 250_000) {
                tax = 0;
            } else if (taxableIncome <= 500_000) {
                tax = (taxableIncome - 250_000) * 5 / 100;
            } else if (taxableIncome <= 1_000_000) {
                tax = 12_500 + (taxableIncome - 500_000) * 20 / 100;
            } else {
                tax = 112_500 + (taxableIncome - 1_000_000) * 30 / 100;
            }
        }

        return tax;
    }

    /**
     * Rebate u/s 87A for old regime:
     * Total income ≤ ₹5,00,000: rebate = min(tax, 12,500)
     */
    public long computeRebate87AOldRegime(long totalIncome, long taxBeforeRebate) {
        if (totalIncome <= 500_000) {
            return Math.min(taxBeforeRebate, 12_500);
        }
        return 0;
    }

    /**
     * Basic exemption limit for old regime.
     */
    public long basicExemptionOldRegime(int ageYears) {
        if (ageYears >= 80) return 500_000;
        if (ageYears >= 60) return 300_000;
        return 250_000;
    }

    /**
     * Standard deduction u/s 16(ia).
     * Old regime: ₹50,000. New regime: ₹75,000 (Budget 2024).
     */
    public long standardDeduction(TaxRegime regime) {
        return regime == TaxRegime.NEW_REGIME ? 75_000L : 50_000L;
    }
}
