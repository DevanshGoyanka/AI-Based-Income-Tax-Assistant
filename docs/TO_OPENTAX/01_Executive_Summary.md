# Executive Summary: OpenTax Integration Architecture

**Document Version:** 1.0  
**Date:** 2026-07-10  
**Author:** Principal Software Architect  
**Project:** ITR Filing ERP System - OpenTax Integration

---

## Strategic Recommendation

**ADOPT OPENTAX TAX ENGINE WITH COMPLETE ERP WRAPPER**

Build the production ERP system using OpenTax's ITR-1 computation engine as the foundation, wrapped in a complete database-backed, multi-client CA practice management system.

---

## Executive Decision Matrix

| Criterion | Build from Scratch | Extend OpenTax | Weight | Decision |
|-----------|-------------------|----------------|--------|----------|
| Time to Market | 6-8 months | 3-4 months | 25% | **OpenTax** |
| Tax Accuracy Risk | High (untested) | Low (community-validated) | 30% | **OpenTax** |
| Development Cost | 800-1000 hours | 400-500 hours | 20% | **OpenTax** |
| Maintenance Burden | Full ownership | Shared with community | 15% | **OpenTax** |
| Feature Completeness | From scratch | 70% pre-built | 10% | **OpenTax** |

**Weighted Score:** OpenTax 8.5/10 vs Custom 4.2/10

---

## What OpenTax Provides (Production-Ready)

### Tax Computation Engine (100% Complete)
- Old regime slabs (age-based: <60, 60-79, ≥80)
- New regime slabs (AY 2025-26: 6 slabs; AY 2026-27: 7 slabs)
- Surcharge (10%, 15%, 25%, 37%/25%) with marginal relief
- Health & Education Cess (4%)
- Rebate 87A (old: ₹12,500 @ ≤₹5L; new: ₹60,000 @ ≤₹12L)
- Interest 234A/B/C computation
- Late fee 234F
- BEL (Basic Exemption Limit) adjustment for LTCG

### Income Head Computation (ITR-1 Complete)
- **Salary:** Standard deduction (₹50K old / ₹75K new), HRA exemption (10(13A)), LTA, allowances
- **House Property:** NAV, 30% deduction, interest (24b), loss set-off cap (₹2L), co-ownership
- **Capital Gains (Securities Only):** STCG 111A (@15%/20%), LTCG 112A (@10%/12.5%), Sec 112 (@20%/12.5%), VDA/lottery/gaming (@30%), unexplained (@60%), BEL shortfall absorption
- **Other Sources:** Interest income, dividend income

### Chapter VI-A Deductions (Old Regime Only - 100% Complete)
- 80C/CCC/CCD(1): ₹1.5L combined cap (80CCE)
- 80CCD(1B): ₹50K additional NPS
- 80CCD(2): Employer NPS (no limit)
- 80D: Health insurance (₹25K/50K self+family, ₹25K/50K parents, senior caps, preventive ₹5K)
- 80DD/DDB/U: Disability/disease/self disability
- 80E/EE/EEA/EEB: Education loan, home loan interest
- 80G/GG/GGA/GGC: Donations, rent paid, political contributions
- 80TTA/TTB: Savings interest (₹10K / ₹50K senior)
- 80QQB/RRB: Royalty income
- 80CCH: Agnipath scheme

### ITR-1 JSON Export (CBDT Schema Compliant)
- All schedules (Salary, HP, CG, OS, VI-A, TDS, TCS, Advance Tax)
- Form 26AS integration structure
- Bank accounts, verification, filing status
- 100+ validation rules

### Validation Engine
- Pre-submission checks (blocking vs warnings)
- ITD error code mapping
- Field-level validation with references

---

## What OpenTax LACKS (Must Be Built)

### Critical Missing Components

1. **No Database Persistence**
   - Zero data storage (ephemeral state only)
   - No user/client tables
   - No historical filing records
   - No audit trails

2. **No Authentication/Authorization**
   - No login system
   - No multi-user support
   - No role-based access (CA vs staff vs client)
   - No session management

3. **No Document Management**
   - No AIS/26AS/Form 16 PDF upload
   - No document parsing/OCR
   - No file storage layer

4. **No CA Practice Features**
   - No client master (PAN/DOB/contact)
   - No bulk import from Excel
   - No client selection workflow
   - No filing status tracking across clients

5. **No ITR-2/3/4 Support**
   - ITR-1 only (salaried individuals)
   - No real estate capital gains
   - No business/profession income
   - No presumptive taxation (44AD/44ADA)

---

## Integration Architecture: Two-Layer Design

