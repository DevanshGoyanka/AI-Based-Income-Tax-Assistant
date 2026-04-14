package com.itr.service.taxengine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Tax Computation Engine - Main orchestrator for complete tax calculation
 * Integrates all tax calculation components:
 * - Tax slabs (old/new regime, all AYs)
 * - Surcharge with marginal relief
 * - Rebate 87A with ₹12L cliff
 * - Special rate income (111A, 112A, 112, 115BB, 115BBH, 115BBE)
 * - Interest (234A/B/C/F)
 * - AMT
 * - Cess @ 4%
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxComputationEngine {

    private final TaxSlabEngine taxSlabEngine;
    private final SurchargeCalculator surchargeCalculator;
    private final RebateCalculator rebateCalculator;
    private final SpecialRateIncomeCalculator specialRateCalculator;
    private final InterestCalculator interestCalculator;
    private final AMTCalculator amtCalculator;

    private static final BigDecimal CESS_RATE = new BigDecimal("0.04");

    /**
     * Complete tax computation
     */
    public TaxComputationResult computeTax(TaxComputationInput input) {
        log.info("Starting tax computation for AY {} in {} regime", 
                input.getAssessmentYear(), input.getRegime());

        List<String> computationSteps = new ArrayList<>();

        // Step 1: Calculate tax on normal income
        BigDecimal taxOnNormalIncome = taxSlabEngine.calculateTaxOnNormalIncome(
                input.getNormalIncome(),
                input.getRegime(),
                input.getAssessmentYear(),
                input.getAgeCategory(),
                input.isHUF());
        computationSteps.add("Tax on normal income: ₹" + taxOnNormalIncome);

        // Step 2: Calculate tax on special rate income
        SpecialRateIncomeTax specialRateTax = calculateSpecialRateIncomeTax(input);
        computationSteps.add("Tax on special rate income: ₹" + specialRateTax.getTotalTax());

        // Step 3: Total tax before rebate
        BigDecimal totalTaxBeforeRebate = taxOnNormalIncome.add(specialRateTax.getTotalTax());
        computationSteps.add("Total tax before rebate: ₹" + totalTaxBeforeRebate);

        // Step 4: Calculate 87A rebate (only on normal income tax)
        RebateCalculator.RebateResult rebateResult = rebateCalculator.calculate87ARebate(
                input.getTotalIncome(),
                taxOnNormalIncome,
                input.getRegime(),
                input.getAssessmentYear());
        computationSteps.add("87A Rebate: ₹" + rebateResult.getRebateAmount() + 
                " (" + rebateResult.getReason() + ")");

        // Step 5: Tax after rebate
        BigDecimal taxAfterRebate = totalTaxBeforeRebate.subtract(rebateResult.getRebateAmount());
        computationSteps.add("Tax after rebate: ₹" + taxAfterRebate);

        // Step 6: Calculate surcharge with marginal relief
        SurchargeCalculator.SurchargeResult surchargeResult = surchargeCalculator.calculateSurcharge(
                input.getTotalIncome(),
                taxAfterRebate,
                specialRateTax.getTotalTax(),
                input.getRegime());
        computationSteps.add("Surcharge: ₹" + surchargeResult.getEffectiveSurcharge() +
                " (Rate: " + surchargeResult.getSurchargeRate().multiply(new BigDecimal("100")) + "%)");

        // Step 7: Tax + Surcharge
        BigDecimal taxPlusSurcharge = taxAfterRebate.add(surchargeResult.getEffectiveSurcharge());

        // Step 8: Calculate ₹12L cliff marginal relief (AY 2026-27 new regime only)
        RebateCalculator.MarginalReliefResult cliffRelief = rebateCalculator.calculate12LCliffRelief(
                input.getTotalIncome(),
                taxPlusSurcharge,
                input.getRegime(),
                input.getAssessmentYear());
        
        if (cliffRelief.isApplicable()) {
            taxPlusSurcharge = cliffRelief.getFinalTax();
            computationSteps.add("₹12L cliff marginal relief applied: ₹" + cliffRelief.getReliefAmount());
        }

        // Step 9: Calculate cess @ 4%
        BigDecimal cess = taxPlusSurcharge.multiply(CESS_RATE).setScale(0, RoundingMode.HALF_UP);
        computationSteps.add("Health & Education Cess @ 4%: ₹" + cess);

        // Step 10: Total tax liability
        BigDecimal totalTaxLiability = taxPlusSurcharge.add(cess);
        computationSteps.add("Total tax liability: ₹" + totalTaxLiability);

        // Step 11: Relief u/s 89 (if applicable)
        BigDecimal relief89 = input.getRelief89() != null ? input.getRelief89() : BigDecimal.ZERO;
        if (relief89.compareTo(BigDecimal.ZERO) > 0) {
            computationSteps.add("Relief u/s 89: ₹" + relief89);
        }

        // Step 12: Tax after relief
        BigDecimal taxAfterRelief = totalTaxLiability.subtract(relief89);

        // Step 13: Calculate AMT (if applicable)
        AMTCalculator.AMTResult amtResult = null;
        if (input.getAmtInput() != null) {
            input.getAmtInput().setRegularTax(taxAfterRelief);
            amtResult = amtCalculator.calculateAMT(input.getAmtInput());
            
            if (amtResult.isAmtApplicable()) {
                taxAfterRelief = amtResult.getTaxPayable();
                computationSteps.add("AMT applicable: ₹" + amtResult.getAmtAmount() + 
                        " (ATI: ₹" + amtResult.getAdjustedTotalIncome() + ")");
            }
        }

        // Step 14: Taxes paid (TDS + TCS + Advance Tax + Self-Assessment)
        BigDecimal totalTaxesPaid = input.getTdsPaid()
                .add(input.getTcsPaid())
                .add(input.getAdvanceTaxPaid())
                .add(input.getSelfAssessmentTaxPaid());
        computationSteps.add("Total taxes paid: ₹" + totalTaxesPaid);

        // Step 15: Net tax payable/refund
        BigDecimal netTaxPayable = taxAfterRelief.subtract(totalTaxesPaid);
        computationSteps.add("Net tax payable/(refund): ₹" + netTaxPayable);

        // Step 16: Calculate interest u/s 234A (late filing)
        InterestCalculator.Interest234AResult interest234A = interestCalculator.calculate234A(
                netTaxPayable.max(BigDecimal.ZERO),
                input.getDueDate(),
                input.getFilingDate());
        if (interest234A.isApplicable()) {
            computationSteps.add("Interest u/s 234A: ₹" + interest234A.getInterest());
        }

        // Step 17: Calculate interest u/s 234B (advance tax shortfall)
        InterestCalculator.Interest234BResult interest234B = interestCalculator.calculate234B(
                taxAfterRelief,
                input.getAdvanceTaxPaid(),
                input.getTdsPaid(),
                input.isSeniorCitizen(),
                input.isHasBusinessIncome());
        if (interest234B.isApplicable()) {
            computationSteps.add("Interest u/s 234B: ₹" + interest234B.getInterest());
        }

        // Step 18: Calculate interest u/s 234C (installment shortfall)
        InterestCalculator.Interest234CResult interest234C = interestCalculator.calculate234C(
                taxAfterRelief,
                input.getInstallmentPayments(),
                input.isPresumptiveIncome());
        if (interest234C.isApplicable()) {
            computationSteps.add("Interest u/s 234C: ₹" + interest234C.getTotalInterest());
        }

        // Step 19: Calculate late filing fee u/s 234F
        InterestCalculator.Fee234FResult fee234F = interestCalculator.calculate234F(
                input.getTotalIncome(),
                input.getDueDate(),
                input.getFilingDate(),
                input.getAssessmentYear());
        if (fee234F.isApplicable()) {
            computationSteps.add("Late filing fee u/s 234F: ₹" + fee234F.getFee());
        }

        // Step 20: Total interest and fees
        BigDecimal totalInterest = interest234A.getInterest()
                .add(interest234B.getInterest())
                .add(interest234C.getTotalInterest());
        BigDecimal totalFees = fee234F.getFee();

        // Step 21: Final demand/refund
        BigDecimal finalDemand = netTaxPayable.add(totalInterest).add(totalFees);
        computationSteps.add("Final demand/(refund): ₹" + finalDemand);

        // Round final amounts
        finalDemand = finalDemand.setScale(0, RoundingMode.HALF_UP);

        return TaxComputationResult.builder()
                .normalIncome(input.getNormalIncome())
                .taxOnNormalIncome(taxOnNormalIncome)
                .specialRateIncomeTax(specialRateTax)
                .totalTaxBeforeRebate(totalTaxBeforeRebate)
                .rebate87A(rebateResult)
                .taxAfterRebate(taxAfterRebate)
                .surcharge(surchargeResult)
                .cliffMarginalRelief(cliffRelief)
                .cess(cess)
                .totalTaxLiability(totalTaxLiability)
                .relief89(relief89)
                .amtResult(amtResult)
                .taxAfterRelief(taxAfterRelief)
                .tdsPaid(input.getTdsPaid())
                .tcsPaid(input.getTcsPaid())
                .advanceTaxPaid(input.getAdvanceTaxPaid())
                .selfAssessmentTaxPaid(input.getSelfAssessmentTaxPaid())
                .totalTaxesPaid(totalTaxesPaid)
                .netTaxPayable(netTaxPayable)
                .interest234A(interest234A)
                .interest234B(interest234B)
                .interest234C(interest234C)
                .fee234F(fee234F)
                .totalInterest(totalInterest)
                .totalFees(totalFees)
                .finalDemand(finalDemand)
                .computationSteps(computationSteps)
                .build();
    }

    /**
     * Calculate tax on all special rate income
     */
    private SpecialRateIncomeTax calculateSpecialRateIncomeTax(TaxComputationInput input) {
        BigDecimal totalTax = BigDecimal.ZERO;
        List<String> breakdown = new ArrayList<>();

        // STCG u/s 111A
        if (input.getStcg111A() != null && input.getStcg111A().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tax = specialRateCalculator.calculateSTCG111A(
                    input.getStcg111A(),
                    input.getStcg111ASaleDate(),
                    input.getAssessmentYear());
            totalTax = totalTax.add(tax);
            breakdown.add("STCG 111A: ₹" + tax);
        }

        // LTCG u/s 112A
        if (input.getLtcg112A() != null && input.getLtcg112A().compareTo(BigDecimal.ZERO) > 0) {
            SpecialRateIncomeCalculator.LTCG112AResult result = specialRateCalculator.calculateLTCG112A(
                    input.getLtcg112A(),
                    input.getLtcg112ASaleDate(),
                    input.getAssessmentYear(),
                    input.getLtcg112AExemptionUsed() != null ? input.getLtcg112AExemptionUsed() : BigDecimal.ZERO);
            totalTax = totalTax.add(result.getTax());
            breakdown.add("LTCG 112A: ₹" + result.getTax() + " (Exemption: ₹" + result.getExemptionUsed() + ")");
        }

        // LTCG u/s 112
        if (input.getLtcg112() != null && input.getLtcg112().compareTo(BigDecimal.ZERO) > 0) {
            SpecialRateIncomeCalculator.LTCG112Result result = specialRateCalculator.calculateLTCG112(
                    input.getLtcg112WithIndexation() != null ? input.getLtcg112WithIndexation() : input.getLtcg112(),
                    input.getLtcg112(),
                    input.getLtcg112SaleDate(),
                    input.getAssessmentYear(),
                    input.isLtcg112IsProperty());
            totalTax = totalTax.add(result.getTax());
            breakdown.add("LTCG 112: ₹" + result.getTax() + 
                    (result.isIndexationApplied() ? " (with indexation)" : " (without indexation)"));
        }

        // Lottery/Gambling u/s 115BB
        if (input.getIncome115BB() != null && input.getIncome115BB().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tax = specialRateCalculator.calculate115BB(input.getIncome115BB());
            totalTax = totalTax.add(tax);
            breakdown.add("Lottery/Gambling 115BB: ₹" + tax);
        }

        // VDA u/s 115BBH
        if (input.getIncome115BBH() != null && input.getIncome115BBH().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tax = specialRateCalculator.calculate115BBH(input.getIncome115BBH());
            totalTax = totalTax.add(tax);
            breakdown.add("VDA 115BBH: ₹" + tax);
        }

        // Unexplained income u/s 115BBE
        if (input.getIncome115BBE() != null && input.getIncome115BBE().compareTo(BigDecimal.ZERO) > 0) {
            SpecialRateIncomeCalculator.UnexplainedIncomeResult result = 
                    specialRateCalculator.calculate115BBE(input.getIncome115BBE());
            totalTax = totalTax.add(result.getTotalTax());
            breakdown.add("Unexplained 115BBE: ₹" + result.getTotalTax() + " (includes surcharge & cess)");
        }

        return SpecialRateIncomeTax.builder()
                .totalTax(totalTax)
                .breakdown(breakdown)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxComputationInput {
        // Basic info
        private String regime; // OLD or NEW
        private String assessmentYear; // 2025-26 or 2026-27
        private String ageCategory; // REGULAR, SENIOR, SUPER_SENIOR
        private boolean isHUF;
        private boolean isSeniorCitizen;
        private boolean hasBusinessIncome;
        private boolean isPresumptiveIncome;

        // Income
        private BigDecimal totalIncome;
        private BigDecimal normalIncome; // Income taxable at slab rates

        // Special rate income
        private BigDecimal stcg111A;
        private LocalDate stcg111ASaleDate;
        
        private BigDecimal ltcg112A;
        private LocalDate ltcg112ASaleDate;
        private BigDecimal ltcg112AExemptionUsed;
        
        private BigDecimal ltcg112;
        private BigDecimal ltcg112WithIndexation;
        private LocalDate ltcg112SaleDate;
        private boolean ltcg112IsProperty;
        
        private BigDecimal income115BB; // Lottery/gambling
        private BigDecimal income115BBH; // VDA
        private BigDecimal income115BBE; // Unexplained

        // Relief
        private BigDecimal relief89;

        // AMT
        private AMTCalculator.AMTInput amtInput;

        // Taxes paid
        @Builder.Default
        private BigDecimal tdsPaid = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal tcsPaid = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal advanceTaxPaid = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal selfAssessmentTaxPaid = BigDecimal.ZERO;

        // Advance tax installments (for 234C)
        @Builder.Default
        private InterestCalculator.InstallmentPayments installmentPayments = 
                InterestCalculator.InstallmentPayments.builder().build();

        // Dates
        private LocalDate dueDate;
        private LocalDate filingDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxComputationResult {
        private BigDecimal normalIncome;
        private BigDecimal taxOnNormalIncome;
        private SpecialRateIncomeTax specialRateIncomeTax;
        private BigDecimal totalTaxBeforeRebate;
        private RebateCalculator.RebateResult rebate87A;
        private BigDecimal taxAfterRebate;
        private SurchargeCalculator.SurchargeResult surcharge;
        private RebateCalculator.MarginalReliefResult cliffMarginalRelief;
        private BigDecimal cess;
        private BigDecimal totalTaxLiability;
        private BigDecimal relief89;
        private AMTCalculator.AMTResult amtResult;
        private BigDecimal taxAfterRelief;
        private BigDecimal tdsPaid;
        private BigDecimal tcsPaid;
        private BigDecimal advanceTaxPaid;
        private BigDecimal selfAssessmentTaxPaid;
        private BigDecimal totalTaxesPaid;
        private BigDecimal netTaxPayable;
        private InterestCalculator.Interest234AResult interest234A;
        private InterestCalculator.Interest234BResult interest234B;
        private InterestCalculator.Interest234CResult interest234C;
        private InterestCalculator.Fee234FResult fee234F;
        private BigDecimal totalInterest;
        private BigDecimal totalFees;
        private BigDecimal finalDemand;
        private List<String> computationSteps;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecialRateIncomeTax {
        private BigDecimal totalTax;
        private List<String> breakdown;
    }
}
