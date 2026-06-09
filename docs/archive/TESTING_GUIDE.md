# ITR Filing Website - Testing Guide

## Server Status ✅

Both servers are now running and ready for testing:

- **Backend (Spring Boot)**: http://localhost:8080
- **Frontend (Next.js)**: http://localhost:3000

## What's Been Completed

### 1. Currency Input Formatting ✅
All currency input fields across all ITR forms now use Indian number formatting:
- Format: 10,00,000 (Indian comma style)
- Files updated: SalaryTab.tsx, HousePropertyTab.tsx, DeductionsTab.tsx, ExemptIncomeTab.tsx
- Total inputs converted: 49 currency fields

### 2. Tax Calculation Engine ✅
Comprehensive tax calculations are implemented in both frontend and backend:

**Frontend** (`src/lib/tax-calculator.ts`):
- AY 2025-26 and AY 2026-27 tax slabs
- Old and New tax regime support
- Section 87A rebate calculations
- Surcharge and cess calculations
- Interest calculations (234A, 234B, 234C)
- Late filing fee (234F)

**Backend** (Java services):
- `ITR1CalculatorService.java` - Complete ITR-1 calculations
- `ITR2CalculatorService.java` - ITR-2 with capital gains
- `ITR3CalculatorService.java` - ITR-3 with business income
- `ITR4CalculatorService.java` - ITR-4 presumptive taxation
- `TaxSlabCalculator.java` - Tax slab computations
- `InterestCalculator.java` - Interest calculations
- `ITR1ValidationService.java` - CBDT validation rules

### 3. Validation System ✅
- Real-time validation panel in frontend
- CBDT validation rules implemented
- Category-based validation (Critical, Defect, Info)
- Backend validation service with comprehensive rules

## How to Test

### 1. Access the Application
Open your browser and navigate to: http://localhost:3000

### 2. Test ITR-1 Form
1. Register/Login to the application
2. Create a new ITR-1 filing
3. Fill in the General Info tab
4. Test Salary Income with Indian number formatting (e.g., type "1000000" and see it format as "10,00,000")
5. Add House Property details
6. Add Deductions (80C, 80D, etc.)
7. View Tax Computation tab to see calculations
8. Check Validation Panel for any errors

### 3. Test ITR-2 Form
Navigate to ITR-2 components in `src/components/itr2/`:
- Test Capital Gains calculations
- Test House Property with multiple properties
- Verify deductions and tax computation

### 4. Test ITR-3 Form
Navigate to ITR-3 components in `src/components/itr3/`:
- Test Business/Profession income
- Test Profit & Loss statement
- Test Depreciation calculations
- Verify Balance Sheet

### 5. Test ITR-4 Form
Navigate to ITR-4 components in `src/components/itr4/`:
- Test Presumptive taxation (44AD, 44ADA, 44AE)
- Verify simplified calculations

## Key Features to Test

### Currency Input Formatting
- Type any number in currency fields
- Should automatically format with Indian commas
- Example: 1500000 → 15,00,000
- Backspace and editing should work smoothly

### Tax Calculations
- Enter different income amounts
- Switch between Old and New regime
- Verify tax calculations match expected values
- Check rebate u/s 87A application
- Verify surcharge for high income

### Validations
- Leave required fields empty
- Enter invalid data (negative numbers, etc.)
- Check validation messages appear
- Verify category-based severity (Critical/Warning/Info)

### Form Persistence
- Fill form partially
- Refresh page
- Verify data is saved (if backend persistence is working)

## API Endpoints to Test

### Authentication
- POST `/api/auth/register` - Register new user
- POST `/api/auth/login` - Login user

### ITR Forms
- GET `/api/itr1/clients` - Get all clients
- POST `/api/itr1/clients` - Create new client
- GET `/api/itr1/clients/{id}/years/{year}` - Get ITR data
- PUT `/api/itr1/clients/{id}/years/{year}` - Update ITR data

### Calculations
- POST `/api/calculation/compute` - Compute tax
- POST `/api/calculation/compare-regimes` - Compare tax regimes

### Prefill
- POST `/api/prefill/parse` - Parse prefill JSON

## Known Issues

1. **403/500 Errors on Initial Load**: This is expected if you're not authenticated. Register/login first.
2. **Database Connection**: Ensure PostgreSQL connection in `.env` is valid.
3. **CORS**: If frontend can't reach backend, check CORS configuration in `SecurityConfig.java`.

## Stopping the Servers

To stop the servers:
1. Close the PowerShell windows that were opened
2. Or use Task Manager to end the Java and Node processes

## Next Steps

After testing, you may want to:
1. Add more validation rules
2. Implement PDF generation for ITR forms
3. Add Form 16 parsing and auto-fill
4. Implement e-filing integration
5. Add more comprehensive error handling

## Support

If you encounter issues:
1. Check browser console for frontend errors
2. Check backend logs in the PowerShell window
3. Verify database connectivity
4. Ensure all dependencies are installed
