# ITR ERP — COMPLETE IMPLEMENTATION DIRECTIVE
## Phase 5: Finishing the Remaining 22% for End-to-End Deployable System
**Date:** April 11, 2026  
**Prepared For:** Agentic AI Implementation  
**Project Status:** 78% CBDT Compliant — ITR-1 Production-Ready; ITR-2/3/4 Incomplete  
**Goal:** Achieve 100% CBDT compliance across all ITR forms, resolve all placeholder implementations, and produce a fully deployable production system.

---

## HOW TO USE THIS DOCUMENT

This document is a **complete, self-contained implementation directive**. Read it in full before writing a single line of code. Each section specifies:
- **WHAT** to build (exact class names, method signatures, fields)
- **WHY** (legal basis and validation rule reference)
- **HOW** (algorithm, formula, or pseudocode)
- **WHERE** to integrate (which existing service to extend or call)

All rule IDs (VR2-HP-001, VR3-BP-001, etc.) correspond to CBDT e-Filing Validation Rules V1.0/V1.1 (July 2025). All monetary amounts are **integers in INR** — no decimals anywhere in ITD JSON output.

---

## SECTION 1 — CURRENT STATE SUMMARY

### What Is Already Built (DO NOT REBUILD):
- `ITDJSONExportService.java` — ITR-1 JSON export (complete)
- `ITR1CBDTValidationService.java` — 99 Category A + 4 Category B + 4 Category D rules for ITR-1 (complete)
- `AISReconciliationService.java` — Rs 100 tolerance, Form 26AS priority (complete)
- `TaxRegimeComparisonService.java` — Old vs New regime, AY 2025-26 slabs, 87A rebate, surcharge, marginal relief (complete)
- `ITRFormSelectionService.java` — Form auto-selection logic (complete)
- `LTCG112AGrandfatheringService.java` — partial (grandfathering formula present but FMV computation incomplete)
- `SystemAlertService.java` — 20 alerts (complete)
- `HRAComputationService.java` — metro/non-metro formula (complete)
- `MultiEmployerConsolidationService.java` — standard deduction once, professional tax cap (complete)
- `VDATransactionService.java` — placeholder only
- `ScheduleALService.java` — manual entry only, no auto-population
- `PreSubmissionChecklistService.java` — 24 checks (complete)
- `GSTReconciliationService.java` — GSTR-1 vs GSTR-3B, Rs 10,000 tolerance (complete)
- `BreakEvenAnalysisService.java` — binary search (complete)
- `SFTProcessingService.java` — 17 SFT categories defined but parsing methods are stubs
- `AuditTrailService.java` — in-memory only

### What Must Be Built (This Directive):

| Priority | Component | Impact |
|----------|-----------|--------|
| 🔴 CRITICAL | ITR-2 CBDT Validation Service (63 rules) | Blocks ITR-2 filing |
| 🔴 CRITICAL | ITR-2 JSON Export Service | Blocks ITR-2 output |
| 🔴 CRITICAL | ITR-3 CBDT Validation Service (23 rules) | Blocks ITR-3 filing |
| 🔴 CRITICAL | ITR-3 JSON Export Service | Blocks ITR-3 output |
| 🔴 CRITICAL | ITR-4 CBDT Validation Service (20 rules) | Blocks ITR-4 filing |
| 🔴 CRITICAL | ITR-4 JSON Export Service | Blocks ITR-4 output |
| 🔴 CRITICAL | Schedule 112A FMV Grandfathering — complete | Wrong CG for equity filers |
| 🟠 HIGH | VDA Transaction Service — real implementation | Wrong tax for crypto filers |
| 🟠 HIGH | SFT Data Parsing — real implementation | AIS import broken |
| 🟠 HIGH | AY 2026-27 Tax Computation Engine | Wrong tax for next year filers |
| 🟠 HIGH | Depreciation Engine (Schedule DPM) | ITR-3 P&L wrong |
| 🟠 HIGH | Schedule CYLA/BFLA/CFL — Loss Set-off Engine | ITR-2/3 losses wrong |
| 🟡 MEDIUM | Schedule AL Auto-population from AIS/SFT | Manual entry required |
| 🟡 MEDIUM | HRA Metro Auto-detection from PAN address | Manual input required |
| 🟡 MEDIUM | Audit Trail — DB persistence | In-memory lost on restart |
| 🟡 MEDIUM | Form 16 Multi-employer cross-validation | Duplicate deduction risk |

---

## SECTION 2 — AY 2026-27 TAX COMPUTATION ENGINE

### 2.1 Why This Is Separate

The existing `TaxRegimeComparisonService.java` implements AY 2025-26 slabs only. Finance Act 2025 introduced substantially different AY 2026-27 slabs. **Both must coexist** in the same engine, driven by an `assessmentYear` parameter.

### 2.2 New Class: `TaxComputationEngine.java`

Refactor the existing `TaxRegimeComparisonService` to delegate to this engine. This engine is the **single source of truth** for all tax computations across the application.

```java
public class TaxComputationEngine {

    public TaxResult computeTax(TaxInput input) {
        // input.assessmentYear = "2025-26" or "2026-27"
        // input.regime = OLD or NEW
        // input.dateOfBirth — for age classification (see age rule below)
        // input.totalIncome — normal income (excluding special rate)
        // input.specialRateIncome — map of Section → Amount
        // input.deductions — Chapter VI-A total (old regime only)
    }
}
```

### 2.3 Age Computation Rule (CRITICAL)

```
// Age is computed as on APRIL 1 of the FINANCIAL YEAR (not AY)
// AY 2025-26 → FY 2024-25 → age as on April 1, 2024
// AY 2026-27 → FY 2025-26 → age as on April 1, 2025

int computeAge(LocalDate dob, String assessmentYear) {
    int fy_start_year = Integer.parseInt(assessmentYear.split("-")[0]) - 1;
    LocalDate referenceDate = LocalDate.of(fy_start_year, 4, 1);
    return Period.between(dob, referenceDate).getYears();
}

// Then classify:
// age < 60  → BELOW_60
// 60 <= age < 80 → SENIOR
// age >= 80 → SUPER_SENIOR
// NOTE: New regime has NO age distinction — all use single slab
```

### 2.4 AY 2026-27 New Regime Slabs (Finance Act 2025)

```
Slab Table — New Regime AY 2026-27:
  0 to 4,00,000         → 0%
  4,00,001 to 8,00,000  → 5%
  8,00,001 to 12,00,000 → 10%
  12,00,001 to 16,00,000 → 15%
  16,00,001 to 20,00,000 → 20%
  20,00,001 to 24,00,000 → 25%
  Above 24,00,000        → 30%

Rebate u/s 87A — New Regime AY 2026-27:
  IF total normal income <= 12,00,000: tax = 0 (full rebate, max Rs 60,000)
  IF total normal income > 12,00,000: NO rebate at all
  SPECIAL RATE INCOME (111A, 112A, 115BBH) is EXCLUDED from the 12L threshold test.
  
  MARGINAL RELIEF at 12L cliff:
    If income > 12,00,000 and income <= some threshold:
      marginal_relief = max(0, tax_computed - (income - 12,00,000))
    This ensures tax does not exceed the amount by which income exceeds Rs 12L.
    Compute threshold: solve for x where slab_tax(x) = x - 12,00,000
    (approximately Rs 12,75,000)
```

### 2.5 AY 2026-27 Old Regime Slabs (UNCHANGED from AY 2025-26)

Old regime slabs, 87A cliff at Rs 5,00,000, and surcharge rates remain identical. Only new regime changed. Reuse AY 2025-26 old regime slab table for AY 2026-27.

### 2.6 Standard Deduction Changes for AY 2026-27

```
AY 2025-26:
  Old regime salary: Rs 50,000
  New regime salary: Rs 75,000
  Family pension (both regimes): min(1/3 of pension, Rs 15,000) old; min(1/3, Rs 25,000) new

AY 2026-27: (Verify with CBDT notification — no change announced in Finance Act 2025.
  Use same figures unless CBDT notification says otherwise. Flag for manual verification.)
```

### 2.7 Surcharge — Both AYs (Same Table)

```
50L to 1Cr   → 10% surcharge
1Cr to 2Cr   → 15% surcharge
2Cr to 5Cr   → 25% surcharge
Above 5Cr    → 37% (old regime) / 25% (new regime — CAPPED since Budget 2023)

SURCHARGE ON SPECIAL RATE INCOME:
  111A (STCG listed equity) and 112A (LTCG listed equity): surcharge CAPPED at 15%
  All other incomes: normal surcharge table applies

MARGINAL RELIEF — implement at EVERY threshold (50L, 1Cr, 2Cr, 5Cr):
  tax_with_surcharge = base_tax + (surcharge_rate * base_tax)
  tax_on_threshold = compute_slab_tax(threshold)
  income_excess = total_income - threshold
  max_payable = tax_on_threshold + income_excess
  if (tax_with_surcharge > max_payable):
      marginal_relief = tax_with_surcharge - max_payable
  Apply the relief that MAXIMIZES benefit to taxpayer.
```

### 2.8 Section 234A/234B/234C Interest Computation

These must be computed in the engine and output to the ITR JSON.

```
234A — Interest for late filing:
  Applicable if: return filed after due date (July 31 for non-audit; Oct 31 for audit)
  Rate: 1% per month or part of month
  Amount: (Tax payable - TDS - Advance tax) × 1% × months_delay
  Minimum: if amount < 0, treat as 0

234B — Interest for short payment of advance tax:
  Applicable if: advance tax paid < 90% of assessed tax
  Rate: 1% per month from April 1 of AY to date of filing (or date of assessment)
  Amount: (Assessed tax - TDS - Advance tax) × 1% × months

234C — Interest for deferment of advance tax installments:
  Check each installment:
    June 15: should have paid 15% of tax liability
    Sep 15:  should have paid 45% of tax liability
    Dec 15:  should have paid 75% of tax liability
    Mar 15:  should have paid 100%
  For each shortfall: 1% per month for 3 months (1 month for March installment)
  Exception: No 234C for 44AD/44ADA — they pay 100% by March 15

234F — Late filing fee:
  If return filed after due date:
    Income > Rs 5,00,000: Rs 5,000
    Income <= Rs 5,00,000: Rs 1,000
  Zero if income below basic exemption limit
```

