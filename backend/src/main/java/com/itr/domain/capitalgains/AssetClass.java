package com.itr.domain.capitalgains;

/** Classification of capital assets. */
public enum AssetClass {
    EQUITY_LISTED,
    EQUITY_UNLISTED,
    DEBT_MF,        // Post-April 2023: always short-term, slab rate
    PREFERENCE_SHARES,
    PROPERTY,
    GOLD,
    VDA,            // Virtual Digital Assets (crypto)
    BONDS,
    MUTUAL_FUND_EQUITY,
    MUTUAL_FUND_DEBT,
    OTHER
}
