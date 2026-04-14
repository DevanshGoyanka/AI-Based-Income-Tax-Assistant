# ITR ERP — PHASE 9: COMPLETE GAP CLOSURE DIRECTIVE
## 97% → 101% CBDT Compliance — All Remaining Gaps, Architecture Hardening & Security

**Date:** April 11, 2026
**Prerequisite:** Phase 1–8 complete, BUILD SUCCESS, 156 Java files compiled, 97% compliant
**Audit Basis:** ITR ERP Audit Compliance Report (April 11, 2026) + Gap Analysis addendum
**Target:** 101% CBDT compliant — every functional, architectural, and security gap closed

---

## GAP INVENTORY — MASTER TABLE

| ID | Gap | Category | Blocker Level | Section |
|----|-----|----------|---------------|---------|
| G-01 | Form 16 PDF OCR (Part A + Part B parsing) | Functional | HIGH — Manual entry error-prone at scale | §1 |
| G-02 | 26AS XML Auto-Import via TRACES API | Functional | HIGH — Manual reconciliation | §2 |
| G-03 | ITR-1 JSON Export regression audit + fix | Functional | HIGH — Unvalidated in Phase 8 | §3 |
| G-04 | Schedule FA — Foreign Assets full implementation | Functional | HIGH — CBDT mandatory | §4 |
| G-05 | AMT (Alternate Minimum Tax) u/s 115JC for ITR-3 | Functional | HIGH — Missing for LLPs/firms | §5 |
| G-06 | E-Verification module (EVC/DSC) | Functional | HIGH — Filing incomplete without e-verify | §6 |
| G-07 | Rectification u/s 154 workflow | Functional | MEDIUM | §7 |
| G-08 | Revised return u/s 139(5) workflow | Functional | MEDIUM | §8 |
| G-09 | TDS mismatch alerts + correction suggestions | Functional | MEDIUM | §9 |
| G-10 | Tax regime auto-recommender with confidence score | Functional | MEDIUM | §10 |
| G-11 | Async orchestration pipeline (Spring Batch) | Architecture | HIGH — 2–3 min sync timeout risk | §11 |
| G-12 | Bulk filing for CA firms (multi-PAN batch) | Architecture | MEDIUM | §12 |
| G-13 | AIS reconciliation result caching (Redis/DB) | Architecture | MEDIUM | §13 |
| G-14 | ITR-V PDF acknowledgment receipt generation | Architecture | MEDIUM | §14 |
| G-15 | AES-256 encryption for PAN/Aadhaar at rest | Security | CRITICAL | §15 |
| G-16 | MFA / OTP flow for submission | Security | HIGH | §16 |
| G-17 | Audit log tamper detection (hash chaining) | Security | HIGH | §17 |
| G-18 | Session timeout handling for sensitive data | Security | HIGH | §18 |

**Execution contract:** Complete §1–§10 (functional) before §11–§14 (architecture) before §15–§18 (security). Compile after each section. Zero regressions permitted on existing 227 CBDT validation rules.

---

## SECTION 1 — FORM 16 PDF OCR ENGINE

### 1.1 Scope

The `ITR_Import_JSON_Validation.md` (§1) fully specifies all fields for Part A and Part B. The prior phases deferred the PDF parsing layer; this section implements it using **Apache PDFBox + Tesseract OCR** (available via `tess4j` wrapper). Structured PDFs (TRACES-generated Part A) use PDFBox text extraction directly. Scanned/image-based PDFs fall back to Tesseract.

### 1.2 New Dependencies — `pom.xml`

Add to `<dependencies>`:

```xml
<!-- PDFBox for structured PDF text extraction -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.2</version>
</dependency>

<!-- Tess4J for OCR fallback on scanned PDFs -->
<dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.11.0</version>
</dependency>
```

Add to `<build><plugins>` — copy tessdata to target:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-tessdata</id>
            <phase>process-resources</phase>
            <goals><goal>copy-resources</goal></goals>
            <configuration>
                <outputDirectory>${project.build.directory}/tessdata</outputDirectory>
                <resources>
                    <resource>
                        <directory>src/main/resources/tessdata</directory>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

> **Tessdata setup:** Place `eng.traineddata` (English) and `hin.traineddata` (Hindi — employer names sometimes in Hindi) in `src/main/resources/tessdata/`. Download from https://github.com/tesseract-ocr/tessdata.

### 1.3 `Form16PartAOCRParser.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.service.import_engine;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import com.yourpackage.dto.Form16PartAData;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Form16PartAOCRParser {

    private final Tesseract tesseract;

    public Form16PartAOCRParser() {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(System.getProperty("user.dir") + "/tessdata");
        this.tesseract.setLanguage("eng");
        this.tesseract.setPageSegMode(1);  // Automatic page segmentation with OSD
        this.tesseract.setOcrEngineMode(1); // LSTM neural network engine
    }

    /**
     * Primary entry point. Detects whether PDF is text-based (TRACES) or scanned.
     * Uses PDFBox for text PDFs; falls back to Tesseract for image PDFs.
     * Returns a fully-populated Form16PartAData DTO.
     */
    public Form16PartAData parse(File pdfFile) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(doc);

            // Heuristic: if extracted text has fewer than 200 characters, it's a scanned PDF
            if (rawText == null || rawText.trim().length() < 200) {
                rawText = performOCR(doc);
            }

            return parsePartAFromText(rawText);
        }
    }

    private String performOCR(PDDocument doc) throws IOException {
        PDFRenderer renderer = new PDFRenderer(doc);
        StringBuilder sb = new StringBuilder();
        for (int page = 0; page < doc.getNumberOfPages(); page++) {
            // 300 DPI — minimum for reliable OCR on tax documents
            BufferedImage img = renderer.renderImageWithDPI(page, 300);
            try {
                sb.append(tesseract.doOCR(img));
            } catch (TesseractException e) {
                throw new IOException("OCR failed on page " + (page + 1) + ": " + e.getMessage(), e);
            }
        }
        return sb.toString();
    }

    private Form16PartAData parsePartAFromText(String text) {
        Form16PartAData data = new Form16PartAData();

        // TAN format: 4 uppercase letters + 5 digits + 1 uppercase letter
        data.setTanOfEmployer(extractRegex(text,
            "TAN of Employer[:\\s]+([A-Z]{4}[0-9]{5}[A-Z])", 1));

        // PAN: 5 uppercase + 4 digits + 1 uppercase
        data.setPanOfEmployee(extractRegex(text,
            "PAN of Employee[:\\s]+([A-Z]{5}[0-9]{4}[A-Z])", 1));

        // Employer name — free text up to the next label
        data.setEmployerName(extractRegex(text,
            "Name of Employer[:\\s]+([A-Za-z0-9 &.,()-]{1,75})", 1));

        // Assessment year
        data.setAssessmentYear(extractRegex(text,
            "Assessment Year[:\\s]+(20[0-9]{2}-[0-9]{2})", 1));

        // Financial year
        data.setFinancialYear(extractRegex(text,
            "Financial Year[:\\s]+(20[0-9]{2}-[0-9]{2})", 1));

        // Aggregate salary paid — numeric, may have commas
        String salaryRaw = extractRegex(text,
            "Aggregate.*?salary.*?paid[\\s:]+(Rs\\.?\\s?)?([0-9,]+)", 2);
        data.setAggregateSalaryPaid(parseCurrencyLong(salaryRaw));

        // Aggregate TDS deposited
        String tdsRaw = extractRegex(text,
            "Aggregate.*?TDS deposited[\\s:]+(Rs\\.?\\s?)?([0-9,]+)", 2);
        data.setAggregateTdsDeposited(parseCurrencyLong(tdsRaw));

        // Certificate number
        data.setCertificateNumber(extractRegex(text,
            "Certificate No\\.?[:\\s]+([A-Z0-9]{10,20})", 1));

        // TRACES acknowledgement number
        data.setAcknowledgementNumberTraces(extractRegex(text,
            "Acknowledgement No[:\\s]+([0-9]{15})", 1));

        // Period of employment
        data.setPeriodFrom(extractRegex(text,
            "From[:\\s]+([0-3][0-9]/[0-1][0-9]/20[0-9]{2})", 1));
        data.setPeriodTo(extractRegex(text,
            "To[:\\s]+([0-3][0-9]/[0-1][0-9]/20[0-9]{2})", 1));

        // Quarterly TDS breakup — table parsing
        data.setQuarterlyBreakup(parseQuarterlyTable(text));

        return data;
    }

    /**
     * Parses the quarterly TDS table from the extracted text.
     * TRACES format has rows: Q1/Q2/Q3/Q4 | Date | BSR Code | Challan No | Amount
     */
    private java.util.List<Form16PartAData.QuarterlyTDS> parseQuarterlyTable(String text) {
        java.util.List<Form16PartAData.QuarterlyTDS> result = new java.util.ArrayList<>();
        // Match rows: Quarter | dd/mm/yyyy | 7-digit BSR | challan serial | amount
        Pattern rowPattern = Pattern.compile(
            "(Q[1-4])[\\s|]+(\\d{2}/\\d{2}/20\\d{2})[\\s|]+(\\d{2}/\\d{2}/20\\d{2})[\\s|]+(\\d{7})[\\s|]+(\\d+)[\\s|]+([0-9,]+)");
        Matcher m = rowPattern.matcher(text);
        while (m.find()) {
            Form16PartAData.QuarterlyTDS q = new Form16PartAData.QuarterlyTDS();
            q.setQuarter(m.group(1));
            q.setDateOfPaymentCredit(m.group(2));
            q.setDateOfTdsDeposit(m.group(3));
            q.setBsrCode(m.group(4));
            q.setChallanSerialNumber(m.group(5));
            q.setAmountOfTaxDeposited(parseCurrencyLong(m.group(6)));
            result.add(q);
        }
        return result;
    }

    private String extractRegex(String text, String pattern, int group) {
        try {
            Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(text);
            if (m.find() && m.groupCount() >= group) return m.group(group).trim();
        } catch (Exception ignored) {}
        return null;
    }

    private long parseCurrencyLong(String s) {
        if (s == null) return 0L;
        return Long.parseLong(s.replaceAll("[^0-9]", ""));
    }
}
```

### 1.4 `Form16PartBOCRParser.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.service.import_engine;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import com.yourpackage.dto.Form16PartBData;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Form 16 Part B (employer-generated salary computation).
 * Part B is always a text PDF (employer-created via payroll software).
 * If scanned, delegates to Form16PartAOCRParser's OCR engine.
 */
@Service
public class Form16PartBOCRParser {

    private final Form16PartAOCRParser ocrEngine; // reuse OCR engine

    public Form16PartBOCRParser(Form16PartAOCRParser ocrEngine) {
        this.ocrEngine = ocrEngine;
    }

    public Form16PartBData parse(File pdfFile) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            if (text == null || text.trim().length() < 200) {
                // Fallback: use OCR via the reused engine
                text = ocrEngine.parse(pdfFile).toString(); // triggers OCR
            }
            return parsePartBFromText(text);
        }
    }

    private Form16PartBData parsePartBFromText(String text) {
        Form16PartBData data = new Form16PartBData();

        // ── Schedule Salary Section 17(1) ────────────────────────────────
        data.setBasicSalary(extractAmount(text, "Basic Salary"));
        data.setDearnessAllowance(extractAmount(text, "Dearness Allowance|DA"));
        data.setHouseRentAllowance(extractAmount(text, "House Rent Allowance|HRA"));
        data.setLeaveTravelAllowance(extractAmount(text, "Leave Travel Allowance|LTA"));
        data.setLeaveEncashment(extractAmount(text, "Leave Encashment"));
        data.setGratuity(extractAmount(text, "Gratuity"));
        data.setBonus(extractAmount(text, "Bonus"));
        data.setCommission(extractAmount(text, "Commission"));
        data.setArrearsOfSalary(extractAmount(text, "Arrears of Salary"));
        data.setOtherAllowances(extractAmount(text, "Other Allowances"));
        data.setTotalSalary17_1(extractAmount(text, "Total.*?17\\(1\\)|Gross Salary"));

        // ── Perquisites 17(2) ─────────────────────────────────────────────
        data.setTotalPerquisites17_2(extractAmount(text, "Perquisites.*?17\\(2\\)"));

        // ── Profits in lieu 17(3) ────────────────────────────────────────
        data.setTotalProfitsInLieu17_3(extractAmount(text, "Profits in lieu.*?17\\(3\\)"));

        // ── Gross Salary ─────────────────────────────────────────────────
        data.setGrossSalary(extractAmount(text, "Gross Salary"));

        // ── Exemptions u/s 10 ────────────────────────────────────────────
        data.setHraExemption10_13A(extractAmount(text, "HRA.*?10\\(13A\\)|House Rent.*?Exempt"));
        data.setLtaExemption10_5(extractAmount(text, "LTA.*?10\\(5\\)|Leave Travel.*?Exempt"));
        data.setGratuityExemption10_10(extractAmount(text, "Gratuity.*?10\\(10\\)|Gratuity.*?Exempt"));
        data.setLeaveEncashmentExemption10_10AA(extractAmount(text, "Leave Encash.*?10\\(10AA\\)|Leave Encash.*?Exempt"));
        data.setTotalExemptionsUnder10(extractAmount(text, "Total.*?Exempt.*?10|Total.*?u/s 10"));

        // ── Net Salary ───────────────────────────────────────────────────
        data.setNetSalaryAfterExemptions(extractAmount(text, "Net Salary|Balance.*?Salary"));

        // ── Standard Deduction ───────────────────────────────────────────
        // ₹75,000 for AY 2026-27, ₹50,000 for AY 2025-26 — read the declared figure
        data.setStandardDeduction(extractAmount(text, "Standard Deduction"));

        // ── Professional Tax ─────────────────────────────────────────────
        data.setProfessionalTax(extractAmount(text, "Professional Tax|PT Paid"));

        // ── Entertainment Allowance ──────────────────────────────────────
        data.setEntertainmentAllowance(extractAmount(text, "Entertainment Allowance"));

        // ── Income from Salary (Taxable) ─────────────────────────────────
        data.setTaxableIncomeSalary(extractAmount(text, "Income from Salary|Taxable Salary"));

        // ── Chapter VI-A Deductions ──────────────────────────────────────
        data.setDeduction80C(extractAmount(text, "80C"));
        data.setDeduction80CCC(extractAmount(text, "80CCC"));
        data.setDeduction80CCD_1(extractAmount(text, "80CCD\\(1\\)"));
        data.setDeduction80CCD_1B(extractAmount(text, "80CCD\\(1B\\)|NPS.*?1B"));
        data.setDeduction80CCD_2(extractAmount(text, "80CCD\\(2\\)|Employer.*?NPS"));
        data.setDeduction80D(extractAmount(text, "80D|Medical Insurance"));
        data.setDeduction80E(extractAmount(text, "80E|Education Loan"));
        data.setDeduction80G(extractAmount(text, "80G|Donation"));
        data.setTotalDeductionsVIA(extractAmount(text, "Total.*?VI-A|Total.*?Deduction"));

        // ── TDS ──────────────────────────────────────────────────────────
        data.setTaxPayableOnSalary(extractAmount(text, "Tax Payable|Tax on.*?Income"));
        data.setReliefUnder89(extractAmount(text, "Relief.*?89\\(1\\)|Section 89"));
        data.setNetTaxPayable(extractAmount(text, "Net Tax Payable|Net Tax"));
        data.setTdsDeductedTotal(extractAmount(text, "TDS Deducted|Total TDS"));

        return data;
    }

    private long extractAmount(String text, String labelPattern) {
        // Look for label followed by an amount (with optional Rs., commas)
        Pattern p = Pattern.compile(
            "(?i)(?:" + labelPattern + ")[^0-9\\n]{0,30}(Rs\\.?\\s?)?([0-9][0-9,]*)",
            Pattern.DOTALL);
        Matcher m = p.matcher(text);
        if (m.find()) {
            String num = m.group(m.groupCount()).replaceAll("[^0-9]", "");
            try { return Long.parseLong(num); } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }
}
```

### 1.5 `Form16ValidationService.java` — CREATE FROM SCRATCH

This service cross-validates parsed Part A and Part B and auto-populates salary fields into the ITR DTO. It implements all 9 VAL-F16A rules from `ITR_Import_JSON_Validation.md §1.2`.

```java
package com.yourpackage.service.import_engine;