---

## SECTION 3 — ITR-2 COMPLETE IMPLEMENTATION

### 3.1 New Class: `ITR2CBDTValidationService.java`

This service must implement all 63 Category A validation rules for ITR-2. It extends the base ITR-1 rules (reuse `ITR1CBDTValidationService` by calling it and adding ITR-2-specific rules). All rules that cause upload rejection are CAT-A.

#### 3.1.1 Schedule HP Rules (12 rules)

```java
// VR2-HP-001: Standard deduction = 30% of Net Annual Value exactly
assert(property.standardDeduction == Math.round(0.30 * property.netAnnualValue));

// VR2-HP-002: Co-owned property — share percentages sum to 100%
if (property.isCoOwned) {
    assert(property.assesseeSharePct + property.coOwnerSharePct == 100);
}

// VR2-HP-003: Assessee's annual value = own% × total annual value
if (property.isCoOwned) {
    assert(property.assesseeNAV == Math.round(property.totalNAV * property.assesseeSharePct / 100));
}

// VR2-HP-004: Cannot claim interest if assessee's share = 0%
if (property.assesseeSharePct == 0) {
    assert(property.interestOnBorrowedCapital == 0);
}

// VR2-HP-005: Municipal tax not allowed where GAV = 0
if (property.grossAnnualValue == 0) {
    assert(property.municipalTax == 0);
}

// VR2-HP-006: Old regime, Self-Occupied: interest <= Rs 2,00,000
if (regime == OLD && property.type == SELF_OCCUPIED) {
    assert(property.interestOnBorrowedCapital <= 200000);
}

// VR2-HP-007: New regime, Self-Occupied: interest = 0 (NO HP interest deduction in new regime for SOP)
if (regime == NEW && property.type == SELF_OCCUPIED) {
    assert(property.interestOnBorrowedCapital == 0);
}

// VR2-HP-008: Let-out / Deemed Let-out: Gross Rent > 0
if (property.type == LET_OUT || property.type == DEEMED_LET_OUT) {
    assert(property.grossRentReceived > 0);
}

// VR2-HP-009: HP income = NAV - 30% deduction - interest + arrear
int expectedHPIncome = property.netAnnualValue 
    - property.standardDeduction 
    - property.interestOnBorrowedCapital 
    + property.arrearUnrealisedRent;
assert(property.hpIncome == expectedHPIncome);

// VR2-HP-010: HP pass-through income = PTI schedule HP amount
if (property.hasPassThroughIncome) {
    assert(property.passThroughIncome == schedulePTI.hpAmount);
}

// VR2-HP-011: More than 2 SOP → 3rd property must be DLO
long sopCount = properties.stream().filter(p -> p.type == SELF_OCCUPIED).count();
if (sopCount > 2) {
    addError("VR2-HP-011", "Only 2 properties can be Self-Occupied. Mark additional properties as Deemed Let-Out.");
}

// VR2-HP-012: Co-owner PAN != assessee PAN
if (property.isCoOwned) {
    assert(!property.coOwnerPAN.equals(assesseePAN));
}
```

**HP Loss Aggregation Logic (REQUIRED in ITR2FormData processing):**
```java
int aggregateHPIncome = properties.stream().mapToInt(p -> p.hpIncome).sum();
int hpLossSetOffLimit = 200000; // Rs 2,00,000 cap per section 71(3A)
if (aggregateHPIncome < 0) {
    int setOffAllowed = Math.min(Math.abs(aggregateHPIncome), hpLossSetOffLimit);
    int carryForward = Math.abs(aggregateHPIncome) - setOffAllowed;
    // setOffAllowed goes to CYLA; carryForward goes to CFL Schedule
}
```

#### 3.1.2 Schedule 112A Grandfathering Rules (8 rules)

These fix the existing incomplete `LTCG112AGrandfatheringService`. **Rewrite** the service to be complete:

```java
// VR2-CG-112A-001: Col6 (Total Sale Value) = Col4 (units) × Col5 (sale price per unit)
assert(row.totalSaleValue == row.units * row.salePricePerUnit);

// VR2-CG-112A-002: Col7 (cost without indexation) = max(Col8 actual cost, Col9 FMV Jan31 2018)
// BUT: Col9 is only applicable if acquired BEFORE Feb 1, 2018
if (row.dateOfAcquisition.isBefore(LocalDate.of(2018, 2, 1))) {
    row.costWithoutIndexation = Math.max(row.actualCostOfAcquisition, row.fmvOn31Jan2018);
} else {
    row.costWithoutIndexation = row.actualCostOfAcquisition;
    row.fmvOn31Jan2018 = 0; // not applicable
}

// VR2-CG-112A-003: FMV Jan31 2018 cannot exceed sale value
// Col9 <= min(Col6 sale value, Col11 total FMV)
if (row.dateOfAcquisition.isBefore(LocalDate.of(2018, 2, 1))) {
    assert(row.fmvOn31Jan2018 <= Math.min(row.totalSaleValue, row.totalFMV55_2_ac));
}

// VR2-CG-112A-004: Col11 Total FMV = Col4 (units) × Col10 (FMV per share on Jan31 2018)
if (row.dateOfAcquisition.isBefore(LocalDate.of(2018, 2, 1))) {
    assert(row.totalFMV55_2_ac == row.units * row.fmvPerShareOn31Jan2018);
}

// VR2-CG-112A-005: Col13 Total deductions = Col7 + Col12 (transfer expenses)
assert(row.totalDeductions == row.costWithoutIndexation + row.expenditureOnTransfer);

// VR2-CG-112A-006: Col14 Balance LTCG = Col6 - Col13
assert(row.balanceLTCG == row.totalSaleValue - row.totalDeductions);

// VR2-CG-112A-007: Schedule 112A totals = sum of individual rows
assert(schedule112A.totalLTCG112A == rows.stream().mapToInt(r -> r.balanceLTCG).sum());

// VR2-CG-112A-008: LTCG u/s 112A in Schedule CG (B4a) = Total of Col14 of Schedule 112A
assert(scheduleCG.ltcg112A_B4a == schedule112A.totalLTCG112A);

// EXEMPTION COMPUTATION (fix incomplete LTCG112AGrandfatheringService):
// Split rate for AY 2025-26:
// Pre-July 23, 2024 sales: 10% rate; exemption threshold Rs 1,00,000
// Post-July 23, 2024 sales: 12.5% rate; exemption threshold Rs 1,25,000
// Aggregate the two pools separately. 
// For each pool: taxable LTCG = max(0, pool_LTCG - pool_exemption_threshold)
```

#### 3.1.3 Schedule CG — Capital Gains Arithmetic Rules (15 rules)

```java
// VR2-CG-001: Total STCG = sum of individual STCG components
assert(scheduleCG.totalSTCG == stcg111A_pre + stcg111A_post + stcgSlab + stcgOther);

// VR2-CG-002: Total LTCG = sum of individual LTCG components
assert(scheduleCG.totalLTCG == ltcg112A_pre + ltcg112A_post + ltcgProperty + ltcgOther);

// VR2-CG-003: Income under CG = Total STCG + Total LTCG
assert(scheduleCG.totalCGIncome == scheduleCG.totalSTCG + scheduleCG.totalLTCG);

// VR2-CG-004: If Full Value of Consideration = 0, transfer expenses = 0
if (transaction.fullValueOfConsideration == 0) {
    assert(transaction.expenditureOnTransfer == 0);
}

// VR2-CG-005: STCG A1c (Balance) = Full value - Deductions
assert(stcg.balance == stcg.fullValue - stcg.deductions);

// VR2-CG-006: STCG A1e = A1c - Exemption
assert(stcg.netTaxable == stcg.balance - stcg.exemption);

// VR2-CG-007: For property — Section 50C stamp duty value rule
// If SDV > 110% of actual sale price, use SDV as full consideration
if (property.stampDutyValue > property.actualSalePrice * 1.10) {
    property.fullValueOfConsideration = property.stampDutyValue;
} else {
    property.fullValueOfConsideration = property.actualSalePrice;
}

// VR2-CG-008: Indexed cost = max(actual cost, indexed cost)
// Actual = original cost; indexed = original × (CII_sale / CII_acquisition)
int cii_sale = getCII(saleYear); // from CII table — 2024-25 = 363
int cii_acquisition = getCII(acquisitionYear);
int indexedCost = (int) Math.round((double) actualCost * cii_sale / cii_acquisition);
// NOTE: If acquired before April 1, 2001, use FMV on April 1, 2001 as cost base

// VR2-CG-009: Split rate computation (AY 2025-26)
// Sales before July 23, 2024: STCG at 15%, LTCG at 10%
// Sales on/after July 23, 2024: STCG at 20%, LTCG at 12.5%
// Separate transactions into two pools by date

// VR2-CG-010: Property LTCG — taxpayer chooses indexation or not
// Old (pre-Jul 23) assets: 20% with indexation OR 12.5% without (taxpayer choice)
// Post-Jul 23: only 12.5% without indexation
// ERP must present BOTH options and recommend lower tax

// VR2-CG-011: LTCG B4c = B4a (112A total) - B4b (exemption threshold)
assert(scheduleCG.ltcgAboveThreshold == Math.max(0, scheduleCG.ltcg112A_B4a - 125000));

// VR2-CG-012: Schedule CYLA and BFLA cross-references internally consistent
// (validated in CYLA/BFLA rules below)

// VR2-CG-013: CGAS deposit — if exemption under 54/54F/54EC claimed but
// investment not made by filing date, Capital Gains Account Scheme deposit must be present
if (exemption.sec54 > 0 || exemption.sec54F > 0 || exemption.sec54EC > 0) {
    if (!newPropertyPurchased && !bondsPurchased) {
        assert(cgasDepositConfirmed);
    }
}

// VR2-CG-014: Section 50C — already handled in VR2-CG-007

// VR2-CG-015: Resident cannot claim 112(1)(c) unless 115H option exercised
// (NRI-related rule; check residency status)
```

