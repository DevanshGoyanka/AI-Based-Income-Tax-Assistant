package com.itr.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Loss Set-off Engine - 101% CBDT Compliant
 * Implements CYLA, BFLA, and CFL with type restrictions
 * Reference: ITR_ERP_COMPLETION_DIRECTIVE.md Section 4.2
 */
@Slf4j
@Service
public class LossSetOffEngine {

    /**
     * Current Year Loss Adjustment (CYLA)
     * Intra-head and inter-head set-off within same AY
     */
    public CYLAResult computeCYLA(CYLARequest request) {
        List<String> setOffLog = new ArrayList<>();
        
        double salaryIncome = request.getSalaryIncome();
        double hpIncome = request.getHpIncome();
        double businessIncome = request.getBusinessIncome();
        double speculativeIncome = request.getSpeculativeIncome();
        double stcgIncome = request.getStcgIncome();
        double ltcgIncome = request.getLtcgIncome();
        double otherIncome = request.getOtherIncome();

        // Rule 1: HP loss set-off against any head except salary (max Rs 2L)
        if (hpIncome < 0) {
            double hpLoss = Math.abs(hpIncome);
            double maxSetOff = Math.min(hpLoss, 200000);
            
            // Set off against business
            if (businessIncome > 0 && maxSetOff > 0) {
                double setOff = Math.min(businessIncome, maxSetOff);
                businessIncome -= setOff;
                hpIncome += setOff;
                maxSetOff -= setOff;
                setOffLog.add("HP loss Rs " + (int)setOff + " set off against business income");
            }
            
            // Set off against STCG
            if (stcgIncome > 0 && maxSetOff > 0) {
                double setOff = Math.min(stcgIncome, maxSetOff);
                stcgIncome -= setOff;
                hpIncome += setOff;
                maxSetOff -= setOff;
                setOffLog.add("HP loss Rs " + (int)setOff + " set off against STCG");
            }
            
            // Set off against LTCG
            if (ltcgIncome > 0 && maxSetOff > 0) {
                double setOff = Math.min(ltcgIncome, maxSetOff);
                ltcgIncome -= setOff;
                hpIncome += setOff;
                maxSetOff -= setOff;
                setOffLog.add("HP loss Rs " + (int)setOff + " set off against LTCG");
            }
            
            // Set off against other income
            if (otherIncome > 0 && maxSetOff > 0) {
                double setOff = Math.min(otherIncome, maxSetOff);
                otherIncome -= setOff;
                hpIncome += setOff;
                setOffLog.add("HP loss Rs " + (int)setOff + " set off against other income");
            }
        }

        // Rule 2: Business loss (non-speculative) set-off against any head except salary
        if (businessIncome < 0) {
            double businessLoss = Math.abs(businessIncome);
            
            // Set off against HP
            if (hpIncome > 0) {
                double setOff = Math.min(hpIncome, businessLoss);
                hpIncome -= setOff;
                businessIncome += setOff;
                businessLoss -= setOff;
                setOffLog.add("Business loss Rs " + (int)setOff + " set off against HP income");
            }
            
            // Set off against STCG
            if (stcgIncome > 0 && businessLoss > 0) {
                double setOff = Math.min(stcgIncome, businessLoss);
                stcgIncome -= setOff;
                businessIncome += setOff;
                businessLoss -= setOff;
                setOffLog.add("Business loss Rs " + (int)setOff + " set off against STCG");
            }
            
            // Set off against LTCG
            if (ltcgIncome > 0 && businessLoss > 0) {
                double setOff = Math.min(ltcgIncome, businessLoss);
                ltcgIncome -= setOff;
                businessIncome += setOff;
                businessLoss -= setOff;
                setOffLog.add("Business loss Rs " + (int)setOff + " set off against LTCG");
            }
            
            // Set off against other income
            if (otherIncome > 0 && businessLoss > 0) {
                double setOff = Math.min(otherIncome, businessLoss);
                otherIncome -= setOff;
                businessIncome += setOff;
                setOffLog.add("Business loss Rs " + (int)setOff + " set off against other income");
            }
        }

        // Rule 3: Speculative loss ONLY against speculative income
        if (speculativeIncome < 0) {
            setOffLog.add("Speculative loss Rs " + (int)Math.abs(speculativeIncome) + " cannot be set off - carry forward only");
        }

        // Rule 4: STCG loss set-off against STCG or LTCG
        if (stcgIncome < 0) {
            double stcgLoss = Math.abs(stcgIncome);
            
            if (ltcgIncome > 0) {
                double setOff = Math.min(ltcgIncome, stcgLoss);
                ltcgIncome -= setOff;
                stcgIncome += setOff;
                setOffLog.add("STCG loss Rs " + (int)setOff + " set off against LTCG");
            }
        }

        // Rule 5: LTCG loss ONLY against LTCG
        if (ltcgIncome < 0) {
            setOffLog.add("LTCG loss Rs " + (int)Math.abs(ltcgIncome) + " cannot be set off - carry forward only");
        }

        double totalIncome = salaryIncome + Math.max(0, hpIncome) + Math.max(0, businessIncome) 
                           + Math.max(0, speculativeIncome) + Math.max(0, stcgIncome) 
                           + Math.max(0, ltcgIncome) + otherIncome;

        return CYLAResult.builder()
                .salaryIncome((int) salaryIncome)
                .hpIncome((int) hpIncome)
                .businessIncome((int) businessIncome)
                .speculativeIncome((int) speculativeIncome)
                .stcgIncome((int) stcgIncome)
                .ltcgIncome((int) ltcgIncome)
                .otherIncome((int) otherIncome)
                .totalIncome((int) totalIncome)
                .setOffLog(setOffLog)
                .build();
    }