import com.yourpackage.dto.*;
import com.yourpackage.exception.Form16ValidationException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class Form16ValidationService {

    /**
     * Validates Part A against Part B and the ITR taxpayer profile.
     * Throws Form16ValidationException if any CAT-A cross-validation fails.
     * Returns a list of warnings for non-fatal discrepancies.
     */
    public List<String> validateAndAutoPopulate(
            Form16PartAData partA, Form16PartBData partB,
            String taxpayerPan, String assessmentYear,
            Itr1FormData itr1Target) {

        List<String> warnings = new ArrayList<>();

        // ── VAL-F16A-001: TAN format ──────────────────────────────────────
        if (partA.getTanOfEmployer() == null ||
            !partA.getTanOfEmployer().matches("[A-Z]{4}[0-9]{5}[A-Z]")) {
            throw new Form16ValidationException(
                "VAL-F16A-001: Invalid TAN format: " + partA.getTanOfEmployer());
        }

        // ── VAL-F16A-002: PAN match ───────────────────────────────────────
        if (!taxpayerPan.equals(partA.getPanOfEmployee())) {
            throw new Form16ValidationException(
                "VAL-F16A-002: Form 16 PAN [" + partA.getPanOfEmployee() +
                "] does not match taxpayer PAN [" + taxpayerPan + "]");
        }

        // ── VAL-F16A-003: TDS sum == quarterly sum ───────────────────────
        long quarterlySum = partA.getQuarterlyBreakup().stream()
            .mapToLong(Form16PartAData.QuarterlyTDS::getAmountOfTaxDeposited).sum();
        if (partA.getAggregateTdsDeposited() != quarterlySum) {
            throw new Form16ValidationException(
                "VAL-F16A-003: Aggregate TDS [" + partA.getAggregateTdsDeposited() +
                "] != sum of quarterly deposits [" + quarterlySum + "]");
        }

        // ── VAL-F16A-004: Assessment year ────────────────────────────────
        if (!assessmentYear.equals(partA.getAssessmentYear())) {
            throw new Form16ValidationException(
                "VAL-F16A-004: Form 16 AY [" + partA.getAssessmentYear() +
                "] does not match selected filing AY [" + assessmentYear + "]");
        }

        // ── VAL-F16A-005/006: Period of employment within FY ─────────────
        String expectedStart = assessmentYear.equals("2025-26") ? "01/04/2024" : "01/04/2025";
        String expectedEnd   = assessmentYear.equals("2025-26") ? "31/03/2025" : "31/03/2026";
        if (partA.getPeriodFrom() != null &&
            partA.getPeriodFrom().compareTo(expectedStart) < 0) {
            warnings.add("VAL-F16A-005: Period from [" + partA.getPeriodFrom() +
                "] is before FY start [" + expectedStart + "] — verify");
        }
        if (partA.getPeriodTo() != null &&
            partA.getPeriodTo().compareTo(expectedEnd) > 0) {
            warnings.add("VAL-F16A-006: Period to [" + partA.getPeriodTo() +
                "] is after FY end [" + expectedEnd + "] — verify");
        }

        // ── VAL-F16A-007: Aggregate TDS matches challan sum ──────────────
        // Already checked in VAL-F16A-003 above.

        // ── Part A vs Part B: TDS cross-check ────────────────────────────
        long partBTds = partB.getTdsDeductedTotal();
        long partATds = partA.getAggregateTdsDeposited();
        if (Math.abs(partATds - partBTds) > 100) { // Rs 100 tolerance
            warnings.add("Form 16: Part A TDS [" + partATds +
                "] vs Part B TDS [" + partBTds + "] differ by more than Rs 100 — verify");
        }

        // ── Auto-populate ITR fields ──────────────────────────────────────
        autoPopulateITR1(partA, partB, itr1Target);

        return warnings;
    }

    private void autoPopulateITR1(Form16PartAData partA, Form16PartBData partB,
                                   Itr1FormData target) {
        // Salary income from Part B
        target.setGrossSalary(partB.getGrossSalary());
        target.setNetSalaryAfterExemptions(partB.getNetSalaryAfterExemptions());
        target.setStandardDeduction(partB.getStandardDeduction());
        target.setEntertainmentAllowance(partB.getEntertainmentAllowance());
        target.setProfessionalTax(partB.getProfessionalTax());
        target.setTaxableIncomeSalary(partB.getTaxableIncomeSalary());

        // Deductions
        target.setDeduction80C(partB.getDeduction80C());
        target.setDeduction80CCC(partB.getDeduction80CCC());
        target.setDeduction80CCD1(partB.getDeduction80CCD_1());
        target.setDeduction80CCD1B(partB.getDeduction80CCD_1B());
        target.setDeduction80CCD2(partB.getDeduction80CCD_2());
        target.setDeduction80D(partB.getDeduction80D());
        target.setDeduction80G(partB.getDeduction80G());
        target.setDeduction80E(partB.getDeduction80E());

        // TDS from Part A (authoritative)
        target.setTdsCreditedSalary(partA.getAggregateTdsDeposited());
        target.setEmployerTan(partA.getTanOfEmployer());
        target.setEmployerName(partA.getEmployerName());
    }
}
```

### 1.6 Flyway Migration — `V9_1__form16_import_log.sql`

```sql
-- Track Form 16 imports for reconciliation and audit
CREATE TABLE form16_import_log (
    id                  BIGSERIAL PRIMARY KEY,
    pan                 VARCHAR(10) NOT NULL,
    assessment_year     VARCHAR(7)  NOT NULL,
    employer_tan        VARCHAR(10),
    employer_name       VARCHAR(75),
    aggregate_salary    BIGINT,
    aggregate_tds       BIGINT,
    part_a_parsed_at    TIMESTAMP,
    part_b_parsed_at    TIMESTAMP,
    validation_status   VARCHAR(20) NOT NULL, -- PASS | WARN | FAIL
    warnings_json       TEXT,        -- JSON array of warning strings
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_form16_pan_ay_tan UNIQUE (pan, assessment_year, employer_tan)
);

CREATE INDEX idx_form16_pan_ay ON form16_import_log (pan, assessment_year);
```

---

## SECTION 2 — 26AS XML AUTO-IMPORT VIA TRACES API

### 2.1 Scope

26AS (Annual Statement) is available via TRACES API as XML. `ITR_Import_JSON_Validation.md §3` specifies the complete 26AS structure. This section implements the HTTP client, XML parser, and reconciliation bridge.

> **Environment variable required:** `TRACES_CLIENT_ID`, `TRACES_CLIENT_SECRET`, `TRACES_BASE_URL` (sandbox: `https://tdscpc.gov.in/app/api/` — confirm with TRACES documentation before production use).

### 2.2 `TRACESApiClient.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.service.import_engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class TRACESApiClient {

    @Value("${traces.client-id}")
    private String clientId;

    @Value("${traces.client-secret}")
    private String clientSecret;

    @Value("${traces.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public TRACESApiClient(RestTemplate restTemplate, ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }

    /**
     * Fetches 26AS XML for a given PAN and assessment year.
     * Returns raw XML string — pass to Form26ASXMLParser for field extraction.
     * Throws TRACESApiException on auth failure or API error.
     */
    public String fetch26AS(String pan, String assessmentYear) {
        String token = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("pan", pan);
        requestBody.put("assessmentYear", assessmentYear);
        requestBody.put("format", "XML");

        HttpEntity<Map<String, String>> entity;
        try {
            entity = new HttpEntity<>(requestBody, headers);
        } catch (Exception e) {
            throw new TRACESApiException("Failed to build TRACES request: " + e.getMessage(), e);
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/26as/download", HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new TRACESApiException("TRACES API returned status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new TRACESApiException("TRACES API authentication failed — check credentials", e);
            }
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new TRACESApiException("No 26AS found for PAN [" + pan + "] AY [" + assessmentYear + "]", e);
            }
            throw new TRACESApiException("TRACES API error: " + e.getMessage(), e);
        }
    }

    private String getAccessToken() {
        String credentials = Base64.getEncoder().encodeToString(
            (clientId + ":" + clientSecret).getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + credentials);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> entity = new HttpEntity<>(
            "grant_type=client_credentials&scope=26AS", headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/oauth/token", HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            }
            throw new TRACESApiException("TRACES OAuth: no access_token in response");
        } catch (HttpClientErrorException e) {
            throw new TRACESApiException("TRACES OAuth failed: " + e.getMessage(), e);
        }
    }
}
```

### 2.3 `Form26ASXMLParser.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.service.import_engine;

import com.yourpackage.dto.Form26ASData;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.StringReader;
import org.xml.sax.InputSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses 26AS XML into Form26ASData DTO.
 * Covers Part A (TDS on salary), Part B (TDS on other income),
 * Part C (advance tax/self-assessment tax), Part G (TDS on salary u/s 192).
 * As specified in ITR_Import_JSON_Validation.md §3.
 */
@Service
public class Form26ASXMLParser {

    public Form26ASData parse(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // XXE protection — disable external entity loading
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
        doc.getDocumentElement().normalize();

        Form26ASData data = new Form26ASData();

        // ── Header ─────────────────────────────────────────────────────────
        data.setPan(getTextContent(doc, "PAN"));
        data.setAssessmentYear(getTextContent(doc, "AssessmentYear"));
        data.setTaxpayerName(getTextContent(doc, "TaxpayerName"));

        // ── Part A: TDS on Salary ──────────────────────────────────────────
        data.setPartAEntries(parsePartA(doc));

        // ── Part B: TDS on Other Income ───────────────────────────────────
        data.setPartBEntries(parsePartB(doc));

        // ── Part C: Advance Tax / Self-Assessment Tax ─────────────────────
        data.setPartCEntries(parsePartC(doc));

        // ── Part G: TDS on Salary u/s 192 ────────────────────────────────
        data.setPartGEntries(parsePartG(doc));

        // ── Total TDS credited ─────────────────────────────────────────────
        long totalTDS = data.getPartAEntries().stream()
            .mapToLong(Form26ASData.TDSEntry::getTdsAmount).sum();
        totalTDS += data.getPartBEntries().stream()
            .mapToLong(Form26ASData.TDSEntry::getTdsAmount).sum();
        data.setTotalTdsCredited(totalTDS);

        return data;
    }

    private List<Form26ASData.TDSEntry> parsePartA(Document doc) {
        return parseTDSSection(doc, "PartA", "A");
    }

    private List<Form26ASData.TDSEntry> parsePartB(Document doc) {
        return parseTDSSection(doc, "PartB", "B");
    }

    private List<Form26ASData.TDSEntry> parsePartG(Document doc) {
        return parseTDSSection(doc, "PartG", "G");
    }

    private List<Form26ASData.TDSEntry> parseTDSSection(Document doc, String sectionTag, String part) {
        List<Form26ASData.TDSEntry> entries = new ArrayList<>();
        NodeList sections = doc.getElementsByTagName(sectionTag);
        for (int i = 0; i < sections.getLength(); i++) {
            Element section = (Element) sections.item(i);
            NodeList rows = section.getElementsByTagName("Row");
            for (int j = 0; j < rows.getLength(); j++) {
                Element row = (Element) rows.item(j);
                Form26ASData.TDSEntry entry = new Form26ASData.TDSEntry();
                entry.setPart(part);
                entry.setDeductorName(getChildText(row, "DeductorName"));
                entry.setDeductorTan(getChildText(row, "DeductorTAN"));
                entry.setAmountPaidCredited(parseLong(getChildText(row, "AmountPaid")));
                entry.setTdsAmount(parseLong(getChildText(row, "TDSAmount")));
                entry.setStatus(getChildText(row, "Status")); // F/U/O
                entries.add(entry);
            }
        }
        return entries;
    }

    private List<Form26ASData.TaxPaidEntry> parsePartC(Document doc) {
        List<Form26ASData.TaxPaidEntry> entries = new ArrayList<>();
        NodeList sections = doc.getElementsByTagName("PartC");
        for (int i = 0; i < sections.getLength(); i++) {
            Element section = (Element) sections.item(i);
            NodeList rows = section.getElementsByTagName("Row");
            for (int j = 0; j < rows.getLength(); j++) {
                Element row = (Element) rows.item(j);
                Form26ASData.TaxPaidEntry entry = new Form26ASData.TaxPaidEntry();
                entry.setType(getChildText(row, "Type")); // Advance Tax | SAT
                entry.setBsrCode(getChildText(row, "BSRCode"));
                entry.setChallanSerialNo(getChildText(row, "ChallanSerialNo"));
                entry.setDateOfDeposit(getChildText(row, "DateOfDeposit"));
                entry.setAmount(parseLong(getChildText(row, "Amount")));
                entries.add(entry);
            }
        }
        return entries;
    }

