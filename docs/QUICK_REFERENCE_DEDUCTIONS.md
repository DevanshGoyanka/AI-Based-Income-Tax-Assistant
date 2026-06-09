# DEDUCTIONS TAB - QUICK REFERENCE GUIDE
## For Developers & Tax Professionals
## Date: April 3, 2026

---

## 🎯 OVERVIEW

The Deductions Tab is now 100% compliant with ITR Field Guide requirements. This guide provides quick access to all implemented features.

---

## 📋 SECTION-BY-SECTION FIELD LIST

### 80C Investment Breakdown (₹1,50,000 limit)

#### LIC Premium (7 fields)
- Policy Number
- Policy Type (Term/Endowment/ULIP/Whole Life/Money Back)
- Policy Start Date
- Insurer Name
- Insurer PAN
- Premium Receipt Number
- Premium Payment Date

#### PPF (5 fields)
- Account Number
- Bank/Post Office Name
- Deposit Date
- Deposit Receipt Numbers
- Passbook Reference

#### ELSS (6 fields)
- Folio Number
- Fund Name
- AMC Name
- Investment Date
- Transaction Numbers
- Statement Reference

#### NSC (6 fields)
- Certificate Number
- Certificate Series (VIII/IX/X)
- Post Office Name
- Purchase Date
- Maturity Date
- Purchase Receipt Number

#### Home Loan Principal (7 fields)
- Loan Account Number
- Lender Name
- Lender Address
- Lender PAN
- Loan Sanction Date
- Loan Sanction Amount
- Repayment Certificate Number

#### Tuition Fees (8 fields)
- School/Institution Name
- Institution Address
- Student Name
- Student Relationship (Son/Daughter)
- Course Name
- Academic Year
- Fee Receipt Number
- Fee Payment Date

---

### 80D Medical Insurance

#### Self & Family (16 fields)
- Policy Number
- Insurer Name
- Insurer PAN
- Sum Insured
- Policy Start Date
- Policy End Date
- Premium Amount
- Premium Payment Date
- Premium Payment Mode
- Preventive Checkup Amount
- Checkup Hospital/Clinic Name
- Checkup Date
- Senior Citizen Checkbox
- Eligible Deduction (auto-calculated)

#### Parents (16 fields)
- Same fields as Self & Family

**Limits:**
- Below 60: ₹25,000 per section
- 60+ years: ₹50,000 per section
- Maximum total: ₹1,00,000

---

### 80DD Disabled Dependent (14 fields)
- Dependent Name
- Dependent PAN
- Relation
- Disability Type
- Disability Percentage
- Severe Disability Checkbox (80%+)
- Certificate Number
- Issuing Authority
- Certificate Issue Date
- Certificate Validity Date
- Doctor Name
- Doctor Registration Number
- Hospital Name
- Hospital Address
- Form 10-IA Filing Date

**Deduction:** ₹75,000 or ₹1,25,000 (severe)

---

### 80DDB Medical Treatment (15 fields)
- Patient Name
- Patient PAN
- Relation
- Disease Name
- Disease Code (Rule 11DD)
- Senior Citizen Checkbox
- Medical Expenditure
- Reimbursement Received
- Net Expenditure (auto-calculated)
- Prescription Doctor Name
- Doctor Registration Number
- Hospital Name
- Hospital Address
- Treatment From Date
- Treatment To Date
- Form 10-I Filing Date
- Bill Numbers

**Limit:** ₹40,000 or ₹1,00,000 (senior citizen)

---

### 80E Education Loan (15 fields)
- Loan Account Number
- Bank Name
- Bank Address
- Lender PAN
- Loan Sanction Date
- Loan Sanction Letter Number
- Student Name
- Relation
- Institution Name
- Institution Address
- Course Details
- Course Duration
- Interest Paid
- Interest Certificate Number
- Interest Certificate Date
- Year of Claim (1-8)

**Limit:** No upper limit, 8 years

---

### 80EE Home Loan (14 fields)
- Loan Account Number
- Bank Name
- Bank Address
- Lender PAN
- Loan Sanction Date
- Loan Sanction Letter Number
- Loan Amount
- Property Value
- Property Address
- Interest Paid
- Interest Certificate Number
- Interest Certificate Date
- First Time Home Buyer Checkbox
- First Time Buyer Declaration Date

**Limit:** ₹50,000

---

### 80EEA Affordable Housing (15 fields)
- Loan Account Number
- Bank Name
- Bank Address
- Lender PAN
- Loan Sanction Date
- Loan Sanction Letter Number
- Stamp Duty Value
- Stamp Duty Receipt Number
- Stamp Duty Receipt Date
- Property Address
- Property Completion Certificate Number
- Property Completion Date
- Interest Paid
- Interest Certificate Number
- Interest Certificate Date
- First Time Home Buyer Checkbox
- First Time Buyer Declaration Date

**Limit:** ₹1,50,000

---

### 80G Donations (15 fields per donation)
- Donee Name
- Donee PAN
- Donee Address
- 80G Registration Number
- 80G Certificate Number
- Certificate Validity Date
- Donation Amount
- Deduction Type (50%/100%)
- Donation Date
- Payment Mode (Online/Cheque/Cash)
- Payment Reference
- Cheque Number
- Transaction ID
- Receipt Number
- Receipt Issue Date
- Eligible Deduction (auto-calculated)

