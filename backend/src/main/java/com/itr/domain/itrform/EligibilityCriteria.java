package com.itr.domain.itrform;

import com.itr.domain.itrselection.ITRFormType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * EligibilityCriteria - defines who can use an ITR form.
 * Document 2 §A.1 applicability matrix.
 */
@Data
@Builder
public class EligibilityCriteria {
    private final List<String> allowedStatuses;           // INDIVIDUAL, HUF, FIRM
    private final List<String> allowedResidentialStatuses; // RESIDENT_ROR, RNOR, NON_RESIDENT
    private final Long maxTotalIncome;                     // null = no ceiling
    private final Integer maxHouseProperties;
    private final boolean allowsRegularBusiness;
    private final boolean allowsPresumptiveBusiness;
    private final boolean allowsCapitalGains;
    private final Long maxLTCG112A;                        // null = any CG disallowed
    private final boolean allowsBroughtForwardLoss;
    private final boolean allowsCarryForwardLoss;
    private final boolean allowsForeignAssets;
    private final boolean allowsDirectorship;
    private final boolean allowsUnlistedEquity;
    private final Long maxAgriculturalIncome;
}
