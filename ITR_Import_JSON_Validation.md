# INCOME TAX ERP — IMPORT ENGINE, JSON SCHEMA & VALIDATION REFERENCE
## Form 16 PDF Import | AIS / TIS / 26AS Import | Prefill JSON | ITR Output JSON | Validation Rules ITR-1 to ITR-4
### AY 2025-26 & AY 2026-27 | Based on CBDT Validation Rules V1.1 (10 July 2025)

> **LEGAL BASIS:** CBDT e-Filing Validation Rules for ITR-1 (V1.1, July 2025), ITR-2 (V1.0, July 2025), ITR-3 (V1.0, July 2025), ITR-4 (V1.1, July 2025). ITD Prefill JSON Schema, AIS/TIS Technical Specifications. All validation rules marked **[CAT-A]** cause upload rejection. Rules marked **[CAT-B]** generate defective return notice u/s 139(9). Rules marked **[CAT-D]** generate deduction disallowance warning.

---

## SECTION 1 — FORM 16 PDF IMPORT ENGINE

### 1.1 Form 16 Structure Overview

Form 16 has two parts that must be parsed independently:

| Part | Source | Purpose | Issued By |
|---|---|---|---|
| **Part A** | Generated from TRACES portal | TDS certificate — challan-wise TDS deposited, employer TAN, employee PAN, quarterly breakup | Employer (via TRACES download) |
| **Part B** | Employer-generated | Salary computation — gross salary, perquisites, exemptions, deductions, net taxable salary | Employer (prepared manually or via payroll) |

> **CRITICAL:** Part A is the authoritative TDS record and matches 26AS. Part B is the salary computation. Both are mandatory. The system must parse and cross-validate both parts.

---

### 1.2 Form 16 Part A — Fields to Extract

```json
{
  "form16_partA": {
    "certificate_number": "String — unique TRACES certificate number",
    "tan_of_employer": "String[10] — format: AAAA99999A — mandatory",
    "pan_of_employer": "String[10] — format: AAAAA9999A",
    "employer_name": "String[75]",
    "employer_address": {
      "flat_door": "String",
      "street": "String",
      "locality": "String",
      "city": "String",
      "state": "String",
      "pincode": "String[6]",
      "email": "String",
      "phone": "String"
    },
    "pan_of_employee": "String[10] — MUST match taxpayer PAN in ITR",
    "employee_name": "String[75] — MUST match PAN database name",
    "assessment_year": "String — e.g. 2025-26",
    "financial_year": "String — e.g. 2024-25",
    "period_of_employment": {
      "from": "String[DD/MM/YYYY]",
      "to": "String[DD/MM/YYYY]"
    },
    "quarterly_tds_breakup": [
      {
        "quarter": "Q1|Q2|Q3|Q4",
        "date_of_payment_credit": "String[DD/MM/YYYY]",
        "date_of_tds_deposit": "String[DD/MM/YYYY]",
        "bsr_code": "String[7]",
        "challan_serial_number": "String",
        "amount_of_tax_deposited": "Integer — in paise? No. In rupees, no decimals",
        "remarks": "String — A/C = nil deduction or lower deduction"
      }
    ],
    "total_amount_of_tax_deposited_credited": "Integer — sum of quarterly deposits",
    "aggregate_amount_of_salary_paid": "Integer — gross salary per this TAN",
    "aggregate_tds_deposited": "Integer",
    "acknowledgement_number_traces": "String[15]",
    "date_of_issue": "String[DD/MM/YYYY]"
  }
}
```

**Part A Cross-Validation Rules:**
```
VAL-F16A-001: tan_of_employer format = [A-Z]{4}[0-9]{5}[A-Z]{1}
VAL-F16A-002: pan_of_employee must equal taxpayer's PAN in ITR (exact match)
VAL-F16A-003: total_amount_of_tax_deposited_credited == SUM(quarterly_tds_breakup[].amount_of_tax_deposited)
VAL-F16A-004: assessment_year must be "2025-26" or "2026-27"
VAL-F16A-005: period_of_employment.from must be >= 01/04/{FY_start_year}
VAL-F16A-006: period_of_employment.to must be <= 31/03/{FY_end_year}
VAL-F16A-007: aggregate_tds_deposited must match sum of challan entries
VAL-F16A-008: All quarterly dates must fall within the relevant FY
VAL-F16A-009: Form 16 Part A must be downloaded from TRACES (verify TRACES watermark if available)
```

---

### 1.3 Form 16 Part B — Fields to Extract

```json
{
  "form16_partB": {
    "employee_pan": "String[10]",
    "employer_tan": "String[10]",
    "assessment_year": "String",
    "nature_of_employment": "CentralGovt|StateGovt|PSU|CG-Pensioner|SG-Pensioner|PSU-Pensioner|OtherPensioner|Others|NotApplicable",

    "schedule_salary": {
      "salary_17_1": {
        "basic_salary": "Integer",
        "dearness_allowance": "Integer",
        "house_rent_allowance": "Integer",
        "leave_travel_allowance": "Integer",
        "leave_encashment": "Integer",
        "gratuity": "Integer",
        "commission": "Integer",
        "bonus": "Integer",
        "other_allowances": "Integer",
        "arrears_of_salary": "Integer",
        "retirement_benefit_account_notified_country_89A": "Integer",
        "retirement_benefit_account_other_country_89A": "Integer",
        "total_17_1": "Integer — MUST equal sum of above"
      },
      "perquisites_17_2": {
        "accommodation_rent_free_concessional": "Integer",
        "cars_other_transport": "Integer",
        "sweeper_gardener_watchman_personal_attendant": "Integer",
        "gas_electricity_water": "Integer",
        "interest_free_concessional_loan": "Integer",
        "holiday_expenses": "Integer",
        "free_or_concessional_educational_facility": "Integer",
        "free_meals": "Integer",
        "free_gifts_vouchers_tokens": "Integer",
        "credit_card_expenses": "Integer",
        "club_expenses": "Integer",
        "use_of_movable_assets": "Integer",
        "transfer_of_movable_assets": "Integer",
        "value_of_other_benefits_amenities": "Integer",
        "tax_paid_on_non_monetary_perquisites_10_10CC": "Integer",
        "esop_value": "Integer — FMV on exercise minus exercise price",
        "total_17_2": "Integer — MUST equal sum of above"
      },
      "profits_in_lieu_17_3": {
        "compensation_on_termination": "Integer",
        "gratuity_excess": "Integer",
        "leave_encashment_excess": "Integer",
        "vrs_compensation_taxable": "Integer",
        "any_amount_received_before_employment": "Integer",
        "payment_received_from_unrecognized_pf": "Integer",
        "total_17_3": "Integer"
      },
      "gross_salary": "Integer — MUST equal 17_1 total + 17_2 total + 17_3 total + 89A amounts",

      "allowances_exempt_under_10": {
        "sec_10_5_lta": "Integer — Leave Travel Concession",
        "sec_10_6_embassy_remuneration": "Integer",
        "sec_10_7_allowances_outside_india": "Integer",
        "sec_10_10_gratuity": "Integer — exempt gratuity (max 20L non-govt/25L govt)",
        "sec_10_10A_commuted_pension": "Integer",
        "sec_10_10AA_leave_encashment": "Integer — max 25L non-govt",
        "sec_10_10B_i_retrenchment_compensation": "Integer — max 5L",
        "sec_10_10B_ii_approved_scheme": "Integer — max 5L",
        "sec_10_10C_vrs": "Integer — max 5L, only one of 10B/10C claimable",
        "sec_10_10CC_tax_on_non_monetary_perquisite": "Integer",
        "sec_10_13A_hra": "Integer — HRA exemption (see formula)",
        "sec_10_14_i_prescribed_allowances_wholly_incurred": "Integer",
        "sec_10_14_ii_personal_expenses_allowances": "Integer",
        "sec_10_14_ii_transport_handicapped": "Integer — max 38,400 per year",
        "children_education_allowance": "Integer — max 100/month/child × 2 children",
        "children_hostel_allowance": "Integer — max 300/month/child × 2 children",
        "underground_mine_allowance": "Integer",
        "any_other_exempt_allowance": "Integer",
        "total_exempt_allowances": "Integer — MUST equal sum of above"
      },
      "relief_89A": "Integer — Income from retirement benefit account in notified country",
      "net_salary": "Integer — gross_salary - total_exempt_allowances - relief_89A",

      "deductions_u_s_16": {
        "standard_deduction_16_ia": "Integer — max 50000 (old) / 75000 (new)",
        "entertainment_allowance_16_ii": "Integer — govt only; max min(EA, 1/5 salary, 5000)",
        "professional_tax_16_iii": "Integer — max 2500 (old regime only)"
      },
      "total_deductions_16": "Integer — MUST equal sum of 16(ia)+16(ii)+16(iii)",
      "income_from_salaries": "Integer — net_salary - total_deductions_16"
    },

    "other_income_disclosed_by_employee": {
      "income_from_house_property": "Integer — may be negative",
      "income_from_other_sources": "Integer",
      "loss_from_hp_set_off": "Integer — up to 2L can be set off against salary"
    },
    "gross_total_income_for_tds_purpose": "Integer",

    "deductions_for_tds_purposes": {
      "sec_80C": "Integer — max 150000",
      "sec_80CCC": "Integer — within 80C limit",
      "sec_80CCD_1_employee_nps": "Integer — within 80C limit (10% of salary)",
      "sec_80CCD_1B_additional_nps": "Integer — max 50000",
      "sec_80CCD_2_employer_nps": "Integer — 10%/14% of salary",
      "sec_80D_health_insurance": "Integer",
      "sec_80DD_disabled_dependent": "Integer",
      "sec_80DDB_specified_disease": "Integer",
      "sec_80E_education_loan_interest": "Integer",
      "sec_80EE_housing_loan_first_buyer": "Integer",
      "sec_80EEA_affordable_housing": "Integer",
      "sec_80G_donations": "Integer",
      "sec_80TTA_savings_interest": "Integer — max 10000",
      "sec_80TTB_senior_interest": "Integer — max 50000",
      "sec_80U_self_disabled": "Integer",
      "total_chapter_vi_a": "Integer — MUST equal sum above; MUST NOT exceed GTI"
    },

    "tax_computation": {
      "total_income": "Integer — GTI minus Chapter VI-A",
      "tax_on_total_income": "Integer — slab computation",
      "surcharge": "Integer",
      "health_education_cess": "Integer — 4% of (tax + surcharge)",
      "gross_tax_liability": "Integer",
      "relief_u_s_89": "Integer — salary arrears relief",
      "net_tax_payable": "Integer — after relief",
      "tds_deducted_and_deposited_this_employer": "Integer",
      "tds_deducted_previous_employer_if_any": "Integer",
      "total_tds_for_year": "Integer",
      "balance_tax_payable_refundable": "Integer"
    }
  }
}
```

---

### 1.4 Form 16 PDF Parsing Strategy (Technical)

**PDF Parser Architecture:**
```
STEP 1: PDF Text Extraction
  Tool: pdf-parse (Node.js) or pdfplumber (Python)
  Strategy: 
    - Detect Form 16 Part A vs Part B by looking for:
      * Part A: "TRACES" watermark, "Certificate Number", "BSR Code", quarterly table
      * Part B: "Schedule of Salary", "Allowances to the extent exempt", "Computation of Income"
    
STEP 2: Layout Detection
  Forms from different employers use different layouts.
  Use keyword anchors:
    * "Salary as per provisions contained in section 17(1)" → maps to salary_17_1
    * "Value of perquisites under section 17(2)" → maps to perquisites_17_2
    * "Profits in lieu of salary under section 17(3)" → maps to profits_17_3
    * "Allowances to the extent exempt under section 10" → exempt allowances section
    * "Standard Deduction u/s 16(ia)" → standard_deduction_16_ia
    * "Net amount deducted (TDS)" → tds_deducted_and_deposited

STEP 3: Amount Extraction Rules
  - All amounts are in whole rupees (no paise)
  - Look for RIGHT-ALIGNED numbers in tabular columns
  - Handle negative amounts: shown as (xxx) or -xxx
  - Remove commas from thousands separator: "1,50,000" → 150000
  - Handle blank/dash as zero
  
STEP 4: Multi-employer Consolidation
  - If taxpayer has multiple Form 16s (multiple employers):
    * Create separate partB objects for each employer
    * Standard deduction claimed only ONCE in aggregation (use max applicable)
    * Sum all salary components across employers for gross salary
    * Combine TDS from all Part-As
    
STEP 5: Post-Extraction Verification
  - Cross-check: Part A TDS == Part B TDS deducted
  - Cross-check: Part B gross salary is arithmetically consistent
  - Raise flags for any discrepancy > ₹1
```

