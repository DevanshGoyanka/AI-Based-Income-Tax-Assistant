package com.itr.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * SHA-256 Digest Utility for ITR JSON
 * As per ITD schema, CreationInfo must contain SHA-256 digest of JSON content
 */
@Slf4j
public class SHA256DigestUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Compute SHA-256 digest of JSON object
     */
    public static String computeDigest(Object jsonObject) {
        try {
            String jsonString = objectMapper.writeValueAsString(jsonObject);
            return computeDigest(jsonString);
        } catch (Exception e) {
            log.error("Error computing digest for object", e);
            return null;
        }
    }
    
    /**
     * Compute SHA-256 digest of string
     */
    public static String computeDigest(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return null;
        }
    }
    
    /**
     * Generate HMAC-SHA256 digest with iterations (ITD compatible)
     */
    public static String generateDigest(String jsonString, String secretKey, int iterations) throws Exception {
        try {
            byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hashBytes = mac.doFinal(jsonBytes);

            for (int i = 1; i <= iterations; i++) {
                mac.reset();
                hashBytes = mac.doFinal(hashBytes);
            }

            return Base64.getEncoder().encodeToString(hashBytes);

        } catch (Exception e) {
            log.error("Error generating digest: {}", e.getMessage());
            throw new Exception("Failed to generate digest", e);
        }
    }
    
    /**
     * Convert byte array to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
