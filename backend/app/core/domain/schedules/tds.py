"""Schedule TDS - Tax Deducted at Source."""
from dataclasses import dataclass
from typing import List

from ..value_objects import Money
from .base import Schedule, ValidationError


@dataclass
class TDSDetail:
    """TDS detail from one deductor."""
    deductor_name: str
    deductor_tan: str
    section_code: str
    amount_paid: Money
    tax_deducted: Money
    tax_deposited: Money
    quarter: str


@dataclass
class ScheduleTDS(Schedule[List[TDSDetail]]):
    """Schedule TDS - Tax Deducted at Source."""
    
    tds_entries: List[TDSDetail]
    
    def compute_income(self):
        """TDS schedule doesn't contribute to income."""
        return 0
    
    def total_tds_credit(self) -> int:
        """Total TDS available as tax credit."""
        return sum(
            int(entry.tax_deposited.to_rupees()) 
            for entry in self.tds_entries
        )
    
    def to_itr_json(self) -> dict:
        """Export to ITR JSON format."""
        return {
            "ScheduleTDS": {
                "TDSDetails": [
                    {
                        "DeductorName": entry.deductor_name,
                        "TAN": entry.deductor_tan,
                        "SectionCode": entry.section_code,
                        "AmountPaid": int(entry.amount_paid.to_rupees()),
                        "TaxDeducted": int(entry.tax_deducted.to_rupees()),
                        "TaxDeposited": int(entry.tax_deposited.to_rupees()),
                        "Quarter": entry.quarter,
                    }
                    for entry in self.tds_entries
                ]
            }
        }
    
    def validate(self) -> List[ValidationError]:
        """Validate TDS schedule."""
        errors = []
        
        for i, entry in enumerate(self.tds_entries):
            if not entry.deductor_tan or len(entry.deductor_tan) != 10:
                errors.append(ValidationError(
                    field=f"ScheduleTDS.TDSDetails[{i}].TAN",
                    message="Invalid TAN format",
                    severity="BLOCKING"
                ))
            
            if entry.tax_deducted.paise > entry.amount_paid.paise:
                errors.append(ValidationError(
                    field=f"ScheduleTDS.TDSDetails[{i}]",
                    message="TDS cannot exceed amount paid",
                    severity="WARNING"
                ))
        
        return errors
