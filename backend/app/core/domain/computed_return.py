"""Computed return aggregate - immutable tax computation snapshot."""
from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal
from typing import List, Optional
from uuid import UUID, uuid4


@dataclass
class TaxBreakdown:
    """Detailed tax breakdown."""
    gross_total_income: int = 0
    total_deductions: int = 0
    total_income: int = 0
    tax_before_rebate: int = 0
    rebate_87a: int = 0
    surcharge: int = 0
    cess: int = 0
    total_tax_liability: int = 0
    tds: int = 0
    tcs: int = 0
    advance_tax: int = 0
    tax_payable: int = 0
    refund: int = 0


@dataclass
class InterestBreakdown:
    """Interest calculation breakdown."""
    interest_234a: int = 0
    interest_234b: int = 0
    interest_234c: int = 0
    late_fee_234f: int = 0


@dataclass
class SlabBreakdown:
    """Individual slab breakdown."""
    from_amount: int
    to_amount: Optional[int]
    rate: float
    taxable_amount: int
    tax: int


@dataclass
class ComputationStep:
    """Individual computation step for audit trail."""
    step: str
    description: str
    input_value: str
    output_value: str
    rule_applied: str = ""


@dataclass
class ComputedReturn:
    """Immutable aggregate for tax computation results.
    
    Created as snapshot after each tax computation.
    Never modified - new instance created for each computation.
    """
    id: UUID
    filing_id: UUID
    client_id: UUID
    pan: str
    ay: str
    itr_form: str
    regime: str
    rule_version: str
    payload: dict
    slab_breakdown: List[SlabBreakdown] = field(default_factory=list)
    explanation: List[ComputationStep] = field(default_factory=list)
    created_at: datetime = field(default_factory=datetime.utcnow)
    
    @classmethod
    def create(
        cls,
        filing_id: UUID,
        client_id: UUID,
        pan: str,
        ay: str,
        itr_form: str,
        regime: str,
        payload: dict,
        rule_version: str
    ) -> "ComputedReturn":
        """Create new immutable computation snapshot."""
        return cls(
            id=uuid4(),
            filing_id=filing_id,
            client_id=client_id,
            pan=pan,
            ay=ay,
            itr_form=itr_form,
            regime=regime,
            rule_version=rule_version,
            payload=payload
        )
    
    def to_dict(self) -> dict:
        """Serialize to dictionary for storage."""
        return {
            "id": str(self.id),
            "filing_id": str(self.filing_id),
            "client_id": str(self.client_id),
            "pan": self.pan,
            "ay": self.ay,
            "itr_form": self.itr_form,
            "regime": self.regime,
            "rule_version": self.rule_version,
            "payload": self.payload,
            "slab_breakdown": [
                {
                    "from_amount": sb.from_amount,
                    "to_amount": sb.to_amount,
                    "rate": sb.rate,
                    "taxable_amount": sb.taxable_amount,
                    "tax": sb.tax
                }
                for sb in self.slab_breakdown
            ],
            "explanation": [
                {
                    "step": e.step,
                    "description": e.description,
                    "input_value": e.input_value,
                    "output_value": e.output_value,
                    "rule_applied": e.rule_applied
                }
                for e in self.explanation
            ],
            "created_at": self.created_at.isoformat()
        }
    
    @property
    def total_tax_liability(self) -> int:
        """Get total tax liability from payload."""
        return self.payload.get("total_tax_liability", 0)
    
    @property
    def tax_payable(self) -> int:
        """Get tax payable from payload."""
        return self.payload.get("tax_payable", 0)
    
    @property
    def refund(self) -> int:
        """Get refund amount from payload."""
        return self.payload.get("refund", 0)
