"""Generate ITR JSON use case."""
from typing import Optional
from uuid import UUID

from app.core.interfaces.filing_repository import IFilingRepository
from app.core.interfaces.itr_builder import IITRBuilder


class GenerateITRJsonUseCase:
    """Generate ITR JSON from filing schedules."""
    
    def __init__(
        self,
        filing_repo: IFilingRepository,
        itr_builder: IITRBuilder
    ):
        self.filing_repo = filing_repo
        self.itr_builder = itr_builder
    
    async def execute(
        self,
        filing_id: UUID,
        snapshot_id: Optional[UUID] = None
    ) -> dict:
        """Generate ITR JSON.
        
        Args:
            filing_id: Filing ID
            snapshot_id: Optional specific computation snapshot
            
        Returns:
            ITR JSON matching CBDT schema
        """
        filing = await self.filing_repo.get_by_id(filing_id)
        if not filing:
            raise ValueError(f"Filing {filing_id} not found")
        
        if filing.status.value not in ("computed", "validated", "submitted"):
            raise ValueError(
                f"Filing must be computed before generating JSON. Current status: {filing.status.value}"
            )
        
        schedules = list(filing.schedules.values())
        
        # TODO: Load computed snapshot by ID if provided
        # For now, assume latest computation is available
        computed = None  # Load from snapshot_repo
        
        itr_json = await self.itr_builder.build(schedules, computed)
        
        filing.status.value = "validated"
        await self.filing_repo.save(filing)
        
        return itr_json
