# ITR-1 MASTER COMPLIANCE AUDIT REPORT
## Comprehensive CBDT Compliance Analysis - AY 2025-26 & AY 2026-27

---

**Document Version:** 2.0 (Combined)  
**Generated:** May 2026  
**Assessment Years:** AY 2025-26, AY 2026-27  
**Requirement:** 101% CBDT Compliance  
**Source Documents:**  
- ITR1_STRICT_CBDT_COMPLIANCE_AUDIT.md  
- ITR1_2026-27_CBDT_SCHEMA_AUDIT.md

---

## EXECUTIVE SUMMARY

This master audit report consolidates findings from both the general CBDT compliance analysis and the official ITD JSON schema analysis for AY 2026-27. The report identifies **critical compliance gaps** that will result in ITR rejection when filing returns through this system.

### Overall Compliance Status

| Metric | AY 2025-26 | AY 2026-27 |
|--------|------------|------------|
| **Compliance Score** | ~52-68% | ~45% |
| **ITR Status** | NON-COMPLIANT | NON-COMPLIANT |
| **Critical Blockers** | 10+ | 15+ |
| **High Priority Gaps** | 15+ | 20+ |

### Key Findings

1. **Property Section** - Completely missing detailed fields (address, co-owners, tenants, loans)
2. **Verification Section** - Father's name NOT exported to JSON
3. **Filing Status** - New AY 2026-27 fields not implemented
4. **80G Donations** - No detailed breakdown (Donee PAN/Name)
5. **Bank Details** - Not exported to JSON for refunds
6. **New AY 2026-27 Schema** - Multiple new sections missing

---

## PART 1: MANDATORY FIELD GAPS BY SECTION

### 1.1 Personal Information (BLOCKER)

| Field | CBDT Requirement | AY 2025-26 | AY 2026-27 | Priority |
|-------|-----------------|------------|------------|----------|
| Father's Name | MANDATORY | ✗ MISSING | ✗ MISSING | BLOCKER |
| Marital Status | MANDATORY | ✗ MISSING | ✗ MISSING | BLOCKER |
| Gender | MANDATORY (M/F/T) | ✗ MISSING | ✗ MISSING | BLOCKER |
| Bank Details (Refund) | IF REFUND | ✗ MISSING | ✗ MISSING | BLOCKER |
| SecondaryAdd Flag | - | NOT REQUIRED | ✗ MISSING | BLOCKER |
| Alternate Address | - | NOT REQUIRED | ✗ MISSING | HIGH |

### 1.2 Filing Status

| Field | AY 2025-26 | AY 2026-27 | Priority |
|-------|------------|------------|----------|
| Return File Sec | ✓ | ✓ | OK |
| Opt Out New Regime | ✓ | ✓ | OK |
| Seventh Proviso 139(i) | ✗ NOT REQUIRED | ✗ MISSING | MEDIUM |
| High Value Transaction Flags | ✗ NOT REQUIRED | ✗ MISSING | MEDIUM |
| Assessee Rep Flag | NOT REQUIRED | ✗ MISSING | HIGH |
| Notice Details | ✗ NOT REQUIRED | ✗ MISSING | MEDIUM |

### 1.3 Salary Section

| Component | Status | Gap |
|-----------|--------|-----|
| Gross Salary | ✓ OK | - |
| 17(1) Salary | ✓ OK | - |
| Perquisites | ✓ OK | - |
| Profits in Lieu | ✓ OK | - |
| HRA Exemption Calc | ✓ OK | BUT worksheet not shown |
| AllwncExemptUs10 Details | ✗ PARTIAL | Not detailed array |
| Std Deduction (75K) | ✓ OK | - |
| Entertainment Allowance | ✓ OK | Only calculated, not shown |
| Professional Tax | ✓ OK | - |

### 1.4 House Property (CRITICAL BLOCKER)

This is the **most deficient section** with 30+ fields missing.

#### Current Status vs Schema Requirement

