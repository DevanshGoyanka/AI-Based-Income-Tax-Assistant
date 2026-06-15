package com.itr.domain.othersources;

import java.math.BigDecimal;

/**
 * TraditionalWinningEntry — Section 115BB winnings (Lottery, crossword, horse race, card games offline).
 */
public class TraditionalWinningEntry {
    private String winningType;      // "LOTTERY", "CROSSWORD", "HORSE_RACE", "CARD_GAME", "BETTING"
    private BigDecimal grossAmount;
    private BigDecimal tdsDeducted;

    public TraditionalWinningEntry() {}

    public TraditionalWinningEntry(String winningType, BigDecimal grossAmount, BigDecimal tdsDeducted) {
        this.winningType = winningType;
        this.grossAmount = grossAmount;
        this.tdsDeducted = tdsDeducted;
    }

    public String getWinningType() { return winningType; }
    public void setWinningType(String winningType) { this.winningType = winningType; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getTdsDeducted() { return tdsDeducted; }
    public void setTdsDeducted(BigDecimal tdsDeducted) { this.tdsDeducted = tdsDeducted; }
}
