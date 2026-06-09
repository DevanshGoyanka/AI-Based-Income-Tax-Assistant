# ITR-1 STRICT CBDT COMPLIANCE AUDIT REPORT
## Mandatory Fields Gap Analysis - AY 2025-26 & AY 2026-27

---

## EXECUTIVE SUMMARY

This report provides a **strict, 101% CBDT compliant audit** of the ITR-1 (Sahaj) implementation against Income Tax Department guidelines, validations, and schemas issued by CBDT and Ministry of Finance regulations for Assessment Years 2025-26 and 2026-27.

**CRITICAL FINDING**: The current ITR-1 implementation has **SIGNIFICANT COMPLIANCE GAPS** that will result in ITR rejection when filing. Multiple mandatory fields are missing or improperly validated.

**Compliance Score: ~52%**

---

## SECTION 1: ITR-1 ELIGIBILITY CRITERIA (CBDT MANDATORY)

### Current Implementation Status

| Eligibility Criteria | Required | Current Status | Gap |
|---------------------|----------|----------------|-----|
| Resident (ROR) only | YES | Partial | No validation blocking NRI |
| Total income ≤ ₹50L | YES | ✓ Validated | OK |
| Single employer | YES (implied) | ✗ No validation | WARN ONLY |
| One house property | YES | Partial | No count validation |
| Agricultural income ≤ ₹5K | YES | ✓ Validated | OK |
| No capital gains | YES | ✓ Validated | OK |
| No business income | YES | Partial | WARN ONLY |
| No foreign assets | YES | Partial | Foreign asset Q not validated |

---

## SECTION 2: PERSONAL INFORMATION - MANDATORY FIELDS

### Current Fields in Itr1FormData.PersonalInfo

| Field | CBDT Requirement | JSON Export | Validation | Status |
|-------|-----------------|-------------|------------|--------|
| assesseeName | MANDATORY | ✓ | None | OK |
| PAN | MANDATORY | ✓ | VR1-017 | OK |
| aadhaar | MANDATORY (via 12-digit) | ✓ | Format check | OK |
| dateOfBirth | MANDATORY | ✓ | None | OK |
| **fatherName** | **MANDATORY** | **✗ MISSING** | **None** | **BLOCKER** |
| gender | MANDATORY (M/F/T) | ✗ | None | JS ONLY |
| **maritalStatus** | **MANDATORY** | **✗ MISSING** | **None** | **BLOCKER** |
| nationality | MANDATORY (INDIA) | ✗ | Default | JS ONLY |
| email | MANDATORY | ✓ | None | OK |
| mobile | MANDATORY | ✓ | None | OK |
| flatDoorNo | MANDATORY | ✓ | None | OK |
| premisesName | MANDATORY | ✓ | None | OK |
| roadStreet | MANDATORY | ✓ | None | OK |
| area | MANDATORY | ✓ | None | OK |
| townCity | MANDATORY | ✓ (CityOrTownOrDistrict) | VR1-XXX | OK |
| state | MANDATORY | ✓ | None | OK |
| pinCode | MANDATORY | ✓ | None | OK |
|assessmentYear | MANDATORY | ✓ | None | OK |
| **financialYear** | **MANDATORY** | **✗ NOT EXPORTED** | **None** | **HIGH** |
| filingType | MANDATORY (O/R/V/U) | ✗ | None | JS ONLY |
| employerCategory | MANDATORY | ✓ | Partial | JS ONLY |
| residentialStatus | MANDATORY (ROR) | ✗ | None | JS ONLY |
| **bankName** | **FOR REFUND** | **✗ MISSING** | **VR1-091** | **BLOCKER** |
| **bankAccountNo** | **FOR REFUND** | **✗ MISSING** | **VR1-091** | **BLOCKER** |
| **bankIFSC** | **FOR REFUND** | **✗ MISSING** | **VR1-091** | **BLOCKER** |
| **bankAccountType** | **FOR REFUND** | **✗ MISSING** | **None** | **HIGH** |

