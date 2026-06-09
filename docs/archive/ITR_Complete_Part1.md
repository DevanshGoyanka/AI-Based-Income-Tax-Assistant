# INCOME TAX ERP — ITR FIELD GUIDE & CALCULATION REFERENCE (PART 1)
## ITR-1 | ITR-2 | ITR-3 | ITR-4
### Assessment Year 2025-26 (FY 2024-25) & Assessment Year 2026-27 (FY 2025-26)
### Sections 1–5: Tax Rates, Interest, ITR-1, ITR-2, Salary Deep Dive

> **LEGAL BASIS:** Finance Act 2024 (Budget 2024, effective AY 2025-26), Finance Act 2025 (Budget 2025, effective AY 2026-27), Income-tax Act 1961, CBDT Circulars, ITD Schema Specifications. Always verify against the latest CBDT notifications before implementation.

---

## SECTION 1 — TAX RATE REFERENCE TABLES

### 1.1 Old Tax Regime — AY 2025-26 & AY 2026-27

Old regime slabs are UNCHANGED between both AYs. Budget 2025 made NO changes to old regime.

**Individual below 60 years**

| Income Slab | Tax Rate | Tax on Slab (Max) |
|---|---|---|
| Up to ₹2,50,000 | NIL | — |
| ₹2,50,001 to ₹5,00,000 | 5% | ₹12,500 |
| ₹5,00,001 to ₹10,00,000 | 20% | ₹1,00,000 |
| Above ₹10,00,000 | 30% | On balance |

**Senior Citizen (60 years or more but less than 80 years)**

| Income Slab | Tax Rate | Tax on Slab (Max) |
|---|---|---|
| Up to ₹3,00,000 | NIL | — |
| ₹3,00,001 to ₹5,00,000 | 5% | ₹10,000 |
| ₹5,00,001 to ₹10,00,000 | 20% | ₹1,00,000 |
| Above ₹10,00,000 | 30% | On balance |

**Super Senior Citizen (80 years and above)**

| Income Slab | Tax Rate | Tax on Slab (Max) |
|---|---|---|
| Up to ₹5,00,000 | NIL | — |
| ₹5,00,001 to ₹10,00,000 | 20% | ₹1,00,000 |
| Above ₹10,00,000 | 30% | On balance |

> **AGE DETERMINATION RULE (CRITICAL):** Age is computed as on the FIRST DAY of the Financial Year (April 1) of the relevant FY. For AY 2025-26 → age as on April 1, 2024. For AY 2026-27 → age as on April 1, 2025. If a taxpayer turns 60 on April 2, 2024, they are NOT a senior citizen for AY 2025-26. The ERP must compute age = DOB to April 1 of FY in completed years.

---

### 1.2 New Tax Regime (Section 115BAC) — AY 2025-26

The new regime is the **DEFAULT** from AY 2024-25. Taxpayers must explicitly opt out to use old regime. No age-based distinction exists in the new regime — all individuals (below 60, senior, super senior) use the same slab.

| Income Slab | Tax Rate | Cumulative Tax |
|---|---|---|
| Up to ₹3,00,000 | NIL | ₹0 |
| ₹3,00,001 to ₹7,00,000 | 5% | Up to ₹20,000 |
| ₹7,00,001 to ₹10,00,000 | 10% | Up to ₹50,000 |
| ₹10,00,001 to ₹12,00,000 | 15% | Up to ₹80,000 |
| ₹12,00,001 to ₹15,00,000 | 20% | Up to ₹1,40,000 |
| Above ₹15,00,000 | 30% | On balance above ₹15L |

**Rebate u/s 87A — New Regime — AY 2025-26:** If total income ≤ ₹7,00,000 → tax fully rebated (max rebate ₹25,000). Cliff applies: ₹7,00,001 → NO rebate. Rebate NOT on STCG u/s 111A, LTCG u/s 112A, or any other special rate income. Rebate applies only to normal slab tax.

---

### 1.3 New Tax Regime (Section 115BAC) — AY 2026-27

Finance Act 2025 substantially revised slabs. **Implement separately in computation engine.**

| Income Slab | Tax Rate | Cumulative Tax |
|---|---|---|
| Up to ₹4,00,000 | NIL | ₹0 |
| ₹4,00,001 to ₹8,00,000 | 5% | Up to ₹20,000 |
| ₹8,00,001 to ₹12,00,000 | 10% | Up to ₹60,000 |
| ₹12,00,001 to ₹16,00,000 | 15% | Up to ₹1,20,000 |
| ₹16,00,001 to ₹20,00,000 | 20% | Up to ₹2,00,000 |
| ₹20,00,001 to ₹24,00,000 | 25% | Up to ₹3,00,000 |
| Above ₹24,00,000 | 30% | On balance above ₹24L |

**Rebate u/s 87A — New Regime — AY 2026-27:** If total income ≤ ₹12,00,000 → tax = NIL (max rebate ₹60,000). CRITICAL: Income of ₹12,00,001 → NO rebate, full tax ≈ ₹61,500. Special rate income (STCG u/s 111A, LTCG u/s 112A, VDA u/s 115BBH) is EXCLUDED from the ₹12L rebate threshold test. ERP MUST implement marginal relief for ₹12L cliff (see Section 1.6).

**Rebate u/s 87A — Old Regime — Both AYs:** If total income ≤ ₹5,00,000 → rebate = actual tax payable (max ₹12,500). Applicable for both AY 2025-26 and 2026-27.

---

### 1.4 Surcharge — Both AYs

| Total Income Range | Surcharge % (Old Regime) | Surcharge % (New Regime) |
|---|---|---|
| Up to ₹50,00,000 | NIL | NIL |
| ₹50,00,001 to ₹1,00,00,000 | 10% | 10% |
| ₹1,00,00,001 to ₹2,00,00,000 | 15% | 15% |
| ₹2,00,00,001 to ₹5,00,00,000 | 25% | 25% |
| Above ₹5,00,00,000 | 37% | **25% (CAPPED — Budget 2023)** |

> **SURCHARGE ON SPECIAL RATE INCOME:** For LTCG u/s 112A and STCG u/s 111A — surcharge is CAPPED at 15% regardless of total income level. For other capital gains and special rate income — normal surcharge rates apply.

---

### 1.5 Marginal Relief on Surcharge — Complete Algorithm

The ERP **MUST** compute marginal relief at EVERY surcharge threshold. Formula:

```
MARGINAL RELIEF COMPUTATION ALGORITHM:
For each threshold T in [50L, 1Cr, 2Cr, 5Cr]:
  Tax_with_surcharge = Tax_on_TI + (Surcharge_rate × Tax_on_TI)
  Tax_on_threshold = Tax computed on T (using applicable slabs)
  Income_excess = Total_Income - T
  Maximum_Tax_Payable = Tax_on_threshold + Income_excess
  
  If Tax_with_surcharge > Maximum_Tax_Payable:
    Marginal_Relief = Tax_with_surcharge - Maximum_Tax_Payable
    Effective_Tax = Maximum_Tax_Payable
  Else:
    Marginal_Relief = 0

Apply marginal relief that MAXIMIZES relief to the taxpayer.
```

**Example:** Income = ₹51,00,000. Old regime, below 60.
- Tax on ₹51L = ₹2,50,000 (basic) + 5%×(5L-2.5L) + 20%×(5L) + 30%×(41L) = ₹12,500 + ₹1,00,000 + ₹12,30,000 = ₹13,42,500
- Surcharge @ 10% = ₹1,34,250 → Total = ₹14,76,750
- Tax on ₹50L = ₹12,50,000 + ₹1,00,000 = wait: Tax on ₹50L = ₹12,500 + ₹1,00,000 + ₹12,00,000 = ₹13,12,500 (no surcharge threshold applies)
- Excess income = ₹1,00,000
- Max payable = ₹13,12,500 + ₹1,00,000 = ₹14,12,500
- Since ₹14,76,750 > ₹14,12,500 → Marginal Relief = ₹64,250
- Effective tax before cess = ₹14,12,500

**AY 2026-27 New Regime ₹12L Cliff — Marginal Relief:**
```
If income slightly exceeds ₹12,00,000 (new regime AY 26-27):
  Tax_payable_without_rebate = full slab tax on income
  Since no rebate available for income > 12L:
  Marginal_relief = max(0, Tax_payable - (Income - 12,00,000))
  This ensures tax does not exceed income above ₹12L
```

---

### 1.6 Health and Education Cess

**4%** of (Income Tax after 87A rebate + Surcharge after marginal relief).

- No exemption or concession on cess
- Cess computed AFTER 87A rebate and AFTER surcharge marginal relief
- Cess rounded to nearest ₹1
- Applied in ALL cases — resident, non-resident, any regime, any income

---

### 1.7 Special Rate Tax — Section-wise

These incomes are taxed at FLAT rates regardless of slab. They do NOT benefit from the basic exemption limit. However, in computation: basic exemption is first utilized against normal income; the balance exemption (if any) is adjusted against special rate income.

| Section | Income Type | Rate (Both AYs) | Surcharge Cap |
|---|---|---|---|
| 111A | STCG on listed equity/equity MF (STT paid) | 15% (pre Jul 23, 2024) / 20% (post) | 15% |
| 112 | LTCG on other assets (with/without indexation) | 20% with idx / 12.5% without | Normal |
| 112A | LTCG on listed equity/equity MF (STT paid) above ₹1L/₹1.25L | 10% (pre Jul 23) / 12.5% (post) | 15% |
| 115BB | Winnings from lottery, crossword, horse race, card game, gambling | 30% | Normal |
| 115BBH | Income from transfer of VDA (Crypto/NFT) | 30% | Normal |
| 115BBE | Unexplained cash credits (68), investments (69), expenditure (69A/B/C), deemed gifts (69D) | **60%** + 25% surcharge on tax = effective **78%** + 4% cess | No relief |
| 115BBDA | Aggregate dividend > ₹10L from domestic companies (pre-AY 2021-22 only; now all dividend taxable at slab) | N/A from AY 2021-22 | — |

> **SECTION 115BBH — VDA RULES (CRITICAL):**
> - Applicable from AY 2023-24 onwards
> - 30% flat tax on gains from transfer of Virtual Digital Assets
> - VDA defined u/s 2(47A): any code/number/token generated through cryptographic means; NFTs; any other digital asset notified
> - NO deduction allowed EXCEPT cost of acquisition
> - CANNOT set off VDA loss against any other income (not even other VDA income)
> - CANNOT carry forward VDA loss
> - Gift of VDA to non-relative taxable in recipient's hands u/s 56(2)(x)
> - TDS u/s 194S @ 1% by specified persons (exchanges); threshold ₹10,000/year (₹50,000 for specified persons)
> - Report in Schedule VDA in ITR-2/3/4; also in Schedule SI