| Schema Field (AY 2026-27) | Required | Current | Priority |
|---------------------------|----------|---------|----------|
| PropertyDetails (Array, max 2) | YES | Only 1 | FIX NEEDED |
| HPSNo | YES | ✗ | BLOCKER |
| AddressDetailWithZipCode | YES | ✗ | BLOCKER |
| AddrDetail | YES | ✗ | BLOCKER |
| CityOrTownOrDistrict | YES | Partial | BLOCKER |
| StateCode (01-37,99) | YES | ✗ | BLOCKER |
| CountryCode (91) | YES | ✗ | BLOCKER |
| PinCode (6-digit) | YES | ✗ | BLOCKER |
| PropertyOwner (SE/MI/SP/OT) | YES | ✗ | BLOCKER |
| PropCoOwnedFlg (YES/NO) | YES | ✗ | BLOCKER |
| AsseseeShareProperty | NO | ✗ | HIGH |
| CoOwners Array | IF JOINT | ✗ | BLOCKER |
| CoOwnersSNo | IF JOINT | ✗ | BLOCKER |
| NameCoOwner | IF JOINT | ✗ | BLOCKER |
| PAN_CoOwner | IF JOINT | ✗ | HIGH |
| PercentShareProperty | IF JOINT | ✗ | HIGH |
| ifLetOut (L/D/S) | YES | ✓ | OK |
| TenantDetails Array | IF LET-OUT | ✗ | HIGH |
| TenantSNo | IF LET-OUT | ✗ | HIGH |
| NameofTenant | IF LET-OUT | ✗ | HIGH |
| PANofTenant | IF LET-OUT | ✗ | HIGH |
| Rentdetails Object | IF LET-OUT | ✗ | BLOCKER |
| AnnualLetableValue | IF LET-OUT | ✓ | OK |
| RentNotRealized | IF LET-OUT | ✗ | MEDIUM |
| LocalTaxes | IF LET-OUT | ✓ | OK |
| BalanceALV | COMPUTED | ✓ | OK |
| ThirtyPercentOfBalance | COMPUTED | ✓ | OK |
| IntOnBorwCap | IF APPLICABLE | ✓ | OK |
| Section24B | **NEW** | ✗ | BLOCKER |
| Section24BDtls Array | **NEW** | ✗ | BLOCKER |
| LoanTknFrom (B/I) | **NEW** | ✗ | BLOCKER |
| BankOrInstnName | **NEW** | ✗ | BLOCKER |
| LoanAccNoOfBankOrInstnRefNo | **NEW** | ✗ | BLOCKER |
| DateofLoan | **NEW** | ✗ | BLOCKER |
| TotalLoanAmt | **NEW** | ✗ | BLOCKER |
| LoanOutstndngAmt | **NEW** | ✗ | BLOCKER |
| InterestUs24B | **NEW** | ✗ | BLOCKER |
| TotalInterestUs24B | **NEW** | ✗ | BLOCKER |

### 1.5 Other Sources

| Component | Status | Gap |
|-----------|--------|-----|
| Savings Account Interest | ✓ OK | - |
| FD Interest | ✓ OK | - |
| NSC/SCSS/Post Office | ✓ OK | - |
| Dividend Income | ✓ OK | - |
| Family Pension | ✓ OK | - |
| OthersInc Details Array | ✗ NOT UI | HIGH |
| OthSrcNatureDesc breakdown | ✗ NOT UI | HIGH |

### 1.6 Exempt Income

| Component | Status | Gap |
|-----------|--------|-----|
| Agriculture (≤₹5K) | ✓ OK | - |
| Gratuity Exempt | ✓ OK | - |
| Leave Encashment Exempt | ✓ OK | - |
| PPF/Sukanya Interest | ✓ OK | - |
| ExemptIncAgriOthUs10 Array | ✗ NOT EXPANDED | HIGH |

### 1.7 Deductions

