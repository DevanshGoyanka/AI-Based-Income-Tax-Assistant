# ITR ERP — PART 1: Computation Engine & CBDT Compliance Fixes
## For Agentic AI — Backend + Frontend Changes

> **Scope:** Tax computation correctness, CBDT-mandatory field additions, income head fixes,
> JSON export schema compliance, and frontend `computeTax()` logic.
> **Stack:** Spring Boot 3.2 (Java 17) backend + React 19 / TypeScript frontend.
> All monetary values in INR. AY referenced = 2025-26 unless stated.

---

## 1. CRITICAL: NEW REGIME TAX SLABS — AY 2025-26 (Finance Act 2025)

### File: `src/main/java/com/itr/service/taxengine/TaxSlabEngine.java`
### File: `src/main/java/com/itr/service/taxengine/TaxSlabCalculator.java`

The current implementation uses **wrong slabs**. Replace the new-regime slab logic with the
Finance Act 2025 slabs (effective AY 2025-26):

```java
// NEW REGIME SLABS — AY 2025-26 (Finance Act 2025)
// Replace existing newRegimeSlabs() method with:
private static final long[][] NEW_REGIME_SLABS_AY2526 = {
    {0,          400000,  0},    // 0%
    {400001,     800000,  5},    // 5%
    {800001,    1200000, 10},    // 10%
    {1200001,   1600000, 15},    // 15%
    {1600001,   2000000, 20},    // 20%
    {2000001,   2400000, 25},    // 25%
    {2400001, Long.MAX_VALUE, 30} // 30%
};

// REBATE u/s 87A — NEW REGIME AY 2025-26
// Income ≤ ₹12,00,000 → FULL TAX REBATE (effectively ₹0 tax)
// Max rebate amount = ₹60,000
public long calculateRebate87ANewRegime(long taxableIncome, long taxBeforeCess) {
    if (taxableIncome <= 1200000) {
        return Math.min(taxBeforeCess, 60000L);
    }
    return 0L;
}

// REBATE u/s 87A — OLD REGIME (unchanged)
// Income ≤ ₹5,00,000 → Max rebate ₹12,500
public long calculateRebate87AOldRegime(long taxableIncome, long taxBeforeCess) {
    if (taxableIncome <= 500000) {
        return Math.min(taxBeforeCess, 12500L);
    }
    return 0L;
}
```

**IMPORTANT — Rebate 87A and Special Rate Income:**
The rebate u/s 87A in the new regime does NOT apply to:
- STCG u/s 111A (equity STCG)
- LTCG u/s 112A (equity LTCG)
- VDA income u/s 115BBH

These special-rate incomes are excluded from rebate eligibility. Implement a check:

```java
// In RebateCalculator.java — add this guard
public long calculateRebate(long totalIncome, long specialRateIncome,
                             long taxOnNormalIncome, boolean isNewRegime) {
    long normalIncome = totalIncome - specialRateIncome;
    if (isNewRegime) {
        // Rebate is on tax computed on normal-rate income only
        if (normalIncome <= 1200000) {
            return Math.min(taxOnNormalIncome, 60000L);
        }
    } else {
        if (totalIncome <= 500000) {
            return Math.min(taxOnNormalIncome, 12500L);
        }
    }
    return 0L;
}
```

---

## 2. CRITICAL: STANDARD DEDUCTION — NEW REGIME AY 2025-26

### File: `src/main/java/com/itr/service/ITR1CalculatorService.java`

Standard deduction for salaried is **₹75,000** in BOTH old and new regime for AY 2025-26.
Ensure the constant is correct:

```java
private static final long STANDARD_DEDUCTION_AY2526 = 75000L;
// This applies to:
// 1. Salaried employees (Section 16(ia))
// 2. Pensioners (family pension deduction is LOWER: ₹15,000 or 1/3rd of pension, whichever is less)

// For FAMILY PENSION (not regular pension from employer):
public long calculateFamilyPensionDeduction(long familyPensionAmount) {
    // Deduction u/s 57(iia): Lower of ₹15,000 or 1/3rd of family pension
    return Math.min(15000L, familyPensionAmount / 3);
}
```

---

## 3. CRITICAL: CAPITAL GAINS — PRE/POST 23 JUL 2024 RATE CORRECTIONS

### File: `src/main/java/com/itr/service/ITR2CalculatorService.java`
### File: `src/main/java/com/itr/service/CapitalGainsExemptionService.java`

The current UI shows "Other Assets (20%)" under STCG — **THIS IS WRONG**.
STCG on non-listed/other assets is taxed at **NORMAL SLAB RATES** (added to GTI, not a special rate).

### Correct Capital Gains Tax Structure:

```
STCG:
├── Listed equity shares / equity-oriented MF / units of business trust:
│   ├── Transferred BEFORE 23-Jul-2024  → 15% (Section 111A)
│   └── Transferred ON/AFTER 23-Jul-2024 → 20% (Section 111A, amended)
└── All OTHER assets (debt, gold, property, unlisted shares, etc.):
    → ADD TO NORMAL INCOME → taxed at slab rates (NOT a special rate)

LTCG:
├── Listed equity shares / equity MF / units (Section 112A):
│   ├── Transferred BEFORE 23-Jul-2024  → 10% (above ₹1,00,000 exemption)
│   └── Transferred ON/AFTER 23-Jul-2024 → 12.5% (above ₹1,25,000 exemption)
│   NOTE: Grandfathering (FMV as on 31-Jan-2018) applies only for pre-Jul-2024
│         Post-Jul-2024: Cost of acquisition = actual cost, NO indexation
└── All OTHER assets (Section 112):
    ├── Purchased BEFORE 23-Jul-2024    → 20% WITH indexation
    └── Purchased ON/AFTER 23-Jul-2024  → 12.5% WITHOUT indexation
        (Finance Act 2024 amendment — no indexation for new purchases)
```