**Cost Inflation Index Table — Embed in service:**
```java
private static final Map<Integer, Integer> CII_TABLE = Map.ofEntries(
    Map.entry(2001, 100), Map.entry(2002, 105), Map.entry(2003, 109),
    Map.entry(2004, 113), Map.entry(2005, 117), Map.entry(2006, 122),
    Map.entry(2007, 129), Map.entry(2008, 137), Map.entry(2009, 148),
    Map.entry(2010, 167), Map.entry(2011, 184), Map.entry(2012, 200),
    Map.entry(2013, 220), Map.entry(2014, 240), Map.entry(2015, 254),
    Map.entry(2016, 264), Map.entry(2017, 272), Map.entry(2018, 280),
    Map.entry(2019, 289), Map.entry(2020, 301), Map.entry(2021, 317),
    Map.entry(2022, 331), Map.entry(2023, 348), Map.entry(2024, 363)
    // AY 2026-27: FY 2025-26 CII to be fetched from CBDT notification; 
    // use configurable parameter; default 375 as placeholder — FLAG FOR UPDATE
);
// Key = FY start year (e.g. 2024 for FY 2024-25)
// For assets acquired before April 1, 2001: cost = max(actual_cost, FMV_as_on_1April2001)
// For FMV 2001, ERP must accept manual input from taxpayer/CA
```

#### 3.1.4 Schedule CYLA — Current Year Loss Adjustment (5 rules)

```java
// New Class: LossSetOffEngine.java

// VR2-CYLA-001: HP loss set-off against other heads <= Rs 2,00,000
int hpLossAvailable = Math.abs(aggregateHPIncome); // only if negative
int hpLossSetOff = Math.min(hpLossAvailable, 200000);
// Distribute this Rs 2L set-off against: salary income first, then OS, then CG
// HP loss CANNOT be set off against salary from speculative business
// HP loss CANNOT be set off against special rate income (111A, 112A, VDA)

// VR2-CYLA-002: Speculative business loss set off ONLY against speculative income
// Non-speculative business loss: can set off against salary, HP, OS, CG
// NOT against speculative income (one-directional restriction)

// VR2-CYLA-003: LTCG loss set off ONLY against LTCG income
// STCG loss: can set off against STCG and LTCG
// LTCG loss: CANNOT set off against STCG

// VR2-CYLA-004: Income after set-off >= 0 for each head
// (cannot create negative income in a head through set-off)
for (IncomeHead head : incomeHeads) {
    assert(head.incomeAfterSetOff >= 0);
}

// VR2-CYLA-005: STCG at slab rate in CYLA = Sl.11vi of item E of Schedule CG
assert(cyla.stcgAtSlab == scheduleCG.stcgAtSlabRateItem11vi);

// IMPLEMENTATION — Full CYLA Matrix:
//
// Losses from each head and what they can offset:
// HP Loss (<=2L):      Salary ✓  Business_NS ✓  CG_STCG ✓  CG_LTCG ✓  OS ✓  (NOT speculative, NOT special rate)
// Business_NS Loss:    HP ✓       Salary ✓       CG ✓       OS ✓         (NOT speculative)
// Speculative Loss:    Speculative_income ✓ ONLY
// STCG Loss:           Other STCG ✓  LTCG ✓      (NOT salary, NOT HP, NOT business, NOT OS)
// LTCG Loss:           Other LTCG ✓ ONLY
// VDA Loss:            CANNOT set off against anything (115BBH)
```

#### 3.1.5 Schedule BFLA — Brought Forward Loss Adjustment (5 rules)

```java
// VR2-BFLA-001: BFLA Sl.2(xi) must match CFL Sl.6(x) from prior year filing
// This requires storing and retrieving prior year CFL schedule from database
// If prior year ITR not in system: accept manual input with CA verification flag

// VR2-BFLA-002: BFLA Sl.2(ii) = CFL Sl.3c(x) from prior year
assert(bfla.row2_col2 == priorYearCFL.row3c_col10);

// VR2-BFLA-003: BFLA Col3 = Col1 - Col2 (remaining after set-off)
for (BFLARow row : bflaRows) {
    assert(row.remaining == row.available - row.setOff);
}

// VR2-BFLA-004: BFLA Sl.1ix = CYLA Sl.4x
assert(bfla.row1_col9 == cyla.row4_col10);

// VR2-BFLA-005: BF HP loss can only be set off against HP income in subsequent years
// BF business loss: against business income (and CG for non-speculative)
// BF STCG/LTCG: only against CG of same type
// Carry forward: HP loss — 8 years; Business loss — 8 years; CG loss — 8 years;
//   Speculative — 4 years; Unabsorbed depreciation — UNLIMITED years

// SCHEDULE CFL (Carry Forward Losses) — generate automatically from CYLA balance:
ScheduleCFL cfl = new ScheduleCFL();
cfl.hpLossCarriedForward = cyla.hpLossUnabsorbed;
cfl.businessLossCarriedForward = cyla.businessLossUnabsorbed;
cfl.stcgLossCarriedForward = cyla.stcgLossUnabsorbed;
cfl.ltcgLossCarriedForward = cyla.ltcgLossUnabsorbed;
// Set year = current AY; used for matching in next year's BFLA
```

#### 3.1.6 Schedule VIA — Deduction Validations (9 rules)

```java
// VR2-VIA-001: New regime deductions — ONLY 80CCD(2), 80JJAA, 80CCH(2) allowed
if (regime == NEW) {
    assert(deductions.sec80C == 0);
    assert(deductions.sec80D == 0);
    assert(deductions.sec80E == 0);
    assert(deductions.sec80G == 0);
    assert(deductions.sec80TTA == 0);
    assert(deductions.sec80TTB == 0);
    // ... all other deductions = 0 except below
    // Allowed: 80CCD(2), 80JJAA, 80CCH(2)
}

// VR2-VIA-002: 80G deduction > 0 → Schedule 80G mandatory
if (deductions.sec80G > 0) assert(schedule80G != null && !schedule80G.entries.isEmpty());

// VR2-VIA-003: 80G deduction <= eligible amount in Schedule 80G
assert(deductions.sec80G <= schedule80G.totalEligibleDeduction);

// VR2-VIA-004: 80G cash donations > Rs 2,000 = Rs 0 deduction
for (Donation d : schedule80G.donations) {
    if (d.modeOfPayment == CASH && d.amount > 2000) {
        d.eligibleForDeduction = 0; // disallowed
        addWarning("ALERT-015", "Cash donation of Rs " + d.amount + " to " + d.doneeName + " disallowed");
    }
}

// VR2-VIA-005: Donee PAN != assessee PAN != verification PAN
for (Donation d : schedule80G.donations) {
    assert(!d.doneePAN.equals(assesseePAN));
}

// VR2-VIA-006: 80GGA eligible <= total donation
assert(deductions.sec80GGA <= schedule80GGA.totalDonation);

// VR2-VIA-007: AMT — if income > Rs 20,00,000 with profit-linked deductions
// AMT Rate = 18.5% on Adjusted Total Income
// ATI = Total Income + 10AA + 35AD + 80H-to-80RRB deductions
// If AMT > Regular Tax AND ATI > Rs 20L: AMT applies
// ERP must compute both and take the higher
int adjustedTotalIncome = totalIncome 
    + deductions.sec10AA + deductions.sec35AD 
    + deductions.profitLinkedDeductions; // 80-IC, 80-IE, 80JJAA etc.
if (adjustedTotalIncome > 2000000) {
    int amt = (int) Math.round(0.185 * adjustedTotalIncome);
    int amtWithCess = amt + (int) Math.round(0.04 * amt);
    if (amt > regularTax) {
        // AMT applies — output to Schedule AMT
    }
}

// VR2-VIA-008: HUF cannot claim 89A relief
if (assesseeType == HUF) assert(reliefUs89A == 0);

// VR2-VIA-009: 89A income <= income offered in salary 1d
assert(income89A <= salaryIncome.total17_1);
```

#### 3.1.7 Special Rate Income Schedule SI (9 rules)

```java
// VR2-SI-001: Tax on special rate income = sum(income × rate) per category
// Pre-July 23, 2024: STCG 111A at 15%, LTCG 112A at 10%
// Post-July 23, 2024: STCG 111A at 20%, LTCG 112A at 12.5%
// VDA 115BBH at 30%
// Lottery 115BB at 30%
// Unexplained income 115BBE at 60% + 25% surcharge on tax = effective 78% + cess

// VR2-SI-002: STCG 111A rate = 15% (pre-Jul23) / 20% (post-Jul23)
// VR2-SI-003: LTCG 112A rate = 10% (pre-Jul23) / 12.5% (post-Jul23)
// VR2-SI-004: VDA income rate = 30% exactly
// VR2-SI-005: Lottery income rate = 30% exactly
// VR2-SI-006: 87A rebate = 0 for special rate income
// VR2-SI-007: Surcharge on 111A and 112A income CAPPED at 15%
// VR2-SI-008: VDA income (115BBH) cannot be negative (loss not allowed)
if (vdaIncome < 0) vdaIncome = 0; // per 115BBH — no set-off

// VR2-SI-009: 115BBC anonymous donation not applicable to ITR-2
// (this rule just means: do not include 115BBC fields in ITR-2 JSON)
```

### 3.2 New Class: `ITR2JSONExportService.java`

Produces the complete ITR-2 JSON per ITD schema. Structure:

```
{
  "ITR": {
    "ITR2": {
      "CreationInfo": { /* same as ITR-1 — reuse ITDCommonDtos */ },
      "Form_ITR2": {
        "PersonalInfo": { /* same as ITR-1 */ },
        "FilingStatus": { /* same as ITR-1 */ },
        
        // Salary (same as ITR-1 salary section)
        "ScheduleS": { ... },
        
        // House Property — one entry per property
        "ScheduleHP": {
          "PropertyDetails": [ /* array of property objects */ ],
          "TotalHPIncome": Integer,
          "HPLossSetOff": Integer,    // max Rs 2,00,000
          "HPLossCarriedForward": Integer
        },
        
        // Capital Gains
        "ScheduleCG": {
          "ShortTermCapGainFor23": { /* pre-July23 at 15% */ },
          "ShortTermCapGainFor23Later": { /* post-July23 at 20% */ },
          "STCGOnOtherAssets": { /* at slab rate */ },
          "LTCGEquity_Pre112A": { /* pre-July23 at 10% */ },
          "LTCGEquity_Post112A": { /* post-July23 at 12.5% */ },
          "LTCG_Property_WithIndexation": { ... },
          "LTCG_Property_WithoutIndexation": { ... },
          "Schedule112A": [ /* grandfathering rows */ ],
          "TotalSTCG": Integer,
          "TotalLTCG": Integer
        },
        
        "ScheduleVDA": { /* see Section 4.2 of this document */ },
        
        "ScheduleCYLA": { /* loss set-off matrix */ },
        "ScheduleBFLA": { /* brought forward loss adjustment */ },
        "ScheduleCFL": { /* carry forward to next year */ },
        
        "ScheduleOS": { /* other sources with deductions u/s 57 */ },
        
        "ScheduleVIA": { /* all Chapter VI-A deductions */ },
        "Schedule80G": { /* donation-wise details */ },
        
        "ScheduleAL": { /* mandatory if income > Rs 50L */ },
        "ScheduleFA": { /* foreign assets */ },
        "ScheduleFSI": { /* foreign source income */ },
        "ScheduleTR": { /* tax relief — foreign tax credit */ },
        "ScheduleSI": { /* special rate income */ },
        "ScheduleEI": { /* exempt income */ },
        "ScheduleAMT": { /* alternate minimum tax */ },
        "ScheduleAMTC": { /* AMT credit */ },
        
        "TaxComputation": { /* same structure as ITR-1 */ },
        "TaxPaid": { /* TDS1, TDS2, TDS3, TCS, AdvanceTax — same as ITR-1 */ },
        "Verification": { /* same as ITR-1 */ },
        "BankAccountDetail": { /* same as ITR-1 */ }
      }
    }
  }
}
```

All amounts: integers. All dates: DD/MM/YYYY. SHA-256 digest in CreationInfo. Intermediary city <= 25 chars.

---

## SECTION 4 — VDA TRANSACTION SERVICE (COMPLETE IMPLEMENTATION)

Replace the existing placeholder in `VDATransactionService.java`.

### 4.1 Core Rules (Section 115BBH)

```java
public class VDATransactionService {

    public VDAComputationResult compute(List<VDATransaction> transactions) {
        int totalIncome = 0;
        int totalTDS194S = 0;
        List<VDATransactionDetail> details = new ArrayList<>();
        
        for (VDATransaction txn : transactions) {
            int gain = txn.considerationReceived - txn.costOfAcquisition;
            // NOTE: Only cost of acquisition is allowed as deduction — NO other deduction
            // If gain < 0 (loss): treat as 0 — VDA losses CANNOT be set off
            int taxableGain = Math.max(0, gain);
            totalIncome += taxableGain;
            totalTDS194S += txn.tdsDeductedUnder194S;
            
            details.add(VDATransactionDetail.builder()
                .dateOfAcquisition(txn.dateOfAcquisition)
                .dateOfTransfer(txn.dateOfTransfer)
                .headUnderWhichTaxable("CG") // always Capital Gains for transfers
                .costOfAcquisition(txn.costOfAcquisition)
                .considerationReceived(txn.considerationReceived)
                .incomeFromVDA(taxableGain) // 0 if loss
                .build());
        }
        
        // Tax on VDA = 30% flat on totalIncome
        // No basic exemption against VDA income
        // No Chapter VI-A deductions against VDA income
        // Surcharge at normal rates (NOT capped at 15% — cap is only for 111A/112A)
        int tax = (int) Math.round(0.30 * totalIncome);
        
        return VDAComputationResult.builder()
            .totalIncomeFromVDA(totalIncome)
            .taxOnVDA(tax)
            .tdsUnder194S(totalTDS194S)
            .netTDSToCredit(totalTDS194S) // claim full 194S TDS as credit
            .transactionDetails(details)
            .build();
    }
    
    // VDA from AIS parsing — match SFT transactions tagged as 194S
    public List<VDATransaction> parseFromAIS(AISData aisData) {
        return aisData.getTdsEntries().stream()
            .filter(e -> e.section.equals("194S"))
            .map(e -> VDATransaction.builder()
                .considerationReceived(e.transactionAmount)
                .tdsDeductedUnder194S(e.taxDeducted)
                .dateOfTransfer(e.transactionDate)
                // costOfAcquisition requires manual input from taxpayer
                .costOfAcquisition(0) // flag for taxpayer to fill
                .requiresManualCost(true)
                .build())
            .collect(Collectors.toList());
    }
}
```

### 4.2 Schedule VDA JSON Output

```json
{
  "ScheduleVDA": {
    "VDADetails": [
      {
        "DateOfAcquisition": "DD/MM/YYYY",
        "DateOfTransfer": "DD/MM/YYYY",
        "HeadUnderWhichIncomeTaxable": "CG",
        "CostOfAcquisition": Integer,
        "ConsiderationReceived": Integer,
        "IncomeFromVDA": Integer
      }
    ],
    "TotalIncomeFromVDA": Integer,
    "TDSUs194S": Integer
  }
}
```

---

## SECTION 5 — ITR-3 COMPLETE IMPLEMENTATION

### 5.1 New Class: `ITR3CBDTValidationService.java`

Implements all 23 Category A rules for ITR-3. Inherits from ITR-2 validation for common sections (HP, CG, VDA, etc.) and adds business-specific rules.

#### 5.1.1 Schedule BP Rules (12 rules)

```java
// VR3-BP-001/002/003: Income reduced in A3b/c/d cannot exceed income in corresponding head
// When removing business income counted elsewhere (e.g., rental from business premises counted as HP),
// the reduction cannot exceed actual income offered in that head
assert(bp.incomeReducedA3b <= otherHeadIncome.relevantSection);

// VR3-BP-004: Schedule BP A6 arithmetic check
int bp_A6 = bp.netProfitFromPL 
    + bp.addAdmissibleDebits.total 
    - bp.lessAdmissibleCredits.total;
assert(scheduleBP.profitFromBusiness == bp_A6);

// VR3-BP-005: 80JJAA deduction conditions
// New employees must have emoluments <= Rs 25,000/month
// Business must be subject to tax audit (turnover > Rs 1 Cr for trading; Rs 50L for profession)
if (deductions.sec80JJAA > 0) {
    assert(employeeEmoluments.all(e -> e.monthlyEmoluments <= 25000));
    assert(isSubjectToTaxAudit);
}

// VR3-BP-006: (Covered in 005)

// VR3-BP-007: Section 40A(3) — Cash payments > Rs 10,000 per day per person must be added back
// ERP must track each cash payment entry in P&L
// Exception: payments in notified difficult areas; certain agricultural payments up to Rs 35,000 for transporters
for (Expense expense : businessExpenses) {
    if (expense.modeOfPayment == CASH && expense.amount > 10000 && !expense.isExempted40A3()) {
        bp.addAdmissibleDebits.disallowanceUs40A3 += expense.amount;
    }
}

// VR3-BP-008: Section 43B — Add back unpaid expenses as of filing date
// Taxes, PF/ESIC employer contributions, bonus, leave encashment, interest on loans
// Must be paid before ITR filing date to be deductible
LocalDate filingDate = LocalDate.now();
for (Section43BItem item : section43BItems) {
    if (item.paymentDate == null || item.paymentDate.isAfter(filingDate)) {
        bp.addAdmissibleDebits.disallowanceUs43B += item.amount;
    }
}

// VR3-BP-008h: MSME 43B(h) — Payments to MSME suppliers outstanding > 45 days
for (MSMEPayment p : msmePayments) {
    long daysPending = ChronoUnit.DAYS.between(p.supplyDate, p.paymentDate != null ? p.paymentDate : LocalDate.of(financialYear + 1, 3, 31));
    if (daysPending > 45) {
        bp.addAdmissibleDebits.disallowanceMSME43B += p.amount;
        addAlert("ALERT-019", "MSME payment of Rs " + p.amount + " to " + p.supplierName + " outstanding > 45 days — disallowed");
    }
}

// VR3-BP-009: Employee PF contributions — must be deposited by 139(1) due date
// If deposited late: DISALLOWED as deduction (Budget 2021 amendment)
for (EPFContribution contrib : employeePFContributions) {
    if (contrib.depositDate.isAfter(itrDueDate)) {
        bp.addAdmissibleDebits.disallowanceEmployeePF += contrib.amount;
    }
}

// VR3-BP-010: Partner remuneration — Section 40(b) limits
// Limit = max(Rs 1,50,000, (90% × first Rs 3L of book profit) + (60% × balance book profit))
int bookProfit = scheduleBP.bookProfit;
int limitPart1 = (int) Math.round(Math.min(bookProfit, 300000) * 0.90);
int limitPart2 = (int) Math.round(Math.max(0, bookProfit - 300000) * 0.60);
int partnerRemunerationLimit = Math.max(150000, limitPart1 + limitPart2);
if (partnerRemuneration > partnerRemunerationLimit) {
    addError("VR3-BP-010", "Partner remuneration Rs " + partnerRemuneration + " exceeds Section 40(b) limit Rs " + partnerRemunerationLimit);
}

// VR3-BP-011: F&O Turnover = absolute profit + absolute loss (NOT gross contract value)
// This is a critical determination for audit applicability
int fandoTurnover = fandoTrades.stream()
    .mapToInt(t -> Math.abs(t.netProfit))
    .sum();
// If fandoTurnover > Rs 10 Cr (or Rs 1 Cr if cash > 5%): tax audit required (44AB)

// VR3-BP-012: Speculative business income kept separate
// Speculative = intraday equity trading
// Non-speculative = F&O, regular business
assert(scheduleBP.speculativeBusinessIncome != null || scheduleBP.speculativeBusinessIncome == 0);
// They must be in separate fields, not combined
```

#### 5.1.2 Schedule DPM Rules (7 rules)

