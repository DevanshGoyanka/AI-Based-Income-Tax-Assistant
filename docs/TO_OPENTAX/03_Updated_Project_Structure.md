# Updated Project Structure

**Document Version:** 1.0  
**Date:** 2026-07-10  
**Status:** BLUEPRINT (Pre-Implementation)

---

## Overview

This document defines the complete production-grade project structure integrating OpenTax tax engine with the ERP system. The architecture follows Clean Architecture, DDD principles, and Hexagonal Architecture patterns.

---

## Root Directory Structure

```
ITR-FilingWebsite-main/
├── backend/                    # FastAPI backend
├── frontend/                   # Next.js 16 frontend
├── shared/                     # Shared types/schemas
├── docs/                       # Documentation
├── scripts/                    # Deployment/maintenance scripts
├── docker-compose.yml
├── Makefile
├── .env.example
└── README.md
```

---

## Backend Structure (Complete)

```
backend/
├── app/
│   ├── core/                           # Core domain (business logic)
│   │   ├── __init__.py
│   │   ├── domain/                     # Domain entities
│   │   │   ├── __init__.py
│   │   │   ├── client.py              # Client aggregate root
│   │   │   ├── filing.py              # Filing aggregate root
│   │   │   ├── computed_return.py     # Immutable computation snapshot (with TaxBreakdown)
│   │   │   ├── user.py                # User entity
│   │   │   ├── document.py            # Document entity
│   │   │   ├── schedules/              # ITR schedule entities
│   │   │   │   ├── base.py            # Schedule base class
│   │   │   │   ├── salary.py          # ScheduleSalary
│   │   │   │   ├── house_property.py  # ScheduleHP
│   │   │   │   ├── capital_gains.py  # ScheduleCG
│   │   │   │   ├── other_sources.py  # ScheduleOS
│   │   │   │   ├── schedule_via.py   # ScheduleVIA (deductions)
│   │   │   │   ├── tds.py            # ScheduleTDS
│   │   │   │   ├── schedule_it.py    # ScheduleIT (advance tax)
│   │   │   │   └── bank_accounts.py  # ScheduleBA
│   │   │   └── value_objects/
│   │   │       ├── __init__.py
│   │   │       ├── pan.py            # PAN value object
│   │   │       ├── money.py          # Money value object (integer paise)
│   │   │       ├── assessment_year.py
│   │   │       └── tax_regime.py
│   │   ├── interfaces/                # Port definitions (Hexagonal)
│   │   │   ├── __init__.py
│   │   │   ├── repositories.py       # Repository interfaces
│   │   │   ├── tax_engine.py         # Tax engine port
│   │   │   ├── document_parser.py    # Parser port
│   │   │   ├── storage.py            # File storage port
│   │   │   └── notification.py       # Notification port
│   │   └── use_cases/                # Application services
│   │       ├── __init__.py
│   │       ├── client/
│   │       │   ├── __init__.py
│   │       │   ├── create_client.py
│   │       │   ├── update_client.py
│   │       │   ├── delete_client.py
│   │       │   ├── import_clients_bulk.py
│   │       │   └── search_clients.py
│   │       ├── filing/
│   │       │   ├── __init__.py
│   │       │   ├── create_filing.py
│   │       │   ├── compute_tax.py
│   │       │   ├── generate_itr_json.py
│   │       │   ├── generate_itr_pdf.py
│   │       │   ├── submit_filing.py
│   │       │   └── import_ais_26as.py
│   │       ├── computation/
│   │       │   ├── __init__.py
│   │       │   ├── compute_salary.py
│   │       │   ├── compute_house_property.py
│   │       │   ├── compute_capital_gains.py
│   │       │   ├── compute_other_sources.py
│   │       │   ├── compute_deductions.py
│   │       │   └── compute_tax_liability.py
│   │       └── snapshot/
│   │           ├── __init__.py
│   │           ├── create_snapshot.py
│   │           ├── restore_snapshot.py
│   │           └── compare_snapshots.py
│   │
│   ├── adapters/                      # Adapters (Hexagonal)
│   │   ├── __init__.py
│   │   ├── opentax/                   # OpenTax adapter
│   │   │   ├── __init__.py
│   │   │   ├── tax_engine_adapter.py  # Implements ITaxEngine, delegates to our TaxEngine
│   │   │   ├── vendor/                # REFERENCE ONLY per ADR-022
│   │   │   └── vendor/                # Vendored OpenTax code
│   │   │       ├── __init__.py
│   │   │       ├── tax_calculation/   # Copied from OpenTax
│   │   │       └── itr/               # Copied from OpenTax
│   │   ├── repositories/              # Database implementations
│   │   │   ├── __init__.py
│   │   │   ├── sqlalchemy_client_repo.py
│   │   │   ├── sqlalchemy_filing_repo.py
│   │   │   ├── sqlalchemy_snapshot_repo.py
│   │   │   └── sqlalchemy_document_repo.py
│   │   ├── parsers/                   # Document parser implementations
│   │   │   ├── __init__.py
│   │   │   ├── ais_json_parser.py    # Keep existing
│   │   │   ├── ais_pdf_parser.py     # Keep existing
│   │   │   ├── form26as_zip_parser.py # 26AS ZIP (password-protected TXT)
│   │   │   ├── form26as_pdf_parser.py # 26AS PDF (table extraction)
│   │   │   ├── form16_parser.py       # NEW
│   │   │   └── prefill_parser.py     # Keep existing
│   │   ├── storage/                   # File storage implementations
│   │   │   ├── __init__.py
│   │   │   ├── local_storage.py
│   │   │   └── s3_storage.py         # Future
│   │   └── notification/              # Notification implementations
│   │       ├── __init__.py
│   │       ├── email_notifier.py     # Future
│   │       └── sms_notifier.py       # Future
│   │
│   ├── infra/                         # Infrastructure layer
│   │   ├── __init__.py
│   │   ├── database/
│   │   │   ├── __init__.py
│   │   │   ├── base.py               # SQLAlchemy Base
│   │   │   ├── session.py            # Session management
│   │   │   ├── models/               # SQLAlchemy models
│   │   │   │   ├── __init__.py
│   │   │   │   ├── client_model.py
│   │   │   │   ├── filing_model.py
│   │   │   │   ├── snapshot_model.py
│   │   │   │   ├── user_model.py
│   │   │   │   ├── document_model.py
│   │   │   │   ├── tds_model.py
│   │   │   │   ├── form26as_model.py
│   │   │   │   ├── ais_model.py
│   │   │   │   └── audit_model.py
│   │   │   └── migrations/           # Alembic migrations
│   │   ├── security/
│   │   │   ├── __init__.py
│   │   │   ├── auth.py               # JWT handling
│   │   │   ├── password.py           # Bcrypt hashing
│   │   │   └── permissions.py        # RBAC
│   │   └── config/
│   │       ├── __init__.py
│   │       ├── settings.py           # Pydantic settings
│   │       └── logging.py            # Logging config
│   │
│   ├── api/                           # API layer (FastAPI)
│   │   ├── __init__.py
│   │   ├── v1/
│   │   │   ├── __init__.py
│   │   │   ├── router.py             # Main v1 router
│   │   │   ├── auth.py               # /api/v1/auth
│   │   │   ├── clients.py            # /api/v1/clients
│   │   │   ├── filings.py            # /api/v1/filings
│   │   │   ├── computation.py        # /api/v1/computation
│   │   │   ├── documents.py          # /api/v1/documents
│   │   │   ├── snapshots.py          # /api/v1/snapshots
│   │   │   ├── imports.py            # /api/v1/imports (AIS/26AS)
│   │   │   ├── bulk.py               # /api/v1/bulk (Excel import)
│   │   │   └── dashboard.py          # /api/v1/dashboard
│   │   └── dependencies.py           # FastAPI dependencies
│   │
│   ├── schemas/                       # Pydantic DTOs (API contracts)
│   │   ├── __init__.py
│   │   ├── client.py
│   │   ├── filing.py
│   │   ├── computation.py
│   │   ├── snapshot.py
│   │   ├── document.py
│   │   ├── import_data.py
│   │   ├── validation.py
│   │   └── common.py
│   │
│   ├── services/                      # Application services
│   │   ├── __init__.py
│   │   ├── canonical/                 # Canonical model normalization
│   │   │   ├── __init__.py
│   │   │   ├── canonical_income.py   # Normalized income model
│   │   │   ├── canonical_deduction.py
│   │   │   └── normalizer.py         # Normalize from all sources
│   │   ├── tax/                       # OWNED tax engine (per ADR-022)
│   │   │   ├── __init__.py
│   │   │   ├── slab_tables.py        # AY-versioned CBDT slab data
│   │   │   ├── tax_engine.py         # Main computation engine
│   │   │   └── interest_234_engine.py  # Interest u/s 234A/B/C
│   │   └── utils/
│   │       ├── __init__.py
│   │       ├── rounding.py           # CBDT rounding rules
│   │       ├── date_utils.py
│   │       └── validators.py
│   │
│   ├── main.py                        # FastAPI app entry
│   ├── deps.py                        # Dependency injection
│   └── settings.py                    # Global settings
│
├── tests/
│   ├── __init__.py
│   ├── unit/                          # Unit tests
│   │   ├── core/
│   │   │   ├── domain/
│   │   │   └── use_cases/
│   │   └── adapters/
│   │       └── opentax/
│   ├── integration/                   # Integration tests
│   │   ├── api/
│   │   ├── database/
│   │   └── adapters/
│   ├── e2e/                           # End-to-end tests
│   └── fixtures/                      # Test data
│       ├── clients.json
│       ├── form16_samples/
│       ├── ais_samples/
│       └── golden_computations/       # Known-good tax computations
│
├── alembic/                           # Database migrations
│   ├── versions/
│   └── env.py
│
├── scripts/
│   ├── seed_data.py
│   └── migration_helper.py
│
├── requirements.txt
├── requirements-dev.txt
├── pyproject.toml                     # Black, isort, mypy config
├── pytest.ini
└── .env.example
```

