package com.itr.domain.vda;

import com.itr.domain.common.TaxRegime;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * ScheduleVDAComputer — Section 115BBH: 30% flat tax on VDA income.
 * No deductions except cost of acquisition.
 * No loss set-off, no carry-forward, no 87A rebate.
 * TDS under Section 194S (1%) is credit, not deduction.
 */
@Slf4j
public class ScheduleVDAComputer {

    public VDAComputationResult compute(List<VDAEntry> entries, TaxRegime regime) {
        BigDecimal totalVdaIncome = BigDecimal.ZERO;
        BigDecimal totalTdsU194S = BigDecimal.ZERO;

        for (VDAEntry entry : entries) {
            BigDecimal income = entry.getSaleConsideration()
                                    .subtract(entry.getCostOfAcquisition());
            // Losses from one VDA cannot offset gains from another
            if (income.compareTo(BigDecimal.ZERO) > 0) {
                totalVdaIncome = totalVdaIncome.add(income);
            }
            totalTdsU194S = totalTdsU194S.add(entry.getTdsU194S());
        }

        BigDecimal vdaTax = totalVdaIncome.multiply(new BigDecimal("0.30"));
        BigDecimal cess = vdaTax.multiply(new BigDecimal("0.04"));
        BigDecimal totalTax = vdaTax.add(cess);

        log.info("Schedule VDA: Income={}, Tax@30%={}, Cess@4%={}, Total={}, TDS={}",
            totalVdaIncome, vdaTax, cess, totalTax, totalTdsU194S);

        return new VDAComputationResult(totalVdaIncome, vdaTax, cess, totalTax, totalTdsU194S);
    }
}