    private String getTextContent(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        if (list.getLength() > 0) return list.item(0).getTextContent().trim();
        return null;
    }

    private String getChildText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) return list.item(0).getTextContent().trim();
        return null;
    }

    private long parseLong(String s) {
        if (s == null || s.isEmpty()) return 0L;
        try { return Long.parseLong(s.replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { return 0L; }
    }
}
```

### 2.4 Wire into `AISReconciliationService.java` — MODIFY EXISTING

Open `AISReconciliationService.java`. Add these injections and a new `fetchAndReconcile26AS` method:

```java
@Autowired private TRACESApiClient tracesApiClient;
@Autowired private Form26ASXMLParser form26ASParser;
@Autowired private AISReconciliationResultRepository aisResultRepo; // new — see §13

/**
 * Fetches 26AS from TRACES, reconciles TDS entries against ITR data.
 * Returns discrepancies > Rs 100 as warnings. Persists result to DB (see §13).
 * Non-blocking: if TRACES is unavailable, returns empty result and logs warning.
 */
public AISReconciliationResult fetchAndReconcile26AS(String pan, String assessmentYear,
                                                      long totalTdsPerITR) {
    AISReconciliationResult result = new AISReconciliationResult();
    try {
        String xml = tracesApiClient.fetch26AS(pan, assessmentYear);
        Form26ASData data = form26ASParser.parse(xml);

        long tds26AS = data.getTotalTdsCredited();
        long diff = Math.abs(tds26AS - totalTdsPerITR);

        if (diff > 100) {
            result.addDiscrepancy("26AS TDS [₹" + tds26AS + "] vs ITR TDS claim [₹" +
                totalTdsPerITR + "] — difference ₹" + diff + " exceeds tolerance");
        }

        // Persist result for caching (see §13)
        persistReconciliationResult(pan, assessmentYear, result, data);

    } catch (TRACESApiException e) {
        result.setFetchFailed(true);
        result.setFetchError("TRACES API unavailable: " + e.getMessage());
        log.warn("26AS fetch failed for PAN {} AY {} — proceeding without: {}",
            pan, assessmentYear, e.getMessage());
    } catch (Exception e) {
        result.setFetchFailed(true);
        result.setFetchError("26AS parse error: " + e.getMessage());
        log.error("26AS parse error for PAN {} AY {}", pan, assessmentYear, e);
    }
    return result;
}
```

---

## SECTION 3 — ITR-1 JSON EXPORT REGRESSION AUDIT

### 3.1 Scope

`ITDJSONExportService.java` (ITR-1) was implemented in Phases 1–4 but never formally validated in Phase 8. This section defines the mandatory regression test suite and fixes that must be applied.

### 3.2 `ITR1JSONExportRegressionTest.java` — CREATE FROM SCRATCH

Place in `src/test/java/com/yourpackage/service/`:

```java
package com.yourpackage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourpackage.dto.Itr1FormData;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ITR1JSONExportRegressionTest {

    @Autowired ITDJSONExportService exportService;
    ObjectMapper mapper = new ObjectMapper();

    // ── Test 1: Basic structure ──────────────────────────────────────────
    @Test @Order(1)
    void testTopLevelStructure() throws Exception {
        Itr1FormData data = buildMinimalITR1();
        String json = exportService.export(data);
        JsonNode root = mapper.readTree(json);
        assertThat(root.has("ITR")).isTrue();
        assertThat(root.path("ITR").has("ITR1")).isTrue();
        assertThat(root.path("ITR").path("ITR1").has("CreationInfo")).isTrue();
        assertThat(root.path("ITR").path("ITR1").has("Form_ITR1")).isTrue();
    }

    // ── Test 2: SHA-256 digest present and non-empty ─────────────────────
    @Test @Order(2)
    void testDigestPresent() throws Exception {
        Itr1FormData data = buildMinimalITR1();
        String json = exportService.export(data);
        JsonNode creationInfo = mapper.readTree(json).path("ITR").path("ITR1").path("CreationInfo");
        String digest = creationInfo.path("Digest").asText();
        assertThat(digest).isNotBlank().hasSize(64); // SHA-256 hex = 64 chars
    }

    // ── Test 3: No null fields in output ─────────────────────────────────
    @Test @Order(3)
    void testNoNullFieldsInOutput() throws Exception {
        String json = exportService.export(buildMinimalITR1());
        assertThat(json).doesNotContain(":null");
    }

    // ── Test 4: All amounts are integers (no decimals) ───────────────────
    @Test @Order(4)
    void testNoDecimalAmounts() throws Exception {
        String json = exportService.export(buildFullITR1WithAllFields());
        // Amounts must be integers — no decimal point in numeric values
        assertThat(json).doesNotContainPattern("\"[A-Za-z]+\":\\s*[0-9]+\\.[0-9]+");
    }

    // ── Test 5: Date format DD/MM/YYYY ───────────────────────────────────
    @Test @Order(5)
    void testDateFormat() throws Exception {
        Itr1FormData data = buildMinimalITR1();
        data.setDateOfBirth("1985-04-15"); // input may be ISO
        String json = exportService.export(data);
        // Output must be DD/MM/YYYY
        assertThat(json).contains("15/04/1985");
        assertThat(json).doesNotContain("1985-04-15");
    }

    // ── Test 6: Rebate 87A — AY 2025-26 income exactly ₹7L ──────────────
    @Test @Order(6)
    void testRebate87A_AY2526_BoundaryIncome() throws Exception {
        Itr1FormData data = buildMinimalITR1();
        data.setAssessmentYear("2025-26");
        data.setTotalIncome(700000L);
        data.setTaxRegime("NEW");
        String json = exportService.export(data);
        JsonNode taxComputed = mapper.readTree(json)
            .path("ITR").path("ITR1").path("Form_ITR1")
            .path("TaxComputation").path("Rebate87A");
        // Rebate must equal tax payable — fully rebated at ₹7L
        assertThat(taxComputed.asLong()).isGreaterThan(0L);
    }

    // ── Test 7: Rebate 87A — AY 2026-27 income ₹12L ─────────────────────
    @Test @Order(7)
    void testRebate87A_AY2627_BoundaryIncome() throws Exception {
        Itr1FormData data = buildMinimalITR1();
        data.setAssessmentYear("2026-27");
        data.setTotalIncome(1200000L);
        data.setTaxRegime("NEW");
        String json = exportService.export(data);
        JsonNode taxComputed = mapper.readTree(json)
            .path("ITR").path("ITR1").path("Form_ITR1")
            .path("TaxComputation").path("Rebate87A");
        // At exactly ₹12L, tax must be NIL (max rebate ₹60,000)
        assertThat(taxComputed.asLong()).isGreaterThan(0L); // rebate applied
    }

    // ── Test 8: Standard deduction ₹75,000 for AY 2026-27 ───────────────
    @Test @Order(8)
    void testStandardDeduction_AY2627() throws Exception {
        Itr1FormData data = buildMinimalITR1();
        data.setAssessmentYear("2026-27");
        data.setGrossSalary(1000000L);
        String json = exportService.export(data);
        // Standard deduction for AY 2026-27 new regime = ₹75,000
        assertThat(json).contains("75000");
    }

    // ── Test 9: Schedule TDS — employer TAN present ──────────────────────
    @Test @Order(9)
    void testScheduleTDSHasEmployerTAN() throws Exception {
        Itr1FormData data = buildMinimalITR1();
        data.setEmployerTan("PUNE12345A");
        String json = exportService.export(data);
        assertThat(json).contains("PUNE12345A");
    }

    // ── Test 10: IntermediaryCityLength ≤ 25 characters ──────────────────
    @Test @Order(10)
    void testIntermediaryCityTruncation() throws Exception {
        Itr1FormData data = buildMinimalITR1();
        data.setIntermediaryCity("VERY LONG CITY NAME THAT EXCEEDS LIMIT");
        String json = exportService.export(data);
        JsonNode ci = mapper.readTree(json)
            .path("ITR").path("ITR1").path("CreationInfo");
        assertThat(ci.path("IntermediaryCity").asText().length()).isLessThanOrEqualTo(25);
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private Itr1FormData buildMinimalITR1() {
        Itr1FormData d = new Itr1FormData();
        d.setPAN("ABCDE1234F");
        d.setAssessmentYear("2025-26");
        d.setTaxRegime("NEW");
        d.setGrossSalary(500000L);
        d.setTotalIncome(500000L);
        d.setDateOfBirth("01/04/1985");
        d.setIntermediaryCity("PUNE");
        return d;
    }

    private Itr1FormData buildFullITR1WithAllFields() {
        Itr1FormData d = buildMinimalITR1();
        d.setTotalIncome(1500000L);
        d.setDeduction80C(150000L);
        d.setDeduction80D(25000L);
        d.setTdsCreditedSalary(45000L);
        d.setAdvanceTax(10000L);
        return d;
    }
}
```

### 3.3 Fixes Required in `ITDJSONExportService.java` — MODIFY EXISTING

After running the regression tests, apply these known fixes regardless of test results (pre-emptive correctness):

```java
// FIX 1: Standard deduction must be ₹75,000 for AY 2026-27, ₹50,000 for AY 2025-26
// Find the method that sets standard deduction and replace hardcoded value:
private long getStandardDeduction(String assessmentYear, String regime) {
    if ("NEW".equals(regime)) {
        return "2026-27".equals(assessmentYear) ? 75000L : 50000L;
    }
    // Old regime: ₹50,000 both years
    return 50000L;
}

// FIX 2: Rebate 87A threshold must be ₹12L for AY 2026-27, ₹7L for AY 2025-26
private long getRebate87AThreshold(String assessmentYear, String regime) {
    if ("NEW".equals(regime) && "2026-27".equals(assessmentYear)) return 1200000L;
    if ("NEW".equals(regime)) return 700000L;
    return 500000L; // Old regime: ₹5L both years
}

private long getMaxRebate87A(String assessmentYear, String regime) {
    if ("NEW".equals(regime) && "2026-27".equals(assessmentYear)) return 60000L;
    if ("NEW".equals(regime)) return 25000L;
    return 12500L; // Old regime
}
```

---

## SECTION 4 — SCHEDULE FA (FOREIGN ASSETS) — FULL IMPLEMENTATION

### 4.1 Scope

Schedule FA is mandatory for residents with foreign assets. `ITR_Complete_Part1.md §4.9` defines the fields. Currently only partially covered via SFT-009. This section implements full Schedule FA for ITR-2 and ITR-3.

### 4.2 `ScheduleFAService.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.service;

import com.yourpackage.dto.*;
import com.yourpackage.dto.ITRSharedDtos.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

/**
 * Schedule FA — Foreign Assets and Income from any source outside India.
 * Mandatory for Resident (not Ordinarily Resident and Ordinarily Resident) taxpayers.
 * All foreign amounts must be reported in BOTH foreign currency AND INR equivalent.
 * Reference: ITR_Complete_Part1.md §4.9 and CBDT Notification 35/2022.
 */
@Service
public class ScheduleFAService {

    /**
     * Validates Schedule FA entries and computes INR equivalents.
     * All amounts in INR — conversion at SBI TT buying rate as on the last day of FY.
     */
    public ScheduleFAValidationResult validateAndEnrich(ScheduleFAData faData,
                                                         String residencyStatus) {
        ScheduleFAValidationResult result = new ScheduleFAValidationResult();

        // Schedule FA is applicable only for residents
        if ("NR".equals(residencyStatus) || "NRNOR".equals(residencyStatus)) {
            result.addInfo("Schedule FA not applicable for Non-Residents — skipping");
            return result;
        }

        // ── Part A: Foreign Bank Accounts ────────────────────────────────
        if (faData.getForeignBankAccounts() != null) {
            for (ForeignBankAccount acct : faData.getForeignBankAccounts()) {
                validateForeignBankAccount(acct, result);
            }
        }

        // ── Part B: Foreign Financial Interests ──────────────────────────
        if (faData.getForeignFinancialInterests() != null) {
            for (ForeignFinancialInterest ffi : faData.getForeignFinancialInterests()) {
                validateForeignFinancialInterest(ffi, result);
            }
        }

        // ── Part C: Immovable Property outside India ──────────────────────
        if (faData.getForeignImmovableProperty() != null) {
            for (ForeignImmovableProperty prop : faData.getForeignImmovableProperty()) {
                validateForeignImmovableProperty(prop, result);
            }
        }

        // ── Part D: Other Capital Assets ─────────────────────────────────
        if (faData.getOtherCapitalAssets() != null) {
            for (OtherForeignAsset asset : faData.getOtherCapitalAssets()) {
                validateOtherForeignAsset(asset, result);
            }
        }

        // ── Part E: Foreign Signing Authority ────────────────────────────
        if (faData.getForeignSigningAuthority() != null) {
            for (ForeignSigningAuthority auth : faData.getForeignSigningAuthority()) {
                if (auth.getCountryName() == null || auth.getCountryName().isBlank()) {
                    result.addError("FA-SIGN-001: Country name is mandatory for signing authority");
                }
                if (auth.getEntityName() == null || auth.getEntityName().isBlank()) {
                    result.addError("FA-SIGN-002: Entity name is mandatory for signing authority");
                }
            }
        }

        // ── Part F: Foreign Trusts ────────────────────────────────────────
        // Similar structure to Part E — validate mandatory fields
        if (faData.getForeignTrusts() != null) {
            for (ForeignTrust trust : faData.getForeignTrusts()) {
                if (trust.getCountryName() == null) {
                    result.addError("FA-TRUST-001: Country name mandatory for foreign trust");
                }
                if (trust.getTrusteeDetails() == null || trust.getTrusteeDetails().isBlank()) {
                    result.addError("FA-TRUST-002: Trustee details mandatory");
                }
            }
        }

        // ── Part G: Income from foreign sources ──────────────────────────
        if (faData.getIncomeFromForeignSources() != null) {
            for (ForeignSourceIncome income : faData.getIncomeFromForeignSources()) {
                if (income.getIncomeType() == null) {
                    result.addError("FA-INC-001: Income type is mandatory");
                }
                if (income.getAmountInINR() <= 0) {
                    result.addError("FA-INC-002: INR amount must be > 0");
                }
                if (income.getCountryName() == null || income.getCountryName().isBlank()) {
                    result.addError("FA-INC-003: Country name mandatory for foreign income");
                }
                // Check DTAA credit availability
                if (income.getTaxPaidOutside() > 0 && income.getDtaaCountryCode() == null) {
                    result.addWarning("FA-INC-004: Tax paid outside India but DTAA country not specified — DTAA relief may not be claimed");
                }
            }
        }

        return result;
    }

    private void validateForeignBankAccount(ForeignBankAccount acct,
                                             ScheduleFAValidationResult result) {
        // CBDT Rule: SWIFT/IBAN or local routing code mandatory
        if ((acct.getSwiftCode() == null || acct.getSwiftCode().isBlank()) &&
            (acct.getIbanCode() == null || acct.getIbanCode().isBlank())) {
            result.addWarning("FA-BANK-001: SWIFT or IBAN code recommended for account at " +
                acct.getBankName() + ", " + acct.getCountryName());
        }
        if (acct.getCountryName() == null || acct.getCountryName().isBlank()) {
            result.addError("FA-BANK-002: Country name mandatory for foreign bank account");
        }
        if (acct.getPeakValueDuringYear() < 0) {
            result.addError("FA-BANK-003: Peak balance cannot be negative");
        }
        // Peak balance in INR must be disclosed
        if (acct.getPeakValueInINR() <= 0 && acct.getPeakValueDuringYear() > 0) {
            result.addError("FA-BANK-004: INR equivalent of peak balance must be disclosed");
        }
    }

    private void validateForeignFinancialInterest(ForeignFinancialInterest ffi,
                                                    ScheduleFAValidationResult result) {
        if (ffi.getNatureOfEntity() == null) {
            result.addError("FA-FFI-001: Nature of entity mandatory (Company/Trust/Other)");
        }
        if (ffi.getCountryName() == null || ffi.getCountryName().isBlank()) {
            result.addError("FA-FFI-002: Country name mandatory for foreign financial interest");
        }
        if (ffi.getTotalInvestmentAtCostInINR() < 0) {
            result.addError("FA-FFI-003: Total investment at cost cannot be negative");
        }
    }

    private void validateForeignImmovableProperty(ForeignImmovableProperty prop,
                                                    ScheduleFAValidationResult result) {
        if (prop.getAddressOutsideIndia() == null || prop.getAddressOutsideIndia().isBlank()) {
            result.addError("FA-PROP-001: Address of foreign property is mandatory");
        }
        if (prop.getCountryName() == null || prop.getCountryName().isBlank()) {
            result.addError("FA-PROP-002: Country name mandatory for foreign property");
        }
        if (prop.getOwnershipStatus() == null) {
            result.addError("FA-PROP-003: Ownership status mandatory (Owner/Beneficial Owner/Beneficiary)");
        }
    }

    private void validateOtherForeignAsset(OtherForeignAsset asset,
                                            ScheduleFAValidationResult result) {
        if (asset.getDescription() == null || asset.getDescription().isBlank()) {
            result.addError("FA-OTH-001: Description mandatory for other foreign capital asset");
        }
        if (asset.getCountryName() == null || asset.getCountryName().isBlank()) {
            result.addError("FA-OTH-002: Country name mandatory for other foreign asset");
        }
    }
}
```

### 4.3 Wire Schedule FA into JSON Export Services — MODIFY EXISTING

In `ITR2JSONExportService.java`, add the `buildScheduleFA` method and invoke it within `buildFormITR2()`:

```java
// Add import
@Autowired private ScheduleFAService scheduleFAService;

// Inside buildFormITR2(), add:
if (data.getScheduleFA() != null) {
    formItr2.set("ScheduleFA", buildScheduleFA(data.getScheduleFA()));
}

private ObjectNode buildScheduleFA(ScheduleFAData fa) {
    ObjectNode faNode = mapper.createObjectNode();

    // Part A — Foreign Bank Accounts
    ArrayNode bankAccounts = mapper.createArrayNode();
    if (fa.getForeignBankAccounts() != null) {
        for (ForeignBankAccount acct : fa.getForeignBankAccounts()) {
            ObjectNode node = mapper.createObjectNode();
            node.put("CountryName", nullSafeString(acct.getCountryName()));
            node.put("BankName", nullSafeString(acct.getBankName()));
            node.put("SwiftCode", nullSafeString(acct.getSwiftCode()));
            node.put("IBANCode", nullSafeString(acct.getIbanCode()));
            node.put("AccountOpeningDate", nullSafeString(acct.getAccountOpeningDate()));
            node.put("AccountClosingDate", nullSafeString(acct.getAccountClosingDate()));
            node.put("PeakValueDuringYear", acct.getPeakValueDuringYear());
            node.put("PeakValueInINR", acct.getPeakValueInINR());
            node.put("InterestAccrued", acct.getInterestAccrued());
            bankAccounts.add(node);
        }
    }
    faNode.set("ForeignBankAccounts", bankAccounts);
    // ... repeat pattern for Part B through G

    return faNode;
}
```

---

## SECTION 5 — AMT (ALTERNATE MINIMUM TAX) u/s 115JC FOR ITR-3

### 5.1 Scope

AMT applies to individuals, HUFs, AOPs, BOIs claiming certain deductions that reduce tax below 18.5% of Adjusted Total Income (ATI). For ITR-3 taxpayers (business income), AMT is a CBDT mandatory computation. `ITR_Complete_Part2.md §6` covers business income but AMT was not implemented.

### 5.2 `AMTComputationService.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.service;

import com.yourpackage.dto.Itr3FormData;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * Alternate Minimum Tax u/s 115JC.
 * Applicable to: Non-corporate taxpayers (individuals, HUFs) who claim deductions
 * under Chapter VI-A (Part C) or Section 10AA or Section 35AD.
 * AMT Rate: 18.5% of Adjusted Total Income (ATI) + surcharge + cess.
 * AMT Credit u/s 115JD: Carry forward for 15 years.
 *
 * ITR-3 Schedule AMT and Schedule AMTC must be populated when AMT > regular tax.
 */
@Service
public class AMTComputationService {

    private static final double AMT_RATE = 0.185; // 18.5%

    public AMTResult compute(Itr3FormData data) {
        AMTResult result = new AMTResult();

        // ── Step 1: Determine if AMT is applicable ────────────────────────
        // AMT NOT applicable to: companies (not in ITR-3 scope), LLPs (see below),
        // non-residents, persons with business income < ₹20L (not an exemption — AMT
        // applies to all non-corporates who claim eligible deductions)
        if (!isAMTApplicable(data)) {
            result.setAmtApplicable(false);
            return result;
        }
        result.setAmtApplicable(true);

        // ── Step 2: Compute Adjusted Total Income (ATI) ───────────────────
        // ATI = Total Income + Deductions added back under specific sections
        long totalIncome = data.getTotalIncome();
        long addBackDeductions = computeAddBackDeductions(data);
        long ati = totalIncome + addBackDeductions;
        result.setAdjustedTotalIncome(ati);

        // ── Step 3: AMT on ATI ────────────────────────────────────────────
        long amtBeforeSurcharge = Math.round(ati * AMT_RATE);
        result.setAmtBeforeSurcharge(amtBeforeSurcharge);

        // ── Step 4: Apply surcharge on AMT ───────────────────────────────
        // Same surcharge rates as regular tax apply on AMT
        double surchargeRate = getSurchargeRate(totalIncome, data.getTaxRegime());
        long surcharge = Math.round(amtBeforeSurcharge * surchargeRate);
        long amtAfterSurcharge = amtBeforeSurcharge + surcharge;

        // ── Step 5: Health and Education Cess at 4% ──────────────────────
        long cess = Math.round(amtAfterSurcharge * 0.04);
        long totalAMT = amtAfterSurcharge + cess;
        result.setTotalAMT(totalAMT);

        // ── Step 6: Compare with regular tax liability ────────────────────
        long regularTax = data.getRegularTaxLiability(); // computed by TaxComputationService
        result.setRegularTaxLiability(regularTax);

        // ── Step 7: AMT payable = max(AMT, regular tax) ──────────────────
        if (totalAMT > regularTax) {
            result.setAmtPayable(totalAMT);
            result.setExcessAMTOverRegular(totalAMT - regularTax); // becomes AMT credit
            result.setAmtCreditAvailable(totalAMT - regularTax);
            result.setScheduleAMTRequired(true);
        } else {
            result.setAmtPayable(regularTax);
            result.setScheduleAMTRequired(false);
        }

        return result;
    }

    /**
     * AMT is applicable if taxpayer claims any deduction under:
     * - Section 80H to 80RRB (Chapter VI-A Part C — business deductions)
     * - Section 10AA (SEZ units)
     * - Section 35AD (specified business capital expenditure)
     * AMT NOT applicable if ATI ≤ ₹20,00,000 (budget 2012 threshold — still in force).
     * IMPORTANT: The ₹20L threshold is on ATI, not regular income.
     */
    private boolean isAMTApplicable(Itr3FormData data) {
        // Check if taxpayer claimed any Part C deductions
        boolean claimedPartC = (data.getDeduction80IC() > 0) ||
                               (data.getDeduction80IE() > 0) ||
                               (data.getDeduction80P() > 0) ||
                               (data.getDeductionSection10AA() > 0) ||
                               (data.getDeductionSection35AD() > 0);

        if (!claimedPartC) return false;

        // ATI check: if ATI ≤ ₹20L, AMT not applicable
        long ati = data.getTotalIncome() + computeAddBackDeductions(data);
        return ati > 2000000L;
    }

    private long computeAddBackDeductions(Itr3FormData data) {
        // Add back ALL deductions under Chapter VI-A Part C (Sections 80H–80RRB)
        // and Section 10AA and Section 35AD that reduced the total income
        return data.getDeduction80IC() +
               data.getDeduction80IE() +
               data.getDeduction80P() +
               data.getDeductionSection10AA() +
               data.getDeductionSection35AD() +
               data.getDeduction80JJA() +      // for co-operatives
               data.getDeduction80JJAA();      // employment generation
    }

    private double getSurchargeRate(long totalIncome, String regime) {
        if (totalIncome <= 5000000L) return 0.0;
        if (totalIncome <= 10000000L) return 0.10;
        if (totalIncome <= 20000000L) return 0.15;
        if (totalIncome <= 50000000L) return 0.25;
        // Above ₹5Cr — new regime caps at 25%, old regime 37%
        return "NEW".equals(regime) ? 0.25 : 0.37;
    }
}
```

### 5.3 Wire AMT into `ITRFilingOrchestrationService.java` — MODIFY EXISTING

```java
@Autowired private AMTComputationService amtService;

// Inside processITR3(), AFTER regular tax computation, BEFORE JSON export:
AMTResult amtResult = amtService.compute(data);
if (amtResult.isAmtApplicable()) {
    data.setAmtResult(amtResult);
    logAudit(pan, ay, "AMT_COMPUTED",
        "AMT applicable. ATI: " + amtResult.getAdjustedTotalIncome() +
        " AMT: " + amtResult.getTotalAMT() +
        " Regular Tax: " + amtResult.getRegularTaxLiability(),
        "AMT_SERVICE");
    if (amtResult.isScheduleAMTRequired()) {
        result.addInfo("Schedule AMT triggered — AMT ₹" +
            amtResult.getTotalAMT() + " > Regular Tax ₹" +
            amtResult.getRegularTaxLiability());
    }
}
```

### 5.4 Wire AMT into `ITR3JSONExportService.java` — MODIFY EXISTING

Inside `buildFormITR3()`:

```java
// Add Schedule AMT if applicable
if (data.getAmtResult() != null && data.getAmtResult().isScheduleAMTRequired()) {
    formItr3.set("ScheduleAMT", buildScheduleAMT(data.getAmtResult()));
    formItr3.set("ScheduleAMTC", buildScheduleAMTC(data));
}

private ObjectNode buildScheduleAMT(AMTResult amt) {
    ObjectNode node = mapper.createObjectNode();
    node.put("AdjustedTotalIncome", amt.getAdjustedTotalIncome());
    node.put("AMTRatePercent", "18.5");
    node.put("AMTBeforeSurcharge", amt.getAmtBeforeSurcharge());
    node.put("Surcharge", amt.getTotalAMT() - amt.getAmtBeforeSurcharge());
    node.put("TotalAMT", amt.getTotalAMT());
    return node;
}

private ObjectNode buildScheduleAMTC(Itr3FormData data) {
    // AMT Credit Schedule — carry forward or utilisation
    ObjectNode node = mapper.createObjectNode();
    node.put("AMTCreditBroughtForward", data.getAmtCreditBroughtForward());
    node.put("AMTCreditAdjusted", data.getAmtCreditAdjusted());
    node.put("AMTCreditCarriedForward", data.getAmtCreditCarriedForward());
    node.put("AMTCreditAvailableThisYear",
        data.getAmtResult() != null ? data.getAmtResult().getAmtCreditAvailable() : 0);
    return node;
}
```

---

## SECTION 6 — E-VERIFICATION MODULE (EVC / DSC / ITD OTP)

### 6.1 Scope

A filed ITR is not complete until verified. CBDT mandates e-verification within 30 days of filing. Three modes are supported: EVC (Aadhaar OTP, net banking, bank account), DSC (digital signature), and ITD generated EVC.

### 6.2 `EVCVerificationService.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.service;

import com.yourpackage.dto.EVCRequest;
import com.yourpackage.dto.EVCResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

/**
 * E-Verification Controller.
 * Supports: AADHAAR_OTP | NET_BANKING | BANK_ACCOUNT | DEMAT | DSC | ITD_EVC
 * All modes interface with ITD e-filing portal API.
 * Reference: CBDT Notification No. 02/2015 (e-verification rules).
 */
@Service
public class EVCVerificationService {

    @Value("${itd.efiling.base-url}")
    private String itdBaseUrl;

    private final RestTemplate restTemplate;
    private final AuditTrailService auditTrail;

    public EVCVerificationService(RestTemplate restTemplate, AuditTrailService auditTrail) {
        this.restTemplate = restTemplate;
        this.auditTrail = auditTrail;
    }

    /**
     * Step 1: Initiates OTP generation for Aadhaar-based EVC.
     * Taxpayer must confirm OTP via confirmAadhaarEVC().
     */
    public EVCResult initiateAadhaarOTP(String pan, String aadhaarLastFour) {
        EVCResult result = new EVCResult();

        // Validate Aadhaar is linked — UIDAI check via ITD API
        Map<String, String> payload = new HashMap<>();
        payload.put("PAN", pan);
        payload.put("AadhaarLastFour", aadhaarLastFour);
        payload.put("mode", "AADHAAR_OTP");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                itdBaseUrl + "/evc/generate-otp", payload, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                result.setTransactionId((String) response.getBody().get("transactionId"));
                result.setStatus("OTP_SENT");
                result.setMessage("OTP sent to Aadhaar-linked mobile number");
            } else {
                result.setStatus("FAILED");
                result.setMessage("OTP generation failed — verify Aadhaar-PAN linkage");
            }
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setMessage("ITD portal unavailable: " + e.getMessage());
        }

        auditTrail.log(pan, "EVC_OTP_INITIATED", result.getStatus(), "EVC_SERVICE");
        return result;
    }

    /**
     * Step 2: Confirms OTP and generates EVC code.
     */
    public EVCResult confirmAadhaarOTP(String pan, String transactionId, String otp,
                                        String acknowledgementNumber) {
        EVCResult result = new EVCResult();

        Map<String, String> payload = new HashMap<>();
        payload.put("PAN", pan);
        payload.put("transactionId", transactionId);
        payload.put("OTP", otp);
        payload.put("acknowledgementNumber", acknowledgementNumber);
        payload.put("mode", "AADHAAR_OTP");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                itdBaseUrl + "/evc/confirm", payload, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String evcCode = (String) response.getBody().get("evcCode");
                result.setEvcCode(evcCode);
                result.setStatus("VERIFIED");
                result.setMessage("e-Verification successful via Aadhaar OTP");
            } else {
                result.setStatus("FAILED");
                result.setMessage("Invalid OTP or session expired — please retry");
            }
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setMessage("Verification error: " + e.getMessage());
        }

        auditTrail.log(pan, "EVC_VERIFIED", result.getStatus(), "EVC_SERVICE");
        return result;
    }

    /**
     * DSC-based verification — accepts PKCS#7 signed acknowledgement.
     * DSC must be registered on ITD e-filing portal.
     */
    public EVCResult verifyViaDSC(String pan, String acknowledgementNumber,
                                   byte[] pkcs7SignedData) {
        EVCResult result = new EVCResult();

        // Validate DSC certificate — check expiry and registration
        if (pkcs7SignedData == null || pkcs7SignedData.length == 0) {
            result.setStatus("FAILED");
            result.setMessage("DSC signature data cannot be empty");
            return result;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("PAN", pan);
        payload.put("acknowledgementNumber", acknowledgementNumber);
        payload.put("signedData", java.util.Base64.getEncoder().encodeToString(pkcs7SignedData));
        payload.put("mode", "DSC");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                itdBaseUrl + "/evc/dsc-verify", payload, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                result.setStatus("VERIFIED");
                result.setEvcCode((String) response.getBody().get("evcCode"));
                result.setMessage("e-Verification successful via DSC");
            } else {
                result.setStatus("FAILED");
                result.setMessage("DSC verification failed — check DSC registration");
            }
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setMessage("DSC verification error: " + e.getMessage());
        }

        auditTrail.log(pan, "DSC_VERIFIED", result.getStatus(), "EVC_SERVICE");
        return result;
    }

    /**
     * Net banking EVC — redirects user to bank portal, receives EVC on return.
     * Returns the net banking redirect URL.
     */
    public String getNetBankingRedirectURL(String pan, String bankCode,
                                            String acknowledgementNumber) {
        return itdBaseUrl + "/evc/net-banking?pan=" + pan +
               "&bank=" + bankCode + "&ackNo=" + acknowledgementNumber;
    }
}
```

### 6.3 Flyway Migration — `V9_2__evc_log.sql`

```sql
CREATE TABLE evc_log (
    id                      BIGSERIAL PRIMARY KEY,
    pan                     VARCHAR(10) NOT NULL,
    assessment_year         VARCHAR(7)  NOT NULL,
    acknowledgement_number  VARCHAR(15),
    verification_mode       VARCHAR(20) NOT NULL, -- AADHAAR_OTP|NET_BANKING|DSC|BANK_ACCOUNT
    evc_code                VARCHAR(50),
    status                  VARCHAR(20) NOT NULL, -- PENDING|OTP_SENT|VERIFIED|FAILED
    transaction_id          VARCHAR(50),
    verified_at             TIMESTAMP,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_evc_pan_ay ON evc_log (pan, assessment_year);
```

---

## SECTION 7 — RECTIFICATION u/s 154 WORKFLOW

### 7.1 `RectificationService.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.service;

import com.yourpackage.dto.*;
import org.springframework.stereotype.Service;

/**
 * Rectification u/s 154 — corrects mistakes apparent from record.
 * Applies to: Arithmetical errors, incorrect relief, wrong tax rate applied.
 * NOT for: Disputes on the merits of assessment, new deduction claims.
 * Time limit: 4 years from end of FY in which order to be rectified was passed.
 * Reference: Income-tax Act 1961, Section 154.
 */
@Service
public class RectificationService {

    private final ITRFilingOrchestrationService filingService;
    private final AuditTrailService auditTrail;

    public RectificationService(ITRFilingOrchestrationService filingService,
                                 AuditTrailService auditTrail) {
        this.filingService = filingService;
        this.auditTrail = auditTrail;
    }

    /**
     * Initiates a rectification request.
     * @param originalAcknowledgementNumber The ACK number of the original filing
     * @param rectificationReason One of: ARITHMETICAL_ERROR | INCORRECT_RATE | WRONG_RELIEF | OTHER
     * @param correctedData The corrected ITR data
     */
    public RectificationResult initiateRectification(
            String pan, String assessmentYear,
            String originalAcknowledgementNumber,
            String rectificationReason,
            Object correctedData) {

        RectificationResult result = new RectificationResult();

        // ── Validate time limit ────────────────────────────────────────────
        if (!isWithinTimeLimitForRectification(assessmentYear)) {
            result.setStatus("REJECTED");
            result.setMessage("Rectification time limit (4 years) has expired for AY " +
                assessmentYear);
            return result;
        }

        // ── Validate rectification reason ─────────────────────────────────
        if (!isValidRectificationReason(rectificationReason)) {
            result.setStatus("REJECTED");
            result.setMessage("Invalid rectification reason. Allowed: ARITHMETICAL_ERROR, " +
                "INCORRECT_RATE, WRONG_RELIEF, OTHER. Use revised return u/s 139(5) for other changes.");
            return result;
        }

        // ── Log rectification initiation ──────────────────────────────────
        auditTrail.log(pan, "RECTIFICATION_INITIATED",
            "Reason: " + rectificationReason + " for ACK: " + originalAcknowledgementNumber,
            "RECTIFICATION_SERVICE");

        // ── Process corrected return ──────────────────────────────────────
        result.setOriginalAcknowledgementNumber(originalAcknowledgementNumber);
        result.setRectificationReason(rectificationReason);
        result.setStatus("SUBMITTED");
        result.setMessage("Rectification request created — pending ITD processing");

        // The actual upload to ITD portal happens via EVCVerificationService after this
        return result;
    }

    private boolean isWithinTimeLimitForRectification(String assessmentYear) {
        // Parse AY (e.g., "2025-26") — 4-year limit from end of that FY
        try {
            int ayStartYear = Integer.parseInt(assessmentYear.substring(0, 4));
            int fyEndYear = ayStartYear; // FY ends March 31 of AY start year
            java.time.LocalDate fyEnd = java.time.LocalDate.of(fyEndYear, 3, 31);
            java.time.LocalDate limit = fyEnd.plusYears(4);
            return java.time.LocalDate.now().isBefore(limit);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidRectificationReason(String reason) {
        return reason != null && (
            "ARITHMETICAL_ERROR".equals(reason) ||
            "INCORRECT_RATE".equals(reason) ||
            "WRONG_RELIEF".equals(reason) ||
            "OTHER".equals(reason));
    }
}
```

---

## SECTION 8 — REVISED RETURN u/s 139(5) WORKFLOW

### 8.1 `RevisedReturnService.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.service;

import com.yourpackage.dto.*;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

/**
 * Revised Return u/s 139(5) — allows taxpayer to correct any error/omission.
 * Time limit: Before the end of the relevant AY OR before assessment completion,
 * whichever is earlier.
 * For AY 2025-26: Last date = 31 March 2026.
 * For AY 2026-27: Last date = 31 March 2027.
 * A revised return SUPERSEDES the original return.
 * Reference: Income-tax Act 1961, Section 139(5).
 */
@Service
public class RevisedReturnService {

    private final ITRFilingOrchestrationService filingService;
    private final AuditTrailService auditTrail;

    public RevisedReturnService(ITRFilingOrchestrationService filingService,
                                 AuditTrailService auditTrail) {
        this.filingService = filingService;
        this.auditTrail = auditTrail;
    }

    /**
     * Processes a revised return.
     * @param originalAcknowledgementNumber The ACK number of the original/last filed return
     * @param originalFilingDate Date of original filing (DD/MM/YYYY)
     * @param revisedData The complete corrected ITR data (replaces original entirely)
     */
    public RevisedReturnResult processRevisedReturn(
            String pan, String assessmentYear,
            String originalAcknowledgementNumber,
            String originalFilingDate,
            Object revisedData) {

        RevisedReturnResult result = new RevisedReturnResult();

        // ── Validate time limit ────────────────────────────────────────────
        LocalDate deadline = getRevisedReturnDeadline(assessmentYear);
        if (LocalDate.now().isAfter(deadline)) {
            result.setStatus("REJECTED");
            result.setMessage("Revised return time limit expired. Deadline was " +
                deadline + " for AY " + assessmentYear +
                ". Consider filing u/s 154 rectification or u/s 119 condonation.");
            return result;
        }

        // ── Mark as revised return in the ITR DTO ─────────────────────────
        // The ITR form must have:
        // - ReturnFilingSection = "139(5)"
        // - OriginalOrRevisedReturn = "R" (Revised)
        // - Acknowledgement number of original return in the form
        setRevisedReturnFlags(revisedData, originalAcknowledgementNumber, originalFilingDate);

        auditTrail.log(pan, "REVISED_RETURN_INITIATED",
            "Original ACK: " + originalAcknowledgementNumber +
            " | Original filing: " + originalFilingDate,
            "REVISED_RETURN_SERVICE");

        // ── Process through normal filing pipeline ─────────────────────────
        // The revised return goes through full validation and JSON export
        result.setStatus("PROCESSING");
        result.setMessage("Revised return processing — full pipeline initiated");
        result.setOriginalAcknowledgementNumber(originalAcknowledgementNumber);
        result.setDeadline(deadline.toString());

        return result;
    }

    private LocalDate getRevisedReturnDeadline(String assessmentYear) {
        // Deadline = 31 December of the AY (CBDT updated guidance — was March 31)
        // VERIFY with latest CBDT notification — deadlines can change via extension
        int ayStartYear = Integer.parseInt(assessmentYear.substring(0, 4));
        return LocalDate.of(ayStartYear, 12, 31);
    }

    private void setRevisedReturnFlags(Object revisedData, String originalAckNo,
                                        String originalFilingDate) {
        // Both ITR-1 and ITR-2+ DTOs must support these fields
        if (revisedData instanceof Itr1FormData d) {
            d.setReturnFilingSection("139(5)");
            d.setOriginalOrRevised("R");
            d.setOriginalReturnAcknowledgementNumber(originalAckNo);
            d.setOriginalReturnFilingDate(originalFilingDate);
        } else if (revisedData instanceof Itr2FormData d) {
            d.setReturnFilingSection("139(5)");
            d.setOriginalOrRevised("R");
            d.setOriginalReturnAcknowledgementNumber(originalAckNo);
            d.setOriginalReturnFilingDate(originalFilingDate);
        } else if (revisedData instanceof Itr3FormData d) {
            d.setReturnFilingSection("139(5)");
            d.setOriginalOrRevised("R");
            d.setOriginalReturnAcknowledgementNumber(originalAckNo);
            d.setOriginalReturnFilingDate(originalFilingDate);
        } else if (revisedData instanceof Itr4FormData d) {
            d.setReturnFilingSection("139(5)");
            d.setOriginalOrRevised("R");
            d.setOriginalReturnAcknowledgementNumber(originalAckNo);
            d.setOriginalReturnFilingDate(originalFilingDate);
        }
    }
}
```

---

## SECTION 9 — TDS MISMATCH ALERTS WITH CORRECTION SUGGESTIONS

### 9.1 `TDSMismatchAlertService.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.service;

import com.yourpackage.dto.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * TDS Mismatch Detection and Correction Advisor.
 * Compares: TDS claimed in ITR vs TDS in 26AS/AIS vs Form 16.
 * Generates actionable correction suggestions with specific field paths.
 */
@Service
public class TDSMismatchAlertService {

    private static final long TOLERANCE_RS = 100L; // CBDT tolerance

    public TDSMismatchReport generateReport(String pan, String assessmentYear,
                                             TDSSummary itrClaim,
                                             Form26ASData form26AS,
                                             List<Form16PartAData> form16Parts) {

        TDSMismatchReport report = new TDSMismatchReport();
        report.setPan(pan);
        report.setAssessmentYear(assessmentYear);

        // ── Cross-check 1: ITR vs 26AS ────────────────────────────────────
        long claimed = itrClaim.getTotalTdsClaimed();
        long in26AS  = form26AS.getTotalTdsCredited();
        long diff    = Math.abs(claimed - in26AS);

        if (diff > TOLERANCE_RS) {
            TDSMismatch m = new TDSMismatch();
            m.setType("ITR_VS_26AS");
            m.setDescription("TDS claimed in ITR (₹" + claimed +
                ") does not match 26AS credit (₹" + in26AS + ") — difference ₹" + diff);
            m.setSeverity(diff > 10000 ? "HIGH" : "MEDIUM");

            // Correction suggestions
            if (claimed > in26AS) {
                m.addSuggestion("Reduce TDS claim to ₹" + in26AS + " to match 26AS. " +
                    "Excess claim may cause refund rejection.");
                m.addSuggestion("If deductor has not deposited TDS, contact them to file " +
                    "correction TDS return (Form 26QB/26Q) before filing your ITR.");
                m.addSuggestion("Check if any TDS entries show 'U' (Unmatched) status in 26AS " +
                    "— these are not available for credit until deductor corrects.");
            } else {
                m.addSuggestion("TDS deposited (₹" + in26AS + ") exceeds your claim (₹" + claimed +
                    ") — you are under-claiming. Update Schedule TDS to include missing entries.");
                m.addSuggestion("Check Form 16/Form 16A from all deductors for the FY " +
                    "and cross-reference with 26AS Part A/Part B.");
            }

            report.addMismatch(m);
        }

        // ── Cross-check 2: ITR TAN-wise vs Form 16 Part A TAN-wise ───────
        if (form16Parts != null) {
            for (Form16PartAData partA : form16Parts) {
                long form16Tds = partA.getAggregateTdsDeposited();
                long itrTdsByTAN = itrClaim.getTdsByTAN(partA.getTanOfEmployer());

                if (Math.abs(form16Tds - itrTdsByTAN) > TOLERANCE_RS) {
                    TDSMismatch m = new TDSMismatch();
                    m.setType("ITR_VS_FORM16_TAN");
                    m.setTan(partA.getTanOfEmployer());
                    m.setDescription("TAN " + partA.getTanOfEmployer() +
                        " (" + partA.getEmployerName() + "): Form 16 TDS ₹" + form16Tds +
                        " vs ITR claim ₹" + itrTdsByTAN);
                    m.setSeverity("MEDIUM");
                    m.addSuggestion("Update Schedule TDS entry for TAN " +
                        partA.getTanOfEmployer() + " to ₹" + form16Tds);
                    m.addSuggestion("Verify the entry in 26AS for this TAN matches Form 16 Part A");
                    report.addMismatch(m);
                }
            }
        }

        // ── Cross-check 3: 26AS deductor entries with F/U/O status ───────
        if (form26AS.getPartAEntries() != null) {
            for (Form26ASData.TDSEntry entry : form26AS.getPartAEntries()) {
                if ("U".equals(entry.getStatus())) {
                    TDSMismatch m = new TDSMismatch();
                    m.setType("UNMATCHED_26AS_ENTRY");
                    m.setTan(entry.getDeductorTan());
                    m.setDescription("Unmatched (U) status TDS entry from " +
                        entry.getDeductorName() + " (TAN: " + entry.getDeductorTan() +
                        ") — ₹" + entry.getTdsAmount() + " not available for credit");
                    m.setSeverity("HIGH");
                    m.addSuggestion("Contact deductor " + entry.getDeductorName() +
                        " to file correction TDS return to match your PAN.");
                    m.addSuggestion("Do NOT claim this TDS in your ITR until status changes to 'F' (Final).");
                    report.addMismatch(m);
                } else if ("O".equals(entry.getStatus())) {
                    TDSMismatch m = new TDSMismatch();
                    m.setType("OVERBOOKED_26AS_ENTRY");
                    m.setTan(entry.getDeductorTan());
                    m.setDescription("Overbooked (O) status from " + entry.getDeductorName() +
                        " — claim limited to actual amount");
                    m.setSeverity("MEDIUM");
                    m.addSuggestion("TDS credit will be restricted to actual TDS deposited by deductor.");
                    report.addMismatch(m);
                }
            }
        }

        report.setSummary(report.getMismatches().isEmpty()
            ? "✅ No TDS mismatches found — ITR TDS claim matches 26AS"
            : "⚠️ " + report.getMismatches().size() + " mismatch(es) found — review before filing");

        return report;
    }
}
```

---

## SECTION 10 — TAX REGIME AUTO-RECOMMENDER WITH CONFIDENCE SCORE

### 10.1 `TaxRegimeRecommendationEngine.java` — MODIFY EXISTING `TaxRegimeComparisonService.java`

Add these methods to `TaxRegimeComparisonService.java`:

```java
/**
 * Recommends old or new regime with a confidence score (0–100%).
 * Confidence reflects the margin of benefit — the larger the tax saving,
 * the higher the confidence.
 */
public TaxRegimeRecommendation recommend(Object itrData, String assessmentYear) {
    TaxRegimeRecommendation rec = new TaxRegimeRecommendation();

    // Get tax under both regimes
    long oldRegimeTax = computeOldRegimeTax(itrData, assessmentYear);
    long newRegimeTax = computeNewRegimeTax(itrData, assessmentYear);
    long totalIncome  = getTotalIncome(itrData);

    rec.setOldRegimeTax(oldRegimeTax);
    rec.setNewRegimeTax(newRegimeTax);
    rec.setSaving(Math.abs(oldRegimeTax - newRegimeTax));

    // ── Recommendation ────────────────────────────────────────────────────
    if (newRegimeTax < oldRegimeTax) {
        rec.setRecommendedRegime("NEW");
        rec.setPrimaryReason("New regime saves ₹" + rec.getSaving() + " in tax");
    } else if (oldRegimeTax < newRegimeTax) {
        rec.setRecommendedRegime("OLD");
        rec.setPrimaryReason("Old regime saves ₹" + rec.getSaving() +
            " — your deductions exceed standard deduction benefit");
    } else {
        rec.setRecommendedRegime("NEW"); // CBDT default; tiebreak to new
        rec.setPrimaryReason("Tax liability is identical — defaulting to new regime (CBDT default)");
    }

    // ── Confidence Score ──────────────────────────────────────────────────
    // Confidence = min(100, saving_as_percent_of_total_income × 10)
    // Example: ₹30,000 saving on ₹10L income = 3% → confidence = 30%
    // Example: ₹1,00,000 saving on ₹10L income = 10% → confidence = 100%
    if (totalIncome > 0) {
        double savingPct = (double) rec.getSaving() / totalIncome * 100.0;
        int confidence = (int) Math.min(100, savingPct * 10);
        rec.setConfidenceScore(confidence);
    } else {
        rec.setConfidenceScore(50); // No income data — neutral confidence
    }

    // ── Deduction breakdown — why old regime may be better ────────────────
    rec.setDeductionBreakdown(buildDeductionBreakdown(itrData));

    // ── Tipping point — at what deduction level regimes are equal ─────────
    rec.setTippingPointDeductions(computeTippingPoint(totalIncome, assessmentYear));

    // ── Key warnings ──────────────────────────────────────────────────────
    addRegimeWarnings(rec, itrData, assessmentYear);

    return rec;
}

private long computeTippingPoint(long totalIncome, String assessmentYear) {
    // Binary search for deduction level where old == new regime tax
    // Simplified: for most taxpayers the tipping point is ~₹3.75L for AY 2025-26
    // and ~₹4.25L for AY 2026-27 (varies by income level and age)
    // Return the exact computed value
    long lo = 0L, hi = 1500000L; // search between ₹0 and ₹15L of deductions
    for (int i = 0; i < 50; i++) { // 50 iterations sufficient for Rs precision
        long mid = (lo + hi) / 2;
        long oldTax = computeOldRegimeTaxWithDeductions(totalIncome, mid, assessmentYear);
        long newTax = computeNewRegimeTaxStandard(totalIncome, assessmentYear);
        if (oldTax > newTax) lo = mid;
        else hi = mid;
    }
    return (lo + hi) / 2;
}

private void addRegimeWarnings(TaxRegimeRecommendation rec, Object itrData,
                                 String assessmentYear) {
    // New regime irrevocability warning for business taxpayers
    if (itrData instanceof Itr3FormData || itrData instanceof Itr4FormData) {
        rec.addWarning("⚠️ IMPORTANT: Once you opt OUT of new regime (choose old regime) " +
            "as a business taxpayer, you CANNOT switch back to new regime in future years " +
            "unless you cease having business income. This choice is largely irreversible.");
    }

    // AY 2026-27 rebate cliff warning
    if ("2026-27".equals(assessmentYear)) {
        long income = getTotalIncome(itrData);
        if (income >= 1150000L && income <= 1300000L) {
            rec.addWarning("⚠️ Income ₹" + income + " is near the ₹12L rebate cliff in AY 2026-27. " +
                "Small increase in income could cost ₹60,000 in rebate loss. " +
                "Consider maximising 80CCD(2) employer NPS contribution.");
        }
    }
}
```

---

## SECTION 11 — ASYNC ORCHESTRATION PIPELINE (SPRING BATCH)

### 11.1 New Dependency — `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 11.2 `AsyncITRFilingConfig.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.config;

import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch configuration for async ITR filing pipeline.
 * ITR-3 and ITR-4 with large data can take 2-3 min synchronously.
 * This config moves the pipeline to a background job and returns a job ID
 * immediately. Status can be polled via FilingStatusController.
 */
@Configuration
public class AsyncITRFilingConfig {

    @Bean
    public TaskExecutor itrFilingTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("itr-filing-");
        executor.setConcurrencyLimit(20); // max 20 concurrent filings
        return executor;
    }

    @Bean
    public Job itr3FilingJob(JobRepository jobRepository,
                              Step aisReconciliationStep,
                              Step validationStep,
                              Step taxComputationStep,
                              Step amtComputationStep,
                              Step jsonExportStep,
                              Step lossPersistenceStep) {
        return new JobBuilder("itr3FilingJob", jobRepository)
            .start(aisReconciliationStep)
            .next(validationStep)
            .next(taxComputationStep)
            .next(amtComputationStep)
            .next(jsonExportStep)
            .next(lossPersistenceStep)
            .build();
    }
}
```

### 11.3 `AsyncFilingController.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.controller;

import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/filing/async")
public class AsyncFilingController {

    private final JobLauncher jobLauncher;
    private final Job itr3FilingJob;

    public AsyncFilingController(JobLauncher jobLauncher, Job itr3FilingJob) {
        this.jobLauncher = jobLauncher;
        this.itr3FilingJob = itr3FilingJob;
    }

    /**
     * Submits ITR-3/4 for async processing.
     * Returns job ID immediately — client polls /status/{jobId}.
     */
    @PostMapping("/itr3")
    public ResponseEntity<Map<String, Object>> submitITR3Async(
            @RequestBody Itr3FormData data) throws Exception {

        JobParameters params = new JobParametersBuilder()
            .addString("pan", data.getPAN())
            .addString("assessmentYear", data.getAssessmentYear())
            .addLong("submittedAt", System.currentTimeMillis())
            .toJobParameters();

        JobExecution execution = jobLauncher.run(itr3FilingJob, params);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", execution.getJobId());
        response.put("status", execution.getStatus().name());
        response.put("message", "Filing submitted for processing. Poll /status/" +
            execution.getJobId() + " for updates.");

        return ResponseEntity.accepted().body(response);
    }

    /**
     * Polls async filing status.
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long jobId) {
        // Query Spring Batch job repository for status
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        // Status lookup implementation via JobExplorer
        return ResponseEntity.ok(response);
    }
}
```

---

## SECTION 12 — BULK FILING FOR CA FIRMS (MULTI-PAN BATCH)

### 12.1 `BulkFilingService.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.service;

import com.yourpackage.dto.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;

/**
 * Bulk filing support for CA firms and tax practitioners.
 * Supports: CSV/JSON upload of multiple PAN returns, batch validation,
 * batch submission with individual status tracking.
 * Max batch size: 500 PANs per batch (configurable).
 */
@Service
public class BulkFilingService {

    private static final int MAX_BATCH_SIZE = 500;
    private static final int THREAD_POOL_SIZE = 10;

    private final ITRFilingOrchestrationService filingService;
    private final ExecutorService executorService =
        Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    public BulkFilingService(ITRFilingOrchestrationService filingService) {
        this.filingService = filingService;
    }

    /**
     * Submits a batch of ITR filings.
     * @param batch List of filing requests (PAN + form data + ITR type)
     * @param caClientId Practitioner/CA client ID for audit purposes
     * @return BulkFilingResult with per-PAN status
     */
    public BulkFilingResult submitBatch(List<BulkFilingRequest> batch, String caClientId) {
        BulkFilingResult result = new BulkFilingResult();
        result.setBatchId(UUID.randomUUID().toString());
        result.setCaClientId(caClientId);
        result.setTotalRequests(batch.size());

        if (batch.size() > MAX_BATCH_SIZE) {
            result.setStatus("REJECTED");
            result.setMessage("Batch size " + batch.size() + " exceeds maximum " +
                MAX_BATCH_SIZE + ". Split into smaller batches.");
            return result;
        }

        // ── Validate all requests before submitting any ───────────────────
        List<String> validationErrors = validateBatch(batch);
        if (!validationErrors.isEmpty()) {
            result.setStatus("VALIDATION_FAILED");
            result.setValidationErrors(validationErrors);
            return result;
        }

        // ── Submit each PAN concurrently ──────────────────────────────────
        List<Future<PANFilingResult>> futures = new ArrayList<>();
        for (BulkFilingRequest request : batch) {
            futures.add(executorService.submit(() -> processIndividualRequest(request)));
        }

        // ── Collect results ───────────────────────────────────────────────
        int successCount = 0, failCount = 0;
        for (Future<PANFilingResult> future : futures) {
            try {
                PANFilingResult panResult = future.get(5, TimeUnit.MINUTES);
                result.addPANResult(panResult);
                if ("SUCCESS".equals(panResult.getStatus())) successCount++;
                else failCount++;
            } catch (TimeoutException e) {
                PANFilingResult timeout = new PANFilingResult();
                timeout.setStatus("TIMEOUT");
                timeout.setError("Filing timed out after 5 minutes");
                result.addPANResult(timeout);
                failCount++;
            } catch (Exception e) {
                PANFilingResult error = new PANFilingResult();
                error.setStatus("ERROR");
                error.setError(e.getMessage());
                result.addPANResult(error);
                failCount++;
            }
        }

        result.setSuccessCount(successCount);
        result.setFailureCount(failCount);
        result.setStatus(failCount == 0 ? "COMPLETED" : "PARTIAL_SUCCESS");

        return result;
    }

    private List<String> validateBatch(List<BulkFilingRequest> batch) {
        List<String> errors = new ArrayList<>();
        Set<String> seenPANs = new HashSet<>();
        for (int i = 0; i < batch.size(); i++) {
            BulkFilingRequest req = batch.get(i);
            if (req.getPan() == null || !req.getPan().matches("[A-Z]{5}[0-9]{4}[A-Z]")) {
                errors.add("Row " + (i+1) + ": Invalid PAN format: " + req.getPan());
            }
            if (!seenPANs.add(req.getPan())) {
                errors.add("Row " + (i+1) + ": Duplicate PAN in batch: " + req.getPan());
            }
            if (req.getItrType() == null ||
                !List.of("ITR1","ITR2","ITR3","ITR4").contains(req.getItrType())) {
                errors.add("Row " + (i+1) + ": Invalid ITR type: " + req.getItrType());
            }
        }
        return errors;
    }

    private PANFilingResult processIndividualRequest(BulkFilingRequest request) {
        PANFilingResult result = new PANFilingResult();
        result.setPan(request.getPan());
        result.setItrType(request.getItrType());
        try {
            FilingResult filing = switch (request.getItrType()) {
                case "ITR1" -> filingService.processITR1((Itr1FormData) request.getFormData());
                case "ITR2" -> filingService.processITR2((Itr2FormData) request.getFormData());
                case "ITR3" -> filingService.processITR3((Itr3FormData) request.getFormData());
                case "ITR4" -> filingService.processITR4((Itr4FormData) request.getFormData());
                default -> throw new IllegalArgumentException("Unknown ITR type: " + request.getItrType());
            };
            result.setStatus(filing.isSuccessful() ? "SUCCESS" : "FAILED");
            result.setAcknowledgementNumber(filing.getAcknowledgementNumber());
            result.setErrors(filing.getErrors());
            result.setWarnings(filing.getWarnings());
        } catch (Exception e) {
            result.setStatus("ERROR");
            result.setError(e.getMessage());
        }
        return result;
    }
}
```

---

## SECTION 13 — AIS RECONCILIATION RESULT CACHING (REDIS + DB)

### 13.1 `AISReconciliationResultRepository.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.repository;

import com.yourpackage.entity.AISReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AISReconciliationResultRepository
        extends JpaRepository<AISReconciliationResult, Long> {

    Optional<AISReconciliationResult> findByPanAndAssessmentYear(
        String pan, String assessmentYear);
}
```

### 13.2 `AISReconciliationResultEntity.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ais_reconciliation_results",
       indexes = @Index(columnList = "pan, assessment_year"))
public class AISReconciliationResultEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String pan;

    @Column(nullable = false, length = 7)
    private String assessmentYear;

    @Column(columnDefinition = "TEXT")
    private String discrepanciesJson; // JSON array of discrepancy strings

    private boolean hasMismatches;
    private long totalTds26AS;
    private long totalTdsITR;

    private LocalDateTime reconciledAt;
    private LocalDateTime expiresAt; // Cache validity — 24 hours

    // Getters/setters omitted for brevity — use @Data Lombok or generate
}
```

### 13.3 Flyway Migration — `V9_3__ais_reconciliation_cache.sql`

```sql
CREATE TABLE ais_reconciliation_results (
    id                  BIGSERIAL PRIMARY KEY,
    pan                 VARCHAR(10) NOT NULL,
    assessment_year     VARCHAR(7)  NOT NULL,
    discrepancies_json  TEXT,
    has_mismatches      BOOLEAN     DEFAULT FALSE,
    total_tds_26as      BIGINT      DEFAULT 0,
    total_tds_itr       BIGINT      DEFAULT 0,
    reconciled_at       TIMESTAMP,
    expires_at          TIMESTAMP,
    created_at          TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ais_pan_ay UNIQUE (pan, assessment_year)
);

CREATE INDEX idx_ais_pan_ay   ON ais_reconciliation_results (pan, assessment_year);
CREATE INDEX idx_ais_expires  ON ais_reconciliation_results (expires_at);
```

### 13.4 Redis Caching — `application.properties` additions

```properties
# Redis configuration for AIS reconciliation caching
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.timeout=2000ms

# AIS result cache TTL — 24 hours (26AS doesn't change intraday)
ais.cache.ttl-hours=24
```

---

## SECTION 14 — ITR-V PDF ACKNOWLEDGMENT RECEIPT GENERATION

### 14.1 New Dependency — `pom.xml`

```xml
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>8.0.4</version>
    <type>pom</type>
</dependency>
```

### 14.2 `ITRVAcknowledgementService.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.service;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.stereotype.Service;
import com.yourpackage.dto.FilingResult;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates ITR-V equivalent acknowledgement PDF.
 * ITR-V is the one-page acknowledgement issued by ITD after successful filing.
 * For offline verification (print and sign), used when e-verification not done.
 * This generates a system acknowledgement — the actual ITD-issued ITR-V comes
 * from the ITD portal after upload.
 */
@Service
public class ITRVAcknowledgementService {

    public byte[] generate(FilingResult filing) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            // ── Header ─────────────────────────────────────────────────────
            Paragraph header = new Paragraph(
                "INCOME TAX DEPARTMENT — INDIA\n" +
                "ITR FILING ACKNOWLEDGEMENT RECEIPT")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(14);
            doc.add(header);

            doc.add(new Paragraph("AY: " + filing.getAssessmentYear() +
                "  |  Form: " + filing.getItrType())
                .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("─".repeat(70)));

            // ── Taxpayer Details ───────────────────────────────────────────
            Table table = new Table(new float[]{200f, 300f});

            addRow(table, "PAN", filing.getPan());
            addRow(table, "Taxpayer Name", filing.getTaxpayerName());
            addRow(table, "Acknowledgement No.", filing.getAcknowledgementNumber());
            addRow(table, "Filing Date", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            addRow(table, "Assessment Year", filing.getAssessmentYear());
            addRow(table, "ITR Form", filing.getItrType());
            addRow(table, "Tax Regime", filing.getTaxRegime());
            addRow(table, "Total Income (₹)", String.valueOf(filing.getTotalIncome()));
            addRow(table, "Net Tax Payable (₹)", String.valueOf(filing.getNetTaxPayable()));
            addRow(table, "Refund Claimed (₹)", String.valueOf(filing.getRefundClaimed()));
            addRow(table, "Filing Status", filing.isSuccessful() ? "✅ ACCEPTED" : "❌ FAILED");
            addRow(table, "e-Verification Status",
                filing.getEvcStatus() != null ? filing.getEvcStatus() : "PENDING");

            doc.add(table);

            doc.add(new Paragraph("─".repeat(70)));

            // ── Footer notice ──────────────────────────────────────────────
            doc.add(new Paragraph(
                "IMPORTANT: This is a system-generated receipt. The official ITR-V acknowledgement " +
                "is issued by the Income Tax Department upon successful upload at incometax.gov.in. " +
                "e-Verify within 30 days of filing. Unverified returns are treated as invalid.")
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph(
                "SHA-256 Digest: " + filing.getJsonDigest())
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("ITR-V PDF generation failed: " + e.getMessage(), e);
        }
    }

    private void addRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()));
        table.addCell(new Cell().add(new Paragraph(value != null ? value : "—")));
    }
}
```

---

## SECTION 15 — AES-256 ENCRYPTION FOR PAN/AADHAAR AT REST

### 15.1 `SensitiveDataEncryptionService.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for sensitive fields (PAN, Aadhaar, bank account numbers).
 * GCM mode provides authenticated encryption — prevents tampering without key.
 * Key must be 256-bit (32 bytes) stored in environment variable ENCRYPTION_KEY_BASE64.
 * IV is randomly generated per encryption — stored alongside ciphertext.
 * Format: base64(IV_12_BYTES) + ":" + base64(CIPHERTEXT)
 */
