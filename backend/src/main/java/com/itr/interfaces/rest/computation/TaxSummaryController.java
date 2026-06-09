package com.itr.interfaces.rest.computation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * TaxSummaryController — /api/v1/tax-summary/*
 */
@RestController
@RequestMapping("/api/v1/tax-summary")
public class TaxSummaryController {

    @PostMapping("/compute")
    public ResponseEntity<Map<String, Object>> compute(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "grossIncome", 0,
            "deductions", 0,
            "taxableIncome", 0,
            "tax", 0,
            "cess", 0,
            "totalTax", 0
        ));
    }
}
