package com.itr.service;

import com.itr.entity.AuditTrail;
import com.itr.repository.AuditTrailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Audit Log Hash Chaining Service
 * Implements tamper detection using cryptographic hash chaining
 * Each audit entry contains hash of previous entry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogHashChainService {

    private final AuditTrailRepository auditTrailRepository;

    /**
     * Create audit entry with hash chain
     */
    public AuditTrail createAuditEntry(String pan, String assessmentYear, 
                                       String action, String details, String source) {
        AuditTrail entry = new AuditTrail();
        entry.setTaxpayerPAN(pan);
        entry.setAssessmentYear(assessmentYear);
        entry.setEventType(action);
        entry.setEventDescription(details);
        entry.setDataSource(source);
        entry.setCreatedAt(LocalDateTime.now());

        // Get previous entry hash
        AuditTrail previousEntry = auditTrailRepository.findTopByTaxpayerPANOrderByCreatedAtDesc(pan);
        String previousHash = previousEntry != null ? previousEntry.getEntryHash() : "GENESIS";
        entry.setPreviousHash(previousHash);

        // Compute current entry hash
        String currentHash = computeHash(entry);
        entry.setEntryHash(currentHash);

        auditTrailRepository.save(entry);
        log.debug("Audit entry created with hash: {}", currentHash.substring(0, 8));

        return entry;
    }

    /**
     * Verify audit trail integrity for a PAN
     */
    public boolean verifyIntegrity(String pan) {
        var entries = auditTrailRepository.findByTaxpayerPANOrderByCreatedAtAsc(pan);
        
        if (entries.isEmpty()) {
            return true;
        }

        String expectedPreviousHash = "GENESIS";
        
        for (AuditTrail entry : entries) {
            // Verify previous hash matches
            if (!expectedPreviousHash.equals(entry.getPreviousHash())) {
                log.error("Hash chain broken at entry ID: {} - expected previous hash: {}, found: {}",
                         entry.getId(), expectedPreviousHash, entry.getPreviousHash());
                return false;
            }

            // Verify current hash is correct
            String computedHash = computeHash(entry);
            if (!computedHash.equals(entry.getEntryHash())) {
                log.error("Entry hash mismatch at ID: {} - computed: {}, stored: {}",
                         entry.getId(), computedHash, entry.getEntryHash());
                return false;
            }

            expectedPreviousHash = entry.getEntryHash();
        }

        log.info("Audit trail integrity verified for PAN: {} ({} entries)", pan, entries.size());
        return true;
    }

    /**
     * Compute SHA-256 hash of audit entry
     */
    private String computeHash(AuditTrail entry) {
        try {
            String data = entry.getTaxpayerPAN() + "|" +
                         entry.getAssessmentYear() + "|" +
                         entry.getEventType() + "|" +
                         entry.getEventDescription() + "|" +
                         entry.getDataSource() + "|" +
                         entry.getCreatedAt() + "|" +
                         entry.getPreviousHash();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new RuntimeException("Hash computation failed", e);
        }
    }
}
