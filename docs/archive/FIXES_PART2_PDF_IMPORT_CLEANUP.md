# ITR ERP — PART 2: PDF Import Logic, Auto-Population & File Cleanup
## For Agentic AI — 26AS / AIS / TIS Extraction + Field Mapping

> **Scope:** Complete rewrite of PDF parsing for 26AS, AIS, and TIS — including
> part-wise extraction logic, field-to-income-head mapping table, reconciliation
> rules, and backend/frontend wiring. All amounts in INR. AY 2025-26 context.
>
> **Reference PDFs analysed:**
> - 26AS (TRACES Annual Tax Statement) — `ACUPG3482G-2025.pdf`
> - AIS (Annual Information Statement) — `AIS_SAMPLE.pdf`
> - TIS (Taxpayer Information Summary) — `TIS_sample.pdf`

---

## 1. PDF PASSWORD DECRYPTION (All Three Documents)

### File: `src/main/java/com/itr/util/ITDPdfDecryptor.java`

All three ITD PDFs share the same password scheme:

```java
/**
 * Password format: {pan_lowercase}{ddmmyyyy}
 * Example: PAN = ACUPG3482G, DOB = 14-Jun-1974
 *          Password = "acupg3482g14061974"
 *
 * IMPORTANT: DOB is from the CLIENT MASTER — never ask user to type it again.
 * Always retrieve from DB: clientRepository.findById(clientId).getDob()
 */
public String generateITDPassword(String pan, LocalDate dob) {
    String panLower = pan.toLowerCase().trim();
    String ddobStr = dob.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
    return panLower + ddobStr;
}

public String decryptAndExtractText(byte[] pdfBytes, String password)
        throws IOException {
    try (PDDocument doc = PDDocument.load(pdfBytes,
            password, MemoryUsageSetting.setupTempFileOnly())) {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        return stripper.getText(doc);
    }
}
```

---

## 2. FORM 26AS — COMPLETE PART-WISE EXTRACTION

### File: `src/main/java/com/itr/service/integration/Form26ASImportService.java`

The 26AS PDF has a specific structure. Parse it PART-WISE as described below.
The key insight: **always use the SUMMARY ROW** (the row with the deductor's name and TAN
showing total amounts) to get NET TDS, not the individual transaction rows.
The individual rows exist for audit purposes and contain "G" (Reprocessing) reversal entries
that cancel each other. The summary is the definitive net figure.

### 26AS Structure Map:

```
PART-I   → TDS on Income from Salary & Other than Salary (Section 192, 194A, 194C, etc.)
PART-II  → TDS where Form 15G/15H submitted (usually empty for salaried)
PART-III → Section 194B/194R/194S winnings/VDA (usually empty)
PART-IV  → TDS u/s 194IA/194IB/194M/194S (for Seller/Landlord — property transactions)
PART-V   → VDA seller (Form 26QE) — usually empty
PART-VI  → TCS (Tax Collected at Source)
PART-VII → Refunds issued by CPC-TDS
PART-VIII→ TDS u/s 194IA/194IB (Buyer of property — only if assessee bought property)
PART-X   → TDS/TCS Defaults (processing defaults — informational only)
```

### Parsing Logic for PART-I (Most Important):

```java
/**
 * PART-I: TDS on Salary and Other Income
 *
 * The PDF text layout (after extraction):
 * Line contains deductor name, TAN, and three totals:
 * "STATE BANK OF INDIA   MUMS89569E   224329.00   22443.00   22443.00"
 * (Deductor Name)        (TAN)        (Amt Paid)  (TDS Ded)  (TDS Dep)
 *
 * Then detail rows follow — SKIP individual rows marked "G" (Reprocessing).
 * USE ONLY THE SUMMARY ROW for amounts.
 *
 * Section detection:
 * - If section = 192  → Salary TDS → populate tds192 (Schedule TDS1)
 * - If section = 194A → Interest TDS → populate tds194A (Schedule TDS2)
 * - If section = 194C → Contractor TDS → populate tds194C (Schedule TDS2)
 * - If section = 194D → Insurance commission TDS
 * - If section = 192A → PF withdrawal TDS
 */

public List<TDSEntry26AS> parsePartI(String pdfText) {
    List<TDSEntry26AS> entries = new ArrayList<>();

    // Regex to match summary (deductor) rows in Part-I:
    // Format: <Deductor Name> <TAN 10-char> <Amount> <TDS Deducted> <TDS Deposited>
    Pattern deductorPattern = Pattern.compile(
        "([A-Z][A-Z\\s&\\.,'()-]{2,50})\\s+([A-Z]{4}\\d{5}[A-Z])\\s+" +
        "([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})"
    );

    // Section detection — look BEFORE each deductor block:
    Pattern sectionPattern = Pattern.compile(
        "(?:Section|Sec\\.)\\s*(\\d{2,3}[A-Z]?(?:\\([a-zA-Z]\\))?)"
    );

    Matcher matcher = deductorPattern.matcher(pdfText);
    while (matcher.find()) {
        String deductorName = matcher.group(1).trim();
        String tan = matcher.group(2);
        BigDecimal amtPaid = parseAmount(matcher.group(3));
        BigDecimal tdsDeducted = parseAmount(matcher.group(4));
        BigDecimal tdsDeposited = parseAmount(matcher.group(5));

        // Skip if this looks like a detail row (amounts are negative = reversal)
        if (amtPaid.compareTo(BigDecimal.ZERO) < 0) continue;

        // Detect section from surrounding text context
        String surrounding = pdfText.substring(
            Math.max(0, matcher.start() - 200), matcher.start()
        );
        String section = detectSection(surrounding);

        TDSEntry26AS entry = new TDSEntry26AS();
        entry.setDeductorName(deductorName);
        entry.setTan(tan);
        entry.setAmountPaid(amtPaid);
        entry.setTaxDeducted(tdsDeducted);
        entry.setTaxDeposited(tdsDeposited);
        entry.setSection(section);
        entries.add(entry);
    }
    return entries;
}

private String detectSection(String contextText) {
    if (contextText.contains("192A")) return "192A";
    if (contextText.contains("194A")) return "194A";
    if (contextText.contains("194C")) return "194C";
    if (contextText.contains("194D")) return "194D";
    if (contextText.contains("194I")) return "194I";
    if (contextText.contains("194J")) return "194J";
    if (contextText.contains("194H")) return "194H";
    if (contextText.contains("192"))  return "192";
    return "OTHER";
}
```

### Parsing PART-IV (Property Transactions — 194IA):

```java
/**
 * PART-IV: TDS on property sale (SELLER side)
 * If assessee SOLD a property during the year, buyer deducted 1% TDS u/s 194IA.
 * This TDS credit belongs to the assessee.
 * Extract: Acknowledgement number, deductor (buyer) name/PAN, transaction amount, TDS.
 */
public List<PropertyTDS26AS> parsePartIV(String pdfText) {
    List<PropertyTDS26AS> entries = new ArrayList<>();
    // Locate "PART-IV" section in text
    int partIVStart = pdfText.indexOf("PART-IV");
    int partIVEnd = pdfText.indexOf("PART-V", partIVStart);
    if (partIVStart == -1) return entries;

    String partIVText = pdfText.substring(partIVStart,
        partIVEnd != -1 ? partIVEnd : pdfText.length());

    if (partIVText.contains("No Transactions Present")) return entries;

    // Extract acknowledgement, PAN, amount, TDS rows
    Pattern propPattern = Pattern.compile(
        "(\\d{15})\\s+([A-Z ]{5,50})\\s+([A-Z]{5}\\d{4}[A-Z])\\s+" +
        "(\\d{2}-[A-Za-z]{3}-\\d{4})\\s+([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})"
    );
    Matcher m = propPattern.matcher(partIVText);
    while (m.find()) {
        PropertyTDS26AS entry = new PropertyTDS26AS();
        entry.setAcknowledgementNo(m.group(1));
        entry.setBuyerName(m.group(2).trim());
        entry.setBuyerPAN(m.group(3));
        entry.setTransactionDate(parseDate(m.group(4)));
        entry.setTransactionAmount(parseAmount(m.group(5)));
        entry.setTdsDeposited(parseAmount(m.group(6)));
        entries.add(entry);
    }
    return entries;
}
```

