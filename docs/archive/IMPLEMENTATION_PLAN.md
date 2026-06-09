# ITR FILING SYSTEM — 101% COMPLIANCE REWRITE PLAN

## EXECUTIVE SUMMARY

**Current State:** ~30% compliant, missing critical tax engine components and mandatory schedules  
**Target State:** 101% CBDT/ITD compliant for AY 2025-26 & AY 2026-27  
**Estimated Effort:** 6-8 weeks (2 developers)  
**Approach:** Complete rewrite of tax calculation engine + all missing schedules

---

## PHASE 1: CORE TAX CALCULATION ENGINE (Week 1-2)

### 1.1 Tax Slab Engine
- [ ] Age-based slab determination (Regular/Senior/Super Senior)
- [ ] Old regime slabs (₹2.5L-5L-10L with ₹5L rebate)
- [ ] New regime AY 2025-26 (₹3L-7L-10L-12L-15L with ₹7L rebate)
- [ ] New regime AY 2026-27 (₹4L-8L-12L-16L-20L-24L with ₹12L rebate)
- [ ] HUF slab handling (no senior citizen benefit)

### 1.2 Surcharge & Marginal Relief
- [ ] 4-tier surcharge (10%/15%/25%/37% at ₹50L/₹1Cr/₹2Cr/₹5Cr)
- [ ] 15% cap on surcharge for 111A/112A income
- [ ] Marginal relief computation at all 4 thresholds
- [ ] ₹12L cliff marginal relief for AY 2026-27 new regime

### 1.3 Rebate 87A
- [ ] Old regime: ₹5L threshold, ₹12,500 max
- [ ] New AY 2025-26: ₹7L threshold, ₹25,000 max
- [ ] New AY 2026-27: ₹12L threshold, ₹60,000 max
- [ ] Apply ONLY to normal income (exclude 111A/112A/112/115BB/115BBH)

### 1.4 Special Rate Income Computation
- [ ] STCG u/s 111A: 15% (pre-July 23, 2024) / 20% (post-July 23, 2024)
- [ ] LTCG u/s 112A: 10% (pre-July 23) / 12.5% (post-July 23) with ₹1.25L exemption
- [ ] LTCG u/s 112: 20% with indexation OR 12.5% without (choice for pre-July 23 property)
- [ ] Lottery/gambling u/s 115BB: 30%
- [ ] VDA u/s 115BBH: 30%
- [ ] Unexplained income u/s 115BBE: 60% + 25% surcharge + 4% cess

### 1.5 Interest Calculations
- [ ] 234A: 1%/month from day after due date to filing date
- [ ] 234B: 1%/month on shortfall (90% threshold, senior citizen exemption)
- [ ] 234C: 1%/month on each installment shortfall (15%/45%/75%/100%)
- [ ] 234F: Late filing fee (₹5K if income >₹5L, ₹1K if ≤₹5L, ₹10K after Dec 31)

### 1.6 AMT (Alternate Minimum Tax)
- [ ] ATI computation (add back 10AA/35AD/80H-80RRB deductions)
- [ ] 18.5% AMT on ATI if >₹20L
- [ ] AMT credit u/s 115JD (carry forward 15 years)
- [ ] AMT vs regular tax comparison

---

## PHASE 2: ITR-1 COMPLETE IMPLEMENTATION (Week 2-3)

### 2.1 Part A — General Information
- [ ] All 24 mandatory fields
- [ ] PAN-Aadhaar linkage validation
- [ ] Filing type (Original/Revised/Updated/Belated)
- [ ] Residential status (ROR/RNOR/NR)
- [ ] 206AA flag (non-filer status check)
- [ ] Employer category validation

### 2.2 Schedule Salary (B1)
- [ ] Gross salary u/s 17(1), 17(2), 17(3) separately
- [ ] HRA exemption: min(actual HRA, rent-10% salary, 50%/40% metro/non-metro)
- [ ] LTA exemption (2 journeys per 4-year block)
- [ ] Standard deduction (₹50K old / ₹75K new)
- [ ] Entertainment allowance (Govt only): min(1/5 salary, ₹5K, actual)
- [ ] Professional tax (max ₹2,500)
- [ ] Perquisites valuation (RFA, car, ESOP, loans)

