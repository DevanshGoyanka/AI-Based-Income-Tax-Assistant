package com.itr.shared.util;

/**
 * AadhaarMasker — masks Aadhaar number for display.
 * Format: XXXX XXXX 5678 (only last 4 digits visible).
 */
public final class AadhaarMasker {

    private AadhaarMasker() {}

    public static String mask(String aadhaar) {
        if (aadhaar == null || aadhaar.trim().isEmpty()) return "";
        String clean = aadhaar.replaceAll("\\s", "");
        if (clean.length() < 4) return clean;
        int len = clean.length();
        return "XXXX XXXX " + clean.substring(len - 4);
    }
}
