package com.itr.interfaces.rest.tds;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * TDSController — /api/v1/tds/*
 */
@RestController
@RequestMapping("/api/v1/tds")
public class TDSController {

    @GetMapping("/returns")
    public ResponseEntity<Map<String, Object>> listReturns() {
        return ResponseEntity.ok(Map.of("returns", new Object[]{}));
    }

    @PostMapping("/returns")
    public ResponseEntity<Map<String, Object>> createReturn(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("id", 0, "status", "CREATED"));
    }

    @GetMapping("/returns/{id}/form16")
    public ResponseEntity<Map<String, Object>> getForm16(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("status", "GENERATED", "url", ""));
    }

    @PostMapping("/returns/{id}/fvu")
    public ResponseEntity<Map<String, Object>> generateFVU(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("status", "GENERATED", "fvuUrl", ""));
    }

    @PostMapping("/reconcile/{clientId}/{ay}")
    public ResponseEntity<Map<String, Object>> reconcile(
            @PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("clientId", clientId, "ay", ay, "mismatches", new Object[]{}));
    }

    @PostMapping("/challans")
    public ResponseEntity<Map<String, Object>> recordChallan(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("id", 0, "status", "RECORDED"));
    }

    @GetMapping("/challans/{clientId}/{ay}")
    public ResponseEntity<Map<String, Object>> getChallanHistory(
            @PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("clientId", clientId, "ay", ay, "challans", new Object[]{}));
    }
}
