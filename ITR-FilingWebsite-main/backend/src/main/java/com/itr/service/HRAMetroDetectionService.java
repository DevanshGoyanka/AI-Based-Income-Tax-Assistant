package com.itr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * HRA Metro City Detection Service - 101% CBDT Compliant
 * Determines if city qualifies for 50% HRA exemption (metro) vs 40% (non-metro)
 * Reference: Section 10(13A) and Rule 2A
 */
@Slf4j
@Service
public class HRAMetroDetectionService {

    // Metro cities as per CBDT: Mumbai, Delhi, Kolkata, Chennai
    private static final Set<String> METRO_CITIES = new HashSet<>(Arrays.asList(
        "MUMBAI", "DELHI", "NEW DELHI", "KOLKATA", "CHENNAI",
        "GREATER MUMBAI", "NAVI MUMBAI", "THANE", // Mumbai Metropolitan Region
        "NOIDA", "GURGAON", "GURUGRAM", "FARIDABAD", "GHAZIABAD" // NCR Delhi
    ));

    /**
     * Detect if city is metro for HRA exemption calculation
     * Metro: 50% of salary, Non-metro: 40% of salary
     */
    public boolean isMetroCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return false;
        }
        
        String normalizedCity = city.trim().toUpperCase();
        boolean isMetro = METRO_CITIES.contains(normalizedCity);
        
        log.debug("HRA Metro Detection: {} -> {}", city, isMetro ? "METRO (50%)" : "NON-METRO (40%)");
        return isMetro;
    }

    /**
     * Get HRA exemption percentage based on city
     */
    public double getHRAExemptionRate(String city) {
        return isMetroCity(city) ? 0.50 : 0.40;
    }

    /**
     * Compute HRA exemption as per Section 10(13A)
     * Exemption = Minimum of:
     * 1. Actual HRA received
     * 2. Rent paid - 10% of salary
     * 3. 50% of salary (metro) or 40% of salary (non-metro)
     */
    public int computeHRAExemption(String city, int hraReceived, int rentPaid, int basicSalary) {
        if (hraReceived <= 0 || rentPaid <= 0) {
            return 0;
        }

        int tenPercentSalary = (int) Math.round(basicSalary * 0.10);
        int rentMinusTenPercent = Math.max(0, rentPaid - tenPercentSalary);
        
        double exemptionRate = getHRAExemptionRate(city);
        int percentageOfSalary = (int) Math.round(basicSalary * exemptionRate);

        int exemption = Math.min(hraReceived, 
                        Math.min(rentMinusTenPercent, percentageOfSalary));

        log.info("HRA Exemption computed for {}: Received={}, Rent={}, Basic={}, Exemption={}", 
                 city, hraReceived, rentPaid, basicSalary, exemption);

        return exemption;
    }
}
