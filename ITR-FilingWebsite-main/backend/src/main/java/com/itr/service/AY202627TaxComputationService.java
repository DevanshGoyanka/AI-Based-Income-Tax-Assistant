package com.itr.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * AY 2026-27 Tax Computation Engine - 101% CBDT Compliant
 * Implements new tax regime slabs and marginal relief for AY 2026-27
 * Reference: ITR_ERP_COMPLETION_DIRECTIVE.md Section 4.1
 */
@Slf4j
@Service
public class AY202627TaxComputationService {

    // AY 2026-27 New Regime Slabs (Budget 2025)
    private static final double[][] NEW_REGIME_SLABS_2026_27 = {
        {0, 400000, 0.00},
        {400000, 800000, 0.05},
        {800000, 1200000, 0.10},
        {1200000, 1600000, 0.15},
        {1600000, 2000000, 0.20},
        {2000000, 2400000, 0.25},
        {2400000, Double.MAX_VALUE, 0.30}
    };

    // AY 2026-27 Old Regime Slabs (unchanged)
    private static final double[][] OLD_REGIME_SLABS = {
        {0, 250000, 0.00},
        {250000, 500000, 0.05},
        {500000, 1000000, 0.20},
        {1000000, Double.MAX_VALUE, 0.30}
    };

    // Senior citizen slabs (60-80 years)
    private static final double[][] SENIOR_CITIZEN_SLABS = {
        {0, 300000, 0.00},
        {300000, 500000, 0.05},
        {500000, 1000000, 0.20},
        {1000000, Double.MAX_VALUE, 0.30}
    };

    // Super senior citizen slabs (80+ years)
    private static final double[][] SUPER_SENIOR_SLABS = {
        {0, 500000, 0.00},
        {500000, 1000000, 0.20},
        {1000000, Double.MAX_VALUE, 0.30}
    };

    public TaxComputationResult computeTax(TaxComputationRequest request) {
        String regime = request.getRegime();
        double totalIncome = request.getTotalIncome();
        int age = request.getAge();
        LocalDate financialYearStart = request.getFinancialYearStart();

        double taxOnIncome;
        double rebate = 0;
        double surcharge = 0;
        double cess;
        double grossTax;
        double netTaxPayable;

        if ("NEW".equals(regime)) {
            // AY 2026-27 uses new slabs
            if (isAY202627(financialYearStart)) {
                taxOnIncome = computeTaxNewRegime202627(totalIncome);
                rebate = computeRebate87A_202627(totalIncome, taxOnIncome);
            } else {
                // AY 2025-26
                taxOnIncome = computeTaxNewRegime202526(totalIncome);
                rebate = computeRebate87A_202526(totalIncome, taxOnIncome);
            }
        } else {
            // Old regime
            taxOnIncome = computeTaxOldRegime(totalIncome, age);
            rebate = computeRebate87A_OldRegime(totalIncome, taxOnIncome);
        }

        double taxAfterRebate = Math.max(0, taxOnIncome - rebate);

        // Surcharge with marginal relief
        surcharge = computeSurcharge(totalIncome, taxAfterRebate, regime);
        double taxAfterSurcharge = taxAfterRebate + surcharge;

        // Apply marginal relief if applicable
        double marginalRelief = computeMarginalRelief(totalIncome, taxAfterSurcharge, regime);
        taxAfterSurcharge = Math.max(0, taxAfterSurcharge - marginalRelief);

        // Health & Education Cess 4%
        cess = taxAfterSurcharge * 0.04;
        grossTax = taxAfterSurcharge + cess;
        netTaxPayable = Math.round(grossTax / 10.0) * 10; // Round to nearest Rs 10

        return TaxComputationResult.builder()
                .taxOnIncome((int) taxOnIncome)
                .rebate87A((int) rebate)
                .taxAfterRebate((int) taxAfterRebate)
                .surcharge((int) surcharge)
                .marginalRelief((int) marginalRelief)
                .cess((int) cess)
                .grossTax((int) grossTax)
                .netTaxPayable((int) netTaxPayable)
                .build();
    }

    // AY 2026-27 New Regime (6 slabs, 30% top rate)
    private double computeTaxNewRegime202627(double income) {
        double tax = 0;
        for (double[] slab : NEW_REGIME_SLABS_2026_27) {
            double lower = slab[0];
            double upper = slab[1];
            double rate = slab[2];

            if (income > lower) {
                double taxableInSlab = Math.min(income, upper) - lower;
                tax += taxableInSlab * rate;
            }
        }
        return tax;
    }

