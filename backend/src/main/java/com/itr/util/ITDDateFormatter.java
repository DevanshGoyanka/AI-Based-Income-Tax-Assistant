package com.itr.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * ITD Date Formatter - 101% CBDT Compliant
 * All dates in ITR JSON must be in DD/MM/YYYY format as per ITD schema
 */
public class ITDDateFormatter {
    
    private static final DateTimeFormatter ITD_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    /**
     * Format LocalDate to ITD format (DD/MM/YYYY)
     */
    public static String format(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(ITD_DATE_FORMAT);
    }
    
    /**
     * Parse ITD format date string to LocalDate
     */
    public static LocalDate parse(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr, ITD_DATE_FORMAT);
    }
    
    /**
     * Validate date string is in ITD format
     */
    public static boolean isValidITDDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false;
        }
        try {
            parse(dateStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
