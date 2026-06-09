package com.itr.domain.deductions;

/**
 * Section80G — computes donation deduction under Section 80G.
 * <p>
 * 80G donations have 5 categories:
 * - 100% without 10% GTI limit (Prime Minister's National Relief Fund)
 * - 100% with 10% GTI limit (government funds, Jawaharlal Nehru Memorial Fund)
 * - 50% without 10% GTI limit
 * - 50% with 10% GTI limit (various approved institutions)
 * - 100% subject to 10% GTI with cap
 * <p>
 * Cash donations > ₹2,000 are disallowed.
 */
public final class Section80G {

    private Section80G() {}

    public record Donation(
        long amount,
        int eligiblePercent,   // 100 or 50
        boolean has10PercentLimit,
        boolean isCash,
        String doneeName,
        String doneePAN
    ) {
        public long getEligibleAmount() {
            if (isCash && amount > 2000_00L) return 0; // Cash > ₹2K disallowed
            return amount * eligiblePercent / 100;
        }
    }
}
