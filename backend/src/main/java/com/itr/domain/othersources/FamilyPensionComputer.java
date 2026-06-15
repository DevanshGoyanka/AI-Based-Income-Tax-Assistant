package com.itr.domain.othersources;

import com.itr.domain.common.TaxRegime;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * FamilyPensionComputer — Section 57(iia) deduction.
 *
 * Family pension is income under "Other Sources" (NOT salary).
 * Deduction = LOWER of:
 *   (a) 1/3rd of gross family pension received, OR
 *   (b) Rs 15,000 [Old Tax Regime]
 *       Rs 25,000 [New Tax Regime - enhanced by Finance Act 2024, applicable from AY 2025-26]
 *
 * Available in BOTH old and new tax regimes.
 * NOT available for armed forces family pension (separate exemption).
 */
@Slf4j
public class FamilyPensionComputer {

    public FamilyPensionResult compute(BigDecimal grossPension, TaxRegime regime) {
        if (grossPension == null || grossPension.compareTo(BigDecimal.ZERO) <= 0) {
            return FamilyPensionResult.ZERO;
        }

        BigDecimal oneThird = grossPension.divide(new BigDecimal("3"), 0, RoundingMode.FLOOR);

        BigDecimal cap = regime == TaxRegime.OLD
                         ? new BigDecimal("15000")   // Old Regime cap
                         : new BigDecimal("25000");  // New Regime cap (Finance Act 2024)

        BigDecimal deduction = oneThird.min(cap);
        BigDecimal taxableAmount = grossPension.subtract(deduction);

        log.info("Family Pension u/s 57(iia): Gross={}, 1/3rd={}, Cap={}, Deduction={}, Taxable={}",
            grossPension, oneThird, cap, deduction, taxableAmount);

        return new FamilyPensionResult(grossPension, deduction, taxableAmount);
    }
}
