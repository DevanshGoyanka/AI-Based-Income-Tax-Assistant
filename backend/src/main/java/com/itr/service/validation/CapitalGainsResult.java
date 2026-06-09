package com.itr.service.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Capital Gains Computation Result - 101% CBDT Compliant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapitalGainsResult {
    // Input values
    private double inputStcgEquityPre;
    private double inputStcgEquityPost;
    private double inputLtcg112APre;
    private double inputLtcg112APost;
    private double inputLtcg112Pre;
    private double inputLtcg112Post;
    private double inputStcgOther;
    private double inputLtcgOther;
    
    // Gross totals
    private double grossTotalSTCG;
    private double grossTotalLTCG;
    
    // Set-off details
    private double stcgLossSetOffAgainstLTCG;
    private double ltcgLossSetOffAgainstSTCG;
    
    // Net gains after set-off
    private double netSTCG;
    private double netLTCG;
    
    // Exemptions
    private double ltcg112AExemptionPre;
    private double ltcg112AExemptionPost;
    
    // Taxable gains
    private double taxableLTCG112A;
    private double taxableLTCG112WithIndex;
    private double taxableLTCG112WithoutIndex;
    private double taxableLTCGOther;
    
    // Tax computation
    private double taxOnSTCGEquityPre;
    private double taxOnSTCGEquityPost;
    private double taxOnSTCGOther;
    private double taxOnLTCG112APre;
    private double taxOnLTCG112APost;
    private double taxOnLTCG112WithIndex;
    private double taxOnLTCG112WithoutIndex;
    private double taxOnLTCGOther;
    private double totalCGTax;
    
    // Loss carry forward
    private double stcgLossCarriedForward;
    private double ltcgLossCarriedForward;
}
