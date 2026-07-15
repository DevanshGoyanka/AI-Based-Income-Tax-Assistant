"""Base schedule entity."""
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import List, Generic, TypeVar
from uuid import UUID
from decimal import Decimal

T = TypeVar('T')


@dataclass
class ValidationError:
    """Validation error."""
    field: str
    message: str
    severity: str = "BLOCKING"


@dataclass
class Schedule(ABC, Generic[T]):
    """Abstract base for all ITR schedules."""
    
    id: UUID
    filing_id: UUID
    ay: str
    
    @abstractmethod
    def compute_income(self) -> Decimal:
        """Compute total income from this schedule."""
        pass
    
    @abstractmethod
    def to_itr_json(self) -> dict:
        """Export to ITR JSON format."""
        pass
    
    @abstractmethod
    def validate(self) -> List[ValidationError]:
        """Validate schedule data."""
        pass
