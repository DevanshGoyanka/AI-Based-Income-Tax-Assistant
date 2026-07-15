"""Compute tax use case."""
from typing import Optional
from uuid import UUID

from app.core.domain.computed_return import ComputedReturn
from app.core.interfaces.filing_repository import IFilingRepository
from app.core.interfaces.tax_engine import ITaxEngine


class ComputeTaxUseCase:
    """Compute tax for a filing using tax engine (OpenTax adapter)."""
    
    def __init__(
        self,
        filing_repo: IFilingRepository,
        tax_engine: ITaxEngine
    ):
        self.filing_repo = filing_repo
        self.tax_engine = tax_engine
    
    async def execute(
        self,
        filing_id: UUID,
        regime: Optional[str] = None
    ) -> ComputedReturn:
        """Compute tax for filing.
        
        Args:
            filing_id: Filing ID
            regime: Tax regime override (optional)
            
        Returns:
            ComputedReturn with tax computation results
            
        Raises:
            ValueError: If filing not found
        """
        filing = await self.filing_repo.get_by_id(filing_id)
        if not filing:
            raise ValueError(f"Filing {filing_id} not found")
        
        # Use regime from filing if not overridden
        tax_regime = regime or filing.regime
        
        # Extract schedules from filing
        schedules = list(filing.schedules.values())
        
        # Compute tax via tax engine (OpenTax adapter)
        computed = await self.tax_engine.compute_tax(
            schedules=schedules,
            regime=tax_regime,
            ay=filing.ay
        )
        
        # Update filing status
        filing.status = "computed"
        await self.filing_repo.save(filing)
        
        return computed
