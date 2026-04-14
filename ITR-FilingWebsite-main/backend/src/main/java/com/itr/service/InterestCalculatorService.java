package com.itr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Interest and Fee Calculator - Sections 234A/B/C/F and 244A
 * 101% CBDT Compliant
 */
@Slf4j
@Service
public class InterestCalculatorService {

    /**
     * Section 234A - Late Filing Interest
     * Rate: 1% per month or part thereof
     * Base: Net tax payable after TDS/TCS/Advance Tax
     */
    public double calculate234A(double netTaxPayable, LocalDate dueDate, LocalDate filingDate) {
        if (netTaxPayable <= 0 || filingDate == null || dueDate == null) {
            return 0;
        }
        
        if (!filingDate.isAfter(dueDate)) {
            return 0; // Filed on time
        }
        
        // Calculate months from day after due date to filing date
        long days = ChronoUnit.DAYS.between(dueDate, filingDate);
        int months = (int) Math.ceil(days / 30.0);
        months = Math.max(1, months); // Minimum 1 month
        
        double interest = netTaxPayable * 0.01 * months;
        
        log.debug("234A: Tax={}, DueDate={}, FilingDate={}, Months={}, Interest={}", 
                netTaxPayable, dueDate, filingDate, months, interest);
        
        return Math.round(interest);
    }

    /**
     * Section 234B - Advance Tax Shortfall Interest
     * Rate: 1% per month or part thereof
     * Period: April 1 of AY to date of payment/filing
     * Threshold: Total tax < ₹10,000 → NO 234B
     * Exemption: Senior citizen (60+) with NO business income
     */
    public double calculate234B(double totalTax, double advanceTaxPaid, double tdsDeducted, 
                                 boolean isSeniorCitizen, boolean hasBusinessIncome, 
                                 LocalDate assessmentYearStart, LocalDate paymentDate) {
        
        // Threshold check
        if (totalTax < 10000) {
            return 0;
        }
        
        // Senior citizen exemption
        if (isSeniorCitizen && !hasBusinessIncome) {
            return 0;
        }
        
        // Calculate assessed tax (after TDS)
        double assessedTax = totalTax - tdsDeducted;
        
        // 90% threshold
        double threshold90Pct = assessedTax * 0.90;
        
        if (advanceTaxPaid >= threshold90Pct) {
            return 0; // Advance tax sufficient
        }
        
        // Calculate shortfall
        double shortfall = assessedTax - advanceTaxPaid;
        
        // Calculate months from April 1 of AY to payment date
        long days = ChronoUnit.DAYS.between(assessmentYearStart, paymentDate);
        int months = (int) Math.ceil(days / 30.0);
        
        double interest = shortfall * 0.01 * months;
        
        log.debug("234B: TotalTax={}, AdvTax={}, TDS={}, Shortfall={}, Months={}, Interest={}", 
                totalTax, advanceTaxPaid, tdsDeducted, shortfall, months, interest);
        
        return Math.round(interest);
    }