```java
// VR3-DPM-001: Total building depreciation = sum of sub-items
assert(dpm.buildings.totalBuildingDepreciation == 
    dpm.buildings.depreciation5Pct + dpm.buildings.depreciation10Pct + dpm.buildings.depreciation40Pct);

// VR3-DPM-002: Total depreciation = sum of all blocks
assert(dpm.totalDepreciation == 
    dpm.buildings.total + dpm.plantMachinery.total + dpm.furniture.total + dpm.intangibles.total);

// VR3-DPM-003: DPM fields cross-referenced with Schedule DEP totals
assert(dpm.totalDepreciation == scheduleDEP.totalITActDepreciation);

// VR3-DPM-004: STCG on depreciable assets in CG A6e = Schedule DCG Sl.6
assert(scheduleCG.stcgDepreciableAssets == scheduleDCG.totalDeemedCapitalGains);

// VR3-DPM-005: Goodwill: 0% depreciation from AY 2021-22
// If goodwill exists in any block, depreciation on that block = 0
if (dpm.hasGoodwill) {
    assert(dpm.goodwillBlock.depreciation == 0);
}

// VR3-DPM-006: Additional depreciation (32(1)(iia)):
// Only on new P&M (not ships, aircraft, office equipment, vehicles)
// Rate: 20% in year of installation; 10% if installed for < 180 days
if (asset.isNewPlantMachinery && asset.isEligibleForAdditionalDep) {
    int additionalDep;
    if (asset.daysInUse >= 180) {
        additionalDep = (int) Math.round(0.20 * asset.cost);
    } else {
        additionalDep = (int) Math.round(0.10 * asset.cost); // 10% in year 1, 10% next year
        asset.additionalDepCarriedForward = (int) Math.round(0.10 * asset.cost); // remaining for next year
    }
}

// VR3-DPM-007: (Covered in DPM-006)
```

**Depreciation Rate Table — Embed in DepreciationEngine.java:**
```java
private static final Map<AssetType, Integer> DEPRECIATION_RATES = Map.of(
    BUILDING_RCC_RESIDENTIAL, 5,
    BUILDING_RCC_NON_RESIDENTIAL, 10,
    BUILDING_TEMPORARY, 40,
    FURNITURE, 10,
    PLANT_MACHINERY_GENERAL, 15,
    COMPUTERS_PERIPHERALS_SOFTWARE, 40,
    MOTOR_CARS, 15,
    MOTOR_BUSES_LORRIES_HIRE, 30,
    SHIPS, 20,
    INTANGIBLES, 25
    // GOODWILL: 0% from AY 2021-22
);

// Block of Assets Method:
int computeBlockDepreciation(DepreciationBlock block) {
    int openingWDV = block.openingWDV;
    int additions = block.additionsInYear;
    int disposals = block.saleProceeds;
    int closingWDV = openingWDV + additions - disposals;
    
    if (closingWDV <= 0) {
        // Entire block exhausted — Short-Term Capital Gain = |closingWDV|
        block.stcgOnBlock = Math.abs(closingWDV);
        return 0; // no depreciation
    }
    
    int depreciation = (int) Math.round(closingWDV * DEPRECIATION_RATES.get(block.assetType) / 100.0);
    
    // Half-rate rule: assets acquired in second half of year (after Oct 1) get 50% depreciation
    int halfYearAdditions = block.additionsAfterOct1;
    int halfYearDep = (int) Math.round(halfYearAdditions * DEPRECIATION_RATES.get(block.assetType) / 200.0);
    int fullYearAdditions = additions - halfYearAdditions;
    int fullYearDep = (int) Math.round((openingWDV + fullYearAdditions - disposals) * DEPRECIATION_RATES.get(block.assetType) / 100.0);
    
    return fullYearDep + halfYearDep;
}
```

#### 5.1.3 Schedule DCG — Deemed Capital Gains (4 rules)

```java
// VR3-DCG-001: DCG Sl.1e = sum of (1a+1b+1c+1d)
assert(dcg.row1_total == dcg.row1a + dcg.row1b + dcg.row1c + dcg.row1d);

// VR3-DCG-002: DCG Sl.2d = sum of (2a+2b+2c)
assert(dcg.row2d == dcg.row2a + dcg.row2b + dcg.row2c);

// VR3-DCG-003: DCG Sl.1a = DPM opening WDV reference
assert(dcg.row1a == dpm.correspondingBlock.openingWDV);

// VR3-DCG-004: DCG cross-references to DPM must be consistent
assert(dcg.row1a == dpm.buildings.openingWDV);
assert(dcg.row1b == dpm.plantMachinery.openingWDV);
// (validate all cross-reference pairs)
```

### 5.2 New Class: `ITR3JSONExportService.java`

Builds on ITR-2 JSON structure and adds:
- `ScheduleBP` — Business/Profession income with all add-backs and deductions
- `ScheduleDPM` — Depreciation block-wise
- `ScheduleDCG` — Deemed capital gains on depreciable assets
- `ScheduleGST` — GST reconciliation (reuse existing `GSTReconciliationService`)
- All ITR-2 schedules are inherited (HP, CG, VDA, CYLA, BFLA, CFL, etc.)
- `BalanceSheet` — Complete financial statements (for non-audit cases: abridged)
- `ProfitAndLoss` — Trading/Manufacturing/P&L account

---

## SECTION 6 — ITR-4 COMPLETE IMPLEMENTATION (PRESUMPTIVE INCOME)

### 6.1 New Class: `ITR4CBDTValidationService.java`

Implements 20 Category A rules specific to ITR-4 (Sugam).

```java
// VR4-001: Total income (excluding LTCG 112A) <= Rs 50,00,000 for ITR-4
int totalIncomeExcluding112A = totalIncome - ltcg112A;
if (totalIncomeExcluding112A > 5000000) {
    addError("VR4-001", "Total income exceeds Rs 50L — must file ITR-3");
}

// VR4-002: No capital gains other than LTCG 112A (and that <= Rs 1,25,000)
if (hasSTCG || hasLTCGOtherThan112A) {
    addError("VR4-002", "Capital gains other than LTCG 112A not allowed in ITR-4 — file ITR-3");
}

// VR4-003: LTCG 112A > Rs 1,25,000 → must file ITR-2/3
if (ltcg112A > 125000) {
    addError("VR4-003", "LTCG u/s 112A exceeds Rs 1,25,000 — must file ITR-2 or ITR-3");
}

// VR4-004: 44AD turnover limits
// If cash receipts <= 5% of total: limit = Rs 3 Crore
// Otherwise: limit = Rs 2 Crore
double cashPct = (double) turnoverCash / totalTurnover;
int turnoverLimit = (cashPct <= 0.05) ? 30000000 : 20000000;
if (sec44AD.grossTurnover > turnoverLimit) {
    addError("VR4-004", "44AD turnover exceeds limit — must maintain books and file ITR-3");
}

// VR4-005: 44ADA receipts limits
// Cash <= 5%: limit Rs 75 Lakh; otherwise: Rs 50 Lakh
int sec44ADALimit = (cashPct <= 0.05) ? 7500000 : 5000000;
if (sec44ADA.grossReceipts > sec44ADALimit) {
    addError("VR4-005", "44ADA receipts exceed limit — must file ITR-3");
}

// VR4-006: 44AE — max 10 goods vehicles at any point during the year
if (sec44AE.vehicles.stream().anyMatch(v -> v.vehiclesAtAnyTime > 10)) {
    addError("VR4-006", "Cannot own > 10 goods vehicles for 44AE — file ITR-3");
}

// VR4-007: 44AD presumptive income >= 8% of cash turnover AND >= 6% of digital turnover
int required8pct = (int) Math.round(0.08 * sec44AD.turnoverCash);
int required6pct = (int) Math.round(0.06 * sec44AD.turnoverDigital);
int minimumRequired = required8pct + required6pct;
if (sec44AD.totalPresumptiveIncome < minimumRequired) {
    addError("VR4-007", "Declared profit below statutory minimum. At 8% cash and 6% digital = Rs " + minimumRequired);
}

// VR4-008: 44ADA presumptive income >= 50% of gross receipts
int required50pct = (int) Math.round(0.50 * sec44ADA.grossReceipts);
if (sec44ADA.presumptiveIncome < required50pct) {
    addError("VR4-008", "44ADA declared profit below 50% of receipts. Minimum = Rs " + required50pct);
}

// VR4-009: 44AE income computation per vehicle
for (Vehicle v : sec44AE.vehicles) {
    int expectedIncome;
    if (v.isHeavyVehicle) {
        expectedIncome = (int) v.gvwInTonnes * 1000 * v.monthsOwned;
    } else {
        expectedIncome = 7500 * v.monthsOwned;
    }
    if (v.declaredIncome < expectedIncome) {
        addWarning("VR4-009", "Vehicle " + v.registrationNumber + ": declared Rs " + v.declaredIncome + ", statutory minimum Rs " + expectedIncome);
    }
}

// VR4-010: LLP cannot file ITR-4
if (assesseeType == LLP) {
    addError("VR4-010", "LLP must file ITR-5, not ITR-4");
}

// VR4-011: 44AD opt-out — lock-in applies
if (priorYear44ADClaimed && !current44ADClaimed) {
    addAlert("ALERT-011", "44AD OPT-OUT: You claimed 44AD in prior year. Opting out requires books + audit for next 5 years.");
    addError("VR4-011", "Must maintain books and consider tax audit due to 44AD opt-out");
}

// VR4-012: Advance tax — 100% by March 15; 234C for single installment only
// (different from regular taxpayer who has 4 installments)

// VR4-013: Only one house property in ITR-4
if (houseProperties.size() > 1) {
    addError("VR4-013", "Multiple house properties detected — must file ITR-3");
}

// VR4-014: Balance sheet fields MANDATORY
assert(balanceSheet.sundryDebtors != null);
assert(balanceSheet.sundryCreditors != null);
assert(balanceSheet.stockInTrade != null);
assert(balanceSheet.cashBalance != null);

// VR4-015: CreationInfo intermediary city <= 25 chars (already implemented in ITR-1 — reuse check)

// VR4-016: GST turnover reconciliation mandatory if GSTIN declared
if (gstin != null && !gstin.isEmpty()) {
    assert(scheduleGST != null && !scheduleGST.gstinList.isEmpty());
}

// VR4-017: Prior year 44AD opt-out — must show audit status
if (priorYear44ADClaimed && !current44ADClaimed) {
    assert(auditStatus != null); // must declare if under audit or not
}

// VR4-018: Cannot carry forward business losses in ITR-4
if (businessLoss > 0) {
    addError("VR4-018", "Business losses cannot be carried forward in ITR-4. Consider filing ITR-3.");
}

// VR4-019: New regime — only 80CCD(2), 80CCH, standard deduction allowed
if (regime == NEW) {
    assert(deductions.sec80C == 0 && deductions.sec80D == 0 && deductions.sec80G == 0);
}

// VR4-020: 44AD and 44ADA cannot both be claimed for same business
// They CAN be claimed for different businesses of the same person
List<String> busTypes = new ArrayList<>();
if (sec44AD != null && sec44AD.grossTurnover > 0) busTypes.add("44AD");
if (sec44ADA != null && sec44ADA.grossReceipts > 0) busTypes.add("44ADA");
// If same GSTIN or same business code: error
// If different businesses: allow
```

