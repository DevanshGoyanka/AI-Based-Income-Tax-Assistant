package com.itr.interfaces.rest.prefill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr.dto.ITDPrefillData;
import com.itr.dto.Form26ASData;
import com.itr.dto.FlatFormData;
import com.itr.service.integration.ITDPrefillImportService;
import com.itr.service.integration.Form26ASImportService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PrefillController — /api/v1/prefill/*
 * Uses existing ITDPrefillImportService with proper DTO-based auto-population
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/prefill")
public class PrefillController {

    private final ITDPrefillImportService prefillService;
    private final Form26ASImportService form26asService;
    private final ObjectMapper objectMapper;

    public PrefillController(ITDPrefillImportService prefillService, 
                            Form26ASImportService form26asService,
                            ObjectMapper objectMapper) {
        this.prefillService = prefillService;
        this.form26asService = form26asService;
        this.objectMapper = objectMapper;
    }

    /**
     * Import ITD Prefill JSON file
     */
    @PostMapping("/import")
    public ResponseEntity<?> importPrefill(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Importing ITD Prefill JSON: {}", file.getOriginalFilename());
            ITDPrefillData data = prefillService.importPrefillData(file);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Failed to import prefill: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Auto-populate from ITD Prefill JSON
     * Uses EXISTING ITDPrefillImportService.autoPopulateFromPrefillFlat() method
     */
    @PostMapping("/autopopulate")
    public ResponseEntity<?> autoPopulateFromPrefill(@RequestBody AutoPopulatePrefillRequest request) {
        try {
            log.info("Auto-populating from ITD Prefill using DTO-based service");
            
            // Get prefill data
            ITDPrefillData prefillData = null;
            
            if (request.getPrefillData() != null) {
                prefillData = objectMapper.convertValue(request.getPrefillData(), ITDPrefillData.class);
            } else if (request.getJsonContent() != null) {
                prefillData = objectMapper.readValue(request.getJsonContent(), ITDPrefillData.class);
            } else if (request.getFile() != null) {
                prefillData = prefillService.importPrefillData(request.getFile());
            }
            
            if (prefillData == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No prefill data provided"));
            }
            
            log.info("Prefill data - PersonalInfo: {}, Form24Q: {}, Form26AS: {}", 
                prefillData.getPersonalInfo() != null,
                prefillData.getForm24q() != null,
                prefillData.getForm26as() != null);
            
            // Create FlatFormData from request
            FlatFormData flatFormData = buildFlatFormData(request.getFormData());
            
            // Use the EXISTING DTO-based service method
            FlatFormData result = prefillService.autoPopulateFromPrefillFlat(flatFormData, prefillData);
            
            log.info("Auto-population complete via ITDPrefillImportService");
            log.info("Result - Name: {}, PAN: {}, Basic: {}, Employers: {}", 
                result.getName(), result.getPan(), result.getBasic(), 
                result.getEmployerEntries() != null ? result.getEmployerEntries().size() : 0);
            
            // Convert to Map for frontend
            return ResponseEntity.ok(flatFormDataToMap(result));
            
        } catch (Exception e) {
            log.error("Failed to auto-populate from prefill: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/26as/upload")
    public ResponseEntity<?> upload26AS(@RequestParam("file") MultipartFile file, 
                                        @RequestParam("pan") String pan,
                                        @RequestParam("dob") String dob) {
        try {
            log.info("Importing Form 26AS for PAN: {}", pan);
            Form26ASData data = form26asService.import26AS(file.getBytes(), pan, java.time.LocalDate.parse(dob));
            
            // If no entries found, try to extract text directly and use fallback parser
            boolean hasAnyEntry = 
                (data.getTdsOnInterest() != null && !data.getTdsOnInterest().isEmpty()) ||
                (data.getTdsOnContractor() != null && !data.getTdsOnContractor().isEmpty()) ||
                (data.getTdsOnProfessional() != null && !data.getTdsOnProfessional().isEmpty()) ||
                (data.getTdsOnOther() != null && !data.getTdsOnOther().isEmpty()) ||
                (data.getTdsOnRent() != null && !data.getTdsOnRent().isEmpty()) ||
                (data.getTdsOnCommission() != null && !data.getTdsOnCommission().isEmpty()) ||
                (data.getTdsOnInsurance() != null && !data.getTdsOnInsurance().isEmpty()) ||
                (data.getTdsOnProperty() != null && !data.getTdsOnProperty().isEmpty());
            
            if (!hasAnyEntry) {
                log.warn("No TDS entries extracted - PDF may have different format");
            }
            
            // Return summary for frontend
            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("assesseName", data.getAssesseName());
            result.put("assessePAN", data.getAssessePAN());
            result.put("totalTDSInterest", data.getTotalTDSInterest() != null ? data.getTotalTDSInterest() : 0);
            result.put("totalTDSOther", data.getTotalTDSOther() != null ? data.getTotalTDSOther() : 0);
            result.put("totalTaxPaid", data.getTotalTaxPaid() != null ? data.getTotalTaxPaid() : 0);
            result.put("taxPaymentCount", data.getTaxPayments() != null ? data.getTaxPayments().size() : 0);
            result.put("tdsInterestCount", data.getTdsOnInterest() != null ? data.getTdsOnInterest().size() : 0);
            result.put("tdsContractorCount", data.getTdsOnContractor() != null ? data.getTdsOnContractor().size() : 0);
            result.put("tdsProfessionalCount", data.getTdsOnProfessional() != null ? data.getTdsOnProfessional().size() : 0);
            result.put("tdsOtherCount", data.getTdsOnOther() != null ? data.getTdsOnOther().size() : 0);
            result.put("tdsRentCount", data.getTdsOnRent() != null ? data.getTdsOnRent().size() : 0);
            result.put("tdsCommissionCount", data.getTdsOnCommission() != null ? data.getTdsOnCommission().size() : 0);
            
            // Store raw data for auto-population
            result.put("_rawData", data);
            
            log.info("26AS Import result: {} interest, {} contractor, {} professional, {} other entries", 
                result.get("tdsInterestCount"), result.get("tdsContractorCount"), 
                result.get("tdsProfessionalCount"), result.get("tdsOtherCount"));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to import Form 26AS: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
    
    /**
     * Auto-populate from all sources (26AS, AIS, TIS combined)
     * Maps TDS entries, Tax Payments, Interest income from 26AS to frontend fields
     */
    @PostMapping("/autoPopulateAll")
    public ResponseEntity<?> autoPopulateFromAll(@RequestBody Map<String, Object> request) {
        try {
            log.info("Auto-populating from all sources");
            
            Map<String, Object> result = new HashMap<>();
            boolean hasData = false;
            
            // ====== PRIMARY SOURCE: ITD PREFILL JSON ======
            // If user uploads the ITD Prefill JSON (which has all data), use that directly
            Object prefillObj = request.get("prefillData");
            if (prefillObj instanceof Map) {
                log.info("Using ITD Prefill JSON data for auto-population");
                @SuppressWarnings("unchecked")
                Map<String, Object> prefill = (Map<String, Object>) prefillObj;
                
                // Extract from form26as in prefill JSON
                Object form26asObj = prefill.get("form26as");
                if (form26asObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> form26 = (Map<String, Object>) form26asObj;
                    Map<String, Object> form26Data = extractFromPrefill26AS(form26);
                    result.putAll(form26Data);
                    hasData = true;
                }
                
                // Extract from insights
                Object insightsObj = prefill.get("insights");
                if (insightsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> insights = (Map<String, Object>) insightsObj;
                    Map<String, Object> insightsData = extractFromInsights(insights);
                    result.putAll(insightsData);
                    hasData = true;
                }
                
                // Extract from Form24Q (salary)
                Object form24qObj = prefill.get("form24q");
                if (form24qObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> form24q = (Map<String, Object>) form24qObj;
                    Map<String, Object> salaryData = extractFromForm24Q(form24q);
                    result.putAll(salaryData);
                    hasData = true;
                }
                
                // Extract bank account details
                Object bankObj = prefill.get("bankAccountDtls");
                if (bankObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> banks = (List<Map<String, Object>>) bankObj;
                    List<Map<String, Object>> bankEntries = extractBankDetails(banks);
                    result.put("bankAccountDetails", bankEntries);
                }
                
                if (hasData) {
                    log.info("Auto-populated from ITD Prefill JSON successfully");
                    result.put("status", "SUCCESS");
                    result.put("autoPopulated", true);
                    result.put("source", "ITD_PREFILL_JSON");
                    return ResponseEntity.ok(result);
                }
            }
            
            // ====== FALLBACK: Process 26AS PDF data ======
            // Extract 26AS data from PDF import result
            Object f26asRaw = request.get("form26ASData");
            if (f26asRaw == null) {
                f26asRaw = request.get("data26AS");
            }
            if (f26asRaw == null) {
                f26asRaw = request.get("26as");
            }
            
            if (f26asRaw != null && f26asRaw instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> f26as = (Map<String, Object>) f26asRaw;
                
                // IMPORTANT: The response from upload26AS nests TDS data inside "_rawData"
                // Unwrap it if present
                Object rawDataObj = f26as.get("_rawData");
                if (rawDataObj instanceof Map) {
                    log.info("Unwrapping _rawData from upload26AS response");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rawData = (Map<String, Object>) rawDataObj;
                    f26as = rawData;
                }
                
                // Check ALL TDS categories
                double totalTDS = 0;
                double totalTDS194A = 0;
                List<Map<String, Object>> tdsEntries = new ArrayList<>();
                
                // Check each category
                String[] tdsCategories = {"tdsOnInterest", "tdsOnContractor", "tdsOnProfessional", 
                                         "tdsOnCommission", "tdsOnRent", "tdsOnOther"};
                
                for (String category : tdsCategories) {
                    Object tdsList = f26as.get(category);
                    if (tdsList instanceof List && !((List<?>) tdsList).isEmpty()) {
                        hasData = true;
                        for (Object tdsObj : (List<?>) tdsList) {
                            if (tdsObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> tds = (Map<String, Object>) tdsObj;
                                log.debug("Raw TDS entry from {}: section={}, taxDeducted={}, amountPaid={}", 
                                    category, tds.get("section"), tds.get("taxDeducted"), tds.get("amountPaid"));
                                Map<String, Object> entry = extractTDSEntry(tds);
                                tdsEntries.add(entry);
                                totalTDS += getDouble(entry.get("taxDeducted"));
                                
                                Object section = tds.get("section");
                                if ("194A".equals(section) || "194".equals(section)) {
                                    totalTDS194A += getDouble(entry.get("taxDeducted"));
                                }
                            }
                        }
                        log.info("26AS PDF {} entries: {}", category, ((List<?>) tdsList).size());
                    }
                }
                
                if (!tdsEntries.isEmpty()) {
                    result.put("tdsEntries", tdsEntries);
                    result.put("tds194A", totalTDS194A);
                    result.put("tdsTotal", totalTDS);
                    log.info("Auto-populated {} TDS entries from 26AS PDF. Sections extracted: {}", 
                        tdsEntries.size(), tdsEntries.stream().map(e -> e.get("section")).collect(java.util.stream.Collectors.toList()));
                }
                
                Object totalTaxPaid = f26as.get("totalTaxPaid");
                if (totalTaxPaid instanceof Number) {
                    hasData = true;
                    result.put("totalTaxPaid", ((Number) totalTaxPaid).doubleValue());
                    result.put("selfTax", ((Number) totalTaxPaid).doubleValue());
                    log.info("26AS PDF Tax Paid: {}", totalTaxPaid);
                }
            }
            
            // ====== PROCESS AIS PDF data ======
            Object aisRaw = request.get("aisData");
            if (aisRaw != null && aisRaw instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ais = (Map<String, Object>) aisRaw;
                
                // Unwrap _rawData if present (from IntegrationController response)
                Object rawDataObj = ais.get("_rawData");
                if (rawDataObj instanceof Map) {
                    log.info("Unwrapping _rawData from AIS import response");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rawData = (Map<String, Object>) rawDataObj;
                    ais = rawData;
                }
                
                // Extract TDS entries from Part B1
                Object partB1Obj = ais.get("partB1");
                if (partB1Obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> partB1 = (Map<String, Object>) partB1Obj;

                    // Collect TSNs from tdsTransactions keyed by deductor TAN
                    Map<String, Map<String, Object>> firstActiveTxn = new java.util.HashMap<>();
                    Object tdsTxnObj = partB1.get("tdsTransactions");
                    if (tdsTxnObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> tdsTxns = (List<Map<String, Object>>) tdsTxnObj;
                        for (Map<String, Object> txn : tdsTxns) {
                            String tan = (String) txn.get("deductorTAN");
                            String status = (String) txn.get("status");
                            if (tan != null && "Active".equals(status) && !firstActiveTxn.containsKey(tan)) {
                                firstActiveTxn.put(tan, txn);
                            }
                        }
                    }

                    Object tdsEntriesObj = partB1.get("tdsEntries");
                    if (tdsEntriesObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> tdsList = (List<Map<String, Object>>) tdsEntriesObj;
                        if (!tdsList.isEmpty()) {
                            hasData = true;
                            List<Map<String, Object>> mapTdsEntries = new ArrayList<>();
                            double totalTDS192 = 0, totalTDS194A = 0, totalTDSSalary = 0;

                            for (Map<String, Object> tds : tdsList) {
                                String section = (String) tds.get("section");
                                if (section == null) section = "OTHER";

                                double amt = getDouble(tds.get("totalAmountPaid"));
                                double tdsDed = getDouble(tds.get("totalTDSDeducted"));
                                String deductorTAN = (String) tds.get("deductorTAN");

                                // Get TSN from first active transaction for this deductor
                                String tsn = "";
                                Map<String, Object> firstTxn = firstActiveTxn.get(deductorTAN);
                                if (firstTxn != null) {
                                    tsn = (String) firstTxn.get("tsnId");
                                }

                                Map<String, Object> entry = new HashMap<>();
                                entry.put("section", mapSectionToFrontend(section));
                                entry.put("deductorName", tds.get("deductorName"));
                                entry.put("deductorTAN", deductorTAN);
                                entry.put("incomeAmount", amt);
                                entry.put("tdsDeducted", tdsDed);
                                entry.put("taxDeducted", tdsDed);
                                entry.put("uniqueTransactionNo", tsn);
                                entry.put("verified26AS", false);
                                entry.put("claimedInReturn", true);
                                mapTdsEntries.add(entry);
                                
                                if ("192".equals(section)) { totalTDS192 += tdsDed; totalTDSSalary += amt; }
                                else if ("194A".equals(section)) totalTDS194A += tdsDed;
                            }
                            
                            result.put("tdsEntries", mapTdsEntries);
                            result.put("tdsS192", totalTDS192);
                            result.put("tds194A", totalTDS194A);
                            result.put("tdsTotal", totalTDS192 + totalTDS194A);
                            
                            // If Section 192 salary exists, create employer entry
                            if (totalTDSSalary > 0) {
                                for (Map<String, Object> tds : tdsList) {
                                    if ("192".equals(tds.get("section"))) {
                                        List<Map<String, Object>> empEntries = new ArrayList<>();
                                        Map<String, Object> emp = new HashMap<>();
                                        emp.put("employerName", tds.get("deductorName"));
                                        emp.put("employerTAN", tds.get("deductorTAN"));
                                        emp.put("basic", totalTDSSalary);
                                        emp.put("grossSalary", totalTDSSalary);
                                        emp.put("netSalary", totalTDSSalary - 75000);
                                        emp.put("tdsDeducted", totalTDS192);
                                        empEntries.add(emp);
                                        result.put("employerEntries", empEntries);
                                        result.put("basic", totalTDSSalary);
                                        break;
                                    }
                                }
                            }
                            
                            log.info("AIS TDS entries extracted: {} entries (192: {}, 194A: {})", mapTdsEntries.size(), totalTDS192, totalTDS194A);
                        }
                    }
                }
                
                // Extract SFT from Part B2
                Object partB2Obj = ais.get("partB2");
                if (partB2Obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> partB2 = (Map<String, Object>) partB2Obj;
                    
                    // Dividend income
                    Object divObj = partB2.get("dividendIncome");
                    if (divObj instanceof Number && ((Number) divObj).doubleValue() > 0) {
                        double div = ((Number) divObj).doubleValue();
                        result.put("dividends", div);
                        result.put("dividendShares", div);
                        hasData = true;
                        log.info("AIS Dividend: {}", div);
                    }
                    
                    // Securities sale (capital gains) - transform to frontend CapitalGainTransaction format
                    Object saleObj = partB2.get("securitiesSale");
                    if (saleObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> aisSales = (List<Map<String, Object>>) saleObj;
                        if (!aisSales.isEmpty()) {
                            hasData = true;
                            List<Map<String, Object>> cgTransactions = new ArrayList<>();
                            double totalStcg = 0, totalLtcg = 0;
                            
                            for (Map<String, Object> sale : aisSales) {
                                Map<String, Object> txn = new HashMap<>();

                                String aisAssetType = (String) sale.get("assetType");
                                String securityName = (String) sale.get("securityName");

                                // Use AIS assetType directly (derived from security name + AMC)
                                if ("MUTUAL_FUND".equals(aisAssetType)) {
                                    txn.put("assetType", "MUTUAL_FUND");
                                    txn.put("assetDescription", securityName);
                                } else if ("PROPERTY".equals(aisAssetType)) {
                                    txn.put("assetType", "PROPERTY");
                                    txn.put("assetDescription", securityName != null ? securityName : "Land/Building Sale");
                                } else {
                                    txn.put("assetType", "EQUITY");
                                    txn.put("assetDescription", securityName != null ? securityName : "Listed Equity Share");
                                }

                                // Sale details
                                String saleDateStr = (String) sale.get("transferDate");
                                txn.put("saleDate", saleDateStr);

                                double saleCost = getDouble(sale.get("salesConsideration"));
                                double purchaseCost = getDouble(sale.get("costOfAcquisition"));

                                txn.put("saleCost", saleCost);
                                txn.put("purchaseCost", purchaseCost);
                                txn.put("expenses", 0);

                                // Purchase date: AIS doesn't provide it, so use sale date - 12 months as fallback
                                // This enables the calculation to run. User should update it.
                                if (saleDateStr != null && saleDateStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
                                    try {
                                        String[] parts = saleDateStr.split("/");
                                        int day = Integer.parseInt(parts[0]);
                                        int month = Integer.parseInt(parts[1]);
                                        int year = Integer.parseInt(parts[2]);
                                        java.time.LocalDate saleDate = java.time.LocalDate.of(year, month, day);
                                        java.time.LocalDate fallbackPurchase = saleDate.minusMonths(12);
                                        String purchaseDate = String.format("%02d/%02d/%04d",
                                            fallbackPurchase.getDayOfMonth(),
                                            fallbackPurchase.getMonthValue(),
                                            fallbackPurchase.getYear());
                                        txn.put("purchaseDate", purchaseDate);
                                    } catch (Exception e) {
                                        txn.put("purchaseDate", null);
                                    }
                                } else {
                                    txn.put("purchaseDate", null);
                                }

                                double gain = Math.max(0, saleCost - purchaseCost);
                                txn.put("gain", gain);
                                txn.put("taxableGain", gain);

                                if ("PROPERTY".equals(aisAssetType)) {
                                    // Property: use LTCG_112 (Section 54/112). User must fill purchase cost manually.
                                    txn.put("gainType", "LTCG_112");
                                    txn.put("longTerm", true);
                                    txn.put("holdingPeriodMonths", 24);
                                    txn.put("taxRate", 0.20);
                                    totalLtcg += gain;
                                } else if ("MUTUAL_FUND".equals(aisAssetType)) {
                                    // MF: LTCG_112A (Section 112A, 12.5%)
                                    txn.put("gainType", "LTCG_112A");
                                    txn.put("longTerm", true);
                                    txn.put("holdingPeriodMonths", 24);
                                    txn.put("taxRate", 0.125);
                                    totalLtcg += gain;
                                } else {
                                    // Equity listed: STCG_111A (Section 111A, 20%)
                                    txn.put("gainType", "STCG_111A");
                                    txn.put("longTerm", false);
                                    txn.put("holdingPeriodMonths", 3);
                                    txn.put("taxRate", 0.20);
                                    totalStcg += gain;
                                }

                                cgTransactions.add(txn);
                                log.info("AIS CG Txn: {} - {} Gain:{} Type:{}",
                                    txn.get("assetDescription"), txn.get("assetType"), gain, txn.get("gainType"));
                            }
                            
                            result.put("capitalGainTransactions", cgTransactions);
                            result.put("stcgEquityPost", totalStcg);
                            result.put("ltcg112APost", totalLtcg);
                            log.info("AIS Capital Gains: STCG={}, LTCG={}", totalStcg, totalLtcg);
                        }
                    }
                    
                    // MF Purchase info (log only)
                    Object mfObj = partB2.get("mutualFundPurchase");
                    if (mfObj instanceof List && !((List<?>) mfObj).isEmpty()) {
                        log.info("AIS MF Purchases: {}", ((List<?>) mfObj).size());
                    }

                    // === BANK INTEREST ENTRIES from AIS Part B2 SFT ===
                    // Read from savingsInterest and depositInterest (not from partB1 TDS)
                    List<Map<String, Object>> bankEntries = new ArrayList<>();
                    List<Map<String, Object>> bankAccountDetails = new ArrayList<>();

                    Object savingsObj = partB2.get("savingsInterest");
                    if (savingsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> savingsList = (List<Map<String, Object>>) savingsObj;
                        for (Map<String, Object> entry : savingsList) {
                            String bankName = (String) entry.get("sourceName");
                            long interest = ((Number) entry.get("totalAmount")).longValue();
                            String accountNo = (String) entry.get("accountNumber");
                            if (interest > 0 && bankName != null) {
                                Map<String, Object> bie = new HashMap<>();
                                bie.put("bankName", bankName);
                                bie.put("accountType", "SAVINGS");
                                bie.put("section", "194A");
                                bie.put("interestEarned", (double) interest);
                                bie.put("tdsDeducted", 0.0);
                                if (accountNo != null && !accountNo.isEmpty()) bie.put("accountNumber", accountNo);
                                bankEntries.add(bie);

                                Map<String, Object> ac = new HashMap<>();
                                ac.put("bankName", bankName);
                                ac.put("accountType", "SAVINGS");
                                ac.put("accountNumber", accountNo != null ? accountNo : "");
                                ac.put("ifscCode", "");
                                ac.put("interestEarned", (double) interest);
                                bankAccountDetails.add(ac);

                                double existingSB = getDouble(result.get("interestSB"));
                                result.put("interestSB", existingSB + (double) interest);
                                log.info("BankInterestEntry (SB): {} = {} | account: {}", bankName, interest, accountNo);
                            }
                        }
                    }

                    Object depObj = partB2.get("depositInterest");
                    if (depObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> depList = (List<Map<String, Object>>) depObj;
                        for (Map<String, Object> entry : depList) {
                            String bankName = (String) entry.get("sourceName");
                            long interest = ((Number) entry.get("totalAmount")).longValue();
                            String accountNo = (String) entry.get("accountNumber");
                            if (interest > 0 && bankName != null) {
                                Map<String, Object> bie = new HashMap<>();
                                bie.put("bankName", bankName);
                                bie.put("accountType", "FD");
                                bie.put("section", "194A");
                                bie.put("interestEarned", (double) interest);
                                bie.put("tdsDeducted", 0.0);
                                if (accountNo != null && !accountNo.isEmpty()) bie.put("accountNumber", accountNo);
                                bankEntries.add(bie);

                                Map<String, Object> ac = new HashMap<>();
                                ac.put("bankName", bankName);
                                ac.put("accountType", "FD");
                                ac.put("accountNumber", accountNo != null ? accountNo : "");
                                ac.put("ifscCode", "");
                                ac.put("interestEarned", (double) interest);
                                bankAccountDetails.add(ac);

                                double existingFD = getDouble(result.get("interestFD"));
                                result.put("interestFD", existingFD + (double) interest);
                                log.info("BankInterestEntry (FD): {} = {} | account: {}", bankName, interest, accountNo);
                            }
                        }
                    }

                    if (!bankEntries.isEmpty()) {
                        result.put("bankInterestEntries", bankEntries);
                        result.put("bankAccountDetails", bankAccountDetails);
                        log.info("AIS bankInterestEntries: {} entries | bankAccountDetails: {} entries",
                                bankEntries.size(), bankAccountDetails.size());
                    }

                    // === DIVIDEND PARTICULARS ===
                    Object divEntriesObj = partB2.get("dividendEntries");
                    if (divEntriesObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> divEntries = (List<Map<String, Object>>) divEntriesObj;
                        if (!divEntries.isEmpty()) {
                            // Use first dividend entry for company name / TAN
                            Map<String, Object> first = divEntries.get(0);
                            String divCompany = (String) first.get("securityName");
                            double totalDiv = 0;
                            for (Map<String, Object> d : divEntries) {
                                totalDiv += getDouble(d.get("salesConsideration"));
                            }
                            result.put("dividendCompanyName", divCompany);
                            result.put("dividendCompanyTAN", "");
                            result.put("dividends", totalDiv);
                            log.info("AIS Dividend: company={} total={}", divCompany, totalDiv);
                        }
                    }
                    }

                // Extract tax payments from Part B3 - map to both Advance Tax and Self Assessment Tax
                Object partB3Obj = ais.get("partB3");
                if (partB3Obj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> taxPayments = (List<Map<String, Object>>) partB3Obj;
                    if (!taxPayments.isEmpty()) {
                        hasData = true;
                        
                        // Advance Tax: quarterly buckets
                        double advJun = 0, advSep = 0, advDec = 0, advMar = 0;
                        // Self Assessment Tax: detail entries
                        List<Map<String, Object>> satEntries = new ArrayList<>();
                        // Total for Tax Computation tab
                        double totalTaxPaid = 0;
                        
                        for (Map<String, Object> tp : taxPayments) {
                            double amtVal = getDouble(tp.get("totalAmount"));
                            if (amtVal <= 0) continue;
                            
                            String minorHead = (String) tp.get("minorHead");
                            String depositDate = (String) tp.get("depositDate");
                            
                            // Map to quarterly buckets based on deposit date
                            if (depositDate != null && depositDate.matches("\\d{2}/\\d{2}/\\d{4}")) {
                                String[] parts = depositDate.split("/");
                                int month = Integer.parseInt(parts[1]);
                                int year = Integer.parseInt(parts[2]);
                                int quarter = (month - 1) / 3 + 1; // 1=Jan-Mar, 2=Apr-Jun, 3=Jul-Sep, 4=Oct-Dec
                                
                                // Map to advance tax quarter: Q1=Mar, Q2=Jun, Q3=Sep, Q4=Dec
                                if ("Advance Tax".equalsIgnoreCase(minorHead) || 
                                    ("Income Tax".equalsIgnoreCase(minorHead) && quarter <= 4)) {
                                    if (month <= 6) {
                                        // If it's early in the year, could be June installment
                                        if (month <= 3) advMar += amtVal;
                                        else advJun += amtVal;
                                    } else if (month <= 9) {
                                        advSep += amtVal;
                                    } else {
                                        advDec += amtVal;
                                    }
                                    totalTaxPaid += amtVal;
                                }
                            }
                            
                            // Self Assessment Tax: separate entries
                            if (minorHead != null && minorHead.toLowerCase().contains("self assessment")) {
                                Map<String, Object> satEntry = new HashMap<>();
                                satEntry.put("bsrCode", tp.get("bsrCode"));
                                satEntry.put("challanNo", tp.get("challanSerialNo"));
                                satEntry.put("depositDate", depositDate);
                                satEntry.put("amount", amtVal);
                                satEntry.put("minorHead", minorHead);
                                satEntries.add(satEntry);
                                totalTaxPaid += amtVal;
                            }
                        }
                        
                        // Only set advance tax if there are amounts
                        if (advJun > 0 || advSep > 0 || advDec > 0 || advMar > 0) {
                            result.put("adv15Jun", advJun);
                            result.put("adv15Sep", advSep);
                            result.put("adv15Dec", advDec);
                            result.put("adv15Mar", advMar);
                            
                            List<Map<String, Object>> advEntries = new ArrayList<>();
                            if (advJun > 0) { Map<String, Object> e = new HashMap<>(); e.put("date", "15-Jun"); e.put("amount", advJun); advEntries.add(e); }
                            if (advSep > 0) { Map<String, Object> e = new HashMap<>(); e.put("date", "15-Sep"); e.put("amount", advSep); advEntries.add(e); }
                            if (advDec > 0) { Map<String, Object> e = new HashMap<>(); e.put("date", "15-Dec"); e.put("amount", advDec); advEntries.add(e); }
                            if (advMar > 0) { Map<String, Object> e = new HashMap<>(); e.put("date", "15-Mar"); e.put("amount", advMar); advEntries.add(e); }
                            result.put("advanceTaxEntries", advEntries);
                            log.info("AIS Advance Tax: Jun={}, Sep={}, Dec={}, Mar={}", advJun, advSep, advDec, advMar);
                        }
                        
                        if (!satEntries.isEmpty()) {
                            result.put("selfAssessmentTaxEntries", satEntries);
                            result.put("selfTax", satEntries.stream().mapToDouble(e -> getDouble(e.get("amount"))).sum());
                            log.info("AIS Self-Assessment Tax: {} entries", satEntries.size());
                        }
                        
                        if (totalTaxPaid > 0) {
                            result.put("totalTaxPaid", totalTaxPaid);
                        }
                        
                        log.info("AIS Tax Payments: totalTaxPaid={}", totalTaxPaid);
                    }
                }
                
                if (hasData) log.info("AIS data extracted and mapped successfully");
            }
            
            result.put("status", "SUCCESS");
            result.put("autoPopulated", hasData);
            String source = hasData ? "26AS_PDF" : "NO_DATA";
            if (request.get("aisData") != null) source = "AIS_PDF_AND_26AS";
            if (request.get("prefillData") != null) source = "ITD_PREFILL_JSON";
            result.put("source", source);
            
            if (!hasData) {
                result.put("warning", "No data found. Please ensure the PDF contains extractable data.");
                log.warn("No data extracted from any source");
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to auto-populate from all sources: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
    
    /**
     * Extract data from ITD Prefill JSON's form26as section
     */
    private Map<String, Object> extractFromPrefill26AS(Map<String, Object> form26as) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> tdsEntries = new ArrayList<>();
        
        // Parse TDS on Other Than Salary
        Object tdsOtherObj = form26as.get("tdsOnOthThanSals");
        if (tdsOtherObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> tdsOther = (Map<String, Object>) tdsOtherObj;
            Object tdsList = tdsOther.get("tdSonOthThanSal");
            if (tdsList instanceof List) {
                for (Object tdsItem : (List<?>) tdsList) {
                    if (tdsItem instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> tds = (Map<String, Object>) tdsItem;
                        
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("section", tds.get("sectionCode"));
                        
                        Object empObj = tds.get("employerOrDeductorOrCollectDetl");
                        if (empObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> emp = (Map<String, Object>) empObj;
                            entry.put("deductorName", emp.get("employerOrDeductorOrCollecterName"));
                            entry.put("deductorTAN", emp.get("tan"));
                        }
                        
                        Object grossObj = tds.get("grossAmount");
                        entry.put("incomeAmount", grossObj instanceof Number ? ((Number) grossObj).doubleValue() : 0);
                        
                        Object taxDtls = tds.get("taxDeductCreditDtls");
                        if (taxDtls instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> tax = (Map<String, Object>) taxDtls;
                            Object taxDed = tax.get("taxDeductedOwnHands");
                            entry.put("taxDeducted", taxDed instanceof Number ? ((Number) taxDed).doubleValue() : 0);
                        }
                        
                        entry.put("verified26AS", true);
                        entry.put("claimedInReturn", true);
                        tdsEntries.add(entry);
                    }
                }
                log.info("Prefill TDS entries extracted: {}", tdsEntries.size());
            }
        }
        
        // Tax payments from form26as
        Object taxPayObj = form26as.get("taxPayments");
        List<Map<String, Object>> taxEntries = new ArrayList<>();
        double totalTaxPaid = 0;
        if (taxPayObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> taxPay = (Map<String, Object>) taxPayObj;
            Object taxList = taxPay.get("taxPayment");
            if (taxList instanceof List) {
                for (Object tp : (List<?>) taxList) {
                    if (tp instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> t = (Map<String, Object>) tp;
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("bsrCode", t.get("bsrCode"));
                        entry.put("challanNo", t.get("srlNoOfChaln"));
                        entry.put("depositDate", t.get("dateDep"));
                        Object amt = t.get("amt");
                        entry.put("amount", amt instanceof Number ? ((Number) amt).doubleValue() : 0);
                        taxEntries.add(entry);
                        if (amt instanceof Number) totalTaxPaid += ((Number) amt).doubleValue();
                    }
                }
            }
        }
        
        result.put("tdsEntries", tdsEntries);
        result.put("selfAssessmentTaxEntries", taxEntries);
        result.put("totalTaxPaid", totalTaxPaid);
        result.put("selfTax", totalTaxPaid);
        
        // Parse other income sources
        Object otherIncObj = form26as.get("incomeDeductionsOthersInc");
        if (otherIncObj instanceof List) {
            for (Object oi : (List<?>) otherIncObj) {
                if (oi instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> oiMap = (Map<String, Object>) oi;
                    Object amount = oiMap.get("othSrcOthAmount");
                    Object desc = oiMap.get("othSrcNatureDesc");
                    
                    if (desc != null) {
                        String nature = desc.toString().toUpperCase();
                        if (nature.contains("IFD") || nature.contains("TERM")) {
                            result.put("interestFD", amount instanceof Number ? ((Number) amount).doubleValue() : 0);
                        } else if (nature.contains("SAV") || nature.contains("INT")) {
                            result.put("interestSB", amount instanceof Number ? ((Number) amount).doubleValue() : 0);
                        } else if (nature.contains("DIV")) {
                            result.put("dividends", amount instanceof Number ? ((Number) amount).doubleValue() : 0);
                        }
                    }
                }
            }
        }
        
        log.info("Extracted from prefill form26as: TDS={}, Tax={}", tdsEntries.size(), totalTaxPaid);
        return result;
    }
    
    /**
     * Extract data from insights (most recent/reliable)
     */
    private Map<String, Object> extractFromInsights(Map<String, Object> insights) {
        Map<String, Object> result = new HashMap<>();
        
        // Salary interest
        Object savingInt = insights.get("intrstFrmSavingBank");
        if (savingInt != null) {
            result.put("interestSB", savingInt instanceof Number ? ((Number) savingInt).doubleValue() : 0);
        }
        
        // Term deposit interest  
        Object termInt = insights.get("intrstFrmTermDeposit");
        if (termInt != null) {
            result.put("interestFD", termInt instanceof Number ? ((Number) termInt).doubleValue() : 0);
        }
        
        // Deductions from insights
        Object dedObj = insights.get("UsrDeductUndChapVIAType");
        if (dedObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> deductions = (Map<String, Object>) dedObj;
            
            Object s80c = deductions.get("section80C");
            if (s80c instanceof Number) result.put("s80C_total", ((Number) s80c).doubleValue());
            
            Object s80ccd1b = deductions.get("section80CCD1B");
            if (s80ccd1b instanceof Number) result.put("s80CCD1B", ((Number) s80ccd1b).doubleValue());
            
            Object s80d = deductions.get("section80D");
            if (s80d instanceof Number) result.put("s80D_self", ((Number) s80d).doubleValue());
            
            Object s80ttb = deductions.get("Section80TTB");
            if (s80ttb instanceof Number) result.put("s80TTB", ((Number) s80ttb).doubleValue());
        }
        
        log.info("Extracted from insights: InterestSB={}, InterestFD={}", 
            result.get("interestSB"), result.get("interestFD"));
        
        return result;
    }
    
    /**
     * Extract salary data from Form24Q
     */
    private Map<String, Object> extractFromForm24Q(Map<String, Object> form24q) {
        Map<String, Object> result = new HashMap<>();
        
        // Get salary from income deductions
        Object incDedObj = form24q.get("incomeDeductions");
        if (incDedObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> incDed = (Map<String, Object>) incDedObj;
            
            Object salary = incDed.get("salary");
            if (salary instanceof Number) {
                result.put("basic", ((Number) salary).doubleValue());
                log.info("Salary from Form24Q: {}", salary);
            }
            
            Object profTax = incDed.get("professionalTaxUs16Iii");
            if (profTax instanceof Number) {
                result.put("profTax", ((Number) profTax).doubleValue());
            }
            
            Object stdDed = incDed.get("deductionUs16Ia");
            if (stdDed instanceof Number) {
                result.put("standardDeduction", ((Number) stdDed).doubleValue());
            }
        }
        
        // Get employer details
        Object salariesObj = form24q.get("salaries");
        if (salariesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> salaries = (Map<String, Object>) salariesObj;
            Object salList = salaries.get("salary");
            if (salList instanceof List && !((List<?>) salList).isEmpty()) {
                Object firstSal = ((List<?>) salList).get(0);
                if (firstSal instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sal = (Map<String, Object>) firstSal;
                    result.put("employerName", sal.get("nameOfEmployer"));
                    result.put("employerTAN", sal.get("tanOfEmployer"));
                }
            }
        }
        
        return result;
    }
    
    /**
     * Extract bank account details
     */
    private List<Map<String, Object>> extractBankDetails(List<Map<String, Object>> banks) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Object bankObj : banks) {
            if (bankObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bank = (Map<String, Object>) bankObj;
                
                Object addBankObj = bank.get("addtnlBankDetails");
                if (addBankObj instanceof List) {
                    for (Object ab : (List<?>) addBankObj) {
                        if (ab instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> abMap = (Map<String, Object>) ab;
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("bankName", abMap.get("bankName"));
                            entry.put("accountNo", abMap.get("bankAccountNo"));
                            entry.put("ifscCode", abMap.get("ifsccode"));
                            entry.put("accountType", abMap.get("AccountType"));
                            result.add(entry);
                        }
                    }
                }
            }
        }
        
        log.info("Extracted bank accounts: {}", result.size());
        return result;
    }
    
    private Map<String, Object> extractTDSEntry(Map<String, Object> tds) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("deductorName", tds.get("deductorName"));
        entry.put("deductorTAN", tds.get("deductorTAN"));
        entry.put("deductorPAN", tds.get("deductorPAN"));
        
        // Map section - frontend expects specific dropdown values
        String section = getString(tds, "section");
        if (section == null || section.isEmpty()) {
            section = "OTHER";
        } else {
            // Map from TRACES section codes to frontend dropdown values
            section = mapSectionToFrontend(section);
        }
        entry.put("section", section);
        
        // Map amount - frontend expects "incomeAmount"
        Object amountPaid = tds.get("amountPaid");
        double amountVal = amountPaid instanceof Number ? ((Number) amountPaid).doubleValue() : 0;
        entry.put("incomeAmount", amountVal);
        
        // Map TDS - frontend expects "tdsDeducted" (NOT "taxDeducted")
        Object taxDeducted = tds.get("taxDeducted");
        double tdsVal = taxDeducted instanceof Number ? ((Number) taxDeducted).doubleValue() : 0;
        entry.put("tdsDeducted", tdsVal);  // Frontend uses this
        entry.put("taxDeducted", tdsVal);   // Backward compatibility
        
        entry.put("verified26AS", true);
        entry.put("claimedInReturn", true);
        entry.put("financialYear", "2024-25");
        return entry;
    }
    
    /**
     * Map TRACES section codes to frontend dropdown values.
     * The frontend dropdown now supports ALL sections from the TRACES PDF glossary.
     */
    private String mapSectionToFrontend(String section) {
        if (section == null || section.isEmpty()) return "OTHER";
        
        // TRACES section codes that directly match frontend dropdown values
        switch (section) {
            case "192": return "192";
            case "192A": return "192A";
            case "193": return "193";
            case "194": return "194";
            case "194A": return "194A";
            case "194B": return "194B";
            case "194BA": return "194BA";
            case "194BB": return "194BB";
            case "194C": return "194C";
            case "194D": return "194D";
            case "194DA": return "194DA";
            case "194E": return "194E";
            case "194EE": return "194EE";
            case "194F": return "194F";
            case "194G": return "194G";
            case "194H": return "194H";
            case "194I": return "194I";
            case "194IA": return "194IA";
            case "194IB": return "194IB";
            case "194IC": return "194IC";
            case "194J": return "194J";
            case "194K": return "194K";
            case "194LA": return "194LA";
            case "194LB": return "194LB";
            case "194LBA": return "194LBA";
            case "194LBB": return "194LBB";
            case "194LBC": return "194LBC";
            case "194LC": return "194LC";
            case "194LD": return "194LD";
            case "194M": return "194M";
            case "194N": return "194N";
            case "194O": return "194O";
            case "194P": return "194P";
            case "194Q": return "194Q";
            case "194R": return "194R";
            case "194S": return "194S";
            case "195": return "195";
            case "196A": return "196A";
            case "196B": return "196B";
            case "196C": return "196C";
            case "196D": return "196D";
            case "196DA": return "196DA";
            case "206C": return "206C";
            case "206CA": return "206CA";
            case "206CB": return "206CB";
            case "206CC": return "206CC";
            case "206CD": return "206CD";
            case "206CE": return "206CE";
            case "206CF": return "206CF";
            case "206CG": return "206CG";
            case "206CH": return "206CH";
            case "206CI": return "206CI";
            case "206CJ": return "206CJ";
            case "206CK": return "206CK";
            case "206CL": return "206CL";
            case "206CM": return "206CM";
            case "206CN": return "206CN";
            case "206CO": return "206CO";
            case "206CP": return "206CP";
            case "206CQ": return "206CQ";
            case "206CR": return "206CR";
            case "206CT": return "206CT";
            default: {
                log.warn("Unknown section code: {} - mapping to OTHER", section);
                return "OTHER";
            }
        }
    }
    
    private double getDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return 0;
        }
    }
    
    @PostMapping("/26as")
    public ResponseEntity<Map<String, Object>> upload26AS(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "PARSED", "entries", 0));
    }

    @PostMapping("/ais")
    public ResponseEntity<Map<String, Object>> uploadAIS(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "PARSED"));
    }

    @PostMapping("/form16")
    public ResponseEntity<Map<String, Object>> uploadForm16(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "PARSED", "employerName", ""));
    }

    @PostMapping("/broker-statement")
    public ResponseEntity<Map<String, Object>> uploadBrokerStatement(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "PARSED", "transactions", 0));
    }

    @PostMapping("/cams")
    public ResponseEntity<Map<String, Object>> uploadCAMS(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "PARSED"));
    }

    @PostMapping("/tally")
    public ResponseEntity<Map<String, Object>> uploadTally(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "PARSED"));
    }
    
    // Helper methods
    
    private FlatFormData buildFlatFormData(Map<String, Object> formDataMap) {
        if (formDataMap == null) {
            return FlatFormData.builder()
                .employerEntries(new ArrayList<>())
                .tdsEntries(new ArrayList<>())
                .selfAssessmentTaxEntries(new ArrayList<>())
                .build();
        }
        
        return FlatFormData.builder()
            .name(getString(formDataMap, "name"))
            .pan(getString(formDataMap, "pan"))
            .fatherName(getString(formDataMap, "fatherName"))
            .dob(getString(formDataMap, "dob"))
            .aadhaar(getString(formDataMap, "aadhaar"))
            .email(getString(formDataMap, "email"))
            .mobile(getString(formDataMap, "mobile"))
            .flatDoorNo(getString(formDataMap, "flatNo"))
            .premisesName(getString(formDataMap, "premises"))
            .roadStreet(getString(formDataMap, "road"))
            .area(getString(formDataMap, "area"))
            .townCity(getString(formDataMap, "city"))
            .state(getString(formDataMap, "state"))
            .pinCode(getString(formDataMap, "pincode"))
            .basic(getDouble(formDataMap, "basic"))
            .profTax(getDouble(formDataMap, "profTax"))
            .employerEntries(new ArrayList<>())
            .tdsEntries(new ArrayList<>())
            .selfAssessmentTaxEntries(new ArrayList<>())
            .build();
    }
    
    private Map<String, Object> flatFormDataToMap(FlatFormData ffd) {
        Map<String, Object> result = new HashMap<>();
        
        // Personal Info
        result.put("name", ffd.getName());
        result.put("pan", ffd.getPan());
        result.put("fatherName", ffd.getFatherName());
        result.put("dob", ffd.getDob());
        result.put("aadhaar", ffd.getAadhaar());
        result.put("email", ffd.getEmail());
        result.put("mobile", ffd.getMobile());
        
        // Address
        result.put("flatNo", ffd.getFlatDoorNo());
        result.put("premises", ffd.getPremisesName());
        result.put("road", ffd.getRoadStreet());
        result.put("area", ffd.getArea());
        result.put("city", ffd.getTownCity());
        result.put("state", ffd.getState());
        result.put("pincode", ffd.getPinCode());
        
        // Salary
        result.put("basic", ffd.getBasic());
        result.put("da", ffd.getDa());
        result.put("hra", ffd.getHra());
        result.put("bonus", ffd.getBonus());
        result.put("allowances", ffd.getAllowances());
        result.put("perquisites", ffd.getPerquisites());
        result.put("hraRent", ffd.getHraRent());
        result.put("hraMetro", ffd.isHraMetro());
        result.put("profTax", ffd.getProfTax());
        
        // Employer details
        result.put("employerName", ffd.getEmployerName());
        result.put("employerTAN", ffd.getEmployerTAN());
        
        // *** OTHER SOURCES - CRITICAL ***
        double interestSB = ffd.getInterestSB();
        double interestFD = ffd.getInterestFD();
        double dividends = ffd.getDividends();
        double otherMisc = ffd.getOtherMisc();
        double familyPension = ffd.getFamilyPension();
        
        // Legacy single-value fields (for backward compatibility)
        result.put("interestSB", interestSB);
        result.put("interestFD", interestFD);
        result.put("dividends", dividends);
        result.put("otherMisc", otherMisc);
        
        // Calculate Total Other Sources
        double totalOtherSources = interestSB + interestFD + dividends + otherMisc + familyPension;
        result.put("totalOtherSources", totalOtherSources);
        
        // Create bank interest entries from prefill data
        // Priority: Bank Account Details from prefill (contains bank name, account, IFSC) + Interest from insights
        List<Map<String, Object>> bankInterestEntries = new ArrayList<>();
        
        // First check if bank account details exist in prefill
        boolean hasBankAccounts = ffd.getBankAccountDetails() != null && !ffd.getBankAccountDetails().isEmpty();
        
        if (hasBankAccounts) {
            for (FlatFormData.BankAccountDetail bank : ffd.getBankAccountDetails()) {
                Map<String, Object> entry = new HashMap<>();
                
                // Use actual bank name from prefill
                String bankName = bank.getBankName();
                if (bankName == null || bankName.isEmpty()) {
                    bankName = "Bank (from prefill)";
                }
                entry.put("bankName", bankName);
                
                // Account type - convert to frontend format
                String accType = bank.getAccountType();
                if (accType == null || accType.isEmpty()) {
                    // Guess based on interest - if has savings interest, assume savings
                    accType = (interestSB > 0) ? "SAVINGS" : "FD";
                } else {
                    String upperType = accType.toUpperCase();
                    if (upperType.contains("SAVINGS") || upperType.contains("SB")) {
                        accType = "SAVINGS";
                    } else if (upperType.contains("CURRENT") || upperType.contains("CC")) {
                        accType = "CURRENT";
                    } else {
                        // Default to FD for any deposit accounts
                        accType = "FD";
                    }
                }
                entry.put("accountType", accType);
                
                // Account number - CRITICAL field
                String accountNo = bank.getAccountNo();
                entry.put("accountNumber", accountNo != null ? accountNo : "");  // For frontend
                entry.put("accountNo", accountNo != null ? accountNo : "");       // Compatibility
                
                // IFSC Code - CRITICAL field  
                String ifscCode = bank.getIfscCode();
                entry.put("ifscCode", ifscCode != null ? ifscCode : "");
                
                // Interest - assign based on account type from prefill
                double interest = 0;
                if ("SAVINGS".equals(accType)) {
                    interest = interestSB;
                } else {
                    // All other types (FD, CURRENT, etc.) get FD interest
                    interest = interestFD;
                }
                entry.put("interestEarned", interest);
                entry.put("tdsDeducted", 0);
                
                bankInterestEntries.add(entry);
                log.info("Bank entry: {} - {} - Interest: {}", bankName, accountNo, interest);
            }
        }
        
        // If no bank details but have interest data, create entries
        if (bankInterestEntries.isEmpty()) {
            if (interestSB > 0) {
                bankInterestEntries.add(createBankEntry("Bank (from prefill)", "SAVINGS", interestSB, "", ""));
            }
            if (interestFD > 0) {
                bankInterestEntries.add(createBankEntry("Bank (from prefill)", "FD", interestFD, "", ""));
            }
        }
        
        result.put("bankInterestEntries", bankInterestEntries);
        
        // Deductions
        result.put("s80C_epf", ffd.getS80C_epf());
        result.put("s80C_ppf", ffd.getS80C_ppf());
        result.put("s80C_elss", ffd.getS80C_elss());
        result.put("s80C_lic", ffd.getS80C_lic());
        result.put("s80C_home", ffd.getS80C_home());
        result.put("s80CCD1B", ffd.getS80CCD1B());
        result.put("s80CCD2", ffd.getS80CCD2());
        result.put("s80D_self", ffd.getS80D_self());
        result.put("s80D_parent", ffd.getS80D_parent());
        result.put("s80E", ffd.getS80E());
        result.put("s80TTA", ffd.getS80TTA());
        
        // TDS
        double tdsS192 = ffd.getTdsS192();
        double tdsOther = ffd.getTdsOther();
        result.put("tdsS192", tdsS192);
        result.put("tdsOther", tdsOther);
        result.put("tdsTotal", tdsS192 + tdsOther);
        
        // Complex objects
        result.put("employerEntries", ffd.getEmployerEntries() != null ? ffd.getEmployerEntries() : new ArrayList<>());
        result.put("tdsEntries", ffd.getTdsEntries() != null ? ffd.getTdsEntries() : new ArrayList<>());
        result.put("selfAssessmentTaxEntries", ffd.getSelfAssessmentTaxEntries() != null ? ffd.getSelfAssessmentTaxEntries() : new ArrayList<>());
        result.put("bankAccountDetails", ffd.getBankAccountDetails() != null ? ffd.getBankAccountDetails() : new ArrayList<>());
        
        result.put("autoPopulated", true);
        result.put("source", "ITD_PREFILL");
        
        log.info("Returning to frontend - Total Other Sources: {}, InterestSB: {}, InterestFD: {}, BankEntries: {}", 
            totalOtherSources, interestSB, interestFD, bankInterestEntries.size());
        
        return result;
    }
    
    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
    
    private Double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private Map<String, Object> createBankEntry(String bankName, String accountType, double interest, String accountNo, String ifscCode) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("bankName", bankName != null ? bankName : "Bank (from prefill)");
        entry.put("accountType", accountType != null ? accountType : "SAVINGS");
        entry.put("accountNumber", accountNo != null ? accountNo : "");  // Use accountNumber for frontend
        entry.put("accountNo", accountNo != null ? accountNo : "");       // Keep for compatibility
        entry.put("ifscCode", ifscCode != null ? ifscCode : "");
        entry.put("interestEarned", interest);
        entry.put("tdsDeducted", 0);
        return entry;
    }
    
    @Data
    public static class AutoPopulatePrefillRequest {
        private Map<String, Object> formData;
        private Object prefillData;
        private String jsonContent;
        private MultipartFile file;
    }
}