**HRA Computation Engine (invoked during Part B parse):**
```javascript
function computeHRAExemption(hra_received, basic_da, rent_paid, city_type) {
  // city_type: "METRO" (Delhi/Mumbai/Kolkata/Chennai) or "NON_METRO"
  const salary_pct = city_type === "METRO" ? 0.50 : 0.40;
  const component_1 = hra_received;
  const component_2 = Math.max(0, rent_paid - (0.10 * basic_da));
  const component_3 = salary_pct * basic_da;
  const exemption = Math.min(component_1, component_2, component_3);
  return Math.max(0, exemption); // cannot be negative
}
// Validation: if rent_paid < 10% of basic_da → component_2 = 0 → exemption = 0
```

---

## SECTION 2 — AIS / TIS / 26AS IMPORT ENGINE

### 2.1 Data Source Hierarchy and Priority

| Source | Format | Priority | Scope |
|---|---|---|---|
| **AIS (Annual Information Statement)** | JSON / PDF (from portal) | HIGHEST | Most comprehensive — includes SFT, TDS, TCS, dividends, securities, property, GST |
| **TIS (Taxpayer Information Summary)** | JSON (from portal) | MEDIUM | Aggregated AIS — shows taxpayer feedback incorporated |
| **Form 26AS** | PDF / Text / JSON (from TRACES) | BASELINE | TDS/TCS, advance tax, self-assessment tax — authoritative for TDS credits |
| **Form 16 (as above)** | PDF | SALARY-SPECIFIC | Salary TDS only |

> **RECONCILIATION RULE:** When AIS and Form 26AS differ, **Form 26AS is authoritative for TDS/TCS credits**. AIS is authoritative for **income sources not covered by TDS** (dividends, share transactions, property, etc.).

---

### 2.2 AIS JSON Structure — Complete Schema

```json
{
  "ais_data": {
    "taxpayer_pan": "String[10]",
    "taxpayer_name": "String",
    "assessment_year": "String — e.g. 2025-26",
    "financial_year": "String — e.g. 2024-25",
    "generated_date": "String[DD/MM/YYYY]",
    "version": "String",

    "tds_tcs": {
      "tds_salary": [
        {
          "deductor_name": "String",
          "deductor_tan": "String[10]",
          "deductor_pan": "String[10]",
          "section": "192",
          "amount_paid_credited": "Integer",
          "tds_deducted": "Integer",
          "tds_deposited": "Integer",
          "quarter": "Q1|Q2|Q3|Q4",
          "financial_year": "String",
          "ais_feedback": "CORRECT|PARTIALLY_INCORRECT|NOT_RELATED|DUPLICATE|DENIED"
        }
      ],
      "tds_other_than_salary": [
        {
          "deductor_name": "String",
          "deductor_tan": "String[10]",
          "section": "194A|194|194K|194H|194J|194C|194N|194O|194S|194R|194T|...",
          "nature_of_payment": "Interest|Dividend|Commission|Rent|Professional|Contractor|...",
          "amount_paid_credited": "Integer",
          "tds_deducted": "Integer",
          "tds_deposited": "Integer",
          "gross_receipt_in_hands_of_recipient": "Integer",
          "financial_year": "String",
          "ais_feedback": "String"
        }
      ],
      "tcs_collected": [
        {
          "collector_name": "String",
          "collector_tan": "String[10]",
          "section": "206C",
          "nature_of_collection": "LRS|MotorVehicle|Scrap|...",
          "amount_collected": "Integer",
          "tcs_deposited": "Integer",
          "financial_year": "String"
        }
      ]
    },

    "sft_information": {
      "SFT_001_savings_deposits": [
        {
          "reporting_entity": "String — Bank/NBFC name",
          "reporting_entity_pan": "String[10]",
          "account_number": "String",
          "amount_credited": "Integer",
          "amount_debited": "Integer",
          "cash_deposits_savings": "Integer"
        }
      ],
      "SFT_002_fixed_deposits": [
        {
          "reporting_entity": "String",
          "amount_deposited": "Integer",
          "account_number": "String"
        }
      ],
      "SFT_003_credit_card_payment": [
        {
          "bank_name": "String",
          "credit_card_number_masked": "String",
          "payment_amount": "Integer",
          "mode": "Cash|Account"
        }
      ],
      "SFT_004_prepaid_instruments": [
        {
          "entity_name": "String",
          "amount": "Integer"
        }
      ],
      "SFT_005_mutual_fund_purchase": [
        {
          "amc_name": "String",
          "folio_number": "String",
          "scheme_name": "String",
          "amount_purchased": "Integer",
          "redemption_amount": "Integer"
        }
      ],
      "SFT_006_share_purchase": [
        {
          "broker_name": "String",
          "isin": "String[12]",
          "scrip_name": "String",
          "purchase_quantity": "Integer",
          "purchase_value": "Integer",
          "sale_quantity": "Integer",
          "sale_value": "Integer",
          "exchange": "NSE|BSE"
        }
      ],
      "SFT_007_buy_back": "Array — same structure",
      "SFT_010_mf_dividends": [
        {
          "amc_name": "String",
          "folio_number": "String",
          "dividend_amount": "Integer",
          "tds_deducted": "Integer"
        }
      ],
      "SFT_011_dividend_shares": [
        {
          "company_name": "String",
          "company_pan": "String[10]",
          "dividend_amount": "Integer",
          "tds_deducted": "Integer",
          "section": "194"
        }
      ],
      "SFT_012_immovable_property_purchase": [
        {
          "property_address": "String",
          "buyer_share_pct": "Float",
          "registration_value": "Integer",
          "stamp_duty_value": "Integer",
          "tds_deducted_194IA": "Integer",
          "registration_date": "String[DD/MM/YYYY]"
        }
      ],
      "SFT_013_immovable_property_sale": [
        {
          "property_address": "String",
          "seller_share_pct": "Float",
          "sale_consideration": "Integer",
          "stamp_duty_value": "Integer",
          "date_of_sale": "String[DD/MM/YYYY]"
        }
      ],
      "SFT_016_cash_payment": "Object — cash payments > 2L",
      "SFT_017_foreign_remittance_lrs": [
        {
          "bank_name": "String",
          "purpose": "String",
          "amount_inr": "Integer",
          "tcs_collected_206C_1G": "Integer"
        }
      ]
    },

    "other_information": {
      "gst_turnover": [
        {
          "gstin": "String[15]",
          "turnover_gstr1": "Integer",
          "turnover_gstr3b": "Integer",
          "financial_year": "String"
        }
      ],
      "interest_income": [
        {
          "source_name": "String",
          "source_pan_tan": "String",
          "interest_type": "SavingsAccount|FD|RD|NSC|SCSS|P2P|Other",
          "amount": "Integer",
          "tds_deducted": "Integer"
        }
      ],
      "business_receipts": [
        {
          "source": "String",
          "amount": "Integer",
          "nature": "String"
        }
      ],
      "rent_income": [
        {
          "tenant_name": "String",
          "tenant_pan": "String",
          "annual_rent": "Integer",
          "tds_deducted_194I": "Integer"
        }
      ],
      "income_from_other_sources": [
        {
          "nature": "String",
          "payer_name": "String",
          "amount": "Integer"
        }
      ],
      "advance_tax": [
        {
          "bsr_code": "String[7]",
          "challan_date": "String[DD/MM/YYYY]",
          "challan_serial": "String",
          "amount": "Integer",
          "type": "AdvanceTax|SelfAssessmentTax|RegularAssessmentTax"
        }
      ],
      "vda_transactions": [
        {
          "exchange_platform": "String",
          "sale_value": "Integer",
          "purchase_cost": "Integer",
          "gain_loss": "Integer",
          "tds_194S": "Integer",
          "date_of_transaction": "String[DD/MM/YYYY]"
        }
      ]
    }
  }
}
```

---

### 2.3 Form 26AS JSON Structure (TRACES Download)

```json
{
  "form26AS": {
    "pan": "String[10]",
    "assessment_year": "String",
    "generated_on": "String[DD/MM/YYYY]",

    "partA_tds_salary": [
      {
        "sl_no": "Integer",
        "name_of_deductor": "String",
        "tan_of_deductor": "String[10]",
        "total_amount_credited": "Integer",
        "total_tax_deducted": "Integer",
        "total_tds_deposited": "Integer"
      }
    ],
    "partA_tds_other": [
      {
        "sl_no": "Integer",
        "name_of_deductor": "String",
        "tan_of_deductor": "String[10]",
        "section": "String",
        "total_amount_credited": "Integer",
        "total_tax_deducted": "Integer",
        "total_tds_deposited": "Integer"
      }
    ],
    "partB_tcs": [
      {
        "sl_no": "Integer",
        "name_of_collector": "String",
        "tan_of_collector": "String[10]",
        "section": "String",
        "total_amount_subject_to_tcs": "Integer",
        "total_tcs_collected": "Integer",
        "total_tcs_deposited": "Integer"
      }
    ],
    "partC_advance_self_assessment_tax": [
      {
        "sl_no": "Integer",
        "bsr_code": "String[7]",
        "date_of_deposit": "String[DD/MM/YYYY]",
        "challan_serial_number": "String",
        "type": "AdvanceTax(200)|SelfAssessmentTax(300)|RegularAssessmentTax(400)",
        "amount": "Integer",
        "remarks": "String"
      }
    ],
    "partD_refunds": [
      {
        "assessment_year": "String",
        "mode": "String",
        "amount_of_refund": "Integer",
        "date_of_payment": "String[DD/MM/YYYY]",
        "remarks": "String"
      }
    ],
    "partE_sft_high_value": [
      {
        "transaction_type": "String",
        "reporting_entity_name": "String",
        "reporting_entity_pan": "String[10]",
        "transaction_date": "String[DD/MM/YYYY]",
        "amount": "Integer",
        "remarks": "String"
      }
    ],
    "partF_tds_on_sale_property_194IA": [
      {
        "name_of_buyer": "String",
        "pan_of_buyer": "String[10]",
        "transaction_date": "String[DD/MM/YYYY]",
        "consideration_amount": "Integer",
        "tds_deducted": "Integer"
      }
    ],
    "partG_tds_on_rent_194IB": [
      {
        "name_of_tenant": "String",
        "pan_of_tenant": "String[10]",
        "rent_amount": "Integer",
        "tds_deducted": "Integer",
        "period": "String"
      }
    ]
  }
}
```

---

### 2.4 AIS/26AS Data Mapping to ITR Schedules

| AIS/26AS Source | Maps to ITR Schedule | Field | Notes |
|---|---|---|---|
| partA_tds_salary | Schedule TDS1 | TDS on Salary | Match TAN, amount |
| partA_tds_other (194A banks) | Schedule TDS2 | TDS on Interest | Group by TAN |
| partA_tds_other (194 dividend) | Schedule TDS2 | TDS on Dividend | Group by deductor |
| partA_tds_other (194S VDA) | Schedule TDS3 | TDS on VDA | Separate schedule |
| partB_tcs | Schedule TCS | TCS Credit | Use for FY only |
| partC advance tax | Schedule IT | Advance Tax | Match BSR+serial |
| SFT_011 dividends | Schedule OS / B3(e) | Dividend Income | Gross up by TDS |
| SFT_005 MF dividends | Schedule OS / B3(e) | MF Dividend | Gross up |
| SFT_006 share transactions | Schedule CG | Capital Gains | STCG/LTCG by holding period |
| SFT_012 property purchase | Schedule AL | Asset disclosure | If income > 50L |
| SFT_013 property sale | Schedule CG | Capital Gains | Property STCG/LTCG |
| SFT_017 LRS | Schedule FSI/FA | Foreign Remittance | For NR/foreign income |
| vda_transactions | Schedule VDA | VDA Income | 30% tax + TDS 194S |
| gst_turnover | Schedule GST / ITR-3 BP | Turnover Reconciliation | Reconcile with P&L |
| interest_income FD | B3(b) / Schedule OS | FD Interest | Accrual basis |
| interest_income savings | B3(a) | Savings Interest | 80TTA/80TTB |
| rent_income | Schedule HP | HP Income | Gross rent |

