"""Schedule OS - Other Sources Income."""
from dataclasses import dataclass
from decimal import Decimal
from typing import List

from ..value_objects import Money
from .base import Schedule, ValidationError


@dataclass
class InterestDetail:
    """Interest income detail."""
    bank_name: str
    account_number: str
    interest_amount: Money


@dataclass
class DividendDetail:
    """Dividend income detail."""
    company_name: str
    dividend_amount: Money


@dataclass
class ScheduleOS(Schedule[dict]):
    """Schedule OS - Other Sources Income."""
    
    interest_income: List[InterestDetail]
    dividend_income: List[DividendDetail]
    other_income: Money
    
    def compute_income(self) -> Decimal:
        """Total other sources income."""
        total_interest = sum(
            detail.interest_amount.to_rupees() 
            for detail in self.interest_income
        )
        total_dividend = sum(
            detail.dividend_amount.to_rupees() 
            for detail in self.dividend_income
        )
        other = self.other_income.to_rupees()
        
        return total_interest + total_dividend + other
    
    def to_itr_json(self) -> dict:
        """Export to ITR JSON format."""
        return {
            "ScheduleOS": {
                "InterestIncome": [
                    {
                        "BankName": detail.bank_name,
                        "AccountNumber": detail.account_number,
                        "InterestAmount": int(detail.interest_amount.to_rupees()),
                    }
                    for detail in self.interest_income
                ],
                "DividendIncome": [
                    {
                        "CompanyName": detail.company_name,
                        "DividendAmount": int(detail.dividend_amount.to_rupees()),
                    }
                    for detail in self.dividend_income
                ],
                "OtherIncome": int(self.other_income.to_rupees()),
            }
        }
    
    def validate(self) -> List[ValidationError]:
        """Validate other sources schedule."""
        errors = []
        
        for i, detail in enumerate(self.interest_income):
            if detail.interest_amount.paise < 0:
                errors.append(ValidationError(
                    field=f"ScheduleOS.InterestIncome[{i}]",
                    message="Interest amount cannot be negative",
                    severity="BLOCKING"
                ))
        
        return errors
