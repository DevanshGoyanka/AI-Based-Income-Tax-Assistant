package com.itr.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM Encryption Service for Sensitive Data
 * Encrypts PAN, Aadhaar, bank account numbers at rest
 * GCM mode provides authenticated encryption (prevents tampering)
 */
@Slf4j
@Service
public class SensitiveDataEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;  // 96-bit IV for GCM
    private static final int TAG_LENGTH_BITS = 128; // 128-bit auth tag
    private static final int KEY_SIZE_BITS = 256;   // AES-256

    @Value("${encryption.key.base64:}")
    private String keyBase64;

    private SecretKey secretKey;

    /**
     * Initialize encryption key from environment or generate new one
     */
    private SecretKey getSecretKey() {
        if (secretKey != null) {
            return secretKey;
        }

        try {
            if (keyBase64 != null && !keyBase64.isEmpty()) {
                byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
                secretKey = new SecretKeySpec(keyBytes, "AES");
                log.info("Encryption key loaded from configuration");
            } else {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(KEY_SIZE_BITS, new SecureRandom());
                secretKey = keyGen.generateKey();
                log.warn("Generated new encryption key - store this in environment: {}",
                        Base64.getEncoder().encodeToString(secretKey.getEncoded()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption key", e);
        }

        return secretKey;
    }

    /**
     * Encrypt sensitive data
     * Format: base64(IV):base64(ciphertext+tag)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            GCMParameterSpec paramSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), paramSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

            return Base64.getEncoder().encodeToString(iv) + ":" +
                   Base64.getEncoder().encodeToString(cipherText);

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt sensitive data
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            String[] parts = encryptedText.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);

            GCMParameterSpec paramSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), paramSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, "UTF-8");

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Mask sensitive data for logging (show first 2 and last 2 chars)
     */
    public String mask(String sensitiveData) {
        if (sensitiveData == null || sensitiveData.length() <= 4) {
            return "****";
        }
        return sensitiveData.substring(0, 2) + "****" + 
               sensitiveData.substring(sensitiveData.length() - 2);
    }
}