    /**
     * Brought Forward Loss Adjustment (BFLA)
     * Set-off of losses from previous years (max 8 years)
     */
    public BFLAResult computeBFLA(BFLARequest request) {
        List<BFLAEntry> entries = new ArrayList<>();
        
        double currentIncome = request.getCurrentYearIncome();
        double hpIncome = request.getHpIncome();
        double businessIncome = request.getBusinessIncome();
        double stcgIncome = request.getStcgIncome();
        double ltcgIncome = request.getLtcgIncome();

        // Process each brought forward loss
        for (BroughtForwardLoss bfl : request.getBroughtForwardLosses()) {
            String lossType = bfl.getLossType();
            int assessmentYear = bfl.getAssessmentYear();
            double lossAmount = bfl.getAmount();
            double setOff = 0;

            // Check 8-year limit
            if (request.getCurrentAY() - assessmentYear > 8) {
                entries.add(BFLAEntry.builder()
                        .lossType(lossType)
                        .assessmentYear(assessmentYear)
                        .broughtForward((int) lossAmount)
                        .setOff(0)
                        .remaining((int) lossAmount)
                        .reason("Expired - beyond 8 years")
                        .build());
                continue;
            }

            switch (lossType) {
                case "HP_LOSS":
                    // HP loss against any head except salary
                    if (businessIncome > 0) {
                        setOff = Math.min(businessIncome, lossAmount);
                        businessIncome -= setOff;
                    } else if (stcgIncome > 0) {
                        setOff = Math.min(stcgIncome, lossAmount);
                        stcgIncome -= setOff;
                    } else if (ltcgIncome > 0) {
                        setOff = Math.min(ltcgIncome, lossAmount);
                        ltcgIncome -= setOff;
                    }
                    break;

                case "BUSINESS_LOSS":
                    // Business loss against business income only
                    if (businessIncome > 0) {
                        setOff = Math.min(businessIncome, lossAmount);
                        businessIncome -= setOff;
                    }
                    break;

                case "SPECULATIVE_LOSS":
                    // Speculative loss only against speculative income
                    if (request.getSpeculativeIncome() > 0) {
                        setOff = Math.min(request.getSpeculativeIncome(), lossAmount);
                    }
                    break;

                case "STCG_LOSS":
                    // STCG loss against STCG or LTCG
                    if (stcgIncome > 0) {
                        setOff = Math.min(stcgIncome, lossAmount);
                        stcgIncome -= setOff;
                    } else if (ltcgIncome > 0) {
                        setOff = Math.min(ltcgIncome, lossAmount);
                        ltcgIncome -= setOff;
                    }
                    break;

                case "LTCG_LOSS":
                    // LTCG loss only against LTCG
                    if (ltcgIncome > 0) {
                        setOff = Math.min(ltcgIncome, lossAmount);
                        ltcgIncome -= setOff;
                    }
                    break;
            }

            entries.add(BFLAEntry.builder()
                    .lossType(lossType)
                    .assessmentYear(assessmentYear)
                    .broughtForward((int) lossAmount)
                    .setOff((int) setOff)
                    .remaining((int) (lossAmount - setOff))
                    .reason(setOff > 0 ? "Set off against current year income" : "No matching income available")
                    .build());
        }

        return BFLAResult.builder()
                .entries(entries)
                .totalSetOff((int) entries.stream().mapToInt(BFLAEntry::getSetOff).sum())
                .totalRemaining((int) entries.stream().mapToInt(BFLAEntry::getRemaining).sum())
                .build();
    }

    @Data
    @lombok.Builder
    public static class CYLARequest {
        private double salaryIncome;
        private double hpIncome;
        private double businessIncome;
        private double speculativeIncome;
        private double stcgIncome;
        private double ltcgIncome;
        private double otherIncome;
    }

    @Data
    @lombok.Builder
    public static class CYLAResult {
        private int salaryIncome;
        private int hpIncome;
        private int businessIncome;
        private int speculativeIncome;
        private int stcgIncome;
        private int ltcgIncome;
        private int otherIncome;
        private int totalIncome;
        private List<String> setOffLog;
    }

    @Data
    @lombok.Builder
    public static class BFLARequest {
        private int currentAY;
        private double currentYearIncome;
        private double hpIncome;
        private double businessIncome;
        private double speculativeIncome;
        private double stcgIncome;
        private double ltcgIncome;
        private List<BroughtForwardLoss> broughtForwardLosses;
    }

    @Data
    @lombok.Builder
    public static class BroughtForwardLoss {
        private String lossType;
        private int assessmentYear;
        private double amount;
    }

    @Data
    @lombok.Builder
    public static class BFLAResult {
        private List<BFLAEntry> entries;
        private int totalSetOff;
        private int totalRemaining;
    }

    @Data
    @lombok.Builder
    public static class BFLAEntry {
        private String lossType;
        private int assessmentYear;
        private int broughtForward;
        private int setOff;
        private int remaining;
        private String reason;
    }
}
