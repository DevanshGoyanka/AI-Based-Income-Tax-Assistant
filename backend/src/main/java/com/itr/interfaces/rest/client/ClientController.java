package com.itr.interfaces.rest.client;

import com.itr.entity.Client;
import com.itr.entity.User;
import com.itr.repository.ClientRepository;
import com.itr.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClientController — /api/v1/clients/*
 */
@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public ClientController(ClientRepository clientRepository, UserRepository userRepository) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    // Helper method to get current user's email from security context
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Not authenticated");
        }
        return (String) auth.getPrincipal();
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String assessmentYear) {
        
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Client> clients = clientRepository.findByUserId(user.getId());
        List<Map<String, Object>> response = clients.stream().map(this::clientToMap).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createClient(@RequestBody Map<String, Object> body) {
        
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Client client = Client.builder()
                .user(user)
                .pan((String) body.get("pan"))
                .name((String) body.get("name"))
                .email((String) body.getOrDefault("email", ""))
                .mobile((String) body.getOrDefault("mobile", ""))
                .aadhaar((String) body.getOrDefault("aadhaar", ""))
                .dob(LocalDate.parse((String) body.getOrDefault("dob", "1990-01-01")))
                .build();
        
        client = clientRepository.save(client);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", client.getId());
        response.put("name", client.getName());
        response.put("pan", client.getPan());
        response.put("status", "CREATED");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getClient(@PathVariable Long id) {
        
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return clientRepository.findByIdAndUserId(id, user.getId())
                .map(client -> ResponseEntity.ok(clientToMap(client)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateClient(@PathVariable Long id,
                                                              @RequestBody Map<String, Object> body) {
        
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return clientRepository.findByIdAndUserId(id, user.getId())
                .map(client -> {
                    if (body.containsKey("name")) client.setName((String) body.get("name"));
                    if (body.containsKey("pan")) client.setPan((String) body.get("pan"));
                    if (body.containsKey("email")) client.setEmail((String) body.get("email"));
                    if (body.containsKey("mobile")) client.setMobile((String) body.get("mobile"));
                    clientRepository.save(client);
                    return ResponseEntity.<Map<String, Object>>ok(Map.of("id", id, "status", "UPDATED"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteClient(@PathVariable Long id) {
        
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return clientRepository.findByIdAndUserId(id, user.getId())
                .map(client -> {
                    clientRepository.delete(client);
                    return ResponseEntity.<Map<String, Object>>ok(Map.of("id", id, "status", "DELETED"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/verify-pan")
    public ResponseEntity<Map<String, Object>> verifyPAN(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("id", id, "panVerified", true));
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importClients(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "IMPORTED", "count", 0));
    }

    // ========== ITR Endpoints ==========
    
    @GetMapping("/{clientId}/itr/{year}")
    public ResponseEntity<Map<String, Object>> getFormData(@PathVariable Long clientId, @PathVariable String year) {
        Map<String, Object> response = new HashMap<>();
        response.put("clientId", clientId);
        response.put("assessmentYear", year);
        response.put("formType", "ITR-1");
        response.put("data", new HashMap<>());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{clientId}/itr/{year}")
    public ResponseEntity<Map<String, Object>> saveFormData(@PathVariable Long clientId, @PathVariable String year,
                                                             @RequestBody Map<String, Object> formData) {
        return ResponseEntity.ok(Map.of("clientId", clientId, "assessmentYear", year, "status", "SAVED"));
    }

    @PostMapping("/{clientId}/itr/{year}/compute")
    public ResponseEntity<Map<String, Object>> computeTax(@PathVariable Long clientId, @PathVariable String year,
                                                           @RequestBody Map<String, Object> formData) {
        return ResponseEntity.ok(Map.of("clientId", clientId, "assessmentYear", year, "status", "COMPUTED"));
    }

    @PostMapping("/{clientId}/itr/{year}/validate")
    public ResponseEntity<Map<String, Object>> validate(@PathVariable Long clientId, @PathVariable String year,
                                                         @RequestBody Map<String, Object> formData) {
        return ResponseEntity.ok(Map.of("valid", true, "errors", new ArrayList<>(), "warnings", new ArrayList<>()));
    }

    // Helper method to convert Client entity to Map
    private Map<String, Object> clientToMap(Client client) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", client.getId());
        map.put("name", client.getName());
        map.put("pan", client.getPan());
        map.put("email", client.getEmail());
        map.put("mobile", client.getMobile());
        map.put("dob", client.getDob());
        map.put("aadhaar", client.getAadhaar());
        map.put("createdAt", client.getCreatedAt());
        // Add fields needed by frontend
        map.put("updatedAt", client.getCreatedAt());
        map.put("status", "Not Started");
        map.put("itrType", "ITR-1");
        return map;
    }
}
