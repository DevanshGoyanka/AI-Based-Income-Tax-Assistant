package com.itr.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Stub AIS Reconciliation Service - temporarily disabled
 */
@Slf4j
@Service
public class AISReconciliationService {

    public static class IncomeFlag {
        private final String message;
        public IncomeFlag(String message) { this.message = message; }
        public String getMessage() { return message; }
    }

    public static class ReconciliationReport {
        private final boolean success;
        private final String message;
        private final List<IncomeFlag> undeclaredIncomeFlags = new ArrayList<>();

        public ReconciliationReport(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public boolean hasUndeclaredIncome() { return !undeclaredIncomeFlags.isEmpty(); }
        public List<IncomeFlag> getUndeclaredIncomeFlags() { return undeclaredIncomeFlags; }
    }

    public ReconciliationReport reconcile(Object aisData, Object form26AS, Object tisData) {
        log.warn("AIS Reconciliation Service is temporarily disabled");
        return new ReconciliationReport(false, "Service disabled");
    }
}
