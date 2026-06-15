# Complete Data Flow: Salary → Tax Output (AY 2026-27)

---

## EXACT FILE NAMES AND CONTENTS

### FRONTEND FILES (User Entry to API Call)

#### 1. **`frontend/src/pages/ITRComputationPage.tsx`**
- **Purpose:** Main ITR computation page container
- **Contains:**
  - Tab navigation (Personal, Salary, HP, CG, Business, Other Sources, Exempt, Deductions, TDS, Tax)
  - `formData` state object holding ALL user inputs
  - Calls `itrApi.computeTaxSummary(formData, ayParam, regime)` on tab 9 (Tax Computation)
  - Receives `taxResult` with all computed values

#### 2. **`frontend/src/components/EmployerEntryManager.tsx`**
- **Purpose:** Multi-employer salary data entry
- **Contains:**
  - `EmployerEntry[]` array - each entry has:
    - `employerName`, `employerTAN`, `employerAddress`
    - `natureOfEmployment` ('GOV' | 'NGOV' | 'PSU' | 'PENSIONER')
    - `basic`, `da`, `hraReceived`, `bonus`, `allowances`, `lta`
    - `rentPaid`, `isMetroCity` (for HRA)
    - `gratuityReceived`, `leaveEncashmentReceived`, `pensionCommutation`
    - `perquisites` (aggregate), `perqVehicle`, `perqRentFree`, etc.
    - `professionalTax`, `tdsDeducted`
    - `isGovernmentEmployee`, `retirementDate`
  - Calls `calculateSalary()` from salaryCalculationService.ts
  - Stores response with `grossSalary`, `netSalary`, exemptions

#### 3. **`frontend/src/services/salaryCalculationService.ts`**
- **Purpose:** Frontend salary calculation (delegates to backend)
- **Contains:**
  - `calculateSalary(employerInputs)` - calls backend
  - `EmployerInput` interface with all fields
  - Returns `SalaryCalculationResponse` with computed values
  - Types: `EmployerCalculation`, `SalaryCalculationResponse`

#### 4. **`frontend/src/api/itr.ts`**
- **Purpose:** API calls to backend
- **Key Methods:**
  ```typescript
  computeTaxSummary(formData, assessmentYear, regime)
  // Calls: POST /tax-summary/compute
  // Returns: TaxComputationResult with all computed fields
  ```

---

### BACKEND FILES (API Entry to Tax Output)

#### 5. **`backend/src/main/java/com/itr/config/TaxController.java`**
- **Purpose:** REST API endpoint for tax computation
- **Contains:**
  - `@PostMapping("/tax-summary/compute")` - MAIN ENDPOINT
  - `@PostMapping("/clients/{clientId}/itr/{ay}/compute")` - backup endpoint
  - Extracts `formData` from frontend
  - Calls `buildScheduleOSInput()` to transform frontend data
  - Calls `taxComputationOrchestrator.computeTax()`
  - Returns `Map<String, Object>` with all computed values

#### 6. **`backend/src/main/java/com/itr/service/TaxComputationOrchestrator.java`**
- **Purpose:** CENTRAL TAX COMPUTATION ENGINE
- **Contains:**
  ```java
  public TaxComputationResult computeTax(ITRForm itrForm, TaxRegime regime)
  ```
  - **Step 1:** Validates ITR form type eligibility (`FormTypeValidator.validate()`)
  - **Step 2:** Computes salary via `SalaryScheduleComputer.compute()`
  - **Step 3:** Gets house property income from `itrForm.getHpIncome()`
  - **Step 4:** Gets capital gains (`stcgIncome + ltcgIncome`)
  - **Step 5:** Gets business income
  - **Step 6:** Computes Schedule VDA via `ScheduleVDAComputer.compute()`
  - **Step 7:** Computes Schedule OS via `ScheduleOSComputer.compute(osInput, regime)`
  - **Step 8:** Calculates gross total income
  - **Step 9:** Applies Chapter VI-A deductions
  - **Step 10:** Computes tax slabs (`computeNewRegimeTax()` / `computeOldRegimeTax()`)
  - **Step 11:** Applies rebate 87A
  - **Step 12:** Calculates surcharge
  - **Step 13:** Calculates cess (4%)
  - **Step 14:** Calculates total tax liability
  - **Step 15:** Calculates interest (234A, 234B, 234C)
  - **Step 16:** Gets TDS/Advance Tax/Self Assessment Tax
  - **Step 17:** Calculates balance tax payable or refund
  - **Returns:** `TaxComputationResult` with all fields

