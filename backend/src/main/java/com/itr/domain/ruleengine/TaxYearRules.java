package com.itr.domain.ruleengine;

import com.itr.domain.common.TaxRegime;

/**
 * TaxYearRules interface - one implementation per Assessment Year.
 * Document 1 §4.1, §5.2
 */
public interface TaxYearRules {
    String assessmentYear();
    String version();
    RuleLifecycleState lifecycleState();
    
    // Slab and rates
    long[] slabLimits(TaxRegime regime);
    int[] slabRates(TaxRegime regime);
    
    // Standard deduction
    long standardDeduction(TaxRegime regime);
    
    // Rebate u/s 87A
    long rebateThreshold(TaxRegime regime);
    long rebateAmount(TaxRegime regime);
    
    // Surcharge slabs
    long[] surchargeLimits();
    int[] surchargRates();
    
    // Cess
    int cessRate();
    
    // Chapter VIA limits
    long section80CLimit(TaxRegime regime);
    long section80CCD1BLimit(TaxRegime regime);
    long section80DLimit(TaxRegime regime, boolean seniorCitizen);
    
    // Special rates
    int section111ARate();  // STCG equity
    int section112ARate();  // LTCG equity
    long section112AExemption();
    int section112Rate();   // LTCG other assets
    
    // CII for indexation
    int costInflationIndex();
    
    // Presumptive taxation
    long section44ADTurnoverLimit();
    int section44ADPresumptiveRate();
    long section44ADATurnoverLimit();
    int section44ADAPresumptiveRate();
    
    // AMT
    long amtThreshold();
    int amtRate();
    
    // Due dates
    String generalDueDate();
    String auditDueDate();
}
