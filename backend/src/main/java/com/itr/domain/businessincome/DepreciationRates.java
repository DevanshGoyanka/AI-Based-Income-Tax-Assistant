package com.itr.domain.businessincome;

import java.util.Map;

/**
 * DepreciationRates — all IT Act Section 32 depreciation rates.
 * <p>
 * Block-of-assets method: rate applied on WDV of the block.
 * Half-year rule: if asset used for less than 180 days, only 50% of the rate applies.
 */
public final class DepreciationRates {

    private DepreciationRates() {}

    /** Asset category → rate in basis points (e.g., 1500 = 15%). */
    public static final Map<String, Integer> RATES = Map.ofEntries(
        Map.entry("BUILDING_RESIDENTIAL", 500),     // 5%
        Map.entry("BUILDING_COMMERCIAL", 1000),     // 10%
        Map.entry("FURNITURE_FITTINGS", 1000),      // 10%
        Map.entry("PLANT_MACHINERY_GENERAL", 1500), // 15%
        Map.entry("PLANT_POLLUTION_CONTROL", 4000), // 40%
        Map.entry("PLANT_ENERGY_SAVING", 4000),     // 40%
        Map.entry("PLANT_POLLUTION_CONTROL_WATER", 1000), // 10%
        Map.entry("COMPUTER_SOFTWARE", 4000),        // 40%
        Map.entry("COMPUTERS", 4000),                // 40%
        Map.entry("VEHICLES", 1500),                 // 15%
        Map.entry("VEHICLES_HEAVY", 3000),           // 30%
        Map.entry("INTANGIBLE_LICENCE", 2500),       // 25%
        Map.entry("INTANGIBLE_PATENT", 2500),        // 25%
        Map.entry("INTANGIBLE_COPYRIGHT", 2500),     // 25%
        Map.entry("INTANGIBLE_TRADEMARK", 2500),     // 25%
        Map.entry("AIRCRAFT", 4000),                 // 40%
        Map.entry("BOOKS_EDUCATIONAL", 4000),        // 40%
        Map.entry("LIVESTOCK", 1000),                // 10%
        Map.entry("SHIPS", 1500),                    // 15%
        Map.entry("ADDITIONAL_NEW_PLANT", 2000)      // 20% additional depreciation (new plant in specified industries)
    );

    public static int getRate(String assetCategory) {
        return RATES.getOrDefault(assetCategory, 1500); // Default: 15%
    }
}