### 2.3 Schedule House Property (B2)
- [ ] GAV = max(actual rent, fair rent, municipal rateable value)
- [ ] Municipal taxes PAID (not accrued)
- [ ] 30% standard deduction
- [ ] Interest u/s 24(b) with limits (₹2L SOP, ₹30K if let-out >5yrs, unlimited let-out)
- [ ] Pre-construction interest (1/5th for 5 years from completion)
- [ ] HP loss cap ₹2L inter-head set-off
- [ ] Warning if HP loss >₹2L: "File ITR-2 to carry forward"

### 2.4 Schedule Other Sources (B3)
- [ ] Savings interest, FD interest, refund interest
- [ ] Family pension with deduction (₹15K old / ₹25K new, or 1/3 whichever lower)
- [ ] Dividend income
- [ ] EPF interest >₹2.5L/₹5L taxable
- [ ] Gifts from non-relatives >₹50K u/s 56(2)(x)
- [ ] Accrual basis prompt for FD/NSC interest

### 2.5 Schedule TDS1 & TDS2
- [ ] TDS1: Employer TAN, Form 16 Part A reference, period from/to
- [ ] TDS2: All other TDS (TAN, deductor name, section code, year of deduction)
- [ ] 26AS/AIS reconciliation flag
- [ ] Mismatch alerts

### 2.6 Schedule IT (Advance/Self-Assessment Tax)
- [ ] BSR code, challan number, date, CIN
- [ ] Tax type (advance/self-assessment)
- [ ] NSDL verification

### 2.7 Schedule EI (Exempt Income)
- [ ] PPF interest, Sukanya Samriddhi
- [ ] Tax-free bonds interest
- [ ] Gratuity exempt portion
- [ ] LTA exempt portion
- [ ] Agricultural income
- [ ] Share of profit from firm u/s 10(2A)

### 2.8 Deductions Chapter VI-A
- [ ] 80C: Aggregate cap ₹1.5L enforced across 80C+80CCC+80CCD(1)
- [ ] 80C breakdown: LIC, PPF, ELSS, home loan principal, tuition fees, NSC, SCSS, etc.
- [ ] 80CCD(1B): ₹50K cap over and above 80C
- [ ] 80CCD(2): Employer NPS (available in new regime)
- [ ] 80D: Self ₹25K/₹50K senior, Parents ₹25K/₹50K senior, cash disallowed
- [ ] 80E: Education loan interest (no cap, 8 years)
- [ ] 80G: 4-category system (100%/50% with/without 10% limit), cash >₹2K disallowed
- [ ] 80TTA vs 80TTB: Age-based, mutually exclusive, unavailable in new regime
- [ ] 80GG: Min of 3 conditions, Form 10BA requirement

### 2.9 Eligibility Guards
- [ ] Director → redirect to ITR-2
- [ ] Unlisted shares → redirect to ITR-2
- [ ] Capital gains → redirect to ITR-2
- [ ] Income >₹50L → redirect to ITR-2
- [ ] More than 1 house property → redirect to ITR-2

---

## PHASE 3: ITR-2 COMPLETE IMPLEMENTATION (Week 3-4)

### 3.1 All ITR-1 Schedules (Inherited)
- [ ] Implement all ITR-1 schedules

### 3.2 Schedule HP (Multiple Properties)
- [ ] Multiple property support
- [ ] Deemed let-out (>2 self-occupied properties)
- [ ] Co-owner PAN, tenant PAN if rent >₹50K/month
- [ ] Unrealised rent deduction
- [ ] Aggregate HP loss → CYLA → max ₹2L set-off, carry forward 8yr

### 3.3 Schedule CG (Capital Gains) — CRITICAL
- [ ] **CII Table:** All years (100 to 363)
- [ ] **STCG Classification:**
  - Section 111A: Listed equity with STT (15% pre-July 23 / 20% post-July 23)
  - Other STCG: Slab rate
- [ ] **LTCG Classification:**
  - Section 112A: Listed equity with STT (10% pre-July 23 / 12.5% post-July 23)
  - Section 112: Property/unlisted (20% with indexation OR 12.5% without)
- [ ] **Grandfathering:** Pre-Jan 31, 2018 equity (higher of cost or FMV on Jan 31, 2018)
- [ ] **Section 50C:** Stamp duty value deemed consideration (110% tolerance)
- [ ] **Section 50CA:** Unlisted shares FMV (Rule 11UA)
- [ ] **Cost of Acquisition Special Cases:**
  - Gift: Donor's cost + donor's acquisition year
  - Will/inheritance: Deceased's cost + deceased's acquisition year
  - Bonus shares: Cost = 0
  - ESOP: Cost = FMV on exercise date
