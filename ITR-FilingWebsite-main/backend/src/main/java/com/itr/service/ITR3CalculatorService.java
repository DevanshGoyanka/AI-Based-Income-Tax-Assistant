package com.itr.service;

import com.itr.dto.Itr1FormData;
import com.itr.dto.Itr2FormData;
import com.itr.dto.Itr3FormData;
import com.itr.service.taxengine.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ITR3CalculatorService {

    private final TaxComputationEngine taxEngine;
    private final SpecialRateIncomeCalculator specialRateCalculator;
    private final LossSetOffService lossSetOffService;
    private final LossCarryForwardService lossCarryForwardService;

    public Itr3FormData calculateTax(Itr3FormData formData) {
        log.info("Starting ITR-3 tax calculation for AY {}", formData.getPartA().getAssessmentYear());

        try {
            // Step 1: Calculate all income heads
            calculateSalaryIncome(formData);
            calculateHousePropertyIncome(formData);
            calculateBusinessIncome(formData);
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
            
            // Step 3: Apply deductions
            double totalDeductions = calculateDeductions(formData);
            double totalIncome = Math.max(0, gti - totalDeductions);
            
            // Step 4: Compute tax
            computeTax(formData, totalIncome);
            
            // Step 5: Calculate interest and fees
            calculateInterestAndFees(formData);
            
            // Step 6: Calculate refund/demand
            calculateRefundOrDemand(formData);
            
            // Step 7: Generate Schedule CFL (Carry Forward Losses)
            generateCarryForwardLosses(formData);
            
            log.info("ITR-3 calculation completed successfully. Total Income: {}, Tax: {}", 
                    totalIncome, formData.getComputation().getTotalTaxLiability());
            return formData;
            
        } catch (Exception e) {
            log.error("Error calculating ITR-3 tax", e);
            formData.getValidationErrors().add("Tax calculation failed: " + e.getMessage());
            return formData;
        }
    }

    private void calculateSalaryIncome(Itr3FormData formData) {
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
        
        log.debug("ITR-3 Salary: Gross={}, Income={}", grossSalary, incomeFromSalary);
    }

    private void calculateHousePropertyIncome(Itr3FormData formData) {
        if (formData.getHouseProperties() == null || formData.getHouseProperties().isEmpty()) return;
        
        for (var hp : formData.getHouseProperties()) {
            String propertyType = hp.getPropertyType();
            
            double gav = 0;
            if ("SELF_OCCUPIED".equals(propertyType)) {
                gav = 0;
            } else {
                double actualRent = hp.getGrossRent();
                double fairRent = hp.getFairRent();
                double mrv = hp.getMunicipalRateableValue();
                gav = Math.max(Math.max(actualRent, fairRent), mrv);
            }
            
            double municipalTax = hp.getMunicipalTaxPaid();
            double nav = gav - municipalTax;
            double stdDed = nav > 0 ? nav * 0.30 : 0;
            
            double currentInterest = hp.getInterestOnLoan();
            double preConstructionInterest = hp.getPreConstructionInterest1_5th();
            double totalInterest = currentInterest + preConstructionInterest;
            
            if ("SELF_OCCUPIED".equals(propertyType) && totalInterest > 200000) {
                totalInterest = 200000;
            }
            
            double incomeFromHP = nav - stdDed - totalInterest;
            
            hp.setAnnualValue(gav);
            hp.setNetAnnualValue(nav);
            hp.setStandardDeduction(stdDed);
            hp.setTotalInterestDeduction(totalInterest);
            hp.setIncomeFromHP(incomeFromHP);
        }
    }

    private void calculateBusinessIncome(Itr3FormData formData) {
        if (formData.getScheduleBP() == null) return;
        
        var bp = formData.getScheduleBP();
        
        // Step 1: Calculate depreciation if schedule exists
        double totalDepreciation = 0;
        double additionalDepreciation = 0;
        if (formData.getScheduleDPM() != null) {
            var dpm = formData.getScheduleDPM();
            for (var block : dpm.getBlocks()) {
                // Opening WDV + Additions (first half full, second half 50%)
                double wdvForDepreciation = block.getOpeningWDV() + 
                                           block.getAdditionsFirstHalf() + 
                                           (block.getAdditionsSecondHalf() * 0.5);
                
                // Less: Sale proceeds (but not below zero)
                wdvForDepreciation = Math.max(0, wdvForDepreciation - block.getSaleProceeds());
                
                // Normal depreciation
                double depreciation = wdvForDepreciation * (block.getDepreciationRate() / 100);
                block.setDepreciationAmount(depreciation);
                
                // Additional depreciation (only on new plant & machinery)
                double addlDep = 0;
                if (block.getAdditionalDepreciationRate() > 0) {
                    double eligibleForAddl = block.getAdditionsFirstHalf() + 
                                            (block.getAdditionsSecondHalf() * 0.5);
                    addlDep = eligibleForAddl * (block.getAdditionalDepreciationRate() / 100);
                }
                block.setAdditionalDepreciationAmount(addlDep);
                
                double totalBlockDep = depreciation + addlDep;
                block.setTotalDepreciation(totalBlockDep);
                
                // Closing WDV
                double closingWDV = wdvForDepreciation - depreciation;
                block.setClosingWDV(closingWDV);
                
                totalDepreciation += depreciation;
                additionalDepreciation += addlDep;
            }
            
            dpm.setTotalDepreciation(totalDepreciation);
            dpm.setAdditionalDepreciation(additionalDepreciation);
        }
        
        // Step 2: Calculate P&L if schedule exists
        double netProfitAsPerBooks = 0;
        if (formData.getSchedulePL() != null) {
            var pl = formData.getSchedulePL();
            
            // Income side
            double totalIncome = pl.getGrossReceipts() + pl.getOtherIncome();
            
            // Expense side
            double cogs = pl.getOpeningStock() + pl.getPurchases() - pl.getClosingStock() + pl.getDirectExpenses();
            double operatingExpenses = pl.getSalariesWages() + pl.getRent() + pl.getRepairsMaintenance() +
                                      pl.getDepreciation() + pl.getInterestOnLoan() + pl.getProfessionalFees() +
                                      pl.getOfficeExpenses() + pl.getTravelConveyance() + 
                                      pl.getAdvertisingMarketing() + pl.getInsurancePremium() + 
                                      pl.getBadDebts() + pl.getOtherExpenses();
            
            double totalExpenses = cogs + operatingExpenses;
            pl.setTotalExpenses(totalExpenses);
            
            netProfitAsPerBooks = totalIncome - totalExpenses;
            pl.setNetProfitLoss(netProfitAsPerBooks);
        } else {
            netProfitAsPerBooks = bp.getNetProfitAsPerBooks();
        }
        
        // Step 3: Compute taxable business income with addbacks and deductions
        double addbacks = bp.getAddPersonalExpenses() + 
                         bp.getAddCapitalExpenses() +
                         bp.getAddDisallowance40Aia() +  // 40A(ia): excessive payments to related parties
                         bp.getAddDisallowance40A2() +   // 40A(2): excessive payments to specified persons
                         bp.getAddDisallowance40A3() +   // 40A(3): cash payments > ₹10,000
                         bp.getAddDisallowance43B() +    // 43B: unpaid statutory dues
                         bp.getAddDisallowance43Bh() +   // 43Bh: unpaid MSE dues
                         bp.getAddDisallowance14A();     // 14A: expenses for exempt income
        
        double deductions = bp.getLessITActDepreciation() +
                           bp.getLessAdditionalDepreciation() +
                           bp.getLessOtherDeductions();
        
        // If depreciation calculated, use that instead of manual entry
        if (totalDepreciation > 0) {
            deductions = totalDepreciation + additionalDepreciation + bp.getLessOtherDeductions();
        }
        
        double profitFromBusiness = netProfitAsPerBooks + addbacks - deductions;
        bp.setProfitFromBusiness(profitFromBusiness);
        
        // Validate GST reconciliation if provided
        if (formData.getScheduleGST() != null) {
            var gst = formData.getScheduleGST();
            double diff = gst.getTurnoverAsPerBooks() - gst.getTurnoverAsPerGSTR1();
            gst.setDifference(diff);
            
            if (Math.abs(diff) > 1000) {
                formData.getValidationWarnings().add(
                    "Turnover difference between books and GSTR-1: ₹" + diff + 
                    ". Reason: " + (gst.getReasonForDifference() != null ? gst.getReasonForDifference() : "Not provided"));
            }
        }
        
        log.debug("Business Income: Net Profit={}, Addbacks={}, Deductions={}, Taxable={}", 
                netProfitAsPerBooks, addbacks, deductions, profitFromBusiness);
    }

    private void calculateOtherSourcesIncome(Itr3FormData formData) {
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
        
        double pensionDedLimit = "NEW".equals(regime) ? 25000 : 15000;
        double pensionDed = Math.min(familyPension / 3, pensionDedLimit);
        
        double totalOS = savingsInterest + depositInterest + otherInterest + 
                        dividendIncome + familyPension + otherIncome + giftsReceived - pensionDed;
        
        os.setFamilyPensionDeduction(pensionDed);
        os.setIncomeFromOtherSources(totalOS);
    }

    private void calculateCapitalGains(Itr3FormData formData) {
        if (formData.getScheduleCG() == null) return;
        
        var cg = formData.getScheduleCG();
        
        // STCG u/s 111A
        double stcg111ATotal = 0;
        if (cg.getStcg111A() != null) {
            for (var txn : cg.getStcg111A()) {
                double gain = txn.getSalePrice() - txn.getTransferExpenses() - txn.getCostOfAcquisition();
                txn.setGain(gain);
                stcg111ATotal += gain;
            }
        }
        cg.setStcg111ATotal(stcg111ATotal);
        
        // STCG - Other
        double stcgOtherTotal = 0;
        if (cg.getStcgOther() != null) {
            for (var txn : cg.getStcgOther()) {
                double gain = txn.getSalePrice() - txn.getTransferExpenses() - txn.getCostOfAcquisition();
                txn.setGain(gain);
                stcgOtherTotal += gain;
            }
        }
        cg.setStcgOtherTotal(stcgOtherTotal);
        
        // LTCG u/s 112A with grandfathering
        double ltcg112ATotal = 0;
        if (cg.getLtcg112A() != null) {
            for (var txn : cg.getLtcg112A()) {
                double cost = txn.getCostOfAcquisition();
                if (txn.getAcquisitionDate() != null && 
                    txn.getAcquisitionDate().isBefore(java.time.LocalDate.of(2018, 1, 31))) {
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
        
        double ltcg112AExemption = Math.min(ltcg112ATotal, 125000);
        double ltcg112ATaxable = Math.max(0, ltcg112ATotal - ltcg112AExemption);
        cg.setLtcg112ATotal(ltcg112ATotal);
        cg.setLtcg112AExemption(ltcg112AExemption);
        cg.setLtcg112ATaxable(ltcg112ATaxable);
        
        // LTCG u/s 112
        double ltcg112Total = 0;
        if (cg.getLtcg112() != null) {
            for (var txn : cg.getLtcg112()) {
                boolean canChooseIndexation = txn.getAcquisitionDate() != null &&
                        txn.getAcquisitionDate().isBefore(java.time.LocalDate.of(2024, 7, 23));
                
                if (canChooseIndexation) {
                    double gainWithIdx = txn.getSalePrice() - txn.getTransferExpenses() - 
                                        txn.getIndexedCostOfAcquisition();
                    double gainWithoutIdx = txn.getSalePrice() - txn.getTransferExpenses() - 
                                           txn.getCostOfAcquisition();
                    
                    double taxWithIdx = gainWithIdx * 0.20;
                    double taxWithoutIdx = gainWithoutIdx * 0.125;
                    
                    if (taxWithIdx < taxWithoutIdx) {
                        txn.setIndexationChosen(true);
                        txn.setGain(gainWithIdx);
                    } else {
                        txn.setIndexationChosen(false);
                        txn.setGain(gainWithoutIdx);
                    }
                } else {
                    txn.setIndexationChosen(false);
                    double gain = txn.getSalePrice() - txn.getTransferExpenses() - txn.getCostOfAcquisition();
                    txn.setGain(gain);
                }
                
                ltcg112Total += txn.getGain();
            }
        }
        cg.setLtcg112Total(ltcg112Total);
        
        double totalExemptions = 0;
        if (cg.getExemptions() != null) {
            for (var exemption : cg.getExemptions()) {
                if ("54EC".equals(exemption.getSection()) && exemption.getExemptionAmount() > 5000000) {
                    exemption.setExemptionAmount(5000000);
                }
                totalExemptions += exemption.getExemptionAmount();
            }
        }
        
        double totalCG = stcg111ATotal + stcgOtherTotal + ltcg112ATaxable + ltcg112Total - totalExemptions;
        cg.setTotalCapitalGains(Math.max(0, totalCG));
    }

    private void calculateVDAIncome(Itr3FormData formData) {
        if (formData.getScheduleVDA() == null) return;
        
        var vda = formData.getScheduleVDA();
        double totalVDAIncome = 0;
        double totalTDS194S = 0;
        
        if (vda.getTransactions() != null) {
            for (var txn : vda.getTransactions()) {
                double profit = txn.getSalePrice() - txn.getCostOfAcquisition();
                txn.setProfit(profit);
                
                if (profit < 0) {
                    formData.getValidationErrors().add(
                        "VDA loss cannot be set off: " + txn.getVdaDescription());
                    profit = 0;
                }
                
                totalVDAIncome += profit;
                totalTDS194S += txn.getTdsDeducted();
            }
        }
        
        vda.setTotalVDAIncome(totalVDAIncome);
        vda.setTotalTDS194S(totalTDS194S);
        vda.setTaxAt30Percent(totalVDAIncome * 0.30);
    }

    private double calculateGrossTotalIncomeBeforeLossSetOff(Itr3FormData formData) {
        double gti = 0;
        
        if (formData.getScheduleSalary() != null) {
            gti += formData.getScheduleSalary().getIncomeFromSalary();
        }
        
        if (formData.getHouseProperties() != null) {
            double hpIncome = formData.getHouseProperties().stream()
                .mapToDouble(hp -> hp.getIncomeFromHP())
                .sum();
            
            if (hpIncome < 0) {
                double hpLoss = Math.abs(hpIncome);
                double setOffAmount = Math.min(hpLoss, 200000);
                gti -= setOffAmount;
            } else {
                gti += hpIncome;
            }
        }
        
        if (formData.getScheduleBP() != null) {
            gti += formData.getScheduleBP().getProfitFromBusiness();
        }
        
        if (formData.getScheduleOS() != null) {
            gti += formData.getScheduleOS().getIncomeFromOtherSources();
        }
        
        if (formData.getScheduleCG() != null) {
            gti += formData.getScheduleCG().getTotalCapitalGains();
        }
        
        if (formData.getScheduleVDA() != null) {
            gti += formData.getScheduleVDA().getTotalVDAIncome();
        }
        
        gti = Math.max(0, gti);
        
        if (formData.getComputation() == null) {
            formData.setComputation(new Itr2FormData.TaxComputation());
        }
        formData.getComputation().setGrossTotalIncome(gti);
        
        return gti;
    }

    private double calculateDeductions(Itr3FormData formData) {
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

    private void computeTax(Itr3FormData formData, double totalIncome) {
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
                .isPresumptiveIncome(false)
                .totalIncome(BigDecimal.valueOf(totalIncome))
                .normalIncome(BigDecimal.valueOf(totalIncome))
                .build();
        
        if (formData.getScheduleCG() != null) {
            var cg = formData.getScheduleCG();
            input.setStcg111A(BigDecimal.valueOf(cg.getStcg111ATotal()));
            input.setLtcg112A(BigDecimal.valueOf(cg.getLtcg112ATaxable()));
            input.setLtcg112(BigDecimal.valueOf(cg.getLtcg112Total()));
        }
        
        if (formData.getScheduleVDA() != null) {
            input.setIncome115BBH(BigDecimal.valueOf(formData.getScheduleVDA().getTotalVDAIncome()));
        }
        
        if (formData.getScheduleTDS() != null) {
            input.setTdsPaid(BigDecimal.valueOf(formData.getScheduleTDS().getTotalTDS()));
        }
        
        var result = taxEngine.computeTax(input);
        
        computation.setTaxOnNormalIncome(result.getTaxOnNormalIncome().doubleValue());
        computation.setTaxOnSpecialRateIncome(result.getSpecialRateIncomeTax().getTotalTax().doubleValue());
        computation.setRebate87A(result.getRebate87A().getRebateAmount().doubleValue());
        computation.setSurcharge(result.getSurcharge().getEffectiveSurcharge().doubleValue());
        computation.setCess(result.getCess().doubleValue());
        computation.setTotalTaxLiability(result.getTotalTaxLiability().doubleValue());
        
        log.info("ITR-3 Tax computed: Total Income={}, Tax={}", totalIncome, computation.getTotalTaxLiability());
    }

    private void calculateInterestAndFees(Itr3FormData formData) {
        var computation = formData.getComputation();
        if (computation == null) return;
        
        computation.setInterest234A(0);
        computation.setInterest234B(0);
        computation.setInterest234C(0);
        computation.setFee234F(0);
    }

    private void calculateRefundOrDemand(Itr3FormData formData) {
        var computation = formData.getComputation();
        if (computation == null) return;
        
        double totalTax = computation.getTotalTaxLiability();
        double totalPaid = 0;
        
        if (formData.getScheduleTDS() != null) {
            totalPaid += formData.getScheduleTDS().getTotalTDS();
        }
        
        double netPayable = totalTax - totalPaid;
        
        if (netPayable > 0) {
            computation.setTaxPayable(netPayable);
            computation.setRefund(0);
        } else {
            computation.setTaxPayable(0);
            computation.setRefund(Math.abs(netPayable));
        }
        
        log.info("ITR-3 Refund/Demand: Tax={}, Paid={}, Net={}", totalTax, totalPaid, netPayable);
    }
    
    private String getAgeCategory(Integer age) {
        if (age == null) return "REGULAR";
        if (age >= 80) return "SUPER_SENIOR";
        if (age >= 60) return "SENIOR";
        return "REGULAR";
    }
    
    /**
     * Apply Schedule CYLA - Current Year Loss Adjustment for ITR-3
     * Includes business loss and speculative business loss
     */
    private void applyCurrentYearLossSetOff(Itr3FormData formData, double gtiBeforeLoss) {
        log.info("Applying Schedule CYLA for ITR-3 - Current Year Loss Adjustment");
        
        com.itr.dto.ScheduleCYLA cyla = com.itr.dto.ScheduleCYLA.builder().build();
        
        // Set current year losses
        double hpLoss = 0;
        double businessLoss = 0;
        double speculativeLoss = 0;
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
        
        // Calculate Business losses (non-speculative and speculative)
        if (formData.getScheduleBP() != null) {
            var bp = formData.getScheduleBP();
            double profitFromBusiness = bp.getProfitFromBusiness();
            
            if (profitFromBusiness < 0) {
                if (bp.isSpeculative()) {
                    speculativeLoss = Math.abs(profitFromBusiness);
                } else {
                    businessLoss = Math.abs(profitFromBusiness);
                }
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
        cyla.setNonSpeculativeBusinessLoss(businessLoss);
        cyla.setSpeculativeBusinessLoss(speculativeLoss);
        cyla.setStcgLoss(stcgLoss);
        cyla.setLtcgLoss(ltcgLoss);
        
        // Set current year positive incomes
        if (formData.getScheduleSalary() != null) {
            cyla.setSalaryIncome(formData.getScheduleSalary().getIncomeFromSalary());
        }
        
        if (formData.getScheduleBP() != null) {
            double businessIncome = Math.max(0, formData.getScheduleBP().getProfitFromBusiness());
            if (formData.getScheduleBP().isSpeculative()) {
                cyla.setSpeculativeBusinessIncome(businessIncome);
            } else {
                cyla.setBusinessIncome(businessIncome);
            }
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
        
        log.info("ITR-3 CYLA completed. Unabsorbed losses: HP=₹{}, Business=₹{}, Speculative=₹{}, STCG=₹{}, LTCG=₹{}",
                cyla.getHpLossUnabsorbed(), cyla.getBusinessLossUnabsorbed(), 
                cyla.getSpeculativeLossUnabsorbed(), cyla.getStcgLossUnabsorbed(), cyla.getLtcgLossUnabsorbed());
    }
    
    /**
     * Apply Schedule BFLA - Brought Forward Loss Adjustment for ITR-3
     */
    private void applyBroughtForwardLossSetOff(Itr3FormData formData) {
        log.info("Applying Schedule BFLA for ITR-3 - Brought Forward Loss Adjustment");
        
        if (formData.getScheduleBFLA() == null) {
            log.debug("No brought forward losses to adjust");
            return;
        }
        
        var bfla = formData.getScheduleBFLA();
        var cyla = formData.getScheduleCYLA();
        
        // Set income available after CYLA
        if (cyla != null) {
            bfla.setHpIncomeAvailable(cyla.getHpIncomeAfterSetOff());
            bfla.setBusinessIncomeAvailable(cyla.getBusinessIncomeAfterSetOff());
            bfla.setSpeculativeBusinessIncomeAvailable(cyla.getSpeculativeBusinessIncomeAfterSetOff());
            bfla.setStcgIncomeAvailable(cyla.getStcgIncomeAfterSetOff());
            bfla.setLtcgIncomeAvailable(cyla.getLtcgIncomeAfterSetOff());
            bfla.setOtherSourcesIncomeAvailable(cyla.getOtherSourcesIncomeAfterSetOff());
        }
        
        // Compute BFLA
        String currentAY = formData.getPartA().getAssessmentYear();
        bfla = lossCarryForwardService.computeBFLA(bfla, currentAY);
        formData.setScheduleBFLA(bfla);
        
        log.info("ITR-3 BFLA completed. Total losses set off: HP=₹{}, Business=₹{}, Speculative=₹{}, STCG=₹{}, LTCG=₹{}",
                bfla.getTotalHpLossSetOff(), bfla.getTotalBusinessLossSetOff(), 
                bfla.getTotalSpeculativeLossSetOff(), bfla.getTotalStcgLossSetOff(), bfla.getTotalLtcgLossSetOff());
    }
    
    /**
     * Generate Schedule CFL - Carry Forward Losses for ITR-3
     */
    private void generateCarryForwardLosses(Itr3FormData formData) {
        log.info("Generating Schedule CFL for ITR-3 - Carry Forward Losses");
        
        var cyla = formData.getScheduleCYLA();
        var bfla = formData.getScheduleBFLA();
        
        if (cyla == null) {
            log.debug("No CYLA schedule - skipping CFL generation");
            return;
        }
        
        // Get unabsorbed losses from CYLA
        double hpLossUnabsorbed = cyla.getHpLossUnabsorbed();
        double businessLossUnabsorbed = cyla.getBusinessLossUnabsorbed();
        double speculativeLossUnabsorbed = cyla.getSpeculativeLossUnabsorbed();
        double stcgLossUnabsorbed = cyla.getStcgLossUnabsorbed();
        double ltcgLossUnabsorbed = cyla.getLtcgLossUnabsorbed();
        
        // Check if return filed on time
        boolean filedOnTime = true;
        String currentAY = formData.getPartA().getAssessmentYear();
        String filingDate = java.time.LocalDate.now().toString();
        String dueDate = currentAY.split("-")[0] + "-10-31"; // Oct 31 for audit cases
        
        // Generate CFL
        var cfl = lossCarryForwardService.computeCFL(
                bfla != null ? bfla : com.itr.dto.ScheduleBFLA.builder().build(),
                hpLossUnabsorbed,
                businessLossUnabsorbed,
                speculativeLossUnabsorbed,
                stcgLossUnabsorbed,
                ltcgLossUnabsorbed,
                currentAY,
                filedOnTime,
                filingDate,
                dueDate
        );
        
        formData.setScheduleCFL(cfl);
        
        log.info("ITR-3 CFL generated. Total losses to carry forward: HP=₹{}, Business=₹{}, Speculative=₹{}, STCG=₹{}, LTCG=₹{}",
                cfl.getTotalHpLossCarryForward(), cfl.getTotalBusinessLossCarryForward(),
                cfl.getTotalSpeculativeLossCarryForward(), cfl.getTotalStcgLossCarryForward(), cfl.getTotalLtcgLossCarryForward());
    }
}