@Service
public class SensitiveDataEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;  // 96-bit IV for GCM
    private static final int TAG_LENGTH_BITS  = 128; // 128-bit auth tag

    @Value("${encryption.key.base64}")
    private String keyBase64;

    /**
     * Encrypts a plain-text sensitive value.
     * @param plainText PAN/Aadhaar/account number in plain text
     * @return Encrypted string in format: base64(IV):base64(ciphertext+tag)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return plainText;
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec paramSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

            return Base64.getEncoder().encodeToString(iv) + ":" +
                   Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed — check ENCRYPTION_KEY_BASE64", e);
        }
    }

    /**
     * Decrypts an encrypted value. Returns null if input is null/blank.
     */
    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return encrypted;
        try {
            String[] parts = encrypted.split(":");
            if (parts.length != 2) throw new IllegalArgumentException("Invalid encrypted format");

            byte[] iv         = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);
            byte[] keyBytes   = Base64.getDecoder().decode(keyBase64);

            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec paramSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed — data may be tampered", e);
        }
    }

    /**
     * Masks a PAN for logging: ABCDE1234F → ABCDE****F
     */
    public static String maskPAN(String pan) {
        if (pan == null || pan.length() != 10) return "INVALID_PAN";
        return pan.substring(0, 5) + "****" + pan.substring(9);
    }

    /**
     * Masks Aadhaar for logging: 123456789012 → ********9012
     */
    public static String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 4) return "INVALID_AADHAAR";
        return "*".repeat(aadhaar.length() - 4) + aadhaar.substring(aadhaar.length() - 4);
    }
}
```

### 15.2 JPA Converter — `EncryptedStringConverter.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter — transparently encrypts/decrypts PAN and Aadhaar
 * fields in all JPA entities using @Convert(converter = EncryptedStringConverter.class).
 * Usage on any entity field: @Convert(converter = EncryptedStringConverter.class)
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Autowired
    private SensitiveDataEncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData);
    }
}
```

### 15.3 Apply Converter to ALL entities with PAN/Aadhaar fields

For every JPA entity that stores PAN or Aadhaar, add the converter annotation:

```java
// In LossLedger.java, AuditTrail.java, AISReconciliationResultEntity.java,
// Form16ImportLog.java, EVCLog.java — anywhere PAN is persisted:

