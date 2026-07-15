# COMPLETE SCHEDULE PLANNING DOCUMENT
## ITR-1 through ITR-4 — All Schedules, Income Heads, Sections & Flows

**Version:** 1.1 (CORRECTED)  
**Date:** 2026-07-15  
**Based on:** ITD JSON Schema v1.1 AY 2026-27 (from your Downloads), TO_OPENTAX docs, OpenTax vendor code

---

# 1. ITR FORM ELIGIBILITY MATRIX (VERIFIED FROM JSON SCHEMAS)

| Criteria | ITR-1 (Sahaj) | ITR-2 | ITR-3 | ITR-4 (Sugam) |
|----------|:---:|:---:|:---:|:---:|
| **Income Types Allowed** | | | | |
| Salary/Pension | ✅ | ✅ | ✅ | ✅ |
| One House Property | ✅ | ✅ | ✅ | ✅ |
| Other Sources (Interest/Dividend) | ✅ | ✅ | ✅ | ✅ |
| Family Pension | ✅ | ✅ | ✅ | ✅ |
| Multiple House Properties | ❌ | ✅ | ✅ | ❌ |
| LTCG u/s 112A ONLY (Listed Equity) | ✅ LTCG112A | ✅ Schedule112A | ✅ Schedule112A | ✅ LTCG112A |
| Other Capital Gains (111A, 112, VDA, etc.) | ❌ | ✅ ScheduleCGFor23,115AD,VDA | ✅ ScheduleCGFor23,115AD,VDA,DCG | ❌ |
| Business/Profession | ❌ | ❌ | ✅ Regular | ✅ Presumptive only |
| Foreign Assets/Income | ❌ | ✅ ScheduleFA,FSI,TR1 | ✅ ScheduleFA,FSI,TR1 | ❌ |
| Agriculture Income > ₹5000 | ❌ | ✅ | ✅ | ✅ (in IncomeDeductions) |
| VDA/Crypto Gains | ❌ | ✅ ScheduleVDA | ✅ ScheduleVDA | ❌ |
| Lottery/Winnings | ❌ | ✅ ScheduleOS | ✅ ScheduleOS | ❌ (OS only) |
| **Eligibility Conditions** | | | | |
| Max Total Income | < ₹50L | No limit | No limit | < ₹50L |
| Director in Company | ❌ | ✅ | ✅ | ❌ |
| Unlisted Equity Investment | ❌ | ✅ | ✅ | ❌ |
| ESOP held | ❌ | ✅ | ✅ | ❌ |
| Presumptive Taxation | N/A | N/A | N/A | ✅ (44AD/ADA/AE) |

### CRITICAL: LTCG 112A in ITR-1

ITR-1 **DOES** allow LTCG u/s 112A (listed equity/MF with STT paid), but ONLY 112A - nothing else:
- **ITR-1 Schema:** Has `LTCG112A` schedule with: TotSaleCnsdrn, TotCstAcqisn, LongCap112A(max ₹125,000)
- **ITR-1 IncomeDeductions:** Has `GrossTotIncomeIncLTCG112A` field = GTI including 112A LTCG
- **ITR-1 TaxComputation:** Tax is computed on GTI (including 112A LTCG), slab tax + 112A special rate
- **ITR-4 Schema:** ALSO has `LTCG112A` + `GrossTotIncomeIncLTCG112A` — same pattern

This means: **All four ITR forms support LTCG 112A. Only ITR-2/3 support FULL CG (all sections).**



---

# 2. COMPLETE SCHEDULE INVENTORY

## 2.1 CORE SCHEDULES (All ITR Forms)

### ScheduleS — Salary Income
**ITD JSON Path:** `ScheduleS` → `SalaryDtls[]`  
**Used In:** ITR-1, ITR-2, ITR-3, ITR-4  
**OIDAR Section:** Sec 15-17