| Section | Status | Primary Gap |
|---------|--------|-------------|
| 80C | ✓ OK | No itemized breakdown |
| 80CCD(1) | ✓ OK | - |
| 80CCD(1B) | ✓ OK | - |
| 80CCD(2) | ✓ OK | - |
| 80D | ✓ OK | No self/family/parents breakdown |
| 80DD | PARTIAL | No form details |
| 80DDB | PARTIAL | No patient details |
| 80E | PARTIAL | No loan details |
| 80EE/80EEA/80EEB | PARTIAL | No property details |
| 80G | ✗ | **No breakdown - BLOCKER** |
| 80GG | PARTIAL | No worksheet |
| 80TTA/80TTB | ✓ OK | - |
| 80U | PARTIAL | No certificate |

### 1.8 Tax Payments

| Component | Status | Gap |
|-----------|--------|-----|
| TDS on Salary | ✓ OK | Limited UI |
| TDS on Other | ✓ OK | Limited UI |
| TCS | ✗ NOT UI | MEDIUM |
| ScheduleTDS3Dtls (194N) | ✗ NOT UI | MEDIUM |
| Advance Tax | ✓ OK | Limited UI |
| Self-Assessment Tax | ✗ NOT UI | MEDIUM |

### 1.9 New Sections (AY 2026-27)

| Section | Status | Priority |
|---------|--------|----------|
| LTCG112A | ✗ NOT IMPLEMENTED | HIGH |
| Schedule80GGA | ✗ NOT IMPLEMENTED | HIGH |
| Schedule80GGC | ✗ NOT IMPLEMENTED | HIGH |
| Schedule80C | ✗ NOT IMPLEMENTED | HIGH |
| ScheduleEA10_13A | PARTIAL | MEDIUM |

### 1.10 Verification

| Field | Status | Gap |
|-------|--------|-----|
| Capacity | ✓ OK | - |
| AssesseeVerName | ✓ OK | - |
| **FatherName** | **✗ MISSING** | **BLOCKER** |
| Place | ✓ OK | - |
| Date | ✓ OK | - |
| Declaration | PARTIAL | MEDIUM |

---

## PART 2: VALIDATION RULES

### 2.1 Existing VR1-xxx Rules (Implemented)

| Rule | Description | Status |
|------|-------------|--------|
| VR1-001 | 80C+CCC+CCD(1) > ₹1.5L | ✓ |
| VR1-002 | 80CCD(1) > 20% GTI pensioners | ✓ |
| VR1-003 | 80CCD(1) > 10% salary | ✓ |
| VR1-004 | 80CCD(2) exceeds limit | ✓ |
| VR1-005 | 80DDB > ₹1L senior | ✓ |
| VR1-007 | 80DDB > ₹40K non-senior | ✓ |
| VR1-008 | 80G without Schedule | ✓ |
| VR1-010 | 80TTA > ₹10K | ✓ |
| VR1-012 | Senior cannot claim 80TTA | ✓ |
| VR1-013 | 80TTB > ₹50K | ✓ |
| VR1-014 | Non-senior cannot claim 80TTB | ✓ |
| VR1-015 | Total deductions > GTI | ✓ |
| VR1-016 | Deductions mismatch | ✓ |
| VR1-017 | PAN format invalid | ✓ |
| VR1-018 | GTI must be > 0 when tax > 0 | ✓ |
| VR1-020 | GTI != Salary+HP+OS | ✓ |
| VR1-021 | No 87A when TI > 5L old | ✓ |
| VR1-022 | TI calculation error | ✓ |
| VR1-023 | Tax after rebate mismatch | ✓ |
| VR1-024 | Total tax mismatch | ✓ |
| VR1-026 | Interest/fees mismatch | ✓ |
| VR1-027 | Agri income > 5K - use ITR-2 | ✓ |
| VR1-028 | Exempt income mismatch | ✓ |
| VR1-037 | Std deduction != 30% AV | ✓ |
| VR1-038 | Municipal tax needs rent > 0 | ✓ |
| VR1-039 | Let-out needs rent > 0 | ✓ |
| VR1-040 | AV != Rent - Tax | ✓ |
| VR1-042 | SOP interest > 2L old regime | ✓ |
| VR1-043 | No SOP interest new regime | ✓ |
| VR1-044 | No municipal tax for SOP | ✓ |
| VR1-045 | Gross != 17(1)+17(2)+17(3) | ✓ |
| VR1-046 | Net != Gross - Exempt | ✓ |
| VR1-048 | Income != Net - Sec16 | ✓ |
| VR1-049 | Exempt > Gross | ✓ |
| VR1-050 | LTA > 17(1) | ✓ |
| VR1-061 | No LTA in new regime | ✓ |
| VR1-062 | No HRA in new regime | ✓ |
| VR1-064 | No EA in new regime | ✓ |
| VR1-065 | No PT in new regime | ✓ |
| VR1-066 | EA only for Govt | ✓ |
| VR1-068 | Std deduction > limit | ✓ |
| VR1-071 | No FP deduction in new regime | ✓ |
| VR1-072 | FP deduction > limit | ✓ |
| VR1-077 | OS income mismatch | ✓ |
| VR1-082 | TCS credit > collected | ✓ |
| VR1-083 | Total TCS mismatch | ✓ |
| VR1-086 | Total TDS1 mismatch | ✓ |
| VR1-087 | Total TDS2 mismatch | ✓ |
| VR1-091 | Bank for refund | ✓ |
| VR1-093 | TI > 50L - use ITR-2 | ✓ |
| VR1-094 | Cannot claim 80EE + 80EEA | ✓ |
| VR1-098 | Invalid TAN | ✓ |
| VR1-099 | PAN 4th char must be P | ✓ |