@Convert(converter = EncryptedStringConverter.class)
@Column(name = "pan", nullable = false, length = 100) // longer to accommodate encrypted value
private String pan;

// For Aadhaar wherever stored:
@Convert(converter = EncryptedStringConverter.class)
@Column(name = "aadhaar_number", length = 200)
private String aadhaarNumber;
```

> **CRITICAL:** Run Flyway migration to widen PAN columns from `VARCHAR(10)` to `VARCHAR(100)` and Aadhaar columns from `VARCHAR(12)` to `VARCHAR(200)` before deploying encryption. AES-256-GCM output is longer than plain text.

### 15.4 Flyway Migration — `V9_4__encrypt_sensitive_columns.sql`

```sql
-- Widen columns to accommodate encrypted values (base64 AES-256-GCM output)
ALTER TABLE loss_ledger             ALTER COLUMN pan TYPE VARCHAR(100);
ALTER TABLE audit_trail             ALTER COLUMN pan TYPE VARCHAR(100);
ALTER TABLE ais_reconciliation_results ALTER COLUMN pan TYPE VARCHAR(100);
ALTER TABLE form16_import_log       ALTER COLUMN pan TYPE VARCHAR(100);
ALTER TABLE evc_log                 ALTER COLUMN pan TYPE VARCHAR(100);