    /**
     * Section 234C - Installment Shortfall Interest
     * Installments: June 15 (15%), Sep 15 (45%), Dec 15 (75%), Mar 15 (100%)
     * Rate: 1% per month for each shortfall
     * Special: Presumptive income → only Mar 15 (100%)
     */
    public double calculate234C(double totalTax, double tdsDeducted, 
                                 InstallmentPayments payments, boolean isPresumptive) {
        
        if (totalTax < 10000) {
            return 0;
        }
        
        double netTax = totalTax - tdsDeducted;
        double totalInterest = 0;
        
        if (isPresumptive) {
            // Presumptive: Only March 15 installment (100%)
            double required = netTax;
            double paid = payments.getMarch15();
            
            if (paid < required) {
                double shortfall = required - paid;
                totalInterest = shortfall * 0.01 * 1; // 1 month (March only)
            }
        } else {
            // Regular taxpayer: All 4 installments
            
            // June 15: 15% required, 3 months interest (Jun-Aug)
            double june15Required = netTax * 0.15;
            if (payments.getJune15() < june15Required) {
                double shortfall = june15Required - payments.getJune15();
                totalInterest += shortfall * 0.01 * 3;
            }
            
            // Sep 15: 45% cumulative required, 3 months interest (Sep-Nov)
            double sep15Required = netTax * 0.45;
            double sep15Paid = payments.getJune15() + payments.getSep15();
            if (sep15Paid < sep15Required) {
                double shortfall = sep15Required - sep15Paid;
                totalInterest += shortfall * 0.01 * 3;
            }
            
            // Dec 15: 75% cumulative required, 3 months interest (Dec-Feb)
            double dec15Required = netTax * 0.75;
            double dec15Paid = payments.getJune15() + payments.getSep15() + payments.getDec15();
            if (dec15Paid < dec15Required) {
                double shortfall = dec15Required - dec15Paid;
                totalInterest += shortfall * 0.01 * 3;
            }
            
            // Mar 15: 100% cumulative required, 1 month interest (Mar only)
            double mar15Required = netTax;
            double mar15Paid = payments.getJune15() + payments.getSep15() + 
                              payments.getDec15() + payments.getMarch15();
            if (mar15Paid < mar15Required) {
                double shortfall = mar15Required - mar15Paid;
                totalInterest += shortfall * 0.01 * 1;
            }
        }
        
        log.debug("234C: NetTax={}, Presumptive={}, TotalInterest={}", 
                netTax, isPresumptive, totalInterest);
        
        return Math.round(totalInterest);
    }

    /**
     * Section 234F - Late Filing Fee
     * On/before due date: NIL
     * After due date, on/before Dec 31: ₹5,000 (₹1,000 if income ≤₹5L)
     * After Dec 31: ₹10,000 (₹1,000 if income ≤₹5L)
     */
    public double calculate234F(double totalIncome, LocalDate dueDate, LocalDate filingDate) {
        if (filingDate == null || dueDate == null) {
            return 0;
        }
        
        if (!filingDate.isAfter(dueDate)) {
            return 0; // Filed on time
        }
        
        // Income threshold
        boolean lowIncome = totalIncome <= 500000;
        
        // December 31 of assessment year
        LocalDate dec31 = LocalDate.of(dueDate.getYear(), 12, 31);
        
        if (filingDate.isAfter(dec31)) {
            // Filed after Dec 31
            return lowIncome ? 1000 : 10000;
        } else {
            // Filed after due date but before Dec 31
            return lowIncome ? 1000 : 5000;
        }
    }

    /**
     * Section 244A - Interest on Refund
     * Rate: 0.5% per month or part thereof
     * Period: April 1 of AY to date of refund grant
     * Minimum: 3 months if refund within 3 months
     * De minimis: No interest if < ₹100
     */
    public double calculate244A(double refundAmount, LocalDate assessmentYearStart, 
                                 LocalDate refundDate) {
        if (refundAmount <= 0 || refundDate == null || assessmentYearStart == null) {
            return 0;
        }
        
        // Calculate months from April 1 of AY to refund date
        long days = ChronoUnit.DAYS.between(assessmentYearStart, refundDate);
        int months = (int) Math.ceil(days / 30.0);
        months = Math.max(3, months); // Minimum 3 months
        
        double interest = refundAmount * 0.005 * months;
        
        // De minimis rule
        if (interest < 100) {
            return 0;
        }
        
        log.debug("244A: Refund={}, Months={}, Interest={}", refundAmount, months, interest);
        
        return Math.round(interest);
    }

    /**
     * Installment Payments holder
     */
    public static class InstallmentPayments {
        private double june15;
        private double sep15;
        private double dec15;
        private double march15;

        public InstallmentPayments() {
            this.june15 = 0;
            this.sep15 = 0;
            this.dec15 = 0;
            this.march15 = 0;
        }

        public InstallmentPayments(double june15, double sep15, double dec15, double march15) {
            this.june15 = june15;
            this.sep15 = sep15;
            this.dec15 = dec15;
            this.march15 = march15;
        }

        public double getJune15() { return june15; }
        public void setJune15(double june15) { this.june15 = june15; }
        
        public double getSep15() { return sep15; }
        public void setSep15(double sep15) { this.sep15 = sep15; }
        
        public double getDec15() { return dec15; }
        public void setDec15(double dec15) { this.dec15 = dec15; }
        
        public double getMarch15() { return march15; }
        public void setMarch15(double march15) { this.march15 = march15; }
    }
}
