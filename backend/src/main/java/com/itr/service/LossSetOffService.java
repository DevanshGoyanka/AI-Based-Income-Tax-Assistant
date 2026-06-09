package com.itr.service;

import com.itr.dto.ScheduleCYLA;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Loss Set-Off Service - Schedule CYLA Implementation
 * 101% CBDT Compliant - Section 70, 71
 * 
 * Order of Set-Off (CRITICAL):
 * 1. HP Loss → Set off against: Salary, Other Sources, CG (all types), Business income. Cap: ₹2,00,000
 * 2. Non-speculative business loss → Set off against: All heads EXCEPT salary
 * 3. Speculative business loss → ONLY against speculative business income
 * 4. STCG loss → Set off against STCG and LTCG of any type
 * 5. LTCG loss → Set off against LTCG only
 */
@Slf4j
@Service
public class LossSetOffService {

    private static final double HP_LOSS_INTER_HEAD_CAP = 200000; // ₹2,00,000

    /**
     * Compute Schedule CYLA - Current Year Loss Adjustment
     */
    public ScheduleCYLA computeCYLA(ScheduleCYLA cyla) {
        log.info("Computing Schedule CYLA - Current Year Loss Adjustment");
        
        // Step 1: HP Loss Set-Off (Max ₹2L inter-head)
        setOffHPLoss(cyla);
        
        // Step 2: Non-Speculative Business Loss Set-Off (Cannot touch salary)
        setOffBusinessLoss(cyla);
        
        // Step 3: Speculative Business Loss Set-Off (Only against speculative income)
        setOffSpeculativeLoss(cyla);
        
        // Step 4: STCG Loss Set-Off (Against STCG first, then LTCG)
        setOffSTCGLoss(cyla);
        
        // Step 5: LTCG Loss Set-Off (Only against LTCG)
        setOffLTCGLoss(cyla);
        
        // Calculate final income after all set-offs
        calculateFinalIncome(cyla);
        
        log.info("CYLA completed. GTI after CYLA: ₹{}", cyla.getGrossTotalIncomeAfterCYLA());
        
        return cyla;
    }

