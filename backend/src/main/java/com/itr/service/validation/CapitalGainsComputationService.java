package com.itr.service.validation;

import com.itr.dto.Itr1FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

/**
 * Capital Gains Computation Service - CBDT Compliant
 * Handles all capital gains scenarios with proper set-off rules and grandfathering
 * Includes CII-based indexation for LTCG computation under Section 48
 */
@Service
@Slf4j
public class CapitalGainsComputationService {

    private static final LocalDate BUDGET_2024_DATE = LocalDate.of(2024, 7, 23);
    private static final LocalDate GRANDFATHERING_DATE = LocalDate.of(2018, 1, 31);
    
    // Tax rates
    private static final double STCG_EQUITY_RATE_PRE = 0.15;  // Pre 23-Jul-2024
    private static final double STCG_EQUITY_RATE_POST = 0.20; // Post 23-Jul-2024
    private static final double LTCG_112A_RATE_PRE = 0.10;    // Pre 23-Jul-2024
    private static final double LTCG_112A_RATE_POST = 0.125;  // Post 23-Jul-2024
    private static final double LTCG_112_RATE_PRE = 0.20;     // With indexation
    private static final double LTCG_112_RATE_POST = 0.125;   // Without indexation
    
    // Exemption limits
    private static final double LTCG_112A_EXEMPTION_PRE = 100000;  // ₹1,00,000
    private static final double LTCG_112A_EXEMPTION_POST = 125000; // ₹1,25,000
    
    // Cost Inflation Index (CII) - CBDT Official Table
    // Base Year: 2001-02 (Index = 100)
    private static final Map<Integer, Double> CII_TABLE = new TreeMap<>();
    static {
        // FY 2001-02 to FY 2012-13
        CII_TABLE.put(2001, 100.0);
        CII_TABLE.put(2002, 105.0);
        CII_TABLE.put(2003, 109.0);
        CII_TABLE.put(2004, 113.0);
        CII_TABLE.put(2005, 117.0);
        CII_TABLE.put(2006, 122.0);
        CII_TABLE.put(2007, 129.0);
        CII_TABLE.put(2008, 137.0);
        CII_TABLE.put(2009, 148.0);
        CII_TABLE.put(2010, 167.0);
        CII_TABLE.put(2011, 184.0);
        CII_TABLE.put(2012, 200.0);
        CII_TABLE.put(2013, 220.0);
        // FY 2013-14 onwards (revised base)
        CII_TABLE.put(2014, 240.0);
        CII_TABLE.put(2015, 254.0);
        CII_TABLE.put(2016, 264.0);
        CII_TABLE.put(2017, 272.0);
        CII_TABLE.put(2018, 280.0);
        CII_TABLE.put(2019, 289.0);
        CII_TABLE.put(2020, 301.0);
        CII_TABLE.put(2021, 317.0);
        CII_TABLE.put(2022, 331.0);
        CII_TABLE.put(2023, 348.0);
        CII_TABLE.put(2024, 363.0);  // FY 2024-25
        CII_TABLE.put(2025, 383.0);  // FY 2025-26 (expected)
    }
    
    /**
     * Get CII for a given financial year
     */
    public double getCII(int financialYear) {
        return CII_TABLE.getOrDefault(financialYear, 1.0);
    }
    
    /**
     * Get FY from assessment year
     */
    public int getFinancialYearFromAY(String assessmentYear) {
        // AY 2025-26 -> FY 2024-25
        String[] parts = assessmentYear.split("-");
        return Integer.parseInt(parts[0]);
    }
    
    /**
     * Calculate indexed cost of acquisition using CII
     * Formula: Indexed Cost = Actual Cost × (CII of year of transfer / CII of year of acquisition)
     * 
     * For property acquired before 01-04-2001, use CII of FY 2001-02 as per CBDT Rule 11AA
     */
    public double calculateIndexedCost(double actualCost, int acquisitionFY, int transferFY, boolean isOldProperty) {
        // For property acquired before 01-04-2001, use FY 2001-02 CII
        int actualAcquisitionFY = isOldProperty ? 2001 : acquisitionFY;
        
        double ciiAcquisition = getCII(actualAcquisitionFY);
        double ciiTransfer = getCII(transferFY);
        
        if (ciiAcquisition == 0) {
            log.warn("CII not available for FY {}, using 1", acquisitionFY);
            ciiAcquisition = 1.0;
        }
        
        double indexedCost = actualCost * (ciiTransfer / ciiAcquisition);
        log.info("Indexed Cost: {} × ({}/{}) = {}", actualCost, ciiTransfer, ciiAcquisition, indexedCost);
        
        return indexedCost;
    }
    