### LTCG Exemption Thresholds:
```java
// Section 112A exemption (equity LTCG only)
private static final long LTCG_EXEMPTION_AY2425 = 100000L;  // ₹1,00,000
private static final long LTCG_EXEMPTION_AY2526 = 125000L;  // ₹1,25,000

public long calculateLTCG112ATax(long ltcgPreJul2024, long ltcgPostJul2024,
                                  String assessmentYear) {
    long totalLTCG = ltcgPreJul2024 + ltcgPostJul2024;
    long exemption = "2025-26".equals(assessmentYear)
        ? LTCG_EXEMPTION_AY2526 : LTCG_EXEMPTION_AY2425;

    long taxablePreJul = Math.max(0, ltcgPreJul2024 - exemption);
    long taxablePostJul = ltcgPostJul2024; // exemption applied proportionally to pre-Jul first

    // If pre-Jul alone is < exemption, carry remaining exemption to post-Jul
    long remainingExemption = Math.max(0, exemption - ltcgPreJul2024);
    taxablePostJul = Math.max(0, ltcgPostJul2024 - remainingExemption);

    return (long)(taxablePreJul * 0.10 + taxablePostJul * 0.125);
}
```

### Fix Frontend CapitalGainsTab:
The "Other Assets (20%)" field under STCG must be REMOVED. Replace with a note:

```tsx
// In ITRComputationTabs.tsx — CapitalGainsTab component
// REMOVE: <Field label="Other Assets (20%)" ... />
// ADD: A section "STCG on Other Assets (Slab Rate)" that feeds into GTI as normal income

// The formData should have:
// stcgEquityPre   → STCG on listed equity/MF (pre 23-Jul-2024) → 15% tax
// stcgEquityPost  → STCG on listed equity/MF (post 23-Jul-2024) → 20% tax
// stcgOtherSlab   → STCG on debt/gold/unlisted etc → added to normal GTI (slab rate)
// ltcg112APre     → Listed equity LTCG pre 23-Jul-2024 → 10% (minus exemption)
// ltcg112APost    → Listed equity LTCG post 23-Jul-2024 → 12.5% (minus exemption)
// ltcgOtherPre    → Other asset LTCG purchased before 23-Jul-2024 → 20% with indexation
// ltcgOtherPost   → Other asset LTCG purchased on/after 23-Jul-2024 → 12.5% no indexation
```

---

## 4. CRITICAL: FRONTEND `computeTax()` FUNCTION FIXES

### File: `src/pages/ITRComputationTabs.tsx` — `computeTax()` function

Replace the capital gains tax computation block entirely:

```typescript
// === CAPITAL GAINS TAX (SPECIAL RATES) ===
// STCG on equity: 15% (pre-Jul 2024) and 20% (post-Jul 2024)
const stcgEquityPre = parseFloat(formData.stcgEquityPre || '0');
const stcgEquityPost = parseFloat(formData.stcgEquityPost || '0');
const stcgOtherSlab = parseFloat(formData.stcgOtherSlab || '0'); // Added to GTI below

// LTCG 112A (equity)
const ltcg112APre = parseFloat(formData.ltcg112APre || '0');
const ltcg112APost = parseFloat(formData.ltcg112APost || '0');
// LTCG 112 (other assets)
const ltcgOtherPre = parseFloat(formData.ltcgOtherPre || '0');   // 20% with indexation
const ltcgOtherPost = parseFloat(formData.ltcgOtherPost || '0');  // 12.5% no indexation

// VDA
const vdaIncome = parseFloat(formData.vdaIncome || '0');

// LTCG 112A exemption (₹1,25,000 for AY 2025-26)
const ltcg112AExemption = 125000;
const totalLtcg112A = ltcg112APre + ltcg112APost;
let exemptionUsed = Math.min(ltcg112APre, ltcg112AExemption);
const taxableLtcg112APre = Math.max(0, ltcg112APre - ltcg112AExemption);
const remainingExemption = Math.max(0, ltcg112AExemption - ltcg112APre);
const taxableLtcg112APost = Math.max(0, ltcg112APost - remainingExemption);

const cgTax =
  stcgEquityPre * 0.15 +        // STCG equity pre-Jul: 15%
  stcgEquityPost * 0.20 +       // STCG equity post-Jul: 20%
  taxableLtcg112APre * 0.10 +   // LTCG 112A pre-Jul: 10%
  taxableLtcg112APost * 0.125 + // LTCG 112A post-Jul: 12.5%
  ltcgOtherPre * 0.20 +         // LTCG other pre: 20%
  ltcgOtherPost * 0.125;        // LTCG other post: 12.5%

const vdaTax = vdaIncome * 0.30; // VDA always 30%

// stcgOtherSlab → add to GTI (slab taxed, not special rate)
// GTI calculation must include stcgOtherSlab as normal income:
const gti = netSalary + hpIncome + otherSourcesTotal + businessIncome + stcgOtherSlab;

// === REBATE u/s 87A ===
// Special-rate incomes are EXCLUDED from rebate eligibility:
const specialRateIncome = stcgEquityPre + stcgEquityPost +
  taxableLtcg112APre + taxableLtcg112APost + ltcgOtherPre + ltcgOtherPost + vdaIncome;
const normalRateIncome = totalTaxableIncome - specialRateIncome;

let rebate87A = 0;
if (isNewRegime) {
  // New regime: Full rebate if normal income ≤ ₹12,00,000
  if (normalRateIncome <= 1200000) {
    rebate87A = Math.min(taxOnNormalIncome, 60000);
  }
} else {
  // Old regime: Rebate if total income ≤ ₹5,00,000
  if (totalTaxableIncome <= 500000) {
    rebate87A = Math.min(taxOnNormalIncome, 12500);
  }
}
```

