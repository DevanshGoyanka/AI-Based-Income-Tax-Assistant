# ITR-1 CBDT Compliance Gaps Analysis

## Status: ITR-2 Components Removed ✓

All ITR-2 related files have been deleted:
- ✓ `ITR2CalculatorService.java` - Deleted
- ✓ `ITR2Result.java` - Deleted  
- ✓ `frontend/src/app/client/[id]/itr2/[year]/page.tsx` - Deleted
- ✓ `AutoITRFormSelector.java` - Cleaned up (removed requiresITR2 method)
- ✓ `prefill/success/page.tsx` - Simplified (removed ITR-2 routing)

---

## Critical Missing Fields & Features in Current ITR-1 Implementation

### Reference Implementation
- **OpenTax**: https://opentax.indiatax.ai/
- **GitHub**: https://github.com/nootus/OpenTax
- **CBDT ITR-1 Schema**: https://www.incometax.gov.in/iec/foportal/

---

## 1. PART A - GENERAL INFORMATION

### Missing Fields:
- ✗ **Father's Name** - Currently only stored in backend, not displayed/editable in UI
- ✗ **Flat/Door/Block No** - Address fields not broken down properly
- ✗ **Premises/Building/Village** - Missing
- ✗ **Road/Street/Post Office** - Missing
- ✗ **Area/Locality** - Missing
- ✗ **Town/City/District** - Missing
- ✗ **State** - Missing
- ✗ **Country** - Missing
- ✗ **PIN Code** - Missing
- ✗ **Employer Category** - Dropdown not properly mapped (CG/SG/PSU/PE/OTH/NA)
- ✗ **Return Filed u/s** - Missing (139(1)/139(4)/139(5)/142(1)/148/153A/153C)
- ✗ **Original/Revised Return** - Missing toggle
- ✗ **Receipt Number** (if revised) - Missing
- ✗ **Date of Filing Original Return** (if revised) - Missing
- ✗ **Bank Account Details** - Missing (for refund)
  - Bank Name
  - Account Number
  - IFSC Code
  - Account Type (Savings/Current)

### Current Implementation Issues:
- Age calculation exists but not displayed in UI
- Age category (Regular/Senior/Super Senior) calculated but not shown
- Residential status hardcoded to "resident" - should support NRI/RNOR

---

## 2. SCHEDULE SALARY

### Missing Fields:
- ✗ **Employer Details Table** - Should support multiple employers
  - Employer Name
  - TAN
  - Address
  - Salary from each employer
- ✗ **Allowances Exempt u/s 10** - Breakdown missing:
  - 10(5) - Leave Travel Allowance
  - 10(13A) - House Rent Allowance (with HRA calculation worksheet)
  - 10(14)(i) - Special Allowances
  - 10(14)(ii) - Other Allowances
  - 10(10) - Gratuity
  - 10(10AA) - Leave Encashment
  - 10(10C) - VRS Amount
- ✗ **HRA Calculation Worksheet** - Missing (required for 10(13A) claim)
  - Rent paid
  - 10% of salary
  - Excess of rent over 10% of salary
  - 40%/50% of salary (metro/non-metro)
  - Least of above = HRA exemption
- ✗ **Entertainment Allowance Calculation** - Missing for CG/SG employees
  - Least of: ₹5,000, 20% of salary, actual entertainment allowance

### Current Implementation Issues:
- Only single employer supported
- Allowances exempt shown as single field, not itemized
- No validation for HRA claim without rent receipts

---

## 3. SCHEDULE HOUSE PROPERTY

### Missing Fields:
- ✗ **Property Address** - Not captured
- ✗ **Co-owner Details** - Missing
  - Co-owner name
  - Co-owner PAN
  - Percentage of ownership
- ✗ **Tenant Details** (for let-out property) - Missing
  - Tenant name
  - Tenant PAN
- ✗ **Let-out Period** - Missing (if let-out for part of year)
- ✗ **Pre-construction Interest** - Missing
  - Interest paid during construction
  - Year of completion
  - 1/5th deduction calculation
- ✗ **HP Loss Set-off Worksheet** - Missing
  - Current year HP loss
  - Set-off against other heads (max ₹2L)
  - Balance loss to carry forward

