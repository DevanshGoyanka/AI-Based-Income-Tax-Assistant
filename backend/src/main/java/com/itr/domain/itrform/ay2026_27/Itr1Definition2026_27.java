package com.itr.domain.itrform.ay2026_27;

import com.itr.domain.itrform.EligibilityCriteria;
import com.itr.domain.itrform.ItrFormDefinition;
import com.itr.domain.itrselection.ITRFormType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ITR-1 Definition for AY 2026-27 - Sahaj form.
 * Document 2 §A.1 - residents only, ≤50L income, ≤2 house properties,
 * LTCG 112A up to 1.25L (new for AY 2026-27).
 */
@Component
public class Itr1Definition2026_27 implements ItrFormDefinition {
    
    @Override
    public ITRFormType formType() {
        return ITRFormType.ITR_1;
    }
    
    @Override
    public String assessmentYear() {
        return "2026-27";
    }
    
    @Override
    public String version() {
        return "2026.2"; // Post-corrigendum (10 April 2026)
    }
    
    @Override
    public EligibilityCriteria eligibility() {
        return EligibilityCriteria.builder()
            .allowedStatuses(List.of("INDIVIDUAL"))
            .allowedResidentialStatuses(List.of("RESIDENT_ROR"))
            .maxTotalIncome(5000000L)  // ≤50 lakh
            .maxHouseProperties(2)      // NEW for AY 2026-27
            .allowsRegularBusiness(false)
            .allowsPresumptiveBusiness(false)
            .allowsCapitalGains(true)   // Limited to 112A only
            .maxLTCG112A(125000L)       // NEW carve-out for AY 2026-27
            .allowsBroughtForwardLoss(false)
            .allowsCarryForwardLoss(false)
            .allowsForeignAssets(false)
            .allowsDirectorship(false)
            .allowsUnlistedEquity(false)
            .maxAgriculturalIncome(5000L)
            .build();
    }
}
