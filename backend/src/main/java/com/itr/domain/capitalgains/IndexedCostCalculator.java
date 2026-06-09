package com.itr.domain.capitalgains;

import static com.itr.domain.common.AssessmentYear.*;

/**
 * IndexedCostCalculator — computes indexed cost of acquisition for LTCG (Section 48).
 * <p>
 * Indexed Cost = Actual Cost × (CII of sale year / CII of acquisition year)
 * <p>
 * Base year for CII: 2001-02 (Index = 100).
 * For assets acquired before 2001, fair market value as on 1 Apr 2001 is used.
 */
public final class IndexedCostCalculator {

    private IndexedCostCalculator() {}

    /**
     * Compute indexed cost of acquisition.
     *
     * @param actualCost             Actual cost of acquisition in paise
     * @param acquisitionFY          Financial year of acquisition (e.g., 2010 for FY 2010-11)
     * @param saleFY                 Financial year of sale (e.g., 2025 for FY 2025-26)
     * @return Indexed cost of acquisition in paise
     */
    public static long compute(long actualCost, int acquisitionFY, int saleFY) {
        int ciiAcq = getCII(acquisitionFY);
        int ciiSale = getCII(saleFY);

        if (ciiAcq <= 0 || ciiSale <= 0) {
            return actualCost; // Cannot index — return actual cost
        }

        // Indexed Cost = Cost × (CII Sale / CII Acquisition)
        return Math.round((double) actualCost * ciiSale / ciiAcq);
    }
}
