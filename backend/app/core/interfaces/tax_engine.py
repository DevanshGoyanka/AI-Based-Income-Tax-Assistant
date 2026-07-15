"""Tax engine interface.

OpenTax is ONE historical implementation.
All production computation uses our owned TaxEngine (app.core.services.tax_engine).
"""
from abc import ABC, abstractmethod
from datetime import date
from typing import List, Optional
from uuid import UUID

from app.core.domain.schedules.base import Schedule
from app.core.domain.value_objects import TaxRegime, AssessmentYear
from app.core.domain.computed_return import ComputedReturn


class ITaxEngine(ABC):
    """Tax computation engine interface.

    All production code calls TaxEngine (our own implementation).
    OpenTax is no longer called at runtime (only used as test oracle).
    """

    @abstractmethod
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
        """Compute tax from schedules.

        Args:
            schedules: List of income/deduction schedules.
            regime: Tax regime (old/new).
            ay: Assessment year.
            dob: Date of birth for age-based slabs/surcharge.
            pan: PAN number (for metadata).
            filing_id: Filing UUID (for metadata).
            client_id: Client UUID (for metadata).
            filing_date: Date of filing (for 234A/234F interest).
            due_date: Due date for filing (defaults to Jul 31 AY).

        Returns:
            ComputedReturn with tax breakdown.
        """
        pass

    @abstractmethod
    def explain_computation(
        self,
        computed: ComputedReturn
    ) -> List[dict]:
        """Generate computation explanation for CA review.

        Returns list of steps showing how tax was calculated.
        """
        pass