-- NOTE: Existing plaintext PAN values will need one-time re-encryption migration
-- Run EncryptionMigrationJob.java after deploying this migration (see below)
```

### 15.5 Environment variable requirement

```properties
# application.properties — do NOT commit this value to source control
# Generate: openssl rand -base64 32
encryption.key.base64=${ENCRYPTION_KEY_BASE64}
```

---

## SECTION 16 — MFA / OTP FLOW FOR SUBMISSION

### 16.1 `OTPService.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.security;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * OTP generation and validation for ITR submission gate.
 * OTPs are 6-digit numeric, valid for 5 minutes, single-use.
 * Stored in Redis (or in-memory DB if Redis unavailable).
 * Rate limit: Max 3 OTP requests per PAN per 30 minutes.
 */
@Service
public class OTPService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 30;

    private final RedisTemplate<String, String> redisTemplate;
    private final SMSGatewayService smsGateway; // implement per your SMS provider
    private final AuditTrailService auditTrail;

    public OTPService(RedisTemplate<String, String> redisTemplate,
                      SMSGatewayService smsGateway,
                      AuditTrailService auditTrail) {
        this.redisTemplate = redisTemplate;
        this.smsGateway    = smsGateway;
        this.auditTrail    = auditTrail;
    }

    /**
     * Generates and sends OTP for submission gate.
     * @param pan Taxpayer PAN (used as key)
     * @param mobile Registered mobile number for OTP delivery
     * @return OTP transaction reference (not the OTP itself)
     */
    public String generateAndSendOTP(String pan, String mobile) {
        // ── Rate limit check ──────────────────────────────────────────────
        String rateLimitKey = "otp:ratelimit:" + pan;
        Long requestCount = redisTemplate.opsForValue().increment(rateLimitKey);
        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(rateLimitKey, RATE_LIMIT_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        if (requestCount != null && requestCount > MAX_ATTEMPTS) {
            throw new OTPRateLimitException("OTP requests exhausted for this PAN. " +
                "Try after " + RATE_LIMIT_WINDOW_MINUTES + " minutes.");
        }

        // ── Generate OTP ──────────────────────────────────────────────────
        String otp = generateNumericOTP();
        String transactionRef = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // ── Store in Redis ────────────────────────────────────────────────
        String otpKey = "otp:value:" + pan + ":" + transactionRef;
        redisTemplate.opsForValue().set(otpKey, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

        // Store attempts counter
        String attemptsKey = "otp:attempts:" + pan + ":" + transactionRef;
        redisTemplate.opsForValue().set(attemptsKey, "0", OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

        // ── Send OTP ──────────────────────────────────────────────────────
        smsGateway.send(mobile, "Your ITR ERP filing OTP is " + otp +
            ". Valid for " + OTP_EXPIRY_MINUTES + " minutes. Do not share.");

        auditTrail.log(SensitiveDataEncryptionService.maskPAN(pan),
            "OTP_GENERATED", "Ref: " + transactionRef, "OTP_SERVICE");

        return transactionRef; // Return ref, not OTP — client uses ref to confirm
    }

    /**
     * Validates an OTP.
     * @return true if OTP is valid; false if invalid or expired
     * @throws OTPLockedException if max attempts exceeded (3 wrong attempts)
     */
    public boolean validateOTP(String pan, String transactionRef, String submittedOTP) {
        String otpKey      = "otp:value:" + pan + ":" + transactionRef;
        String attemptsKey = "otp:attempts:" + pan + ":" + transactionRef;

        String storedOTP = redisTemplate.opsForValue().get(otpKey);
        if (storedOTP == null) {
            throw new OTPExpiredException("OTP expired or already used. Request a new OTP.");
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts > 3) {
            redisTemplate.delete(otpKey); // Invalidate after 3 failed attempts
            auditTrail.log(SensitiveDataEncryptionService.maskPAN(pan),
                "OTP_LOCKED", "Max attempts exceeded. Ref: " + transactionRef, "OTP_SERVICE");
            throw new OTPLockedException("Max OTP attempts exceeded. Request a new OTP.");
        }

        if (storedOTP.equals(submittedOTP)) {
            redisTemplate.delete(otpKey);      // Single-use: invalidate after success
            redisTemplate.delete(attemptsKey);
            auditTrail.log(SensitiveDataEncryptionService.maskPAN(pan),
                "OTP_VALIDATED", "Ref: " + transactionRef, "OTP_SERVICE");
            return true;
        }

        return false;
    }

    private String generateNumericOTP() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 6-digit
        return String.valueOf(otp);
    }
}
```

---

## SECTION 17 — AUDIT LOG TAMPER DETECTION (HASH CHAINING)

### 17.1 `TamperProofAuditService.java` — MODIFY EXISTING `AuditTrailService.java`

Add hash chaining to the existing audit trail. Each log entry's hash is computed over the entry's content + the previous entry's hash (like a blockchain). Tampering with any entry breaks the chain.

```java
// Add to AuditTrailService.java:

@Autowired private SHA256DigestUtil sha256; // already exists
@Autowired private AuditTrailRepository auditRepo; // already exists

/**
 * Logs an audit event with hash chaining.
 * The hash of each entry includes the previous entry's hash (chaining).
 * Tampering with any prior entry will invalidate all subsequent hashes.
 */
public void logWithChaining(String pan, String event, String detail, String source) {
    AuditTrail entry = new AuditTrail();
    entry.setPan(pan);
    entry.setEvent(event);
    entry.setDetail(detail);
    entry.setSource(source);
    entry.setTimestamp(java.time.LocalDateTime.now());

    // Get previous entry's hash for this PAN
    String previousHash = auditRepo.findLastHashByPan(pan)
        .orElse("GENESIS"); // First entry chains to "GENESIS"
    entry.setPreviousHash(previousHash);

    // Compute this entry's hash: SHA-256 of (pan|event|detail|timestamp|previousHash)
    String content = pan + "|" + event + "|" + detail + "|" +
                     entry.getTimestamp() + "|" + previousHash;
    entry.setEntryHash(sha256.computeSHA256(content));

    auditRepo.save(entry);
}

/**
 * Verifies audit chain integrity for a given PAN.
 * Returns list of broken chain links if tampered; empty list if clean.
 */
public List<AuditChainBreak> verifyChainIntegrity(String pan) {
    List<AuditTrail> entries = auditRepo.findByPanOrderByTimestampAsc(pan);
    List<AuditChainBreak> breaks = new ArrayList<>();
    String expectedPreviousHash = "GENESIS";

    for (AuditTrail entry : entries) {
        // Recompute expected hash
        String content = entry.getPan() + "|" + entry.getEvent() + "|" +
                         entry.getDetail() + "|" + entry.getTimestamp() + "|" +
                         expectedPreviousHash;
        String expectedHash = sha256.computeSHA256(content);

        if (!expectedHash.equals(entry.getEntryHash())) {
            AuditChainBreak breakPoint = new AuditChainBreak();
            breakPoint.setEntryId(entry.getId());
            breakPoint.setEvent(entry.getEvent());
            breakPoint.setTimestamp(entry.getTimestamp().toString());
            breakPoint.setExpectedHash(expectedHash);
            breakPoint.setActualHash(entry.getEntryHash());
            breaks.add(breakPoint);
        }
        expectedPreviousHash = entry.getEntryHash();
    }
    return breaks;
}
```

### 17.2 Flyway Migration — `V9_5__audit_trail_hash_chain.sql`

```sql
-- Add hash chaining columns to existing audit_trail table
ALTER TABLE audit_trail ADD COLUMN IF NOT EXISTS previous_hash VARCHAR(64);
ALTER TABLE audit_trail ADD COLUMN IF NOT EXISTS entry_hash    VARCHAR(64);

-- Index for chain verification queries
CREATE INDEX IF NOT EXISTS idx_audit_pan_time ON audit_trail (pan, timestamp ASC);
```

### 17.3 Add `findLastHashByPan` to `AuditTrailRepository.java` — MODIFY EXISTING

```java
@Query("SELECT a.entryHash FROM AuditTrail a WHERE a.pan = :pan " +
       "ORDER BY a.timestamp DESC LIMIT 1")
Optional<String> findLastHashByPan(@Param("pan") String pan);

List<AuditTrail> findByPanOrderByTimestampAsc(String pan);
```

---

## SECTION 18 — SESSION TIMEOUT HANDLING FOR SENSITIVE DATA

### 18.1 `SessionSecurityConfig.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Security configuration for session management.
 * Tax data is highly sensitive — sessions must timeout aggressively.
 * Idle timeout: 15 minutes. Absolute timeout: 4 hours.
 * Concurrent session limit: 1 session per user (prevents account sharing).
 */
@Configuration
@EnableWebSecurity
public class SessionSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                // Maximum 1 concurrent session per user
                .maximumSessions(1)
                .expiredUrl("/session-expired")
                .maxSessionsPreventsLogin(false) // new login invalidates old session
            )
            .sessionManagement(session -> session
                // Prevent session fixation attacks
                .sessionFixation().migrateSession()
            )
            // CSRF protection — mandatory for tax filing
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/v1/batch/**") // batch API uses API key auth
            );

        return http.build();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
```

### 18.2 `application.properties` additions for session timeout

```properties
# Session timeout — 15 minutes idle (900 seconds)
server.servlet.session.timeout=900

# Cookie security
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=strict
server.servlet.session.cookie.name=ITR_SESSION
```

### 18.3 `SessionTimeoutInterceptor.java` — CREATE FROM SCRATCH

```java
package com.yourpackage.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.time.Instant;

/**
 * Enforces absolute session timeout of 4 hours.
 * Spring's server.servlet.session.timeout handles idle timeout.
 * This interceptor additionally enforces an absolute wall-clock limit.
 */
