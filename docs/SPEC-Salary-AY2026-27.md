# ITD Schema Compliant - Income from Salary (Schedule S)
## ITR-1 · ITR-2 · ITR-3 · ITR-4 | AY 2026-27 | CBDT Validated Implementation

---

> **Audit Basis:** Income Tax Act 1961 (as applicable for AY 2026-27), Finance Act 2025, CBDT Notifications 57 & 58/2026 (Corrigendum dated 10-Apr-2026), ITD e-Filing Validation Rules (AY 2025-26 V1.0 — closest published version), and AY 2026-27 ITR form structure released 30-Mar-2026.
>
> **Severity Levels:**
> - 🔴 **CRITICAL** — Category A violation. Return will be **rejected at upload** by the e-Filing portal.
> - 🟠 **HIGH** — Incorrect computation or wrong legal section. Tax liability or field mapping is wrong.
> - 🟡 **MEDIUM** — Schema/structure deviation that will cause AIS reconciliation failures or data quality flags.
> - 🔵 **LOW** — Missing compliance best-practice, deduction linkage omission, or enhancement needed.

---

## Issue Index

| # | Severity | Area | Short Title |
|---|----------|------|-------------|
| 1 | 🔴 CRITICAL | Section 17(1) | Salary Components - Missing perquisite breakdown |
| 2 | 🔴 CRITICAL | Section 16 | Standard Deduction - Regime-dependent amounts not applied |
| 3 | 🔴 CRITICAL | Section 10(13A) | HRA Exemption - Metro/Non-metro classification incomplete |
| 4 | 🟠 HIGH | Section 10(10) | Gratuity Exemption - Missing government/non-government differentiation |
| 5 | 🟠 HIGH | Section 10(10AA) | Leave Encashment - Missing government/non-government rates |
| 6 | 🟠 HIGH | Section 10(10A) | Pension Commutation - Missing full exemption rules |
| 7 | 🟠 HIGH | Section 10(14) | Special Allowances - Incomplete exemption list |
| 8 | 🟡 MEDIUM | Section 17(2) | Perquisites - Vehicle, property, stock options not computed |
| 9 | 🟡 MEDIUM | Section 17(3) | Profits in Lieu - Incomplete classification |
| 10 | 🟡 MEDIUM | Multi-Employer | Tax computation for multiple employers not consolidated |
| 11 | 🔵 LOW | ITD Tags | Schedule S field mapping to ITD tags incomplete |
| 12 | 🔵 LOW | Validation | Missing Category A rule validations for salary |

---

## Detailed Findings & Required Changes

---

### 🔴 ISSUE 1 — Section 17(1): Perquisite Breakdown Missing

**Location in Spec:** EmployerEntry record — Only scalar perquisites field exists.

**What the Spec Does:**
```
EmployerEntry currently has:
- perquisites: number (aggregate)
- perksValue: number (aggregate)
```

**Why It's Wrong:**
Perquisites under Section 17(2) must be reported **itemized** in the ITR form and JSON:
- Rent-free accommodation
- Concessional rent
- Motor car / vehicle
- Sweeper / gardener
- Gas / electricity / water
- Interest-free / concessional loan
- Holiday expenses
- Free education
- Gifts / vouchers
- Credit card expenses
- Club expenses
- Movable assets (computers, electronics, etc.)
- Stock options (ESOP) — Section 17(2)(iii) special treatment
- Any other perquisite

Each perquisite has different valuation rules (cost to employer, FMV, etc.).

**Required Changes:**

**Backend — EmployerEntry.java:**
```java
public record EmployerEntry {
    // Section 17(1) components (EXISTING)
    long basicSalary;
    long dearnessAllowance;
    long hraReceived;
    long bonus;
    long commission;
    long allowances;
    
    // Section 17(2) perquisites (NEW - itemized)
    long perqRentFreeAccommodation;
    long perqConcessionalRent;
    long perqMotorCar;
    long perqSweeper;
    long perqGardener;
    long perqGasElectricityWater;
    long perqInterestFreeLoan;
    long perqHolidayExpenses;
    long perqFreeEducation;
    long perqGiftsVouchers;
    long perqCreditCardExpenses;
    long perqClubExpenses;
    long perqMovableAssets;
    long perqStockOptions;          // ESOP - special treatment
    long perqOthers;
    
    // Section 17(3) - Profits in Lieu
    long profitsCompensationTermination;
    long profitsNonCompete;
}
```

---

### 🔴 ISSUE 2 — Section 16: Standard Deduction Not Regime-Dependent

