package com.itr.domain.houseproperty;

import com.itr.domain.common.TaxRegime;
import static com.itr.domain.common.AssessmentYear.HP_INTER_HEAD_LOSS_LIMIT;

/**
 * HPScheduleComputer — aggregates all properties and applies set-off rules.
 * <p>
 * In old regime: total HP loss (negative income) can be set off against other heads
 * up to ₹2,00,000. Remaining loss carried forward for 8 years.
 * <p>
 * In new regime: HP loss CANNOT be set off against any other head.
 * Only loss from let-out property is deductible against HP income — cannot flow out.
 */
public final class HPScheduleComputer {

    private HPScheduleComputer() {}

    /**
     * Computes the net HP income after loss aggregation and set-off application.
     *
     * @param propertyIncomes Array of individual property incomes (paise, negative = loss)
     * @param regime          Tax regime
     * @return HPAggregationResult with net HP income, loss disallowed, carried forward
     */
    public static HPAggregationResult aggregate(long[] propertyIncomes, TaxRegime regime) {
        long totalHPIncome = 0;
        for (long pi : propertyIncomes) {
            totalHPIncome += pi;
        }

        long lossDisallowed = 0;
        long lossCarriedForward = 0;

        if (totalHPIncome < 0) {
            long totalLoss = -totalHPIncome;

            if (regime == TaxRegime.NEW) {
                // New regime: NO HP loss set-off allowed
                lossDisallowed = totalLoss;
                totalHPIncome = 0;
            } else {
                // Old regime: cap at ₹2L
                long maxSetoff = Math.min(totalLoss, HP_INTER_HEAD_LOSS_LIMIT);
                long excessLoss = totalLoss - maxSetoff;
                lossDisallowed = 0; // excess goes to carry forward, not disallowed
                totalHPIncome = -maxSetoff; // negative = can set off against other heads
                lossCarriedForward = excessLoss;
            }
        }

        return new HPAggregationResult(totalHPIncome, lossDisallowed, lossCarriedForward);
    }

    public record HPAggregationResult(
        long totalHPIncome,     // net HP income after aggregation (paise)
        long lossDisallowed,    // loss that cannot be set off this year
        long lossCarriedForward // loss to carry forward for future set-off
    ) {}
}
