# ITR-FilingWebsite — Project Status

## Active Branch: `3-doc`

This branch implements the three-document architecture plan:
- **Doc 1** — `01_ARCHITECTURE_SPECIFICATION.md` (in `~/Downloads/`)
- **Doc 2** — `02_CBDT_SCHEDULE_COMPLIANCE_AUDIT_AY2026-27.md` (in `~/Downloads/`)
- **Doc 3** — `03_IMPLEMENTATION_ROADMAP_AND_CODING_TASKS.md` (in `~/Downloads/`)

## Current Phase: Phase 1 — Project Structure & Dead-Code Remediation

See `03_IMPLEMENTATION_ROADMAP_AND_CODING_TASKS.md` §2 for the full task list.

## Branching Strategy

```
main              — production-stable
restructure/cleanup-v1  — prior restructure attempt
3-doc             — THIS BRANCH — implements the 3-document plan, phase by phase
```

## Phase Progress

| Phase | Description | Status |
|-------|-------------|--------|
| Phase 1 | Project Structure & Dead-Code Remediation | 🔄 In Progress |
| Phase 2 | Rule Engine, Three-Module Split, Canonical ComputedReturn, Rounding, Immutable Snapshots | ⬜ Not Started |
| Phase 3 | Validation Engine | ⬜ Not Started |
| Phase 4 | Manual Import Fixes: 26AS / AIS / TIS JSON | ⬜ Not Started |
| Phase 5 | Audit Engine | ⬜ Not Started |
| Phase 6 | Income Head 1: Salary | ⬜ Not Started |
| Phase 7 | Income Head 2: House Property | ⬜ Not Started |
| Phase 8 | Income Head 3: Capital Gains | ⬜ Not Started |
| Phase 9 | Income Head 4: Business / Profession | ⬜ Not Started |
| Phase 10 | Income Head 5: Other Sources + VDA | ⬜ Not Started |
| Phase 11 | Compute Tax Page: CYLA/BFLA/CFL, Schedule SI, Regime Comparison, Rounding | ⬜ Not Started |
| Phase 12 | Workflow Engine | ⬜ Not Started |
| Phase 13 | JSON Export for ITD Portal Upload | ⬜ Not Started |
| Phase 14 | Computation PDF Service | ⬜ Not Started |
| Phase 15 | Backup & Restore | ⬜ Not Started |
| Phase 16 | Bulk Client Import | ⬜ Not Started |
| Phase 17 | Extensibility Provisioning | ⬜ Not Started |

## Audit Score

| Metric | Value |
|--------|-------|
| Static compliance score (pre-plan) | 27/100 |
| Target | 100/100 |
| Phases completed | 0/17 |

## Key Architecture Decisions (Document 1)

- `IndianAmount` (paise as `long`) is the only permitted money type — no `double`/`float`
- `ComputedReturn` is the single canonical computation object — no parallel computation paths
- Three independently-versioned modules: Tax Rules, ITR Form Definitions, JSON Schema
- `TaxYearRulesRegistry` — one new AY = one new class, zero existing-file edits
- Validation Engine is a separate pipeline stage (not folded into import or computation)
- Audit Engine records user actions; Snapshot Store records computation results — distinct, both append-only
- Workflow Engine is a proper state machine with role-gated transitions

## Deprecated / Deleted Files (cleaned up this branch)

Old documentation sprawl collapsed to this single STATUS.md. All prior `.md` files, `.zip` files, and `temp_extract/` contents have been removed. The `docs/archive/` folder contains documentation that was moved from the root `docs/` directory.

## AY 2026-27 Compliance Notes

- Governing law: Income-tax Act, 1961 (not the 2025 Act, which applies from AY 2027-28)
- Forms notified: 30 March 2026 + corrigendum 10 April 2026 (Notifications 57–63)
- ITR-1/ITR-4 now permit up to 2 house properties (expanded from 1)
- LTCG u/s 112A up to ₹1,25,000 now eligible for ITR-1/ITR-4 (new carve-out)
- VDA: no netting of losses against gains or any other income head
- Updated return (ITR-U) window extended to 48 months