| Field | Type | Required | Limits |
|-------|------|----------|--------|
| EmployerName | string | Yes | Max 255 |
| EmployerTAN | string | Yes | `[A-Z]{4}[0-9]{5}[A-Z]` |
| EmployerCategory | enum | Yes | CGOV, SGOV, PSU, PE, PESG, PEPS, PEO, OTH, NA |
| GrossSalary | integer | Yes | 0 to 99,999,999,999,999 |
| PerquisitesValue | integer | No | 0 to max |
| ProfitsInSalary | integer | No | 0 to max |
| EntertainmentAllowance | integer | No | 0 to 5,000 |
| ProfessionalTax | integer | No | 0 to 5,000 |
| AllowancesExempt | JSONB | No | Array of {Section10Code, Amount} |
| DeductionUs16ia | integer | No | 0 to 75,000 (std deduction) |
| NetSalary | integer | Yes | Auto-computed |
| IncomeFromSal | integer | Yes | Auto-computed |

**Computation:**
```
GrossSalary + Perquisites + Profits 
- AllowancesExempt(U/s 10) 
- StdDeduction (New: ₹75,000, Old: ₹50,000)  
- EntertainmentAllowance  
- ProfessionalTax  
= IncomeFromSal
```

**File:** `backend/app/core/domain/schedules/salary.py` ✅ EXISTS

---

### ScheduleHP — House Property  
**ITD JSON Path:** `ScheduleHP` → `PropertyDtls[]`  
**Used In:** ITR-1, ITR-2, ITR-3, ITR-4  
**OIDAR Section:** Sec 22-27

| Field | Type | Required | Limits |
|-------|------|----------|--------|
| TypeOfHP | enum | Yes | SelfOccupied, LetOut, DeemedLetOut |
| Address | JSON | Yes | Line1, City, State, Pin |
| GrossRentReceived | integer | No | Annual rent |
| TaxPaidLocalAuthority | integer | No | Municipal tax |
| AnnualValue | integer | Auto | GRR - Municipal Tax |
| StandardDeduction30Percent | integer | Auto | 30% of NAV |
| InterestPayable | integer | No | Housing loan interest |
| ArrearsUnrealizedRent | integer | No | Sec 25A arrears |
| TotalIncomeOfHP | integer | Auto | Capped at -₹2,00,000 |

**Computation:**
```
NAV = GrossRent - MunicipalTax  
30% StdDed = 30% of NAV  
HP Income = NAV - StdDed - Interest - Arrears  
Loss cap = max(HP Income, -₹200,000)
```

**File:** `backend/app/core/domain/schedules/house_property.py` ✅ EXISTS

---

### ScheduleOS — Other Sources
**ITD JSON Path:** `ScheduleOS` → `OtherSrcDtls[]`  
**Used In:** ITR-1, ITR-2, ITR-3, ITR-4  
**OIDAR Section:** Sec 56-59

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| SourceType | enum | Yes | SAV, IFD, TAX, FAP, DIV, NOT89A, OTH |
| SourceDesc | string | No | Custom description |
| Amount | integer | Yes | Gross amount |
| TDS | integer | No | TDS deducted |
| DeductionUs57 | integer | No | Only for Family Pension: min(₹15,000 or 1/3) |

**File:** `backend/app/core/domain/schedules/other_sources.py` ✅ EXISTS

---

### ScheduleVIA — Chapter VI-A Deductions
**ITD JSON Path:** `ScheduleVIA` → `DeductUndChapVIA[]`  
**Used In:** ITR-1, ITR-2, ITR-3, ITR-4  
**OIDAR Section:** Chapter VI-A

