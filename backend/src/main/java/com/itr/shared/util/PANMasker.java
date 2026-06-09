package com.itr.shared.util;

/**
 * PANMasker — masks PAN for logs and display.
 * Format: ABCDE****F (only first 5 and last 1 character visible).
 */
public final class PANMasker {

    private PANMasker() {}

    public static String mask(String pan) {
        if (pan == null || pan.length() != 10) return "****";
        return pan.substring(0, 5) + "****" + pan.charAt(9);
    }
}