### 6.2 New Class: `ITR4JSONExportService.java`

```
{
  "ITR": {
    "ITR4": {
      "CreationInfo": { /* same as ITR-1 */ },
      "Form_ITR4": {
        "PersonalInfo": { /* same */ },
        "FilingStatus": { /* same */ },
        
        "BusinessProfessionIncome": {
          "NatureOfBusiness": { /* business/profession code from ITD table */ },
          "Sec44AD": {
            "GrossTurnoverReceipts": Integer,
            "TurnoverCash": Integer,
            "TurnoverDigital": Integer,
            "GrossIncomeCash_8Pct": Integer,     // 8% of TurnoverCash
            "GrossIncomeDigital_6Pct": Integer,  // 6% of TurnoverDigital
            "TotalPresumptiveIncome44AD": Integer,
            "BusinessCode": String
          },
          "Sec44ADA": {
            "GrossProfessionalReceipts": Integer,
            "ReceiptsCash": Integer,
            "ReceiptsDigital": Integer,
            "PresumptiveIncome50Pct": Integer,
            "ProfessionCode": String
          },
          "Sec44AE": {
            "VehicleDetails": [ { /* per vehicle fields */ } ],
            "TotalPresumptiveIncome44AE": Integer
          },
          "TotalPresumptiveBusinessIncome": Integer
        },
        
        "IncomeDeductions": { /* salary + HP (max 1) + OS */ },
        
        "ScheduleAL": { /* if income > Rs 50L */ },
        
        "SimplifiedBalanceSheet": {
          "GrossReceipts": Integer,
          "NetProfit": Integer,
          "TotalSundryDebtors": Integer,
          "TotalSundryCreditors": Integer,
          "TotalStockInTrade": Integer,
          "CashBalance": Integer,
          "OpeningCapital": Integer,
          "Drawings": Integer,
          "AdditionsToCapital": Integer,
          "ClosingCapital": Integer,
          "TotalSecuredLoans": Integer,
          "TotalUnsecuredLoans": Integer,
          "TotalFixedAssets_ClosingWDV": Integer
        },
        
        "ScheduleGST": { /* if GSTIN present */ },
        
        "TaxComputation": { /* same as ITR-1 */ },
        "TaxPaid": { /* TDS1, TDS2, TCS, AdvanceTax */ },
        "Verification": { /* same as ITR-1 */ },
        "BankAccountDetail": { /* same as ITR-1 */ }
      }
    }
  }
}
```

---

## SECTION 7 — SFT DATA PARSING (REAL IMPLEMENTATION)

Replace all placeholder methods in `SFTProcessingService.java`.

### 7.1 AIS JSON Structure to Parse

The AIS JSON downloaded from the IT portal has this structure for SFT data:
```json
{
  "aisData": {
    "sftTransactions": [
      {
        "sftCode": "SFT-001",        // identifies transaction type
        "reportingEntity": "String",
        "transactionDate": "DD/MM/YYYY",
        "transactionAmount": Integer,
        "pan": "String",
        "remarks": "String"
      }
    ]
  }
}
```

### 7.2 SFT Processing Rules

```java
public class SFTProcessingService {

    public SFTSummary processSFTData(AISData aisData) {
        Map<String, List<SFTTransaction>> byCode = aisData.getSftTransactions()
            .stream()
            .collect(Collectors.groupingBy(t -> t.getSftCode()));
        
        SFTSummary summary = new SFTSummary();
        
        // SFT-001: Savings Account Deposits — alert if > Rs 10L in a year
        List<SFTTransaction> sft001 = byCode.getOrDefault("SFT-001", List.of());
        int totalSavingsDeposit = sft001.stream().mapToInt(t -> t.transactionAmount).sum();
        if (totalSavingsDeposit > 1000000) {
            summary.addFlag("SFT-001", "Savings deposits Rs " + totalSavingsDeposit + " — verify source and ensure declared");
        }

        // SFT-002: Fixed Deposits > Rs 10L — must reconcile interest with AIS
        // SFT-003: Credit Card Payments > Rs 1L (cash) or Rs 10L (any mode)
        // SFT-004: Prepaid Instruments > Rs 10L
        // SFT-005: Mutual Fund purchases > Rs 10L
        // SFT-006: Share transactions > Rs 10L — reconcile with Schedule CG
        // SFT-007: Buyback transactions — report as CG
        // SFT-010: Mutual Fund dividends — reconcile with OS
        // SFT-011: Share dividends — reconcile with OS
        // SFT-012: Property purchase > Rs 30L — reconcile with Schedule CG cost base
        // SFT-013: Property sale > Rs 30L — MANDATORY CG disclosure
        // SFT-014: Foreign remittance under LRS — Schedule FA disclosure
        // SFT-015: Cash deposits in FY > Rs 10L aggregate — alert scrutiny risk
        // SFT-016: Cash withdrawals > Rs 10L — alert
        // SFT-017: Foreign assets — Schedule FA mandatory
        
        // Auto-populate Schedule AL from SFT:
        // SFT-012 property purchases → ImmovablePropertyDetails
        // SFT-005/006 investments → FinancialAssets.SharesDebentures
        // SFT-002 FD balances → FinancialAssets.BankDeposits
        
        // Reconcile against declared income:
        // SFT-010 dividends vs declared dividend income in OS
        // SFT-011 dividends vs declared
        // SFT-013 property sales vs Schedule CG transactions
        // If SFT shows income not declared: ALERT-005
        
        return summary;
    }
}
```

### 7.3 Auto-populate Schedule AL from SFT

```java
public ScheduleAL autoPopulateFromSFT(SFTSummary sft, int totalIncome) {
    if (totalIncome <= 5000000) return null; // Schedule AL mandatory only if income > Rs 50L
    
    ScheduleAL al = new ScheduleAL();
    
    // Properties from SFT-012/013
    sft.getPropertyPurchases().forEach(p -> 
        al.immovableProperties.add(new ImmovableProperty(p.address, p.purchaseAmount, p.purchaseYear))
    );
    
    // Shares from SFT-005/006
    al.financialAssets.sharesDebentures = sft.getNetInvestmentInMFAndShares();
    
    // FD balances from SFT-002
    al.financialAssets.bankDeposits = sft.getTotalFDBalance();
    
    // Flag all items as "pre-populated from AIS — please verify" 
    al.requiresVerification = true;
    
    return al;
}
```

---

## SECTION 8 — HRA METRO AUTO-DETECTION

Extend `HRAComputationService.java` to auto-detect metro status from the taxpayer's address in PAN records.

```java
private static final Set<String> METRO_CITIES = Set.of(
    "MUMBAI", "DELHI", "KOLKATA", "CHENNAI",
    // Also treated as metro for HRA:
    "NEW DELHI", "BOMBAY"
    // NOTE: Bengaluru, Hyderabad, Pune are NOT metro for HRA u/s 10(13A)
    // Only Mumbai, Delhi, Kolkata, Chennai = 50% of salary; all others = 40%
);

public boolean isMetroCity(String city) {
    if (city == null) return false;
    return METRO_CITIES.contains(city.toUpperCase().trim());
}

public HRAExemption compute(HRAInput input) {
    boolean isMetro = isMetroCity(input.cityOfResidence);
    double basicDASalary = input.basicSalary + input.dearessAllowance;
    
    int component1 = input.hraReceived;
    int component2 = (int) Math.round(basicDASalary * (isMetro ? 0.50 : 0.40));
    int component3 = (int) Math.round(input.rentPaid - (0.10 * basicDASalary));
    
    // If rent paid <= 10% of salary: component3 = 0 (no HRA exemption from this component)
    component3 = Math.max(0, component3);
    
    int exemption = Math.min(Math.min(component1, component2), component3);
    
    return HRAExemption.builder()
        .exemptAmount(exemption)
        .taxableHRA(input.hraReceived - exemption)
        .isMetroCity(isMetro)
        .component1_hraReceived(component1)
        .component2_percentOfSalary(component2)
        .component3_rentMinus10Pct(component3)
        .build();
}
```

---

## SECTION 9 — AUDIT TRAIL PERSISTENCE

Extend `AuditTrailService.java` from in-memory to database-backed storage.

### 9.1 Database Schema

```sql
CREATE TABLE audit_trail (
    id BIGSERIAL PRIMARY KEY,
    taxpayer_pan VARCHAR(10) NOT NULL,
    assessment_year VARCHAR(7) NOT NULL,  -- e.g. "2025-26"
    event_type VARCHAR(50) NOT NULL,      -- COMPUTATION, VALIDATION, DATA_IMPORT, FORM_GENERATION
    event_description TEXT NOT NULL,
    data_source VARCHAR(100),             -- FORM_16, AIS, 26AS, MANUAL_ENTRY, SYSTEM
    field_name VARCHAR(200),
    old_value TEXT,
    new_value TEXT,
    validation_rule_id VARCHAR(30),       -- e.g. "VR1-001"
    validation_result VARCHAR(10),        -- PASS, FAIL, WARNING
    created_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(100)               -- user or system
);

CREATE INDEX idx_audit_pan_ay ON audit_trail(taxpayer_pan, assessment_year);
```

### 9.2 Service Methods

