# Deductions Tab - Complete Implementation Guide

## Overview
This document provides the complete implementation pattern for all deduction breakdowns in Priority Fix #5.

## Backend Status
✅ All detailed breakdown classes added to `Itr1FormData.java`:
- `Deduction80CItem` - 80C investment breakdown
- `Deduction80DWorksheet` - Medical insurance worksheet
- `Deduction80DDDetails` - Disability of dependent
- `Deduction80DDBDetails` - Medical treatment
- `Deduction80EDetails` - Education loan
- `Deduction80EEDetails` - Home loan (first time buyers)
- `Deduction80EEADetails` - Affordable housing
- `Deduction80GDonation` - Donation breakdown
- `Deduction80GGWorksheet` - Rent paid worksheet
- `BankInterestEntry` - Bank-wise interest (80TTA/80TTB)
- `Deduction80UDetails` - Person with disability

## Frontend Status
✅ TypeScript interfaces added to `api.ts`
🚧 DeductionsTab.tsx - In progress

## Implementation Pattern

### 1. 80C Breakdown Modal (PRIORITY)
**Fields Required:**
- Investment Type dropdown (LIC, PPF, ELSS, NSC, Home Loan Principal, Tuition Fees, Sukanya Samriddhi, 5-yr FD, SCSS)
- Description text field
- Amount number field
- Add/Delete rows
- Auto-calculate total (max ₹1,50,000)

**Validation:**
- Total cannot exceed ₹1,50,000
- Each amount must be > 0

### 2. 80D Medical Insurance Worksheet (PRIORITY)
**Fields Required:**
- Self & Family Section:
  - Premium paid
  - Preventive health checkup
  - Is senior citizen checkbox
  - Computed deduction (max ₹25K or ₹50K)
  
- Parents Section:
  - Premium paid
  - Preventive health checkup
  - Are senior citizens checkbox
  - Computed deduction (max ₹25K or ₹50K)
  
- Total deduction (max ₹1,00,000)

**Calculation Logic:**
```
selfLimit = selfIsSeniorCitizen ? 50000 : 25000
selfDeduction = min(selfPremium + selfCheckup, selfLimit)

parentsLimit = parentsAreSeniorCitizens ? 50000 : 25000
parentsDeduction = min(parentsPremium + parentsCheckup, parentsLimit)

totalDeduction = selfDeduction + parentsDeduction
```

### 3. 80G Donations Breakdown (PRIORITY)
**Fields Required:**
- Donee name
- Donee PAN
- Donee address
- Donation amount
- Deduction type (100% or 50%)
- Payment mode (Cash/Cheque/Online)
- Receipt number
- Donation date
- Add/Delete rows
- Auto-calculate eligible deduction

**Calculation Logic:**
```
if (deductionType === "100%") {
  eligibleDeduction = donationAmount
} else {
  eligibleDeduction = donationAmount * 0.5
}
```

**Validation:**
- Cash donations > ₹2,000 not allowed
- PAN mandatory for donations > ₹2,000

### 4. 80DD Disability of Dependent
**Fields Required:**
- Dependent name
- Dependent PAN
- Relation dropdown
- Disability type
- Disability percentage
- Is severe disability checkbox (>= 80%)
- Certificate number
- Issuing authority
- Auto-calculate deduction (₹75,000 or ₹1,25,000)

### 5. 80DDB Medical Treatment
**Fields Required:**
- Patient name
- Patient PAN
- Relation
- Disease name
- Is senior citizen checkbox
- Medical expenditure
- Reimbursement received
- Net expenditure (auto-calculated)
- Deduction amount (max ₹40K or ₹1L for senior citizen)

### 6. 80E Education Loan
**Fields Required:**
- Loan account number
- Bank name
- Loan sanction date
- Student name
- Relation
- Course details
- Interest paid
- Year of claim (1-8)

**Validation:**
- Can claim for max 8 years
- No upper limit on deduction

### 7. 80EE Home Loan (First Time Buyers)
**Fields Required:**
- Loan account number
- Bank name
- Loan sanction date (must be 01-Apr-2016 to 31-Mar-2017)
- Loan amount (max ₹35L)
- Property value (max ₹50L)
- Interest paid
- Is first time home buyer checkbox
- Deduction amount (max ₹50,000)

