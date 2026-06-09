# PHASE 2 IMPLEMENTATION PLAN
## Fixing All Discrepancies (Except API Integrations)

**Start Date**: March 26, 2026
**API Production Access**: Expected in 2 weeks
**Goal**: Fix all 90 discrepancies, keep API integration ready for production

---

## IMPLEMENTATION PRIORITY

### IMMEDIATE (P0) - Days 1-3
**Goal**: Fix user-reported issues and critical UI/UX problems

1. ✅ Add country code dropdown for mobile number
2. ✅ Add policy details to 80D modal (policy number, insurer, dates)
3. ✅ Add policy details to all other deduction modals
4. ✅ Make address fields more prominent and mandatory
5. ✅ Remove fake "verified" status, add "Not Verified" labels
6. ✅ Add document reference fields (not upload, just references)
7. ✅ Add IFSC validation (client-side with known IFSC list)
8. ✅ Add PIN code validation (client-side with known PIN list)

### HIGH PRIORITY (P1) - Days 4-7
**Goal**: Add all missing fields and details

9. ✅ Form 16 reference fields (not upload yet)
10. ✅ Employer TAN validation (format only)
11. ✅ Multiple employers - add employment period fields
12. ✅ Allowances breakdown - add detailed fields
13. ✅ Home loan certificate fields
14. ✅ Rent agreement fields
15. ✅ Bank interest certificate fields
16. ✅ Dividend detailed fields
17. ✅ Family pension detailed fields
18. ✅ Agricultural land detailed fields
19. ✅ LTCG transaction detailed fields
20. ✅ All 80C investment proof reference fields
21. ✅ TDS certificate detailed fields
22. ✅ Advance tax challan detailed fields

### MEDIUM PRIORITY (P2) - Days 8-12
**Goal**: Add document management and validation

23. ✅ Document upload system (local storage for now)
24. ✅ Document categorization
25. ✅ Document checklist
26. ✅ File size and format validation
27. ✅ Document viewer
28. ✅ Enhanced validation rules
29. ✅ Cross-field validation
30. ✅ Regime-specific validation
31. ✅ Interest calculation (234A, 234B, 234C, 234F)
32. ✅ Marginal relief calculation
33. ✅ Relief u/s 89 worksheet
34. ✅ Improved computation report

### LOW PRIORITY (P3) - Days 13-15
**Goal**: UX improvements and polish

35. ✅ Auto-save functionality
36. ✅ Enhanced progress indicator
37. ✅ Detailed help text and tooltips
38. ✅ Improved prefill validation
39. ✅ Enhanced PDF generation
40. ✅ Enhanced Excel export
41. ✅ Previous year ITR import (manual JSON)
42. ✅ Revised return workflow
43. ✅ Belated return workflow

### API INTEGRATION (HOLD) - After Production Access
**Goal**: Integrate with ITD APIs once production is live

44. ⏸️ PAN verification API
45. ⏸️ Aadhaar verification API
46. ⏸️ Form 26AS fetch API
47. ⏸️ AIS fetch API
48. ⏸️ Prefill JSON fetch API
49. ⏸️ ITR submission API
50. ⏸️ E-verification APIs
51. ⏸️ ITR-V generation API
52. ⏸️ Status tracking API

---

## DETAILED TASK BREAKDOWN

### Day 1: Critical UI Fixes
- [ ] Add country code dropdown to mobile field (GeneralInfoTab)
- [ ] Update backend PartA to include countryCode and mobile separately
- [ ] Add policy details modal to 80D (policy number, insurer, dates, payment mode)
- [ ] Update backend Deduction80DWorksheet with policy fields
- [ ] Add "Not Verified" badges to PAN/Aadhaar/Email/Mobile
- [ ] Remove fake checkmarks from PreSubmissionChecklist

