package com.itr.domain.capitalgains;

import java.time.LocalDate;

/**
 * HoldingPeriodClassifier — classifies capital assets as STCG or LTCG per Section 2(42A).
 * <p>
 * Key rules:
 * - Listed equity (STT paid): >12 months = LTCG (Section 112A)
 * - Property/unlisted shares: >24 months = LTCG (Section 112)
 * - Debt MF acquired on/after 1 Apr 2023: always short-term (Section 50AA)
 * - Debt MF acquired before 1 Apr 2023: >36 months = LTCG
 * - Gold: >36 months = LTCG
 * - Unlisted debentures/bonds: always short-term
 * - VDA: >36 months = LTCG, but taxed at flat 30% either way
 */
public final class HoldingPeriodClassifier {

    private static final LocalDate DEBT_MF_CUTOFF = LocalDate.of(2023, 4, 1);

    private HoldingPeriodClassifier() {}

    public static boolean isLongTerm(AssetClass assetClass, LocalDate acquisitionDate, LocalDate saleDate) {
        if (acquisitionDate == null || saleDate == null) return false;
        long months = acquisitionDate.until(saleDate).toTotalMonths();

        return switch (assetClass) {
            case EQUITY_LISTED, MUTUAL_FUND_EQUITY -> months > 12;
            case PROPERTY, EQUITY_UNLISTED -> months > 24;
            case DEBT_MF -> isDebtMFLongTerm(acquisitionDate, months);
            case GOLD, VDA, MUTUAL_FUND_DEBT -> months > 36;
            case PREFERENCE_SHARES -> isPreferenceLongTerm(acquisitionDate, months);
            case BONDS -> false; // Listed/unlisted bonds: always short-term
            case OTHER -> months > 36;
        };
    }

    private static boolean isDebtMFLongTerm(LocalDate acquisitionDate, long months) {
        return acquisitionDate.isBefore(DEBT_MF_CUTOFF) && months > 36;
    }

    private static boolean isPreferenceLongTerm(LocalDate acquisitionDate, long months) {
        // Listed: >12 months; Unlisted: >36 months (simplified)
        return months > 12;
    }
}
