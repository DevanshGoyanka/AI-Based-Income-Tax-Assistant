package com.itr.domain.salary;

import java.util.Set;

/**
 * Metro city classification for HRA Section 10(13A) r.w. Rule 2A.
 *
 * AY 2026-27 (FY 2025-26): 4 metro cities at 50%.
 *   Delhi, Mumbai, Kolkata, Chennai
 *
 * AY 2027-28 onwards (Income Tax Act 2025): 8 metro cities.
 *   Delhi, Mumbai, Kolkata, Chennai, Bengaluru, Hyderabad, Pune, Ahmedabad
 *
 * All amounts in PAISE.
 */
public enum HRAMetroCity {
    METRO(50),     // 50% of salary for HRA
    NON_METRO(40); // 40% of salary for HRA

    private final int percentageOfSalary;

    HRAMetroCity(int percentageOfSalary) {
        this.percentageOfSalary = percentageOfSalary;
    }

    public int getPercentageOfSalary() {
        return percentageOfSalary;
    }

    // AY 2026-27: 4 metros per Section 10(13A) r.w. Rule 2A
    private static final Set<String> METRO_CITIES_AY2026_27 = Set.of(
        "DELHI", "MUMBAI", "KOLKATA", "CHENNAI"
    );

    // AY 2027-28+: 8 metros per Income Tax Act 2025
    private static final Set<String> METRO_CITIES_AY2027_28 = Set.of(
        "DELHI", "MUMBAI", "KOLKATA", "CHENNAI",
        "BENGALURU", "HYDERABAD", "PUNE", "AHMEDABAD"
    );

    /**
     * Classify a city as METRO or NON_METRO based on assessment year.
     *
     * @param city           City name (case-insensitive)
     * @param assessmentYear Assessment year string e.g. "2026-27"
     * @return METRO or NON_METRO
     */
    public static HRAMetroCity fromCity(String city, String assessmentYear) {
        if (city == null || city.trim().isEmpty()) {
            return NON_METRO;
        }
        String normalized = city.trim().toUpperCase();
        // Normalize common variations
        normalized = normalizeCityName(normalized);

        Set<String> metros = isAY2027OrLater(assessmentYear)
            ? METRO_CITIES_AY2027_28
            : METRO_CITIES_AY2026_27;

        return metros.contains(normalized) ? METRO : NON_METRO;
    }

    /** Simple check: is this city a metro for AY 2026-27? */
    public static HRAMetroCity fromCity(String city) {
        return fromCity(city, "2026-27");
    }

    private static boolean isAY2027OrLater(String ay) {
        if (ay == null) return false;
        // AY format: "2026-27" — compare first 4 chars
        try {
            int year = Integer.parseInt(ay.substring(0, 4));
            return year >= 2027;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Normalize common city name variations */
    private static String normalizeCityName(String city) {
        return switch (city) {
            case "NEW DELHI" -> "DELHI";
            case "GREATER MUMBAI", "NAVI MUMBAI", "THANE", "MUMBAI SUBURBAN" -> "MUMBAI";
            case "CALCUTTA" -> "KOLKATA";
            case "MADRAS" -> "CHENNAI";
            case "BANGALORE", "BENGALURU RURAL", "BENGALURU URBAN" -> "BENGALURU";
            default -> city;
        };
    }
}
