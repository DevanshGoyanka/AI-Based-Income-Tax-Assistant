package com.itr.service.reconciliation;

import com.itr.dto.FlatFormData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Employer Reconciliation Service
 * Prevents duplicate employer entries and detects discrepancies across import sources
 */
@Service
@Slf4j
public class EmployerReconciliationService {

    private static final double TOLERANCE = 1.0; // ₹1 tolerance for floating point comparison

    /**
     * Reconcile new employer entries with existing ones
     * Returns reconciliation result with merged entries and discrepancies
     */
    public EmployerReconciliationResult reconcileEmployers(
            List<FlatFormData.EmployerEntry> existingEntries,
            List<FlatFormData.EmployerEntry> newEntries,
            String importSource) {
        
        EmployerReconciliationResult result = new EmployerReconciliationResult();
        result.setImportSource(importSource);
        
        if (existingEntries == null) {
            existingEntries = new ArrayList<>();
        }
        if (newEntries == null || newEntries.isEmpty()) {
            result.setMergedEntries(new ArrayList<>(existingEntries));
            return result;
        }
        
        // Build map of existing entries by TAN (unique identifier)
        Map<String, FlatFormData.EmployerEntry> existingMap = existingEntries.stream()
            .filter(e -> e.getEmployerTAN() != null && !e.getEmployerTAN().isEmpty())
            .collect(Collectors.toMap(
                FlatFormData.EmployerEntry::getEmployerTAN,
                e -> e,
                (e1, e2) -> e1 // Keep first if duplicate TAN
            ));
        
        List<FlatFormData.EmployerEntry> mergedEntries = new ArrayList<>(existingEntries);
        
        for (FlatFormData.EmployerEntry newEntry : newEntries) {
            String tan = newEntry.getEmployerTAN();
            
            if (tan == null || tan.isEmpty()) {
                // No TAN - add as new entry with warning
                mergedEntries.add(newEntry);
                result.addWarning("Employer entry without TAN added: " + newEntry.getEmployerName());
                continue;
            }
            
            if (existingMap.containsKey(tan)) {
                // Employer exists - check for discrepancies
                FlatFormData.EmployerEntry existing = existingMap.get(tan);
                EmployerDiscrepancy discrepancy = compareEmployers(existing, newEntry, importSource);
                
                if (discrepancy.hasDiscrepancies()) {
                    result.addDiscrepancy(discrepancy);
                    log.warn("Discrepancy found for employer TAN {}: {}", tan, discrepancy.getSummary());
                } else {
                    // Exact match - skip duplicate
                    result.addSkipped(newEntry.getEmployerName() + " (TAN: " + tan + ") - exact match");
                    log.info("Skipped duplicate employer: {} (TAN: {})", newEntry.getEmployerName(), tan);
                }
            } else {
                // New employer - add to merged list
                mergedEntries.add(newEntry);
                existingMap.put(tan, newEntry);
                result.addNewEntry(newEntry.getEmployerName() + " (TAN: " + tan + ")");
                log.info("Added new employer: {} (TAN: {})", newEntry.getEmployerName(), tan);
            }
        }
        
        result.setMergedEntries(mergedEntries);
        return result;
    }

