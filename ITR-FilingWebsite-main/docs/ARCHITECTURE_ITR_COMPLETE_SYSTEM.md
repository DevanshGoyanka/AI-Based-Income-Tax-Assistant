# Complete ITR System Architecture
**Date**: April 4, 2026  
**Compliance**: 101% accurate per ITR_Field_Guide.docx and ERP_Execution_Plan.docx

---

## 1. Automatic ITR Form Selection Engine

### 1.1 PAN-Based Entity Type Detection

**PAN Format**: AAAAA9999A (10 characters)
- **4th Character** determines entity type:
  - **P** = Individual (Person)
  - **H** = HUF (Hindu Undivided Family)
  - **F** = Firm (Partnership)
  - **C** = Company
  - **A** = AOP (Association of Persons)
  - **T** = Trust
  - **B** = BOI (Body of Individuals)
  - **L** = Local Authority
  - **J** = Artificial Juridical Person
  - **G** = Government

**ITR Form Applicability by Entity**:
- ITR-1, ITR-2, ITR-3, ITR-4: Only **P** (Individual) and **H** (HUF)
- ITR-5: F, A, T, B, L, J
- ITR-6: C (Company)
- ITR-7: Trust/Political Party u/s 139(4A/4B/4C/4D)

### 1.2 Income-Based ITR Form Selection Logic

```
IF (4th char of PAN != 'P' AND != 'H') THEN
    IF (4th char == 'C') THEN ITR-6
    ELSE IF (4th char IN ['F','A','T','B','L','J']) THEN ITR-5
    ELSE ITR-7
    EXIT
END IF

// For Individuals (P) and HUF (H) only:

IF (hasBusinessIncome OR hasProfessionIncome) THEN
    IF (eligibleForPresumptive44AD OR eligible44ADA OR eligible44AE) THEN
        ITR-4 (Sugam)
    ELSE
        ITR-3
    END IF
ELSE IF (hasCapitalGains) THEN
    ITR-2
ELSE IF (hasMultipleHouseProperties) THEN  // >1 property
    ITR-2
ELSE IF (hasForeignIncome OR hasForeignAssets) THEN
    ITR-2
ELSE IF (totalIncome > 5000000) THEN  // >₹50L
    ITR-2
ELSE IF (residentialStatus != 'RES') THEN  // NRI/RNOR
    ITR-2
ELSE IF (isDirectorInCompany) THEN
    ITR-2
ELSE IF (hasUnlistedEquityShares) THEN
    ITR-2
ELSE IF (agriculturalIncome > 5000) THEN
    ITR-2
ELSE IF (hasIncomeFromLottery OR hasIncomeFromRaceHorses) THEN
    ITR-2
ELSE
    ITR-1 (Sahaj)
END IF
```

---

## 2. ITR-2 Calculation Engine

### 2.1 Income Heads (All from ITR-1 PLUS):

**Additional Schedules**:
- **Schedule HP**: Multiple house properties (unlimited)
- **Schedule CG**: Capital Gains (STCG + LTCG)
  - Part A: Short-Term Capital Gains
  - Part B: Long-Term Capital Gains
  - Special rates: 111A (15% equity STCG), 112A (10% equity LTCG)
- **Schedule CYLA**: Current Year Loss Adjustment
- **Schedule BFLA**: Brought Forward Loss Adjustment
- **Schedule AL**: Assets & Liabilities (if income >₹50L)
- **Schedule FA**: Foreign Assets & Income
- **Schedule TR**: Trust/Estate Income
- **Schedule EI**: Exempt Income (expanded)

### 2.2 Capital Gains Tax Computation

**STCG (Short-Term Capital Gains)**:
- **STCG u/s 111A**: Equity/MF held ≤12 months, STT paid → **15%** flat
- **Other STCG**: Added to normal income, taxed at slab rates

**LTCG (Long-Term Capital Gains)**:
- **LTCG u/s 112A**: Equity/MF held >12 months, STT paid → **10%** on gains >₹1,25,000 (no indexation)
- **LTCG u/s 112**: Other assets (property, gold, debt MF) → **20%** with indexation
- **LTCG on listed bonds/debentures**: **10%** without indexation

**Tax Computation Sequence for ITR-2**:
```
1. Compute normal income (salary + HP + OS) → Apply slab rates
2. Compute STCG u/s 111A → Apply 15% flat
3. Compute LTCG u/s 112A → Apply 10% on (gain - ₹1.25L)
4. Compute LTCG u/s 112 → Apply 20% with indexation
5. Total Tax = Tax on normal income + Tax on STCG + Tax on LTCG
6. Apply 87A rebate ONLY on normal income tax (NOT on CG tax)
7. Apply surcharge on total tax
8. Apply 4% cess on (tax + surcharge)
```

**Critical Rule**: 87A rebate does NOT apply to capital gains tax, only to normal slab income tax.

### 2.3 Multiple House Properties

