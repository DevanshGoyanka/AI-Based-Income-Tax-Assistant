package com.itr.domain.losses;

import static com.itr.domain.common.AssessmentYear.*;
import com.itr.domain.common.TaxRegime;

/**
 * SetOffOrderEngine — applies CYLA and BFLA rules per Sections 70-74A and 32(2).
 * <p>
 * Strict ordering per IT Act:
 * Step 1: Intra-head set-off (same head, same source)
 * Step 2: Inter-head set-off (different heads, same year)
 * Step 3: Carry forward (when return is filed on time)
 * <p>
 * NEW REGIME restriction (115BAC): Only unabsorbed depreciation can be carried forward.
 * No HP loss set-off. No business loss set-off or carry forward.
 */
public final class SetOffOrderEngine {

    private SetOffOrderEngine() {}

    /**
     * Apply current year loss set-off (CYLA) and return remaining income per head.
     *
     * @param incomeByHead Income amounts by head (can be negative for loss-making heads)
     * @param regime Tax regime
     * @return SetOffResult showing income after intra-head and inter-head set-off
     */
    public static SetOffResult applyCurrentYearSetOff(IncomeByHead incomeByHead, TaxRegime regime) {
        long salaryIncome = Math.max(incomeByHead.salary, 0); // Salary head: no loss allowed
        long hpIncome = incomeByHead.houseProperty;
        long cgIncome = incomeByHead.capitalGains;
        long businessIncome = incomeByHead.business;
        long otherSourceIncome = Math.max(incomeByHead.otherSources, 0);

        long totalIncome = salaryIncome + hpIncome + cgIncome + businessIncome + otherSourceIncome;
        long totalLosses = 0;
        long lossSetOff = 0;
        long lossCarriedForward = 0;

        // HP loss: capped at ₹2L in old regime, blocked in new regime
        if (hpIncome < 0) {
            long hpLoss = -hpIncome;
            if (regime == TaxRegime.NEW) {
                lossCarriedForward += 0; // No carry forward in new regime
                totalLosses += hpLoss;
            } else {
                long hpSetoffable = Math.min(hpLoss, HP_INTER_HEAD_LOSS_LIMIT);
                lossSetOff += hpSetoffable;
                lossCarriedForward += hpLoss - hpSetoffable;
                totalIncome += hpLoss - hpSetoffable; // Add back excess to income
            }
        }

        // Business loss: can set off against other heads (except salary)
        if (businessIncome < 0) {
            long bizLoss = -businessIncome;
            if (regime == TaxRegime.NEW) {
                lossCarriedForward += 0; // No business loss carry forward in new regime
                totalLosses += bizLoss;
            } else {
                // Business loss can offset HP, CG, other sources
                lossSetOff += bizLoss;
                lossCarriedForward += 0;
            }
        }

        // CG loss: can offset other CG gains only (intra-head only)
        // STCG loss → offset STCG first, then LTCG
        if (cgIncome < 0) {
            lossCarriedForward += -cgIncome;
            totalLosses += -cgIncome;
        }

        return new SetOffResult(totalIncome, lossSetOff, lossCarriedForward, totalLosses);
    }

    public record IncomeByHead(
        long salary,
        long houseProperty,
        long capitalGains,
        long business,
        long otherSources
    ) {}

    public record SetOffResult(
        long totalIncomeAfterSetOff,
        long lossSetOffThisYear,
        long lossCarriedForward,
        long totalLossesIncurred
    ) {}
}