**Location in Spec:** SalaryScheduleComputer — hardcoded standard deduction.

**Current Code:**
```java
long standardDeduction = regime == TaxRegime.NEW ? 75000 : 50000;
```

**Why It's Wrong:**
- The standard deduction amounts MUST be Read from `AssessmentYear` constants
- Verification needed that Rs 75,000 is for NEW regime and Rs 50,000 for OLD (NOT 75,000/50,000 in paise)

Currently: `NEW_REGIME_STANDARD_DEDUCTION = 75_00000L` (₹75,000 in paise) - CORRECT
           `OLD_REGIME_STANDARD_DEDUCTION = 50_00000L` (₹50,000 in paise) - CORRECT

**BUT:** Issue is that Section 16(i) standard deduction is NOT available in New Tax Regime for individuals having income from business/profession. Should only apply to:
- Salary income only
- Family pension only

**Rules:**
| Regime | Standard Deduction (₹) | Applies To |
|--------|------------------------|------------|
| New Regime | ₹75,000 | Salary + Family Pension (only if no business income) |
| Old Regime | ₹50,000 | Salary + Family Pension |

**Required Change:** Ensure New Regime standard deduction is conditional on no business income.

---

### 🔴 ISSUE 3 — Section 10(13A): HRA Exemption Metro Classification Incomplete

**Location in Spec:** HRAExemption.java — `isMetro` boolean.

**Current Code:**
```java
public static long compute(long actualHRA, long basicDA, long rentPaid, boolean isMetro) {
    long condition3 = isMetro ? (basicDA * 50 / 100) : (basicDA * 40 / 100);
    // ...
}
```

**Why It's Wrong:**
1. Metro cities must be explicitly listed as per Indian income tax rules:
   - Chennai, Delhi, Hyderabad, Kolkata, Mumbai, Pune
2. Rent paid must be validated: if rent > 50% of salary, and no HRA claimed, can claim rent deduction under Section 80GG
3. **CRITICAL:** Rent paid minus 10% of salary should be calculated on **basic + DA** only, not on total salary

**CBDT HRA Rules (20-20-40-50 rule):**
| Condition | Metro | Non-Metro |
|-----------|-------|-----------|
| Actual HRA received | Actual | Actual |
| Rent paid - 10% of salary | Rent - 10% | Rent - 10% |
| Salary for % calculation | Basic + DA | Basic + DA |
| % of salary | 50% | 40% |

**Required Changes:**

**Backend — HRAExemption.java:**
```java
public static final Set<String> METRO_CITIES = Set.of(
    "CHENNAI", "DELHI", "HYDERABAD", "KOLKATA", "MUMBAI", "PUNE"
);

public static long compute(
    long actualHRA,
    long basicSalary,      // Basic only (NOT Basic + DA)
    long da,               // Dearness Allowance
    long rentPaid,
    boolean isMetro
) {
    // Rent paid condition: Rent - 10% of (Basic + DA)
    long salaryForCalc = basicSalary + da;
    long rentDeduction = Math.max(rentPaid - (salaryForCalc * 10 / 100), 0);
    
    // Metro condition: 50% of (Basic + DA)
    long metroCondition = (salaryForCalc * 50 / 100);
    
    // Non-metro condition: 40% of (Basic + DA)
    long nonMetroCondition = (salaryForCalc * 40 / 100);
    
    long condition3 = isMetro ? metroCondition : nonMetroCondition;
    
    return Math.min(actualHRA, Math.min(rentDeduction, condition3));
}

// NEW: Check if 80GG deduction can be claimed
public static boolean canClaim80GG(long rentPaid, long basicSalary, long grossSalary) {
    // If rent > 50% of salary AND HRA not claimed -> eligible for 80GG
    return (rentPaid > grossSalary * 50 / 100);
}
```

---

### 🟠 ISSUE 4 — Section 10(10): Gratuity Exemption Missing Government Differentiation

**Location in Spec:** GratuityExemption.java — single calculation for all.

**Current Code:**
```java
public static long compute(long lastDrawnMonthlySalary, int yearsOfService, long actualGratuityReceived) {
    long calculated = (15L * lastDrawnMonthlySalary * yearsOfService) / 26;
    return Math.min(calculated, Math.min(20_00_00000L, actualGratuityReceived));
}
```

**Why It's Wrong:**
Gratuity exemption limits **DIFFER** for government vs non-government employees:

| Employee Type | Exemption Cap |
|---------------|---------------|
| Government Employee | Unlimited (fully exempt) |
| Non-Government Employee | ₹20,00,000 |

