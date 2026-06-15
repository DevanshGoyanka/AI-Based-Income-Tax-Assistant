package com.itr.domain.othersources;

import java.math.BigDecimal;

/**
 * WinningsResult — result of winnings computation (115BB or 115BBJ).
 */
public class WinningsResult {
    public static final WinningsResult ZERO = new WinningsResult(
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
    );

    private final BigDecimal grossWinnings;
    private final BigDecimal tax;          // @ 30%
    private final BigDecimal cess;         // @ 4%
    private final BigDecimal totalTax;     // Tax + Cess
    private final BigDecimal tdsAmount;    // TDS credit

    public WinningsResult(BigDecimal grossWinnings, BigDecimal tax, BigDecimal cess,
                          BigDecimal totalTax, BigDecimal tdsAmount) {
        this.grossWinnings = grossWinnings;
        this.tax = tax;
        this.cess = cess;
        this.totalTax = totalTax;
        this.tdsAmount = tdsAmount;
    }

    public WinningsResult(BigDecimal grossWinnings, BigDecimal tax, BigDecimal cess, BigDecimal totalTax) {
        this(grossWinnings, tax, cess, totalTax, BigDecimal.ZERO);
    }

    public BigDecimal getGrossWinnings() { return grossWinnings; }
    public BigDecimal getTax() { return tax; }
    public BigDecimal getCess() { return cess; }
    public BigDecimal getTotalTax() { return totalTax; }
    public BigDecimal getTdsAmount() { return tdsAmount; }
}
