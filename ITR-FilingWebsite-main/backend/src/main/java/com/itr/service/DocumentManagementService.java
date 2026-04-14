package com.itr.service;

import com.itr.model.DocumentMetadata;
import com.itr.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Document management service - orchestrates storage, encryption, and metadata.
 */
@Service
public class DocumentManagementService {
    
    @Autowired
    private DocumentStorageService storageService;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    /**
     * Upload and store document with encryption.
     */
    @Transactional
    public DocumentMetadata uploadDocument(MultipartFile file, Long clientId, String assessmentYear,
                                          DocumentMetadata.DocumentType documentType) throws Exception {
        
        // Validate file
        validateFile(file);
        
        // Check for duplicate
        checkDuplicate(clientId, assessmentYear, documentType);
        
        // Store document with encryption
        DocumentMetadata metadata = storageService.storeDocument(file, clientId, assessmentYear, documentType);
        
        // Save metadata to database
        return documentRepository.save(metadata);
    }
    
    /**
     * Retrieve document by ID.
     */
    public byte[] downloadDocument(Long documentId) throws Exception {
        DocumentMetadata metadata = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        // Update last accessed time
        metadata.setLastAccessedAt(LocalDateTime.now());
        documentRepository.save(metadata);
        
        return storageService.retrieveDocument(metadata);
    }
    
    /**
     * Get all documents for a client and assessment year.
     */
    public List<DocumentMetadata> getDocuments(Long clientId, String assessmentYear) {
        return documentRepository.findByClientIdAndAssessmentYear(clientId, assessmentYear);
    }
    
    /**
     * Get document by type.
     */
    public DocumentMetadata getDocumentByType(Long clientId, String assessmentYear, 
                                              DocumentMetadata.DocumentType documentType) {
        return documentRepository.findByClientIdAndAssessmentYearAndDocumentType(
                clientId, assessmentYear, documentType).orElse(null);
    }
    
    /**
     * Delete document.
     */
    @Transactional
    public void deleteDocument(Long documentId) throws Exception {
        DocumentMetadata metadata = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        // Delete from storage
        storageService.deleteDocument(metadata);
        
        // Delete metadata
        documentRepository.delete(metadata);
    }
    
    /**
     * Update document status.
     */
    @Transactional
    public void updateDocumentStatus(Long documentId, DocumentMetadata.DocumentStatus status, 
                                     String statusMessage) {
        DocumentMetadata metadata = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        metadata.setStatus(status);
        metadata.setStatusMessage(statusMessage);
        metadata.setUpdatedAt(LocalDateTime.now());
        
        documentRepository.save(metadata);
    }
    
    /**
     * Mark document as parsed with extracted data.
     */
    @Transactional
    public void markAsParsed(Long documentId, String parsedDataJson, double confidence) {
        DocumentMetadata metadata = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        metadata.setParsed(true);
        metadata.setParsedDataJson(parsedDataJson);
        metadata.setParsingConfidence(confidence);
        metadata.setStatus(DocumentMetadata.DocumentStatus.PARSED);
        metadata.setUpdatedAt(LocalDateTime.now());
        
        documentRepository.save(metadata);
    }
    
    // ========== Validation ==========
    
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // Check file size (max 10MB)
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
        
        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("Invalid file type: " + contentType);
        }
    }
    
    private boolean isAllowedContentType(String contentType) {
        return contentType.equals("application/pdf") ||
               contentType.equals("image/jpeg") ||
               contentType.equals("image/png") ||
               contentType.equals("application/vnd.ms-excel") ||
               contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }
    
    private void checkDuplicate(Long clientId, String assessmentYear, 
                                DocumentMetadata.DocumentType documentType) {
        documentRepository.findByClientIdAndAssessmentYearAndDocumentType(
                clientId, assessmentYear, documentType).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "Document of type " + documentType + " already exists for this assessment year");
        });
    }
}