    /**
     * Step 1: HP Loss Set-Off
     * HP loss can be set off against: Salary, Other Sources, CG (all types), Business income
     * Maximum inter-head set-off: ₹2,00,000
     */
    private void setOffHPLoss(ScheduleCYLA cyla) {
        double hpLoss = cyla.getHpLoss();
        if (hpLoss <= 0) {
            return; // No HP loss to set off
        }
        
        double remainingLoss = hpLoss;
        double totalSetOff = 0;
        
        // Set off against Salary (up to ₹2L cap)
        if (remainingLoss > 0 && cyla.getSalaryIncome() > 0) {
            double setOff = Math.min(Math.min(remainingLoss, cyla.getSalaryIncome()), HP_LOSS_INTER_HEAD_CAP);
            cyla.setHpLossSetOffAgainstSalary(setOff);
            remainingLoss -= setOff;
            totalSetOff += setOff;
        }
        
        // Set off against Other Sources (up to remaining ₹2L cap)
        if (remainingLoss > 0 && cyla.getOtherSourcesIncome() > 0 && totalSetOff < HP_LOSS_INTER_HEAD_CAP) {
            double availableCap = HP_LOSS_INTER_HEAD_CAP - totalSetOff;
            double setOff = Math.min(Math.min(remainingLoss, cyla.getOtherSourcesIncome()), availableCap);
            cyla.setHpLossSetOffAgainstOtherSources(setOff);
            remainingLoss -= setOff;
            totalSetOff += setOff;
        }
        
        // Set off against Business Income (up to remaining ₹2L cap)
        if (remainingLoss > 0 && cyla.getBusinessIncome() > 0 && totalSetOff < HP_LOSS_INTER_HEAD_CAP) {
            double availableCap = HP_LOSS_INTER_HEAD_CAP - totalSetOff;
            double setOff = Math.min(Math.min(remainingLoss, cyla.getBusinessIncome()), availableCap);
            cyla.setHpLossSetOffAgainstBusiness(setOff);
            remainingLoss -= setOff;
            totalSetOff += setOff;
        }
        
        // Set off against STCG (up to remaining ₹2L cap)
        if (remainingLoss > 0 && cyla.getStcgIncome() > 0 && totalSetOff < HP_LOSS_INTER_HEAD_CAP) {
            double availableCap = HP_LOSS_INTER_HEAD_CAP - totalSetOff;
            double setOff = Math.min(Math.min(remainingLoss, cyla.getStcgIncome()), availableCap);
            cyla.setHpLossSetOffAgainstSTCG(setOff);
            remainingLoss -= setOff;
            totalSetOff += setOff;
        }
        
        // Set off against LTCG (up to remaining ₹2L cap)
        if (remainingLoss > 0 && cyla.getLtcgIncome() > 0 && totalSetOff < HP_LOSS_INTER_HEAD_CAP) {
            double availableCap = HP_LOSS_INTER_HEAD_CAP - totalSetOff;
            double setOff = Math.min(Math.min(remainingLoss, cyla.getLtcgIncome()), availableCap);
            cyla.setHpLossSetOffAgainstLTCG(setOff);
            remainingLoss -= setOff;
            totalSetOff += setOff;
        }
        
        cyla.setTotalHpLossSetOff(totalSetOff);
        cyla.setHpLossUnabsorbed(remainingLoss);
        
        // Warning if HP loss exceeds ₹2L
        if (hpLoss > HP_LOSS_INTER_HEAD_CAP) {
            cyla.setHpLossWarning(String.format(
                "HP loss of ₹%.2f exceeds ₹2,00,000 inter-head set-off cap. " +
                "Unabsorbed loss of ₹%.2f will be carried forward for 8 years (set off against HP income only). " +
                "ITR-1 cannot carry forward HP loss - must file ITR-2.",
                hpLoss, remainingLoss
            ));
        }
        
        log.debug("HP Loss Set-Off: Total=₹{}, Unabsorbed=₹{}", totalSetOff, remainingLoss);
    }

    /**
     * Step 2: Non-Speculative Business Loss Set-Off
     * Can be set off against: All heads EXCEPT salary
     */
    private void setOffBusinessLoss(ScheduleCYLA cyla) {
        double businessLoss = cyla.getNonSpeculativeBusinessLoss();
        if (businessLoss <= 0) {
            return;
        }
        
        double remainingLoss = businessLoss;
        double totalSetOff = 0;
        
        // Set off against HP Income (after HP loss set-off)
        double hpIncomeAvailable = cyla.getHpIncome() - cyla.getHpLossSetOffAgainstBusiness();
        if (remainingLoss > 0 && hpIncomeAvailable > 0) {
            double setOff = Math.min(remainingLoss, hpIncomeAvailable);
            cyla.setBusinessLossSetOffAgainstHP(setOff);
            remainingLoss -= setOff;
            totalSetOff += setOff;
        }
        
        // Set off against Other Sources (after HP loss set-off)
        double osIncomeAvailable = cyla.getOtherSourcesIncome() - cyla.getHpLossSetOffAgainstOtherSources();
        if (remainingLoss > 0 && osIncomeAvailable > 0) {
            double setOff = Math.min(remainingLoss, osIncomeAvailable);
            cyla.setBusinessLossSetOffAgainstOtherSources(setOff);
            remainingLoss -= setOff;
            totalSetOff += setOff;
        }
        
        // Set off against STCG (after HP loss set-off)
        double stcgIncomeAvailable = cyla.getStcgIncome() - cyla.getHpLossSetOffAgainstSTCG();
        if (remainingLoss > 0 && stcgIncomeAvailable > 0) {
            double setOff = Math.min(remainingLoss, stcgIncomeAvailable);
            cyla.setBusinessLossSetOffAgainstSTCG(setOff);
            remainingLoss -= setOff;
            totalSetOff += setOff;
        }
        
        // Set off against LTCG (after HP loss set-off)
        double ltcgIncomeAvailable = cyla.getLtcgIncome() - cyla.getHpLossSetOffAgainstLTCG();
        if (remainingLoss > 0 && ltcgIncomeAvailable > 0) {
            double setOff = Math.min(remainingLoss, ltcgIncomeAvailable);
            cyla.setBusinessLossSetOffAgainstLTCG(setOff);
            remainingLoss -= setOff;
            totalSetOff += setOff;
        }
        
        cyla.setTotalBusinessLossSetOff(totalSetOff);
        cyla.setBusinessLossUnabsorbed(remainingLoss);
        
        if (remainingLoss > 0) {
            cyla.setBusinessLossWarning(String.format(
                "Non-speculative business loss of ₹%.2f will be carried forward for 8 years " +
                "(set off against business income only in subsequent years).",
                remainingLoss
            ));
        }
        
        log.debug("Business Loss Set-Off: Total=₹{}, Unabsorbed=₹{}", totalSetOff, remainingLoss);
    }

