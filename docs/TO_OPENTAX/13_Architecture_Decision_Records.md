# Architecture Decision Records (ADRs)

**Format:** MADR 3.0  
**Status Legend:** Proposed | Accepted | Deprecated | Superseded

---

## ADR-001: Adopt OpenTax as Tax Computation Engine

**Status:** Superseded by ADR-022  
**Date:** 2026-07-10

### Context
Need production-grade tax computation engine supporting ITR-1/2/3/4 with AY-versioned rules.

**Options Considered:**
1. Build custom tax engine from scratch
2. Use OpenTax (community OSS) **← original decision**
3. License commercial solution (ClearTax API)

### Original Decision (SUPERSEDED)
Adopt OpenTax tax_calculation module, vendored and wrapped in adapter layer.

### Superseding Decision (ADR-022)
Build our OWN tax engine, using OpenTax as reference implementation only.
We study OpenTax's logic and reimplement it ourselves operating on our
domain schedules directly. OpenTax is NOT called at runtime.

### Rationale for change
- OpenTax's `FilingModel` data structure doesn't match our domain model
- OpenTax reads salary from `salary_section_171/172/173` (ESOP fields),
  not from a simple `gross_salary` field
- The adapter layer (model_mapper → FilingModel → response_mapper) adds
  complexity, fragility, and a maintenance burden
- Our domain schedules already have `compute_income()` methods
- We need full control over AY versioning, explanation engine, and
  computation logic
- OpenTax will still be used for ITR JSON generation (Phase 7)

### Consequences (updated)
- We own 100% of production tax computation code
- OpenTax vendored code is reference + test oracle only
- No runtime dependency on OpenTax for tax computation
- Simpler data flow: Schedule → TaxEngine → ComputedReturn
- Explanation engine built natively (ADR-021)

---

## ADR-002: Hexagonal Architecture with DDD

**Status:** Accepted  
**Date:** 2026-07-10

### Context
System will grow to support ITR-1 through ITR-7, multiple import sources, future AI features.

### Decision
Use Hexagonal Architecture + DDD patterns with clear port/adapter separation.

### Rationale
- **Testability:** Core logic isolated from infrastructure
- **Flexibility:** Easy to swap OpenTax for alternative
- **Maintainability:** Clear boundaries between layers
- **Scalability:** Can extract bounded contexts later

### Consequences
**Positive:**
- OpenTax isolated behind ITaxEngine interface
- Database changes don't affect business logic
- Easy to add new parsers (IDocumentParser)

**Negative:**
- More boilerplate (interfaces, mappers)
- Steeper learning curve for junior developers
- Initial setup time higher

---

## ADR-003: Integer Paise for Money Storage

**Status:** Accepted  
**Date:** 2026-07-10

### Context
Tax computations require rupee-precision arithmetic without floating-point errors.

### Decision
Store all monetary values as integer paise (1 rupee = 100 paise).

### Rationale
- **Accuracy:** No floating-point rounding errors
- **Compliance:** CBDT requires exact rupee amounts
- **Compatibility:** OpenTax uses Decimal, convertible

### Consequences
**Positive:**
- Arithmetic always exact
- Database storage efficient (INT vs NUMERIC)
- No 0.30000000000000004 issues

**Negative:**
- Must convert at boundaries (UI, API, OpenTax)
- Developers must remember unit (paise not rupees)

**Example:**
```python
# Store
amount_paise = 1500000  # ₹15,000.00

# Display
amount_rupees = amount_paise / 100  # 15000.0

# OpenTax
opentax_amount = Decimal(amount_paise) / Decimal("100")
```

---

## ADR-004: PostgreSQL with SQLAlchemy ORM

**Status:** Accepted  
**Date:** 2026-07-10

### Context
Need ACID-compliant database with JSONB support for flexible schemas.

**Options Considered:**
1. PostgreSQL + SQLAlchemy
2. MongoDB
3. MySQL

### Decision
PostgreSQL 15+ with SQLAlchemy 2.0 async.

### Rationale
- **ACID:** Transactions critical for filing integrity
- **JSONB:** Flexible storage for ITR form data
- **Performance:** Excellent with proper indexes
- **Tooling:** Mature ecosystem (Alembic, pgAdmin)

### Consequences
**Positive:**
- Strong consistency guarantees
- Powerful query capabilities
- Great Python support

**Negative:**
- Vertical scaling limits (can mitigate with read replicas)
- JSONB queries less efficient than relational

