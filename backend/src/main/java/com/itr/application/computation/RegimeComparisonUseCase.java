package com.itr.application.computation;

import com.itr.domain.common.*;
import com.itr.domain.salary.EmployerEntry;
import com.itr.domain.deductions.Chapter6ADeductionEngine;
import java.util.List;

/**
 * RegimeComparisonUseCase — runs computation for BOTH regimes and returns a comparison.
 * <p>
 * Every salaried individual with no business income should see this comparison.
 */
@org.springframework.stereotype.Service
public class RegimeComparisonUseCase {

    private final TaxComputationUseCase taxComputationUseCase;

    public RegimeComparisonUseCase(TaxComputationUseCase taxComputationUseCase) {
        this.taxComputationUseCase = taxComputationUseCase;
    }

    public RegimeComparisonResult compare(TaxComputationUseCase.ComputationInput baseInput) {
        // Compute for OLD regime
        var oldInput = new TaxComputationUseCase.ComputationInput(
            TaxRegime.OLD,
            baseInput.ageCategory(),
            baseInput.employers(),
            baseInput.hpProperties(),
            baseInput.businessIncome(),
            baseInput.capitalGains(),
            baseInput.otherSourcesIncome(),
            applyOldRegimeDeductions(baseInput.deductionInput()),
            baseInput.relief89Amount(),
            baseInput.tdsAmount(),
            baseInput.advanceTaxPaid(),
            baseInput.selfAssessmentTax(),
            baseInput.filedByDec31()
        );
        TaxComputationResult oldResult = taxComputationUseCase.compute(oldInput);

        // Compute for NEW regime
        var newInput = new TaxComputationUseCase.ComputationInput(
            TaxRegime.NEW,
            baseInput.ageCategory(),
            baseInput.employers(),
            baseInput.hpProperties(),
            baseInput.businessIncome(),
            baseInput.capitalGains(),
            baseInput.otherSourcesIncome(),
            applyNewRegimeDeductions(baseInput.deductionInput()),
            baseInput.relief89Amount(),
            baseInput.tdsAmount(),
            baseInput.advanceTaxPaid(),
            baseInput.selfAssessmentTax(),
            baseInput.filedByDec31()
        );
        TaxComputationResult newResult = taxComputationUseCase.compute(newInput);

        // Determine which regime is better
        boolean oldRegimeBetter = oldResult.getTotalTaxLiability() <= newResult.getTotalTaxLiability();
        long savings = Math.abs(oldResult.getTotalTaxLiability() - newResult.getTotalTaxLiability());

        return new RegimeComparisonResult(
            oldResult, newResult,
            oldRegimeBetter ? TaxRegime.OLD : TaxRegime.NEW,
            savings
        );
    }

    private Chapter6ADeductionEngine.DeductionInput applyOldRegimeDeductions(
            Chapter6ADeductionEngine.DeductionInput input) {
        return input; // All deductions allowed in old regime — pass through
    }

    private Chapter6ADeductionEngine.DeductionInput applyNewRegimeDeductions(
            Chapter6ADeductionEngine.DeductionInput input) {
        if (input == null) return null;
        // New regime: only 80CCD(2) (employer NPS), 80CCH(2) (Agniveer), 80JJAA allowed
        return new Chapter6ADeductionEngine.DeductionInput(
            0, 0, input.section80CCD2(), input.section80CCH2(),
            0, 0, 0, 0, 0, 0, 0, 0,
            input.section80JJAA(), 0, 0, 0,
            0, 0, 0, input.age(), input.parentsAge(),
            false, false, false, List.of()
        );
    }

    public record RegimeComparisonResult(
        TaxComputationResult oldRegimeResult,
        TaxComputationResult newRegimeResult,
        TaxRegime recommendedRegime,
        long savingsInPaise
    ) {}
}