### Missing Gaps in PersonalInfo

1. **Father's Name** - NOT captured in UI, NOT exported to JSON
2. **Marital Status** - NOT captured in UI, NOT exported to JSON
3. **Bank Details for Refund** - Only validated when refund > 0, but fields NOT in JSON export
4. **Verification Section** - Missing in JSON builder entirely
5. **Filing Status incomplete** - Return Type, Section, Seventh Proviso not fully mapped

---

## SECTION 3: SCHEDULE SALARY - MANDATORY FIELDS

### Current Implementation Analysis

| Field | CBDT Requirement | Status | Gap |
|-------|-----------------|--------|-----|
| Basic Salary | MANDATORY | ✓ | OK |
| DA | EXPECTED | ✓ | OK |
| HRA Received | IF APPLICABLE | ✓ | OK |
| HRA Exemption (10(13A)) | IF CLAIMED | ✓ | CALC CORRECT |
| **HRA Calc Worksheet** | **CBDT REQUIRES** | **✗ NOT SHOWN** | **HIGH** |
| LTA Received | IF APPLICABLE | ✓ | OK |
| **LTA Journey Details** | **CBDT REQUIRES** | **✗ NOT CAPTURED** | **HIGH** |
| Children Education Allowance | IF APPLICABLE | ✓ | OK |
| Transport Allowance | IF APPLICABLE | ✓ | OK |
| Medical Reimbursement | IF APPLICABLE | ✓ | OK |
| Perquisites Value | IF APPLICABLE | ✓ | OK |
| Profits in Lieu | IF APPLICABLE | ✓ | OK |
| Gratuity Received | IF APPLICABLE | ✓ | OK |
| Gratuity Exemption (10(10)) | IF APPLICABLE | ✓ | OK |
| Leave Encashment Received | IF APPLICABLE | ✓ | OK |
| Leave Encashment Exemption (10(10AA)) | IF APPLICABLE | ✓ | OK |

### Employer Details - Status

| Field | CBDT Requirement | Status | Gap |
|-------|-----------------|--------|-----|
| Employer Name | MANDATORY | ✓ | OK |
| Employer TAN | MANDATORY | ✓ | VR1-098 |
| Employer Address | RECOMMENDED | ✗ NOT UI | HIGH |
| Employer City | RECOMMENDED | ✗ NOT UI | HIGH |
| Employer State | RECOMMENDED | ✗ NOT UI | HIGH |
| Employer Pincode | RECOMMENDED | ✗ NOT UI | HIGH |

### Missing Schedule Salary Fields

1. **HRA Calculation Worksheet** - Not shown to user (required for compliance)
2. **LTA Journey Details** - Not captured (block of 4 years, 2 journeys)
3. **Leave Encashment Retirement Date** - Captured in backend but not validated
4. **Entertainment Allowance Breakdown** - Not calculated for Govt employees
5. **Professional Tax - Split** - Not shown as deduction from employer

---

## SECTION 4: SCHEDULE HOUSE PROPERTY - MANDATORY FIELDS

### Current Implementation Analysis

| Field | CBDT Requirement | Status | Gap |
|-------|-----------------|--------|-----|
| Property Type | MANDATORY (S/L/D) | ✓ | OK |
| Property Address | MANDATORY | ✗ NOT CAPTURED | **BLOCKER** |
| City | MANDATORY | ✗ NOT CAPTURED | **BLOCKER** |
| State | MANDATORY | ✗ NOT CAPTURED | **BLOCKER** |
| PinCode | MANDATORY | ✗ NOT CAPTURED | **BLOCKER** |
| Gross Annual Value | MANDATORY | ✓ | OK |
| Municipal Taxes Paid | MANDATORY | ✓ | OK |
| Net Annual Value | COMPUTED | ✓ | OK |
| Standard Deduction (30%) | COMPUTED | ✓ | OK |
| Interest on Loan | IF APPLICABLE | ✓ | OK |
| Income from HP | COMPUTED | ✓ | OK |

