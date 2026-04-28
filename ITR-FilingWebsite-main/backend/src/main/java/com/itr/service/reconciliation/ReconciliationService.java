package com.itr.service.reconciliation;

import com.itr.dto.AISData;
import com.itr.dto.Form26ASData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AIS vs 26AS Reconciliation Service - CBDT Compliant
 * Identifies discrepancies between AIS and 26AS data
 */
@Service
@Slf4j
public class ReconciliationService {

    /**
     * Reconcile TDS data between AIS and 26AS
     */
    public ReconciliationResult reconcileTDS(AISData ais, Form26ASData f26as) {
        ReconciliationResult result = new ReconciliationResult();
        
        if (ais == null || f26as == null) {
            result.addWarning("Cannot reconcile: AIS or 26AS data missing");
            return result;
        }
        
        // Build maps for comparison
        Map<String, TDSEntry> aisMap = buildAISMap(ais);
        Map<String, TDSEntry> f26asMap = build26ASMap(f26as);
        
        // Find entries in 26AS but not in AIS
        for (Map.Entry<String, TDSEntry> entry : f26asMap.entrySet()) {
            String key = entry.getKey();
            TDSEntry f26asEntry = entry.getValue();
            
            if (!aisMap.containsKey(key)) {
                result.addDiscrepancy(new Discrepancy(
                    "MISSING_IN_AIS",
                    "TDS entry in 26AS not found in AIS",
                    f26asEntry.deductorName,
                    f26asEntry.section,
                    0,
                    f26asEntry.tdsAmount,
                    f26asEntry.tdsAmount
                ));
            } else {
                // Compare amounts
                TDSEntry aisEntry = aisMap.get(key);
                double diff = Math.abs(aisEntry.tdsAmount - f26asEntry.tdsAmount);
                
                if (diff > 1) { // Allow 1 rupee tolerance
                    result.addDiscrepancy(new Discrepancy(
                        "AMOUNT_MISMATCH",
                        "TDS amount mismatch between AIS and 26AS",
                        f26asEntry.deductorName,
                        f26asEntry.section,
                        aisEntry.tdsAmount,
                        f26asEntry.tdsAmount,
                        diff
                    ));
                }
            }
        }
        
        // Find entries in AIS but not in 26AS
        for (Map.Entry<String, TDSEntry> entry : aisMap.entrySet()) {
            String key = entry.getKey();
            TDSEntry aisEntry = entry.getValue();
            
            if (!f26asMap.containsKey(key)) {
                result.addDiscrepancy(new Discrepancy(
                    "MISSING_IN_26AS",
                    "TDS entry in AIS not found in 26AS",
                    aisEntry.deductorName,
                    aisEntry.section,
                    aisEntry.tdsAmount,
                    0,
                    aisEntry.tdsAmount
                ));
            }
        }
        
        // Summary
        result.totalAISTDS = aisMap.values().stream().mapToDouble(e -> e.tdsAmount).sum();
        result.total26ASTDS = f26asMap.values().stream().mapToDouble(e -> e.tdsAmount).sum();
        result.totalDiscrepancy = Math.abs(result.totalAISTDS - result.total26ASTDS);
        
        log.info("Reconciliation complete: {} discrepancies found, total difference: ₹{}", 
                result.discrepancies.size(), result.totalDiscrepancy);
        
        return result;
    }

    private Map<String, TDSEntry> buildAISMap(AISData ais) {
        Map<String, TDSEntry> map = new HashMap<>();
        
        if (ais.getPartB1() != null && ais.getPartB1().getTdsEntries() != null) {
            for (AISData.AISTDSEntry entry : ais.getPartB1().getTdsEntries()) {
                String key = entry.getDeductorTAN() + "_" + entry.getSection();
                map.put(key, new TDSEntry(
                    entry.getDeductorName(),
                    entry.getDeductorTAN(),
                    entry.getSection(),
                    entry.getTotalTDSDeducted()
                ));
            }
        }
        
        return map;
    }

    private Map<String, TDSEntry> build26ASMap(Form26ASData f26as) {
        Map<String, TDSEntry> map = new HashMap<>();
        
        if (f26as.getPartIEntries() != null) {
            for (Form26ASData.TDSEntry26AS entry : f26as.getPartIEntries()) {
                String key = entry.getTan() + "_" + entry.getSection();
                map.put(key, new TDSEntry(
                    entry.getDeductorName(),
                    entry.getTan(),
                    entry.getSection(),
                    entry.getTaxDeducted() != null ? entry.getTaxDeducted().doubleValue() : 0
                ));
            }
        }
        
        return map;
    }

    @Data
    private static class TDSEntry {
        private final String deductorName;
        private final String deductorTAN;
        private final String section;
        private final double tdsAmount;
    }

    @Data
    public static class ReconciliationResult {
        private final List<Discrepancy> discrepancies = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private double totalAISTDS;
        private double total26ASTDS;
        private double totalDiscrepancy;
        
        public void addDiscrepancy(Discrepancy d) {
            discrepancies.add(d);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
            log.warn("Reconciliation Warning: {}", warning);
        }
        
        public boolean hasDiscrepancies() {
            return !discrepancies.isEmpty();
        }
        
        public int getDiscrepancyCount() {
            return discrepancies.size();
        }
    }

    @Data
    public static class Discrepancy {
        private final String type; // MISSING_IN_AIS, MISSING_IN_26AS, AMOUNT_MISMATCH
        private final String description;
        private final String deductorName;
        private final String section;
        private final double aisAmount;
        private final double f26asAmount;
        private final double difference;
        
        public String getSuggestion() {
            switch (type) {
                case "MISSING_IN_AIS":
                    return "Add this TDS entry manually from 26AS. It may not have been reported in AIS yet.";
                case "MISSING_IN_26AS":
                    return "Verify with deductor. This entry appears in AIS but not in 26AS.";
                case "AMOUNT_MISMATCH":
                    return "Use 26AS amount as it is authoritative. Contact deductor if significant difference.";
                default:
                    return "Review and resolve manually.";
            }
        }
    }
}
