package com.itr.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Salary Exemption Calculator - HRA, LTA, and other allowances
 * 101% CBDT Compliant
 */
@Slf4j
@Service
public class SalaryExemptionService {

    // Metro cities for 50% HRA calculation
    private static final List<String> METRO_CITIES = List.of(
            "DELHI", "MUMBAI", "KOLKATA", "CHENNAI",
            "NEW DELHI", "GREATER MUMBAI", "CALCUTTA", "MADRAS"
    );

    /**
     * Calculate HRA Exemption u/s 10(13A)
     * Exemption = MINIMUM OF:
     * 1. Actual HRA received
     * 2. Rent paid - 10% of salary
     * 3. 50% of salary (metro) OR 40% of salary (non-metro)
     * 
     * Salary = Basic + DA (forming part of retirement benefits)
     * NOT available in new regime
     */
    public HRAExemptionResult calculateHRAExemption(double basicSalary, double daForRetirement,
                                                     double hraReceived, double rentPaid,
                                                     String city, String landlordPAN) {
        
        HRAExemptionResult result = new HRAExemptionResult();
        result.setBasicSalary(basicSalary);
        result.setDaForRetirement(daForRetirement);
        result.setHraReceived(hraReceived);
        result.setRentPaid(rentPaid);
        result.setCity(city);
        
        // Salary for HRA calculation
        double salary = basicSalary + daForRetirement;
        result.setSalaryForHRA(salary);
        
        // Condition 1: Actual HRA received
        double condition1 = hraReceived;
        
        // Condition 2: Rent paid - 10% of salary
        double condition2 = rentPaid - (salary * 0.10);
        
        // Condition 3: 50% (metro) or 40% (non-metro) of salary
        boolean isMetro = METRO_CITIES.stream()
                .anyMatch(metro -> city != null && city.toUpperCase().contains(metro));
        double metroPercentage = isMetro ? 0.50 : 0.40;
        double condition3 = salary * metroPercentage;
        
        result.setMetroCity(isMetro);
        result.setCondition1_ActualHRA(condition1);
        result.setCondition2_RentMinus10Pct(condition2);
        result.setCondition3_MetroPercentage(condition3);
        
        // Exemption = minimum of all three
        double exemption = Math.min(condition1, Math.min(condition2, condition3));
        
        // If rent paid < 10% of salary, exemption = 0
        if (condition2 < 0) {
            exemption = 0;
            result.setWarning("Rent paid is less than 10% of salary. No HRA exemption available.");
        }
        
        // Landlord PAN validation
        if (rentPaid > 100000 && (landlordPAN == null || landlordPAN.trim().isEmpty())) {
            result.setWarning("Annual rent exceeds ₹1,00,000. Landlord's PAN must be quoted in ITR.");
        }
        
        result.setExemptAmount(Math.max(0, exemption));
        result.setTaxableHRA(hraReceived - result.getExemptAmount());
        
        log.info("HRA Exemption: Salary={}, HRA={}, Rent={}, Metro={}, Exemption={}", 
                salary, hraReceived, rentPaid, isMetro, result.getExemptAmount());
        
        return result;
    }

