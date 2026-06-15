package com.itr.domain.othersources;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * Sec57DeductionsComputer — Section 57 deductions against "Other Sources" income.
 *
 * Section 57 sub-sections:
 *   57(i) — Interest on borrowed capital for earning dividend/securities income
 *   57(ii) — Depreciation on machinery/plant used for rental income
 *   57(iia) — Family pension deduction (handled separately by FamilyPensionComputer)
 *   57(iii) — Any other expenditure (not personal, capital) for generating OS income
 *
 * CBDT Validation Rule OS-04:
 *   Sl.3c Deduction u/s 57 = sum(3a + 3b + 3c sub-items)
 *
 * CBDT Validation Rule OS-05:
 *   Depreciation (3b) can only be claimed if machinery rental > 0
 */
@Slf4j
public class Sec57DeductionsComputer {

    private BigDecimal interestExpU57 = BigDecimal.ZERO;   // 57(i) — interest on borrowed capital
    private BigDecimal depreciation = BigDecimal.ZERO;     // 57(ii) — depreciation on machinery
    private BigDecimal otherExpU57 = BigDecimal.ZERO;      // 57(iii) — other legitimate expenses

    /**
     * Compute Section 57 deductions.
     */
    public Sec57DeductionsResult compute(BigDecimal machineryRentalIncome) {
        // Validation OS-05: Depreciation can only be claimed if machinery rental > 0
        if (machineryRentalIncome == null || machineryRentalIncome.compareTo(BigDecimal.ZERO) <= 0) {
            if (depreciation.compareTo(BigDecimal.ZERO) > 0) {
                log.warn("OS-05 violation: Depreciation claimed but machinery rental income is 0");
                depreciation = BigDecimal.ZERO;
            }
        }

        BigDecimal total = interestExpU57.add(depreciation).add(otherExpU57);
        log.info("Section 57 deductions: Interest={}, Depreciation={}, Other={}, Total={}",
            interestExpU57, depreciation, otherExpU57, total);

        return new Sec57DeductionsResult(interestExpU57, depreciation, otherExpU57, total);
    }

    public void setInterestExpU57(BigDecimal v) { this.interestExpU57 = v; }
    public void setDepreciation(BigDecimal v) { this.depreciation = v; }
    public void setOtherExpU57(BigDecimal v) { this.otherExpU57 = v; }
}
