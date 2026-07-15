"""Submit filing use case."""
from uuid import UUID

from app.core.domain.filing import Filing, FilingStatus
from app.core.interfaces.filing_repository import IFilingRepository


class SubmitFilingUseCase:
    """Submit validated filing to ITD portal."""
    
    def __init__(self, filing_repo: IFilingRepository):
        self.filing_repo = filing_repo
    
    async def execute(self, filing_id: UUID) -> Filing:
        """Submit filing to ITD.
        
        Args:
            filing_id: Filing ID
            
        Returns:
            Updated Filing with submitted status
        """
        filing = await self.filing_repo.get_by_id(filing_id)
        if not filing:
            raise ValueError(f"Filing {filing_id} not found")
        
        if filing.status.value != "validated":
            raise ValueError(
                f"Filing must be validated before submission. Current status: {filing.status.value}"
            )
        
        # TODO: Integrate with ITD API
        # For now, mark as submitted
        filing.status = FilingStatus(FilingStatus.SUBMITTED)
        await self.filing_repo.save(filing)
        
        return filing
