package com.itr.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.itr.service.ClientService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ClientService clientService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        
        // Get all clients for the user
        var clients = clientService.getClientsByUser(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", clients.size());
        stats.put("filed", 0); // TODO: Calculate from client years
        stats.put("inProgress", 0); // TODO: Calculate from client years
        stats.put("docPending", 0); // TODO: Calculate from client years
        stats.put("watchList", 0); // TODO: Implement watch list feature
        stats.put("totalMismatches", 0); // TODO: Implement reconciliation tracking
        stats.put("totalNotices", 0); // TODO: Implement notice tracking
        
        return ResponseEntity.ok(stats);
    }
}