---

## ADR-005: Next.js for Frontend

**Status:** Accepted  
**Date:** 2026-07-10

### Context
Existing React+Vite frontend; OpenTax uses Next.js. Need modern SSR-capable framework.

### Decision
Rebuild frontend in Next.js 16 (App Router).

### Rationale
- **SSR:** Better SEO, faster initial load
- **OpenTax UI:** Can reuse components directly
- **Developer Experience:** Better than Vite for large apps
- **Future:** Server actions, streaming

### Consequences
**Positive:**
- Reuse OpenTax filing form components
- Better performance (RSC, streaming)
- Modern patterns (server components)

**Negative:**
- Full rewrite required (2 weeks effort)
- Learning curve (App Router new)
- More complex deployment

---

## ADR-006: JWT Authentication with 30-Minute Expiry

**Status:** Accepted  
**Date:** 2026-07-10

### Context
Multi-user system needs secure, scalable authentication.

### Decision
JWT tokens with 30-minute expiry, refresh tokens for extended sessions.

### Rationale
- **Stateless:** No server-side session storage
- **Scalable:** Works across multiple servers
- **Secure:** Short expiry limits exposure

### Consequences
**Positive:**
- Horizontal scaling easy
- No session store needed

**Negative:**
- Cannot revoke tokens (until expiry)
- Must implement refresh token rotation

---

## ADR-007: Vendor OpenTax, Don't Fork Permanently

**Status:** Accepted (Updated)  
**Date:** 2026-07-10 (Updated 2026-07-15)

### Context
Need OpenTax code but want control over updates.

### Decision
Vendor OpenTax code into our repo, locked to specific commit. Update on our schedule.

**IMPORTANT UPDATE (2026-07-15):** Per ADR-022, the vendored OpenTax code is now **reference only, not a runtime dependency** for tax computation. It is used for:
1. Test oracle — verifying our engine matches within ₹1 tolerance
2. ITR JSON generation (Phase 7)
3. Validation rules reference (Phase 7)

It is NOT called in production for tax computation.

### Rationale
- **Stability:** No surprise breakage
- **Control:** Update when we're ready
- **Reliability:** Works offline, no external dependency

**Alternative Rejected:** Git submodule (too fragile), pip install (too dynamic)

### Consequences
**Positive:**
- Complete control over tax engine
- Can patch urgently if needed
- No external service dependency

**Negative:**
- Miss automatic OpenTax updates
- Must manually merge upstream changes
- Larger repo size

---

## ADR-008: Rule Engine with AY Versioning

**Status:** Accepted  
**Date:** 2026-07-10

### Context
Tax rules change yearly, must support multiple AYs simultaneously.

### Decision
Database-backed rule engine with immutable rules per AY.

### Rationale
- **Compliance:** Old filings use old rules (reproducible)
- **Flexibility:** Update current AY without touching past
- **Audit:** Clear trail of rule changes

### Consequences
**Positive:**
- Historical filings always reproducible
- A/B test rule changes safely
- Support future Act (2025 vs 2061)

**Negative:**
- More complex than hardcoded rules
- Must seed rules for each AY
- Storage overhead (rules duplicated per AY)

---

## ADR-009: Canonical Data Model for Imports

**Status:** Accepted  
**Date:** 2026-07-10

### Context
5+ import sources (AIS, 26AS, Form 16, Prefill, manual) with different schemas.

### Decision
Normalize all imports to single CanonicalIncomeData model before merging.

### Rationale
- **Consistency:** Single format for all sources
- **Deduplication:** Easier to detect duplicates
- **Extensibility:** New sources just need normalizer

### Consequences
**Positive:**
- Merge logic reusable
- Duplicate detection centralized
- Easy to add new import types

**Negative:**
- Extra transformation step
- Must maintain normalizers for each source

---

## ADR-010: Async Processing for Heavy Tasks

**Status:** Accepted  
**Date:** 2026-07-10

### Context
Document parsing, PDF generation take >5s, blocking API requests.

### Decision
Use Celery + Redis for async task queue.

### Rationale
- **Responsiveness:** API returns immediately
- **Scalability:** Horizontal scaling of workers
- **Reliability:** Task retry on failure

### Consequences
**Positive:**
- Better UX (no timeouts)
- Can handle spikes (queue buffers)

**Negative:**
- More infrastructure (Redis, workers)
- Complexity (task state management)

