package com.itr.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itr.dto.AISData;
import com.itr.dto.AISData.*;
import com.itr.util.AISJsonDecryptor;
import com.itr.util.PIIMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Import AIS from the JSON format downloaded from the ITD compliance portal.
 *
 * <p>Decryption: PBKDF2-HMAC-SHA256 (1000 iter, 32B key) -> AES-256-CBC
 * Password: {pan_lower}{"GQ39%*g"}{ddmmyyyy}
 *
 * <p>Extracts all AIS sections:
 * <ul>
 *   <li>Part A: General information (personal details)</li>
 *   <li>Part B1: TDS/TCS — aggregated entries + individual transactions</li>
 *   <li>Part B2: SFT — dividend, interest, property sale, MF/securities, purchase</li>
 *   <li>Part B3: Tax payments (advance tax, self-assessment)</li>
 *   <li>Part B4: Demand & refund</li>
 *   <li>Part B7: Other information</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AISJsonImportService {

    private final AISJsonDecryptor decryptor;
    private final ObjectMapper objectMapper;
    private final PIIMaskingUtil piiMasking;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ============================================================
    //  Public API
    // ============================================================

    /**
     * Import and decrypt an AIS JSON file.
     *
     * @param jsonBytes      encrypted AIS JSON file bytes
     * @param pan            PAN number (used for password derivation)
     * @param dob            date of birth (for display)
     * @param dobForPassword date of birth in ddMMyyyy format (for password derivation)
     */
    public AISData importAIS(byte[] jsonBytes, String pan, LocalDate dob, String dobForPassword) throws Exception {
        String encrypted = new String(jsonBytes, java.nio.charset.StandardCharsets.UTF_8);
        String json = decryptor.decrypt(encrypted, pan, dobForPassword);
        return parseAISJson(json, pan);
    }

    /**
     * Import from already-decrypted JSON (for testing).
     */
    public AISData importDecryptedAIS(String json, String pan) throws Exception {
        return parseAISJson(json, pan);
    }

    // ============================================================
    //  Root parsing
    // ============================================================

    private AISData parseAISJson(String json, String pan) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode partB = root.path("partB");

        AISData data = AISData.builder()
                .pan(pan)
                .assessmentYear(getAssessmentYear(root))
                .financialYear(getFinancialYear(root))
                .generalInfo(parsePartA(root.path("partA")))
                .partB1(parsePartB1(partB))
                .partB2(parsePartB2(partB))
                .partB3(parsePartB3(partB))
                .tdsSalary(new ArrayList<>())
                .tdsOther(new ArrayList<>())
                .build();

        AISPartB2Data b2Data = data.getPartB2();
        List<DemandRefund> b4Results = parsePartB4(partB);
        List<OtherInfoEntry> b7Results = parsePartB7(partB);
        log.info("AIS JSON complete: PAN={}, FY={}, AY={} | B1: {} TDS entries + {} transactions | B2: div={}, savInt={}, depInt={}, propSale={}, secSale={}, mfPur={} | B3: {} payments | B4: {} demands | B7: {} other",
                pan, data.getFinancialYear(), data.getAssessmentYear(),
                size(data.getPartB1() != null ? data.getPartB1().getTdsEntries() : null),
                size(data.getPartB1() != null ? data.getPartB1().getTdsTransactions() : null),
                b2Data != null ? b2Data.getDividendIncome() : 0,
                size(b2Data != null ? b2Data.getSavingsInterest() : null),
                size(b2Data != null ? b2Data.getDepositInterest() : null),
                size(b2Data != null ? b2Data.getPropertySales() : null),
                size(b2Data != null ? b2Data.getMfSales() : null),
                size(b2Data != null ? b2Data.getMutualFundPurchase() : null),
                size(data.getPartB3()),
                size(b4Results),
                size(b7Results));

        return data;
    }

    // ============================================================
    //  Part A — General Information
    // ============================================================

    /**
     * Part A uses flat parallel arrays: columnLabel[i] maps to columnData[i]
     * e.g. ["PAN", "Name"] + ["ACUPG3482G", "John"] → {pan: ACUPG..., name: John}
     */
    private AISGeneralInfo parsePartA(JsonNode partA) {
        if (partA.isMissingNode()) return null;

        AISGeneralInfo info = new AISGeneralInfo();
        JsonNode labels = partA.path("columnLabel");
        JsonNode values = partA.path("columnData");

        if (!labels.isArray() || !values.isArray()) return info;

        // First row contains all values (flat arrays, not nested rows)
        JsonNode firstRow = values.get(0);
        if (firstRow == null) return info;

        for (int i = 0; i < labels.size() && i < firstRow.size(); i++) {
            String label = labels.get(i).asText("").trim();
            String value = safeText(firstRow.get(i));
            assignGeneralInfoField(info, label, value);
        }

        return info;
    }

    private void assignGeneralInfoField(AISGeneralInfo info, String label, String value) {
        if (value.isEmpty()) return;
        String l = label.toLowerCase();

        if (l.contains("pan") && !l.contains("mobile")) {
            info.setPan(value.toUpperCase());
        } else if (l.contains("aadhaar")) {
            info.setAadhaar(value.replace(" ", ""));
        } else if (l.contains("name") && !l.contains("father")) {
            info.setName(value.toUpperCase());
        } else if (l.contains("date") && l.contains("birth") || l.contains("dob")) {
            info.setDob(parseDate(value));
        } else if (l.contains("mobile") || l.contains("phone")) {
            info.setMobile(value.replaceAll("[^0-9]", ""));
        } else if (l.contains("e-mail") || l.contains("email")) {
            info.setEmail(value.toLowerCase());
        } else if (l.contains("address")) {
            info.setAddress(value);
        }
    }

    // ============================================================
    //  Part B1 — TDS/TCS
    // ============================================================

    /**
     * Section key: "tdsTcs"
     *
     * Each element has:
     *   - title: category name (e.g. "Interest from deposit")
     *   - infoSrcId: source entity ID (TAN)
     *   - l2: aggregated system data (one row with deductor name, section, totals)
     *   - l1: individual transaction rows (columnLabel[] of field objects + columnData[][])
     *
     * L1 column fields (from JSON objects):
     *   tsnId, quarter, transactionDate, amtPaid, amountDeducted, amountDeposited,
     *   status, transFeedback
     *
     * L2 columns: Information Category, Information Code (TDS-194A), Information Source,
     *             Count, Amount, Information Category Code, Derived Amount, Qualifies For
     */
    private AISPartB1Data parsePartB1(JsonNode partB) {
        List<AISTDSEntry> entries = new ArrayList<>();
        List<AISTransaction> transactions = new ArrayList<>();

        for (JsonNode section : partB.path("sections")) {
            if (!"tdsTcs".equals(section.path("sectionKey").asText(""))) continue;

            for (JsonNode element : section.path("elements")) {
                if (element == null || element.isNull()) continue;

                String category = element.path("title").asText("");
                String sourceId = element.path("infoSrcId").asText("");

                // ---- L2: aggregated data (deductor, section, total) ----
                String deductorName = "";
                String sectionCode = "";
                long l2TotalAmount = 0;
                long l2TotalTDS = 0;
                int transactionCount = 0;

                JsonNode l2 = element.path("l2");
                if (!l2.isMissingNode() && l2.has("columnData") && l2.get("columnData").isArray()) {
                    JsonNode l2Data = l2.get("columnData");
                    JsonNode l2Labels = l2.path("columnLabel");

                    if (l2Data.size() > 0) {
                        JsonNode l2Row = l2Data.get(0);
                        for (int ci = 0; ci < l2Labels.size() && ci < l2Row.size(); ci++) {
                            String l2Label = l2Labels.get(ci).asText("").toLowerCase();
                            String l2Val = safeText(l2Row.get(ci));

                            if (l2Label.contains("source") && !l2Val.isEmpty()) {
                                deductorName = extractNameFromSource(l2Val);
                            } else if (l2Label.contains("code") && l2Val.matches("(?i)(TDS|TCS)-\\d+.*")) {
                                sectionCode = extractSectionCode(l2Val);
                            } else if (l2Label.contains("amount") && l2Val.length() > 0 && l2TotalAmount == 0) {
                                l2TotalAmount = parseAmount(l2Val);
                            } else if (l2Label.contains("count") && transactionCount == 0) {
                                transactionCount = (int) parseAmount(l2Val);
                            }
                        }
                    }
                }

                // Fallback section detection from category title
                if (sectionCode.isEmpty()) {
                    sectionCode = detectSectionCode(category);
                }
                if (deductorName.isEmpty()) {
                    deductorName = category;
                }

                // ---- L1: individual transaction rows ----
                JsonNode l1 = element.path("l1");
                if (!l1.isMissingNode() && l1.has("columnData") && l1.get("columnData").isArray()) {
                    JsonNode l1Labels = l1.path("columnLabel");
                    JsonNode l1Data = l1.get("columnData");
                    Map<String, Integer> colIdx = indexL1Columns(l1Labels);

                    for (JsonNode row : l1Data) {
                        if (!row.isArray()) continue;

                        String tsnId = safeText(col(row, colIdx, "tsnid"));
                        String quarter = safeText(col(row, colIdx, "quarter"));
                        String dateStr = safeText(col(row, colIdx, "transactiondate"));
                        String amountStr = safeText(col(row, colIdx, "amtpaid"));
                        String deductStr = safeText(col(row, colIdx, "amountdeducted"));
                        String depositStr = safeText(col(row, colIdx, "amountdeposited"));
                        String status = safeText(col(row, colIdx, "status"));
                        String feedback = safeText(col(row, colIdx, "transfeedback"));

                        if (amountStr.isEmpty()) continue;

                        long amount = parseAmount(amountStr);
                        long deducted = deductStr.isEmpty() ? 0 : parseAmount(deductStr);
                        long deposited = depositStr.isEmpty() ? 0 : parseAmount(depositStr);

                        AISTransaction txn = AISTransaction.builder()
                                .tsnId(tsnId)
                                .quarter(quarter)
                                .transactionDate(parseDate(dateStr))
                                .amountPaid(amount)
                                .tdsDeducted(deducted)
                                .tdsDeposited(deposited)
                                .status(status)
                                .section(sectionCode)
                                .deductorName(deductorName)
                                .deductorTAN(extractTAN(sourceId))
                                .build();
                        transactions.add(txn);
                    }

                    // Add one aggregated entry per source (from L2)
                    // Sum TDS from individual L1 rows (L2 doesn't have a TDS column)
                    long sumTDS = transactions.stream()
                            .filter(t -> t.getDeductorTAN().equals(extractTAN(sourceId)))
                            .mapToLong(AISTransaction::getTdsDeducted)
                            .sum();
                    if (l2TotalAmount > 0 || !deductorName.isEmpty()) {
                        AISTDSEntry entry = AISTDSEntry.builder()
                                .section(sectionCode)
                                .deductorName(deductorName)
                                .deductorTAN(extractTAN(sourceId))
                                .totalAmountPaid(l2TotalAmount > 0 ? l2TotalAmount : transactions.stream().mapToLong(AISTransaction::getAmountPaid).sum())
                                .totalTDSDeducted(sumTDS)
                                .build();
                        entries.add(entry);
                        log.info("B1 TDS entry: Sec={}, Deductor={}, TAN={}, Amt={}, Count={}, Transactions={}",
                                sectionCode, deductorName, extractTAN(sourceId), l2TotalAmount,
                                transactionCount, l1Data.size());
                    }
                }
            }
        }

        // Deduplicate entries by deductorTAN + section
        List<AISTDSEntry> deduplicated = aggregateTDSEntries(entries);
        log.info("Part B1: {} aggregated entries, {} individual transactions",
                deduplicated.size(), transactions.size());

        return AISPartB1Data.builder()
                .tdsEntries(deduplicated)
                .tdsTransactions(transactions)
                .build();
    }

    /**
     * Index L1 column labels (JSON objects with "field" and "name" keys).
     * Maps both field name (camelCase) and name (Title Case) to column index.
     */
    private Map<String, Integer> indexL1Columns(JsonNode labels) {
        Map<String, Integer> idx = new HashMap<>();
        if (!labels.isArray()) return idx;
        for (int i = 0; i < labels.size(); i++) {
            JsonNode col = labels.get(i);
            if (col.has("field")) {
                idx.put(col.get("field").asText("").toLowerCase(), i);
            }
            if (col.has("name")) {
                idx.put(col.get("name").asText("").toLowerCase(), i);
            }
        }
        return idx;
    }

    private List<AISTDSEntry> aggregateTDSEntries(List<AISTDSEntry> raw) {
        Map<String, AISTDSEntry> byKey = new LinkedHashMap<>();
        for (AISTDSEntry e : raw) {
            String key = (e.getDeductorTAN() != null ? e.getDeductorTAN() : "") + "|" +
                    (e.getSection() != null ? e.getSection() : "OTHER");
            byKey.merge(key, e, (existing, incoming) -> {
                if (incoming.getTotalAmountPaid() > existing.getTotalAmountPaid()) return incoming;
                return existing;
            });
        }
        return new ArrayList<>(byKey.values());
    }

    // ============================================================
    //  Part B2 — SFT (Specified Financial Transactions)
    // ============================================================

    /**
     * Section key: "sft"
     *
     * Handles all SFT categories:
     *   - Dividend (SFT-015): tsnId, reportedOn, amount, status
     *   - Interest from savings bank: entity name, amount per source
     *   - Interest from deposit: entity name, amount per source (FD interest)
     *   - Sale of land or building: full transaction details
     *   - Sale of securities/MF: security details, cost basis, STT
     *   - Purchase of securities/MF: AMC name, client ID, amounts
     *
     * SFT sale L1 columns (from field objects):
     *   tsnId, quarter, amcNameCode, securityName, transferDate, securityClass,
     *   assetType, quantity, sellPricePerUnit, salesConsideration, sttPaid,
     *   costOfAcquisition, fmvValueUnit, fmvValue, indexCostOfAcquisition, status
     *
     * SFT purchase L1 columns:
     *   tsnId, quarter, clientID, amcNameCode, holderFlag,
     *   totalPurchaseAmount, totalSalesValue, status
     */
    private AISPartB2Data parsePartB2(JsonNode partB) {
        AISPartB2Data b2 = new AISPartB2Data();
        b2.setSecuritiesSale(new ArrayList<>());
        b2.setMutualFundPurchase(new ArrayList<>());
        b2.setSavingsInterest(new ArrayList<>());
        b2.setDepositInterest(new ArrayList<>());
        b2.setPropertySales(new ArrayList<>());
        b2.setMfSales(new ArrayList<>());
        b2.setDividendIncome(0);

        long totalMFPurchase = 0;

        for (JsonNode section : partB.path("sections")) {
            if (!"sft".equals(section.path("sectionKey").asText(""))) continue;

            for (JsonNode element : section.path("elements")) {
                if (element == null || element.isNull()) continue;

                String category = element.path("title").asText("").toLowerCase();
                String sourceId = element.path("infoSrcId").asText("");

                JsonNode l2 = element.path("l2");
                JsonNode l1 = element.path("l1");

                // Extract L2 aggregated values
                String entityName = "";
                String infoCode = "";
                String infoCategory = "";
                long l2TotalAmount = 0;
                int l2Count = 0;

                if (!l2.isMissingNode() && l2.has("columnData") && l2.get("columnData").isArray()) {
                    JsonNode l2Data = l2.get("columnData");
                    JsonNode l2Labels = l2.path("columnLabel");

                    if (l2Data.size() > 0) {
                        JsonNode l2Row = l2Data.get(0);
                        for (int ci = 0; ci < l2Labels.size() && ci < l2Row.size(); ci++) {
                            String lLabel = l2Labels.get(ci).asText("").toLowerCase();
                            String lVal = safeText(l2Row.get(ci));

                            if (lLabel.contains("source") && !lVal.isEmpty()) {
                                entityName = extractNameFromSource(lVal);
                            } else if (lLabel.contains("code")) {
                                infoCode = lVal;
                            } else if (lLabel.contains("amount") && l2TotalAmount == 0) {
                                l2TotalAmount = parseAmount(lVal);
                            } else if (lLabel.contains("count") && l2Count == 0) {
                                l2Count = (int) parseAmount(lVal);
                            } else if (lLabel.contains("category") && !lLabel.contains("code")) {
                                infoCategory = lVal;
                            }
                        }
                    }
                }

                // ---- CATEGORY: Dividend ----
                if (category.contains("dividend")) {
                    // L1 rows for dividend income, but do NOT add to securitiesSale
                    // (securitiesSale is only for capital gains, dividends are Other Sources)
                    if (!l1.isMissingNode() && l1.has("columnData") && l1.get("columnData").isArray()) {
                        JsonNode l1Labels = l1.path("columnLabel");
                        JsonNode l1Data = l1.get("columnData");
                        Map<String, Integer> colIdx = indexL1Columns(l1Labels);

                        for (JsonNode row : l1Data) {
                            if (!row.isArray()) continue;
                            String tsnId = safeText(col(row, colIdx, "tsnid"));
                            String dateStr = safeText(col(row, colIdx, "reportedon"));
                            String amountStr = safeText(col(row, colIdx, "amount"));
                            String status = safeText(col(row, colIdx, "status"));

                            if (amountStr.isEmpty()) continue;

                            // Store dividend as separate list entry (not in securitiesSale)
                            SFTSaleEntry dividend = SFTSaleEntry.builder()
                                    .transferDate(parseDate(dateStr))
                                    .securityName(entityName)
                                    .assetType("Dividend")
                                    .quantity(BigDecimal.ONE)
                                    .salesConsideration(BigDecimal.valueOf(parseAmount(amountStr)))
                                    .build();
                            b2.getDividendEntries().add(dividend);
                        }
                    }
                    b2.setDividendIncome(b2.getDividendIncome() + l2TotalAmount);
                    log.info("B2 Dividend: {} from {} ({} transactions)",
                            l2TotalAmount, entityName, l2Count);
                }

                // ---- CATEGORY: Interest from savings bank ----
                else if (category.contains("savings") && category.contains("bank")) {
                    String accountNo = "";
                    String accountType = "SAVINGS";
                    if (!l1.isMissingNode() && l1.has("columnData") && l1.get("columnData").isArray()) {
                        JsonNode l1Labels = l1.path("columnLabel");
                        JsonNode l1Data = l1.get("columnData");
                        Map<String, Integer> colIdx = indexL1Columns(l1Labels);
                        if (l1Data.size() > 0) {
                            JsonNode row = l1Data.get(0);
                            accountNo = safeText(col(row, colIdx, "accountNo"));
                            String rawType = safeText(col(row, colIdx, "accountType"));
                            if (rawType.contains("Time") || rawType.contains("Fixed")) accountType = "FD";
                            else if (rawType.contains("Current")) accountType = "CURRENT";
                        }
                    }
                    InterestEntry ie = InterestEntry.builder()
                            .sourceName(entityName)
                            .sourceId(extractTAN(sourceId))
                            .totalAmount(l2TotalAmount)
                            .transactionCount(l2Count)
                            .accountNumber(accountNo)
                            .accountType(accountType)
                            .build();
                    b2.getSavingsInterest().add(ie);
                    log.info("B2 Savings Interest: {} from {} ({} txns) | account: {} | type: {}",
                            l2TotalAmount, entityName, l2Count, accountNo, accountType);
                }

                // ---- CATEGORY: Interest from deposit (FD interest) ----
                else if (category.contains("interest") && category.contains("deposit")) {
                    String accountNo = "";
                    if (!l1.isMissingNode() && l1.has("columnData") && l1.get("columnData").isArray()) {
                        JsonNode l1Labels = l1.path("columnLabel");
                        JsonNode l1Data = l1.get("columnData");
                        Map<String, Integer> colIdx = indexL1Columns(l1Labels);
                        if (l1Data.size() > 0) {
                            JsonNode row = l1Data.get(0);
                            accountNo = safeText(col(row, colIdx, "accountNo"));
                        }
                    }
                    InterestEntry ie = InterestEntry.builder()
                            .sourceName(entityName)
                            .sourceId(extractTAN(sourceId))
                            .totalAmount(l2TotalAmount)
                            .transactionCount(l2Count)
                            .accountNumber(accountNo)
                            .accountType("FD")
                            .build();
                    b2.getDepositInterest().add(ie);
                    log.info("B2 FD Interest: {} from {} ({} txns) | account: {}",
                            l2TotalAmount, entityName, l2Count, accountNo);
                }

                // ---- CATEGORY: Sale of land or building ----
                else if (category.contains("sale") && (category.contains("land") || category.contains("building"))) {
                    if (!l1.isMissingNode() && l1.has("columnData") && l1.get("columnData").isArray()) {
                        JsonNode l1Labels = l1.path("columnLabel");
                        JsonNode l1Data = l1.get("columnData");
                        Map<String, Integer> colIdx = indexL1Columns(l1Labels);

                        for (JsonNode row : l1Data) {
                            if (!row.isArray()) continue;

                            // transDate = actual sale date, reportedOn = when AIS reported it
                            String saleDateStr = safeText(col(row, colIdx, "transdate"));
                            String amountStr = safeText(col(row, colIdx, "totalvalue"));
                            if (amountStr.isEmpty()) amountStr = safeText(col(row, colIdx, "amount"));
                            if (amountStr.isEmpty()) continue;

                            SFTSaleEntry sale = SFTSaleEntry.builder()
                                    .transferDate(parseDate(saleDateStr))
                                    .securityName(entityName + " (Property Sale)")
                                    .assetType("PROPERTY")
                                    .salesConsideration(BigDecimal.valueOf(parseAmount(amountStr)))
                                    .build();
                            b2.getPropertySales().add(sale);
                            log.info("B2 Property Sale: {} from {} | saleDate={}",
                                    parseAmount(amountStr), entityName, saleDateStr);
                        }
                    } else {
                        // Use L2 total
                        SFTSaleEntry sale = SFTSaleEntry.builder()
                                .securityName(entityName + " (Property Sale)")
                                .assetType("PROPERTY")
                                .salesConsideration(BigDecimal.valueOf(l2TotalAmount))
                                .build();
                        b2.getPropertySales().add(sale);
                        log.info("B2 Property Sale (L2): {} from {}", l2TotalAmount, entityName);
                    }
                }

                // ---- CATEGORY: Sale of securities/MF ----
                else if (category.contains("sale") && (category.contains("securit") || category.contains("mutual"))) {
                    if (!l1.isMissingNode() && l1.has("columnData") && l1.get("columnData").isArray()) {
                        JsonNode l1Labels = l1.path("columnLabel");
                        JsonNode l1Data = l1.get("columnData");
                        Map<String, Integer> colIdx = indexL1Columns(l1Labels);

                        for (JsonNode row : l1Data) {
                            if (!row.isArray()) continue;

                            String amcName = safeText(col(row, colIdx, "amcnamecode"));
                            String secName = safeText(col(row, colIdx, "securityname"));
                            String dateStr = safeText(col(row, colIdx, "transferdate"));
                            String qtyStr = safeText(col(row, colIdx, "quantity"));
                            String priceStr = safeText(col(row, colIdx, "sellpriceperunit"));
                            String saleAmtStr = safeText(col(row, colIdx, "salesconsideration"));
                            String costStr = safeText(col(row, colIdx, "costofacquisition"));
                            String sttStr = safeText(col(row, colIdx, "sttpaid"));
                            String fmvStr = safeText(col(row, colIdx, "fmvvalue"));
                            String idxCostStr = safeText(col(row, colIdx, "indexcostofacquisition"));
                            String assetType = safeText(col(row, colIdx, "assettype"));

                            if (saleAmtStr.isEmpty() && dateStr.isEmpty()) continue;

                            SFTSaleEntry sale = SFTSaleEntry.builder()
                                    .transferDate(parseDate(dateStr))
                                    .securityName(amcName.isEmpty() ? entityName : amcName)
                                    .assetType(deriveAssetType(secName, amcName, assetType))
                                    .quantity(parseBD(qtyStr))
                                    .salePricePerUnit(parseBD(priceStr))
                                    .salesConsideration(parseBD(saleAmtStr))
                                    .costOfAcquisition(parseBD(costStr))
                                    .fmvPerUnit(parseBD(fmvStr))
                                    .indexedCostOfAcquisition(parseBD(idxCostStr))
                                    .build();
                            b2.getMfSales().add(sale);
                        }
                    } else {
                        SFTSaleEntry sale = SFTSaleEntry.builder()
                                .securityName(entityName)
                                .assetType("LTCG")
                                .salesConsideration(BigDecimal.valueOf(l2TotalAmount))
                                .build();
                        b2.getMfSales().add(sale);
                    }
                    log.info("B2 MF/Sec Sale: {} from {}, {} rows",
                            l2TotalAmount, entityName, l1.has("columnData") ? l1.get("columnData").size() : 0);
                }

                // ---- CATEGORY: Purchase of securities/MF ----
                else if (category.contains("purchase") && (category.contains("securit") || category.contains("mutual"))) {
                    if (!l1.isMissingNode() && l1.has("columnData") && l1.get("columnData").isArray()) {
                        JsonNode l1Labels = l1.path("columnLabel");
                        JsonNode l1Data = l1.get("columnData");
                        Map<String, Integer> colIdx = indexL1Columns(l1Labels);

                        for (JsonNode row : l1Data) {
                            if (!row.isArray()) continue;

                            String amcName = safeText(col(row, colIdx, "amcnamecode"));
                            String clientId = safeText(col(row, colIdx, "clientid"));
                            String purchaseStr = safeText(col(row, colIdx, "totalpurchaseamount"));
                            String salesStr = safeText(col(row, colIdx, "totalsalesvalue"));

                            long purchaseAmt = parseAmount(purchaseStr);
                            long salesAmt = parseAmount(salesStr);

                            if (purchaseAmt == 0 && salesAmt == 0) continue;

                            MFPurchase mf = MFPurchase.builder()
                                    .amcName(amcName.isEmpty() ? entityName : amcName)
                                    .clientId(clientId)
                                    .totalPurchase(purchaseAmt)
                                    .totalSales(salesAmt)
                                    .build();
                            b2.getMutualFundPurchase().add(mf);
                            totalMFPurchase += purchaseAmt;
                        }
                    } else {
                        MFPurchase mf = MFPurchase.builder()
                                .amcName(entityName)
                                .totalPurchase(l2TotalAmount)
                                .build();
                        b2.getMutualFundPurchase().add(mf);
                        totalMFPurchase += l2TotalAmount;
                    }
                    log.info("B2 MF Purchase: {} from {}, {} rows",
                            l2TotalAmount, entityName, l1.has("columnData") ? l1.get("columnData").size() : 0);
                }

                // ---- CATEGORY: Unhandled ----
                else {
                    log.debug("B2 unhandled SFT category: {} | source: {} | amount: {}",
                            category, entityName, l2TotalAmount);
                }
            }
        }

        // PrefillController reads "securitiesSale" for capital gains — merge propertySales + mfSales
        if (!b2.getPropertySales().isEmpty()) {
            b2.getSecuritiesSale().addAll(b2.getPropertySales());
        }
        if (!b2.getMfSales().isEmpty()) {
            b2.getSecuritiesSale().addAll(b2.getMfSales());
        }

        b2.setSecuritiesPurchaseAmount(totalMFPurchase);
        log.info("Part B2 complete: dividend={}, savingsInterest={}, depositInterest={}, propertySales={}, mfSales={}, mfPurchases={}",
                b2.getDividendIncome(), b2.getSavingsInterest().size(), b2.getDepositInterest().size(),
                b2.getPropertySales().size(), b2.getMfSales().size(), b2.getMutualFundPurchase().size());

        return b2;
    }

    // ============================================================
    //  Part B3 — Tax Payments
    // ============================================================

    /**
     * Section key: "paymentOfTaxes"
     *
     * Uses direct columnLabel/columnData on elements (not L1/L2 nested).
     * Columns: Financial Year, Major Head, Minor Head, Tax (A), Surcharge (B),
     *          Education Cess (C), Others (D), Total (A+B+C+D), BSR Code,
     *          Date Of Deposit, Challan Serial Number, Challan Identification Number
     */
    private List<TaxPaymentAIS> parsePartB3(JsonNode partB) {
        List<TaxPaymentAIS> payments = new ArrayList<>();

        for (JsonNode section : partB.path("sections")) {
            if (!"paymentOfTaxes".equals(section.path("sectionKey").asText(""))) continue;

            for (JsonNode element : section.path("elements")) {
                if (element == null || element.isNull()) continue;

                JsonNode topLabels = element.path("columnLabel");
                JsonNode topData = element.path("columnData");

                if (!topLabels.isArray() || !topData.isArray() || topData.isEmpty()) continue;

                Map<String, Integer> idx = indexSimpleLabels(topLabels);

                for (JsonNode row : topData) {
                    if (!row.isArray()) continue;

                    String fy = safeText(col(row, idx, "financial year")).replaceAll("[^0-9\\-]", "");
                    String majorHead = safeText(col(row, idx, "major head"));
                    String minorHead = safeText(col(row, idx, "minor head"));
                    String taxAStr = safeText(col(row, idx, "tax (a)"));
                    String surchStr = safeText(col(row, idx, "surcharge (b)"));
                    String cessStr = safeText(col(row, idx, "education cess (c)"));
                    String othersStr = safeText(col(row, idx, "others (d)"));
                    String totalStr = safeText(col(row, idx, "total (a+b+c+d)"));
                    String bsr = safeText(col(row, idx, "bsr code"));
                    String dateStr = safeText(col(row, idx, "date of deposit"));
                    String challan = safeText(col(row, idx, "challan serial number"));
                    String cin = safeText(col(row, idx, "challan identification number"));

                    if (fy.isEmpty() && majorHead.isEmpty() && minorHead.isEmpty()) continue;

                    TaxPaymentAIS payment = TaxPaymentAIS.builder()
                            .financialYear(fy)
                            .minorHead(minorHead)
                            .taxAmount(parseAmount(taxAStr))
                            .surcharge(parseAmount(surchStr))
                            .educationCess(parseAmount(cessStr))
                            .totalAmount(parseAmount(totalStr))
                            .bsrCode(bsr)
                            .depositDate(parseDate(dateStr))
                            .challanSerialNo(challan)
                            .build();
                    payments.add(payment);

                    log.info("B3 Tax Payment: FY={}, Major={}, Minor={}, Total={}, BSR={}, Date={}, Challan={}",
                            fy, majorHead, minorHead, payment.getTotalAmount(), bsr, dateStr, challan);
                }
            }
        }

        log.info("Part B3: {} tax payment entries", payments.size());
        return payments;
    }

    /**
     * Index simple string labels (not JSON objects like L1 columns).
     * Direct string matching on array elements.
     */
    private Map<String, Integer> indexSimpleLabels(JsonNode labels) {
        Map<String, Integer> idx = new HashMap<>();
        if (!labels.isArray()) return idx;
        for (int i = 0; i < labels.size(); i++) {
            String lbl = labels.get(i).asText("").trim().toLowerCase();
            idx.put(lbl, i);
        }
        return idx;
    }

    // ============================================================
    //  Part B4 — Demand & Refund
    // ============================================================

    /**
     * Section key: "demandAndRefund"
     * Uses subSections: [{sectionKey, title, elements[]}]
     */
    private List<DemandRefund> parsePartB4(JsonNode partB) {
        List<DemandRefund> results = new ArrayList<>();

        for (JsonNode section : partB.path("sections")) {
            if (!"demandAndRefund".equals(section.path("sectionKey").asText(""))) continue;

            for (JsonNode sub : section.path("subSections")) {
                if (sub == null || sub.isNull()) continue;

                String subTitle = sub.path("title").asText("");

                for (JsonNode element : sub.path("elements")) {
                    if (element == null || element.isNull()) continue;

                    JsonNode l2 = element.path("l2");
                    JsonNode l1 = element.path("l1");

                    if (!l2.isMissingNode() && l2.has("columnData") && l2.get("columnData").isArray()) {
                        JsonNode l2Data = l2.get("columnData");
                        JsonNode l2Labels = l2.path("columnLabel");

                        for (JsonNode row : l2Data) {
                            if (!row.isArray()) continue;
                            Map<String, Integer> idx = indexSimpleLabels(l2Labels);

                            String ay = safeText(col(row, idx, "assessment year"));
                            String demandStr = safeText(col(row, idx, "demand amount"));
                            String refundStr = safeText(col(row, idx, "refund amount"));
                            String status = safeText(col(row, idx, "status"));

                            DemandRefund dr = DemandRefund.builder()
                                    .assessmentYear(ay.isEmpty() ? subTitle : ay)
                                    .demandAmount((double) parseAmount(demandStr))
                                    .refundAmount((double) parseAmount(refundStr))
                                    .status(status)
                                    .build();
                            results.add(dr);
                            log.info("B4 Demand/Refund: AY={}, Demand={}, Refund={}, Status={}",
                                    ay, demandStr, refundStr, status);
                        }
                    }

                    // Also check L1 for transaction-level data
                    if (!l1.isMissingNode() && l1.has("columnData") && l1.get("columnData").isArray()) {
                        JsonNode l1Labels = l1.path("columnLabel");
                        JsonNode l1Data = l1.get("columnData");
                        Map<String, Integer> colIdx = indexL1Columns(l1Labels);

                        for (JsonNode row : l1Data) {
                            if (!row.isArray()) continue;
                            // Extract transaction-level fields
                            String ay = safeText(col(row, colIdx, "assessmentyear"));
                            String demandStr = safeText(col(row, colIdx, "demandamount"));
                            String refundStr = safeText(col(row, colIdx, "refundamount"));
                            String status = safeText(col(row, colIdx, "status"));

                            if (demandStr.isEmpty() && refundStr.isEmpty()) continue;

                            DemandRefund dr = DemandRefund.builder()
                                    .assessmentYear(ay.isEmpty() ? subTitle : ay)
                                    .demandAmount((double) parseAmount(demandStr))
                                    .refundAmount((double) parseAmount(refundStr))
                                    .status(status)
                                    .build();
                            results.add(dr);
                        }
                    }
                }
            }
        }

        log.info("Part B4: {} demand/refund entries", results.size());
        return results;
    }

    // ============================================================
    //  Part B7 — Other Information
    // ============================================================

    /**
     * Section key: "other-info"
     * Contains misc information per rule 114-I(2)
     */
    private List<OtherInfoEntry> parsePartB7(JsonNode partB) {
        List<OtherInfoEntry> results = new ArrayList<>();

        for (JsonNode section : partB.path("sections")) {
            if (!"other-info".equals(section.path("sectionKey").asText(""))) continue;

            for (JsonNode element : section.path("elements")) {
                if (element == null || element.isNull()) continue;

                String category = element.path("title").asText("");
                String sourceId = element.path("infoSrcId").asText("");

                JsonNode l2 = element.path("l2");
                JsonNode l1 = element.path("l1");

                String entityName = "";
                String description = "";

                if (!l2.isMissingNode() && l2.has("columnData") && l2.get("columnData").isArray()) {
                    JsonNode l2Data = l2.get("columnData");
                    JsonNode l2Labels = l2.path("columnLabel");

                    if (l2Data.size() > 0) {
                        JsonNode l2Row = l2Data.get(0);
                        for (int ci = 0; ci < l2Labels.size() && ci < l2Row.size(); ci++) {
                            String lLabel = l2Labels.get(ci).asText("").toLowerCase();
                            String lVal = safeText(l2Row.get(ci));

                            if (lLabel.contains("source") && !lVal.isEmpty()) {
                                entityName = extractNameFromSource(lVal);
                            } else if (lLabel.contains("description") && !lVal.isEmpty()) {
                                description = lVal;
                            }
                        }
                    }
                }

                if (!entityName.isEmpty() || !description.isEmpty()) {
                    OtherInfoEntry entry = OtherInfoEntry.builder()
                            .sourceName(entityName)
                            .sourceId(extractTAN(sourceId))
                            .category(category)
                            .description(description)
                            .build();
                    results.add(entry);
                    log.info("B7 Other Info: source={}, category={}, desc={}",
                            entityName, category, description);
                }

                // Check L1 for transaction-level other info
                if (!l1.isMissingNode() && l1.has("columnData") && l1.get("columnData").isArray()) {
                    JsonNode l1Labels = l1.path("columnLabel");
                    JsonNode l1Data = l1.get("columnData");
                    Map<String, Integer> colIdx = indexL1Columns(l1Labels);

                    for (JsonNode row : l1Data) {
                        if (!row.isArray()) continue;

                        // Try to extract any fields from the row
                        for (Map.Entry<String, Integer> e : colIdx.entrySet()) {
                            String val = safeText(row.get(e.getValue()));
                            if (!val.isEmpty() && val.length() > 2) {
                                OtherInfoEntry entry = OtherInfoEntry.builder()
                                        .sourceName(entityName)
                                        .sourceId(e.getKey())
                                        .category(category)
                                        .description(val)
                                        .build();
                                results.add(entry);
                            }
                        }
                    }
                }
            }
        }

        log.info("Part B7: {} other info entries", results.size());
        return results;
    }

    // ============================================================
    //  Helper methods
    // ============================================================

    private String getFinancialYear(JsonNode root) {
        JsonNode cd = root.path("header").path("columnData");
        if (cd.isArray() && !cd.isEmpty()) {
            return cd.get(0).asText("").trim();
        }
        return "";
    }

    private String getAssessmentYear(JsonNode root) {
        String fy = getFinancialYear(root);
        if (fy.matches("\\d{4}-\\d{2}")) {
            int y = Integer.parseInt(fy.substring(0, 4)) + 1;
            return y + "-" + String.format("%02d", y % 100);
        }
        return "";
    }

    private String safeText(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return "";
        return node.asText("").trim();
    }

    private JsonNode col(JsonNode row, Map<String, Integer> idx, String field) {
        Integer i = idx.get(field.toLowerCase());
        if (i == null || i < 0 || i >= row.size()) return null;
        return row.get(i);
    }

    private long parseAmount(String str) {
        if (str == null || str.trim().isEmpty()) return 0;
        try {
            String cleaned = str.replaceAll("[,₹\\s]", "");
            return (long) Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private BigDecimal parseBD(String str) {
        if (str == null || str.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            String cleaned = str.replaceAll("[,₹\\s]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate parseDate(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(str.trim(), DATE_FMT);
        } catch (Exception e) {
            try {
                return LocalDate.parse(str.trim(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private String deriveAssetType(String secName, String amcName, String aisAssetType) {
        String sn = secName != null ? secName.toUpperCase() : "";
        String am = amcName != null ? amcName.toUpperCase() : "";

        // Priority 1: security name contains MF/Fund → MUTUAL_FUND
        if (sn.contains("MUTUAL FUND") || sn.contains(" FUND ") || sn.endsWith(" FUND")
                || sn.contains("REITS") || sn.contains("AIF")
                || sn.contains("BALANCED ADVANTAGE") || sn.contains("EQUITY FUND")
                || sn.contains("GROWTH FUND") || sn.contains("INCOME FUND")) {
            return "MUTUAL_FUND";
        }
        // Priority 2: AMC name contains MF indicators
        if (am.contains("MUTUAL") || am.contains("FUND")
                || am.contains("HDFC ASSET") || am.contains("ICICI PRUDENTIAL")
                || am.contains("FRANKLIN") || am.contains("SUNDARAM")
                || am.contains("KOTAK") || am.contains("SBI MF") || am.contains("AXIS AMC")
                || am.contains("NIPPON") || am.contains("ADITYA BIRLA")) {
            return "MUTUAL_FUND";
        }
        // Priority 3: AIS asset type field ("Long term" → LTCG, etc.)
        if (aisAssetType != null) {
            String a = aisAssetType.toUpperCase();
            if (a.contains("LONG")) return "MUTUAL_FUND";
            if (a.contains("SHORT")) return "STCG";
        }
        return "LTCG"; // default to LTCG for listed securities
    }

    private String extractNameFromSource(String source) {
        if (source == null || source.isEmpty()) return "";
        int paren = source.lastIndexOf('(');
        if (paren > 0) {
            return source.substring(0, paren).trim().toUpperCase();
        }
        return source.trim().toUpperCase();
    }

    private String extractTAN(String sourceId) {
        if (sourceId == null || sourceId.isEmpty()) return "";
        if (sourceId.matches("[A-Z]{4}\\d{5}[A-Z]")) return sourceId;
        int dot = sourceId.indexOf('.');
        if (dot > 0) {
            String before = sourceId.substring(0, dot);
            if (before.matches("[A-Z]{4}\\d{5}[A-Z]")) return before;
        }
        return "";
    }

    private String extractSectionCode(String val) {
        if (val == null || val.isEmpty()) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(?:TDS|TCS)-(\\d+[A-Za-z]?)").matcher(val);
        if (m.find()) return m.group(1);
        return "";
    }

    private String detectSectionCode(String category) {
        if (category == null) return "OTHER";
        String c = category.toLowerCase();
        if (c.contains("salary") || c.contains("192")) return "192";
        if (c.contains("interest") && (c.contains("deposit") || c.contains("194a"))) return "194A";
        if (c.contains("194c")) return "194C";
        if (c.contains("194j") || c.contains("professional") || c.contains("royalty")) return "194J";
        if (c.contains("194h") || c.contains("commission")) return "194H";
        if (c.contains("194i") || c.contains("rent")) return "194I";
        if (c.contains("194d") || c.contains("insurance")) return "194D";
        return "OTHER";
    }

    private int size(Object list) {
        if (list == null) return 0;
        if (list instanceof List) return ((List<?>) list).size();
        return 0;
    }
}
