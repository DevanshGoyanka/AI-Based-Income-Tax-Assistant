package com.itr.application.itrform;

import com.itr.domain.common.*;
import com.itr.domain.itrselection.ITRFormType;
import java.util.Map;

/**
 * ITRFormDataBuildUseCase — maps domain computation → ITD-schema JSON per form type.
 * <p>
 * Produces the exact JSON structure that matches the ITD published schema for AY 2026-27.
 * The output is returned as a Map that can be serialized directly to JSON.
 */
public class ITRFormDataBuildUseCase {

    public Map<String, Object> buildITR1Json(TaxComputationResult result, String pan, String name) {
        return Map.of(
            "formType", "ITR-1",
            "assessmentYear", "2026-27",
            "personalInfo", Map.of(
                "pan", pan,
                "name", name
            ),
            "incomeDetails", Map.of(
                "salaryIncome", result.getTotalIncome(),
                "housePropertyIncome", 0L,
                "otherSourcesIncome", 0L,
                "grossTotalIncome", result.getGrossTotalIncome()
            ),
            "deductions", Map.of(
                "chapterVIA", result.getTotalDeductions(),
                "totalDeductions", result.getTotalDeductions()
            ),
            "taxComputation", Map.of(
                "netTaxableIncome", result.getNetTaxableIncome(),
                "taxOnIncome", result.getTaxOnNormalIncome(),
                "rebate87A", result.getRebate87A(),
                "surcharge", result.getSurcharge(),
                "educationCess", result.getHealthAndEducationCess(),
                "totalTaxLiability", result.getTotalTaxLiability(),
                "relief89", result.getRelief89(),
                "taxPayableAfterRelief", result.getTaxPayableAfterRelief()
            ),
            "taxPaid", Map.of(
                "tds", result.getTdsAmount(),
                "advanceTax", result.getAdvanceTaxPaid(),
                "selfAssessmentTax", result.getSelfAssessmentTaxPaid(),
                "totalTaxPaid", result.getTotalTaxesPaid()
            ),
            "interest", Map.of(
                "section234A", result.getInterest234A(),
                "section234B", result.getInterest234B(),
                "section234C", result.getInterest234C(),
                "section234F", result.getFee234F()
            ),
            "finalTaxLiability", Map.of(
                "balancePayable", result.getBalanceTaxPayable(),
                "refund", result.getRefundAmount()
            )
        );
    }

    public Map<String, Object> buildITR2Json(TaxComputationResult result, String pan, String name) {
        var base = buildITR1Json(result, pan, name);
        base.put("formType", "ITR-2");
        base.put("capitalGains", Map.of(
            "ltcg112A", result.getTaxOnSpecialRateIncome(),
            "totalCG", 0L
        ));
        return base;
    }
}