- [ ] **Exemptions:**
  - 54: Residential house → new residential house
  - 54EC: Land/building → NHAI/REC bonds (max ₹50L)
  - 54F: Any asset → residential house (proportionate)
  - 54GB: Residential → startup equity
- [ ] **CGAS Tracking:** Deposit date, investment deadline, utilization
- [ ] **LTCG 112A Exemption:** ₹1,25,000 annual aggregate

### 3.4 Schedule VDA (Virtual Digital Assets) — NEW
- [ ] VDA transactions (crypto, NFT)
- [ ] 30% flat tax u/s 115BBH
- [ ] No loss set-off or carry forward
- [ ] 194S TDS reconciliation

### 3.5 Schedule CYLA (Current Year Loss Adjustment)
- [ ] Loss set-off order: HP → Business → STCG → LTCG → Speculative
- [ ] HP loss max ₹2L inter-head
- [ ] Speculative loss only against speculative income
- [ ] LTCG loss only against LTCG

### 3.6 Schedule BFLA (Brought Forward Loss Adjustment)
- [ ] HP loss: 8 years, only against HP
- [ ] Business loss: 8 years, against any income except salary
- [ ] Speculative loss: 4 years, only against speculative
- [ ] STCG loss: 8 years, against STCG or LTCG
- [ ] LTCG loss: 8 years, only against LTCG
- [ ] Unabsorbed depreciation: Unlimited

### 3.7 Schedule CFL (Carry Forward Loss)
- [ ] Future carry-forward tracking
- [ ] Period limits per loss type

### 3.8 Schedule AL (Assets & Liabilities)
- [ ] Mandatory if income >₹50L
- [ ] Immovable property (address, cost, year)
- [ ] Movable assets (jewellery, vehicles, shares)
- [ ] Insurance policies
- [ ] Loans given/taken
- [ ] Cash in hand >₹5L
- [ ] Bank deposits

### 3.9 Schedule FA (Foreign Assets)
- [ ] Foreign bank accounts (country, bank, account, balance, interest)
- [ ] Foreign equity/debt
- [ ] Foreign immovable property
- [ ] Signing authority
- [ ] Trust beneficiary
- [ ] Black Money Act warning (₹10L penalty per asset)

### 3.10 Schedule FSI & TR (Foreign Income & Tax Relief)
- [ ] Foreign source income by country
- [ ] Foreign tax paid
- [ ] DTAA credit u/s 90 (exemption or credit method)
- [ ] Unilateral relief u/s 91 (no DTAA)
- [ ] Form 67 pre-filing requirement

### 3.11 Schedule AMT & AMTC
- [ ] ATI computation
- [ ] 18.5% AMT if ATI >₹20L
- [ ] AMT credit carry forward (15 years)

### 3.12 Schedule SI (Special Rate Income)
- [ ] Separate computation for each special rate
- [ ] Basic exemption utilization order
- [ ] 87A rebate exclusion for special rate income

### 3.13 Schedule SPI (Clubbing)
- [ ] Spouse income u/s 64(1)(iv)
- [ ] Minor child income u/s 64(1A) with ₹1,500 exemption per child
- [ ] HUF member income u/s 64(2)

---

## PHASE 4: ITR-3 COMPLETE IMPLEMENTATION (Week 4-5)

### 4.1 All ITR-2 Schedules (Inherited)
- [ ] Implement all ITR-2 schedules

### 4.2 Schedule BP (Business/Professional Income)
- [ ] Net profit from P&L
- [ ] Add: Disallowances
  - 40(a)(ia): 30% for no TDS
  - 40A(2): Excess payment to relatives
  - 40A(3): Cash >₹10K per person per day
  - 40A(7): Gratuity provision
  - 40B: Partner remuneration limits
- [ ] Add: 43B payment-basis items
  - Taxes, PF/ESI, bonus, interest, leave encashment
  - 43B(h): MSME payment rule (45 days)
- [ ] Add: 14A exempt income disallowance (Rule 8D)
- [ ] Less: IT Act depreciation
- [ ] Less: Additional depreciation 20% u/s 32(1)(iia)
- [ ] Less: Deductions u/s 30-37

