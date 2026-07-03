package com.itr.domain.itrform;

import com.itr.domain.itrselection.ITRFormType;

/**
 * ItrFormDefinition - defines which schedules a form carries, eligibility criteria.
 * Document 1 §4.2 - stub for Phase 2, full implementation later.
 */
public interface ItrFormDefinition {
    ITRFormType formType();
    String assessmentYear();
    String version();
    EligibilityCriteria eligibility();
}
