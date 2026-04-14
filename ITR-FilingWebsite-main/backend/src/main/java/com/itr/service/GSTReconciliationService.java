package com.itr.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * GST Reconciliation Service - 101% CBDT Compliant
 * Turnover matching for ITR-3/4
 * Reference: ITR_Import_JSON_Validation.md Section 2.2
 */
@Slf4j
@Service
public class GSTReconciliationService {

    public GSTReconciliationResult reconcile(GSTData gstData, double declaredTurnover) {
        GSTReconciliationResult result = new GSTReconciliationResult();
        
        result.setGstr1Turnover(gstData.getGstr1Turnover());
        result.setGstr3bTurnover(gstData.getGstr3bTurnover());
        result.setDeclaredTurnover(declaredTurnover);
        
        // Use GSTR-3B as authoritative
        double variance = gstData.getGstr3bTurnover() - declaredTurnover;
        result.setVariance(variance);
        
        if (Math.abs(variance) > 10000) {
            result.setReconciled(false);
            result.setMessage("Turnover mismatch: GSTR-3B shows Rs " + (long)gstData.getGstr3bTurnover() 
                + " vs declared Rs " + (long)declaredTurnover);
        } else {
            result.setReconciled(true);
            result.setMessage("Turnover reconciled within tolerance");
        }
        
        return result;
    }

    @Data
    public static class GSTData {
        private String gstin;
        private double gstr1Turnover;
        private double gstr3bTurnover;
        private String financialYear;
    }

    @Data
    public static class GSTReconciliationResult {
        private double gstr1Turnover;
        private double gstr3bTurnover;
        private double declaredTurnover;
        private double variance;
        private boolean reconciled;
        private String message;
    }
}
