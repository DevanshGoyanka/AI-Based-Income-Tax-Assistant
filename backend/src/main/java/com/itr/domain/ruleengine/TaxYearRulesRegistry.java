package com.itr.domain.ruleengine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TaxYearRulesRegistry - auto-collects all TaxYearRules implementations via Spring.
 * Document 1 §4.1 - adding a new AY = implement TaxYearRules + @Component, zero config.
 */
@Component
@Slf4j
public class TaxYearRulesRegistry {
    
    private final Map<String, TaxYearRules> rulesByAY;
    
    public TaxYearRulesRegistry(List<TaxYearRules> allRules) {
        this.rulesByAY = allRules.stream()
            .collect(Collectors.toMap(TaxYearRules::assessmentYear, Function.identity()));
        log.info("TaxYearRulesRegistry initialized with {} AY implementations: {}", 
            allRules.size(), rulesByAY.keySet());
    }
    
    public TaxYearRules forAY(String assessmentYear) {
        TaxYearRules rules = rulesByAY.get(assessmentYear);
        if (rules == null) {
            throw new IllegalArgumentException("No tax rules found for AY " + assessmentYear);
        }
        return rules;
    }
    
    public List<String> availableAYs() {
        return rulesByAY.keySet().stream().sorted().toList();
    }
}
