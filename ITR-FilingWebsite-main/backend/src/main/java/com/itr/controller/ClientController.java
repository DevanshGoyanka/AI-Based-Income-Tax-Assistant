package com.itr.controller;

import com.itr.dto.ClientRequest;
import com.itr.dto.ClientResponse;
import com.itr.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final com.itr.service.AutoITRFormSelector autoITRFormSelector;
    private final com.itr.service.PANTypeDetectionService panService;

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getClients(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(clientService.getClientsByUser(userId));
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody ClientRequest request,
                                                        Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ClientResponse response = clientService.createClient(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable Long clientId,
                                                     Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(clientService.getClientById(clientId, userId));
    }

    @GetMapping("/{clientId}/years")
    public ResponseEntity<List<ClientResponse.YearStatus>> getClientYears(@PathVariable Long clientId,
                                                                           Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(clientService.getClientYears(clientId, userId));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<ClientResponse> updateClient(@PathVariable Long clientId,
                                                        @Valid @RequestBody ClientRequest request,
                                                        Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ClientResponse response = clientService.updateClient(clientId, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long clientId,
                                              Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        clientService.deleteClient(clientId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{clientId}/years/{year}/itr-type")
    public ResponseEntity<Void> updateItrType(@PathVariable Long clientId,
                                               @PathVariable String year,
                                               @RequestBody UpdateItrTypeRequest request,
                                               Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        clientService.updateItrType(clientId, year, request.getItrType(), userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * NEW: Get ITR classification for a client based on income profile.
     * POST /api/clients/{clientId}/itr-classification
     */
    @PostMapping("/{clientId}/itr-classification")
    public ResponseEntity<com.itr.dto.ITRClassificationResult> getITRClassification(
            @PathVariable Long clientId,
            @RequestBody com.itr.dto.IncomeProfile incomeProfile,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        
        // Verify client belongs to user
        clientService.getClientById(clientId, userId);
        
        // Classify ITR form based on income profile
        com.itr.dto.ITRClassificationResult result = autoITRFormSelector.classifyITRForm(incomeProfile);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * NEW: Get PAN analysis for a client.
     * GET /api/clients/{clientId}/pan-analysis
     */
    @GetMapping("/{clientId}/pan-analysis")
    public ResponseEntity<com.itr.service.PANTypeDetectionService.PANAnalysis> getPANAnalysis(
            @PathVariable Long clientId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        
        // Get client
        ClientResponse client = clientService.getClientById(clientId, userId);
        
        // Analyze PAN
        com.itr.service.PANTypeDetectionService.PANAnalysis analysis = 
                panService.analyzePAN(client.getPan());
        
        return ResponseEntity.ok(analysis);
    }

    public static class UpdateItrTypeRequest {
        private String itrType;
        
        public String getItrType() {
            return itrType;
        }
        
        public void setItrType(String itrType) {
            this.itrType = itrType;
        }
    }
}
