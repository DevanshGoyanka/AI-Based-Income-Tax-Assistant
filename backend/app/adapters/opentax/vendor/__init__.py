"""OpenTax vendor package — REFERENCE ONLY, not a runtime dependency.

Per ADR-022, our owned TaxEngine (core/services/tax_engine.py) handles
all tax computation at runtime. This vendored code is used ONLY for:
1. Test oracle — verifying our engine matches within ₹1 tolerance
2. ITR JSON generation (Phase 7)
3. Validation rules reference (Phase 7)

Do NOT import from this package in production code. Use core/services/tax_engine.py instead.
"""

# These exports are available for test oracle and Phase 7 (ITR JSON) usage only.
from .filing.tax_calculation.tax_calculation_service import TaxCalculationService
from .filing.tax_calculation.interest_234_service import Interest234Service

__all__ = [
    "TaxCalculationService",
    "Interest234Service",
]
