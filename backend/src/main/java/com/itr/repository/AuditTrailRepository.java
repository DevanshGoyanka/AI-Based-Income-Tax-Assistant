package com.itr.repository;

import com.itr.entity.AuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Audit Trail Repository - Section 7 ITR_ERP_FINAL_COMPLETION_DIRECTIVE.md
 */
@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {
    
    List<AuditTrail> findByTaxpayerPANAndAssessmentYear(String pan, String ay);
    
    List<AuditTrail> findByEventTypeOrderByCreatedAtDesc(String eventType);
    
    AuditTrail findTopByTaxpayerPANOrderByCreatedAtDesc(String pan);
    
    List<AuditTrail> findByTaxpayerPANOrderByCreatedAtAsc(String pan);
}
