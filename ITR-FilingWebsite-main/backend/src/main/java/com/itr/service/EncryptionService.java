package com.itr.service;

import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for document security.
 * Provides encryption at rest with authenticated encryption.
 */
@Service
public class EncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    
    private final SecureRandom secureRandom;
    
    public EncryptionService() {
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Generate a new AES-256 encryption key.
     */
    public SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE, secureRandom);
        return keyGenerator.generateKey();
    }
    
    /**
     * Encrypt data using AES-256-GCM.
     */
    public EncryptedData encrypt(byte[] data, SecretKey key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
        
        byte[] encryptedData = cipher.doFinal(data);
        
        return EncryptedData.builder()
                .encryptedBytes(encryptedData)
                .iv(iv)
                .algorithm(TRANSFORMATION)
                .build();
    }
    
    /**
     * Decrypt data using AES-256-GCM.
     */
    public byte[] decrypt(byte[] encryptedData, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
        
        return cipher.doFinal(encryptedData);
    }
    
    /**
     * Convert SecretKey to Base64 string for storage.
     */
    public String keyToString(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    /**
     * Convert Base64 string back to SecretKey.
     */
    public SecretKey stringToKey(String keyString) {
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }
    
    /**
     * Convert IV to Base64 string for storage.
     */
    public String ivToString(byte[] iv) {
        return Base64.getEncoder().encodeToString(iv);
    }
    
    /**
     * Convert Base64 string back to IV.
     */
    public byte[] stringToIv(String ivString) {
        return Base64.getDecoder().decode(ivString);
    }
    
    @lombok.Data
    @lombok.Builder
    public static class EncryptedData {
        private byte[] encryptedBytes;
        private byte[] iv;
        private String algorithm;
    }
}