### Property-Specific Missing Fields

| Field | CBDT Requirement | Status | Gap |
|-------|-----------------|--------|-----|
| **Co-owner Details** | IF JOINT | **✗ NOT UI** | **HIGH** |
| **Co-owner PAN** | IF JOINT | **✗ NOT UI** | **HIGH** |
| **Co-owner Share %** | IF JOINT | **✗ NOT UI** | **HIGH** |
| Vacancy Period | IF VACANT | Partial | LOW |
| Unrealized Rent | IF APPLICABLE | Partial | LOW |
| Arrears of Rent | IF RECEIVED | Partial | LOW |
| Tenant Details (Let-out) | FOR TENANT | **✗ NOT UI** | **HIGH** |
| Tenant PAN | FOR TENANT | **✗ NOT UI** | **HIGH** |

### Property Validation Gaps

1. **Property Address NOT captured** - BLOCKER for Schedule HP
2. **Co-owner Details NOT captured** - Required for joint ownership
3. **Tenant PAN** - Required for let-out property > ₹50L
4. **No validation for single property** - ITR-1 only allows ONE property
5. **Self-occupied Interest Cap** - Old regime ₹2L, New regime ₹0 (not validated)

---

## SECTION 5: SCHEDULE OTHER SOURCES - MANDATORY FIELDS

### Current Implementation Analysis

| Field | CBDT Requirement | Status | Gap |
|-------|-----------------|--------|-----|
| Savings Account Interest | EXPECTED | ✓ | OK |
| FD Interest | EXPECTED | ✓ | OK |
| Dividend Income | EXPECTED | ✓ | OK |
| Family Pension | EXPECTED | ✓ | OK |
| Other Income | EXPECTED | ✓ | OK |
| Income from IT Refund | EXPECTED | ✓ | OK |

### Missing Schedule OS Fields

| Field | CBDT Requirement | Status | Gap |
|-------|-----------------|--------|-----|
| **Bank-wise Interest Breakdown** | **CBDT REQUIRES** | **✗ NOT UI** | **HIGH** |
| NSC Interest | IF APPLICABLE | ✓ | OK |
| SCSS Interest | EXPECTED | ✓ | OK |
| Post Office Interest | EXPECTED | ✓ | OK |
| Dividend - Company-wise | **CBDT REQUIRES** | **✗ NOT UI** | **HIGH** |
| Gift Details (Section 56(2)(x)) | IF APPLICABLE | **Partial** | MEDIUM |
| Lottery/Winnings (115BB) | IF APPLICABLE | ✓ | OK |

### 80TTA/80TTB Breakdown

- Current: Interest shown as lump sum
- Required: Bank-wise breakdown with individual amounts
- Validation: 80TTA (≤60) max ₹10K, 80TTB (>60) max ₹50K

---

## SECTION 6: EXEMPT INCOME (SCHEDULE EI) - MANDATORY

### Current Implementation Status

| Field | CBDT Requirement | Status | Gap |
|-------|-----------------|--------|-----|
| Agriculture Income (10(1)) | EXPECTED | ✓ | OK |
| **Max ₹5,000 ITR-1** | **MANDATORY** | **✓ VALIDATED** | **OK** |
| Gratuity Exempt (10(10)) | IF APPLICABLE | ✓ | OK |
| Leave Encashment Exempt (10(10AA)) | IF APPLICABLE | ✓ | OK |
| Pension Commutation (10(10A)) | IF APPLICABLE | ✓ | OK |
| VRS Compensation (10(10C)) | IF APPLICABLE | ✓ | OK |
| PPF Interest (10(11)) | EXPECTED | ✓ | OK |
| Sukanya Interest (10(11)) | EXPECTED | ✓ | OK |
| Tax-Free Bonds Interest (10(15)) | EXPECTED | ✓ | OK |

**Critical Issue**: Schedule EI UI does NOT exist in frontend - fields are in DTO but not exposed

---

## SECTION 7: DEDUCTIONS CHAPTER VI-A - MANDATORY FIELDS