```java
// Replace existing in-memory implementation:
@Transactional
public void logComputation(String pan, String ay, String description, String dataSource, String field, String oldVal, String newVal) {
    auditTrailRepository.save(new AuditTrailEntry(pan, ay, "COMPUTATION", description, dataSource, field, oldVal, newVal));
}

@Transactional
public void logValidation(String pan, String ay, String ruleId, String field, ValidationResult result) {
    auditTrailRepository.save(new AuditTrailEntry(pan, ay, "VALIDATION", result.getMessage(), "SYSTEM", field, null, null, ruleId, result.getStatus().name()));
}

public List<AuditTrailEntry> getAuditTrail(String pan, String ay) {
    return auditTrailRepository.findByTaxpayerPanAndAssessmentYear(pan, ay);
}
```

---

## SECTION 10 — FORM 16 MULTI-EMPLOYER CROSS-VALIDATION

Extend `MultiEmployerConsolidationService.java` with the following cross-validation logic:

```java
public MultiEmployerValidationResult validate(List<Form16Data> form16List) {
    MultiEmployerValidationResult result = new MultiEmployerValidationResult();
    
    // VAL-ME-001: Each Form 16 must have unique TAN
    Set<String> tans = new HashSet<>();
    for (Form16Data f16 : form16List) {
        if (!tans.add(f16.partA.tanOfEmployer)) {
            result.addError("Duplicate TAN " + f16.partA.tanOfEmployer + " — same employer Form 16 uploaded twice");
        }
    }
    
    // VAL-ME-002: PAN on all Form 16s must match taxpayer PAN
    for (Form16Data f16 : form16List) {
        if (!f16.partA.panOfEmployee.equals(taxpayerPAN)) {
            result.addError("Form 16 from " + f16.partA.employerName + " has PAN " + f16.partA.panOfEmployee + " — does not match taxpayer PAN");
        }
    }
    
    // VAL-ME-003: Employment periods should not overlap
    for (int i = 0; i < form16List.size(); i++) {
        for (int j = i + 1; j < form16List.size(); j++) {
            if (periodsOverlap(form16List.get(i).partA.periodOfEmployment, form16List.get(j).partA.periodOfEmployment)) {
                result.addWarning("Employment periods overlap between " + form16List.get(i).partA.employerName + " and " + form16List.get(j).partA.employerName + " — verify if concurrent employment");
            }
        }
    }
    
    // VAL-ME-004: Standard deduction claimed only once across all Form 16s
    int totalStandardDeductionClaimed = form16List.stream()
        .mapToInt(f -> f.partB.scheduleSalary.standardDeductionUs16ia)
        .sum();
    int maxAllowedStandardDeduction = 75000; // AY 2025-26 new regime; 50000 old regime
    if (totalStandardDeductionClaimed > maxAllowedStandardDeduction) {
        int excess = totalStandardDeductionClaimed - maxAllowedStandardDeduction;
        result.addError("STANDARD DEDUCTION OVERCLAIMED: Total Rs " + totalStandardDeductionClaimed + " across employers. Auto-correcting to Rs " + maxAllowedStandardDeduction + ". Increase income by Rs " + excess);
        result.setStandardDeductionCorrection(excess);
    }
    
    // VAL-ME-005: Professional tax max Rs 2,500 aggregate
    int totalProfTax = form16List.stream()
        .mapToInt(f -> f.partB.scheduleSalary.professionalTax)
        .sum();
    if (totalProfTax > 2500) {
        result.addWarning("Professional tax total Rs " + totalProfTax + " exceeds Rs 2,500 cap — capped to Rs 2,500");
        result.setProfessionalTaxCap(2500);
    }
    
    // VAL-ME-006: Aggregate TDS must match 26AS
    int aggregateTDSFromForm16 = form16List.stream()
        .mapToInt(f -> f.partA.aggregateTdsDeposited)
        .sum();
    // Cross-check against 26AS (done in AISReconciliationService)
    result.setAggregateTDSForReconciliation(aggregateTDSFromForm16);
    
    return result;
}
```

---

## SECTION 11 — NEW DATA TRANSFER OBJECTS (DTOs)

Create the following new DTOs that the above services require. Add to the existing DTO package:

### 11.1 `Itr2FormData.java` (extend existing)

Ensure the existing `Itr2FormData.java` has all these fields populated:
- `List<PropertyDetail> houseProperties` — multiple HP
- `ScheduleCG scheduleCG` — full capital gains with all components
- `ScheduleVDA scheduleVDA` — VDA transactions
- `ScheduleCYLA scheduleCYLA` — loss adjustment
- `ScheduleBFLA scheduleBFLA` — brought forward losses
- `ScheduleCFL scheduleCFL` — carry forward
- `ScheduleAL scheduleAL` — assets and liabilities
- `ScheduleFA scheduleFA` — foreign assets
- `Schedule80G schedule80G` — donation details
- `ScheduleAMT scheduleAMT` — AMT computation

### 11.2 `Itr3FormData.java` (extend existing)

Add to existing:
- `ScheduleBP scheduleBP` — business income with add-backs
- `ScheduleDPM scheduleDPM` — depreciation blocks
- `ScheduleDCG scheduleDCG` — deemed capital gains
- `BalanceSheet balanceSheet` — full or abridged
- `ProfitAndLoss profitAndLoss` — P&L account
- `List<MSMEPayment> msmePayments` — for 43B(h) tracking
- `List<Section43BItem> section43BItems` — for 43B disallowances
- `List<EPFContribution> employeeEPFContributions` — for 36(1)(va) check
- `boolean isSubjectToTaxAudit` — for 80JJAA
- `FandODetail fandoDetail` — F&O turnover computed correctly

### 11.3 `Itr4FormData.java` (extend existing)

Add to existing:
- `Sec44AD sec44AD` — 44AD presumptive
- `Sec44ADA sec44ADA` — 44ADA presumptive
- `Sec44AE sec44AE` — 44AE vehicle-wise
- `SimplifiedBalanceSheet simplifiedBalanceSheet` — mandatory fields
- `boolean priorYear44ADClaimed` — for opt-out check

### 11.4 `VDATransaction.java`

```java
public class VDATransaction {
    private LocalDate dateOfAcquisition;
    private LocalDate dateOfTransfer;
    private int costOfAcquisition;       // only allowed deduction
    private int considerationReceived;
    private int tdsDeductedUnder194S;
    private boolean requiresManualCost;  // flag if cost needs taxpayer input
    private String cryptoExchangeName;
    private String assetName;            // BTC, ETH, NFT description, etc.
}
```

### 11.5 `LossLedger.java`

```java
public class LossLedger {
    private String taxpayerPAN;
    private String assessmentYear;       // AY in which loss was incurred
    private LossType lossType;           // HP, BUSINESS_NS, SPECULATIVE, STCG, LTCG, UNABSORBED_DEP
    private int lossAmount;
    private int lossUtilized;
    private int lossRemaining;
    private int expiryAY;                // AY after which this loss lapses
    // HP loss: expires after 8 AYs; Business: 8 AYs; CG: 8 AYs; Spec: 4 AYs; Unabsorbed dep: unlimited
}
```

---

## SECTION 12 — INTEGRATION & WIRING

### 12.1 Controller Layer

Ensure API endpoints exist for:
```
POST /api/itr/validate/{formType}  — formType: ITR1, ITR2, ITR3, ITR4
POST /api/itr/export/{formType}    — generates ITD JSON
POST /api/itr/regime-compare       — old vs new regime comparison
POST /api/itr/form-select          — auto ITR form selection
GET  /api/itr/pre-submission-check/{pan}/{ay}
POST /api/vda/compute              — VDA tax computation
POST /api/sft/process              — SFT data parsing from AIS
GET  /api/audit-trail/{pan}/{ay}   — full audit trail
POST /api/capital-gains/compute    — CG with split rate + grandfathering
POST /api/losses/set-off           — CYLA + BFLA computation
```

### 12.2 Service Orchestration — Complete Pipeline

```java
// ITRFilingOrchestrationService.java — new class

public ITRFilingResult processAndFile(FilingRequest request) {
    // Step 1: Import data
    Form16ConsolidatedData form16 = multiEmployerConsolidationService.consolidate(request.form16List);
    AISData ais = aisReconciliationService.parse(request.aisJson);
    SFTSummary sft = sftProcessingService.processSFTData(ais);
    
    // Step 2: Reconcile
    ReconciliationReport recon = aisReconciliationService.reconcile(form16, ais);
    if (recon.hasUnreconciledItems()) {
        auditTrailService.logComputation(pan, ay, "AIS reconciliation gaps detected", "AIS", null, null, recon.getGapCount() + " gaps");
    }
    
    // Step 3: Select ITR form
    String itrForm = itrFormSelectionService.selectForm(request.taxpayerData);
    
    // Step 4: Compute taxes under both regimes
    TaxResult oldRegime = taxComputationEngine.computeTax(TaxInput.withOldRegime(request.taxpayerData));
    TaxResult newRegime = taxComputationEngine.computeTax(TaxInput.withNewRegime(request.taxpayerData));
    RegimeComparison comparison = TaxRegimeComparisonService.compare(oldRegime, newRegime);
    
    // Step 5: Validate
    ValidationResult validation = switch(itrForm) {
        case "ITR1" -> itr1ValidationService.validate(request.taxpayerData);
        case "ITR2" -> itr2ValidationService.validate(request.taxpayerData);
        case "ITR3" -> itr3ValidationService.validate(request.taxpayerData);
        case "ITR4" -> itr4ValidationService.validate(request.taxpayerData);
        default -> throw new IllegalStateException("Unknown ITR form: " + itrForm);
    };
    
    // Step 6: Pre-submission checklist
    ChecklistResult checklist = preSubmissionChecklistService.runChecklist(request.taxpayerData, itrForm);
    if (!checklist.allMandatoryPassed()) {
        return ITRFilingResult.blockedByChecklist(checklist.getFailedChecks());
    }
    
    // Step 7: Generate JSON
    String itdJson = switch(itrForm) {
        case "ITR1" -> itdJsonExportService.export(request.taxpayerData);
        case "ITR2" -> itr2JsonExportService.export(request.taxpayerData);
        case "ITR3" -> itr3JsonExportService.export(request.taxpayerData);
        case "ITR4" -> itr4JsonExportService.export(request.taxpayerData);
        default -> throw new IllegalStateException("Unknown ITR form: " + itrForm);
    };
    
    // Step 8: Audit trail
    auditTrailService.logComputation(pan, ay, "ITR JSON generated for " + itrForm, "SYSTEM", "JSON_OUTPUT", null, "SUCCESS");
    
    return ITRFilingResult.success(itdJson, validation, comparison, checklist);
}
```

