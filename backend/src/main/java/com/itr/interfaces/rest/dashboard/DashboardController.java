package com.itr.interfaces.rest.dashboard;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * DashboardController — /api/v1/dashboard/*
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of(
            "totalClients", 0,
            "filed", 0,
            "pending", 0,
            "overdue", 0,
            "pendingNotices", 0,
            "revenueThisMonth", 0L,
            "dueDates", new Object[]{}
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(@RequestParam(required = false) String ay) {
        // Return mock stats for now - in production this would fetch from database
        return ResponseEntity.ok(Map.of(
            "total", 0,
            "filed", 0,
            "inProgress", 0,
            "docPending", 0,
            "watchList", 0,
            "totalMismatches", 0,
            "totalNotices", 0
        ));
    }
}