### Parsing PART-VII (Refunds):

```java
/**
 * PART-VII: Refunds paid to assessee by Income Tax Dept
 * Important: If a refund was received, the INTEREST component (244A) is taxable
 * and must be included in "Other Sources" income.
 */
public List<RefundEntry26AS> parsePartVII(String pdfText) {
    // Find "PART-VII" section, extract AY, nature, refund amount, interest, date
    // Interest on IT refund → feeds into OtherSources.interestOnITRefund
}
```

---

## 3. AIS — COMPLETE PART-WISE EXTRACTION

### File: `src/main/java/com/itr/service/integration/AISImportService.java`

The AIS PDF has two sections:
- **Part A**: General information (PAN, name, DOB, address)
- **Part B**: Financial transaction details (B1 through B7)

### AIS Structure Map:

```
Part A  → General Info (PAN, Aadhaar, Name, DOB, Mobile, Email, Address)
Part B1 → Tax Deducted/Collected at Source (TDS/TCS transactions)
           Sub-types: TDS-192 (Salary), TDS-194A (Interest), TDS-194C, etc.
Part B2 → Specified Financial Transactions (SFT)
           Sub-types: SFT-015 (Dividend), SFT-17 (Securities), SFT-18 (MF), etc.
Part B3 → Payment of Taxes (Self-Assessment, Advance Tax challan details)
Part B4 → Demand and Refund
Part B5 → Other Information (Foreign remittances, turnover, etc.)
Part B6 → Interest / Dividend from SFT in SPECIFIC formats
Part B7 → Information u/s 114-I (high-value transactions — informational only)
```

### Part A — Extract General Info:

```java
public AISGeneralInfo parsePartA(String pdfText) {
    AISGeneralInfo info = new AISGeneralInfo();

    // PAN: 10-char alphanumeric
    Pattern panPattern = Pattern.compile("([A-Z]{5}\\d{4}[A-Z])");
    Matcher pm = panPattern.matcher(pdfText.substring(0, Math.min(500, pdfText.length())));
    if (pm.find()) info.setPan(pm.group(1));

    // Aadhaar: "XXXX XXXX XXXX" or masked
    Pattern aadhaarPattern = Pattern.compile("(\\d{4}\\s\\d{4}\\s\\d{4}|XXXX\\sXXXX\\s\\d{4})");
    Matcher am = aadhaarPattern.matcher(pdfText.substring(0, 500));
    if (am.find()) info.setAadhaar(am.group(1));

    // DOB: DD/MM/YYYY
    Pattern dobPattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})");
    // Name: Line after "Name of Assessee" label

    // Mobile: 10 digits starting with 6-9
    Pattern mobilePattern = Pattern.compile("\\b([6-9]\\d{9})\\b");

    // Email: standard email regex
    Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    // Address: Line(s) after "Address"
    return info;
}
```

### Part B1 — TDS/TCS Transactions (CRITICAL):

```java
/**
 * Part B1 contains TDS entries grouped by section code and deductor.
 *
 * Layout in PDF text:
 * "TDS-194A"
 * "Interest other than 'Interest on Securities' received (Section 194A)"
 * "ANAND PURUSHOTTAM AGRAWAL (NGPA14339D)"    ← Deductor Name (TAN)
 * SR NO | QUARTER | DATE | AMOUNT PAID | TDS DEDUCTED | TDS DEPOSITED | STATUS
 * "1  Q3(Oct-Dec)  31/12/2025  15,000  1,500  1,500  Active"
 * "2  Q2(Jul-Sep)  30/09/2025  30,000  3,000  3,000  Active"
 *
 * IMPORTANT: AIS deduplicates entries (vs 26AS which shows raw G-entries).
 * The "Reported by Source" column shows what the deductor reported.
 * The "Processed by System" column shows after ITD deduplication.
 * USE "Processed by System" values for tax computation.
 *
 * NOTE on AIS vs 26AS discrepancy:
 * AIS for SBI shows ₹1,410 (only confirmed/matched transactions).
 * 26AS for SBI shows ₹2,24,329 total (all transactions including pending matches).
 * Per ITD guidance: "Taxpayer may rely on TRACES (26AS) for filing purposes."
 * STRATEGY: Extract from BOTH, show the discrepancy to the user in Reconciliation,
 * and USE 26AS figures for TDS credit (more conservative, TRACES-backed).
 */

public List<AISTDSEntry> parsePartB1(String pdfText) {
    List<AISTDSEntry> entries = new ArrayList<>();

    // Split text into sections by TDS code (TDS-192, TDS-194A, etc.)
    String[] sections = pdfText.split("(?=TDS-\\d{3}[A-Z]?)");

    for (String section : sections) {
        String sectionCode = extractSectionCode(section); // e.g., "194A"
        String deductorLine = extractDeductorLine(section); // "ANAND... (NGPA14339D)"
        String deductorName = extractName(deductorLine);
        String deductorTAN = extractTAN(deductorLine); // In parentheses

        // Extract table rows: Quarter, Date, Amount, TDS, Status
        Pattern rowPattern = Pattern.compile(
            "(\\d+)\\s+(Q[1-4]\\([A-Za-z-]+\\))\\s+(\\d{2}/\\d{2}/\\d{4})" +
            "\\s+([\\d,]+)\\s+([\\d,]+)\\s+([\\d,]+)\\s+(Active|Inactive)"
        );
        Matcher rm = rowPattern.matcher(section);
        long totalAmtPaid = 0, totalTDSDeducted = 0;
        while (rm.find()) {
            totalAmtPaid += parseAmountLong(rm.group(4));
            totalTDSDeducted += parseAmountLong(rm.group(5));
        }

        if (totalAmtPaid > 0) {
            AISTDSEntry entry = new AISTDSEntry();
            entry.setSection(sectionCode);
            entry.setDeductorName(deductorName);
            entry.setDeductorTAN(deductorTAN);
            entry.setTotalAmountPaid(totalAmtPaid);
            entry.setTotalTDSDeducted(totalTDSDeducted);
            entries.add(entry);
        }
    }
    return entries;
}
```

### Part B2 — Specified Financial Transactions (SFT):

