package com.itr.domain.othersources;

import java.math.BigDecimal;

/**
 * GiftComputationResult — result of Section 56(2)(x) gift income computation.
 */
public class GiftComputationResult {
    private final BigDecimal totalValue;
    private final BigDecimal taxableAmount;
    private final int exemptMarriageCount;
    private final int exemptRelativeCount;

    public static final GiftComputationResult ZERO = new GiftComputationResult(
        BigDecimal.ZERO, BigDecimal.ZERO, 0, 0
    );

    public GiftComputationResult(BigDecimal totalValue, BigDecimal taxableAmount,
                                  int exemptMarriageCount, int exemptRelativeCount) {
        this.totalValue = totalValue;
        this.taxableAmount = taxableAmount;
        this.exemptMarriageCount = exemptMarriageCount;
        this.exemptRelativeCount = exemptRelativeCount;
    }

    public BigDecimal getTotalValue() { return totalValue; }
    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public int getExemptMarriageCount() { return exemptMarriageCount; }
    public int getExemptRelativeCount() { return exemptRelativeCount; }
}
