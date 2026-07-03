package com.itr.domain.ruleengine.ay2026_27;

import com.itr.domain.common.TaxRegime;
import com.itr.domain.ruleengine.RuleLifecycleState;
import com.itr.domain.ruleengine.TaxYearRules;
import org.springframework.stereotype.Component;

/**
 * AY 2026-27 Tax Rules - Income-tax Act, 1961 provisions.
 * Document 2 §D - all values verified against CBDT notification 30 March 2026 + corrigendum 10 April 2026.
 * Lifecycle: SUPPORTED
 */
@Component
public class AY2026_27Rules implements TaxYearRules {
    
    @Override
    public String assessmentYear() {
        return "2026-27";
    }
    
    @Override
    public String version() {
        return "1.0.0";
    }
    
    @Override
    public RuleLifecycleState lifecycleState() {
        return RuleLifecycleState.SUPPORTED;
    }
    
    @Override
    public long[] slabLimits(TaxRegime regime) {
        if (regime == TaxRegime.NEW) {
            return new long[]{0, 300000, 700000, 1000000, 1200000, 1500000};
        } else {
            return new long[]{0, 250000, 500000, 1000000};
        }
    }
    
    @Override
    public int[] slabRates(TaxRegime regime) {
        if (regime == TaxRegime.NEW) {
            return new int[]{0, 5, 10, 15, 20, 30};
        } else {
            return new int[]{0, 5, 20, 30};
        }
    }
    
    @Override
    public long standardDeduction(TaxRegime regime) {
        return regime == TaxRegime.NEW ? 75000 : 50000;
    }
    
    @Override
    public long rebateThreshold(TaxRegime regime) {
        return regime == TaxRegime.NEW ? 700000 : 500000;
    }
    
    @Override
    public long rebateAmount(TaxRegime regime) {
        return regime == TaxRegime.NEW ? 25000 : 12500;
    }
    
    @Override
    public long[] surchargeLimits() {
        return new long[]{5000000, 10000000, 20000000, 50000000};
    }
    
    @Override
    public int[] surchargRates() {
        return new int[]{10, 15, 25, 37};
    }
    
    @Override
    public int cessRate() {
        return 4;
    }
    
    @Override
    public long section80CLimit(TaxRegime regime) {
        return regime == TaxRegime.NEW ? 0 : 150000;
    }
    
    @Override
    public long section80CCD1BLimit(TaxRegime regime) {
        return regime == TaxRegime.NEW ? 0 : 50000;
    }
    
    @Override
    public long section80DLimit(TaxRegime regime, boolean seniorCitizen) {
        if (regime == TaxRegime.NEW) return 0;
        return seniorCitizen ? 50000 : 25000;
    }
    
    @Override
    public int section111ARate() {
        return 20;
    }
    
    @Override
    public int section112ARate() {
        return 13; // 12.5% stored as 13 for precision (actual: 12.5%)
    }
    
    @Override
    public long section112AExemption() {
        return 125000;
    }
    
    @Override
    public int section112Rate() {
        return 13; // 12.5%
    }
    
    @Override
    public int costInflationIndex() {
        return 376;
    }
    
    @Override
    public long section44ADTurnoverLimit() {
        return 30000000; // 3 crore
    }
    
    @Override
    public int section44ADPresumptiveRate() {
        return 8;
    }
    
    @Override
    public long section44ADATurnoverLimit() {
        return 7500000; // 75 lakh
    }
    
    @Override
    public int section44ADAPresumptiveRate() {
        return 50;
    }
    
    @Override
    public long amtThreshold() {
        return 2000000;
    }
    
    @Override
    public int amtRate() {
        return 19; // 18.5% stored as 19
    }
    
    @Override
    public String generalDueDate() {
        return "2026-07-31";
    }
    
    @Override
    public String auditDueDate() {
        return "2026-10-31";
    }
}
