package com.itr.domain.deductions;

import com.itr.domain.common.AssessmentYear;

/**
 * NewRegimeAllowedDeductions — validates which deductions are permitted in the new regime.
 * <p>
 * Under Section 115BAC (new regime):
 * ALLOWED: 80CCD(2) [Employer NPS], 80CCH(2) [Agniveer], 80JJAA [Additional employee cost]
 * DISALLOWED: Everything else (80C, 80D, 80G, 80TTA, 80E, 80U, etc.)
 */
public final class NewRegimeAllowedDeductions {

    private NewRegimeAllowedDeductions() {}

    public static boolean isAllowed(String sectionName) {
        return switch (sectionName) {
            case "80CCD(2)", "80CCH(2)", "80JJAA" -> true;
            default -> false;
        };
    }

    public static DeductionLimit getLimit(String sectionName) {
        return switch (sectionName) {
            case "80CCD(2)" -> new DeductionLimit("80CCD(2)", true, "14% govt / 10% private of basic+DA");
            case "80CCH(2)" -> new DeductionLimit("80CCH(2)", true, "Actual contribution (Agniveer Corpus Fund)");
            case "80JJAA" -> new DeductionLimit("80JJAA", true, "30% of additional employee cost for 3 years");
            case "80C", "80CCC", "80CCD(1)" -> new DeductionLimit("80C", false, "₹1,50,000 — NOT allowed in new regime");
            default -> new DeductionLimit(sectionName, false, "NOT allowed in new regime under Section 115BAC");
        };
    }

    public record DeductionLimit(String section, boolean allowedInNewRegime, String description) {}
}
