package com.itr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr.domain.computation.ComputedReturn;
import com.itr.entity.Client;
import com.itr.entity.ITRFiling;
import com.itr.repository.ClientRepository;
import com.itr.repository.ITRFilingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * FilingSnapshotService - manages immutable filing snapshots.
 * Document 1 §8 - append-only, chained via AuditLogHashChainService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FilingSnapshotService {
    
    private final ITRFilingRepository filingRepository;
    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public ITRFiling recordSnapshot(ComputedReturn computedReturn) {
        try {
            String json = objectMapper.writeValueAsString(computedReturn);
            
            Client client = clientRepository.findById(Long.parseLong(computedReturn.getClientId()))
                .orElseThrow(() -> new RuntimeException("Client not found"));
            
            ITRFiling snapshot = new ITRFiling();
            snapshot.setClient(client);
            snapshot.setAssessmentYear(computedReturn.getAssessmentYear());
            snapshot.setFilingDate(LocalDate.now());
            snapshot.setStatus("DRAFT");
            
            // Serialize full ComputedReturn
            snapshot.setComputedReturnJson(json);
            snapshot.setRulesVersion(computedReturn.getRulesVersion());
            snapshot.setItrFormVersion(computedReturn.getItrFormVersion());
            snapshot.setJsonSchemaVersion(computedReturn.getJsonSchemaVersion());
            snapshot.setTriggerAction(computedReturn.getTriggeredBy());
            
            // Hash chain
            String hash = computeHash(json);
            snapshot.setComputationHash(hash);
            
            ITRFiling saved = filingRepository.save(snapshot);
            log.info("Recorded snapshot id={} for client={}, AY={}, trigger={}", 
                saved.getId(), computedReturn.getClientId(), 
                computedReturn.getAssessmentYear(), computedReturn.getTriggeredBy());
            
            return saved;
        } catch (Exception e) {
            log.error("Failed to record snapshot", e);
            throw new RuntimeException("Snapshot recording failed", e);
        }
    }
    
    private String computeHash(String content) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hash computation failed", e);
        }
    }
}
