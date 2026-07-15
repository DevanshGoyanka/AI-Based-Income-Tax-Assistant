# Phase-wise Implementation Plan

**Version:** 2.1  
**Timeline:** 10 weeks  
**Team Size:** 2-3 developers

---

## Phase 0: Repository Cleanup — ✅ COMPLETE

**Status:** 7/9 items complete (2 deprioritized)

| # | Task | Status | Notes |
|---|------|--------|-------|
| 1 | Delete duplicate AIS decrypt | ✅ | `ais_decrypt.py` removed, no stale imports |
| 2 | Delete 18 debug scripts | ✅ | All `debug_*.py`, `examine_*.py`, `manual_*.py`, `standalone_*.py`, `test_*.py` removed |
| 3 | Split form26as.py into modules | ⏳ Not needed | Already in hexagonal structure: `Form26ASZipParser` + `Form26ASPDFParser` + `Form26ASNormalizer` handle all 10 parts in a single codebase via `Canonical26AS` domain model. No separate part-specific files needed. |
| 4 | Add .env.example | ✅ | Root `.env.example` exists with all required variables |
| 5 | Update .gitignore for vendor code | ✅ | `.gitignore` explicitly tracks `backend/app/adapters/opentax/vendor/` |
| 6 | Add missing foreign keys | ✅ | Migration `0005_add_missing_foreign_keys.py` adds all FKs |
| 7 | Fix prefill_data.id default | ✅ | Migration `0004_fix_prefill_id_default.py` adds `gen_random_uuid()` |
| 8 | Add unique constraints | ✅ | Migration `0006_add_unique_constraints_and_indexes.py` |
| 9 | Add auth guards (ProtectedRoute.tsx) | ⏳ Deferred | Frontend task, deferred to Phase 8. Backend auth is functional. |

**Migration chain:** `9976106a1337` → `aa8526dc8b3d` → `0004` → `0005` → `0006`

---

## Phase 1: OpenTax Vendoring — ✅ COMPLETE

**Status:** 12/12 items complete

| # | Task | Status | Notes |
|---|------|--------|-------|
| 1 | Fork OpenTax repository | ✅ | OpenTax-Reference@90c60bb exists |
| 2 | Copy tax_calculation/ to vendor | ✅ | `vendor/filing/tax_calculation/` with all models |
| 3 | Copy itr/ to vendor | ✅ | `vendor/filing/itr/` with itr1, validations, models |
| 4 | Copy models/ to vendor | ✅ | `vendor/filing/models/` with 60+ Pydantic models |
| 5 | Create __version__.py with commit SHA | ✅ | `OPENTAX_COMMIT = "90c60bb"`, `VENDOR_DATE = "2026-07-14"` |
| 6 | Update requirements.txt | ✅ | Root `requirements.txt` has `pydantic>=2.12.4` and `python-dateutil>=2.8.2` |
| 7 | Create facade module | ✅ | `vendor/__init__.py` re-exports `TaxCalculationService`, `Interest234Service` |
| 8 | All __init__.py for sub-packages | ✅ | 11 `__init__.py` files in vendor tree |
| 9 | Convert absolute imports to relative | ✅ | 69 `from filing.X` → relative imports, verified zero remaining |
| 10 | Verify all imports work | ✅ | 17 key imports verified working |
| 11 | .gitignore tracks vendor code | ✅ | Vendor directory is NOT ignored |
| 12 | vendor/README.md updated | ✅ | States "REFERENCE ONLY, not runtime dependency per ADR-022" |

**CRITICAL:** Per ADR-022, the vendored OpenTax code is **reference only** — never imported in production code for tax computation. Our owned `TaxEngine` handles all computation at runtime.

---

## Phase 2: Owned Tax Engine (Weeks 3-5)

**Version:** 2.0  
**Date:** 2026-07-15  
**Objective:** Build our OWN tax computation engine, using OpenTax as reference only.

---

## CRITICAL ARCHITECTURAL DECISION

**OpenTax is a REFERENCE IMPLEMENTATION, not a runtime dependency.**