---

## 5. INCOME HEAD — SALARY TAB (Missing CBDT-Mandatory Fields)

### File: `src/pages/ITRComputationTabs.tsx` — `SalaryTab` component
### File: `src/main/java/com/itr/dto/Itr1FormData.java` — `SalaryIncome` inner class

**CBDT Schedule S requires these additional mandatory fields — ADD ALL:**

```typescript
// ADD to SalaryTab UI (and corresponding formData keys):

// Employer Details (already partially present, complete it):
// - Employer Name: formData.employerName (MANDATORY)
// - Employer TAN: formData.employerTAN (MANDATORY — 10-char alphanumeric)
// - Employer PAN: formData.employerPAN (MANDATORY for govt/PSU)
// - Employer Address: formData.employerAddress (MANDATORY)
// - Nature of Employment: formData.employerCategory (Govt/PSU/Pensioners/Others)

// Salary Breakup (CBDT Schedule S line items — all mandatory):
// - Basic Pay: formData.basicSalary
// - Dearness Allowance: formData.daAmount
// - Bonus: formData.bonusAmount
// - Commission: formData.commissionAmount
// - HRA Received: formData.hraReceived
// - Leave Travel Allowance (LTA): formData.ltaReceived
// - Children Education Allowance: formData.ceaReceived
// - Any other allowance: formData.otherAllowance
// - Perquisites u/s 17(2): formData.perquisitesAmount
// - Profits in lieu of salary u/s 17(3): formData.profitsInLieu
// - Gross Salary (auto-computed): sum of above

// Exemptions u/s 10:
// - HRA Exempt u/s 10(13A): formData.hraExempt (auto-computed)
// - LTA Exempt u/s 10(5): formData.ltaExempt
// - CEA Exempt u/s 10(14): formData.ceaExempt (max ₹100/month per child, 2 children)
// - Transport Allowance Exempt (for disabled): formData.transportExempt
// - Any other exempt allowance: formData.otherExempt

// Deductions u/s 16:
// - Standard Deduction u/s 16(ia): ₹75,000 (auto-populated, non-editable)
// - Entertainment Allowance u/s 16(ii): formData.entertainmentAllowance (Govt employees only)
// - Professional Tax u/s 16(iii): formData.professionalTax (max ₹2,500)

// Validation: Employer TAN format = 10 chars (AAAA99999A)
// Validation: Professional Tax max ₹2,500 p.a.
```

**Add to `Itr1FormData.java` SalaryIncome class:**

```java
// Add these fields to SalaryIncome inner class:
private String employerPAN;           // Mandatory for govt
private BigDecimal commissionAmount;
private BigDecimal ltaReceived;
private BigDecimal ltaExempt;
private BigDecimal ceaReceived;
private BigDecimal ceaExempt;
private BigDecimal otherAllowance;
private BigDecimal profitsInLieu;     // Section 17(3)
private BigDecimal entertainmentAllowance;  // Section 16(ii) - govt only
private BigDecimal otherExempt;
```

---

## 6. INCOME HEAD — OTHER SOURCES (Missing CBDT Fields)

### File: `src/pages/ITRComputationTabs.tsx` — `OtherSourcesTab`
### File: `src/main/java/com/itr/dto/Itr1FormData.java` — `OtherSourcesIncome`

**CBDT Schedule OS requires:**

```typescript
// CURRENTLY MISSING — ADD THESE:

// 1. Multiple TDS entries for interest (not just one bank):
//    Support array: formData.interestEntries[] = [{deductorName, tan, amount, tdsAmt}]
//    Each entry from 26AS/AIS must be captured separately for TDS credit

// 2. Interest on Income Tax Refund (Section 244A):
//    formData.interestOnITRefund

// 3. Receipts from letting of plant/machinery:
//    formData.rentFromMachinery

// 4. Casual Income / Winnings from Lottery (Section 115BB — 30% flat):
//    formData.lotteryWinnings → taxed at 30%, NO deduction allowed

// 5. Royalty income (Section 80QQB / 80RRB):
//    formData.royaltyIncome

// 6. Section 80TTA / 80TTB — Savings Bank Interest Deduction:
//    (This is a deduction but flows from here)
//    80TTA: SB interest up to ₹10,000 (for non-senior)
//    80TTB: ALL interest up to ₹50,000 (for senior citizen ≥ 60 yrs)
//    Auto-compute based on age from DOB

// RENAME FIELDS for clarity:
// formData.sbInterest → savings bank interest (feeds 80TTA/TTB)
// formData.fdInterest → FD/RD/TD interest (taxable, no deduction)
// formData.dividendIncome → dividend from shares/MF (Section 8)
```

**Family Pension** (already present but needs correction):
```java
// Family pension deduction u/s 57(iia):
// Deduction = Lower of 1/3rd of pension OR ₹15,000
// This deduction must appear in "Deductions from Other Sources" NOT in Chapter VI-A
public BigDecimal familyPensionDeduction(BigDecimal familyPension) {
    BigDecimal oneThird = familyPension.divide(BigDecimal.valueOf(3), RoundingMode.DOWN);
    return oneThird.min(BigDecimal.valueOf(15000));
}
```

