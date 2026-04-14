package com.itr.repository;

import com.itr.model.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository for document metadata persistence.
 */
@Repository
public interface DocumentRepository extends JpaRepository<DocumentMetadata, Long> {
    
    List<DocumentMetadata> findByClientIdAndAssessmentYear(Long clientId, String assessmentYear);
    
    List<DocumentMetadata> findByClientIdAndDocumentType(Long clientId, DocumentMetadata.DocumentType documentType);
    
    Optional<DocumentMetadata> findByClientIdAndAssessmentYearAndDocumentType(
            Long clientId, String assessmentYear, DocumentMetadata.DocumentType documentType);
    
    @Query("SELECT d FROM DocumentMetadata d WHERE d.clientId = ?1 AND d.status = ?2")
    List<DocumentMetadata> findByClientIdAndStatus(Long clientId, DocumentMetadata.DocumentStatus status);
    
    @Query("SELECT d FROM DocumentMetadata d WHERE d.clientId = ?1 AND d.assessmentYear = ?2 AND d.parsed = true")
    List<DocumentMetadata> findParsedDocuments(Long clientId, String assessmentYear);
    
    long countByClientIdAndAssessmentYear(Long clientId, String assessmentYear);
}
