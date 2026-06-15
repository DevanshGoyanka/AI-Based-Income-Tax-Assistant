package com.itr.domain.othersources;

import java.math.BigDecimal;

/**
 * InterestEntry — single interest income entry.
 */
public class InterestEntry {
    private String itdTag;           // ITD schema tag
    private String description;
    private BigDecimal grossAmount;
    private BigDecimal tdsDeducted;

    public InterestEntry() {}

    public InterestEntry(String itdTag, String description, BigDecimal grossAmount, BigDecimal tdsDeducted) {
        this.itdTag = itdTag;
        this.description = description;
        this.grossAmount = grossAmount;
        this.tdsDeducted = tdsDeducted;
    }

    public String getItdTag() { return itdTag; }
    public void setItdTag(String itdTag) { this.itdTag = itdTag; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getTdsDeducted() { return tdsDeducted; }
    public void setTdsDeducted(BigDecimal tdsDeducted) { this.tdsDeducted = tdsDeducted; }
}
