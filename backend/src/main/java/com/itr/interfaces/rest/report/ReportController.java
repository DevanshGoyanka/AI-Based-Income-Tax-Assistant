package com.itr.interfaces.rest.report;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ReportController — /api/v1/reports/*
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    @GetMapping("/computation-sheet/{clientId}/{ay}")
    public ResponseEntity<Map<String, Object>> computationSheet(
            @PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("url", ""));
    }

    @GetMapping("/filing-status")
    public ResponseEntity<Map<String, Object>> filingStatus() {
        return ResponseEntity.ok(Map.of("filed", 0, "pending", 0, "overdue", 0));
    }

    @GetMapping("/tax-savings/{clientId}/{ay}")
    public ResponseEntity<Map<String, Object>> taxSavings(
            @PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("suggestions", new Object[]{}));
    }

    @GetMapping("/ais-vs-itr/{clientId}/{ay}")
    public ResponseEntity<Map<String, Object>> aisVsItr(
            @PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("mismatches", new Object[]{}));
    }
}