> **SECTION 115BBE — UNEXPLAINED INCOME (VERY CRITICAL):**
> - If AO adds income u/s 68 (unexplained cash credit), 69 (unexplained investment), 69A (undisclosed money), 69B (under-reported investment), 69C (unexplained expenditure), 69D (deemed gifts)
> - Tax = 60% flat + 25% surcharge on the 60% tax = effectively **78%** of income + 4% cess on 78% = **81.12% effective rate**
> - NO deduction, exemption, or set-off allowed
> - No benefit of basic exemption
> - ERP must flag any income declared under these sections automatically

> **LOTTERY / CASUAL INCOME u/s 115BB:**
> - Any winning from lottery, crossword puzzle, horse race, card game, gambling or betting
> - 30% flat, no basic exemption
> - TDS u/s 194B @ 30% if winning > ₹10,000 per transaction
> - For online gaming: TDS u/s 194BA @ 30% on NET winnings per withdrawal (Finance Act 2023)
> - Losses from gambling NOT deductible (u/s 58(4))

---

### 1.8 Alternate Minimum Tax (AMT) — Section 115JC

**Applicable to:** Individuals/HUFs/firms/persons (NOT companies — those have MAT u/s 115JB) who claim specified deductions and their adjusted total income exceeds ₹20,00,000.

**AMT Rate:** 18.5% of Adjusted Total Income (+ surcharge + cess)

**Adjusted Total Income (ATI) Computation:**

```
ATI = Total Income (as computed in ITR)
    + Deduction claimed u/s 10AA (SEZ)
    + Deduction claimed u/s 35AD (Specified Business)
    + Deduction claimed u/s 80H to 80RRB (except 80P)
```

**AMT Trigger:** If Income Tax payable < AMT on ATI AND ATI > ₹20,00,000 → Pay AMT.

**AMT Credit u/s 115JD:** AMT paid in excess of regular tax can be carried forward for 15 years and set off in a year when regular tax > AMT.

| AMT Field | Description |
|---|---|
| Schedule AMT | Compute ATI, AMT @ 18.5% |
| Schedule AMTC | AMT credit b/f, AMT credit utilized this year, AMT credit c/f |
| Applicable ITR | ITR-2 (if deductions claimed), ITR-3, ITR-4 (if Sec 35AD/10AA) |

> **AMT NOT APPLICABLE IN NEW REGIME:** Since new regime allows very few deductions, AMT rarely triggers. However, if taxpayer is under old regime with SEZ or 35AD deductions and ATI > ₹20L, ERP must compute AMT.

---

### 1.9 Standard Deduction — Both AYs

| Regime | AY 2025-26 | AY 2026-27 | Note |
|---|---|---|---|
| New Regime — Salaried | ₹75,000 | ₹75,000 | Increased from ₹50,000 by Budget 2024 |
| Old Regime — Salaried | ₹50,000 | ₹50,000 | Unchanged |
| New Regime — Family Pension | ₹25,000 or 1/3 of pension (lower) | ₹25,000 or 1/3 (lower) | Introduced AY 2025-26 |
| Old Regime — Family Pension | ₹15,000 or 1/3 of pension (lower) | ₹15,000 or 1/3 (lower) | Unchanged |

---

### 1.10 New Regime — Permissible Deductions/Exemptions (Complete List)

The following ARE available in the new regime (all others are NOT):

| Item | Section | Limit |
|---|---|---|
| Standard deduction from salary | 16(ia) | ₹75,000 |
| Standard deduction from family pension | 57(iia) | ₹25,000 or 1/3 |
| Entertainment allowance deduction | 16(ii) | Govt employees only — lesser of 1/5 salary or ₹5,000 |
| Professional tax | 16(iii) | Actual, max ₹2,500 |
| Employer's NPS contribution | 80CCD(2) | 10% of salary (Govt: 14%) |
| Employer's contribution to Agniveer Corpus Fund | 80CCH(2) | Actual |
| Gratuity exemption u/s 10(10) | 10(10) | As computed |
| Leave encashment on retirement u/s 10(10AA) | 10(10AA) | Max ₹25,00,000 |
| VRS compensation u/s 10(10C) | 10(10C) | ₹5,00,000 (Govt employees) |
| HRA exemption | 10(13A) | **NOT AVAILABLE** in new regime |
| LTA exemption | 10(5) | **NOT AVAILABLE** in new regime |
| Children education allowance | 10(14) | NOT available |
| Transport allowance | 10(14) | NOT available (except for disabled) |
| New employment deduction | 80JJAA | Available for employers meeting conditions |
| Interest on home loan for let-out property | 24(b) | No limit (let-out only); SOP deduction NOT available in new regime |
| 80P — Co-op society deductions | 80P | Available |
| All Chapter VI-A deductions (80C, 80D, 80E, etc.) | Various | **NOT AVAILABLE** in new regime |

> **CRITICAL — OPTING OUT OF NEW REGIME:**
> - **Salaried/pensioners/other income (non-business):** Can choose between old and new regime EVERY YEAR — the choice is made at the time of filing ITR. Employer may be informed of choice for TDS purposes (Form 10IEA not required for non-business; declaration to employer suffices).
> - **Business income (ITR-3/ITR-4):** Once opted out of new regime, CANNOT re-enter for 5 years (except if business income ceases). File Form 10-IEA before due date of filing.
> - **DEFAULT:** New regime unless taxpayer explicitly opts out.

---

### 1.11 Key Budget Changes Reference Table

**Budget 2024 Changes — Effective AY 2025-26 (FY 2024-25)**

| Change | Before | From AY 2025-26 | Applicable Date |
|---|---|---|---|
| STCG on listed equity u/s 111A | 15% | 20% | July 23, 2024 |
| LTCG on listed equity u/s 112A | 10% above ₹1,00,000 | 12.5% above ₹1,25,000 | July 23, 2024 |
| LTCG on property u/s 112 | 20% with indexation | 12.5% without indexation (default) or old rate (choice for pre-Jul 23 assets) | July 23, 2024 |
| LTCG holding — bonds/debentures | 36 months | 24 months | July 23, 2024 |
| Standard deduction new regime salaried | ₹50,000 | ₹75,000 | AY 2025-26 |
| New regime family pension deduction | Not available | ₹25,000 or 1/3 (lower) | AY 2025-26 |
| Angel tax u/s 56(2)(viib) | Applicable | **Abolished** | AY 2025-26 |
| STT on futures | 0.0125% | 0.02% | July 23, 2024 |
| STT on options (sale) | 0.0625% | 0.1% | July 23, 2024 |
| STT on buyback of shares by companies | N/A | 0.01% (applied on value) | October 1, 2024 |

> **CRITICAL — SPLIT RATE COMPUTATION FOR AY 2025-26:**
> Since STCG/LTCG rates changed on July 23, 2024, the ERP must:
> - Capture date of each sale transaction
> - Split into pre-July 23, 2024 bucket (15% STCG / 10% LTCG) and post-July 23, 2024 bucket (20% STCG / 12.5% LTCG)
> - The ₹1,25,000 LTCG exemption is AGGREGATE across the whole year regardless of split
> - The system must apply the aggregate exemption in the most tax-efficient manner

**Budget 2025 Changes — Effective AY 2026-27 (FY 2025-26)**

| Change | AY 2025-26 | AY 2026-27 |
|---|---|---|
| New regime tax slabs | 3L–7L–10L–12L–15L | 4L–8L–12L–16L–20L–24L |
| New regime 87A rebate threshold | ₹7,00,000 (max ₹25,000) | ₹12,00,000 (max ₹60,000) |
| TDS threshold on salary u/s 192 | ₹2,50,000/₹3,00,000 | ₹4,00,000 (new regime basic exemption raised) |
| TDS threshold — bank interest u/s 194A | ₹40,000 (₹50,000 senior) | ₹50,000 (₹1,00,000 senior) |
| TDS threshold — various sections | Per old limits | Doubled in most cases (see TDS Section 8) |
| STCG 111A rate | 20% | 20% (unchanged) |
| LTCG 112A rate | 12.5% above ₹1,25,000 | 12.5% above ₹1,25,000 (unchanged) |

---

## SECTION 2 — INTEREST, FEE & DEMAND CALCULATIONS

### 2.1 Section 234A — Late Filing Interest

**Applicability:** ITR filed after the due date.

**Due Dates:**
- Non-audit individuals: **July 31** of the AY
- Audit cases: **October 31** of the AY
- Partners in audit firm: **October 31**
- Transfer pricing cases: **November 30**

**Formula:**

```
234A Interest = Net Tax Payable (after TDS/TCS/Advance Tax) × 1% × Months

Where:
  Net Tax Payable = Total Tax Liability − TDS − TCS − Advance Tax
  Months = from day AFTER due date to date of filing (or assessment, earlier)
  Any part of a month = 1 full month
  Minimum: 1 month
  If Net Tax Payable ≤ 0: 234A = NIL
```

| Parameter | Detail |
|---|---|
| Rate | 1% per month or part thereof |
| Base | Tax payable AFTER TDS/TCS/Advance Tax credit |
| Period Start | Day AFTER due date (August 1 for July 31 filers) |
| Period End | Date of actual filing OR date of assessment completion |
| Minimum Period | 1 month |
| If tax payable = 0 | 234A = NIL |

**Edge Cases:**
- If revised return filed late: 234A computed on ORIGINAL return due date; revising an already-late return does not reset the clock
- If return filed before due date: 234A = NIL
- If return never filed: Interest until date of assessment u/s 144 (Best Judgment Assessment)
- 234A base does NOT include the 234A interest itself (no circular computation)
- Belated return (u/s 139(4)) attracts 234A from August 1

---

### 2.2 Section 234B — Default in Advance Tax

**Trigger:** Advance tax paid < 90% of assessed tax liability (net of TDS/TCS).

**Formula:**

```
234B Trigger Check:
  Assessed Tax = Total Tax − TDS − TCS
  90% Threshold = 90% × Assessed Tax
  Advance Tax Paid = All challans paid before/during FY
  
  If Advance Tax Paid < 90% Threshold → 234B APPLIES

234B Amount = (Assessed Tax − Advance Tax Paid) × 1% × Months

Months = from April 1 of AY to date of payment / filing
```

| Parameter | Detail |
|---|---|
| Rate | 1% per month or part thereof |
| Period Start | April 1 of Assessment Year |
| Period End | Date of actual payment (self-assessment tax) or date of filing |
| Threshold | Total tax < ₹10,000 → NO advance tax, NO 234B |
| Exemption | Senior citizen (60+) with NO business income → FULLY EXEMPT |

**TDS Treatment in 234B:** TDS/TCS are credited and REDUCE the assessed tax for purposes of determining both the 90% threshold and the base for 234B. They are NOT treated as advance tax paid but as reduction of tax liability.

