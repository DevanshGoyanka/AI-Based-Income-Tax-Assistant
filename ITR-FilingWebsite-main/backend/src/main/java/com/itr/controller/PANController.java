package com.itr.controller;

import com.itr.model.ITRFormType;
import com.itr.service.PANTypeDetectionService;
import com.itr.service.PANTypeDetectionService.PANAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for PAN validation and entity type detection.
 */
@RestController
@RequestMapping("/api/pan")
@RequiredArgsConstructor
public class PANController {

    private final PANTypeDetectionService panService;

    /**
     * Validate PAN format.
     * GET /api/pan/{pan}/validate
     */
    @GetMapping("/{pan}/validate")
    public ResponseEntity<Map<String, Object>> validatePAN(@PathVariable String pan) {
        boolean isValid = panService.isValidPAN(pan);
        
        Map<String, Object> response = new HashMap<>();
        response.put("pan", pan.trim().toUpperCase());
        response.put("valid", isValid);
        
        if (!isValid) {
            response.put("message", "Invalid PAN format. Expected: AAAAA9999A");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get entity type from PAN.
     * GET /api/pan/{pan}/entity-type
     */
    @GetMapping("/{pan}/entity-type")
    public ResponseEntity<Map<String, Object>> getEntityType(@PathVariable String pan) {
        try {
            char entityType = panService.getEntityTypeFromPAN(pan);
            String description = panService.getEntityTypeDescription(entityType);
            boolean isIndividualOrHUF = panService.isEligibleForIndividualITRForms(entityType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("pan", pan.trim().toUpperCase());
            response.put("entityType", String.valueOf(entityType));
            response.put("entityDescription", description);
            response.put("isIndividualOrHUF", isIndividualOrHUF);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("pan", pan);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get eligible ITR forms based on PAN.
     * GET /api/pan/{pan}/eligible-itr-forms
     */
    @GetMapping("/{pan}/eligible-itr-forms")
    public ResponseEntity<Map<String, Object>> getEligibleITRForms(@PathVariable String pan) {
        try {
            List<ITRFormType> eligibleForms = panService.getEligibleITRFormsByPAN(pan);
            char entityType = panService.getEntityTypeFromPAN(pan);
            String description = panService.getEntityTypeDescription(entityType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("pan", pan.trim().toUpperCase());
            response.put("entityType", String.valueOf(entityType));
            response.put("entityDescription", description);
            response.put("eligibleITRForms", eligibleForms);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("pan", pan);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Complete PAN analysis with all details.
     * GET /api/pan/{pan}/analyze
     */
    @GetMapping("/{pan}/analyze")
    public ResponseEntity<PANAnalysis> analyzePAN(@PathVariable String pan) {
        PANAnalysis analysis = panService.analyzePAN(pan);
        
        if (!analysis.isValid()) {
            return ResponseEntity.badRequest().body(analysis);
        }
        
        return ResponseEntity.ok(analysis);
    }
}