### Current Implementation Issues:
- Property type dropdown exists but limited options
- HP loss carry forward logic exists in backend but not explained in UI
- No validation for self-occupied property with rent received

---

## 4. SCHEDULE OTHER SOURCES

### Missing Fields:
- ✗ **Interest Income Breakdown** - Not itemized:
  - Savings account interest (bank-wise)
  - Fixed deposit interest (bank-wise)
  - Income tax refund interest
  - Interest from company deposits
  - Interest from bonds/debentures
- ✗ **Dividend Income Breakdown** - Missing:
  - Domestic company dividends
  - Foreign company dividends
  - Mutual fund dividends
- ✗ **Family Pension Details** - Missing:
  - Pension from which organization
  - Deduction u/s 57(iia) calculation shown
- ✗ **Other Income Details** - Missing:
  - Gifts received (taxable)
  - Winnings from lottery/crossword puzzles
  - Income from subletting
  - Any other income

### Current Implementation Issues:
- All interest lumped together - no bank-wise breakdown
- No quarterly dividend breakdown (required by CBDT)
- Family pension deduction calculated but not explained

---

## 5. SCHEDULE EXEMPT INCOME

### Missing Fields:
- ✗ **Agricultural Income** - Not captured
- ✗ **Exempt Interest Income** - Not captured
  - PPF interest
  - Sukanya Samriddhi interest
  - Other exempt interest
- ✗ **LTCG u/s 112A** - Not captured
  - Sale consideration
  - Cost of acquisition
  - Exemption (max ₹1.25L)
- ✗ **Other Exempt Income** - Not captured
  - Exempt allowances
  - Exempt perquisites
  - Any other exempt income

### Current Implementation Issues:
- Schedule exists in backend but completely missing from UI
- No tab for exempt income in frontend

---

## 6. DEDUCTIONS UNDER CHAPTER VI-A

### Missing Fields & Worksheets:

#### 80C/CCC/CCD(1) - Missing Breakdown:
- ✗ Life Insurance Premium (with policy details)
- ✗ PPF Contribution
- ✗ ELSS Mutual Funds
- ✗ NSC Interest
- ✗ Principal repayment of home loan
- ✗ Tuition fees (2 children max)
- ✗ Sukanya Samriddhi Account
- ✗ 5-year bank FD
- ✗ Senior Citizen Savings Scheme
- ✗ NPS Tier-I (80CCD(1))

#### 80D - Missing Worksheet:
- ✗ Self & family premium
- ✗ Parents premium
- ✗ Preventive health checkup (₹5K limit)
- ✗ Senior citizen additional limit
- ✗ Total 80D calculation

#### 80DD - Missing Details:
- ✗ Dependent details (name, relation, disability %)
- ✗ Normal disability (₹75K) vs severe (₹1.25L)

#### 80DDB - Missing Details:
- ✗ Patient details (name, relation, disease)
- ✗ Medical expenditure
- ✗ Reimbursement received
- ✗ Net deduction

#### 80E - Missing Details:
- ✗ Loan details (bank, loan account number)
- ✗ Interest paid
- ✗ Year of loan taken

#### 80EE/80EEA - Missing Details:
- ✗ Loan sanction date
- ✗ Property value
- ✗ Loan amount
- ✗ Interest paid
- ✗ Eligibility check

#### 80G - Missing Breakdown:
- ✗ Donation details (donee name, PAN, amount)
- ✗ 100% deduction donations
- ✗ 50% deduction donations
- ✗ Qualifying limit calculation

#### 80GG - Missing Worksheet:
- ✗ Rent paid
- ✗ Total income
- ✗ 25% of total income
- ✗ Rent minus 10% of total income
- ✗ Least of above (max ₹60K)

#### 80TTA/80TTB - Missing:
- ✗ Bank-wise interest breakdown
- ✗ Age-based eligibility (TTA <60, TTB ≥60)

#### 80U - Missing Details:
- ✗ Disability certificate details
- ✗ Normal (₹75K) vs severe (₹1.25L)

### Current Implementation Issues:
- All deductions shown as single input fields
- No supporting worksheets or calculations shown
- No validation for mutually exclusive deductions (80EE vs 80EEA)
- No validation for limits based on GTI