### 4.3 Schedule DPM (Depreciation)
- [ ] Block of assets method
- [ ] All IT Act rates (40+ categories)
- [ ] Additional depreciation 20% (50% if <180 days)
- [ ] Goodwill 0% from AY 2021-22
- [ ] Block becomes zero → STCG on excess

### 4.4 Schedule GST Reconciliation
- [ ] Turnover as per books
- [ ] Turnover as per GSTR-1
- [ ] Difference explanation
- [ ] Exempt supply, composition turnover

### 4.5 F&O (Futures & Options)
- [ ] Non-speculative classification u/s 43(5)
- [ ] Turnover = absolute profit + absolute loss
- [ ] Audit threshold (₹10Cr digital / ₹1Cr if cash >5%)
- [ ] Loss set-off against any income except salary
- [ ] Carry forward 8 years

### 4.6 Intraday Trading
- [ ] Speculative classification
- [ ] Turnover = absolute profit + absolute loss
- [ ] Loss only against speculative income
- [ ] Carry forward 4 years

### 4.7 Partner Income from Firm
- [ ] Share of profit u/s 10(2A) exempt
- [ ] Interest on capital taxable
- [ ] Remuneration taxable (limits u/s 40(b))

### 4.8 Section 80JJAA (New Employee Deduction)
- [ ] 30% of additional employee cost × 3 AYs
- [ ] Conditions: emoluments ≤₹25K, 240 days, EPFO registered

### 4.9 Section 35AD (Specified Business)
- [ ] 100% CAPEX deduction (not land/goodwill)
- [ ] Eligible businesses (cold chain, hospital, hotel, etc.)

### 4.10 Balance Sheet & P&L
- [ ] Complete balance sheet (assets, liabilities, capital)
- [ ] Complete P&L (all expense heads u/s 30-37)

### 4.11 Audit Details
- [ ] Form 3CA-3CD
- [ ] Auditor PAN, membership number
- [ ] Audit date

---

## PHASE 5: ITR-4 COMPLETE IMPLEMENTATION (Week 5-6)

### 5.1 Section 44AD (Presumptive Business)
- [ ] 6% digital / 8% cash split computation
- [ ] Turnover limit: ₹2Cr OR ₹3Cr (both receipts AND payments ≤5% cash)
- [ ] **5-year lock-in warning:** Opt-out → books + audit for 5 years
- [ ] Commission/brokerage agents NOT eligible
- [ ] Can declare higher income
- [ ] Cannot declare lower without books + audit

### 5.2 Section 44ADA (Presumptive Professional)
- [ ] 50% of gross receipts
- [ ] Limit: ₹50L OR ₹75L (both receipts AND payments ≤5% cash)
- [ ] Eligible professions: Legal, Medical, Engineering, Accounting, etc.
- [ ] Firm of professionals NOT eligible
- [ ] LLP NOT eligible

### 5.3 Section 44AE (Goods Vehicle)
- [ ] Heavy (>12T): ₹1,000/ton/month
- [ ] Light (≤12T): ₹7,500/vehicle/month
- [ ] Part month = full month
- [ ] Max 10 vehicles at ANY TIME during year
- [ ] Passenger vehicles NOT eligible

### 5.4 Simplified Balance Sheet
- [ ] Sundry debtors (closing)
- [ ] Sundry creditors (closing)
- [ ] Stock-in-trade (closing)
- [ ] Cash balance
- [ ] Opening capital, drawings, additions
- [ ] Secured/unsecured loans
- [ ] Fixed assets (closing WDV)

### 5.5 Advance Tax Schedule
- [ ] Single installment: 100% by March 15
- [ ] NOT quarterly (15%/45%/75%/100%)

### 5.6 Eligibility Guards
- [ ] Capital gains → redirect to ITR-3
- [ ] >1 house property → redirect to ITR-3
- [ ] Total income >₹50L → redirect to ITR-3
- [ ] LLP → redirect to ITR-5

---

## PHASE 6: DATA IMPORT & EXPORT (Week 6)

### 6.1 Form 16 PDF Upload & Extraction
- [ ] PDF upload endpoint (multipart/form-data)
- [ ] Apache PDFBox integration for text extraction
- [ ] Part A extraction: Employer details, TAN, PAN, salary breakup, TDS deducted
- [ ] Part B extraction: Allowances, perquisites, deductions (80C, 80D, etc.)
- [ ] Quarterly TDS extraction (Q1-Q4)
- [ ] Auto-populate Schedule Salary fields
- [ ] Auto-populate Schedule TDS1 fields
- [ ] Auto-populate deductions (80C, 80D, etc.)
- [ ] Validation: Cross-check extracted PAN with client PAN
- [ ] Error handling: Scanned PDFs, password-protected PDFs
- [ ] Support multiple Form 16 (multiple employers)

