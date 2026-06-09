package com.itr.dto;

import com.itr.model.ITRFormType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of ITR Form Classification.
 * Contains recommended form, eligible forms, and detailed reasoning.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ITRClassificationResult {
    
    private String pan;
    private Character entityType;
    private String entityDescription;
    
    private ITRFormType recommendedForm;
    @Builder.Default
    private List<ITRFormType> eligibleForms = new ArrayList<>();
    
    private String classificationReason;
    @Builder.Default
    private List<String> reasonDetails = new ArrayList<>();
    
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    @Builder.Default
    private List<String> ineligibilityReasons = new ArrayList<>();
    
    // Income summary
    private Long totalIncome;
    private Boolean hasCapitalGains;
    private Boolean hasBusinessIncome;
    private Boolean hasMultipleProperties;
    private Boolean hasForeignIncome;
}
