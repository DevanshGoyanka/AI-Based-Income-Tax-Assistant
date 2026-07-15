# Repository Audit Report

**Document Version:** 1.0  
**Date:** 2026-07-10  
**Audited Repositories:**
- ITR-FilingWebsite-main (Custom ERP)
- OpenTax (Community OSS)

---

## 1. ITR-FilingWebsite-main Audit

### 1.1 Backend Structure

```
backend/app/
├── api/           # REST endpoints
├── domain/        # Business entities
├── infra/         # Database, external services
├── schemas/       # Pydantic DTOs
└── services/      # Business logic, parsers
```

### 1.2 Critical Issues Found

#### 🔴 CRITICAL: Duplicate AIS Decryption Logic

**Location:**
- `backend/app/services/ais_decrypt.py` (1000 iterations)
- `backend/app/services/importers/crypto.py` (100,000 iterations)

**Impact:** Incompatible implementations will fail on same encrypted files. AIS files encrypted with ITD's reference implementation use 100,000 PBKDF2 iterations.

**Evidence:**
```python
# ais_decrypt.py (WRONG)
key = PBKDF2(password, salt, dkLen=32, count=1000)

# importers/crypto.py (CORRECT)
key = PBKDF2(password, salt, dkLen=32, count=100000)
```

**Recommendation:** Delete `ais_decrypt.py`, use only `importers/crypto.py`

#### 🔴 CRITICAL: Missing Foreign Key Constraints

**Location:** All migrations in `backend/migrations/versions/`

**Impact:** No referential integrity at database level. Orphaned records possible.

**Evidence:**
```python
# clients table
assigned_user_id = Column(UUID, nullable=True)  # No FK to users(id)
created_by = Column(UUID, nullable=True)        # No FK to users(id)

# itr_filings table
client_id = Column(UUID, nullable=False)        # No FK to clients(id)
created_by = Column(UUID, nullable=True)        # No FK to users(id)
```

**Recommendation:** Add FK constraints in new migration, cascade delete rules

#### 🟢 RESOLVED: form26as.py Split (Hexagonal Architecture)

**Status:** RESOLVED — The codebase now uses a clean hexagonal structure:
- `Form26ASZipParser` (in `core/parsers/form26as_zip_parser.py`) handles password-protected ZIP files containing all 10 parts via `Canonical26AS` domain model
- `Form26ASPDFParser` (in `core/parsers/form26as_pdf_parser.py`) handles PDF extraction for Part I, II, VI, VII
- `Form26ASNormalizer` (in `core/normalizers/form26as_normalizer.py`) converts `Canonical26AS` to `CanonicalTDS` / `CanonicalAdvanceTax`
- `ImportForm26ASUseCase` (in `core/use_cases/import_form26as.py`) orchestrates the flow
- `Canonical26AS` (in `core/domain/canonical_26as.py`) domain model holds all 10 parts

All 10 parts of Form 26AS (Part I–X) are handled in a single `Canonical26AS`
domain model. No separate part-specific files needed. This is cleaner than
the original recommendation of splitting into 10 separate parsers.
├── part5_parser.py  # Self-assessment tax
└── common.py        # Shared utilities
```

#### 🟡 MEDIUM: No Foreign Key on prefill_data.id

**Location:** `backend/migrations/versions/9976106a1337_add_prefill_data_table.py`

**Issue:**
```python
id = sa.Column(sa.UUID(), nullable=False)  # No server_default
```

**Impact:** Will error if application passes None for id.

**Recommendation:** Add `server_default=sa.text("gen_random_uuid()")`

### 1.3 Dead Code Detected

**Test/Debug Scripts (18 files in backend/):**
- `comprehensive_ais_test.py`
- `debug_ais.py`
- `debug_mf_parse.py`
- `debug_pdf.py`
- `debug_tables.py`
- `examine_ais.py`
- `examine_all_sections.py`
- `examine_full_ais.py`
- `examine_sft.py`
- `manual_ais_test.py`
- `standalone_ais_test.py`
- `test_ais_pdf.py`
- `test_info_items.py`
- `test_parser.py`

**Recommendation:** Move to `backend/tests/manual/` or delete if obsolete

### 1.4 Database Schema Issues

#### Missing Constraints

| Table | Missing Constraint | Impact |
|-------|-------------------|--------|
| `form_26as_data` | UNIQUE(client_id, ay) | Duplicate imports possible |
| `ais_data` | UNIQUE(client_id, ay, source) | Duplicate imports possible |
| `tis_data` | UNIQUE(client_id, ay) | Duplicate imports possible |
| `prefill_data` | UNIQUE(client_id, ay) | Duplicate imports possible |
| `loss_ledger` | UNIQUE(client_id, head, source_ay) | Duplicate loss entries |

#### Missing Indexes

| Table | Missing Index | Query Pattern |
|-------|--------------|---------------|
| `computed_return_snapshots` | (client_id, ay) | Find snapshot by client+AY |
| `audit_trail` | (entity, entity_id) | Entity-scoped audit queries |
| `itr_form_data` | UNIQUE(filing_id, head_type) | Prevent duplicate heads |

### 1.5 Frontend Issues

#### 🔴 CRITICAL: No Auth Guard on Routes

**Impact:** Unauthenticated users can navigate to `/filing` or `/clients` directly.

**Evidence:** `frontend/src/App.tsx` has no ProtectedRoute wrapper.

**Recommendation:**
```tsx
<Route element={<ProtectedRoute />}>
  <Route path="/dashboard" element={<DashboardPage />} />
  <Route path="/clients" element={<ClientMasterPage />} />
  <Route path="/filing/:id" element={<ITRFilingPage />} />