---

## Frontend Structure (Next.js 16)

```
frontend/
├── app/                               # Next.js 16 App Router
│   ├── layout.tsx                    # Root layout
│   ├── page.tsx                      # Home page
│   ├── globals.css
│   ├── (auth)/                       # Auth route group
│   │   ├── login/
│   │   │   └── page.tsx
│   │   └── register/
│   │       └── page.tsx
│   ├── (dashboard)/                  # Dashboard route group
│   │   ├── layout.tsx               # Dashboard layout with sidebar
│   │   ├── dashboard/
│   │   │   └── page.tsx             # Main dashboard
│   │   ├── clients/
│   │   │   ├── page.tsx             # Client list
│   │   │   ├── [id]/
│   │   │   │   ├── page.tsx         # Client detail
│   │   │   │   └── edit/
│   │   │   │       └── page.tsx     # Edit client
│   │   │   └── new/
│   │   │       └── page.tsx         # New client
│   │   ├── filing/
│   │   │   ├── [clientId]/
│   │   │   │   ├── [ay]/
│   │   │   │   │   ├── page.tsx     # Filing form
│   │   │   │   │   ├── preview/
│   │   │   │   │   │   └── page.tsx # ITR preview
│   │   │   │   │   └── history/
│   │   │   │   │       └── page.tsx # Snapshot history
│   │   │   │   └── select-ay/
│   │   │   │       └── page.tsx     # AY selection
│   │   │   └── wizard/
│   │   │       └── page.tsx         # Wizard mode (reuse OpenTax)
│   │   ├── imports/
│   │   │   ├── ais/
│   │   │   │   └── page.tsx
│   │   │   ├── form26as/
│   │   │   │   └── page.tsx
│   │   │   └── bulk/
│   │   │       └── page.tsx         # Excel bulk import
│   │   └── reports/
│   │       └── page.tsx
│   │
│   └── api/                          # API routes (if needed)
│       └── health/
│           └── route.ts
│
├── components/                        # Reusable components
│   ├── ui/                           # Shadcn-style primitives
│   │   ├── button.tsx
│   │   ├── input.tsx
│   │   ├── select.tsx
│   │   ├── dialog.tsx
│   │   ├── table.tsx
│   │   └── form.tsx
│   ├── layout/
│   │   ├── AppHeader.tsx
│   │   ├── Sidebar.tsx
│   │   └── Footer.tsx
│   ├── client/
│   │   ├── ClientTable.tsx
│   │   ├── ClientForm.tsx
│   │   └── ClientSearchBar.tsx
│   ├── filing/                       # Adapted from OpenTax
│   │   ├── PersonalDetailsForm.tsx
│   │   ├── IncomeForm.tsx
│   │   │   ├── SalaryIncomeForm.tsx
│   │   │   ├── HousePropertyForm.tsx
│   │   │   ├── CapitalGainsForm.tsx
│   │   │   └── OtherSourcesForm.tsx
│   │   ├── DeductionsForm.tsx
│   │   ├── TaxPaidForm.tsx
│   │   └── SummaryView.tsx
│   ├── imports/
│   │   ├── AISUploadForm.tsx
│   │   ├── Form26ASUploadForm.tsx
│   │   └── BulkImportWizard.tsx
│   └── validation/
│       └── ValidationReportPanel.tsx
│
├── lib/                              # Utility libraries
│   ├── api/                          # API client
│   │   ├── client.ts                # Axios instance
│   │   ├── auth.ts
│   │   ├── clients.ts
│   │   ├── filings.ts
│   │   ├── computation.ts
│   │   └── imports.ts
│   ├── hooks/                        # Custom React hooks
│   │   ├── useAuth.ts
│   │   ├── useClient.ts
│   │   ├── useFiling.ts
│   │   ├── useComputation.ts
│   │   └── useDebounce.ts
│   ├── context/                      # React contexts
│   │   ├── AuthContext.tsx
│   │   └── FilingContext.tsx
│   ├── utils/
│   │   ├── formatters.ts            # Currency, date formatting
│   │   ├── validators.ts
│   │   └── pan.ts
│   └── types/                        # TypeScript types
│       ├── client.ts
│       ├── filing.ts
│       ├── computation.ts
│       └── api.ts
│
├── public/
│   ├── logo.svg
│   └── favicon.ico
│
├── styles/
│   └── tailwind.css
│
├── middleware.ts                     # Auth middleware
├── next.config.ts
├── tailwind.config.ts
├── tsconfig.json
├── package.json
└── .env.local.example
```

