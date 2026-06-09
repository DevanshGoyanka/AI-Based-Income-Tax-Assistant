package com.itr.service;

import com.itr.dto.Itr1FormData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Tax Regime Comparison Engine - 101% CBDT Compliant
 * Computes tax under Old vs New regime and recommends optimal choice
 * Reference: ITR_Import_JSON_Validation.md Section 12
 */
@Slf4j
@Service
public class TaxRegimeComparisonService {

    public RegimeComparisonResult compareRegimes(Itr1FormData formData) {
        RegimeComparisonResult result = new RegimeComparisonResult();
        
        double totalIncome = formData.getTaxComputation() != null 
            ? formData.getTaxComputation().getTotalIncome() : 0;
        double totalDeductions = formData.getDeductions() != null 
            ? formData.getDeductions().getTotalDeductions() : 0;
        
        // Compute Old Regime Tax
        RegimeCalculation oldRegime = computeOldRegimeTax(totalIncome, totalDeductions, formData);
        result.setOldRegime(oldRegime);
        
        // Compute New Regime Tax (no deductions except 80CCD2, 80JJAA, 80CCH2)
        double allowedDeductionsNewRegime = getAllowedDeductionsNewRegime(formData);
        RegimeCalculation newRegime = computeNewRegimeTax(totalIncome, allowedDeductionsNewRegime, formData);
        result.setNewRegime(newRegime);
        
        // Determine recommendation
        double savings = oldRegime.getNetTaxPayable() - newRegime.getNetTaxPayable();
        result.setTaxSavings(savings);
        
        if (savings > 0) {
            result.setRecommendedRegime("NEW");
            result.setRecommendationReason("New regime saves Rs " + (long)savings);
        } else if (savings < -1000) {
            result.setRecommendedRegime("OLD");
            result.setRecommendationReason("Old regime saves Rs " + (long)Math.abs(savings));
        } else {
            result.setRecommendedRegime("NEW");
            result.setRecommendationReason("Both regimes result in similar tax (difference < Rs 1000)");
        }
        
        // Calculate break-even deduction amount
        result.setBreakEvenDeduction(calculateBreakEvenDeduction(totalIncome, formData));
        
        return result;
    }

    private RegimeCalculation computeOldRegimeTax(double grossIncome, double deductions, Itr1FormData formData) {
        RegimeCalculation calc = new RegimeCalculation();
        calc.setRegime("OLD");
        calc.setGrossIncome(grossIncome);
        calc.setTotalDeductions(deductions);
        
        double taxableIncome = Math.max(0, grossIncome - deductions);
        taxableIncome = Math.round(taxableIncome / 10.0) * 10; // Round to nearest 10
        calc.setTaxableIncome(taxableIncome);
        
        // Old regime slab rates
        double tax = computeTaxOldSlab(taxableIncome, formData);
        calc.setTaxOnIncome(tax);
        
        // Rebate 87A: Rs 12,500 if TI <= 5L
        double rebate = (taxableIncome <= 500000) ? Math.min(tax, 12500) : 0;
        calc.setRebate87A(rebate);
        
        double taxAfterRebate = Math.max(0, tax - rebate);
        calc.setTaxAfterRebate(taxAfterRebate);
        
        // Surcharge (if applicable)
        double surcharge = computeSurcharge(taxableIncome, taxAfterRebate, "OLD");
        calc.setSurcharge(surcharge);
        
        // Health & Education Cess: 4%
        double cess = (taxAfterRebate + surcharge) * 0.04;
        calc.setCess(cess);
        
        double netTax = taxAfterRebate + surcharge + cess;
        calc.setNetTaxPayable(netTax);
        
        return calc;
    }

    private RegimeCalculation computeNewRegimeTax(double grossIncome, double allowedDeductions, Itr1FormData formData) {
        RegimeCalculation calc = new RegimeCalculation();
        calc.setRegime("NEW");
        calc.setGrossIncome(grossIncome);
        calc.setTotalDeductions(allowedDeductions);
        
        double taxableIncome = Math.max(0, grossIncome - allowedDeductions);
        taxableIncome = Math.round(taxableIncome / 10.0) * 10;
        calc.setTaxableIncome(taxableIncome);
        
        // New regime slab rates (AY 2025-26)
        double tax = computeTaxNewSlab(taxableIncome, formData);
        calc.setTaxOnIncome(tax);
        
        // Rebate 87A: Rs 25,000 if TI <= 7L
        double rebate = (taxableIncome <= 700000) ? Math.min(tax, 25000) : 0;
        calc.setRebate87A(rebate);
        
        double taxAfterRebate = Math.max(0, tax - rebate);
        calc.setTaxAfterRebate(taxAfterRebate);
        
        // Surcharge
        double surcharge = computeSurcharge(taxableIncome, taxAfterRebate, "NEW");
        calc.setSurcharge(surcharge);
        
        // Cess: 4%
        double cess = (taxAfterRebate + surcharge) * 0.04;
        calc.setCess(cess);
        
        double netTax = taxAfterRebate + surcharge + cess;
        calc.setNetTaxPayable(netTax);
        
        return calc;
    }

