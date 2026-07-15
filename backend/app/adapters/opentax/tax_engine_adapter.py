"""TaxEngineAdapter — bridges ITaxEngine interface to our owned TaxEngine.

OpenTax is no longer called at runtime. This adapter delegates entirely to
our owned TaxEngine (app.core.services.tax_engine).

OpenTax is kept as a vendored test oracle (reference-only) for:
  - Cross-validating our owned engine's output
  - Regression testing when rules change
  - Comparing slab/deduction calculations

DO NOT add any OpenTax calls to the compute_tax() path.
"""
from datetime import date
from typing import List, Optional
from uuid import UUID

from app.core.domain.schedules.base import Schedule
from app.core.domain.value_objects import TaxRegime, AssessmentYear
from app.core.domain.computed_return import ComputedReturn, SlabBreakdown
from app.core.interfaces.tax_engine import ITaxEngine
from app.core.services.tax_engine import TaxEngine, TaxEngineResult


class TaxEngineAdapter(ITaxEngine):
    """ITaxEngine implementation backed by our owned TaxEngine.

    This is what application code injects as ITaxEngine.
    Production tax computation goes through here → TaxEngine.

    OpenTax is NOT called here at runtime.
    """

    def __init__(self):
        self._engine = TaxEngine()

    async def compute_tax(
        self,
        schedules: List[Schedule],
        regime: TaxRegime,
        ay: AssessmentYear,
        dob: Optional[date] = None,
        pan: str = "",
        filing_id: Optional[UUID] = None,
        client_id: Optional[UUID] = None,
        filing_date: Optional[date] = None,
        due_date: Optional[date] = None,
    ) -> ComputedReturn:
        """Compute tax using our owned TaxEngine.

        Passes through to app.core.services.tax_engine.TaxEngine.
        No OpenTax calls in this path.
        """
        # Accept both str and TaxRegime
        regime_str = str(regime) if isinstance(regime, TaxRegime) else regime
        ay_str = str(ay) if isinstance(ay, AssessmentYear) else ay

        result: TaxEngineResult = self._engine.compute(
            schedules=schedules,
            regime=regime_str,
            ay=ay_str,
            dob=dob,
            pan=pan,
            filing_id=filing_id,
            client_id=client_id,
            filing_date=filing_date,
            due_date=due_date,
        )

        # Build ComputedReturn from our owned result
        computed = ComputedReturn(
            id=UUID("00000000-0000-0000-0000-000000000000"),  # Will be set by caller
            filing_id=filing_id or UUID("00000000-0000-0000-0000-000000000000"),
            client_id=client_id or UUID("00000000-0000-0000-0000-000000000001"),
            pan=pan,
            ay=ay_str,
            itr_form="ITR1",  # Default; caller can override
            regime=regime_str,
            rule_version=result.rule_version,
            payload=result.to_payload(),
            slab_breakdown=result.slab_breakdown,
            explanation=result.explanation,
        )
        return computed

    def explain_computation(
        self,
        computed: ComputedReturn,
    ) -> List[dict]:
        """Return step-by-step explanation from the ComputedReturn."""
        return [
            {
                "step": e.step,
                "description": e.description,
                "input_value": e.input_value,
                "output_value": e.output_value,
                "rule_applied": e.rule_applied,
            }
            for e in computed.explanation
        ]