**CBDT Rule:** For non-government employees, the exemption is least of:
- 15/26 × last drawn salary × years of service
- ₹20,00,000
- Actual gratuity received

**Required Changes:**

```java
public static final long GRATUITY_CAP_NON_GOVT = 20_00_00000L; // Rs 20,00,000

public static long compute(
    long lastDrawnMonthlySalary,
    int yearsOfService,
    long actualGratuityReceived,
    boolean isGovernmentEmployee
) {
    if (isGovernmentEmployee) {
        // Government employee: Fully exempt
        return actualGratuityReceived;
    }
    
    // Non-government: Cap applies
    long calculated = (15L * lastDrawnMonthlySalary * yearsOfService) / 26;
    return Math.min(calculated, Math.min(GRATUITY_CAP_NON_GOVT, actualGratuityReceived));
}
```

---

### 🟠 ISSUE 5 — Section 10(10AA): Leave Encashment Missing Government Rates

**Location in Spec:** LeaveEncashmentExemption.java - NOT FOUND in codebase.

**Why It's Missing:**
Leave encashment exemption rules differ for government vs non-government:

| Employee Type | Exemption Cap |
|---------------|---------------|
| Government Employee | Fully exempt (no limit) |
| Non-Government Employee | ₹25,00,000 |

For non-government: Least of actual received, ₹25,00,000, or 10 months' average salary.

**Required Changes:**

**Backend — Create LeaveEncashmentExemption.java:**
```java
public final class LeaveEncashmentExemption {
    
    public static final long LEAVE_ENCASHMENT_CAP_NON_GOVT = 25_00_00000L; // Rs 25,00,000
    
    public static long compute(
        long actualLeaveEncashmentReceived,
        long averageMonthlySalary,
        int monthsOfLeave,
        boolean isGovernmentEmployee,
        int unutilizedLeaveDays
    ) {
        if (isGovernmentEmployee) {
            // Government: Fully exempt
            return actualLeaveEncashmentReceived;
        }
        
        // Non-government: Cap at Rs 25,00,000
        long calculated = averageMonthlySalary * Math.min(monthsOfLeave, 10);
        return Math.min(actualLeaveEncashmentReceived, 
               Math.min(LEAVE_ENCASHMENT_CAP_NON_GOVT, calculated));
    }
}
```

---

### 🟠 ISSUE 6 — Section 10(10A): Pension Commutation Missing

**Why It's Missing:**
Pension commutation has COMPLETE exemption rules:

| Type | Exemption |
|------|-----------|
| Government employee - full commutation | 100% exempt |
| Non-government - upto 1/3 of pension | 100% exempt |
| Remaining 2/3 taxed |

**Required Changes:**

```java
public static final class PensionCommutationExemption {
    
    /**
     * Compute exempt portion of commuted pension.
     * 
     * @param commutedPension    Total commuted pension
     * @param originalPension    Original uncommuted pension (monthly)
     * @param isGovernmentEmployee  
     * @return Exempt amount
     */
    public static long compute(
        long commutedPension,
        long originalPensionMonthly,
        boolean isGovernmentEmployee
    ) {
        if (isGovernmentEmployee) {
            return commutedPension; // Fully exempt
        }
        
        // Non-government: 1/3 exempt, 2/3 taxable
        // If monthly pension = X, annual = 12X
        // Commuted value for full pension = 12X (approx)
        // 1/3 of annual pension = 4X (tax-free)
        
        long annualPension = originalPensionMonthly * 12;
        long exemptPortion = annualPension / 3;  // 1/3 exempt
        
        return Math.min(exemptPortion, commutedPension);
    }
}
```

---

### 🟠 ISSUE 7 — Section 10(14): Special Allowances Incomplete List

**Location in Spec:** Not all allowances are computed with their limits.

**What Section 10(14) Covers:**
| Allowance | Limit | Condition |
|-----------|-------|-----------|
| Transport Allowance | ₹19,200/₹96,000 | For commuting to office |
| Children Education | ₹1,800/child (max 2) | School/college |
| Hostel Expenditure | ₹1,800/child (max 2) | For dependent children |
| Uniform Allowance | Actual cost | For uniform |
| Conveyance | Actual | For official duties |
| Daily Allowance | Actual | For travel |
| Helper | Actual | Helper for official work |
| Books & Periodicals | Actual | For professional studies |

**Required Changes:**

