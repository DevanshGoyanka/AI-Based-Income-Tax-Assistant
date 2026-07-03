package com.itr.domain.itrform.ay2026_27;

import com.itr.domain.itrform.EligibilityCriteria;
import com.itr.domain.itrform.ItrFormDefinition;
import com.itr.domain.itrselection.ITRFormType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ITR-3 Definition for AY 2026-27 - For business/profession income.
 * Document 2 §A.1 - includes regular business (books-based) or optional presumptive.
 */
@Component
public class Itr3Definition2026_27 implements ItrFormDefinition {
    
    @Override
    public ITRFormType formType() {
        return ITRFormType.ITR_3;
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
            .maxTotalIncome(null)
            .maxHouseProperties(null)
            .allowsRegularBusiness(true)  // This defines ITR-3
            .allowsPresumptiveBusiness(true)  // Optional
            .allowsCapitalGains(true)
            .maxLTCG112A(null)
            .allowsBroughtForwardLoss(true)
            .allowsCarryForwardLoss(true)
            .allowsForeignAssets(true)
            .allowsDirectorship(true)
            .allowsUnlistedEquity(true)
            .maxAgriculturalIncome(null)
            .build();
    }
}
