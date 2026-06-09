package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Document metadata for proof document management.
 * Supports all ITR-related documents with encryption and versioning.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentMetadata {
    
    private Long id;
    private Long clientId;
    private String assessmentYear;
    
    // Document classification
    private DocumentType documentType;
    private DocumentCategory category;
    private String subCategory;
    
    // File details
    private String originalFileName;
    private String storedFileName;  // UUID-based encrypted filename
    private String fileExtension;
    private Long fileSizeBytes;
    private String mimeType;
    
    // Storage details
    private StorageLocation storageLocation;
    private String storagePath;  // S3 key or file system path
    private String s3Bucket;
    private String s3Region;
    
    // Encryption details
    private boolean encrypted;
    private String encryptionAlgorithm;  // AES-256-GCM
    private String encryptionKeyId;  // Reference to key in key management system
    private String initializationVector;  // IV for AES-GCM
    
    // Checksum for integrity
    private String sha256Checksum;
    private String md5Checksum;
    
    // Parsed data reference
    private boolean parsed;
    private String parsedDataJson;  // JSON of extracted data
    private Double parsingConfidence;  // 0.0 to 1.0
    
    // Metadata
    private LocalDateTime uploadedAt;
    private LocalDateTime lastAccessedAt;
    private String uploadedBy;
    private Integer version;
    
    // Status
    private DocumentStatus status;
    private String statusMessage;
    
    // Audit trail
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Retention policy
    private LocalDateTime expiresAt;
    private boolean archived;
    
    public enum DocumentType {
        FORM_16,
        FORM_16A,
        FORM_26AS,
        AIS,
        BANK_STATEMENT,
        RENT_RECEIPT,
        LOAN_CERTIFICATE,
        INSURANCE_PREMIUM_RECEIPT,
        TUITION_FEE_RECEIPT,
        DONATION_RECEIPT,
        MEDICAL_BILL,
        INVESTMENT_PROOF,
        PROPERTY_DOCUMENT,
        DEMAT_STATEMENT,
        CAPITAL_GAINS_STATEMENT,
        BUSINESS_BOOKS,
        AUDIT_REPORT,
        BALANCE_SHEET,
        PL_STATEMENT,
        OTHER
    }
    
    public enum DocumentCategory {
        INCOME_PROOF,
        TDS_CERTIFICATE,
        DEDUCTION_PROOF,
        TAX_PAYMENT_PROOF,
        PROPERTY_PROOF,
        BUSINESS_PROOF,
        GOVERNMENT_FORM,
        OTHER
    }
    
    public enum StorageLocation {
        LOCAL_FILE_SYSTEM,
        AWS_S3,
        AZURE_BLOB,
        GCP_STORAGE
    }
    
    public enum DocumentStatus {
        UPLOADED,
        PROCESSING,
        PARSED,
        VERIFIED,
        REJECTED,
        ARCHIVED,
        DELETED
    }
}
