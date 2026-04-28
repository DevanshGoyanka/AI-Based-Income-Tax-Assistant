package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Reconciliation report for AIS vs 26AS discrepancies
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationReport {
    private boolean hasDiscrepancies;
    private List<ReconciliationItem> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconciliationItem {
        private String deductorName;
        private String tan;
        private long income26AS;
        private long incomeAIS;
        private long tds26AS;
        private long tdsAIS;
        private long incomeDiff;
        private long tdsDiff;
        private String recommendedAction;
    }

    public void addDiscrepancy(ReconciliationItem item) {
        this.items.add(item);
        this.hasDiscrepancies = true;
    }
}