```java
/**
 * Part B2 SFT codes and what they map to in ITR:
 *
 * SFT-001: Cash deposits in SB accounts > ₹10L     → Informational
 * SFT-002: Cash deposits in other accounts > ₹50L  → Informational
 * SFT-003: Cash purchase of bank drafts/prepaid     → Informational
 * SFT-004: Cash purchase of foreign currency        → Informational
 * SFT-005: Purchase of mutual funds > ₹10L         → Informational
 * SFT-006: Purchase of shares > ₹10L               → Informational
 * SFT-007: Credit card payment > ₹10L              → Informational
 * SFT-008: Purchase of debentures/bonds > ₹10L     → Informational
 * SFT-009: Cash deposits in FD > ₹10L              → Informational
 * SFT-010: Acquisition of immovable property > ₹30L → Informational
 * SFT-011: Sale of immovable property > ₹30L        → Capital Gains (CG tab)
 * SFT-012: Receipt of cash for sale of goods        → Business income
 * SFT-013: Cash receipt for sale of services        → Business income
 * SFT-014: Rent received > ₹2.4L p.a.              → House Property income
 * SFT-015: Dividend income                          → Other Sources (dividend)
 * SFT-016: Interest on securities                   → Other Sources
 * SFT-017: Sale of securities (equity/MF/bonds)     → Capital Gains
 * SFT-018: Purchase of mutual funds                 → Capital Gains (cost basis)
 * SFT-019: Purchase of immovable property           → Capital Gains (cost basis)
 * SFT-020: Cash payment for goods/services          → Informational
 * SFT-021: Interest income from bank/post office    → Other Sources (interest)
 */

public AISPartB2Data parsePartB2(String pdfText) {
    AISPartB2Data data = new AISPartB2Data();

    // SFT-015: Dividend
    data.setDividendIncome(extractSFTAmount(pdfText, "SFT-015", "SFT-15",
        "Dividend income"));

    // SFT-017: Sale of securities (listed equity)
    data.setSecuritiesSaleConsideration(extractSFTSaleDetails(pdfText,
        "SFT-17-LES", "Sale of listed equity share"));
    // Fields: security name, ISIN, quantity, sale price per unit, total sale consideration,
    //         cost of acquisition, FMV, indexed cost, date of transfer

    // SFT-017 (Purchase): Cost basis for securities
    data.setSecuritiesPurchaseAmount(extractSFTAmount(pdfText,
        "SFT-17(Pur)", "SFT-17-Pur", "Purchase of securities"));

    // SFT-018 (Purchase): Mutual fund purchase
    List<MFPurchase> mfPurchases = extractMFPurchases(pdfText);
    // Each MF entry: AMC name, AMC code, quarter, total purchase, total sales value
    data.setMutualFundPurchases(mfPurchases);

    // SFT-011: Immovable property sale
    data.setPropertySaleAmount(extractSFTAmount(pdfText, "SFT-011", "SFT-11",
        "Sale of immovable property"));

    // SFT-016: Interest on securities
    data.setInterestOnSecurities(extractSFTAmount(pdfText, "SFT-016", "SFT-16",
        "Interest on securities"));

    return data;
}

/**
 * Extract sale details for listed equity (SFT-17-LES):
 * This entry contains RICH data for capital gains computation:
 * - Security Name, Security Code (ISIN)
 * - Security Class (Listed Equity Share)
 * - Debit Type (Market/Off-Market)
 * - Credit Type
 * - Asset Type (Short-term/Long-term)
 * - Quantity
 * - Sale Price per unit
 * - Sales Consideration (total)
 * - Cost of Acquisition
 * - Unit FMV (Fair Market Value as on 31-Jan-2018 for grandfathering)
 * - Indexed Cost of Acquisition
 * - Date of Sale/Transfer
 *
 * AUTO-POPULATE TO: Capital Gains tab (STCG/LTCG based on asset type in AIS)
 */
private SFTSaleEntry extractSFTSaleDetails(String pdfText, String... codes) {
    for (String code : codes) {
        int idx = pdfText.indexOf(code);
        if (idx == -1) continue;

        String block = pdfText.substring(idx, Math.min(idx + 2000, pdfText.length()));

        // Extract from table row:
        // DATE | SECURITY NAME (CODE) | CLASS | DEBIT | CREDIT | TYPE | QTY |
        // SALE PRICE | SALES CONSID | COST | FMV | INDEXED COST | STATUS
        Pattern salePattern = Pattern.compile(
            "(\\d{2}/\\d{2}/\\d{4})\\s+" +
            "([A-Z0-9\\s]+(?:\\(INE[A-Z0-9]+\\))?)\\s+" +
            "(?:Listed Equity Share)\\s+" +
            "(?:Market)\\s+(?:Market)\\s+" +
            "(Short-?term|Long-?term)\\s+" +
            "([\\d.]+)\\s+" +      // Quantity
            "([\\d,]+\\.\\d+)\\s+" + // Sale price per unit
            "([\\d,]+)\\s+" +     // Sales consideration
            "([\\d,]+)\\s+" +     // Cost of acquisition
            "([\\d,]+\\.\\d+)\\s+" + // Unit FMV
            "([\\d,]+\\.\\d+)\\s+" + // FMV total
            "([\\d,]+)"            // Indexed cost
        );
        Matcher m = salePattern.matcher(block);
        if (m.find()) {
            SFTSaleEntry entry = new SFTSaleEntry();
            entry.setTransferDate(parseDate(m.group(1)));
            entry.setSecurityName(m.group(2).trim());
            entry.setAssetType(m.group(3).contains("Short") ? "STCG" : "LTCG");
            entry.setQuantity(new BigDecimal(m.group(4)));
            entry.setSalePricePerUnit(parseAmount(m.group(5)));
            entry.setSalesConsideration(parseAmount(m.group(6)));
            entry.setCostOfAcquisition(parseAmount(m.group(7)));
            entry.setFmvPerUnit(parseAmount(m.group(8)));
            entry.setIndexedCostOfAcquisition(parseAmount(m.group(10)));
            return entry;
        }
    }
    return null;
}
```

### Part B3 — Tax Payments:

```java
/**
 * Part B3: Tax payments made by assessee (Self-Assessment + Advance Tax)
 *
 * Format in PDF:
 * SR NO | FINANCIAL YEAR | MAJOR HEAD | MINOR HEAD | TAX(A) | SURCHARGE(B) |
 * EDUCATION CESS(C) | OTHERS(D) | TOTAL(A+B+C+D) | BSR CODE | DATE OF DEPOSIT |
 * CHALLAN SERIAL NO | CHALLAN IDENTIFICATION NUMBER
 *
 * Minor Head:
 * - "Self Assessment" (300) → Self-Assessment Tax (SAT)
 * - "Advance Tax" (100)     → Advance Tax
 */
public List<TaxPaymentAIS> parsePartB3(String pdfText) {
    List<TaxPaymentAIS> payments = new ArrayList<>();
    int b3Start = pdfText.indexOf("Part B3");
    if (b3Start == -1) b3Start = pdfText.indexOf("B3-Information relating to payment of taxes");
    if (b3Start == -1) return payments;

    int b3End = pdfText.indexOf("Part B4", b3Start);
    String b3Text = pdfText.substring(b3Start, b3End != -1 ? b3End : pdfText.length());

    if (b3Text.contains("No Transactions Present")) return payments;

    Pattern taxPayPattern = Pattern.compile(
        "(\\d+)\\s+(\\d{4}-\\d{2})\\s+" +  // SR, FY
        "(Income Tax[^\\n]+?)\\s+" +           // Major head
        "(Self Assessment|Advance Tax)\\s+" +  // Minor head
        "([\\d,]+)\\s+" +   // Tax (A)
        "(\\d+)\\s+" +       // Surcharge (B)
        "([\\d,]+)\\s+" +   // Education Cess (C)
        "([\\d,]+)\\s+" +   // Others (D)
        "([\\d,]+)\\s+" +   // Total
        "(\\d{7})\\s+" +    // BSR Code
        "(\\d{2}/\\d{2}/\\d{4})\\s+" + // Date
        "(\\d{5})"           // Challan Serial
    );
    Matcher m = taxPayPattern.matcher(b3Text);
    while (m.find()) {
        TaxPaymentAIS payment = new TaxPaymentAIS();
        payment.setFinancialYear(m.group(2));
        payment.setMinorHead(m.group(4)); // "Self Assessment" or "Advance Tax"
        payment.setTaxAmount(parseAmountLong(m.group(5)));
        payment.setSurcharge(parseAmountLong(m.group(6)));
        payment.setEducationCess(parseAmountLong(m.group(7)));
        payment.setTotalAmount(parseAmountLong(m.group(9)));
        payment.setBsrCode(m.group(10));
        payment.setDepositDate(parseDate(m.group(11)));
        payment.setChallanSerialNo(m.group(12));
        payments.add(payment);
    }
    return payments;
}
```