| Section | Description | Max Limit | Notes |
|---------|-------------|-----------|-------|
| 80C | LIC, PPF, ELSS, Tuition, etc. | ₹1,50,000 | Combined with 80CCC, 80CCD(1) |
| 80CCC | Annuity Pension | ₹1,50,000 | Included in ₹1.5L 80C cap |
| 80CCD(1) | NPS Employee/SE | ₹1,50,000 | Included in ₹1.5L 80C cap |
| 80CCD(1B) | Additional NPS | ₹50,000 | Additional to 80C (NOT in ₹1.5L cap) |
| 80CCD(2) | Employer NPS | No limit | New regime allowed |
| 80D | Health Insurance | ₹25K/₹50K/₹1L | Self 25K + Sr 50K + Parents 25K/50K |
| 80DD | Dependent Disability | ₹75K/₹1.25L | Normal/Severe |
| 80DDB | Medical Treatment | ₹40K/₹1L | Normal/Senior |
| 80E | Education Loan Interest | No limit | Max 8 years |
| 80EE | First Home Loan | ₹50,000 | Conditions apply |
| 80EEA | First Home Loan (Affordable) | ₹1,50,000 | Stamp duty ≤₹45L |
| 80EEB | Electric Vehicle Loan | ₹1,50,000 | Vehicle conditions |
| 80G | Donations | Variable | 50% or 100% eligible |
| 80GG | Rent Paid | ₹60,000/yr | No HRA |
| 80GGA | Scientific Research Donations | No limit | Only if no business income |
| 80GGC | Political Party Donations | No limit | Only if no business income |
| 80TTA | Savings Interest | ₹10,000 | Non-senior |
| 80TTB | Interest (Senior) | ₹50,000 | Age ≥ 60 |
| 80U | Self Disability | ₹75K/₹1.25L | Normal/Severe |

**New Regime Notes:** Only 80CCD(2) and 80JJAA (employment) apply.

**File:** `backend/app/core/domain/schedules/schedule_via.py` ✅ EXISTS

---

### ScheduleTDS — Tax Deducted at Source
**ITD JSON Path:** ITR-1: `TDSonSalaries`/`TDSonOthThanSals`, ITR-2: `ScheduleTDS1`/`TDS2`/`TDS3`  
**Used In:** ITR-1, ITR-2, ITR-3, ITR-4

| Field | Type | Required |
|-------|------|----------|
| DeductorTAN | string | Yes |
| DeductorName | string | Yes |
| TDSSection | enum | Yes (192, 194A, 194C, etc.) |
| AmountPaid/Credited | integer | Yes |
| TaxDeducted | integer | Yes |
| TaxDeposited | integer | Yes |
| DeductedYear | string | Yes (e.g. "2025" = AY 2025-26) |

**File:** `backend/app/core/domain/schedules/tds.py` ✅ EXISTS

---

### ScheduleIT — Tax Payments (Advance Tax / Self-Assessment Tax)
**ITD JSON Path:** `ScheduleIT`  
**Used In:** ITR-1, ITR-2, ITR-3, ITR-4

| Field | Type | Required |
|-------|------|----------|
| BSRCode | string | Yes (7 chars) |
| DateDep | date | Yes (YYYY-MM-DD) |
| ChallanSerialNo | integer | Yes |
| Amount | integer | Yes |
| Type | enum | AdvanceTax, SelfAssessmentTax |

**File:** `backend/app/core/domain/schedules/schedule_it.py` ✅ EXISTS

---

### ScheduleBA — Bank Accounts
**ITD JSON Path:** ITR-1: `Refund.BankAccountDtls`, ITR-2+: `ScheduleBA`  
**Used In:** All ITR forms (ITR-1 as part of Refund)

| Field | Type | Required |
|-------|------|----------|
| BankName | string | Yes |
| IFSCCode | string | Yes (11 chars) |
| AccountNumber | string | Yes |
| AccountType | enum | SB, CA, CC, OD, NRO, OTH |
| UseForRefund | boolean | Yes |

**File:** `backend/app/core/domain/schedules/schedule_ba.py` ✅ EXISTS

---

### ScheduleCG — Capital Gains (ITR-2, ITR-3 for full CG)
**ITD JSON Path:** `ScheduleCGFor23`, `Schedule112A`, `Schedule115AD`, `ScheduleVDA`  
**ITR-1 / ITR-4:** `LTCG112A` ONLY (listed equity LTCG u/s 112A, max ₹1,25,000 exempt)  
**Used In:** ALL forms (ITR-1/4=112A only, ITR-2/3=full CG)