### 6.2 AIS/TIS/26AS JSON Import
- [ ] JSON upload endpoint (application/json)
- [ ] AIS JSON parser (ITD schema v2.0)
  - TDS (Part A) → Schedule TDS2
  - TCS (Part B) → Schedule TCS
  - SFT (Part E) → Schedule AL, Schedule FA
  - Interest income → Schedule OS
  - Dividend income → Schedule OS
  - Capital gains → Schedule CG
  - GST turnover → Schedule GST
- [ ] 26AS JSON parser
  - TDS deducted → Schedule TDS1/TDS2
  - TCS collected → Schedule TCS
  - Advance tax → Schedule IT
  - Self-assessment tax → Schedule IT
  - Refund → Schedule IT
- [ ] TIS JSON parser (Taxpayer Information Summary)
  - Aggregated income data
  - High-value transactions
- [ ] Prefill JSON parser (ITD Prefill API response)
  - Personal details → Part A
  - Salary details → Schedule Salary
  - House property → Schedule HP
  - TDS → Schedule TDS1/TDS2
- [ ] Auto-population with conflict resolution
  - If field already filled → show comparison UI
  - Allow user to accept/reject imported data
- [ ] Validation: Check for duplicate entries
- [ ] Mismatch alerts: Imported vs manually entered data

### 6.3 Statement of Income PDF Generation
- [ ] iText 7 integration
- [ ] Reference format from `reference computation.pdf`
- [ ] Cover page: Client name, PAN, AY, filing date
- [ ] Section 1: Personal Information
  - Name, PAN, Aadhaar, DOB, age, residential status
  - Address, email, mobile
- [ ] Section 2: Income Summary
  - Salary income (gross, exemptions, net)
  - House property income (GAV, deductions, net)
  - Business/professional income (turnover, net profit)
  - Capital gains (STCG, LTCG separately)
  - Other sources (interest, dividend, etc.)
  - Gross Total Income
- [ ] Section 3: Deductions Chapter VI-A
  - 80C breakdown (LIC, PPF, ELSS, etc.)
  - 80D (self, parents)
  - 80E, 80G, 80TTA/80TTB, etc.
  - Total deductions
- [ ] Section 4: Tax Computation
  - Total income after deductions
  - Tax on normal income (slab-wise breakup)
  - Tax on special rate income (111A, 112A, 112, etc.)
  - Surcharge (with marginal relief calculation shown)
  - Health & Education Cess
  - Total tax liability
- [ ] Section 5: Rebate & Relief
  - 87A rebate (if applicable)
  - Relief u/s 89 (if applicable)
  - Foreign tax credit (if applicable)
- [ ] Section 6: Taxes Paid
  - TDS (employer + others)
  - TCS
  - Advance tax
  - Self-assessment tax
  - Total taxes paid
- [ ] Section 7: Net Tax Payable/Refund
  - Tax liability - Taxes paid
  - Interest u/s 234A/B/C (if applicable)
  - Late filing fee u/s 234F (if applicable)
  - Final demand/refund
- [ ] Section 8: Schedules Summary
  - Schedule CG: Transaction-wise capital gains
  - Schedule HP: Property-wise income
  - Schedule AL: Assets & liabilities (if income >₹50L)
  - Schedule FA: Foreign assets (if applicable)
- [ ] Watermark: "For Reference Only - Not for ITD Submission"
- [ ] Page numbers, table of contents
- [ ] Download endpoint: `/api/clients/{id}/itr/{year}/statement-of-income.pdf`

### 6.4 ITD-Compliant JSON Export
- [ ] ITD JSON schema v7.0 (AY 2025-26) integration
- [ ] ITD JSON schema v8.0 (AY 2026-27) integration
- [ ] Schema validation library (JSON Schema Validator)
- [ ] ITR-1 JSON generator
  - Map all fields to ITD schema
  - Handle optional vs mandatory fields
  - Date format: DD-MM-YYYY
  - Amount format: Integer (no decimals)
  - Boolean: "Y"/"N" (not true/false)
- [ ] ITR-2 JSON generator
  - All ITR-1 fields
  - Schedule CG with transaction details
  - Schedule VDA
  - Schedule AL, FA, FSI, TR
  - Schedule AMT