    /**
     * Step 3: Speculative Business Loss Set-Off
     * Can ONLY be set off against speculative business income
     */
    private void setOffSpeculativeLoss(ScheduleCYLA cyla) {
        double speculativeLoss = cyla.getSpeculativeBusinessLoss();
        if (speculativeLoss <= 0) {
            return;
        }
        
        double speculativeIncome = cyla.getSpeculativeBusinessIncome();
        double setOff = Math.min(speculativeLoss, speculativeIncome);
        
        cyla.setSpeculativeLossSetOff(setOff);
        cyla.setSpeculativeLossUnabsorbed(speculativeLoss - setOff);
        
        if (speculativeLoss > setOff) {
            cyla.setSpeculativeLossWarning(String.format(
                "Speculative business loss of ₹%.2f will be carried forward for 4 years " +
                "(set off against speculative business income only).",
                speculativeLoss - setOff
            ));
        }
        
        log.debug("Speculative Loss Set-Off: Total=₹{}, Unabsorbed=₹{}", setOff, speculativeLoss - setOff);
    }

    /**
     * Step 4: STCG Loss Set-Off
     * Can be set off against STCG first, then LTCG
     */
    private void setOffSTCGLoss(ScheduleCYLA cyla) {
        double stcgLoss = cyla.getStcgLoss();
        if (stcgLoss <= 0) {
            return;
        }
        
        double remainingLoss = stcgLoss;
        
        // Set off against STCG income (after HP and business loss set-offs)
        double stcgIncomeAvailable = cyla.getStcgIncome() 
                - cyla.getHpLossSetOffAgainstSTCG() 
                - cyla.getBusinessLossSetOffAgainstSTCG();
        
        if (remainingLoss > 0 && stcgIncomeAvailable > 0) {
            double setOff = Math.min(remainingLoss, stcgIncomeAvailable);
            cyla.setStcgLossSetOffAgainstSTCG(setOff);
            remainingLoss -= setOff;
        }
        
        // Set off against LTCG income (after HP and business loss set-offs)
        double ltcgIncomeAvailable = cyla.getLtcgIncome() 
                - cyla.getHpLossSetOffAgainstLTCG() 
                - cyla.getBusinessLossSetOffAgainstLTCG();
        
        if (remainingLoss > 0 && ltcgIncomeAvailable > 0) {
            double setOff = Math.min(remainingLoss, ltcgIncomeAvailable);
            cyla.setStcgLossSetOffAgainstLTCG(setOff);
            remainingLoss -= setOff;
        }
        
        cyla.setTotalStcgLossSetOff(stcgLoss - remainingLoss);
        cyla.setStcgLossUnabsorbed(remainingLoss);
        
        log.debug("STCG Loss Set-Off: Total=₹{}, Unabsorbed=₹{}", stcgLoss - remainingLoss, remainingLoss);
    }

