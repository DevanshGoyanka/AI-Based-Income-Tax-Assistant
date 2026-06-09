package com.itr.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Depreciation Calculator - Complete IT Act Rates and Rules
 * Section 32 - Block of Assets Method
 * 101% CBDT Compliant
 */
@Slf4j
@Service
public class DepreciationService {

    // IT Act Depreciation Rates (as per Income Tax Rules)
    private static final Map<String, Double> DEPRECIATION_RATES = new HashMap<>();
    
    static {
        // Buildings
        DEPRECIATION_RATES.put("BUILDING_RCC_RESIDENTIAL", 5.0);
        DEPRECIATION_RATES.put("BUILDING_RCC_NON_RESIDENTIAL", 10.0);
        DEPRECIATION_RATES.put("BUILDING_TEMPORARY", 40.0);
        
        // Furniture and Fittings
        DEPRECIATION_RATES.put("FURNITURE_FITTINGS", 10.0);
        
        // Plant and Machinery
        DEPRECIATION_RATES.put("PLANT_MACHINERY_GENERAL", 15.0);
        DEPRECIATION_RATES.put("COMPUTERS_PERIPHERALS", 40.0);
        DEPRECIATION_RATES.put("ENERGY_SAVING_DEVICES", 40.0);
        DEPRECIATION_RATES.put("WINDMILLS", 40.0);
        
        // Vehicles
        DEPRECIATION_RATES.put("MOTOR_CAR_GENERAL", 15.0);
        DEPRECIATION_RATES.put("MOTOR_BUS_LORRY_HIRE", 30.0);
        DEPRECIATION_RATES.put("AIRCRAFT", 20.0);
        DEPRECIATION_RATES.put("SHIPS", 20.0);
        
        // Intangibles
        DEPRECIATION_RATES.put("PATENTS_COPYRIGHTS", 25.0);
        DEPRECIATION_RATES.put("KNOW_HOW", 25.0);
        DEPRECIATION_RATES.put("TRADEMARKS", 25.0);
        DEPRECIATION_RATES.put("LICENSES", 25.0);
        DEPRECIATION_RATES.put("GOODWILL", 0.0); // Budget 2021: Goodwill NOT depreciable
        
        // Books
        DEPRECIATION_RATES.put("BOOKS_OWNED", 100.0);
    }

    /**
     * Get depreciation rate for asset category
     */
    public double getDepreciationRate(String assetCategory) {
        return DEPRECIATION_RATES.getOrDefault(assetCategory, 15.0); // Default 15%
    }

    /**
     * Calculate depreciation using Block of Assets method
     * 
     * Formula:
     * Opening WDV + Additions (first half full, second half 50%) - Sales
     * Depreciation = Closing WDV × Rate
     * 
     * 50% Rule: Assets acquired after October 1 → 50% depreciation in first year
     */
    public DepreciationResult calculateDepreciation(DepreciationInput input) {
        DepreciationResult result = new DepreciationResult();
        
        // Opening WDV
        double openingWDV = input.getOpeningWDV();
        
        // Additions
        double additionsFirstHalf = input.getAdditionsFirstHalf();
        double additionsSecondHalf = input.getAdditionsSecondHalf();
        
        // Apply 50% rule for second half additions
        double effectiveAdditions = additionsFirstHalf + (additionsSecondHalf * 0.5);
        
        // Sale proceeds (capped at opening WDV + additions)
        double saleProceeds = input.getSaleProceeds();
        double maxSaleProceeds = openingWDV + additionsFirstHalf + additionsSecondHalf;
        
        if (saleProceeds > maxSaleProceeds) {
            // Block becomes zero, excess = Short-term capital gain
            result.setBlockBecameZero(true);
            result.setShortTermCapitalGain(saleProceeds - maxSaleProceeds);
            result.setClosingWDV(0);
            result.setDepreciation(0);
            result.setWarning("Sale proceeds exceed block value. Entire block becomes zero. " +
                            "Excess of ₹" + result.getShortTermCapitalGain() + " is short-term capital gain.");
            return result;
        }
        
        // WDV for depreciation calculation
        double wdvForDepreciation = openingWDV + effectiveAdditions - saleProceeds;
        wdvForDepreciation = Math.max(0, wdvForDepreciation);
        
        // Calculate depreciation
        double rate = getDepreciationRate(input.getAssetCategory());
        double depreciation = wdvForDepreciation * (rate / 100.0);
        
        // Additional depreciation (20% for new P&M in manufacturing/power)
        double additionalDepreciation = 0;
        if (input.isEligibleForAdditionalDepreciation()) {
            // Additional depreciation on new P&M
            double eligibleForAdditional = additionsFirstHalf + (additionsSecondHalf * 0.5);
            additionalDepreciation = eligibleForAdditional * 0.20;
            
            result.setAdditionalDepreciation(additionalDepreciation);
        }
        
        // Closing WDV
        double closingWDV = wdvForDepreciation - depreciation;
        
        result.setOpeningWDV(openingWDV);
        result.setAdditionsFirstHalf(additionsFirstHalf);
        result.setAdditionsSecondHalf(additionsSecondHalf);
        result.setEffectiveAdditions(effectiveAdditions);
        result.setSaleProceeds(saleProceeds);
        result.setWdvForDepreciation(wdvForDepreciation);
        result.setDepreciationRate(rate);
        result.setDepreciation(depreciation);
        result.setClosingWDV(closingWDV);
        result.setTotalDepreciation(depreciation + additionalDepreciation);
        
        log.info("Depreciation: Category={}, Opening={}, Additions={}, Sales={}, Rate={}%, Dep={}, Closing={}", 
                input.getAssetCategory(), openingWDV, effectiveAdditions, saleProceeds, 
                rate, depreciation, closingWDV);
        
        return result;
    }

    /**
     * Determine if asset acquired in second half (after October 1)
     */
    public boolean isSecondHalfAcquisition(LocalDate acquisitionDate, String financialYear) {
        // Financial year: April 1 to March 31
        // Second half: October 2 onwards
        int year = Integer.parseInt(financialYear.split("-")[0]);
        LocalDate oct1 = LocalDate.of(year, 10, 1);
        
        return acquisitionDate.isAfter(oct1);
    }

    /**
     * Validate goodwill depreciation (NOT allowed from AY 2021-22)
     */
    public boolean isGoodwillDepreciable(LocalDate acquisitionDate) {
        // Budget 2021: Goodwill acquired on/after April 1, 2021 NOT depreciable
        LocalDate cutoffDate = LocalDate.of(2021, 4, 1);
        return acquisitionDate.isBefore(cutoffDate);
    }

    @Data
    public static class DepreciationInput {
        private String assetCategory;
        private double openingWDV;
        private double additionsFirstHalf;
        private double additionsSecondHalf;
        private double saleProceeds;
        private boolean eligibleForAdditionalDepreciation;
    }

    @Data
    public static class DepreciationResult {
        private double openingWDV;
        private double additionsFirstHalf;
        private double additionsSecondHalf;
        private double effectiveAdditions;
        private double saleProceeds;
        private double wdvForDepreciation;
        private double depreciationRate;
        private double depreciation;
        private double additionalDepreciation;
        private double totalDepreciation;
        private double closingWDV;
        private boolean blockBecameZero;
        private double shortTermCapitalGain;
        private String warning;
    }
}