    /**
     * Calculate long-term capital gain with indexation
     * Section 48: LTCG = Sale Consideration - (Indexed Cost of Acquisition + Indexed Cost of Improvement + Transfer Expenses)
     * 
     * If sale is post-Budget 2024 (23-Jul-2024), indexation optional - taxpayer can choose better of:
     * - With indexation: 20% tax
     * - Without indexation: 12.5% tax
     */
    public double calculateLTCGWithIndexation(
            double saleConsideration,
            double costOfAcquisition,
            double costOfImprovement,
            int acquisitionFY,
            int transferFY,
            boolean isOldProperty) {
        
        // Calculate indexed cost of acquisition
        double indexedCostAcquisition = calculateIndexedCost(costOfAcquisition, acquisitionFY, transferFY, isOldProperty);
        
        // Calculate indexed cost of improvement (if any)
        double indexedCostImprovement = 0;
        if (costOfImprovement > 0) {
            // Improvement cost is indexed from FY of improvement, not original acquisition
            int improvementFY = acquisitionFY + 2; // Assuming improvement done 2 years after acquisition
            indexedCostImprovement = calculateIndexedCost(costOfImprovement, improvementFY, transferFY, false);
        }
        
        // Total indexed cost
        double totalIndexedCost = indexedCostAcquisition + indexedCostImprovement;
        
        // Long term capital gain
        double ltcg = Math.max(0, saleConsideration - totalIndexedCost);
        
        log.info("LTCG Calculation: Sale {} - Indexed Cost {} = {}", saleConsideration, totalIndexedCost, ltcg);
        
        return ltcg;
    }
    
    /**
     * Calculate LTCG without indexation (for comparison with Budget 2024 option)
     * Section 48 alternative: Sale Consideration - (Cost of Acquisition + Cost of Improvement)
     */
    public double calculateLTCGWithoutIndexation(
            double saleConsideration,
            double costOfAcquisition,
            double costOfImprovement) {
        
        double totalCost = costOfAcquisition + costOfImprovement;
        double ltcg = Math.max(0, saleConsideration - totalCost);
        
        log.info("LTCG (no indexation): Sale {} - Cost {} = {}", saleConsideration, totalCost, ltcg);
        
        return ltcg;
    }
    
    /**
     * Choose optimal LTCG calculation (with or without indexation)
     * For transfers after 23-Jul-2024, taxpayer can choose better option
     */
    public LTCGCalculation chooseOptimalLTCG(
            double saleConsideration,
            double costOfAcquisition,
            double costOfImprovement,
            int acquisitionFY,
            int transferFY,
            boolean isOldProperty,
            boolean isPostJul2024) {
        
        // Calculate with indexation
        double ltcgWithIndexation = calculateLTCGWithIndexation(
                saleConsideration, costOfAcquisition, costOfImprovement,
                acquisitionFY, transferFY, isOldProperty);
        
        double taxWithIndexation = ltcgWithIndexation * LTCG_112_RATE_PRE;  // 20%
        
        // Calculate without indexation
        double ltcgWithoutIndexation = calculateLTCGWithoutIndexation(
                saleConsideration, costOfAcquisition, costOfImprovement);
        
        double taxWithoutIndexation = ltcgWithoutIndexation * LTCG_112_RATE_POST;  // 12.5%
        
        // Choose optimal
        if (isPostJul2024) {
            // Post July 2024: allow taxpayer to choose better option
            if (taxWithoutIndexation < taxWithIndexation) {
                return new LTCGCalculation(ltcgWithoutIndexation, false, taxWithoutIndexation);
            } else {
                return new LTCGCalculation(ltcgWithIndexation, true, taxWithIndexation);
            }
        } else {
            // Pre July 2024: indexation mandatory
            return new LTCGCalculation(ltcgWithIndexation, true, taxWithIndexation);
        }
    }
    
    /**
     * Result wrapper for optimal LTCG calculation
     */
    public static class LTCGCalculation {
        public final double ltcgAmount;
        public final boolean usedIndexation;
        public final double taxAmount;
        
        public LTCGCalculation(double ltcgAmount, boolean usedIndexation, double taxAmount) {
            this.ltcgAmount = ltcgAmount;
            this.usedIndexation = usedIndexation;
            this.taxAmount = taxAmount;
        }
    }

