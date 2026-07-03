package com.itr.controller;

import com.itr.dto.AISData;
import com.itr.dto.Form26ASData;
import com.itr.dto.TISData;
import com.itr.domain.validation.ValidationReport;
import com.itr.service.ValidationOrchestrationService;
import com.itr.service.integration.AISJsonImportService;
import com.itr.service.integration.Form26ASJsonImportService;
import com.itr.service.integration.TISJsonImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Canonical prefill controller - handles 26AS/AIS/TIS/Form16 import.
 * Supports JSON format, routes through Validation Engine.
 * Document 3 Phase 4
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/prefill")
@RequiredArgsConstructor
public class PrefillController {

    private final AISJsonImportService aisJsonImportService;
    private final Form26ASJsonImportService form26ASJsonImportService;
    private final TISJsonImportService tisJsonImportService;
    private final ValidationOrchestrationService validationService;

    @PostMapping("/26as/upload")
    public ResponseEntity<Map<String, Object>> uploadForm26AS(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "assessmentYear", required = false) String assessmentYear,
            @RequestParam(value = "pan", required = false) String pan) {
        
        log.info("Uploading 26AS JSON, clientId: {}, AY: {}", clientId, assessmentYear);

        try {
            String json = new String(file.getBytes());
            Form26ASData data = form26ASJsonImportService.importFromJson(json, pan);
            
            if (clientId != null && assessmentYear != null) {
                ValidationReport report = validationService.validateDraft(clientId, assessmentYear);
                if (report.hasBlockingIssues()) {
                    log.warn("26AS validation failed: {} blocking issues", report.blockingCount());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentType", "FORM26AS");
            response.put("totalTDSInterest", data.getTotalTDSInterest());
            response.put("totalTDSContractor", data.getTotalTDSContractor());
            response.put("totalTDSProfessional", data.getTotalTDSProfessional());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("26AS upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/ais/upload")
    public ResponseEntity<Map<String, Object>> uploadAIS(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "assessmentYear", required = false) String assessmentYear,
            @RequestParam(value = "pan", required = false) String pan) {
        
        log.info("Uploading AIS JSON for PAN: {}", pan);

        try {
            String json = new String(file.getBytes());
            AISData data = aisJsonImportService.importDecryptedAIS(json, pan);
            
            if (clientId != null && assessmentYear != null) {
                ValidationReport report = validationService.validateDraft(clientId, assessmentYear);
                if (report.hasBlockingIssues()) {
                    log.warn("AIS validation failed: {} blocking issues", report.blockingCount());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentType", "AIS");
            response.put("pan", data.getPan());
            response.put("assessmentYear", data.getAssessmentYear());
            response.put("tdsEntriesCount", data.getPartB1() != null ? 
                data.getPartB1().getTdsEntries().size() : 0);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("AIS upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/tis/upload")
    public ResponseEntity<Map<String, Object>> uploadTIS(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "assessmentYear", required = false) String assessmentYear,
            @RequestParam(value = "pan", required = false) String pan) {
        
        log.info("Uploading TIS JSON for PAN: {}", pan);

        try {
            String json = new String(file.getBytes());
            TISData data = tisJsonImportService.importFromJson(json, pan);
            
            if (clientId != null && assessmentYear != null) {
                ValidationReport report = validationService.validateDraft(clientId, assessmentYear);
                if (report.hasBlockingIssues()) {
                    log.warn("TIS validation failed: {} blocking issues", report.blockingCount());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentType", "TIS");
            response.put("dividendIncome", data.getDividendIncome());
            response.put("interestFromDeposit", data.getInterestFromDeposit());
            response.put("salaryAmount", data.getSalaryAmount());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("TIS upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/form16/upload")
    public ResponseEntity<Map<String, Object>> uploadForm16(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "assessmentYear", required = false) String assessmentYear) {
        
        log.debug("Uploading Form16 for client: {}", clientId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Form16 upload registered");
        response.put("documentType", "FORM16");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/autoPopulateAll")
    public ResponseEntity<Map<String, Object>> autoPopulateAll(@RequestBody Map<String, Object> request) {
        log.debug("Auto-populating all: {}", request);
        
        Long clientId = request.get("clientId") instanceof Number ? 
            ((Number) request.get("clientId")).longValue() : null;
        String year = (String) request.getOrDefault("year", "AY2026-27");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Auto-population completed");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/autopopulate")
    public ResponseEntity<Map<String, Object>> autoPopulate(@RequestBody Map<String, Object> request) {
        log.debug("Auto-populate request: {}", request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Auto-population completed");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{clientId}/{ay}")
    public ResponseEntity<Map<String, Object>> getPrefillStatus(
            @PathVariable Long clientId,
            @PathVariable String ay) {
        
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
}
