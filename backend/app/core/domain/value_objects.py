"""Value objects for domain model."""
from dataclasses import dataclass
from decimal import Decimal
import re


@dataclass(frozen=True)
class PAN:
    """PAN value object with validation."""
    value: str
    
    def __post_init__(self):
        if not re.match(r'^[A-Z]{5}[0-9]{4}[A-Z]$', self.value):
            raise ValueError(f"Invalid PAN format: {self.value}")
    
    def __str__(self) -> str:
        return self.value


@dataclass(frozen=True)
class Money:
    """Money in paise (to avoid float precision issues)."""
    paise: int
    
    @classmethod
    def from_rupees(cls, rupees: int | Decimal) -> "Money":
        """Create Money from rupees."""
        return cls(int(Decimal(str(rupees)) * 100))
    
    def to_rupees(self) -> Decimal:
        """Convert to rupees."""
        return Decimal(self.paise) / 100
    
    def __add__(self, other: "Money") -> "Money":
        return Money(self.paise + other.paise)
    
    def __sub__(self, other: "Money") -> "Money":
        return Money(self.paise - other.paise)
    
    def __mul__(self, factor: Decimal) -> "Money":
        return Money(int(self.paise * factor))


@dataclass(frozen=True)
class AssessmentYear:
    """Assessment year value object."""
    value: str
    
    def __post_init__(self):
        if not re.match(r'^\d{4}-\d{2}$', self.value):
            raise ValueError(f"Invalid AY format: {self.value}")
    
    def to_fy(self) -> str:
        """Convert to financial year."""
        start = int(self.value[:4])
        return f"{start - 1}-{str(start)[-2:]}"
    
    def __str__(self) -> str:
        return self.value


@dataclass(frozen=True)
class TaxRegime:
    """Tax regime value object."""
    value: str
    
    def __post_init__(self):
        if self.value not in ("old", "new"):
            raise ValueError(f"Invalid regime: {self.value}")
    
    def __str__(self) -> str:
        return self.value