| Sub-Schedule | ITR Forms | OIDAR | Rate | Notes |
|--------------|-----------|-------|------|-------|
| LTCG112A | ITR-1,2,3,4 | 112A | 12.5% | Listed equity LTCG (>₹1.25L exempt) |
| ScheduleCGFor23 | ITR-2,3 | 45-55A | Varies | STCG+LTCG all other assets |
| Schedule112A | ITR-2,3 | 112A | 12.5% | Same as LTCG112A but detailed schedule |
| Schedule115AD | ITR-2,3 | 115AD | 30%/10% | FII capital gains |
| ScheduleVDA | ITR-2,3 | 115BBH | 30% | VDA/Crypto |
| ScheduleDCG | ITR-3 | 50 | Slab | Deemed CG on depreciable assets |

---

## 2.2 EXTENDED SCHEDULES (ITR-2, ITR-3, ITR-4)

### ScheduleBP — Business / Profession (ITR-3, ITR-4)
**ITD Path:** `ScheduleBP`  
**Used In:** ITR-3 (non-presumptive), ITR-4 (presumptive)

ITR-4 (Presumptive):
```
ScheduleBP: {
  BusinessDetails: [
    {
      NatureOfBusiness, BusinessCode,
      GrossTurnover, PresumptiveRate (6%/8%),
      PresumptiveIncome,
      DisallowedExpenses, Depreciation
    }
  ]
}
```

ITR-3 (Non-presumptive):
```
ManufacturingAccount → TradingAccount → PARTA_PL → PARTA_BS
ScheduleDPM (Depreciation Plant & Machinery)
ScheduleDOA (Depreciation Other Assets)
ScheduleDEP (Depreciation Summary)
ScheduleDCG (Deemed CG on sale of asset)
ScheduleESR (Expenditure on Scientific Research)
```

### ScheduleScheduleVIARestricted — AMT (ITR-2, ITR-3)
**ITD Path:** `ScheduleAMT`/`ScheduleAMTC`  
Used for Alternate Minimum Tax computation.

### ScheduleEI — Exempt Income (ITR-2, ITR-3)
**ITD Path:** `ScheduleEI`  
Income exempt u/s 10 (agriculture >₹5000, PPF, etc.)

### ScheduleFA — Foreign Assets (ITR-2, ITR-3)
**ITD Path:** `ScheduleFA`  
Foreign bank accounts, assets, trusts, signing authority.

### ScheduleAL — Assets & Liabilities (ITR-2, ITR-3)
**ITD Path:** `ScheduleAL`  
Required when Total Income > ₹50L.

### ScheduleFSI — Foreign Source Income (ITR-2, ITR-3)
### ScheduleTR1 — Foreign Tax Relief (ITR-2, ITR-3)
### ScheduleSI — Special Income (ITR-2, ITR-3)
### ScheduleIF — Income from Firm (ITR-3)
### SchedulePTI — Profit from Transfer of Immovable Property (ITR-3)
### ScheduleTPSA — Tax on Presumptive Special Assets (ITR-3)
### Schedule10AA — SEZ Deduction (ITR-3)
### Schedule80_IA/IB/IC — Infrastructure Deductions (ITR-3)
### Schedule5A2014 — Grandfathering Disclosure (ITR-2, ITR-3)
### ScheduleCYLA/BFLA/CFL — Loss Setoff & Carry Forward
**ITD Path:** `ScheduleCYLA`, `ScheduleBFLA`, `ScheduleCFL`
All ITR forms except ITR-1.

---

## 2.3 DEDUCTION SCHEDULES (All ITR Forms)

These are detailed sub-schedules that populate ScheduleVIA aggregates:

| Schedule | Sections | Fields Required |
|----------|----------|----------------|
| Schedule80C | 80C | InvestmentType, Amount, ReferenceNo, DateOfInvestment |
| Schedule80D | 80D | InsurerName, PolicyNo, Premium, CoverType, Age |
| Schedule80DD | 80DD | DisabilityNature, Type, PAN/Aadhaar, Form10IA |
| Schedule80U | 80U | DisabilityNature, Type, Form10IA, UDID |
| Schedule80E | 80E | BankName, LoanAcctNo, LoanDate, Amount, Interest |
| Schedule80EE | 80EE | Same as 80E but loan cap ₹35L |
| Schedule80EEA | 80EEA | Same as 80EE but stamp duty cap ₹45L |
| Schedule80EEB | 80EEB | Same as 80EE + VehicleRegNo |
| Schedule80G | 80G | DoneeName, DoneePAN, Amount, DonationType, Address |
| Schedule80GGA | 80GGA | DoneeName, DoneePAN, Clause, Address |
| Schedule80GGC | 80GGC | DoneeName, DoneePAN, Amount |
| ScheduleUs24B | 24B | LoanFromType, BankName, DateOfLoan, Interest |

**All Deduction Schedules Status:** ❌ NOT YET IMPLEMENTED AS DOMAIN MODELS

---

# 3. INCOME HEAD COMPUTATION FLOW

