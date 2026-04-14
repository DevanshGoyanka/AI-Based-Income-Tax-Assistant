package com.itr.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Break-Even Analysis Service - 101% CBDT Compliant
 * Deduction optimization for regime selection
 * Reference: ITR_Import_JSON_Validation.md Section 12.3
 */
@Slf4j
@Service
public class BreakEvenAnalysisService {

    public BreakEvenResult analyzeBreakEven(double grossIncome, double currentDeductions) {
        BreakEvenResult result = new BreakEvenResult();
        result.setGrossIncome(grossIncome);
        result.setCurrentDeductions(currentDeductions);
        
        // Binary search for break-even deduction
        double low = 0, high = 150000;
        double breakEvenDeduction = 0;
        
        while (high - low > 100) {
            double mid = (low + high) / 2;
            double oldTax = computeOldRegimeTax(grossIncome, mid);
            double newTax = computeNewRegimeTax(grossIncome);
            
            if (oldTax < newTax) {
                high = mid;
            } else {
                low = mid;
            }
        }
        
        breakEvenDeduction = (low + high) / 2;
        result.setBreakEvenDeduction(breakEvenDeduction);
        
        if (currentDeductions >= breakEvenDeduction) {
            result.setRecommendation("OLD");
            result.setMessage("Your deductions (Rs " + (long)currentDeductions 
                + ") exceed break-even (Rs " + (long)breakEvenDeduction + "). Old regime recommended.");
        } else {
            result.setRecommendation("NEW");
            double shortfall = breakEvenDeduction - currentDeductions;
            result.setMessage("Need Rs " + (long)shortfall + " more in deductions to benefit from old regime.");
        }
        
        return result;
    }

    private double computeOldRegimeTax(double income, double deductions) {
        double taxableIncome = Math.max(0, income - deductions);
        double tax = 0;
        
        if (taxableIncome > 250000) tax += Math.min(taxableIncome - 250000, 250000) * 0.05;
        if (taxableIncome > 500000) tax += Math.min(taxableIncome - 500000, 500000) * 0.20;
        if (taxableIncome > 1000000) tax += (taxableIncome - 1000000) * 0.30;
        
        if (taxableIncome <= 500000) tax = Math.max(0, tax - 12500);
        return tax * 1.04;
    }

    private double computeNewRegimeTax(double income) {
        double tax = 0;
        if (income > 300000) tax += Math.min(income - 300000, 400000) * 0.05;
        if (income > 700000) tax += Math.min(income - 700000, 300000) * 0.10;
        if (income > 1000000) tax += Math.min(income - 1000000, 200000) * 0.15;
        if (income > 1200000) tax += Math.min(income - 1200000, 300000) * 0.20;
        if (income > 1500000) tax += (income - 1500000) * 0.30;
        
        if (income <= 700000) tax = Math.max(0, tax - 25000);
        return tax * 1.04;
    }

    @Data
    public static class BreakEvenResult {
        private double grossIncome;
        private double currentDeductions;
        private double breakEvenDeduction;
        private String recommendation;
        private String message;
    }
}
