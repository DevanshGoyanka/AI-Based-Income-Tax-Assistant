package com.itr.domain.itrform.ay2026_27;

import com.itr.domain.itrform.EligibilityCriteria;
import com.itr.domain.itrform.ItrFormDefinition;
import com.itr.domain.itrselection.ITRFormType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ITR-2 Definition for AY 2026-27 - For individuals/HUF not eligible for ITR-1.
 * Document 2 §A.1 - any capital gains, multiple properties, foreign assets allowed.
 */
@Component
public class Itr2Definition2026_27 implements ItrFormDefinition {
    
    @Override
    public ITRFormType formType() {
        return ITRFormType.ITR_2;
    }
    
    @Override
    public String assessmentYear() {
        return "2026-27";
    }
    
    @Override
    public String version() {
        return "2026.2";
    }
    
    @Override
    public EligibilityCriteria eligibility() {
        return EligibilityCriteria.builder()
            .allowedStatuses(List.of("INDIVIDUAL", "HUF"))
            .allowedResidentialStatuses(List.of("RESIDENT_ROR", "RNOR", "NON_RESIDENT"))
            .maxTotalIncome(null)  // No ceiling
            .maxHouseProperties(null)  // Any number
            .allowsRegularBusiness(false)
            .allowsPresumptiveBusiness(false)
            .allowsCapitalGains(true)  // Full Schedule CG
            .maxLTCG112A(null)  // Any amount
            .allowsBroughtForwardLoss(true)
            .allowsCarryForwardLoss(true)
            .allowsForeignAssets(true)
            .allowsDirectorship(true)
            .allowsUnlistedEquity(true)
            .maxAgriculturalIncome(null)
            .build();
    }
}