- [ ] ITR-3 JSON generator
  - All ITR-2 fields
  - Schedule BP, DPM
  - Balance sheet, P&L
  - Audit details (Form 3CA-3CD)
- [ ] ITR-4 JSON generator
  - Presumptive income fields
  - Simplified balance sheet
- [ ] Pre-validation checks
  - All mandatory fields present
  - Field length limits
  - Enum value validation
  - Cross-field validations
- [ ] Schema validation
  - Validate against official ITD JSON schema
  - Report validation errors with field path
- [ ] Digital signature placeholder
  - DSC (Digital Signature Certificate) field
  - EVC (Electronic Verification Code) field
- [ ] Download endpoint: `/api/clients/{id}/itr/{year}/export-json`
- [ ] Filename format: `ITR{1-4}_{PAN}_{AY}.json`
- [ ] Success message: "JSON ready for upload to ITD portal"

### 6.5 File Management
- [ ] Secure file storage (encrypted at rest)
- [ ] File size limits (Form 16: 10MB, JSON: 5MB)
- [ ] Virus scanning (ClamAV integration)
- [ ] Retention policy (delete after 7 years)
- [ ] Audit trail: Who uploaded, when, what was extracted

---

## PHASE 7: VALIDATION ENGINE (Week 6-7)

### 7.1 PAN-Aadhaar Validation
- [ ] ITD API integration
- [ ] Inoperative PAN check
- [ ] Reactivation prompt (₹1,000 fee)

### 7.2 Form 26AS/AIS/TIS Reconciliation
- [ ] TDS reconciliation (Part A)
- [ ] TCS reconciliation (Part B)
- [ ] Advance tax reconciliation (Part C)
- [ ] SFT reconciliation (Part E)
- [ ] AIS comprehensive data
- [ ] Mismatch alerts

### 7.3 Eligibility Guards
- [ ] ITR-1: Director/unlisted shares/CG → ITR-2
- [ ] ITR-2: Business income → ITR-3
- [ ] ITR-3: Presumptive eligible → ITR-4 option
- [ ] ITR-4: CG/multiple HP/income >₹50L → ITR-3

### 7.4 Field Validations
- [ ] PAN format: [A-Z]{5}[0-9]{4}[A-Z]
- [ ] Aadhaar format: 12 digits
- [ ] TAN format: [A-Z]{4}[0-9]{5}[A-Z]
- [ ] IFSC format: [A-Z]{4}0[A-Z0-9]{6}
- [ ] Email format
- [ ] Mobile format
- [ ] PIN code: 6 digits

### 7.5 Pre-Filing Checklist
- [ ] PAN-Aadhaar linked
- [ ] Regime elected
- [ ] Form 10E filed (if 89 relief)
- [ ] Form 10-IEA filed (if business, opting out of new regime)
- [ ] 26AS/AIS reconciled
- [ ] Bank account pre-validated
- [ ] CGAS deposit (if applicable)
- [ ] 80G cash donation check
- [ ] Foreign asset disclosure (if applicable)

---

## PHASE 8: FRONTEND IMPLEMENTATION (Week 7-8)

### 8.1 File Upload UI
- [ ] Form 16 PDF upload component
  - Drag & drop support
  - File type validation (.pdf only)
  - Progress bar during upload
  - Extraction status indicator
  - Preview extracted data before auto-populate
  - Accept/reject extracted fields
- [ ] JSON upload component (AIS/TIS/26AS/Prefill)
  - File type validation (.json only)
  - JSON structure validation
  - Preview imported data
  - Conflict resolution UI (imported vs existing)
  - Bulk accept/reject
- [ ] Upload history
  - List of uploaded files
  - Re-extract option
  - Delete uploaded file

### 8.2 ITR-1 UI
- [ ] All schedules with validation
- [ ] Real-time tax computation
- [ ] Regime comparison
- [ ] Eligibility warnings

### 8.3 ITR-2 UI
- [ ] Capital gains wizard (CII, grandfathering, exemptions)
- [ ] VDA transaction entry
- [ ] Foreign asset disclosure
- [ ] Loss adjustment UI

### 8.4 ITR-3 UI
- [ ] P&L and Balance Sheet
- [ ] Depreciation schedule
- [ ] GST reconciliation
- [ ] F&O/intraday classification

