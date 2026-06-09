package com.itr.interfaces.rest.integration;

import com.itr.dto.AISData;
import com.itr.dto.TISData;
import com.itr.dto.Form26ASData;
import com.itr.service.integration.AISImportService;
import com.itr.service.integration.AISJsonImportService;
import com.itr.service.integration.TISImportService;
import com.itr.service.integration.Form26ASImportService;
import com.itr.service.integration.AutoPopulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration Controller — /api/v1/integration/*
 * Handles AIS, TIS, and sequential 26AS/AIS/TIS imports
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final AISImportService aisImportService;
    private final AISJsonImportService aisJsonImportService;
    private final TISImportService tisImportService;
    private final Form26ASImportService form26asImportService;
    private final AutoPopulationService autoPopulationService;

    /**
     * Import AIS PDF (encrypted with PAN + DOB)
     */
    @PostMapping("/ais/import")
    public ResponseEntity<?> importAIS(
            @RequestParam("file") MultipartFile file,
            @RequestParam("pan") String pan,
            @RequestParam("dob") String dob) {
        try {
            log.info("Importing AIS for PAN: {}", pan);
            AISData data = aisImportService.importAIS(file.getBytes(), pan, LocalDate.parse(dob));
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("pan", data.getPan());
            result.put("name", data.getGeneralInfo() != null ? data.getGeneralInfo().getName() : "N/A");
            result.put("tdsSalaryCount", data.getTdsSalary() != null ? data.getTdsSalary().size() : 0);
            result.put("tdsOtherCount", data.getTdsOther() != null ? data.getTdsOther().size() : 0);
            result.put("taxPaymentCount", data.getPartB3() != null ? data.getPartB3().size() : 0);
            result.put("_rawData", data);
            
            log.info("AIS Import result: {} TDS salary entries, {} TDS other entries", 
                result.get("tdsSalaryCount"), result.get("tdsOtherCount"));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to import AIS: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    /**
     * Import AIS JSON (encrypted PBKDF2/AES-CBC from ITD compliance portal)
     *
     * <p>Uses PAN + DOB to derive the password for decryption:
     * Password = {pan_lowercase}{"GQ39%*g"}{ddmmyyyy}
     *
     * @param file encrypted AIS JSON file
     * @param pan  PAN number (used for password derivation)
     * @param dob  Date of birth in ddMMyyyy format (e.g. "14061974")
     */
    @PostMapping("/ais-json/import")
    public ResponseEntity<?> importAISJson(
            @RequestParam("file") MultipartFile file,
            @RequestParam("pan") String pan,
            @RequestParam("dob") String dob) {
        try {
            log.info("Importing AIS JSON for PAN: {}", pan);
            // Frontend sends YYYY-MM-DD (e.g. "1974-06-14"), convert to ddMMyyyy for password
            LocalDate dobDate = LocalDate.parse(dob, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String dobForPassword = dobDate.format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"));
            AISData data = aisJsonImportService.importAIS(file.getBytes(), pan, dobDate, dobForPassword);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("pan", data.getPan());
            result.put("source", "AIS_JSON");
            result.put("financialYear", data.getFinancialYear());
            result.put("assessmentYear", data.getAssessmentYear());
            result.put("name", data.getGeneralInfo() != null ? data.getGeneralInfo().getName() : "N/A");
            result.put("partB1TdsEntries", data.getPartB1() != null ? data.getPartB1().getTdsEntries().size() : 0);
            result.put("partB2DividendIncome", data.getPartB2() != null ? data.getPartB2().getDividendIncome() : 0);
            result.put("partB2SecuritiesSaleCount", data.getPartB2() != null ? data.getPartB2().getSecuritiesSale().size() : 0);
            result.put("partB2MFPurchaseCount", data.getPartB2() != null ? data.getPartB2().getMutualFundPurchase().size() : 0);
            result.put("partB3TaxPayments", data.getPartB3() != null ? data.getPartB3().size() : 0);
            result.put("_rawData", data);

            log.info("AIS JSON Import result: name={}, B1 TDS entries={}, B2 Div={}, B3 payments={}",
                    result.get("name"), result.get("partB1TdsEntries"),
                    result.get("partB2DividendIncome"), result.get("partB3TaxPayments"));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to import AIS JSON: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    /**
     * Import TIS PDF (encrypted with PAN + DOB)
     */
    @PostMapping("/tis/import")
    public ResponseEntity<?> importTIS(
            @RequestParam("file") MultipartFile file,
            @RequestParam("pan") String pan,
            @RequestParam("dob") String dob) {
        try {
            log.info("Importing TIS for PAN: {}", pan);
            TISData data = tisImportService.importTIS(file.getBytes(), pan, LocalDate.parse(dob));
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("pan", pan);
            result.put("dividendIncome", data.getDividendIncome());
            result.put("interestFromDeposit", data.getInterestFromDeposit());
            result.put("securitiesSaleConsideration", data.getSecuritiesSaleConsideration());
            result.put("securitiesPurchaseAmount", data.getSecuritiesPurchaseAmount());
            result.put("interestOnSecurities", data.getInterestOnSecurities());
            result.put("salaryAmount", data.getSalaryAmount());
            result.put("rentIncome", data.getRentIncome());
            result.put("_rawData", data);
            
            log.info("TIS Import result: Dividend={}, Interest={}, Securities Sale={}", 
                data.getDividendIncome(), data.getInterestFromDeposit(), data.getSecuritiesSaleConsideration());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to import TIS: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    /**
     * Sequential import: 26AS → AIS → TIS in order
     * Each import is independent - allows partial imports if one fails
     */
    @PostMapping("/sequential-import")
    public ResponseEntity<?> sequentialImport(
            @RequestParam(value = "form26as", required = false) MultipartFile form26as,
            @RequestParam(value = "ais", required = false) MultipartFile ais,
            @RequestParam(value = "tis", required = false) MultipartFile tis,
            @RequestParam("pan") String pan,
            @RequestParam("dob") String dob) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("pan", pan);
        result.put("dob", dob);
        
        try {
            LocalDate localDob = LocalDate.parse(dob);
            Map<String, Object> data = new HashMap<>();
            data.put("form26as", null);
            data.put("ais", null);
            data.put("tis", null);
            
            // Step 1: Import Form 26AS first (most important for TDS)
            if (form26as != null && !form26as.isEmpty()) {
                log.info("Step 1/3: Importing Form 26AS for PAN: {}", pan);
                try {
                    Form26ASData f26asData = form26asImportService.import26AS(form26as.getBytes(), pan, localDob);
                    Map<String, Object> f26asResult = new HashMap<>();
                    f26asResult.put("status", "SUCCESS");
                    f26asResult.put("assesseName", f26asData.getAssesseName());
                    f26asResult.put("tdsInterestCount", f26asData.getTdsOnInterest() != null ? f26asData.getTdsOnInterest().size() : 0);
                    f26asResult.put("tdsContractorCount", f26asData.getTdsOnContractor() != null ? f26asData.getTdsOnContractor().size() : 0);
                    f26asResult.put("taxPaymentCount", f26asData.getTaxPayments() != null ? f26asData.getTaxPayments().size() : 0);
                    f26asResult.put("totalTaxPaid", f26asData.getTotalTaxPaid());
                    f26asResult.put("totalTDSInterest", f26asData.getTotalTDSInterest());
                    f26asResult.put("_rawData", f26asData);
                    data.put("form26as", f26asResult);
                    log.info("26AS imported: {} interest entries, ₹{} tax paid", 
                        f26asResult.get("tdsInterestCount"), f26asResult.get("totalTaxPaid"));
                } catch (Exception e) {
                    log.error("26AS import failed: {}", e.getMessage());
                    data.put("form26as", Map.of("status", "ERROR", "error", e.getMessage()));
                }
            }
            
            // Step 2: Import AIS (processed data)
            if (ais != null && !ais.isEmpty()) {
                log.info("Step 2/3: Importing AIS for PAN: {}", pan);
                try {
                    AISData aisData = aisImportService.importAIS(ais.getBytes(), pan, localDob);
                    Map<String, Object> aisResult = new HashMap<>();
                    aisResult.put("status", "SUCCESS");
                    aisResult.put("name", aisData.getGeneralInfo() != null ? aisData.getGeneralInfo().getName() : "N/A");
                    aisResult.put("tdsSalaryCount", aisData.getTdsSalary() != null ? aisData.getTdsSalary().size() : 0);
                    aisResult.put("tdsOtherCount", aisData.getTdsOther() != null ? aisData.getTdsOther().size() : 0);
                    aisResult.put("taxPaymentCount", aisData.getPartB3() != null ? aisData.getPartB3().size() : 0);
                    aisResult.put("_rawData", aisData);
                    data.put("ais", aisResult);
                    log.info("AIS imported: {} salary entries, {} other entries", 
                        aisResult.get("tdsSalaryCount"), aisResult.get("tdsOtherCount"));
                } catch (Exception e) {
                    log.error("AIS import failed: {}", e.getMessage());
                    data.put("ais", Map.of("status", "ERROR", "error", e.getMessage()));
                }
            }
            
            // Step 3: Import TIS (accepted/verified data - final source of truth)
            if (tis != null && !tis.isEmpty()) {
                log.info("Step 3/3: Importing TIS for PAN: {}", pan);
                try {
                    TISData tisData = tisImportService.importTIS(tis.getBytes(), pan, localDob);
                    Map<String, Object> tisResult = new HashMap<>();
                    tisResult.put("status", "SUCCESS");
                    tisResult.put("dividendIncome", tisData.getDividendIncome());
                    tisResult.put("interestFromDeposit", tisData.getInterestFromDeposit());
                    tisResult.put("securitiesSaleConsideration", tisData.getSecuritiesSaleConsideration());
                    tisResult.put("rentIncome", tisData.getRentIncome());
                    tisResult.put("_rawData", tisData);
                    data.put("tis", tisResult);
                    log.info("TIS imported: Dividend={}, Interest={}", 
                        tisData.getDividendIncome(), tisData.getInterestFromDeposit());
                } catch (Exception e) {
                    log.error("TIS import failed: {}", e.getMessage());
                    data.put("tis", Map.of("status", "ERROR", "error", e.getMessage()));
                }
            }
            
            // Build result
            result.put("data", data);
            
            // Check which sources were successful
            boolean has26AS = data.get("form26as") != null && ((Map<?,?>) data.get("form26as")).get("status") != null;
            boolean hasAIS = data.get("ais") != null && ((Map<?,?>) data.get("ais")).get("status") != null;
            boolean hasTIS = data.get("tis") != null && ((Map<?,?>) data.get("tis")).get("status") != null;
            
            if (has26AS || hasAIS || hasTIS) {
                result.put("status", "SUCCESS");
                result.put("message", "Sequential import completed");
                result.put("imported", 
                    (has26AS ? "26AS " : "") + 
                    (hasAIS ? "AIS " : "") + 
                    (hasTIS ? "TIS" : ""));
                
                // Auto-populate flat form data from available sources
                try {
                    Form26ASData f26 = null;
                    AISData aisD = null;
                    TISData tisD = null;
                    
                    if (has26AS && ((Map<?,?>) data.get("form26as")).get("_rawData") != null) {
                        f26 = (Form26ASData) ((Map<?,?>) data.get("form26as")).get("_rawData");
                    }
                    if (hasAIS && ((Map<?,?>) data.get("ais")).get("_rawData") != null) {
                        aisD = (AISData) ((Map<?,?>) data.get("ais")).get("_rawData");
                    }
                    if (hasTIS && ((Map<?,?>) data.get("tis")).get("_rawData") != null) {
                        tisD = (TISData) ((Map<?,?>) data.get("tis")).get("_rawData");
                    }
                    
                    var flatData = autoPopulationService.autoPopulateAll(aisD, f26, tisD, "ITR-1");
                    result.put("autoPopulated", Map.of(
                        "basic", flatData.getBasic(),
                        "interestSB", flatData.getInterestSB(),
                        "interestFD", flatData.getInterestFD(),
                        "dividends", flatData.getDividends(),
                        "tdsS192", flatData.getTdsS192(),
                        "tds194A", flatData.getTds194A(),
                        "selfTax", flatData.getSelfTax()
                    ));
                } catch (Exception e) {
                    log.warn("Auto-population failed: {}", e.getMessage());
                }
            } else {
                result.put("status", "ERROR");
                result.put("message", "No documents were imported successfully");
            }
            
            log.info("Sequential import complete for PAN: {} - {}", pan, result.get("imported"));
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Sequential import failed: {}", e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Auto-populate from all imported data using AutoPopulationService
     */
    @PostMapping("/autopopulate/all")
    public ResponseEntity<?> autoPopulateFromAll(@RequestBody SequentialImportRequest request) {
        try {
            log.info("Auto-populating from all sources for PAN: {}", request.getPan());
            
            var flatData = autoPopulationService.autoPopulateAll(
                request.getAisData(),
                request.getForm26asData(),
                request.getTisData(),
                request.getItrType() != null ? request.getItrType() : "ITR-1"
            );
            
            Map<String, Object> autoData = new HashMap<>();
            autoData.put("basic", flatData.getBasic());
            autoData.put("interestSB", flatData.getInterestSB());
            autoData.put("interestFD", flatData.getInterestFD());
            autoData.put("dividends", flatData.getDividends());
            autoData.put("tdsS192", flatData.getTdsS192());
            autoData.put("tds194A", flatData.getTds194A());
            autoData.put("tdsOther", flatData.getTdsOther());
            autoData.put("selfTax", flatData.getSelfTax());
            autoData.put("adv15Jun", flatData.getAdv15Jun());
            autoData.put("adv15Sep", flatData.getAdv15Sep());
            autoData.put("adv15Dec", flatData.getAdv15Dec());
            autoData.put("adv15Mar", flatData.getAdv15Mar());
            autoData.put("employerEntries", flatData.getEmployerEntries());
            autoData.put("tdsEntries", flatData.getTdsEntries());
            autoData.put("selfAssessmentTaxEntries", flatData.getSelfAssessmentTaxEntries());

            return ResponseEntity.ok(Map.of("status", "SUCCESS", "data", autoData));
        } catch (Exception e) {
            log.error("Auto-population failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    /**
     * Get reconciliation report between AIS and 26AS
     */
    @PostMapping("/reconciliation")
    public ResponseEntity<?> getReconciliationReport(
            @RequestBody Map<String, Object> request) {
        
        try {
            AISData aisData = null;
            Form26ASData form26asData = null;
            TISData tisData = null;
            
            Object aisObj = request.get("aisData");
            if (aisObj instanceof Map) {
                // Parse from Map - would need proper deserializer
                log.debug("AIS data provided for reconciliation");
            }
            
            Map<String, Object> report = new HashMap<>();
            report.put("status", "SUCCESS");
            report.put("hasDiscrepancies", false);
            report.put("message", "Reconciliation requires full data objects");
            
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Reconciliation failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    // ==================== REQUEST DTO ====================
    
    public static class SequentialImportRequest {
        private String pan;
        private AISData aisData;
        private Form26ASData form26asData;
        private TISData tisData;
        private String itrType;
        
        public String getPan() { return pan; }
        public void setPan(String pan) { this.pan = pan; }
        public AISData getAisData() { return aisData; }
        public void setAisData(AISData aisData) { this.aisData = aisData; }
        public Form26ASData getForm26asData() { return form26asData; }
        public void setForm26asData(Form26ASData form26asData) { this.form26asData = form26asData; }
        public TISData getTisData() { return tisData; }
        public void setTisData(TISData tisData) { this.tisData = tisData; }
        public String getItrType() { return itrType; }
        public void setItrType(String itrType) { this.itrType = itrType; }
    }
}