We do NOT call OpenTax at runtime. We study its logic and reimplement
it ourselves using our own domain models. The vendored OpenTax code
(`vendor/filing/`) is kept for:

1. Reference — studying how slabs, deductions, surcharge, cess are computed
2. Test oracle — verifying our engine matches OpenTax within ₹1 tolerance
3. Future ITR JSON generation (Phase 7) — this IS worth reusing

We NEVER import from `vendor/filing/` in production code. Only in test files.

---

## Data Flow Architecture

```
User Request
    │
    ▼
┌─────────────────────────────────────────────────┐
│  API Controller (FastAPI)                       │
│  POST /api/v1/filings/{id}/compute?regime=new  │
└────────────┬────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────┐
│  ComputeTaxUseCase                              │
│  1. Load Filing from DB                        │
│  2. Load Client (for dob, pan)                 │
│  3. Extract schedules from filing              │
│  4. Call TaxEngine.compute()                   │
│  5. Create ComputedReturn snapshot             │
│  6. Persist snapshot                           │
└────────────┬────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────┐
│  TaxEngine (OUR engine, no OpenTax)            │
│                                                │
│  1. Compute salary income (from ScheduleSalary) │
│  2. Compute HP income (from ScheduleHP)        │
│  3. Compute other income (from ScheduleOS)     │
│  4. Sum → Gross Total Income                  │
│  5. Compute deductions (from ScheduleVIA)       │
│  6. GTI - Deductions = Total Income           │
│  7. Apply slab rates (from slab_tables.py)    │
│  8. Apply surcharge (if >50L)                 │
│  9. Apply 4% Health & Education Cess          │
│  10. Apply Rebate 87A (if applicable)          │
│  11. Compute TDS/TCS/Advance tax credit        │
│  12. Compute Interest 234A/B/C (our impl)     │
│  13. Net Tax Payable / Refund                  │
│  14. Generate ComputationStep[] explanation    │
│  15. Build ComputedReturn with TaxBreakdown     │
│                                                │
│  For BOTH regimes (old + new), always.          │
│  Return requested regime as primary,            │
│  other regime as comparison.                     │
└────────────┬────────────────────────────────────┘
             │
             ▼
┌────────────────────────────────────────────────���┐
│  ComputedReturn (immutable aggregate)           │
│  - payload: dict (full breakdown)               │
│  - tax_breakdown: TaxBreakdown (current regime) │
│  - old_regime_breakdown: TaxBreakdown            │
│  - new_regime_breakdown: TaxBreakdown            │
│  - slab_breakdown: List[SlabBreakdown]           │
│  - explanation: List[ComputationStep]             │
│  - created_at: datetime                          │
└─────────────────────────────────────────────────┘
```

---

## Income Computation Rules

### Salary Income
```
For each employer in ScheduleSalary.employers:
  gross = employer.gross_salary
  allowances_exempt = employer.allowances_exempt
  professional_tax = employer.professional_tax

total_gross = sum(gross for all employers)
total_allowances = sum(allowances_exempt for all employers)
total_prof_tax = sum(professional_tax for all employers)

New regime:
  net_salary = total_gross - NEW_STD_DEDUCTION(₹75,000)

Old regime:
  net_salary = total_gross - total_allowances - total_prof_tax - OLD_STD_DEDUCTION(₹50,000)
  net_salary = max(net_salary, 0)
```

### House Property Income
```
For each property in ScheduleHP.properties:
  nav = property.annual_rent - property.municipal_taxes
  std_deduction = nav * 30% (if nav > 0, else 0)
  interest = property.interest_paid
  hp_income = (nav - std_deduction - interest) * ownership_share

Total HP income = sum(hp_income for all properties)
HP loss set-off cap = max(total, -₹2,00,000)  [Section 24B]
```

### Other Sources Income (Interest + Dividend)
```
interest_income = sum(detail.interest_amount for detail in ScheduleOS.interest_income)
dividend_income = sum(detail.dividend_amount for detail in ScheduleOS.dividend_income)
other_income = ScheduleOS.other_income

total_os = interest_income + dividend_income + other_income
```

