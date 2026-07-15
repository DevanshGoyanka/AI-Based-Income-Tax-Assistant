"""Filing repository interface."""
from abc import ABC, abstractmethod
from typing import List, Optional
from uuid import UUID

from app.core.domain.filing import Filing
from app.core.domain.value_objects import AssessmentYear


class IFilingRepository(ABC):
    """Filing repository interface."""
    
    @abstractmethod
    async def get_by_id(self, filing_id: UUID) -> Optional[Filing]:
        """Get filing by ID."""
        pass
    
    @abstractmethod
    async def get_by_client_and_ay(
        self, client_id: UUID, ay: AssessmentYear
    ) -> Optional[Filing]:
        """Get filing by client and assessment year."""
        pass
    
    @abstractmethod
    async def save(self, filing: Filing) -> None:
        """Save filing (create or update)."""
        pass
    
    @abstractmethod
    async def delete(self, filing_id: UUID) -> None:
        """Delete filing."""
        pass
    
    @abstractmethod
    async def list_by_client(self, client_id: UUID) -> List[Filing]:
        """List all filings for a client."""
        pass
