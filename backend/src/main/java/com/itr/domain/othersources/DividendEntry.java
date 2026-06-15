package com.itr.domain.othersources;

import java.math.BigDecimal;

/**
 * DividendEntry — dividend income entry.
 * Sections corrected per CBDT:
 *   - "other" — Regular dividends (TDS u/s 194)
 *   - "2(22)(e)" — Deemed dividend from closely held companies
 *   - "2(22)(f)" — Dividend on capital reduction (unlisted companies)
 */
public class DividendEntry {
    private String section;           // "other", "2(22)(e)", "2(22)(f)"
    private String itdTag;            // "DividendOth", "DividendUs2_22_e", "DividendUs2_22_f"
    private BigDecimal grossAmount;
    private BigDecimal tdsDeducted;
    // Quarterly breakup — mandatory for CBDT Category A validation
    private BigDecimal q1;  // April–June
    private BigDecimal q2;  // July–September
    private BigDecimal q3;  // October–December
    private BigDecimal q4;  // January–March

    public DividendEntry() {}

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getItdTag() { return itdTag; }
    public void setItdTag(String itdTag) { this.itdTag = itdTag; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getTdsDeducted() { return tdsDeducted; }
    public void setTdsDeducted(BigDecimal tdsDeducted) { this.tdsDeducted = tdsDeducted; }
    public BigDecimal getQ1() { return q1; }
    public void setQ1(BigDecimal q1) { this.q1 = q1; }
    public BigDecimal getQ2() { return q2; }
    public void setQ2(BigDecimal q2) { this.q2 = q2; }
    public BigDecimal getQ3() { return q3; }
    public void setQ3(BigDecimal q3) { this.q3 = q3; }
    public BigDecimal getQ4() { return q4; }
    public void setQ4(BigDecimal q4) { this.q4 = q4; }
}