    /**
     * Compare two employer entries and identify discrepancies
     */
    private EmployerDiscrepancy compareEmployers(
            FlatFormData.EmployerEntry existing,
            FlatFormData.EmployerEntry newEntry,
            String importSource) {
        
        EmployerDiscrepancy discrepancy = new EmployerDiscrepancy();
        discrepancy.setEmployerName(existing.getEmployerName());
        discrepancy.setEmployerTAN(existing.getEmployerTAN());
        discrepancy.setImportSource(importSource);
        
        // Compare each field
        compareField(discrepancy, "Basic Salary", existing.getBasic(), newEntry.getBasic());
        compareField(discrepancy, "DA", existing.getDa(), newEntry.getDa());
        compareField(discrepancy, "HRA", existing.getHra(), newEntry.getHra());
        compareField(discrepancy, "Bonus", existing.getBonus(), newEntry.getBonus());
        compareField(discrepancy, "Allowances", existing.getAllowances(), newEntry.getAllowances());
        compareField(discrepancy, "Perquisites", existing.getPerquisites(), newEntry.getPerquisites());
        compareField(discrepancy, "Professional Tax", existing.getProfessionalTax(), newEntry.getProfessionalTax());
        compareField(discrepancy, "TDS Deducted", existing.getTdsDeducted(), newEntry.getTdsDeducted());
        compareField(discrepancy, "Gross Salary", existing.getGrossSalary(), newEntry.getGrossSalary());
        compareField(discrepancy, "Net Salary", existing.getNetSalary(), newEntry.getNetSalary());
        
        return discrepancy;
    }

    private void compareField(EmployerDiscrepancy discrepancy, String fieldName, Double existing, Double newValue) {
        if (existing == null) existing = 0.0;
        if (newValue == null) newValue = 0.0;
        
        double diff = Math.abs(existing - newValue);
        if (diff > TOLERANCE) {
            discrepancy.addFieldDiscrepancy(fieldName, existing, newValue, diff);
        }
    }

    @Data
    public static class EmployerReconciliationResult {
        private String importSource;
        private List<FlatFormData.EmployerEntry> mergedEntries = new ArrayList<>();
        private List<EmployerDiscrepancy> discrepancies = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> newEntries = new ArrayList<>();
        private List<String> skippedDuplicates = new ArrayList<>();
        
        public void addDiscrepancy(EmployerDiscrepancy d) {
            discrepancies.add(d);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public void addNewEntry(String entry) {
            newEntries.add(entry);
        }
        
        public void addSkipped(String entry) {
            skippedDuplicates.add(entry);
        }
        
        public boolean hasDiscrepancies() {
            return !discrepancies.isEmpty();
        }
        
        public int getDiscrepancyCount() {
            return discrepancies.size();
        }
        
        public String getSummary() {
            return String.format("Import from %s: %d new, %d duplicates skipped, %d discrepancies found",
                importSource, newEntries.size(), skippedDuplicates.size(), discrepancies.size());
        }
    }

    @Data
    public static class EmployerDiscrepancy {
        private String employerName;
        private String employerTAN;
        private String importSource;
        private List<FieldDiscrepancy> fieldDiscrepancies = new ArrayList<>();
        
        public void addFieldDiscrepancy(String fieldName, double existingValue, double newValue, double difference) {
            fieldDiscrepancies.add(new FieldDiscrepancy(fieldName, existingValue, newValue, difference));
        }
        
        public boolean hasDiscrepancies() {
            return !fieldDiscrepancies.isEmpty();
        }
        
        public String getSummary() {
            return String.format("%s (TAN: %s) has %d field discrepancies from %s",
                employerName, employerTAN, fieldDiscrepancies.size(), importSource);
        }
        
        public String getRecommendation() {
            return "Review discrepancies and choose the correct values. Form26AS/AIS data is generally more authoritative than Form16.";
        }
    }

    @Data
    public static class FieldDiscrepancy {
        private final String fieldName;
        private final double existingValue;
        private final double newValue;
        private final double difference;
        
        public String getDescription() {
            return String.format("%s: Existing ₹%.2f vs New ₹%.2f (diff: ₹%.2f)",
                fieldName, existingValue, newValue, difference);
        }
        
        public String getSuggestion() {
            if (fieldName.equals("TDS Deducted")) {
                return "Use Form26AS/AIS value for TDS as it is authoritative";
            } else if (fieldName.contains("Salary")) {
                return "Use Form26AS value for total salary; Form16 may show partial year";
            }
            return "Verify with employer documents and choose the correct value";
        }
    }
}