---

## Key Architectural Decisions

### 1. Hexagonal Architecture

**Ports (Interfaces):**
- `ITaxEngine` - Tax computation port
- `IClientRepository` - Client persistence port
- `IFilingRepository` - Filing persistence port
- `IDocumentParser` - Document parsing port
- `IStorage` - File storage port

**Adapters:**
- `OpenTaxAdapter` implements `ITaxEngine`
- `SQLAlchemyClientRepo` implements `IClientRepository`
- `AISJsonParser` implements `IDocumentParser`

### 2. Domain-Driven Design

**Aggregates:**
- `Client` (root) - PAN, DOB, contact info, assigned_user
- `Filing` (root) - client_id, AY, regime, status, income heads
- `ComputedReturn` (immutable) - snapshot of computed tax

**Value Objects:**
- `PAN` - Validation, formatting
- `Money` - Integer paise, arithmetic operations
- `AssessmentYear` - Validation, FY conversion
- `TaxRegime` - Old/New enum

### 3. Clean Architecture Layers

**Layer 1 (Core):**
- Domain entities
- Use cases
- Interfaces (ports)

**Layer 2 (Adapters):**
- OpenTax adapter
- Repository implementations
- Parser implementations

**Layer 3 (Infrastructure):**
- Database models
- Framework (FastAPI)
- External services