### Day 2: Deduction Details
- [ ] Add policy/certificate fields to 80DD modal
- [ ] Add medical document fields to 80DDB modal
- [ ] Add loan document fields to 80E modal
- [ ] Add eligibility fields to 80EE modal
- [ ] Add eligibility fields to 80EEA modal
- [ ] Add 80G certificate fields to donation modal
- [ ] Add rent receipt fields to 80GG modal
- [ ] Add certificate fields to 80U modal
- [ ] Update all backend DTOs

### Day 3: Address & Bank Validation
- [ ] Add address proof document type field
- [ ] Add document number field
- [ ] Create IFSC validation function (client-side)
- [ ] Create PIN code validation function (client-side)
- [ ] Add bank account holder name field
- [ ] Add account opening date field
- [ ] Update backend DTOs

### Day 4: Salary Schedule Details
- [ ] Add Form 16 reference fields (Part A, Part B numbers)
- [ ] Add TAN validation function
- [ ] Add employment period fields to employers
- [ ] Add reason for change field
- [ ] Add overlap detection
- [ ] Add TDS certificate number per employer
- [ ] Update backend EmployerDetail DTO

### Day 5: Allowances & Perquisites
- [ ] Add HRA calculation worksheet (rent, landlord details)
- [ ] Add LTA details (travel dates, destination, tickets)
- [ ] Add special allowance nature field
- [ ] Add perquisites breakdown modal
- [ ] Add perquisite type and valuation fields
- [ ] Update backend DTOs

### Day 6: House Property Details
- [ ] Add property document fields (type, number, date)
- [ ] Add home loan certificate fields (account, lender, certificate)
- [ ] Add principal repayment field (for 80C)
- [ ] Add loan purpose field
- [ ] Add rent agreement fields (dates, registration)
- [ ] Add municipal tax receipt fields
- [ ] Add co-owner consent fields
- [ ] Update backend ScheduleHouseProperty DTO

### Day 7: Other Sources Details
- [ ] Add bank details to interest fields (name, account, certificate)
- [ ] Add TDS on interest field
- [ ] Add dividend company details (name, PAN, type, date)
- [ ] Add TDS on dividend field
- [ ] Add family pension authority details (name, PPO number)
- [ ] Add Form 16 reference for pension
- [ ] Update backend ScheduleOtherSources DTO

### Day 8: Exempt Income Details
- [ ] Add agricultural land details (location, survey number, area, crop)
- [ ] Add agricultural income certificate field
- [ ] Add LTCG transaction details (security, ISIN, quantity, dates)
- [ ] Add broker details (name, PAN)
- [ ] Add contract note references
- [ ] Add STT payment proof reference
- [ ] Add demat statement reference
- [ ] Update backend ScheduleExemptIncome DTO

### Day 9: 80C Investment Proofs
- [ ] Add LIC policy fields (number, premium receipt, policy doc)
- [ ] Add PPF account fields (number, deposit receipts, passbook)
- [ ] Add ELSS fields (folio, statement, transactions)
- [ ] Add NSC fields (certificate number, purchase receipt)
- [ ] Add home loan principal fields (statement, schedule)
- [ ] Add tuition fees fields (school, receipt, structure)
- [ ] Update backend Deduction80CItem DTO

### Day 10: Taxes Paid Details
- [ ] Add TDS certificate fields (issue date, filing date, ack number)
- [ ] Add Form 16/16A reference fields
- [ ] Add correction statement tracking
- [ ] Add advance tax CIN field
- [ ] Add payment confirmation reference
- [ ] Add self-assessment tax CIN field
- [ ] Add TCS detailed fields (seller, rate, section, transaction)
- [ ] Update backend ScheduleTaxesPaid DTOs

### Day 11: Document Management System
- [ ] Create DocumentUpload component
- [ ] Create document storage service (local for now)
- [ ] Add file upload functionality
- [ ] Add file size validation (max 5MB)
- [ ] Add format validation (PDF, JPG, PNG)
- [ ] Add document categorization
- [ ] Create document list view
- [ ] Add document viewer
- [ ] Add delete/replace functionality
- [ ] Create backend document entity and repository