### 2.2 Missing Validation Rules

| Rule | Description | Priority |
|------|-------------|----------|
| R01 | Residential Status must be ROR | HIGH |
| R02 | Only 1 property for ITR-1 | HIGH |
| R03 | HRA requires rent proof | HIGH |
| R04 | Form 10IEA for new regime opt-out | HIGH |
| R05 | Gift donor PAN if > ₹50K | HIGH |
| R06 | Employer Category exact enum (CGOV, SGOV, etc.) | HIGH |
| R07 | Property Type L/D/S | HIGH |
| R08 | State Code validation (01-37,99) | HIGH |
| R09 | Section 24B loan details | HIGH |
| R10 | Co-owner PAN format | HIGH |

---

## PART 3: COMPREHENSIVE GAP ANALYSIS

### 3.1 Priority 1 - BLOCKERS (Will Cause ITR Rejection)

| # | Section | Missing Field | Action Required |
|---|---------|---------------|-----------------|
| 1 | PersonalInfo | Father's Name | Add to JSON export |
| 2 | PersonalInfo | Marital Status | Add to UI + JSON |
| 3 | PersonalInfo | Gender | Add to UI + JSON |
| 4 | PersonalInfo | Bank Details | Add to JSON export |
| 5 | PersonalInfo | SecondaryAdd (AY26-27) | Add field |
| 6 | House Property | Property Address | Completely rebuild |
| 7 | House Property | Property Pincode | Add to UI |
| 8 | House Property | Co-owner Details | Add array |
| 9 | House Property | Tenant Details | Add array |
| 10 | House Property | Section24B Loans | Add array |
| 11 | House Property | AddressDetailWithZipCode | Rebuild structure |
| 12 | Verification | FatherName | Add to JSON export |
| 13 | 80G | Donee breakdown | Add detailed UI |

### 3.2 Priority 2 - HIGH (May Cause Rejection)

| # | Section | Missing Field |
|---|---------|---------------|
| 1 | Salary | HRA Calculation Worksheet |
| 2 | Salary | LTA Journey Details |
| 3 | Salary | Employer Address details |
| 4 | Other Sources | Bank-wise Interest breakdown |
| 5 | House Property | Second property support |
| 6 | 80D | Breakdown by self/family/parents |
| 7 | Tax Payments | TCS UI |
| 8 | Tax Payments | Self-Assessment Tax UI |
| 9 | Tax Payments | ScheduleTDS3Dtls (194N) |
| 10 | LTCG | LTCG112A section (AY26-27) |
| 11 | Schedules | Additional deduction schedules |
| 12 | FilingStatus | Seventh Proviso fields |
| 13 | FilingStatus | AssesseeRep details |

### 3.3 Priority 3 - MEDIUM (Improvements Needed)