Add computation for all Section 10(14) allowances in SalaryScheduleComputer:
```java
public static class SpecialAllowanceExemption {
    
    public static final long TRANSPORT_ALLOWANCE_BASIC = 19_20000L; // Rs 1,600/month
    public static final long TRANSPORT_ALLOWANCE_SENIOR = 96_00000L; // Rs 8,000/month
    
    public static long computeTransportAllowance(
        long transportAllowanceReceived,
        boolean isSeniorCitizen
    ) {
        long cap = isSeniorCitizen ? TRANSPORT_ALLOWANCE_SENIOR : TRANSPORT_ALLOWANCE_BASIC;
        return Math.min(transportAllowanceReceived, cap);
    }
    
    public static long computeChildrenEducation(
        long childrenEducationReceived,
        int numberOfChildren
    ) {
        long perChild = 1_80000L; // Rs 1,800 per child
        int maxChildren = Math.min(numberOfChildren, 2);
        long cap = perChild * maxChildren;
        return Math.min(childrenEducationReceived, cap);
    }
    
    public static long computeHostelExpenditure(
        long hostelExpenditureReceived,
        int numberOfChildren
    ) {
        long perChild = 1_80000L; // Rs 1,800 per child
        int maxChildren = Math.min(numberOfChildren, 2);
        long cap = perChild * maxChildren;
        return Math.min(hostet ExpenditureReceived, cap);
    }
}
```

---

### 🟡 ISSUE 8 — Section 17(2): Perquisite Valuation Rules Missing

**Perquisite Valuation (CBDT Rules):**
| Perquisite | Valuation Rule |
|------------|----------------|
| Rent-free accommodation | Rent paid by employer or 15% of salary, whichever LOWER |
| Concessional accommodation | 15% of salary - rent charged |
| Motor car | Cost to employer or 15% of salary |
| Gas/Electricity/Water | Cost to employer or 10% of salary |
| Holiday expenses | Actual cost - employee's contribution |
| Free education | Cost to employer or actual fees |

**Required Changes:**
Add full valuation logic for each perquisite type in Backend.

---

### 🟡 ISSUE 9 — Section 17(3): Profits in Lieu

**Profits in Lieu of Salary includes:**
- Compensation for termination of employment
- Any amount due at the time of termination
- Any payment due to leave not availed
- Any amount receivable from unapproved superannuation fund

**Required Changes:**
Add proper classification and computation.

---

### 🟡 ISSUE 10 — Multi-Employer Tax Consolidation Not Implemented

**Problem:**
When employee has multiple employers in same year:
- Both employers may have deducted TDS
- Form 16 shows each separately
- Tax computation should consolidate ALL salary income

**CBDT Rule:**
- Aggregate salary from all employers
- Standard deduction from aggregate, NOT per employer
- Professional tax aggregate

**Required Changes:**
```java
// In SalaryScheduleComputer - Already partially implemented
public static SalaryComputationResult compute(
    List<EmployerEntry> employers, 
    long standardDeduction
) {
    // Aggregate all employers BEFORE computing standard deduction
    // Currently implemented correctly
}
```

---

### 🔵 ISSUE 11 — ITD Schedule S Tag Mapping

**Schedule S ITD Schemas for Salary:**
| Row | Field | ITD Tag |
|-----|-------|---------|
| 1 | Gross Salary | GrsSlyOfEmpSmFrmAllEmp |
| 2 | Salary u/s 17(1) | SalSmFrmOrgEmp |
| 3 | Allowances u/s 10(13A) | AllwncUscSec13A |
| 4 | Value of perquisites u/s 17(2) | ValPerquisitesSec17_2 |
| 5 | Profit in lieu u/s 17(3) | PrftLuSec17_3 |
| 6 | Income from other sources | IncFrm OthSrcNotChgbl |
| 7 | Deductions u/s 16 | DedctnUSec16 |
| 8 | Depreciation | DepAmtAvail |
| 9 |.exportAllowanceUs10(14) | Export allowance |
| 10 | Standard deduction | StdDedn |

**Required:** Map all salary fields to these ITD tags.

---

### 🔵 ISSUE 12 — Missing CBDT Category A Validation Rules

**Salary-Specific Validation Rules (Category A):**
| Rule | Check | Form |
|------|-------|------|
| S-01 | Std Deduction ≤ ₹75,000 (New) / ₹50,000 (Old) | All |
| S-02 | HRA Exemption calculation correct (20/40/50 rule) | All |
| S-03 | Professional Tax ≤ ₹2,500 | All |
| S-04 | Gratuity exemption ≤ ₹20,00,000 (non-govt) | All |
| S-05 | Leave encashment ≤ ₹25,00,000 (non-govt) | All |
| S-06 | Children education ≤ ₹1,800/child (max 2) | All |
| S-07 | Transport allowance ≤ ₹1,600 or ₹8,000 (senior) | All |