---

## 7. SCHEDULE TAXES PAID

### Current Implementation: ✓ GOOD
- TDS on Salary entries with TAN, employer name, certificate number ✓
- TDS other than Salary entries ✓
- Advance tax entries with BSR, date, challan number ✓

### Missing Fields:
- ✗ **TDS3 (194N)** - TDS on cash withdrawal - field exists but no UI
- ✗ **TCS** - Tax Collected at Source - field exists but no UI
- ✗ **Self-Assessment Tax** - Separate from advance tax - field exists but no UI

### Improvements Needed:
- Add TDS3 entry table (similar to TDS1/TDS2)
- Add TCS entry table
- Add self-assessment tax entry table
- Show total taxes paid summary prominently

---

## 8. TAX COMPUTATION

### Missing Fields:
- ✗ **Regime Comparison Table** - Missing
  - Side-by-side comparison of old vs new regime
  - Tax liability in each regime
  - Recommendation based on lower tax
- ✗ **Marginal Relief** - Missing
  - For surcharge (if applicable)
- ✗ **Rebate 87A Calculation** - Not shown
  - Eligibility check (income ≤ ₹12L new, ≤ ₹5L old)
  - Rebate amount (max ₹60K new, ₹12.5K old)
- ✗ **Interest Calculation Breakdown** - Partially missing
  - 234A: Delay in filing return - ✓ Calculated
  - 234B: Shortfall in advance tax - ✓ Calculated
  - 234C: Deferment of advance tax - ✓ Calculated
  - 234F: Late filing fee - ✓ Calculated
  - **Missing**: Detailed month-wise interest calculation shown to user

### Current Implementation Issues:
- Interest fields added but not prominently displayed
- No explanation of why interest is charged
- No breakdown of interest calculation shown

---

## 9. VERIFICATION & FILING

### Missing Features:
- ✗ **Verification Method** - Missing
  - EVC (Electronic Verification Code)
  - Aadhaar OTP
  - Net Banking
  - Bank Account
  - Demat Account
- ✗ **Place of Filing** - Missing
- ✗ **Date of Filing** - Missing
- ✗ **Capacity** - Missing (Self/Representative/Legal Heir)
- ✗ **Representative Details** (if applicable) - Missing
  - Name
  - PAN
  - Capacity

---

## 10. VALIDATION RULES

### Missing CBDT Validation Rules:

#### Critical Validations:
- ✗ **Rule 59**: Gross salary cannot be negative
- ✗ **Rule 63**: Allowances exempt cannot exceed gross salary
- ✗ **Rule 112/224**: Standard deduction limits (₹50K old, ₹75K new)
- ✗ **Rule 164**: Entertainment allowance not allowed in new regime
- ✗ **Rule 169**: Professional tax not allowed in new regime
- ✗ **Rule 44/250**: Self-occupied property annual value must be 0
- ✗ **Rule 48**: Self-occupied interest max ₹2L
- ✗ **Rule 163/263**: Self-occupied interest not allowed in new regime
- ✗ **Rule 71(3A)**: HP loss set-off max ₹2L per year
- ✗ **Rule 115**: 80CCD(1B) max ₹50K
- ✗ **Rule 122**: 80EE max ₹50K
- ✗ **Rule 123**: 80EEA max ₹1.5L
- ✗ **Rule 124**: Only one of 80EE/80EEA allowed
- ✗ **Rule 125**: 80EEB max ₹1.5L
- ✗ **Rule 114**: 80GG max ₹60K
- ✗ **Rule 226**: LTCG 112A exemption max ₹1.25L

#### Cross-field Validations:
- ✗ HRA claim requires rent paid > 10% of salary
- ✗ 80GG not allowed if HRA claimed
- ✗ 80CCD(2) max 14% of salary (CG/SG) or 10% (others)
- ✗ 80TTA not allowed if age ≥ 60 (use 80TTB instead)
- ✗ 80TTB not allowed if age < 60 (use 80TTA instead)
- ✗ Total deductions cannot exceed GTI
- ✗ Refund/balance tax < ₹10 should be ignored (CBDT practice)

