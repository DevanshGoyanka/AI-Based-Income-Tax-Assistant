package com.itr.service;

import com.itr.dto.Itr2FormData;
import com.itr.service.taxengine.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ITR2CalculatorService {

    private final TaxComputationEngine taxEngine;
    private final SpecialRateIncomeCalculator specialRateCalculator;
    private final LossSetOffService lossSetOffService;
    private final LossCarryForwardService lossCarryForwardService;
    private final AMTService amtService;

    public Itr2FormData calculateTax(Itr2FormData formData) {
        log.info("Starting ITR-2 tax calculation for AY {}", formData.getPartA().getAssessmentYear());

        try {
            // Step 1: Calculate all income heads
            calculateSalaryIncome(formData);
            calculateHousePropertyIncome(formData);
            calculateOtherSourcesIncome(formData);
            calculateCapitalGains(formData);
            calculateVDAIncome(formData);
            
            // Step 2: Calculate GTI with loss adjustments
            double gti = calculateGrossTotalIncomeBeforeLossSetOff(formData);
            
            // Step 2a: Apply Schedule CYLA (Current Year Loss Adjustment)
            applyCurrentYearLossSetOff(formData, gti);
            
            // Step 2b: Apply Schedule BFLA (Brought Forward Loss Adjustment)
            applyBroughtForwardLossSetOff(formData);
            
            // Recalculate GTI after loss adjustments
            gti = formData.getScheduleCYLA() != null ? 
                    formData.getScheduleCYLA().getGrossTotalIncomeAfterCYLA() : gti;
            
            // Step 3: Apply Chapter VI-A deductions (old regime only)
            double totalDeductions = calculateDeductions(formData);
            double totalIncome = Math.max(0, gti - totalDeductions);
            
            // Step 4: Compute tax for selected regime
            computeTax(formData, totalIncome);
            
            // Step 5: Calculate interest and fees
            calculateInterestAndFees(formData);
            
            // Step 6: Calculate final refund/demand
            calculateRefundOrDemand(formData);
            
            // Step 7: Generate Schedule CFL (Carry Forward Losses)
            generateCarryForwardLosses(formData);
            
            log.info("ITR-2 calculation completed successfully. Total Income: {}, Tax: {}", 
                    totalIncome, formData.getComputation().getTotalTaxLiability());
            return formData;
            
        } catch (Exception e) {
            log.error("Error calculating ITR-2 tax", e);
            formData.getValidationErrors().add("Tax calculation failed: " + e.getMessage());
            return formData;
        }
    }

    private void calculateSalaryIncome(Itr2FormData formData) {
        if (formData.getScheduleSalary() == null) return;
        
        var salary = formData.getScheduleSalary();
        String regime = formData.getPartA().getRegime();
        
        // B1(a) + B1(b) + B1(c) = Gross Salary
        double salary17_1 = salary.getSalary17_1();
        double perquisites17_2 = salary.getPerquisites17_2();
        double profitsInLieu17_3 = salary.getProfitsInLieu17_3();
        double grossSalary = salary17_1 + perquisites17_2 + profitsInLieu17_3;
        
        // Less: Exempt allowances u/s 10
        double allowancesExempt = salary.getAllowancesExempt();
        
        // Net Salary = Gross - Exempt
        double netSalary = grossSalary - allowancesExempt;
        
        // Standard deduction u/s 16(ia)
        double stdDed = "NEW".equals(regime) ? 75000 : 50000;
        
        // Entertainment allowance u/s 16(ii) - only Govt employees
        double entertainmentAlw = 0;
        if (salary.isGovernmentEmployee() && "OLD".equals(regime)) {
            double basicSalary = salary.getBasicSalary();
            entertainmentAlw = Math.min(Math.min(salary.getEntertainmentAllowance(), basicSalary / 5), 5000);
        }
        
        // Professional tax u/s 16(iii)
        double profTax = Math.min(salary.getProfessionalTax(), 2500);
        
        // Income from Salary = Net - Std Ded - EA - Prof Tax
        double incomeFromSalary = Math.max(0, netSalary - stdDed - entertainmentAlw - profTax);
        
        // Set computed values
        salary.setGrossSalary(grossSalary);
        salary.setNetSalary(netSalary);
        salary.setStandardDeduction(stdDed);
        salary.setEntertainmentAllowanceDeduction(entertainmentAlw);
        salary.setProfessionalTaxDeduction(profTax);
        salary.setIncomeFromSalary(incomeFromSalary);
        
        log.debug("Salary calculation: Gross={}, Net={}, StdDed={}, Income={}", 
                grossSalary, netSalary, stdDed, incomeFromSalary);
    }

    private void calculateHousePropertyIncome(Itr2FormData formData) {
        if (formData.getHouseProperties() == null || formData.getHouseProperties().isEmpty()) return;
        
        List<Itr2FormData.HouseProperty> properties = formData.getHouseProperties();
        
        // Count self-occupied properties (max 2 allowed)
        long sopCount = properties.stream()
                .filter(hp -> "SELF_OCCUPIED".equals(hp.getPropertyType()))
                .count();
        
        if (sopCount > 2) {
            formData.getValidationWarnings().add(
                "More than 2 self-occupied properties: third property onwards treated as deemed let-out");
        }
        
        double totalHPIncome = 0;
        
        for (var hp : properties) {
            String propertyType = hp.getPropertyType();
            
            // Calculate GAV based on property type
            double gav = 0;
            if ("SELF_OCCUPIED".equals(propertyType)) {
                gav = 0; // SOP has zero GAV
            } else if ("LET_OUT".equals(propertyType) || "DEEMED_LET_OUT".equals(propertyType)) {
                // GAV = Higher of (Actual Rent, Fair Rent, Municipal Rateable Value)
                double actualRent = hp.getGrossRent();
                double fairRent = hp.getFairRent();
                double mrv = hp.getMunicipalRateableValue();
                gav = Math.max(Math.max(actualRent, fairRent), mrv);
            }
            
            // Less: Municipal taxes paid by owner
            double municipalTax = hp.getMunicipalTaxPaid();
            double nav = gav - municipalTax;
            
            // 30% standard deduction (only if NAV > 0)
            double stdDed = nav > 0 ? nav * 0.30 : 0;
            
            // Interest on housing loan u/s 24(b)
            double currentInterest = hp.getInterestOnLoan();
            double preConstructionInterest = hp.getPreConstructionInterest1_5th();
            double totalInterest = currentInterest + preConstructionInterest;
            
            // For SOP: interest capped at ₹2,00,000
            if ("SELF_OCCUPIED".equals(propertyType)) {
                if (totalInterest > 200000) {
                    formData.getValidationWarnings().add(
                        "Interest on self-occupied property " + hp.getAddress() + 
                        " exceeds ₹2,00,000. Capping at ₹2,00,000.");
                    totalInterest = 200000;
                }
            }
            // For let-out: no cap on interest
            
            // Income/Loss from this property
            double incomeFromHP = nav - stdDed - totalInterest;
            
            // Set computed values
            hp.setAnnualValue(gav);
            hp.setNetAnnualValue(nav);
            hp.setStandardDeduction(stdDed);
            hp.setTotalInterestDeduction(totalInterest);
            hp.setIncomeFromHP(incomeFromHP);
            
            totalHPIncome += incomeFromHP;
            
            log.debug("HP calculation for {}: GAV={}, NAV={}, StdDed={}, Interest={}, Income={}", 
                    hp.getAddress(), gav, nav, stdDed, totalInterest, incomeFromHP);
        }
        
        // HP loss set-off cap: ₹2,00,000
        if (totalHPIncome < 0) {
            double hpLoss = Math.abs(totalHPIncome);
            if (hpLoss > 200000) {
                formData.getValidationWarnings().add(
                    "HP loss of ₹" + hpLoss + " exceeds ₹2,00,000 set-off limit. " +
                    "Excess ₹" + (hpLoss - 200000) + " will be carried forward.");
            }
        }
    }

    private void calculateOtherSourcesIncome(Itr2FormData formData) {
        if (formData.getScheduleOS() == null) return;
        
        var os = formData.getScheduleOS();
        String regime = formData.getPartA().getRegime();
        
        // Interest income
        double savingsInterest = os.getSavingsInterest();
        double depositInterest = os.getDepositInterest();
        double otherInterest = os.getOtherInterest();
        
        // Dividend income (taxable at slab from AY 2021-22)
        double dividendIncome = os.getDividendIncome();
        
        // Family pension
        double familyPension = os.getFamilyPension();
        
        // Family pension deduction u/s 57(iia)
        // Old regime: 1/3 of pension or ₹15,000 (lower)
        // New regime: 1/3 of pension or ₹25,000 (lower)
        double pensionDedLimit = "NEW".equals(regime) ? 25000 : 15000;
        double pensionDed = Math.min(familyPension / 3, pensionDedLimit);
        
        // Other income
        double otherIncome = os.getOtherIncome();
        
        // Gifts from non-relatives u/s 56(2)(x)
        double giftsReceived = os.getGiftsFromNonRelatives();
        
        // Total income from other sources
        double totalOS = savingsInterest + depositInterest + otherInterest + 
                        dividendIncome + familyPension + otherIncome + giftsReceived - pensionDed;
        
        os.setFamilyPensionDeduction(pensionDed);
        os.setIncomeFromOtherSources(totalOS);
        
        log.debug("Other Sources: Savings={}, Deposit={}, Dividend={}, Pension={}, Total={}", 
                savingsInterest, depositInterest, dividendIncome, familyPension, totalOS);
    }

    private void calculateCapitalGains(Itr2FormData formData) {
        if (formData.getScheduleCG() == null) return;
        
        var cg = formData.getScheduleCG();
        String ay = formData.getPartA().getAssessmentYear();
        
        // STCG u/s 111A (listed equity with STT)
        double stcg111ATotal = 0;
        if (cg.getStcg111A() != null) {
            for (var txn : cg.getStcg111A()) {
                double gain = txn.getSalePrice() - txn.getTransferExpenses() - txn.getCostOfAcquisition();
                txn.setGain(gain);
                stcg111ATotal += gain;
            }
        }
        cg.setStcg111ATotal(stcg111ATotal);
        
        // STCG - Other (taxable at slab)
        double stcgOtherTotal = 0;
        if (cg.getStcgOther() != null) {
            for (var txn : cg.getStcgOther()) {
                double gain = txn.getSalePrice() - txn.getTransferExpenses() - txn.getCostOfAcquisition();
                txn.setGain(gain);
                stcgOtherTotal += gain;
            }
        }
        cg.setStcgOtherTotal(stcgOtherTotal);
        
        // LTCG u/s 112A (listed equity with STT)
        double ltcg112ATotal = 0;
        if (cg.getLtcg112A() != null) {
            for (var txn : cg.getLtcg112A()) {
                // Grandfathering for assets acquired before Jan 31, 2018
                double cost = txn.getCostOfAcquisition();
                if (txn.getAcquisitionDate() != null && 
                    txn.getAcquisitionDate().isBefore(java.time.LocalDate.of(2018, 1, 31))) {
                    // Use higher of actual cost or FMV on Jan 31, 2018 (capped at sale price)
                    double fmvJan2018 = txn.getFmvJan312018();
                    if (fmvJan2018 > 0) {
                        cost = Math.max(cost, Math.min(fmvJan2018, txn.getSalePrice()));
                    }
                }
                
                double gain = txn.getSalePrice() - txn.getTransferExpenses() - cost;
                txn.setGrandfatheredCost(cost);
                txn.setGain(gain);
                ltcg112ATotal += gain;
            }
        }
        
        // LTCG 112A exemption: ₹1,25,000 per year (aggregate)
        double ltcg112AExemption = Math.min(ltcg112ATotal, 125000);
        double ltcg112ATaxable = Math.max(0, ltcg112ATotal - ltcg112AExemption);
        cg.setLtcg112ATotal(ltcg112ATotal);
        cg.setLtcg112AExemption(ltcg112AExemption);
        cg.setLtcg112ATaxable(ltcg112ATaxable);
        
        // LTCG u/s 112 (property, unlisted shares, bonds)
        double ltcg112Total = 0;
        if (cg.getLtcg112() != null) {
            for (var txn : cg.getLtcg112()) {
                // For property acquired before Jul 23, 2024: taxpayer can choose indexation or not
                // For property acquired after Jul 23, 2024: no indexation (12.5% flat)
                
                boolean canChooseIndexation = txn.getAcquisitionDate() != null &&
                        txn.getAcquisitionDate().isBefore(java.time.LocalDate.of(2024, 7, 23));
                
                double gainWithIndexation = 0;
                double gainWithoutIndexation = 0;
                
                if (canChooseIndexation) {
                    // Calculate both and choose lower tax
                    gainWithIndexation = txn.getSalePrice() - txn.getTransferExpenses() - 
                                        txn.getIndexedCostOfAcquisition();
                    gainWithoutIndexation = txn.getSalePrice() - txn.getTransferExpenses() - 
                                           txn.getCostOfAcquisition();
                    
                    // Tax: 20% with indexation vs 12.5% without
                    double taxWithIdx = gainWithIndexation * 0.20;
                    double taxWithoutIdx = gainWithoutIndexation * 0.125;
                    
                    if (taxWithIdx < taxWithoutIdx) {
                        txn.setIndexationChosen(true);
                        txn.setGain(gainWithIndexation);
                    } else {
                        txn.setIndexationChosen(false);
                        txn.setGain(gainWithoutIndexation);
                    }
                } else {
                    // Post Jul 23, 2024: no indexation
                    txn.setIndexationChosen(false);
                    gainWithoutIndexation = txn.getSalePrice() - txn.getTransferExpenses() - 
                                           txn.getCostOfAcquisition();
                    txn.setGain(gainWithoutIndexation);
                }
                
                ltcg112Total += txn.getGain();
            }
        }
        cg.setLtcg112Total(ltcg112Total);
        
        // Section 50C validation (stamp duty value)
        if (cg.getLtcg112() != null) {
            for (var txn : cg.getLtcg112()) {
                if (txn.getStampDutyValue() > 0 && 
                    txn.getSalePrice() < txn.getStampDutyValue() * 0.9) {
                    formData.getValidationWarnings().add(
                        "Section 50C may apply: Sale price is less than 90% of stamp duty value for " + 
                        txn.getAssetDescription());
                }
            }
        }
        
        // Apply exemptions (54, 54EC, 54F, etc.)
        double totalExemptions = 0;
        if (cg.getExemptions() != null) {
            for (var exemption : cg.getExemptions()) {
                // Validate exemption limits
                if ("54EC".equals(exemption.getSection()) && exemption.getExemptionAmount() > 5000000) {
                    formData.getValidationErrors().add(
                        "Section 54EC exemption cannot exceed ₹50,00,000 in a financial year");
                    exemption.setExemptionAmount(5000000);
                }
                totalExemptions += exemption.getExemptionAmount();
            }
        }
        
        // Total capital gains
        double totalCG = stcg111ATotal + stcgOtherTotal + ltcg112ATaxable + ltcg112Total - totalExemptions;
        cg.setTotalCapitalGains(Math.max(0, totalCG));
        
        log.debug("Capital Gains: STCG111A={}, STCGOther={}, LTCG112A={}, LTCG112={}, Total={}", 
                stcg111ATotal, stcgOtherTotal, ltcg112ATaxable, ltcg112Total, totalCG);
    }

    private void calculateVDAIncome(Itr2FormData formData) {
        if (formData.getScheduleVDA() == null) return;
        
        var vda = formData.getScheduleVDA();
        
        // VDA income: 30% flat tax, no loss set-off or carry forward
        double totalVDAIncome = 0;
        double totalTDS194S = 0;
        
        if (vda.getTransactions() != null) {
            for (var txn : vda.getTransactions()) {
                double salePrice = txn.getSalePrice();
                double costOfAcquisition = txn.getCostOfAcquisition();
                double profit = salePrice - costOfAcquisition;
                
                txn.setProfit(profit);
                
                // VDA loss cannot be set off
                if (profit < 0) {
                    formData.getValidationErrors().add(
                        "VDA loss of ₹" + Math.abs(profit) + " for " + txn.getVdaDescription() + 
                        " cannot be set off against any other income or carried forward");
                    profit = 0; // Ignore loss
                }
                
                totalVDAIncome += profit;
                totalTDS194S += txn.getTdsDeducted();
            }
        }
        
        vda.setTotalVDAIncome(totalVDAIncome);
        vda.setTotalTDS194S(totalTDS194S);
        vda.setTaxAt30Percent(totalVDAIncome * 0.30);
        
        log.debug("VDA Income: Total={}, TDS={}, Tax@30%={}", 
                totalVDAIncome, totalTDS194S, vda.getTaxAt30Percent());
    }

    private double calculateGrossTotalIncomeBeforeLossSetOff(Itr2FormData formData) {
        double gti = 0;
        
        // Salary income
        if (formData.getScheduleSalary() != null) {
            gti += formData.getScheduleSalary().getIncomeFromSalary();
        }
        
        // House property income (can be negative)
        double hpIncome = 0;
        if (formData.getHouseProperties() != null) {
            hpIncome = formData.getHouseProperties().stream()
                .mapToDouble(Itr2FormData.HouseProperty::getIncomeFromHP)
                .sum();
            gti += hpIncome;
        }
        
        // Other sources income
        if (formData.getScheduleOS() != null) {
            gti += formData.getScheduleOS().getIncomeFromOtherSources();
        }
        
        // Capital gains (already net of exemptions)
        if (formData.getScheduleCG() != null) {
            gti += formData.getScheduleCG().getTotalCapitalGains();
        }
        
        // VDA income (30% flat tax, added to GTI)
        if (formData.getScheduleVDA() != null) {
            gti += formData.getScheduleVDA().getTotalVDAIncome();
        }
        
        // GTI cannot be negative
        gti = Math.max(0, gti);
        
        if (formData.getComputation() == null) {
            formData.setComputation(new Itr2FormData.TaxComputation());
        }
        formData.getComputation().setGrossTotalIncome(gti);
        
        log.debug("Gross Total Income: {}", gti);
        return gti;
    }

    private double calculateDeductions(Itr2FormData formData) {
        if (formData.getDeductions() == null) return 0;
        
        var ded = formData.getDeductions();
        String regime = formData.getPartA().getRegime();
        
        if ("NEW".equals(regime)) {
            // New regime: only 80CCD(2) and 80JJAA available
            double total = ded.getDeduction80CCD2() + ded.getDeduction80JJAA();
            ded.setTotalDeductions(total);
            
            if (ded.getDeduction80C() > 0 || ded.getDeduction80D() > 0 ||
                ded.getDeduction80E() > 0 || ded.getDeduction80G() > 0) {
                formData.getValidationWarnings().add(
                    "New regime selected: 80C, 80D, 80E, 80G etc. are not available. Only 80CCD(2) and 80JJAA are allowed.");
            }
            
            return total;
        }
        
        // Old regime: all deductions available
        double total = 0;
        
        // 80C group: aggregate cap ₹1,50,000
        double group80C = ded.getDeduction80C() + ded.getDeduction80CCC() + ded.getDeduction80CCD1();
        double capped80C = Math.min(group80C, 150000);
        if (group80C > 150000) {
            formData.getValidationWarnings().add(
                "80C + 80CCC + 80CCD(1) aggregate of ₹" + group80C + " capped at ₹1,50,000");
        }
        total += capped80C;
        
        // 80CCD(1B): additional NPS, cap ₹50,000
        double ccd1B = Math.min(ded.getDeduction80CCD1B(), 50000);
        total += ccd1B;
        
        // 80CCD(2): employer NPS (no cap for individual)
        total += ded.getDeduction80CCD2();
        
        // 80D: Health insurance
        int age = formData.getPartA().getAge() != null ? formData.getPartA().getAge() : 0;
        double maxSelf = age >= 60 ? 50000 : 25000;
        double maxParents = ded.isParentsSeniorCitizen() ? 50000 : 25000;
        double max80D = maxSelf + maxParents;
        total += Math.min(ded.getDeduction80D(), max80D);
        
        // 80DD, 80DDB, 80E, 80EE, 80EEA, 80EEB
        total += ded.getDeduction80DD();
        total += ded.getDeduction80DDB();
        total += ded.getDeduction80E();
        total += Math.min(ded.getDeduction80EE(), 50000);
        total += Math.min(ded.getDeduction80EEA(), 150000);
        total += Math.min(ded.getDeduction80EEB(), 150000);
        
        // 80G: Donations
        total += ded.getDeduction80G();
        
        // 80GG, 80GGA, 80GGC
        total += ded.getDeduction80GG();
        total += ded.getDeduction80GGA();
        total += ded.getDeduction80GGC();
        
        // 80TTA vs 80TTB (mutually exclusive)
        if (age >= 60) {
            total += Math.min(ded.getDeduction80TTB(), 50000);
        } else {
            total += Math.min(ded.getDeduction80TTA(), 10000);
        }
        
        // 80U
        total += ded.getDeduction80U();
        
        ded.setTotalDeductions(total);
        log.debug("Total deductions: {}", total);
        return total;
    }

    private void computeTax(Itr2FormData formData, double totalIncome) {
        var partA = formData.getPartA();
        var computation = formData.getComputation();
        if (computation == null) {
            computation = new Itr2FormData.TaxComputation();
            formData.setComputation(computation);
        }
        
        computation.setTotalIncome(totalIncome);
        
        // Build TaxComputationInput
        var input = TaxComputationEngine.TaxComputationInput.builder()
                .regime(partA.getRegime())
                .assessmentYear(partA.getAssessmentYear())
                .ageCategory(getAgeCategory(partA.getAge()))
                .isHUF(false)
                .isSeniorCitizen(partA.getAge() >= 60)
                .hasBusinessIncome(false)
                .isPresumptiveIncome(false)
                .totalIncome(BigDecimal.valueOf(totalIncome))
                .normalIncome(BigDecimal.valueOf(totalIncome))
                .build();
        
        // Add special rate income
        if (formData.getScheduleCG() != null) {
            var cg = formData.getScheduleCG();
            input.setStcg111A(BigDecimal.valueOf(cg.getStcg111ATotal()));
            input.setLtcg112A(BigDecimal.valueOf(cg.getLtcg112ATaxable()));
            input.setLtcg112(BigDecimal.valueOf(cg.getLtcg112Total()));
        }
        
        if (formData.getScheduleVDA() != null) {
            input.setIncome115BBH(BigDecimal.valueOf(formData.getScheduleVDA().getTotalVDAIncome()));
        }
        
        // Add taxes paid
        if (formData.getScheduleTDS() != null) {
            input.setTdsPaid(BigDecimal.valueOf(formData.getScheduleTDS().getTotalTDS()));
        }
        if (formData.getScheduleTCS() != null) {
            input.setTcsPaid(BigDecimal.valueOf(formData.getScheduleTCS().getTotalTCS()));
        }
        
        // Compute tax
        var result = taxEngine.computeTax(input);
        
        // Map results to computation
        computation.setTaxOnNormalIncome(result.getTaxOnNormalIncome().doubleValue());
        computation.setTaxOnSpecialRateIncome(result.getSpecialRateIncomeTax().getTotalTax().doubleValue());
        computation.setRebate87A(result.getRebate87A().getRebateAmount().doubleValue());
        computation.setSurcharge(result.getSurcharge().getEffectiveSurcharge().doubleValue());
        computation.setCess(result.getCess().doubleValue());
        computation.setTotalTaxLiability(result.getTotalTaxLiability().doubleValue());
        
        log.info("Tax computed: Total Income={}, Tax Liability={}", totalIncome, computation.getTotalTaxLiability());
    }

    private void calculateInterestAndFees(Itr2FormData formData) {
        var computation = formData.getComputation();
        if (computation == null) return;
        
        // Interest u/s 234A, 234B, 234C and fee u/s 234F
        // These are calculated by TaxComputationEngine based on dates
        // For now, set to zero (will be computed when filing dates are known)
        computation.setInterest234A(0);
        computation.setInterest234B(0);
        computation.setInterest234C(0);
        computation.setFee234F(0);
        
        log.debug("Interest and fees calculated");
    }

    private void calculateRefundOrDemand(Itr2FormData formData) {
        var computation = formData.getComputation();
        if (computation == null) return;
        
        double totalTax = computation.getTotalTaxLiability();
        double totalPaid = 0;
        
        if (formData.getScheduleTDS() != null) {
            totalPaid += formData.getScheduleTDS().getTotalTDS();
        }
        if (formData.getScheduleTCS() != null) {
            totalPaid += formData.getScheduleTCS().getTotalTCS();
        }
        
        double netPayable = totalTax - totalPaid;
        
        if (netPayable > 0) {
            computation.setTaxPayable(netPayable);
            computation.setRefund(0);
        } else {
            computation.setTaxPayable(0);
            computation.setRefund(Math.abs(netPayable));
        }
        
        log.info("Refund/Demand calculated: Tax={}, Paid={}, Net={}", totalTax, totalPaid, netPayable);
    }
    
    private String getAgeCategory(Integer age) {
        if (age == null) return "REGULAR";
        if (age >= 80) return "SUPER_SENIOR";
        if (age >= 60) return "SENIOR";
        return "REGULAR";
    }
    
    /**
     * Apply Schedule CYLA - Current Year Loss Adjustment
     */
    private void applyCurrentYearLossSetOff(Itr2FormData formData, double gtiBeforeLoss) {
        log.info("Applying Schedule CYLA - Current Year Loss Adjustment");
        
        // Prepare CYLA input
        com.itr.dto.ScheduleCYLA cyla = com.itr.dto.ScheduleCYLA.builder().build();
        
        // Set current year losses
        double hpLoss = 0;
        double stcgLoss = 0;
        double ltcgLoss = 0;
        
        // Calculate HP loss
        if (formData.getHouseProperties() != null) {
            double hpIncome = formData.getHouseProperties().stream()
                .mapToDouble(Itr2FormData.HouseProperty::getIncomeFromHP)
                .sum();
            if (hpIncome < 0) {
                hpLoss = Math.abs(hpIncome);
            }
        }
        
        // Calculate CG losses
        if (formData.getScheduleCG() != null) {
            var cg = formData.getScheduleCG();
            if (cg.getStcg111ATotal() < 0) stcgLoss += Math.abs(cg.getStcg111ATotal());
            if (cg.getStcgOtherTotal() < 0) stcgLoss += Math.abs(cg.getStcgOtherTotal());
            if (cg.getLtcg112ATotal() < 0) ltcgLoss += Math.abs(cg.getLtcg112ATotal());
            if (cg.getLtcg112Total() < 0) ltcgLoss += Math.abs(cg.getLtcg112Total());
        }
        
        cyla.setHpLoss(hpLoss);
        cyla.setStcgLoss(stcgLoss);
        cyla.setLtcgLoss(ltcgLoss);
        
        // Set current year positive incomes
        if (formData.getScheduleSalary() != null) {
            cyla.setSalaryIncome(formData.getScheduleSalary().getIncomeFromSalary());
        }
        
        if (formData.getScheduleOS() != null) {
            cyla.setOtherSourcesIncome(formData.getScheduleOS().getIncomeFromOtherSources());
        }
        
        if (formData.getScheduleCG() != null) {
            var cg = formData.getScheduleCG();
            cyla.setStcgIncome(Math.max(0, cg.getStcg111ATotal() + cg.getStcgOtherTotal()));
            cyla.setLtcgIncome(Math.max(0, cg.getLtcg112ATaxable() + cg.getLtcg112Total()));
        }
        
        // Compute CYLA
        cyla = lossSetOffService.computeCYLA(cyla);
        formData.setScheduleCYLA(cyla);
        
        log.info("CYLA completed. Unabsorbed losses: HP=₹{}, STCG=₹{}, LTCG=₹{}",
                cyla.getHpLossUnabsorbed(), cyla.getStcgLossUnabsorbed(), cyla.getLtcgLossUnabsorbed());
    }
    
    /**
     * Apply Schedule BFLA - Brought Forward Loss Adjustment
     */
    private void applyBroughtForwardLossSetOff(Itr2FormData formData) {
        log.info("Applying Schedule BFLA - Brought Forward Loss Adjustment");
        
        // Check if there are brought forward losses
        if (formData.getScheduleBFLA() == null) {
            log.debug("No brought forward losses to adjust");
            return;
        }
        
        var bfla = formData.getScheduleBFLA();
        var cyla = formData.getScheduleCYLA();
        
        // Set income available after CYLA
        if (cyla != null) {
            bfla.setHpIncomeAvailable(cyla.getHpIncomeAfterSetOff());
            bfla.setStcgIncomeAvailable(cyla.getStcgIncomeAfterSetOff());
            bfla.setLtcgIncomeAvailable(cyla.getLtcgIncomeAfterSetOff());
            bfla.setOtherSourcesIncomeAvailable(cyla.getOtherSourcesIncomeAfterSetOff());
        }
        
        // Compute BFLA
        String currentAY = formData.getPartA().getAssessmentYear();
        bfla = lossCarryForwardService.computeBFLA(bfla, currentAY);
        formData.setScheduleBFLA(bfla);
        
        log.info("BFLA completed. Total losses set off: HP=₹{}, STCG=₹{}, LTCG=₹{}",
                bfla.getTotalHpLossSetOff(), bfla.getTotalStcgLossSetOff(), bfla.getTotalLtcgLossSetOff());
    }
    
    /**
     * Generate Schedule CFL - Carry Forward Losses
     */
    private void generateCarryForwardLosses(Itr2FormData formData) {
        log.info("Generating Schedule CFL - Carry Forward Losses");
        
        var cyla = formData.getScheduleCYLA();
        var bfla = formData.getScheduleBFLA();
        
        if (cyla == null) {
            log.debug("No CYLA schedule - skipping CFL generation");
            return;
        }
        
        // Get unabsorbed losses from CYLA
        double hpLossUnabsorbed = cyla.getHpLossUnabsorbed();
        double stcgLossUnabsorbed = cyla.getStcgLossUnabsorbed();
        double ltcgLossUnabsorbed = cyla.getLtcgLossUnabsorbed();
        
        // Check if return filed on time (for now, assume true - will be set by user)
        boolean filedOnTime = true;
        String currentAY = formData.getPartA().getAssessmentYear();
        String filingDate = java.time.LocalDate.now().toString();
        String dueDate = currentAY.split("-")[0] + "-07-31"; // July 31 for non-audit cases
        
        // Generate CFL
        var cfl = lossCarryForwardService.computeCFL(
                bfla != null ? bfla : com.itr.dto.ScheduleBFLA.builder().build(),
                hpLossUnabsorbed,
                0, // No business loss in ITR-2
                0, // No speculative loss in ITR-2
                stcgLossUnabsorbed,
                ltcgLossUnabsorbed,
                currentAY,
                filedOnTime,
                filingDate,
                dueDate
        );
        
        formData.setScheduleCFL(cfl);
        
        log.info("CFL generated. Total losses to carry forward: HP=₹{}, STCG=₹{}, LTCG=₹{}",
                cfl.getTotalHpLossCarryForward(), cfl.getTotalStcgLossCarryForward(), cfl.getTotalLtcgLossCarryForward());
    }
    
    /**
     * Compute AMT (Alternate Minimum Tax) - Section 115JC
     * Applicable when deductions like 10AA, 35AD, 80H-80RRB claimed
     */
    private void computeAMT(Itr2FormData formData, double totalIncome, double totalDeductions) {
        log.info("Computing AMT for ITR-2");
        
        var deductions = formData.getDeductions();
        if (deductions == null) {
            log.debug("No deductions - AMT not applicable");
            return;
        }
        
        String regime = formData.getPartA().getRegime();
        
        // Extract AMT-relevant deductions
        double deduction10AA = 0; // SEZ deduction (not in standard deductions)
        double deduction35AD = 0; // Specified business deduction (not in standard deductions)
        double deductions80HTo80RRB = deductions.getDeduction80JJAA(); // Only 80JJAA from standard list
        
        // Get regular tax from computation
        double regularTax = formData.getComputation() != null ? 
                formData.getComputation().getTotalTaxLiability() : 0;
        
        // Compute AMT
        var amtSchedule = amtService.computeAMT(
                totalIncome,
                deduction10AA,
                deduction35AD,
                deductions80HTo80RRB,
                regularTax,
                regime,
                null // No brought forward AMT credit for now
        );
        
        formData.setScheduleAMT(amtSchedule);
        
        // Update tax payable if AMT is higher
        if (amtSchedule.isAmtApplicable() && formData.getComputation() != null) {
            formData.getComputation().setTotalTaxLiability(amtSchedule.getTaxPayable());
            formData.getValidationWarnings().add(
                    "AMT applicable: Paying AMT of ₹" + amtSchedule.getTaxPayable() + 
                    " instead of regular tax ₹" + regularTax + 
                    ". AMT credit of ₹" + amtSchedule.getAmtCreditCarryForward() + 
                    " can be carried forward for 15 years.");
        }
        
        log.info("AMT computation completed. AMT Applicable: {}, Tax Payable: ₹{}", 
                amtSchedule.isAmtApplicable(), amtSchedule.getTaxPayable());
    }
}
