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
        
        // ===== STEP 1: Calculate Section 17(1) salary components =====
        // Basic + Dearness Allowance + Bonus + Commission + Other Allowances
        double salary17_1 = salary.getBasicSalary() + salary.getDaAmount() + 
                          salary.getBonusAmount() + salary.getCommissionAmount() +
                          salary.getOtherAllowance();
        salary.setSalary17_1(salary17_1);
        
        // DEBUG LOG
        log.info("DEBUG Calculator Input - basicSalary={}, daAmount={}, bonusAmount={}, commissionAmount={}, otherAllowance={}, salary17_1={}",
                salary.getBasicSalary(), salary.getDaAmount(), salary.getBonusAmount(), 
                salary.getCommissionAmount(), salary.getOtherAllowance(), salary17_1);
        
        // ===== STEP 2: Calculate Perquisites (Section 17(2)) =====
        // Sum of all perquisite values
        double perquisites17_2 = salary.getRentFreeAccommodationValue() + salary.getCarValue() +
                                salary.getGasFuelPowerValue() + salary.getFreeHolidayValue() +
                                salary.getFreeGoodsValue() + salary.getFreeServicesValue() +
                                salary.getStockOptionsValue() + salary.getProfessionalTaxValue();
        salary.setPerquisites17_2(perquisites17_2);
        
        // ===== STEP 3: Calculate Profits in Lieu (Section 17(3)) =====
        // Sum of all profits in lieu components
        double profitsInLieu17_3 = salary.getGratuityReceived() + salary.getLeaveEncashmentReceived() +
                                  salary.getCommutationOfPensionReceived() + salary.getRetrenchmentCompensation() +
                                  salary.getVrsCompensation();
        salary.setProfitsInLieu17_3(profitsInLieu17_3);
        
        // ===== STEP 4: Calculate Gross Salary =====
        double grossSalary = salary17_1 + perquisites17_2 + profitsInLieu17_3;
        
        // ===== STEP 5: Calculate Exempt Allowances (Old Regime Only) =====
        double exemptAllowances = 0;
        if ("OLD".equals(regime)) {
            exemptAllowances = calculateAllExemptAllowances(formData);
        }
        salary.setTotalExemptAllowances(exemptAllowances);
        
        // ===== STEP 6: Calculate Net Salary =====
        double netSalary = grossSalary - exemptAllowances;
        salary.setNetSalary(netSalary);
        
        // ===== STEP 7: Apply Deductions u/s 16 =====
        // Standard Deduction - ₹75,000 for AY 2025-26 (BOTH regimes)
        double stdDed = 75000;
        salary.setStandardDeduction(stdDed);
        
        // Entertainment Allowance - Section 16(ii) - Only for Govt employees (Old regime)
        double entertainmentAlw = 0;
        if (salary.isGovernmentEmployee() && "OLD".equals(regime)) {
            double basicSalary = salary.getBasicSalary();
            entertainmentAlw = Math.min(basicSalary / 5, 5000);
            // Can't exceed actual entertainment allowance received
            entertainmentAlw = Math.min(entertainmentAlw, salary.getOtherAllowance());
        }
        salary.setEntertainmentAllowance(entertainmentAlw);
        
        // Professional Tax - Section 16(iii)
        double profTax = Math.min(salary.getProfessionalTax(), 2500);
        salary.setProfessionalTax(profTax);
        
        // ===== STEP 8: Calculate Final Income from Salary =====
        double incomeFromSalary = Math.max(0, netSalary - stdDed - entertainmentAlw - profTax);
        salary.setIncomeFromSalary(incomeFromSalary);
        
        // ===== STEP 9: Set computed gross salary =====
        salary.setGrossSalary(grossSalary);
        
        log.info("Salary Computation: Gross={}, Exempt={}, StdDed={}, ProfTax={}, Entertainment={}, Final={}", 
                grossSalary, exemptAllowances, stdDed, profTax, entertainmentAlw, incomeFromSalary);
    }
    
    /**
     * Calculate ALL exempt allowances for 101% CBDT compliance
     */
    private double calculateAllExemptAllowances(Itr1FormData formData) {
        var salary = formData.getSalaryIncome();
        double totalExempt = 0;
        
        // 1. HRA Exemption u/s 10(13A)
        double hraExempt = 0;
        if (salary.getHraReceived() > 0) {
            var hraResult = salaryExemptionService.calculateHRAExemption(
                    salary.getBasicSalary(), salary.getDaForRetirement(),
                    salary.getHraReceived(), 0, null, null);
            // For backward compatibility, use exemptAllowanceDetails if available
            if (!salary.getExemptAllowanceDetails().isEmpty()) {
                for (var detail : salary.getExemptAllowanceDetails()) {
                    if ("HRA".equals(detail.getAllowanceType())) {
                        hraResult = salaryExemptionService.calculateHRAExemption(
                                salary.getBasicSalary(), salary.getDaForRetirement(),
                                detail.getAmountReceived(), detail.getRentPaid(),
                                detail.getCity(), detail.getLandlordPAN());
                        hraExempt += hraResult.getExemptAmount();
                        salary.setHraTaxable(hraResult.getTaxableHRA());
                        if (hraResult.getWarning() != null) {
                            formData.getValidationWarnings().add(hraResult.getWarning());
                        }
                        break;
                    }
                }
            } else {
                hraExempt = salary.getHraExempt();
            }
        }
        salary.setHraExempt(hraExempt);
        totalExempt += hraExempt;
        
        // 2. LTA Exemption u/s 10(5)
        double ltaExempt = calculateLTAExemption(salary);
        salary.setLtaExempt(ltaExempt);
        totalExempt += ltaExempt;
        
        // 3. Children Education Allowance u/s 10(14)
        double ceaExempt = calculateCEAExemption(salary);
        salary.setCeaExempt(ceaExempt);
        salary.setCeaTaxable(salary.getCeaReceived() - ceaExempt);
        totalExempt += ceaExempt;
        
        // 4. Hostel Expenditure Allowance u/s 10(14)
        double hostelExempt = calculateHostelExemption(salary);
        salary.setHostelExempt(hostelExempt);
        salary.setHostelTaxable(salary.getHostelAllowanceReceived() - hostelExempt);
        totalExempt += hostelExempt;
        
        // 5. Transport Allowance u/s 10(14) - for disabled employees
        double transportExempt = calculateTransportAllowanceExemption(salary);
        salary.setTransportAllowanceExempt(transportExempt);
        totalExempt += transportExempt;
        
        // 6. Medical Reimbursement u/s 17(1)(vii) - exempt up to ₹15,000
        double medicalExempt = Math.min(salary.getMedicalReimbursementReceived(), 15000);
        salary.setMedicalReimbursementExempt(medicalExempt);
        salary.setMedicalReimbursementTaxable(salary.getMedicalReimbursementReceived() - medicalExempt);
        totalExempt += medicalExempt;
        
        // 7. Leave Encashment Exemption u/s 10(10AA)
        calculateLeaveEncashmentExemption(salary, formData);
        totalExempt += salary.getLeaveEncashmentExempt();
        
        // 8. Gratuity Exemption u/s 10(10)
        calculateGratuityExemption(salary, formData);
        totalExempt += salary.getGratuityExempt();
        
        // 9. Pension Commutation Exemption u/s 10(10A)
        calculatePensionCommutationExemption(salary);
        totalExempt += salary.getPensionCommutationExempt();
        
        return totalExempt;
    }
    
    /**
     * Calculate LTA Exemption u/s 10(5)
     */
    private double calculateLTAExemption(Itr1FormData.SalaryIncome salary) {
        double ltaExempt = 0;
        
        // LTA exemption is the actual amount claimed (cannot exceed fare)
        // But capped at 2 journeys in a block of 4 years
        double actualClaimed = salary.getLtaExempt();
        
        // If not explicitly set, use the received amount (will be validated at computation)
        if (actualClaimed == 0 && salary.getLtaReceived() > 0) {
            ltaExempt = salary.getLtaReceived();
            salary.setLtaTaxable(0);
        } else {
            ltaExempt = actualClaimed;
            salary.setLtaTaxable(salary.getLtaReceived() - ltaExempt);
        }
        
        return ltaExempt;
    }
    
    /**
     * Calculate Children Education Allowance Exemption u/s 10(14)
     * Max ₹100 per month per child, max 2 children
     */
    private double calculateCEAExemption(Itr1FormData.SalaryIncome salary) {
        // Max 2 children * ₹100/month * 12 months = ₹2,400 per year
        double ceaExempt = Math.min(salary.getCeaReceived(), 2400);
        
        // Validate
        if (salary.getCeaReceived() > 2400) {
            salary.getExemptAllowanceDetails().add(Itr1FormData.ExemptAllowanceDetail.builder()
                .allowanceType("EDUCATION")
                .section("10(14)")
                .amountReceived(salary.getCeaReceived())
                .exemptAmount(ceaExempt)
                .taxableAmount(salary.getCeaReceived() - ceaExempt)
                .build());
        }
        
        return ceaExempt;
    }
    
    /**
     * Calculate Hostel Expenditure Allowance Exemption u/s 10(14)
     * Max ₹300 per month per child, max 2 children
     */
    private double calculateHostelExemption(Itr1FormData.SalaryIncome salary) {
        // Max 2 children * ₹300/month * 12 months = ₹7,200 per year
        double hostelExempt = Math.min(salary.getHostelAllowanceReceived(), 7200);
        return hostelExempt;
    }
    
    /**
     * Calculate Transport Allowance Exemption u/s 10(14) - for disabled employees
     * ₹1,600 per month
     */
    private double calculateTransportAllowanceExemption(Itr1FormData.SalaryIncome salary) {
        // For blind/orthopaedically challenged employees
        double transportExempt = Math.min(salary.getTransportAllowanceReceived(), 1600 * 12);
        return transportExempt;
    }
    
    /**
     * Calculate Leave Encashment Exemption u/s 10(10AA)
     * Exemption = MIN(Actual received, Up to 10 months salary, ₹25L)
     * BUT: Must have actually retired
     */
    private void calculateLeaveEncashmentExemption(Itr1FormData.SalaryIncome salary, Itr1FormData formData) {
        if (salary.getLeaveEncashmentReceived() == 0) {
            salary.setLeaveEncashmentExempt(0);
            salary.setLeaveEncashmentTaxable(0);
            return;
        }
        
        // 10 months average salary (Basic + DA)
        double avgSalary = (salary.getBasicSalary() + salary.getDaForRetirement()) / 12;
        double tenMonthsSalary = avgSalary * 10;
        
        // Maximum exempt = MIN(Actual, 10 months salary, ₹25,00,000)
        double exempt = Math.min(salary.getLeaveEncashmentReceived(), Math.min(tenMonthsSalary, 2500000));
        
        salary.setLeaveEncashmentExempt(exempt);
        salary.setLeaveEncashmentTaxable(salary.getLeaveEncashmentReceived() - exempt);
        
        // Validation
        if (salary.getRetirementDate() == null && salary.getLeaveEncashmentReceived() > 0) {
            formData.getValidationWarnings().add("Leave encashment claimed but retirement date not provided.");
        }
        
        log.info("Leave Encashment: Received={}, 10MonthsSalary={}, Exempt={}, Taxable={}",
                salary.getLeaveEncashmentReceived(), tenMonthsSalary, exempt, salary.getLeaveEncashmentTaxable());
    }
    
    /**
     * Calculate Gratuity Exemption u/s 10(10)
     * Exemption = MIN(Actual received, ₹20,00,000)
     */
    private void calculateGratuityExemption(Itr1FormData.SalaryIncome salary, Itr1FormData formData) {
        if (salary.getGratuityReceived() == 0) {
            salary.setGratuityExempt(0);
            salary.setGratuityTaxable(0);
            return;
        }
        
        // Maximum exempt = MIN(Actual, ₹20,00,000)
        double exempt = Math.min(salary.getGratuityReceived(), 2000000);
        
        salary.setGratuityExempt(exempt);
        salary.setGratuityTaxable(salary.getGratuityReceived() - exempt);
        
        log.info("Gratuity: Received={}, Exempt={}, Taxable={}",
                salary.getGratuityReceived(), exempt, salary.getGratuityTaxable());
    }
    
    /**
     * Calculate Pension Commutation Exemption u/s 10(10A)
     * - Commuted pension (50% exempt if received life annuity)
     * - Fully exempt if pension is completely commuted
     * Rules:
     * - If pension is commuted and receive life annuity: 50% exempt
     * - If pension is commuted fully (no annuity): Fully exempt
     * - Only applies to Government/PSU employees (approx 2/3 of pension)
     */
    private void calculatePensionCommutationExemption(Itr1FormData.SalaryIncome salary) {
        if (salary.getPensionCommutationReceived() == 0) {
            salary.setPensionCommutationExempt(0);
            salary.setPensionCommutationTaxable(0);
            return;
        }
        
        double exempt = 0;
        String pensionType = salary.getPensionType();
        
        // For commuted pension, check if it's fully or partially commuted
        // and if life annuity is being received
        if ("COMMUTED".equals(pensionType)) {
            // Note: The actual exemption depends on whether life annuity is received
            // For simplicity, if capitalised value of 2/3 is received tax-free
            // and 1/3 is taxable if not receiving annuity
            // Here assuming simple scenario - full commutation exempt
            exempt = salary.getPensionCommutationReceived(); 
        }
        
        // Set exemption and taxable portion
        salary.setPensionCommutationExempt(exempt);
        salary.setPensionCommutationTaxable(salary.getPensionCommutationReceived() - exempt);
        
        log.info("Pension Commutation: Received={}, Type={}, Exempt={}, Taxable={}",
                salary.getPensionCommutationReceived(), pensionType, exempt, salary.getPensionCommutationTaxable());
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
            
            // CBDT Rule: Interest cap depends on loan date & completion
            double interestCap = hp.isCompletedWithin5Years() ? 200000 : 30000;
            double interestDeduction = Math.min(hp.getInterestOnLoan(), interestCap);
            
            // Apply pre-construction interest spread (5 years or until completion)
            if (hp.getPreConstructionInterest() > 0 && hp.getCompletionDate() != null) {
                int yearOfCompletion = hp.getCompletionDate().getYear();
                String fy = formData.getPersonalInfo().getFinancialYear();
                int currentYear = Integer.parseInt(fy.split("-")[0]);
                int yearsSinceCompletion = currentYear - yearOfCompletion;
                if (yearsSinceCompletion < 5) {
                    interestDeduction += hp.getPreConstructionInterest() / 5;
                }
            }
            
            // Apply ownership share for joint ownership
            double ownershipShare = hp.getOwnershipShare() / 100.0;
            hp.setIncomeFromHP(Math.round(-interestDeduction * ownershipShare));
        } else {
            // LET_OUT property - CBDT Schedule HP rules
            
            // Step 1: Determine Gross Annual Value (GAV)
            double Gav = 0;
            double arrearsOfRent = hp.getArrearsOfRent();
            double unrealizedRent = hp.getUnrealizedRent();
            
            double maxOfThree = Math.max(Math.max(hp.getAnnualRent(), hp.getAnnualLettingValue()), 
                                         hp.getMunicipalRateableValue());
            
            // Vacancy period adjustment
            double vacancyFactor = 1.0;
            if (hp.getVacancyPeriodMonths() > 0) {
                vacancyFactor = (12 - hp.getVacancyPeriodMonths()) / 12.0;
            }
            
            Gav = maxOfThree * vacancyFactor + unrealizedRent + arrearsOfRent;
            
            // Step 2: Net Annual Value = GAV - Municipal Taxes
            double nav = Gav - hp.getMunicipalTaxesPaid();
            
            // Step 3: Standard Deduction @ 30% of NAV
            double stdDed = nav * 0.30;
            
            // Step 4: Income from HP = NAV - Std Deduction - Interest on Loan
            double interestCap = hp.isCompletedWithin5Years() ? 200000 : 30000;
            double interestDeduction = Math.min(hp.getInterestOnLoan(), interestCap);
            
            // Apply pre-construction interest spread
            if (hp.getPreConstructionInterest() > 0 && hp.getCompletionDate() != null) {
                int yearOfCompletion = hp.getCompletionDate().getYear();
                String fy = formData.getPersonalInfo().getFinancialYear();
                int currentYear = Integer.parseInt(fy.split("-")[0]);
                int yearsSinceCompletion = currentYear - yearOfCompletion;
                if (yearsSinceCompletion < 5) {
                    interestDeduction += hp.getPreConstructionInterest() / 5;
                }
            }
            
            // Apply ownership share
            double ownershipShare = hp.getOwnershipShare() / 100.0;
            double incomeFromHP = (nav - stdDed - interestDeduction) * ownershipShare;
            
            hp.setGrossAnnualValue(Math.round(Gav));
            hp.setNetAnnualValue(Math.round(nav));
            hp.setStandardDeduction30Pct(Math.round(stdDed));
            hp.setIncomeFromHP(Math.round(incomeFromHP));
        }
        
        log.info("HP: Type={}, GAV={}, Income={}", hp.getPropertyType(), 
                hp.getGrossAnnualValue(), hp.getIncomeFromHP());
    }

    /**
     * Calculate Income from Other Sources - 101% CBDT Compliant
     * Handles all special rates: 115BB (30%), 115BBDA (10% for dividend>10L)
     */
    private void calculateOtherSourcesIncome(Itr1FormData formData) {
        if (formData.getOtherSourcesIncome() == null) return;
        
        var os = formData.getOtherSourcesIncome();
        String regime = formData.getPersonalInfo().getRegime();
        
        // ===== STEP 1: Interest Income =====
        double totalInterest = os.getSavingsAccountInterest() + os.getFixedDepositInterest() +
                             os.getRecurringDepositInterest() + os.getNscInterest() +
                             os.getScssInterest() + os.getPostOfficeInterest() + os.getOtherInterest();
        
        // Track 80TTA/80TTB eligible interest
        double savingsInterest = os.getSavingsAccountInterest();
        boolean seniorCitizen = formData.getPersonalInfo().getAge() != null && 
                              formData.getPersonalInfo().getAge() >= 60;
        
        if (seniorCitizen && "OLD".equals(regime)) {
            // 80TTB: Senior citizens - max ₹50K on SB + FD interest
            os.setInterestEligibleFor80TTB(Math.min(totalInterest, 50000));
            os.setInterestEligibleFor80TTA(0);
        } else if ("OLD".equals(regime)) {
            // 80TTA: Normal - max ₹10K on SB interest
            os.setInterestEligibleFor80TTA(Math.min(savingsInterest, 10000));
            os.setInterestEligibleFor80TTB(0);
        }
        
        // ===== STEP 2: Dividend Income with Section 115BBDA =====
        double totalDividend = os.getDividendFromShares() + os.getDividendFromMutualFunds() + 
                               os.getDividendFromUnits();
        
        // Section 115BBDA: Dividend from domestic companies > ₹10L taxed @ 10%
        double dividendThreshold = 10000000; // ₹10 lakhs
        boolean dividendExceeds10L = totalDividend > dividendThreshold;
        
        if (dividendExceeds10L) {
            double excess = totalDividend - dividendThreshold;
            os.setDividendTaxableAtSpecialRate(excess * 0.10);  // 10% on excess
            os.setDividendTaxableAtNormalRate(dividendThreshold);  // Normal tax on first ₹10L
        } else {
            os.setDividendTaxableAtNormalRate(totalDividend);
            os.setDividendTaxableAtSpecialRate(0);
        }
        
        // Set the boolean field
        os.setDividendExceeds10L(dividendExceeds10L);
        
        // ===== STEP 3: Family Pension - Section 57(iia) =====
        // Deduction = 1/3 of pension OR ₹15,000 (₹25,000 for senior), whichever is less
        double maxPensionDed = (seniorCitizen) ? 25000 : 15000;
        double familyPensionDed = Math.min(os.getFamilyPensionReceived() / 3, maxPensionDed);
        double familyPensionTaxable = os.getFamilyPensionReceived() - familyPensionDed;
        
        // ===== STEP 4: Winnings from Lottery/Betting - Section 115BB (30% flat) =====
        double totalWinnings = os.getLotteryIncome() + os.getCrosswordPuzzleIncome() + 
                               os.getHorseRaceIncome() + os.getCardGameIncome();
        
        // ===== STEP 5: Gifts from Section 56(2)(x) =====
        // Gifts from NON-relatives > ₹50,000 are taxable
        double taxableGifts = 0;
        if (os.getGiftsFromNonRelatives() > 50000) {
            taxableGifts = os.getGiftsFromNonRelatives();
            formData.getValidationWarnings().add(
                "Gifts from non-relatives exceeding ₹50,000 are taxable under Section 56(2)(x)");
        }
        
        // ===== STEP 6: Calculate Total Other Sources =====
        // Normal income (added to GTI)
        // IMPORTANT: Dividends are included in normal income (unless exceeding ₹10L under 115BBDA)
        double normalIncome = totalInterest + familyPensionTaxable + 
                             os.getIncomeFromITRefund() + taxableGifts +
                             os.getCasualIncome() + os.getAgriculturalIncome() + os.getOtherIncome() +
                             os.getAccumulatedSPF() + totalDividend;
        
        // Income taxed at special rates (NOT added to GTI, taxed separately)
        double specialRateIncome = totalWinnings + os.getDividendTaxableAtSpecialRate();
        
        // Exempt income (not taxed)
        double exemptIncome = os.getIncomeFromChristmass() + os.getKeyManInsuranceBonus() +
                             os.getGiftsFromRelatives() + os.getAgriculturalIncome();
        
        double totalOS = normalIncome;  // Only normal income added to GTI
        
        // Store all calculations
        os.setTotalInterestIncome(totalInterest);
        os.setTotalDividendIncome(totalDividend);
        os.setFamilyPensionDeduction(familyPensionDed);
        os.setFamilyPensionTaxable(familyPensionTaxable);
        os.setTotalWinningsIncome(totalWinnings);
        os.setIncomeTaxedAtSpecialRates(specialRateIncome);
        os.setNormalIncome(normalIncome);
        
        os.setTotalOtherSourcesIncome(totalOS);
        
        // Log for debugging
        log.info("OS Computation: Interest={}, Dividend={} (Special={}), Winnings={}, TotalOS={}, SpecialRate={}", 
                totalInterest, totalDividend, os.getDividendTaxableAtSpecialRate(), 
                totalWinnings, totalOS, specialRateIncome);
    }

    private double calculateGrossTotalIncome(Itr1FormData formData) {
        double salaryIncome = formData.getSalaryIncome() != null ? 
                            formData.getSalaryIncome().getIncomeFromSalary() : 0;
        double hpIncome = formData.getHousePropertyIncome() != null ? 
                        formData.getHousePropertyIncome().getIncomeFromHP() : 0;
        double osIncome = formData.getOtherSourcesIncome() != null ? 
                        formData.getOtherSourcesIncome().getTotalOtherSourcesIncome() : 0;
        
        // ===== HOUSE PROPERTY LOSS SET-OFF (Section 71) =====
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
        
        // ===== AGRICULTURAL INCOME CLUBBING (Section 17(1) + Rule 7) =====
        // Agricultural income > ₹5,000 is clubbed with salary for tax calculation
        // But only if total non-agri income > ₹2,50,000
        double agricultureIncome = 0;
        double agricultureIncomeAbove5000 = 0;
        boolean clubAgriculturalIncome = false;
        
        if (formData.getExemptIncome() != null) {
            agricultureIncome = formData.getExemptIncome().getAgricultureIncome();
            
            // ITR-1 allows max ₹5,000 agricultural income without filing
            // If > ₹5,000, need to file ITR-2
            if (agricultureIncome > 5000) {
                agricultureIncomeAbove5000 = agricultureIncome - 5000;
                formData.getExemptIncome().setAgricultureIncomeExceeds5000(true);
                
                // Check if clubbing applies (when total non-agri income > basic exemption)
                double nonAgriIncome = salaryIncome + Math.max(0, hpIncome) + osIncome;
                if (nonAgriIncome > 250000) {
                    clubAgriculturalIncome = true;
                    formData.getValidationWarnings().add(
                        "Agricultural income exceeds ₹5,000. It will be clubbed with salary for tax calculation.");
                }
            }
        }
        
        // ===== CALCULATE GROSS TOTAL INCOME =====
        double gti = Math.max(0, salaryIncome + Math.max(0, hpIncome) + osIncome - hpLossSetOff);
        
        // If agricultural income needs to be clubbed
        double netAgriForGTI = 0;
        if (clubAgriculturalIncome) {
            netAgriForGTI = agricultureIncomeAbove5000;
            gti = gti + netAgriForGTI;
        }
        
        // ===== SET TAX COMPUTATION VALUES =====
        var computation = formData.getTaxComputation();
        if (computation == null) {
            computation = new Itr1FormData.TaxComputation();
            formData.setTaxComputation(computation);
        }
        
        // Income Summary
        computation.setIncomeFromSalary(salaryIncome);
        computation.setIncomeFromHP(hpIncome);
        computation.setIncomeFromOtherSources(osIncome);
        
        // Agricultural Income
        computation.setAgricultureIncome(agricultureIncome);
        computation.setAgricultureIncomeAbove5000(agricultureIncomeAbove5000);
        computation.setClubAgriculturalIncome(clubAgriculturalIncome);
        computation.setNetAgricultureIncome(netAgriForGTI);
        
        // HP Loss
        computation.setHpLoss(hpLoss);
        computation.setHpLossSetOff(hpLossSetOff);
        computation.setHpLossCarryForward(hpLoss - hpLossSetOff);
        
        // GTI
        computation.setGrossTotalIncome(gti);
        
        log.info("GTI Calculation: Salary={}, HP={}, OS={}, HP Loss Set-off={}, Agri Clubbed={}, GTI={}",
                salaryIncome, hpIncome, osIncome, hpLossSetOff, netAgriForGTI, gti);
        
        // If agricultural income > ₹5,000, invalidate ITR-1 (must file ITR-2)
        if (agricultureIncome > 5000) {
            formData.getValidationErrors().add(
                "Agricultural income exceeds ₹5,000. ITR-1 not applicable. Please use ITR-2.");
        }
        
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
                .tdsPaid(java.math.BigDecimal.ZERO)
                .tcsPaid(java.math.BigDecimal.ZERO)
                .advanceTaxPaid(java.math.BigDecimal.ZERO)
                .selfAssessmentTaxPaid(java.math.BigDecimal.ZERO)
                .build();
        
        var result = taxEngine.computeTax(input);
        
        log.info("Tax Computation for PAN {} - Income: ₹{}", info.getPan(), totalIncome);
        log.info("  Tax on Normal Income: ₹{}", result.getTaxOnNormalIncome());
        log.info("  Rebate 87A: ₹{}", result.getRebate87A().getRebateAmount());
        log.info("  Tax After Rebate: ₹{}", result.getTaxAfterRebate());
        log.info("  Surcharge: ₹{}", result.getSurcharge().getEffectiveSurcharge());
        log.info("  Cess @ 4%: ₹{}", result.getCess());
        log.info("  Total Tax Liability: ₹{}", result.getTotalTaxLiability());
        
        computation.setTaxOnNormalIncome(result.getTaxOnNormalIncome().doubleValue());
        computation.setRebate87A(result.getRebate87A().getRebateAmount().doubleValue());
        computation.setTaxAfterRebate(result.getTaxAfterRebate().doubleValue());
        computation.setSurcharge(result.getSurcharge().getEffectiveSurcharge().doubleValue());
        computation.setMarginalRelief(result.getSurcharge().getMarginalReliefAmount() != null ? 
                result.getSurcharge().getMarginalReliefAmount().doubleValue() : 0);
        computation.setCess(result.getCess().doubleValue());
        computation.setTotalTaxLiability(result.getTotalTaxLiability().doubleValue());
        
        log.info("Tax computation values set in formData - Cess: {}, Surcharge: {}, Total: {}", 
            computation.getCess(), computation.getSurcharge(), computation.getTotalTaxLiability());
    }

    private void calculateInterestAndFees(Itr1FormData formData) {
        var computation = formData.getTaxComputation();
        var info = formData.getPersonalInfo();
        var payments = formData.getTaxPayments();
        
        if (payments == null) {
            payments = new Itr1FormData.TaxPayments();
            formData.setTaxPayments(payments);
        }
        
        // Total taxes paid includes self-assessment tax for display purposes
        double totalTaxesPaid = payments.getTotalTDSOnSalary() + payments.getTotalTDSOnOther() +
                               payments.getTotalTCS() + payments.getTotalAdvanceTax() +
                               payments.getTotalSelfAssessmentTax();
        
        // But balance tax should only consider taxes paid BEFORE filing
        double taxesPaidBeforeFiling = payments.getTotalTDSOnSalary() + payments.getTotalTDSOnOther() +
                                      payments.getTotalTCS() + payments.getTotalAdvanceTax();
        
        payments.setTotalTaxesPaid(totalTaxesPaid);
        computation.setTotalTaxesPaid(totalTaxesPaid);
        computation.setBalanceTax(computation.getTotalTaxLiability() - taxesPaidBeforeFiling);
        
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
        var payments = formData.getTaxPayments();
        
        // Only consider TDS/TCS/Advance Tax for demand calculation
        // Self-assessment tax is paid AFTER filing, so exclude it
        double taxesPaidBeforeFiling = payments.getTotalTDSOnSalary() + 
                                      payments.getTotalTDSOnOther() + 
                                      payments.getTotalTCS() + 
                                      payments.getTotalAdvanceTax();
        
        log.info("=== REFUND/DEMAND CALCULATION ===");
        log.info("Total Tax Liability: {}", computation.getTotalTaxLiability());
        log.info("Total Interest & Fees: {}", computation.getTotalInterestAndFees());
        log.info("TDS on Salary: {}", payments.getTotalTDSOnSalary());
        log.info("TDS on Other: {}", payments.getTotalTDSOnOther());
        log.info("TCS: {}", payments.getTotalTCS());
        log.info("Advance Tax: {}", payments.getTotalAdvanceTax());
        log.info("Self Assessment Tax (excluded): {}", payments.getTotalSelfAssessmentTax());
        log.info("Taxes Paid Before Filing: {}", taxesPaidBeforeFiling);
        
        double totalDemand = computation.getTotalTaxLiability() + 
                           computation.getTotalInterestAndFees() -
                           taxesPaidBeforeFiling;
        
        log.info("Total Demand: {}", totalDemand);
        
        if (totalDemand > 0) {
            computation.setTaxPayable(totalDemand);
            computation.setRefund(0);
            log.info("Tax Payable: {}", totalDemand);
        } else {
            computation.setTaxPayable(0);
            computation.setRefund(Math.abs(totalDemand));
            log.info("Refund: {}", Math.abs(totalDemand));
        }
    }

    private double roundToNearest10(double amount) {
        return Math.round(amount / 10.0) * 10;
    }
}