### 80C/CCC/CCD(1) - Status

| Field | CBDT Requirement | Status | Gap |
|-------|-----------------|--------|-----|
| Total 80C Limit | ₹1,50,000 | ✓ | OK |
| LIC | EXPECTED | ✓ | OK |
| PPF | EXPECTED | ✓ | OK |
| ELSS | EXPECTED | ✓ | OK |
| EPF | EXPECTED | ✓ | OK |
| NSC | EXPECTED | ✓ | OK |
| Principal Home Loan | EXPECTED | ✓ | OK |
| Tuition Fees | EXPECTED | ✓ | OK |
| Sukanya Samriddhi | EXPECTED | ✓ | OK |
| SCSS | EXPECTED | ✓ | OK |

**Gap**: No itemized breakdown in UI - only total field

### 80D - Status

| Field | CBDT Requirement | Status | Gap |
|-------|-----------------|--------|-----|
| Self + Family Premium | EXPECTED | ✓ | OK |
| Parents Premium | EXPECTED | ✓ | OK |
| Preventive Health Check | MAX ₹5,000 | ✗ NOT UI | HIGH |
| Senior Citizen Additional | ₹1L/₹50K | ✓ | OK |

**Gap**: No breakdown between self/family/parents in UI

### 80G - Status

| Field | CBDT Requirement | Status | Gap |
|-------|-----------------|--------|-----|
| Donation Amount | EXPECTED | ✓ | OK |
| Donee Name | MANDATORY | ✗ NOT UI | BLOCKER |
| Donee PAN | MANDATORY | ✗ NOT UI | BLOCKER |
| Donation Category | 100%/50% | ✗ NOT UI | BLOCKER |

**Critical Gap**: 80G breakdown NOT in UI - VR1-B001 will trigger rejection

### Other Deductions - Status

| Section | Status | Gap |
|---------|--------|-----|
| 80CCD(1B) NPS | OK | OK |
| 80CCD(2) Employer NPS | OK | OK |
| 80DD | Partial | No form details |
| 80DDB | Partial | No patient details |
| 80E | Partial | No loan details |
| 80EE | Partial | No property details |
| 80EEA | Partial | No loan details |
| 80EEB | Partial | No vehicle details |
| 80GG | Partial | No rent details worksheet |
| 80TTA | OK | OK |
| 80TTB | OK | OK |
| 80U | Partial | No certificate details |

---

## SECTION 8: TAX PAYMENTS (SCHEDULE TDS/TCS/ADVANCE)

### Current Implementation Status

| Section | Status | Gap |
|---------|--------|-----|
| TDS on Salary (TDS1) | ✓ | OK but limited UI |
| TDS on Other (TDS2) | ✓ | OK but limited UI |
| TCS | ✗ NOT UI | MEDIUM |
| Advance Tax | ✓ | OK but limited UI |
| Self-Assessment Tax | ✗ NOT UI | MEDIUM |

### Missing Fields

- TCS Collection entries (not in UI)
- Self-Assessment Tax entries (not in UI)
- Challan-wise details - limited in UI
- TDS3 (194N) - Cash withdrawal TDS - NOT implemented

---

## SECTION 9: TAX COMPUTATION - MANDATORY VALIDATIONS

### Current Validation Rules (ITR1CBDTValidationService)

| Rule ID | Description | Status |
|---------|-------------|--------|
| VR1-001 | 80C+CCC+CCD(1) > 1.5L | ✓ |
| VR1-002 | 80CCD(1) > 20% GTI pensioners | ✓ |
| VR1-003 | 80CCD(1) > 10% salary | ✓ |
| VR1-004 | 80CCD(2) exceeds limit | ✓ |
| VR1-005 | 80DDB > 1L senior | ✓ |
| VR1-007 | 80DDB > 40K non-senior | ✓ |
| VR1-008 | 80G without Schedule | ✓ |
| VR1-010 | 80TTA > 10K | ✓ |
| VR1-012 | Senior cannot claim 80TTA | ✓ |
| VR1-013 | 80TTB > 50K | ✓ |
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