**Computation per Property**:
- Self-Occupied: Max 2 properties can be self-occupied (AY 2020-21 onwards)
- Let-Out: All other properties treated as let-out
- Deemed Let-Out: If >2 self-occupied, treat additional as deemed let-out

**Loss Set-off & Carry Forward**:
- HP loss set-off against other heads: Max **₹2,00,000** per year (Section 71(3A))
- Remaining loss: Carry forward for **8 years** (Section 71B)

---

## 3. ITR-3 Calculation Engine

### 3.1 Business/Profession Income Computation

**Method 1: Regular Books (Section 44AA)**:
- Maintain books of accounts
- Prepare P&L and Balance Sheet
- Audit required if:
  - Turnover >₹1 crore (business)
  - Gross receipts >₹50 lakh (profession)

**Deductions u/s 30-37**:
- Sec 30: Rent, rates, taxes, repairs
- Sec 31: Depreciation (as per IT Act rates, not Companies Act)
- Sec 32: Additional depreciation (20% on new P&M)
- Sec 35: Scientific research, patents
- Sec 36: Insurance, bonus, interest on borrowed capital
- Sec 37: General business expenses

**Disallowances**:
- Sec 40(a): TDS not deducted/paid → 30% disallowance
- Sec 40A(3): Cash payments >₹10,000 → 100% disallowance
- Sec 43B: Statutory dues not paid before due date → disallowed

**Depreciation Rates (Block of Assets)**:
- Building: 10%
- Plant & Machinery: 15%
- Computers: 40%
- Intangible assets: 25%
- Furniture: 10%

### 3.2 Speculative vs Non-Speculative Business

**Speculative Business** (Section 43(5)):
- Intraday equity trading (no delivery)
- Loss can ONLY be set-off against speculative profits
- Carry forward: 4 years

**Non-Speculative Business**:
- F&O trading (derivatives) → Non-speculative
- Loss can be set-off against any business income
- Carry forward: 8 years

### 3.3 Professional Income

**Eligible Professions** (Section 44AA):
- Legal, Medical, Engineering, Architecture, Accountancy, Technical Consultancy, Interior Decoration, etc.

**Deductions**:
- Same as business (Sec 30-37)
- No depreciation on goodwill (post-2021)

---

## 4. ITR-4 Calculation Engine (Presumptive Taxation)

### 4.1 Section 44AD (Business)

**Eligibility**:
- Turnover ≤₹2 crore
- Not applicable to: Commission agents, professionals, plying/hiring goods carriages

**Presumptive Income**:
- **8%** of turnover (if cash/bank receipt)
- **6%** of turnover (if digital payment via bank/UPI)

**Tax Computation**:
```
Presumptive Income = (Cash Turnover × 8%) + (Digital Turnover × 6%)
No deductions u/s 30-37 allowed (already presumed)
No depreciation allowed
Advance Tax: Single installment by March 15 (100%)
```

**Opting Out**:
- Once opted out, must maintain books for **5 years**
- Cannot opt back into 44AD for 5 years

### 4.2 Section 44ADA (Profession)

**Eligibility**:
- Gross receipts ≤₹50 lakh
- Specified professions only (legal, medical, engineering, etc.)

**Presumptive Income**:
- **50%** of gross receipts (flat)

**Tax Computation**:
```
Presumptive Income = Gross Receipts × 50%
No deductions u/s 30-37 allowed
Advance Tax: Single installment by March 15 (100%)
```

### 4.3 Section 44AE (Goods Carriage)

**Eligibility**:
- Owns ≤10 goods vehicles
- Plying, hiring, or leasing goods carriages

**Presumptive Income**:
- **₹7,500 per vehicle per month** (or part thereof)
- Heavy goods vehicle (>12 tons): ₹1,000 per ton per month

**Tax Computation**:
```
Presumptive Income = Number of Vehicles × ₹7,500 × Months Owned
No deductions allowed
```

---

## 5. Common Tax Computation Rules (All ITRs)

### 5.1 Section 288A Rounding

**Rounding Logic**:
- Total Income: Round to nearest ₹10
- Tax Amount: Round to nearest ₹10
- Rounding Rule:
  - Last digit 0: No change
  - Last digit 1-4: Round down (e.g., 12,344 → 12,340)
  - Last digit 5-9: Round up (e.g., 12,345 → 12,350)

### 5.2 Interest Calculations

**234A (Late Filing)**:
```
IF (filingDate > dueDate) THEN
    delayMonths = CEIL(daysBetween(dueDate, filingDate) / 30)
    interest234A = taxPayable × 1% × delayMonths
END IF
```

**234B (Short Payment of Advance Tax)**:
```
IF (advanceTaxPaid + TDS < 90% of assessedTax) THEN
    shortfall = assessedTax - advanceTaxPaid - TDS
    months = monthsBetween(Apr 1, filingDate)
    interest234B = shortfall × 1% × months
END IF
```