---

### 2.5 AIS Reconciliation Engine

```
RECONCILIATION_ALGORITHM:

FOR EACH AIS_ENTRY in ais_data:
  1. Find matching ITR_FIELD (using mapping table above)
  2. Compare: AIS_AMOUNT vs DECLARED_AMOUNT
  3. Compute variance = AIS_AMOUNT - DECLARED_AMOUNT
  
  IF variance > TOLERANCE_THRESHOLD (₹100):
    IF ais_feedback == "CORRECT":
      FLAG: "UNDECLARED_INCOME" — must be added to ITR
    IF ais_feedback == "PARTIALLY_INCORRECT":
      FLAG: "PARTIAL_MISMATCH" — require taxpayer confirmation
    IF ais_feedback in ["NOT_RELATED", "DUPLICATE"]:
      FLAG: "FEEDBACK_GIVEN" — document feedback; exclude from ITR
    IF ais_feedback == null:
      FLAG: "NO_FEEDBACK" — require taxpayer to review and submit feedback
      
OUTPUT: reconciliation_report = {
  matched_entries: [],
  undeclared_income_flags: [],   // CRITICAL — must be declared
  excess_declared_flags: [],      // May indicate refund
  feedback_pending_flags: [],     // Action required before filing
  tds_mismatch_flags: []          // TDS in 26AS != Form 16
}
```

---

## SECTION 3 — ITD PREFILL JSON SCHEMA

### 3.1 Structure of ITD Prefill JSON (Downloaded from Portal)

The ITD prefill JSON is downloaded from the e-filing portal and contains pre-populated data based on 26AS, AIS, and PAN database. The JSON structure follows this pattern for ALL ITR forms:

```json
{
  "ITR": {
    "ITR1": {  // or ITR2, ITR3, ITR4 — only one present
      "CreationInfo": {
        "SWVersionNo": "String — e.g. SW101",
        "SWCreatedBy": "String — ERP name (ERI registered)",
        "XMLCreatedBy": "String — same",
        "XMLCreationDate": "String[DD/MM/YYYY]",
        "IntermediaryCity": "String[25] — max 25 chars",
        "Digest": "String — SHA-256 hash of JSON content",
        "CreatedBy": "TP|CA|ERI",
        "FolioNo": "String — ERI folio if applicable",
        "FormName": "ITR-1|ITR-2|ITR-3|ITR-4"
      },
      "Form_ITR1": {  // or Form_ITR2, etc.
        // Form-specific content — see Section 4
      }
    }
  }
}
```