---

## 7. INCOME HEAD — HOUSE PROPERTY (Missing CBDT Fields)

### File: `src/pages/ITRComputationTabs.tsx` — `HousePropertyTab`

**CBDT Schedule HP requires — ADD MISSING:**

```typescript
// For LET-OUT property (currently partially implemented):
// - Annual Lettable Value (ALV): formData.grossAnnualValue
// - Municipal Taxes Paid (actually paid during year): formData.municipalTaxesPaid
// - Net Annual Value (NAV = ALV - Municipal Tax): auto-computed
// - Standard Deduction u/s 24(a): 30% of NAV → auto-computed (NON-EDITABLE)
// - Interest on borrowed capital u/s 24(b): formData.letOutLoanInterest (NO LIMIT for let-out)
// - Net Income = NAV - 30% - Interest

// For SELF-OCCUPIED (SOP):
// - Annual Value = NIL (by law)
// - Pre-construction interest: formData.preConstructionInterest
//   (1/5th of pre-construction interest, spread over 5 years)
// - Deduction u/s 24(b): formData.sopLoanInterest
//   Limit: ₹2,00,000 for SOP (loan taken on/after 01-Apr-1999 and construction within 5 yrs)
//          ₹30,000 for SOP (loan taken before 01-Apr-1999 or construction beyond 5 yrs)
// - Net Income = NEGATIVE (loss from HP for SOP)

// CBDT MANDATORY — Lender Details (if interest claimed):
// - Lender Name: formData.lenderName (MANDATORY if any loan interest claimed)
// - Lender PAN: formData.lenderPAN (MANDATORY if lender is company/firm)
// - Date of Loan: formData.loanDate
// - Loan Amount: formData.loanAmount

// IMPORTANT: HP loss set-off rules:
// 1. HP loss (from SOP/let-out) can be set off against any other head (max ₹2L per year)
// 2. Balance HP loss CANNOT be carried forward beyond this ₹2L cap
// 3. The ₹2L cap is for CURRENT YEAR set-off. Excess is LOST (not carried forward).
//    EXCEPTION: If taxpayer has opted out of new regime, C/F of HP loss is possible.
//    In NEW REGIME: HP loss set-off NOT ALLOWED (Section 115BAC(2))

// Fix in computeTax():
if (isNewRegime) {
  hpLossSetOff = 0; // HP loss set-off NOT permitted in new regime
} else {
  hpLossSetOff = Math.min(Math.abs(hpLoss), 200000); // Max ₹2L
}
```

---

## 8. DEDUCTIONS TAB — OLD REGIME (Complete Chapter VI-A)

### File: `src/pages/ITRComputationTabs.tsx` — `DeductionsTab`
### File: `src/main/java/com/itr/service/DeductionCalculatorService.java`

The old regime deductions tab must include ALL sections. Current implementation is incomplete.

**Add ALL missing deductions:**

```typescript
// 80C (Max ₹1,50,000 — aggregate):
// formData.deduction80C_epf    → EPF contribution
// formData.deduction80C_ppf    → PPF contribution
// formData.deduction80C_elss   → ELSS / Tax Saving MF
// formData.deduction80C_lic    → Life Insurance Premium
// formData.deduction80C_nsc    → NSC
// formData.deduction80C_fd     → 5-Year Tax Saving FD
// formData.deduction80C_ssy    → Sukanya Samriddhi Yojana
// formData.deduction80C_homeLoanPrincipal → Home Loan Principal repaid
// formData.deduction80C_tuitionFee → Children Tuition Fee (max 2 children)
// formData.deduction80C_stamps → Registration/Stamp Duty for house purchase
// Total 80C = sum of above, capped at ₹1,50,000

// 80CCC: Pension Fund Contribution (included in 80C aggregate)
// formData.deduction80CCC

// 80CCD(1): NPS Contribution (included in 80C aggregate, max 10% of salary)
// formData.deduction80CCD1

// 80CCD(1B): ADDITIONAL NPS contribution — ₹50,000 OVER 80C limit
// formData.deduction80CCD1B (max ₹50,000 — this is NOT part of ₹1.5L limit)

// 80CCD(2): Employer NPS contribution — available in BOTH regimes
// Max: 10% of salary for private; 14% for govt employees
// formData.deduction80CCD2

// 80D: Health Insurance Premium:
// Self/Family (below 60): max ₹25,000
// Self/Family (≥ 60): max ₹50,000
// Parents (below 60): additional max ₹25,000
// Parents (≥ 60): additional max ₹50,000
// formData.deduction80D_self
// formData.deduction80D_parents
// formData.parentsSeniorCitizen (boolean)
// formData.selfSeniorCitizen (boolean — derived from DOB)

// 80DD: Disability dependent (₹75,000 or ₹1,25,000 for severe)
// formData.deduction80DD
// formData.isDisabilitySevere (boolean)

// 80DDB: Medical treatment of specified disease
// Self < 60: max ₹40,000; ≥ 60: max ₹1,00,000
// formData.deduction80DDB

// 80E: Interest on Education Loan (No limit, max 8 years)
// formData.deduction80E

// 80EE: Interest on home loan (first-time buyer, AY 2017-18 to AY 2021-22 only)
// formData.deduction80EE (₹50,000 — only if relevant years)

// 80EEA: Interest on affordable housing loan (stamp value ≤ ₹45L, loan before 31-Mar-2022)
// formData.deduction80EEA (max ₹1,50,000)

// 80G: Donations (50%/100% of qualifying amount):
// formData.donations80G[] = [{entityName, PAN, amount, eligibilityPct}]
// 80G donations with 100% deduction (no qualifying limit): PM Relief Fund etc.
// 80G donations with 50% deduction: Various charities

// 80GG: House Rent (for non-HRA employees):
// Min of: ₹5,000/month, 25% of total income, rent paid - 10% of income
// formData.deduction80GG
// formData.rentPaidMonthly_80GG

// 80TTA: SB Interest (non-senior citizen) — max ₹10,000
// Auto-computed from formData.sbInterest (from Other Sources tab)
// formData.deduction80TTA = min(sbInterest, 10000)

// 80TTB: ALL interest (senior citizen ≥ 60) — max ₹50,000
// Replaces 80TTA if assessee is senior citizen
// formData.deduction80TTB = min(totalInterest, 50000)
// NOTE: 80TTA and 80TTB are MUTUALLY EXCLUSIVE

// 80U: Self disability (₹75,000 or ₹1,25,000 for severe)
// formData.deduction80U
```

