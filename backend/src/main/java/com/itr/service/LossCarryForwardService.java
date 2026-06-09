package com.itr.service;

import com.itr.dto.ScheduleBFLA;
import com.itr.dto.ScheduleCFL;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Loss Carry Forward Service - Schedule BFLA and CFL Implementation
 * 101% CBDT Compliant - Section 72, 73, 74, 32(2)
 * 
 * Carry Forward Periods:
 * - HP Loss: 8 years (set off against HP income only)
 * - Non-Speculative Business Loss: 8 years (against business income only)
 * - Speculative Business Loss: 4 years (against speculative income only)
 * - STCG Loss: 8 years (against STCG and LTCG)
 * - LTCG Loss: 8 years (against LTCG only)
 * - Unabsorbed Depreciation: UNLIMITED (against business first, then any head except salary)
 */
@Slf4j
@Service
public class LossCarryForwardService {

    private static final int HP_LOSS_CARRY_FORWARD_YEARS = 8;
    private static final int BUSINESS_LOSS_CARRY_FORWARD_YEARS = 8;
    private static final int SPECULATIVE_LOSS_CARRY_FORWARD_YEARS = 4;
    private static final int STCG_LOSS_CARRY_FORWARD_YEARS = 8;
    private static final int LTCG_LOSS_CARRY_FORWARD_YEARS = 8;
    private static final int DEPRECIATION_CARRY_FORWARD_YEARS = -1; // UNLIMITED

    /**
     * Compute Schedule BFLA - Brought Forward Loss Adjustment
     * FIFO (First In First Out) - oldest losses are set off first
     */
    public ScheduleBFLA computeBFLA(ScheduleBFLA bfla, String currentAY) {
        log.info("Computing Schedule BFLA for AY {}", currentAY);
        
        // Sort all losses by assessment year (oldest first - FIFO)
        sortLossesByYear(bfla);
        
        // Check for expired losses
        checkExpiredLosses(bfla, currentAY);
        
        // Set off HP losses (against HP income only)
        setOffHPLosses(bfla);
        
        // Set off Business losses (against business income only)
        setOffBusinessLosses(bfla);
        
        // Set off Speculative losses (against speculative income only)
        setOffSpeculativeLosses(bfla);
        
        // Set off STCG losses (against STCG and LTCG)
        setOffSTCGLosses(bfla);
        
        // Set off LTCG losses (against LTCG only)
        setOffLTCGLosses(bfla);
        
        // Set off Unabsorbed Depreciation (against business first, then other heads except salary)
        setOffUnabsorbedDepreciation(bfla);
        
        // Calculate final income after BFLA
        calculateFinalIncome(bfla);
        
        log.info("BFLA completed. Total losses set off: HP=₹{}, Business=₹{}, Speculative=₹{}, STCG=₹{}, LTCG=₹{}, Depreciation=₹{}",
                bfla.getTotalHpLossSetOff(), bfla.getTotalBusinessLossSetOff(), bfla.getTotalSpeculativeLossSetOff(),
                bfla.getTotalStcgLossSetOff(), bfla.getTotalLtcgLossSetOff(), bfla.getTotalUnabsorbedDepreciationSetOff());
        
        return bfla;
    }