#### 7. **`backend/src/main/java/com/itr/domain/salary/SalaryScheduleComputer.java`**
- **Purpose:** Computes salary income
- **Contains:**
  ```java
  public static SalaryComputationResult compute(
      List<EmployerEntry> employers, 
      long standardDeduction
  )
  ```
  - Aggregates multiple employers
  - Sums: `grossSalary`, `basicDA`, `hraReceived`, `professionalTax`, `tds`
  - Applies standard deduction
  - Returns `SalaryComputationResult`

#### 8. **`backend/src/main/java/com/itr/domain/salary/EmployerEntry.java`**
- **Purpose:** DTO for per-employer salary data
- **Contains:** (record)
  ```java
  EmployerEntry(
      String employerName,
      String tan,
      long grossSalary,
      long basicDA,
      long hraReceived,
      long ltaReceived,
      long specialAllowances,
      long bonus,
      long gratuityReceived,
      long leaveEncashmentReceived,
      long employerPFContribution,
      long employerNPSContribution,
      long perksValue,
      long professionalTax,
      long tdsDeducted
  )
  ```

#### 9. **`backend/src/main/java/com/itr/domain/salary/SalaryComputationResult.java`**
- **Purpose:** Result of salary computation
- **Contains:**
  ```java
  SalaryComputationResult(
      long totalGrossSalary,
      long totalExemptAllowances,
      long netSalary,
      long standardDeduction,
      long professionalTaxDeduction,
      long totalDeductionsU16,
      long totalEmployerNPS,
      long totalTDSDeducted,
      int employerCount
  )
  ```

#### 10. **`backend/src/main/java/com/itr/domain/salary/HRAExemption.java`**
- **Purpose:** HRA calculation under Section 10(13A)
- **Contains:**
  ```java
  public static long compute(
      long actualHRA,
      long basicDA,
      long rentPaid,
      boolean isMetro
  )
  ```
  - Condition 1: Actual HRA
  - Condition 2: Rent - 10% of salary
  - Condition 3: 50%/40% of salary (metro/non-metro)
  - Returns minimum of three

#### 11. **`backend/src/main/java/com/itr/domain/salary/GratuityExemption.java`**
- **Purpose:** Gratuity calculation under Section 10(10)
- **Contains:**
  ```java
  public static long compute(
      long lastDrawnMonthlySalary,
      int yearsOfService,
      long actualGratuityReceived
  )
  ```
  - 15/26 × salary × years
  - Cap: ₹20,00,000
  - Actual received

#### 12. **`backend/src/main/java/com/itr/domain/othersources/ScheduleOSComputer.java`**
- **Purpose:** Computes Income from Other Sources
- **Contains:**
  ```java
  public static ScheduleOSResult compute(ScheduleOSInput input, TaxRegime regime)
  ```
  - Interest income (17A-17I)
  - Dividend income (1ai, 1aii, 1aiii)
  - Winnings (115BB, 115BBJ)
  - Gifts (56(2)(x))
  - Family pension (57(iia))
  - Section 57 deductions

#### 13. **`backend/src/main/java/com/itr/domain/vda/ScheduleVDAComputer.java`**
- **Purpose:** VDA computation under Section 115BBH
- **Contains:**
  ```java
  public VDAComputationResult compute(List<VDAEntry> entries)
  ```
  - Income = Sale consideration - Cost of acquisition
  - Tax @ 30%
  - Cess @ 4%
  - No loss set-off

#### 14. **`backend/src/main/java/com/itr/domain/common/TaxComputationResult.java`**
- **Purpose:** FINAL RESULT - all tax computation output
- **Contains ALL computed fields:**
  ```java
  // Income heads
  totalIncome, grossTotalIncome, netTaxableIncome
  taxOnNormalIncome, taxOnSpecialRateIncome
  rebate87A, surcharge, healthAndEducationCess, totalTaxLiability
  
  // Other sources breakdown
  otherIncome, totalInterest, totalDividend, totalWinnings
  winningsTax115BB, winningsTax115BBJ, vdaGains, vdaTax
  familyPensionIncome, familyPensionDed, taxableGifts
  
  // Tax payments
  tdsAmount, advanceTaxPaid, selfAssessmentTaxPaid, totalTaxesPaid
  
  // Interest/Fees
  interest234A, interest234B, interest234C, fee234F, totalInterestAndFees
  
  // Final
  balanceTaxPayable, refundAmount, taxRegime, warnings
  ```

---

