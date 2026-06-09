package com.itr.domain.capitalgains;

import java.time.LocalDate;
import static com.itr.domain.common.AssessmentYear.*;

/**
 * LTCG112AComputer — computes LTCG on listed equity under Section 112A.
 * <p>
 * Finance Act 2024 (effective 23 July 2024):
 * - Rate: 12.5% (was 10%)
 * - Exemption: ₹1,25,000 (was ₹1,00,000)
 * <p>
 * Grandfathering (Section 55(2)(ac)):
 * For assets acquired before 1 Feb 2018, cost = max(actual cost, min(FMV Jan 31, 2018, sale value))
 */
public final class LTCG112AComputer {

    private static final LocalDate GRANDFATHERING_DATE = LocalDate.of(2018, 1, 31);

    private LTCG112AComputer() {}

    public static LTCG112AResult compute(
            long saleValue,
            long actualCost,
            LocalDate acquisitionDate,
            LocalDate saleDate,
            Long fmvJan312018,
            long transferExpenses) {

        // Step 1: Determine cost basis with grandfathering
        long costBasis = actualCost;
        boolean grandfathered = false;
        long grandfatheringBenefit = 0;

        if (acquisitionDate != null && acquisitionDate.isBefore(GRANDFATHERING_DATE.plusDays(1))) {
            grandfathered = true;
            // FMV cannot exceed sale value
            long effectiveFMV = fmvJan312018 != null ? Math.min(fmvJan312018, saleValue) : actualCost;
            costBasis = Math.max(actualCost, effectiveFMV);
            grandfatheringBenefit = costBasis - actualCost;
        }

        // Step 2: Compute LTCG
        long ltcg = saleValue - costBasis - transferExpenses;
        if (ltcg < 0) ltcg = 0;

        // Step 3: Apply exemption
        long exemption = Math.min(ltcg, LTCG_112A_EXEMPTION);
        long taxableLTCG = ltcg - exemption;

        // Step 4: Compute tax
        long tax = Math.round((double) taxableLTCG * LTCG_112A_RATE_BPS / 10000.0);

        return new LTCG112AResult(ltcg, exemption, taxableLTCG, tax, costBasis, grandfathered, grandfatheringBenefit);
    }

    public record LTCG112AResult(
        long ltcgAmount,
        long exemption,
        long taxableAmount,
        long taxPayable,
        long costBasis,
        boolean isGrandfathered,
        long grandfatheringBenefit
    ) {}
}
