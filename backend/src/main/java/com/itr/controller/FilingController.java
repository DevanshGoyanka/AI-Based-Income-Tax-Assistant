package com.itr.controller;

import com.itr.dto.FilingRequest;
import com.itr.dto.FilingResponse;
import com.itr.service.ITRFilingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for ITR filing operations.
 * Manages creation, retrieval, update, and deletion of ITR filings.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/filing")
@RequiredArgsConstructor
public class FilingController {

    private final ITRFilingService filingService;

    /**
     * Get all filings for the authenticated user.
     *
     * @param status optional status filter (e.g., "filed", "in_progress", "draft")
     * @param year optional assessment year filter (e.g., "2026-27")
     * @return list of filing responses
     */
    @GetMapping
    public ResponseEntity<List<FilingResponse>> getFilings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String year) {
        
        Long userId = getUserId();
        log.debug("Getting filings for user: {}, status: {}, year: {}", userId, status, year);
        
        List<FilingResponse> filings = filingService.getAllFilings(userId, status, year);
        return ResponseEntity.ok(filings);
    }

    /**
     * Create a new filing.
     */
    @PostMapping
    public ResponseEntity<FilingResponse> createFiling(@Valid @RequestBody FilingRequest request) {
        Long userId = getUserId();
        log.debug("Creating filing for client: {} and AY: {}", request.getClientId(), request.getAssessmentYear());
        
        FilingResponse filing = filingService.createFiling(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(filing);
    }

    /**
     * Update an existing filing.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FilingResponse> updateFiling(@PathVariable Long id,
                                                         @Valid @RequestBody FilingRequest request) {
        Long userId = getUserId();
        log.debug("Updating filing {} for user: {}", id, userId);
        
        FilingResponse filing = filingService.updateFiling(id, request, userId);
        return ResponseEntity.ok(filing);
    }

    /**
     * Delete a filing.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFiling(@PathVariable Long id) {
        Long userId = getUserId();
        log.debug("Deleting filing {} for user: {}", id, userId);
        
        filingService.deleteFiling(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extract user ID from SecurityContextHolder.
     */
    private Long getUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return 0L;
    }
}

