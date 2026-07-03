package com.itr.controller;

import com.itr.model.DocumentMetadata;
import com.itr.service.DocumentManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for document management operations.
 * Handles document upload, download, listing, and deletion.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentManagementService documentService;

    /**
     * Upload a document.
     *
     * @param file the file to upload
     * @param clientId the client ID
     * @param assessmentYear the assessment year
     * @param documentType the document type (e.g., FORM_16, FORM_26AS, AIS)
     * @return document metadata
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("clientId") Long clientId,
            @RequestParam("assessmentYear") String assessmentYear,
            @RequestParam("documentType") String documentType) {
        
        Long userId = getUserId();
        log.debug("Uploading document for client {}, AY {}, type: {}", clientId, assessmentYear, documentType);

        try {
            DocumentMetadata.DocumentType docType = DocumentMetadata.DocumentType.valueOf(documentType.toUpperCase());
            DocumentMetadata metadata = documentService.uploadDocument(file, clientId, assessmentYear, docType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", metadata.getId());
            response.put("clientId", metadata.getClientId());
            response.put("assessmentYear", metadata.getAssessmentYear());
            response.put("documentType", metadata.getDocumentType());
            response.put("originalFileName", metadata.getOriginalFileName());
            response.put("fileSizeBytes", metadata.getFileSizeBytes());
            response.put("uploadedAt", metadata.getUploadedAt());
            response.put("status", metadata.getStatus());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid document type: {}", documentType, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid document type: " + documentType);
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Failed to upload document", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to upload document: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * List documents for a client and assessment year.
     */
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> listDocuments(
            @RequestParam Long clientId,
            @RequestParam String assessmentYear) {
        
        log.debug("Listing documents for client {}, AY: {}", clientId, assessmentYear);
        
        List<DocumentMetadata> documents = documentService.getDocuments(clientId, assessmentYear);
        
        List<Map<String, Object>> response = documents.stream()
                .map(doc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", doc.getId());
                    map.put("clientId", doc.getClientId());
                    map.put("assessmentYear", doc.getAssessmentYear());
                    map.put("documentType", doc.getDocumentType());
                    map.put("originalFileName", doc.getOriginalFileName());
                    map.put("fileSizeBytes", doc.getFileSizeBytes());
                    map.put("uploadedAt", doc.getUploadedAt());
                    map.put("status", doc.getStatus());
                    map.put("parsed", doc.isParsed());
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get document metadata by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDocument(@PathVariable Long id) {
        log.debug("Getting document metadata for id: {}", id);
        
        // This would need a findById method in DocumentRepository
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Document retrieval by ID not fully implemented");
        return ResponseEntity.ok(error);
    }

    /**
     * Download a document.
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        log.debug("Downloading document id: {}", id);
        
        try {
            byte[] fileData = documentService.downloadDocument(id);
            
            ByteArrayResource resource = new ByteArrayResource(fileData);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(fileData.length)
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to download document {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Delete a document.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable Long id) {
        log.debug("Deleting document id: {}", id);
        
        try {
            documentService.deleteDocument(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete document {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to delete document: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Extract user ID from SecurityContextHolder.
     */
    private Long getUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return 0L;
    }
}