**Edge Cases:**
- If TDS alone covers 90% of tax → Advance tax = NIL → 234B = NIL even if no advance tax paid
- If regular assessment is completed (scrutiny): 234B continues from April 1 of AY to date of demand payment
- ESOP deferred tax: 234B computed excluding ESOP tax deferred to date of sale
- Advance tax paid in wrong AY: Risk of 234B in correct AY — rectification required

---

### 2.3 Section 234C — Deferment of Advance Tax

**Installment Schedule and Interest:**

| Due Date | Required Cumulative % | Shortfall Period | Interest Period |
|---|---|---|---|
| June 15 (FY) | 15% | Shortfall × 1% × 3 months | June–August |
| September 15 (FY) | 45% | Shortfall × 1% × 3 months | Sep–November |
| December 15 (FY) | 75% | Shortfall × 1% × 3 months | Dec–February |
| March 15 (FY) | 100% | Shortfall × 1% × 1 month | March only |

```
234C per Installment = Shortfall × 1% × Period_Months

Where:
  Shortfall at June 15 = max(0, 15% of Net Tax − Advance Tax paid by June 15)
  Shortfall at Sep 15 = max(0, 45% of Net Tax − Advance Tax paid by Sep 15)
  Shortfall at Dec 15 = max(0, 75% of Net Tax − Advance Tax paid by Dec 15)
  Shortfall at Mar 15 = max(0, 100% of Net Tax − Advance Tax paid by Mar 15)
  
  Net Tax = Total Tax Liability − TDS − TCS (estimated/actual for that date)

Total 234C = Sum of interest at all four installment dates
```

**Special Rules:**
- If advance tax estimated on original income, then income increased due to retrospective amendments: 234C waived on the incremental tax
- **Windfall income (capital gains, lottery) after Dec 15:** If entire tax on windfall income paid by March 15 → NO 234C on that windfall. If windfall arises after March 15 → pay tax before ITR due date → NO 234C
- **Presumptive income (44AD/44ADA):** Only ONE installment — 100% by March 15. No June/Sep/Dec installments. If short by March 15 → 234C for 1 month
- Threshold: Total tax < ₹10,000 → NO 234C

---

### 2.4 Section 234F — Late Filing Fee

| Filing Date | Fee (Income > ₹5 Lakh) | Fee (Income ≤ ₹5 Lakh) |
|---|---|---|
| On/before due date (31 July / 31 Oct) | NIL | NIL |
| After due date, on/before December 31 of AY | ₹5,000 | ₹1,000 |
| After December 31 of AY | ₹10,000 | ₹1,000 |

> - 234F is a FEE, not interest — added to tax demand separately
> - If income ≤ basic exemption limit → NO 234F even if filed late (but only if filing is genuinely not required)
> - The ₹5 lakh threshold = total income BEFORE Chapter VI-A deductions
> - For ITR filed within 2 years (updated return u/s 139(8A)): Additional tax of 25%/50% also applies

---

### 2.5 Section 244A — Interest on Income Tax Refund

When excess TDS/advance tax creates a refund, the assessee is entitled to interest:

```
Interest u/s 244A = Refund Amount × Rate × Months

Rate: 0.5% per month or part thereof

Period:
  For TDS/TCS refunds: from April 1 of AY to date of grant of refund
  For advance tax refunds: from April 1 of AY to date of refund
  If refund arises due to taxpayer's error (late filing): interest from date of filing to date of refund
  
Minimum: 3 months (if refund ≤ 3 months from April 1)
If interest < ₹100: NO interest paid (de minimis)
Interest received u/s 244A is taxable in the year of receipt under "Other Sources" (u/s 56)
```

---

### 2.6 Section 220(2) — Interest on Tax Demand After Assessment

If tax demand raised after assessment (scrutiny/revision) is not paid within 30 days of demand notice:

```
Interest u/s 220(2) = Outstanding Demand × 1% × Months
Period: From day 31 after demand notice to date of payment
Compounded: Simple interest (not compound)
```

> **This interest is NOT included in the ITR computation itself — it arises post-assessment. However, the ERP should track outstanding demands and alert the taxpayer.**

---

### 2.7 Updated Return — Section 139(8A)

An updated return can be filed within **2 years** from end of relevant AY (i.e., up to March 31 of AY+2) if:
- Original/belated/revised return was filed, AND
- Additional income to declare (cannot be filed to claim refund or reduce demand)

**Additional Tax on Updated Return:**

| Filed Within | Additional Tax Rate |
|---|---|
| 12 months from end of AY | 25% of (tax + interest on underpaid tax) |
| 12–24 months from end of AY | 50% of (tax + interest on underpaid tax) |

ERP must calculate additional tax, interest u/s 234A/B/C on incremental income, and the 25%/50% additional tax.

---

## SECTION 3 — ITR-1 (SAHAJ) — COMPLETE FIELD GUIDE

### 3.1 Eligibility

| Criteria | ITR-1 Eligible? |
|---|---|
| Resident Individual ONLY (not HUF) | ✓ ELIGIBLE |
| Total income up to ₹50,00,000 | ✓ ELIGIBLE |
| Salary or Pension income | ✓ ELIGIBLE |
| ONE house property (no carried forward HP loss) | ✓ ELIGIBLE |
| Other sources: Interest, Dividend, Casual income | ✓ ELIGIBLE |
| Agricultural income up to ₹5,000 | ✓ ELIGIBLE |
| Resident and Ordinarily Resident (ROR) only | ✓ ELIGIBLE |

### 3.2 Who CANNOT File ITR-1

| Disqualifying Condition | Correct Form |
|---|---|
| More than one house property | ITR-2 |
| Capital gains (STCG or LTCG) of any kind | ITR-2 |
| VDA (Crypto/NFT) income | ITR-2 |
| Business or professional income | ITR-3 or ITR-4 |
| Foreign assets or foreign income | ITR-2 |
| Agricultural income > ₹5,000 | ITR-2 |
| Total income > ₹50 lakhs | ITR-2 |
| Director in any company (listed/unlisted) | ITR-2 |
| Held unlisted equity shares at any time during year | ITR-2 |
| TDS u/s 194N (cash withdrawal > ₹1 crore) | ITR-2 |
| ESOP tax deferred u/s 191(2) | ITR-2 |
| Non-resident or RNOR | ITR-2 |
| Claiming relief u/s 90/90A/91 (foreign tax credit) | ITR-2 |
| Losses to be carried forward (any head) | ITR-2 |
| Brought forward losses from prior years | ITR-2 |
| Clubbed income from spouse/minor if it includes capital gains/business | ITR-2 |

---

### 3.3 Part A — General Information (Complete Fields)

| Field | Data Type | Source / Validation |
|---|---|---|
| First Name, Middle Name, Last Name | Text | As per PAN card |
| PAN | 10-char alphanumeric | Validated against ITD API; format: AAAAA9999A |
| Date of Birth | DD/MM/YYYY | Determines age category for slabs |
| Aadhaar Number | 12 digits | Mandatory for resident filers (u/s 139AA) |
| Aadhaar Enrollment ID | 28 digits | Only if Aadhaar not yet assigned |
| Mobile Number | 10 digits | OTP verification for e-filing |
| Email Address | Text | Communications and OTP |
| Address — Flat/Door, Street, Locality | Text | — |
| Town/City, District, State, PIN Code | Text/Enum | State → jurisdiction of AO |
| Employer Category | Enum | Govt / PSU / Pensioners / Others / Not Applicable |
| Nature of Employment | Enum | Central Govt / State Govt / Public Sector / Pensioner / Others |
| Assessment Year | YYYY-YY | Auto-set by system |
| Whether return is original / revised / defective | Radio | Original / Revised (139(5)) / Defective (Sec 139(9)) |
| If revised — Original Ack No. | 15-digit | Mandatory if revised return |
| If revised — Date of original filing | DD/MM/YYYY | Mandatory if revised |
| Filing Type | Enum | 11 (voluntary u/s 139(1)) / 12 (u/s 142(1) notice) / 16 (u/s 119(2)(b) condonation) / 17 (u/s 139(8A) updated) |
| Opt out of new tax regime? | Yes/No | Yes → old regime; No → new regime (default) |
| Form 10-IEA filed (for business)? | Yes/No | Only for ITR-3/4; not applicable ITR-1 |
| Due Date of Filing | Date | Auto-computed by system based on audit status |
| Date of Filing | DD/MM/YYYY | Auto-set on submission |
| Whether audit u/s 44AB applicable? | Yes/No | ITR-1 should be No always |
| Is return filed in response to notice? | Yes/No | Section of notice |
| Residential Status | Enum | ROR / RNOR / NR (ITR-1 only for ROR) |
| Whether claiming relief u/s 90/90A/91? | Yes/No | ITR-1 should be No |
| Whether any TDS was deducted at higher rate u/s 206AA? | Yes/No | Flag for PAN/Aadhaar mismatch issues |

---

### 3.4 Part B — Gross Total Income

**B1 — Income from Salary/Pension**

| Field No. | Field | Formula / Source |
|---|---|---|
| B1(a) | Salary as per Section 17(1) | Gross salary from Form 16 Part B, Sl. 1 — includes basic, DA, bonus, commission, all monetary payments |
| B1(b) | Perquisites u/s 17(2) | From Form 16 — perquisites: rent-free accommodation, ESOP, car, club membership, soft furnishings, etc. |
| B1(c) | Profits in lieu of salary u/s 17(3) | Terminal benefits: gratuity excess, leave encashment excess, VRS excess, compensation on termination |
| B1(d) | **Gross Salary = B1(a) + B1(b) + B1(c)** | CALCULATED |
| B1(e) | Less: Exempt allowances u/s 10 | Sum of all exempt allowances: HRA, LTA, Children Education (₹100/month/child, max 2), Hostel (₹300/month), Transport (disabled), Uniform, Underground (miners), etc. |
| B1(f) | Income chargeable under 'Salaries' = B1(d) − B1(e) | CALCULATED |
| B1(g) | Standard deduction u/s 16(ia) | Old regime: ₹50,000. New regime: ₹75,000 (AY 2025-26 onwards) |
| B1(h) | Entertainment allowance u/s 16(ii) | ONLY for Government employees. Deduction = least of: (a) actual EA received, (b) 1/5th of basic salary, (c) ₹5,000 |
| B1(i) | Professional tax u/s 16(iii) | Actual professional tax paid to state government. Max ₹2,500 per year. NOT available in new regime (but professional tax deducted by employer is included in computation) |
| B1(j) | **Income from Salaries = B1(f) − B1(g) − B1(h) − B1(i)** | CALCULATED — Final salary income |

**HRA Exemption Computation (u/s 10(13A)) — IMPORTANT:**

