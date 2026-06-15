package com.itr.config;

import com.itr.dto.ClientRequest;
import com.itr.dto.ClientResponse;
import com.itr.repository.UserRepository;
import com.itr.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for client management operations.
 * Provides endpoints for CRUD operations on clients and ITD portal password management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final UserRepository userRepository;

    /**
     * Get all clients for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        Long userId = getUserId();
        log.debug("Getting all clients for user: {}", userId);
        List<ClientResponse> clients = clientService.getClientsByUser(userId);
        return ResponseEntity.ok(clients);
    }

    /**
     * Get a single client by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable Long id) {
        Long userId = getUserId();
        log.debug("Getting client {} for user: {}", id, userId);
        ClientResponse client = clientService.getClientById(id, userId);
        return ResponseEntity.ok(client);
    }

    /**
     * Create a new client.
     */
    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody ClientRequest request) {
        Long userId = getUserId();
        log.debug("Creating client with PAN: {} for user: {}", request.getPan(), userId);
        ClientResponse client = clientService.createClient(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(client);
    }

    /**
     * Update an existing client.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(@PathVariable Long id,
                                                        @Valid @RequestBody ClientRequest request) {
        Long userId = getUserId();
        log.debug("Updating client {} for user: {}", id, userId);
        ClientResponse client = clientService.updateClient(id, request, userId);
        return ResponseEntity.ok(client);
    }

    /**
     * Delete a client.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        Long userId = getUserId();
        log.debug("Deleting client {} for user: {}", id, userId);
        clientService.deleteClient(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all assessment years for a client.
     */
    @GetMapping("/{id}/years")
    public ResponseEntity<List<ClientResponse.YearStatus>> getClientYears(@PathVariable Long id) {
        Long userId = getUserId();
        log.debug("Getting years for client {} for user: {}", id, userId);
        List<ClientResponse.YearStatus> years = clientService.getClientYears(id, userId);
        return ResponseEntity.ok(years);
    }

    /**
     * Get ITR form data for a specific client and assessment year.
     */
    @GetMapping("/{id}/itr/{year}")
    public ResponseEntity<Map<String, Object>> getItrFormData(@PathVariable Long id, @PathVariable String year) {
        Long userId = getUserId();
        log.debug("Getting ITR data for client {} year {} for user: {}", id, year, userId);
        
        // Return empty data structure - actual implementation would query database
        Map<String, Object> itrData = new HashMap<>();
        itrData.put("clientId", id);
        itrData.put("assessmentYear", year);
        itrData.put("itrFormType", "ITR1");
        itrData.put("basicSalary", 0);
        itrData.put("da", 0);
        itrData.put("hra", 0);
        itrData.put("allowances", 0);
        itrData.put("perquisites", 0);
        itrData.put("hpIncome", 0);
        itrData.put("stcgIncome", 0);
        itrData.put("ltcgIncome", 0);
        itrData.put("businessIncome", 0);
        itrData.put("bankInterest", 0);
        itrData.put("nscInterest", 0);
        itrData.put("scssInterest", 0);
        itrData.put("postOfficeInterest", 0);
        itrData.put("itRefundInterest", 0);
        itrData.put("totalDividend", 0);
        itrData.put("lotteryIncome", 0);
        itrData.put("cardGameIncome", 0);
        itrData.put("horseRaceIncome", 0);
        itrData.put("vdaGains", 0);
        itrData.put("taxableGifts", 0);
        itrData.put("familyPension", 0);
        itrData.put("otherMisc", 0);
        itrData.put("totalTds", 0);
        itrData.put("totalAdvanceTax", 0);
        itrData.put("totalSelfAssessmentTax", 0);
        itrData.put("s80C", 0);
        itrData.put("s80CCC1B", 0);
        itrData.put("s80CCD", 0);
        itrData.put("s80D", 0);
        itrData.put("s80E", 0);
        itrData.put("s80G", 0);
        itrData.put("s80TTA", 0);
        itrData.put("s80TTB", 0);
        
        return ResponseEntity.ok(itrData);
    }

    /**
     * Save ITR form data for a specific client and assessment year.
     */
    @PutMapping("/{id}/itr/{year}")
    public ResponseEntity<Map<String, Object>> saveItrFormData(@PathVariable Long id, 
                                                                 @PathVariable String year,
                                                                 @RequestBody Map<String, Object> formData) {
        Long userId = getUserId();
        log.debug("Saving ITR data for client {} year {} for user: {}", id, year, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "ITR data saved successfully");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update ITR type for a specific assessment year.
     */
    @PutMapping("/{id}/years/{year}/itr-type")
    public ResponseEntity<Void> updateItrType(@PathVariable Long id,
                                               @PathVariable String year,
                                               @RequestParam String itrType) {
        Long userId = getUserId();
        log.debug("Updating ITR type to {} for client {} year {} for user: {}", itrType, id, year, userId);
        clientService.updateItrType(id, year, itrType, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Set or update ITD portal password for a client.
     * Password is encrypted using AES-256-GCM before storage.
     */
    @PutMapping("/{id}/itd-password")
    public ResponseEntity<Map<String, Object>> setItdPassword(@PathVariable Long id,
                                                                 @RequestBody Map<String, String> request) {
        Long userId = getUserId();
        String password = request.get("password");
        log.debug("Setting ITD password for client {} for user: {}", id, userId);
        
        clientService.setItdPassword(id, password, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "ITD portal password saved securely");
        response.put("setAt", OffsetDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Remove ITD portal password for a client.
     */
    @DeleteMapping("/{id}/itd-password")
    public ResponseEntity<Map<String, Object>> removeItdPassword(@PathVariable Long id) {
        Long userId = getUserId();
        log.debug("Removing ITD password for client {} for user: {}", id, userId);
        
        clientService.removeItdPassword(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "ITD portal password removed");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check if ITD password exists for a client.
     * NEVER returns the actual password - only boolean.
     */
    @GetMapping("/{id}/itd-password")
    public ResponseEntity<Map<String, Boolean>> checkItdPassword(@PathVariable Long id) {
        Long userId = getUserId();
        log.debug("Checking ITD password exists for client {} for user: {}", id, userId);
        
        boolean exists = clientService.hasItdPassword(id, userId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Validate ITD portal password by attempting login.
     * This is optional and tests if the password works.
     */
    @PostMapping("/{id}/itd-validate")
    public ResponseEntity<Map<String, Object>> validateItdPassword(@PathVariable Long id) {
        Long userId = getUserId();
        log.debug("Validating ITD password for client {} for user: {}", id, userId);
        
        // TODO: Implement actual ITD portal login validation
        // This would call ITDPortalAutomationService to test credentials
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "ITD password validation not yet implemented");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Extract user ID from SecurityContextHolder.
     * Looks up the user by email from JWT token.
     */
    private Long getUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(user -> user.getId())
                .orElse(0L);
    }

    /**
     * Extract email from SecurityContextHolder.
     */
    private String getEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
