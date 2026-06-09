package com.itr.service;

import com.itr.model.DocumentMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Document storage service with encryption support.
 * Handles local file system and AWS S3 storage.
 */
@Service
public class DocumentStorageService {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Value("${document.storage.location:LOCAL_FILE_SYSTEM}")
    private String storageLocation;
    
    @Value("${document.storage.base-path:./documents}")
    private String basePath;
    
    @Value("${document.storage.encryption.enabled:true}")
    private boolean encryptionEnabled;
    
    @Value("${document.storage.s3.bucket:}")
    private String s3Bucket;
    
    @Value("${document.storage.s3.region:us-east-1}")
    private String s3Region;
    
    /**
     * Store document with encryption.
     */
    public DocumentMetadata storeDocument(MultipartFile file, Long clientId, String assessmentYear,
                                          DocumentMetadata.DocumentType documentType) throws Exception {
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + fileExtension;
        
        // Read file bytes
        byte[] fileBytes = file.getBytes();
        
        // Calculate checksums
        String sha256 = calculateSHA256(fileBytes);
        String md5 = calculateMD5(fileBytes);
        
        // Encrypt if enabled
        byte[] dataToStore = fileBytes;
        String iv = null;
        String encryptionKeyId = null;
        
        if (encryptionEnabled) {
            SecretKey key = encryptionService.generateKey();
            EncryptionService.EncryptedData encrypted = encryptionService.encrypt(fileBytes, key);
            dataToStore = encrypted.getEncryptedBytes();
            iv = encryptionService.ivToString(encrypted.getIv());
            encryptionKeyId = storeEncryptionKey(key, clientId, storedFilename);
        }
        
        // Store based on storage location
        String storagePath;
        if ("AWS_S3".equals(storageLocation)) {
            storagePath = storeToS3(dataToStore, clientId, assessmentYear, storedFilename);
        } else {
            storagePath = storeToFileSystem(dataToStore, clientId, assessmentYear, storedFilename);
        }
        
        // Build metadata
        return DocumentMetadata.builder()
                .clientId(clientId)
                .assessmentYear(assessmentYear)
                .documentType(documentType)
                .category(getCategoryForType(documentType))
                .originalFileName(originalFilename)
                .storedFileName(storedFilename)
                .fileExtension(fileExtension)
                .fileSizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .storageLocation(DocumentMetadata.StorageLocation.valueOf(storageLocation))
                .storagePath(storagePath)
                .s3Bucket(s3Bucket)
                .s3Region(s3Region)
                .encrypted(encryptionEnabled)
                .encryptionAlgorithm(encryptionEnabled ? "AES-256-GCM" : null)
                .encryptionKeyId(encryptionKeyId)
                .initializationVector(iv)
                .sha256Checksum(sha256)
                .md5Checksum(md5)
                .parsed(false)
                .uploadedAt(LocalDateTime.now())
                .uploadedBy("system")
                .version(1)
                .status(DocumentMetadata.DocumentStatus.UPLOADED)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Retrieve document with decryption.
     */
    public byte[] retrieveDocument(DocumentMetadata metadata) throws Exception {
        byte[] encryptedData;
        
        if (metadata.getStorageLocation() == DocumentMetadata.StorageLocation.AWS_S3) {
            encryptedData = retrieveFromS3(metadata.getStoragePath());
        } else {
            encryptedData = retrieveFromFileSystem(metadata.getStoragePath());
        }
        
        if (metadata.isEncrypted()) {
            SecretKey key = retrieveEncryptionKey(metadata.getEncryptionKeyId());
            byte[] iv = encryptionService.stringToIv(metadata.getInitializationVector());
            return encryptionService.decrypt(encryptedData, key, iv);
        }
        
        return encryptedData;
    }
    
    /**
     * Delete document.
     */
    public void deleteDocument(DocumentMetadata metadata) throws Exception {
        if (metadata.getStorageLocation() == DocumentMetadata.StorageLocation.AWS_S3) {
            deleteFromS3(metadata.getStoragePath());
        } else {
            deleteFromFileSystem(metadata.getStoragePath());
        }
        
        if (metadata.isEncrypted()) {
            deleteEncryptionKey(metadata.getEncryptionKeyId());
        }
    }
    
    // ========== File System Storage ==========
    
    private String storeToFileSystem(byte[] data, Long clientId, String assessmentYear, 
                                     String filename) throws IOException {
        Path directory = Paths.get(basePath, clientId.toString(), assessmentYear);
        Files.createDirectories(directory);
        
        Path filePath = directory.resolve(filename);
        Files.write(filePath, data);
        
        return filePath.toString();
    }
    
    private byte[] retrieveFromFileSystem(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }
    
    private void deleteFromFileSystem(String path) throws IOException {
        Files.deleteIfExists(Paths.get(path));
    }
    
    // ========== S3 Storage (Placeholder) ==========
    
    private String storeToS3(byte[] data, Long clientId, String assessmentYear, 
                            String filename) throws Exception {
        // TODO: Implement AWS S3 upload using AWS SDK
        // String key = String.format("%s/%s/%s", clientId, assessmentYear, filename);
        // s3Client.putObject(s3Bucket, key, new ByteArrayInputStream(data), metadata);
        // return key;
        throw new UnsupportedOperationException("S3 storage not yet implemented");
    }
    
    private byte[] retrieveFromS3(String key) throws Exception {
        // TODO: Implement AWS S3 download
        throw new UnsupportedOperationException("S3 storage not yet implemented");
    }
    
    private void deleteFromS3(String key) throws Exception {
        // TODO: Implement AWS S3 delete
        throw new UnsupportedOperationException("S3 storage not yet implemented");
    }
    
    // ========== Encryption Key Management ==========
    
    private String storeEncryptionKey(SecretKey key, Long clientId, String filename) throws IOException {
        String keyId = UUID.randomUUID().toString();
        String keyString = encryptionService.keyToString(key);
        
        Path keyDirectory = Paths.get(basePath, "keys", clientId.toString());
        Files.createDirectories(keyDirectory);
        
        Path keyFile = keyDirectory.resolve(keyId + ".key");
        Files.write(keyFile, keyString.getBytes());
        
        return keyId;
    }
    
    private SecretKey retrieveEncryptionKey(String keyId) throws IOException {
        // TODO: Implement proper key retrieval from secure key management system
        // For now, reading from file system (NOT PRODUCTION READY)
        throw new UnsupportedOperationException("Key retrieval not yet implemented");
    }
    
    private void deleteEncryptionKey(String keyId) throws IOException {
        // TODO: Implement key deletion
    }
    
    // ========== Utility Methods ==========
    
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
    
    private DocumentMetadata.DocumentCategory getCategoryForType(DocumentMetadata.DocumentType type) {
        switch (type) {
            case FORM_16:
            case FORM_16A:
            case FORM_26AS:
            case AIS:
                return DocumentMetadata.DocumentCategory.TDS_CERTIFICATE;
            case RENT_RECEIPT:
            case LOAN_CERTIFICATE:
            case INSURANCE_PREMIUM_RECEIPT:
            case TUITION_FEE_RECEIPT:
            case DONATION_RECEIPT:
            case MEDICAL_BILL:
            case INVESTMENT_PROOF:
                return DocumentMetadata.DocumentCategory.DEDUCTION_PROOF;
            case PROPERTY_DOCUMENT:
                return DocumentMetadata.DocumentCategory.PROPERTY_PROOF;
            case BUSINESS_BOOKS:
            case AUDIT_REPORT:
            case BALANCE_SHEET:
            case PL_STATEMENT:
                return DocumentMetadata.DocumentCategory.BUSINESS_PROOF;
            default:
                return DocumentMetadata.DocumentCategory.OTHER;
        }
    }
    
    private String calculateSHA256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        return bytesToHex(hash);
    }
    
    private String calculateMD5(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(data);
        return bytesToHex(hash);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
