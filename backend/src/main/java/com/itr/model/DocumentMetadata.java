package com.itr.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Document metadata entity for proof document management.
 * Supports all ITR-related documents with encryption and versioning.
 */
@Entity
@Table(name = "document_metadata")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentMetadata {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "client_id", nullable = false)
    private Long clientId;
    
    @Column(name = "assessment_year", nullable = false)
    private String assessmentYear;
    
    // Document classification
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private DocumentCategory category;
    
    @Column(name = "sub_category")
    private String subCategory;
    
    // File details
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;
    
    @Column(name = "stored_file_name", nullable = false)
    private String storedFileName;
    
    @Column(name = "file_extension")
    private String fileExtension;
    
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    
    @Column(name = "mime_type")
    private String mimeType;
    
    // Storage details
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_location", nullable = false)
    private StorageLocation storageLocation;
    
    @Column(name = "storage_path", nullable = false)
    private String storagePath;
    
    @Column(name = "s3_bucket")
    private String s3Bucket;
    
    @Column(name = "s3_region")
    private String s3Region;
    
    // Encryption details
    @Column(name = "encrypted")
    private boolean encrypted;
    
    @Column(name = "encryption_algorithm")
    private String encryptionAlgorithm;
    
    @Column(name = "encryption_key_id")
    private String encryptionKeyId;
    
    @Column(name = "initialization_vector")
    private String initializationVector;
    
    // Checksum for integrity
    @Column(name = "sha256_checksum")
    private String sha256Checksum;
    
    @Column(name = "md5_checksum")
    private String md5Checksum;
    
    // Parsed data reference
    @Column(name = "parsed")
    private boolean parsed;
    
    @Column(name = "parsed_data_json", columnDefinition = "TEXT")
    private String parsedDataJson;
    
    @Column(name = "parsing_confidence")
    private Double parsingConfidence;
    
    // Metadata
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @Column(name = "uploaded_by")
    private String uploadedBy;
    
    @Column(name = "version")
    private Integer version;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;
    
    @Column(name = "status_message")
    private String statusMessage;
    
    // Audit trail
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    // Retention policy
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "archived")
    private boolean archived;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = DocumentStatus.UPLOADED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
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
