"""Schedule HP - House Property Income."""
from dataclasses import dataclass
from decimal import Decimal
from typing import List
from uuid import UUID

from ..value_objects import Money
from .base import Schedule, ValidationError


@dataclass
class HPDetail:
    """House property income detail."""
    address: str
    ownership_share: Decimal
    annual_rent: Money
    municipal_taxes: Money
    interest_paid: Money
    co_owners: List[str]
    
    def compute_income(self) -> Decimal:
        """Compute income from this property."""
        nav = self.annual_rent.to_rupees() - self.municipal_taxes.to_rupees()
        std_ded = nav * Decimal("0.30") if nav > 0 else Decimal("0")
        interest = self.interest_paid.to_rupees()
        income = (nav - std_ded - interest) * self.ownership_share
        return income


@dataclass
class ScheduleHP(Schedule[List[HPDetail]]):
    """Schedule HP - House Property Income (Section 22-27)."""
    
    properties: List[HPDetail]
    
    def compute_income(self) -> Decimal:
        """Total house property income with loss cap."""
        total = sum(prop.compute_income() for prop in self.properties)
        return max(total, Decimal("-200000"))
    
    def to_itr_json(self) -> dict:
        """Export to ITR JSON format."""
        return {
            "ScheduleHP": {
                "PropertyDetails": [
                    {
                        "Address": prop.address,
                        "OwnershipShare": float(prop.ownership_share),
                        "AnnualRent": int(prop.annual_rent.to_rupees()),
                        "MunicipalTaxes": int(prop.municipal_taxes.to_rupees()),
                        "InterestPaid": int(prop.interest_paid.to_rupees()),
                    }
                    for prop in self.properties
                ]
            }
        }
    
    def validate(self) -> List[ValidationError]:
        """Validate house property schedule."""
        errors = []
        
        for i, prop in enumerate(self.properties):
            if prop.ownership_share <= 0 or prop.ownership_share > 1:
                errors.append(ValidationError(
                    field=f"ScheduleHP.PropertyDetails[{i}].OwnershipShare",
                    message="Ownership share must be between 0 and 1",
                    severity="BLOCKING"
                ))
        
        return errors