    /**
     * Compute Schedule CFL - Carry Forward Losses
     */
    public ScheduleCFL computeCFL(ScheduleBFLA bfla, double hpLossUnabsorbedCYLA, double businessLossUnabsorbedCYLA,
                                   double speculativeLossUnabsorbedCYLA, double stcgLossUnabsorbedCYLA,
                                   double ltcgLossUnabsorbedCYLA, String currentAY, boolean filedOnTime,
                                   String filingDate, String dueDate) {
        
        log.info("Computing Schedule CFL for AY {}", currentAY);
        
        ScheduleCFL cfl = ScheduleCFL.builder()
                .currentAssessmentYear(currentAY)
                .filedOnTime(filedOnTime)
                .filingDate(filingDate)
                .dueDate(dueDate)
                .lossesToCarryForward(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();
        
        if (!filedOnTime) {
            cfl.getWarnings().add("Return filed after due date. Current year losses CANNOT be carried forward " +
                    "(except unabsorbed depreciation and brought forward losses from prior years).");
        }
        
        // Add current year unabsorbed losses (only if filed on time)
        if (filedOnTime) {
            addCurrentYearLosses(cfl, currentAY, hpLossUnabsorbedCYLA, businessLossUnabsorbedCYLA,
                    speculativeLossUnabsorbedCYLA, stcgLossUnabsorbedCYLA, ltcgLossUnabsorbedCYLA);
        }
        
        // Add brought forward losses that are still unabsorbed
        addBroughtForwardLosses(cfl, bfla);
        
        // Calculate totals
        calculateCFLTotals(cfl);
        
        log.info("CFL completed. Total losses to carry forward: HP=₹{}, Business=₹{}, Speculative=₹{}, STCG=₹{}, LTCG=₹{}, Depreciation=₹{}",
                cfl.getTotalHpLossCarryForward(), cfl.getTotalBusinessLossCarryForward(),
                cfl.getTotalSpeculativeLossCarryForward(), cfl.getTotalStcgLossCarryForward(),
                cfl.getTotalLtcgLossCarryForward(), cfl.getTotalUnabsorbedDepreciationCarryForward());
        
        return cfl;
    }

    private void sortLossesByYear(ScheduleBFLA bfla) {
        // Sort HP losses by year (oldest first)
        bfla.getHpLosses().sort(Comparator.comparing(ScheduleBFLA.BroughtForwardLoss::getAssessmentYear));
        
        // Sort Business losses by year
        bfla.getBusinessLosses().sort(Comparator.comparing(ScheduleBFLA.BroughtForwardLoss::getAssessmentYear));
        
        // Sort Speculative losses by year
        bfla.getSpeculativeLosses().sort(Comparator.comparing(ScheduleBFLA.BroughtForwardLoss::getAssessmentYear));
        
        // Sort STCG losses by year
        bfla.getStcgLosses().sort(Comparator.comparing(ScheduleBFLA.BroughtForwardLoss::getAssessmentYear));
        
        // Sort LTCG losses by year
        bfla.getLtcgLosses().sort(Comparator.comparing(ScheduleBFLA.BroughtForwardLoss::getAssessmentYear));
        
        // Sort Unabsorbed Depreciation by year
        bfla.getUnabsorbedDepreciation().sort(Comparator.comparing(ScheduleBFLA.BroughtForwardLoss::getAssessmentYear));
    }

    private void checkExpiredLosses(ScheduleBFLA bfla, String currentAY) {
        int currentYear = Integer.parseInt(currentAY.split("-")[0]);
        
        // Check HP losses
        checkAndExpireLosses(bfla.getHpLosses(), bfla.getExpiredLosses(), currentYear, HP_LOSS_CARRY_FORWARD_YEARS, "HP");
        
        // Check Business losses
        checkAndExpireLosses(bfla.getBusinessLosses(), bfla.getExpiredLosses(), currentYear, BUSINESS_LOSS_CARRY_FORWARD_YEARS, "BUSINESS");
        
        // Check Speculative losses
        checkAndExpireLosses(bfla.getSpeculativeLosses(), bfla.getExpiredLosses(), currentYear, SPECULATIVE_LOSS_CARRY_FORWARD_YEARS, "SPECULATIVE");
        
        // Check STCG losses
        checkAndExpireLosses(bfla.getStcgLosses(), bfla.getExpiredLosses(), currentYear, STCG_LOSS_CARRY_FORWARD_YEARS, "STCG");
        
        // Check LTCG losses
        checkAndExpireLosses(bfla.getLtcgLosses(), bfla.getExpiredLosses(), currentYear, LTCG_LOSS_CARRY_FORWARD_YEARS, "LTCG");
        
        // Unabsorbed Depreciation never expires
    }

    private void checkAndExpireLosses(List<ScheduleBFLA.BroughtForwardLoss> losses,
                                      List<ScheduleBFLA.ExpiredLoss> expiredLosses,
                                      int currentYear, int carryForwardYears, String lossType) {
        
        List<ScheduleBFLA.BroughtForwardLoss> toRemove = new ArrayList<>();
        
        for (ScheduleBFLA.BroughtForwardLoss loss : losses) {
            int lossYear = Integer.parseInt(loss.getAssessmentYear().split("-")[0]);
            int yearsPassed = currentYear - lossYear;
            
            if (yearsPassed > carryForwardYears) {
                // Loss has expired
                loss.setExpired(true);
                loss.setYearsRemaining(0);
                
                expiredLosses.add(ScheduleBFLA.ExpiredLoss.builder()
                        .assessmentYear(loss.getAssessmentYear())
                        .lossType(lossType)
                        .lossAmount(loss.getLossRemaining())
                        .reason("Time limit expired (" + carryForwardYears + " years)")
                        .build());
                
                toRemove.add(loss);
            } else {
                loss.setYearsRemaining(carryForwardYears - yearsPassed);
            }
        }
        
        losses.removeAll(toRemove);
    }

    private void setOffHPLosses(ScheduleBFLA bfla) {
        double hpIncomeAvailable = bfla.getHpIncomeAvailable();
        double totalSetOff = 0;
        
        for (ScheduleBFLA.BroughtForwardLoss loss : bfla.getHpLosses()) {
            if (hpIncomeAvailable <= 0) break;
            
            double setOff = Math.min(loss.getLossRemaining(), hpIncomeAvailable);
            loss.setLossSetOffThisYear(setOff);
            loss.setLossCarryForward(loss.getLossRemaining() - setOff);
            
            hpIncomeAvailable -= setOff;
            totalSetOff += setOff;
        }
        
        bfla.setTotalHpLossSetOff(totalSetOff);
        bfla.setHpIncomeAfterBFLA(bfla.getHpIncomeAvailable() - totalSetOff);
    }

    private void setOffBusinessLosses(ScheduleBFLA bfla) {
        double businessIncomeAvailable = bfla.getBusinessIncomeAvailable();
        double totalSetOff = 0;
        
        for (ScheduleBFLA.BroughtForwardLoss loss : bfla.getBusinessLosses()) {
            if (businessIncomeAvailable <= 0) break;
            
            double setOff = Math.min(loss.getLossRemaining(), businessIncomeAvailable);
            loss.setLossSetOffThisYear(setOff);
            loss.setLossCarryForward(loss.getLossRemaining() - setOff);
            
            businessIncomeAvailable -= setOff;
            totalSetOff += setOff;
        }
        
        bfla.setTotalBusinessLossSetOff(totalSetOff);
        bfla.setBusinessIncomeAfterBFLA(bfla.getBusinessIncomeAvailable() - totalSetOff);
    }

    private void setOffSpeculativeLosses(ScheduleBFLA bfla) {
        double speculativeIncomeAvailable = bfla.getSpeculativeBusinessIncomeAvailable();
        double totalSetOff = 0;
        
        for (ScheduleBFLA.BroughtForwardLoss loss : bfla.getSpeculativeLosses()) {
            if (speculativeIncomeAvailable <= 0) break;
            
            double setOff = Math.min(loss.getLossRemaining(), speculativeIncomeAvailable);
            loss.setLossSetOffThisYear(setOff);
            loss.setLossCarryForward(loss.getLossRemaining() - setOff);
            
            speculativeIncomeAvailable -= setOff;
            totalSetOff += setOff;
        }
        
        bfla.setTotalSpeculativeLossSetOff(totalSetOff);
        bfla.setSpeculativeBusinessIncomeAfterBFLA(bfla.getSpeculativeBusinessIncomeAvailable() - totalSetOff);
    }

    private void setOffSTCGLosses(ScheduleBFLA bfla) {
        double stcgIncomeAvailable = bfla.getStcgIncomeAvailable();
        double ltcgIncomeAvailable = bfla.getLtcgIncomeAvailable();
        double totalSetOff = 0;
        
        for (ScheduleBFLA.BroughtForwardLoss loss : bfla.getStcgLosses()) {
            double remainingLoss = loss.getLossRemaining();
            double setOff = 0;
            
            // Set off against STCG first
            if (stcgIncomeAvailable > 0 && remainingLoss > 0) {
                double stcgSetOff = Math.min(remainingLoss, stcgIncomeAvailable);
                setOff += stcgSetOff;
                remainingLoss -= stcgSetOff;
                stcgIncomeAvailable -= stcgSetOff;
            }
            
            // Then set off against LTCG
            if (ltcgIncomeAvailable > 0 && remainingLoss > 0) {
                double ltcgSetOff = Math.min(remainingLoss, ltcgIncomeAvailable);
                setOff += ltcgSetOff;
                remainingLoss -= ltcgSetOff;
                ltcgIncomeAvailable -= ltcgSetOff;
            }
            
            loss.setLossSetOffThisYear(setOff);
            loss.setLossCarryForward(loss.getLossRemaining() - setOff);
            totalSetOff += setOff;
        }
        
        bfla.setTotalStcgLossSetOff(totalSetOff);
        bfla.setStcgIncomeAfterBFLA(bfla.getStcgIncomeAvailable() - (totalSetOff - (bfla.getLtcgIncomeAvailable() - ltcgIncomeAvailable)));
        bfla.setLtcgIncomeAfterBFLA(ltcgIncomeAvailable);
    }

    private void setOffLTCGLosses(ScheduleBFLA bfla) {
        double ltcgIncomeAvailable = bfla.getLtcgIncomeAfterBFLA();
        double totalSetOff = 0;
        
        for (ScheduleBFLA.BroughtForwardLoss loss : bfla.getLtcgLosses()) {
            if (ltcgIncomeAvailable <= 0) break;
            
            double setOff = Math.min(loss.getLossRemaining(), ltcgIncomeAvailable);
            loss.setLossSetOffThisYear(setOff);
            loss.setLossCarryForward(loss.getLossRemaining() - setOff);
            
            ltcgIncomeAvailable -= setOff;
            totalSetOff += setOff;
        }
        
        bfla.setTotalLtcgLossSetOff(totalSetOff);
        bfla.setLtcgIncomeAfterBFLA(ltcgIncomeAvailable);
    }

    private void setOffUnabsorbedDepreciation(ScheduleBFLA bfla) {
        double businessIncomeAvailable = bfla.getBusinessIncomeAfterBFLA();
        double otherIncomeAvailable = bfla.getOtherSourcesIncomeAvailable();
        double totalSetOff = 0;
        
        for (ScheduleBFLA.BroughtForwardLoss loss : bfla.getUnabsorbedDepreciation()) {
            double remainingLoss = loss.getLossRemaining();
            double setOff = 0;
            
            // Set off against business income first
            if (businessIncomeAvailable > 0 && remainingLoss > 0) {
                double businessSetOff = Math.min(remainingLoss, businessIncomeAvailable);
                setOff += businessSetOff;
                remainingLoss -= businessSetOff;
                businessIncomeAvailable -= businessSetOff;
            }
            
            // Then set off against other heads (except salary)
            if (otherIncomeAvailable > 0 && remainingLoss > 0) {
                double otherSetOff = Math.min(remainingLoss, otherIncomeAvailable);
                setOff += otherSetOff;
                remainingLoss -= otherSetOff;
                otherIncomeAvailable -= otherSetOff;
            }
            
            loss.setLossSetOffThisYear(setOff);
            loss.setLossCarryForward(loss.getLossRemaining() - setOff);
            totalSetOff += setOff;
        }
        
        bfla.setTotalUnabsorbedDepreciationSetOff(totalSetOff);
        bfla.setBusinessIncomeAfterBFLA(businessIncomeAvailable);
        bfla.setOtherSourcesIncomeAfterBFLA(otherIncomeAvailable);
    }

    private void calculateFinalIncome(ScheduleBFLA bfla) {
        // Income values are already calculated in individual set-off methods
        log.debug("Final income after BFLA: HP=₹{}, Business=₹{}, Speculative=₹{}, STCG=₹{}, LTCG=₹{}, OS=₹{}",
                bfla.getHpIncomeAfterBFLA(), bfla.getBusinessIncomeAfterBFLA(),
                bfla.getSpeculativeBusinessIncomeAfterBFLA(), bfla.getStcgIncomeAfterBFLA(),
                bfla.getLtcgIncomeAfterBFLA(), bfla.getOtherSourcesIncomeAfterBFLA());
    }

    private void addCurrentYearLosses(ScheduleCFL cfl, String currentAY, double hpLoss, double businessLoss,
                                      double speculativeLoss, double stcgLoss, double ltcgLoss) {
        
        if (hpLoss > 0) {
            cfl.getLossesToCarryForward().add(createLossEntry("HP", currentAY, hpLoss, HP_LOSS_CARRY_FORWARD_YEARS,
                    "HP income only", true, null));
        }
        
        if (businessLoss > 0) {
            cfl.getLossesToCarryForward().add(createLossEntry("BUSINESS", currentAY, businessLoss, BUSINESS_LOSS_CARRY_FORWARD_YEARS,
                    "Business income only", true, null));
        }
        
        if (speculativeLoss > 0) {
            cfl.getLossesToCarryForward().add(createLossEntry("SPECULATIVE", currentAY, speculativeLoss, SPECULATIVE_LOSS_CARRY_FORWARD_YEARS,
                    "Speculative business income only", true, null));
        }
        
        if (stcgLoss > 0) {
            cfl.getLossesToCarryForward().add(createLossEntry("STCG", currentAY, stcgLoss, STCG_LOSS_CARRY_FORWARD_YEARS,
                    "STCG and LTCG", true, null));
        }
        
        if (ltcgLoss > 0) {
            cfl.getLossesToCarryForward().add(createLossEntry("LTCG", currentAY, ltcgLoss, LTCG_LOSS_CARRY_FORWARD_YEARS,
                    "LTCG only", true, null));
        }
    }

    private void addBroughtForwardLosses(ScheduleCFL cfl, ScheduleBFLA bfla) {
        // Add unabsorbed HP losses
        for (ScheduleBFLA.BroughtForwardLoss loss : bfla.getHpLosses()) {
            if (loss.getLossCarryForward() > 0) {
                cfl.getLossesToCarryForward().add(createLossEntry("HP", loss.getAssessmentYear(),
                        loss.getLossCarryForward(), HP_LOSS_CARRY_FORWARD_YEARS,
                        "HP income only", true, null));
            }
        }
        
        // Add unabsorbed Business losses
        for (ScheduleBFLA.BroughtForwardLoss loss : bfla.getBusinessLosses()) {
            if (loss.getLossCarryForward() > 0) {
                cfl.getLossesToCarryForward().add(createLossEntry("BUSINESS", loss.getAssessmentYear(),
                        loss.getLossCarryForward(), BUSINESS_LOSS_CARRY_FORWARD_YEARS,
                        "Business income only", true, null));
            }
        }
        
        // Add unabsorbed Speculative losses
        for (ScheduleBFLA.BroughtForwardLoss loss : bfla.getSpeculativeLosses()) {
            if (loss.getLossCarryForward() > 0) {
                cfl.getLossesToCarryForward().add(createLossEntry("SPECULATIVE", loss.getAssessmentYear(),
                        loss.getLossCarryForward(), SPECULATIVE_LOSS_CARRY_FORWARD_YEARS,
                        "Speculative business income only", true, null));
            }
        }
        
        // Add unabsorbed STCG losses
        for (ScheduleBFLA.BroughtForwardLoss loss : bfla.getStcgLosses()) {
            if (loss.getLossCarryForward() > 0) {
                cfl.getLossesToCarryForward().add(createLossEntry("STCG", loss.getAssessmentYear(),
                        loss.getLossCarryForward(), STCG_LOSS_CARRY_FORWARD_YEARS,
                        "STCG and LTCG", true, null));
            }
        }
        
        // Add unabsorbed LTCG losses
        for (ScheduleBFLA.BroughtForwardLoss loss : bfla.getLtcgLosses()) {
            if (loss.getLossCarryForward() > 0) {
                cfl.getLossesToCarryForward().add(createLossEntry("LTCG", loss.getAssessmentYear(),
                        loss.getLossCarryForward(), LTCG_LOSS_CARRY_FORWARD_YEARS,
                        "LTCG only", true, null));
            }
        }
        
        // Add unabsorbed Depreciation (unlimited carry forward)
        for (ScheduleBFLA.BroughtForwardLoss loss : bfla.getUnabsorbedDepreciation()) {
            if (loss.getLossCarryForward() > 0) {
                cfl.getLossesToCarryForward().add(createLossEntry("DEPRECIATION", loss.getAssessmentYear(),
                        loss.getLossCarryForward(), DEPRECIATION_CARRY_FORWARD_YEARS,
                        "Business income first, then any head except salary", true, null));
            }
        }
    }

    private ScheduleCFL.LossCarryForward createLossEntry(String lossType, String ayOfLoss, double amount,
                                                         int carryForwardPeriod, String setOffAgainst,
                                                         boolean eligible, String ineligibilityReason) {
        
        int lossYear = Integer.parseInt(ayOfLoss.split("-")[0]);
        int yearsRemaining = carryForwardPeriod == -1 ? -1 : carryForwardPeriod;
        String expiryAY = carryForwardPeriod == -1 ? "UNLIMITED" :
                String.format("%d-%d", lossYear + carryForwardPeriod, (lossYear + carryForwardPeriod + 1) % 100);
        
        return ScheduleCFL.LossCarryForward.builder()
                .lossType(lossType)
                .assessmentYearOfLoss(ayOfLoss)
                .lossAmount(amount)
                .carryForwardPeriod(carryForwardPeriod)
                .yearsRemaining(yearsRemaining)
                .expiryAssessmentYear(expiryAY)
                .setOffAgainst(setOffAgainst)
                .eligible(eligible)
                .ineligibilityReason(ineligibilityReason)
                .build();
    }

    private void calculateCFLTotals(ScheduleCFL cfl) {
        double hpTotal = 0, businessTotal = 0, speculativeTotal = 0, stcgTotal = 0, ltcgTotal = 0, depreciationTotal = 0;
        
        for (ScheduleCFL.LossCarryForward loss : cfl.getLossesToCarryForward()) {
            switch (loss.getLossType()) {
                case "HP":
                    hpTotal += loss.getLossAmount();
                    break;
                case "BUSINESS":
                    businessTotal += loss.getLossAmount();
                    break;
                case "SPECULATIVE":
                    speculativeTotal += loss.getLossAmount();
                    break;
                case "STCG":
                    stcgTotal += loss.getLossAmount();
                    break;
                case "LTCG":
                    ltcgTotal += loss.getLossAmount();
                    break;
                case "DEPRECIATION":
                    depreciationTotal += loss.getLossAmount();
                    break;
            }
        }
        
        cfl.setTotalHpLossCarryForward(hpTotal);
        cfl.setTotalBusinessLossCarryForward(businessTotal);
        cfl.setTotalSpeculativeLossCarryForward(speculativeTotal);
        cfl.setTotalStcgLossCarryForward(stcgTotal);
        cfl.setTotalLtcgLossCarryForward(ltcgTotal);
        cfl.setTotalUnabsorbedDepreciationCarryForward(depreciationTotal);
    }
}