---

## ADR-011: No ORMs in Domain Layer

**Status:** Accepted  
**Date:** 2026-07-10

### Context
Clean Architecture requires domain layer independent of infrastructure.

### Decision
Domain entities are pure Python classes, no SQLAlchemy decorators.

### Rationale
- **Testability:** Domain logic tests don't need database
- **Flexibility:** Can swap ORM later
- **Clarity:** Business logic not mixed with persistence

### Consequences
**Positive:**
- Fast unit tests (no DB)
- Clear separation of concerns

**Negative:**
- Must map between domain entities and ORM models
- More boilerplate (repositories)

---

## ADR-012: Immutable Snapshots for Filings

**Status:** Accepted  
**Date:** 2026-07-10

### Context
Once filed, ITR cannot change (ITD compliance). Need audit trail.

### Decision
ComputedReturn is immutable aggregate, new computation = new snapshot.

### Rationale
- **Compliance:** Filed return never changes
- **Audit:** Complete history of computations
- **Diff:** Can compare versions

### Consequences
**Positive:**
- Perfect audit trail
- Reproducible computations
- Can restore any version

**Negative:**
- Storage grows (snapshot per computation)
- Must clean up old drafts

---

## ADR-013: Redis for Caching, Not Session Storage

**Status:** Accepted  
**Date:** 2026-07-10

### Context
Need caching for rules, master data. JWTs are stateless.

### Decision
Redis for cache only (LRU eviction), not for session tokens.

### Rationale
- **Simplicity:** JWTs don't need session store
- **Performance:** Cache hot data (rules, master data)
- **Scalability:** Cache miss just fetches from DB

### Consequences
**Positive:**
- Fast lookups (rules cached)
- No session state to manage

**Negative:**
- Cannot revoke JWTs before expiry
- Cache invalidation complexity

---

## ADR-014: Playwright for E2E Tests

**Status:** Accepted  
**Date:** 2026-07-10

### Context
Need reliable E2E tests for filing workflow.

**Options:** Cypress, Playwright, Selenium

### Decision
Playwright for E2E tests.

### Rationale
- **Speed:** Faster than Selenium
- **Reliability:** Better handling of async
- **Multi-browser:** Chrome, Firefox, Safari

### Consequences
**Positive:**
- Reliable E2E tests
- Good developer experience

**Negative:**
- Learning curve if team knows Cypress

---

## ADR-015: Feature Flags for Gradual Rollout

**Status:** Proposed  
**Date:** 2026-07-10

### Context
Want to release OpenTax integration gradually, rollback if issues.

### Decision
Environment-based feature flags (ENABLE_OPENTAX=true).

### Rationale
- **Safety:** Can disable if issues found
- **Gradual:** Enable for pilot clients first
- **A/B:** Compare OpenTax vs old system

### Consequences
**Positive:**
- Safe rollout
- Instant rollback

**Negative:**
- Code complexity (dual code paths)
- Must clean up after rollout

---

---

## ADR-016: ComputedReturn is Single Source of Truth

**Status:** Accepted  
**Date:** 2026-07-10

### Context
Multiple parts of system need tax computation results (ITR JSON, PDF, dashboard, analytics).

### Decision
ComputedReturn is the ONLY source of truth. Nothing else recomputes tax.

### Rationale
- **Consistency:** All consumers see identical numbers
- **Performance:** No redundant computation
- **Auditability:** Single immutable snapshot
- **Reproducibility:** Historical computations preserved

### Consequences
**Positive:**
- ITR JSON builder reads from ComputedReturn
- PDF generator reads from ComputedReturn
- Dashboard queries ComputedReturn table
- Analytics consume snapshots

**Negative:**
- Cannot recompute on-the-fly (must create new snapshot)
- Storage grows with each computation

**Enforcement:**
```python
# WRONG - recomputing
def generate_pdf(filing: Filing):
    tax = compute_engine.compute(filing, filing.regime)  # ❌
    return pdf_builder.build(tax)

# CORRECT - reading snapshot
def generate_pdf(filing_id: UUID):
    snapshot = snapshot_repo.get_latest(filing_id)  # ✅
    return pdf_builder.build(snapshot)
```

---

## ADR-017: OpenTax Never Accesses Database

**Status:** Accepted  
**Date:** 2026-07-10

### Context
OpenTax is vendored external code. Must remain database-agnostic.

