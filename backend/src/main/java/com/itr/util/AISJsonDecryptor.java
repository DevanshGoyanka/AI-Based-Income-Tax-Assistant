package com.itr.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Decryptor for AIS (Annual Information Statement) JSON files downloaded from
 * the Income Tax Department compliance portal.
 *
 * <p>The AIS JSON is encrypted with the following scheme:
 * <ul>
 *   <li>First 32 hex chars (16 bytes): AES-CBC IV</li>
 *   <li>Next 32 hex chars (16 bytes): PBKDF2 salt</li>
 *   <li>Remaining content: Base64-encoded ciphertext</li>
 * </ul>
 *
 * <p>Encryption details:
 * <ul>
 *   <li>Key derivation: PBKDF2-HMAC-SHA256, 1000 iterations, 32-byte key</li>
 *   <li>Cipher: AES-256-CBC with PKCS#5 (PKCS#7) padding</li>
 *   <li>Password format: {pan_lowercase}{PASSWORD_MIDDLE}{dob_ddmmyyyy}</li>
 * </ul>
 *
 * <p>Example: PAN=ACUPG3482G, DOB=14/06/1974 → Password: acupg3482gGQ39%*g14061974
 */
@Component
@Slf4j
public class AISJsonDecryptor {

    /** Fixed middle segment in the AIS JSON password. */
    private static final String PASSWORD_MIDDLE = "GQ39%*g";

    /** PBKDF2 iterations for key derivation. */
    private static final int PBKDF2_ITERATIONS = 1000;

    /** Derived key length in bytes (256 bits). */
    private static final int KEY_LENGTH_BYTES = 32;

    /** AES block size in bytes. */
    private static final int AES_BLOCK_SIZE = 16;

    /** AES-CBC transformation string. */
    private static final String AES_CBC_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    /**
     * Decrypt an AIS JSON file using PAN and date of birth.
     *
     * @param encryptedContent the raw encrypted string from the AIS JSON file
     * @param pan              PAN (will be lowercased)
     * @param dob              date of birth in ddMMyyyy format (e.g. "14061974")
     * @return decrypted JSON string
     * @throws Exception if decryption fails
     */
    public String decrypt(String encryptedContent, String pan, String dob) throws Exception {
        return decryptWithPassword(encryptedContent, buildPassword(pan, dob));
    }

    /**
     * Decrypt with an explicitly provided password.
     *
     * @param encryptedContent the raw encrypted string
     * @param password         full password string for PBKDF2
     * @return decrypted JSON string
     * @throws Exception if decryption fails
     */
    public String decryptWithPassword(String encryptedContent, String password) throws Exception {
        String raw = encryptedContent.trim();

        // Handle JSON string wrapper (some export tools wrap the string in JSON quotes)
        if (raw.startsWith("\"") && raw.endsWith("\"")) {
            raw = raw.substring(1, raw.length() - 1);
            // Unescape any escaped characters
            raw = raw.replace("\\\"", "\"");
        }

        if (raw.length() < 64) {
            throw new IllegalArgumentException(
                "Encrypted AIS file is too short. Expected IV(32 hex) + salt(32 hex) + ciphertext.");
        }

        // Parse IV and salt from hex
        String ivHex = raw.substring(0, 32);
        String saltHex = raw.substring(32, 64);
        String ciphertextBase64 = raw.substring(64);

        byte[] iv;
        byte[] salt;
        try {
            iv = hexToBytes(ivHex);
            salt = hexToBytes(saltHex);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "First 64 characters must be hexadecimal (IV + salt)", e);
        }

        // Decode ciphertext
        byte[] ciphertext;
        try {
            ciphertext = Base64.getDecoder().decode(ciphertextBase64);
        } catch (IllegalArgumentException e) {
            // Fallback: try hex decoding
            try {
                ciphertext = hexToBytes(ciphertextBase64);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                    "Ciphertext must be valid Base64 or hexadecimal data", ex);
            }
        }

        if (ciphertext.length == 0) {
            throw new IllegalArgumentException("Ciphertext payload is empty.");
        }

        // Derive key using PBKDF2
        SecretKey secretKey = deriveKey(password, salt);

        // Decrypt with AES-CBC
        Cipher cipher = Cipher.getInstance(AES_CBC_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] decrypted = cipher.doFinal(ciphertext);

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * Build the AIS JSON password from PAN and DOB.
     * Format: {pan_lowercase}{PASSWORD_MIDDLE}{dob_ddmmyyyy}
     *
     * @param pan PAN number
     * @param dob date of birth as ddMMyyyy string
     * @return the password for PBKDF2 key derivation
     */
    public String buildPassword(String pan, String dob) {
        return pan.toLowerCase().trim() + PASSWORD_MIDDLE + dob;
    }

    /**
     * Derive a 256-bit AES key from password and salt using PBKDF2.
     */
    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH_BYTES * 8
        );
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Convert a hex string to bytes.
     */
    private byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    /**
     * Generate a random salt for testing purposes.
     */
    public byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    /**
     * Generate a random IV for testing purposes.
     */
    public byte[] generateIV() {
        byte[] iv = new byte[AES_BLOCK_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * Encrypt plaintext for testing (reverse of decrypt).
     */
    public String encrypt(String plaintext, String password, byte[] salt, byte[] iv) throws Exception {
        SecretKey secretKey = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance(AES_CBC_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        String ivHex = bytesToHex(iv);
        String saltHex = bytesToHex(salt);
        String ciphertextB64 = Base64.getEncoder().encodeToString(ciphertext);

        return ivHex + saltHex + ciphertextB64;
    }

    /**
     * Convert bytes to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
