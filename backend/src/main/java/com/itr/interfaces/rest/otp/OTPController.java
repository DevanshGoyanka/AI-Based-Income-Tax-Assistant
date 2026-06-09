package com.itr.interfaces.rest.otp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OTPController — /api/v1/otp/*
 */
@RestController
@RequestMapping("/api/v1/otp")
public class OTPController {

    @PostMapping("/batch-send")
    public ResponseEntity<Map<String, Object>> batchSend(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "status", "SENT",
            "count", request.getOrDefault("clientIds", 0)
        ));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of(
            "collected", 0, "pending", 0, "expired", 0, "total", 0
        ));
    }

    @GetMapping("/collect/{token}")
    public ResponseEntity<Map<String, Object>> getCollectPage(@PathVariable String token) {
        return ResponseEntity.ok(Map.of("token", token, "clientName", "", "status", "PENDING"));
    }

    @PostMapping("/collect/{token}")
    public ResponseEntity<Map<String, Object>> submitOTP(@PathVariable String token,
                                                           @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("status", "RECEIVED", "token", token));
    }

    @PostMapping("/{clientId}/{ay}/manual")
    public ResponseEntity<Map<String, Object>> manualEntry(@PathVariable Long clientId,
                                                             @PathVariable String ay,
                                                             @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("status", "RECORDED", "clientId", clientId, "ay", ay));
    }

    @PostMapping("/{clientId}/{ay}/auto-file")
    public ResponseEntity<Map<String, Object>> autoFile(@PathVariable Long clientId,
                                                          @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("status", "QUEUED_FOR_FILING", "clientId", clientId, "ay", ay));
    }
}