```
HRA Exemption = MINIMUM OF:
  (i)  Actual HRA received during the year
  (ii) Actual rent paid minus 10% of Salary*
  (iii) 50% of Salary* (if metro) OR 40% of Salary* (if non-metro)

Where: Salary* = Basic Salary + Dearness Allowance (forming part of retirement benefits)
               (Does NOT include HRA, bonus, commission, perquisites)

Metro cities for 50%: Delhi, Mumbai, Kolkata, Chennai
All other cities: 40%

RULES:
- If rent paid < 10% of salary → HRA exemption = NIL (third condition is negative → min = 0)
- If rent paid to immediate relative (parents) — need rent agreement; AO may scrutinize
- If rent > ₹1,00,000 per year: Landlord's PAN mandatory to be quoted in ITR
- HRA not applicable in new regime from AY 2023-24 (new regime restriction)
- Not available if HRA not received as part of salary (then 80GG available in old regime)
```

**Leave Travel Allowance (LTA) u/s 10(5):**

```
LTA Exemption = Actual fare for economy class air OR AC first class rail
               (whichever applicable, for shortest route by economy mode)

CONDITIONS:
- Only for travel within India
- Maximum 2 journeys in a block of 4 calendar years
- Current block: 2022–2025; Next block: 2026–2029
- Only fare — hotel/food/local transport NOT exempt
- Exemption = actual fare subject to maximum of fare for shortest route
- Not available in new regime
```

**B2 — Income from House Property**

| Field No. | Field | Formula |
|---|---|---|
| B2(a) | Gross Annual Value (GAV) | Let-out: Higher of (actual rent received, Annual Letting Value, Municipal Rateable Value). Self-occupied: NIL |
| B2(b) | Less: Municipal taxes paid by owner | Actual taxes PAID (not accrued) during the FY. Only owner-paid — not tenant-paid |
| B2(c) | Net Annual Value (NAV) = B2(a) − B2(b) | CALCULATED |
| B2(d) | Less: 30% standard deduction u/s 24(a) | 30% of NAV. Only if NAV > 0. NOT applicable for self-occupied property |
| B2(e) | Less: Interest on housing loan u/s 24(b) | See limits below |
| B2(f) | **Income from House Property = B2(c) − B2(d) − B2(e)** | CALCULATED — Can be negative (loss) |

**Interest on Housing Loan Limits u/s 24(b):**

| Property Type | Loan Purpose | Condition | Maximum Deduction |
|---|---|---|---|
| Self-Occupied | Purchase or Construction | Completed within 5 years of loan | ₹2,00,000 |
| Self-Occupied | Purchase or Construction | NOT completed within 5 years | ₹30,000 |
| Self-Occupied | Repair/Renewal/Reconstruction | Any | ₹30,000 |
| Let-Out | Any purpose | Any | NO LIMIT (full interest) |
| Let-Out with loss | — | Set-off cap | Max ₹2,00,000 inter-head set-off |

**Pre-Construction Interest:**
```
Pre-construction period = from date of loan to date of completion/acquisition (whichever earlier)
Deductible in 5 equal installments starting from year of completion
Annual deduction = Total pre-construction interest ÷ 5
This deduction is subject to the overall ₹2,00,000 cap for SOP
```

> **HP LOSS SET-OFF RULES:**
> - HP Loss (aggregate from all properties) CAN be set off against salary, other sources
> - MAXIMUM inter-head set-off cap = ₹2,00,000 per year
> - Unabsorbed HP loss (above ₹2L cap) → carried forward for **8 assessment years**
> - Carried forward HP loss → set off ONLY against HP income in subsequent years (NOT salary)
> - IN ITR-1: If HP loss > ₹2,00,000, taxpayer MUST file ITR-2 (ITR-1 cannot carry forward HP loss)

**B3 — Income from Other Sources**

| Field | Income Type | Tax Treatment |
|---|---|---|
| B3(a) | Interest from savings account (bank, post office, co-op) | Taxable at slab; 80TTA/80TTB deduction available |
| B3(b) | Interest from deposits (FD, RD, NSC interest, SCSS interest) | Taxable at slab; NO 80TTA benefit |
| B3(c) | Interest from income tax refund (u/s 244A) | Taxable at slab rate |
| B3(d) | Family Pension | Less standard deduction (1/3 of pension or ₹15,000 old regime / ₹25,000 new regime); balance taxable |
| B3(e) | Dividend income from shares/MF | Taxable at slab; TDS u/s 194 at 10% if > ₹5,000 |
| B3(f) | Any other income (gifts from non-relatives, prize money if not lottery) | Taxable at slab |
| B3(g) | **Total Other Sources = Sum of above** | CALCULATED |

> **ACCRUAL BASIS FOR INTEREST INCOME (CRITICAL):**
> - FD interest: Taxable on accrual basis (not receipt). ERP must compute FD interest accrued per year
> - NSC interest: Taxable on accrual or maturity basis (consistent method). Also qualifies for 80C in old regime
> - RD interest: Taxable on accrual — each year's interest should be declared
> - The system should prompt if client has FD/NSC but no interest declared

> **80TTA vs 80TTB (CRITICAL):**
> - Below 60 years: 80TTA = up to ₹10,000 on SAVINGS ACCOUNT interest only
> - 60+ years: 80TTB = up to ₹50,000 on ALL bank/post office interest (savings + FD + RD + SCSS)
> - These are MUTUALLY EXCLUSIVE; senior citizen uses ONLY 80TTB
> - BOTH unavailable in new regime

**B4 — Gross Total Income**

```
GTI = B1(j) + B2(f) + B3(g)

Where B2(f) can be NEGATIVE (HP loss):
  HP Loss set-off = min(|B2(f)|, ₹2,00,000) — this REDUCES GTI
  If |B2(f)| > ₹2,00,000: taxpayer must file ITR-2 to carry forward excess
  GTI cannot be below 0
```

---

### 3.5 Part C — Deductions and Taxable Total Income

**Chapter VI-A Deductions — Old Regime (Most NOT available in new regime)**

| Section | Deduction | Limit | New Regime |
|---|---|---|---|
| 80C | LIC, PPF, ELSS, EPF (employee), NSC, Sukanya Samriddhi, Home loan principal, Tuition fees (max 2 children), ULIP, NABARD Rural Bonds, Senior Citizen Savings Scheme, 5-yr bank FD | ₹1,50,000 combined | NO |
| 80CCC | Pension fund premium — part of 80C | ₹1,50,000 (combined with 80C) | NO |
| 80CCD(1) | NPS employee own contribution — part of 80C | ₹1,50,000 (within 80C) | NO |
| 80CCD(1B) | Additional NPS over 80C | ₹50,000 extra | NO |
| 80CCD(2) | Employer's NPS contribution | 10% of salary (Govt: 14%) | **YES** |
| 80D | Health insurance premium — self/family | ₹25,000 (₹50,000 if senior); Parents: ₹25,000 (₹50,000 if senior parents) | NO |
| 80DD | Disabled dependent — fixed deduction | ₹75,000 (40–80% disability); ₹1,25,000 (≥80%) | NO |
| 80DDB | Medical treatment of specified diseases | ₹40,000; ₹1,00,000 (senior citizen) | NO |
| 80E | Interest on education loan (self/spouse/children) | Actual interest for up to 8 years; no cap | NO |
| 80EE | Interest on housing loan (first buyer; loan ≤ ₹35L; property ≤ ₹50L) | ₹50,000 extra over 24(b) | NO |
| 80EEA | Interest on affordable housing loan (stamp duty value ≤ ₹45L) | ₹1,50,000 additional | NO |
| 80EEB | Interest on electric vehicle loan | ₹1,50,000 | NO |
| 80G | Donations to approved funds/institutions | 50%/100% with/without 10% income limit | NO |
| 80GG | Rent paid (no HRA in salary) | Least of: (i) actual rent − 10% income, (ii) ₹5,000/month, (iii) 25% of total income | NO |
| 80GGC | Donation to political party (not in cash) | 100% of donation | NO |
| 80TTA | Savings account interest (below 60) | ₹10,000 | NO |
| 80TTB | All interest income (senior citizens 60+) | ₹50,000 | NO |
| 80U | Self-disabled assessee | ₹75,000 (40–80%); ₹1,25,000 (≥80%) | NO |

**80C Aggregate Computation:**

```
80C_Investment = Sum of all qualifying investments/expenses
80C_Deduction = min(80C_Investment, ₹1,50,000)

80CCD(1B) = Additional NPS (employee own contribution over 80C limit)
80CCD(1B)_Deduction = min(Additional NPS contributed, ₹50,000)

Total_80C_Group = min(80C + 80CCC + 80CCD(1), ₹1,50,000)
Total_80CCD_Extra = min(80CCD(1B), ₹50,000)
Total_80CCD(2) = Employer NPS contribution (no cap for individual; 10%/14% of salary limit)

Note: 80CCD(2) is OVER AND ABOVE the ₹1,50,000 limit
```

**80D Health Insurance Computation:**

```
Own/Family: min(actual premium, ₹25,000) [₹50,000 if assessee is senior citizen]
Parents: min(actual premium, ₹25,000) [₹50,000 if parents are senior citizens]
Preventive health check-up: ₹5,000 (within the above limits)
Total 80D = Own/Family component + Parents component
```

**80G Computation Algorithm:**

```
Step 1: Identify each donation category:
  Cat A: 100% without qualifying limit (PM CARES Fund, PM National Relief Fund, etc.)
  Cat B: 100% with 10% qualifying limit (National Children's Fund, etc.)
  Cat C: 50% without qualifying limit (Jawaharlal Nehru Memorial Fund, etc.)
  Cat D: 50% with 10% qualifying limit (Most approved hospitals, educational institutions)

Step 2: Compute Adjusted GTI for 10% limit:
  AGTI = GTI − LTCG − STCG u/s 111A − Foreign income NR − 80C to 80U deductions (other than 80G)

Step 3: 10% Qualifying Limit = 10% × AGTI

Step 4: For Cat A: Deduction = 100% of donation (no limit check)
        For Cat B: Deduction = 100% × min(donation, 10% qualifying limit)
        For Cat C: Deduction = 50% × donation
        For Cat D: Deduction = 50% × min(donation, 10% qualifying limit)

Step 5: CASH DONATIONS > ₹2,000 → DISALLOWED (only digital/cheque eligible)
        Foreign contributions under FCRA need separate verification

Step 6: Total 80G deduction = Sum of all eligible amounts
```

**Taxable Total Income Computation:**

```
Old Regime:
  Total Income = max(0, GTI − Total Chapter VI-A Deductions)
  Round to nearest ₹10 u/s 288A

New Regime:
  Total Income = max(0, GTI − Standard Deduction − 80CCD(2) − Other permissible)
  Round to nearest ₹10 u/s 288A
```

---

### 3.6 Part D — Tax Computation (ITR-1 Complete)