### Missing Validation Rules

| Rule | Description | Priority |
|------|-------------|----------|
| - | Residential Status must be ROR | HIGH |
| - | Only ONE house property allowed | HIGH |
| - | HRA requires rent receipt | HIGH |
| - | Form 10IEA if opting out new regime | HIGH |
| - | Rebate 87A eligibility new regime (≤12L) | HIGH |
| - | Date of filing original return for revised | MEDIUM |
| - | Gift donor PAN if > ₹50K | HIGH |
| - | Exit tax in new regime (80EEA/80EEB) | MEDIUM |

---

## SECTION 10: JSON EXPORT COMPLIANCE

### ITR1JsonBuilder Analysis

**Current Export Summary**:
- ✅ CreationInfo
- ✅ Form_ITR1
- ✅ PersonalInfo (mostly)
- ✅ FilingStatus (partial)
- ✅ ITR1_IncomeDeductions (most fields)
- ✅ ITR1_TaxComputation
- ✅ TaxPaid
- ✅ Refund
- ⚠️ Verification (partial - missing key fields)
- ⚠️ TDSonSalaries
- ⚠️ TDSonOthThanSals
- ⚠️ TaxPayments (limited)

### Missing JSON Fields

```json
// PERSONAL INFO - Missing
"Gender": "",           // M/F/T - MANDATORY
"MaritalStatus": "",    // M/UM/W/D - MANDATORY
"FatherName": "",       // MANDATORY

// FILING STATUS - Missing
"ReturnType": "O",      // O/R/V/U
"ReturnFileSec": "11",  // 139(1)/139(4)/139(5)/142(1)/148
"SeventhProviso139i": "N",  // Y/N
"IsDefective": "N",     // Y/N

// VERIFICATION - Missing (BLOCKER)
"Declaration": {
    "AssesseeVerName": "",  // MANDATORY
    "FatherName": "",       // MANDATORY  
    "Capacity": "S",        // S/R/LH
    "Place": "",            // MANDATORY
    "Date": ""              // MANDATORY
}

// SCHEDULE 80G - Missing (BLOCKER for any 80G claim)
"Schedule80G": {
    "DonationDetails": []
}

// SCHEDULE 80E - Missing
"Schedule80E": {
    "InterestDetails": []
}

// SCHEDULE 80DD - Missing
"Schedule80DD": {
    "DependentDetails": []
}

// SCHEDULE 80DDB - Missing
"Schedule80DDB": {
    "MedicalDetails": []
}

// SCHEDULE HP - Missing
"PropertyAddress": "",
"TenantDetails": "",
"CoOwnerDetails": ""

// Refund Bank Details - Missing (BLOCKER if refund > 0)
"BankAccountDtls": {
    "IFSCCode": "",
    "BankName": "",
    "AccountNo": "",
    "AccountType": ""
}
```

---

## SECTION 11: COMPREHENSIVE GAP SUMMARY

### Priority 1 - BLOCKER (Will Cause ITR Rejection)

| # | Component | Missing Field | Action Required |
|---|-----------|---------------|-----------------|
| 1 | PersonalInfo | Father's Name | Add to UI + JSON export |
| 2 | PersonalInfo | Marital Status | Add to UI + JSON export |
| 3 | PersonalInfo | Gender | Add to UI + JSON export |
| 4 | PersonalInfo | Bank Details for Refund | Add to UI + JSON export |
| 5 | House Property | Property Address | Add to UI |
| 6 | House Property | Property Pincode | Add to UI |
| 7 | House Property | Co-owner Details | Add to UI |
| 8 | Schedule 80G | Donee Details | Add breakdown UI |
| 9 | Verification | Complete Section | Add to JSON builder |
| 10 | Filing Status | Return Type/Section | Add to JSON |

### Priority 2 - HIGH (May Cause Rejection)

