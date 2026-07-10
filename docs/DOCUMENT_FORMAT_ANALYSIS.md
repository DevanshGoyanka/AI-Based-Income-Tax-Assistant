# Document Format Analysis - ITD Portal Documents

**Date:** 2026-07-03  
**Purpose:** Comprehensive analysis of all 4 document types from ITD portal before implementing extraction logic

## Sample Files Location
`C:\Users\Devansh\Desktop\E-FILE_karo`

---

## 1. PREFILL JSON (ITD Portal Download)

### Format
- **File Pattern:** `{PAN}-Prefill-{AY}-{timestamp}.json`
- **Examples:** 
  - `ACUPG3482G-Prefill-2025-14_31_2026_18_50.json`
  - `COVPC5929M-Prefill-2025-27_31_2026_23_24.json`
  - `ABHPB8923F-Prefill-2025-16_21_2026_13_21.json`
- **Encryption:** None (plain JSON)
- **Size:** 3-7 KB

### Structure Analysis (from actual samples)

#### Root Keys
```
personalInfo
Form10BC, Form10BA, form10DA, form3CD
scheduleAL, form3CE, form3CEA, form3CEB
form26as ⭐ PRIMARY DATA SOURCE
form10IBICID, otherSourceIncome, form10IE
form10CCE, form10CCF, form10CCD, form10CCB
itba, form64D, form64A, form67, form66
verification
bankAccountDtls
form56F, insights ⭐ DERIVED DATA
form29b, scheduleCFL, filingStatus
form24q ⭐ SALARY TDS DATA
partAGEN2, ScheduleEI, Form10A, Form10IFA
ais, ScheduleESOP, formCCBA, formCCBD, formCCBC, formCCBB
auditInfo, incDeductionsOthIncCPC, incDeductionsOthersInc
assesseeRep, form10E, Schedule80RA, Form10IA, Schedule80G
Form10AB, Form10IEA, form10IF, form10AC, form10BOR10BB
natOfBus, filingReturn, lastFiledITR
```

#### Critical Data Paths

**Personal Information:**
```json
personalInfo.assesseeName.firstName
personalInfo.assesseeName.middleName
personalInfo.assesseeName.surNameOrOrgName
personalInfo.pan
personalInfo.aadhaarCardNo (base64 encoded)
personalInfo.dob
personalInfo.address.*
personalInfo.mobileNo
personalInfo.emailAddress
```

**26AS Data - TDS on Other Than Salary:**
```json
form26as.tdsOnOthThanSals.tdSonOthThanSal[] {
  sectionCode: "94A", "94C", "94J", etc.
  grossAmount: number
  employerOrDeductorOrCollectDetl: {
    tan: string
    employerOrDeductorOrCollecterName: string
  }
  taxDeductCreditDtls: {
    taxDeductedOwnHands: number
    taxClaimedOwnHands: number
  }
  headOfIncome: "OS", "SAL", etc.
}
```

**26AS Data - TDS on Salary:**
```json
form26as.tdsOnSalaries.tdsOnSalary[] {
  totalTDSSal: number
  incChrgSal: number (income charged to salary)
  employerOrDeductorOrCollectDetl: {
    tan: string
    employerOrDeductorOrCollecterName: string
  }
}
```

**26AS Data - Tax Payments:**
```json
form26as.taxPayments.taxPayment[] {
  srlNoOfChaln: number
  dateDep: "YYYY-MM-DD"
  bsrCode: string
  nameOfBankAndBranch: {
    nameOfBank: string
    nameOfBranch: string
  }
  amt: number
  receiptNumber: string
}
```

**Insights (Derived/Summary Data):**
```json
insights.intrstFrmSavingBank: number
insights.intrstFrmTermDeposit: number
insights.cumulativeSalary: {
  perquisitesValue: number
  profitsInSalary: number
  salary: number
  salaryUpdateTimestamp: string
}
insights.salaries.salary[] {
  nameOfEmployer: string
  tanOfEmployer: string
  salarys: {
    valueOfPerquisites: number
    profitsinLieuOfSalary: number
    salary: number
  }
}
insights.scheduleOS.incOthThanOwnRaceHorse: {
  dividendGross: number
  DividendOthThan22e: number
}
insights.incomeDeductionsOthersInc[] {
  othSrcOthAmount: number
  othSrcNatureDesc: "IFD" | "SAV" | "DIV" | "TAX"
}
insights.UsrDeductUndChapVIAType: {
  Section80TTB: number
}
```

