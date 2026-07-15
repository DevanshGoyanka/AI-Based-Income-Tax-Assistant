"""Schedule CG - Capital Gains."""
from dataclasses import dataclass
from datetime import date
from decimal import Decimal
from typing import List, Optional
from uuid import UUID

from ..value_objects import Money
from .base import Schedule, ValidationError


@dataclass
class CGTransaction:
    """Capital gains transaction."""
    asset_type: str  # "listed_equity", "unlisted_equity", "real_estate", "bonds", "gold"
    purchase_date: date
    sale_date: date
    sale_price: Money
    purchase_price: Money
    transfer_expenses: Money = Money.from_rupees(0)
    indexed_cost: Optional[Money] = None
    section: str = ""  # 111A, 112, 112A, etc.
    
    def holding_period_days(self) -> int:
        """Calculate holding period in days."""
        return (self.sale_date - self.purchase_date).days
    
    def is_long_term(self) -> bool:
        """Check if long-term based on asset type and holding period."""
        if self.asset_type in ["listed_equity", "equity_mf"]:
            return self.holding_period_days() > 365
        elif self.asset_type == "real_estate":
            return self.holding_period_days() > 730  # 24 months
        else:
            return self.holding_period_days() > 1095  # 36 months
    
    def compute_gain(self) -> Decimal:
        """Compute capital gain."""
        cost = self.indexed_cost or self.purchase_price
        gain = (
            self.sale_price.to_rupees() 
            - cost.to_rupees() 
            - self.transfer_expenses.to_rupees()
        )
        return gain


@dataclass
class ScheduleCG(Schedule[List[CGTransaction]]):
    """Schedule CG - Capital Gains (ITR-2+)."""
    
    transactions: List[CGTransaction]
    
    def compute_income(self) -> Decimal:
        """Sum of all capital gains."""
        return sum(txn.compute_gain() for txn in self.transactions)
    
    def categorize_by_section(self) -> dict:
        """Categorize gains by tax section."""
        by_section = {}
        
        for txn in self.transactions:
            if txn.is_long_term():
                if txn.asset_type == "listed_equity":
                    section = "112A"  # LTCG @ 10%/12.5%
                else:
                    section = "112"   # LTCG @ 20% with indexation
            else:
                if txn.asset_type == "listed_equity":
                    section = "111A"  # STCG @ 15%/20%
                else:
                    section = "normal"  # STCG at slab rates
            
            if section not in by_section:
                by_section[section] = Decimal("0")
            by_section[section] += txn.compute_gain()
        
        return by_section
    
    def to_itr_json(self) -> dict:
        """Export to ITR JSON format."""
        return {
            "ScheduleCG": {
                "ShortTermCapitalGains": [
                    {
                        "AssetType": txn.asset_type,
                        "PurchaseDate": txn.purchase_date.isoformat(),
                        "SaleDate": txn.sale_date.isoformat(),
                        "SalePrice": int(txn.sale_price.to_rupees()),
                        "PurchasePrice": int(txn.purchase_price.to_rupees()),
                        "TransferExpenses": int(txn.transfer_expenses.to_rupees()),
                        "Gain": int(txn.compute_gain())
                    }
                    for txn in self.transactions if not txn.is_long_term()
                ],
                "LongTermCapitalGains": [
                    {
                        "AssetType": txn.asset_type,
                        "PurchaseDate": txn.purchase_date.isoformat(),
                        "SaleDate": txn.sale_date.isoformat(),
                        "SalePrice": int(txn.sale_price.to_rupees()),
                        "PurchasePrice": int(txn.purchase_price.to_rupees()),
                        "IndexedCost": int(txn.indexed_cost.to_rupees()) if txn.indexed_cost else None,
                        "TransferExpenses": int(txn.transfer_expenses.to_rupees()),
                        "Gain": int(txn.compute_gain())
                    }
                    for txn in self.transactions if txn.is_long_term()
                ]
            }
        }
    
    def validate(self) -> List[ValidationError]:
        """Validate capital gains transactions."""
        errors = []
        
        for i, txn in enumerate(self.transactions):
            if txn.sale_date <= txn.purchase_date:
                errors.append(ValidationError(
                    field=f"ScheduleCG.Transactions[{i}]",
                    message="Sale date must be after purchase date",
                    severity="BLOCKING"
                ))
            
            if txn.sale_price.paise <= 0:
                errors.append(ValidationError(
                    field=f"ScheduleCG.Transactions[{i}].SalePrice",
                    message="Sale price must be positive",
                    severity="BLOCKING"
                ))
        
        return errors