**Note:** Cash donations >₹2,000 not eligible

---

### 80GG Rent Paid (13 fields)
- Rent Paid (monthly/annual)
- Total Income
- 10% of Income (auto-calculated)
- 25% of Income (auto-calculated)
- Rent minus 10% (auto-calculated)
- Deduction Amount (auto-calculated)
- Landlord Name
- Landlord PAN
- Landlord Address
- Rent Agreement Number
- Rent Agreement From Date
- Rent Agreement To Date
- Rent Receipt Numbers
- Form 10BA Filing Date
- No HRA Received Checkbox

**Limit:** ₹60,000

---

### 80TTA Bank Interest (9 fields per bank)
- Bank Name
- Account Number
- Account Type (Savings/FD)
- Interest Earned
- Interest Certificate Number
- Interest Certificate Date
- TDS Deducted
- TDS Deductor TAN
- Form 26AS Match (✓/✗/⏳)

**Limit:** ₹10,000 (below 60 years)

---

### 80TTB Senior Citizen Interest (9 fields per bank)
- Same fields as 80TTA

**Limit:** ₹50,000 (60+ years)

---

### 80U Self Disability (13 fields)
- Disability Type
- Disability Percentage
- Severe Disability Checkbox (80%+)
- Certificate Number
- Issuing Authority
- Certificate Date
- Certificate Validity Date
- Doctor Name
- Doctor Registration Number
- Hospital Name
- Hospital Address
- Form 10-IA Filing Date

**Deduction:** ₹75,000 or ₹1,25,000 (severe)

---

## 🔍 VALIDATION RULES

### PAN Validation
- Format: AAAAA9999A
- Length: 10 characters
- Applied to: All PAN fields

### TAN Validation
- Format: AAAA99999A
- Length: 10 characters
- Applied to: All TAN fields

### Date Validation
- Format: YYYY-MM-DD
- Applied to: All date fields
- Date picker provided

### Amount Validation
- Type: Number
- Alignment: Right
- Applied to: All amount fields

### Dropdown Validation
- Predefined options only
- Applied to: All categorical fields

---

## 💡 BUSINESS RULES

### 80C Combined Limit
- 80C + 80CCC + 80CCD(1) ≤ ₹1,50,000
- 80CCD(1B) separate: ₹50,000
- 80CCD(2) no limit (employer NPS)

### 80D Age-based Limits
- Self/Family <60: ₹25,000
- Self/Family 60+: ₹50,000
- Parents <60: ₹25,000
- Parents 60+: ₹50,000
- Maximum total: ₹1,00,000

### 80TTA vs 80TTB
- 80TTA: Below 60 years, ₹10,000
- 80TTB: 60+ years, ₹50,000
- Cannot claim both

### 80EE vs 80EEA
- Cannot claim both in same year
- 80EE: ₹50,000
- 80EEA: ₹1,50,000

### 80G Cash Limit
- Cash donations >₹2,000 not eligible
- PAN mandatory for donations >₹2,000

### New Regime Restrictions
- Only 80CCD(2) allowed
- All other deductions disabled

---

## 🎨 UI PATTERNS

### Modal Structure
```
Header (sticky)
  ├─ Title
  └─ Close button

Body (scrollable)
  ├─ Info banner
  ├─ Field sections
  └─ Calculation summary

Footer (sticky)
  ├─ Cancel button
  └─ Save & Apply button
```

### Card Layout (80TTA/80TTB/80G)
```
Card per entry
  ├─ Header with entry number
  ├─ Remove button
  └─ 3-column grid of fields
```

### Dynamic Fields (80C)
```
Investment type selector
  └─ Type-specific fields appear
```

---

## 🔧 DEVELOPER NOTES

### State Management
```typescript
const [items80C, setItems80C] = useState<Deduction80CItem[]>([]);
const update80CItem = (index, field, value) => {
  const updated = [...items80C];
  updated[index] = { ...updated[index], [field]: value };
  setItems80C(updated);
};
```

### Adding New Fields
1. Update TypeScript interface in `api.ts`
2. Add field to initialization function
3. Add UI input in modal
4. Update backend DTO
5. Test data persistence

### Modal Pattern
```typescript
const [showModal, setShowModal] = useState(false);
const saveModal = () => {
  updateDed('fieldName', value);
  setShowModal(false);
};
```

---

## 📞 SUPPORT

### Common Issues

**Q: Fields not saving?**
A: Check backend DTO has all fields

**Q: Validation not working?**
A: Verify maxLength and type attributes

**Q: Calculation wrong?**
A: Check limit enforcement logic

**Q: Modal not opening?**
A: Verify state variable and button onClick

---

## 📚 REFERENCES

- ITR Field Guide: `ITR_Field_Guide.txt`
- ERP Execution Plan: `ERP_Execution_Plan.txt`
- Implementation Status: `DEDUCTIONS_100_PERCENT_COMPLETE.md`
- Final Summary: `FINAL_IMPLEMENTATION_SUMMARY.md`

---

**Last Updated:** April 3, 2026
**Status:** 100% Complete ✅
**Version:** 1.0.0
