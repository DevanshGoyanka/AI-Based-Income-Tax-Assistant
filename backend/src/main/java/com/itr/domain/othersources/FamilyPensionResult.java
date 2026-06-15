package com.itr.domain.othersources;

import java.math.BigDecimal;

/**
 * FamilyPensionResult — result of Section 57(iia) family pension computation.
 */
public class FamilyPensionResult {
    public static final FamilyPensionResult ZERO = new FamilyPensionResult(
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
    );

    private final BigDecimal grossPension;
    private final BigDecimal deduction;      // Lower of 1/3rd or Rs 15,000/25,000
    private final BigDecimal taxableAmount;  // Gross - Deduction

    public FamilyPensionResult(BigDecimal grossPension, BigDecimal deduction, BigDecimal taxableAmount) {
        this.grossPension = grossPension;
        this.deduction = deduction;
        this.taxableAmount = taxableAmount;
    }

    public BigDecimal getGrossPension() { return grossPension; }
    public BigDecimal getDeduction() { return deduction; }
    public BigDecimal getTaxableAmount() { return taxableAmount; }
}
