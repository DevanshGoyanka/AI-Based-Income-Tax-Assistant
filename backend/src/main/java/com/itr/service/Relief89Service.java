package com.itr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Relief u/s 89 Service - Salary Arrears Relief
 * 101% CBDT Compliant
 * 
 * When salary arrears/advance are received in one year but relate to previous years,
 * relief u/s 89 allows tax to be calculated as if arrears were received in respective years.
 * 
 * Form 10E must be filed BEFORE filing ITR to claim this relief.
 */
@Slf4j
@Service
public class Relief89Service {

    /**
     * Calculate relief u/s 89 for salary arrears
     * 
     * Method:
     * 1. Calculate tax on total income (including arrears) in current year
     * 2. Calculate tax on total income (excluding arrears) in current year
     * 3. Calculate tax on arrears in respective years they relate to
     * 4. Relief = (Tax with arrears - Tax without arrears) - Tax on arrears in respective years
     */
    public Relief89Result calculateRelief89(Relief89Input input) {
        
        log.info("Calculating relief u/s 89 for salary arrears");
        
        // Step 1: Tax on total income including arrears (current year)
        double taxWithArrears = input.getTaxOnTotalIncomeWithArrears();
        
        // Step 2: Tax on total income excluding arrears (current year)
        double taxWithoutArrears = input.getTaxOnTotalIncomeWithoutArrears();
        
        // Step 3: Calculate tax on arrears in respective years
        double totalTaxOnArrearsInRespectiveYears = 0;
        List<String> calculations = new ArrayList<>();
        
        for (ArrearEntry arrear : input.getArrearEntries()) {
            String year = arrear.getYearToWhichArrearRelates();
            double arrearAmount = arrear.getArrearAmount();
            
            // Tax calculation for that year
            // Tax on (income of that year + arrear) - Tax on (income of that year)
            double taxWithArrearInThatYear = arrear.getTaxOnIncomeWithArrear();
            double taxWithoutArrearInThatYear = arrear.getTaxOnIncomeWithoutArrear();
            double taxOnArrearInThatYear = taxWithArrearInThatYear - taxWithoutArrearInThatYear;
            
            totalTaxOnArrearsInRespectiveYears += taxOnArrearInThatYear;
            
            calculations.add(String.format("Year %s: Arrear ₹%.2f, Tax on arrear ₹%.2f", 
                    year, arrearAmount, taxOnArrearInThatYear));
            
            log.debug("Arrear for {}: Amount=₹{}, Tax=₹{}", year, arrearAmount, taxOnArrearInThatYear);
        }
        
        // Step 4: Calculate relief
        double relief = (taxWithArrears - taxWithoutArrears) - totalTaxOnArrearsInRespectiveYears;
        relief = Math.max(0, relief); // Relief cannot be negative
        
        log.info("Relief u/s 89 calculated: ₹{} (Tax with arrears: ₹{}, Tax without: ₹{}, Tax on arrears in respective years: ₹{})",
                relief, taxWithArrears, taxWithoutArrears, totalTaxOnArrearsInRespectiveYears);
        
        return Relief89Result.builder()
                .reliefAmount(relief)
                .taxWithArrears(taxWithArrears)
                .taxWithoutArrears(taxWithoutArrears)
                .taxOnArrearsInRespectiveYears(totalTaxOnArrearsInRespectiveYears)
                .calculations(calculations)
                .form10ERequired(true)
                .build();
    }

    /**
     * Input for Relief u/s 89 calculation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Relief89Input {
        private double taxOnTotalIncomeWithArrears;
        private double taxOnTotalIncomeWithoutArrears;
        private List<ArrearEntry> arrearEntries;
    }

    /**
     * Arrear entry for a specific year
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ArrearEntry {
        private String yearToWhichArrearRelates; // e.g., "2023-24"
        private double arrearAmount;
        private double incomeOfThatYear; // Original income of that year
        private double taxOnIncomeWithoutArrear; // Tax on original income
        private double taxOnIncomeWithArrear; // Tax on (original income + arrear)
    }

    /**
     * Result of Relief u/s 89 calculation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Relief89Result {
        private double reliefAmount;
        private double taxWithArrears;
        private double taxWithoutArrears;
        private double taxOnArrearsInRespectiveYears;
        private List<String> calculations;
        private boolean form10ERequired;
    }
}
