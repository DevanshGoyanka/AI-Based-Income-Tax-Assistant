package com.itr.domain.capitalgains;

import static com.itr.domain.common.AssessmentYear.*;

/**
 * STCG111AComputer — computes STCG on listed equity under Section 111A.
 * <p>
 * Finance Act 2024 (effective 23 July 2024): rate increased from 15% to 20%.
 * <p>
 * For transactions BEFORE 23 July 2024: 15%
 * For transactions ON/AFTER 23 July 2024: 20%
 */
public final class STCG111AComputer {

    private STCG111AComputer() {}

    /**
     * Compute STCG tax under Section 111A.
     *
     * @param stcgAmount Short-term capital gain in paise
     * @return Tax payable in paise
     */
    public static long compute(long stcgAmount) {
        if (stcgAmount <= 0) return 0;
        return Math.round((double) stcgAmount * STCG_111A_RATE_BPS / 10000.0);
    }
}