**Dependency Rule:** Outer layers depend on inner, never reverse.

---

## File Movement Plan

### Files to KEEP (No Change)

```
backend/app/infra/database/models/
├── client_model.py
├── user_model.py
└── audit_model.py

backend/app/services/importers/
├── crypto.py              # AIS decryption (100k iterations)
├── ais_pdf_parser.py
└── prefill.py

backend/app/core/parsers/
├── form26as_zip_parser.py  # 26AS ZIP (password-protected TXT)
├── form26as_pdf_parser.py  # 26AS PDF (table extraction)
├── ais_json_parser.py      # AIS JSON parsing
├── ais_pdf_parser.py       # AIS PDF parsing
└── tis_pdf_parser.py       # TIS PDF parsing

backend/app/core/normalizers/
├── form26as_normalizer.py  # 26AS → CanonicalTDS/CanonicalAdvanceTax
├── prefill_normalizer.py    # Prefill → CanonicalIncome
└── canonical_models.py      # Shared canonical models
```

**Note on Form 26AS:** The original audit flagged `form26as.py` as "900+ lines, needs splitting."
This has already been resolved — the codebase now uses a clean hexagonal structure:
- `Form26ASZipParser` handles password-protected ZIP files
- `Form26ASPDFParser` handles PDF extraction
- `Form26ASNormalizer` converts to canonical models
- `ImportForm26ASUseCase` orchestrates the flow
- `Canonical26AS` domain model holds all 10 parts (Part I–X)

