package com.itr.model;

/**
 * Employer category codes as per ITR-1 schema.
 * Affects deduction limits (80CCD(2), entertainment allowance).
 */
public enum EmployerCategory {
    CG("CG", "Central Government"),
    SG("SG", "State Government"),
    PSU("PSU", "Public Sector Undertaking"),
    PEN("PEN", "Pensioner"),
    OTH("OTH", "Others");

    private final String code;
    private final String description;

    EmployerCategory(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCentralOrStateGovt() {
        return this == CG || this == SG;
    }

    public boolean isPensioner() {
        return this == PEN;
    }

    public static EmployerCategory fromCode(String code) {
        if (code == null || code.isBlank()) return OTH;
        for (EmployerCategory ec : values()) {
            if (ec.code.equalsIgnoreCase(code)) return ec;
        }
        return OTH;
    }
}
