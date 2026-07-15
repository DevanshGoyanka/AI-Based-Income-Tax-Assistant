"""Schedule BA - Bank Accounts."""
from dataclasses import dataclass
from typing import List
from uuid import UUID
from decimal import Decimal

from .base import Schedule, ValidationError


@dataclass
class BankAccount:
    """Bank account details."""
    ifsc_code: str
    account_number: str
    bank_name: str
    account_type: str = "SAVINGS"  # SAVINGS, CURRENT


@dataclass
class ScheduleBA(Schedule[List[BankAccount]]):
    """Schedule BA - Bank Accounts (All ITRs)."""
    
    accounts: List[BankAccount]
    
    def compute_income(self) -> Decimal:
        """Bank accounts don't affect income computation."""
        return Decimal("0")
    
    def to_itr_json(self) -> dict:
        """Export to ITR JSON format."""
        return {
            "ScheduleBA": {
                "BankAccounts": [
                    {
                        "IFSCCode": acc.ifsc_code,
                        "AccountNumber": acc.account_number,
                        "BankName": acc.bank_name,
                        "AccountType": acc.account_type
                    }
                    for acc in self.accounts
                ]
            }
        }
    
    def validate(self) -> List[ValidationError]:
        """Validate bank account records."""
        errors = []
        
        if not self.accounts:
            errors.append(ValidationError(
                field="ScheduleBA",
                message="At least one bank account required",
                severity="BLOCKING"
            ))
        
        for i, acc in enumerate(self.accounts):
            if not acc.ifsc_code or len(acc.ifsc_code) != 11:
                errors.append(ValidationError(
                    field=f"ScheduleBA.BankAccounts[{i}].IFSCCode",
                    message="Invalid IFSC code format",
                    severity="BLOCKING"
                ))
            
            if not acc.account_number:
                errors.append(ValidationError(
                    field=f"ScheduleBA.BankAccounts[{i}].AccountNumber",
                    message="Account number required",
                    severity="BLOCKING"
                ))
        
        return errors