**234C (Deferment of Advance Tax Installments)**:
```
Installment Schedule:
- Jun 15: 15% of tax liability
- Sep 15: 45% of tax liability (cumulative)
- Dec 15: 75% of tax liability (cumulative)
- Mar 15: 100% of tax liability (cumulative)

For each installment:
    IF (paidTillDate < duePercentage × taxLiability) THEN
        shortfall = (duePercentage × taxLiability) - paidTillDate
        monthsToMar15 = monthsBetween(installmentDate, Mar 15)
        interest += shortfall × 1% × monthsToMar15
    END IF
```

**234F (Late Filing Fee)**:
```
IF (filingDate > dueDate) THEN
    IF (totalIncome ≤ 500000) THEN
        fee234F = ₹1,000
    ELSE
        fee234F = ₹5,000
    END IF
END IF
```

### 5.3 Surcharge Rates

**Old Regime**:
- ₹50L - ₹1Cr: 10%
- ₹1Cr - ₹2Cr: 15%
- ₹2Cr - ₹5Cr: 25%
- Above ₹5Cr: 37%

**New Regime**:
- ₹50L - ₹1Cr: 10%
- ₹1Cr - ₹2Cr: 15%
- ₹2Cr - ₹5Cr: 25%
- Above ₹5Cr: 25% (lower than old regime)

**Marginal Relief**: If surcharge increases tax by more than income above threshold, limit surcharge to that excess.

---

## 6. Implementation Plan

### Phase 1: Enhanced ITR Form Selector (2 hours)
- PAN validation with 4th character logic
- Comprehensive income-based selection
- Detailed eligibility checks
- Selection reason explanation

### Phase 2: ITR-2 Calculator (4 hours)
- Multiple house properties computation
- Capital gains engine (STCG/LTCG with special rates)
- Loss set-off and carry forward
- Assets & Liabilities schedule
- Foreign income/assets handling

### Phase 3: ITR-3 Calculator (4 hours)
- Business P&L computation
- Depreciation calculator (block of assets)
- Speculative vs non-speculative logic
- Professional income computation
- Balance sheet validation

### Phase 4: ITR-4 Calculator (2 hours)
- Section 44AD computation (8%/6% logic)
- Section 44ADA computation (50% flat)
- Section 44AE computation (₹7,500/vehicle/month)
- Advance tax single installment logic

### Phase 5: Common Services (2 hours)
- Enhanced interest calculator (234A/B/C/F)
- Surcharge calculator with marginal relief
- Section 288A rounding service
- Validation engine for all forms

### Phase 6: Integration & Testing (2 hours)
- DTOs for ITR-2, ITR-3, ITR-4
- API endpoints for all forms
- Frontend form selection UI
- End-to-end testing

**Total Estimated Time**: 16 hours

---

## 7. Validation Rules (101% Compliance)

### ITR-1 Validation:
- Total income ≤₹50L
- Max 1 house property
- No capital gains
- No business income
- Resident individual only
- No foreign income/assets

### ITR-2 Validation:
- Capital gains properly classified (STCG/LTCG)
- STT paid for 111A/112A rates
- Indexation applied for 112
- HP loss set-off capped at ₹2L
- Assets & Liabilities if income >₹50L

### ITR-3 Validation:
- P&L and Balance Sheet balance
- Depreciation as per IT Act rates
- Disallowances properly computed
- Speculative loss only vs speculative profit
- Audit report if required

### ITR-4 Validation:
- Turnover/receipts within limits
- Presumptive % correctly applied
- No deductions claimed
- Single advance tax installment by Mar 15
- Cannot have capital gains (use ITR-3)

---

## 8. Database Schema Extensions

**New Tables**:
- `itr2_capital_gains` (STCG/LTCG transactions)
- `itr2_house_properties` (multiple properties)
- `itr3_business_pl` (P&L accounts)
- `itr3_balance_sheet` (assets/liabilities)
- `itr3_depreciation` (block of assets)
- `itr4_presumptive` (44AD/44ADA/44AE details)

**Existing Table Extensions**:
- `client_year_data`: Add `itr_form_type` (ITR1/ITR2/ITR3/ITR4)
- `client_year_data`: Add `form_selection_reason` (TEXT)

---

## 9. API Endpoints

```
POST /api/itr/select-form
  → Auto-select ITR form based on PAN + income profile

GET /api/itr2/{clientId}/{year}
POST /api/itr2/{clientId}/{year}/save
POST /api/itr2/{clientId}/{year}/compute

GET /api/itr3/{clientId}/{year}
POST /api/itr3/{clientId}/{year}/save
POST /api/itr3/{clientId}/{year}/compute

GET /api/itr4/{clientId}/{year}
POST /api/itr4/{clientId}/{year}/save
POST /api/itr4/{clientId}/{year}/compute
```

---

**End of Architecture Document**
