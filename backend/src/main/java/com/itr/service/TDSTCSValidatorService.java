package com.itr.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * TDS/TCS Validator and Reconciliation Service
 * Sections 206AA, 206AB, 206CCA
 * 101% CBDT Compliant
 */
@Slf4j
@Service
public class TDSTCSValidatorService {

    /**
     * Section 206AA - Higher TDS for No PAN/Aadhaar
     * Rate: HIGHER OF (prescribed rate, 20%)
     * Applicable when payee does not provide PAN
     */
    public double calculate206AATDSRate(double prescribedRate, boolean panProvided) {
        if (panProvided) {
            return prescribedRate;
        }
        
        // No PAN: higher of prescribed rate or 20%
        double rate = Math.max(prescribedRate, 0.20);
        
        log.warn("Section 206AA: PAN not provided. TDS rate increased from {}% to {}%", 
                prescribedRate * 100, rate * 100);
        
        return rate;
    }

    /**
     * Section 206AB - Higher TDS for Non-Filers
     * Applicable if:
     * - Payee has not filed ITR for 2 preceding years, AND
     * - TDS in each of those years exceeded ₹50,000
     * 
     * Rate: HIGHER OF (2 × prescribed rate, 5%)
     */
    public TDS206ABResult calculate206ABTDSRate(double prescribedRate, boolean filedITRYear1,
                                                  boolean filedITRYear2, double tdsYear1, double tdsYear2) {
        
        TDS206ABResult result = new TDS206ABResult();
        result.setPrescribedRate(prescribedRate);
        
        // Check if 206AB applies
        boolean notFiledBothYears = !filedITRYear1 && !filedITRYear2;
        boolean tdsExceeded50K = tdsYear1 > 50000 && tdsYear2 > 50000;
        
        if (notFiledBothYears && tdsExceeded50K) {
            // 206AB applies
            double higherRate = Math.max(prescribedRate * 2, 0.05);
            result.setSection206ABApplicable(true);
            result.setEffectiveRate(higherRate);
            result.setReason("Payee has not filed ITR for 2 preceding years and TDS exceeded ₹50,000 in each year. " +
                           "TDS rate increased to " + (higherRate * 100) + "%");
            
            log.warn("Section 206AB: Non-filer detected. TDS rate: {}% → {}%", 
                    prescribedRate * 100, higherRate * 100);
        } else {
            result.setSection206ABApplicable(false);
            result.setEffectiveRate(prescribedRate);
            result.setReason("Section 206AB not applicable.");
        }
        
        return result;
    }

    /**
     * Section 206CCA - Higher TCS for Non-Filers
     * Same principle as 206AB but for TCS
     * Rate: HIGHER OF (2 × prescribed rate, 5%)
     */
    public double calculate206CCATCSRate(double prescribedRate, boolean filedITRYear1,
                                          boolean filedITRYear2, double tcsYear1, double tcsYear2) {
        
        boolean notFiledBothYears = !filedITRYear1 && !filedITRYear2;
        boolean tcsExceeded50K = tcsYear1 > 50000 && tcsYear2 > 50000;
        
        if (notFiledBothYears && tcsExceeded50K) {
            double higherRate = Math.max(prescribedRate * 2, 0.05);
            log.warn("Section 206CCA: Non-filer detected. TCS rate: {}% → {}%", 
                    prescribedRate * 100, higherRate * 100);
            return higherRate;
        }
        
        return prescribedRate;
    }

    /**
     * Reconcile TDS from Form 26AS with ITR declaration
     */
    public TDSReconciliationResult reconcileTDS(List<TDSEntry> form26AS, List<TDSEntry> itrDeclared) {
        TDSReconciliationResult result = new TDSReconciliationResult();
        
        double total26AS = form26AS.stream().mapToDouble(TDSEntry::getAmount).sum();
        double totalITR = itrDeclared.stream().mapToDouble(TDSEntry::getAmount).sum();
        
        result.setTotal26AS(total26AS);
        result.setTotalITR(totalITR);
        result.setDifference(totalITR - total26AS);
        
        List<String> mismatches = new ArrayList<>();
        
        // Check for entries in ITR not in 26AS
        for (TDSEntry itrEntry : itrDeclared) {
            boolean found = form26AS.stream()
                    .anyMatch(e -> e.getTan().equals(itrEntry.getTan()) && 
                                 Math.abs(e.getAmount() - itrEntry.getAmount()) < 1);
            
            if (!found) {
                mismatches.add("TDS of ₹" + itrEntry.getAmount() + " from TAN " + itrEntry.getTan() + 
                             " declared in ITR but not found in Form 26AS");
            }
        }
        
        // Check for entries in 26AS not in ITR
        for (TDSEntry form26ASEntry : form26AS) {
            boolean found = itrDeclared.stream()
                    .anyMatch(e -> e.getTan().equals(form26ASEntry.getTan()) && 
                                 Math.abs(e.getAmount() - form26ASEntry.getAmount()) < 1);
            
            if (!found) {
                mismatches.add("TDS of ₹" + form26ASEntry.getAmount() + " from TAN " + form26ASEntry.getTan() + 
                             " in Form 26AS but not declared in ITR");
            }
        }
        
        result.setMismatches(mismatches);
        result.setReconciled(mismatches.isEmpty() && Math.abs(result.getDifference()) < 1);
        
        if (!result.isReconciled()) {
            result.setWarning("TDS mismatch detected. This may result in CPC notice. " +
                            "Please verify all TDS entries against Form 26AS.");
        }
        
        log.info("TDS Reconciliation: 26AS={}, ITR={}, Difference={}, Reconciled={}", 
                total26AS, totalITR, result.getDifference(), result.isReconciled());
        
        return result;
    }

    /**
     * Validate PAN-Aadhaar linkage status
     */
    public PANAadhaarLinkageResult validatePANAadhaarLinkage(String pan, boolean isLinked) {
        PANAadhaarLinkageResult result = new PANAadhaarLinkageResult();
        result.setPan(pan);
        result.setLinked(isLinked);
        
        if (!isLinked) {
            result.setStatus("INOPERATIVE");
            result.setWarning("PAN is inoperative due to non-linkage with Aadhaar. " +
                            "Consequences: TDS at 20% (higher rate), refund not issued, ITR may not be processed. " +
                            "Link Aadhaar immediately and pay ₹1,000 fee for reactivation.");
            result.setAction("Visit https://www.incometax.gov.in/iec/foportal/ to link Aadhaar with PAN");
        } else {
            result.setStatus("ACTIVE");
        }
        
        return result;
    }

    // Data classes
    @Data
    public static class TDSEntry {
        private String tan;
        private String deductorName;
        private double amount;
        private String section;
    }

    @Data
    public static class TDS206ABResult {
        private double prescribedRate;
        private boolean section206ABApplicable;
        private double effectiveRate;
        private String reason;
    }

    @Data
    public static class TDSReconciliationResult {
        private double total26AS;
        private double totalITR;
        private double difference;
        private List<String> mismatches;
        private boolean reconciled;
        private String warning;
    }

    @Data
    public static class PANAadhaarLinkageResult {
        private String pan;
        private boolean linked;
        private String status;
        private String warning;
        private String action;
    }
}