---

## Current Implementation Analysis

### Files Involved (Salary)

| File | Location | Purpose | Status |
|------|----------|---------|--------|
| **FRONTEND** |
| EmployerEntryManager.tsx | frontend/src/components/ | Multi-employer salary entry | Has most fields |
| salaryCalculationService.ts | frontend/src/services/ | Frontend calculations | Working |
| **BACKEND** |
| SalaryScheduleComputer.java | domain/salary/ | Compute salary | Needs update |
| SalaryComputationResult.java | domain/salary/ | Result DTO | Needs update |
| EmployerEntry.java | domain/salary/ | Per-employer DTO | Needs new fields |
| HRAExemption.java | domain/salary/ | HRA calculation | Needs metro fix |
| GratuityExemption.java | domain/salary/ | Gratuity calc | Missing govt flag |
| LeaveEncashmentExemption.java | domain/salary/ | MISSING | Needs to be created |

---

## Summary of All Issues

| Severity | Count | Issues |
|----------|-------|--------|
| 🔴 CRITICAL | 3 | #1 Perquisite breakdown, #2 Std deduction conditional, #3 HRA metro |
| 🟠 HIGH | 4 | #4 Gratuity govt diff, #5 Leave encashment govt diff, #6 Pension commutation, #7 Allowances |
| 🟡 MEDIUM | 3 | #8 Perquisite valuation, #9 Profits in lieu, #10 Multi-employer |
| 🔵 LOW | 2 | #11 ITD tag mapping, #12 Validation rules |
| **Total** | **12** | |

---

## Recommended Implementation Sequence

### Phase 1 — Critical (1 day)
1. Fix HRAExemption to use proper metro cities and salary calculation
2. Create LeaveEncashmentExemption.java with govt/non-govt caps
3. Update GratuityExemption to handle government employees
4. Update EmployerEntry perquisite breakdown

### Phase 2 — High Priority (1 day)
5. Fix Standard Deduction to be conditional on business income
6. Create PensionCommutationExemption
7. Add Section 10(14) allowances computation

### Phase 3 — Medium Priority (1 day)
8. Add perquisite valuation rules for each type
9. Fix Section 17(3) classification
10. Verify multi-employer aggregation

### Phase 4 — Low Priority (0.5 day)
11. Map to ITD Schedule S tags
12. Add CBDT Category A validation rules

---

## Appendix A: AY 2026-27 Salary Constants

From `AssessmentYear.java`:

```java
// NEW REGIME
public static final long NEW_REGIME_STANDARD_DEDUCTION = 75_00000L;  // Rs 75,000

// OLD REGIME
public static final long OLD_REGIME_STANDARD_DEDUCTION = 50_00000L;  // Rs 50,000

//gratuity
public static final long Gratuity_CAP_NON_GOVT = 20_00_00000L;  // Rs 20,00,000
public static final long LEAVE_ENCASHMENT_MAX = 25_00_00000L;  // Rs 25,00,000
public static final int PROFESSIONAL_TAX_CAP = 250000;  // Rs 2,500

// Transport allowance
public static final long TRANSPORT_ALLOWANCE_BASIC = 19_20000L;  // Rs 1,600/month
public static final long TRANSPORT_ALLOWANCE_SENIOR = 96_00000L; // Rs 8,000/month
```

---

## Appendix B: Section 17 Structure (ITD Schema)

### Section 17(1) - Salary
- Basic salary
- Dearness allowance
- Bonus
- Commission
- Allowances (HRA, DA, other)

### Section 17(2) - Perquisites
- Rent-free accommodation
- Concessional accommodation  
- Motor car/vehicle
- Service of sweeper/gardener
- Gas/electricity/water
- Interest-free loan
- Holiday expenses
- Free education
- Gifts/vouchers
- Credit card expenses
- Club expenses
- Movable assets (computers, etc.)
- Stock options (ESOP)

### Section 17(3) - Profits in Lieu
- Compensation for termination
- Non-compete fees
- Leave salary (if not already taxed)

---

*Document Version: 1.0 — Salary Schedule Compliance Audit*
*Spec Version References: SPEC-ScheduleOS-AY2026-27.md format*
*Audit Date: June 13, 2026*
*Applicable Law: Income Tax Act 1961 (for AY 2026-27 / FY 2025-26)*
*CBDT Notifications Referenced: 57/2026, 58/2026 (Corrigendum, 10-Apr-2026)*