No further splitting is needed. The single codebase handles all 26AS formats
(ZIP, PDF) through a unified `Canonical26AS` domain model.

### Files to DELETE

```
backend/app/services/
├── ais_decrypt.py         # Duplicate (wrong iteration count)

backend/ (root)
├── comprehensive_ais_test.py
├── debug_*.py             # All debug scripts
├── examine_*.py           # All examine scripts
├── manual_*.py            # All manual test scripts
├── standalone_*.py
├── test_ais_pdf.py
├── test_info_items.py
└── test_parser.py
```

### Files to MOVE

```
(No files to move. 26AS is already in the correct hexagonal structure.)
```

### Form 26AS Architecture (Clarification)

The original audit recommended splitting `form26as.py` into part-specific modules.
This is **NOT needed** because the codebase already uses a clean hexagonal structure:

```
Form 26AS Data Flow:

  User Upload (ZIP or PDF)
       │
       ▼
  ImportForm26ASUseCase.execute(file, file_type, password)
       │
       ├──► Form26ASZipParser.parse()    # ZIP → Canonical26AS
       │         or
       └──► Form26ASPDFParser.parse()    # PDF → Canonical26AS
                    │
                    ▼
              Canonical26AS (domain model, all 10 parts)
                    │
                    ▼
              Form26ASNormalizer.normalize_to_tds() → List[CanonicalTDS]
              Form26ASNormalizer.normalize_to_tcs() → List[dict]
                    │
                    ▼
              CanonicalIncome (unified output)
```

All 10 parts of Form 26AS (Part I–X) are handled in a single
`Canonical26AS` domain model. No separate files per part needed.

### Files to CREATE (New)

```
backend/app/core/interfaces/
├── repositories.py        # NEW
├── tax_engine.py          # NEW
├── document_parser.py     # NEW
└── storage.py             # NEW

backend/app/adapters/opentax/
├── tax_engine_adapter.py  # Implements ITaxEngine, delegates to our TaxEngine
├── vendor/                # REFERENCE ONLY per ADR-022
└── (model_mapper.py and response_mapper.py DELETED per ADR-022)

backend/app/adapters/opentax/vendor/
├── tax_calculation/       # VENDOR (from OpenTax)
└── itr/                   # VENDOR (from OpenTax)

backend/app/services/canonical/
├── canonical_income.py    # NEW
├── canonical_deduction.py # NEW
└── normalizer.py          # NEW
```

---

## Database Schema Updates

### New Tables

```sql
CREATE TABLE rule_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_year VARCHAR(7) NOT NULL UNIQUE,
    slab_structure JSONB NOT NULL,
    deduction_limits JSONB NOT NULL,
    special_rate_rules JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE filing_snapshots_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filing_id UUID NOT NULL REFERENCES itr_filings(id) ON DELETE CASCADE,
    snapshot_id UUID NOT NULL REFERENCES computed_return_snapshots(id),
    snapshot_type VARCHAR(20) NOT NULL, -- 'auto', 'manual', 'pre_submit'
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### Migration Plan

```
0003_add_foreign_keys.py
0004_fix_prefill_id_default.py
0005_add_unique_constraints.py
0006_add_composite_indexes.py
0007_create_rule_versions.py
0008_create_filing_snapshots_metadata.py
```

---

## Next Steps

1. **Phase 0:** Execute file cleanup (deletions, moves)
2. **Phase 1:** Create new directory structure
3. **Phase 2:** Vendor OpenTax code
4. **Phase 3:** Implement adapter layer
5. **Phase 4:** Migrate existing services
6. **Phase 5:** Database migrations
7. **Phase 6:** Frontend rebuild (Next.js)
8. **Phase 7:** Integration testing

---

**Document Status:** APPROVED  
**Implementation Start Date:** TBD  
**Review Date:** 2026-07-10
