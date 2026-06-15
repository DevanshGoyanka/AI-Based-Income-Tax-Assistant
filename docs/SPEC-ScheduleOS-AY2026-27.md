# ITD Schema Compliant - Income from Other Sources (Schedule OS)
## ITR-2 AY 2026-27 | CBDT Validated Implementation

---

## PART A: CURRENT IMPLEMENTATION ANALYSIS

### 1. CURRENT DATA FLOW (for Other Sources Only)

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     FRONTEND                                            │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ITRComputationPage.tsx                                                              │
│       │                                                                                  │
│       ├── formData (contains ALL fields including Other Sources)                         │
│       │                                                                                  │
│       ├── bankInterestEntries[] ──────────────► BankInterestEntryManager.tsx            │
│       │                                              (type: "SAVINGS" | "FD")      │
│       │                                              (BUT accepts ALL sections!)       │
│       │                                                                                  │
│       ├── dividendEntries[] ────────────────► DividendEntryManager.tsx                 │
│       │                                              (no 10(22e) vs 10(22f)      │
│       │                                              (classification!)                │
│       │                                                                                  │
│       ├── nscInterest (manual field)                                                     │
│       ├── scssInterest (manual field)                                                    │
│       ├── postOfficeInterest (manual field)                                              │
│       ├── interestFromITRefund (manual field)                                           │
│       ├── familyPension (manual field)                                                  │
│       ├── lotteryIncome (manual field)                                                  │
│       ├── cardGameIncome (manual field)                                                 │
│       ├── horseRaceIncome (manual field)                                                │
│       ├── vdaGains (manual field)                                                      │
│       └── giftsFromNonRelatives (manual field)                                          │
│                                                                                          │
│       └── calls itrApi.computeTaxSummary(formData, ayParam, regime)                    │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
                                           │
                                           ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     BACKEND                                              │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ENDPOINTS (Multiple - CONFUSION!):                                                       │
│  ───────────────────────────────────                                                      │
│  1. /api/v1/tax-summary/compute (TaxController.java) ◄── ACTIVE                        │
│     - Extracts bankInterestEntries[], dividendEntries[]                                   │
│     - Falls back to manual fields if arrays empty                                       │
│     - Returns TaxComputationResult                                                       │
│                                                                                          │
│  2. /tax-summary/compute (TaxSummaryController.java) ◄── NOT USED                     │
│     - Same logic as TaxController                                                       │
│     - Could be deleted                                                                 │
│                                                                                          │
│  3. /api/tax/compute (TaxComputationController.java) ◄── NOT USED                     │
│     - Needs ITRForm object                                                             │
│     - Could be deleted                                                                 │
│                                                                                          │
│  FLOW IN TaxController.java:                                                            │
│  ────────────────────────                                                               │
│  request.get("formData") or request (direct)                                            │
│       │                                                                                  │
│       ▼                                                                                  │
│  Extracts:                                                                              │
│  - dividendEntries[] → sum(dividendAmount) → totalDividend                             │
│  - bankInterestEntries[] → sum(interestEarned) → bankInterest                          │
│  - nscInterest → direct field                                                          │
│  - scssInterest → direct field                                                         │
│  - postOfficeInterest → direct field                                                   │
│  - interestFromITRefund → direct field                                                 │
│  - familyPension → direct field                                                        │
│  - lotteryIncome → direct field                                                        │
│  - etc...                                                                              │
│       │                                                                                  │
│       ▼                                                                                  │
│  ITRForm builder → TaxComputationOrchestrator                                           │
│       │                                                                                  │
│       ▼                                                                                  │
│  TaxComputationOrchestrator.computeTax(ITRForm, TaxRegime)                            │
│       │                                                                                  │
│       ▼                                                                                  │
│  OtherSourcesIncomeComputer.compute(OtherSourcesIncomeInput)                           │
│       │                                                                                  │
│       ▼                                                                                  │
│  Returns:                                                                               │
│  - otherIncomeTaxable                                                                    │
│  - totalInterest (bankInterest + nsc + scss + postOffice + itRefund)                   │
│  - totalDividend                                                                        │
│  - familyPensionDed (after u/s 16(iv))                                               │
│  - totalWinnings (lottery + cardGame + horseRace)                                      │
│  - winningsTax (30%)                                                                    │
│  - vdaGains                                                                            │
│  - vdaTax (30%)                                                                         │
│  - taxableGifts                                                                         │
│       │                                                                                  │
│       ▼                                                                                  │
│  TaxComputationResult → JSON response → Frontend                                         │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2. CURRENT FILES INVOLVED (Other Sources)

| File | Location | Purpose | Status |
|------|----------|---------|--------|
| **FRONTEND** |
| ITRComputationPage.tsx | frontend/src/pages/ | Main page, calls computeTaxSummary | ACTIVE |
| ITRComputationTabs.tsx | frontend/src/pages/ | OtherSourcesTab component | ACTIVE |
| itr.ts (API) | frontend/src/api/ | computeTaxSummary() | ACTIVE |
| BankInterestEntryManager.tsx | frontend/src/components/ | Bank interest entries | NEEDS FIX |
| DividendEntryManager.tsx | frontend/src/components/ | Dividend entries | NEEDS FIX |
| **BACKEND** |
| TaxController.java | backend/.../config/ | /api/v1/tax-summary/compute | ACTIVE |
| TaxSummaryController.java | backend/.../controller/ | /tax-summary/compute | ORPHAN - DELETE |
| TaxComputationController.java | backend/.../controller/ | /api/tax/compute | ORPHAN - DELETE |
| TaxComputationOrchestrator.java | backend/.../service/ | Computes tax | NEEDS UPDATE |
| ITRForm.java | backend/.../domain/itr/ | DTO for form data | NEEDS UPDATE |
| TaxComputationResult.java | backend/.../domain/common/ | Result DTO | NEEDS UPDATE |
| OtherSourcesIncomeComputer.java | backend/.../domain/othersources/ | Compute Other Sources | NEEDS DELETE |

### 3. FILES TO DELETE (OLD/ORPHAN)

| File | Full Path | Reason |
|------|----------|--------|
| TaxSummaryController.java | backend/src/main/java/com/itr/controller/TaxSummaryController.java | Duplicate of TaxController logic - /tax-summary endpoint never called |
| TaxComputationController.java | backend/src/main/java/com/itr/controller/TaxComputationController.java | Duplicate - /api/tax endpoint never called |
| InterestEntry.ts | frontend/src/components/InterestEntry.ts | Was created but not used |

### 4. CURRENT ISSUES IN FLOW

1. **Duplicate Endpoints**: 3 different endpoints for same computation
2. **BankInterestEntryManager**: Accepts ALL interest types (should be Bank ONLY)
3. **Manual Fields**: NSC/SCSS/PostOffice entered separately but should come from dedicated managers
4. **Dividend Classification**: No 10(22e) vs 10(22f) vs 194 distinction
5. **Family Pension**: No u/s 16(iv) deduction visible in UI
6. **ITD Tags**: None of the fields map to actual ITD JSON tags (17A-17H)

---

## PART B: PROPOSED ITD-COMPLIANT IMPLEMENTATION

### 5. UPDATED DATA FLOW

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     FRONTEND                                            │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ITRComputationPage.tsx                                                              │
│       │                                                                                  │
│       └── formData: {                                                                    │
│             interestEntries: [                                                          │
│               { itdTag: "IntrstFrmSavingBank", grossAmount: X, tdsDeducted: Y },       │
│               { itdTag: "IntrstFrmTermDeposit", grossAmount: X, tdsDeducted: Y },        │
│               { itdTag: "IntrstSec10XIFirstProviso", grossAmount: X, tdsDeducted: Y },│
│               { itdTag: "IntrstSec10XISecondProviso", grossAmount: X, tdsDeducted: 0 },│
│               { itdTag: "IntrstSec10XIIFirstProviso", grossAmount: X, tdsDeducted: 0 },│
│               { itdTag: "IntrstFrmIncmTaxRefund", grossAmount: X, tdsDeducted: 0 }     │
│             ],                                                                            │
│                                                                                          │
│             dividendEntries: [                                                           │
│               { itdTag: "Dividend22e", section: "10(22e)", grossAmount: X },            │
│               { itdTag: "Dividend22f", section: "10(22f)", grossAmount: X },            │
│               { itdTag: "DividendOthThan22e", section: "194", grossAmount: X }         │
│             ],                                                                            │
│                                                                                          │
│             familyPension: { grossAmount: X },                                          │
│                                                                                          │
│             winningsEntries: [                                                          │
│               { type: "LOTTERY", grossAmount: X, tdsDeducted: Y },                    │
│               { type: "HORSE_RACE", grossAmount: X, tdsDeducted: Y }                  │
│             ],                                                                            │
│                                                                                          │
│             giftEntries: [                                                               │
│               { propertyType: "IMMOVABLE", value: X }                                   │
│             ]                                                                            │
│       }                                                                                  │
│                                                                                          │
│       ├── itrApi.computeTaxSummary(formData, ayParam, regime)                           │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
                                           │
                                           ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     BACKEND                                              │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  SINGLE ACTIVE ENDPOINT:                                                                 │
│  ────────────────────────                                                                 │
│  /api/v1/tax-summary/compute (TaxController.java)                                        │
│       │                                                                                  │
│       ▼                                                                                  │
│  Extract from formData:                                                                  │
│  - interestEntries[] → ScheduleOSComputer.computeInterest()                              │
│  - dividendEntries[] → ScheduleOSComputer.computeDividend()                              │
│  - familyPension → ScheduleOSComputer.computeFamilyPension()                             │
│  - winningsEntries[] → ScheduleOSComputer.computeWinnings()                             │
│  - giftEntries[] → ScheduleOSComputer.computeGifts()                                  │
│       │                                                                                  │
│       ▼                                                                                  │
│  ScheduleOSComputer (NEW COMPONENT)                                                      │
│  ├─ InterestIncomeComputer → Maps to ITD tags (17A-17H)                               │
│  │   - Applies TDS rules                                                                │
│  │   - Identifies exempt income                                                        │
│  ├─ DividendIncomeComputer → Maps to 10(22e), 10(22f), 194                          │
│  │   - Identifies exempt dividends                                                     │
│  ├─ FamilyPensionComputer → Applies u/s 16(iv) deduction                             │
│  ├─ WinningsComputer → 30% + 4% cess                                                  │
│  ├─ GiftPropertyComputer → Maps to 56(2)(x)                                           │
│  └─ Aggregates all → ITD Schema JSON response                                           │
│       │                                                                                  │
│       ▼                                                                                  │
│  Returns TaxComputationResult with all ITD-compliant fields                            │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 6. NEW FILE STRUCTURE

#### Frontend Files to CREATE:

| # | File | Path | Purpose |
|---|------|------|---------|
| 1 | InterestEntryManager.tsx | frontend/src/components/interest/ | Unified interest entry (Bank/PostOffice/NSC/SCSS/ITRef) |
| 2 | BankInterestManager.tsx | frontend/src/components/interest/ | RENAME existing to this |
| 3 | PostOfficeInterestManager.tsx | frontend/src/components/interest/ | NEW - Post Office interest |
| 4 | NSCInterestManager.tsx | frontend/src/components/interest/ | NEW - NSC interest |
| 5 | SCSSInterestManager.tsx | frontend/src/components/interest/ | NEW - SCSS interest |
| 6 | ITRefundInterestManager.tsx | frontend/src/components/interest/ | NEW - IT refund interest |
| 7 | InterestSummary.tsx | frontend/src/components/interest/ | NEW - Aggregate display |
| 8 | DividendEntryManager.tsx | frontend/src/components/dividend/ | UPDATE - Add 10(22e)/10(22f) classification |
| 9 | WinningsManager.tsx | frontend/src/components/winnings/ | NEW - Lottery/Card Game/Horse Race |
| 10 | GiftPropertyManager.tsx | frontend/src/components/gifts/ | NEW - 56(2)(x) gifts |
| 11 | FamilyPensionManager.tsx | frontend/src/components/familyPension/ | NEW - Family pension |

#### Frontend Files to MODIFY:

| # | File | Changes |
|---|------|---------|
| 1 | ITRComputationTabs.tsx | Update OtherSourcesTab to use new managers |
| 2 | itr.ts (API) | May need updates for new field names |
| 3 | ITRComputationPage.tsx | Update to handle new field structures |

#### Backend Files to CREATE:

| # | File | Path | Purpose |
|---|------|------|---------|
| 1 | ScheduleOSComputer.java | backend/.../domain/othersources/ | NEW - Main orchestrator |
| 2 | InterestIncomeComputer.java | backend/.../domain/othersources/ | NEW - Interest with ITD tags |
| 3 | DividendIncomeComputer.java | backend/.../domain/othersources/ | NEW - Dividend with 10(22e)/10(22f) |
| 4 | WinningsComputer.java | backend/.../domain/othersources/ | NEW - Winnings at 30% |
| 5 | FamilyPensionComputer.java | backend/.../domain/othersources/ | NEW - Family pension |
| 6 | GiftPropertyComputer.java | backend/.../domain/othersources/ | NEW - 56(2)(x) gifts |

#### Backend Files to MODIFY:

| # | File | Changes |
|---|------|---------|
| 1 | TaxController.java | Update /api/v1/tax-summary/compute to use new ScheduleOSComputer |
| 2 | ITRForm.java | Add new fields for entries (interestEntries, giftEntries, etc.) |
| 3 | TaxComputationOrchestrator.java | Remove OtherSourcesIncomeComputer, use ScheduleOSComputer |
| 4 | TaxComputationResult.java | Add ITD-compliant fields |

#### Backend Files to DELETE:

| # | File | Full Path | Reason |
|---|------|----------|--------|
| 1 | TaxSummaryController.java | backend/src/main/java/com/itr/controller/TaxSummaryController.java | Duplicate - /tax-summary never called |
| 2 | TaxComputationController.java | backend/src/main/java/com/itr/controller/TaxComputationController.java | Duplicate - /api/tax never called |
| 3 | OtherSourcesIncomeComputer.java | backend/src/main/java/com/itr/domain/othersources/OtherSourcesIncomeComputer.java | Will be replaced by new components |

### 7. COMPLETE FILE LIST

#### ALL FILES (Current + New):

```
FRONTEND:
├── src/
│   ├── api/
│   │   └── itr.ts  ◄────────── MODIFY (may need new field names)
│   │
│   ├── components/
│   │   ├── interest/
│   │   │   ├── InterestEntryManager.tsx     ◄── NEW (unified entry)
│   │   │   ├── BankInterestManager.tsx       ◄── MODIFY (bank only)
│   │   │   ├── PostOfficeInterestManager.tsx ◄── NEW
│   │   │   ├── NSCInterestManager.tsx        ◄── NEW
│   │   │   ├── SCSSInterestManager.tsx      ◄── NEW
│   │   │   ├── ITRefundInterestManager.tsx  ◄── NEW
│   │   │   └── InterestSummary.tsx          ◄── NEW
│   │   │
│   │   ├── dividend/
│   │   │   └── DividendEntryManager.tsx    ◄── MODIFY (add 10(22e)/10(22f))
│   │   │
│   │   ├── winnings/
│   │   │   └── WinningsManager.tsx          ◄── NEW
│   │   │
│   │   ├── gifts/
│   │   │   └── GiftPropertyManager.tsx      ◄── NEW
│   │   │
│   │   ├── familyPension/
│   │   │   └── FamilyPensionManager.tsx     ◄── NEW
│   │   │
│   │   ├── BankInterestEntryManager.tsx    ◄── DELETE (will be replaced)
│   │   ├── InterestEntry.ts               ◄── DELETE (was temp file)
│   │   └── DividendEntryManager.tsx        ◄── KEEP but MODIFY
│   │
│   └── pages/
│       ├── ITRComputationPage.tsx     ◄── MODIFY (new field names)
│       └── ITRComputationTabs.tsx     ◄── MODIFY (use new managers)

BACKEND:
├── src/main/java/com/itr/
│   ├── config/
│   │   └── TaxController.java           ◄── MODIFY (use ScheduleOSComputer)
│   │
│   ├── controller/
│   │   ├── TaxSummaryController.java   ◄── DELETE (duplicate)
│   │   └── TaxComputationController.java ◄── DELETE (duplicate)
│   │
│   ├── domain/
│   │   ├── itr/
│   │   │   └── ITRForm.java            ◄── MODIFY (add entry fields)
│   │   │
│   │   ├── common/
│   │   │   └── TaxComputationResult.java ◄── MODIFY (ITD fields)
│   │   │
│   │   └── othersources/
│   │       ├── ScheduleOSComputer.java        ◄── NEW
│   │       ├── InterestIncomeComputer.java    ◄── NEW
│   │       ├── DividendIncomeComputer.java    ◄── NEW
│   │       ├── WinningsComputer.java         ◄── NEW
│   │       ├── FamilyPensionComputer.java   ◄── NEW
│   │       ├── GiftPropertyComputer.java    ◄── NEW
│   │       └── OtherSourcesIncomeComputer.java ◄── DELETE (replace)
│   │
│   └── service/
│       └── TaxComputationOrchestrator.java  ◄── MODIFY (use ScheduleOSComputer)
```

### 8. IMPLEMENTATION SEQUENCE

#### Phase 1: Backend (3 days)
| Day | Tasks |
|-----|-------|
| Day 1 | DELETE: TaxSummaryController, TaxComputationController, OtherSourcesIncomeComputer |
| Day 1 | CREATE: ScheduleOSComputer.java, InterestIncomeComputer.java, DividendIncomeComputer.java |
| Day 2 | CREATE: WinningsComputer.java, FamilyPensionComputer.java, GiftPropertyComputer.java |
| Day 3 | MODIFY: TaxController, ITRForm, TaxComputationResult, TaxComputationOrchestrator |
| Day 3 | TEST: Verify backend endpoints work correctly |

#### Phase 2: Frontend (3 days)
| Day | Tasks |
|-----|-------|
| Day 4 | CREATE: Interest managers (PostOffice, NSC, SCSS, ITRefund, Summary) |
| Day 5 | CREATE: WinningsManager, GiftPropertyManager, FamilyPensionManager |
| Day 5 | MODIFY: DividendEntryManager (add 10(22e)/10(22f) classification) |
| Day 6 | MODIFY: ITRComputationTabs to use new managers |
| Day 6 | TEST: Verify UI components work correctly |

#### Phase 3: Integration (1 day)
| Day | Tasks |
|-----|-------|
| Day 7 | Connect frontend to backend with new field structure |
| Day 7 | TEST: End-to-end flow with sample data |

#### Phase 4: Testing & Validation (2 days)
| Day | Tasks |
|-----|-------|
| Day 8 | CBDT validation rules test |
| Day 9 | ITD JSON schema validation |

---

## PART C: SUMMARY

### Files to CREATE: 17 total
- Frontend: 11 files
- Backend: 6 files

### Files to MODIFY: 7 total
- Frontend: 3 files
- Backend: 4 files

### Files to DELETE: 5 total
- Frontend: 2 files
- Backend: 3 files

### Total Files in System After Implementation: ~25 files
- Frontend: ~14 files
- Backend: ~11 files

---

**Document Version**: 2.0
**ITD Schema Version**: ITR-2_2026_Main_V1.0.json
**Assessment Year**: 2026-27
**Last Updated**: 2026-06-12
