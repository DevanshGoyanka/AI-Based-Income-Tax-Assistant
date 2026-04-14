package com.itr.service.taxengine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Interest Calculator - Sections 234A, 234B, 234C, 234F
 * 234A: Late filing interest (1% per month from due date to filing date)
 * 234B: Advance tax shortfall (1% per month, 90% threshold)
 * 234C: Installment shortfall (1% per month per installment)
 * 234F: Late filing fee (₹5K/₹1K/₹10K based on income and date)
 */
@Slf4j
@Service
public class InterestCalculator {

    private static final BigDecimal INTEREST_RATE_PER_MONTH = new BigDecimal("0.01");
    private static final BigDecimal ADVANCE_TAX_THRESHOLD = new BigDecimal("0.90"); // 90%

    /**
     * Calculate 234A interest - Late filing
     * From day after due date to actual filing date
     */
    public Interest234AResult calculate234A(
            BigDecimal netTaxPayable,
            LocalDate dueDate,
            LocalDate filingDate) {

        if (netTaxPayable.compareTo(BigDecimal.ZERO) <= 0) {
            return Interest234AResult.builder()
                    .applicable(false)
                    .interest(BigDecimal.ZERO)
                    .months(0)
                    .build();
        }

        if (filingDate == null || !filingDate.isAfter(dueDate)) {
            return Interest234AResult.builder()
                    .applicable(false)
                    .interest(BigDecimal.ZERO)
                    .months(0)
                    .reason("Filed on or before due date")
                    .build();
        }

        // Calculate months (part month = full month)
        long days = ChronoUnit.DAYS.between(dueDate, filingDate);
        int months = (int) Math.ceil(days / 30.0);

        BigDecimal interest = netTaxPayable
                .multiply(INTEREST_RATE_PER_MONTH)
                .multiply(new BigDecimal(months))
                .setScale(0, RoundingMode.HALF_UP);

        return Interest234AResult.builder()
                .applicable(true)
                .interest(interest)
                .months(months)
                .dueDate(dueDate)
                .filingDate(filingDate)
                .reason("Filed " + days + " days late")
                .build();
    }

    /**
     * Calculate 234B interest - Advance tax shortfall
     * If advance tax paid < 90% of assessed tax
     * Interest from April 1 to March 31
     */
    public Interest234BResult calculate234B(
            BigDecimal assessedTax,
            BigDecimal advanceTaxPaid,
            BigDecimal tdsDeducted,
            boolean isSeniorCitizen,
            boolean hasBusinessIncome) {

        // Senior citizens without business income are exempt
        if (isSeniorCitizen && !hasBusinessIncome) {
            return Interest234BResult.builder()
                    .applicable(false)
                    .interest(BigDecimal.ZERO)
                    .reason("Senior citizen without business income - exempt")
                    .build();
        }

        // Tax liability < ₹10,000 - no advance tax required
        if (assessedTax.compareTo(new BigDecimal("10000")) < 0) {
            return Interest234BResult.builder()
                    .applicable(false)
                    .interest(BigDecimal.ZERO)
                    .reason("Tax liability < ₹10,000")
                    .build();
        }

        // Calculate required advance tax (90% of assessed tax)
        BigDecimal requiredAdvanceTax = assessedTax.multiply(ADVANCE_TAX_THRESHOLD);
        
        // Total prepaid tax (advance tax + TDS)
        BigDecimal totalPrepaid = advanceTaxPaid.add(tdsDeducted);

        // Check if shortfall exists
        if (totalPrepaid.compareTo(requiredAdvanceTax) >= 0) {
            return Interest234BResult.builder()
                    .applicable(false)
                    .interest(BigDecimal.ZERO)
                    .reason("Advance tax paid >= 90% of assessed tax")
                    .build();
        }

        // Shortfall amount
        BigDecimal shortfall = assessedTax.subtract(totalPrepaid);

        // Interest = shortfall × 1% × number of months (April to March = 12 months)
        // But calculated from April 1 to March 31 (simplified as 12 months)
        BigDecimal interest = shortfall
                .multiply(INTEREST_RATE_PER_MONTH)
                .multiply(new BigDecimal("12"))
                .setScale(0, RoundingMode.HALF_UP);

        return Interest234BResult.builder()
                .applicable(true)
                .interest(interest)
                .shortfall(shortfall)
                .requiredAdvanceTax(requiredAdvanceTax)
                .actualPrepaid(totalPrepaid)
                .reason("Advance tax shortfall")
                .build();
    }