**Validation in `DeductionCalculatorService.java`:**
```java
// Section 80C aggregate cap:
BigDecimal total80C = sum(deduction80C_epf, deduction80C_ppf, /* ...all 80C items */,
                          deduction80CCC, deduction80CCD1).min(BigDecimal.valueOf(150000));

// 80CCD(2) — available in new regime too:
BigDecimal deduction80CCD2Limit;
if ("Government".equals(employerCategory)) {
    deduction80CCD2Limit = grossSalary.multiply(BigDecimal.valueOf(0.14)); // 14%
} else {
    deduction80CCD2Limit = grossSalary.multiply(BigDecimal.valueOf(0.10)); // 10%
}
BigDecimal deduction80CCD2 = actual80CCD2.min(deduction80CCD2Limit);

// 80TTA vs 80TTB — mutually exclusive:
boolean isSeniorCitizen = age >= 60;
BigDecimal deduction80TTA = isSeniorCitizen ? BigDecimal.ZERO :
    sbInterest.min(BigDecimal.valueOf(10000));
BigDecimal deduction80TTB = isSeniorCitizen ?
    totalInterest.min(BigDecimal.valueOf(50000)) : BigDecimal.ZERO;
```

---

## 9. TDS & ADVANCE TAX TAB — CBDT COMPLIANCE

### File: `src/pages/ITRComputationTabs.tsx` — `TDSTab`
### File: `src/main/java/com/itr/dto/Itr1FormData.java` — `TaxPayments`

**Current issues and fixes:**

```typescript
// 1. SUPPORT MULTIPLE TDS ENTRIES PER SECTION:
// Currently: Single entry for TDS on Interest (194A)
// Required: Array of entries (one per deductor from 26AS)
// formData.tdsEntries194A = [
//   { deductorName: "STATE BANK OF INDIA", tan: "MUMS89569E", 
//     amount: 224329, tdsDeducted: 22443, certificateNo: "", year: "2025-26" },
//   { deductorName: "ANAND PURUSHOTTAM AGRAWAL", tan: "NGPA14339D",
//     amount: 45000, tdsDeducted: 4500, ... }
// ]

// 2. SCHEDULE TDS1 (TDS on Salary — Section 192):
// Only ONE employer typically — but support multiple for job changes
// formData.tds192Entries = [{ employerName, tan, totalSalary, tdsDeducted }]

// 3. SCHEDULE TDS2 (TDS on Income other than Salary):
// Multiple entries possible — array format:
// formData.tds194AEntries[] (interest)
// formData.tds194Entries[]  (dividends — 194)
// formData.tds195Entries[]  (non-resident payments)
// Each entry: { deductorName, deductorTAN, grossAmount, taxDeducted,
//               certificateNo, dateOfDeduction }

// 4. ADVANCE TAX — 4 instalments (Section 207-219):
// Due dates for AY 2025-26 (FY 2024-25):
// 1st instalment: 15-Jun-2024 (15% of advance tax)
// 2nd instalment: 15-Sep-2024 (45% cumulative)
// 3rd instalment: 15-Dec-2024 (75% cumulative)
// 4th instalment: 15-Mar-2025 (100% cumulative)
// formData.advTax_jun, advTax_sep, advTax_dec, advTax_mar

// 5. SELF-ASSESSMENT TAX (Section 140A):
// formData.selfAssessmentTax
// formData.satBSRCode     → Bank BSR code (MANDATORY for SAT)
// formData.satChallanNo   → Challan serial number
// formData.satDate        → Date of payment
// (From AIS Part B3 / 26AS Part D — auto-populate)

// 6. INTEREST u/s 234A/234B/234C — COMPUTE AND SHOW:
// Must be calculated and shown in Tax Computation summary
// 234A: Delay in filing (1% per month from due date to actual filing)
// 234B: Shortfall in advance tax payment (90% rule)
// 234C: Deferment of quarterly advance tax instalments
```

**Backend — `InterestCalculator.java` — ensure correct logic:**
```java
// 234B — Advance Tax shortfall:
// If advance tax paid < 90% of assessed tax → 1% per month on shortfall
// From 1-Apr of AY to date of assessment/filing

// 234C — Quarterly deferment:
// If cumulative AT paid < threshold at each due date → 1% per month
// Q1 (by 15-Jun): If paid < 12% → interest on shortfall for 3 months
// Q2 (by 15-Sep): If cumulative < 36% → 3 months interest
// Q3 (by 15-Dec): If cumulative < 75% → 3 months interest
// Q4 (by 15-Mar): If cumulative < 100% → 1 month interest
// NOTE: 234C NOT applicable if advance tax < ₹10,000 (Section 208)
```

---

