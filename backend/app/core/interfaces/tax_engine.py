"""Tax engine interface."""
from abc import ABC, abstractmethod
from typing import List

from app.core.domain.schedules.base import Schedule
from app.core.domain.value_objects import TaxRegime, AssessmentYear
from app.core.domain.computed_return import ComputedReturn


class ITaxEngine(ABC):
    """Tax computation engine interface.
    
    OpenTax is ONE implementation. Can be swapped for CustomEngine or CommercialEngine.
    """
    
    @abstractmethod
    async def compute_tax(
        self,
        schedules: List[Schedule],
        regime: TaxRegime,
        ay: AssessmentYear
    ) -> ComputedReturn:
        """Compute tax from schedules.
        
        Args:
            schedules: List of income/deduction schedules
            regime: Tax regime (old/new)
            ay: Assessment year
            
        Returns:
            ComputedReturn with tax breakdown
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
