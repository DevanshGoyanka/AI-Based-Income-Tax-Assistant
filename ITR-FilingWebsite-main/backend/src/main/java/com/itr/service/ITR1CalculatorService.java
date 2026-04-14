package com.itr.service;

import com.itr.dto.Itr1FormData;
import com.itr.service.taxengine.TaxComputationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

/**
 * ITR-1 (Sahaj) Calculator Service - 101% CBDT Compliant
 * 
 * Eligibility:
 * - Resident Individual ONLY (ROR)
 * - Total income ≤ ₹50 lakh
 * - Salary/Pension income
 * - ONE house property (no carried forward HP loss)
 * - Other sources (interest, dividend)
 * - Agricultural income ≤ ₹5,000
 * - NO capital gains, business income, foreign assets
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ITR1CalculatorService {

    private final TaxComputationEngine taxEngine;
    private final InterestCalculatorService interestCalculator;
    private final SalaryExemptionService salaryExemptionService;
    private final DeductionCalculatorService deductionCalculator;

    public Itr1FormData calculateITR1(Itr1FormData formData) {
        log.info("Starting ITR-1 calculation for PAN: {}", formData.getPersonalInfo().getPan());
        
        validateEligibility(formData);
        calculateSalaryIncome(formData);
        calculateHousePropertyIncome(formData);
        calculateOtherSourcesIncome(formData);
        
        double gti = calculateGrossTotalIncome(formData);
        double deductions = calculateDeductions(formData);
        double totalIncome = Math.max(0, gti - deductions);
        totalIncome = roundToNearest10(totalIncome);
        
        computeTax(formData, totalIncome);
        calculateInterestAndFees(formData);
        calculateRefundOrDemand(formData);
        
        log.info("ITR-1 completed: Income=₹{}, Tax=₹{}", totalIncome, 
                formData.getTaxComputation().getTotalTaxLiability());
        
        return formData;
    }

    private void validateEligibility(Itr1FormData formData) {
        var info = formData.getPersonalInfo();
        
        if (!"ROR".equals(info.getResidentialStatus())) {
            formData.getValidationErrors().add("ITR-1 only for Resident and Ordinarily Resident (ROR). Use ITR-2.");
        }
        
        if (formData.getExemptIncome() != null && 
            formData.getExemptIncome().getAgricultureIncome() > 5000) {
            formData.getValidationErrors().add("Agricultural income > ₹5,000. Use ITR-2.");
        }
        
        if (!info.isPanAadhaarLinked()) {
            formData.getValidationWarnings().add("PAN-Aadhaar not linked. TDS at 20%, refund may be blocked.");
        }
        
        calculateAge(formData);
    }

    private void calculateAge(Itr1FormData formData) {
        var info = formData.getPersonalInfo();
        LocalDate dob = info.getDateOfBirth();
        String fy = info.getFinancialYear();
        
        int year = Integer.parseInt(fy.split("-")[0]);
        LocalDate april1 = LocalDate.of(year, 4, 1);
        
        int age = Period.between(dob, april1).getYears();
        info.setAge(age);
        
        if (age >= 80) {
            info.setAgeCategory("SUPER_SENIOR_80_PLUS");
        } else if (age >= 60) {
            info.setAgeCategory("SENIOR_60_TO_80");
        } else {
            info.setAgeCategory("BELOW_60");
        }
    }

    private void calculateSalaryIncome(Itr1FormData formData) {
        if (formData.getSalaryIncome() == null) return;
        
        var salary = formData.getSalaryIncome();
        String regime = formData.getPersonalInfo().getRegime();
        
        double grossSalary = salary.getSalary17_1() + salary.getPerquisites17_2() + 
                           salary.getProfitsInLieu17_3();
        
        double exemptAllowances = calculateExemptAllowances(formData);
        double netSalary = grossSalary - exemptAllowances;
        
        double stdDed = "NEW".equals(regime) ? 75000 : 50000;
        
        double entertainmentAlw = 0;
        if (salary.isGovernmentEmployee() && "OLD".equals(regime)) {
            double actualEA = salary.getEntertainmentAllowance();
            double basicSalary = salary.getBasicSalary();
            entertainmentAlw = Math.min(Math.min(actualEA, basicSalary / 5), 5000);
        }
        
        double profTax = Math.min(salary.getProfessionalTax(), 2500);
        double incomeFromSalary = Math.max(0, netSalary - stdDed - entertainmentAlw - profTax);
        
        salary.setGrossSalary(grossSalary);
        salary.setTotalExemptAllowances(exemptAllowances);
        salary.setNetSalary(netSalary);
        salary.setStandardDeduction(stdDed);
        salary.setEntertainmentAllowance(entertainmentAlw);
        salary.setProfessionalTax(profTax);
        salary.setIncomeFromSalary(incomeFromSalary);
        
        log.debug("Salary: Gross={}, Exempt={}, StdDed={}, Final={}", 
                grossSalary, exemptAllowances, stdDed, incomeFromSalary);
    }

    private double calculateExemptAllowances(Itr1FormData formData) {
        var salary = formData.getSalaryIncome();
        String regime = formData.getPersonalInfo().getRegime();
        
        if ("NEW".equals(regime)) {
            return 0;
        }
        
        double totalExempt = 0;
        
        for (var detail : salary.getExemptAllowanceDetails()) {
            double exemptAmount = 0;
            
            if ("HRA".equals(detail.getAllowanceType())) {
                var hraResult = salaryExemptionService.calculateHRAExemption(
                        salary.getBasicSalary(), salary.getDaForRetirement(),
                        detail.getAmountReceived(), detail.getRentPaid(),
                        detail.getCity(), detail.getLandlordPAN());
                
                exemptAmount = hraResult.getExemptAmount();
                detail.setExemptAmount(exemptAmount);
                detail.setTaxableAmount(detail.getAmountReceived() - exemptAmount);
                
                if (hraResult.getWarning() != null) {
                    formData.getValidationWarnings().add(hraResult.getWarning());
                }
            } else {
                exemptAmount = detail.getExemptAmount();
            }
            
            totalExempt += exemptAmount;
        }
        
        salary.setHraExempt(totalExempt);
        return totalExempt;
    }

    private void calculateHousePropertyIncome(Itr1FormData formData) {
        if (formData.getHousePropertyIncome() == null) return;
        
        var hp = formData.getHousePropertyIncome();
        
        if ("SELF_OCCUPIED".equals(hp.getPropertyType())) {
            hp.setGrossAnnualValue(0);
            hp.setNetAnnualValue(0);
            hp.setStandardDeduction30Pct(0);
            
            double interestCap = hp.isCompletedWithin5Years() ? 200000 : 30000;
            double interestDeduction = Math.min(hp.getInterestOnLoan(), interestCap);
            
            hp.setIncomeFromHP(-interestDeduction);
        } else {
            double gav = Math.max(Math.max(hp.getAnnualRent(), hp.getAnnualLettingValue()), 
                                hp.getMunicipalRateableValue());
            double nav = gav - hp.getMunicipalTaxesPaid();
            double stdDed = nav * 0.30;
            
            double incomeFromHP = nav - stdDed - hp.getInterestOnLoan();
            
            hp.setGrossAnnualValue(gav);
            hp.setNetAnnualValue(nav);
            hp.setStandardDeduction30Pct(stdDed);
            hp.setIncomeFromHP(incomeFromHP);
        }
        
        log.debug("HP: Type={}, GAV={}, Income={}", hp.getPropertyType(), 
                hp.getGrossAnnualValue(), hp.getIncomeFromHP());
    }

    private void calculateOtherSourcesIncome(Itr1FormData formData) {
        if (formData.getOtherSourcesIncome() == null) return;
        
        var os = formData.getOtherSourcesIncome();
        String regime = formData.getPersonalInfo().getRegime();
        
        double totalInterest = os.getSavingsAccountInterest() + os.getFixedDepositInterest() +
                             os.getRecurringDepositInterest() + os.getNscInterest() +
                             os.getScssInterest() + os.getOtherInterest();
        
        double totalDividend = os.getDividendFromShares() + os.getDividendFromMutualFunds();
        
        double familyPensionDed = "NEW".equals(regime) ? 
                                Math.min(os.getFamilyPensionReceived() / 3, 25000) :
                                Math.min(os.getFamilyPensionReceived() / 3, 15000);
        double familyPensionTaxable = os.getFamilyPensionReceived() - familyPensionDed;
        
        double totalOS = totalInterest + totalDividend + familyPensionTaxable +
                        os.getIncomeFromITRefund() + os.getGiftsFromNonRelatives() +
                        os.getCasualIncome() + os.getOtherIncome();
        
        os.setTotalInterestIncome(totalInterest);
        os.setTotalDividendIncome(totalDividend);
        os.setFamilyPensionDeduction(familyPensionDed);
        os.setFamilyPensionTaxable(familyPensionTaxable);
        os.setTotalOtherSourcesIncome(totalOS);
        
        log.debug("OS: Interest={}, Dividend={}, FamilyPension={}, Total={}", 
                totalInterest, totalDividend, familyPensionTaxable, totalOS);
    }

    private double calculateGrossTotalIncome(Itr1FormData formData) {
        double salaryIncome = formData.getSalaryIncome() != null ? 
                            formData.getSalaryIncome().getIncomeFromSalary() : 0;
        double hpIncome = formData.getHousePropertyIncome() != null ? 
                        formData.getHousePropertyIncome().getIncomeFromHP() : 0;
        double osIncome = formData.getOtherSourcesIncome() != null ? 
                        formData.getOtherSourcesIncome().getTotalOtherSourcesIncome() : 0;
        
        double hpLoss = 0;
        double hpLossSetOff = 0;
        
        if (hpIncome < 0) {
            hpLoss = Math.abs(hpIncome);
            hpLossSetOff = Math.min(hpLoss, 200000);
            
            if (hpLoss > 200000) {
                formData.getValidationErrors().add(
                    "HP loss exceeds ₹2L. Must file ITR-2 to carry forward excess loss of ₹" + 
                    (hpLoss - 200000));
            }
        }
        
        double gti = Math.max(0, salaryIncome + Math.max(0, hpIncome) + osIncome - hpLossSetOff);
        
        var computation = formData.getTaxComputation();
        if (computation == null) {
            computation = new Itr1FormData.TaxComputation();
            formData.setTaxComputation(computation);
        }
        
        computation.setIncomeFromSalary(salaryIncome);
        computation.setIncomeFromHP(hpIncome);
        computation.setIncomeFromOtherSources(osIncome);
        computation.setHpLoss(hpLoss);
        computation.setHpLossSetOff(hpLossSetOff);
        computation.setHpLossCarryForward(hpLoss - hpLossSetOff);
        computation.setGrossTotalIncome(gti);
        
        return gti;
    }

    private double calculateDeductions(Itr1FormData formData) {
        if (formData.getDeductions() == null) return 0;
        
        var ded = formData.getDeductions();
        String regime = formData.getPersonalInfo().getRegime();
        
        if ("NEW".equals(regime)) {
            double total = ded.getNpsEmployer80CCD2();
            ded.setTotalDeductions(total);
            return total;
        }
        
        double total80C = ded.getLic() + ded.getPpf() + ded.getElss() + ded.getEpfEmployee() +
                         ded.getVpf() + ded.getNsc() + ded.getSukanyaSamriddhi() + 
                         ded.getHomeLoanPrincipal() + ded.getTuitionFees() + ded.getUlip() +
                         ded.getSeniorCitizenSavings() + ded.getFiveYearBankFD() + ded.getOther80C();
        ded.setTotal80C(total80C);
        ded.setDeduction80C(Math.min(total80C, 150000));
        
        double nps80CCD1B = Math.min(ded.getNpsEmployee80CCD1B(), 50000);
        ded.setNpsEmployee80CCD1B(nps80CCD1B);
        
        double selfLimit = ded.isSelfSeniorCitizen() ? 50000 : 25000;
        double parentsLimit = ded.isParentsSeniorCitizen() ? 50000 : 25000;
        double ded80D = Math.min(ded.getHealthInsuranceSelf(), selfLimit) +
                       Math.min(ded.getHealthInsuranceParents(), parentsLimit);
        ded.setDeduction80D(ded80D);
        
        int age = formData.getPersonalInfo().getAge();
        if (age >= 60) {
            double allInterest = formData.getOtherSourcesIncome().getTotalInterestIncome();
            ded.setDeduction80TTB(Math.min(allInterest, 50000));
            ded.setDeduction80TTA(0);
        } else {
            double savingsInterest = formData.getOtherSourcesIncome().getSavingsAccountInterest();
            ded.setDeduction80TTA(Math.min(savingsInterest, 10000));
            ded.setDeduction80TTB(0);
        }
        
        double totalDeductions = ded.getDeduction80C() + nps80CCD1B + ded.getNpsEmployer80CCD2() +
                                ded.getDeduction80D() + ded.getDeduction80DD() + ded.getDeduction80DDB() +
                                ded.getDeduction80E() + ded.getDeduction80EE() + ded.getDeduction80EEA() +
                                ded.getDeduction80EEB() + ded.getDeduction80G() + ded.getDeduction80GG() +
                                ded.getDeduction80GGC() + ded.getDeduction80TTA() + ded.getDeduction80TTB() +
                                ded.getDeduction80U();
        
        ded.setTotalDeductions(totalDeductions);
        return totalDeductions;
    }

    private void computeTax(Itr1FormData formData, double totalIncome) {
        var computation = formData.getTaxComputation();
        var info = formData.getPersonalInfo();
        
        computation.setTotalIncome(totalIncome);
        
        if (totalIncome > 5000000) {
            formData.getValidationErrors().add("Total income exceeds ₹50 lakh. Use ITR-2.");
        }
        
        var input = TaxComputationEngine.TaxComputationInput.builder()
                .regime(info.getRegime())
                .assessmentYear(info.getAssessmentYear())
                .ageCategory(info.getAgeCategory())
                .totalIncome(java.math.BigDecimal.valueOf(totalIncome))
                .normalIncome(java.math.BigDecimal.valueOf(totalIncome))
                .isHUF(false)
                .build();
        
        var result = taxEngine.computeTax(input);
        
        computation.setTaxOnNormalIncome(result.getTaxOnNormalIncome().doubleValue());
        computation.setRebate87A(result.getRebate87A().getRebateAmount().doubleValue());
        computation.setTaxAfterRebate(result.getTaxAfterRebate().doubleValue());
        computation.setSurcharge(result.getSurcharge().getEffectiveSurcharge().doubleValue());
        computation.setMarginalRelief(result.getSurcharge().getMarginalReliefAmount() != null ? 
                result.getSurcharge().getMarginalReliefAmount().doubleValue() : 0);
        computation.setCess(result.getCess().doubleValue());
        computation.setTotalTaxLiability(result.getTotalTaxLiability().doubleValue());
    }

    private void calculateInterestAndFees(Itr1FormData formData) {
        var computation = formData.getTaxComputation();
        var info = formData.getPersonalInfo();
        var payments = formData.getTaxPayments();
        
        if (payments == null) {
            payments = new Itr1FormData.TaxPayments();
            formData.setTaxPayments(payments);
        }
        
        double totalTaxesPaid = payments.getTotalTDSOnSalary() + payments.getTotalTDSOnOther() +
                               payments.getTotalTCS() + payments.getTotalAdvanceTax() +
                               payments.getTotalSelfAssessmentTax();
        
        payments.setTotalTaxesPaid(totalTaxesPaid);
        computation.setTotalTaxesPaid(totalTaxesPaid);
        computation.setBalanceTax(computation.getTotalTaxLiability() - totalTaxesPaid);
        
        LocalDate dueDate = LocalDate.of(Integer.parseInt(info.getFinancialYear().split("-")[0]), 7, 31);
        LocalDate filingDate = info.getOriginalFilingDate() != null ? 
                              info.getOriginalFilingDate() : LocalDate.now();
        
        double balanceTax = computation.getBalanceTax();
        double interest234A = balanceTax > 0 ? 
                interestCalculator.calculate234A(balanceTax, dueDate, filingDate) : 0;
        
        LocalDate ayStart = LocalDate.of(Integer.parseInt(info.getAssessmentYear().split("-")[0]), 4, 1);
        double interest234B = interestCalculator.calculate234B(
                computation.getTotalTaxLiability(), payments.getTotalAdvanceTax(),
                payments.getTotalTDSOnSalary() + payments.getTotalTDSOnOther(),
                info.getAge() >= 60, false, ayStart, filingDate);
        
        InterestCalculatorService.InstallmentPayments installments = 
                new InterestCalculatorService.InstallmentPayments();
        double interest234C = interestCalculator.calculate234C(
                computation.getTotalTaxLiability(),
                payments.getTotalTDSOnSalary() + payments.getTotalTDSOnOther(),
                installments, false);
        
        double fee234F = interestCalculator.calculate234F(
                computation.getTotalIncome(), dueDate, filingDate);
        
        computation.setInterest234A(interest234A);
        computation.setInterest234B(interest234B);
        computation.setInterest234C(interest234C);
        computation.setFee234F(fee234F);
        computation.setTotalInterestAndFees(interest234A + interest234B + interest234C + fee234F);
    }

    private void calculateRefundOrDemand(Itr1FormData formData) {
        var computation = formData.getTaxComputation();
        
        double totalDemand = computation.getTotalTaxLiability() + 
                           computation.getTotalInterestAndFees() -
                           computation.getTotalTaxesPaid();
        
        if (totalDemand > 0) {
            computation.setTaxPayable(totalDemand);
            computation.setRefund(0);
        } else {
            computation.setTaxPayable(0);
            computation.setRefund(Math.abs(totalDemand));
        }
    }

    private double roundToNearest10(double amount) {
        return Math.round(amount / 10.0) * 10;
    }
}
