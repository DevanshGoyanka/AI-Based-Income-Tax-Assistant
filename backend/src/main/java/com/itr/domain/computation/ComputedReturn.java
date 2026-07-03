package com.itr.domain.computation;

import com.itr.domain.common.IndianAmount;
import com.itr.domain.common.TaxRegime;
import com.itr.domain.itrselection.ITRFormType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * The single, canonical representation of everything computed for one client,
 * for one AY, under one rule version, under one regime choice.
 * Immutable once constructed - a new ComputedReturn is built for every recomputation.
 * Document 1 §3.2
 */
@Data
@Builder
public final class ComputedReturn {
    
    // Identity & versioning
    private final String clientId;
    private final String assessmentYear;
    private final TaxRegime regimeSelected;
    private final String rulesVersion;
    private final String itrFormVersion;
    private final String jsonSchemaVersion;
    private final Instant computedAt;
    private final String triggeredBy;
    
    // ITR form selection
    private final ITRFormType selectedForm;
    private final String formSelectionTrace;
    
    // Per-schedule results - stub types for Phase 2
    private final ScheduleSResult salary;
    private final ScheduleHPResult houseProperty;
    private final ScheduleCGResult capitalGains;
    private final ScheduleBPResult businessProfession;
    private final ScheduleOSResult otherSources;
    private final ScheduleVDAResult virtualDigitalAssets;
    private final ScheduleCYLAResult currentYearLossAdjustment;
    private final ScheduleBFLAResult broughtForwardLossAdjustment;
    private final ScheduleCFLResult carryForwardOfLosses;
    private final ScheduleVIAResult chapterVIADeductions;
    private final Schedule80GResult donations;
    private final ScheduleAMTResult alternateMinimumTax;
    private final ScheduleSIResult specialRateIncome;
    private final ScheduleEIResult exemptIncome;
    private final ScheduleITResult advanceAndSelfAssessmentTax;
    private final ScheduleTDSResult taxDeductedAtSource;
    private final ScheduleTCSResult taxCollectedAtSource;
    private final ScheduleALResult assetsAndLiabilities;
    
    // Aggregation
    private final IndianAmount grossTotalIncome;
    private final IndianAmount totalDeductionsUnderChapterVIA;
    private final IndianAmount totalIncomeRounded;
    private final IndianAmount taxOnSlabRateIncome;
    private final IndianAmount taxOnSpecialRateIncome;
    private final IndianAmount rebateUnder87A;
    private final IndianAmount surcharge;
    private final IndianAmount healthAndEducationCess;
    private final IndianAmount interestUnder234ABC;
    private final IndianAmount feeUnder234F;
    private final IndianAmount totalTaxLiabilityRounded;
    private final IndianAmount taxesPaid;
    private final IndianAmount refundOrDemand;
    
    // Regime comparison
    private final RegimeComparisonResult regimeComparison;
    
    // Provenance
    private final List<String> sourceDocuments;
    private final String computationHash;
    
    // Stub result classes - real implementations in Phases 6-11
    @Data @Builder public static class ScheduleSResult {}
    @Data @Builder public static class ScheduleHPResult {}
    @Data @Builder public static class ScheduleCGResult {}
    @Data @Builder public static class ScheduleBPResult {}
    @Data @Builder public static class ScheduleOSResult {}
    @Data @Builder public static class ScheduleVDAResult {}
    @Data @Builder public static class ScheduleCYLAResult {}
    @Data @Builder public static class ScheduleBFLAResult {}
    @Data @Builder public static class ScheduleCFLResult {}
    @Data @Builder public static class ScheduleVIAResult {}
    @Data @Builder public static class Schedule80GResult {}
    @Data @Builder public static class ScheduleAMTResult {}
    @Data @Builder public static class ScheduleSIResult {}
    @Data @Builder public static class ScheduleEIResult {}
    @Data @Builder public static class ScheduleITResult {}
    @Data @Builder public static class ScheduleTDSResult {}
    @Data @Builder public static class ScheduleTCSResult {}
    @Data @Builder public static class ScheduleALResult {}
    
    @Data @Builder
    public static class RegimeComparisonResult {
        private final IndianAmount oldRegimeTax;
        private final IndianAmount newRegimeTax;
        private final TaxRegime recommended;
        private final String notes;
    }
}