### Day 12: Document Checklist
- [ ] Create DocumentChecklist component
- [ ] Define required documents based on income/deductions
- [ ] Add upload status tracking
- [ ] Add mandatory vs optional marking
- [ ] Add document expiry tracking
- [ ] Add reminder for missing documents
- [ ] Integrate with main ITR form

### Day 13: Enhanced Validation
- [ ] Add cross-field validation rules
- [ ] Add regime-specific validation
- [ ] Add age-based validation
- [ ] Add residential status validation
- [ ] Add format validations (PAN, Aadhaar, TAN, IFSC)
- [ ] Add amount limit validations
- [ ] Add date range validations
- [ ] Update ValidationPanel component

### Day 14: Interest & Relief Calculations
- [ ] Implement 234A calculation (month-wise)
- [ ] Implement 234B calculation (detailed)
- [ ] Implement 234C calculation (installment-wise)
- [ ] Implement 234F calculation
- [ ] Implement marginal relief calculation
- [ ] Create Relief u/s 89 worksheet component
- [ ] Add arrears of salary fields
- [ ] Add year-wise tax calculation
- [ ] Update backend TaxComputation DTO

### Day 15: UX Improvements
- [ ] Implement auto-save (every 2 minutes)
- [ ] Add save indicator
- [ ] Add restore unsaved changes
- [ ] Enhance progress indicator
- [ ] Add completion percentage per section
- [ ] Add detailed help text to all fields
- [ ] Add examples for complex fields
- [ ] Add CBDT rule references
- [ ] Add tooltips with explanations

### Day 16: Reports & Export
- [ ] Enhance PDF generation (ITR-1 format)
- [ ] Add computation sheet to PDF
- [ ] Add supporting schedules to PDF
- [ ] Add watermark for draft
- [ ] Enhance Excel export (multiple sheets)
- [ ] Add formulas to Excel
- [ ] Add charts to Excel
- [ ] Create detailed computation report

### Day 17: Previous Year & Workflows
- [ ] Create previous year ITR import (manual JSON)
- [ ] Add loss carry forward import
- [ ] Add pre-fill recurring data
- [ ] Add comparison with previous year
- [ ] Enhance revised return workflow
- [ ] Add change highlighting
- [ ] Add reason for revision
- [ ] Enhance belated return workflow
- [ ] Add late fee calculation

### Day 18: Testing & Bug Fixes
- [ ] Test all new fields
- [ ] Test all validations
- [ ] Test document upload
- [ ] Test calculations
- [ ] Test PDF/Excel generation
- [ ] Fix bugs
- [ ] Cross-browser testing
- [ ] Mobile responsiveness testing

### Day 19-20: API Integration Preparation
- [ ] Create API service layer
- [ ] Define API endpoints
- [ ] Create mock responses for testing
- [ ] Add API error handling
- [ ] Add retry logic
- [ ] Add loading states
- [ ] Document API integration points
- [ ] Prepare for production API switch

---

## NOTES

1. **Document Upload**: Using local storage for now, will move to cloud storage later
2. **Validation**: Client-side validation with known data (IFSC, PIN codes)
3. **API Integration**: All API calls will be mocked until production access
4. **Testing**: Manual testing for now, automated tests later
5. **Security**: Basic security, will enhance after API integration

---

## SUCCESS CRITERIA

- ✅ All 90 discrepancies addressed (except API integrations)
- ✅ All fields have proper labels and help text
- ✅ All modals have complete information
- ✅ Document management system working
- ✅ Enhanced validation working
- ✅ Calculations accurate
- ✅ Reports comprehensive
- ✅ Ready for API integration

---

## NEXT STEPS AFTER PHASE 2

1. Wait for ITD production API access
2. Integrate all APIs
3. Test with real data
4. Security audit
5. Performance testing
6. CA review
7. Soft launch
8. Production launch