## COMPLETE DATA FLOW DIAGRAM

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│ 1. EmployerEntryManager.tsx                                                │
│    ├── User enters: employer details, salary components, perquisites      │
│    ├── HRA details (rent, metro city)                                     │
│    ├── Retirement benefits (gratuity, leave encashment, pension)          │
│    └── Arrays: employerEntries[]                                          │
│                                                                             │
│ 2. salaryCalculationService.ts                                            │
│    ├── Sends employerEntries[] to backend                                 │
│    └── Receives: grossSalary, netSalary, exemptions                       │
│                                                                             │
│ 3. ITRComputationPage.tsx                                                 │
│    ├── Aggregates: employerEntries[], hpIncome, capitalGains, etc.        │
│    ├── All form data → formData object                                     │
│    └── Calls: itrApi.computeTaxSummary(formData, "2026-27", "NEW")        │
│                                                                             │
│ 4. api/itr.ts → POST /tax-summary/compute                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              BACKEND                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│ 5. TaxController.java                                                     │
│    ├── @PostMapping("/tax-summary/compute")                                │
│    ├── Extracts formData from request                                      │
│    ├── Builds ScheduleOSInput from formData                               │
│    └── Calls: TaxComputationOrchestrator.computeTax(itrForm, regime)      │
│                                                                             │
│ 6. TaxComputationOrchestrator.java (CENTRAL ENGINE)                      │
│    │                                                                       │
│    ├── [STEP 1] FormTypeValidator.validate()                              │
│    │    └── ITR-1/ITR-4 eligibility gates                                │
│    │                                                                       │
│    ├── [STEP 2] SalaryScheduleComputer.compute()                         │
│    │    ├── Input: List<EmployerEntry>, standardDeduction                │
│    │    ├── Aggregates: grossSalary, basicDA, professionalTax            │
│    │    ├── Applies: standardDeduction (75K/50K)                         │
│    │    └── Returns: SalaryComputationResult                              │
│    │                                                                       │
│    ├── [STEP 3] House Property: itrForm.getHpIncome()                    │
│    │                                                                       │
│    ├── [STEP 4] Capital Gains: stcgIncome + ltcgIncome                   │
│    │                                                                       │
│    ├── [STEP 5] Business Income: itrForm.getBusinessIncome()             │
│    │                                                                       │
│    ├── [STEP 6] ScheduleVDA: ScheduleVDAComputer.compute()               │
│    │    ├── Input: List<VDAEntry>                                        │
│    │    └── Returns: VDA income, tax @ 30%, cess                          │
│    │                                                                       │
│    ├── [STEP 7] ScheduleOS: ScheduleOSComputer.compute()                 │
│    │    ├── Input: Interest, Dividend, Winnings, Gifts, Family Pension   │
│    │    ├── Returns: OS income, taxes, taxable amounts                   │
│    │    │                                                                    │
│    │    └── Uses:                                                          │
│    │         • InterestIncomeComputer (17A-17I)                          │
│    │         • DividendIncomeComputer (1ai,1aii,1aiii)                  │
│    │         • WinningsComputer (115BB, 115BBJ)                          │
│    │         • GiftPropertyComputer (56(2)(x))                           │
│    │         • FamilyPensionComputer (57(iia))                           │
│    │         • Sec57DeductionsComputer                                     │
│    │                                                                       │
│    ├── [STEP 8] Gross Total = Salary + HP + CG + Business + OS            │
│    │                                                                       │
│    ├── [STEP 9] Deductions (Chapter VI-A)                                │
│    │    ├── 80C, 80CCC, 80CCD, 80D, 80E, 80G, 80TTA/80TTB               │
│    │    └── Applies based on regime (NEW regime limited)                  │
│    │                                                                       │
│    ├── [STEP 10] Tax on Normal Income                                     │
│    │    ├── computeNewRegimeTax() or computeOldRegimeTax()               │
│    │    └── Tax slabs for AY 2026-27                                     │
│    │                                                                       │
│    ├── [STEP 11] Rebate 87A                                               │
│    │    ├── New regime: income ≤ ₹7L → rebate up to ₹60,000              │
│    │    └── Old regime: income ≤ ₹5L → rebate up to ₹25,000              │
│    │                                                                       │
│    ├── [STEP 12] Surcharge                                                │
│    │    └── 10%/15%/25%/37% based on income slabs                        │
│    │                                                                       │
│    ├── [STEP 13] Health & Education Cess = 4%                            │
│    │                                                                       │
│    ├── [STEP 14] Total Tax = Normal Tax + Special Rate Tax + Cess         │
│    │    └── Special rate: winnings (30%), VDA (30%)                      │
│    │                                                                       │
│    ├── [STEP 15] Interest (234A, 234B, 234C)                             │
│    │                                                                       │
│    ├── [STEP 16] TDS + Advance Tax + Self Assessment                     │
│    │                                                                       │
│    ├── [STEP 17] Balance Tax Payable / Refund                            │
│    │                                                                       │
│    └── Returns: TaxComputationResult                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TAX RESULT OUTPUT                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│ TaxComputationResult contains:                                             │
│                                                                             │
│ ► INCOME HEADS:                                                            │
│   - grossTotalIncome = salary + hp + cg + business + otherSources         │
│   - netTaxableIncome                                                      │
│   - totalDeductions (Chapter VI-A)                                        │
│                                                                             │
│ ► SALARY BREAKDOWN (from SalaryComputationResult):                        │
│   - totalGrossSalary = sum of all employers                              │
│   - standardDeduction = 75000 (NEW) or 50000 (OLD)                       │
│   - professionalTaxDeduction                                              │
│   - netSalary = gross - exemptions - deductions                           │
│                                                                             │
│ ► OTHER SOURCES BREAKDOWN:                                                 │
│   - totalInterest (bank, fd, nsc, scss, post office, etc.)               │
│   - totalDividend (1ai + 1aii + 1aiii)                                    │
│   - totalWinnings (115BB traditional + 115BBJ online)                      │
│   - winningsTax115BB, winningsTax115BBJ                                   │
│   - vdaGains, vdaTax                                                      │
│   - familyPensionIncome, familyPensionDed                                 │
│   - taxableGifts                                                         │
│                                                                             │
│ ► TAX CALCULATION:                                                         │
│   - taxOnNormalIncome                                                    │
│   - taxOnSpecialRateIncome (winnings + vda)                              │
│   - rebate87A                                                             │
│   - surcharge                                                            │
│   - healthAndEducationCess                                               │
│   - totalTaxLiability                                                    │
│                                                                             │
│ ► TAX PAYMENTS:                                                            │
│   - tdsAmount = total TDS from all sources                               │
│   - advanceTaxPaid                                                       │
│   - selfAssessmentTaxPaid                                                │
│   - totalTaxesPaid                                                       │
│                                                                             │
│ ► FINAL RESULT:                                                            │
│   - balanceTaxPayable = tax + interest - taxesPaid                       │
│   - refundAmount = taxesPaid - tax - interest                            │
│   - taxRegime = NEW / OLD                                                │
│   - warnings (list of any issues)                                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## FILE QUICK REFERENCE TABLE