    /**
     * Step 5: LTCG Loss Set-Off
     * Can ONLY be set off against LTCG
     */
    private void setOffLTCGLoss(ScheduleCYLA cyla) {
        double ltcgLoss = cyla.getLtcgLoss();
        if (ltcgLoss <= 0) {
            return;
        }
        
        // Set off against LTCG income (after all previous set-offs)
        double ltcgIncomeAvailable = cyla.getLtcgIncome() 
                - cyla.getHpLossSetOffAgainstLTCG() 
                - cyla.getBusinessLossSetOffAgainstLTCG()
                - cyla.getStcgLossSetOffAgainstLTCG();
        
        double setOff = Math.min(ltcgLoss, ltcgIncomeAvailable);
        
        cyla.setLtcgLossSetOff(setOff);
        cyla.setLtcgLossUnabsorbed(ltcgLoss - setOff);
        
        log.debug("LTCG Loss Set-Off: Total=₹{}, Unabsorbed=₹{}", setOff, ltcgLoss - setOff);
    }

    /**
     * Calculate final income after all set-offs
     */
    private void calculateFinalIncome(ScheduleCYLA cyla) {
        // Salary (only HP loss can be set off against salary)
        cyla.setSalaryIncomeAfterSetOff(
            cyla.getSalaryIncome() - cyla.getHpLossSetOffAgainstSalary()
        );
        
        // HP Income (HP loss and business loss can be set off)
        cyla.setHpIncomeAfterSetOff(
            cyla.getHpIncome() - cyla.getBusinessLossSetOffAgainstHP()
        );
        
        // Business Income (only HP loss can be set off against business)
        cyla.setBusinessIncomeAfterSetOff(
            cyla.getBusinessIncome() - cyla.getHpLossSetOffAgainstBusiness()
        );
        
        // Speculative Business Income
        cyla.setSpeculativeBusinessIncomeAfterSetOff(
            cyla.getSpeculativeBusinessIncome() - cyla.getSpeculativeLossSetOff()
        );
        
        // STCG Income (HP loss, business loss, and STCG loss can be set off)
        cyla.setStcgIncomeAfterSetOff(
            cyla.getStcgIncome() 
            - cyla.getHpLossSetOffAgainstSTCG() 
            - cyla.getBusinessLossSetOffAgainstSTCG()
            - cyla.getStcgLossSetOffAgainstSTCG()
        );
        
        // LTCG Income (HP loss, business loss, STCG loss, and LTCG loss can be set off)
        cyla.setLtcgIncomeAfterSetOff(
            cyla.getLtcgIncome() 
            - cyla.getHpLossSetOffAgainstLTCG() 
            - cyla.getBusinessLossSetOffAgainstLTCG()
            - cyla.getStcgLossSetOffAgainstLTCG()
            - cyla.getLtcgLossSetOff()
        );
        
        // Other Sources (HP loss and business loss can be set off)
        cyla.setOtherSourcesIncomeAfterSetOff(
            cyla.getOtherSourcesIncome() 
            - cyla.getHpLossSetOffAgainstOtherSources()
            - cyla.getBusinessLossSetOffAgainstOtherSources()
        );
        
        // Calculate Gross Total Income after CYLA
        double gti = cyla.getSalaryIncomeAfterSetOff()
                + cyla.getHpIncomeAfterSetOff()
                + cyla.getBusinessIncomeAfterSetOff()
                + cyla.getSpeculativeBusinessIncomeAfterSetOff()
                + cyla.getStcgIncomeAfterSetOff()
                + cyla.getLtcgIncomeAfterSetOff()
                + cyla.getOtherSourcesIncomeAfterSetOff();
        
        cyla.setGrossTotalIncomeAfterCYLA(Math.max(0, gti));
    }
}
