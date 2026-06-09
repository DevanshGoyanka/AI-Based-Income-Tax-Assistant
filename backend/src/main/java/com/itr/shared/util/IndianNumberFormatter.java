package com.itr.shared.util;

import java.text.DecimalFormat;

/**
 * IndianNumberFormatter — formats numbers in Indian numbering system.
 * <p>
 * Indian: 1,23,45,678 (grouping: after first 3 digits, groups of 2)
 * Western: 12,345,678
 */
public final class IndianNumberFormatter {

    private IndianNumberFormatter() {}

    private static final DecimalFormat INDIAN_FORMAT = new DecimalFormat("#,##,##0");

    /**
     * Format a number in Indian numbering system.
     * Example: 12345678 → "1,23,45,678"
     */
    public static String format(long number) {
        String numStr = String.valueOf(Math.abs(number));
        StringBuilder result = new StringBuilder();
        int len = numStr.length();

        // First group: last 3 digits (or less if number is smaller)
        if (len <= 3) {
            return (number < 0 ? "-" : "") + numStr;
        }

        // First 1-3 digits from the right
        result.insert(0, numStr.substring(len - 3));

        // Remaining digits: groups of 2
        int remaining = len - 3;
        for (int i = remaining - 2; i >= 0; i -= 2) {
            result.insert(0, ",");
            int end = i + 2;
            if (end > remaining) {
                end = remaining;
            }
            result.insert(0, numStr.substring(i, end));
        }

        return (number < 0 ? "-" : "") + result.toString();
    }

    /**
     * Format an amount in Indian rupees.
     * Example: 12345678 paise → "₹1,23,456.78"
     */
    public static String formatRupees(long paise) {
        long rupees = paise / 100;
        long remainingPaise = paise % 100;
        return "₹" + format(rupees) + "." + (remainingPaise < 10 ? "0" : "") + remainingPaise;
    }

    /**
     * Format amount in paise to words (simplified).
     * Example: 12500000 → "₹1,25,000"
     */
    public static String formatAmount(long amountInPaise) {
        return formatRupees(amountInPaise);
    }
}
