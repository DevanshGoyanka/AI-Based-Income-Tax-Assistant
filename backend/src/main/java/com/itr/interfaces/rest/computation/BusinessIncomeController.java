package com.itr.interfaces.rest.computation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * BusinessIncomeController — /api/v1/business-income/*
 */
@RestController
@RequestMapping("/api/v1/business-income")
public class BusinessIncomeController {

    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculate(@RequestBody Map<String, Object> request,
                                                          @RequestParam(defaultValue = "2025-26") String assessmentYear) {
        return ResponseEntity.ok(Map.of(
            "scheme", "Regular",
            "assessmentYear", assessmentYear,
            "grossTurnover", 0,
            "taxableIncome", 0,
            "isLoss", false
        ));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "isValid", true,
            "errors", new String[]{}
        ));
    }
}