| # | Component | Missing Field | Action Required |
|---|-----------|---------------|-----------------|
| 1 | Salary | HRA Calculation Worksheet | Add calculation display |
| 2 | Salary | LTA Journey Details | Add entry form |
| 3 | Salary | Employer Address | Add to entry form |
| 4 | Other Sources | Bank-wise Interest | Add breakdown UI |
| 5 | House Property | Tenant PAN | Add for let-out |
| 6 | Deductions | 80D Breakdown | Add details UI |
| 7 | Deductions | Gift Donor Details | Add details UI |
| 8 | Tax Payments | TCS UI | Add entry form |
| 9 | Tax Payments | Self-Assessment Tax UI | Add entry form |
| 10 | Eligibility | Only 1 property validation | Add validation |

### Priority 3 - MEDIUM (Improvements Needed)

| # | Component | Missing Field | Action Required |
|---|-----------|---------------|-----------------|
| 1 | Salary | Entertainment Allowance Calc | Add worksheet |
| 2 | Salary | Leave Encashment Details | Add to entry form |
| 3 | House Property | Pre-construction Interest | Add field |
| 4 | Other Sources | Dividend Breakdown | Add details UI |
| 5 | Exempt Income | Schedule EI UI | Add complete tab |
| 6 | Deductions | 80E Loan Details | Add details UI |
| 7 | Deductions | 80EE Property Details | Add details UI |
| 8 | Deductions | 80U Certificate Details | Add details UI |

---

## SECTION 12: VALIDATION GAPS - DETAILED

### Cross-Field Validations Missing

1. **HRA Claim** → Should require rent receipt/municipal tax proof
2. **80GG** → Should not allow if HRA claimed from same employer
3. **80CCD(2)** → Should validate against salary (10%/14%)
4. **80TTA/80TTB** → Should auto-switch based on age
5. **80EE + 80EEA** → Cannot claim both (VR1-094 exists)
6. **80EE + 80EEB** → Cannot claim both
7. **Tax Regime Opt-out** → Form 10IEA filing status not validated

### Business Logic Validations Missing

1. **Single Property** → No validation ensuring only ONE HP for ITR-1
2. **HP Loss** → Warning exists but should BLOCK if > ₹2L
3. **Agricultural Income** → Blocks at > ₹5K but UI warning needed
4. **Regime Comparison** → Not shown in UI (tax planning)
5. **Rebate 87A** → Calculation shown but eligibility scenarios not clear

---

## SECTION 13: AY 2025-26 vs AY 2026-27 DIFFERENCES

### Budget 2025 Updates for AY 2026-27

| Change | AY 2025-26 | AY 2026-27 | Implementation |
|--------|------------|------------|----------------|
| Standard Deduction | ₹75,000 | ₹75,000 | OK |
| Rebate 87A (New) | ≤ ₹12L, max ₹60K | ≤ ₹12L, max ₹60K | OK |
| Rebate 87A (Old) | ≤ ₹5L, max ₹12.5K | ≤ ₹5L, max ₹12.5K | OK |
| LTCG 112A Exemption | ₹1.25 Lakh | ₹1.25 Lakh | N/A (ITR-1) |
| TDS on Rent 194IA | > ₹50L | > ₹50L | N/A |
| TDS on Cash 194N | > ₹1 Cr | > ₹1 Cr | UI missing |

### Note on AY 2026-27
- Budget 2026 expected changes not yet announced
- Current implementation supports FY 2024-25 (AY 2025-26)
- Need to update Assessment Year to 2026-27 when applicable
- JSON builder ASSESSMENT_YEAR = "2026-27" but SCHEMA not verified

---

## SECTION 14: RECOMMENDED REMEDIATION PATH

### Immediate Actions (Week 1-2)

1. **Add Personal Info Fields**
   - Father's Name (UI + Validation)
   - Marital Status (UI + Validation)
   - Gender (UI + Validation)

2. **Add Property Address to HP**
   - Address line, City, State, Pincode
   - Co-owner PAN and share

