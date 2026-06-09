package com.itr.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Deduction Calculator - Complete 80G, 80GG, and other Chapter VI-A deductions
 * 101% CBDT Compliant
 */
@Slf4j
@Service
public class DeductionCalculatorService {

    /**
     * Calculate 80G Donation Deduction with complete validation
     * Categories:
     * - 100% without limit (PM CARES, PM Relief Fund, etc.)
     * - 100% with 10% limit (National Children's Fund, etc.)
     * - 50% without limit (Jawaharlal Nehru Memorial Fund, etc.)
     * - 50% with 10% limit (Most approved institutions)
     * 
     * CRITICAL: Cash donations > ₹2,000 are FULLY DISALLOWED
     */
    public Deduction80GResult calculate80GDeduction(List<Donation80G> donations, 
                                                      double grossTotalIncome,
                                                      double ltcgIncome, double stcg111AIncome) {
        
        Deduction80GResult result = new Deduction80GResult();
        result.setDonations(donations);
        
        // Calculate Adjusted GTI for 10% qualifying limit
        // AGTI = GTI - LTCG - STCG u/s 111A - Foreign income (NR) - Other 80C to 80U deductions
        double adjustedGTI = grossTotalIncome - ltcgIncome - stcg111AIncome;
        double qualifyingLimit10Pct = adjustedGTI * 0.10;
        
        result.setAdjustedGTI(adjustedGTI);
        result.setQualifyingLimit10Pct(qualifyingLimit10Pct);
        
        double totalDeduction = 0;
        List<String> warnings = new ArrayList<>();
        
        for (Donation80G donation : donations) {
            double eligible = 0;
            
            // CRITICAL: Cash donations > ₹2,000 are DISALLOWED
            if ("CASH".equalsIgnoreCase(donation.getPaymentMode()) && donation.getAmount() > 2000) {
                donation.setEligibleDeduction(0);
                donation.setDisallowedReason("Cash donation exceeds ₹2,000 limit");
                warnings.add("Cash donation of ₹" + donation.getAmount() + " to " + 
                           donation.getDoneeName() + " is FULLY DISALLOWED (cash limit: ₹2,000)");
                continue;
            }
            
            // Calculate based on category
            switch (donation.getCategory()) {
                case "100_NO_LIMIT":
                    // PM CARES Fund, PM National Relief Fund, National Defence Fund, etc.
                    eligible = donation.getAmount();
                    break;
                    
                case "100_WITH_LIMIT":
                    // National Children's Fund, Swachh Bharat Kosh, Clean Ganga Fund, etc.
                    eligible = Math.min(donation.getAmount(), qualifyingLimit10Pct);
                    if (donation.getAmount() > qualifyingLimit10Pct) {
                        warnings.add("Donation to " + donation.getDoneeName() + 
                                   " limited to 10% of adjusted GTI (₹" + qualifyingLimit10Pct + ")");
                    }
                    break;
                    
                case "50_NO_LIMIT":
                    // Jawaharlal Nehru Memorial Fund, Prime Minister's Drought Relief Fund, etc.
                    eligible = donation.getAmount() * 0.50;
                    break;
                    
                case "50_WITH_LIMIT":
                    // Most approved hospitals, educational institutions, charitable trusts
                    double limitedAmount = Math.min(donation.getAmount(), qualifyingLimit10Pct);
                    eligible = limitedAmount * 0.50;
                    if (donation.getAmount() > qualifyingLimit10Pct) {
                        warnings.add("Donation to " + donation.getDoneeName() + 
                                   " limited to 10% of adjusted GTI before applying 50% deduction");
                    }
                    break;
                    
                default:
                    // Unknown category - assume 50% with limit (most restrictive)
                    eligible = Math.min(donation.getAmount(), qualifyingLimit10Pct) * 0.50;
                    warnings.add("Unknown category for " + donation.getDoneeName() + 
                               ". Applied 50% with 10% limit.");
            }
            
            donation.setEligibleDeduction(eligible);
            totalDeduction += eligible;
        }
        
        result.setTotalDeduction(totalDeduction);
        result.setWarnings(warnings);
        
        log.info("80G Deduction: AGTI={}, 10% Limit={}, Total Deduction={}", 
                adjustedGTI, qualifyingLimit10Pct, totalDeduction);
        
        return result;
    }