### Decision
OpenTax shall NEVER import SQLAlchemy models or access database directly.

### Rationale
- **Portability:** Can swap OpenTax for alternative
- **Testability:** OpenTax logic testable without DB
- **Separation:** Clear adapter boundary

### Consequences
**Positive:**
- OpenTax remains pure computation
- Can unit test without database
- Easy to replace OpenTax if needed

**Negative:**
- Adapter layer maps ERP models ↔ OpenTax models
- Extra boilerplate

**Enforcement:**
```python
# WRONG - OpenTax imports ERP models
from infra.database.models import ClientModel  # ❌ in OpenTax code

# CORRECT - OpenTax only uses its own models
from .vendor.models.filing_model import FilingModel  # ✅

# Adapter handles mapping
class OpenTaxAdapter:
    def compute_tax(self, filing: Filing):
        opentax_model = self.mapper.to_opentax(filing)  # ✅
        return self.tax_service.calculate(opentax_model)
```

---

## ADR-018: Deterministic Computation (Pure Functions)

**Status:** Accepted  
**Date:** 2026-07-10

### Context
Tax computations must be reproducible for audits, debugging, testing.

### Decision
Every computation is deterministic. No side effects.

### Rationale
- **Reproducibility:** Same input → same output
- **Testability:** No mocks needed
- **Auditability:** Can replay computation

### Banned Inside Computation:
```python
# ❌ BANNED
datetime.now()           # Non-deterministic
random.random()          # Non-deterministic
os.getenv()             # Environment-dependent
requests.get()          # I/O operation
db.query()              # Database access
redis.get()             # External state
```

### Allowed:
```python
# ✅ ALLOWED
def compute_tax(
    income: Decimal,        # Pure input
    regime: str,
    ay: str,
    rules: RuleVersion      # Explicitly passed
) -> Decimal:
    # Pure computation
    return income * rules.get_rate()
```

### Consequences
**Positive:**
- Tests don't need mocks
- Can replay any computation
- Easy debugging (no hidden state)

**Negative:**
- Must pass all data explicitly
- No shortcuts (no global config)

---

## ADR-019: Schedule-Based Architecture

**Status:** Accepted  
**Date:** 2026-07-10

### Context
ITR forms (ITR-1 through ITR-7) are composed of reusable schedules.

### Decision
Model schedules as first-class domain entities. ITR builders consume schedules.

### Rationale
- **Reusability:** ScheduleSalary used in all ITRs
- **Extensibility:** Adding ITR-2 reuses ITR-1 schedules
- **Clarity:** ITR structure mirrors CBDT schema
- **Testability:** Test schedules independently

### Structure:
```python
# Filing contains schedules
filing = Filing(
    schedules={
        "ScheduleSalary": ScheduleSalary(...),
        "ScheduleHP": ScheduleHP(...),
        "ScheduleTDS": ScheduleTDS(...)
    }
)

# ITR builder consumes schedules
itr1_json = ITR1Builder().build(
    schedules=filing.schedules.values(),
    computed=computed_return
)
```

### Consequences
**Positive:**
- ITR-2/3/4 easy to add (same schedules)
- Each schedule self-contained
- Clear mapping to ITD schema

**Negative:**
- Refactor existing Filing entity
- Migration needed for current data

---

## ADR-020: Separate Tax Rules, Schema Rules, Validation Rules

**Status:** Accepted  
**Date:** 2026-07-10

### Context
Current rule engine conflates tax computation rules, ITR schema versions, and validation rules.

### Decision
Split into three independent systems:
1. **TaxRules:** Slabs, rates, deduction limits
2. **SchemaRules:** ITR JSON structure per AY
3. **ValidationRules:** CBDT checks per ITR type

### Rationale
- **Change Frequency:** Schema changes yearly, tax rules change rarely
- **Versioning:** Each needs independent versioning
- **Separation:** Different concerns, different storage

### Structure:
```python
# Separate repositories
tax_rules = TaxRuleRepository().get(ay="2026-27")
schema = SchemaRepository().get(itr_type="ITR1", ay="2026-27")
validators = ValidationRuleRepository().get(itr_type="ITR1", ay="2026-27")

# Used independently
tax = compute_tax(income, tax_rules)
itr_json = build_itr(schedules, schema)
errors = validate_itr(itr_json, validators)
```

### Consequences
**Positive:**
- Schema updates don't touch tax logic
- Validation rules evolve independently
- Clear ownership (tax team vs schema team)