---

## 11. REPORTS & DOWNLOADS

### Current Implementation:
- ✓ JSON download (ITR-1 e-filing format)
- ✓ Excel download (computation report)
- ✓ PDF download (summary report)

### Missing Features:
- ✗ **Form 16 Upload** - Allow user to upload Form 16 for auto-population
- ✗ **26AS Integration** - Fetch TDS details from 26AS
- ✗ **AIS Integration** - Fetch Annual Information Statement
- ✗ **Pre-validation** - Validate before final submission
- ✗ **Acknowledgement** - Generate ITR-V acknowledgement
- ✗ **Computation Sheet** - Detailed tax computation in PDF

---

## 12. USER EXPERIENCE ISSUES

### Navigation & Flow:
- ✗ No progress indicator showing completion %
- ✗ No "Save & Continue Later" prominent button
- ✗ No "Previous/Next" buttons for tab navigation
- ✗ No summary page before final submission

### Data Entry:
- ✗ No auto-save (currently manual save only)
- ✗ No inline help text for complex fields
- ✗ No tooltips explaining CBDT rules
- ✗ No examples for each field
- ✗ No currency formatting in input fields (₹ symbol)

### Validation Feedback:
- ✗ Validation errors shown only after compute
- ✗ No real-time validation as user types
- ✗ No field-level error messages
- ✗ No warning for unusual values (e.g., very high deductions)

---

## PRIORITY FIXES (Phase 1 - Critical for CBDT Compliance)

### High Priority:
1. **Part A - Complete Address Fields** - Required for e-filing
2. **Part A - Bank Account Details** - Required for refund
3. **Schedule Salary - Allowances Breakdown** - Required for 10(13A) HRA
4. **Schedule HP - Property Address** - Required field
5. **Schedule Exempt Income - Add UI Tab** - Currently missing entirely
6. **Deductions - 80C/80D Breakdown** - Required for validation
7. **Taxes Paid - TDS3/TCS/Self-Assessment UI** - Fields exist, need UI
8. **Validation Rules - Implement all CBDT rules** - Critical for accuracy
9. **Verification Section** - Required for e-filing

### Medium Priority:
10. **HRA Calculation Worksheet** - Helpful for users
11. **80G Donation Breakdown** - Required for audit trail
12. **Regime Comparison Table** - User-friendly feature
13. **Interest Calculation Breakdown** - Transparency
14. **Progress Indicator** - UX improvement

### Low Priority:
15. **Form 16 Upload** - Nice to have
16. **26AS Integration** - Requires API access
17. **Auto-save** - UX improvement
18. **Inline Help** - UX improvement

---

## NEXT STEPS

1. **Study OpenTax Implementation**
   - Clone repo: `git clone https://github.com/nootus/OpenTax`
   - Analyze their ITR-1 form structure
   - Compare field-by-field with CBDT schema

2. **Download CBDT ITR-1 Schema**
   - Get latest JSON schema from incometax.gov.in
   - Map all required fields to our data model

3. **Create Detailed Implementation Plan**
   - Break down into sprints
   - Prioritize based on CBDT compliance requirements
   - Estimate effort for each feature

4. **Implement Phase 1 (Critical Fields)**
   - Start with Part A address fields
   - Add bank account details
   - Complete Schedule Exempt Income UI
   - Implement all validation rules

5. **Test with Real Data**
   - Use Mahendra's prefill data
   - Verify all calculations match CBDT rules
   - Compare with OpenTax calculations

---

## REFERENCE LINKS

- **CBDT ITR-1 Form**: https://www.incometax.gov.in/iec/foportal/help/individual/return-applicable-1
- **ITR-1 Instructions**: https://www.incometax.gov.in/iec/foportal/help/individual/instructions-for-itr-1
- **OpenTax GitHub**: https://github.com/nootus/OpenTax
- **OpenTax Live**: https://opentax.indiatax.ai/
- **Income Tax Act Sections**: https://www.incometax.gov.in/iec/foportal/help/individual/income-tax-act

---

**Document Created**: March 25, 2026
**Status**: ITR-2 removed, ITR-1 gaps identified
**Next Action**: Study OpenTax implementation and create detailed fix plan