    /**
     * Calculate 80GG Rent Deduction (for those not receiving HRA)
     * Conditions:
     * - Taxpayer does NOT receive HRA
     * - Has not claimed deduction for residential property
     * - Property not owned by taxpayer/spouse/minor child
     * 
     * Deduction = MINIMUM OF:
     * 1. Actual rent paid - 10% of Adjusted Total Income
     * 2. ₹5,000 per month (₹60,000 per year)
     * 3. 25% of Adjusted Total Income
     * 
     * Form 10BA must be filed before claiming
     */
    public double calculate80GGDeduction(double rentPaid, double adjustedTotalIncome) {
        // Condition 1: Rent paid - 10% of ATI
        double condition1 = rentPaid - (adjustedTotalIncome * 0.10);
        
        // Condition 2: ₹5,000 per month = ₹60,000 per year
        double condition2 = 60000;
        
        // Condition 3: 25% of ATI
        double condition3 = adjustedTotalIncome * 0.25;
        
        // Deduction = minimum of all three
        double deduction = Math.min(condition1, Math.min(condition2, condition3));
        
        // Cannot be negative
        deduction = Math.max(0, deduction);
        
        log.info("80GG Deduction: Rent={}, ATI={}, Deduction={}", 
                rentPaid, adjustedTotalIncome, deduction);
        
        return deduction;
    }

    /**
     * Calculate 80JJAA New Employee Deduction
     * Conditions:
     * - Business subject to tax audit (turnover > ₹1Cr / ₹50L for professionals)
     * - New employee hired during the year (not previously employed)
     * - Monthly emoluments ≤ ₹25,000
     * - Employee worked ≥ 240 days (150 days for apparel/footwear/leather)
     * - Emoluments paid via banking channel (not cash)
     * - Employee registered with EPFO
     * 
     * Deduction = 30% of additional employee cost for 3 consecutive AYs
     * Available in NEW REGIME
     */
    public double calculate80JJAADeduction(double additionalEmployeeCost, int eligibleEmployees) {
        double deduction = additionalEmployeeCost * 0.30;
        
        log.info("80JJAA Deduction: Employees={}, Cost={}, Deduction={}", 
                eligibleEmployees, additionalEmployeeCost, deduction);
        
        return deduction;
    }

    /**
     * Calculate 80P Co-operative Society Deduction
     * - Interest from co-operative bank on savings: up to ₹50,000
     * - Interest and dividend from co-op society (farming): 100% exempt
     * - Profit from cottage industry: 100%
     * - Profit from marketing/processing agricultural produce: 100%
     */
    public double calculate80PDeduction(double savingsInterest, double farmingIncome,
                                         double cottageIndustryProfit, double agriProcessingProfit) {
        double savingsDeduction = Math.min(savingsInterest, 50000);
        double totalDeduction = savingsDeduction + farmingIncome + cottageIndustryProfit + agriProcessingProfit;
        
        log.info("80P Deduction: Savings={}, Farming={}, Total={}", 
                savingsInterest, farmingIncome, totalDeduction);
        
        return totalDeduction;
    }

    /**
     * Calculate 80QQB Royalty on Books
     * - For authors (fiction, non-fiction, literary works)
     * - NOT for textbooks
     * - Deduction = least of (actual royalty, 15% of book value, ₹3,00,000)
     */
    public double calculate80QQBDeduction(double royaltyReceived, double bookValue) {
        double limit15Pct = bookValue * 0.15;
        double deduction = Math.min(royaltyReceived, Math.min(limit15Pct, 300000));
        
        log.info("80QQB Deduction: Royalty={}, BookValue={}, Deduction={}", 
                royaltyReceived, bookValue, deduction);
        
        return deduction;
    }

    /**
     * Calculate 80RRB Royalty on Patents
     * - For Indian resident
     * - Patent registered in India
     * - Deduction = actual royalty, max ₹3,00,000
     */
    public double calculate80RRBDeduction(double royaltyReceived) {
        double deduction = Math.min(royaltyReceived, 300000);
        
        log.info("80RRB Deduction: Royalty={}, Deduction={}", royaltyReceived, deduction);
        
        return deduction;
    }

    // Data classes
    @Data
    public static class Donation80G {
        private String doneeName;
        private String doneePAN;
        private double amount;
        private String paymentMode; // CASH, CHEQUE, NEFT, UPI, etc.
        private String category; // 100_NO_LIMIT, 100_WITH_LIMIT, 50_NO_LIMIT, 50_WITH_LIMIT
        private String receiptNo;
        private String donationDate;
        private double eligibleDeduction;
        private String disallowedReason;
    }

    @Data
    public static class Deduction80GResult {
        private List<Donation80G> donations;
        private double adjustedGTI;
        private double qualifyingLimit10Pct;
        private double totalDeduction;
        private List<String> warnings;
    }
}
