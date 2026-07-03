package com.itr.domain.jsonschema;

import com.itr.domain.computation.ComputedReturn;
import com.itr.domain.itrselection.ITRFormType;

/**
 * JsonSchemaMapper - maps ComputedReturn to ITD JSON format.
 * Document 1 §4.3 - one implementation per (form × AY).
 * Phase 2: stub interface only.
 */
public interface JsonSchemaMapper {
    ITRFormType formType();
    String assessmentYear();
    String jsonSchemaVersion();
    
    /**
     * Generate JSON from ComputedReturn - reads ONLY from ComputedReturn,
     * never from raw input DTOs per Doc 1 §4.3.
     */
    String generateJson(ComputedReturn computedReturn);
}
