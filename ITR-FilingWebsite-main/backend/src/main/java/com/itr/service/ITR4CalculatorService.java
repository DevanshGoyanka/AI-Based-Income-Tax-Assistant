package com.itr.service;

import com.itr.dto.Itr1FormData;
import com.itr.dto.Itr2FormData;
import com.itr.dto.Itr4FormData;
import com.itr.service.taxengine.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ITR4CalculatorService {

    private final TaxComputationEngine taxEngine;

    public Itr4FormData calculateTax(Itr4FormData formData) {
        log.info("Starting ITR-4 (Sugam) tax calculation for AY {}", formData.getPartA().getAssessmentYear());

        try {
            // Validate eligibility for ITR-4
            validateITR4Eligibility(formData);
            
            // Calculate all income heads
            calculateSalaryIncome(formData);
            calculateHousePropertyIncome(formData);
            calculatePresumptiveIncome(formData);
            calculateOtherSourcesIncome(formData);
            
            // Calculate GTI
            double gti = calculateGrossTotalIncome(formData);
            
            // Apply deductions
            double totalDeductions = calculateDeductions(formData);
            double totalIncome = Math.max(0, gti - totalDeductions);
            
            // Compute tax
            computeTax(formData, totalIncome);
            
            // Calculate interest and fees
            calculateInterestAndFees(formData);
            
            // Calculate refund/demand
            calculateRefundOrDemand(formData);
            
            log.info("ITR-4 calculation completed. Total Income: {}, Tax: {}", 
                    totalIncome, formData.getComputation().getTotalTaxLiability());
            return formData;
            
        } catch (Exception e) {
            log.error("Error calculating ITR-4 tax", e);
            formData.getValidationErrors().add("Tax calculation failed: " + e.getMessage());
            return formData;
        }
    }

    private void validateITR4Eligibility(Itr4FormData formData) {
        var presumptive = formData.getSchedulePresumptive();
        if (presumptive == null) {
            formData.getValidationErrors().add("ITR-4 requires presumptive income under 44AD/44ADA/44AE");
            return;
        }
        
        // Check total income limit ≤ ₹50 lakh
        double totalIncome = calculateGrossTotalIncome(formData);
        if (totalIncome > 5000000) {
            formData.getValidationErrors().add(
                "ITR-4 not eligible: Total income ₹" + totalIncome + " exceeds ₹50 lakh limit. Use ITR-3.");
        }
        
        // Validate 44AD limits
        if (presumptive.getBusiness44AD() != null && presumptive.getBusiness44AD().isApplicable()) {
            var business = presumptive.getBusiness44AD();
            double turnover = business.getGrossTurnoverTotal();
            
            // Check 5% cash test for ₹3 crore limit
            boolean eligible3Cr = business.isCashReceiptsBelow5Pct() && business.isCashPaymentsBelow5Pct();
            double limit = eligible3Cr ? 30000000 : 20000000;
            
            if (turnover > limit) {
                formData.getValidationErrors().add(
                    "44AD not eligible: Turnover ₹" + turnover + " exceeds limit ₹" + limit + ". Use ITR-3 with books.");
            }
            
            business.setEligibleFor3CroreLimit(eligible3Cr);
        }
        
        // Validate 44ADA limits
        if (presumptive.getProfessional44ADA() != null && presumptive.getProfessional44ADA().isApplicable()) {
            var professional = presumptive.getProfessional44ADA();
            double receipts = professional.getGrossReceipts();
            
            boolean eligible75L = professional.isCashReceiptsBelow5Pct() && professional.isCashPaymentsBelow5Pct();
            double limit = eligible75L ? 7500000 : 5000000;
            
            if (receipts > limit) {
                formData.getValidationErrors().add(
                    "44ADA not eligible: Receipts ₹" + receipts + " exceeds limit ₹" + limit + ". Use ITR-3 with books.");
            }
            
            professional.setEligibleFor75LakhLimit(eligible75L);
        }
        
        // Validate 44AE vehicle count
        if (presumptive.getVehicles44AE() != null && presumptive.getVehicles44AE().size() > 10) {
            formData.getValidationErrors().add(
                "44AE not eligible: More than 10 vehicles owned. Use ITR-3.");
        }
    }

    private void calculateSalaryIncome(Itr4FormData formData) {
        if (formData.getScheduleSalary() == null) return;
        
        var salary = formData.getScheduleSalary();
        String regime = formData.getPartA().getRegime();
        
        double salary17_1 = salary.getSalary17_1();
        double perquisites17_2 = salary.getPerquisites17_2();
        double profitsInLieu17_3 = salary.getProfitsInLieu17_3();
        double grossSalary = salary17_1 + perquisites17_2 + profitsInLieu17_3;
        
        double allowancesExempt = salary.getAllowancesExempt();
        double netSalary = grossSalary - allowancesExempt;
        
        double stdDed = "NEW".equals(regime) ? 75000 : 50000;
        
        double entertainmentAlw = 0;
        if (salary.isGovernmentEmployee() && "OLD".equals(regime)) {
            double basicSalary = salary.getBasicSalary();
            entertainmentAlw = Math.min(Math.min(salary.getEntertainmentAllowance(), basicSalary / 5), 5000);
        }
        
        double profTax = Math.min(salary.getProfessionalTax(), 2500);
        double incomeFromSalary = Math.max(0, netSalary - stdDed - entertainmentAlw - profTax);
        
        salary.setGrossSalary(grossSalary);
        salary.setNetSalary(netSalary);
        salary.setStandardDeduction(stdDed);
        salary.setEntertainmentAllowanceDeduction(entertainmentAlw);
        salary.setProfessionalTaxDeduction(profTax);
        salary.setIncomeFromSalary(incomeFromSalary);
    }

    private void calculateHousePropertyIncome(Itr4FormData formData) {
        if (formData.getScheduleHP() == null) return;
        
        var hp = formData.getScheduleHP();
        String propertyType = hp.getPropertyType();
        
        double gav = 0;
        if ("SELF_OCCUPIED".equals(propertyType)) {
            gav = 0;
        } else {
            gav = hp.getGrossRent();
        }
        
        double municipalTax = hp.getMunicipalTaxPaid();
        double nav = gav - municipalTax;
        double stdDed = nav > 0 ? nav * 0.30 : 0;
        
        double interest = hp.getInterestOnLoan();
        if ("SELF_OCCUPIED".equals(propertyType) && interest > 200000) {
            interest = 200000;
        }
        
        double incomeFromHP = nav - stdDed - interest;
        
        hp.setAnnualValue(gav);
        hp.setStandardDeduction(stdDed);
        hp.setIncomeFromHP(incomeFromHP);
    }

    private void calculatePresumptiveIncome(Itr4FormData formData) {
        if (formData.getSchedulePresumptive() == null) return;
        
        var presumptive = formData.getSchedulePresumptive();
        double totalIncome = 0;
        
        // Section 44AD - Business Income
        if (presumptive.getBusiness44AD() != null && presumptive.getBusiness44AD().isApplicable()) {
            var business = presumptive.getBusiness44AD();
            
            // Calculate presumptive income: 6% on digital, 8% on cash
            double digitalIncome = business.getGrossTurnoverDigital() * 0.06;
            double cashIncome = business.getGrossTurnoverCash() * 0.08;
            double totalPresumptive = digitalIncome + cashIncome;
            
            business.setPresumptiveIncomeDigital(digitalIncome);
            business.setPresumptiveIncomeCash(cashIncome);
            business.setTotalPresumptiveIncome(totalPresumptive);
            
            // Declared income must be >= presumptive income
            double declared = business.getDeclaredIncome();
            if (declared > 0 && declared < totalPresumptive) {
                formData.getValidationErrors().add(
                    "44AD: Declared income ₹" + declared + " is less than presumptive ₹" + totalPresumptive + 
                    ". This requires maintaining books and audit u/s 44AB.");
            }
            
            // Use higher of declared or presumptive
            double finalIncome = Math.max(declared, totalPresumptive);
            totalIncome += finalIncome;
            
            // 5-year lock-in warning
            if (business.isOptedIn() && business.getFirstOptInYear() != null) {
                formData.getValidationWarnings().add(
                    "44AD opt-in from " + business.getFirstOptInYear() + ": Must continue for 5 years or maintain books/audit if opting out.");
            }
            
            log.debug("44AD: Turnover Digital=₹{}, Cash=₹{}, Presumptive=₹{}, Final=₹{}", 
                    business.getGrossTurnoverDigital(), business.getGrossTurnoverCash(), 
                    totalPresumptive, finalIncome);
        }
        
        // Section 44ADA - Professional Income
        if (presumptive.getProfessional44ADA() != null && presumptive.getProfessional44ADA().isApplicable()) {
            var professional = presumptive.getProfessional44ADA();
            
            // Presumptive income = 50% of gross receipts
            double presumptiveIncome = professional.getGrossReceipts() * 0.50;
            professional.setPresumptiveIncome(presumptiveIncome);
            
            double declared = professional.getDeclaredIncome();
            if (declared > 0 && declared < presumptiveIncome) {
                formData.getValidationErrors().add(
                    "44ADA: Declared income ₹" + declared + " is less than 50% of receipts. Regular books required.");
            }
            
            double finalIncome = Math.max(declared, presumptiveIncome);
            totalIncome += finalIncome;
            
            log.debug("44ADA: Receipts=₹{}, Presumptive=₹{}, Final=₹{}", 
                    professional.getGrossReceipts(), presumptiveIncome, finalIncome);
        }
        
        // Section 44AE - Goods Vehicle Operators
        if (presumptive.getVehicles44AE() != null && !presumptive.getVehicles44AE().isEmpty()) {
            double vehicleIncome = 0;
            
            for (var vehicle : presumptive.getVehicles44AE()) {
                double gwt = vehicle.getGrossVehicleWeight();
                int months = vehicle.getMonthsOwned();
                
                // Heavy goods vehicle (>12T): ₹1,000 per ton per month
                // Light goods vehicle (≤12T): ₹7,500 per vehicle per month
                double monthlyRate;
                if (gwt > 12000) {
                    // Heavy vehicle: rate per ton
                    double tons = gwt / 1000.0;
                    monthlyRate = tons * 1000;
                } else {
                    // Light vehicle: flat rate
                    monthlyRate = 7500;
                }
                
                double vehiclePresumptive = monthlyRate * months;
                vehicle.setPresumptiveIncome(vehiclePresumptive);
                
                double declared = vehicle.getDeclaredIncome();
                double finalVehicleIncome = Math.max(declared, vehiclePresumptive);
                vehicleIncome += finalVehicleIncome;
                
                log.debug("44AE Vehicle {}: GVW={}kg, Months={}, Rate=₹{}/month, Presumptive=₹{}", 
                        vehicle.getVehicleRegNo(), gwt, months, monthlyRate, vehiclePresumptive);
            }
            
            presumptive.setTotal44AEIncome(vehicleIncome);
            totalIncome += vehicleIncome;
        }
        
        presumptive.setTotalPresumptiveIncome(totalIncome);
        log.info("Total presumptive income: ₹{}", totalIncome);
    }

    private void calculateOtherSourcesIncome(Itr4FormData formData) {
        if (formData.getScheduleOS() == null) return;
        
        var os = formData.getScheduleOS();
        String regime = formData.getPartA().getRegime();
        
        double savingsInterest = os.getSavingsInterest();
        double depositInterest = os.getDepositInterest();
        double otherInterest = os.getOtherInterest();
        double dividendIncome = os.getDividendIncome();
        double familyPension = os.getFamilyPension();
        double otherIncome = os.getOtherIncome();
        double giftsReceived = os.getGiftsFromNonRelatives();
        
        // Family pension deduction: 1/3 or ₹15K/₹25K (lower)
        double pensionDedLimit = "NEW".equals(regime) ? 25000 : 15000;
        double pensionDed = Math.min(familyPension / 3, pensionDedLimit);
        
        double totalOS = savingsInterest + depositInterest + otherInterest + 
                        dividendIncome + familyPension + otherIncome + giftsReceived - pensionDed;
        
        os.setFamilyPensionDeduction(pensionDed);
        os.setIncomeFromOtherSources(totalOS);
    }

    private double calculateGrossTotalIncome(Itr4FormData formData) {
        double gti = 0;
        
        if (formData.getScheduleSalary() != null) {
            gti += formData.getScheduleSalary().getIncomeFromSalary();
        }
        
        if (formData.getScheduleHP() != null) {
            double hpIncome = formData.getScheduleHP().getIncomeFromHP();
            if (hpIncome < 0) {
                double hpLoss = Math.abs(hpIncome);
                double setOffAmount = Math.min(hpLoss, 200000);
                gti -= setOffAmount;
            } else {
                gti += hpIncome;
            }
        }
        
        if (formData.getSchedulePresumptive() != null) {
            gti += formData.getSchedulePresumptive().getTotalPresumptiveIncome();
        }
        
        if (formData.getScheduleOS() != null) {
            gti += formData.getScheduleOS().getIncomeFromOtherSources();
        }
        
        gti = Math.max(0, gti);
        
        if (formData.getComputation() == null) {
            formData.setComputation(new Itr2FormData.TaxComputation());
        }
        formData.getComputation().setGrossTotalIncome(gti);
        
        return gti;
    }

    private double calculateDeductions(Itr4FormData formData) {
        if (formData.getDeductions() == null) return 0;
        
        var ded = formData.getDeductions();
        String regime = formData.getPartA().getRegime();
        
        if ("NEW".equals(regime)) {
            double total = ded.getDeduction80CCD2() + ded.getDeduction80JJAA();
            ded.setTotalDeductions(total);
            return total;
        }
        
        double total = 0;
        
        double group80C = ded.getDeduction80C() + ded.getDeduction80CCC() + ded.getDeduction80CCD1();
        total += Math.min(group80C, 150000);
        total += Math.min(ded.getDeduction80CCD1B(), 50000);
        total += ded.getDeduction80CCD2();
        
        int age = formData.getPartA().getAge() != null ? formData.getPartA().getAge() : 0;
        double maxSelf = age >= 60 ? 50000 : 25000;
        double maxParents = ded.isParentsSeniorCitizen() ? 50000 : 25000;
        total += Math.min(ded.getDeduction80D(), maxSelf + maxParents);
        
        total += ded.getDeduction80DD();
        total += ded.getDeduction80DDB();
        total += ded.getDeduction80E();
        total += Math.min(ded.getDeduction80EE(), 50000);
        total += Math.min(ded.getDeduction80EEA(), 150000);
        total += Math.min(ded.getDeduction80EEB(), 150000);
        total += ded.getDeduction80G();
        total += ded.getDeduction80GG();
        total += ded.getDeduction80GGA();
        total += ded.getDeduction80GGC();
        
        if (age >= 60) {
            total += Math.min(ded.getDeduction80TTB(), 50000);
        } else {
            total += Math.min(ded.getDeduction80TTA(), 10000);
        }
        
        total += ded.getDeduction80U();
        
        ded.setTotalDeductions(total);
        return total;
    }

    private void computeTax(Itr4FormData formData, double totalIncome) {
        var partA = formData.getPartA();
        var computation = formData.getComputation();
        if (computation == null) {
            computation = new Itr2FormData.TaxComputation();
            formData.setComputation(computation);
        }
        
        computation.setTotalIncome(totalIncome);
        
        var input = TaxComputationEngine.TaxComputationInput.builder()
                .regime(partA.getRegime())
                .assessmentYear(partA.getAssessmentYear())
                .ageCategory(getAgeCategory(partA.getAge()))
                .isHUF(false)
                .isSeniorCitizen(partA.getAge() >= 60)
                .hasBusinessIncome(true)
                .isPresumptiveIncome(true)
                .totalIncome(BigDecimal.valueOf(totalIncome))
                .normalIncome(BigDecimal.valueOf(totalIncome))
                .build();
        
        if (formData.getScheduleTDS() != null) {
            input.setTdsPaid(BigDecimal.valueOf(formData.getScheduleTDS().getTotalTDS()));
        }
        
        var result = taxEngine.computeTax(input);
        
        computation.setTaxOnNormalIncome(result.getTaxOnNormalIncome().doubleValue());
        computation.setRebate87A(result.getRebate87A().getRebateAmount().doubleValue());
        computation.setSurcharge(result.getSurcharge().getEffectiveSurcharge().doubleValue());
        computation.setCess(result.getCess().doubleValue());
        computation.setTotalTaxLiability(result.getTotalTaxLiability().doubleValue());
        
        log.info("ITR-4 Tax computed: Total Income=₹{}, Tax=₹{}", totalIncome, computation.getTotalTaxLiability());
    }

    private void calculateInterestAndFees(Itr4FormData formData) {
        var computation = formData.getComputation();
        if (computation == null) return;
        
        // Presumptive taxpayers: advance tax due 100% by March 15 (single installment)
        // Interest 234B/C calculated differently for presumptive
        computation.setInterest234A(0);
        computation.setInterest234B(0);
        computation.setInterest234C(0);
        computation.setFee234F(0);
    }

    private void calculateRefundOrDemand(Itr4FormData formData) {
        var computation = formData.getComputation();
        if (computation == null) return;
        
        double totalTax = computation.getTotalTaxLiability();
        double totalPaid = 0;
        
        if (formData.getScheduleTDS() != null) {
            totalPaid += formData.getScheduleTDS().getTotalTDS();
        }
        
        if (formData.getScheduleIT() != null) {
            totalPaid += formData.getScheduleIT().getTotalAdvanceTax();
            totalPaid += formData.getScheduleIT().getTotalSelfAssessmentTax();
        }
        
        double netPayable = totalTax - totalPaid;
        
        if (netPayable > 0) {
            computation.setTaxPayable(netPayable);
            computation.setRefund(0);
        } else {
            computation.setTaxPayable(0);
            computation.setRefund(Math.abs(netPayable));
        }
        
        log.info("ITR-4 Refund/Demand: Tax=₹{}, Paid=₹{}, Net=₹{}", totalTax, totalPaid, netPayable);
    }
    
    private String getAgeCategory(Integer age) {
        if (age == null) return "REGULAR";
        if (age >= 80) return "SUPER_SENIOR";
        if (age >= 60) return "SENIOR";
        return "REGULAR";
    }
}