**Negative:**
- Three separate tables
- More complex seeding

---

## ADR-021: Explanation Engine for Computation Traceability

**Status:** Accepted  
**Date:** 2026-07-10

### Context
CAs and clients ask "Why is tax ₹1,17,000?" System currently returns only final number.

### Decision
Capture computation trace with every calculation.

### Structure:
```python
@dataclass
class ComputationStep:
    step: str
    formula: str
    value: Decimal
    
computed_return = ComputedReturn(
    total_tax=117000,
    explanation=[
        ComputationStep("Gross Salary", "1,500,000", 1500000),
        ComputationStep("Standard Deduction", "-75,000", -75000),
        ComputationStep("Gross Total Income", "1,500,000 - 75,000", 1425000),
        ComputationStep("Taxable Income", "1,425,000 - 0", 1425000),
        ComputationStep("Slab Tax", "0 + 20,000 + 62,500 + 30,000", 112500),
        ComputationStep("Health & Edu Cess", "112,500 × 4%", 4500),
        ComputationStep("Total Tax", "112,500 + 4,500", 117000),
    ]
)
```

### Rationale
- **Transparency:** Show how tax was computed
- **Debugging:** Identify computation errors quickly
- **Client Communication:** Explain tax to clients
- **Notice Response:** Trace discrepancies

### Consequences
**Positive:**
- CA can verify computation logic
- Easier to debug incorrect results
- Better client trust

**Negative:**
- Extra storage per snapshot
- Computation engine must capture trace

---

## ADR-022: Owned Tax Engine (Not OpenTax Runtime Dependency)

**Status:** Accepted  
**Date:** 2026-07-15  
**Deciders:** Tech Lead, Product Owner  
**Supersedes:** ADR-001 (OpenTax as tax computation engine)

### Context
Phase 1 vendored OpenTax code. Phase 2 planned to use it as runtime dependency via adapter/mapper pattern. After detailed analysis, the OpenTax `FilingModel` data structure fundamentally mismatches our domain model. The adapter layer would be fragile, hard to maintain, and OpenTax reads salary from ESOP fields we don't use.

### Decision
Build our OWN tax computation engine that operates directly on our domain schedules (`ScheduleSalary`, `ScheduleHP`, etc.). OpenTax vendored code is retained as **reference implementation and test oracle only** — never called at runtime for tax computation.

### Rationale
1. **Data model mismatch:** OpenTax reads salary from `salary_section_171/172/173` (ESOP/perquisite), not `gross_salary`. Our ERP has `SalaryDetail.gross_salary` — no natural mapping exists.
2. **Our schedules already compute:** `ScheduleSalary.compute_income()`, `ScheduleHP.compute_income()`, `ScheduleVIA.total_deductions()` already exist and work.
3. **Full control:** We own AY versioning, explanation engine, computation logic. No external dependency.
4. **Simpler data flow:** `Schedule → TaxEngine → ComputedReturn` instead of `Schedule → Mapper → FilingModel → OpenTax → FilingModel → Mapper → ComputedReturn`.
5. **Testability:** Compare our engine vs OpenTax oracle within ₹1 tolerance.
6. **Explanation engine:** Every computation produces `List[ComputationStep]` for CA audit trail.

### What We Build Ourselves
| Component | Description |
|-----------|-------------|
| `core/services/slab_tables.py` | AY-versioned CBDT slab rates, deduction limits, surcharge thresholds |
| `core/services/tax_engine.py` | Owned computation engine operating on our domain schedules |
| `core/services/interest_234_engine.py` | Interest u/s 234A/B/C (reimplemented from OpenTax algorithm) |
| `core/domain/computed_return.py` | Updated with `TaxBreakdown`, dual-regime fields |

### What We Keep from OpenTax (Reference Only)
| Component | Usage |
|-----------|-------|
| `vendor/filing/tax_calculation/tax_calculation_service.py` | Test oracle — verify our engine matches within ₹1 |
| `vendor/filing/itr/itr_building_orchestrator.py` | ITR JSON generation (Phase 7) |
| `vendor/filing/itr/validations/tax_validation_service.py` | Validation rules reference (Phase 7) |
| `vendor/filing/models/` | Pydantic model schemas for ITR JSON (Phase 7) |

