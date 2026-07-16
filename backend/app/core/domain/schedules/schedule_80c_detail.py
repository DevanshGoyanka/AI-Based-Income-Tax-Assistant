"""Detailed Section 80C entries for Schedule80C.
Used in: ITR-2, ITR-3, ITR-4.
"""
from dataclasses import dataclass, field
from datetime import date
from decimal import Decimal
from typing import List, Optional
from uuid import UUID, uuid4

from ..value_objects import Money
from .base import ValidationError


INVESTMENT_TYPE_CODES = {
    "LIC": "LIC Life Insurance Premium",
    "PPF": "Public Provident Fund",
    "ELSS": "Equity Linked Savings Scheme",
    "NSC": "National Savings Certificate",
    "KV": "Kisan Vikas Patra",
    "SSY": "Sukanya Samriddhi Yojana",
    "EPF": "EPF Contribution (Employee)",
    "VPF": "Voluntary Provident Fund",
    "HOME_LOAN": "Home Loan Principal Repayment",
    "TUITION": "Children Tuition Fees",
    "INFRA_BONDS": "Infrastructure Bonds",
    "TAX_SAVER_FD": "Tax Saver Fixed Deposit",
    "PENSION": "Annuity Fund / Pension (80CCC)",
    "NPS_EMP": "NPS Employee Contribution (80CCD(1))",
    "OTHER": "Other Eligible Investment",
}


@dataclass
class Section80CEntry:
    """Single 80C investment / payment entry."""
    id: UUID = field(default_factory=uuid4)
    investment_type: str = ""     # Code from INVESTMENT_TYPE_CODES
    description: str = ""
    amount: int = 0               # Amount invested / paid
    date_of_payment: Optional[date] = None
    reference_number: str = ""
    policy_number: str = ""
    is_80ccc: bool = False        # Annuity/pension (80CCC)
    is_80ccd1: bool = False      # NPS employee (80CCD(1))
    is_80ccd2_employer: bool = False  # Employer NPS (80CCD(2)) — separate schedule

    def is_80c(self) -> bool:
        return not (self.is_80ccc or self.is_80ccd1)


@dataclass
class Schedule80C:
    """Schedule 80C — Itemized 80C deductions.
    
    Combined limit for 80C + 80CCC + 80CCD(1) = ₹1,50,000.
    
    Components:
    - 80C: LIC, PPF, ELSS, NSC, KV, SSY, EPF, VPF, home loan principal, tuition fees, etc.
    - 80CCC: Annuity from LIC or approved pension fund
    - 80CCD(1): NPS employee contribution (within ₹1.5L)
    """
    entries: List[Section80CEntry] = field(default_factory=list)

    def total_80c(self) -> int:
        """Total 80C deductions (within ₹1.5L cap)."""
        total = sum(e.amount for e in self.entries if e.is_80c())
        return min(total, 150_000)

    def total_80ccc(self) -> int:
        return sum(e.amount for e in self.entries if e.is_80ccc)

    def total_80ccd1(self) -> int:
        return sum(e.amount for e in self.entries if e.is_80ccd1)

    def combined_80c_ccc_ccd1(self) -> int:
        """80C + 80CCC + 80CCD(1) combined cap ₹1.5L."""
        combined = sum(e.amount for e in self.entries)
        return min(combined, 150_000)

    def to_itr_json(self) -> dict:
        entries_json = []
        for e in self.entries:
            entries_json.append({
                "InvestmentType": INVESTMENT_TYPE_CODES.get(e.investment_type, e.description),
                "Amount": e.amount,
                "DateOfPayment": str(e.date_of_payment) if e.date_of_payment else "",
                "ReferenceNo": e.reference_number,
            })
        return {
            "Schedule80C": {
                "EltgAmnt": entries_json,
                "TotalAmount": self.combined_80c_ccc_ccd1(),
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        for e in self.entries:
            if e.amount < 0:
                errors.append(ValidationError(
                    field="Schedule80C.entries",
                    message=f"Amount cannot be negative for {e.investment_type}",
                    severity="BLOCKING"
                ))
        # Note: Combined limit is validated at tax engine level
        return errors