---

## SECTION 13 — TESTING REQUIREMENTS

### 13.1 Unit Tests Required (Create JUnit tests for each)

For each new service, create at least these test cases:

**ITR2CBDTValidationService:**
- Test: HP with 3 SOPs → error VR2-HP-011
- Test: Co-owner PAN = assessee PAN → error VR2-HP-012
- Test: New regime + SOP interest > 0 → error VR2-HP-007
- Test: VDA income negative → auto-correct to 0
- Test: 80G cash donation Rs 2,500 → eligible amount = 0
- Test: LTCG 112A post-July23 at 12.5%; pre-July23 at 10%
- Test: Section 50C — SDV 115% of sale price → use SDV

**ITR3CBDTValidationService:**
- Test: F&O turnover = absolute profit + absolute loss (not gross)
- Test: Partner remuneration exceeds 40(b) limit → error
- Test: Cash payment Rs 15,000 → 40A(3) disallowance
- Test: MSME payment day 60 → 43B(h) disallowance
- Test: Goodwill in depreciation block → 0% depreciation
- Test: Additional depreciation asset < 180 days → 10% only

**ITR4CBDTValidationService:**
- Test: Turnover Rs 2.5 Cr, cash > 5% → error VR4-004
- Test: 44ADA declared profit < 50% → error VR4-008
- Test: 44AE heavy vehicle 10 tonnes, 12 months → income Rs 1,20,000
- Test: LTCG 112A Rs 1.5 Lakh in ITR-4 → error VR4-003

**TaxComputationEngine AY 2026-27:**
- Test: Income Rs 12,00,000 → tax = 0 (full rebate)
- Test: Income Rs 12,75,000 → tax capped at Rs 75,000 (marginal relief)
- Test: Income Rs 15,00,000 → tax Rs 1,09,200

### 13.2 Integration Test

Create one end-to-end integration test that:
1. Loads a sample Form 16 (JSON mock) + AIS JSON mock
2. Runs the full orchestration pipeline
3. Produces ITR-2 JSON
4. Validates JSON against all 63 Category A rules
5. Asserts zero Category A failures

---

## SECTION 14 — CONFIGURATION & DEPLOYMENT

### 14.1 Application Properties to Add

```properties
# Tax computation parameters (update these each AY without code change)
itr.ay2026-27.new-regime.rebate-threshold=1200000
itr.ay2026-27.new-regime.max-rebate=60000
itr.ay2026-27.cii-value=375  # UPDATE when CBDT notifies official value

# SFT thresholds
itr.sft.savings-deposit-alert-threshold=1000000
itr.sft.property-transaction-alert-threshold=3000000

# AIS reconciliation
itr.ais.tolerance-amount=100
itr.ais.form26as-priority=true

# MSME payment days
itr.43bh.msme-payment-days=45
```

### 14.2 Database Migrations Required

Create Flyway/Liquibase migration scripts for:
1. `audit_trail` table (Section 9.1)
2. `loss_ledger` table (for carry-forward losses across years)
3. `cfl_entries` table (carry forward loss entries per AY)

```sql
-- V2__create_loss_ledger.sql
CREATE TABLE loss_ledger (
    id BIGSERIAL PRIMARY KEY,
    taxpayer_pan VARCHAR(10) NOT NULL,
    incurred_ay VARCHAR(7) NOT NULL,
    loss_type VARCHAR(30) NOT NULL,
    original_loss_amount INTEGER NOT NULL,
    utilized_amount INTEGER NOT NULL DEFAULT 0,
    remaining_amount INTEGER NOT NULL,
    expiry_ay VARCHAR(7) NOT NULL,
    is_expired BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_loss_ledger_pan ON loss_ledger(taxpayer_pan, is_expired);
```

### 14.3 Build Verification

After implementation, confirm:
```bash
mvn clean compile        # must succeed with 0 errors
mvn test                 # all new unit tests pass
mvn verify               # integration tests pass
```

---

## SECTION 15 — COMPLETE GAP CLOSURE CHECKLIST

Use this as your completion tracking list. Mark each item as DONE only when:
(a) code is written, (b) unit test covers it, (c) it compiles successfully.

### Critical (Must complete before deployment):

- [ ] `TaxComputationEngine.java` — AY 2026-27 new regime slabs + marginal relief at Rs 12L
- [ ] `TaxComputationEngine.java` — Age computation as on April 1 of FY (not AY)
- [ ] `TaxComputationEngine.java` — 234A/234B/234C/234F interest computation
- [ ] `ITR2CBDTValidationService.java` — All 12 HP rules (VR2-HP-001 to VR2-HP-012)
- [ ] `ITR2CBDTValidationService.java` — All 8 Schedule 112A rules (VR2-CG-112A-001 to 008)
- [ ] `ITR2CBDTValidationService.java` — All 15 Schedule CG rules (VR2-CG-001 to 015)
- [ ] `ITR2CBDTValidationService.java` — All 5 CYLA rules (VR2-CYLA-001 to 005)
- [ ] `ITR2CBDTValidationService.java` — All 5 BFLA rules (VR2-BFLA-001 to 005)
- [ ] `ITR2CBDTValidationService.java` — All 9 VIA rules (VR2-VIA-001 to 009)
- [ ] `ITR2CBDTValidationService.java` — All 9 SI rules (VR2-SI-001 to 009)
- [ ] `LTCG112AGrandfatheringService.java` — rewritten per exact VR2-CG-112A-001 to 008
- [ ] `LossSetOffEngine.java` — full CYLA matrix with all loss type restrictions
- [ ] `ITR2JSONExportService.java` — complete JSON output for ITR-2
- [ ] `ITR3CBDTValidationService.java` — All 12 BP rules (VR3-BP-001 to 012)
- [ ] `ITR3CBDTValidationService.java` — All 7 DPM rules (VR3-DPM-001 to 007)
- [ ] `ITR3CBDTValidationService.java` — All 4 DCG rules (VR3-DCG-001 to 004)
- [ ] `DepreciationEngine.java` — full block depreciation with all rates and half-rate rule
- [ ] `ITR3JSONExportService.java` — complete JSON output for ITR-3
- [ ] `ITR4CBDTValidationService.java` — All 20 rules (VR4-001 to VR4-020)
- [ ] `ITR4JSONExportService.java` — complete JSON output for ITR-4
- [ ] `VDATransactionService.java` — real implementation replacing placeholder
- [ ] `SFTProcessingService.java` — real AIS JSON parsing for all 17 SFT types
- [ ] `ITRFilingOrchestrationService.java` — full pipeline wiring

### High Priority (Complete before production for ITR-2/3/4):

- [ ] `AuditTrailService.java` — database persistence replacing in-memory
- [ ] `MultiEmployerConsolidationService.java` — cross-validation logic (VAL-ME-001 to 006)
- [ ] `HRAComputationService.java` — metro auto-detection from PAN city
- [ ] `ScheduleALService.java` — auto-population from SFT data
- [ ] Database migrations — audit_trail, loss_ledger, cfl_entries tables

### Medium Priority (Complete within 30 days):

- [ ] `CIIService.java` — Cost Inflation Index table with configurable AY 2026-27 value
- [ ] `Section50CService.java` — stamp duty value vs sale price comparison
- [ ] `AMTComputationService.java` — Alternate Minimum Tax (18.5% on ATI)
- [ ] Enhanced error messages with user-friendly text and actionable guidance
- [ ] Configuration parameters for all threshold values (application.properties)

---

## IMPORTANT NOTES FOR AGENTIC AI IMPLEMENTOR

1. **Do not modify any existing ITR-1 service** unless you are only adding a new method. ITR-1 is production-ready at 100% compliance.

2. **All monetary amounts in ITD JSON must be integers.** Never output float/double. Use `Math.round()` before casting.

3. **All dates in ITD JSON must be DD/MM/YYYY format.** Use `ITDDateFormatter.java` (already exists) for all date conversions.

4. **SHA-256 digest** must be computed on the final JSON string before adding the Digest field to CreationInfo. This is already implemented in `SHA256DigestUtil.java` — call it for ITR-2/3/4 JSON as well.

5. **IntermediaryCity in CreationInfo must be <= 25 characters** for ALL ITR forms. Truncate if longer. This causes upload rejection at ITD portal.

6. **No special characters** in any text field: `~ @ # $ % ^ & * ( ) _ + { } | : < > ?`. Validate before JSON generation.

7. **New regime deductions:** In new regime, the ONLY deductions allowed are: Standard deduction on salary (Rs 75,000), Standard deduction on family pension (Rs 25,000 or 1/3), 80CCD(2) employer NPS, 80JJAA, 80CCH(2). All other Chapter VI-A deductions = 0 and must be blocked.

8. **AY parameter is mandatory** for every computation. Do not default silently. Throw an exception if AY is missing or invalid.

9. **CBDT validation rules are upload blockers** — a single CAT-A failure must prevent ITR JSON from being generated. Implement hard blocks, not soft warnings, for CAT-A rules.

10. **Loss carry-forward data** must be stored in the database after every successful ITR filing so next year's BFLA can reference it. This is the `LossLedger` table defined in Section 11.5 and 14.2.

---

*This directive covers all components required to bring the ITR ERP from 78% to 100% CBDT compliance. All reference data (validation rule IDs, tax slabs, depreciation rates, CII table, SFT codes) is embedded in this document. No additional reference lookup should be required.*

*Legal basis: Finance Act 2024 (AY 2025-26), Finance Act 2025 (AY 2026-27), Income-tax Act 1961, CBDT e-Filing Validation Rules V1.0/V1.1 (July 2025). Always verify CII for AY 2026-27 from official CBDT gazette notification before production deployment.*
