package com.itr.domain.othersources;

import java.math.BigDecimal;

/**
 * DividendComputationResult — result of dividend income computation.
 */
public class DividendComputationResult {
    private final BigDecimal regular;        // 1ai — other than 2(22)(e)
    private final BigDecimal deemed;         // 1aii — u/s 2(22)(e)
    private final BigDecimal capitalRedn;    // 1aiii — u/s 2(22)(f)
    private final BigDecimal total1a;        // Total = sum of above

    public DividendComputationResult(BigDecimal regular, BigDecimal deemed,
                                      BigDecimal capitalRedn, BigDecimal total1a) {
        this.regular = regular;
        this.deemed = deemed;
        this.capitalRedn = capitalRedn;
        this.total1a = total1a;
    }

    public BigDecimal getRegular() { return regular; }
    public BigDecimal getDeemed() { return deemed; }
    public BigDecimal getCapitalRedn() { return capitalRedn; }
    public BigDecimal getTotal1a() { return total1a; }
}