| # | Section | Missing Field |
|---|---------|---------------|
| 1 | Salary | Entertainment Allowance Calc |
| 2 | Salary | Leave Encashment Details |
| 3 | House Property | Pre-construction Interest |
| 4 | Other Sources | Dividend breakdown |
| 5 | Exempt Income | Schedule EI UI |
| 6 | Deductions | 80E loan details |
| 7 | Deductions | 80EE property details |
| 8 | Deductions | 80U certificate details |
| 9 | PersonalInfo | Alternate Address (AY26-27) |

---

## PART 4: EMPLOYER CATEGORY ENUMS

The AY 2026-27 schema requires EXACT enum values:

```
CGOV    - Central Government
SGOV    - State Government
PSU     - Public Sector Undertaking
PE      - Pensioners - Central Government
PESG    - Pensioners - State Government
PEPS    - Pensioners - Public sector undertaking
PEO     - Pensioners - Others
OTH     - Others
NA      - Not Applicable
```

**Action Required:** Update all dropdowns to use these exact codes.

---

## PART 5: STATE CODES

Required in PropertyDetails AddressDetailWithZipCode:

| Code | State |
|------|-------|
| 01 | Andaman and Nicobar islands |
| 02 | Andhra Pradesh |
| 03 | Arunachal Pradesh |
| 04 | Assam |
| 05 | Bihar |
| 06 | Chandigarh |
| 07 | Dadra Nagar and Haveli |
| 08 | Daman and Diu |
| 09 | Delhi |
| 10 | Goa |
| 11 | Gujarat |
| 12 | Haryana |
| 13 | Himachal Pradesh |
| 14 | Jammu and Kashmir |
| 15 | Karnataka |
| 16 | Kerala |
| 17 | Lakshadweep |
| 18 | Madhya Pradesh |
| 19 | Maharashtra |
| 20 | Manipur |
| 21 | Meghalaya |
| 22 | Mizoram |
| 23 | Nagaland |
| 24 | Odisha |
| 25 | Puducherry |
| 26 | Punjab |
| 27 | Rajasthan |
| 28 | Sikkim |
| 29 | Tamil Nadu |
| 30 | Tripura |
| 31 | Uttar Pradesh |
| 32 | West Bengal |
| 33 | Chhattisgarh |
| 34 | Uttarakhand |
| 35 | Jharkhand |
| 36 | Telangana |
| 37 | Ladakh |
| 99 | Foreign |

---

## PART 6: JSON STRUCTURE COMPARISON

### Current vs Required (AY 2026-27 Schema)

