"""Client repository interface."""
from abc import ABC, abstractmethod
from typing import List, Optional
from uuid import UUID


class Client:
    """Client aggregate (placeholder - will be defined in domain layer)."""
    
    def __init__(
        self,
        id: UUID,
        pan: str,
        name: str,
        dob: str = None,
        email: str = None,
        mobile: str = None
    ):
        self.id = id
        self.pan = pan
        self.name = name
        self.dob = dob
        self.email = email
        self.mobile = mobile


class IClientRepository(ABC):
    """Client repository interface."""
    
    @abstractmethod
    async def get_by_id(self, client_id: UUID) -> Optional[Client]:
        """Get client by ID."""
        pass
    
    @abstractmethod
    async def get_by_pan(self, pan: str) -> Optional[Client]:
        """Get client by PAN."""
        pass
    
    @abstractmethod
    async def save(self, client: Client) -> None:
        """Save client (create or update)."""
        pass
    
    @abstractmethod
    async def delete(self, client_id: UUID) -> None:
        """Delete client."""
        pass
    
    @abstractmethod
    async def list_all(self, user_id: UUID) -> List[Client]:
        """List all clients for a user."""
        pass
