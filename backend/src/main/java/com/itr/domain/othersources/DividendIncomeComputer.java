package com.itr.domain.othersources;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

/**
 * DividendIncomeComputer — computes dividend income with corrected sections.
 *
 * Schedule OS aggregate validation (CBDT Category A):
 *   sl.1a = sl.1ai + sl.1aii + sl.1aiii
 *
 * Quarterly dividend breakup (CBDT Category A):
 *   q1 + q2 + q3 + q4 === grossAmount (±1 rounding tolerance)
 */
@Slf4j
public class DividendIncomeComputer {

    /**
     * Compute dividend income and validate quarterly breakup.
     */
    public DividendComputationResult compute(List<DividendEntry> entries) {
        BigDecimal regular = BigDecimal.ZERO;       // 1ai — other than 2(22)(e)
        BigDecimal deemed = BigDecimal.ZERO;        // 1aii — u/s 2(22)(e)
        BigDecimal capitalRedn = BigDecimal.ZERO;   // 1aiii — u/s 2(22)(f)

        for (DividendEntry entry : entries) {
            String section = entry.getSection() != null ? entry.getSection() : "other";
            switch (section) {
                case "2(22)(e)":
                    deemed = deemed.add(entry.getGrossAmount());
                    break;
                case "2(22)(f)":
                    capitalRedn = capitalRedn.add(entry.getGrossAmount());
                    break;
                default:
                    regular = regular.add(entry.getGrossAmount());
                    break;
            }

            // Validate quarterly breakup (Category A rule)
            validateQuarterlyBreakup(entry);
        }

        BigDecimal total1a = regular.add(deemed).add(capitalRedn);

        log.info("Dividend: Regular={}, Deemed2_22_e={}, CapitalRedn2_22_f={}, Total1a={}",
            regular, deemed, capitalRedn, total1a);

        return new DividendComputationResult(regular, deemed, capitalRedn, total1a);
    }

    /**
     * Validate quarterly dividend breakup — Category A validation rule OS-03.
     * Sum of quarters must equal gross (±1 rounding tolerance).
     */
    private void validateQuarterlyBreakup(DividendEntry entry) {
        if (entry.getQ1() == null || entry.getQ2() == null ||
            entry.getQ3() == null || entry.getQ4() == null) {
            return; // Skip if quarterly data not provided
        }
        BigDecimal sumQuarters = entry.getQ1().add(entry.getQ2())
                                      .add(entry.getQ3())
                                      .add(entry.getQ4());
        BigDecimal diff = entry.getGrossAmount().subtract(sumQuarters).abs();
        if (diff.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                "Category A violation (OS-03): Quarterly dividend breakup (" + sumQuarters +
                ") does not match gross amount (" + entry.getGrossAmount() +
                ") for entry: " + entry.getSection());
        }
    }
}