```json
// CURRENT JSON EXPORT - DEFICIENT
{
  "ITR": {
    "ITR1": {
      "CreationInfo": { ... },                    // ✓ OK
      "Form_ITR1": { ... },                       // ✓ OK
      "PersonalInfo": { ... },                    // ⚠ MISSING: SecondaryAdd, FatherName
      "FilingStatus": { ... },                    // ⚠ MISSING: 10+ new fields
      "ITR1_IncomeDeductions": { ... },           // ⚠ PropertyDetails incomplete
      "ITR1_TaxComputation": { ... },             // ✓ OK
      "TaxPaid": { ... },                         // ✓ OK
      "Refund": { ... },                          // ⚠ MISSING: BankAccountDtls
      "Verification": { ... }                     // ⚠ MISSING: FatherName
    }
  }
}

// REQUIRED AY 2026-27 STRUCTURE
{
  "ITR": {
    "ITR1": {
      "CreationInfo": { ... },
      "Form_ITR1": { 
        "AssessmentYear": "2026",          // Changed to just "2026"
        "SchemaVer": "Ver1.0",             // Changed from Ver1.2
        "FormVer": "Ver1.0"                // Changed from Ver1.2
      },
      "PersonalInfo": {
        "AssesseeName": { ... },
        "PAN": "...",
        "Address": { ... },
        "SecondaryAdd": "Y|N",             // NEW
        "AlternateAddress": { ... },       // NEW
        "DOB": "...",
        "EmployerCategory": "CGOV|...",
        "AadhaarCardNo": "..."
      },
      "FilingStatus": {
        "ReturnFileSec": 11,
        "OptOutNewTaxRegime": "Y|N",
        "SeventhProvisio139": "Y|N",       // NEW
        "IncrExpAggAmt2LkTrvFrgnCntryFlg": "Y|N", // NEW
        "AmtSeventhProvisio139ii": 200000, // NEW
        "IncrExpAggAmt1LkElctrctyPrYrFlg": "Y|N", // NEW
        "AmtSeventhProvisio139iii": 100000,       // NEW
        "clauseiv7provisio139i": "Y|N",   // NEW
        "clauseiv7provisio139iDtls": [...],       // NEW
        "AsseseeRepFlg": "Y|N",           // NEW
        "AssesseeRep": { ... },           // NEW
        "ItrFilingDueDate": "2026-07-31"
      },
      "ITR1_IncomeDeductions": {
        "GrossSalary": 0,
        "Salary": 0,
        "PerquisitesValue": 0,
        "ProfitsInSalary": 0,
        "AllwncExemptUs10": { "AllwncExemptUs10Dtls": [], "TotalAllwncExemptUs10": 0 },
        "NetSalary": 0,
        "DeductionUs16": 0,
        "DeductionUs16ia": 0,
        "EntertainmentAlw16ii": 0,
        "ProfessionalTaxUs16iii": 0,
        "IncomeFromSal": 0,
        "PropertyDetails": [               // COMPLETELY REBUILT
          {
            "HPSNo": 1,
            "AddressDetailWithZipCode": {
              "AddrDetail": "...",
              "CityOrTownOrDistrict": "...",
              "StateCode": "19",
              "CountryCode": "91",
              "PinCode": 400001
            },
            "PropertyOwner": "SE",
            "PropCoOwnedFlg": "NO",
            "ifLetOut": "S",
            "Rentdetails": {
              "IntOnBorwCap": 0,
              "Section24B": {
                "Section24BDtls": [],
                "TotalInterestUs24B": 0
              }
            }
          }
        ],
        "GrossTotIncome": 0,
        "GrossTotIncomeIncLTCG112A": 0,
        "UsrDeductUndChapVIA": { ... },
        "DeductUndChapVIA": { ... },
        "TotalIncome": 0,
        "ExemptIncAgriOthUs10": { ... }
      },
      "LTCG112A": { ... },                 // NEW SECTION
      "Schedule80G": { ... },
      "Schedule80GGA": { ... },
      "Schedule80GGC": { ... },
      "Schedule80D": { ... },
      "Schedule80DD": { ... },
      "Schedule80U": { ... },
      "Schedule80E": { ... },
      "Schedule80EE": { ... },
      "Schedule80EEA": { ... },
      "Schedule80EEB": { ... },
      "Schedule80C": { ... },
      "ScheduleEA10_13A": { ... },
      "TDSonSalaries": { ... },
      "TDSonOthThanSals": { ... },
      "ScheduleTDS3Dtls": { ... },         // NEW
      "ScheduleTCS": { ... },
      "TaxPayments": { ... },
      "Refund": {
        "RefundDue": 0,
        "BankAccountDtls": {               // MISSING
          "IFSCCode": "...",
          "BankName": "...",
          "AccountNo": "...",
          "AccountType": "SB"
        }
      },
      "Verification": {
        "AssesseeVerName": "...",
        "FatherName": "...",               // MISSING - BLOCKER
        "Capacity": "S",
        "Place": "...",
        "Date": "2026-05-31",
        "Declaration": "..."
      }
    }
  }
}
```

---

## PART 7: REMEDIATION ROADMAP

### Phase 1: Critical Fixes (Week 1-2)

1. **Rebuild House Property Section**
   - Add PropertyDetails array (max 2)
   - Add AddressDetailWithZipCode with PinCode
   - Add CoOwners array with PAN/Aadhaar
   - Add TenantDetails array
   - Add Section24B with loan details

2. **Fix JSON Export**
   - Add FatherName to Verification section
   - Add BankAccountDtls to Refund section
   - Update Form_ITR1 for AY 2026-27 (AssessmentYear="2026")

