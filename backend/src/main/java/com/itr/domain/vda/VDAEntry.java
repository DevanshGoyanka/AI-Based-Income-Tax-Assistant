package com.itr.domain.vda;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * VDAEntry — Virtual Digital Asset transaction entry.
 * Section 115BBH — 30% flat tax, no deductions except cost of acquisition.
 */
public class VDAEntry {
    private String vdaType;           // e.g., "CRYPTOCURRENCY", "NFT"
    private String headOfIncome;      // "CAPITAL_GAIN" or "OTHER_SOURCES"
    private LocalDate dateOfAcquisition;
    private LocalDate dateOfSale;
    private BigDecimal saleConsideration;
    private BigDecimal costOfAcquisition;
    private BigDecimal tdsU194S;      // TDS credit under Section 194S

    public VDAEntry() {}

    public VDAEntry(String vdaType, String headOfIncome, LocalDate dateOfAcquisition,
                    LocalDate dateOfSale, BigDecimal saleConsideration,
                    BigDecimal costOfAcquisition, BigDecimal tdsU194S) {
        this.vdaType = vdaType;
        this.headOfIncome = headOfIncome;
        this.dateOfAcquisition = dateOfAcquisition;
        this.dateOfSale = dateOfSale;
        this.saleConsideration = saleConsideration;
        this.costOfAcquisition = costOfAcquisition;
        this.tdsU194S = tdsU194S;
    }

    public String getVdaType() { return vdaType; }
    public void setVdaType(String vdaType) { this.vdaType = vdaType; }
    public String getHeadOfIncome() { return headOfIncome; }
    public void setHeadOfIncome(String headOfIncome) { this.headOfIncome = headOfIncome; }
    public LocalDate getDateOfAcquisition() { return dateOfAcquisition; }
    public void setDateOfAcquisition(LocalDate dateOfAcquisition) { this.dateOfAcquisition = dateOfAcquisition; }
    public LocalDate getDateOfSale() { return dateOfSale; }
    public void setDateOfSale(LocalDate dateOfSale) { this.dateOfSale = dateOfSale; }
    public BigDecimal getSaleConsideration() { return saleConsideration; }
    public void setSaleConsideration(BigDecimal saleConsideration) { this.saleConsideration = saleConsideration; }
    public BigDecimal getCostOfAcquisition() { return costOfAcquisition; }
    public void setCostOfAcquisition(BigDecimal costOfAcquisition) { this.costOfAcquisition = costOfAcquisition; }
    public BigDecimal getTdsU194S() { return tdsU194S; }
    public void setTdsU194S(BigDecimal tdsU194S) { this.tdsU194S = tdsU194S; }
}