### Chapter VI-A Deductions (Old Regime Only)
```
Section 80C: min(schedule.section_80c.total(), ₹1,50,000)
Section 80CCD(1B): min(section.section_80ccd_1b, ₹50,000)
Section 80CCD(2): section.section_80ccd_2 (no limit)
Section 80D: schedule.section_80d.total() [with age-based caps]
Section 80E: section.section_80e (no limit)
Section 80G: section.section_80g (with 10% of adjusted GTI cap)
Section 80TTA: min(section.section_80tta, ₹10,000) — or 80TTB for seniors
Section 80U: ₹75,000 (₹1,25,000 for severe disability)

New regime deductions: Only 80CCD(2) employer NPS
```

### Tax Slab Computation
```
OLD REGIME (age < 60):
  ₹0 - ₹2,50,000: 0%
  ₹2,50,001 - ₹5,00,000: 5%
  ₹5,00,001 - ₹10,00,000: 20%
  Above ₹10,00,000: 30%

OLD REGIME (age 60-79):
  ₹0 - ₹3,00,000: 0%
  ₹3,00,001 - ₹5,00,000: 5%
  ₹5,00,001 - ₹10,00,000: 20%
  Above ₹10,00,000: 30%

OLD REGIME (age 80+):
  ₹0 - ₹5,00,000: 0%
  ₹5,00,001 - ₹10,00,000: 20%
  Above ₹10,00,000: 30%

NEW REGIME AY 2026-27 (all ages):
  ₹0 - ₹4,00,000: 0%
  ₹4,00,001 - ₹8,00,000: 5%
  ₹8,00,001 - ₹12,00,000: 10%
  ₹12,00,001 - ₹16,00,000: 15%
  ₹16,00,001 - ₹20,00,000: 20%
  ₹20,00,001 - ₹24,00,000: 25%
  Above ₹24,00,000: 30%
```

### Surcharge
```
₹50L - ₹1Cr: 10%
₹1Cr - ₹2Cr: 15%
₹2Cr - ₹5Cr: 25%
Above ₹5Cr: 37% (old) / 25% (new)

With marginal relief calculation for each bracket.
```

### Rebate 87A
```
NEW REGIME (AY 2026-27):
  If tax ≤ ₹60,000: full rebate
  If income > ₹12,00,000: no rebate
  Marginal relief between ₹60,000 and actual tax

OLD REGIME:
  If tax ≤ ₹12,500: full rebate
  If income > ₹5,00,000: no rebate
```

### Health & Education Cess
```
4% on (tax after rebate + surcharge)
```

### Interest u/s 234A/B/C
```
234A: 1% per month on unpaid tax from due date
234B: 1% per month on advance tax shortfall
234C: 1% per month on deferment of advance tax installments
234F: Late filing fee (₹1,000-₹5,000)

Implemented by our own Interest234Engine (not OpenTax's).
```

---

## Files to Create

### `core/services/slab_tables.py`
CBDT slab rates, deduction limits, and rule constants indexed by AY.
All values are hardcoded with a `RuleVersion` class for easy AY switching.

### `core/services/tax_engine.py`
Our owned tax computation engine. Pure functions, no side effects,
no database calls. Takes domain schedules + context, returns `ComputedReturn`.

Methods:
- `compute_salary_income(schedules, regime)` → `(gross, allowances)`
- `compute_hp_income(schedules)` → `Decimal`
- `compute_os_income(schedules)` → `Decimal`
- `compute_deductions(schedules, regime, age)` → `Decimal`
- `compute_slab_tax(income, age, regime, ay)` → `(slab_entries, tax)`
- `compute_surcharge(tax, income, regime)` → `(amount, SurchargeBreakdown)`
- `compute_rebate_87a(tax, income, regime)` → `Decimal`
- `compute_cess(tax_after_rebate, surcharge)` → `(amount, CessBreakdown)`
- `compute_interest_234(net_tax, tds, advance_tax, ...)` → `InterestBreakdown`
- `compute(schedules, regime, ay, dob)` → `ComputedReturn` (main entry)

