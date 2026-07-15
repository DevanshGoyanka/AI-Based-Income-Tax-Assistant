"""OpenTax vendor version tracking.

This file tracks the vendored OpenTax code version for reproducibility.
When updating OpenTax, update this file with the new commit SHA.
"""

OPENTAX_REPO = "https://github.com/opentax/opentax"
OPENTAX_COMMIT = "90c60bb"
OPENTAX_VERSION = "0.1.0-dev"
VENDOR_DATE = "2026-07-14"

VENDORED_MODULES = [
    "tax_calculation/",  # Core tax computation engine
    "itr/itr1/",        # ITR-1 builder
    "itr/validations/", # Validation rules
    "models/",          # Pydantic schemas
]

NOTES = """
OpenTax Integration Status:
- Architecture: Owned Tax Engine per ADR-022
- Status: VENDORED for reference/test oracle only, NOT runtime dependency
- Vendored modules: tax_calculation/, itr/, models/
- Production tax computation: core/services/tax_engine.py (OUR engine)
- Phase 2: Owned engine replaces OpenTax adapter layer
"""
