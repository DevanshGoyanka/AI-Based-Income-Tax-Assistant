package com.itr.controller;

import com.itr.domain.common.PAN;
import com.itr.dto.PrefillResponse;
import com.itr.entity.ITRFormData;
import com.itr.repository.ClientYearDataRepository;
import com.itr.repository.ITRFormDataRepository;
import com.itr.service.ClientService;
import com.itr.service.PrefillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * REST controller for PAN validation and analysis.
 * Provides PAN format validation and eligibility determination for ITR forms.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pan")
@RequiredArgsConstructor
public class PANController {

    private static final java.util.regex.Pattern PAN_PATTERN = 
        java.util.regex.Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    /**
     * Validate PAN format.
     *
     * @param pan the PAN to validate
     * @return validation result with PAN, validity status, and format check
     */
    @GetMapping("/{pan}/validate")
    public ResponseEntity<Map<String, Object>> validatePan(@PathVariable String pan) {
        log.debug("Validating PAN: {}", pan);

        boolean valid = PAN_PATTERN.matcher(pan).matches();

        Map<String, Object> response = new HashMap<>();
        response.put("pan", pan);
        response.put("valid", valid);

        if (!valid) {
            response.put("error", "Invalid PAN format. Must be like ABCDE1234F");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Analyze PAN to determine entity type and eligible ITR forms.
     *
     * @param pan the PAN to analyze
     * @return analysis result with entity type, eligible ITR forms, and warnings
     */
    @GetMapping("/{pan}/analyze")
    public ResponseEntity<Map<String, Object>> analyzePan(@PathVariable String pan) {
        log.debug("Analyzing PAN: {}", pan);

        Map<String, Object> response = new HashMap<>();
        response.put("pan", pan);

        // Validate PAN format first
        if (!PAN_PATTERN.matcher(pan).matches()) {
            response.put("valid", false);
            response.put("error", "Invalid PAN format");
            return ResponseEntity.ok(response);
        }

        response.put("valid", true);

        // Determine entity type from PAN structure
        String entityType;
        switch (pan.charAt(3)) {
            case 'P': entityType = "INDIVIDUAL"; break;
            case 'H': entityType = "HUF"; break;
            case 'F': entityType = "FIRM"; break;
            case 'C': entityType = "COMPANY"; break;
            case 'A': entityType = "AOP"; break;
            case 'T': entityType = "TRUST"; break;
            case 'B': entityType = "BOI"; break;
            case 'J': entityType = "ARTIFICIAL_JURIDICAL_PERSON"; break;
            case 'L': entityType = "LOCAL_AUTHORITY"; break;
            default: entityType = "UNKNOWN"; break;
        }
        response.put("entityType", entityType);

        // Eligible ITR forms based on entity type
        List<String> eligibleForms = new ArrayList<>();
        switch (entityType) {
            case "INDIVIDUAL":
                eligibleForms.add("ITR-1");
                eligibleForms.add("ITR-2");
                eligibleForms.add("ITR-3");
                eligibleForms.add("ITR-4");
                break;
            case "HUF":
            case "FIRM":
                eligibleForms.add("ITR-5");
                break;
            case "COMPANY":
                eligibleForms.add("ITR-6");
                break;
            default:
                eligibleForms.add("ITR-7");
        }
        response.put("eligibleITRForms", eligibleForms);

        // Warnings
        List<String> warnings = new ArrayList<>();
        response.put("warnings", warnings);

        return ResponseEntity.ok(response);
    }

    /**
     * Extract user ID from SecurityContextHolder.
     */
    private Long getUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return 0L;
    }
}

