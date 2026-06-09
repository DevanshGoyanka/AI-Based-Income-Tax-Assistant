package com.itr.domain.common;

import java.util.ArrayList;
import java.util.List;

/**
 * TaxComputationResult — the aggregate output of full tax computation.
 * Immutable once built.
 */
public class TaxComputationResult {

    private final long totalIncome;
    private final long grossTotalIncome;
    private final long totalDeductions;
    private final long netTaxableIncome;
    private final long taxOnNormalIncome;
    private final long taxOnSpecialRateIncome;
    private final long rebate87A;
    private final long taxAfterRebate;
    private final long surcharge;
    private final long healthAndEducationCess;
    private final long totalTaxLiability;
    private final long relief89;
    private final long taxPayableAfterRelief;
    private final long tdsAmount;
    private final long advanceTaxPaid;
    private final long selfAssessmentTaxPaid;
    private final long totalTaxesPaid;
    private final long interest234A;
    private final long interest234B;
    private final long interest234C;
    private final long fee234F;
    private final long totalInterestAndFees;
    private final long balanceTaxPayable;
    private final long refundAmount;
    private final TaxRegime taxRegime;
    private final List<String> warnings;

    private TaxComputationResult(Builder b) {
        this.totalIncome = b.totalIncome;
        this.grossTotalIncome = b.grossTotalIncome;
        this.totalDeductions = b.totalDeductions;
        this.netTaxableIncome = b.netTaxableIncome;
        this.taxOnNormalIncome = b.taxOnNormalIncome;
        this.taxOnSpecialRateIncome = b.taxOnSpecialRateIncome;
        this.rebate87A = b.rebate87A;
        this.taxAfterRebate = b.taxAfterRebate;
        this.surcharge = b.surcharge;
        this.healthAndEducationCess = b.healthAndEducationCess;
        this.totalTaxLiability = b.totalTaxLiability;
        this.relief89 = b.relief89;
        this.taxPayableAfterRelief = b.taxPayableAfterRelief;
        this.tdsAmount = b.tdsAmount;
        this.advanceTaxPaid = b.advanceTaxPaid;
        this.selfAssessmentTaxPaid = b.selfAssessmentTaxPaid;
        this.totalTaxesPaid = b.totalTaxesPaid;
        this.interest234A = b.interest234A;
        this.interest234B = b.interest234B;
        this.interest234C = b.interest234C;
        this.fee234F = b.fee234F;
        this.totalInterestAndFees = b.totalInterestAndFees;
        this.balanceTaxPayable = b.balanceTaxPayable;
        this.refundAmount = b.refundAmount;
        this.taxRegime = b.taxRegime;
        this.warnings = List.copyOf(b.warnings);
    }

    public long getTotalIncome() { return totalIncome; }
    public long getGrossTotalIncome() { return grossTotalIncome; }
    public long getTotalDeductions() { return totalDeductions; }
    public long getNetTaxableIncome() { return netTaxableIncome; }
    public long getTaxOnNormalIncome() { return taxOnNormalIncome; }
    public long getTaxOnSpecialRateIncome() { return taxOnSpecialRateIncome; }
    public long getRebate87A() { return rebate87A; }
    public long getTaxAfterRebate() { return taxAfterRebate; }
    public long getSurcharge() { return surcharge; }
    public long getHealthAndEducationCess() { return healthAndEducationCess; }
    public long getTotalTaxLiability() { return totalTaxLiability; }
    public long getRelief89() { return relief89; }
    public long getTaxPayableAfterRelief() { return taxPayableAfterRelief; }
    public long getTdsAmount() { return tdsAmount; }
    public long getAdvanceTaxPaid() { return advanceTaxPaid; }
    public long getSelfAssessmentTaxPaid() { return selfAssessmentTaxPaid; }
    public long getTotalTaxesPaid() { return totalTaxesPaid; }
    public long getInterest234A() { return interest234A; }
    public long getInterest234B() { return interest234B; }
    public long getInterest234C() { return interest234C; }
    public long getFee234F() { return fee234F; }
    public long getTotalInterestAndFees() { return totalInterestAndFees; }
    public long getBalanceTaxPayable() { return balanceTaxPayable; }
    public long getRefundAmount() { return refundAmount; }
    public TaxRegime getTaxRegime() { return taxRegime; }
    public List<String> getWarnings() { return warnings; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private long totalIncome;
        private long grossTotalIncome;
        private long totalDeductions;
        private long netTaxableIncome;
        private long taxOnNormalIncome;
        private long taxOnSpecialRateIncome;
        private long rebate87A;
        private long taxAfterRebate;
        private long surcharge;
        private long healthAndEducationCess;
        private long totalTaxLiability;
        private long relief89;
        private long taxPayableAfterRelief;
        private long tdsAmount;
        private long advanceTaxPaid;
        private long selfAssessmentTaxPaid;
        private long totalTaxesPaid;
        private long interest234A;
        private long interest234B;
        private long interest234C;
        private long fee234F;
        private long totalInterestAndFees;
        private long balanceTaxPayable;
        private long refundAmount;
        private TaxRegime taxRegime = TaxRegime.NEW;
        private List<String> warnings = new ArrayList<>();

        public Builder totalIncome(long v) { this.totalIncome = v; return this; }
        public Builder grossTotalIncome(long v) { this.grossTotalIncome = v; return this; }
        public Builder totalDeductions(long v) { this.totalDeductions = v; return this; }
        public Builder netTaxableIncome(long v) { this.netTaxableIncome = v; return this; }
        public Builder taxOnNormalIncome(long v) { this.taxOnNormalIncome = v; return this; }
        public Builder taxOnSpecialRateIncome(long v) { this.taxOnSpecialRateIncome = v; return this; }
        public Builder rebate87A(long v) { this.rebate87A = v; return this; }
        public Builder taxAfterRebate(long v) { this.taxAfterRebate = v; return this; }
        public Builder surcharge(long v) { this.surcharge = v; return this; }
        public Builder healthAndEducationCess(long v) { this.healthAndEducationCess = v; return this; }
        public Builder totalTaxLiability(long v) { this.totalTaxLiability = v; return this; }
        public Builder relief89(long v) { this.relief89 = v; return this; }
        public Builder taxPayableAfterRelief(long v) { this.taxPayableAfterRelief = v; return this; }
        public Builder tdsAmount(long v) { this.tdsAmount = v; return this; }
        public Builder advanceTaxPaid(long v) { this.advanceTaxPaid = v; return this; }
        public Builder selfAssessmentTaxPaid(long v) { this.selfAssessmentTaxPaid = v; return this; }
        public Builder totalTaxesPaid(long v) { this.totalTaxesPaid = v; return this; }
        public Builder interest234A(long v) { this.interest234A = v; return this; }
        public Builder interest234B(long v) { this.interest234B = v; return this; }
        public Builder interest234C(long v) { this.interest234C = v; return this; }
        public Builder fee234F(long v) { this.fee234F = v; return this; }
        public Builder totalInterestAndFees(long v) { this.totalInterestAndFees = v; return this; }
        public Builder balanceTaxPayable(long v) { this.balanceTaxPayable = v; return this; }
        public Builder refundAmount(long v) { this.refundAmount = v; return this; }
        public Builder taxRegime(TaxRegime v) { this.taxRegime = v; return this; }
        public Builder warnings(List<String> v) { this.warnings = new ArrayList<>(v); return this; }
        public Builder addWarning(String w) { this.warnings.add(w); return this; }

        public TaxComputationResult build() { return new TaxComputationResult(this); }
    }
}
