package com.itr.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PII Masking Utility - CBDT Compliant Data Protection
 * Masks sensitive personal information for display/logging
 */
@Slf4j
@Component
public class PIIMaskingUtil {

    /**
     * Mask PAN: XXXXX9999X
     */
    public static String maskPAN(String pan) {
        if (pan == null || pan.length() != 10) {
            return pan;
        }
        return "XXXXX" + pan.substring(5, 9) + "X";
    }

    /**
     * Mask Aadhaar: XXXX XXXX 9999
     */
    public static String maskAadhaar(String aadhaar) {
        if (aadhaar == null) {
            return null;
        }
        String cleaned = aadhaar.replaceAll("\\s+", "");
        if (cleaned.length() != 12) {
            return aadhaar;
        }
        return "XXXX XXXX " + cleaned.substring(8);
    }

    /**
     * Mask Bank Account: XXXXXX9999
     */
    public static String maskBankAccount(String accountNo) {
        if (accountNo == null || accountNo.length() < 4) {
            return accountNo;
        }
        int visibleDigits = Math.min(4, accountNo.length());
        String masked = "X".repeat(accountNo.length() - visibleDigits);
        return masked + accountNo.substring(accountNo.length() - visibleDigits);
    }

    /**
     * Mask Mobile: XXXXXX9999
     */
    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 10) {
            return mobile;
        }
        return "XXXXXX" + mobile.substring(6);
    }

    /**
     * Mask Email: x***@domain.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts[0].length() <= 1) {
            return email;
        }
        return parts[0].charAt(0) + "***@" + parts[1];
    }

    /**
     * Mask TAN: XXXX99999X
     */
    public static String maskTAN(String tan) {
        if (tan == null || tan.length() != 10) {
            return tan;
        }
        return "XXXX" + tan.substring(4, 9) + "X";
    }

    /**
     * Check if string contains PAN pattern
     */
    public static boolean containsPAN(String text) {
        if (text == null) return false;
        return text.matches(".*[A-Z]{5}[0-9]{4}[A-Z].*");
    }

    /**
     * Check if string contains Aadhaar pattern
     */
    public static boolean containsAadhaar(String text) {
        if (text == null) return false;
        return text.matches(".*\\d{12}.*") || text.matches(".*\\d{4}\\s\\d{4}\\s\\d{4}.*");
    }

    /**
     * Sanitize log message - mask all PII
     */
    public static String sanitizeForLog(String message) {
        if (message == null) return null;
        
        String sanitized = message;
        
        // Mask PAN patterns
        sanitized = sanitized.replaceAll("[A-Z]{5}[0-9]{4}[A-Z]", "XXXXX****X");
        
        // Mask Aadhaar patterns
        sanitized = sanitized.replaceAll("\\d{12}", "XXXXXXXX****");
        sanitized = sanitized.replaceAll("\\d{4}\\s\\d{4}\\s\\d{4}", "XXXX XXXX ****");
        
        // Mask 10-digit mobile numbers
        sanitized = sanitized.replaceAll("\\b[6-9]\\d{9}\\b", "XXXXXX****");
        
        return sanitized;
    }
}