### `core/services/interest_234_engine.py`
Our own implementation of interest u/s 234A/B/C calculation.
Studies OpenTax's `Interest234Service` algorithm and reimplements
from scratch using our data structures.

### `core/domain/computed_return.py` (UPDATE)
Add `TaxBreakdown` dataclass and dual-regime fields:
```python
@dataclass
class TaxBreakdown:
    gross_total_income: int = 0
    total_deductions: int = 0
    total_income: int = 0
    tax_before_rebate: int = 0
    rebate_87a: int = 0
    tax_after_rebate: int = 0
    surcharge: int = 0
    health_education_cess: int = 0
    total_tax_liability: int = 0
    tds: int = 0
    tcs: int = 0
    advance_tax: int = 0
    total_taxes_paid: int = 0
    tax_payable: int = 0
    refund: int = 0
    interest_234a: int = 0
    interest_234b: int = 0
    interest_234c: int = 0
    late_fee_234f: int = 0

@dataclass
class ComputedReturn:
    # ... existing fields ...
    tax_breakdown: Optional[TaxBreakdown] = None
    old_regime_breakdown: Optional[TaxBreakdown] = None  # Always computed
    new_regime_breakdown: Optional[TaxBreakdown] = None  # Always computed
```

---

## Files to Modify

### `core/interfaces/tax_engine.py` (UPDATE)
Add `person_dob` and `person_pan` parameters:
```python
class ITaxEngine(ABC):
    @abstractmethod
    async def compute_tax(
        self,
        schedules: List[Schedule],
        regime: TaxRegime,
        ay: AssessmentYear,
        person_dob: Optional[date] = None,  # NEW
        person_pan: Optional[str] = None,     # NEW
    ) -> ComputedReturn: ...
```

### `core/use_cases/compute_tax.py` (UPDATE)
Fetch `person_dob` and `person_pan` from client and pass to engine.

### `app/adapters/opentax/tax_engine_adapter.py` (REWRITE)
Delegate to our `TaxEngine` instead of OpenTax:
```python
class OpenTaxAdapter(ITaxEngine):
    def __init__(self):
        self.engine = TaxEngine()
    
    async def compute_tax(
        self,
        schedules: List[Schedule],
        regime: TaxRegime,
        ay: AssessmentYear,
        person_dob: Optional[date] = None,
        person_pan: Optional[str] = None,
    ) -> ComputedReturn:
        return self.engine.compute(
            schedules=schedules,
            regime=regime.value,
            ay=ay.value,
            dob=person_dob,
        )
```

---

## Files to DELETE

### `app/adapters/opentax/model_mapper.py`
No longer needed — we don't map to OpenTax models.

### `app/adapters/opentax/response_mapper.py`
No longer needed — our engine produces `ComputedReturn` directly.

---

## Files to KEEP AS-IS (Reference Only)

All vendored OpenTax code in `app/adapters/opentax/vendor/filing/` is kept
for reference and test oracle purposes. A README is added stating:

> This code is for REFERENCE and TESTING only.
> It is NOT imported in production code.
> It is NOT a runtime dependency.
> Our owned TaxEngine implements the same logic.

---

## Schedules Supported in Phase 2

| Schedule | Status | Notes |
|----------|--------|-------|
| ScheduleSalary | ✅ Full | gross, allowances, prof tax, std deduction |
| ScheduleHP | ✅ Full | rent, municipal tax, interest, ownership share |
| ScheduleOS | ✅ Full | interest, dividend, other income |
| ScheduleVIA | ✅ Full | 80C/CCD/1B/2/D/E/G/TTA/TTB/U |
| ScheduleTDS | ✅ Full | deductor, TAN, section, amount |
| ScheduleIT | ✅ Full | advance tax, self-assessment tax |
| ScheduleCG | ⏳ Phase 3 | Special rate logic (111A, 112A, 112) |
| ScheduleBA | ⏳ Phase 7 | No tax impact, needed for ITR JSON only |

---

## Computation Explanation (ADR-021)