## 10. TAX COMPUTATION SUMMARY TAB — COMPLETE CBDT STRUCTURE

### File: `src/pages/ITRComputationTabs.tsx` — `TaxComputationTab`

The computation summary must follow exact CBDT Part B-TTI structure:

```typescript
// REQUIRED COMPUTATION FLOW (Part B-TTI of ITR):
interface TaxComputationSummary {
  // INCOME HEADS:
  salaryIncome: number;          // Schedule S → Net Taxable Salary
  housePropertyIncome: number;   // Schedule HP → can be negative
  capitalGainsIncome: number;    // Schedule CG (STCG slab-rated only)
  businessIncome: number;        // Schedule BP
  otherSourcesIncome: number;    // Schedule OS

  grossTotalIncome: number;      // Sum of all positive heads after set-off

  // LOSS SET-OFF (inter-head, current year):
  hpLossSetOff: number;         // HP loss set-off (max ₹2L, not in new regime)
  businessLossSetOff: number;   // Business loss set-off

  // BROUGHT-FORWARD LOSSES:
  bfLossHPSetOff: number;       // B/F HP loss (max ₹2L per year)
  bfLossCGSetOff: number;       // B/F CG loss (only against CG)
  bfLossBusinessSetOff: number; // B/F business loss

  gtiAfterSetOff: number;       // GTI after all set-offs

  // DEDUCTIONS:
  deductionsVIA: number;        // Chapter VI-A total
  deduction80CCD2: number;      // Shown separately (available in new regime)

  totalTaxableIncome: number;   // GTI after set-offs and deductions

  // TAX COMPUTATION (on normal income):
  taxOnNormalIncome: number;    // From slab computation
  rebate87A: number;            // u/s 87A
  surcharge: number;            // If income > ₹50L
  healthEducationCess: number;  // 4% on (tax + surcharge)

  // SPECIAL RATE TAXES:
  stcgTax111A: number;          // STCG on equity (15%/20%)
  ltcgTax112A: number;          // LTCG on equity (10%/12.5%)
  ltcgTax112: number;           // LTCG on other assets (20%/12.5%)
  vdaTax115BBH: number;         // VDA @ 30%
  lotteryTax115BB: number;      // Lottery/game winnings @ 30%

  totalTaxLiability: number;    // Sum of all taxes + cess

  // INTEREST PAYABLE:
  interest234A: number;         // Late filing interest
  interest234B: number;         // Advance tax shortfall
  interest234C: number;         // Quarterly deferment
  feeUnder234F: number;         // Late filing fee (₹5,000 or ₹1,000)

  totalTaxWithInterest: number; // Tax + interest + fee

  // PREPAID TAXES:
  tdsOnSalary: number;          // Schedule TDS1
  tdsOnOtherIncome: number;     // Schedule TDS2
  advanceTaxPaid: number;       // Schedule IT
  selfAssessmentTax: number;    // Schedule IT

  taxPayableOrRefund: number;   // Positive = payable, Negative = refund
}

// LATE FILING FEE u/s 234F:
// Filed after due date (31-Jul for non-audit):
// Income ≤ ₹5,00,000 → ₹1,000
// Income > ₹5,00,000 → ₹5,000
function calculateFee234F(totalIncome: number, filingDate: Date, dueDate: Date): number {
  if (filingDate <= dueDate) return 0;
  return totalIncome <= 500000 ? 1000 : 5000;
}
```

---

## 11. JSON EXPORT — CBDT SCHEMA COMPLIANCE

### File: `src/main/java/com/itr/service/ITR1JSONExportService.java`

**Fix the JSON export to match ITD utility schema exactly:**

```java
// PART A-GEN (General Information):
// Add missing mandatory fields:
// - "FilingStatus": { "ReturnFileSec": "11" (139(1) normal), "SeventhProvisio139": false }
// - "ResidentialStatus": "Resident" | "RNorR" | "NR"
// - "EmployerCategory": "Govt" | "PSU" | "PensionersOthers" | "NA"
// - "AadhaarCardNo" or "AadhaarEnrolmentId" (one mandatory)
// - "DOB": "DD/MM/YYYY" format
// - "Gender": "M" | "F" | "T"

// SCHEDULE S (Salary):
// Must include exact ITR-1 fields:
// {
//   "Salaries": [{
//     "NameOfEmployer": "...",
//     "TANofEmployer": "...",
//     "AddressofEmployer": "...",
//     "TypeofEmployer": "Govt|PSU|PensionersOthers|Others",
//     "Salarys": {
//       "Salary": gross_salary,
//       "PerquisitesValue": perquisites,
//       "ProfitsInSalary": profits_in_lieu,
//       "GrossSalary": total_gross
//     },
//     "AllwncExemptUs10": {
//       "AllwncExemptUs10_Dtls": [
//         { "SalNatureDesc": "HRA", "SalOthAmount": hra_exempt },
//         { "SalNatureDesc": "LTA", "SalOthAmount": lta_exempt }
//       ],
//       "TotalAllwncExemptUs10": total_exempt_10
//     },
//     "NetSalary": net_salary,
//     "DeductionUs16": {
//       "StdDeduction": 75000,
//       "EntertainmentAlw": 0,
//       "ProfessionalTax": prof_tax
//     },
//     "TaxableSalary": taxable_salary
//   }]
// }

// SCHEDULE TDS1 (TDS on Salary):
// {
//   "TDSonSalaries": [{
//     "EmployerOrDeductorOrCollectOfTaxName": "...",
//     "TANOfEmployerOrDeductorOrCollector": "...",
//     "TotalTaxDeductedAtSourceFromSal": tds_amount
//   }]
// }

// SCHEDULE TDS2 (TDS on other income):
// {
//   "TDSonOthThanSalaries": [{
//     "DeductorName": "...",
//     "TANOfDedu": "...",
//     "GrossAmt": gross_amount,
//     "TaxDeductedAtSrc": tds_amount,
//     "AmtClaimedForTaxDeducted": claimed_amount  // same as TaxDeductedAtSrc unless partial claim
//   }]
// }

// SCHEDULE IT (Advance Tax & Self-Assessment Tax):
// {
//   "TaxPayments": [{
//     "BSRCode": "...",
//     "DateDep": "DD/MM/YYYY",
//     "SrlNoOfChaln": "...",
//     "Amt": amount,
//     "MinorHead500": false  // true = self-assessment, false = advance tax
//   }]
// }

// PART B-TTI (Tax computation — the critical section):
// Ensure this follows the exact ITD utility structure with all fields non-null
// Zero values must be "0" not null
```

