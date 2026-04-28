package com.itr.controller;

import com.itr.dto.*;
import com.itr.dto.Itr1FormData;
import com.itr.dto.Itr2FormData;
import com.itr.service.integration.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration Controller - 101% CBDT Compliant
 * Handles Form 16 PDF, AIS, 26AS, and ITD Prefill imports
 */
@Slf4j
@RestController
@RequestMapping("/api/integration")
public class IntegrationController {

    private final Form16ExtractionService form16Service;
    private final AISImportService aisService;
    private final Form26ASImportService form26ASService;
    private final TISImportService tisService;
    private final ITDPrefillImportService prefillService;
    private final AutoPopulationService autoPopulationService;
    private final com.itr.service.reconciliation.ReconciliationService reconciliationService;

    public IntegrationController(Form16ExtractionService form16Service,
                                AISImportService aisService,
                                Form26ASImportService form26ASService,
                                TISImportService tisService,
                                ITDPrefillImportService prefillService,
                                AutoPopulationService autoPopulationService,
                                com.itr.service.reconciliation.ReconciliationService reconciliationService) {
        this.form16Service = form16Service;
        this.aisService = aisService;
        this.form26ASService = form26ASService;
        this.tisService = tisService;
        this.prefillService = prefillService;
        this.autoPopulationService = autoPopulationService;
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/form16/extract")
    public ResponseEntity<Form16Data> extractForm16(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received Form 16 PDF upload: {}", file.getOriginalFilename());
            Form16Data data = form16Service.extractForm16(file);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Form 16 extraction failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/ais/import")
    public ResponseEntity<AISData> importAIS(
            @RequestParam("file") MultipartFile file,
            @RequestParam("pan") String pan,
            @RequestParam("dob") String dob) {
        try {
            log.info("Received AIS file upload: {}, PAN: {}, DOB: {}", file.getOriginalFilename(), pan, dob);
            java.time.LocalDate dobDate = java.time.LocalDate.parse(dob);
            AISData data = aisService.importAIS(file.getBytes(), pan, dobDate);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("AIS import failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/tis/import")
    public ResponseEntity<TISData> importTIS(
            @RequestParam("file") MultipartFile file,
            @RequestParam("pan") String pan,
            @RequestParam("dob") String dob) {
        try {
            log.info("Received TIS file upload: {}", file.getOriginalFilename());
            java.time.LocalDate dobDate = java.time.LocalDate.parse(dob);
            TISData data = tisService.importTIS(file.getBytes(), pan, dobDate);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("TIS import failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/26as/import")
    public ResponseEntity<Form26ASData> import26AS(
            @RequestParam("file") MultipartFile file,
            @RequestParam("pan") String pan,
            @RequestParam("dob") String dob) {
        try {
            log.info("Received Form 26AS file upload: {}", file.getOriginalFilename());
            java.time.LocalDate dobDate = java.time.LocalDate.parse(dob);
            Form26ASData data = form26ASService.import26AS(file.getBytes(), pan, dobDate);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Form 26AS import failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/autopopulate/all")
    public ResponseEntity<FlatFormData> autoPopulateAll(@RequestBody AutoPopulateAllRequest request) {
        try {
            FlatFormData flat = autoPopulationService.autoPopulateAll(
                request.getAisData(), request.getForm26ASData(), request.getTisData(), request.getItrType());
            return ResponseEntity.ok(flat);
        } catch (Exception e) {
            log.error("Auto-population from all sources failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/reconciliation")
    public ResponseEntity<?> getReconciliation(@RequestBody ReconciliationRequest request) {
        try {
            var result = reconciliationService.reconcileTDS(
                request.getAisData(), request.getData26AS());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Reconciliation failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/prefill/import")
    public ResponseEntity<ITDPrefillData> importPrefill(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received ITD Prefill JSON upload: {}", file.getOriginalFilename());
            ITDPrefillData data = prefillService.importPrefillData(file);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("ITD Prefill import failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    

    @PostMapping("/autopopulate/form16")
    public ResponseEntity<Itr1FormData> autoPopulateFromForm16(
            @RequestBody AutoPopulateRequest request) {
        try {
            Itr1FormData populated = autoPopulationService.autoPopulateFromForm16(request.getFormData(), request.getForm16Data());
            return ResponseEntity.ok(populated);
        } catch (Exception e) {
            log.error("Auto-population from Form 16 failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/autopopulate/ais")
    public ResponseEntity<FlatFormData> autoPopulateFromAIS(
            @RequestBody AutoPopulateAISRequest request) {
        try {
            Itr1FormData populated = autoPopulationService.autoPopulateFromAIS(request.getFormData(), request.getAisData());
            FlatFormData flatData = flattenItr1Data(populated, request.getAisData());
            return ResponseEntity.ok(flatData);
        } catch (Exception e) {
            log.error("Auto-population from AIS failed", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    private FlatFormData flattenItr1Data(Itr1FormData itr1, AISData ais) {
        FlatFormData flat = new FlatFormData();
        
        log.info("Flattening ITR-1 data from AIS");
        
        // PART B1: TDS on Interest (Section 194A)
        if (ais.getTdsOther() != null && !ais.getTdsOther().isEmpty()) {
            double total194A = ais.getTdsOther().stream()
                    .filter(t -> "194A".equals(t.getSection()))
                    .mapToDouble(t -> t.getTaxDeducted() != null ? t.getTaxDeducted() : 0)
                    .sum();
            flat.setTds194A(total194A);
            log.info("Set tds194A: {}", total194A);
            
            // Interest from deposits (amount paid in 194A entries)
            double totalInterest = ais.getTdsOther().stream()
                    .filter(t -> "194A".equals(t.getSection()))
                    .mapToDouble(t -> t.getAmountPaid() != null ? t.getAmountPaid() : 0)
                    .sum();
            flat.setInterestFD(totalInterest);
            log.info("Set interestFD (from TDS 194A): {}", totalInterest);
            
            double totalOther = ais.getTdsOther().stream()
                    .filter(t -> !"194A".equals(t.getSection()))
                    .mapToDouble(t -> t.getTaxDeducted() != null ? t.getTaxDeducted() : 0)
                    .sum();
            flat.setTdsOther(totalOther);
        }
        
        // PART B2: SFT - Dividends
        if (ais.getSft() != null && ais.getSft().getEntries() != null) {
            double totalDividends = ais.getSft().getEntries().stream()
                    .filter(e -> e.getTransactionType() != null && e.getTransactionType().contains("Dividend"))
                    .mapToDouble(AISData.SFTEntry::getAmount)
                    .sum();
            flat.setDividends(totalDividends);
            log.info("Set dividends: {}", totalDividends);
            
            // PART B2: Capital Gains - Sale of Securities
            double saleOfSecurities = ais.getSft().getEntries().stream()
                    .filter(e -> e.getTransactionType() != null && e.getTransactionType().contains("Sale of Securities"))
                    .mapToDouble(AISData.SFTEntry::getAmount)
                    .sum();
            
            // PART B2: Capital Gains - Purchase of Securities
            double purchaseOfSecurities = ais.getSft().getEntries().stream()
                    .filter(e -> e.getTransactionType() != null && e.getTransactionType().contains("Purchase of Securities"))
                    .mapToDouble(AISData.SFTEntry::getAmount)
                    .sum();
            
            // Calculate capital gain (sale - purchase)
            if (saleOfSecurities > 0 || purchaseOfSecurities > 0) {
                double capitalGain = saleOfSecurities - purchaseOfSecurities;
                if (capitalGain != 0) {
                    // Determine if STCG or LTCG based on holding period (default to STCG if not specified)
                    flat.setStcgOtherSlab(Math.abs(capitalGain)); // Short term capital gain
                    log.info("Set stcgOtherSlab (Securities): {}", capitalGain);
                }
            }
            
            // PART B2: Mutual Fund Purchases (informational - not directly taxable)
            double mfPurchases = ais.getSft().getEntries().stream()
                    .filter(e -> e.getTransactionType() != null && e.getTransactionType().contains("Purchase of Mutual Funds"))
                    .mapToDouble(AISData.SFTEntry::getAmount)
                    .sum();
            log.info("MF Purchases (informational): {}", mfPurchases);
        }
        
        // PART B1: TDS on Salary
        if (ais.getTdsSalary() != null && !ais.getTdsSalary().isEmpty()) {
            double totalTDS = ais.getTdsSalary().stream()
                    .mapToDouble(AISData.TDSSalary::getTotalTaxDeducted)
                    .sum();
            flat.setTdsS192(totalTDS);
            log.info("Set tdsS192: {}", totalTDS);
        }
        
        // PART B3: Tax Payments
        if (ais.getTaxPayments() != null && !ais.getTaxPayments().isEmpty()) {
            double selfTax = ais.getTaxPayments().stream()
                    .filter(p -> "Self Assessment Tax".equalsIgnoreCase(p.getTaxType()))
                    .mapToDouble(AISData.TaxPayment::getAmount)
                    .sum();
            flat.setSelfTax(selfTax);
            log.info("Set selfTax: {}", selfTax);
            
            double advTax = ais.getTaxPayments().stream()
                    .filter(p -> "Advance Tax".equalsIgnoreCase(p.getTaxType()))
                    .mapToDouble(AISData.TaxPayment::getAmount)
                    .sum();
            
            // Split advance tax into quarters if present
            if (advTax > 0) {
                flat.setAdv15Jun(advTax / 4);
                flat.setAdv15Sep(advTax / 4);
                flat.setAdv15Dec(advTax / 4);
                flat.setAdv15Mar(advTax / 4);
            }
        }
        
        log.info("Flattened AIS data: dividends={}, interestFD={}, tds194A={}, selfTax={}", 
                 flat.getDividends(), flat.getInterestFD(), flat.getTds194A(), flat.getSelfTax());
        
        return flat;
    }

    @PostMapping("/autopopulate/26as")
    public ResponseEntity<Itr1FormData> autoPopulateFrom26AS(
            @RequestBody AutoPopulate26ASRequest request) {
        try {
            Itr1FormData populated = autoPopulationService.autoPopulateFrom26AS(request.getFormData(), request.getData26AS());
            return ResponseEntity.ok(populated);
        } catch (Exception e) {
            log.error("Auto-population from Form 26AS failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Request DTOs
    @lombok.Data
    public static class AutoPopulateRequest {
        private Itr1FormData formData;
        private Form16Data form16Data;
    }

    @lombok.Data
    public static class AutoPopulateAISRequest {
        private Itr1FormData formData;
        private AISData aisData;
    }

    @lombok.Data
    public static class AutoPopulate26ASRequest {
        private Itr1FormData formData;
        private Form26ASData data26AS;
    }

    @lombok.Data
    public static class AutoPopulateAllRequest {
        private Long clientId;
        private String year;
        private AISData aisData;
        private Form26ASData form26ASData;
        private TISData tisData;
        private String itrType;
    }

    @lombok.Data
    public static class ReconciliationRequest {
        private AISData aisData;
        private Form26ASData data26AS;
        private TISData tisData;
    }

    @PostMapping("/prefill/debug")
    public ResponseEntity<?> debugPrefillStructure(@RequestParam("file") MultipartFile file) {
        try {
            ITDPrefillData prefill = prefillService.importPrefillData(file);
            
            Map<String, Object> debug = new HashMap<>();
            debug.put("hasPersonalInfo", prefill.getPersonalInfo() != null);
            debug.put("hasInsights", prefill.getInsights() != null);
            debug.put("hasForm24q", prefill.getForm24q() != null);
            debug.put("hasForm26as", prefill.getForm26as() != null);
            debug.put("hasBankDetails", prefill.getBankAccountDtls() != null && !prefill.getBankAccountDtls().isEmpty());
            
            if (prefill.getInsights() != null) {
                Map<String, Object> insights = new HashMap<>();
                insights.put("hasCumulativeSalary", prefill.getInsights().getCumulativeSalary() != null);
                insights.put("hasSalaries", prefill.getInsights().getSalaries() != null);
                insights.put("hasInterest", prefill.getInsights().getIntrstFrmSavingBank() != null);
                debug.put("insightsDetails", insights);
            }
            
            if (prefill.getForm24q() != null) {
                Map<String, Object> form24q = new HashMap<>();
                form24q.put("hasIncomeDeductions", prefill.getForm24q().getIncomeDeductions() != null);
                form24q.put("hasSalaries", prefill.getForm24q().getSalaries() != null);
                debug.put("form24qDetails", form24q);
            }
            
            if (prefill.getForm26as() != null) {
                Map<String, Object> form26as = new HashMap<>();
                form26as.put("hasTdsOnSalaries", prefill.getForm26as().getTdsOnSalaries() != null);
                form26as.put("hasScheduleOS", prefill.getForm26as().getScheduleOS() != null);
                form26as.put("hasTaxPayments", prefill.getForm26as().getTaxPayments() != null);
                debug.put("form26asDetails", form26as);
            }
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            log.error("Debug prefill structure failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/autopopulate/prefill")
    public ResponseEntity<?> autoPopulateFromPrefill(
            @RequestBody PrefillAutoPopulateRequest request) {
        try {
            String itrType = request.getItrType();
            ITDPrefillData prefillData = request.getPrefillData();
            
            if ("ITR-1".equals(itrType)) {
                // Check if flat form data is provided (frontend compatibility)
                if (request.getFlatFormData() != null) {
                    FlatFormData formData = request.getFlatFormData();
                    FlatFormData populated = prefillService.autoPopulateFromPrefillFlat(formData, prefillData);
                    log.info("Prefill auto-population complete - returning flat data with {} TDS entries", 
                        populated.getTdsEntries() != null ? populated.getTdsEntries().size() : 0);
                    return ResponseEntity.ok(populated);
                } else if (request.getItr1FormData() != null) {
                    Itr1FormData formData = request.getItr1FormData();
                    Itr1FormData populated = prefillService.autoPopulateFromPrefill(formData, prefillData);
                    return ResponseEntity.ok(populated);
                } else {
                    // Initialize empty flat form data if none provided
                    log.info("No form data provided, initializing empty FlatFormData");
                    FlatFormData formData = FlatFormData.builder().build();
                    FlatFormData populated = prefillService.autoPopulateFromPrefillFlat(formData, prefillData);
                    log.info("Prefill auto-population complete - returning flat data with {} TDS entries", 
                        populated.getTdsEntries() != null ? populated.getTdsEntries().size() : 0);
                    return ResponseEntity.ok(populated);
                }
            } else if ("ITR-2".equals(itrType)) {
                Itr2FormData formData = request.getItr2FormData();
                Itr2FormData populated = prefillService.autoPopulateFromPrefill(formData, prefillData);
                return ResponseEntity.ok(populated);
            } else if ("ITR-3".equals(itrType)) {
                Itr3FormData formData = request.getItr3FormData();
                Itr3FormData populated = prefillService.autoPopulateFromPrefill(formData, prefillData);
                return ResponseEntity.ok(populated);
            } else if ("ITR-4".equals(itrType)) {
                Itr4FormData formData = request.getItr4FormData();
                Itr4FormData populated = prefillService.autoPopulateFromPrefill(formData, prefillData);
                return ResponseEntity.ok(populated);
            } else {
                return ResponseEntity.badRequest().body("Unsupported ITR type: " + itrType);
            }
        } catch (Exception e) {
            log.error("Auto-population from ITD Prefill failed", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @lombok.Data
    public static class PrefillAutoPopulateRequest {
        private String itrType;
        private ITDPrefillData prefillData;
        private FlatFormData flatFormData;  // Frontend compatibility
        private Itr1FormData itr1FormData;
        private Itr2FormData itr2FormData;
        private Itr3FormData itr3FormData;
        private Itr4FormData itr4FormData;
    }

    @PostMapping("/autopopulate/itr2/ais")
    public ResponseEntity<Itr2FormData> autoPopulateITR2FromAIS(
            @RequestBody Itr2FormData formData,
            @RequestParam("ais") AISData aisData) {
        try {
            Itr2FormData populated = autoPopulationService.autoPopulateITR2FromAIS(formData, aisData);
            return ResponseEntity.ok(populated);
        } catch (Exception e) {
            log.error("Auto-population ITR-2 from AIS failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/autopopulate/itr2/26as")
    public ResponseEntity<Itr2FormData> autoPopulateITR2From26AS(
            @RequestBody Itr2FormData formData,
            @RequestParam("data26as") Form26ASData data26AS) {
        try {
            Itr2FormData populated = autoPopulationService.autoPopulateITR2From26AS(formData, data26AS);
            return ResponseEntity.ok(populated);
        } catch (Exception e) {
            log.error("Auto-population ITR-2 from Form 26AS failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @lombok.Data
    static class AutoPopulatePrefillRequest {
        private Itr1FormData formData;
        private ITDPrefillData prefillData;
    }
}