    /**
     * Compute capital gains with proper set-off rules
     * CBDT Rules:
     * 1. STCG can be set off against LTCG
     * 2. LTCG cannot be set off against STCG
     * 3. Intra-head set-off before inter-head set-off
     */
    public CapitalGainsResult computeCapitalGains(
            double stcgEquityPre, double stcgEquityPost,
            double ltcg112APre, double ltcg112APost,
            double ltcg112Pre, double ltcg112Post,
            double stcgOther, double ltcgOther) {
        
        CapitalGainsResult result = new CapitalGainsResult();
        
        // Step 1: Compute gross gains
        result.grossSTCGEquityPre = stcgEquityPre;
        result.grossSTCGEquityPost = stcgEquityPost;
        result.grossLTCG112APre = ltcg112APre;
        result.grossLTCG112APost = ltcg112APost;
        result.grossLTCG112Pre = ltcg112Pre;
        result.grossLTCG112Post = ltcg112Post;
        result.grossSTCGOther = stcgOther;
        result.grossLTCGOther = ltcgOther;
        
        // Step 2: Intra-head set-off (within same category)
        // STCG Equity set-off
        double totalSTCGEquity = stcgEquityPre + stcgEquityPost;
        if (totalSTCGEquity < 0) {
            // Loss in STCG equity - can be set off against LTCG equity
            double stcgLoss = Math.abs(totalSTCGEquity);
            
            // Set off against LTCG 112A first
            if (ltcg112APre > 0) {
                double setOff = Math.min(stcgLoss, ltcg112APre);
                result.grossLTCG112APre -= setOff;
                stcgLoss -= setOff;
                result.setOffSTCGAgainstLTCG112APre = setOff;
            }
            
            if (stcgLoss > 0 && ltcg112APost > 0) {
                double setOff = Math.min(stcgLoss, ltcg112APost);
                result.grossLTCG112APost -= setOff;
                stcgLoss -= setOff;
                result.setOffSTCGAgainstLTCG112APost = setOff;
            }
            
            // Remaining loss carried forward
            if (stcgLoss > 0) {
                result.stcgLossCarriedForward = stcgLoss;
            }
            
            result.netSTCGEquity = 0;
        } else {
            result.netSTCGEquity = totalSTCGEquity;
        }
        
        // Step 3: Apply exemption limits for LTCG 112A
        double totalLTCG112A = result.grossLTCG112APre + result.grossLTCG112APost;
        
        if (totalLTCG112A > 0) {
            // Pre-budget exemption
            if (result.grossLTCG112APre > 0) {
                double exemption = Math.min(result.grossLTCG112APre, LTCG_112A_EXEMPTION_PRE);
                result.ltcg112AExemptionPre = exemption;
                result.taxableLTCG112APre = result.grossLTCG112APre - exemption;
            }
            
            // Post-budget exemption (only if pre-budget exemption not fully utilized)
            if (result.grossLTCG112APost > 0) {
                double remainingExemption = LTCG_112A_EXEMPTION_POST - result.ltcg112AExemptionPre;
                if (remainingExemption > 0) {
                    double exemption = Math.min(result.grossLTCG112APost, remainingExemption);
                    result.ltcg112AExemptionPost = exemption;
                    result.taxableLTCG112APost = result.grossLTCG112APost - exemption;
                } else {
                    result.taxableLTCG112APost = result.grossLTCG112APost;
                }
            }
        }
        
        // Step 4: Compute tax
        result.taxOnSTCGEquityPre = result.netSTCGEquity > 0 ? 
                (stcgEquityPre / totalSTCGEquity) * result.netSTCGEquity * STCG_EQUITY_RATE_PRE : 0;
        result.taxOnSTCGEquityPost = result.netSTCGEquity > 0 ? 
                (stcgEquityPost / totalSTCGEquity) * result.netSTCGEquity * STCG_EQUITY_RATE_POST : 0;
        
        result.taxOnLTCG112APre = result.taxableLTCG112APre * LTCG_112A_RATE_PRE;
        result.taxOnLTCG112APost = result.taxableLTCG112APost * LTCG_112A_RATE_POST;
        
        result.taxOnLTCG112Pre = ltcg112Pre * LTCG_112_RATE_PRE;
        result.taxOnLTCG112Post = ltcg112Post * LTCG_112_RATE_POST;
        
        result.totalCapitalGainsTax = result.taxOnSTCGEquityPre + result.taxOnSTCGEquityPost +
                result.taxOnLTCG112APre + result.taxOnLTCG112APost +
                result.taxOnLTCG112Pre + result.taxOnLTCG112Post;
        
        return result;
    }

    /**
     * Apply grandfathering for shares purchased before 31-Jan-2018
     */
    public double applyGrandfathering(double costOfAcquisition, double fmvAsOn31Jan2018, 
                                      double salePrice) {
        // Use higher of cost or FMV as on 31-Jan-2018
        double effectiveCost = Math.max(costOfAcquisition, fmvAsOn31Jan2018);
        
        // Gain = Sale Price - Effective Cost
        double gain = salePrice - effectiveCost;
        
        // Gain cannot be negative due to grandfathering
        return Math.max(0, gain);
    }

    @lombok.Data
    public static class CapitalGainsResult {
        // Gross gains
        private double grossSTCGEquityPre;
        private double grossSTCGEquityPost;
        private double grossLTCG112APre;
        private double grossLTCG112APost;
        private double grossLTCG112Pre;
        private double grossLTCG112Post;
        private double grossSTCGOther;
        private double grossLTCGOther;
        
        // Set-off details
        private double setOffSTCGAgainstLTCG112APre;
        private double setOffSTCGAgainstLTCG112APost;
        private double stcgLossCarriedForward;
        
        // Net gains after set-off
        private double netSTCGEquity;
        
        // Exemptions
        private double ltcg112AExemptionPre;
        private double ltcg112AExemptionPost;
        
        // Taxable gains
        private double taxableLTCG112APre;
        private double taxableLTCG112APost;
        
        // Tax computation
        private double taxOnSTCGEquityPre;
        private double taxOnSTCGEquityPost;
        private double taxOnLTCG112APre;
        private double taxOnLTCG112APost;
        private double taxOnLTCG112Pre;
        private double taxOnLTCG112Post;
        private double totalCapitalGainsTax;
    }
}
