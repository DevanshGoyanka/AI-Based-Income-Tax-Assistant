package com.itr.domain.itrselection;

/** All ITR form types as per Income Tax Department. */
public enum ITRFormType {
    ITR_1("Sahaj", "For salaried individuals, one house property, income ≤ ₹50L"),
    ITR_2("", "For individuals/HUF with capital gains, no business income"),
    ITR_3("", "For individuals/HUF with business/profession income"),
    ITR_4("Sugam", "For presumptive business income (44AD/44ADA/44AE)"),
    ITR_5("", "For firms/LLPs/AOPs/BOIs"),
    ITR_6("", "For companies"),
    ITR_7("", "For trusts/political institutions"),
    ITR_U("", "Updated return under Section 139(8A)");

    private final String displayName;
    private final String description;

    ITRFormType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
