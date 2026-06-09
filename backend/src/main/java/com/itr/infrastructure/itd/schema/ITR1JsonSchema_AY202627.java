package com.itr.infrastructure.itd.schema;

import java.util.Map;

/**
 * ITR1JsonSchema_AY202627 — mirrors the ITD published JSON schema for ITR-1 AY 2026-27.
 * <p>
 * Used for validation of generated JSON before submission to ITD portal.
 */
public final class ITR1JsonSchema_AY202627 {

    private ITR1JsonSchema_AY202627() {}

    /** Required top-level fields. */
    public static final String[] REQUIRED_FIELDS = {
        "formType", "assessmentYear", "personalInfo", "incomeDetails",
        "deductions", "taxComputation", "taxPaid", "interest", "finalTaxLiability"
    };

    /** Validate a generated JSON map against the schema. */
    public static ValidationResult validate(Map<String, Object> json) {
        for (String field : REQUIRED_FIELDS) {
            if (!json.containsKey(field)) {
                return new ValidationResult(false, "Missing required field: " + field);
            }
        }
        if (!"ITR-1".equals(json.get("formType"))) {
            return new ValidationResult(false, "formType must be 'ITR-1'");
        }
        if (!"2026-27".equals(json.get("assessmentYear"))) {
            return new ValidationResult(false, "assessmentYear must be '2026-27'");
        }
        return new ValidationResult(true, "JSON is valid against AY 2026-27 ITR-1 schema");
    }

    public record ValidationResult(boolean isValid, String message) {}
}