3. **Personal Info Updates**
   - Add SecondaryAdd flag
   - Add field capturing for Gender/MaritalStatus

### Phase 2: High Priority (Week 3-4)

4. **Add 80G Breakdown UI**
   - Donee Name, PAN, Address, Amount
   - Category (100%/50%)
   - Payment mode

5. **Add New AY 2026-27 Sections**
   - LTCG112A section
   - Schedule80GGA, Schedule80GGC
   - ScheduleTDS3Dtls (194N)

6. **Add Filing Status Fields**
   - Seventh Proviso fields
   - AssesseeRep details

### Phase 3: Medium Priority (Month 2)

7. **Expand Other Sections**
   - HRA calculation worksheet display
   - Bank-wise interest breakdown
   - Dividend company-wise details

8. **Additional Schedules**
   - 80D breakdown
   - 80E loan details
   - 80EE property details

9. **UI Components**
   - TCS entry form
   - Self-Assessment Tax form

---

## PART 8: COMPLIANCE SCORE SUMMARY

| Component | AY 2025-26 Score | AY 2026-27 Score | Gap |
|-----------|-----------------|-----------------|-----|
| CreationInfo | 100% | 100% | - |
| Form_ITR1 | 100% | 100% | - |
| PersonalInfo | 67% | 50% | -17% |
| FilingStatus | 44% | 30% | -14% |
| Salary | 100% | 100% | - |
| House Property | 14% | 14% | - |
| Other Sources | 70% | 60% | -10% |
| Deductions | 75% | 70% | -5% |
| Exempt Income | 50% | 50% | - |
| Tax Payments | 75% | 60% | -15% |
| New Sections | N/A | 0% | NEW |
| Verification | 80% | 80% | - |
| **OVERALL** | **~52-68%** | **~45%** | **-15%** |

---

## APPENDIX A: TESTING CHECKLIST

Before declaring 101% compliance, verify:

- [ ] JSON validates against ITR-1_2026_Main_V1.0_0.json schema
- [ ] All required fields present in generated JSON
- [ ] Field types match (integer vs string vs boolean)
- [ ] Enum values match exactly (CGOV, SGOV, etc.)
- [ ] Date formats are YYYY-MM-DD
- [ ] Assessment Year shows "2026" (not "2026-27")
- [ ] SchemaVer shows "Ver1.0"
- [ ] FormVer shows "Ver1.0"
- [ ] All new AY 2026-27 fields included
- [ ] PropertyDetails array supports max 2 items
- [ ] Section24B loan details present
- [ ] FatherName exported in Verification
- [ ] BankAccountDtls exported in Refund

---

## APPENDIX B: KEY DIFFERENCES AY 2025-26 vs AY 2026-27

| Aspect | AY 2025-26 | AY 2026-27 |
|--------|------------|------------|
| Properties | 1 max | 2 max |
| Property Address | Basic | Detailed (AddressDetailWithZipCode) |
| Section 24B | Not required | Detailed loan tracking |
| LTCG | Not in ITR-1 | LTCG112A section new |
| Seventh Proviso | Not required | High-value transaction reporting |
| Secondary Address | Not required | SecondaryAdd flag |
| AssessmentYear Value | "2025" | "2026" |
| Schema Version | Ver1.2 | Ver1.0 |

---

## APPENDIX C: REFERENCE DOCUMENTS

1. **Official JSON Schema**: ITR-1_2026_Main_V1.0_0.json (Released May 15, 2026)
2. **Validation Rules**: CBDT_e-Filing_ITR_1_Validation_Rules_AY_2026-27.pdf
3. **ITD Portal**: https://www.incometax.gov.in/iec/foportal/downloads
4. **Form Description**: For individuals being a resident (other than not ordinarily resident) having total income upto Rs.50 lakh and having Income from Salaries, two house properties, other sources (Interest etc.), long-term capital gains under section 112A up to Rs. 1.25 lakh, and agricultural income up to Rs.5 thousand

---

*Document Consolidated: May 2026*  
*Purpose: ITR-1 Implementation 101% CBDT Compliance*  
*Status: Action Required - Multiple Blockers Identified*