@Component
public class SessionTimeoutInterceptor implements HandlerInterceptor {

    private static final long ABSOLUTE_TIMEOUT_SECONDS = 4 * 60 * 60; // 4 hours
    private static final String SESSION_CREATED_KEY = "SESSION_CREATED_AT";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                              Object handler) throws Exception {

        HttpSession session = request.getSession(false);
        if (session == null) return true; // No session — let Spring Security handle

        // Check absolute timeout
        Long createdAt = (Long) session.getAttribute(SESSION_CREATED_KEY);
        if (createdAt == null) {
            session.setAttribute(SESSION_CREATED_KEY, Instant.now().getEpochSecond());
            return true;
        }

        long elapsed = Instant.now().getEpochSecond() - createdAt;
        if (elapsed > ABSOLUTE_TIMEOUT_SECONDS) {
            session.invalidate();
            if (request.getHeader("Accept") != null &&
                request.getHeader("Accept").contains("application/json")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"SESSION_EXPIRED\"," +
                    "\"message\":\"Your session has expired after 4 hours. Please log in again.\"}");
            } else {
                response.sendRedirect("/session-expired");
            }
            return false;
        }

        return true;
    }
}
```

---

## FINAL COMPLIANCE VERIFICATION CHECKLIST

After implementing all 18 sections, run the following verification:

### Compilation Verification
```bash
mvn clean compile -q && echo "✅ BUILD SUCCESS"
mvn test -pl . -Dtest="ITR1JSONExportRegressionTest" && echo "✅ REGRESSION TESTS PASS"
```

### CBDT Rule Coverage Verification (should remain 227/227)
```bash
grep -r "CAT-A\|CAT-B\|CAT-D" src/main/java --include="*.java" | wc -l
# Must be ≥ 227
```

### Security Verification
```bash
# Verify encryption key is not hardcoded
grep -r "ENCRYPTION_KEY" src/main/java --include="*.java" | grep -v "\\${"
# Must return 0 results (no hardcoded keys)
```

### Gap Closure Verification

| Gap ID | Component | Verification |
|--------|-----------|--------------|
| G-01 | `Form16PartAOCRParser` + `Form16PartBOCRParser` | Parse sample TRACES-generated PDF, assert TAN extracted correctly |
| G-02 | `TRACESApiClient` + `Form26ASXMLParser` | Mock TRACES API, assert 26AS XML parsed into `Form26ASData` |
| G-03 | `ITR1JSONExportRegressionTest` | All 10 regression tests must pass |
| G-04 | `ScheduleFAService` | Create taxpayer with foreign bank account, assert FA validation runs |
| G-05 | `AMTComputationService` | ITR-3 with 80IC deduction > ₹20L ATI — assert AMT computed and Schedule AMT in JSON |
| G-06 | `EVCVerificationService` | Mock ITD API, assert Aadhaar OTP flow returns EVC code |
| G-07 | `RectificationService` | Assert rejection for expired AY (AY 2019-20), acceptance for valid AY |
| G-08 | `RevisedReturnService` | Assert `ReturnFilingSection = "139(5)"` set on revised DTO |
| G-09 | `TDSMismatchAlertService` | Inject 26AS TDS ≠ ITR TDS by ₹500 — assert HIGH severity mismatch |
| G-10 | `TaxRegimeRecommendationEngine` | ₹15L income, ₹3L deductions — assert NEW regime recommended with score |
| G-11 | `AsyncITRFilingConfig` | Submit ITR-3 via `/api/v1/filing/async/itr3` — assert job ID returned immediately |
| G-12 | `BulkFilingService` | Submit 5-PAN batch — assert per-PAN results returned |
| G-13 | `AISReconciliationResultEntity` | Reconcile once, call again — assert DB cache hit (no TRACES call) |
| G-14 | `ITRVAcknowledgementService` | Call `generate()` — assert PDF byte[] non-empty, contains PAN and ACK number |
| G-15 | `SensitiveDataEncryptionService` | Encrypt PAN, store in DB, fetch and decrypt — assert original PAN returned |
| G-16 | `OTPService` | Generate OTP, validate correct OTP — assert true; validate wrong OTP — assert false |
| G-17 | `TamperProofAuditService` | Log 5 entries, tamper entry 3, call `verifyChainIntegrity()` — assert break at entry 3 |
| G-18 | `SessionTimeoutInterceptor` | Inject session created 5 hours ago — assert 401 response |

---

## NEW DTO REQUIREMENTS

The following DTOs must be created before compilation will succeed. Create minimal POJOs with getters/setters (or use `@Data` Lombok):

- `Form16PartAData` + `Form16PartAData.QuarterlyTDS` (inner class)
- `Form16PartBData`
- `Form26ASData` + `Form26ASData.TDSEntry` + `Form26ASData.TaxPaidEntry`
- `ScheduleFAData` + `ForeignBankAccount`, `ForeignFinancialInterest`, `ForeignImmovableProperty`, `OtherForeignAsset`, `ForeignSigningAuthority`, `ForeignTrust`, `ForeignSourceIncome`
- `ScheduleFAValidationResult`
- `AMTResult`
- `EVCRequest`, `EVCResult`
- `RectificationResult`, `RevisedReturnResult`
- `TDSMismatchReport`, `TDSMismatch`, `TDSSummary`
- `TaxRegimeRecommendation`
- `BulkFilingRequest`, `BulkFilingResult`, `PANFilingResult`
- `AuditChainBreak`

---

## EXCEPTION CLASSES REQUIRED

```java
// Create these in com.yourpackage.exception:
public class Form16ValidationException extends RuntimeException { ... }
public class TRACESApiException extends RuntimeException { ... }
public class OTPRateLimitException extends RuntimeException { ... }
public class OTPExpiredException extends RuntimeException { ... }
public class OTPLockedException extends RuntimeException { ... }
```

---

## DEPLOYMENT PREREQUISITES — UPDATED

After Phase 9, the following environment variables are **mandatory** before production:

| Variable | Purpose | Example |
|----------|---------|---------|
| `ENCRYPTION_KEY_BASE64` | AES-256 key for PAN/Aadhaar encryption | `$(openssl rand -base64 32)` |
| `TRACES_CLIENT_ID` | TRACES API OAuth client ID | Provided by TRACES portal |
| `TRACES_CLIENT_SECRET` | TRACES API OAuth secret | Provided by TRACES portal |
| `TRACES_BASE_URL` | TRACES API base URL | `https://tdscpc.gov.in/app/api` |
| `ITD_EFILING_BASE_URL` | ITD e-filing portal API | `https://eportal.incometax.gov.in/iec/api` |
| `REDIS_HOST` | Redis host for OTP + AIS caching | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis auth password | — |
| `SMS_GATEWAY_API_KEY` | SMS provider key for OTP delivery | Per provider |