### 8.5 ITR-4 UI
- [ ] Presumptive income calculator
- [ ] 5-year lock-in warning
- [ ] Simplified balance sheet

### 8.6 Download & Export UI
- [ ] Statement of Income PDF download button
  - Preview before download
  - Email option (send to client)
- [ ] ITD JSON export button
  - Pre-validation status indicator
  - Download JSON file
  - Copy to clipboard option
  - Instructions: "Upload this JSON to ITD portal"
- [ ] Bulk export (multiple clients)

### 8.7 Common Components
- [ ] Tax computation preview (live calculation)
- [ ] Interest calculator (234A/B/C/F)
- [ ] Regime comparison widget
- [ ] Validation error panel
- [ ] Progress tracker (% completion)

---

## PHASE 9: TESTING & VALIDATION (Week 8)

### 9.1 Unit Tests
- [ ] Tax calculation engine (100+ test cases)
- [ ] All schedules
- [ ] Validation rules

### 9.2 Integration Tests
- [ ] End-to-end ITR filing flow
- [ ] 26AS/AIS reconciliation
- [ ] PDF generation
- [ ] ITD JSON export

### 9.3 ITD Schema Validation
- [ ] Validate against official ITD JSON schema
- [ ] Test with ITD validation utility

### 9.4 Form 16 Extraction Tests
- [ ] Test with 50+ real Form 16 PDFs
- [ ] Different formats (various employers)
- [ ] Scanned PDFs (OCR required)
- [ ] Password-protected PDFs
- [ ] Corrupted PDFs

### 9.5 JSON Import Tests
- [ ] Valid AIS/26AS/TIS/Prefill JSONs
- [ ] Invalid JSON structure
- [ ] Missing mandatory fields
- [ ] Duplicate entries
- [ ] Large files (>1MB)

### 9.6 PDF Generation Tests
- [ ] All 4 ITR types
- [ ] Edge cases (₹0 income, negative income)
- [ ] Large datasets (100+ CG transactions)
- [ ] Special characters in names/addresses

### 9.7 JSON Export Tests
- [ ] Validate against ITD schema v7.0 (AY 2025-26)
- [ ] Validate against ITD schema v8.0 (AY 2026-27)
- [ ] Test with ITD validation utility
- [ ] Upload to ITD test portal (if available)

### 9.8 Edge Cases
- [ ] ₹5L/₹7L/₹12L rebate cliffs
- [ ] Surcharge marginal relief
- [ ] Split-rate capital gains
- [ ] CGAS scenarios
- [ ] Loss carry forward

---

## DEPENDENCIES TO ADD

### Backend (Maven)
```xml
<!-- PDF Processing -->
<dependency>
  <groupId>org.apache.pdfbox</groupId>
  <artifactId>pdfbox</artifactId>
  <version>3.0.1</version>
</dependency>

<!-- PDF Generation -->
<dependency>
  <groupId>com.itextpdf</groupId>
  <artifactId>itext7-core</artifactId>
  <version>8.0.3</version>
  <type>pom</type>
</dependency>

<!-- JSON Schema Validation -->
<dependency>
  <groupId>com.networknt</groupId>
  <artifactId>json-schema-validator</artifactId>
  <version>1.0.87</version>
</dependency>

<!-- Already added -->
<!-- <dependency>
  <groupId>io.hypersistence</groupId>
  <artifactId>hypersistence-utils-hibernate-63</artifactId>
  <version>3.7.0</version>
</dependency> -->
```

---

## DATABASE SCHEMA CHANGES