**Form 24Q (Employer TDS Certificate):**
```json
form24q.salaries.salary[] {
  nameOfEmployer: string
  tanOfEmployer: string
  addressDetail: {
    addDetail: string
    pinCode: string
    stateCode: string
    cityOrTownOrDistrict: string
  }
  salarys: {
    salary: number
    valueOfPerquisites: number
    profitsinLieuOfSalary: number
    natureOfSalary.othersIncDtls[]: {
      othAmount: number
      natureDesc: "1" (basic salary)
    }
  }
}
form24q.incomeDeductions: {
  salary: number
  perquisitesValue: number
  profitsInSalary: number
  deductionUs16Ia: number (standard deduction)
  professionalTaxUs16Iii: number
  entertainmentAlw16Ii: number
  totalIncomeOfHP: number
  totalIncomeChargeableUnHP: number
}
form24q.usrDeductUndChapVIAType: {
  section80C: number
  section80CCC: number
  section80CCD1B: number
  section80CCDEmployeeOrSE: number
  section80CCDEmployer: number
  section80TTA: number
  section80E: number
}
form24q.intrstFrmSavingBank: number
form24q.TotalAllwncExemptUs10: number
form24q.dedDetId24Q: string (deductor details ID)
form24q.form24qTimestamp: string
form24q.PensionerFlag: "Y" | "N"
```

**Bank Details:**
```json
bankAccountDtls[].addtnlBankDetails[] {
  useForRefund: "true" | "false"
  bankAccountNo: string
  bankName: string
  ifsccode: string
  AccountType: "SB" | "CA" | "CC" | "OD"
}
```

**Carry Forward Losses:**
```json
scheduleCFL.CarryFwdLossDetail[] {
  AssessmentYear: string
  DateOfFiling: "YYYY-MM-DD"
  HpLossCF: number (house property loss)
  StcgLossCF: number (short-term capital gains loss)
  LtcgLossCF: number (long-term capital gains loss)
  OthSrcLossRaceHorseCF: number
}
```

**Filing Status:**
```json
filingStatus.OptingNewTaxRegimeForm10IF: 1 | 2  // 1=old, 2=new
filingStatus.SeventhProvisio139: "Y" | "N"
filingStatus.clauseiv7provisio139i: "Y" | "N"
filingStatus.receiptNo: string
filingStatus.returnFileSec: number
filingStatus.origRetFiledDate: "YYYY-MM-DD"
```

### Extraction Priority
1. ✅ **IMPLEMENTED:** Basic 26AS TDS parsing via `Form26ASJsonImportService.importFromPrefillJson()`
2. **PENDING:** Form 24Q (salary details with employer TAN)
3. **PENDING:** Insights data (savings interest, dividends, term deposits)
4. **PENDING:** Bank account details
5. **PENDING:** Carry forward losses
6. **PENDING:** Tax payment challan details

---

## 2. FORM 26AS (ZIP/TXT/PDF)

### Format
- **File Pattern:** `{PAN}-{AY}.zip` or `{PAN}-{AY}.pdf`
- **Examples:**
  - `ACUPG3482G-2025.zip` (2.5 KB)
  - `ACUPG3482G-2025.pdf` (141 KB)
  - `COVPC5929M-2025.pdf` (113 KB)
- **Password:** Date of Birth in `ddmmyyyy` format
- **Encryption:** ZIP with password, PDF may be password-protected

### ZIP Contents
- Single TXT file: `{PAN}-{AY}.txt`
- Plain text format (tabular/structured)
- **Current sample:** `ACUPG3482G-2025.txt` (empty/corrupted in extracted temp_26as)

### PDF Structure (Visual Analysis Required)
- ITD official format with header/footer
- Sections:
  - Part A: TDS/TCS details
  - Part B: Tax deposited details
  - Part C: Refund details
  - Part D: AIR transaction details
  - Part E: Tax collected at source (TCS)
- Tabular data with columns:
  - Deductor/Collector TAN
  - Deductor/Collector Name
  - Section
  - Transaction Date
  - Amount Paid/Credited
  - Tax Deducted/Collected
  - TDS/TCS Deposited Date

### Extraction Approach
1. **ZIP:** Extract with DOB password → parse TXT file (tabular format)
2. **PDF:** Use PDF parsing library (Apache PDFBox, iText, or pdf-parse)
   - Extract tables from each section
   - Parse column data
   - Map to `TDSOtherThanSalary` entities

### Current Implementation Status
- `Form26ASImportService.import26AS()` exists but incomplete
- Uses `PDFBoxReader` - may need enhancement for ITD format
- No ZIP extraction logic yet

---

## 3. AIS JSON (Annual Information Statement - Encrypted)

### Format
- **File Pattern:** `XXX{last5PAN}_{AY}_AIS_{date}.json`
- **Examples:**
  - `XXXPG3482X_2025-26_AIS_11062026.json` (80 KB)
  - `XXXPC5929X_2025-26_AIS_27042026.json` (33 KB)
  - `XXXPB8923X_2025-26_AIS_27042026.json` (25 KB)
- **Encryption:** PBKDF2 + AES
  - Format: `{32-char-hash}{base64-encrypted-payload}`
  - Password: Last 4 PAN digits + DOB `ddmmyyyy`
- **Size:** 25-80 KB (encrypted), likely 50-200 KB decrypted

### Encryption Details
From earlier analysis:
```
Hash: First 64 hex chars (32 bytes)
Salt: Derived from hash
IV: First 16 bytes of encrypted data
Encrypted payload: Remainder of base64 string
Algorithm: PBKDF2 (100,000 iterations) → AES-256-CBC
```