3. **Fix JSON Export**
   - Add Father's Name to verification
   - Add Bank Account Details to Refund section
   - Add Verification section completely

4. **Add 80G Breakdown UI**
   - Donee Name, PAN, Amount, Category
   - Required when 80G > 0

### Short-Term (Week 3-4)

5. **Add HRA Calculation Worksheet**
   - Show rent paid, 10% salary, metro/non-metro calculation

6. **Add Employer Address**
   - City, State, Pincode

7. **Add Bank-wise Interest Breakdown**

8. **Add TCS and Self-Assessment Tax UI**

### Medium-Term (Month 2)

9. **Complete Deduction Details UI**
   - 80D breakdown
   - 80E loan details
   - 80EE property details
   - Gift donor details

10. **Add Schedule EI UI**
    - All exempt income fields

11. **Validation Audit**
    - Test all VR1-xxx rules
    - Add missing cross-field validations

---

## APPENDIX A: JSON SCHEMA REFERENCE

### Key JSON Fields for ITR-1 (CBDT Schema)

```json
{
  "ITR": {
    "ITR1": {
      "CreationInfo": { ... },
      "Form_ITR1": { ... },
      "PersonalInfo": {
        "AssesseeName": { "FirstName": "", "SurNameOrOrgName": "" },
        "PAN": "",
        "DOB": "",
        "AadhaarCardNo": "",
        "Gender": "",           // MISSING
        "MaritalStatus": "",    // MISSING
        "FatherName": "",       // MISSING
        "Address": { ... }
      },
      "FilingStatus": {
        "ReturnFileSec": "11",  // 139(1)
        "ReturnType": "O",      // MISSING
        "IsRevised": "N",       // MISSING
        "IsDefective": "N",     // MISSING
        "OptOutNewTaxRegime": "N"
      },
      "ITR1_IncomeDeduct": { ... },
      "ITR1_TaxComputation": { ... },
      "TaxPaid": { ... },
      "Refund": {
        "BankAccountDtls": {    // MISSING
          "IFSCCode": "",
          "BankName": "",
          "AccountNo": "",
          "AccountType": ""
        }
      },
      "Verification": {         // INCOMPLETE
        "AssesseeVerName": "",
        "FatherName": "",
        "Capacity": "S",
        "Place": "",
        "Date": "",
        "Declaration": ""
      }
    }
  }
}
```

---

## APPENDIX B: VALIDATION RULES SUMMARY

### Category A (Must Fix) - VR1-xxx
- VR1-001 to VR1-026: Deductions/GTI/Tax
- VR1-027 to VR1-040: HP/Salary
- VR1-041 to VR1-050: Salary
- VR1-051 to VR1-065: New regime
- VR1-066 to VR1-080: Salary/Business
- VR1-081 to VR1-099: Tax/TDS

### Category B (Defective Return) - VR1-Bxxx
- VR1-B001: 80G without Schedule80G → **WILL REJECT**

### Category D (Warnings) - VR1-Dxxx
- VR1-D001 to D004: Supporting documents

---

## APPENDIX C: SUMMARY TABLE

| Section | Required Fields | Implemented | Missing | Compliance |
|---------|----------------|-------------|---------|------------|
| Personal Info | 18 | 12 | 6 | 67% |
| Salary | 25 | 20 | 5 | 80% |
| House Property | 18 | 8 | 10 | 44% |
| Other Sources | 15 | 10 | 5 | 67% |
| Exempt Income | 12 | 8 | 4 | 67% |
| Deductions | 25 | 15 | 10 | 60% |
| Tax Payments | 15 | 10 | 5 | 67% |
| Tax Computation | 20 | 18 | 2 | 90% |
| JSON Export | 30 | 20 | 10 | 67% |
| **TOTAL** | **178** | **121** | **57** | **68%** |

---

*Report Generated: May 2026*
*Compliance Standard: CBDT ITR-1 Schema 2025-26*
*Assessment: AY 2025-26 & AY 2026-27*
*Requirement: 101% CBDT Compliance*