### New Tables
```sql
-- Tax computation cache
CREATE TABLE tax_computation_cache (
  id BIGSERIAL PRIMARY KEY,
  client_id BIGINT REFERENCES clients(id),
  assessment_year VARCHAR(10),
  regime VARCHAR(10),
  computation_data JSONB,
  created_at TIMESTAMP DEFAULT NOW()
);

-- CII master table
CREATE TABLE cost_inflation_index (
  financial_year VARCHAR(10) PRIMARY KEY,
  index_value INTEGER NOT NULL
);

-- DTAA master table
CREATE TABLE dtaa_countries (
  country_code VARCHAR(3) PRIMARY KEY,
  country_name VARCHAR(100),
  treaty_article TEXT,
  relief_method VARCHAR(20),
  withholding_rate DECIMAL(5,2)
);

-- Loss carry forward tracking
CREATE TABLE loss_carry_forward (
  id BIGSERIAL PRIMARY KEY,
  client_id BIGINT REFERENCES clients(id),
  loss_type VARCHAR(50),
  assessment_year VARCHAR(10),
  loss_amount DECIMAL(15,2),
  utilized_amount DECIMAL(15,2),
  balance_amount DECIMAL(15,2),
  expiry_year VARCHAR(10)
);

-- CGAS tracking
CREATE TABLE cgas_deposits (
  id BIGSERIAL PRIMARY KEY,
  client_id BIGINT REFERENCES clients(id),
  deposit_date DATE,
  amount DECIMAL(15,2),
  exemption_section VARCHAR(10),
  investment_deadline DATE,
  utilized_amount DECIMAL(15,2),
  balance_amount DECIMAL(15,2)
);

-- Uploaded files tracking
CREATE TABLE uploaded_files (
  id BIGSERIAL PRIMARY KEY,
  client_id BIGINT REFERENCES clients(id),
  file_type VARCHAR(50), -- 'FORM16', 'AIS', 'TIS', '26AS', 'PREFILL'
  file_name VARCHAR(255),
  file_path VARCHAR(500),
  file_size BIGINT,
  upload_date TIMESTAMP DEFAULT NOW(),
  uploaded_by BIGINT REFERENCES users(id),
  extraction_status VARCHAR(20), -- 'PENDING', 'SUCCESS', 'FAILED'
  extraction_data JSONB,
  error_message TEXT
);

-- ITD JSON export history
CREATE TABLE json_exports (
  id BIGSERIAL PRIMARY KEY,
  client_id BIGINT REFERENCES clients(id),
  assessment_year VARCHAR(10),
  itr_type VARCHAR(10),
  json_data JSONB,
  validation_status VARCHAR(20), -- 'VALID', 'INVALID'
  validation_errors JSONB,
  export_date TIMESTAMP DEFAULT NOW(),
  exported_by BIGINT REFERENCES users(id)
);

-- Statement of Income PDF history
CREATE TABLE statement_pdfs (
  id BIGSERIAL PRIMARY KEY,
  client_id BIGINT REFERENCES clients(id),
  assessment_year VARCHAR(10),
  file_path VARCHAR(500),
  generation_date TIMESTAMP DEFAULT NOW(),
  generated_by BIGINT REFERENCES users(id)
);
```

---

## FILES TO DELETE (Cleanup)

### Backend (Keep but refactor)
- [ ] Refactor `ITR1CalculatorService.java` (integrate new engine)
- [ ] Refactor `ITR2CalculatorService.java` (integrate new engine)
- [ ] Refactor `ITR3CalculatorService.java` (integrate new engine)
- [ ] Refactor `ITR4CalculatorService.java` (integrate new engine)
- [ ] Keep `Form16ParserService.java` (already exists, enhance)
- [ ] Keep `PDFComputationService.java` (already exists, enhance with reference format)
- [ ] Keep `ITDJSONExportService.java` (already exists, enhance with schema validation)
- [ ] Keep `JSONImportService.java` (already exists, enhance with AIS/TIS/26AS parsers)

### Frontend
- [ ] Duplicate ITR pages (`/itr2/page.tsx`, `/itr3/page.tsx`, `/itr4/page.tsx`)
- [ ] Incomplete schedule components

---

## SUCCESS CRITERIA

✅ All 4 ITR forms pass ITD JSON schema validation  
✅ Tax computation matches manual calculation for 100+ test cases  
✅ All mandatory schedules implemented  
✅ 234A/B/C/F interest calculations accurate  
✅ Surcharge marginal relief working  
✅ Split-rate capital gains working  
✅ Form 16 PDF extraction working (90%+ accuracy)  
✅ AIS/TIS/26AS/Prefill JSON import working  
✅ Statement of Income PDF matches reference format  
✅ ITD JSON export passes official validation utility  
✅ 26AS/AIS reconciliation working  
✅ Zero compilation errors  
✅ Zero validation errors  
✅ Production-ready code quality  

---

## RISK MITIGATION

1. **Parallel Development:** Keep old system running during rewrite
2. **Incremental Testing:** Test each phase before moving to next
3. **Data Backup:** Full database backup before migration
4. **Rollback Plan:** Ability to revert to old system if needed
5. **User Acceptance Testing:** Test with real client data before production

---

**READY TO PROCEED?**

This is a complete rewrite. Confirm you want to proceed with Phase 1 (Tax Calculation Engine).