| Field No. | Field | Formula |
|---|---|---|
| D1 | Tax on Total Income | Apply slab table per age category and regime |
| D2 | Rebate u/s 87A | Old: if TI ≤ 5L → min(tax, 12,500). New AY25-26: if TI ≤ 7L → min(tax, 25,000). New AY26-27: if TI ≤ 12L → min(tax on normal income, 60,000) |
| D3 | Tax after rebate = D1 − D2 | Cannot go below 0 |
| D4 | Surcharge | Apply Section 1.4 rates; compute marginal relief |
| D5 | Health and Education Cess = (D3 + D4) × 4% | — |
| D6 | Total Tax Liability = D3 + D4 + D5 | — |
| D7 | Relief u/s 89 (salary arrears) | From Form 10E (filed electronically before ITR) |
| D8 | Tax payable after relief = D6 − D7 | — |
| D9 | Less: TDS u/s 192 (salary TDS) | From Schedule TDS1 |
| D10 | Less: TDS on other income (bank, etc.) | From Schedule TDS2 |
| D11 | Less: TCS (Tax Collected at Source) | From Schedule TCS |
| D12 | Less: Advance Tax paid | From Schedule IT — challan-wise |
| D13 | Tax Payable / (Refund) = D8 − D9 − D10 − D11 − D12 | — |
| D14 | Interest u/s 234A | Only if D13 > 0 and filed late |
| D15 | Interest u/s 234B | Only if D13 > 0 and advance tax short |
| D16 | Interest u/s 234C | Only if installments short |
| D17 | Late filing fee u/s 234F | Only if filed late |
| D18 | **Total Demand / (Refund) = D13 + D14 + D15 + D16 + D17** | FINAL FIGURE |

**Rounding Rules:**
- Income: rounded to nearest ₹10 u/s 288A (anything above ₹5 → up; ₹5 or below → down)
- Tax: rounded to nearest ₹10 u/s 288B
- Final demand/refund: rounded to nearest ₹1
- 234A/B/C: rounded to nearest ₹1

**Relief u/s 89 — Salary Arrears:**
- Form 10E must be filed BEFORE filing ITR (mandatory prerequisite)
- Formula: Tax on (current income incl. arrears) − Tax that would have been paid if arrears received in respective years
- CPC automatically verifies Form 10E; if not filed → 89 relief denied → demand raised
- ERP MUST alert advocate if 89 relief is claimed without Form 10E

---

### 3.7 Schedule TDS1 — TDS on Salary (from Form 16)

| Field | Details |
|---|---|
| Employer Name | As per Form 16 |
| TAN of Employer | 10-character TAN — mandatory; cross-verified with 26AS |
| Amount of Salary | Gross salary per Form 16 |
| Tax Deducted (Form 16 Part A) | Total TDS for the year |
| Tax Deposited (26AS) | Must match 26AS; flag discrepancy |
| Period (From Date — To Date) | Employment period |

**Multiple Employer Handling:**
- Enter each employer separately
- Verify TDS from ALL Form 16s is entered
- Standard deduction claimed only ONCE across all employers in ITR

---

### 3.8 Schedule TDS2 — TDS on Other Income (from Form 26AS/AIS)

| Field | Details |
|---|---|
| Deductor Name | Bank / institution |
| TAN of Deductor | 10-character TAN |
| Amount on which TDS was deducted | Gross income before TDS |
| TDS Amount | As per Form 26AS Part A/AIS |
| Year of Deduction | Important for prior-year TDS reflected this year |
| Section under which TDS deducted | 194A (interest), 194 (dividend), 194J (professional fees), etc. |

> **FORM 26AS vs AIS vs TIS (CRITICAL RECONCILIATION):**
> - **Form 26AS:** TDS/TCS summary; advance tax/self-assessment tax payments
> - **Annual Information Statement (AIS):** Comprehensive — includes 26AS data PLUS SFT transactions, bank deposits, securities, GST data, property transactions
> - **Taxpayer Information Summary (TIS):** Aggregated view of AIS for quick review
> - The ERP must help the taxpayer reconcile AIS data against declared income
> - Any unexplained AIS transaction = risk of scrutiny notice u/s 143(2)
> - Income appearing in AIS but not declared in ITR → CPC flags → defective return notice

---

### 3.9 Schedule IT — Advance Tax and Self-Assessment Tax

| Field | Details |
|---|---|
| BSR Code | 7-digit bank branch code (from challan) |
| Date of Deposit | DD/MM/YYYY |
| Challan Serial Number | From challan receipt / bank acknowledgement |
| Amount | Advance tax / self-assessment tax |
| Type | Advance Tax (Type 2) / Self-Assessment Tax (Type 3) / Regular Assessment Tax (Type 4) |

---

### 3.10 Schedule EI — Exempt Incomes

Exempt incomes are NOT included in Gross Total Income but must be disclosed:

| Exempt Income | Section | What to Report |
|---|---|---|
| Interest from PPF | 10(11) | Amount received |
| Interest from Sukanya Samriddhi | 10(11A) | Amount received |
| Tax-free bonds interest (NHAI, REC, etc.) | 10(15) | Amount received |
| Maturity proceeds of life insurance (LIC) | 10(10D) | Amount received (see conditions) |
| Long-term capital gains exempt u/s 10(38) | 10(38) | N/A from AY 2019-20; now taxable u/s 112A |
| Dividend received up to AY 2020-21 | 10(34) | N/A now |
| Agricultural income | 10(1) | Amount |
| Share of profit from partnership firm | 10(2A) | Amount |
| HUF income received by member | 10(2) | Amount |
| Gratuity exempt portion | 10(10) | Exempt amount |
| Leave encashment exempt | 10(10AA) | Exempt amount |
| VRS compensation exempt | 10(10C) | Exempt amount |
| Commutation of pension | 10(10A) | Exempt amount |
| Any other exempt income | — | Describe and amount |

**Life Insurance Proceeds u/s 10(10D) — Conditions for Exemption:**
```
For policies issued ON OR BEFORE March 31, 2003:
  Exempt regardless of premium-to-sum-assured ratio

For policies issued AFTER March 31, 2003 but before April 1, 2012:
  Exempt if premium in any year ≤ 20% of sum assured; else TAXABLE

For policies issued ON OR AFTER April 1, 2012:
  Exempt if premium in any year ≤ 10% of sum assured; else TAXABLE

For ULIP policies with premium > ₹2,50,000 per year (issued on/after Feb 1, 2021):
  Proceeds taxable as capital gains (LTCG u/s 112A if listed; else 112)

Keyman Insurance: Always taxable in employer's hands
Death benefit: ALWAYS exempt regardless of conditions
```

---

### 3.11 Part E — Verification (Mandatory)

| Field | Details |
|---|---|
| Capacity | Self / Authorized Representative / Legal Heir |
| Name of person signing | Full name |
| Father's name | If signing as self |
| PAN of authorized representative | If signing as representative |
| IP Address | Auto-captured at time of e-filing |
| Date of filing | Auto-set |
| Verification declaration | "I solemnly declare that information given in this Return is correct, complete and truly stated..." |

**E-Verification Methods (in order of preference):**
1. Aadhaar OTP (immediate verification)
2. Net banking EVC (Electronic Verification Code)
3. Bank account EVC
4. Demat account EVC
5. Physical signed ITR-V (send to CPC Bengaluru within 30 days if e-verify not done)

---

### 3.12 Bank Account Details (Pre-Validated — Mandatory for Refund)

| Field | Details |
|---|---|
| Account Number | Full account number |
| IFSC Code | 11-character code |
| Account Type | Savings / Current / Cash Credit |
| Bank Name | Auto-populated from IFSC |
| Pre-validation Status | Must be pre-validated on e-filing portal for refund to be credited |

> **CRITICAL:** If bank account is not pre-validated, refund will fail. ERP must flag and prompt pre-validation. Multiple bank accounts can be added; only ONE nominated for refund.

---

### 3.13 Complete ITR-1 Edge Cases