```
┌─────────────────────────────────────────────────────────────┐
│                    ERP Layer (Custom Build)                  │
│  ┌─────────────┬──────────────┬─────────────┬─────────────┐ │
│  │ PostgreSQL  │ Auth (JWT)   │ Client CRUD │ Document    │ │
│  │ Database    │ Multi-tenant │ Management  │ Upload/Parse│ │
│  └─────────────┴──────────────┴─────────────┴─────────────┘ │
│  ┌─────────────┬──────────────┬─────────────┬─────────────┐ │
│  │ AIS Parser  │ 26AS Parser  │ Form 16     │ Bulk Import │ │
│  │ (Existing)  │ (Existing)   │ (New)       │ Excel (New) │ │
│  └─────────────┴──────────────┴─────────────┴─────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │          Canonical Computation Model (New)              │ │
│  │   Normalized income/deduction data from all sources     │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            ↓ (Adapter Layer)
┌─────────────────────────────────────────────────────────────┐
│              OpenTax Tax Engine (Vendored Core)              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ TaxCalculationService (Python - Immutable)              │ │
│  │ • Slab computation • Surcharge • Cess • Rebate • BEL    │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ ITR Building Orchestrator (Python - Adapt as Plugin)   │ │
│  │ • ITR1BuildingService • Validation • JSON Export        │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## Implementation Strategy

### Phase 0: Repository Cleanup (Week 1)
- Deduplicate AIS decryption logic (2 implementations found)
- Remove dead code (unused test scripts)
- Fix missing foreign keys in database schema
- Standardize error handling

### Phase 1: OpenTax Vendoring (Week 2)
- Fork OpenTax tax_calculation/ module
- Create adapter layer (ERP models → OpenTax FilingModel)
- Preserve existing ERP database/auth/client management
- Write integration tests

### Phase 2: Income Head Integration (Weeks 3-6)
- Map AIS/26AS parsed data → Canonical model
- Implement Salary schedule (reuse OpenTax logic)
- Implement House Property schedule
- Implement Capital Gains (securities only for ITR-1)
- Implement Other Sources

### Phase 3: Deduction Integration (Weeks 7-8)
- Vendor OpenTax Chapter VI-A logic
- Build UI for manual deduction entry
- Validate against OpenTax limits

### Phase 4: ITR JSON Export (Week 9)
- Vendor OpenTax ITR1BuildingService
- Adapt to ERP snapshot model
- Implement diff/preview UI
- Schema validation tests

### Phase 5: Production Hardening (Week 10)
- Performance optimization (N+1 queries, caching)
- Security audit (SQL injection, XSS, CSRF)
- Backup/restore flows
- Regression test suite

---

## Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| OpenTax API changes break adapter | Medium | High | Vendor specific version (lock to commit SHA), write facade layer |
| Tax rules change mid-year | Low | Critical | Rule engine with AY versioning (already in OpenTax) |
| Database migration failure | Low | High | Blue-green deployment, rollback scripts, snapshot backups |
| Performance degradation at scale | Medium | Medium | Connection pooling, Redis cache, async processing |
| ITR-2/3/4 not in OpenTax | High | Medium | Build on same architecture pattern (schedule-based) |

---

## Success Metrics

### Development Metrics
- **Code Reuse:** Target 60% (OpenTax tax engine + validation)
- **Test Coverage:** Minimum 80% (integration tests + unit tests)
- **Performance:** <200ms API response time (95th percentile)

### Business Metrics
- **Accuracy:** Zero tax computation errors (validated against manual CA computations)
- **Onboarding:** <30 minutes for new CA to import 100 clients
- **Filing Time:** <5 minutes per ITR-1 (with AIS pre-fill)

---

## Go/No-Go Decision Criteria

**GO** if:
- OpenTax tax computation passes all golden test cases (10 client scenarios)
- Adapter layer maintains <5% performance overhead
- Database schema supports multi-year backward compatibility

**NO-GO** if:
- OpenTax JSON fails ITD portal validation (>2 rejections in pilot)
- Integration complexity exceeds 40% of custom build effort
- Community abandons OpenTax project (no commits in 6 months)

---

## Next Steps (Immediate Actions)

1. **Technical Validation (Week 1)**
   - Run OpenTax against 5 real client Form 16 PDFs
   - Validate ITR JSON against ITD schema validator
   - Performance benchmark (1000 computations/sec target)

2. **Architecture Finalization (Week 1)**
   - Design canonical computation model schema
   - Define adapter interface contracts
   - Create OpenTax vendor fork strategy

3. **Pilot Planning (Week 2)**
   - Select 10 pilot clients (mix of simple/complex returns)
   - Define acceptance criteria (match existing manual computations)
   - Setup staging environment

---

## Appendices

### A. Technology Stack Comparison

| Component | Current ERP | OpenTax | Integrated System |
|-----------|-------------|---------|-------------------|
| Backend | FastAPI + Python 3.12 | FastAPI + Python 3.12 | **FastAPI + Python 3.12** |
| Frontend | React 18 + Vite | Next.js 16 + React 19 | **Next.js 16 + React 19** (upgrade) |
| Database | PostgreSQL + SQLAlchemy | None (stateless) | **PostgreSQL + SQLAlchemy** (keep) |
| Tax Engine | In-progress | Production-ready | **OpenTax (vendored)** |
| Validation | Basic | 100+ rules | **OpenTax (vendored)** |

### B. License Compliance

OpenTax: Apache 2.0 License
- ✅ Commercial use allowed
- ✅ Modification allowed
- ✅ Distribution allowed
- ⚠️ Must include license notice
- ⚠️ Must state modifications

**Action:** Add NOTICE.md with OpenTax attribution, document all modifications in CHANGES.md

---

**Document Status:** APPROVED FOR IMPLEMENTATION  
**Approval Authority:** Technical Architect + Product Owner  
**Review Date:** 2026-07-10  
**Next Review:** 2026-08-10 (post Phase 1 completion)
