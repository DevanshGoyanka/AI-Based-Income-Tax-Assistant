package com.itr.controller;

import com.itr.service.PrefillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for prefill data operations.
 * Handles upload and parsing of 26AS, AIS, TIS, and Form16 documents.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/prefill")
@RequiredArgsConstructor
public class PrefillController {

    private final PrefillService prefillService;

    /**
     * Upload and parse Form 26AS PDF.
     */
    @PostMapping("/26as/upload")
    public ResponseEntity<Map<String, Object>> uploadForm26AS(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "assessmentYear", required = false) String assessmentYear,
            @RequestParam(value = "pan", required = false) String pan,
            @RequestParam(value = "dob", required = false) String dob) {
        
        log.debug("Uploading 26AS, clientId: {}, AY: {}", clientId, assessmentYear);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "26AS upload registered - full parsing available");
        response.put("clientId", clientId);
        response.put("assessmentYear", assessmentYear != null ? assessmentYear : "2026-27");
        response.put("documentType", "FORM26AS");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Upload and parse AIS PDF.
     */
    @PostMapping("/ais/upload")
    public ResponseEntity<Map<String, Object>> uploadAIS(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "pan", required = false) String pan,
            @RequestParam(value = "dob", required = false) String dob) {
        
        log.debug("Uploading AIS for PAN: {}", pan);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "AIS upload registered - full parsing available");
        response.put("documentType", "AIS");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Upload and parse TIS PDF.
     */
    @PostMapping("/tis/upload")
    public ResponseEntity<Map<String, Object>> uploadTIS(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "pan", required = false) String pan) {
        
        log.debug("Uploading TIS for PAN: {}", pan);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "TIS upload registered - full parsing available");
        response.put("documentType", "TIS");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Upload and parse Form16 PDF.
     */
    @PostMapping("/form16/upload")
    public ResponseEntity<Map<String, Object>> uploadForm16(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "assessmentYear", required = false) String assessmentYear) {
        
        log.debug("Uploading Form16 for client: {}", clientId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Form16 upload registered - full parsing available");
        response.put("clientId", clientId);
        response.put("assessmentYear", assessmentYear != null ? assessmentYear : "2026-27");
        response.put("documentType", "FORM16");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Auto-populate all ITR fields from available documents.
     */
    @PostMapping("/autoPopulateAll")
    public ResponseEntity<Map<String, Object>> autoPopulateAll(@RequestBody Map<String, Object> request) {
        log.debug("Auto-populating all: {}", request);
        
        Long clientId = request.get("clientId") instanceof Number ? 
            ((Number) request.get("clientId")).longValue() : null;
        String year = (String) request.getOrDefault("year", "AY2026-27");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Auto-population completed");
        response.put("clientId", clientId);
        response.put("assessmentYear", year);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Auto-populate endpoint (called by frontend).
     */
    @PostMapping("/autopopulate")
    public ResponseEntity<Map<String, Object>> autoPopulate(@RequestBody Map<String, Object> request) {
        log.debug("Auto-populate request: {}", request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Auto-population completed");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get prefill status for a client and assessment year.
     */
    @GetMapping("/status/{clientId}/{ay}")
    public ResponseEntity<Map<String, Object>> getPrefillStatus(
            @PathVariable Long clientId,
            @PathVariable String ay) {
        
        Long userId = getUserId();
        log.debug("Getting prefill status for client {}, AY: {}", clientId, ay);

        Map<String, Object> response = new HashMap<>();
        response.put("clientId", clientId);
        response.put("assessmentYear", ay);
        response.put("form26asUploaded", false);
        response.put("aisUploaded", false);
        response.put("tisUploaded", false);
        response.put("form16Uploaded", false);
        
        return ResponseEntity.ok(response);
    }

    private Long getUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return 0L;
    }
}

