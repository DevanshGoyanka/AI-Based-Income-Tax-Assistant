package com.itr.domain.businessincome;

import static com.itr.domain.common.AssessmentYear.*;

/**
 * AMTComputer — Alternate Minimum Tax under Section 115JC.
 * <p>
 * AMT = 18.5% of Adjusted Total Income (ATI).
 * ATI = Total Income + specified deductions added back.
 * If AMT > regular tax, AMT is payable. Credit can be carried forward for 15 years.
 */
public final class AMTComputer {

    private AMTComputer() {}

    public static AMTResult compute(long regularTax, long adjustedTotalIncome) {
        if (adjustedTotalIncome <= 20_00_00000L) {
            return new AMTResult(0, 0, false); // Below ₹20L threshold
        }
        long amt = Math.round((double) adjustedTotalIncome * AMT_RATE_BPS / 10000.0);
        // Add surcharge and cess on AMT
        long amtWithCess = amt + Math.round(amt * 0.04);
        boolean isAmtApplicable = amtWithCess > regularTax;
        return new AMTResult(
            amt,
            amtWithCess,
            isAmtApplicable
        );
    }

    public record AMTResult(long amtBeforeCess, long amtWithCess, boolean isAmtApplicable) {}
}