Every computation method returns a `List[ComputationStep]` explaining
each calculation:

```python
@dataclass
class ComputationStep:
    step: str          # "salary_income", "standard_deduction", "slab_tax", etc.
    description: str   # Human-readable explanation
    input_value: str   # "₹15,00,000"
    output_value: str  # "₹14,25,000"
    rule_applied: str  # "New regime standard deduction ₹75,000"
```

This enables:
- CA audit trail for every computation
- Client-facing explanation ("Your tax is ₹97,500 because...")
- Debugging when results don't match expectations

---

## Testing Strategy

### Unit Tests: Tax Engine

Each computation method tested independently:
- `test_salary_income_new_regime()` — standard deduction ₹75,000
- `test_salary_income_old_regime()` — standard deduction ₹50,000
- `test_hp_income_self_occupied()` — loss from interest
- `test_hp_income_let_out()` — NAV - 30% - interest
- `test_deductions_80c_cap()` — ₹1.5L cap
- `test_deductions_80d_senior()` — ₹50K cap
- `test_slab_tax_new_regime()` — all brackets
- `test_slab_tax_old_regime_senior()` — age-based exemptions
- `test_surcharge_marginal_relief()` — edge cases
- `test_rebate_87a_new_regime()` — ₹60K rebate
- `test_rebate_87a_old_regime()` — ₹12.5K rebate
- `test_cess_4_percent()` — on tax + surcharge
- `test_interest_234b_advance_tax_shortfall()` — installment logic

### Integration Tests: Golden Cases vs OpenTax Oracle

For each golden case, compute with BOTH our engine and OpenTax,
assert results match within ₹1:

```python
@pytest.mark.parametrize("case", GOLDEN_TEST_CASES)
async def test_matches_opentax(case):
    # Build our domain schedules
    schedules = build_schedules_from_case(case)
    
    # Compute with our engine
    our_result = tax_engine.compute(schedules, case.regime, case.ay, case.dob)
    
    # Build OpenTax FilingModel (test oracle only)
    filing_model = build_opentax_filing_model(case)
    opentax_result = await TaxCalculationService().calculate(filing_model)
    opentax_tax = int(opentax_result.tax_computation.current_regime.total_tax_liability)
    
    # Must match within ₹1 (rounding tolerance)
    assert abs(our_result.tax_breakdown.total_tax_liability - opentax_tax) <= 1
```

### 10 Golden Test Cases

1. Salaried, new regime, ₹15L — basic slab test
2. Salaried, old regime, ₹15L + ₹1.5L deductions — 80C cap test
3. Low income, new regime, ₹5.75L — rebate 87A test
4. High income, ₹60L — surcharge 10% test
5. Senior citizen (age 65), old regime — age-based exemption
6. HP let-out property — NAV - 30% - interest
7. Interest + dividend income — other sources
8. Super senior (age 82), old regime — ₹5L exemption
9. TDS + advance tax — tax credit calculation
10. Multiple employers — salary aggregation

---

## Definition of Done

- [ ] `core/services/slab_tables.py` created with AY 2026-27 rules
- [ ] `core/services/tax_engine.py` created with all computation methods
- [ ] `core/services/interest_234_engine.py` created
- [ ] `core/domain/computed_return.py` updated with `TaxBreakdown`
- [ ] `core/interfaces/tax_engine.py` updated with `person_dob`/`person_pan`
- [ ] `core/use_cases/compute_tax.py` updated to pass DOB/PAN
- [ ] `app/adapters/opentax/tax_engine_adapter.py` rewritten to delegate to TaxEngine
- [ ] `app/adapters/opentax/model_mapper.py` DELETED
- [ ] `app/adapters/opentax/response_mapper.py` DELETED
- [ ] All 10 golden tests pass (our engine matches OpenTax within ₹1)
- [ ] Every computation method produces `List[ComputationStep]` explanation
- [ ] Both regimes always computed (user can compare in UI)
- [ ] Zero imports from `vendor/filing/` in production code
- [ ] `vendor/README.md` updated to state "reference only, not runtime dependency"
