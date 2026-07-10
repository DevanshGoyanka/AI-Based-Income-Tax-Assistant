# Phase-wise Implementation Plan

**Version:** 1.0  
**Timeline:** 10 weeks  
**Team Size:** 2-3 developers

---

## Phase 0: Repository Cleanup (Week 1)

**Objective:** Fix critical issues, remove dead code, prepare for integration.

**Tasks:**
1. Delete duplicate AIS decryption (`ais_decrypt.py`)
2. Move 18 debug scripts to `tests/manual/` or delete
3. Split `form26as.py` into part-specific modules
4. Add `.env.example` with all required variables
5. Update `.gitignore` for vendor code

**Files to Delete:**
- `backend/app/services/ais_decrypt.py`
- `backend/comprehensive_ais_test.py`
- `backend/debug_*.py` (all 8 files)
- `backend/examine_*.py` (all 4 files)
- `backend/manual_*.py` (all 3 files)
- `backend/standalone_*.py`
- `backend/test_ais_pdf.py`
- `backend/test_info_items.py`
- `backend/test_parser.py`

**Files to Move:**
- `backend/app/services/form26as.py` → split into `adapters/parsers/form26as/part1_parser.py` through `part10_parser.py`

**Database Migrations:**
```bash
# Create migrations
alembic revision --autogenerate -m "0003_add_foreign_keys"
alembic revision --autogenerate -m "0004_fix_prefill_id_default"
alembic revision --autogenerate -m "0005_add_unique_constraints"

# Review and apply
alembic upgrade head
```

**Testing:**
- Run existing test suite
- Verify all imports resolve
- Check database constraints

**Definition of Done:**
- Zero dead code in repository
- All migrations applied successfully
- All tests pass
- No duplicate logic

---

## Phase 1: OpenTax Vendoring (Week 2)

**Objective:** Vendor OpenTax tax engine as isolated module.

**Tasks:**
1. Fork OpenTax repository
2. Copy `api/filing/tax_calculation/` to `backend/app/adapters/opentax/vendor/`
3. Copy `api/filing/itr/` to vendor
4. Copy `api/filing/models/` to vendor
5. Create `__version__.py` with commit SHA
6. Update requirements.txt with dependencies
7. Create facade module for clean imports

**Directory Structure:**
```
backend/app/adapters/opentax/
├── __init__.py
├── __version__.py
├── vendor/
│   ├── __init__.py
│   ├── tax_calculation/
│   │   ├── tax_calculation_service.py
│   │   ├── interest_234_service.py
│   │   └── models/
│   ├── itr/
│   │   ├── itr_building_orchestrator.py
│   │   ├── itr1/
│   │   └── validations/
│   └── models/
│       └── filing_model.py
```

**Testing:**
- Import vendored modules
- Run OpenTax's own tests (if any)
- Verify no breaking changes

**Definition of Done:**
- OpenTax code vendored and importable
- No external OpenTax dependencies
- Version locked to specific commit
- Imports work without modification

---

## Phase 2: Adapter Layer (Weeks 3-4)

**Objective:** Build adapter between ERP and OpenTax.

**Tasks:**

**Week 3: Model Mappers**
1. Create `ERPToOpenTaxMapper` class
2. Map Client → PersonModel
3. Map Filing income heads → OpenTax models
4. Map TDS/TCS records
5. Unit tests for each mapper

**Week 4: Response Mappers**
1. Create `OpenTaxToERPMapper` class
2. Map TaxRegimeBreakdownModel → ComputedReturn
3. Map validation errors
4. Integration tests

**Files to Create:**
- `backend/app/adapters/opentax/tax_engine_adapter.py`
- `backend/app/adapters/opentax/model_mapper.py`
- `backend/app/adapters/opentax/response_mapper.py`
- `tests/unit/adapters/opentax/test_model_mapper.py`
- `tests/unit/adapters/opentax/test_response_mapper.py`

**Testing:**
- Round-trip test: ERP → OpenTax → ERP
- Golden test cases (5 scenarios)
- Performance test (<100ms per mapping)

**Definition of Done:**
- All mappers tested
- Zero data loss in round-trip
- Type-safe mappings
- 90%+ test coverage

---

## Phase 3: Canonical Data Model (Week 5)

**Objective:** Normalize all import sources to single model.

**Tasks:**
1. Define `CanonicalIncomeData` structure
2. Create `CanonicalNormalizer` service
3. Implement normalizers for:
   - AIS JSON
   - AIS PDF
   - Form 26AS TXT
   - Form 26AS PDF
   - Prefill JSON
