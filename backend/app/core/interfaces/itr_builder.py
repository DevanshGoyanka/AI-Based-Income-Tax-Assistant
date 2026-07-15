"""ITR builder interface."""
from abc import ABC, abstractmethod
from typing import List

from app.core.domain.computed_return import ComputedReturn
from app.core.domain.schedules.base import Schedule, ValidationError


class IITRBuilder(ABC):
    """ITR JSON generation interface - separate from tax computation."""
    
    @abstractmethod
    async def build(
        self,
        schedules: List[Schedule],
        computed: ComputedReturn
    ) -> dict:
        """Build ITR JSON from schedules and computation.
        
        Args:
            schedules: Domain schedules (Salary, HP, CG, etc.)
            computed: Tax computation result
            
        Returns:
            ITR JSON matching CBDT schema
        """
        pass
    
    @abstractmethod
    async def validate(
        self,
        itr_json: dict
    ) -> List[ValidationError]:
        """Validate ITR JSON against CBDT rules.
        
        Returns:
            List of validation errors (blocking + warnings)
        """
        pass
