package com.itr.model;

/**
 * ITR form types based on income sources and taxpayer category.
 */
public enum ITRFormType {
    ITR1("ITR-1", "For individuals with salary, one house property, other sources (interest), total income ≤ ₹50L"),
    ITR2("ITR-2", "For individuals/HUFs with capital gains, multiple house properties, foreign income/assets, total income > ₹50L"),
    ITR3("ITR-3", "For individuals/HUFs with business/profession income"),
    ITR4("ITR-4", "For presumptive income from business/profession (Sections 44AD, 44ADA, 44AE)"),
    ITR5("ITR-5", "For firms, LLPs, AOPs, BOIs"),
    ITR6("ITR-6", "For companies other than those claiming exemption under section 11"),
    ITR7("ITR-7", "For persons including companies required to furnish return under sections 139(4A), 139(4B), 139(4C), 139(4D), 139(4E), 139(4F)"),
    NOT_APPLICABLE("N/A", "Cannot determine applicable ITR form");

    private final String formName;
    private final String description;

    ITRFormType(String formName, String description) {
        this.formName = formName;
        this.description = description;
    }

    public String getFormName() {
        return formName;
    }

    public String getDescription() {
        return description;
    }
}
