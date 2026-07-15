"""Response mapper — OpenTax FilingModel to ERP ComputedReturn."""
from uuid import UUID, uuid4
from decimal import Decimal
from typing import Optional

from app.core.domain.computed_return import ComputedReturn, SlabBreakdown
from app.adapters.opentax.vendor.filing.models.filing_model import FilingModel
from app.adapters.opentax.vendor.filing.tax_calculation.models.tax_regime_breakdown import (
    TaxRegimeBreakdownModel,
    TaxSlabEntry,
)


class OpenTaxToERPMapper:
    """Maps OpenTax FilingModel results to ERP ComputedReturn."""

    def map_computation(
        self,
        filing_id: Optional[UUID],
        client_id: Optional[UUID],
        pan: str,
        ay: str,
        itr_form: str,
        opentax_result: FilingModel,
        regime: str
    ) -> ComputedReturn:
        """Convert OpenTax FilingModel to ERP ComputedReturn.

        TaxCalculationService.calculate() returns a FilingModel with the
        tax_computation field populated. We extract the regime-specific
        breakdown from tax_computation.current_regime (or old_regime / new_regime).

        Args:
            filing_id: Filing UUID
            client_id: Client UUID
            pan: Client PAN
            ay: Assessment year
            itr_form: ITR form type
            opentax_result: FilingModel returned by OpenTax (with tax_computation set)
            regime: "old" or "new"

        Returns:
            ComputedReturn domain entity
        """
        tc = opentax_result.tax_computation
        if tc is None:
            raise ValueError(
                f"OpenTax FilingModel has no tax_computation. "
                f"Ensure TaxCalculationService.calculate() was called correctly."
            )

        # Select the right regime breakdown
        breakdown: Optional[TaxRegimeBreakdownModel] = None
        if regime == "new":
            breakdown = tc.new_regime
        elif regime == "old":
            breakdown = tc.old_regime

        if breakdown is None:
            raise ValueError(
                f"OpenTax tax_computation has no {regime!r} regime breakdown. "
                f"Available: current={tc.current_regime is not None}, "
                f"old={tc.old_regime is not None}, new={tc.new_regime is not None}"
            )

        # Extract slab breakdown
        slab_breakdown = self._map_slabs(breakdown.slab_breakdown or [])

        # Build flat payload dict with all regime fields
        payload = self._breakdown_to_dict(breakdown)
        payload["_source"] = "opentax@90c60bb"

        return ComputedReturn(
            id=uuid4(),
            filing_id=filing_id or uuid4(),
            client_id=client_id or uuid4(),
            pan=pan or "UNKNOWN",
            ay=ay,
            itr_form=itr_form,
            regime=regime,
            rule_version=f"AY-{ay}-{regime}-opentax@90c60bb",
            payload=payload,
            slab_breakdown=slab_breakdown,
        )

    def _map_slabs(
        self, slabs: list[TaxSlabEntry]
    ) -> list[SlabBreakdown]:
        """Convert OpenTax TaxSlabEntry list to ERP SlabBreakdown."""
        result: list[SlabBreakdown] = []
        for slab in slabs:
            result.append(SlabBreakdown(
                from_amount=float(slab.from_amount) if slab.from_amount else 0.0,
                to_amount=float(slab.to_amount) if slab.to_amount else None,
                rate=float(slab.rate) if slab.rate else 0.0,
                taxable_amount=float(slab.taxable_amount) if slab.taxable_amount else 0.0,
                tax=float(slab.tax) if slab.tax else 0.0,
            ))
        return result

    def _breakdown_to_dict(self, b: TaxRegimeBreakdownModel) -> dict:
        """Flatten a TaxRegimeBreakdownModel into a dict for ComputedReturn.payload."""
        def dec(v) -> float:
            if v is None:
                return 0.0
            if isinstance(v, Decimal):
                return float(v)
            return float(v)

        return {
            "regime": b.regime or "",
            "gross_total_income": dec(b.gross_total_income),
            "total_deductions": dec(b.total_deductions),
            "total_income": dec(b.total_income),
            "tax_before_rebate": dec(b.tax_before_rebate),
            "rebate_87a": dec(b.rebate_87a),
            "surcharge": dec(b.surcharge),
            "tax_after_rebate": dec(b.tax_after_rebate),
            "health_education_cess": dec(b.health_education_cess),
            "total_tax_liability": dec(b.total_tax_liability),
            "tds": dec(b.tds),
            "tcs": dec(b.tcs),
            "advance_tax": dec(b.advance_tax),
            "total_taxes_paid": dec(b.total_taxes_paid),
            "tax_payable": dec(b.tax_payable),
            "refund": dec(b.refund),
            "tax_intrest": dec(b.tax_intrest),
        }