### What We Delete
| File | Reason |
|------|--------|
| `app/adapters/opentax/model_mapper.py` | No longer map to OpenTax FilingModel |
| `app/adapters/opentax/response_mapper.py` | No longer map from OpenTax response |

### Consequences
**Positive:**
- Full ownership of core business logic
- Simpler data flow (no mapper layer)
- Native explanation engine (ADR-021)
- Both regimes always computed for comparison
- No runtime dependency on external code
- AY versioning controlled by our `slab_tables.py`

**Negative:**
- Must reimplement interest 234A/B/C logic (~35KB from OpenTax)
- Must maintain slab tables for new AY rules
- Must keep OpenTax test oracle in sync

### Enforcement
```python
# WRONG — importing OpenTax in production code
from app.adapters.opentax.vendor.filing.tax_calculation_service import TaxCalculationService  # ❌

# CORRECT — using our owned engine
from app.core.services.tax_engine import TaxEngine  # ✅

# ACCEPTABLE — using OpenTax as test oracle in test files only
from app.adapters.opentax.vendor.filing.tax_calculation_service import TaxCalculationService  # ✅ (in tests/)

# ACCEPTABLE — using OpenTax for ITR JSON generation (Phase 7)
from app.adapters.opentax.vendor.filing.itr.itr_building_orchestrator import ItrBuildingOrchestrator  # ✅ (Phase 7)
```

---

## ADR-023: Explanation Engine for Every Computation

**Status:** Accepted  
**Date:** 2026-07-15

### Context
CAs and clients ask "Why is tax ₹1,17,000?" The system currently returns only final numbers. ADR-021 mentioned this conceptually but did not specify implementation requirements.

### Decision
Every tax computation method MUST produce a `List[ComputationStep]` explaining each calculation. This is a hard requirement, not optional.

### Structure
```python
@dataclass
class ComputationStep:
    step: str          # "salary_income", "standard_deduction", "slab_tax"
    description: str   # "Gross salary from Tech Corp"
    input_value: str   # "₹15,00,000"
    output_value: str  # "₹14,25,000"
    rule_applied: str  # "New regime standard deduction ₹75,000 u/s 16(ia)"
```

### Example Output
```
Step 1:  salary_income         | Gross salary from Tech Corp       | ₹15,00,000
Step 2:  standard_deduction      | New regime u/s 16(ia)           | -₹75,000
Step 3:  gross_total_income     | ₹15,00,000 - ₹75,000            | ₹14,25,000
Step 4:  total_deductions       | New regime: 80CCD(2) only        | ₹0
Step 5:  total_income           | ₹14,25,000 - ₹0                  | ₹14,25,000
Step 6:  slab_tax               | See slab breakdown                | ₹93,750
Step 7:  rebate_87a             | Tax > ₹60,000, no rebate         | ₹0
Step 8:  surcharge              | Income < ₹50L, no surcharge       | ₹0
Step 9:  health_education_cess  | ₹93,750 × 4%                     | ₹3,750
Step 10: total_tax_liability    | ₹93,750 + ₹0 + ₹3,750           | ₹97,500
Step 11: tds_credit             | TDS from employer                 | -₹1,50,000
Step 12: tax_payable             | ₹97,500 < ₹1,50,000             | ₹0
Step 13: refund                 | ₹1,50,000 - ₹97,500              | ₹52,500
```

### Consequences
**Positive:**
- CA audit trail for every computation
- Client-facing explanation page
- Debugging incorrect results
- Regulatory compliance (CBDT expects explanation)

**Negative:**
- Extra storage per snapshot (~2KB per computation)
- Computation engine must track every step

---

## ADR-024: Single Regime Computation with On-Demand Comparison

**Status:** Accepted  
**Date:** 2026-07-15

### Context
The UI needs to show both regimes for comparison, but computing both for every request is wasteful if the user only views one.

### Decision
Compute the requested regime by default. Provide a separate endpoint/method to compute the other regime on demand. The UI offers a "Compare Regimes" button that calls the comparison endpoint.

### Rationale
- **Performance:** Computing both regimes doubles computation time
- **UX:** Most users view one regime at a time
- **Flexibility:** Comparison available when needed

### Consequences
**Positive:**
- Faster default computation (single regime)
- Comparison available on demand
- Simpler API

**Negative:**
- Two API calls for comparison
- Must ensure both computations use identical inputs

**Total ADRs:** 21  
**Status:** Living document, updated as decisions made  
**Last Major Update:** 2026-07-10 (Architecture Review)