---

## 4. TIS — PARSING LOGIC

### File: `src/main/java/com/itr/service/integration/TISImportService.java` (NEW FILE)

The TIS is a SUMMARY document. It shows the reconciled/verified amounts after ITD processing.
Use TIS as the "source of truth" for amounts visible to the assessee (accepted by taxpayer/source).

```java
/**
 * TIS Structure:
 * 1. General Information (same as AIS Part A)
 * 2. Summary Table:
 *    SR NO | INFORMATION CATEGORY | PROCESSED BY SYSTEM | ACCEPTED BY TAXPAYER/SOURCE
 *    1. Dividend
 *    2. Interest from deposit
 *    3. Sale of securities and units of mutual fund
 *    4. Purchase of securities and units of mutual funds
 *
 * 3. Annexure (detailed breakdown per category with sources)
 *
 * IMPORTANT: "Accepted by Taxpayer / Confirmed by Source" is the column to use.
 * It represents the final agreed amount after any taxpayer feedback.
 */
public TISData parseTIS(String pdfText) {
    TISData data = new TISData();

    // Extract summary table
    // Look for rows with pattern: number, category text, amount1, amount2
    Pattern summaryRow = Pattern.compile(
        "(\\d+)\\s+([A-Za-z][A-Za-z\\s]+?)\\s+([\\d,]+)\\s+([\\d,]+)"
    );

    int summaryStart = pdfText.indexOf("INFORMATION CATEGORY");
    int summaryEnd = pdfText.indexOf("The information details", summaryStart);
    if (summaryStart == -1) return data;

    String summaryText = pdfText.substring(summaryStart,
        summaryEnd != -1 ? summaryEnd : summaryStart + 2000);

    Matcher m = summaryRow.matcher(summaryText);
    while (m.find()) {
        String category = m.group(2).trim().toLowerCase();
        long acceptedAmount = parseAmountLong(m.group(4)); // Use "Accepted by Taxpayer" column

        if (category.contains("dividend")) {
            data.setDividendIncome(acceptedAmount);
        } else if (category.contains("interest from deposit")) {
            data.setInterestFromDeposit(acceptedAmount);
        } else if (category.contains("sale of securities")) {
            data.setSecuritiesSaleConsideration(acceptedAmount);
        } else if (category.contains("purchase of securities")) {
            data.setSecuritiesPurchaseAmount(acceptedAmount);
        } else if (category.contains("interest on securities")) {
            data.setInterestOnSecurities(acceptedAmount);
        } else if (category.contains("salary")) {
            data.setSalaryAmount(acceptedAmount);
        } else if (category.contains("rent")) {
            data.setRentIncome(acceptedAmount);
        }
    }

    // Parse Annexure for source-level breakdown (for reconciliation)
    parseTISAnnexure(pdfText, data);
    return data;
}
```

---

## 5. AUTO-POPULATION MAPPING TABLE (26AS + AIS + TIS → ITR Fields)

### File: `src/main/java/com/itr/service/integration/AutoPopulationService.java`

This is the MASTER MAPPING. For each extracted field, the target ITR form field is specified.

```
╔══════════════════════════════════════════════════════════════════════════════════════╗
║              AUTO-POPULATION MAPPING TABLE — AY 2025-26                            ║
╠══════════════╦═══════════════════════════╦═════════════════════════════════════════╣
║ SOURCE       ║ EXTRACTED FIELD           ║ TARGET ITR FIELD                        ║
╠══════════════╬═══════════════════════════╬═════════════════════════════════════════╣
║ AIS Part A   ║ PAN                       ║ PersonalInfo.pan (read-only, validate)  ║
║ AIS Part A   ║ Aadhaar (masked)          ║ PersonalInfo.aadhaar (display only)     ║
║ AIS Part A   ║ Name                      ║ PersonalInfo.name (read-only)           ║
║ AIS Part A   ║ DOB                       ║ PersonalInfo.dob (read-only)            ║
║ AIS Part A   ║ Mobile                    ║ PersonalInfo.mobile                     ║
║ AIS Part A   ║ Email                     ║ PersonalInfo.email                      ║
║ AIS Part A   ║ Address                   ║ PersonalInfo.address (pre-fill only)    ║
╠══════════════╬═══════════════════════════╬═════════════════════════════════════════╣
║ 26AS Part-I  ║ TDS u/s 192 Entries       ║ TDSTab.tds192Entries[] (Schedule TDS1) ║
║              ║   Deductor Name           ║   .employerName                         ║
║              ║   TAN                     ║   .employerTAN                          ║
║              ║   Amount Paid (Salary)    ║   SalaryTab.grossSalary (editable)      ║
║              ║   TDS Deducted            ║   .tdsDeducted                          ║
╠══════════════╬═══════════════════════════╬═════════════════════════════════════════╣
║ 26AS Part-I  ║ TDS u/s 194A Entries      ║ TDSTab.tds194AEntries[] (Schedule TDS2)║
║              ║   Deductor Name           ║   .deductorName                         ║
║              ║   TAN                     ║   .deductorTAN                          ║
║              ║   Total Amount Paid       ║   OtherSources.fdInterest (additive)    ║
║              ║   TDS Deposited (NET)     ║   .tdsDeducted                          ║
╠══════════════╬═══════════════════════════╬═════════════════════════════════════════╣
║ AIS Part B1  ║ TDS-194A Interest         ║ OtherSources.fdInterest (for income)    ║
║              ║   (Processed by System)   ║   [AIS amount — for income side]        ║
║              ║   TDS deducted (AIS)      ║   [Show in reconciliation vs 26AS]      ║
╠══════════════╬═══════════════════════════╬═════════════════════════════════════════╣
║ TIS Row 2    ║ Interest from deposit     ║ OtherSources.totalInterestIncome        ║
║              ║   (Accepted by Source)    ║   [TIS is reconciled — use this]        ║
╠══════════════╬═══════════════════════════╬═════════════════════════════════════════╣
║ AIS Part B2  ║ SFT-015 Dividend          ║ OtherSources.dividendIncome             ║
║ TIS Row 1    ║ Dividend (Accepted)       ║   [Use TIS accepted amount]             ║
╠══════════════╬═══════════════════════════╬═════════════════════════════════════════╣
║ AIS Part B2  ║ SFT-17-LES Sale Entries   ║ CapitalGains.ltcg112APost (if LTCG)    ║
║              ║   Transfer Date           ║   or CapitalGains.stcgEquityPost        ║
║              ║   Sales Consideration     ║   [Asset type from AIS: Short/Long-term]║
║              ║   Cost of Acquisition     ║   [Auto-compute: Consid - Cost = Gain]  ║
║              ║   FMV (31-Jan-2018)       ║   [For grandfathering pre-Jul-2024]     ║
║              ║   Indexed Cost            ║   [For 112 computation]                 ║
╠══════════════╬═══════════════════════════╬═════════════════════════════════════════╣
║ AIS Part B2  ║ SFT-17(Pur) Purchase      ║ [Cost basis — for capital gains]        ║
║              ║   Securities purchase     ║   Store in CG cost field, not income    ║
╠══════════════╬═══════════════════════════╬═════════════════════════════════════════╣
║ AIS Part B2  ║ SFT-18(Pur) MF Purchase   ║ [Cost basis for MF capital gains]       ║
║              ║   AMC Name                ║   Store per-AMC for reconciliation       ║
║              ║   Total Purchase Amount   ║   Not directly in ITR — cost basis       ║
╠══════════════╬═══════════════════════════╬═════════════════════════════════════════╣
║ AIS Part B3  ║ Self Assessment Tax       ║ TDSTab.selfAssessmentTax                 ║
║              ║   Tax Amount              ║   .selfAssessmentTaxAmount               ║
║              ║   Surcharge               ║   [Informational — include in total]     ║
║              ║   BSR Code                ║   TDSTab.satBSRCode (MANDATORY)          ║
║              ║   Date of Deposit         ║   TDSTab.satDate                         ║
║              ║   Challan Serial No       ║   TDSTab.satChallanNo (MANDATORY)        ║
╠══════════════╬═══════════════════════════╬═════════════════════════════════════════╣
║ AIS Part B3  ║ Advance Tax Payments      ║ TDSTab.advTaxEntries[]                  ║
║              ║   Per instalment amounts  ║   .advTax_jun / _sep / _dec / _mar       ║
║              ║   BSR Code                ║   .advTaxBSRCode[]                       ║
║              ║   Challan No              ║   .advTaxChallanNo[]                     ║
╠══════════════╬═══════════════════════════╬═════════════════════════════════════════╣
║ 26AS Part-IV ║ Property Sale TDS (194IA) ║ TDSTab.tds194IAEntries[] (Schedule TDS2)║
║              ║   Buyer Name              ║   .buyerName                             ║
║              ║   Buyer PAN               ║   .buyerPAN                              ║
║              ║   Transaction Amount      ║   → CapitalGains (property sale consid.) ║
║              ║   TDS Deposited           ║   .tdsDeducted (TDS credit)              ║
╠══════════════╬═══════════════════════════╬═════════════════════════════════════════╣
║ 26AS Part-VII║ IT Refund Interest (244A) ║ OtherSources.interestOnITRefund          ║
║              ║   Interest Amount         ║   [Taxable — must be declared]           ║
╚══════════════╩═══════════════════════════╩═════════════════════════════════════════╝
```

