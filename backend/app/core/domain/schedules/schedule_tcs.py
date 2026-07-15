"""Schedule TCS — Tax Collected at Source.

Implements Schedule TCS per Income Tax Act Section 206C.
TCS is collected by collectors on:
- Sale of motor vehicle > ₹10L (206C(1))
- Foreign travel > ₹7L (206C(1F))
- Sale of goods > ₹50L per buyer (206C(1H))
- Cash withdrawal > ₹1Cr from bank (206C(1G))
- Bullion/jewellery > ₹2L (206C(1C))

TCS credit can be claimed against tax liability.
Credit owner: 1=Self, 2=Spouse/Other.
"""
from dataclasses import dataclass, field
from datetime import date
from decimal import Decimal
from typing import List, Literal, Optional
from uuid import UUID, uuid4

from ..value_objects import Money
from .base import Schedule


TCSCreditOwner = Literal["1", "2"]  # 1=Self, 2=Spouse/Other


@dataclass
class TCSDetail:
    """Single TCS entry — one collector."""
    id: UUID = field(default_factory=uuid4)
    
    # Credit ownership
    credit_owner: TCSCreditOwner = "1"
    spouse_or_other_pan: Optional[str] = None  # Required if credit_owner = "2"
    
    # Collector info
    collector_tan: str = ""  # 10-char TAN of TCS collector
    collector_name: str = ""  # Name of TCS collector
    
    # Collection details
    financial_year: int = 0  # FY of collection (2008–2024)
    
    # TCS amounts
    tcs_collected_own_hand: Money = field(default_factory=lambda: Money.from_rupees(0))
    tcs_collected_spouse_hand: Money = field(default_factory=lambda: Money.from_rupees(0))
    
    # TDS brought forward (for credit)
    brought_forward_tds: Money = field(default_factory=lambda: Money.from_rupees(0))
    
    # Claim this year
    tcs_claimed_own_hand: Money = field(default_factory=lambda: Money.from_rupees(0))
    tcs_claimed_spouse_hand: Money = field(default_factory=lambda: Money.from_rupees(0))
    spouse_pan_for_claim: Optional[str] = None
    
    # Carried forward
    tcs_carried_forward: Money = field(default_factory=lambda: Money.from_rupees(0))
    
    @property
    def total_collected(self) -> Money:
        """Total TCS collected this year."""
        return self.tcs_collected_own_hand + self.tcs_collected_spouse_hand
    
    @property
    def total_claimed(self) -> Money:
        """Total TCS claimed this year."""
        return self.tcs_claimed_own_hand + self.tcs_claimed_spouse_hand
    
    def validate(self) -> List[str]:
        """Return list of validation errors, empty if valid."""
        errors = []
        if self.credit_owner not in ("1", "2"):
            errors.append("TCS credit_owner must be '1' (self) or '2' (spouse/other)")
        if self.credit_owner == "2" and not self.spouse_or_other_pan:
            errors.append("Spouse/Other PAN required when credit_owner is '2'")
        if len(self.collector_tan) != 10:
            errors.append("TCS collector TAN must be exactly 10 characters")
        if self.financial_year < 2008 or self.financial_year > 2024:
            errors.append(f"TCS financial_year must be between 2008 and 2024")
        return errors


@dataclass
class ScheduleTCS(Schedule):
    """Schedule TCS — Tax Collected at Source.
    
    TCS credit is available against tax liability in the same AY.
    Unclaimed TCS can be carried forward subject to provisions.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"  # AY in which credit is claimed
    
    # All TCS entries
    tcs_entries: List[TCSDetail] = field(default_factory=list)
    
    def total_tcs_collected(self) -> Money:
        """Total TCS collected across all entries."""
        return sum((e.total_collected for e in self.tcs_entries), Money.from_rupees(0))
    
    def total_tcs_claimed(self) -> Money:
        """Total TCS claimed this year."""
        return sum((e.total_claimed for e in self.tcs_entries), Money.from_rupees(0))
    
    def total_tcs_credit(self) -> Money:
        """Total TCS credit available (claimed, limited to collected)."""
        claimed = self.total_tcs_claimed()
        collected = self.total_tcs_collected()
        return Money.from_rupees(min(int(claimed.to_rupees()), int(collected.to_rupees())))
    
    def total_brought_forward(self) -> Money:
        """Total TDS brought forward across all entries."""
        return sum((e.brought_forward_tds for e in self.tcs_entries), Money.from_rupees(0))
    
    def validate(self) -> List[str]:
        """Validate all TCS entries."""
        errors = []
        for i, entry in enumerate(self.tcs_entries):
            entry_errors = entry.validate()
            if entry_errors:
                errors.extend(f"TCS entry {i+1}: {e}" for e in entry_errors)
        
        # Total claimed cannot exceed total collected + brought forward
        total_claimed = int(self.total_tcs_claimed().to_rupees())
        total_available = int((self.total_tcs_collected() + self.total_brought_forward()).to_rupees())
        if total_claimed > total_available:
            errors.append(
                f"Total TCS claimed ({total_claimed}) cannot exceed "
                f"total available ({total_available})"
            )
        return errors
    
    def to_json(self) -> dict:
        """Serialize to ITR JSON format."""
        tcs_list = []
        for entry in self.tcs_entries:
            tcs_list.append({
                "TCSCreditOwner": entry.credit_owner,
                "PANOfSpouseOrOthrPrsn": entry.spouse_or_other_pan,
                "EmployerOrDeductorOrCollectTAN": entry.collector_tan,
                "DeductedYr": entry.financial_year,
                "BroughtFwdTDSAmt": int(entry.brought_forward_tds.to_rupees()),
                "TCSCurrFYDtls": {
                    "TCSAmtCollOwnHand": int(entry.tcs_collected_own_hand.to_rupees()),
                    "TCSAmtCollSpouseOrOthrHand": int(entry.tcs_collected_spouse_hand.to_rupees()),
                },
                "TCSClaimedThisYearDtls": {
                    "TCSAmtCollOwnHand": int(entry.tcs_claimed_own_hand.to_rupees()),
                    "TCSAmtCollSpouseOrOthrHand": int(entry.tcs_claimed_spouse_hand.to_rupees()),
                    "PANOfSpouseOrOthrPrsn": entry.spouse_pan_for_claim,
                },
                "AmtCarriedFwd": int(entry.tcs_carried_forward.to_rupees()),
            })
        
        return {
            "TCS": tcs_list,
            "TotalSchTCS": int(self.total_tcs_collected().to_rupees()),
        }
