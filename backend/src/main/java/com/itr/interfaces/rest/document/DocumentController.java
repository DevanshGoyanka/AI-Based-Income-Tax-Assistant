package com.itr.interfaces.rest.document;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * DocumentController — /api/v1/documents/*
 */
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload() {
        return ResponseEntity.ok(Map.of("id", 0, "status", "UPLOADED", "s3Key", ""));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, Object>> download(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("url", "", "id", id));
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<Map<String, Object>> share(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("shareToken", "", "expiresAt", ""));
    }

    @GetMapping("/share/{token}")
    public ResponseEntity<Map<String, Object>> sharedDocument(@PathVariable String token) {
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/{id}/watermark")
    public ResponseEntity<Map<String, Object>> watermark(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("id", id, "status", "WATERMARKED"));
    }

    @PostMapping("/computation-sheet/{clientId}/{ay}")
    public ResponseEntity<Map<String, Object>> generateComputationSheet(
            @PathVariable Long clientId, @PathVariable String ay) {
        return ResponseEntity.ok(Map.of("status", "GENERATED", "url", ""));
    }
}