---

## 6. RECONCILIATION: AIS vs 26AS DISCREPANCY HANDLING

### File: `src/main/java/com/itr/service/ReconciliationService.java`
### Frontend: `src/pages/ReconciliationPage.tsx`

**Critical rule for TDS discrepancy (as in the reference PDFs):**

```java
/**
 * RECONCILIATION RULES:
 *
 * 1. FOR INCOME (What to include as income):
 *    - Use AIS "Processed by System" values for interest/dividend income.
 *    - AIS has better deduplication. TIS "Accepted" values = same.
 *    - If AIS and 26AS income differ: Show alert, let user choose.
 *      (e.g., SBI interest: AIS shows ₹1,410; 26AS shows ₹2,24,329 — user must verify)
 *
 * 2. FOR TDS CREDIT (What to claim as TDS credit):
 *    - USE 26AS (TRACES) values for TDS credit claimed in ITR.
 *    - Per ITD circular: "Taxpayer may rely on TRACES portal for filing purposes."
 *    - 26AS shows NET TDS deposited (summary row) — this is the authoritative figure.
 *    - DO NOT use AIS TDS amounts for Schedule TDS2 — they may differ.
 *
 * 3. RECONCILIATION ALERT when:
 *    - AIS interest income ≠ 26AS total amount paid (for same deductor/TAN)
 *    - AIS TDS ≠ 26AS TDS deposited (for same deductor/TAN)
 *
 * IMPORTANT: The ₹2,24,329 in 26AS from SBI represents gross interest across
 * ALL FD/RD/TD accounts. The ₹1,410 in AIS may be just one confirmed transaction.
 * The correct INCOME to declare = ₹2,24,329 (from 26AS, or actual interest credited,
 * which the assessee should verify with bank statement / Form 16A).
 */

public ReconciliationReport generateReconciliationReport(
        Form26ASData data26AS, AISData dataAIS, TISData dataTIS) {

    ReconciliationReport report = new ReconciliationReport();

    // Check TDS-194A: Compare 26AS vs AIS for each deductor (match by TAN)
    for (TDSEntry26AS entry26AS : data26AS.getPartIEntries()) {
        if (!"194A".equals(entry26AS.getSection())) continue;

        AISTDSEntry aisEntry = dataAIS.findByTAN(entry26AS.getTan());
        if (aisEntry == null) {
            report.addMissing26AS(entry26AS); // In 26AS but not AIS
            continue;
        }

        long diff_income = entry26AS.getAmountPaid() - aisEntry.getTotalAmountPaid();
        long diff_tds = entry26AS.getTaxDeposited() - aisEntry.getTotalTDSDeducted();

        if (Math.abs(diff_income) > 0 || Math.abs(diff_tds) > 0) {
            report.addDiscrepancy(new Discrepancy(
                entry26AS.getDeductorName(),
                entry26AS.getTan(),
                entry26AS.getAmountPaid(),    // 26AS amount
                aisEntry.getTotalAmountPaid(), // AIS amount
                entry26AS.getTaxDeposited(),   // 26AS TDS (USE THIS for credit)
                aisEntry.getTotalTDSDeducted() // AIS TDS (show for info)
            ));
        }
    }

    return report;
}
```

### Frontend — Reconciliation Page Enhancement:

```typescript
// ReconciliationPage.tsx — show the discrepancy table clearly:
// Column headers: Deductor | TAN | Income (26AS) | Income (AIS) | TDS (26AS) | TDS (AIS) | Diff | Action
// Action options:
//   "Use 26AS" → populate ITR with 26AS figures (RECOMMENDED)
//   "Use AIS"  → populate with AIS figures
//   "Manual"   → user enters custom amount
// Default: "Use 26AS" pre-selected (as per ITD guidance)

// ALSO: Show a banner at top:
// "⚠️ We found discrepancies between your 26AS and AIS. Per Income Tax Department 
//  guidelines, 26AS (TRACES) values should be used for TDS credit claims.
//  Income figures should be verified with your bank statements / Form 16A."
```

---

## 7. AUTO-POPULATION SERVICE — REVISED COMPLETE IMPLEMENTATION

### File: `src/main/java/com/itr/service/integration/AutoPopulationService.java`

Replace the existing `autoPopulateFromAIS()` method with this comprehensive version:

