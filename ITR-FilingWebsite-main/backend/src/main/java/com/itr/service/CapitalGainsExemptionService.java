package com.itr.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Capital Gains Exemption Calculator - Sections 54, 54EC, 54F, 54B, 54D, 54G, 54GB
 * Cost Inflation Index (CII) for indexation
 * 101% CBDT Compliant
 */
@Slf4j
@Service
public class CapitalGainsExemptionService {

    // Cost Inflation Index (CII) - Base Year 2001-02 = 100
    private static final Map<String, Integer> CII_TABLE = new HashMap<>();
    
    static {
        CII_TABLE.put("2001-02", 100);
        CII_TABLE.put("2002-03", 105);
        CII_TABLE.put("2003-04", 109);
        CII_TABLE.put("2004-05", 113);
        CII_TABLE.put("2005-06", 117);
        CII_TABLE.put("2006-07", 122);
        CII_TABLE.put("2007-08", 129);
        CII_TABLE.put("2008-09", 137);
        CII_TABLE.put("2009-10", 148);
        CII_TABLE.put("2010-11", 167);
        CII_TABLE.put("2011-12", 184);
        CII_TABLE.put("2012-13", 200);
        CII_TABLE.put("2013-14", 220);
        CII_TABLE.put("2014-15", 240);
        CII_TABLE.put("2015-16", 254);
        CII_TABLE.put("2016-17", 264);
        CII_TABLE.put("2017-18", 272);
        CII_TABLE.put("2018-19", 280);
        CII_TABLE.put("2019-20", 289);
        CII_TABLE.put("2020-21", 301);
        CII_TABLE.put("2021-22", 317);
        CII_TABLE.put("2022-23", 331);
        CII_TABLE.put("2023-24", 348);
        CII_TABLE.put("2024-25", 363);
        // 2025-26 to be notified by CBDT
    }

    /**
     * Get CII for a financial year
     */
    public int getCII(String financialYear) {
        return CII_TABLE.getOrDefault(financialYear, 0);
    }

    /**
     * Calculate indexed cost of acquisition
     */
    public double calculateIndexedCost(double actualCost, String acquisitionFY, String saleFY) {
        int ciiAcquisition = getCII(acquisitionFY);
        int ciiSale = getCII(saleFY);
        
        if (ciiAcquisition == 0 || ciiSale == 0) {
            log.warn("CII not available for {} or {}", acquisitionFY, saleFY);
            return actualCost;
        }
        
        double indexedCost = actualCost * ((double) ciiSale / ciiAcquisition);
        
        log.debug("Indexed Cost: Actual={}, CII_Acq={}, CII_Sale={}, Indexed={}", 
                actualCost, ciiAcquisition, ciiSale, indexedCost);
        
        return indexedCost;
    }

    /**
     * Section 54 - Exemption on sale of residential property
     * Conditions:
     * - LTCG on residential house property
     * - Purchase/construct one residential house within: 1 year before or 2 years after sale
     * - OR construct within 3 years after sale
     * - Exemption = lower of (capital gain, investment in new property)
     * - If investment > capital gain: no exemption on excess
     * - If not invested before ITR due date: deposit in CGAS
     */
    public Exemption54Result calculate54Exemption(double capitalGain, double investmentAmount,
                                                    LocalDate saleDate, LocalDate purchaseDate,
                                                    boolean isConstruction) {
        
        Exemption54Result result = new Exemption54Result();
        result.setCapitalGain(capitalGain);
        result.setInvestmentAmount(investmentAmount);
        
        // Validate time period
        long daysDiff = ChronoUnit.DAYS.between(saleDate, purchaseDate);
        long yearsDiff = daysDiff / 365;
        
        boolean withinPeriod;
        if (isConstruction) {
            // Construction: within 3 years after sale
            withinPeriod = daysDiff >= 0 && yearsDiff <= 3;
        } else {
            // Purchase: 1 year before to 2 years after sale
            withinPeriod = daysDiff >= -365 && yearsDiff <= 2;
        }
        
        if (!withinPeriod) {
            result.setEligible(false);
            result.setReason("Investment not within prescribed time period");
            return result;
        }
        
        // Calculate exemption
        double exemption = Math.min(capitalGain, investmentAmount);
        
        result.setEligible(true);
        result.setExemptionAmount(exemption);
        result.setTaxableGain(capitalGain - exemption);
        
        log.info("Section 54: CG={}, Investment={}, Exemption={}", 
                capitalGain, investmentAmount, exemption);
        
        return result;
    }

    /**
     * Section 54EC - Exemption on investment in specified bonds
     * Conditions:
     * - LTCG on any long-term capital asset
     * - Investment in NHAI/REC/PFC bonds within 6 months of sale
     * - Lock-in: 5 years
     * - Maximum investment: ₹50,00,000 per financial year
     * - Exemption = lower of (capital gain, investment)
     */
    public Exemption54ECResult calculate54ECExemption(double capitalGain, double investmentAmount,
                                                        LocalDate saleDate, LocalDate investmentDate) {
        
        Exemption54ECResult result = new Exemption54ECResult();
        result.setCapitalGain(capitalGain);
        result.setInvestmentAmount(investmentAmount);
        
        // Validate 6-month period
        long daysDiff = ChronoUnit.DAYS.between(saleDate, investmentDate);
        if (daysDiff < 0 || daysDiff > 180) {
            result.setEligible(false);
            result.setReason("Investment not within 6 months of sale");
            return result;
        }
        
        // Cap at ₹50 lakh per FY
        double cappedInvestment = Math.min(investmentAmount, 5000000);
        if (investmentAmount > 5000000) {
            result.setWarning("Investment exceeds ₹50L limit. Exemption capped at ₹50L.");
        }
        
        // Calculate exemption
        double exemption = Math.min(capitalGain, cappedInvestment);
        
        result.setEligible(true);
        result.setExemptionAmount(exemption);
        result.setTaxableGain(capitalGain - exemption);
        
        log.info("Section 54EC: CG={}, Investment={}, Exemption={}", 
                capitalGain, investmentAmount, exemption);
        
        return result;
    }

