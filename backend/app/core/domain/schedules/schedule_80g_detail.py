"""Detailed Schedule 80G donations.
Used in: ITR-2, ITR-3, ITR-4.
"""
from dataclasses import dataclass, field
from decimal import Decimal
from typing import List
from uuid import UUID, uuid4

from ..value_objects import Money
from .base import ValidationError


@dataclass
class DoneeWithPAN:
    """Donation entry with PAN of donee."""
    donee_name: str = ""
    donee_pan: str = ""
    arn_number: str = ""          # Donation reference number (ARN)
    address: str = ""
    city: str = ""
    state: str = ""
    pin_code: str = ""
    donation_cash: int = 0        # Donation made in cash
    donation_other: int = 0       # Donation via other mode (cheque, etc.)
    transaction_ref: str = ""
    ifsc_code: str = ""

    def total_donation(self) -> int:
        return self.donation_cash + self.donation_other


@dataclass
class Schedule80G:
    """Schedule 80G — Itemized Donations.
    
    Donation categories:
    1. 100% eligible without limit: PM's National Relief Fund, etc.
    2. 100% eligible with limit: Certain approved funds
    3. 50% eligible without limit: National Defence Fund, etc.
    4. 50% eligible with limit: Most approved charities
    
    Note: Cash donations >₹2,000 NOT allowed as deduction.
    
    For political parties (NOT in Schedule80G, but 80GGC):
    - 100% deductible
    - E-filing without Aadhaar NOT allowed for electronic donations
    """
    # 100% eligible — no qualifying limit
    don_100_percent_no_limit: List[DoneeWithPAN] = field(default_factory=list)

    # 100% eligible — with qualifying limit (10% of GTI)
    don_100_percent_with_limit: List[DoneeWithPAN] = field(default_factory=list)

    # 50% eligible — no qualifying limit
    don_50_percent_no_limit: List[DoneeWithPAN] = field(default_factory=list)

    # 50% eligible — with qualifying limit (10% of GTI)
    don_50_percent_with_limit: List[DoneeWithPAN] = field(default_factory=list)

    def _total(self, entries: List[DoneeWithPAN], rate: float) -> int:
        return int(sum(e.total_donation() for e in entries) * rate)

    def total_100_no_limit(self) -> int:
        return sum(e.total_donation() for e in self.don_100_percent_no_limit)

    def total_100_with_limit(self) -> int:
        return sum(e.total_donation() for e in self.don_100_percent_with_limit)

    def total_50_no_limit(self) -> int:
        return int(sum(e.total_donation() for e in self.don_50_percent_no_limit) * 0.5)

    def total_50_with_limit(self) -> int:
        return int(sum(e.total_donation() for e in self.don_50_percent_with_limit) * 0.5)

    def total_donations_cash(self) -> int:
        all_entries = (
            self.don_100_percent_no_limit +
            self.don_100_percent_with_limit +
            self.don_50_percent_no_limit +
            self.don_50_percent_with_limit
        )
        return sum(e.donation_cash for e in all_entries)

    def total_donations_other_mode(self) -> int:
        all_entries = (
            self.don_100_percent_no_limit +
            self.don_100_percent_with_limit +
            self.don_50_percent_no_limit +
            self.don_50_percent_with_limit
        )
        return sum(e.donation_other for e in all_entries)

    def total_donations(self) -> int:
        all_entries = (
            self.don_100_percent_no_limit +
            self.don_100_percent_with_limit +
            self.don_50_percent_no_limit +
            self.don_50_percent_with_limit
        )
        return sum(e.total_donation() for e in all_entries)

    def total_eligible_deduction(self) -> int:
        """Total eligible 80G deduction (after applying rates)."""
        return (
            self.total_100_no_limit() +
            self.total_100_with_limit() +
            self.total_50_no_limit() +
            self.total_50_with_limit()
        )

    def to_itr_json(self) -> dict:
        def entries_json(entries: List[DoneeWithPAN]) -> List[dict]:
            return [
                {
                    "DoneeWithPanName": e.donee_name,
                    "DoneePAN": e.donee_pan,
                    "ArnNbr": e.arn_number,
                    "AddressDetail": {"AddressDetail": e.address, "City": e.city, "State": e.state, "PinCode": e.pin_code},
                    "DonationAmtCash": e.donation_cash,
                    "DonationAmtOtherMode": e.donation_other,
                    "TransactionRefNum": e.transaction_ref,
                    "IFSCCode": e.ifsc_code,
                    "DonationAmt": e.total_donation(),
                    "EligibleDonationAmt": e.total_donation(),  # Rate applied at total level
                }
                for e in entries
            ]

        return {
            "Schedule80G": {
                "Don100Percent": {"DoneeWithPan": entries_json(self.don_100_percent_no_limit)},
                "Don50PercentNoApprReqd": {"DoneeWithPan": entries_json(self.don_50_percent_no_limit)},
                "Don100PercentApprReqd": {"DoneeWithPan": entries_json(self.don_100_percent_with_limit)},
                "Don50PercentApprReqd": {"DoneeWithPan": entries_json(self.don_50_percent_with_limit)},
                "TotalDonationsUs80GCash": self.total_donations_cash(),
                "TotalDonationsUs80GOtherMode": self.total_donations_other_mode(),
                "TotalDonationsUs80G": self.total_donations(),
                "TotalEligibleDonationsUs80G": self.total_eligible_deduction(),
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        all_entries = (
            self.don_100_percent_no_limit +
            self.don_100_percent_with_limit +
            self.don_50_percent_no_limit +
            self.don_50_percent_with_limit
        )
        for e in all_entries:
            if e.donation_cash > 2000:
                errors.append(ValidationError(
                    field="Schedule80G.cash_donation",
                    message=f"Cash donation of ₹{e.donation_cash} exceeds ₹2,000 limit for {e.donee_name}",
                    severity="BLOCKING"
                ))
            if e.donation_cash > 0 and e.donation_other == 0:
                pass  # OK - all cash donation
            # Check PAN format (10 chars, last char may be letter or digit)
        return errors


@dataclass
class Section80GGAEntry:
    """80GGA — Scientific Research / Rural Development Donations."""
    id: UUID = field(default_factory=uuid4)
    donee_name: str = ""
    donee_pan: str = ""
    clause: str = ""  # "ii" (scientific research), "iia" (rural dev), "iii" (NIT/IES), "iv" (BC fund)
    amount: int = 0
    address: str = ""
    is_scientific_research: bool = False
    is_national_poverty_eradication: bool = False

    def eligible_deduction(self) -> int:
        return self.amount  # 100% deduction


@dataclass
class Schedule80GGA:
    """Schedule 80GGA — Donations for Scientific Research / Rural Dev."""
    entries: List[Section80GGAEntry] = field(default_factory=list)

    def total_deduction(self) -> int:
        return sum(e.eligible_deduction() for e in self.entries)

    def to_itr_json(self) -> dict:
        return {
            "Schedule80GGA": {
                "DonationDetail": [
                    {
                        "DonorName": e.donee_name,
                        "DonorPAN": e.donee_pan,
                        "Clause": e.clause,
                        "Amount": e.amount,
                        "Address": e.address,
                    }
                    for e in self.entries
                ],
                "TotalAmount": self.total_deduction(),
            }
        }

    def validate(self) -> List[ValidationError]:
        return []


@dataclass
class Section80GGCEntry:
    """80GGC — Political Party Donations."""
    id: UUID = field(default_factory=uuid4)
    political_party_name: str = ""
    political_party_pan: str = ""
    amount: int = 0
    date: str = ""  # "DD/MM/YYYY"
    mode: str = ""   # "Cash", "Cheque", "NEFT", "RTGS"

    def eligible_deduction(self) -> int:
        # No deduction if cash >₹2,000
        if self.mode == "Cash" and self.amount > 2000:
            return 0
        return self.amount


@dataclass
class Schedule80GGC:
    """Schedule 80GGC — Political Party Donations."""
    entries: List[Section80GGCEntry] = field(default_factory=list)

    def total_deduction(self) -> int:
        return sum(e.eligible_deduction() for e in self.entries)

    def to_itr_json(self) -> dict:
        return {
            "Schedule80GGC": {
                "DonationDetail": [
                    {
                        "PoliticalPartyName": e.political_party_name,
                        "PoliticalPartyPAN": e.political_party_pan,
                        "Amount": e.amount,
                        "Date": e.date,
                        "Mode": e.mode,
                    }
                    for e in self.entries
                ],
                "TotalAmount": self.total_deduction(),
            }
        }

    def validate(self) -> List[ValidationError]:
        return []