```java
/**
 * Auto-populates form data from all three documents combined.
 * Priority: TIS (accepted) > AIS (processed) > 26AS (for TDS credit)
 *
 * Call after importing ALL available documents, not just AIS.
 */
public FlatFormData autoPopulateAll(AISData ais, Form26ASData f26as, TISData tis,
                                     String itrType) {
    FlatFormData flat = new FlatFormData();

    // === PERSONAL INFO ===
    if (ais != null && ais.getGeneralInfo() != null) {
        flat.put("mobile", ais.getGeneralInfo().getMobile());
        flat.put("email", ais.getGeneralInfo().getEmail());
        // Address: pre-fill but mark as editable (may have changed)
        flat.put("address", ais.getGeneralInfo().getAddress());
    }

    // === OTHER SOURCES — INTEREST INCOME ===
    // Use TIS "Accepted" amount if available, else AIS "Processed" total
    long interestFromDeposit = 0;
    if (tis != null && tis.getInterestFromDeposit() > 0) {
        interestFromDeposit = tis.getInterestFromDeposit(); // e.g., ₹46,410 from TIS
    } else if (ais != null) {
        interestFromDeposit = ais.getTotalInterest194A(); // Sum of all AIS TDS-194A amounts
    }
    flat.put("totalInterestIncome", String.valueOf(interestFromDeposit));
    // Split: populate fdInterest with total; sbInterest should come from bank passbook
    flat.put("fdInterest", String.valueOf(interestFromDeposit));

    // === OTHER SOURCES — DIVIDEND ===
    long dividend = 0;
    if (tis != null) dividend = tis.getDividendIncome();
    else if (ais != null) dividend = ais.getDividendIncome();
    flat.put("dividendIncome", String.valueOf(dividend));

    // === CAPITAL GAINS — from AIS SFT-17 sale details ===
    if (ais != null && ais.getPartB2() != null) {
        AISPartB2Data b2 = ais.getPartB2();
        if (b2.getSecuritiesSaleEntry() != null) {
            SFTSaleEntry sale = b2.getSecuritiesSaleEntry();
            long gain = sale.getSalesConsideration().longValue()
                      - sale.getCostOfAcquisition().longValue();

            // Determine if STCG or LTCG based on AIS asset type field:
            if ("LTCG".equals(sale.getAssetType())) {
                // Date of sale determines pre/post-Jul-2024 rate:
                if (sale.getTransferDate().isBefore(LocalDate.of(2024, 7, 23))) {
                    flat.put("ltcg112APre", String.valueOf(Math.max(0, gain)));
                } else {
                    flat.put("ltcg112APost", String.valueOf(Math.max(0, gain)));
                }
            } else {
                if (sale.getTransferDate().isBefore(LocalDate.of(2024, 7, 23))) {
                    flat.put("stcgEquityPre", String.valueOf(Math.max(0, gain)));
                } else {
                    flat.put("stcgEquityPost", String.valueOf(Math.max(0, gain)));
                }
            }
        }
    }

    // === TDS FROM 26AS (for Schedule TDS2 — authoritative) ===
    if (f26as != null) {
        List<Map<String, String>> tds194AEntries = new ArrayList<>();
        for (TDSEntry26AS entry : f26as.getPartIEntries()) {
            if ("194A".equals(entry.getSection())) {
                Map<String, String> e = new HashMap<>();
                e.put("deductorName", entry.getDeductorName());
                e.put("deductorTAN", entry.getTan());
                e.put("grossAmount", String.valueOf(entry.getAmountPaid()));
                e.put("tdsDeducted", String.valueOf(entry.getTaxDeposited())); // Net deposited
                tds194AEntries.add(e);
            }
        }
        flat.put("tds194AEntries", new ObjectMapper().writeValueAsString(tds194AEntries));

        // TDS on Salary from 26AS Part-I Section 192:
        for (TDSEntry26AS entry : f26as.getPartIEntries()) {
            if ("192".equals(entry.getSection())) {
                flat.put("tds192Amount", String.valueOf(entry.getTaxDeposited()));
                flat.put("tds192EmployerTAN", entry.getTan());
                flat.put("tds192EmployerName", entry.getDeductorName());
            }
        }
    }

    // === SELF-ASSESSMENT TAX from AIS Part B3 ===
    if (ais != null && ais.getPartB3() != null) {
        for (TaxPaymentAIS payment : ais.getPartB3()) {
            if (payment.getMinorHead().contains("Self Assessment")) {
                flat.put("selfAssessmentTax", String.valueOf(payment.getTotalAmount()));
                flat.put("satBSRCode", payment.getBsrCode());
                flat.put("satChallanNo", payment.getChallanSerialNo());
                flat.put("satDate", payment.getDepositDate().toString());
            } else if (payment.getMinorHead().contains("Advance Tax")) {
                // Map to appropriate quarter based on date:
                mapAdvanceTaxToQuarter(flat, payment);
            }
        }
    }

    return flat;
}

private void mapAdvanceTaxToQuarter(FlatFormData flat, TaxPaymentAIS payment) {
    LocalDate date = payment.getDepositDate();
    int year = date.getYear();
    // Q1: paid by 15-Jun
    if (!date.isAfter(LocalDate.of(year, 6, 15))) {
        addToField(flat, "advTax_jun", payment.getTotalAmount());
    }
    // Q2: paid 16-Jun to 15-Sep
    else if (!date.isAfter(LocalDate.of(year, 9, 15))) {
        addToField(flat, "advTax_sep", payment.getTotalAmount());
    }
    // Q3: paid 16-Sep to 15-Dec
    else if (!date.isAfter(LocalDate.of(year, 12, 15))) {
        addToField(flat, "advTax_dec", payment.getTotalAmount());
    }
    // Q4: paid 16-Dec to 31-Mar
    else {
        addToField(flat, "advTax_mar", payment.getTotalAmount());
    }
}
```

---

## 8. NEW API ENDPOINTS REQUIRED

### File: `src/main/java/com/itr/controller/IntegrationController.java`

**ADD these new endpoints:**

```java
// 1. Import TIS PDF (new endpoint — TIS was not previously supported):
@PostMapping("/api/integration/tis/import")
public ResponseEntity<TISData> importTIS(
        @RequestParam("file") MultipartFile file,
        @RequestParam("pan") String pan,
        @RequestParam("dob") String dob) {
    TISData data = tisImportService.importTIS(file.getBytes(), pan, LocalDate.parse(dob));
    return ResponseEntity.ok(data);
}

// 2. Auto-populate from ALL three documents at once:
@PostMapping("/api/integration/autopopulate/all")
public ResponseEntity<FlatFormData> autoPopulateAll(
        @RequestBody AutoPopulateAllRequest request) {
    // request contains: clientId, year, aisData, form26ASData, tisData
    FlatFormData flat = autoPopulationService.autoPopulateAll(
        request.getAisData(), request.getForm26ASData(), request.getTisData(),
        request.getItrType());
    return ResponseEntity.ok(flat);
}

// 3. Reconciliation report:
@PostMapping("/api/integration/reconciliation")
public ResponseEntity<ReconciliationReport> getReconciliation(
        @RequestBody ReconciliationRequest request) {
    ReconciliationReport report = reconciliationService.generateReconciliationReport(
        request.getData26AS(), request.getAisData(), request.getTisData());
    return ResponseEntity.ok(report);
}

// 4. Import 26AS and return structured data:
@PostMapping("/api/integration/26as/import")
public ResponseEntity<Form26ASData> import26AS(
        @RequestParam("file") MultipartFile file,
        @RequestParam("pan") String pan,
        @RequestParam("dob") String dob) {
    Form26ASData data = form26ASImportService.import26AS(file.getBytes(), pan,
        LocalDate.parse(dob));
    return ResponseEntity.ok(data);
}
```

---

## 9. FRONTEND IMPORT FLOW — COMPLETE REWRITE

### File: `src/pages/ITRComputationPage.tsx` — Import menu handler
### File: `src/lib/api/integration.ts`

```typescript
// integration.ts — ADD new API functions:

export const importTIS = async (file: File, pan: string, dob: string) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('pan', pan);
  formData.append('dob', dob);
  const res = await axios.post('/api/integration/tis/import', formData);
  return res.data as TISData;
};

export const import26AS = async (file: File, pan: string, dob: string) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('pan', pan);
  formData.append('dob', dob);
  const res = await axios.post('/api/integration/26as/import', formData);
  return res.data as Form26ASData;
};

export const autoPopulateAll = async (
  clientId: number, year: string,
  aisData?: AISData, data26AS?: Form26ASData, tisData?: TISData
) => {
  const res = await axios.post('/api/integration/autopopulate/all', {
    clientId, year, aisData, form26ASData: data26AS, tisData,
    itrType: 'ITR-1'
  });
  return res.data as Record<string, string>;
};

export const getReconciliationReport = async (
  aisData: AISData, data26AS: Form26ASData, tisData?: TISData
) => {
  const res = await axios.post('/api/integration/reconciliation',
    { aisData, data26AS, tisData });
  return res.data as ReconciliationReport;
};
```

