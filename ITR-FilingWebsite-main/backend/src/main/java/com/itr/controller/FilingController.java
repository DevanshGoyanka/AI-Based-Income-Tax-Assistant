package com.itr.controller;

import com.itr.dto.FilingRequest;
import com.itr.dto.FilingResponse;
import com.itr.service.ITRFilingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/filing")
@RequiredArgsConstructor
public class FilingController {

    private final ITRFilingService filingService;

    @GetMapping
    public ResponseEntity<List<FilingResponse>> getFilings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String year,
            @AuthenticationPrincipal Long userId) {
        
        log.info("Getting filings for user {} with status={} year={}", userId, status, year);
        List<FilingResponse> filings = filingService.getAllFilings(userId, status, year);
        return ResponseEntity.ok(filings);
    }

    @PostMapping
    public ResponseEntity<FilingResponse> createFiling(
            @RequestBody FilingRequest request,
            @AuthenticationPrincipal Long userId) {
        
        log.info("Creating filing for client {} year {}", request.getClientId(), request.getAssessmentYear());
        FilingResponse filing = filingService.createFiling(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(filing);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FilingResponse> updateFiling(
            @PathVariable Long id,
            @RequestBody FilingRequest request,
            @AuthenticationPrincipal Long userId) {
        
        log.info("Updating filing {}", id);
        FilingResponse filing = filingService.updateFiling(id, request, userId);
        return ResponseEntity.ok(filing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFiling(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        
        log.info("Deleting filing {}", id);
        filingService.deleteFiling(id, userId);
        return ResponseEntity.noContent().build();
    }
}