---

## 12. ITR FORM ELIGIBILITY VALIDATION

### File: `src/main/java/com/itr/service/validation/ITR1ValidationService.java`

**ITR-1 (Sahaj) eligibility — enforce strictly:**
```java
public ValidationResult validateITR1Eligibility(Itr1FormData data) {
    List<String> errors = new ArrayList<>();

    // ITR-1 NOT applicable if:
    if (data.getTotalIncome() > 5000000) {
        errors.add("ITR-1 not applicable: Total income exceeds ₹50 Lakhs. Use ITR-2.");
    }
    if (data.getCapitalGains() != null && data.getCapitalGains().hasTransactions()) {
        errors.add("ITR-1 not applicable: Capital gains present. Use ITR-2.");
    }
    if (data.getBusinessIncome() != null && data.getBusinessIncome().hasTransactions()) {
        errors.add("ITR-1 not applicable: Business/profession income present. Use ITR-3/4.");
    }
    if (data.getHouseProperties() != null && data.getHouseProperties().size() > 1) {
        errors.add("ITR-1 not applicable: More than one house property. Use ITR-2.");
    }
    if (data.getResidentialStatus() != null && !data.getResidentialStatus().equals("Resident")) {
        errors.add("ITR-1 not applicable: Non-resident/RNOR assessee. Use ITR-2.");
    }
    if (isDirectorInCompany(data)) {
        errors.add("ITR-1 not applicable: Director in a company. Use ITR-2.");
    }
    if (hasUnlistedShares(data)) {
        errors.add("ITR-1 not applicable: Holds unlisted shares. Use ITR-2.");
    }
    // Agricultural income > ₹5,000:
    if (data.getAgriculturalIncome() != null && data.getAgriculturalIncome() > 5000) {
        errors.add("ITR-1 not applicable: Agri income > ₹5,000. Use ITR-2.");
    }
    return new ValidationResult(errors);
}
```

---

## 13. PERSONAL INFO TAB — MISSING CBDT-MANDATORY FIELDS

### File: `src/pages/ITRComputationTabs.tsx` — `PersonalInfoTab`

**ADD these currently missing but CBDT-mandatory fields:**

```typescript
// FILING DETAILS (CBDT mandatory):
// formData.filingSection: "11" (139(1) original) | "12" (139(4) belated) | 
//                         "17" (139(5) revised) | "16" (119(2)(b) condonation)
// formData.isBelatedReturn: boolean
// formData.originalReturnAckNo: string (if revised return — 139(5))
// formData.originalReturnDate: Date (if revised return)

// BANK ACCOUNT (CBDT mandatory for refund):
// formData.bankAccountNo  (MANDATORY — for refund credit)
// formData.bankIFSC       (MANDATORY)
// formData.bankName       (MANDATORY)
// formData.isPrimaryAccount: boolean
// Support multiple bank accounts (array) — one marked as primary for refund

// AUDIT DETAILS (Section 44AB):
// formData.isAuditRequired: boolean
// formData.auditorName: string (if audit required)
// formData.auditorMembershipNo: string
// formData.auditReportDate: Date

// GENDER (CBDT mandatory):
// formData.gender: "Male" | "Female" | "Transgender"

// IS DIRECTOR IN COMPANY:
// formData.isDirector: boolean (triggers ITR-2 if true)

// HOLDS UNLISTED SHARES:
// formData.holdsUnlistedShares: boolean

// AGRICULTURAL INCOME:
// formData.agriculturalIncome: number

// POLITICAL PARTY DONATION (Section 29C):
// formData.politicalPartyDonation: number

// FORM 67 (Foreign Tax Credit — if applicable):
// formData.hasForeignIncome: boolean
```

---

## 14. FILE CLEANUP — BACKEND (Remove/Consolidate Duplicates)

### Files to DELETE (confirmed duplicates/unused):

```
DELETE: src/main/java/com/itr/service/LossSetOffEngine.java
  → Functionality already in LossSetOffService.java — full duplicate

DELETE: src/main/java/com/itr/controller/FilingController.java
  → Placeholder only (not implemented), filing handled by ITRUnifiedController

DELETE: src/main/java/com/itr/service/taxengine/TaxSlabCalculator.java
  → Duplicate of TaxSlabEngine.java — consolidate into TaxSlabEngine only

DELETE: src/main/java/com/itr/service/HRAComputationService.java
  → HRA logic already in SalaryExemptionService.java and ITR1CalculatorService.java
  → Keep only in ITR1CalculatorService.java

DELETE: src/main/java/com/itr/service/CapitalGainsExemptionService.java
  → Consolidate into ITR2CalculatorService.java (its only caller)

DELETE: src/main/java/com/itr/service/integration/ITDPrefillImportService.java
  → ITD prefill format changes yearly; currently broken; remove for now

DELETE: src/main/java/com/itr/service/taxengine/AMTCalculator.java
  → AMT applies only to certain ITR-3 cases and is a niche feature; placeholder only
  → Re-add properly when ITR-3 AMT is fully implemented
```

