package com.itr.domain.salary;

/** Metro vs non-metro for HRA exemption (50% vs 40% of salary). */
public enum HRAMetroCity {
    METRO(50),   // 50% of salary
    NON_METRO(40); // 40% of salary

    private final int percentageOfSalary;

    HRAMetroCity(int percentageOfSalary) {
        this.percentageOfSalary = percentageOfSalary;
    }

    public int getPercentageOfSalary() {
        return percentageOfSalary;
    }

    private static final java.util.Set<String> METRO_CITIES = java.util.Set.of(
        "MUMBAI", "DELHI", "NEW DELHI", "KOLKATA", "CHENNAI",
        "GREATER MUMBAI", "NAVI MUMBAI", "THANE",
        "NOIDA", "GURGAON", "GURUGRAM", "FARIDABAD", "GHAZIABAD"
    );

    public static HRAMetroCity fromCity(String city) {
        if (city == null || city.trim().isEmpty()) return NON_METRO;
        return METRO_CITIES.contains(city.trim().toUpperCase()) ? METRO : NON_METRO;
    }
}
