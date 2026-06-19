package com.itr.domain.salary;

import com.itr.domain.common.TaxRegime;

/**
 * HRA Exemption under Section 10(13A) read with Rule 2A.
 *
 * AVAILABLE: Old Tax Regime ONLY.
 * NOT AVAILABLE: New Tax Regime (Section 115BAC) — HRA fully taxable.
 *
 * Three-condition minimum (per Rule 2A):
 *   (a) Actual HRA received from employer
 *   (b) Rent paid − 10% of (Basic + eligible DA + commission)
 *   (c) 50% of salary for metro / 40% for non-metro
 *
 * "Salary" for HRA = Basic + DA (if forming part of retirement benefits) + Commission.
 *
 * AY routing: Metro city expansion (4→8) applies from AY 2027-28 onwards only.
 *
 * All amounts in PAISE.
 */
public final class HRAExemption {

    private HRAExemption() {}

    /**
     * Compute HRA exemption.
     *
     * @param actualHRAReceived    HRA actually received from employer (paise)
     * @param basicSalary         Basic salary (paise)
     * @param da                  DA forming part of retirement benefits (paise)
     * @param commission          Commission as % of turnover (paise)
     * @param annualRentPaid      Actual annual rent paid (paise)
     * @param city                City of employment
     * @param assessmentYear      Assessment year e.g. "2026-27"
     * @param regime              Tax regime (OLD or NEW)
     * @param isGovtEmployee     Is employee a government employee (gets full HRA exempt)
     * @return Exempt amount in paise (0 if new regime)
     */
    public static long compute(
            long actualHRAReceived,
            long basicSalary,
            long da,
            long commission,
            long annualRentPaid,
            String city,
            String assessmentYear,
            TaxRegime regime,
            boolean isGovtEmployee
    ) {
        // GATE: HRA not exempt under new regime
        if (regime == TaxRegime.NEW) {
            return 0L;
        }

        // Government employee gets FULL HRA exempt without any conditions
        if (isGovtEmployee) {
            return actualHRAReceived;
        }

        long salaryForHRA = basicSalary + da + commission;

        // Condition (b): Rent paid − 10% of salary
        long rentMinus10Pct = Math.max(annualRentPaid - (salaryForHRA / 10), 0L);

        // Condition (c): Metro 50% or Non-metro 40%
        HRAMetroCity metroCity = HRAMetroCity.fromCity(city, assessmentYear);
        long metroCondition = (salaryForHRA * metroCity.getPercentageOfSalary()) / 100;

        // Return minimum of the three conditions
        long condition1 = actualHRAReceived;
        return Math.min(condition1, Math.min(rentMinus10Pct, metroCondition));
    }

    /**
     * Convenience overload for AY 2026-27.
     */
    public static long compute(
            long actualHRAReceived,
            long basicSalary,
            long da,
            long commission,
            long annualRentPaid,
            String city,
            TaxRegime regime
    ) {
        return compute(actualHRAReceived, basicSalary, da, commission, annualRentPaid, city, "2026-27", regime, false);
    }

    // ── Backward-compatible overloads for existing controllers ─────────────────

    /**
     * 4-param overload for existing code that uses basicDA combined, isMetro boolean.
     * Defaults: OLD regime, AY 2026-27, commission=0, da=0.
     * NOTE: This merges basic and DA into one value.
     */
    public static long computeForLegacy(
            long actualHRA, long basicDAEquivalent, long rentPaid, boolean isMetro
    ) {
        String metroCity = isMetro ? "MUMBAI" : "NON_METRO";
        return compute(actualHRA, basicDAEquivalent, 0L, 0L, rentPaid, metroCity, TaxRegime.OLD);
    }

    /** Alias for backward compatibility with existing code */
    public static long compute(
            long actualHRA, long basicDAEquivalent, long rentPaid, boolean isMetro
    ) {
        return computeForLegacy(actualHRA, basicDAEquivalent, rentPaid, isMetro);
    }
}