### Structure (Post-Decryption - from earlier sessions)
```json
{
  "partB1": [  // TDS (Tax Deducted at Source)
    {
      "srcDesc": "Income from Salary",
      "transactionDetails": [{
        "tan": "DELH12345A",
        "deductorName": "EMPLOYER NAME",
        "amtPaid": 1500000,
        "tdsClaimed": 150000,
        "tdsDeposited": 150000,
        "financialYear": "2025-26"
      }]
    }
  ],
  "partB2": [  // TCS (Tax Collected at Source)
    {
      "srcDesc": "Sale of Goods",
      "transactionDetails": [...]
    }
  ],
  "partB3": [  // Other Information
    {
      "srcDesc": "Interest from Bank",
      "transactionDetails": [{
        "nature": "Savings Account Interest",
        "amount": 25000,
        "remarks": "SBI Main Branch"
      }]
    }
  ]
}
```

### Current Implementation
- `AISJsonImportService.importAIS()` exists with full decryption logic
- Uses `javax.crypto` for PBKDF2+AES
- Parses Part B1 (TDS), B2 (TCS), B3 (other info)

### Enhancement Needed
- Add validation for encrypted vs plain JSON
- Handle multiple PAN formats (full vs masked)
- Map to domain entities properly

---

## 4. AIS PDF (Annual Information Statement - Visual)

### Format
- **File Pattern:** `XXX{last5PAN}_{AY}_AIS.pdf`
- **Examples:**
  - `XXXPG3482X_2025-26_AIS (1).pdf` (165 KB)
  - `XXXPC5929X_2025-26_AIS.pdf` (160 KB)
  - `XXXPB8923X_2025-26_AIS.pdf` (159 KB)
- **Encryption:** None (plain PDF, may be password-protected with DOB)
- **Size:** 160-180 KB

### Visual Structure
- ITD official format
- Multi-page document with sections:
  - **Part A:** Taxpayer Information (PAN, Name, DOB, Address)
  - **Part B1:** Information Reported by Others - TDS
    - Tables with columns: Source, TAN/PAN, Name, Amount, TDS, Year
  - **Part B2:** Information Reported by Others - TCS
  - **Part B3:** Other Information (Interest, Dividends, Securities, etc.)
  - **Part C:** Demand/Refund Outstanding

### PDF Parsing Requirements
- Extract multi-page tables
- Handle merged cells and spanning headers
- Parse section headers to identify data type
- Extract both transaction-level and summary data

### Extraction Approach
1. Use Apache PDFBox `PDFTextStripper` with position-based extraction
2. Or use `PDFTableExtractor` for structured table data
3. Pattern matching for section headers
4. Map columns to entity fields

---

## 5. TIS PDF (Tax Information Summary - Visual)

### Format
- **File Pattern:** `XXX{last5PAN}_{AY}_TIS.pdf`
- **Examples:**
  - `XXXPG3482X_2025-26_TIS.pdf` (143 KB)
  - `XXXPC5929X_2025-26_TIS.pdf` (139 KB)
  - `XXXPB8923X_2025-26_TIS.pdf` (137 KB)
- **Encryption:** None (plain PDF, may be password-protected)
- **Size:** 135-150 KB

### Visual Structure
- Summary-focused document (vs detailed AIS)
- Sections:
  - **Personal Information**
  - **TDS Summary** (aggregate by deductor)
  - **Tax Payments** (challan summary)
  - **Refund Status**
  - **Outstanding Demand**

### Extraction Approach
- Similar to AIS PDF but simpler (less detail)
- Focus on aggregate values vs transaction-level data
- May be used for quick reconciliation

---

## Implementation Strategy

### Phase 4A: JSON Extraction (Current)
- [x] Prefill JSON - Basic 26AS TDS
- [ ] Prefill JSON - Form 24Q salary details
- [ ] Prefill JSON - Insights data
- [ ] AIS JSON - Decryption validation
- [ ] AIS JSON - Full Part B1/B2/B3 mapping

### Phase 4B: PDF Extraction
- [ ] 26AS PDF - Table extraction with PDFBox
- [ ] 26AS ZIP - Password extraction + TXT parsing
- [ ] AIS PDF - Multi-page table extraction
- [ ] TIS PDF - Summary extraction

### Phase 4C: Integration
- [ ] Unified import service with format detection
- [ ] Duplicate detection across sources (26AS + AIS overlap)
- [ ] Reconciliation logic (prefill vs 26AS vs AIS)
- [ ] Validation rules per Doc 3 Phase 4 requirements

---

## Technical Dependencies

### Already Available
- Jackson (JSON parsing)
- Apache PDFBox (PDF reading) - check version
- `javax.crypto` (AES decryption)

### May Need to Add
- `iText` or `PDFBox-Layout` for advanced table extraction
- ZIP4j or `java.util.zip` for password-protected ZIP
- OCR library if PDFs are scanned (unlikely for ITD docs)

---

## Next Steps

1. Read existing `Form26ASImportService` and `AISJsonImportService` code
2. Test AIS JSON decryption with actual sample
3. Implement 26AS PDF table extraction
4. Add Form 24Q parsing to Prefill JSON handler
5. Create unified `DocumentImportOrchestrator` service
