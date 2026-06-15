package com.itr.domain.othersources;

import java.math.BigDecimal;

/**
 * Sec57DeductionsResult — result of Section 57 deductions computation.
 */
public class Sec57DeductionsResult {
    public static final Sec57DeductionsResult ZERO = new Sec57DeductionsResult(
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
    );

    private final BigDecimal interestExpU57;   // 57(i)
    private final BigDecimal depreciation;     // 57(ii)
    private final BigDecimal otherExpU57;      // 57(iii)
    private final BigDecimal total;            // Total = sum of above

    public Sec57DeductionsResult(BigDecimal interestExpU57, BigDecimal depreciation,
                                  BigDecimal otherExpU57, BigDecimal total) {
        this.interestExpU57 = interestExpU57;
        this.depreciation = depreciation;
        this.otherExpU57 = otherExpU57;
        this.total = total;
    }

    public BigDecimal getInterestExpU57() { return interestExpU57; }
    public BigDecimal getDepreciation() { return depreciation; }
    public BigDecimal getOtherExpU57() { return otherExpU57; }
    public BigDecimal getTotal() { return total; }
}
