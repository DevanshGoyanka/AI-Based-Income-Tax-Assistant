package com.itr.interfaces.rest.computation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * CapitalGainsController — /api/v1/capital-gains/*
 */
@RestController
@RequestMapping("/api/v1/capital-gains")
public class CapitalGainsController {

    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculate(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "gainType", "LONG_TERM",
            "longTerm", true,
            "taxableGain", 0,
            "taxRate", 0.125,
            "taxPayable", 0
        ));
    }

    @PostMapping("/calculate-batch")
    public ResponseEntity<Map<String, Object>> calculateBatch(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "transactions", new Object[]{},
            "summary", Map.of("totalCapitalGains", 0, "totalTax", 0)
        ));
    }
}