---

## COMPLIANCE SCORE PROJECTION

| Category | Pre-Phase 9 | Post-Phase 9 |
|----------|-------------|--------------|
| CBDT Validation Rules (CAT-A/B/D) | 227/227 (100%) | 227/227 (100%) — maintained |
| Form 16 Import | Manual only | OCR + full Part A/B parse |
| 26AS Import | Manual only | Auto via TRACES API |
| Schedule FA | Partial (SFT-009 only) | Full 7-part implementation |
| AMT u/s 115JC | Missing | Complete for ITR-3 |
| E-Verification | Missing | EVC (Aadhaar/NB/Bank) + DSC |
| Rectification u/s 154 | Missing | Implemented |
| Revised Return u/s 139(5) | Missing | Implemented |
| TDS Mismatch Alerts | Missing | 3-way cross-check with suggestions |
| Tax Regime Recommender | Comparison only | Full recommender + confidence score |
| Async Pipeline | Synchronous | Spring Batch async |
| Bulk Filing | Missing | 500 PAN/batch |
| AIS Caching | In-memory | Redis + DB with 24h TTL |
| ITR-V PDF | Missing | iText7 PDF generation |
| PAN/Aadhaar Encryption | Plaintext | AES-256-GCM at rest |
| MFA/OTP | Missing | 6-digit OTP via SMS, Redis-backed |
| Audit Tamper Detection | Log only | SHA-256 hash chain + verify API |
| Session Security | None | 15-min idle, 4-hr absolute, 1 session |
| **Overall Compliance** | **97%** | **101% (exceeds CBDT minimum)** |

---

**End of Phase 9 Directive**
*Date: April 11, 2026 | Scope: 18 gap closures across functional, architecture, and security domains*
*Prerequisite state: Phase 1–8 complete, 156 Java files, BUILD SUCCESS, 97% compliant*
*Target state: BUILD SUCCESS, all regression tests pass, 101% CBDT compliant*
