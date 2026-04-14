package com.itr.dto.itd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIS (Annual Information Statement) Encrypted Data DTO
 * Real AIS JSON files from ITD portal are encrypted/encoded
 * AIS PDFs are password-protected with: {pan_lowercase}{ddmmyyyy}
 * 
 * Example: PAN=AAAAA1234A, DOB=21/01/1991 → Password: aaaaa1234a21011991
 * 
 * Based on actual sample: XXXPG3482X_2025-26_AIS_14042026 (1).json
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AISDataEncrypted {
    
    /**
     * The entire AIS JSON content is encrypted as a single string
     * Format: Base64 or custom ITD encryption
     * Requires decryption using ITD-provided keys or methods
     */
    private String encryptedData;
    
    /**
     * Optional: Encryption metadata if provided
     */
    private String encryptionType;
    private String version;
    private String pan;
    private String assessmentYear;
    
    /**
     * Flag to indicate if this is raw encrypted data
     */
    @Builder.Default
    private boolean isEncrypted = true;
    
    /**
     * PDF Password Format (for AIS/TIS PDFs):
     * {pan_lowercase}{ddmmyyyy}
     * 
     * For individuals: PAN + Date of Birth
     * For non-individuals: PAN + Date of Incorporation/Formation
     * 
     * Example: AAAAA1234A + 21/01/1991 = aaaaa1234a21011991
     * 
     * Use ITDPdfDecryptor utility to decrypt password-protected PDFs
     */
}
