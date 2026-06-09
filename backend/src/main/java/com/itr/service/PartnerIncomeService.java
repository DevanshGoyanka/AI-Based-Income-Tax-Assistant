package com.itr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Partner Income from Firm Service
 * 101% CBDT Compliant
 * 
 * Section 10(2A): Share of profit from partnership firm is EXEMPT in partner's hands
 * (Firm already paid tax on profit before distribution)
 * 
 * Salary/Interest/Remuneration to partner: TAXABLE in partner's hands
 */
@Slf4j
@Service
public class PartnerIncomeService {

    /**
     * Calculate partner's taxable income from firm
     * 
     * Treatment:
     * - Share of profit: EXEMPT u/s 10(2A)
     * - Salary/Interest/Remuneration: TAXABLE as business income
     */
    public PartnerIncomeResult calculatePartnerIncome(PartnerIncomeInput input) {
        
        log.info("Calculating partner income from firm");
        
        double shareOfProfit = input.getShareOfProfit();
        double salary = input.getSalaryFromFirm();
        double interest = input.getInterestOnCapital();
        double remuneration = input.getRemuneration();
        
        // Share of profit is exempt
        double exemptIncome = shareOfProfit;
        
        // Salary, interest, remuneration are taxable
        double taxableIncome = salary + interest + remuneration;
        
        log.info("Partner income: Share of profit (exempt): ₹{}, Taxable (salary+interest+remuneration): ₹{}",
                exemptIncome, taxableIncome);
        
        return PartnerIncomeResult.builder()
                .shareOfProfit(shareOfProfit)
                .salaryFromFirm(salary)
                .interestOnCapital(interest)
                .remuneration(remuneration)
                .exemptIncome(exemptIncome)
                .taxableIncome(taxableIncome)
                .exemptSection("10(2A)")
                .build();
    }

    /**
     * Input for partner income calculation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PartnerIncomeInput {
        private double shareOfProfit;
        private double salaryFromFirm;
        private double interestOnCapital;
        private double remuneration;
    }

    /**
     * Result of partner income calculation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PartnerIncomeResult {
        private double shareOfProfit;
        private double salaryFromFirm;
        private double interestOnCapital;
        private double remuneration;
        private double exemptIncome;
        private double taxableIncome;
        private String exemptSection;
    }
}
