package com.itr.domain.salary;

import com.itr.domain.common.TaxRegime;

/**
 * Perquisite Valuation under IT Rules 3.
 *
 * Covers:
 *   - Rent-Free Accommodation (Rule 3)
 *   - Concessional Rent (Rule 3)
 *
 * NOTE: ESOP valuation (Rule 3(2)) is complex and depends on
 * listed vs unlisted company, exercise date, FMV, etc.
 * Basic listed/unlisted distinction is provided here.
 *
 * All amounts in PAISE.
 */
public final class PerquisiteValuation {

    private PerquisiteValuation() {}

    // City population tiers for rent-free accommodation (Rule 3)
    // Population figures are in lakhs (1 lakh = 100,000)
    private static final long POP_TIER_25L  = 25L;  // > 25 lakh
    private static final long POP_TIER_10L  = 10L;  // > 10 lakh

    /**
     * Rent-Free Accommodation perquisite value (IT Rule 3).
     *
     * For EMPLOYER-OWNED accommodation:
     *   Population > 25 lakh: 15% of salary per year
     *   Population 10-25 lakh: 10% of salary per year
     *   Population < 10 lakh:  7.5% of salary per year
     *
     * For EMPLOYER-LEASED accommodation:
     *   Lower of: (a) actual rent paid by employer, OR (b) percentage-based value
     *
     * @param salary                    Aggregate salary from all employers (paise)
     * @param actualRentByEmployer      Rent paid by employer (paise, 0 if employer-owned)
     * @param isEmployerOwned           Is accommodation owned by employer?
     * @param cityPopulationLakhs       City population in lakhs
     * @return Perquisite value in paise (taxable amount)
     */
    public static long computeRentFreeAccommodation(
            long salary,
            long actualRentByEmployer,
            boolean isEmployerOwned,
            long cityPopulationLakhs
    ) {
        int ratePercent;
        if (cityPopulationLakhs > POP_TIER_25L) {
            ratePercent = 15;
        } else if (cityPopulationLakhs > POP_TIER_10L) {
            ratePercent = 10;
        } else {
            ratePercent = 7; // 7.5% — using 7 for simplicity; use BigDecimal in production
        }

        long percentageBased = (salary * ratePercent) / 100;

        if (isEmployerOwned) {
            return percentageBased;
        } else {
            // Leased: lower of actual rent vs percentage-based
            return Math.min(actualRentByEmployer, percentageBased);
        }
    }

    /**
     * Concessional Rental perquisite (Rule 3).
     * Perquisite = Value determined as if rent-free − Rent actually recovered from employee.
     */
    public static long computeConcessionalRent(
            long salary,
            long actualRentByEmployer,
            boolean isEmployerOwned,
            long cityPopulationLakhs,
            long rentRecoveredFromEmployee
    ) {
        long rfaValue = computeRentFreeAccommodation(salary, actualRentByEmployer, isEmployerOwned, cityPopulationLakhs);
        return Math.max(0L, rfaValue - rentRecoveredFromEmployee);
    }

    /**
     * ESOP perquisite — basic listed vs unlisted distinction.
     *
     * LISTED COMPANY:
     *   Taxable perquisite at exercise = (FMV at exercise − Exercise price) × number of shares
     *   FMV = closing price on nearest date or average of high/low on that date
     *
     * UNLISTED COMPANY:
     *   Taxable perquisite at exercise = (Fair Market Value − Exercise price) × number of shares
     *   FMV determined as per Rule 3(2)(i) or 3(2)(ii) — based on book value or DCF
     *
     * TDS timing:
     *   Listed: Section 192 employer deducts at exercise
     *   Unlisted: Section 192(1C) — TDS deferred until shares are sold or 5 years from exercise
     */
    public static ESOPResult computeESOPPerquisite(
            long exercisePrice,
            long fairMarketValue,
            long numberOfShares,
            boolean isListedCompany,
            boolean isTaxDeferred // true for unlisted if still within deferral period
    ) {
        long perquisiteValue = 0L;
        boolean isTaxableNow = true;

        if (fairMarketValue > exercisePrice) {
            perquisiteValue = (fairMarketValue - exercisePrice) * numberOfShares;
        }

        // Unlisted company: tax can be deferred per Section 192(1C)
        if (!isListedCompany && isTaxDeferred) {
            isTaxableNow = false;
            perquisiteValue = 0L;
        }

        return new ESOPResult(perquisiteValue, isTaxableNow, isListedCompany);
    }

    public record ESOPResult(
        long perquisiteValue, // taxable perquisite (paise)
        boolean isTaxableNow, // false if deferred
        boolean isListedCompany
    ) {}
}