```typescript
// ITRComputationPage.tsx — handleFileImport (REVISED):
// Import ALL three documents and run combined auto-population:

const [importedAIS, setImportedAIS] = useState<AISData | null>(null);
const [imported26AS, setImported26AS] = useState<Form26ASData | null>(null);
const [importedTIS, setImportedTIS] = useState<TISData | null>(null);

const handleImportAndAutoPopulate = async (
  file: File, docType: '26AS' | 'AIS' | 'TIS'
) => {
  setImporting(true);
  try {
    const pan = client.pan;
    const dob = client.dob; // e.g., "1974-06-14"

    let newAIS = importedAIS, new26AS = imported26AS, newTIS = importedTIS;

    if (docType === 'AIS') {
      newAIS = await importAIS(file, pan, dob);
      setImportedAIS(newAIS);
    } else if (docType === '26AS') {
      new26AS = await import26AS(file, pan, dob);
      setImported26AS(new26AS);
    } else if (docType === 'TIS') {
      newTIS = await importTIS(file, pan, dob);
      setImportedTIS(newTIS);
    }

    // Auto-populate from all available documents:
    const populated = await autoPopulateAll(clientId, year, newAIS, new26AS, newTIS);
    setFormData(prev => ({ ...prev, ...populated }));

    // If both AIS and 26AS available, show reconciliation:
    if (newAIS && new26AS) {
      const report = await getReconciliationReport(newAIS, new26AS, newTIS ?? undefined);
      if (report.hasDiscrepancies) {
        setReconciliationReport(report);
        setShowReconciliationModal(true);
      }
    }

    toast.success(`${docType} imported and form auto-populated successfully!`);
  } catch (err) {
    toast.error(`Import failed: ${err.message}`);
  } finally {
    setImporting(false);
  }
};
```

---

## 10. DATABASE — ADD IMPORTED DOCUMENT STORAGE

### Migration: Add columns to `client_year_data` table

```sql
-- Add columns to store imported raw data for reconciliation and re-use:
ALTER TABLE client_year_data
  ADD COLUMN IF NOT EXISTS ais_data JSONB,
  ADD COLUMN IF NOT EXISTS form26as_data JSONB,
  ADD COLUMN IF NOT EXISTS tis_data JSONB,
  ADD COLUMN IF NOT EXISTS import_timestamp TIMESTAMP,
  ADD COLUMN IF NOT EXISTS reconciliation_report JSONB;

-- Index for faster JSON queries:
CREATE INDEX IF NOT EXISTS idx_cyd_ais ON client_year_data USING GIN (ais_data);
CREATE INDEX IF NOT EXISTS idx_cyd_26as ON client_year_data USING GIN (form26as_data);
```

### Update `ClientYearData.java` entity:
```java
@Column(columnDefinition = "jsonb")
@JdbcTypeCode(SqlTypes.JSON)
private AISData aisData;

@Column(columnDefinition = "jsonb")
@JdbcTypeCode(SqlTypes.JSON)
private Form26ASData form26ASData;

@Column(columnDefinition = "jsonb")
@JdbcTypeCode(SqlTypes.JSON)
private TISData tisData;

private LocalDateTime importTimestamp;
```

---

## 11. FRONTEND — TypeScript TYPE DEFINITIONS

### File: `src/types/api.types.ts` — ADD new interfaces

```typescript
export interface TDSEntry26AS {
  deductorName: string;
  tan: string;
  section: string;  // '192' | '194A' | '194C' | etc.
  amountPaid: number;
  taxDeducted: number;
  taxDeposited: number; // Use this for TDS credit
}

export interface Form26ASData {
  partI: TDSEntry26AS[];       // TDS on salary/other income
  partIV: PropertyTDS26AS[];   // Property TDS (seller side)
  partVII: RefundEntry26AS[];  // Refunds received
  partX: DefaultEntry26AS[];   // TDS defaults (informational)
}

export interface AISTDSEntry {
  section: string;
  deductorName: string;
  deductorTAN: string;
  totalAmountPaid: number;   // Income received
  totalTDSDeducted: number;  // TDS (AIS processed)
}

export interface SFTSaleEntry {
  transferDate: string;         // YYYY-MM-DD
  securityName: string;
  assetType: 'STCG' | 'LTCG';
  quantity: number;
  salePricePerUnit: number;
  salesConsideration: number;
  costOfAcquisition: number;
  fmvPerUnit: number;           // For grandfathering
  indexedCostOfAcquisition: number;
}

export interface AISData {
  generalInfo: AISGeneralInfo;
  partB1: {
    tdsEntries: AISTDSEntry[];
  };
  partB2: {
    dividendIncome: number;
    securitiesSale: SFTSaleEntry[];
    securitiesPurchase: number;
    mutualFundPurchase: MFPurchase[];
  };
  partB3: TaxPaymentAIS[];
}

export interface TISData {
  dividendIncome: number;
  interestFromDeposit: number;
  securitiesSaleConsideration: number;
  securitiesPurchaseAmount: number;
}

export interface ReconciliationReport {
  hasDiscrepancies: boolean;
  items: ReconciliationItem[];
}

export interface ReconciliationItem {
  deductorName: string;
  tan: string;
  income26AS: number;
  incomeAIS: number;
  tds26AS: number;   // USE THIS for TDS credit
  tdsAIS: number;    // Informational
  incomeDiff: number;
  tdsDiff: number;
  recommendedAction: 'USE_26AS' | 'USE_AIS' | 'MANUAL';
}
```

---

## 12. BACKEND FILE CLEANUP — ADDITIONAL ITEMS

### Additional files identified for deletion/refactoring:

```
DELETE: src/main/java/com/itr/service/integration/ITDPrefillImportService.java
  Reason: ITD Prefill JSON format changes with every AY. Current implementation
  is for an older AY schema and will produce incorrect imports. Remove entirely.
  Re-implement fresh each AY with the new schema.

DELETE: src/main/java/com/itr/service/DocumentStorageService.java
  Reason: PDFs are processed in-memory and raw bytes are not stored. This service
  has no callers in the current codebase. The extracted data (AISData, Form26ASData)
  is stored in client_year_data.ais_data / form26as_data columns.

RENAME + REFACTOR: src/main/java/com/itr/service/integration/Form26ASImportService.java
  Current issue: Tries to parse raw transaction rows including G-entries, leading
  to double-counting. REPLACE entirely with the Part-wise parsing approach
  described in Section 2 of this document (use summary rows, not detail rows).

CREATE NEW: src/main/java/com/itr/service/integration/TISImportService.java
  This service does not currently exist. Create it per Section 4 of this document.

CREATE NEW: src/main/java/com/itr/service/ReconciliationService.java
  This service is referenced in ReconciliationPage.tsx but may not exist as a
  proper service. Implement per Section 6 of this document.
```

### Frontend files to update:

```
UPDATE: src/pages/ReconciliationPage.tsx
  Currently shows hardcoded/placeholder data. Wire to /api/integration/reconciliation
  endpoint. Show discrepancy table with "Use 26AS / Use AIS / Manual" buttons.
  When user accepts a choice, call /api/itr/{clientId}/{year}/save with the
  resolved amounts. Show "Reconciliation Complete" badge when all items resolved.

UPDATE: src/lib/api/integration.ts
  Add the 4 new functions from Section 9.
  Remove the non-working importITDPrefill() function.
```

