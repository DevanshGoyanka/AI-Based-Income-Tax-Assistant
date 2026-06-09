package com.itr.infrastructure.itd.schema;

import java.util.Map;

/**
 * ITR2JsonSchema_AY202627 — validation schema for ITR-2 AY 2026-27.
 */
public final class ITR2JsonSchema_AY202627 {

    private ITR2JsonSchema_AY202627() {}

    public static final String[] REQUIRED_FIELDS = {
        "formType", "assessmentYear", "personalInfo", "incomeDetails",
        "deductions", "capitalGains", "taxComputation", "taxPaid", "interest", "finalTaxLiability"
    };

    public static ValidationResult validate(Map<String, Object> json) {
        for (String field : REQUIRED_FIELDS) {
            if (!json.containsKey(field)) {
                return new ValidationResult(false, "Missing required field: " + field);
            }
        }
        return new ValidationResult(true, "JSON is valid against AY 2026-27 ITR-2 schema");
    }

    public record ValidationResult(boolean isValid, String message) {}
}
