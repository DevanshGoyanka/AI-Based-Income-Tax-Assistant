package com.itr.interfaces.rest.advancetax;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AdvanceTaxController — /api/v1/advance-tax/*
 */
@RestController
@RequestMapping("/api/v1/advance-tax")
public class AdvanceTaxController {

    @GetMapping("/{clientId}/{ay}")
    public ResponseEntity<Map<String, Object>> getSchedule(
            @PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("clientId", clientId, "ay", ay, "installments", new Object[]{}));
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