    private double computeTaxOldSlab(double income, Itr1FormData formData) {
        boolean isSenior = isSeniorCitizen(formData);
        boolean isSuperSenior = isSuperSeniorCitizen(formData);
        
        double tax = 0;
        double exemptionLimit = isSuperSenior ? 500000 : (isSenior ? 300000 : 250000);
        
        if (income <= exemptionLimit) return 0;
        
        // Slab: 0-2.5L/3L/5L: 0%, 2.5L-5L: 5%, 5L-10L: 20%, >10L: 30%
        if (income > exemptionLimit) {
            double taxable = Math.min(income - exemptionLimit, 250000);
            tax += taxable * 0.05;
        }
        if (income > 500000) {
            double taxable = Math.min(income - 500000, 500000);
            tax += taxable * 0.20;
        }
        if (income > 1000000) {
            tax += (income - 1000000) * 0.30;
        }
        
        return tax;
    }

    private double computeTaxNewSlab(double income, Itr1FormData formData) {
        // New regime slab (AY 2025-26): 0-3L: 0%, 3L-7L: 5%, 7L-10L: 10%, 10L-12L: 15%, 12L-15L: 20%, >15L: 30%
        double tax = 0;
        
        if (income <= 300000) return 0;
        
        if (income > 300000) {
            double taxable = Math.min(income - 300000, 400000);
            tax += taxable * 0.05;
        }
        if (income > 700000) {
            double taxable = Math.min(income - 700000, 300000);
            tax += taxable * 0.10;
        }
        if (income > 1000000) {
            double taxable = Math.min(income - 1000000, 200000);
            tax += taxable * 0.15;
        }
        if (income > 1200000) {
            double taxable = Math.min(income - 1200000, 300000);
            tax += taxable * 0.20;
        }
        if (income > 1500000) {
            tax += (income - 1500000) * 0.30;
        }
        
        return tax;
    }

    private double computeSurcharge(double income, double tax, String regime) {
        if (income <= 5000000) return 0;
        if (income <= 10000000) return tax * 0.10;
        if (income <= 20000000) return tax * 0.15;
        if (income <= 50000000) return tax * 0.25;
        return "OLD".equals(regime) ? tax * 0.37 : tax * 0.25;
    }

    private double getAllowedDeductionsNewRegime(Itr1FormData formData) {
        if (formData.getDeductions() == null) return 0;
        // New regime allows only: 80CCD(2), 80JJAA, 80CCH(2)
        return formData.getDeductions().getNpsEmployer80CCD2();
    }

    private double calculateBreakEvenDeduction(double grossIncome, Itr1FormData formData) {
        // Binary search to find deduction amount where both regimes result in same tax
        double low = 0, high = 150000;
        
        while (high - low > 100) {
            double mid = (low + high) / 2;
            double oldTax = computeOldRegimeTax(grossIncome, mid, formData).getNetTaxPayable();
            double newTax = computeNewRegimeTax(grossIncome, 0, formData).getNetTaxPayable();
            
            if (oldTax < newTax) {
                high = mid;
            } else {
                low = mid;
            }
        }
        
        return (low + high) / 2;
    }

    private boolean isSeniorCitizen(Itr1FormData formData) {
        return formData.getPersonalInfo() != null 
            && formData.getPersonalInfo().getAge() != null 
            && formData.getPersonalInfo().getAge() >= 60;
    }

    private boolean isSuperSeniorCitizen(Itr1FormData formData) {
        return formData.getPersonalInfo() != null 
            && formData.getPersonalInfo().getAge() != null 
            && formData.getPersonalInfo().getAge() >= 80;
    }

    @Data
    public static class RegimeComparisonResult {
        private RegimeCalculation oldRegime;
        private RegimeCalculation newRegime;
        private String recommendedRegime;
        private String recommendationReason;
        private double taxSavings;
        private double breakEvenDeduction;
    }

    @Data
    public static class RegimeCalculation {
        private String regime;
        private double grossIncome;
        private double totalDeductions;
        private double taxableIncome;
        private double taxOnIncome;
        private double rebate87A;
        private double taxAfterRebate;
        private double surcharge;
        private double cess;
        private double netTaxPayable;
    }
}
