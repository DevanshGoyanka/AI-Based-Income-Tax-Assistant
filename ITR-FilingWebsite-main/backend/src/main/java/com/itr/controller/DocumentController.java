package com.itr.controller;

import com.itr.model.DocumentMetadata;
import com.itr.service.DocumentManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST API for document management operations.
 */
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {
    
    @Autowired
    private DocumentManagementService documentService;
    
    /**
     * Upload a document.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("clientId") Long clientId,
            @RequestParam("assessmentYear") String assessmentYear,
            @RequestParam("documentType") DocumentMetadata.DocumentType documentType) {
        
        try {
            DocumentMetadata metadata = documentService.uploadDocument(file, clientId, assessmentYear, documentType);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading document: " + e.getMessage());
        }
    }
    
    /**
     * Download a document.
     */
    @GetMapping("/download/{documentId}")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long documentId) {
        try {
            byte[] data = documentService.downloadDocument(documentId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "document.pdf");
            
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all documents for a client and assessment year.
     */
    @GetMapping("/list")
    public ResponseEntity<List<DocumentMetadata>> getDocuments(
            @RequestParam("clientId") Long clientId,
            @RequestParam("assessmentYear") String assessmentYear) {
        
        List<DocumentMetadata> documents = documentService.getDocuments(clientId, assessmentYear);
        return ResponseEntity.ok(documents);
    }
    
    /**
     * Get document by type.
     */
    @GetMapping("/by-type")
    public ResponseEntity<DocumentMetadata> getDocumentByType(
            @RequestParam("clientId") Long clientId,
            @RequestParam("assessmentYear") String assessmentYear,
            @RequestParam("documentType") DocumentMetadata.DocumentType documentType) {
        
        DocumentMetadata document = documentService.getDocumentByType(clientId, assessmentYear, documentType);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(document);
    }
    
    /**
     * Delete a document.
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long documentId) {
        try {
            documentService.deleteDocument(documentId);
            return ResponseEntity.ok("Document deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting document: " + e.getMessage());
        }
    }
}
