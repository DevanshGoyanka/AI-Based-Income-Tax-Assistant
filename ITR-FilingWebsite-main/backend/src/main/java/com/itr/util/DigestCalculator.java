package com.itr.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * HMAC-SHA256 Digest Calculator for ITD JSON
 * Implements iterative HMAC as per CBDT requirements
 */
@Slf4j
public class DigestCalculator {
    
    /**
     * Generate HMAC-SHA256 digest with iterations (similar to VBA HMACSHA256A function)
     * @param jsonString JSON string to hash
     * @param secretKey Secret key for HMAC
     * @param iterations Number of hash iterations
     * @return Base64 encoded digest
     */
    public static String generateDigest(String jsonString, String secretKey, int iterations) throws Exception {
        try {
            byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

            // Initialize HMAC-SHA256
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
            mac.init(secretKeySpec);

            // First hash computation
            byte[] hashBytes = mac.doFinal(jsonBytes);

            // Iterate the hash (each iteration hashes the previous result)
            for (int i = 1; i < iterations; i++) {
                mac.reset();
                hashBytes = mac.doFinal(hashBytes);
            }

            // Base64 encode the final hash
            String digest = Base64.getEncoder().encodeToString(hashBytes);
            log.info("Digest generated successfully with {} iterations", iterations);
            return digest;

        } catch (java.security.NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            log.error("Error generating digest: {}", e.getMessage(), e);
            throw new Exception("Failed to generate digest", e);
        }
    }
    
    /**
     * Generate digest with default parameters from ITD
     */
    public static String generateDigestWithDefaults(String jsonString) throws Exception {
        // Default ITD parameters
        String secretKey = "4448ffc0cec1a25d";
        int iterations = 1344;
        return generateDigest(jsonString, secretKey, iterations);
    }
}
