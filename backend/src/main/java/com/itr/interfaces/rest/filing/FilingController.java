package com.itr.interfaces.rest.filing;

import com.itr.application.filing.FilingWorkflowUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;

/**
 * FilingController — /api/v1/filing/*
 */
@RestController
@RequestMapping("/api/v1/filing")
public class FilingController {

    @GetMapping
    public ResponseEntity<?> list() {
        // Return empty list - no global filing list per se
        return ResponseEntity.ok(new ArrayList<>());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "CREATED"));
    }

    @PostMapping("/{clientId}/{ay}/prefill")
    public ResponseEntity<Map<String, Object>> prefill(@PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("status", "PREFILLED", "clientId", clientId, "ay", ay));
    }

    @PostMapping("/{clientId}/{ay}/compute")
    public ResponseEntity<Map<String, Object>> compute(@PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("status", "COMPUTED", "clientId", clientId, "ay", ay));
    }

    @PostMapping("/{clientId}/{ay}/validate")
    public ResponseEntity<Map<String, Object>> validate(@PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("status", "VALIDATED", "clientId", clientId, "ay", ay));
    }

    @PostMapping("/{clientId}/{ay}/submit")
    public ResponseEntity<Map<String, Object>> submit(@PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("status", "SUBMITTED", "clientId", clientId, "ay", ay));
    }

    @PostMapping("/{clientId}/{ay}/verify")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("status", "VERIFIED", "clientId", clientId, "ay", ay));
    }

    @GetMapping("/{clientId}/{ay}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("clientId", clientId, "ay", ay, "status", "DRAFT"));
    }

    @PostMapping("/{clientId}/{ay}/revise")
    public ResponseEntity<Map<String, Object>> revise(@PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("status", "REVISION_IN_PROGRESS", "clientId", clientId, "ay", ay));
    }

    @PostMapping("/bulk/queue")
    public ResponseEntity<Map<String, Object>> bulkQueue(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("status", "QUEUED", "count", request.getOrDefault("clientIds", 0)));
    }

    @GetMapping("/bulk/status")
    public ResponseEntity<Map<String, Object>> bulkStatus() {
        return ResponseEntity.ok(Map.of("status", "RUNNING", "completed", 0, "total", 0));
    }
}