4. Implement merge logic with duplicate detection
5. Integration tests with real documents

**Files to Create:**
- `backend/app/services/canonical/canonical_income.py`
- `backend/app/services/canonical/canonical_deduction.py`
- `backend/app/services/canonical/normalizer.py`
- `tests/integration/test_canonical_normalizer.py`

**Testing:**
- Test each normalizer with sample documents
- Test merge logic (detect duplicates)
- Verify all sources map correctly

**Definition of Done:**
- All 5 import sources normalized
- Duplicate detection works
- Data integrity maintained
- Integration tests pass

---

## Phase 4: Core Domain Layer (Week 6)

**Objective:** Implement domain entities and use cases.

**Tasks:**
1. Refactor existing models to domain entities
2. Create value objects (PAN, Money, AssessmentYear)
3. Implement Filing aggregate
4. Implement ComputedReturn aggregate
5. Define repository interfaces
6. Implement use cases:
   - ComputeTaxUseCase
   - GenerateITRJsonUseCase
   - ImportAIS26ASUseCase

**Files to Create:**
- `backend/app/core/domain/value_objects/pan.py`
- `backend/app/core/domain/value_objects/money.py`
- `backend/app/core/domain/filing.py`
- `backend/app/core/domain/computed_return.py`
- `backend/app/core/interfaces/repositories.py`
- `backend/app/core/interfaces/tax_engine.py`
- `backend/app/core/use_cases/filing/compute_tax.py`

**Testing:**
- Unit tests for all entities
- Unit tests for use cases
- Test business logic isolation

**Definition of Done:**
- Domain layer pure (no infrastructure)
- All aggregates implemented
- Use cases tested
- Interfaces defined

---

## Phase 5: Database Refactoring (Week 7)

**Objective:** Add missing constraints, fix schema issues.

**Tasks:**
1. Create migration `0006_add_composite_indexes`
2. Create migration `0007_create_rule_versions`
3. Create migration `0008_create_filing_snapshots_metadata`
4. Add foreign keys to all tables
5. Add unique constraints
6. Add composite indexes

**Migrations:**
```sql
-- 0006_add_composite_indexes
CREATE INDEX idx_filings_client_ay ON itr_filings(client_id, ay);
CREATE INDEX idx_snapshots_client_ay ON computed_return_snapshots(client_id, ay);
CREATE INDEX idx_tds_client_ay ON tds_deductors(client_id, ay);

-- 0007_create_rule_versions
CREATE TABLE rule_versions (...);

-- 0008_create_filing_snapshots_metadata
CREATE TABLE filing_snapshots_metadata (...);
```

**Testing:**
- Test migrations on copy of production data
- Verify no data loss
- Check query performance improvements

**Definition of Done:**
- All migrations applied
- All foreign keys in place
- Query performance improved
- Backward compatible

---

## Phase 6: Tax Computation Integration (Week 8)

**Objective:** Wire OpenTax adapter into computation flow.

**Tasks:**
1. Update ComputeTaxUseCase to use OpenTaxAdapter
2. Implement both regime computation
3. Add snapshot creation
4. Add validation
5. End-to-end integration test

**Files to Modify:**
- `backend/app/api/v1/computation.py`
- `backend/app/core/use_cases/filing/compute_tax.py`

**Testing:**
- Test with 10 real client scenarios
- Compare OpenTax output vs manual calculation
- Verify JSON matches ITD schema
- Performance test (target <2s per computation)

**Golden Test Cases:**
1. Salaried, new regime, ₹15L income
2. Salaried, old regime, ₹15L income, ₹1.5L deductions
3. Salaried + HP, new regime
4. Salaried + LTCG 112A, old regime
5. Salaried + interest + dividend, both regimes
6. Senior citizen, old regime
7. Super senior citizen, old regime
8. High surcharge case (₹1Cr+ income)
9. Rebate 87A case (new regime)
10. Complex case (all income heads)

**Definition of Done:**
- All 10 golden tests pass
- Computation accuracy validated
- Performance meets target
- Both regimes computed

---

## Phase 7: ITR JSON Export (Week 9)

**Objective:** Generate ITR-1 JSON for portal upload.

**Tasks:**
1. Wire OpenTax ITR builder
2. Add validation against ITD schema
3. Implement JSON download API
4. Add pre-submission checks
5. Test with ITD validation tool

**Files to Create:**
- `backend/app/core/use_cases/filing/generate_itr_json.py`
- `backend/app/api/v1/export.py`