</Route>
```

#### 🔴 CRITICAL: Token Expiry Not Handled

**Location:** `frontend/src/api/tokenManager.ts`

**Issue:** JWT stored but never checked for expiry. Stale tokens cause silent API failures.

**Recommendation:** Add axios interceptor to detect 401, refresh token or redirect to login.

#### 🟠 HIGH: Hardcoded API URLs

**Locations:**
- `frontend/src/api/clientApi.ts`: `http://localhost:8080/api`
- `frontend/src/hooks/usePhase2.ts`: `http://localhost:8080/api`

**Recommendation:** Use `import.meta.env.VITE_API_BASE_URL`

#### 🟡 MEDIUM: Missing useDebounce Hook

**Location:** `frontend/src/pages/ClientMasterPage.tsx`

**Issue:** Imports `useDebounce` but file doesn't exist.

**Recommendation:** Implement or remove import.

### 1.6 Services Layer Duplication

| Service | Location 1 | Location 2 | Issue |
|---------|-----------|-----------|-------|
| AIS parsing | `services/ais_parser.py` | `services/ais_pdf_parser.py` | Split logic |
| 26AS parsing | `form26as_zip_parser.py` + `form26as_pdf_parser.py` | `form26as_normalizer.py` | Hexagonal structure ✅ |
| Decryption | `services/ais_decrypt.py` | `services/importers/crypto.py` | Incompatible |

**Recommendation:** Consolidate to single implementation per concern.

---

## 2. OpenTax Audit

### 2.1 Architecture Strengths

✅ **Clean Separation of Concerns:**
```
api/filing/
├── tax_calculation/    # Pure computation (no I/O)
├── itr/               # ITR JSON building
├── models/            # Pydantic schemas
├── utils/             # Helpers
└── filing_controller.py  # API endpoints
```

✅ **Type Safety:** All Python with strict type hints, all TypeScript with strict mode.

✅ **Deterministic Tax Logic:** No side effects in computation, testable.

✅ **Comprehensive Pydantic Models:** 60+ models covering all ITR-1 sections.

### 2.2 What Can Be Reused (Vendor as-is)

#### Tax Computation Engine (100%)
**Location:** `api/filing/tax_calculation/tax_calculation_service.py` (1800+ lines)

**Reusable Methods:**
- `calc_slab_breakdown_old_regime(total_income, age)`
- `calc_slab_breakdown_new_regime_by_ay(total_income, ay)`
- `_compute_surcharge_amount(age, regime, tax, income, ay)`
- `_compute_rebate_amount(regime, slab_tax, taxable_income)`
- `vba_round(val)` (ROUND_HALF_EVEN for ITD compliance)

**Constants (All Production-Ready):**
```python
OLD_STANDARD_DEDUCTION = 50000
NEW_STANDARD_DEDUCTION = 75000
HEALTH_EDU_CESS_RATE = 0.04
NEW_REBATE_TAX_LIMIT = 60000
NEW_REBATE_INCOME_LIMIT = 1200000
OLD_REBATE_TAX_LIMIT = 12500
STCG_111A_PRE_JUL23_RATE = 15
STCG_111A_POST_JUL23_RATE = 20
LTCG_112A_PRE_JUL23_RATE = 10
LTCG_112A_POST_JUL23_RATE = 12.5
LTCG_112A_EXEMPTION = 125000
```

#### Chapter VI-A Deduction Logic (100%)
**Reusable Methods:**
- `ded_80cce(req)` (80C+80CCC+80CCD1 combined cap)
- `ded_80ccd1b(req)` (NPS ₹50K)
- `ded_80ccd2(req)` (Employer NPS)
- `ded_80d(req)` (Health insurance with senior citizen caps)
- `ded_80dd(req)` (Disability)
- `ded_80ddb(req, age)` (Medical expenditure age-based)
- `ded_80u(req)` (Self disability)
- `ded_80e(req)` (Education loan)
- `ded_80ee/eea/eeb(req)` (Home loan variants)
- `ded_80g(req, adjusted_gti)` (Donations with limit logic)
- `ded_80gg(req, adjusted_gti)` (Rent paid)
- `ded_80ggc/gga(req)` (Political, scientific)
- `ded_interest_savings(req, age)` (80TTA/TTB mutual exclusivity)

