"""Create filing use case."""
from uuid import UUID, uuid4

from app.core.domain.filing import Filing, FilingStatus
from app.core.domain.value_objects import AssessmentYear, TaxRegime
from app.core.interfaces.filing_repository import IFilingRepository


class CreateFilingUseCase:
    """Create new filing for client."""
    
    def __init__(self, filing_repo: IFilingRepository):
        self.filing_repo = filing_repo
    
    async def execute(
        self,
        client_id: UUID,
        ay: str,
        regime: str = "new"
    ) -> Filing:
        """Create new filing.
        
        Args:
            client_id: Client UUID
            ay: Assessment year (e.g., "2026-27")
            regime: Tax regime ("old" or "new")
            
        Returns:
            Created Filing entity
        """
        existing = await self.filing_repo.get_by_client_and_ay(
            client_id,
            AssessmentYear(ay)
        )
        
        if existing:
            raise ValueError(f"Filing already exists for client {client_id} AY {ay}")
        
        filing = Filing(
            id=uuid4(),
            client_id=client_id,
            ay=AssessmentYear(ay),
            regime=TaxRegime(regime),
            status=FilingStatus(FilingStatus.DRAFT),
            schedules={}
        )
        
        await self.filing_repo.save(filing)
        
        return filing