```
┌─────────────────────────────────────────────────────────────────────┐
│                      TAX ENGINE COMPUTE FLOW                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  STEP 1: EXTRACT SCHEDULES FROM FILING                              │
│  ┌──────────────┐ ┌───────────┐ ┌──────────┐ ┌──────────┐         │
│  │ ScheduleS    │ │ ScheduleHP │ │ScheduleOS│ │ScheduleCG│         │
│  └──────┬───────┘ └─────┬─────┘ └────┬─────┘ └────┬─────┘         │
│         │               │            │            │                │
│  STEP 2: COMPUTE INCOME HEADS                                       │
│         ▼               ▼            ▼            ▼                │
│  ┌──────────┐  ┌───────────┐ ┌──────────┐ ┌──────────────┐        │
│  │Salary    │  │HP Income  │ │OS Income │ │CG Income     │        │
│  │Income    │  │(capped    │ │          │ │(STCG + LTCG  │        │
│  │(Net)     │  │@-₹2L)     │ │          │ │+ Special)    │        │
│  └────┬─────┘  └─────┬─────┘ └────┬─────┘ └──────┬───────┘        │
│       │              │            │              │                 │
│  STEP 3: GROSS TOTAL INCOME (GTI)                                   │
│       └──────────────┴────────────┴──────────────┘                 │
│                          │                                          │
│                          ▼                                          │
│                   ┌─────────────┐                                   │
│                   │    GTI      │  Sum of all income heads          │
│                   └──────┬──────┘                                   │
│                          │                                          │
│  STEP 4: DEDUCTIONS (Old Regime Only)                               │
│                          ▼                                          │
│                   ┌─────────────┐                                   │
│                   │ ScheduleVIA │  80C, 80D, 80E, 80G, etc.         │
│                   └──────┬──────┘                                   │
│                          │                                          │
│                          ▼                                          │
│                   ┌─────────────┐                                   │
│                   │Total Income │  GTI - Deductions (≥0)            │
│                   └──────┬──────┘                                   │
│                          │                                          │
│  STEP 5: CG SEPARATION                                              │
│       ┌──────────────────┴───────────────────┐                     │
│       ▼                                      ▼                      │
│  ┌─────────┐                           ┌──────────────┐             │
│  │ Normal   │                           │ CG Special   │             │
│  │ Income   │  Salary+HP+OS+STCG(slab) │ Rate Income  │ 111A,112A  │
│  │ (Non-CG) │                           │(CG + Lottery │ 112,115BB  │
│  └────┬────┘                           │+ VDA + Unexp)│ 115BBH,BBE │
│       │                                └──────┬───────┘             │
│       ▼                                       ▼                      │
│  ┌─────────┐                           ┌──────────────┐             │
│  │Slab Tax │                           │Special Rate  │             │
│  │+Rebate  │                           │Tax (12.5%,   │             │
│  │+Surchrg │                           │15%,20%,30%,  │             │
│  │+Cess    │                           │60%)          │             │
│  └────┬────┘                           └──────┬───────┘             │
│       │                                       │                      │
│       └───────────────────┬───────────────────┘                     │
│                           ▼                                          │
│  STEP 6: TOTAL TAX LIABILITY                                        │
│                    ┌───────────────┐                                │
│                    │ TOTAL TAX     │ Slab Tax + CG Special Tax      │
│                    │ + Surcharge   │ (if >₹50L: 10%/15%/25%/37%)   │
│                    │ + Cess @ 4%   │                                │
│                    └───────┬───────┘                                │
│                            │                                        │
│  STEP 7: TAX CREDITS                                                │
│                            ▼                                        │
│                    ┌───────────────┐                                │
│                    │ TDS + TCS +   │ ScheduleTDS + ScheduleTCS      │
│                    │ Advance Tax   │ + ScheduleIT                   │
│                    └───────┬───────┘                                │
│                            │                                        │
│  STEP 8: NET TAX PAYABLE / REFUND                                   │
│                            ▼                                        │
│                    ┌───────────────┐                                │
│                    │ Tax Payable   │ TotalTax - Credits             │
│                    │ or Refund     │ + Interest 234A/B/C            │
│                    └───────────────┘                                │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

# 4. CG RATE BUCKETS — COMPLETE

```
┌───────────────────────────────────────────────────────────────────────┐
│          CAPITAL GAINS TAX RATES — ALL ITR FORMS                      │
├────────────────────┬────────┬────────┬───────────┬────────────────────┤
│ SECTION            │ TERM   │ RATE   │ EXEMPTION │ ITR FORMS          │
├────────────────────┼────────┼────────┼───────────┼────────────────────┤
│ 112A               │ LTCG   │ 12.5%  │ ₹1.25L    │ ALL: ITR-1/2/3/4   │
│                    │ Listed │        │ exempt    │ (LTCG112A schema)  │
├────────────────────┼────────┼────────┼───────────┼────────────────────┤
│ 111A               │ STCG   │ 15%    │ None      │ ITR-2/3 only        │
│                    │ Listed │        │           │ (ScheduleCGFor23)  │
├────────────────────┼────────┼────────┼───────────┼────────────────────┤
│ 111A (pre 1-Oct04) │ STCG   │ 20%    │ None      │ ITR-2/3 only        │
├────────────────────┼────────┼────────┼───────────┼────────────────────┤
│ NORMAL             │ STCG   │ Slab   │ None      │ ITR-2/3 only        │
│                    │ Non-   │ rates  │           │ (ScheduleCGFor23)  │
│                    │ listed │        │           │                    │
├────────────────────┼────────┼────────┼───────────┼────────────────────┤
│ 112 (indexed)      │ LTCG   │ 20%    │ None      │ ITR-2/3 only        │
│                    │ Other  │        │           │ (ScheduleCGFor23)  │
├────────────────────┼────────┼────────┼───────────┼────────────────────┤
│ 112 (no indexation) │ LTCG  │ 12.5%  │ None      │ ITR-2/3 only        │
│                    │ Bonds  │        │           │ (ScheduleCGFor23)  │
├────────────────────┼────────┼────────┼───────────┼────────────────────┤
│ 115AD(1)(iii)      │ STCG   │ 30%    │ None      │ ITR-2/3 only        │
│                    │ FII    │        │           │ (Schedule115AD)    │
├────────────────────┼────────┼────────┼───────────┼────────────────────┤
│ 115AD(1)(iii)b     │ LTCG   │ 10%    │ None      │ ITR-2/3 only        │
│                    │ FII    │        │           │ (Schedule115AD)    │
├────────────────────┼────────┼────────┼───────────┼────────────────────┤
│ 115BBH             │ VDA/   │ 30%    │ None      │ ITR-2/3 only        │
│                    │ Crypto │        │           │ (ScheduleVDA)      │
├────────────────────┼────────┼────────┼───────────┼────────────────────┤
│ 115BBE             │Unexpl. │ 60%    │ None      │ ITR-2/3 only        │
│                    │ income │        │           │ (ScheduleCGFor23)  │
├────────────────────┼────────┼────────┼───────────┼────────────────────┤
│ DCG (50)           │ Deemed │ Slab   │ None      │ ITR-3 only          │
│                    │ CG     │ rates  │           │ (ScheduleDCG)      │
└────────────────────┴────────┴────────┴───────────┴────────────────────┘
```

---

# 5. COMPARISON: WHAT EXISTS vs WHAT'S NEEDED

| Schedule | Domain Model | Tax Engine Integration | ITR JSON Builder | Status |
|----------|:---:|:---:|:---:|--------|
| ScheduleSalary | ✅ | ✅ Full | ⏳ Needed | PHASE 2 COMPLETE |
| ScheduleHP | ✅ | ✅ Full | ⏳ Needed | PHASE 2 COMPLETE |
| ScheduleOS | ✅ | ✅ Full | ⏳ Needed | PHASE 2 COMPLETE |
| ScheduleCG | ✅ | 🟡 Partial | ⏳ Needed | PHASE 3 IN PROGRESS |
| ScheduleVIA | ✅ | ✅ Core (80C/80D/80E) | ⏳ Needed | PHASE 2 COMPLETE |
| ScheduleTDS | ✅ | ✅ Core | ⏳ Needed | PHASE 2 COMPLETE |
| ScheduleIT | ✅ | ✅ Core | ⏳ Needed | PHASE 2 COMPLETE |
| ScheduleBA | ✅ | ⏳ N/A (metadata) | ⏳ Needed | PHASE 1 COMPLETE |
| ScheduleBP | ❌ | ❌ | ❌ | PHASE 5 (ITR-4) |
| ScheduleTCS | ❌ | ❌ | ❌ | PHASE 4 |
| Detailed Deduction Schedules (80G, 80E, etc.) | ❌ | ❌ | ❌ | PHASE 5 |
| ScheduleEI | ❌ | ❌ | ❌ | PHASE 6 |
| ScheduleCYLA/BFLA/CFL | ❌ | ❌ | ❌ | PHASE 6 |
| ScheduleFA/AL | ❌ | ❌ | ❌ | PHASE 6 |
| ScheduleAMT/AMTC | ❌ | ❌ | ❌ | PHASE 6 |
| ITR JSON Generation | ❌ | N/A | ❌ | PHASE 7 |

---

# 6. PHASE 3 (CURRENT): CG COMPLETE IMPLEMENTATION

## 6.1 Define CapitalGainsBreakdown

```python
@dataclass
class CapitalGainsBreakdown:
    """Complete CG breakdown per ITD schema."""
    # ── STCG ──
    stcg_total: int = 0
    stcg_111a_15_pct: int = 0       # Listed equity STT paid
    stcg_111a_20_pct: int = 0       # Listed equity no STT
    stcg_applicable_rate: int = 0   # Non-listed (taxed at slab)
    
    # ── LTCG ──
    ltcg_total: int = 0
    ltcg_112a_12_5_pct: int = 0     # Listed equity (₹1.25L exempt)
    ltcg_112a_exempt: int = 0       # ₹1.25L exemption
    ltcg_112_20_pct: int = 0        # Other assets with indexation
    ltcg_112_12_5_pct: int = 0      # Bonds (without indexation)
    ltcg_112_proviso_credit: int = 0 # BEL shortfall credit
    
    # ── Special Rate ──
    lottery_30: int = 0             # 115BB
    online_gaming_30: int = 0       # 115BB(2)
    vda_30: int = 0                 # 115BBH
    unexplained_60: int = 0         # 115BBE
    
    # ── Computed ──
    cg_total: int = 0              # STCG + LTCG + Special
    cg_tax: int = 0                # Total special rate tax
    rate_buckets: List[CGRateBucket] = field(default_factory=list)
