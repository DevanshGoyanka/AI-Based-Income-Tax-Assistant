package com.itr.domain.itrform.ay2026_27;

import com.itr.domain.itrform.EligibilityCriteria;
import com.itr.domain.itrform.ItrFormDefinition;
import com.itr.domain.itrselection.ITRFormType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ITR-4 Definition for AY 2026-27 - Sugam (presumptive only).
 * Document 2 §A.1 - 44AD/44ADA/44AE, ≤50L, ≤2 properties, same 112A carve-out as ITR-1.
 */
@Component
public class Itr4Definition2026_27 implements ItrFormDefinition {
    
    @Override
    public ITRFormType formType() {
        return ITRFormType.ITR_4;
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
            .allowedStatuses(List.of("INDIVIDUAL", "HUF", "FIRM"))
            .allowedResidentialStatuses(List.of("RESIDENT_ROR"))
            .maxTotalIncome(5000000L)
            .maxHouseProperties(2)
            .allowsRegularBusiness(false)
            .allowsPresumptiveBusiness(true)  // This defines ITR-4
            .allowsCapitalGains(true)  // Same 112A limit as ITR-1
            .maxLTCG112A(125000L)
            .allowsBroughtForwardLoss(false)
            .allowsCarryForwardLoss(false)
            .allowsForeignAssets(false)
            .allowsDirectorship(false)
            .allowsUnlistedEquity(false)
            .maxAgriculturalIncome(5000L)
            .build();
    }
}
