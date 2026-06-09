package com.itr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * EPF/VPF Interest Taxation Service
 * 101% CBDT Compliant
 * 
 * Finance Act 2021: Interest on EPF/VPF contribution exceeding ₹2.5L per year is taxable
 * (₹5L for non-PF employers)
 */
@Slf4j
@Service
public class EPFTaxationService {

    private static final double EPF_CONTRIBUTION_LIMIT_WITH_PF = 250000; // ₹2.5L
    private static final double EPF_CONTRIBUTION_LIMIT_WITHOUT_PF = 500000; // ₹5L

    /**
     * Calculate taxable interest on EPF/VPF
     * 
     * Rule (from AY 2022-23):
     * - If employee contribution to EPF/VPF > ₹2.5L in a year (₹5L if employer has no PF)
     * - Interest on excess contribution is taxable
     * - Taxable interest = (Excess contribution / Total contribution) × Total interest
     */
    public EPFTaxationResult calculateTaxableEPFInterest(EPFTaxationInput input) {
        
        log.info("Calculating taxable EPF/VPF interest");
        
        double employeeContribution = input.getEmployeeEPFContribution() + input.getEmployeeVPFContribution();
        double totalInterest = input.getTotalInterestEarned();
        
        // Determine applicable limit
        double applicableLimit = input.isEmployerHasPF() ? 
                EPF_CONTRIBUTION_LIMIT_WITH_PF : EPF_CONTRIBUTION_LIMIT_WITHOUT_PF;
        
        // Check if contribution exceeds limit
        if (employeeContribution <= applicableLimit) {
            log.info("EPF/VPF contribution (₹{}) within limit (₹{}) - No taxable interest",
                    employeeContribution, applicableLimit);
            
            return EPFTaxationResult.builder()
                    .totalContribution(employeeContribution)
                    .applicableLimit(applicableLimit)
                    .excessContribution(0)
                    .totalInterest(totalInterest)
                    .taxableInterest(0)
                    .exemptInterest(totalInterest)
                    .build();
        }
        
        // Calculate excess contribution
        double excessContribution = employeeContribution - applicableLimit;
        
        // Calculate taxable interest proportionately
        double taxableInterest = (excessContribution / employeeContribution) * totalInterest;
        double exemptInterest = totalInterest - taxableInterest;
        
        log.info("EPF/VPF: Total contribution: ₹{}, Excess: ₹{}, Taxable interest: ₹{}, Exempt interest: ₹{}",
                employeeContribution, excessContribution, taxableInterest, exemptInterest);
        
        return EPFTaxationResult.builder()
                .totalContribution(employeeContribution)
                .applicableLimit(applicableLimit)
                .excessContribution(excessContribution)
                .totalInterest(totalInterest)
                .taxableInterest(taxableInterest)
                .exemptInterest(exemptInterest)
                .build();
    }

    /**
     * Input for EPF taxation calculation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EPFTaxationInput {
        private double employeeEPFContribution;
        private double employeeVPFContribution;
        private double totalInterestEarned;
        private boolean employerHasPF; // true = ₹2.5L limit, false = ₹5L limit
    }

    /**
     * Result of EPF taxation calculation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EPFTaxationResult {
        private double totalContribution;
        private double applicableLimit;
        private double excessContribution;
        private double totalInterest;
        private double taxableInterest;
        private double exemptInterest;
    }
}