| # | File Name | Purpose | Key Input | Key Output |
|---|-----------|---------|-----------|------------|
| **FRONTEND** |
| 1 | EmployerEntryManager.tsx | Salary data entry | User inputs | employerEntries[] |
| 2 | salaryCalculationService.ts | Backend calc wrapper | employerEntries | SalaryCalculationResponse |
| 3 | ITRComputationPage.tsx | Main form container | All form inputs | formData object |
| 4 | api/itr.ts | HTTP client | formData | TaxComputationResult |
| **BACKEND** |
| 5 | TaxController.java | REST endpoint | JSON request | TaxComputationResult |
| 6 | TaxComputationOrchestrator.java | Central engine | ITRForm + Regime | TaxComputationResult |
| 7 | SalaryScheduleComputer.java | Salary computation | employers[] | SalaryComputationResult |
| 8 | EmployerEntry.java | Per-employer DTO | All salary fields | salary data |
| 9 | SalaryComputationResult.java | Salary result | Computed values | salary totals |
| 10 | HRAExemption.java | HRA calc | hra, basic, rent, metro | exempt amount |
| 11 | GratuityExemption.java | Gratuity calc | salary, years, actual | exempt amount |
| 12 | ScheduleOSComputer.java | Other sources | interest, dividend, etc. | OS totals |
| 13 | ScheduleVDAComputer.java | VDA computation | VDA entries | VDA income/tax |
| 14 | TaxComputationResult.java | FINAL OUTPUT | All computations | Complete tax result |

---

## CALL SEQUENCE SUMMARY

```
User Entry → EmployerEntryManager.tsx
     ↓
formData → ITRComputationPage.tsx
     ↓
itrApi.computeTaxSummary() → api/itr.ts
     ↓
POST /tax-summary/compute → TaxController.java
     ↓
TaxComputationOrchestrator.computeTax()
     ├→ FormTypeValidator.validate()
     ├→ SalaryScheduleComputer.compute()
     │   └→ (HRAExemption, GratuityExemption called internally)
     ├→ ScheduleOSComputer.compute() (via TaxController)
     │   ├→ InterestIncomeComputer
     │   ├→ DividendIncomeComputer
     │   ├→ WinningsComputer
     │   ├→ GiftPropertyComputer
     │   └→ FamilyPensionComputer
     ├→ ScheduleVDAComputer.compute()
     ├→ Tax calculation (slabs, rebate, surcharge, cess)
     ├→ Interest calculation
     └→ TDS/Advance Tax processing
     ↓
TaxComputationResult (contains ALL output)
     ↓
Frontend displays in TaxComputationTab
```

---

*Document: Complete Data Flow Reference*
*Generated: June 13, 2026*
*For: AY 2026-27 ITD Filing System*