#### Interest Computation (100%)
**Location:** `api/filing/tax_calculation/interest_234_service.py`

**Reusable:** Complete 234A/B/C and 234F computation per ITD rules.

#### ITR-1 JSON Builder (80%)
**Location:** `api/filing/itr/itr1/itr1_building_service.py` (943 lines)

**Reusable Sections:**
- Personal info mapping
- Bank account structure
- TDS/TCS schedule builders
- Verification schema
- Filing status enum mappings

**Needs Adaptation:**
- No database persistence (expects stateless FilingModel)
- No multi-client context

#### Validation Engine (90%)
**Location:** `api/filing/itr/validations/tax_validation_service.py`

**Reusable:** 100+ validation rules with ITD error code mapping.

**Needs Adaptation:** Return format to match ERP ValidationReport schema.

### 2.3 What Cannot Be Reused (Build Custom)

#### No Database Layer
OpenTax has ZERO persistence. All state is ephemeral.

**Missing:**
- SQLAlchemy models
- Alembic migrations
- CRUD repositories
- Audit trails
- Snapshot versioning

**Action:** Build complete database layer in ERP, map to OpenTax models via adapter.

#### No Authentication
OpenTax is single-user stateless.

**Missing:**
- JWT auth
- User/role management
- Session management
- Multi-tenant isolation

**Action:** Keep existing ERP auth, add user_id to FilingModel adapter.

#### No Document Upload/Parsing
OpenTax has no file handling.

**Missing:**
- AIS/26AS/Form 16 parsers
- PDF decryption
- OCR fallback
- Document storage

**Action:** Keep existing ERP parsers, map output to OpenTax FilingModel.

#### Frontend is Next.js (ERP is React+Vite)
OpenTax uses Next.js 16 + React 19 + App Router.

**Incompatibility:** Cannot directly merge with Vite-based SPA.

**Action:** Rebuild frontend in Next.js OR extract OpenTax UI components as standalone React library.

### 2.4 OpenTax Frontend Audit

**Location:** `OPENTAX/web/app/filing/`

**Structure:**
```
filing/
├── components/        # Income entry forms
│   ├── PersonalDetailsTab.tsx
│   ├── IncomeTab.tsx
│   ├── DeductionsTab.tsx
│   ├── TaxPaidTab.tsx
│   └── SummaryTab.tsx
├── context/FilingContext.tsx  # React Context state
├── api/filing-api.ts         # API client
└── page.tsx                  # Main filing page
```

**Strengths:**
- Clean tab-based UI
- Form validation at field level
- "Fill Test Data" for development
- Assessment year dropdown
- Regime toggle (old/new)

**Weaknesses:**
- No form library (raw useState)
- No client selection (single user)
- No save-as-draft flow
- No filing history

**Recommendation:** Extract components, rebuild state management with React Hook Form.

### 2.5 OpenTax Missing Features

| Feature | Status | Priority |
|---------|--------|----------|
| ITR-2 | Not implemented | High |
| ITR-3 | Not implemented | High |
| ITR-4 | Not implemented | Medium |
| Real estate capital gains | Not implemented | High |
| Business income (44AD/44ADA) | Not implemented | High |
| Multiple house properties | Not implemented | Medium |
| Sec 112 securities (bonds/unlisted) | Partial (triggers ITR-2 but builder missing) | Medium |
| Form 16 import | Not implemented | High |
| AIS/26AS import | Not implemented | High |
| PDF generation | Not implemented | High |
| Bulk client import | Not implemented | High |

---

## 3. Integration Complexity Analysis

### 3.1 Adapter Layer Effort Estimate

| Component | Lines of Code | Complexity | Effort (Hours) |
|-----------|--------------|------------|----------------|
| ERP models → OpenTax FilingModel | 300 | Medium | 12 |
| OpenTax response → ERP ComputedReturn | 200 | Low | 6 |
| Chapter VI-A deduction mapper | 400 | Medium | 16 |
| Income head mappers (Salary/HP/CG/OS) | 500 | High | 24 |
| TDS/TCS mapper | 150 | Low | 4 |
| Validation result mapper | 100 | Low | 3 |
| **Total Adapter Layer** | **1650** | | **65** |

### 3.2 Database Migration Effort

| Task | Effort (Hours) |
|------|----------------|
| Add missing foreign keys | 4 |
| Add missing unique constraints | 3 |
| Add missing indexes | 2 |
| Fix prefill_data.id default | 1 |
| Create snapshot_metadata table | 3 |
| Migration testing | 4 |
| **Total DB Migration** | **17** |

