package com.itr.domain.vda;

import java.math.BigDecimal;

/**
 * VDAComputationResult — result of Virtual Digital Asset income computation.
 * Section 115BBH: 30% flat tax on VDA income. No deductions except cost of acquisition.
 * No loss set-off, no carry-forward, no 87A rebate.
 */
public class VDAComputationResult {
    private final BigDecimal totalVdaIncome;
    private final BigDecimal vdaTax;          // 30% flat
    private final BigDecimal cess;            // 4%
    private final BigDecimal totalTax;        // Tax + Cess
    private final BigDecimal totalTdsU194S;   // TDS credit

    public VDAComputationResult(BigDecimal totalVdaIncome, BigDecimal vdaTax,
                                 BigDecimal cess, BigDecimal totalTax,
                                 BigDecimal totalTdsU194S) {
        this.totalVdaIncome = totalVdaIncome;
        this.vdaTax = vdaTax;
        this.cess = cess;
        this.totalTax = totalTax;
        this.totalTdsU194S = totalTdsU194S;
    }

    public BigDecimal getTotalVdaIncome() { return totalVdaIncome; }
    public BigDecimal getVdaTax() { return vdaTax; }
    public BigDecimal getCess() { return cess; }
    public BigDecimal getTotalTax() { return totalTax; }
    public BigDecimal getTotalTdsU194S() { return totalTdsU194S; }
}