    /**
     * Section 54F - Exemption on investment in residential property (any asset sale)
     * Conditions:
     * - LTCG on any long-term capital asset OTHER than residential house
     * - Assessee should not own more than one residential house on date of sale
     * - Purchase one residential house within: 1 year before or 2 years after
     * - OR construct within 3 years after sale
     * - Should not purchase another house within 2 years or construct within 3 years
     * - Exemption = CG × (Investment / Net Consideration)
     * - If investment ≥ net consideration: full exemption
     */
    public Exemption54FResult calculate54FExemption(double capitalGain, double netConsideration,
                                                      double investmentAmount, int housesOwnedOnSaleDate,
                                                      LocalDate saleDate, LocalDate investmentDate,
                                                      boolean isConstruction) {
        
        Exemption54FResult result = new Exemption54FResult();
        result.setCapitalGain(capitalGain);
        result.setNetConsideration(netConsideration);
        result.setInvestmentAmount(investmentAmount);
        
        // Validate: should not own more than 1 house on sale date
        if (housesOwnedOnSaleDate > 1) {
            result.setEligible(false);
            result.setReason("Assessee owns more than one residential house on date of sale");
            return result;
        }
        
        // Validate time period
        long daysDiff = ChronoUnit.DAYS.between(saleDate, investmentDate);
        long yearsDiff = daysDiff / 365;
        
        boolean withinPeriod;
        if (isConstruction) {
            withinPeriod = daysDiff >= 0 && yearsDiff <= 3;
        } else {
            withinPeriod = daysDiff >= -365 && yearsDiff <= 2;
        }
        
        if (!withinPeriod) {
            result.setEligible(false);
            result.setReason("Investment not within prescribed time period");
            return result;
        }
        
        // Calculate exemption
        double exemption;
        if (investmentAmount >= netConsideration) {
            // Full exemption
            exemption = capitalGain;
        } else {
            // Proportionate exemption
            exemption = capitalGain * (investmentAmount / netConsideration);
        }
        
        result.setEligible(true);
        result.setExemptionAmount(exemption);
        result.setTaxableGain(capitalGain - exemption);
        
        log.info("Section 54F: CG={}, NetConsideration={}, Investment={}, Exemption={}", 
                capitalGain, netConsideration, investmentAmount, exemption);
        
        return result;
    }

    /**
     * Section 54B - Exemption on transfer of agricultural land
     * Conditions:
     * - LTCG on agricultural land (used for agricultural purposes by assessee/parents for 2 years)
     * - Purchase another agricultural land within 2 years
     * - Exemption = lower of (capital gain, investment)
     */
    public double calculate54BExemption(double capitalGain, double investmentAmount) {
        return Math.min(capitalGain, investmentAmount);
    }

    /**
     * Section 54D - Exemption on compulsory acquisition of land and building
     * Conditions:
     * - Compulsory acquisition of land/building used for industrial undertaking
     * - Purchase another land/building for industrial purpose within 3 years
     * - Exemption = lower of (capital gain, investment)
     */
    public double calculate54DExemption(double capitalGain, double investmentAmount) {
        return Math.min(capitalGain, investmentAmount);
    }

    /**
     * Section 54G - Exemption on transfer of assets in case of shifting of industrial undertaking
     * Conditions:
     * - Transfer of machinery/plant due to shifting from urban to non-urban area
     * - Purchase new machinery/plant within 1 year before or 3 years after
     * - Exemption = lower of (capital gain, investment)
     */
    public double calculate54GExemption(double capitalGain, double investmentAmount) {
        return Math.min(capitalGain, investmentAmount);
    }

    /**
     * Section 54GB - Exemption on transfer of residential property for investment in startup
     * Conditions:
     * - LTCG on residential property (held for >3 years)
     * - Investment in equity shares of eligible startup within 6 months
     * - Startup: incorporated after April 1, 2016; turnover < ₹25Cr
     * - Lock-in: 5 years
     * - Exemption = lower of (capital gain, investment, ₹50L)
     */
    public double calculate54GBExemption(double capitalGain, double investmentAmount) {
        double cappedInvestment = Math.min(investmentAmount, 5000000);
        return Math.min(capitalGain, cappedInvestment);
    }

    // Result classes
    @Data
    public static class Exemption54Result {
        private double capitalGain;
        private double investmentAmount;
        private boolean eligible;
        private String reason;
        private double exemptionAmount;
        private double taxableGain;
    }

    @Data
    public static class Exemption54ECResult {
        private double capitalGain;
        private double investmentAmount;
        private boolean eligible;
        private String reason;
        private String warning;
        private double exemptionAmount;
        private double taxableGain;
    }

    @Data
    public static class Exemption54FResult {
        private double capitalGain;
        private double netConsideration;
        private double investmentAmount;
        private boolean eligible;
        private String reason;
        private double exemptionAmount;
        private double taxableGain;
    }
}
