package com.itr.domain.salary;

import com.itr.domain.common.TaxRegime;

/**
 * LTA Exemption under Section 10(5).
 *
 * AVAILABLE: Old Tax Regime ONLY.
 * NOT AVAILABLE: New Tax Regime (Section 115BAC(2)(i)) — LTA fully taxable.
 *
 * Rules:
 *   - Travel within India only (shortest route)
 *   - Maximum 2 journeys in a block of 4 calendar years
 *   - Current block: 2022-2025; Next block: 2026-2029
 *   - Exemption = actual fare for economy class air OR AC first class rail
 *   - Only fare exempt — hotel, food, local transport NOT exempt
 *
 * All amounts in PAISE.
 */
public final class LTAExemption {

    private LTAExemption() {}

    /**
     * Compute LTA exemption.
     *
     * @param ltaReceived          LTA amount received from employer (paise)
     * @param actualFare           Actual travel fare (shortest route, economy class) (paise)
     * @param isDomesticTravel     Must be within India
     * @param journeysInBlock      Number of LTA journeys already claimed in current 4-year block
     * @param assessmentYear       Assessment year
     * @param regime               Tax regime
     * @return Exempt amount in paise (0 if new regime or ineligible)
     */
    public static long compute(
            long ltaReceived,
            long actualFare,
            boolean isDomesticTravel,
            int journeysInBlock,
            String assessmentYear,
            TaxRegime regime
    ) {
        // GATE: LTA not exempt under new regime
        if (regime == TaxRegime.NEW) {
            return 0L;
        }

        // Must be domestic travel
        if (!isDomesticTravel) {
            return 0L;
        }

        // Maximum 2 journeys per block of 4 years
        if (journeysInBlock >= 2) {
            return 0L;
        }

        // Exemption = minimum of LTA received and actual fare
        return Math.min(ltaReceived, actualFare);
    }

    /**
     * Get the current LTA block years based on a given year.
     */
    public static String getBlockForYear(int year) {
        if (year >= 2022 && year <= 2025) {
            return "2022-2025";
        } else if (year >= 2026 && year <= 2029) {
            return "2026-2029";
        } else if (year >= 2030 && year <= 2033) {
            return "2030-2033";
        }
        return "Unknown";
    }
}
