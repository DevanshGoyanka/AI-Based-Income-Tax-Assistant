# OpenTax Vendor Directory

**Status:** VENDORED ✅ — Reference Only (NOT a runtime dependency for tax computation)

## ⚠️ IMPORTANT: This code is NOT called in production for tax computation.

Per **ADR-022**, our owned `TaxEngine` (in `core/services/tax_engine.py`) handles
all tax computation at runtime. This vendored OpenTax code is used ONLY for:

1. **Test Oracle** — Comparing our engine's results against OpenTax within ₹1 tolerance
2. **ITR JSON Generation** (Phase 7) — `ItrBuildingOrchestrator` and ITR models
3. **Validation Rules Reference** (Phase 7) — `TaxValidationService`

**Never import from `vendor/filing/` in production code.** Only in test files.

---

## What Was Removed

The following files previously existed but have been **permanently removed** (Phase 2):
- `adapters/opentax/model_mapper.py` — replaced by our owned `TaxEngine`
- `adapters/opentax/response_mapper.py` — replaced by our owned `TaxEngine`
- `vendor/filing/tax_calculation/tax_calculation_service.py` — no longer called at runtime

Our owned implementations:
- `core/services/tax_engine.py` — main owned tax engine (replaces OpenTax)
- `core/services/interest_234_engine.py` — owned 234A/B/C/F interest engine
- `core/services/slab_tables.py` — owned CBDT slab rates and rule constants

## Vendored Modules

Copied from OpenTax-Reference@90c60bb on 2026-07-14 and converted to relative imports.

## Instructions for Updating OpenTax

To update to a new OpenTax version:

1. Restore all vendored files from the OpenTax reference repo
2. Copy fresh modules from the new OpenTax commit
3. Add `__init__.py` files to all sub-packages
4. Re-run `_fix_absolute_imports.py` to convert imports
5. Update `__version__.py` with the new commit SHA
6. Test all imports
7. Re-enable cross-validation tests comparing owned engine vs OpenTax oracle

## Architecture Note

OpenTax is a **reference implementation**, not our core. Our owned `TaxEngine`
implements `ITaxEngine` and can be swapped for other engines per ADR-002.
