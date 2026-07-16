"""Detailed Section 80D entries for Schedule80D.
Used in: ITR-2, ITR-3, ITR-4.
"""
from dataclasses import dataclass, field
from datetime import date
from decimal import Decimal
from typing import List, Optional
from uuid import UUID, uuid4

from ..value_objects import Money
from .base import ValidationError


COVER_TYPE_CODES = {
    "SELF": "Self",
    "FAMILY": "Family",
    "FAMILY_FLOATER": "Family Floater",
    "SELF_SENIOR": "Self (Senior Citizen)",
    "PARENTS": "Parents",
    "PARENTS_SENIOR": "Parents (Senior Citizen)",
    "PREVENTIVE": "Preventive Health Checkup",
}


@dataclass
class Section80DEntry:
    """Single 80D health insurance premium entry."""
    id: UUID = field(default_factory=uuid4)
    insurer_name: str = ""
    policy_number: str = ""
    premium: int = 0
    cover_type: str = ""          # From COVER_TYPE_CODES
    policy_start_date: Optional[date] = None
    is_preventive_checkup: bool = False
    is_critical_illness_rider: bool = False
    is_ayush: bool = False       # AYUSH treatment
    pin_code: str = ""

    def is_self(self) -> bool:
        return self.cover_type in ("SELF", "SELF_SENIOR", "FAMILY", "FAMILY_FLOATER")

    def is_parent(self) -> bool:
        return self.cover_type in ("PARENTS", "PARENTS_SENIOR")


@dataclass
class Schedule80D:
    """Schedule 80D — Itemized Health Insurance Premiums.
    
    Deduction limits:
    - Self/Family (non-senior): ₹25,000
    - Self/Family (senior citizen): ₹50,000
    - Parents (non-senior): ₹25,000
    - Parents (senior citizen): ₹50,000
    - Preventive checkup: ₹5,000 (within respective limit)
    """
    entries: List[Section80DEntry] = field(default_factory=list)

    def total_self_family(self) -> int:
        self_entries = [e for e in self.entries if e.is_self() and not e.is_preventive_checkup]
        senior = any(e.cover_type == "SELF_SENIOR" for e in self_entries)
        limit = 50_000 if senior else 25_000
        total = sum(e.premium for e in self_entries)
        return min(total, limit)

    def total_parents(self) -> int:
        parent_entries = [e for e in self.entries if e.is_parent() and not e.is_preventive_checkup]
        senior = any(e.cover_type == "PARENTS_SENIOR" for e in parent_entries)
        limit = 50_000 if senior else 25_000
        total = sum(e.premium for e in parent_entries)
        return min(total, limit)

    def total_preventive(self) -> int:
        checkup_entries = [e for e in self.entries if e.is_preventive_checkup]
        return min(sum(e.premium for e in checkup_entries), 5_000)

    def total_deduction(self) -> int:
        return self.total_self_family() + self.total_parents() + self.total_preventive()

    def to_itr_json(self) -> dict:
        return {
            "Schedule80D": {
                "DeductUndSec80D": [
                    {
                        "InsurerName": e.insurer_name,
                        "PolicyNo": e.policy_number,
                        "Premium": e.premium,
                        "CoverType": COVER_TYPE_CODES.get(e.cover_type, e.cover_type),
                        "DateOfPolicy": str(e.policy_start_date) if e.policy_start_date else "",
                    }
                    for e in self.entries
                ],
                "TotalAmount": self.total_deduction(),
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        for e in self.entries:
            if e.premium < 0:
                errors.append(ValidationError(
                    field="Schedule80D.premium",
                    message=f"Premium cannot be negative for {e.insurer_name}",
                    severity="BLOCKING"
                ))
        return errors
