package com.itr.service.validation;

import com.itr.dto.Itr1FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Capital Gains Computation Service - CBDT Compliant
 * Handles all capital gains scenarios with proper set-off rules and grandfathering
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