### 3.3 Code Cleanup Effort

| Task | Effort (Hours) |
|------|----------------|
| Remove duplicate AIS decrypt | 1 |
| Split form26as.py into modules | 8 |
| Move test scripts to tests/ | 2 |
| Consolidate parser logic | 6 |
| Add missing frontend hooks | 3 |
| Fix hardcoded API URLs | 1 |
| Add auth guards | 4 |
| **Total Cleanup** | **25** |

### 3.4 OpenTax Vendoring Effort

| Task | Effort (Hours) |
|------|----------------|
| Fork OpenTax tax_calculation/ | 2 |
| Vendor as Python package | 4 |
| Lock to specific commit SHA | 1 |
| Write facade layer | 8 |
| Integration tests | 12 |
| **Total Vendoring** | **27** |

### 3.5 Total Integration Effort

| Phase | Effort (Hours) |
|-------|----------------|
| Repository Cleanup | 25 |
| Database Migration | 17 |
| OpenTax Vendoring | 27 |
| Adapter Layer | 65 |
| Integration Testing | 20 |
| **Total** | **154** |

**Timeline:** 4-5 weeks with 1 developer (35-40 hours/week)

---

## 4. Recommended Actions (Priority Order)

### Phase 0: Critical Fixes (Week 1) — ✅ COMPLETE

1. **Delete duplicate AIS decrypt** ✅ (1 hour) — `ais_decrypt.py` removed, no stale imports
2. **Add missing foreign keys** ✅ (4 hours) — Migration `0005_add_missing_foreign_keys.py`
3. **Fix prefill_data.id** ✅ (1 hour) — Migration `0004_fix_prefill_id_default.py`
4. **Add unique constraints** ✅ (2 hours) — Migration `0006_add_unique_constraints_and_indexes.py`
5. **Split form26as** ✅ — Already in hexagonal structure, no part-specific split needed
6. **Add .env.example** ✅ — Root `.env.example` exists
7. **Update .gitignore** ✅ — Vendor code is tracked in git

### Phase 1: OpenTax Vendoring (Week 2) — ✅ COMPLETE

5. **Fork and vendor OpenTax** ✅ (6 hours)
   - Fork OpenTax repository
   - Extract tax_calculation/ as standalone package
   - Lock to commit SHA (added to requirements.txt)

6. **Build adapter layer** → Changed to Owned Engine per ADR-022
   - Original plan: Create `model_mapper.py` and `response_mapper.py` to map ERP → OpenTax
   - New plan: Build our OWN `TaxEngine` that operates on our domain schedules directly
   - OpenTax is reference/test oracle only, NOT a runtime dependency
   - `model_mapper.py` and `response_mapper.py` will be DELETED

7. **Integration tests** — To be implemented in Phase 2 comparing our engine vs OpenTax oracle

### Phase 2: Code Consolidation → Owned Tax Engine (Weeks 3-5)

8. **Build owned TaxEngine** (replaces adapter layer)
   - Create `core/services/slab_tables.py` (AY-versioned CBDT rules)
   - Create `core/services/tax_engine.py` (owned computation engine)
   - Create `core/services/interest_234_engine.py` (234A/B/C logic)
   - Delete `model_mapper.py` and `response_mapper.py` (no longer needed)
   - Rewrite `tax_engine_adapter.py` to delegate to owned engine
   - Update `computed_return.py` with `TaxBreakdown`
   - Update `ITaxEngine` interface with `person_dob`/`person_pan`
   - See `10_Phase_Implementation_Plan.md` for full details

9. ~~Move test scripts~~ — Already clean, no debug scripts in root
10. ~~Fix frontend issues~~ — Deferred to Phase 8 (frontend rebuild)

---

## 5. Risk Register

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| OpenTax breaking changes | Medium | High | Pin to commit SHA, maintain fork |
| Adapter performance overhead | Low | Medium | Profile early, optimize hot paths |
| Database migration failure | Low | Critical | Blue-green deployment, test with production copy |
| Foreign key addition blocks writes | Medium | High | Add FKs with NOT VALID, then validate async |
| ITR JSON validation fails | Low | Critical | Test against ITD schema validator before pilot |

---

## Appendix A: File Inventory

### ITR-FilingWebsite-main
- **Total Files:** 487
- **Backend Python:** 143 files
- **Frontend TypeScript:** 87 files
- **Tests:** 34 files
- **Migrations:** 2 files
- **Dead Code:** 18 debug scripts

### OpenTax
- **Total Files:** 312
- **Backend Python:** 89 files
- **Frontend TypeScript:** 145 files
- **Tests:** 0 (no test suite found)
- **Documentation:** 5 markdown files

---

**Audit Status:** COMPLETE  
**Next Action:** Executive approval for Phase 0 cleanup  
**Approval Date:** 2026-07-10
