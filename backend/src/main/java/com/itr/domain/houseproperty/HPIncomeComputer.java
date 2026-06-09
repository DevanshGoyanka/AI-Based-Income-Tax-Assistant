package com.itr.domain.houseproperty;

import com.itr.domain.common.TaxRegime;
import static com.itr.domain.common.AssessmentYear.*;

/**
 * HPIncomeComputer — computes income from one house property per Sections 23-25.
 * <p>
 * For let-out property:
 * NAV = GAV (max of municipal value, fair rent, actual rent) - unrealized rent - municipal taxes paid
 * Standard deduction = 30% of NAV
 * Income from HP = NAV - 30% - interest on loan (Section 24)
 * <p>
 * For self-occupied:
 * NAV = Nil
 * Standard deduction = Nil
 * Income from HP = -interest on loan (capped at ₹2L in old regime, NIL in new regime)
 */
public final class HPIncomeComputer {

    private HPIncomeComputer() {}

    /**
     * Compute income from a single house property.
     *
     * @param propertyType      SELF_OCCUPIED, LET_OUT, or DEEMED_LET_OUT
     * @param annualRent        Actual annual rent received
     * @param municipalRV       Municipal rateable value
     * @param fairRent          Fair rental value
     * @param municipalTaxes    Municipal taxes paid during FY
     * @param unrealizedRent    Unrealized rent (rent not received due to vacancy/default)
     * @param interestOnLoan    Interest on housing loan (Section 24(b))
     * @param preConstructionInt Pre-construction interest (Section 24(b) proviso)
     * @param regime            Tax regime (OLD or NEW — affects self-occupied cap)
     * @return Income from house property in paise (negative = loss)
     */
    public static long compute(
            PropertyType propertyType,
            long annualRent,
            long municipalRV,
            long fairRent,
            long municipalTaxes,
            long unrealizedRent,
            long interestOnLoan,
            long preConstructionInt,
            TaxRegime regime) {

        return switch (propertyType) {
            case SELF_OCCUPIED -> computeSelfOccupied(interestOnLoan, regime);
            case LET_OUT -> computeLetOut(annualRent, municipalRV, fairRent, municipalTaxes, unrealizedRent, interestOnLoan, preConstructionInt);
            case DEEMED_LET_OUT -> computeDeemedLetOut(municipalRV, fairRent, municipalTaxes, interestOnLoan, preConstructionInt);
        };
    }

    private static long computeSelfOccupied(long interestOnLoan, TaxRegime regime) {
        if (regime == TaxRegime.NEW) {
            return -Math.min(interestOnLoan, 0); // No loss for new regime
        }
        // Old regime: cap interest at ₹2L
        return -Math.min(interestOnLoan, HP_INTEREST_SELF_OCC_LIMIT);
    }

    private static long computeLetOut(long annualRent, long municipalRV, long fairRent,
                                       long municipalTaxes, long unrealizedRent,
                                       long interestOnLoan, long preConstructionInt) {
        // Step 1: GAV = max of actual rent, municipal RV, fair rent
        long gav = Math.max(annualRent, Math.max(municipalRV, fairRent));

        // Step 2: NAV = GAV - unrealized rent - municipal taxes
        long nav = gav - unrealizedRent - municipalTaxes;

        // Step 3: Standard deduction = 30% of NAV
        long standardDeduction = nav * HP_STANDARD_DED_RATE_BPS / 10000;

        // Step 4: Total interest = regular + pre-construction (1/5th per year for 5 years)
        long totalInterest = interestOnLoan + preConstructionInt;

        // Step 5: Income from HP = NAV - 30% - total interest
        return Math.max(nav - standardDeduction - totalInterest, -totalInterest);
    }

    private static long computeDeemedLetOut(long municipalRV, long fairRent,
                                             long municipalTaxes, long interestOnLoan,
                                             long preConstructionInt) {
        // For deemed let-out: GAV = max of municipal RV, fair rent (no actual rent)
        long gav = Math.max(municipalRV, fairRent);
        long nav = gav - municipalTaxes;
        long standardDeduction = nav * HP_STANDARD_DED_RATE_BPS / 10000;
        long totalInterest = interestOnLoan + preConstructionInt;
        return Math.max(nav - standardDeduction - totalInterest, -totalInterest);
    }
}
