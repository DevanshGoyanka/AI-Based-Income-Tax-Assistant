package com.itr.service;

import com.itr.dto.Itr2FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AMT (Alternate Minimum Tax) Service - Section 115JC
 * 101% CBDT Compliant
 * 
 * Applicable to: Individuals/HUFs/firms (NOT companies) who claim specified deductions
 * AMT Rate: 18.5% of Adjusted Total Income (ATI)
 * Threshold: ATI > ₹20,00,000
 * 
 * ATI = Total Income + Deduction u/s 10AA + Deduction u/s 35AD + Deductions 80H to 80RRB (except 80P)
 * 
 * AMT Credit u/s 115JD: Carried forward for 15 years
 */
@Slf4j
@Service
public class AMTService {

    private static final double AMT_RATE = 0.185; // 18.5%
    private static final double AMT_THRESHOLD = 2000000; // ₹20,00,000
    private static final int AMT_CREDIT_CARRY_FORWARD_YEARS = 15;

    /**
     * Compute AMT for ITR-2/3/4
     */
    public Itr2FormData.ScheduleAMT computeAMT(double totalIncome, 
                                                double deduction10AA,
                                                double deduction35AD,
                                                double deductions80HTo80RRB,
                                                double regularTax,
                                                String regime,
                                                List<AMTCreditEntry> broughtForwardAMTCredit) {
        
        log.info("Computing AMT - Total Income: ₹{}, Regular Tax: ₹{}", totalIncome, regularTax);
        
        var amt = Itr2FormData.ScheduleAMT.builder().build();
        amt.setTotalIncome(totalIncome);
        amt.setRegularTax(regularTax);
        
        // AMT not applicable in new regime
        if ("NEW".equals(regime)) {
            amt.setAmtApplicable(false);
            amt.setTaxPayable(regularTax);
            log.info("AMT not applicable - New regime selected");
            return amt;
        }
        
        // Calculate Adjusted Total Income (ATI)
        double addback10AA = deduction10AA;
        double addback35AD = deduction35AD;
        double addback80HTo80RRB = deductions80HTo80RRB;
        
        double ati = totalIncome + addback10AA + addback35AD + addback80HTo80RRB;
        
        amt.setAddback10AA(addback10AA);
        amt.setAddback35AD(addback35AD);
        amt.setAddback80HTo80RRB(addback80HTo80RRB);
        amt.setAdjustedTotalIncome(ati);
        
        // Check if AMT threshold exceeded
        if (ati <= AMT_THRESHOLD) {
            amt.setAmtApplicable(false);
            amt.setTaxPayable(regularTax);
            log.info("AMT not applicable - ATI (₹{}) ≤ threshold (₹{})", ati, AMT_THRESHOLD);
            return amt;
        }
        
        // Calculate AMT @ 18.5%
        double amtAmount = ati * AMT_RATE;
        amt.setAmtAt185Percent(amtAmount);
        
        // Compare AMT with regular tax
        if (amtAmount > regularTax) {
            // AMT applicable - pay AMT
            amt.setAmtApplicable(true);
            amt.setTaxPayable(amtAmount);
            
            // AMT Credit = AMT - Regular Tax (can be carried forward for 15 years)
            double amtCredit = amtAmount - regularTax;
            amt.setAmtCreditCarryForward(amtCredit);
            
            log.info("AMT APPLICABLE - AMT: ₹{} > Regular Tax: ₹{}. AMT Credit: ₹{}", 
                    amtAmount, regularTax, amtCredit);
        } else {
            // Regular tax higher - pay regular tax
            amt.setAmtApplicable(false);
            amt.setTaxPayable(regularTax);
            
            // Check if brought forward AMT credit can be utilized
            if (broughtForwardAMTCredit != null && !broughtForwardAMTCredit.isEmpty()) {
                double totalBFCredit = broughtForwardAMTCredit.stream()
                        .mapToDouble(AMTCreditEntry::getCreditRemaining)
                        .sum();
                
                // AMT credit can be set off when regular tax > AMT
                double creditUtilizable = Math.min(totalBFCredit, regularTax - amtAmount);
                
                if (creditUtilizable > 0) {
                    amt.setAmtCreditBroughtForward(totalBFCredit);
                    amt.setAmtCreditUtilized(creditUtilizable);
                    amt.setAmtCreditCarryForward(totalBFCredit - creditUtilizable);
                    amt.setTaxPayable(regularTax - creditUtilizable);
                    
                    log.info("AMT Credit utilized: ₹{} from brought forward ₹{}", 
                            creditUtilizable, totalBFCredit);
                }
            }
            
            log.info("Regular tax applicable - Regular Tax: ₹{} > AMT: ₹{}", regularTax, amtAmount);
        }
        
        return amt;
    }

    /**
     * AMT Credit Entry for tracking brought forward credits
     */
    public static class AMTCreditEntry {
        private String assessmentYear;
        private double creditAmount;
        private double creditRemaining;
        private int yearsRemaining;
        
        public AMTCreditEntry(String assessmentYear, double creditAmount, double creditRemaining, int yearsRemaining) {
            this.assessmentYear = assessmentYear;
            this.creditAmount = creditAmount;
            this.creditRemaining = creditRemaining;
            this.yearsRemaining = yearsRemaining;
        }
        
        public String getAssessmentYear() { return assessmentYear; }
        public double getCreditAmount() { return creditAmount; }
        public double getCreditRemaining() { return creditRemaining; }
        public int getYearsRemaining() { return yearsRemaining; }
    }
}
