package com.itr.interfaces.rest.notice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * NoticeController — /api/v1/notices/*
 */
@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> listNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of("page", page, "size", size, "notices", new Object[]{}));
    }

    @PostMapping("/fetch-all")
    public ResponseEntity<Map<String, Object>> fetchAll() {
        return ResponseEntity.ok(Map.of("status", "FETCH_INITIATED"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getNotice(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("id", id));
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<Map<String, Object>> analyze(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("id", id, "status", "ANALYZED"));
    }

    @PostMapping("/{id}/draft-reply")
    public ResponseEntity<Map<String, Object>> draftReply(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("id", id, "status", "DRAFTED"));
    }

    @PutMapping("/{id}/reply/{version}")
    public ResponseEntity<Map<String, Object>> updateReply(
            @PathVariable Long id, @PathVariable int version,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("id", id, "version", version, "status", "UPDATED"));
    }

    @PostMapping("/{id}/upload-reply")
    public ResponseEntity<Map<String, Object>> uploadReply(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("id", id, "status", "UPLOADED"));
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> analytics() {
        return ResponseEntity.ok(Map.of("totalNotices", 0, "bySection", Map.of()));
    }
}
