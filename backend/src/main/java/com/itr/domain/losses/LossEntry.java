package com.itr.domain.losses;

/**
 * LossEntry — a single loss incurred in a given assessment year.
 * Immutable.
 */
public record LossEntry(
    long id,
    LossHead head,
    String assessmentYear,
    long amountIncurred,   // paise
    long remainingAmount,  // paise (decreases as set off each year)
    int maxCarryForwardAY, // last AY it can be used (e.g., 2034 for 2026-27 HP loss (+8))
    boolean isExhausted
) {
    public boolean canBeSetOffIn(String targetAY) {
        if (isExhausted || remainingAmount <= 0) return false;
        int targetAYInt = Integer.parseInt(targetAY.substring(0, 4));
        return targetAYInt <= maxCarryForwardAY;
    }
}