---

## 13. IMPORT UI — WHAT TO SHOW TO USER (Frontend UX)

### ITRComputationPage.tsx — Import Menu

```typescript
// The Import dropdown should show:
// ┌─────────────────────────────┐
// │  Import Documents           │
// ├─────────────────────────────┤
// │ 📄 Form 16 (PDF)           │
// │ 📊 26AS from TRACES (PDF)  │  ← NEW label (was "26AS")
// │ 📋 AIS (PDF)               │
// │ 📑 TIS (PDF)               │  ← NEW option
// └─────────────────────────────┘
//
// Import sequence recommendation shown to user:
// "For best results, import documents in this order: 26AS → AIS → TIS"
// (because 26AS gives TDS credit, AIS gives income, TIS confirms totals)

// After each import — show a status strip:
// ✅ 26AS imported (TDS credit: ₹22,443 from 1 deductor)
// ✅ AIS imported (Interest: ₹46,410 | Dividend: ₹180 | LTCG: ₹561)
// ✅ TIS imported (Confirmed: Interest ₹46,410 | Dividend ₹180)
// ⚠️  Reconciliation needed — 1 discrepancy found → [View & Resolve]

// After auto-population — show what was populated:
// 🔄 Auto-populated:
//   Other Sources: FD Interest ₹46,410, Dividend ₹180
//   Capital Gains: LTCG (Post-Jul 2024) ₹561
//   TDS Credit: SBI ₹22,443, Anand Purushottam Agrawal ₹4,500
//   Self-Assessment Tax: ₹39,283 (BSR: 0002271, Challan: 34945)
```

---

## 14. SPECIFIC FIX FOR REFERENCE CLIENT (ACUPG3482G)

This section shows exactly what the system should extract and populate for the
reference PDFs provided. Use this as a test case for validation.

```
FROM 26AS (TRACES):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Deductor: STATE BANK OF INDIA | TAN: MUMS89569E
Section: 194A
Total Amount Paid/Credited: ₹2,24,329
Total TDS Deposited: ₹22,443  ← USE THIS FOR TDS CREDIT
→ Populate: tds194AEntries[0] = {SBI, MUMS89569E, 224329, 22443}
→ Income: ₹2,24,329 (FD interest from SBI — verify with passbook/Form 16A)

FROM AIS (Part B1):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Deductor 1: ANAND PURUSHOTTAM AGRAWAL | TAN: NGPA14339D
  Q3 (31/12/2025): ₹15,000 paid, TDS ₹1,500
  Q2 (30/09/2025): ₹30,000 paid, TDS ₹3,000
  Total: ₹45,000 income, TDS ₹4,500
  → Income: ₹45,000 (interest from this deductor)
  → TDS: ₹4,500

Deductor 2: STATE BANK OF INDIA | TAN: MUMS89569E
  Q3 (08/10/2025): ₹1,410 paid, TDS ₹141
  → AIS shows only ₹1,410 (less than 26AS ₹2,24,329)
  → DISCREPANCY: Show alert, recommend use of 26AS ₹2,24,329 for income too

FROM AIS (Part B2):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SFT-015 (Dividend from LIC): ₹180
  → OtherSources.dividendIncome = 180

SFT-17-LES (Sale of RELIANCE INDUSTRIES equity):
  Date: 07/07/2025 → POST 23-Jul-2024? NO → Pre 23-Jul-2024 (07-Jul-2024 < 23-Jul-2024)
  Wait: 07/07/2025 = 7th July 2025 → POST 23-Jul-2024
  Asset Type shown in AIS: "Short-term" → STCG
  Sales Consideration: ₹1,523
  Cost of Acquisition: ₹1,517 (SFT-17 purchase)
  Gain = ₹6 STCG, Post-Jul-2024 → 20% rate
  But wait: AIS shows asset type as "Short term" for Reliance (held < 12 months)
  Actually looking at AIS: SFT shows sale price ₹1,523.45, cost ₹961.15 (FMV)
  Indexed cost: ₹0 → This is listed equity STCG
  Transfer date: 07/07/2025 → POST 23-Jul-2024
  → CapitalGains.stcgEquityPost = 1523 - 1517 = 6  (sale - purchase)
  OR if FMV-based: 1523 - 961 = 562 (using FMV as cost for grandfathering)
  NOTE: STCG on listed equity does NOT use grandfathering (112A grandfathering is LTCG only)
  Actual cost (SFT-17 purchase) = 1517
  STCG = 1523 - 1517 = ₹6

SFT-17(Pur): Purchase ₹1,517 (Reliance shares)
SFT-18(Pur) ICICI Prudential MF: ₹72,996 (Q2 purchase)
SFT-18(Pur) HDFC AMC: ₹37,998 (Q2 purchase)
SFT-18(Pur) Sundaram MF: ₹20,999 (Q1: ₹11,999 + Q2: ₹9,000)
  → These are PURCHASES (cost basis), not sales. No CG income to declare unless sold.
  → Store as investment data, not current year CG.

FROM AIS (Part B3):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Self Assessment Tax:
  Tax: ₹37,772 | Surcharge: ₹0 | Education Cess: ₹1,511 | Total: ₹39,283
  BSR Code: 0002271 | Date: 31/08/2025 | Challan: 34945
  → TDSTab.selfAssessmentTax = 39283
  → TDSTab.satBSRCode = "0002271"
  → TDSTab.satChallanNo = "34945"
  → TDSTab.satDate = "31/08/2025"

FROM TIS (Confirmed Amounts):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Dividend: ₹180 (Accepted) ✓
Interest from Deposit: ₹46,410 (Accepted) ✓
  → This is ₹45,000 (Anand) + ₹1,410 (SBI from AIS) = ₹46,410
  → NOTE: TIS uses AIS processed amounts for income, NOT 26AS
  → For income: ₹46,410 (TIS/AIS accepted)
  → For TDS: ₹22,443 (SBI from 26AS) + ₹4,500 (Anand from AIS) = ₹26,943

Sale of securities: ₹1,523
Purchase of securities/MF: ₹1,33,510

EXPECTED ITR COMPUTATION (after correct population):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Income from Other Sources:
  FD/Interest: ₹46,410 (TIS confirmed)
  Dividend: ₹180
  Total: ₹46,590

Capital Gains:
  STCG (equity, post-Jul-2024, Section 111A): ₹6 (Reliance sale)
  → Tax on STCG: ₹6 × 20% = ₹1.20 ≈ ₹1 (current calc is CORRECT for this)
  LTCG: ₹0 (no LTCG transactions)

Standard Deduction: ₹75,000 (new regime salaried) — but no salary here
Total Taxable Income: ₹46,590 (Other Sources only, no salary)

Tax Computation (New Regime):
  Income: ₹46,590
  Slab tax: ₹0 (below ₹4,00,000 exemption threshold in new regime)
  Rebate 87A: ₹0 (no tax to rebate)
  STCG tax: ₹1
  Cess: ₹0
  Total Tax Liability: ₹1

TDS Credit:
  Section 192 (Salary): ₹0
  Section 194A (Interest):
    SBI: ₹22,443 (from 26AS) ← CLAIM THIS
    Anand Purushottam Agrawal: ₹4,500 (from AIS, 26AS not available for this deductor)
    Total 194A TDS: ₹26,943
  Self-Assessment Tax: ₹39,283

Total Tax Paid: ₹26,943 + ₹39,283 = ₹66,226
REFUND = ₹66,226 - ₹1 = ₹66,225
```

---

*End of Part 2. Implement Part 1 (computation fixes) before Part 2 (PDF imports)
to ensure the populated values are correctly used in tax computation.*