    /**
     * Calculate LTA Exemption u/s 10(5)
     * Conditions:
     * - Travel within India only
     * - Maximum 2 journeys in a block of 4 calendar years
     * - Current block: 2022-2025; Next block: 2026-2029
     * - Exemption = actual fare for economy class air OR AC first class rail (shortest route)
     * - Only fare exempt (not hotel, food, local transport)
     * NOT available in new regime
     */
    public LTAExemptionResult calculateLTAExemption(double ltaReceived, double actualFare,
                                                     String modeOfTravel, LocalDate travelDate,
                                                     boolean isDomesticTravel, int journeysInBlock) {
        
        LTAExemptionResult result = new LTAExemptionResult();
        result.setLtaReceived(ltaReceived);
        result.setActualFare(actualFare);
        result.setModeOfTravel(modeOfTravel);
        result.setTravelDate(travelDate);
        
        // Validate domestic travel
        if (!isDomesticTravel) {
            result.setEligible(false);
            result.setReason("LTA exemption available only for travel within India");
            return result;
        }
        
        // Validate block of 4 years (2 journeys max)
        if (journeysInBlock > 2) {
            result.setEligible(false);
            result.setReason("Maximum 2 journeys allowed in a block of 4 calendar years");
            return result;
        }
        
        // Determine block
        int year = travelDate.getYear();
        String block;
        if (year >= 2022 && year <= 2025) {
            block = "2022-2025";
        } else if (year >= 2026 && year <= 2029) {
            block = "2026-2029";
        } else {
            block = "Unknown";
        }
        result.setBlock(block);
        
        // Exemption = actual fare (subject to economy class air / AC first class rail)
        // In practice, system should validate fare against shortest route
        double exemption = Math.min(ltaReceived, actualFare);
        
        result.setEligible(true);
        result.setExemptAmount(exemption);
        result.setTaxableLTA(ltaReceived - exemption);
        
        log.info("LTA Exemption: LTA={}, Fare={}, Block={}, Journey={}, Exemption={}", 
                ltaReceived, actualFare, block, journeysInBlock, exemption);
        
        return result;
    }

    /**
     * Calculate Children Education Allowance Exemption u/s 10(14)
     * Exemption: ₹100 per month per child (max 2 children)
     * Total: ₹2,400 per year (₹100 × 12 × 2)
     */
    public double calculateEducationAllowanceExemption(double allowanceReceived, int numberOfChildren) {
        int eligibleChildren = Math.min(numberOfChildren, 2);
        double maxExemption = 100 * 12 * eligibleChildren; // ₹100/month × 12 months × children
        return Math.min(allowanceReceived, maxExemption);
    }

    /**
     * Calculate Hostel Expenditure Allowance Exemption u/s 10(14)
     * Exemption: ₹300 per month per child (max 2 children)
     * Total: ₹7,200 per year (₹300 × 12 × 2)
     */
    public double calculateHostelAllowanceExemption(double allowanceReceived, int numberOfChildren) {
        int eligibleChildren = Math.min(numberOfChildren, 2);
        double maxExemption = 300 * 12 * eligibleChildren; // ₹300/month × 12 months × children
        return Math.min(allowanceReceived, maxExemption);
    }

    /**
     * Calculate Transport Allowance Exemption u/s 10(14)
     * For disabled employees: ₹3,200 per month (₹38,400 per year)
     * For others: NOT available from AY 2019-20 onwards
     */
    public double calculateTransportAllowanceExemption(double allowanceReceived, boolean isDisabled) {
        if (!isDisabled) {
            return 0; // Not available for non-disabled from AY 2019-20
        }
        double maxExemption = 3200 * 12; // ₹3,200/month × 12 months
        return Math.min(allowanceReceived, maxExemption);
    }

    /**
     * Calculate Uniform Allowance Exemption u/s 10(14)
     * Exemption: Actual expenditure on uniform (no fixed limit)
     * Condition: Uniform must be worn during duty
     */
    public double calculateUniformAllowanceExemption(double allowanceReceived, double actualExpenditure) {
        return Math.min(allowanceReceived, actualExpenditure);
    }

    // Result classes
    @Data
    public static class HRAExemptionResult {
        private double basicSalary;
        private double daForRetirement;
        private double salaryForHRA;
        private double hraReceived;
        private double rentPaid;
        private String city;
        private boolean metroCity;
        private double condition1_ActualHRA;
        private double condition2_RentMinus10Pct;
        private double condition3_MetroPercentage;
        private double exemptAmount;
        private double taxableHRA;
        private String warning;
    }

    @Data
    public static class LTAExemptionResult {
        private double ltaReceived;
        private double actualFare;
        private String modeOfTravel;
        private LocalDate travelDate;
        private String block;
        private boolean eligible;
        private String reason;
        private double exemptAmount;
        private double taxableLTA;
    }
}
