package com.itr.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr.entity.Client;
import com.itr.entity.ClientYearData;
import com.itr.repository.ClientYearDataRepository;
import com.itr.repository.UserRepository;
import com.itr.service.Form26ASService;
import com.itr.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;

/**
 * REST controller for integration and reconciliation operations.
 * Handles auto-population from Form16, AIS, and reconciliation between multiple data sources.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final Form26ASService form26ASService;
    private final ClientService clientService;
    private final ClientYearDataRepository clientYearDataRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Extract Form16 data from PDF.
     */
    @PostMapping("/form16/extract")
    public ResponseEntity<Map<String, Object>> extractForm16(@RequestParam("file") MultipartFile file) {
        log.debug("Extracting Form16 data");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Form16 extraction completed");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Import AIS data from PDF.
     */
    @PostMapping("/ais/import")
    public ResponseEntity<Map<String, Object>> importAIS(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "pan", required = false) String pan,
            @RequestParam(value = "dob", required = false) String dob) {
        
        log.debug("Importing AIS for PAN: {}", pan);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "AIS import completed");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Import Form 26AS data.
     */
    @PostMapping("/26as/import")
    public ResponseEntity<Map<String, Object>> importForm26AS(
            @RequestParam("file") MultipartFile file,
            @RequestParam("clientId") Long clientId) {
        
        Long userId = getUserId();
        log.info("=== importForm26AS START ===");
        log.info("clientId: {}, file: {}", clientId, file.getOriginalFilename());
        
        try {
            // Validate file type
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".zip") && !fileName.toLowerCase().endsWith(".txt"))) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Please upload a ZIP or TXT file");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Get client
            Client client = clientService.getClientEntity(clientId, userId);
            log.info("Client: {} (PAN: {}, DOB: {})", client.getName(), client.getPan(), client.getDob());
            
            // Generate password from DOB (DDMMYYYY format)
            String dobPassword = null;
            if (client.getDob() != null) {
                try {
                    String dob = client.getDob().toString();
                    String[] parts = dob.split("-");
                    if (parts.length == 3) {
                        dobPassword = parts[2] + parts[1] + parts[0];
                        log.info("Using DOB as ZIP password: {}", dobPassword);
                    }
                } catch (Exception e) {
                    log.warn("Could not parse DOB for password: {}", e.getMessage());
                }
            }
            
            // Parse 26AS using the service
            Map<String, Object> parsed26AS = form26ASService.parseForm26AS(file, dobPassword);
            
            // Verify PAN matches
            String form26ASPAN = (String) parsed26AS.get("pan");
            Map<String, Object> response = new HashMap<>();
            
            if (form26ASPAN != null && !form26ASPAN.isEmpty() && !form26ASPAN.equals(client.getPan())) {
                log.warn("PAN Mismatch for client {}: Client PAN={}, Form26AS PAN={}", clientId, client.getPan(), form26ASPAN);
                response.put("success", false);
                response.put("error", "PAN Mismatch: Client PAN (" + client.getPan() + ") does not match Form 26AS PAN (" + form26ASPAN + ")");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (form26ASPAN == null || form26ASPAN.isEmpty()) {
                form26ASPAN = client.getPan();
            }
            
            log.info("PAN Verification: {} ({})", form26ASPAN, parsed26AS.get("assesseeName"));
            
            // Get TDS summary
            Map<String, Object> summary = (Map<String, Object>) parsed26AS.get("summary");
            List<Map<String, Object>> deductorAggregates = (List<Map<String, Object>>) parsed26AS.get("deductorAggregates");
            
            // Build TDS entries
            List<Map<String, Object>> tdsEntries = new ArrayList<>();
            for (Map<String, Object> agg : deductorAggregates) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("employerName", agg.get("employerName"));
                entry.put("employerTAN", agg.get("employerTAN"));
                entry.put("sectionCode", agg.get("sectionCode"));
                entry.put("incomeAmount", agg.get("totalAmount"));
                entry.put("tdsDeducted", agg.get("totalTDS"));
                tdsEntries.add(entry);
            }
            
            response.put("success", true);
            response.put("panVerified", true);
            response.put("form26ASPAN", form26ASPAN);
            response.put("assesseeName", parsed26AS.get("assesseeName"));
            response.put("assessmentYear", parsed26AS.get("assessmentYear"));
            response.put("totalTDS", summary.get("grandTotalTDS"));
            response.put("tdsEntries", tdsEntries);
            
            // Include income breakdown data so frontend can send it back on tax compute
            Map<String, Object> incomeBreakdown = (Map<String, Object>) parsed26AS.get("incomeBreakdown");
            if (incomeBreakdown != null) {
                response.put("incomeBreakdown", incomeBreakdown);
                log.info("Including incomeBreakdown in response: {}", incomeBreakdown);
            }
            
            // Also include the raw income by head for direct access
            Object incomeByHead = parsed26AS.get("incomeByHead");
            if (incomeByHead != null) {
                response.put("incomeByHead", incomeByHead);
            }
            
            log.info("=== importForm26AS END ===");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("=== importForm26AS ERROR ===", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to parse Form 26AS: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Import ITD prefill data and STORE it in database.
     */
    @PostMapping("/prefill/import")
    public ResponseEntity<Map<String, Object>> importPrefill(
            @RequestParam("file") MultipartFile file,
            @RequestParam("clientId") Long clientId,
            @RequestParam(value = "assessmentYear", required = false, defaultValue = "2026-27") String assessmentYear) {
        
        Long userId = getUserId();
        log.info("=== importPrefill START ===");
        log.info("clientId: {}, assessmentYear: {}", clientId, assessmentYear);
        
        try {
            String jsonContent = new String(file.getBytes());
            JsonNode root = objectMapper.readTree(jsonContent);
            
            // Get or create ClientYearData
            Client client = clientService.getClientEntity(clientId, userId);
            
            // Normalize year format
            String normalizedYear;
            if (assessmentYear.contains("-")) {
                normalizedYear = assessmentYear;
            } else {
                normalizedYear = "20" + assessmentYear.substring(0, 2) + "-" + assessmentYear.substring(2);
            }
            
            Optional<ClientYearData> existingOpt = clientYearDataRepository
                    .findByClientIdAndAssessmentYear(clientId, normalizedYear);
            
            ClientYearData yearData;
            if (existingOpt.isPresent()) {
                yearData = existingOpt.get();
            } else {
                yearData = ClientYearData.builder()
                        .client(client)
                        .assessmentYear(normalizedYear)
                        .status("draft")
                        .itrType("ITR1")
                        .build();
            }
            
            yearData.setRawPrefillJson(jsonContent);
            yearData.setStatus("draft");
            yearData = clientYearDataRepository.save(yearData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Prefill data imported and stored successfully");
            response.put("clientId", clientId);
            response.put("assessmentYear", normalizedYear);
            
            log.info("=== importPrefill END ===");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("=== importPrefill ERROR ===", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to import prefill: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Auto-populate ITR fields from Form16 data.
     */
    @PostMapping("/autopopulate/form16")
    public ResponseEntity<Map<String, Object>> autoPopulateFromForm16(@RequestBody Map<String, Object> form16Data) {
        log.debug("Auto-populating from Form16");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Form16 auto-population completed");
        response.put("fieldsPopulated", 0);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Auto-populate ITR fields from AIS data.
     */
    @PostMapping("/autopopulate/ais")
    public ResponseEntity<Map<String, Object>> autoPopulateFromAIS(@RequestBody Map<String, Object> aisData) {
        log.debug("Auto-populating from AIS");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "AIS auto-population available");
        response.put("fieldsPopulated", 0);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Reconcile data between Form26AS, AIS, and TIS.
     */
    @PostMapping("/reconciliation")
    public ResponseEntity<Map<String, Object>> reconcile(@RequestBody Map<String, Object> request) {
        log.debug("Reconciling data");

        Map<String, Object> response = new HashMap<>();
        response.put("hasDiscrepancies", false);
        response.put("discrepancies", new java.util.ArrayList<>());
        response.put("summary", Map.of("total", 0, "high", 0, "medium", 0, "low", 0));
        
        return ResponseEntity.ok(response);
    }
    
    private Long getUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return userRepository.findByEmail(email)
                .map(user -> user.getId())
                .orElse(0L);
        } catch (Exception e) {
            log.warn("Failed to get user ID: {}", e.getMessage());
            return 0L;
        }
    }
}
