package com.itr.domain.taxyear;

import java.util.List;

/**
 * TaxYearRules - defines all computation parameters for one assessment year.
 * Document 1 §5.1 - one implementation per AY, versioned.
 */
public interface TaxYearRules {
    String assessmentYear();
    String version();
    
    // New regime
    List<TaxSlab> newRegimeSlabs();
    long newRegimeStandardDeduction();
    long newRegimeRebate87A();
    
    // Old regime
    List<TaxSlab> oldRegimeSlabs();
    long oldRegimeStandardDeduction();
    long oldRegimeRebate87A();
    
    // Surcharge thresholds
    long surchargeThreshold50L();
    long surchargeThreshold1Cr();
    long surchargeThreshold2Cr();
    long surchargeThreshold5Cr();
    
    // Cess
    double healthEducationCess();
    
    record TaxSlab(long limit, double rate) {}
}