| Scenario | Correct Handling |
|---|---|
| Multiple employers in same year | Combine all Form 16s. Standard deduction claimed once. Risk: each employer may have given separately — aggregate may show incorrect income. |
| Salary in foreign currency | Convert at RBI TT buying rate on last day of each month. Include in gross salary. |
| Gratuity received (non-Govt) | Exempt = LEAST OF: (a) 15/26 × last drawn salary × completed years of service; (b) ₹20,00,000; (c) actual gratuity received. Excess taxable u/s 17(3). |
| Leave encashment on retirement (non-Govt) | Exempt = LEAST OF: (a) actual amount; (b) ₹25,00,000; (c) 10 months salary (salary = basic + DA for retirement); (d) cash equivalent of unavailed leave (max 30 days per year of service). Excess taxable. |
| VRS/Golden handshake | Exempt u/s 10(10C) = least of: ₹5,00,000, actual amount, last 3 months salary × remaining service months. Available once in lifetime. |
| FD interest: accrual vs receipt | Taxable on ACCRUAL basis. ERP must compute and prompt for undisclosed FD interest. |
| HP loss > ₹2L with only salary income | Only ₹2L set off against salary. ITR-1 cannot carry forward excess. Must file ITR-2 for excess HP loss carry forward. |
| 80G cash donation > ₹2,000 | DISALLOWED. Exclude from deduction. |
| NSC interest — 80C and income | Accrued NSC interest is BOTH income (Other Sources) AND qualifies for 80C investment (old regime). Net effect in some years = zero if 80C limit not exhausted. |
| Dividend income with TDS | Gross up dividend by TDS and declare full amount. Claim TDS credit in Schedule TDS2. |
| Minor child's income | Club in parent's (higher earner's) income. Exemption: ₹1,500 per minor child per year u/s 10(32). Declare in Schedule EI. |
| Non-cash gift from non-relative > ₹50,000 | Taxable u/s 56(2)(x). Aggregate of all non-cash gifts from non-relatives exceeding ₹50,000 → entire amount taxable. |
| Rental income from let-out SOP | Cannot claim SOP interest deduction AND let-out income — property is either SOP or let-out in a given year. |
| Professional tax deducted by employer | Already reduced in Form 16 gross salary. Do not double deduct. Show in B1(i) only if deducted from salary. |

---

## SECTION 4 — ITR-2 — COMPLETE FIELD GUIDE

### 4.1 Eligibility — Who Files ITR-2

- Individuals and HUFs with salary, house property (one or more), capital gains, foreign income
- Total income exceeding ₹50 lakh
- Director in any company
- Holders of unlisted equity shares
- Agricultural income > ₹5,000
- Seeking carry-forward of losses
- Claiming foreign tax credit u/s 90/90A/91
- Residents with foreign assets or signing authority in foreign accounts

### 4.2 ITR-2 Additional Schedules Over ITR-1

| Schedule | Purpose |
|---|---|
| Schedule HP | Multiple property HP computation |
| Schedule CG | Capital gains — STCG and LTCG, transaction-wise |
| Schedule VDA | Virtual Digital Assets (Crypto/NFT) income |
| Schedule OS (detailed) | Other sources with deductions u/s 57 |
| Schedule CYLA | Current Year Loss Adjustment across heads |
| Schedule BFLA | Brought Forward Loss Adjustment |
| Schedule CFL | Carry Forward Losses to future years |
| Schedule VIA | All Chapter VI-A deductions |
| Schedule 80G | Donation-wise 80G details |
| Schedule AL | Assets and Liabilities (mandatory if income > ₹50L) |
| Schedule FA | Foreign Assets disclosure |
| Schedule FSI | Foreign Source Income |
| Schedule TR | Tax Relief claimed (foreign tax credit) |
| Schedule 5A | Portuguese Civil Code — apportionment between spouses |
| Schedule SPI | Clubbing of income (spouse/minor/HUF member) |
| Schedule SI | Income at special rates (111A, 112, 112A, 115BB, 115BBH) |
| Schedule EI | Exempt incomes |
| Schedule AMT | Alternate Minimum Tax computation |
| Schedule AMTC | AMT credit |
| Schedule PTI | Pass-through income from business trust/investment fund |
| Schedule TPSA | Secondary transfer pricing adjustment |

---

### 4.3 Schedule HP — Multiple House Properties

For **each property**, enter:

| Field | Details |
|---|---|
| Property Address | Full address including PIN code |
| Ownership Type | Self-Occupied (SOP) / Let-Out (LO) / Deemed Let-Out (DLO) |
| Date of acquisition | DD/MM/YYYY |
| Ownership Share % | If joint property — only proportionate share of income/loss |
| Co-owner details | PAN and name if co-owned |
| Tenant details (if LO) | Name and PAN of principal tenant if rent > ₹50,000/month |
| Annual Rent Received/Receivable | Actual contracted rent per year |
| Annual Letting Value (Fair Rent) | Municipal or market fair rent |
| Municipal Rateable Value | From municipality assessment order |
| Gross Annual Value (GAV) | Higher of (Annual Rent, Fair Rent, MRV) for let-out |
| Unrealised Rent Deducted | Rent not recovered from tenant (Rule 4 conditions) |
| Municipal Taxes Paid | By owner during the year |
| Net Annual Value (NAV) | GAV − Municipal Taxes |
| 30% Standard Deduction | 30% × NAV (only if NAV > 0 and let-out) |
| Interest on loan u/s 24(b) | Current year interest paid |
| Pre-construction interest (1/5th) | Annual installment of pre-construction period interest |
| Income / (Loss) from this property | NAV − 30% deduction − Interest deduction |
| Loan account number | For housing loan interest verification |
| Lender's PAN/TAN | For cross-verification |
| Lender's name | — |

**Deemed Let-Out (DLO) Rules:**
```
If an individual owns MORE than 2 residential properties:
  Any 2 chosen as Self-Occupied (zero NAV, interest cap ₹2L each)
  Remaining property(ies) = Deemed Let-Out

For DLO:
  GAV = Higher of (Annual Letting Value, Fair Rent)
  Apply 30% standard deduction
  Full interest deduction (no cap)
  
NOTE: From AY 2020-21, maximum SOPs = 2 (Budget 2019 amendment)
```

**HP Loss Aggregation:**
```
Property_1_Income = (positive or negative)
Property_2_Income = (positive or negative)
...
Aggregate_HP_Income = Sum of all properties

If Aggregate_HP_Income < 0:
  Set_off_against_other_heads = min(|Aggregate_HP_Income|, ₹2,00,000)
  Carry_forward = |Aggregate_HP_Income| - ₹2,00,000 (if positive)
```

---

### 4.4 Schedule CG — Capital Gains (Complete Reference)

**Holding Period Classification:**

| Asset Type | STCG Period | LTCG Period |
|---|---|---|
| Listed equity shares / equity MF (STT paid) | ≤ 12 months | > 12 months |
| Unlisted equity shares | ≤ 24 months | > 24 months |
| Immovable property (land/building) | ≤ 24 months (Budget 2024) | > 24 months |
| Debt mutual funds (after April 1, 2023) | Always at slab rate | Always at slab rate (no LTCG benefit) |
| Debt mutual funds (bought before April 1, 2023) | ≤ 36 months | > 36 months |
| Bonds and debentures (listed) | ≤ 24 months (Budget 2024; was 36M) | > 24 months |
| Bonds and debentures (unlisted) | ≤ 24 months | > 24 months |
| Gold / jewellery / paintings / art | ≤ 24 months | > 24 months |

**Capital Gains Per Transaction — Fields:**

| Field | Details |
|---|---|
| Asset Description | Name, type (equity share / plot / flat / gold / etc.) |
| ISIN (for securities) | From broker contract note |
| Date of Purchase / Acquisition | DD/MM/YYYY — determines holding period and CII year |
| Date of Sale / Transfer | DD/MM/YYYY — determines applicable rate (pre/post Jul 23 for AY 2025-26) |
| Full Value of Consideration (Sale Price) | Total sale proceeds |
| Less: Expenditure on transfer | Brokerage, STT, registration charges, legal fees — directly for this transfer |
| Less: Cost of Acquisition | Actual cost / Indexed cost / FMV (grandfathering) |
| Less: Cost of Improvement | Actual / indexed cost of improvements |
| Gross Capital Gain / Loss | Sale proceeds − Transfer costs − CoA − CoI |
| Exemption u/s 54/54B/54EC/54F/54G/54GB | Amount invested in qualifying investment |
| Net Taxable Capital Gain / Loss | Gross CG − Exemption |

**Cost Inflation Index (CII) Table:**

| FY | CII | FY | CII |
|---|---|---|---|
| 2001-02 (Base) | 100 | 2013-14 | 220 |
| 2002-03 | 105 | 2014-15 | 240 |
| 2003-04 | 109 | 2015-16 | 254 |
| 2004-05 | 113 | 2016-17 | 264 |
| 2005-06 | 117 | 2017-18 | 272 |
| 2006-07 | 122 | 2018-19 | 280 |
| 2007-08 | 129 | 2019-20 | 289 |
| 2008-09 | 137 | 2020-21 | 301 |
| 2009-10 | 148 | 2021-22 | 317 |
| 2010-11 | 167 | 2022-23 | 331 |
| 2011-12 | 184 | 2023-24 | 348 |
| 2012-13 | 200 | **2024-25 (AY 2025-26)** | **363** |

> **FY 2025-26 CII (AY 2026-27): To be notified by CBDT. Update from official gazette before filing.**

```
Indexed Cost = Actual Cost × (CII of year of SALE / CII of year of ACQUISITION)

For assets acquired before April 1, 2001:
  Cost = Fair Market Value as on April 1, 2001 (FMV 2001) OR actual cost (whichever higher)
  Use FMV 2001 as the base for indexation
  CII base year = 2001-02 (CII = 100)

For inherited/gifted assets:
  Holding period includes period of previous owner's holding
  Cost = cost to the previous owner (and indexed from their acquisition year)
```

**STCG Tax Rates by Category:**

| Category | Section | AY 2025-26 (pre Jul 23, 2024) | AY 2025-26 (post Jul 23, 2024) | AY 2026-27 |
|---|---|---|---|---|
| Listed equity/equity MF (STT paid) | 111A | 15% | 20% | 20% |
| Other equity shares | Normal | Slab rate | Slab rate | Slab rate |
| Immovable property | Normal | Slab rate | Slab rate | Slab rate |
| Debt MF (post Apr 1, 2023) | Normal | Slab rate | Slab rate | Slab rate |
| Gold / bonds (< holding period) | Normal | Slab rate | Slab rate | Slab rate |

**LTCG Tax Rates by Category:**

| Category | Section | AY 2025-26 (pre Jul 23) | AY 2025-26 (post Jul 23) | AY 2026-27 |
|---|---|---|---|---|
| Listed equity/equity MF (STT paid) | 112A | 10% above ₹1,00,000 | 12.5% above ₹1,25,000 | 12.5% above ₹1,25,000 |
| Property / other assets (with indexation) | 112 | 20% with indexation | N/A (new rule post Jul 23) | N/A |
| Property / other assets (without indexation) | 112 | N/A (old rule pre Jul 23) | 12.5% without indexation | 12.5% without indexation |
| Property acquired before Jul 23, 2024 — taxpayer's choice | 112 | 20% with idx (if lower) | 12.5% without OR 20% with (whichever lower) | 12.5% only |
| Unlisted equity | 112 | 20% with indexation | 20% with indexation | 20% with indexation |
| Debt MF (pre Apr 1, 2023) | 112 | 20% with indexation | 20% with indexation | 20% with indexation |
| Bonds/debentures (> 24 months) | 112 | 20% with indexation | 20% with indexation | 20% with indexation |

**Grandfathering Computation for Listed Equity (u/s 112A):**
```
Applicable ONLY for assets acquired ON OR BEFORE January 31, 2018

Grandfathered Cost = HIGHER OF:
  (a) Actual cost of acquisition
  (b) LOWER OF:
        (i)  FMV on January 31, 2018 (highest price on NSE/BSE that day)
        (ii) Full value of consideration (actual sale price)

Logic: If asset rose from purchase date to Jan 31, 2018 → use Jan 31, 2018 price as cost
       If asset fell after Jan 31, 2018 → loss is allowable
       
For assets acquired AFTER January 31, 2018: Actual cost only (no grandfathering)
```

**Section 50C — Deemed Consideration for Immovable Property:**
```
If sale price < Stamp Duty Value (SDV / Circle Rate):
  Deemed sale consideration = SDV (not actual sale price)
  
UNLESS: SDV ≤ 110% of actual sale price → use actual sale price (Budget 2020 relief: 10% tolerance)
  
For the BUYER: u/s 56(2)(x), if SDV > 110% of purchase price → excess is taxable as Other Sources
  Buyer reports: SDV − purchase price (if > 10% variation) as income

Section 50CA: For unlisted shares sold below FMV → FMV is deemed consideration
Section 50D: Where consideration not determinable → FMV on date of transfer is deemed consideration
```

**Section 47 — Transactions NOT Treated as Transfer (Capital Gains Exempt):**

| Transaction | Section |
|---|---|
| Gift or will / inheritance | 47(iii) |
| HUF property distribution to members on partition | 47(i) |
| Transfer in course of amalgamation (to amalgamated company) | 47(vi) |
| Transfer between holding/subsidiary companies (100% subsidiary) | 47(iv), (v) |
| Conversion of bonds to shares | 47(x) |
| Reverse mortgage under senior citizen scheme | 47(xvi) |
| Transfer of assets on demerger | 47(vib) |
| Conversion of Indian branch of foreign bank to subsidiary | 47(viia) |

**Section 49 — Cost of Acquisition in Special Cases:**

| Situation | Cost of Acquisition |
|---|---|
| Asset received as gift | Cost to the previous owner (donor) |
| Asset received under Will / inheritance | Cost to the testator |
| HUF partition asset to member | Cost to HUF |
| Bonus shares | NIL (cost = 0) |
| Rights shares | Amount paid to company for rights |
| ESOP shares | Perquisite value on exercise date (used as cost for capital gains computation) |
| Shares received in amalgamation | Cost of original shares |
| Property received from Hindu Undivided Family | Proportionate cost to HUF |

---

### 4.5 Schedule VDA — Virtual Digital Assets (Crypto/NFT)

**Applicable from AY 2023-24; Section 115BBH**

| Field | Details |
|---|---|
| VDA Description | Coin name/NFT name/type |
| Date of Acquisition | DD/MM/YYYY |
| Date of Transfer | DD/MM/YYYY |
| Cost of Acquisition | Purchase price + platform fees |
| Sale Consideration | Selling price |
| Gross Gain | Sale − Cost |
| Loss carried forward? | NO — losses cannot be carried forward |
| Set-off against other income? | NO — VDA loss cannot be set off |
| TDS u/s 194S deducted? | Yes/No; if Yes, amount |

**VDA Tax Computation:**
```
Tax on VDA Income:
  Rate = 30% flat (no basic exemption benefit, no slab rate)
  No deduction allowed except COST OF ACQUISITION
  Gift received as VDA: taxable at 30% on FMV at receipt date u/s 56(2)(x)
  
CANNOT:
  - Set off VDA loss against salary/HP/business/other capital gains
  - Set off VDA loss against other VDA income
  - Carry forward VDA loss to next year
  
TDS u/s 194S:
  Rate: 1% of VDA sale consideration
  Threshold: ₹10,000 per FY (₹50,000 if payer is individual/HUF below audit threshold)
  Deducted by: Crypto exchanges, P2P platforms
  
Report:
  - VDA income in Schedule VDA
  - Tax on VDA in Schedule SI (special rate income)
  - TDS u/s 194S in Schedule TDS2
```

---

### 4.6 Schedule OS (Detailed) — Other Sources

| Income Type | Section | Deductions Available u/s 57 |
|---|---|---|
| Dividend income | 56(2)(i) | COMMISSION/REMUNERATION for collecting dividends — 57(i) — OLD REGIME ONLY |
| Family pension | 57(iia) | 1/3 of pension or ₹15,000 (old) / ₹25,000 (new) |
| Income from letting of machinery/plant | 56(2)(ii) | Repairs, insurance, depreciation — 57(ii) |
| Winning from lottery / gambling | 56(2)(ib) | NO DEDUCTION — 58(4) |
| Any income not under other heads | 56(2)(ix) | Reasonable expenses — 57(iii) |
| Gifts from non-relatives | 56(2)(x) | None |
| Cash credits/investments without explanation | 68/69/69A | None — taxed at 60% u/s 115BBE |
| VDA income | 115BBH | Cost of acquisition only |
| Interest on VPF/EPF (above ₹2.5L/₹5L limit) | 10(11)/10(12) | None |

**EPF/VPF Interest Taxation:**
```
For EPF contributions exceeding ₹2,50,000 per year (non-government employees):
OR exceeding ₹5,00,000 per year (government employees):
  Interest on the EXCESS contribution is taxable as Other Sources
  TDS deducted by EPF Trust at 10%
  
For UAN holders contributing > threshold since AY 2022-23 onwards
```

---

### 4.7 Schedule CYLA — Current Year Loss Adjustment

**Order of Set-Off within Current Year (CRITICAL — Follow This Order):**

1. HP Loss → Set off against: Salary, Other Sources, CG (all types), Business income. **Cap: ₹2,00,000**
2. Non-speculative business loss → Set off against: All heads EXCEPT salary (cannot touch salary). Against: HP, CG (any type), Other Sources
3. Speculative business loss → ONLY against speculative business income
4. STCG loss → Set off against STCG and LTCG of any type
5. LTCG loss → Set off against LTCG ONLY (NOT against STCG)
6. Horse race loss → ONLY against horse race income

```
CYLA Algorithm (per head):
  Step 1: Compute income under each head after intra-head adjustments
  Step 2: Apply HP loss (up to ₹2L) against positive income heads
  Step 3: Apply non-speculative business loss against remaining positive heads (not salary)
  Step 4: Apply STCG loss against CG (STCG first, then LTCG)
  Step 5: Apply LTCG loss against LTCG only
  Step 6: Carry forward remaining unabsorbed losses to Schedule CFL
```

---

### 4.8 Schedule BFLA — Brought Forward Loss Adjustment

| Loss Type | Carry Forward Period | Set Off Against | Notes |
|---|---|---|---|
| Unabsorbed HP loss | 8 Assessment Years | HP income only | Cannot touch salary in subsequent years |
| Non-speculative business loss | 8 Assessment Years | Any business income | NOT salary |
| Speculative business loss | 4 Assessment Years | Speculative income only | — |
| Short-term capital loss | 8 Assessment Years | STCG and LTCG (both) | — |
| Long-term capital loss | 8 Assessment Years | LTCG only | NOT against STCG |
| Unabsorbed depreciation | **UNLIMITED** | Business income first, then any head (except salary) | No time limit |
| Loss from horse races | 4 Assessment Years | Horse race income only | — |

> **CARRY-FORWARD CONDITION:** Loss can be carried forward ONLY if return filed on or before due date. Belated return → NO carry forward (except unabsorbed depreciation and BF losses from prior years already established).

---

### 4.9 Schedule AL — Assets and Liabilities (Mandatory if Income > ₹50L)

Declared as on **March 31** of the FY.

**Asset Side:**

| Asset Category | Fields to Declare |
|---|---|
| Immovable property (land/building) | Full address, cost of acquisition, year of acquisition |
| Movable assets — jewellery, bullion | Description, cost of acquisition |
| Movable assets — vehicles | Vehicle type, cost of acquisition |
| Shares and securities | Type (listed/unlisted), cost as on March 31 |
| Insurance policies | Policy amount, surrender value, nominee |
| Loans and advances given | Borrower name, PAN, outstanding amount |
| Cash in hand | Amount > ₹5,00,000 as on March 31 |
| Bank deposits | Bank name, account number, balance |
| Archaeological items, art, paintings | Description, estimated value |

**Liability Side:**

| Liability Category | Fields |
|---|---|
| Loans taken (secured) | Lender name, PAN, outstanding amount, security offered |
| Loans taken (unsecured) | Lender name, PAN, outstanding amount |
| Other outstanding liabilities | Description and amount |

---

### 4.10 Schedule FA — Foreign Assets Disclosure

**Mandatory for Residents (ROR) owning foreign assets at ANY TIME during the year.**

| Asset Type | Fields Required |
|---|---|
| Foreign bank accounts | Country, bank name, account number, opening/closing balance, peak balance, interest credited |
| Foreign equity/debt | Country, entity name, nature of interest, acquisition cost, income derived |
| Foreign immovable property | Country, address, date of acquisition, cost, income |
| Signing authority in foreign accounts | Country, institution, account details |
| Financial interest in foreign entity | Country, nature of interest |
| Trust or beneficiary of trust | Country, trustee details, value |

> **BLACK MONEY ACT:** Non-disclosure of foreign assets attracts penalty of ₹10,00,000 per asset under Black Money (Undisclosed Foreign Income and Assets) Act 2015, PLUS imprisonment. ERP MUST have a dedicated screen for FA disclosure and mandatory cross-check with client.

---

### 4.11 Schedule FSI and Schedule TR — Foreign Income and Tax Relief

**Schedule FSI — Foreign Source Income:**

| Field | Details |
|---|---|
| Country of Source | ISO country code |
| Nature of Income | Salary / Dividends / Interest / Capital Gains / Business |
| Income from foreign source | Amount in INR (at exchange rate) |
| Foreign tax paid/deducted | Amount in INR |
| Eligible for tax relief? | Yes/No (based on DTAA) |

**Schedule TR — Tax Relief Computation:**

```
Relief u/s 90 (where DTAA exists):
  Method 1 — Exemption: Foreign income exempt in India if exclusively taxed abroad
  Method 2 — Tax Credit: India taxes it but grants credit for foreign tax paid
  
  Tax Credit = LOWER OF:
    (a) Foreign tax paid (converted to INR)
    (b) Indian tax payable on the foreign income (computed using Indian tax rates)

Relief u/s 91 (no DTAA — Unilateral Relief):
  Applicable when income doubly taxed and no DTAA with source country
  Relief = LOWER OF:
    (a) Indian rate of tax on the doubly taxed income
    (b) Foreign country's rate of tax on the income
  
Relief Rate = lower of Indian rate or foreign rate (both as % of income)
```

> **DTAA STATUS:** India has treaties with 90+ countries. ERP should maintain country-wise DTAA database covering applicable Article, method of relief, and rate of withholding tax.

---

### 4.12 Capital Gains Exemptions — Complete Reference

| Section | Asset Sold | Investment Required | Time Limit | Key Conditions & Max Exemption |
|---|---|---|---|---|
| 54 | Residential house property | Purchase new residential house | 1yr before / 2yrs after sale. Construction: 3yrs after | Only 1 new house; amount of CG or cost of new house (lower); must not sell new house for 3 years |
| 54B | Agricultural land (rural or urban) | Agricultural land | 2 years from date of sale | Assessee (or parent/spouse) must have used land for agriculture for 2 years before sale |
| 54D | Industrial land/building | Industrial land/building | 3 years from date of sale | Compulsory acquisition cases; new asset used for the same business |
| 54EC | Land/building (LTCG) | Specified bonds (NHAI, REC, PFC, IRFC notified bonds) | 6 months from date of sale | Max ₹50,00,000 per FY (if sale straddles 2 FYs, max ₹50L per FY). Bonds held for 5 years; no premature redemption |
| 54EE | LTCG on any asset | Units of specified fund (notified) | 6 months from sale | Max ₹50,00,000; fund notified u/s 10(23FB) |
| 54F | Any LTCG asset (except residential HP) | Residential house property | 1yr before / 2yrs after. Construction: 3yrs | Proportionate exemption if net consideration not fully invested; cannot own more than 1 other residential HP at time of sale |
| 54G | Industrial undertaking (urban to rural) | P&M / building / other assets | 1yr before / 3yrs after | Industrial relocation from urban area |
| 54GA | Industrial undertaking in Special Economic Zone | Assets in SEZ | 1yr before / 3yrs after | Migration to SEZ |
| 54GB | Residential property | Investment in eligible startup equity (DPIIT registered) | Before due date of filing ITR | Holding: 5 years in startup; startup must use money for purchase of new assets |

**Capital Gains Account Scheme (CGAS) — Critical:**
```
When:
  Capital gain is realized in one FY
  Eligible investment (54/54F/54EC etc.) not made before ITR due date

Then:
  Deposit the unspent capital gain amount in CGAS (Deposit A or B):
  - Deposit A: Savings account type (withdraw as needed)
  - Deposit B: Term deposit (withdraw in specified installments)
  
  The deposit in CGAS = exemption preserved

If CGAS deposit not made before ITR due date:
  Exemption NOT available even if investment made later

If investment from CGAS not utilized within prescribed time:
  The unutilized CGAS amount becomes taxable in year of expiry
  
ERP must track: (a) CGAS deposit date, (b) planned investment deadline, 
               (c) actual investment made
```

---

### 4.13 Schedule SI — Special Rate Income Tax Computation

When income includes special rate income (111A, 112, 112A, 115BB, 115BBH):

**Tax Computation Sequence for Mixed Income:**

```
Step 1: Separate total income into:
  (a) Normal income (taxable at slab rates)
  (b) STCG u/s 111A at 15%/20%
  (c) LTCG u/s 112A at 10%/12.5%
  (d) LTCG u/s 112 at 12.5% or 20% with idx
  (e) VDA income u/s 115BBH at 30%
  (f) Lottery u/s 115BB at 30%

Step 2: Utilize basic exemption against income in order: (a) first, then (b), (c), etc.
  (Only the portion of basic exemption not covered by normal income can be set against CG)

Step 3: Tax on (a) = Apply slab rates on net normal income after basic exemption
Step 4: Tax on (b) = STCG × applicable rate (after utilizing unused exemption)
Step 5: Tax on (c) = LTCG × rate after DEDUCTING ₹1,00,000/₹1,25,000 annual exemption
Step 6: Tax on (d), (e), (f) = respective flat rates

Step 7: Check 87A rebate:
  For old regime: If (a) ≤ ₹5L → rebate up to ₹12,500 on tax on (a) ONLY
  For new regime AY25-26: If (a) ≤ ₹7L → rebate up to ₹25,000 on tax on (a) ONLY
  For new regime AY26-27: If (a) ≤ ₹12L → rebate up to ₹60,000 on tax on (a) ONLY
  Rebate NEVER applies to tax on (b) through (f)

Step 8: Total Tax = Tax on (a) - Rebate + Tax on (b) + Tax on (c) + Tax on (d) + ...
Step 9: Surcharge on total tax (with 15% cap on surcharge for 111A / 112A)
Step 10: Cess @ 4% on (tax + surcharge)
```

**LTCG ₹1,25,000 Exemption (AY 2025-26, post Jul 23, 2024 sales):**
```
The ₹1,25,000 LTCG annual exemption under 112A is:
  - Aggregate across all 112A sales during the year
  - Applied BEFORE computing 12.5% tax
  - NOT a deduction under Chapter VI-A — it's a threshold
  - Pro-rated for split-rate year (AY 2025-26):
    If selling both before and after July 23, 2024:
    The ₹1,00,000 exemption applies to pre-Jul 23 gains (10% rate)
    The ₹1,25,000 exemption applies to post-Jul 23 gains (12.5% rate)
    ERP should use the AGGREGATE ₹1,25,000 as a single pool threshold
```

---

### 4.14 Schedule AMT and AMTC

| Field | Details |
|---|---|
| Total Income as per ITR | From Schedule GTI |
| Add: Deduction u/s 10AA | If SEZ unit income exempt |
| Add: Deduction u/s 35AD | If specified business deduction claimed |
| Add: Deduction u/s 80H to 80RRB (except 80P) | All profit-linked deductions |
| Adjusted Total Income (ATI) | Sum above |
| AMT @ 18.5% on ATI | If ATI > ₹20,00,000 |
| Regular Income Tax | From tax computation |
| AMT applicable? | If AMT > Regular Tax AND ATI > ₹20,00,000 |
| Tax payable | Higher of AMT and Regular Tax |
| AMT Credit b/f | From prior years (max 15 years) |
| AMT Credit utilized this year | If Regular Tax > AMT |
| AMT Credit c/f | For future years |

---

## SECTION 5 — SALARY INCOME DEEP DIVE

### 5.1 Components of Salary u/s 17(1)

| Component | Taxable / Exempt |
|---|---|
| Basic salary | Fully taxable |
| Dearness Allowance (DA) | Fully taxable |
| HRA (see 10(13A) computation above) | Partially exempt |
| Bonus, commission, incentives | Fully taxable |
| Special allowance | Fully taxable unless specifically exempt |
| Children Education Allowance | Exempt up to ₹100/month per child (max 2 children) |
| Children Hostel Allowance | Exempt up to ₹300/month per child (max 2 children) |
| Transport allowance (general) | Fully taxable from AY 2019-20 |
| Transport allowance (handicapped) | Exempt up to ₹3,200/month |
| Underground mines allowance | Exempt up to notified limit |
| High altitude allowance (army/paramilitary) | Exempt as notified |
| Tribal/scheduled area allowance | Exempt as notified |
| Remote area / border area allowance | Exempt as notified |
| Conveyance allowance (actual expenses) | Exempt for actual travel for official duty |
| Uniform allowance (official uniform only) | Exempt for actual maintenance of uniform |
| Academic/research allowance | Exempt for actual expenses on research |
| Fixed Medical Allowance (non-reimbursement) | Fully taxable |
| Medical reimbursement | Taxable (exempt treatment pre-AY 2019-20 — now no blanket exemption) |

---

### 5.2 Perquisites u/s 17(2) — Valuation

| Perquisite | Valuation Method |
|---|---|
| Rent-Free Accommodation (Govt) | License fee as per Govt rules |
| RFA (Non-Govt, unfurnished, owned by employer) | 15% of salary (metro/pop > 25L) / 10% (pop 10L to 25L) / 7.5% (other) |
| RFA (Non-Govt, unfurnished, leased by employer) | Actual rent paid by employer OR 15%/10%/7.5% of salary — LOWER |
| Furnished accommodation | Add 10% of original furniture cost per year (or actual rent of furniture) to above |
| Concessional accommodation | Difference between fair rental value and amount charged from employee |
| Car (owned by employer, use for both official and personal) | Engine up to 1600cc: ₹1,800/month; Engine > 1600cc: ₹2,400/month; Plus ₹900 if chauffeur provided |
| Car (owned by employer, only personal use) | Actual running + maintenance + depreciation @ 10% WDV |
| Car owned by employee but expenses reimbursed | ₹1,800/month (≤1600cc) or ₹2,400/month (>1600cc) |
| ESOP perquisite | FMV on date of exercise − exercise price (taxable in year of exercise) |
| Interest-free / concessional loan | Interest at SBI PLR on the outstanding balance monthly average |
| Gift vouchers or tokens | Fully taxable; no exemption limit for gifts in kind > ₹5,000 per year |
| Club membership fee (personal) | Actual fee paid by employer |
| Free/concessional meals (non-canteen) | Fair market price minus amount paid by employee |
| Canteen meals (employer canteen) | Exempt up to ₹50 per meal |
| Free education for children of employee | Exempt up to ₹1,000/month per child in educational institution |
| Laptop/computer for official use | EXEMPT — not a perquisite |

---

### 5.3 ESOP — Complete Treatment

**Year of Exercise (When Shares are Allotted):**
```
Perquisite Value = FMV on exercise date − Exercise price paid
This perquisite is taxable as SALARY INCOME in the year of exercise
TDS at applicable rate is deducted by employer on this perquisite value
FORM 16 must include this perquisite under Section 17(2)

For ESOP from DPIIT-recognized eligible startups (u/s 192(1C)):
  Tax on ESOP perquisite is deferred to EARLIEST OF:
  (a) Expiry of 48 months from end of relevant AY
  (b) Date of leaving employment
  (c) Date of sale of ESOP shares
  TDS also deferred accordingly
  File ITR-2 (not ITR-1) in such cases
```

**Year of Sale of ESOP Shares (Capital Gains):**
```
Cost of Acquisition = FMV on exercise date (the perquisite value)
Sale Price = Actual sale consideration

If sold within 12 months: STCG (at 15%/20% if listed, STT paid)
If sold after 12 months: LTCG (at 10%/12.5% if listed, STT paid)

Note: The FMV on exercise date becomes the cost base — no double taxation
```

---

### 5.4 Gratuity Exemption — Complete Computation

**For Non-Government Employees (covered under Payment of Gratuity Act 1972):**
```
Exempt Gratuity = MINIMUM OF:
  (a) 15/26 × Last Drawn Salary* × Number of completed years of service
      [Last Drawn Salary* = Basic + DA only]
      [Completed years = full years; fraction ≥ 6 months = 1 year]
  (b) ₹20,00,000 (enhanced from ₹10L in Budget 2023)
  (c) Actual gratuity received

Excess over (a), (b), (c) above = Taxable under Salaries u/s 17(3)
```

**For Non-Government Employees (not covered under PGA 1972):**
```
Exempt Gratuity = MINIMUM OF:
  (a) ½ × Average salary* × Number of completed years of service
      [Average salary = Average of last 10 months salary; fractions not counted]
  (b) ₹20,00,000
  (c) Actual gratuity received

*Salary here = Basic + DA (forming part of retirement benefits) + turnover-based commission
```

**For Government Employees:**
```
Entire gratuity is EXEMPT u/s 10(10)(i)
No ceiling applies to central/state government employees
```

---

### 5.5 Leave Encashment Exemption u/s 10(10AA)

**At Retirement/Superannuation:**
```
For Government Employees:
  Entire amount EXEMPT u/s 10(10AA)(i)

For Non-Government Employees:
  Exempt = MINIMUM OF:
  (a) Actual leave encashment received
  (b) ₹25,00,000 (enhanced by Budget 2023 from ₹3,00,000)
  (c) 10 months' average salary (average of last 10 months)
  (d) Cash equivalent of unavailed leave = (Salary/month × Unavailed leave days)
      where maximum leave credited = 30 days per year of service
      
  Average Salary = Average of last 10 months' salary
  Salary = Basic + DA (for retirement) + commission on fixed % of turnover
```

---

### 5.6 Pension — Types and Taxability

| Type | Taxability |
|---|---|
| Commuted pension (Govt employee) | Fully EXEMPT u/s 10(10A)(i) |
| Commuted pension (Non-Govt, receiving gratuity) | 1/3 of commuted value exempt |
| Commuted pension (Non-Govt, NOT receiving gratuity) | 1/2 of commuted value exempt |
| Uncommuted (regular monthly) pension | Fully TAXABLE as salary |
| Family pension (pension received by family after death of employee) | Taxable as "Other Sources"; standard deduction applies |
| Annuity from life insurance company | Taxable as salary (if from employer scheme) or other sources |
| NPS pension annuity (from NPS corpus) | Taxable as salary or other sources depending on scheme |
| NPS lump sum withdrawal (60% at 60 years) | EXEMPT u/s 10(12A) (for Tier I NPS) |

---

*[Part 1 ends here — Sections 1–5. Continue with Part 2 for ITR-3, ITR-4, TDS Reference, Capital Gains Deep Dive, and Complete Computation Sequences]*