> **CRITICAL:** `IntermediaryCity` max length = 25 characters. Exceeding this causes upload error [#/ITR/ITR4/creationinfo/intermediatorycity: expected maxlength: 25]. The ERP must enforce this.

---

### 3.2 ITD Prefill JSON — Common Fields Across All ITR Forms

```json
{
  "PersonalInfo": {
    "AssesseeName": {
      "FirstName": "String[25] — as per PAN",
      "MiddleName": "String[25]",
      "SurNameOrOrgName": "String[25]"
    },
    "PAN": "String[10] — AAAAA9999A format",
    "DOB": "String[DD/MM/YYYY]",
    "AadhaarCardNo": "String[12] — numeric only",
    "AadhaarEnrolmentID": "String[28] — if Aadhaar not issued",
    "PrimaryMobileNo": "String[10] — Indian mobile",
    "EmailID": "String",
    "Address": {
      "ResidenceName": "String — Flat/Door/Block No.",
      "ResidenceNo": "String",
      "RoadOrStreet": "String",
      "LocalityOrArea": "String",
      "CityOrTownOrDistrict": "String",
      "StateCode": "String[2] — ITD state code (01-37 + 99 for foreign)",
      "PinCode": "String[6]",
      "CountryCode": "String[3] — e.g. 91 for India; ISO for foreign"
    },
    "EmployerCategory": "G|PA|PU|B|S|O|NA",
    // G=CentralGovt, PA=StateGovt, PU=PSU, B=Pensioner, S=Others, O=Others, NA=NotApplicable
    "ResidentialStatus": "RES|NOR|NRI",
    "Status": "I",  // I=Individual for ITR 1-4
    "AssesseeType": "05|06|07|08",
    // 05=Individual Below60, 06=Individual60to80, 07=IndividualAbove80, 08=HUF
    "SeventhProvisotoSec139i": "Y|N",
    "SeventhProvisotoSec139i_ConditionA_500000": "Y|N",
    "SeventhProvisotoSec139i_ConditionB_1Crore": "Y|N",
    "SeventhProvisotoSec139i_ConditionC_2Lakh_Travel": "Y|N",
    "SeventhProvisotoSec139i_ConditionD_1Lakh_Electricity": "Y|N"
  },
  "FilingStatus": {
    "ReturnFileSec": "11|12|13|14|15|16|17|18",
    // 11=139(1) voluntary, 12=139(4) belated, 13=139(5) revised, 
    // 14=142(1) notice, 16=119(2)(b) condonation, 17=139(8A) updated
    "ReturnType": "O|R|D|U",
    // O=Original, R=Revised, D=Defective (response to 139(9)), U=Updated 139(8A)
    "IsRevised": "Y|N",
    "OriginalReturnAckNo": "String[15] — mandatory if IsRevised=Y",
    "OriginalReturnFiledDate": "String[DD/MM/YYYY] — mandatory if revised",
    "IsDefective": "Y|N",
    "DefectiveReturnNoticeNum": "String — mandatory if defective",
    "NoticeNum": "String — if filed in response to notice",
    "DIN": "String — Document Identification Number for notices",
    "NoticeDate": "String[DD/MM/YYYY]",
    "TaxRegime": "O|N",
    // O=Old, N=New (default for all from AY 2024-25)
    "OptOutNewTaxRegime": "Y|N",
    // Y=opted out of new regime (using old), N=using new regime
    "Form10IEAFiled": "Y|N",  // Business income cases only
    "Form10IEAAckNo": "String — if Form 10-IEA filed",
    "ModeOfFiling": "O|F",  // O=Online, F=Offline (JSON upload)
    "IsPortugueseCivilCode5A": "Y|N",
    "PortugueseCivilCodeDetails": {}  // If 5A = Y
  }
}
```

---

## SECTION 4 — ITR-1 OUTPUT JSON SCHEMA (COMPLETE)

### 4.1 ITR-1 Complete JSON Structure

```json
{
  "ITR": {
    "ITR1": {
      "CreationInfo": { /* see Section 3.1 */ },
      "Form_ITR1": {
        "PersonalInfo": { /* see Section 3.2 */ },
        "FilingStatus": { /* see Section 3.2 */ },

        "IncomeDeductions": {
          "GrossSalary": "Integer — B1(d)",
          "Salary17_1": "Integer",
          "PerquisitesValue17_2": "Integer",
          "ProfitsInLieuSalary17_3": "Integer",
          "AllwncExemptUs10": "Integer — B1(e)",
          "NetSalary": "Integer — B1(f) = GrossSalary - AllwncExemptUs10",
          "DeductionUs16": "Integer — B1(iva) total deductions u/s 16",
          "DeductionUs16ia": "Integer — standard deduction (50000/75000)",
          "DeductionUs16ii": "Integer — entertainment allowance govt only",
          "DeductionUs16iii": "Integer — professional tax",
          "IncomeFromSal": "Integer — B1(j) = NetSalary - DeductionUs16",

          "TypeOfHP": "S|L|D",  // S=SelfOccupied, L=LetOut, D=DeemedLetOut
          "GrossRentReceived": "Integer",
          "TaxPaidLocalAuthority": "Integer — municipal taxes (let-out only)",
          "AnnualValue": "Integer",  // GAV - Municipal taxes
          "StandardDeduction30Pct": "Integer — 30% of AnnualValue (let-out only)",
          "InterestPayable": "Integer — interest on housing loan u/s 24(b)",
          "ArrearUnrealizedRentReceived": "Integer",
          "IncomeFromHP": "Integer — AnnualValue - 30% - interest (can be negative)",

          "IncomeOthSrc": "Integer — B3(g) total other sources",
          "OthersInc": {
            "IncNatureDesc1": "INTSB|INTFD|INTOTH|INTITR|NATDIV|FAMPENS|WINLOT|CONTCONS|OTHEROS",
            "OthSrcNatureInc1": "String — description",
            "OthSrcInc1": "Integer"
            // Repeat for each income type
          },
          "FamilyPension": "Integer — gross family pension before deduction",
          "FamilyPensionDedUs57iia": "Integer — min(1/3 of pension, 15000 old / 25000 new)",

          "GrossTotIncome": "Integer — B4 = IncomeFromSal + IncomeFromHP + IncomeOthSrc",
          
          // LTCG u/s 112A (added to ITR-1 from AY 2025-26 — up to Rs 1.25 lakh)
          "LTCGUs112A": "Integer — max 125000 only; if > 125000, must use ITR-2",
          "ExemptInc": "Integer",

          "DeductionUs80C": "Integer — max 150000",
          "DeductionUs80CCC": "Integer — within 80C combined limit",
          "DeductionUs80CCD1": "Integer — within 80C combined limit",
          "DeductionUs80CCD1B": "Integer — max 50000 extra",
          "DeductionUs80CCD2": "Integer — employer NPS; 10%/14% of salary",
          "DeductionUs80CCH": "Integer — Agniveer corpus",
          "DeductionUs80D": "Integer",
          "DeductionUs80DD": "Integer",
          "DeductionUs80DDB": "Integer",
          "DeductionUs80E": "Integer",
          "DeductionUs80EE": "Integer",
          "DeductionUs80EEA": "Integer",
          "DeductionUs80EEB": "Integer",
          "DeductionUs80G": "Integer",
          "DeductionUs80GG": "Integer",
          "DeductionUs80GGA": "Integer",
          "DeductionUs80GGC": "Integer",
          "DeductionUs80TTA": "Integer — max 10000",
          "DeductionUs80TTB": "Integer — max 50000 (senior only)",
          "DeductionUs80U": "Integer",
          "TotalChapVIADeductions": "Integer — sum of above; max = GTI",
          
          "TotalIncome": "Integer — GTI minus deductions; round to nearest 10"
        },

        "TaxComputation": {
          "TaxPayableOnTI": "Integer — slab computation",
          "RebateUs87A": "Integer",
          "TaxAfterRebate": "Integer — TaxPayableOnTI - RebateUs87A",
          "Surcharge25": "Integer — 25% rate",
          "Surcharge37": "Integer — 37% rate (old regime only > 5Cr)",
          "SurchargeOnSpecialIncome": "Integer — capped at 15% for 111A/112A",
          "TotalSurcharge": "Integer",
          "HealthEduCess": "Integer — 4% of (TaxAfterRebate + TotalSurcharge)",
          "TaxPayableOnRebate": "Integer — DEPRECATED field; legacy compatibility",
          "TotalTaxAndCess": "Integer — TaxAfterRebate + TotalSurcharge + HealthEduCess",
          "ReliefUs89": "Integer — Form 10E relief",
          "NetTaxLiability": "Integer — TotalTaxAndCess - ReliefUs89",

          "IntrstUs234A": "Integer — 234A interest",
          "IntrstUs234B": "Integer — 234B interest",
          "IntrstUs234C": "Integer — 234C interest",
          "LateFilingFeeUs234F": "Integer — 234F fee",
          "TotalIntrstPnltyFee": "Integer — sum of 234A+B+C+F",
          "GrossTaxLiability": "Integer — NetTaxLiability + TotalIntrstPnltyFee"
        },

        "TaxPaid": {
          "TDS1": {
            "TDSSal": [
              {
                "EmployerOrDeductorOrCollectTAN": "String[10]",
                "EmployerOrDeductorOrCollectName": "String",
                "TotalTDSSal": "Integer",
                "ClaimOutOfTotTDSSal": "Integer — TDS credit claimed this year",
                "RuleOf26As": "Y|N"  // Whether TDS appears in 26AS
              }
            ],
            "TotalTDS1TaxDeducted": "Integer",
            "TotalTDS1TaxClaimed": "Integer"
          },
          "TDS2": {
            "TDSOthThanSals": [
              {
                "DeductorTAN": "String[10]",
                "DeductorName": "String",
                "AmtOnWhichTDSDeducted": "Integer — gross income before TDS",
                "TaxDeducted": "Integer",
                "YrOfTaxDeduction": "String — FY in which deducted",
                "ClaimOutOfTotTDSOthThanSals": "Integer — credit claimed this year"
              }
            ],
            "TotalTDSOthThanSalTaxDeducted": "Integer",
            "TotalTDSOthThanSalTaxClaimed": "Integer"
          },
          "TDS3": {
            "TDSonSal194P": []  // Section 194P — bank TDS for 75+ senior citizens
          },
          "TCS": {
            "TCS": [
              {
                "CollectorTAN": "String[10]",
                "CollectorName": "String",
                "AmtOnWhichTCSCollected": "Integer",
                "TaxCollected": "Integer",
                "ClaimOutOfTotTCS": "Integer"
              }
            ],
            "TotalTCSClaimedThisYear": "Integer"
          },
          "AdvanceTax": {
            "AdvTaxDetails": [
              {
                "BSRCode": "String[7]",
                "DateDep": "String[DD/MM/YYYY]",
                "SrlNoOfChaln": "String",
                "Amt": "Integer",
                "Type": "200|300|400"
              }
            ],
            "TotalAdvTaxPaid": "Integer"
          },
          "TotalTaxPaid": "Integer — TDS1 + TDS2 + TDS3 + TCS + AdvanceTax",
          "BalTaxPayable": "Integer — NetTaxLiability - TotalTaxPaid (positive = demand, negative = refund)"
        },

        "Refund": {
          "BankAccountNo": "String — pre-validated account only",
          "BankIFSCCode": "String[11]",
          "BankName": "String",
          "AccountType": "10|11|13|14",
          // 10=Savings, 11=Current, 13=Cash Credit, 14=Other
          "IsIfscValid": "Y|N"
        },

        "Schedule80G": {
          "DonationUs80GCash": [
            {
              "NameOfDonee": "String",
              "AddressOfDonee": "String",
              "PINCode": "String[6]",
              "DoneePAN": "String[10] — must not equal taxpayer PAN",
              "DonationAmt": "Integer",
              "EligibleAmt": "Integer",
              "DeductionUs80G": "Integer — 50% or 100% of eligible amount"
            }
          ],
          "DonationUs80GChequeOrDD": "Same structure as above",
          "TotDonationsUs80G": "Integer"
        },

        "ExemptIncome": {
          "ExcAgriInc": "Integer — max 5000 for ITR-1",
          "ShareFromHUF": "Integer",
          "ShareFromFirm": "Integer — u/s 10(2A)",
          "InterestFromGovtSecurities": "Integer",
          "GratuityExempt": "Integer — u/s 10(10)",
          "LeaveEncashmentExempt": "Integer — u/s 10(10AA)",
          "VRSExempt": "Integer — u/s 10(10C)",
          "CommutedPensionExempt": "Integer — u/s 10(10A)",
          "PPFInterest": "Integer — u/s 10(11)",
          "SukanyaInterest": "Integer — u/s 10(11A)",
          "TaxFreeBondInterest": "Integer — u/s 10(15)",
          "LICMaturityExempt": "Integer — u/s 10(10D)",
          "OtherExemptIncome": "Integer",
          "AllExemptIncome": "Integer — total"
        },

        "Verification": {
          "Capacity": "Self|Rep|Karta|LegalHeir|Partner",
          "FatherName": "String",
          "Place": "String",
          "Date": "String[DD/MM/YYYY]",
          "Declaration": "I solemnly declare that the information given in this Return is correct, complete and truly stated and that the amount of total income and other particulars shown therein are truly stated to the best of my knowledge and belief."
        }
      }
    }
  }
}
```

---

## SECTION 5 — ITR-2 OUTPUT JSON SCHEMA (KEY ADDITIONS)

ITR-2 includes all ITR-1 fields PLUS the following schedules:

### 5.1 Schedule CG — Capital Gains JSON

```json
{
  "ScheduleCG": {
    "ShortTermCapGain": {
      "SaleOnOrAfter23July2024_111A": {
        "NilRateTax_111A": {
          "MFD": [],  // Each security-wise
          "ListedSharesNilRate": "Integer"
        },
        "TaxableRateIncomeSec111A_20Pct": {
          "FullValueOfCons": "Integer",
          "DeductSec48i": "Integer",  // transfer expenses
          "DeductSec48iia_WI": "Integer",  // cost without indexation
          "DeductSec48iib_WOI": "Integer", // WOI
          "STCG111ABalance": "Integer",
          "DeductExemptSec54B": "Integer",
          "STCGOnRowE": "Integer"  // net STCG at 20%
        }
      },
      "SaleBeforeJuly2024_111A": {
        "TaxableRate_15Pct": {
          // Same fields as above but at 15% rate
        }
      },
      "STCGOnOtherAssets_Slab": {
        // STCG at normal slab rates
        "FullValueOfCons_OtherAssets": "Integer",
        "CostOfAcquisition": "Integer",
        "CostOfImprovement": "Integer",
        "DeductExpenseTransfer": "Integer",
        "NetSTCGSlab": "Integer",
        "ExemptionUs54B": "Integer",
        "NetTaxableSTCGSlab": "Integer"
      },
      "TotalSTCG": "Integer"
    },

    "LongTermCapGain": {
      "LTCGEquity_PostJuly2024_112A": {
        "Schedule112A": [
          {
            "ISINCode": "String[12]",
            "NameOfScrip": "String",
            "SharesOrUnits": "Integer",
            "SalePricePerUnit": "Integer",
            "TotalSaleValue": "Integer",  // Col6 = Col4 * Col5
            "CostWithoutIndexation": "Integer",  // Col7 = max(Col8, Col9)
            "ActualCostOfAcquisition": "Integer",  // Col8
            "FMVOn31Jan2018": "Integer",  // Col9 (if acquired before 01/02/2018)
            "TotalFMV_55_2_ac": "Integer",  // Col11 = Col4 * Col10; only if pre Feb 2018
            "FMVPerShareOn31Jan2018": "Integer",  // Col10 — market price on Jan 31, 2018
            "ExpenditureOnTransfer": "Integer",  // Col12
            "TotalDeductions": "Integer",  // Col13 = Col7 + Col12
            "BalanceLTCG": "Integer",  // Col14 = Col6 - Col13
            "TransferDate": "String[DD/MM/YYYY]",
            "ExemptionUs54EC": "Integer",
            "ExemptionUs54F": "Integer",
            "NetTaxableLTCG": "Integer"
          }
        ],
        "TotalLTCG112A": "Integer",
        "ExemptionThreshold125000": "Integer",  // always 125000
        "LTCGAboveThreshold": "Integer",  // max(0, TotalLTCG112A - 125000)
        "TaxRate112A": "0.125"  // 12.5%
      },
      "LTCGEquity_PreJuly2024_112A": {
        // Same structure but at 10% rate and 100000 threshold
        "ExemptionThreshold100000": "Integer",
        "TaxRate112A_Pre": "0.10"
      },
      "LTCG_Property_WithIndexation_112": {
        "SaleConsideration": "Integer",
        "StampDutyValue_50C": "Integer",  // if SDV > 110% of sale price, use SDV
        "FullValueOfConsideration": "Integer",  // max(actual, SDV) subject to 110% rule
        "DeductTransferExpenses": "Integer",
        "IndexedCostOfAcquisition": "Integer",
        "IndexedCostOfImprovement": "Integer",
        "NetLTCGWithIndexation": "Integer",
        "ExemptionUs54": "Integer",
        "ExemptionUs54EC": "Integer",
        "ExemptionUs54F": "Integer",
        "TaxableLTCG_20Pct": "Integer"
      },
      "LTCG_Property_WithoutIndexation_112": {
        "SaleConsideration": "Integer",
        "FullValueOfConsideration": "Integer",
        "DeductTransferExpenses": "Integer",
        "ActualCostOfAcquisition": "Integer",
        "CostOfImprovement": "Integer",
        "NetLTCGWithoutIndexation": "Integer",
        "ExemptionUs54": "Integer",
        "ExemptionUs54EC": "Integer",
        "TaxableLTCG_12_5Pct": "Integer"
      },
      "TotalLTCG": "Integer"
    },

    "IncomFromCG": "Integer",  // Total CG = STCG + LTCG
    
    "CGSetOff": {
      // Loss set-off fields per CYLA
      "STCGLossSetOff": "Integer",
      "LTCGLossSetOff": "Integer",
      "RemainingSTCG": "Integer",
      "RemainingLTCG": "Integer"
    }
  }
}
```

### 5.2 Schedule VDA (Virtual Digital Assets)

```json
{
  "ScheduleVDA": {
    "VDADetails": [
      {
        "DateOfAcquisition": "String[DD/MM/YYYY]",
        "DateOfTransfer": "String[DD/MM/YYYY]",
        "HeadUnderWhichIncomeTaxable": "CG|OS",
        "CostOfAcquisition": "Integer",
        "ConsiderationReceived": "Integer",
        "IncomeFromVDA": "Integer"  // ConsiderationReceived - CostOfAcquisition
      }
    ],
    "TotalIncomeFromVDA": "Integer",
    "TDSUs194S": "Integer"
    // VDA loss cannot be set off; if negative, declare 0
  }
}
```

### 5.3 Schedule CYLA (Current Year Loss Adjustment)

```json
{
  "ScheduleCYLA": {
    "HP_Loss_SetOff": {
      "HPLossAvailable": "Integer",  // Total HP loss
      "SetOffAgainstSalary": "Integer",  // max 200000
      "SetOffAgainstCG": "Integer",
      "SetOffAgainstOS": "Integer",
      "TotalHPLossSetOff": "Integer",
      "UnabsorbedHPLoss": "Integer"  // carried forward via Schedule CFL
    },
    "BusinessLoss_SetOff": {
      "NonSpecBusLossAvail": "Integer",
      "SetOffAgainstHP": "Integer",
      "SetOffAgainstCG": "Integer",
      "SetOffAgainstOS": "Integer",
      "TotalNonSpecBusLossSetOff": "Integer"
    },
    "STCGLoss_SetOff": {
      "STCGLossAvail": "Integer",
      "SetOffAgainstSTCG": "Integer",
      "SetOffAgainstLTCG": "Integer"
    },
    "LTCGLoss_SetOff": {
      "LTCGLossAvail": "Integer",
      "SetOffAgainstLTCG": "Integer"
    }
  }
}
```

### 5.4 Schedule AL (Assets and Liabilities)

```json
{
  "ScheduleAL": {
    "ImmovablePropertyDetails": [
      {
        "AddressOfProperty": "String",
        "CostOfAcquisition": "Integer",
        "YearOfAcquisition": "String"
      }
    ],
    "MovableAssets": {
      "Jewellery": "Integer",
      "ArtworkPaintings": "Integer",
      "VehicleOtherThanCommercial": "Integer",
      "BullionGold": "Integer",
      "OtherMovable": "Integer",
      "TotalMovable": "Integer"
    },
    "FinancialAssets": {
      "SharesDebentures": "Integer",
      "InsurancePolicies": "Integer",
      "LoansAndAdvancesGiven": "Integer",
      "CashInHandAbove500K": "Integer",
      "BankDeposits": "Integer",
      "OtherFinancial": "Integer",
      "TotalFinancial": "Integer"
    },
    "TotalAssets": "Integer",
    "Liabilities": {
      "SecuredLoans": "Integer",
      "UnsecuredLoans": "Integer",
      "OtherLiabilities": "Integer",
      "TotalLiabilities": "Integer"
    }
  }
}
```

---

## SECTION 6 — ITR-3 ADDITIONAL JSON SCHEMA

### 6.1 Schedule BP (Business/Profession)

```json
{
  "ScheduleBP": {
    "NetProfitFromPL": "Integer",
    "AddAdmissibleDebits": {
      "DebitPersonalExpenses": "Integer",
      "DebitCapitalExpenditure": "Integer",
      "DisallowanceUs40A3_CashPayments": "Integer",
      "DisallowanceUs36_1_va_EPFLate": "Integer",
      "DisallowanceUs43B": "Integer",
      "DisallowanceUs14A": "Integer",
      "ExcessDepreciation": "Integer",
      "TotalAddBack": "Integer"
    },
    "LessAdmissibleCredits": {
      "DividendIncome": "Integer",
      "RentalIncome": "Integer",
      "ITActDepreciation": "Integer",  // Schedule DPM depreciation
      "AdditionalDepreciation32_1_iia": "Integer",
      "OtherDeductionsNotInPL": "Integer",
      "TotalDeductions": "Integer"
    },
    "ProfitFromBusiness": "Integer",  // NetProfitFromPL + AddBack - Deductions
    "SpeculativeBusinessIncome": "Integer",
    "SpecifiedBusinessIncome35AD": "Integer",
    "TotalBusinessIncome": "Integer"
  }
}
```

### 6.2 Schedule DPM (Depreciation per IT Act)

```json
{
  "ScheduleDPM": {
    "DepreciationOnBuildings": {
      "OpeningWDV": "Integer",
      "AdditionsResiBuilding": "Integer",
      "AdditionsNonResiBuilding": "Integer",
      "Disposals": "Integer",
      "ClosingWDVBeforeDepreciation": "Integer",
      "Depreciation5Pct": "Integer",
      "Depreciation10Pct": "Integer",
      "TotalBuildingDepreciation": "Integer"
    },
    "DepreciationOnPlantMachinery": {
      "OpeningWDV": "Integer",
      "AdditionsGeneral": "Integer",
      "AdditionalDepreciationNewPM": "Integer",
      "Disposals": "Integer",
      "Depreciation15Pct": "Integer",
      "Depreciation40Pct_Computers": "Integer",
      "STCGOnBlock": "Integer"  // if disposal > (opening + additions)
    },
    "DepreciationOnFurniture": {
      "Depreciation10Pct": "Integer"
    },
    "DepreciationOnIntangibles": {
      "Depreciation25Pct": "Integer"
    },
    "TotalDepreciation": "Integer"
  }
}
```

### 6.3 Schedule GST Reconciliation (ITR-3/4)

```json
{
  "ScheduleGST": {
    "GSTINList": [
      {
        "GSTIN": "String[15]",
        "NameOfBusiness": "String",
        "TurnoverAsPerGSTR1": "Integer",
        "TurnoverAsPerGSTR3B": "Integer",
        "TurnoverAsPerBooks": "Integer",
        "DifferenceGSTvsBooks": "Integer",
        "ReasonForDifference": "String"
      }
    ]
  }
}
```

---

## SECTION 7 — ITR-4 OUTPUT JSON SCHEMA (PRESUMPTIVE)

### 7.1 ITR-4 Presumptive Income Fields

```json
{
  "Form_ITR4": {
    // PersonalInfo, FilingStatus — same as ITR-1
    
    "BusinessProfessionIncome": {
      "NatureOfBusiness": {
        "Section44ADCode": "String — business code from ITD table",
        "Section44ADACode": "String — profession code",
        "Section44AECode": "String — transport code"
      },
      
      "Sec44AD": {
        "GrossTurnoverReceipts": "Integer",
        "TurnoverCash": "Integer",
        "TurnoverDigital": "Integer",
        "GrossIncomeCash_8Pct": "Integer",    // 8% of TurnoverCash
        "GrossIncomeDigital_6Pct": "Integer", // 6% of TurnoverDigital
        "TotalPresumptiveIncome44AD": "Integer",
        "BusinessCode": "String"
      },
      
      "Sec44ADA": {
        "GrossProfessionalReceipts": "Integer",
        "ReceiptsCash": "Integer",
        "ReceiptsDigital": "Integer",
        "PresumptiveIncome50Pct": "Integer",  // 50% of total receipts
        "ProfessionCode": "String"
      },
      
      "Sec44AE": {
        "VehicleDetails": [
          {
            "RegistrationNumber": "String",
            "VehicleOwnershipMonths": "Integer",
            "IsHeavyVehicle": "Y|N",
            "GVW_Tonnes": "Float",  // if heavy
            "PresumptiveIncomePerVehicle": "Integer",
            // Heavy: GVW * 1000 * months; Non-heavy: 7500 * months
          }
        ],
        "TotalPresumptiveIncome44AE": "Integer"
      },
      
      "TotalPresumptiveBusinessIncome": "Integer"
    },
    
    "SimplifiedBalanceSheet": {
      "GrossReceipts": "Integer",
      "NetProfit": "Integer",  // presumptive income
      "TotalSundryDebtors": "Integer",
      "TotalSundryCreditors": "Integer",
      "TotalStockInTrade": "Integer",
      "CashBalance": "Integer",
      "OpeningCapital": "Integer",
      "Drawings": "Integer",
      "AdditionsToCapital": "Integer",
      "ClosingCapital": "Integer",
      "TotalSecuredLoans": "Integer",
      "TotalUnsecuredLoans": "Integer",
      "TotalFixedAssets_ClosingWDV": "Integer"
    }
  }
}
```

---

## SECTION 8 — CBDT VALIDATION RULES — ITR-1 (COMPLETE)

*Source: CBDT e-Filing ITR 1 Validation Rules AY 2025-26 V1.1, 10 July 2025*

### 8.1 Category A Rules — Upload Rejection (ITR-1)

#### Group 1: Chapter VI-A Deductions
| Rule ID | Validation | Implementation |
|---|---|---|
| VR1-001 [CAT-A] | 80C + 80CCC + 80CCD(1) ≤ ₹1,50,000 (old regime) | `if(regime=='O') assert(80C+80CCC+80CCD1 <= 150000)` |
| VR1-002 [CAT-A] | 80CCD(1) ≤ 20% of GTI if employer is pensioner category | `if(empCat IN ['CG-Pen','SG-Pen','PSU-Pen','OtherPen','NA']) assert(80CCD1 <= 0.20*GTI)` |
| VR1-003 [CAT-A] | 80CCD(1) ≤ 10% of salary for all other employer categories | `if(empCat NOT IN pensioner_list) assert(80CCD1 <= 0.10*salary)` |
| VR1-004 [CAT-A] | 80CCD(2) ≤ 10% of salary (non-Govt employer) | `if(empCat NOT IN ['CG','SG']) assert(80CCD2 <= 0.10*salary)` |
| VR1-005 [CAT-A] | 80DDB ≤ ₹1,00,000 (senior citizen) | `if(isSenior) assert(80DDB <= 100000)` |
| VR1-006 [CAT-A] | 80DDB: eligible category description mandatory | `if(80DDB > 0) assert(80DDB_category != null)` |
| VR1-007 [CAT-A] | 80DDB ≤ ₹40,000 for "Self or Dependent" if non-senior | `if(!isSenior && 80DDB_cat=='Self') assert(80DDB <= 40000)` |
| VR1-008 [CAT-A] | 80G claimed → Schedule 80G must be filled | `if(80G > 0) assert(Schedule80G.length > 0)` |
| VR1-009 [CAT-A] | Deduction in Schedule VIA u/s 80G must equal eligible amount in Schedule 80G | `assert(VIA_80G == Schedule80G.TotalEligibleDeduction)` |
| VR1-010 [CAT-A] | 80TTA ≤ ₹10,000 | `assert(80TTA <= 10000)` |
| VR1-011 [CAT-A] | 80TTA restricted to savings account interest only (not FD) | ERP must source 80TTA only from savings account field |
| VR1-012 [CAT-A] | Senior citizen (DOB on or before 02/04/1964 for AY 25-26) CANNOT claim 80TTA | `if(dob <= 1964-04-02) assert(80TTA == 0)` |
| VR1-013 [CAT-A] | 80TTB ≤ ₹50,000 | `assert(80TTB <= 50000)` |
| VR1-014 [CAT-A] | Non-senior (below 60) CANNOT claim 80TTB | `if(!isSenior) assert(80TTB == 0)` |
| VR1-015 [CAT-A] | Total VI-A deductions ≤ GTI | `assert(TotalDeductions <= GTI)` |
| VR1-016 [CAT-A] | Total VI-A deductions = sum of individual deductions | `assert(TotalDeductions == sum(all_deductions))` |

#### Group 2: Personal Information
| Rule ID | Validation | Implementation |
|---|---|---|
| VR1-017 [CAT-A] | Taxpayer name must match PAN database | ITD API call to verify name |
| VR1-018 [CAT-A] | If tax liability computed and paid, GTI and all heads must be > 0 | `if(taxPaid > 0) assert(GTI > 0)` |
| VR1-019 [CAT-A] | If taxes paid disclosed, income details must also be disclosed | `if(TaxPaid > 0) assert(IncomeDetails != null)` |
| VR1-020 [CAT-A] | Old regime: GTI = Salary + HP + OS + LTCG 112A | `if(regime=='O') assert(GTI == Sal + HP + OS + LTCG112A)` |
| VR1-021 [CAT-A] | Old regime: 87A rebate not if TI (including LTCG 112A) > ₹5,00,000 | `if(regime=='O' && TI > 500000) assert(rebate87A == 0)` |
| VR1-022 [CAT-A] | TI = max(0, GTI - Deductions) | `assert(TI == max(0, GTI - Deductions))` |
| VR1-023 [CAT-A] | Tax after rebate = tax on TI - rebate 87A | `assert(TaxAfterRebate == TaxOnTI - Rebate87A)` |
| VR1-024 [CAT-A] | Total Tax and Cess = Tax after rebate + H&E Cess | `assert(TotalTaxCess == TaxAfterRebate + Cess)` |
| VR1-025 [CAT-A] | Total Tax Fees Interest = TotalTaxCess + 234A + 234B + 234C + 234F - Relief89 | `assert(TotalTaxFeesInterest == TotalTaxCess + 234A + 234B + 234C + 234F - Relief89)` |
| VR1-026 [CAT-A] | Total Interest/Fee = 234A + 234B + 234C + 234F | `assert(TotalIntFee == sum(234A,234B,234C,234F))` |

#### Group 3: Agricultural Income
| Rule ID | Validation |
|---|---|
| VR1-027 [CAT-A] | Agricultural income shown as exempt ≤ ₹5,000 (ITR-1 limit) |
| VR1-028 [CAT-A] | Exempt income = sum of individual exempt income components |
| VR1-029-036 [CAT-A] | Each exempt income dropdown item cannot be selected more than once (10(10BC), 10(10D), 10(11), 10(12), 10(13), 10(16), 10(17), 10(18), 10(19), 10(26), 10(26AAA)) |

#### Group 4: House Property
| Rule ID | Validation |
|---|---|
| VR1-037 [CAT-A] | Standard deduction = 30% of Annual Value |
| VR1-038 [CAT-A] | Municipal tax claimed only if gross rent > 0 |
| VR1-039 [CAT-A] | If Let-Out/DLO: Gross rent must be > 0 |
| VR1-040 [CAT-A] | Annual Value = Gross Rent - Municipal Tax |
| VR1-041 [CAT-A] | Income from HP = Annual Value - 30% deduction - Interest ± Arrear |
| VR1-042 [CAT-A] | Old regime, SOP: Interest on borrowed capital ≤ ₹2,00,000 |
| VR1-043 [CAT-A] | New regime, SOP: Interest on borrowed capital = 0 (NO deduction in new regime for SOP) |
| VR1-044 [CAT-A] | Municipal tax NOT allowed on Self-Occupied property |

#### Group 5: Salary Income
| Rule ID | Validation |
|---|---|
| VR1-045 [CAT-A] | Gross Salary = 17(1) + 17(2) + 17(3) + 89A income |
| VR1-046 [CAT-A] | Net Salary = Gross Salary - Exempt Allowances - Relief 89A |
| VR1-047 [CAT-A] | Deductions u/s 16 = 16(ia) + 16(ii) + 16(iii) |
| VR1-048 [CAT-A] | Income from Salaries = Net Salary - Deductions u/s 16 |
| VR1-049 [CAT-A] | Total exempt allowances ≤ Gross Salary |
| VR1-050 [CAT-A] | LTA (10(5)) ≤ LTA received in salary 17(1) |
| VR1-051 [CAT-A] | Gratuity exempt ≤ ₹20,00,000 for PSU/Others/Pensioners |
| VR1-052 [CAT-A] | Gratuity exempt ≤ ₹25,00,000 for Central/State Govt employees |
| VR1-053 [CAT-A] | 10(10A) commuted pension ≤ Salary 17(1) |
| VR1-054 [CAT-A] | 10(10AA) leave encashment ≤ Salary 17(1); max ₹25L for non-Govt |
| VR1-055 [CAT-A] | 10(10B)(i) retrenchment ≤ ₹5,00,000 |
| VR1-056 [CAT-A] | 10(10C) VRS ≤ ₹5,00,000 |
| VR1-057 [CAT-A] | Only ONE of 10(10B)(i), 10(10B)(ii), 10(10C) claimable |
| VR1-058 [CAT-A] | 10(10CC) tax on non-monetary perquisite ≤ Perquisites 17(2) |
| VR1-059 [CAT-A] | Old regime: 10(13A) HRA ≤ Salary 17(1) |
| VR1-060 [CAT-A] | 10(14)(ii) transport handicapped ≤ ₹38,400 per year |
| VR1-061 [CAT-A] | New regime: LTA (10(5)) = 0 (NOT available) |
| VR1-062 [CAT-A] | New regime: HRA (10(13A)) = 0 (NOT available) |
| VR1-063 [CAT-A] | New regime: 10(14)(i) and 10(14)(ii) prescribed allowances = 0 |
| VR1-064 [CAT-A] | New regime: Entertainment allowance 16(ii) = 0 |
| VR1-065 [CAT-A] | New regime: Professional tax 16(iii) = 0 (Note: 16(iii) NOT available in new regime) |
| VR1-066 [CAT-A] | Old regime: Entertainment allowance 16(ii) only for Govt/PSU employees |
| VR1-067 [CAT-A] | Old regime: Entertainment allowance ≤ min(actual EA, 1/5 of salary, ₹5,000) |
| VR1-068 [CAT-A] | Standard deduction ≤ min(₹50,000 (old)/₹75,000 (new), Net Salary) |
| VR1-069 [CAT-A] | New regime: Standard deduction = ₹75,000 (salary) or ₹25,000/1/3 (family pension) |
| VR1-070 [CAT-A] | 10(13A) HRA calculation per formula (not to exceed 50%/40% of salary, actual HRA, rent-10% salary) |
| VR1-071 [CAT-A] | Family pension deduction u/s 57(iia) only if family pension > 0 AND taxpayer has NOT opted for new regime |
| VR1-072 [CAT-A] | Old regime: 57(iia) ≤ min(1/3 of family pension, ₹15,000) |
| VR1-073 [CAT-A] | If HRA exempt claimed: 80GG cannot exceed ₹55,000 (changed from ₹60,000) |
| VR1-074 [CAT-A] | Same exempt allowance cannot be selected more than once |
| VR1-075 [CAT-A] | Gratuity cannot be claimed against more than one employer |
| VR1-076 [CAT-A] | Commuted pension cannot be claimed against more than one employer |

#### Group 6: Other Sources
| Rule ID | Validation |
|---|---|
| VR1-077 [CAT-A] | Income from Other Sources = sum of individual components |
| VR1-078 [CAT-A] | "Interest from savings account" dropdown cannot be selected > once |
| VR1-079 [CAT-A] | "Interest from deposits" dropdown cannot be selected > once |
| VR1-080 [CAT-A] | "Interest from IT Refund" dropdown cannot be selected > once |
| VR1-081 [CAT-A] | "Family pension" dropdown cannot be selected > once |

#### Group 7: Tax Paid Schedules
| Rule ID | Validation |
|---|---|
| VR1-082 [CAT-A] | TCS credit claimed ≤ TCS collected |
| VR1-083 [CAT-A] | Total TCS = sum of individual TCS credits |
| VR1-084 [CAT-A] | TDS2 credit claimed ≤ Tax deducted |
| VR1-085 [CAT-A] | TDS year cannot be 0 or null if TDS is claimed |
| VR1-086 [CAT-A] | Total TDS1 = sum of individual values |
| VR1-087 [CAT-A] | Total TDS2 credit = sum of individual credits |
| VR1-088 [CAT-A] | Total TDS3 credit = sum of individual credits |
| VR1-089 [CAT-A] | TDS/TCS/Tax paid challan dates must fall in AY-relevant FY or valid payment period |
| VR1-090 [CAT-A] | No advance tax payment with deposit date after 31/03 of the FY can be counted as advance tax |

#### Group 8: Refund and Demand
| Rule ID | Validation |
|---|---|
| VR1-091 [CAT-A] | If refund > 0, valid pre-validated bank account must be present |
| VR1-092 [CAT-A] | LTCG u/s 112A in ITR-1: if LTCG > ₹1,25,000, must file ITR-2 (not ITR-1) |
| VR1-093 [CAT-A] | Total income (excluding LTCG 112A) must not exceed ₹50 lakhs |
| VR1-094 [CAT-A] | 80EE and 80EEA cannot BOTH be claimed (only one) |
| VR1-095 [CAT-A] | Relief u/s 89 cannot be claimed if salary 17(1), 17(2), 17(3) and family pension all = 0 |

#### Group 9: Special Validations — AY 2025-26
| Rule ID | Validation |
|---|---|
| VR1-096 [CAT-A] | Old regime cannot be selected after due date (139(1)) — option expires |
| VR1-097 [CAT-A] | Return cannot be filed u/s 139 once notice u/s 148/153A/153C issued |
| VR1-098 [CAT-A] | TAN of employer: first three alphabets must match valid TAN codes |
| VR1-099 [CAT-A] | Return role (CD of PAN) must match status selected (Individual PAN = status I) |

### 8.2 Category B Rules — Defective Return Notice (ITR-1)

| Rule ID | Validation |
|---|---|
| VR1-B001 [CAT-B] | 80G/80GGA deduction claimed without donation details |
| VR1-B002 [CAT-B] | TDS on property (194IA) claimed but property details not furnished |
| VR1-B003 [CAT-B] | Exempt income declared but no details |
| VR1-B004 [CAT-B] | Balance of tax payable is significantly > 0 without payment (may indicate missing tax) |

### 8.3 Category D Rules — Warning (ITR-1)

| Rule ID | Validation |
|---|---|
| VR1-D001 [CAT-D] | 80CCD(1B) claimed without NPS PRAN details |
| VR1-D002 [CAT-D] | 80DDB claimed without specified disease prescription document reference |
| VR1-D003 [CAT-D] | 80U claimed without disability certificate reference |
| VR1-D004 [CAT-D] | 80DD claimed without Form 10-IA (disability certificate) reference |

---

## SECTION 9 — CBDT VALIDATION RULES — ITR-2 (COMPLETE)

*Source: CBDT e-Filing ITR 2 Validation Rules AY 2025-26 V1.0, 10 July 2025*

### 9.1 Category A Rules (ITR-2 — Additional over ITR-1)

#### Schedule HP
| Rule ID | Validation |
|---|---|
| VR2-HP-001 [CAT-A] | Standard deduction = 30% of Annual Value exactly |
| VR2-HP-002 [CAT-A] | Co-owned property: assessee share + co-owner shares = 100% |
| VR2-HP-003 [CAT-A] | Co-owned property: assessee's annual value = own% × total annual value |
| VR2-HP-004 [CAT-A] | Cannot claim interest on borrowed capital if assessee's share = 0 |
| VR2-HP-005 [CAT-A] | Municipal tax not allowed where GAV = 0 or null |
| VR2-HP-006 [CAT-A] | Old regime, SOP: Interest on borrowed capital ≤ ₹2,00,000 |
| VR2-HP-007 [CAT-A] | New regime, SOP: Interest on borrowed capital = 0 |
| VR2-HP-008 [CAT-A] | Let-out/DLO: Gross rent > 0 |
| VR2-HP-009 [CAT-A] | HP income = NAV - 30% std deduction - interest + arrear |
| VR2-HP-010 [CAT-A] | HP pass-through income = PTI schedule HP amount |
| VR2-HP-011 [CAT-A] | More than 2 houses claimed as SOP → third house = DLO (cannot have SOP for > 2) |
| VR2-HP-012 [CAT-A] | Co-owner PAN ≠ Assessee PAN |

#### Schedule 112A (LTCG Listed Equity)
| Rule ID | Validation |
|---|---|
| VR2-CG-112A-001 [CAT-A] | Total Sale Value (Col 6) = Units (Col 4) × Sale Price (Col 5) |
| VR2-CG-112A-002 [CAT-A] | Cost without indexation (Col 7) = max(Col 8 actual cost, Col 9 FMV Jan31 2018) |
| VR2-CG-112A-003 [CAT-A] | Col 9 (FMV Jan 31, 2018) applicable only if asset acquired before 01/02/2018; must be ≤ min(Col 6 sale value, Col 11 total FMV) |
| VR2-CG-112A-004 [CAT-A] | Col 11 Total FMV = Col 4 (units) × Col 10 (FMV per unit on Jan 31, 2018) |
| VR2-CG-112A-005 [CAT-A] | Col 13 Total deductions = Col 7 + Col 12 (transfer expenses) |
| VR2-CG-112A-006 [CAT-A] | Col 14 Balance = Col 6 - Col 13 |
| VR2-CG-112A-007 [CAT-A] | Schedule 112A totals = sum of individual rows |
| VR2-CG-112A-008 [CAT-A] | LTCG u/s 112A in Schedule CG (B4a) = Total of Col 14 of Schedule 112A |

#### Schedule CG — Arithmetic Checks
| Rule ID | Validation |
|---|---|
| VR2-CG-001 [CAT-A] | Total STCG = sum of individual STCG components |
| VR2-CG-002 [CAT-A] | Total LTCG = sum of individual LTCG components |
| VR2-CG-003 [CAT-A] | Income under CG = A9 (total STCG) + B13 (total LTCG) |
| VR2-CG-004 [CAT-A] | If Full Value of Consideration = 0, transfer expenses cannot be claimed |
| VR2-CG-005 [CAT-A] | STCG A1c (Balance) = A1aiii (Full value) - A1biv (Deductions) |
| VR2-CG-006 [CAT-A] | STCG A1e = A1c - A1d (Exemption) |
| VR2-CG-007 [CAT-A] | For property: Full value of consideration = max(actual, stamp duty value) — Section 50C |
| VR2-CG-008 [CAT-A] | A4(a)(ic) = max(A4(a)(ia) actual, A4(a)(ib) indexed) — higher of actual or indexed cost |
| VR2-CG-009 [CAT-A] | Split rate computation: for AY 2025-26, STCG before July 23 at 15%, on/after at 20% |
| VR2-CG-010 [CAT-A] | LTCG on property: choice between 20% with indexation OR 12.5% without (for pre-July 23 assets) |
| VR2-CG-011 [CAT-A] | LTCG B4c = B4a (total 112A) - B4b (exemption 125000) |
| VR2-CG-012 [CAT-A] | Schedule CYLA and BFLA cross-references must be internally consistent |
| VR2-CG-013 [CAT-A] | Capital Gains Account Scheme deposit: if exemption claimed under 54/54F/54EC but investment not made, CGAS deposit must be reflected |
| VR2-CG-014 [CAT-A] | Section 50C SDV > 110% of actual sale price → use SDV as full consideration |
| VR2-CG-015 [CAT-A] | Resident cannot claim benefit under 112(1)(c) unless 115H option exercised |

#### Schedule CYLA
| Rule ID | Validation |
|---|---|
| VR2-CYLA-001 [CAT-A] | HP loss set-off against other heads ≤ ₹2,00,000 |
| VR2-CYLA-002 [CAT-A] | Speculative loss can only be set off against speculative income |
| VR2-CYLA-003 [CAT-A] | LTCG loss can only be set off against LTCG (not STCG) |
| VR2-CYLA-004 [CAT-A] | Current income after set-off = income - loss set off (minimum 0 per head) |
| VR2-CYLA-005 [CAT-A] | STCG at slab rate in CYLA = Sl. 11vi of item E of Schedule CG |

#### Schedule BFLA
| Rule ID | Validation |
|---|---|
| VR2-BFLA-001 [CAT-A] | BFLA Sl.2(xi) = CFL Sl.6(x) (prior year carry forward matches) |
| VR2-BFLA-002 [CAT-A] | BFLA Sl.2(ii) = CFL Sl.3c(x) |
| VR2-BFLA-003 [CAT-A] | BFLA Col 3 = Col 1 - Col 2 (remaining after set-off) |
| VR2-BFLA-004 [CAT-A] | BFLA Sl.1ix = CYLA Sl.4x (consistent cross-reference) |
| VR2-BFLA-005 [CAT-A] | BF loss can only be set off per original rules (HP loss only against HP in subsequent years) |

#### Schedule VIA — Deduction Validations (ITR-2)
| Rule ID | Validation |
|---|---|
| VR2-VIA-001 [CAT-A] | New regime: No deductions except 80CCD(2), 80JJAA, 80CCH(2) |
| VR2-VIA-002 [CAT-A] | 80G deduction claimed → Schedule 80G mandatory |
| VR2-VIA-003 [CAT-A] | 80G deduction ≤ eligible amount in Schedule 80G |
| VR2-VIA-004 [CAT-A] | 80G cash donations > ₹2,000 = ₹0 deduction |
| VR2-VIA-005 [CAT-A] | Donee PAN in Schedule 80G ≠ assessee PAN ≠ verification PAN |
| VR2-VIA-006 [CAT-A] | 80GGA: eligible donation ≤ total donation |
| VR2-VIA-007 [CAT-A] | AMT: If net income u/s 115JC > regular tax, AMT applies (Schedules AMT + AMTC) |
| VR2-VIA-008 [CAT-A] | HUF cannot claim relief u/s 89A |
| VR2-VIA-009 [CAT-A] | 89A income claimed for relief ≤ income offered in salary 1d |

#### Special Rate Income (Schedule SI)
| Rule ID | Validation |
|---|---|
| VR2-SI-001 [CAT-A] | Tax on special rate income = sum of (income × applicable rate) for each category |
| VR2-SI-002 [CAT-A] | STCG 111A tax rate: 15% (pre-July 23) / 20% (post July 23) in AY 2025-26 |
| VR2-SI-003 [CAT-A] | LTCG 112A tax rate: 10% (pre-July 23) / 12.5% (post July 23) in AY 2025-26 |
| VR2-SI-004 [CAT-A] | VDA income tax rate = 30% exactly (section 115BBH) |
| VR2-SI-005 [CAT-A] | Lottery/gambling income = 30% exactly (section 115BB) |
| VR2-SI-006 [CAT-A] | 87A rebate cannot be applied to special rate income |
| VR2-SI-007 [CAT-A] | Surcharge on 111A/112A income capped at 15% |
| VR2-SI-008 [CAT-A] | VDA income (115BBH): no loss set-off allowed; cannot be negative |
| VR2-SI-009 [CAT-A] | Anonymous donation u/s 115BBC is not applicable to ITR-2 |

---

## SECTION 10 — CBDT VALIDATION RULES — ITR-3

*Source: CBDT e-Filing ITR 3 Validation Rules AY 2025-26 V1.0, 10 July 2025*

### 10.1 ITR-3 Specific Validations

#### Schedule BP (Business/Profession)
| Rule ID | Validation |
|---|---|
| VR3-BP-001 [CAT-A] | Income reduced in A3b cannot exceed income offered in corresponding head |
| VR3-BP-002 [CAT-A] | Income reduced in A3c cannot exceed income offered |
| VR3-BP-003 [CAT-A] | Income reduced in A3d cannot exceed income offered |
| VR3-BP-004 [CAT-A] | Schedule BP A6 = sum of (1-2a-2b-3a-3b-3c-3d-3e-3f-4a...) — all arithmetic fields |
| VR3-BP-005 [CAT-A] | 80JJAA deduction: new employees must have emoluments ≤ ₹25,000/month |
| VR3-BP-006 [CAT-A] | 80JJAA deduction: business must be subject to tax audit |
| VR3-BP-007 [CAT-A] | 40A(3) disallowance: Cash payments > ₹10,000 per day per person must be added back |
| VR3-BP-008 [CAT-A] | 43B disallowances: Any unpaid expenses (taxes, PF, ESIC, bonus, interest on loans) that were not paid before filing date must be added back |
| VR3-BP-009 [CAT-A] | 36(1)(va): Employee PF contributions deposited after due date u/s 139(1) must be disallowed |
| VR3-BP-010 [CAT-A] | Partner remuneration: within 40(b) limits (90%/60% of book profit, min ₹1,50,000) |
| VR3-BP-011 [CAT-A] | F&O turnover = absolute profit + absolute loss (not gross contract value) |
| VR3-BP-012 [CAT-A] | Speculative business income kept separate from non-speculative |

#### Schedule DPM (Depreciation)
| Rule ID | Validation |
|---|---|
| VR3-DPM-001 [CAT-A] | Total depreciation on buildings = sum of (2a + 2b + ...) sub-items |
| VR3-DPM-002 [CAT-A] | Total depreciation = sum of all blocks |
| VR3-DPM-003 [CAT-A] | DPM fields cross-referenced with Schedule DEP totals |
| VR3-DPM-004 [CAT-A] | STCG on depreciable assets in CG A6e = Sl. 6 of DCG schedule |
| VR3-DPM-005 [CAT-A] | Goodwill: 0% depreciation from AY 2021-22 — if goodwill in block, depreciation = 0 |
| VR3-DPM-006 [CAT-A] | Additional depreciation (32(1)(iia)): only on new P&M; not on ships/aircraft/office equipment |
| VR3-DPM-007 [CAT-A] | If asset installed < 180 days: additional depreciation = 10% (not 20%) in year 1 |

#### Schedule DCG (Deemed Capital Gains on Depreciable Assets)
| Rule ID | Validation |
|---|---|
| VR3-DCG-001 [CAT-A] | DCG Sl.1e = sum of (1a+1b+1c+1d) |
| VR3-DCG-002 [CAT-A] | DCG Sl.2d = sum of (2a+2b+2c) |
| VR3-DCG-003 [CAT-A] | DCG Sl.1a = DPM Sl. corresponding opening WDV reference |
| VR3-DCG-004 [CAT-A] | DCG cross-references to DPM must be consistent |

---

## SECTION 11 — CBDT VALIDATION RULES — ITR-4 (SUGAM)

*Source: CBDT e-Filing ITR 4 Validation Rules AY 2025-26 V1.1, 10 July 2025*

### 11.1 ITR-4 Specific Validations

| Rule ID | Validation |
|---|---|
| VR4-001 [CAT-A] | Total income (excluding LTCG 112A) ≤ ₹50,00,000 for ITR-4 eligibility |
| VR4-002 [CAT-A] | No capital gains other than LTCG u/s 112A (and that too ≤ 1,25,000) in ITR-4 |
| VR4-003 [CAT-A] | LTCG u/s 112A: if > ₹1,25,000, must file ITR-2/3 |
| VR4-004 [CAT-A] | 44AD turnover ≤ ₹3 Cr (if cash ≤ 5%) OR ≤ ₹2 Cr otherwise |
| VR4-005 [CAT-A] | 44ADA receipts ≤ ₹75 Lakh (if cash ≤ 5%) OR ≤ ₹50 Lakh otherwise |
| VR4-006 [CAT-A] | 44AE: ≤ 10 goods vehicles at any time during the year |
| VR4-007 [CAT-A] | 44AD presumptive income ≥ 8% of cash turnover AND ≥ 6% of digital turnover |
| VR4-008 [CAT-A] | 44ADA presumptive income ≥ 50% of gross receipts |
| VR4-009 [CAT-A] | 44AE income: Heavy vehicle = 1000 × GVW × months; Non-heavy = 7500 × months |
| VR4-010 [CAT-A] | LLP cannot file ITR-4 (must file ITR-5) |
| VR4-011 [CAT-A] | If taxpayer had 44AD in prior year but opting out: must maintain books; flag for audit |
| VR4-012 [CAT-A] | Advance tax: 100% by March 15; 234C applies only for that single installment |
| VR4-013 [CAT-A] | One house property only in ITR-4; multiple HP → ITR-3 |
| VR4-014 [CAT-A] | Balance sheet fields (debtors, creditors, stock, cash) are MANDATORY |
| VR4-015 [CAT-A] | ITR-4 CreationInfo intermediary city ≤ 25 characters |
| VR4-016 [CAT-A] | GST turnover reconciliation mandatory if GSTIN declared |
| VR4-017 [CAT-A] | If previous year opted 44AD and not claiming this year: MUST show audit status |
| VR4-018 [CAT-A] | No business losses to be carried forward in ITR-4 (cannot carry forward in sugam form) |
| VR4-019 [CAT-A] | New regime: No deductions except 80CCD(2), 80CCH, standard deduction |
| VR4-020 [CAT-A] | 44AD and 44ADA cannot both be claimed for same taxpayer unless different businesses |

---

## SECTION 12 — END-TO-END DATA FLOW: FROM IMPORT TO JSON OUTPUT

### 12.1 Complete Pipeline

```
IMPORT PIPELINE:
┌─────────────────────────────────────────────────────────────┐
│  INPUT SOURCES                                              │
│  ┌──────────────┐ ┌────────────┐ ┌────────────────────────┐│
│  │ Form 16 PDF  │ │ AIS JSON/  │ │ Client-entered data    ││
│  │ (Part A + B) │ │ TIS/26AS   │ │ (manual fields)        ││
│  └──────┬───────┘ └─────┬──────┘ └────────────┬───────────┘│
│         │               │                     │            │
│         ▼               ▼                     ▼            │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              NORMALISATION LAYER                        ││
│  │  - PDF text extraction (Form 16 parser)                 ││
│  │  - JSON parsing (AIS/26AS)                              ││
│  │  - Amount normalisation (remove commas, to Integer)     ││
│  │  - Date normalisation (DD/MM/YYYY → ISO or vice versa)  ││
│  └────────────────────────┬────────────────────────────────┘│
│                           │                                 │
│                           ▼                                 │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              RECONCILIATION ENGINE                      ││
│  │  - Cross-check Form 16 TDS vs 26AS Part-A               ││
│  │  - Cross-check AIS income vs declared income            ││
│  │  - Generate reconciliation report                       ││
│  │  - Flag discrepancies for taxpayer review               ││
│  └────────────────────────┬────────────────────────────────┘│
│                           │                                 │
│                           ▼                                 │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              ITR FORM SELECTION ENGINE                  ││
│  │  - Determine correct ITR form (1/2/3/4)                 ││
│  │  - Based on income type flags from all sources          ││
│  │  - Override if user selects different form with warning  ││
│  └────────────────────────┬────────────────────────────────┘│
│                           │                                 │
│                           ▼                                 │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              COMPUTATION ENGINE                         ││
│  │  - Compute tax on total income (old/new regime)         ││
│  │  - Compute 87A rebate                                   ││
│  │  - Compute surcharge + marginal relief                  ││
│  │  - Compute cess                                         ││
│  │  - Compute 234A/B/C/F                                   ││
│  │  - Compute capital gains (split rate AY 2025-26)        ││
│  │  - Compute HRA, gratuity, leave encashment exemptions   ││
│  └────────────────────────┬────────────────────────────────┘│
│                           │                                 │
│                           ▼                                 │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              VALIDATION ENGINE                          ││
│  │  - Run ALL Category A rules (upload blockers)           ││
│  │  - Run ALL Category B rules (defective return risk)     ││
│  │  - Run ALL Category D rules (deduction warnings)        ││
│  │  - Output: errors[], warnings[], info[]                 ││
│  └────────────────────────┬────────────────────────────────┘│
│                           │                                 │
│                           ▼                                 │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              JSON GENERATION ENGINE                     ││
│  │  - Assemble ITR JSON per ITD schema                     ││
│  │  - Validate JSON structure (required fields, maxlength) ││
│  │  - Compute SHA-256 Digest                               ││
│  │  - Output: <PAN>.json (upload-ready)                    ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

---

### 12.2 ITR Form Auto-Selection Logic

```javascript
function selectITRForm(taxpayer) {
  const flags = analyzeIncomeSources(taxpayer);
  
  // ITR-1 eligibility
  if (
    flags.isResident === true &&
    flags.isIndividual === true &&
    flags.hasCapitalGains === false &&
    flags.hasBusinessIncome === false &&
    flags.hasMoreThanOneHP === false &&
    flags.hasForeignAssets === false &&
    flags.hasUnlistedShares === false &&
    flags.isDirector === false &&
    flags.totalIncome <= 5000000 &&
    flags.agriculturalIncome <= 5000 &&
    flags.hasTDS194N === false &&
    flags.hasESOPDeferral === false &&
    flags.hasLossCarryForward === false &&
    flags.hasBroughtForwardLosses === false &&
    flags.hasVDA === false &&
    (flags.ltcg112A === 0 || flags.ltcg112A <= 125000)  // From AY 2025-26, ITR-1 allows up to 1.25L 112A LTCG
  ) {
    return "ITR-1";
  }
  
  // ITR-4 eligibility (check before ITR-3)
  if (
    flags.isIndividualOrHUFOrFirm === true &&
    !flags.isLLP &&
    flags.totalIncome <= 5000000 &&
    (flags.is44AD || flags.is44ADA || flags.is44AE) &&
    !flags.hasCapitalGainsOtherThan112A &&
    (flags.ltcg112A <= 125000) &&
    !flags.hasMoreThanOneHP &&
    !flags.hasForeignAssets &&
    !flags.hasBusinessLossToCarryForward
  ) {
    return "ITR-4";
  }
  
  // ITR-3 (business income, F&O, regular business)
  if (
    flags.isIndividualOrHUF === true &&
    (flags.hasBusinessIncome || flags.hasFandO || flags.hasPartnershipIncome)
  ) {
    return "ITR-3";
  }
  
  // Default: ITR-2
  return "ITR-2";
}
```

---

### 12.3 Regime Comparison Engine

```javascript
function compareRegimes(taxpayer) {
  // Compute tax under BOTH regimes
  const oldRegimeTax = computeTax(taxpayer, 'OLD');
  const newRegimeTax = computeTax(taxpayer, 'NEW');
  
  return {
    oldRegimeTaxPayable: oldRegimeTax.netPayable,
    newRegimeTaxPayable: newRegimeTax.netPayable,
    recommendedRegime: oldRegimeTax.netPayable < newRegimeTax.netPayable ? 'OLD' : 'NEW',
    savings: Math.abs(oldRegimeTax.netPayable - newRegimeTax.netPayable),
    breakdown: {
      old: {
        totalIncome: oldRegimeTax.totalIncome,
        deductions: oldRegimeTax.totalDeductions,
        taxableIncome: oldRegimeTax.taxableIncome,
        taxOnIncome: oldRegimeTax.taxOnIncome,
        rebate87A: oldRegimeTax.rebate87A,
        surcharge: oldRegimeTax.surcharge,
        cess: oldRegimeTax.cess
      },
      new: {
        totalIncome: newRegimeTax.totalIncome,
        deductions: newRegimeTax.totalDeductions,
        taxableIncome: newRegimeTax.taxableIncome,
        taxOnIncome: newRegimeTax.taxOnIncome,
        rebate87A: newRegimeTax.rebate87A,
        surcharge: newRegimeTax.surcharge,
        cess: newRegimeTax.cess
      }
    },
    breakEvenDeduction: computeBreakEvenDeduction(taxpayer)
    // At what deduction level are both regimes equal
  };
}
```

---

## SECTION 13 — CRITICAL WARNINGS AND ERP SYSTEM ALERTS

### 13.1 Mandatory System Alerts

The ERP must generate the following alerts before allowing ITR submission:

| Alert Code | Trigger Condition | Alert Message | Action Required |
|---|---|---|---|
| ALERT-001 | 87A cliff — new regime AY 25-26: income between ₹6,90,000 and ₹7,10,000 | "INCOME NEAR ₹7L CLIFF: Tax at ₹7,00,000 = NIL. At ₹7,00,001 = ~₹25,010. Verify income" | Taxpayer confirmation |
| ALERT-002 | 87A cliff — new regime AY 26-27: income between ₹11,90,000 and ₹12,10,000 | "INCOME NEAR ₹12L CLIFF: Tax at ₹12,00,000 = NIL. At ₹12,00,001 = applicable (marginal relief). Verify income" | Taxpayer confirmation |
| ALERT-003 | Form 10E not filed but relief u/s 89 claimed | "FORM 10E IS MANDATORY before claiming Relief u/s 89. File Form 10E first, then file ITR" | Block submission until 10E filed |
| ALERT-004 | Form 10-IEA not filed but business taxpayer claiming old regime | "Form 10-IEA must be filed before ITR due date to opt out of new regime (business income cases)" | Block if late |
| ALERT-005 | AIS shows income not declared in ITR | "UNDECLARED INCOME DETECTED: AIS shows ₹X income not reflected in ITR. Risk of scrutiny notice" | Require taxpayer explanation |
| ALERT-006 | TDS in 26AS ≠ TDS claimed in ITR | "TDS MISMATCH: Form 26AS shows ₹X TDS vs ₹Y claimed in ITR" | Reconcile before filing |
| ALERT-007 | Belated return with losses to carry forward | "BELATED RETURN: Losses CANNOT be carried forward if ITR filed after due date" | Warn; taxpayer confirm |
| ALERT-008 | HP loss > ₹2L in ITR-1 | "HP LOSS EXCEEDS ₹2L CAP: ITR-1 cannot carry forward excess HP loss. Must file ITR-2" | Auto-switch to ITR-2 suggestion |
| ALERT-009 | Capital gains in ITR-1 | "CAPITAL GAINS DETECTED: ITR-1 cannot be used for capital gains (except LTCG 112A ≤ ₹1.25L). Must file ITR-2" | Force ITR-2 |
| ALERT-010 | VDA income detected | "CRYPTO/VDA INCOME DETECTED: Must be declared under Section 115BBH at 30% flat rate. Cannot be set off against any other income" | Mandatory disclosure |
| ALERT-011 | 44AD opted previous year, not claiming this year | "44AD OPT-OUT LOCK-IN: You claimed 44AD in a prior year. Opting out now requires books + audit for next 5 years" | Acknowledge lock-in |
| ALERT-012 | CGAS deposit not made before ITR due date | "CAPITAL GAINS EXEMPTION RISK: 54/54F/54EC exemption claimed but CGAS deposit not confirmed before filing date" | Confirm or remove exemption |
| ALERT-013 | Surcharge threshold crossed — marginal relief computed | "SURCHARGE THRESHOLD CROSSED: Marginal relief of ₹X applied. Verify computation" | Display breakdown |
| ALERT-014 | Standard deduction claimed twice (multiple employers) | "STANDARD DEDUCTION DUPLICATE: Multiple Form 16s detected. Ensuring standard deduction claimed only ONCE" | Auto-correct |
| ALERT-015 | Cash 80G donation > ₹2,000 | "80G CASH DONATION DISALLOWED: Cash donation of ₹X exceeds ₹2,000 limit. Excluded from deduction" | Auto-exclude |
| ALERT-016 | PAN-Aadhaar not linked | "PAN-AADHAAR LINK STATUS: Verify PAN-Aadhaar is linked. Unlinked PAN causes TDS at higher rate (206AA)" | Pre-filing check |
| ALERT-017 | EPF withdrawal before 5 years of service | "EPF WITHDRAWAL TAX: Withdrawal before 5 years of service is TAXABLE. Include in salary income" | Prompt disclosure |
| ALERT-018 | Property sold — Section 50C SDV > 110% of sale price | "STAMP DUTY VALUE (50C): SDV of ₹X is > 110% of sale price ₹Y. ITD will deem SDV as sale consideration" | Auto-compute 50C impact |
| ALERT-019 | MSME payment outstanding > 45 days (ITR-3) | "43B(h) MSME DISALLOWANCE: Payments to MSME suppliers outstanding > 45 days disallowed. Review P&L" | Require verification |
| ALERT-020 | Large cash in balance sheet (ITR-4) | "LARGE CASH BALANCE: Cash in hand of ₹X may attract scrutiny. Verify against books" | Flag for review |

---

### 13.2 JSON Pre-Submission Checklist (System-Enforced)

```javascript
const PRE_SUBMISSION_CHECKLIST = [
  { id: 'CHK-001', check: 'PAN format valid (AAAAA9999A)', mandatory: true },
  { id: 'CHK-002', check: 'PAN matches PAN database name', mandatory: true },
  { id: 'CHK-003', check: 'DOB matches PAN/Aadhaar database', mandatory: true },
  { id: 'CHK-004', check: 'Aadhaar linked to PAN (ITD API)', mandatory: true },
  { id: 'CHK-005', check: 'Regime election confirmed by taxpayer', mandatory: true },
  { id: 'CHK-006', check: 'Form 10E pre-filed (if 89 relief)', mandatory: true },
  { id: 'CHK-007', check: 'Form 10-IEA pre-filed (if business + old regime)', mandatory: true },
  { id: 'CHK-008', check: '26AS/AIS reconciled — no undeclared income', mandatory: true },
  { id: 'CHK-009', check: 'Bank account pre-validated (if refund)', mandatory: true },
  { id: 'CHK-010', check: 'All TDS claimed ≤ TDS in Form 26AS', mandatory: true },
  { id: 'CHK-011', check: 'Advance tax challans verified (BSR+serial match)', mandatory: true },
  { id: 'CHK-012', check: 'Capital Gains Account Scheme deposit confirmed', mandatory: true },
  { id: 'CHK-013', check: '80G donations — no cash > ₹2,000', mandatory: true },
  { id: 'CHK-014', check: 'Standard deduction claimed exactly once', mandatory: true },
  { id: 'CHK-015', check: 'All Category A validation rules: PASS', mandatory: true },
  { id: 'CHK-016', check: 'CreationInfo intermediary city ≤ 25 chars', mandatory: true },
  { id: 'CHK-017', check: 'JSON Digest (SHA-256) computed', mandatory: true },
  { id: 'CHK-018', check: 'All amounts are integers (no decimals)', mandatory: true },
  { id: 'CHK-019', check: 'No special characters in text fields (~@#$%^&*)', mandatory: true },
  { id: 'CHK-020', check: 'Date format: DD/MM/YYYY throughout', mandatory: true },
  { id: 'CHK-021', check: 'VDA income disclosed and 194S TDS verified', mandatory: false },
  { id: 'CHK-022', check: 'Foreign assets declared in Schedule FA', mandatory: false },
  { id: 'CHK-023', check: 'GSTIN turnover reconciled (ITR-3/4)', mandatory: false },
  { id: 'CHK-024', check: 'Schedule AL filled (if income > ₹50L)', mandatory: false },
];
```

---

*END OF IMPORT ENGINE, JSON SCHEMA & VALIDATION REFERENCE*

*Last updated based on: CBDT e-Filing Validation Rules ITR-1 V1.1 (10 July 2025), ITR-2 V1.0 (10 July 2025), ITR-3 V1.0 (10 July 2025), ITR-4 V1.1 (10 July 2025). ITD Prefill JSON schema as implemented in AY 2025-26 offline utility. AIS/TIS technical format as per CBDT AIS portal.*

*ALWAYS verify against the latest ITD schema XSD/JSON released on incometaxindia.gov.in before production deployment. The ITD releases updated utilities and schemas periodically throughout the filing season.*