```

## 6.2 CG Integration into TaxEngine

1. Extract ScheduleCG from schedules list
2. Call `schedule_cg.get_cg_breakdown()` → CapitalGainsBreakdown
3. Normal income for slab = GTI_other + STCG_slab_rate (not special rate CG)
4. CG special rate tax = sum of all CG rate bucket taxes
5. BEL for surcharge = total_income + LTCG_special_rate
6. Rebate 87A only applies to slab tax (NOT CG special rate tax)
7. Total Tax = slab_tax + cg_special_rate_tax + surcharge + cess

## 6.3 Required CG Methods (IN ScheduleCG)

```python
def get_cg_breakdown(self) -> CapitalGainsBreakdown: ...
def compute_cg_tax(self) -> int: ...
def compute_surcharge_base(self, normal_income: int) -> int: ...
def to_itr_json(self) -> dict: ...  # ScheduleCGFor23 + Schedule112A + ScheduleVDA
```

---

# 7. REVISED PHASE PLAN

| Phase | Scope | Status | Key Deliverables |
|-------|-------|--------|-----------------|
| **Phase 0** | Repository Cleanup | ✅ COMPLETE | FK constraints, unique indexes, dead code removal |
| **Phase 1** | OpenTax Vendoring | ✅ COMPLETE | Reference code vendored, 69 import fix, test oracle |
| **Phase 2** | Owned Tax Engine | ✅ COMPLETE | TaxEngine, SlabTables, Interest234, 37 tests pass |
| **Phase 3** | Capital Gains Complete | 🔴 IN PROGRESS | CG breakdown, special rate tax, BEL, all 10 CG sections |
| **Phase 4** | TCS + Full TDS Schedule | ⏳ PENDING | ScheduleTCS, TDS multi-form support |
| **Phase 5** | ScheduleBP + Presumptive | ⏳ PENDING | ITR-4 business income, 44AD/ADA/AE |
| **Phase 6** | Loss Setoff + Carry Forward | ⏳ PENDING | CYLA, BFLA, CFL, inter-head setoff |
| **Phase 7** | ITR JSON Generation | ⏳ PENDING | Full JSON builder for ITR-1 through ITR-4 |
| **Phase 8** | Foreign Assets (ScheduleFA) | ⏳ PENDING | ITR-2/3 FA, TR, FSI schedules |
| **Phase 9** | Full Validation Engine | ⏳ PENDING | All ITD validation rules per schema |

---

## 8. IMMEDIATE PHASE 3 ACTION ITEMS

1. 📝 **Fix CGTransaction field ordering** (non-default before default fields)
2. 📝 **Add `CapitalGainsBreakdown` to computed_return.py**
3. 📝 **Replace `CGIncomeDetail` in tax_engine.py** with CapitalGainsBreakdown
4. 📝 **Add `bel_threshold` to RuleVersion** (slab_tables.py)
5. 📝 **Fix surcharge to use BEL + CG LTCG**
6. 📝 **Fix Rebate 87A to apply only to slab tax** (NOT CG special rate tax)
7. 📝 **Fix `" unexplained"` typo → `"unexplained"` in ScheduleCG**
8. 📝 **Add ScheduleTCS domain model**
9. 📝 **Add 8 CG test cases:**
   - LTCG 112A with ₹1.25L exemption boundary
   - STCG 111A combined with salary income
   - Multiple CG transactions across rate buckets
   - CG loss (no gain scenario)
   - BEL shortfall with LTCG
   - Lottery 115BB @ 30%
   - VDA 115BBH @ 30%
   - Surcharge triggered by CG
  
---

*End of Planning Document*
*Next: Phase 3 Implementation begins after this plan review*
