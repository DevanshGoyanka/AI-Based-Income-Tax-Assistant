package com.itr.interfaces.rest.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AdminController — /api/v1/admin/*
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> listUsers() {
        return ResponseEntity.ok(Map.of("users", new Object[]{}));
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("id", 0, "status", "CREATED"));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> changeRole(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("id", id, "role", body.get("role")));
    }

    @GetMapping("/audit-trail")
    public ResponseEntity<Map<String, Object>> auditTrail() {
        return ResponseEntity.ok(Map.of("entries", new Object[]{}));
    }

    @GetMapping("/firm")
    public ResponseEntity<Map<String, Object>> getFirm() {
        return ResponseEntity.ok(Map.of("name", ""));
    }

    @PutMapping("/firm")
    public ResponseEntity<Map<String, Object>> updateFirm(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "UPDATED"));
    }
}