**Validation:**
- Loan sanction date must be in specified range
- Loan amount <= ₹35L
- Property value <= ₹50L
- Cannot claim both 80EE and 80EEA

### 8. 80EEA Affordable Housing
**Fields Required:**
- Loan account number
- Bank name
- Loan sanction date (must be 01-Apr-2019 to 31-Mar-2022)
- Stamp duty value (max ₹45L)
- Interest paid
- Is first time home buyer checkbox
- Deduction amount (max ₹1,50,000)

**Validation:**
- Loan sanction date must be in specified range
- Stamp duty value <= ₹45L
- Cannot claim both 80EE and 80EEA

### 9. 80GG Rent Paid Worksheet
**Fields Required:**
- Rent paid
- Total income (auto-filled from GTI)
- 10% of income (auto-calculated)
- 25% of income (auto-calculated)
- Rent minus 10% of income (auto-calculated)
- Deduction = least of (rent - 10% income, 25% income, ₹60,000)
- Landlord name
- Landlord PAN
- Landlord address

**Validation:**
- Cannot claim if HRA received
- Max ₹60,000

### 10. 80TTA/80TTB Bank Interest Breakdown
**Fields Required:**
- Bank name
- Account number
- Account type (Savings/FD)
- Interest earned
- Add/Delete rows
- Auto-calculate total

**Validation:**
- 80TTA: Age < 60, max ₹10,000
- 80TTB: Age >= 60, max ₹50,000
- Cannot claim both

### 11. 80U Person with Disability
**Fields Required:**
- Disability type
- Disability percentage
- Is severe disability checkbox (>= 80%)
- Certificate number
- Issuing authority
- Certificate date
- Deduction amount (₹75,000 or ₹1,25,000)

## UI/UX Guidelines

### Modal Structure
```tsx
{showModal && (
  <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
    <div className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto">
      <div className="bg-primary-600 text-white p-4 flex justify-between items-center sticky top-0">
        <h3 className="text-lg font-bold">Modal Title</h3>
        <button onClick={() => setShowModal(false)} className="text-white text-2xl">&times;</button>
      </div>
      <div className="p-6">
        {/* Modal content */}
      </div>
      <div className="border-t p-4 flex justify-end gap-2 sticky bottom-0 bg-white">
        <button onClick={() => setShowModal(false)} className="px-4 py-2 border rounded">Cancel</button>
        <button onClick={handleSave} className="px-4 py-2 bg-primary-600 text-white rounded">Save</button>
      </div>
    </div>
  </div>
)}
```

### Table Structure for Multi-Entry Modals
```tsx
<table className="w-full text-sm">
  <thead>
    <tr className="bg-gray-50">
      <th className="text-left p-2">Column 1</th>
      <th className="text-left p-2">Column 2</th>
      <th className="text-right p-2">Amount</th>
      <th className="w-10"></th>
    </tr>
  </thead>
  <tbody>
    {items.map((item, i) => (
      <tr key={i} className="border-t">
        <td className="p-2"><input /></td>
        <td className="p-2"><input /></td>
        <td className="p-2"><input type="number" className="text-right" /></td>
        <td className="p-2">
          <button onClick={() => removeItem(i)} className="text-red-500">×</button>
        </td>
      </tr>
    ))}
  </tbody>
</table>
<button onClick={addItem} className="mt-2 text-primary-600">+ Add Row</button>
```

## Next Steps

1. Complete DeductionsTab.tsx with all modals
2. Add validation logic for each deduction
3. Test with real data
4. Add help text and examples
5. Implement auto-save functionality

## Testing Checklist

- [ ] 80C total cannot exceed ₹1.5L
- [ ] 80D calculation correct for all combinations
- [ ] 80G cash limit enforced
- [ ] 80EE/80EEA mutual exclusivity
- [ ] 80TTA/80TTB age-based validation
- [ ] All modals save data correctly
- [ ] Data persists after page refresh
- [ ] Compute tax reflects deduction changes