### Files to CONSOLIDATE:

```
MERGE: TaxSlabEngine.java + RebateCalculator.java + SurchargeCalculator.java
  → INTO: src/main/java/com/itr/service/taxengine/TaxComputationEngine.java
  → All three are called sequentially; merge reduces inter-service coupling

MERGE: ITR1CBDTValidationService.java into ITR1ValidationService.java
  → They serve the same purpose (validation); having two separate files is confusing
  → Keep one comprehensive ITR1ValidationService.java
```

### Frontend — Files to DELETE:

```
DELETE: src/pages/AccountingPage.tsx
  → Not linked in router, incomplete, not required for ITR-1/2/4
  → Retain only if ITR-3 full implementation is planned

DELETE: src/pages/AdvancedTaxPage.tsx  
  → Functionality should be INSIDE ITRComputationPage (TDS tab)
  → Standalone page adds navigation complexity
```

---

## 15. NEW REGIME — WHAT IS ALLOWED VS NOT

### For `computeTax()` and backend validation — enforce these rules:

```
NEW REGIME (Section 115BAC) — DISALLOWED deductions/exemptions:
✗ HRA exemption (Section 10(13A))
✗ LTA exemption (Section 10(5))
✗ Standard deduction u/s 16 (EXCEPTION: ₹75,000 for salaried IS allowed from AY 2024-25 onwards)
✗ Chapter VI-A deductions (80C, 80D, 80E, 80G, 80TTA, etc.) — EXCEPT 80CCD(2)
✗ HP loss set-off against other income
✗ Set-off of brought-forward losses from years when old regime was used
✗ 30% standard deduction on let-out HP (ALLOWED — this is part of HP income computation, not Chapter VI-A)
✗ Deduction u/s 24(b) for let-out HP interest (ALLOWED — deduction from HP income)

NEW REGIME — ALLOWED:
✓ Standard deduction ₹75,000 (salaried/pensioners) — Section 16(ia)
✓ Employer NPS contribution u/s 80CCD(2) (10%/14% of salary)
✓ Rebate u/s 87A (₹60,000 effective for income ≤ ₹12L)
✓ Family pension deduction u/s 57(iia) (₹15,000)
✓ 30% standard deduction on let-out property (Section 24(a))
✓ Interest on let-out property loan (Section 24(b) — for let-out only)
✓ Transport allowance for disabled employees
✓ Conveyance allowance for actual travel to office
```

**Enforce in frontend — when regime switches to New:**
```typescript
function enforceNewRegimeRestrictions(formData: FormData): FormData {
  return {
    ...formData,
    hraExempt: 0,           // Zero out HRA exemption
    ltaExempt: 0,           // Zero out LTA exemption
    deduction80C: 0,        // Zero all Chapter VI-A except 80CCD(2)
    deduction80D: 0,
    deduction80E: 0,
    deduction80G: 0,
    deduction80TTA: 0,
    deduction80TTB: 0,
    deduction80CCD1B: 0,
    hpLossSetOff: 0,        // HP loss set-off not allowed
    // Keep: deduction80CCD2 (employer NPS)
  };
}
```

---

## 16. SURCHARGE — MARGINAL RELIEF IMPLEMENTATION

### File: `src/main/java/com/itr/service/taxengine/SurchargeCalculator.java`

```java
// Surcharge slabs (applicable to individuals):
// Income > ₹50L to ₹1Cr: 10%
// Income > ₹1Cr to ₹2Cr: 15%
// Income > ₹2Cr to ₹5Cr: 25%
// Income > ₹5Cr: 37% (Old regime only; New regime max = 25%)

// NEW REGIME: Max surcharge = 25% (capped — Section 115BAC)
// This applies to ALL income including STCG/LTCG in new regime.

// MARGINAL RELIEF: Must implement properly
// If income is ₹50,00,001, the surcharge on ₹50,00,001 income cannot exceed
// (₹50,00,001 - ₹50,00,000) = ₹1. i.e., Net tax after surcharge cannot exceed
// net tax at exactly ₹50,00,000 by more than the income increase.

public BigDecimal applyMarginalRelief(BigDecimal income, BigDecimal taxBeforeSurcharge,
                                      BigDecimal surcharge) {
    // Threshold amounts:
    long[] thresholds = {5000000L, 10000000L, 20000000L, 50000000L};
    for (long threshold : thresholds) {
        if (income.longValue() > threshold) {
            BigDecimal taxAtThreshold = computeTaxAtThreshold(threshold);
            BigDecimal increaseInIncome = income.subtract(BigDecimal.valueOf(threshold));
            BigDecimal increasedTax = taxBeforeSurcharge.add(surcharge).subtract(taxAtThreshold);
            if (increasedTax.compareTo(increaseInIncome) > 0) {
                // Grant marginal relief: tax increase limited to income increase
                return taxAtThreshold.add(increaseInIncome).subtract(taxBeforeSurcharge);
            }
        }
    }
    return surcharge;
}
```

---

*End of Part 1. Refer FIXES_PART2_PDF_IMPORT_CLEANUP.md for PDF extraction,
auto-population logic, 26AS/AIS/TIS part-wise mapping, and remaining file cleanups.*
