package com.itr.domain.losses;

/** Types of losses that can be carried forward under the IT Act. */
public enum LossHead {
    HP_LOSS(8, false, "House Property loss — 8 years"),
    STCG_LOSS(8, false, "Short-term capital loss — 8 years"),
    LTCG_LOSS(8, false, "Long-term capital loss — 8 years"),
    BUSINESS_LOSS(8, true, "Business loss (non-speculative) — 8 years. Must have filed return on time."),
    SPECULATIVE_BUSINESS_LOSS(4, true, "Speculative business loss — 4 years, set off only against speculative income"),
    SPECIFIED_BUSINESS_35AD(15, false, "Section 35AD specified business — 15 years, cannot set off against other income"),
    UNABSORBED_DEPRECIATION(-1, true, "Unabsorbed depreciation — UNLIMITED carry forward. No time limit (Section 32(2))");

    private final int carryForwardYears; // -1 = unlimited
    private final boolean requiresTimelyReturn;
    private final String description;

    LossHead(int carryForwardYears, boolean requiresTimelyReturn, String description) {
        this.carryForwardYears = carryForwardYears;
        this.requiresTimelyReturn = requiresTimelyReturn;
        this.description = description;
    }

    public int getCarryForwardYears() { return carryForwardYears; }
    public boolean isRequiresTimelyReturn() { return requiresTimelyReturn; }
    public String getDescription() { return description; }
}