**Testing:**
- Generate JSON for 10 test cases
- Validate against `itr1-schema.json`
- Upload to ITD test portal
- Verify no rejections

**Definition of Done:**
- Valid ITR-1 JSON generated
- Passes ITD schema validation
- Successfully uploaded to test portal
- Zero rejections in pilot

---

## Phase 8: Frontend Rebuild (Week 10)

**Objective:** Rebuild frontend in Next.js with OpenTax UI components.

**Tasks:**
1. Create Next.js project
2. Port authentication
3. Port client management
4. Adapt OpenTax filing form
5. Add computation preview
6. Add ITR JSON preview

**Directory Structure:**
```
frontend/
├── app/
│   ├── (auth)/login/
│   ├── (dashboard)/
│   │   ├── clients/
│   │   ├── filing/[clientId]/[ay]/
│   │   └── dashboard/
├── components/
│   ├── filing/ (adapted from OpenTax)
│   └── ui/
├── lib/
│   ├── api/
│   └── hooks/
```

**Testing:**
- E2E tests with Playwright
- Mobile responsiveness
- Cross-browser testing

**Definition of Done:**
- All pages migrated
- OpenTax UI components integrated
- Responsive design
- E2E tests pass

---

## Phase 9: Production Hardening (Week 11)

**Objective:** Security, performance, monitoring.

**Tasks:**
1. Security audit
2. Add rate limiting
3. Add caching (Redis)
4. Add logging (structured JSON)
5. Add metrics (Prometheus)
6. Add error tracking (Sentry)
7. Load testing

**Security Checklist:**
- SQL injection prevention
- XSS prevention
- CSRF protection
- Secrets management
- Input validation
- Output encoding

**Performance Targets:**
- API response <200ms (p95)
- Computation <2s
- Page load <1s
- Database queries <50ms

**Definition of Done:**
- Security audit passed
- Performance targets met
- Monitoring in place
- Load test passed (100 concurrent users)

---

## Phase 10: Pilot Deployment (Week 12)

**Objective:** Deploy to staging, run pilot with 10 clients.

**Tasks:**
1. Deploy to staging environment
2. Migrate 10 pilot clients
3. Train CA on new system
4. File 10 pilot returns
5. Monitor for issues
6. Gather feedback

**Pilot Clients:**
- 3 simple (salary only)
- 3 medium (salary + HP)
- 2 complex (salary + HP + CG)
- 2 edge cases (senior, high income)

**Success Criteria:**
- All 10 returns filed successfully
- Zero computation errors
- Zero ITD rejections
- CA satisfaction score >8/10

**Definition of Done:**
- Pilot completed
- All returns acknowledged
- Feedback incorporated
- Production deployment approved

---

## Rollback Plan

Each phase has a rollback strategy:

**Phase 0-2:** Git revert, no data impact
**Phase 3-5:** Database rollback via Alembic downgrade
**Phase 6-8:** Feature flag toggle (disable OpenTax, use old code)
**Phase 9-10:** Blue-green deployment, instant switch back

**Rollback Triggers:**
- Critical bug affecting >10% of users
- Data corruption
- ITD portal rejections >5%
- Performance degradation >50%

---

## Risk Mitigation

| Phase | Risk | Mitigation |
|-------|------|------------|
| 1-2 | OpenTax breaking changes | Pin to commit SHA, test before update |
| 3-5 | Data migration failure | Test on copy, backup before migration |
| 6-7 | Computation errors | Golden tests, parallel run with old system |
| 8 | Frontend bugs | Gradual rollout, feature flags |
| 9-10 | Production issues | Blue-green deployment, instant rollback |

---

## Effort Summary

| Phase | Duration | Developers | Effort (hours) |
|-------|----------|-----------|----------------|
| 0 | 1 week | 1 | 40 |
| 1 | 1 week | 1 | 40 |
| 2 | 2 weeks | 2 | 160 |
| 3 | 1 week | 1 | 40 |
| 4 | 1 week | 2 | 80 |
| 5 | 1 week | 1 | 40 |
| 6 | 1 week | 2 | 80 |
| 7 | 1 week | 2 | 80 |
| 8 | 1 week | 2 | 80 |
| 9 | 1 week | 2 | 80 |
| 10 | 1 week | 3 | 120 |
| **Total** | **12 weeks** | **2-3** | **840** |

---

**Status:** APPROVED FOR IMPLEMENTATION  
**Start Date:** TBD  
**Expected Completion:** 12 weeks from start