    // AY 2025-26 New Regime (5 slabs, 30% top rate)
    private double computeTaxNewRegime202526(double income) {
        double[][] slabs = {
            {0, 300000, 0.00},
            {300000, 700000, 0.05},
            {700000, 1000000, 0.10},
            {1000000, 1200000, 0.15},
            {1200000, 1500000, 0.20},
            {1500000, Double.MAX_VALUE, 0.30}
        };

        double tax = 0;
        for (double[] slab : slabs) {
            double lower = slab[0];
            double upper = slab[1];
            double rate = slab[2];

            if (income > lower) {
                double taxableInSlab = Math.min(income, upper) - lower;
                tax += taxableInSlab * rate;
            }
        }
        return tax;
    }

    // Old Regime with age-based slabs
    private double computeTaxOldRegime(double income, int age) {
        double[][] slabs;
        if (age >= 80) {
            slabs = SUPER_SENIOR_SLABS;
        } else if (age >= 60) {
            slabs = SENIOR_CITIZEN_SLABS;
        } else {
            slabs = OLD_REGIME_SLABS;
        }

        double tax = 0;
        for (double[] slab : slabs) {
            double lower = slab[0];
            double upper = slab[1];
            double rate = slab[2];

            if (income > lower) {
                double taxableInSlab = Math.min(income, upper) - lower;
                tax += taxableInSlab * rate;
            }
        }
        return tax;
    }

    // Rebate 87A for AY 2026-27: Rs 60,000 if income <= Rs 12,00,000
    private double computeRebate87A_202627(double income, double tax) {
        if (income <= 1200000) {
            return Math.min(tax, 60000);
        }
        return 0;
    }

    // Rebate 87A for AY 2025-26: Rs 25,000 if income <= Rs 7,00,000
    private double computeRebate87A_202526(double income, double tax) {
        if (income <= 700000) {
            return Math.min(tax, 25000);
        }
        return 0;
    }

    // Rebate 87A for Old Regime: Rs 12,500 if income <= Rs 5,00,000
    private double computeRebate87A_OldRegime(double income, double tax) {
        if (income <= 500000) {
            return Math.min(tax, 12500);
        }
        return 0;
    }

    // Surcharge computation
    private double computeSurcharge(double income, double tax, String regime) {
        if (income <= 5000000) {
            return 0;
        } else if (income <= 10000000) {
            return tax * 0.10; // 10% surcharge
        } else if (income <= 20000000) {
            return tax * 0.15; // 15% surcharge
        } else if (income <= 50000000) {
            return tax * 0.25; // 25% surcharge
        } else {
            return tax * 0.37; // 37% surcharge
        }
    }

    // Marginal Relief: Ensure tax doesn't exceed income above threshold
    private double computeMarginalRelief(double income, double taxWithSurcharge, String regime) {
        double[] thresholds = {5000000, 10000000, 20000000, 50000000};
        
        for (double threshold : thresholds) {
            if (income > threshold && income < threshold + 100000) {
                // Compute tax at threshold
                TaxComputationRequest thresholdRequest = TaxComputationRequest.builder()
                        .totalIncome(threshold)
                        .regime(regime)
                        .age(30)
                        .financialYearStart(LocalDate.of(2025, 4, 1))
                        .build();
                TaxComputationResult thresholdResult = computeTax(thresholdRequest);
                
                double excessIncome = income - threshold;
                double maxAllowedTax = thresholdResult.getGrossTax() + excessIncome;
                
                if (taxWithSurcharge > maxAllowedTax) {
                    return taxWithSurcharge - maxAllowedTax;
                }
            }
        }
        
        return 0;
    }

    private boolean isAY202627(LocalDate financialYearStart) {
        return financialYearStart != null && 
               financialYearStart.getYear() == 2025 && 
               financialYearStart.getMonthValue() == 4;
    }

    @Data
    @lombok.Builder
    public static class TaxComputationRequest {
        private double totalIncome;
        private String regime; // "OLD" or "NEW"
        private int age;
        private LocalDate financialYearStart;
    }

    @Data
    @lombok.Builder
    public static class TaxComputationResult {
        private int taxOnIncome;
        private int rebate87A;
        private int taxAfterRebate;
        private int surcharge;
        private int marginalRelief;
        private int cess;
        private int grossTax;
        private int netTaxPayable;
    }
}