    /**
     * Calculate 234C interest - Installment shortfall
     * Installments: June 15 (15%), Sep 15 (45%), Dec 15 (75%), Mar 15 (100%)
     * Presumptive income: Only Mar 15 (100%)
     */
    public Interest234CResult calculate234C(
            BigDecimal assessedTax,
            InstallmentPayments payments,
            boolean isPresumptive) {

        if (assessedTax.compareTo(new BigDecimal("10000")) < 0) {
            return Interest234CResult.builder()
                    .applicable(false)
                    .totalInterest(BigDecimal.ZERO)
                    .reason("Tax liability < ₹10,000")
                    .build();
        }

        BigDecimal totalInterest = BigDecimal.ZERO;

        if (isPresumptive) {
            // Presumptive: Only March 15 installment (100%)
            BigDecimal required = assessedTax;
            BigDecimal paid = payments.getMarchPayment();
            
            if (paid.compareTo(required) < 0) {
                BigDecimal shortfall = required.subtract(paid);
                // Interest for 1 month (March 15 to April 15)
                BigDecimal interest = shortfall.multiply(INTEREST_RATE_PER_MONTH);
                totalInterest = totalInterest.add(interest);
            }
        } else {
            // Regular: 4 installments
            
            // June 15: 15%
            BigDecimal juneRequired = assessedTax.multiply(new BigDecimal("0.15"));
            if (payments.getJunePayment().compareTo(juneRequired) < 0) {
                BigDecimal shortfall = juneRequired.subtract(payments.getJunePayment());
                // Interest for 9 months (June to March)
                BigDecimal interest = shortfall.multiply(INTEREST_RATE_PER_MONTH).multiply(new BigDecimal("9"));
                totalInterest = totalInterest.add(interest);
            }

            // September 15: 45% cumulative (30% additional)
            BigDecimal sepRequired = assessedTax.multiply(new BigDecimal("0.45"));
            BigDecimal sepCumulative = payments.getJunePayment().add(payments.getSeptemberPayment());
            if (sepCumulative.compareTo(sepRequired) < 0) {
                BigDecimal shortfall = sepRequired.subtract(sepCumulative);
                // Interest for 6 months (Sep to March)
                BigDecimal interest = shortfall.multiply(INTEREST_RATE_PER_MONTH).multiply(new BigDecimal("6"));
                totalInterest = totalInterest.add(interest);
            }

            // December 15: 75% cumulative (30% additional)
            BigDecimal decRequired = assessedTax.multiply(new BigDecimal("0.75"));
            BigDecimal decCumulative = sepCumulative.add(payments.getDecemberPayment());
            if (decCumulative.compareTo(decRequired) < 0) {
                BigDecimal shortfall = decRequired.subtract(decCumulative);
                // Interest for 3 months (Dec to March)
                BigDecimal interest = shortfall.multiply(INTEREST_RATE_PER_MONTH).multiply(new BigDecimal("3"));
                totalInterest = totalInterest.add(interest);
            }

            // March 15: 100% cumulative (25% additional)
            BigDecimal marRequired = assessedTax;
            BigDecimal marCumulative = decCumulative.add(payments.getMarchPayment());
            if (marCumulative.compareTo(marRequired) < 0) {
                BigDecimal shortfall = marRequired.subtract(marCumulative);
                // Interest for 1 month (March to April)
                BigDecimal interest = shortfall.multiply(INTEREST_RATE_PER_MONTH);
                totalInterest = totalInterest.add(interest);
            }
        }

        return Interest234CResult.builder()
                .applicable(totalInterest.compareTo(BigDecimal.ZERO) > 0)
                .totalInterest(totalInterest.setScale(0, RoundingMode.HALF_UP))
                .reason(totalInterest.compareTo(BigDecimal.ZERO) > 0 ? "Installment shortfall" : "No shortfall")
                .build();
    }

    /**
     * Calculate 234F late filing fee
     * Income > ₹5L: ₹5,000 (if filed by Dec 31), ₹10,000 (after Dec 31)
     * Income ≤ ₹5L: ₹1,000
     */
    public Fee234FResult calculate234F(
            BigDecimal totalIncome,
            LocalDate dueDate,
            LocalDate filingDate,
            String assessmentYear) {

        if (filingDate == null || !filingDate.isAfter(dueDate)) {
            return Fee234FResult.builder()
                    .applicable(false)
                    .fee(BigDecimal.ZERO)
                    .reason("Filed on or before due date")
                    .build();
        }

        // Extract financial year end date (December 31)
        int year = Integer.parseInt(assessmentYear.substring(0, 4));
        LocalDate dec31 = LocalDate.of(year, 12, 31);

        BigDecimal fee;
        String reason;

        if (totalIncome.compareTo(new BigDecimal("500000")) <= 0) {
            fee = new BigDecimal("1000");
            reason = "Income ≤ ₹5L: ₹1,000 late filing fee";
        } else {
            if (filingDate.isAfter(dec31)) {
                fee = new BigDecimal("10000");
                reason = "Income > ₹5L, filed after Dec 31: ₹10,000 late filing fee";
            } else {
                fee = new BigDecimal("5000");
                reason = "Income > ₹5L, filed by Dec 31: ₹5,000 late filing fee";
            }
        }

        return Fee234FResult.builder()
                .applicable(true)
                .fee(fee)
                .dueDate(dueDate)
                .filingDate(filingDate)
                .reason(reason)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Interest234AResult {
        private boolean applicable;
        private BigDecimal interest;
        private int months;
        private LocalDate dueDate;
        private LocalDate filingDate;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Interest234BResult {
        private boolean applicable;
        private BigDecimal interest;
        private BigDecimal shortfall;
        private BigDecimal requiredAdvanceTax;
        private BigDecimal actualPrepaid;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Interest234CResult {
        private boolean applicable;
        private BigDecimal totalInterest;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Fee234FResult {
        private boolean applicable;
        private BigDecimal fee;
        private LocalDate dueDate;
        private LocalDate filingDate;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallmentPayments {
        @Builder.Default
        private BigDecimal junePayment = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal septemberPayment = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal decemberPayment = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal marchPayment = BigDecimal.ZERO;
    }
}
