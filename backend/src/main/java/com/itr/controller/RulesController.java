package com.itr.controller;

import com.itr.domain.taxyear.TaxYearRules;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RulesController - exposes TaxYearRules for frontend access.
 * Document 1 §5.2 - frontend fetches rules at page load.
 */
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RulesController {
    
    private final List<TaxYearRules> allRules;
    
    @GetMapping("/{assessmentYear}")
    public ResponseEntity<Map<String, Object>> getRules(@PathVariable String assessmentYear) {
        TaxYearRules rules = allRules.stream()
            .filter(r -> r.assessmentYear().equals(assessmentYear))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Rules not found for AY " + assessmentYear));
        
        return ResponseEntity.ok(Map.of(
            "assessmentYear", rules.assessmentYear(),
            "version", rules.version(),
            "newRegime", Map.of(
                "slabs", rules.newRegimeSlabs(),
                "standardDeduction", rules.newRegimeStandardDeduction(),
                "rebate87A", rules.newRegimeRebate87A()
            ),
            "oldRegime", Map.of(
                "slabs", rules.oldRegimeSlabs(),
                "standardDeduction", rules.oldRegimeStandardDeduction(),
                "rebate87A", rules.oldRegimeRebate87A()
            ),
            "surcharge", Map.of(
                "threshold50L", rules.surchargeThreshold50L(),
                "threshold1Cr", rules.surchargeThreshold1Cr(),
                "threshold2Cr", rules.surchargeThreshold2Cr(),
                "threshold5Cr", rules.surchargeThreshold5Cr()
            ),
            "cess", rules.healthEducationCess()
        ));
    }
}
