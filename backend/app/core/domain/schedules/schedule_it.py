"""Schedule IT - Tax Payments."""
from dataclasses import dataclass
from decimal import Decimal
from typing import List
from uuid import UUID

from ..value_objects import Money
from .base import Schedule, ValidationError


@dataclass
class AdvanceTaxPayment:
    """Advance tax payment record."""
    bsr_code: str
    date_of_deposit: str
    challan_serial_no: str
    amount: Money


@dataclass
class SelfAssessmentTax:
    """Self-assessment tax payment."""
    bsr_code: str
    date_of_deposit: str
    challan_serial_no: str
    amount: Money


@dataclass
class ScheduleIT(Schedule[dict]):
    """Schedule IT - Tax Payments (Advance Tax, Self-Assessment Tax)."""
    
    advance_tax: List[AdvanceTaxPayment]
    self_assessment_tax: List[SelfAssessmentTax]
    
    def compute_income(self) -> Decimal:
        """Tax payments don't affect income computation."""
        return Decimal("0")
    
    def total_advance_tax(self) -> Decimal:
        """Total advance tax paid."""
        return sum(pay.amount.to_rupees() for pay in self.advance_tax)
    
    def total_self_assessment_tax(self) -> Decimal:
        """Total self-assessment tax paid."""
        return sum(pay.amount.to_rupees() for pay in self.self_assessment_tax)
    
    def total_tax_paid(self) -> Decimal:
        """Total tax paid (excluding TDS/TCS)."""
        return self.total_advance_tax() + self.total_self_assessment_tax()
    
    def to_itr_json(self) -> dict:
        """Export to ITR JSON format."""
        return {
            "ScheduleIT": {
                "AdvanceTax": [
                    {
                        "BSRCode": pay.bsr_code,
                        "DateOfDeposit": pay.date_of_deposit,
                        "ChallanSerialNo": pay.challan_serial_no,
                        "TaxPaid": int(pay.amount.to_rupees())
                    }
                    for pay in self.advance_tax
                ],
                "SelfAssessmentTax": [
                    {
                        "BSRCode": pay.bsr_code,
                        "DateOfDeposit": pay.date_of_deposit,
                        "ChallanSerialNo": pay.challan_serial_no,
                        "TaxPaid": int(pay.amount.to_rupees())
                    }
                    for pay in self.self_assessment_tax
                ],
                "TotalTaxPaid": int(self.total_tax_paid())
            }
        }
    
    def validate(self) -> List[ValidationError]:
        """Validate tax payment records."""
        errors = []
        
        for i, pay in enumerate(self.advance_tax):
            if not pay.bsr_code or len(pay.bsr_code) != 7:
                errors.append(ValidationError(
                    field=f"ScheduleIT.AdvanceTax[{i}].BSRCode",
                    message="Invalid BSR code format",
                    severity="BLOCKING"
                ))
            
            if pay.amount.paise <= 0:
                errors.append(ValidationError(
                    field=f"ScheduleIT.AdvanceTax[{i}].Amount",
                    message="Tax amount must be positive",
                    severity="BLOCKING"
                ))
        
        for i, pay in enumerate(self.self_assessment_tax):
            if not pay.bsr_code or len(pay.bsr_code) != 7:
                errors.append(ValidationError(
                    field=f"ScheduleIT.SelfAssessmentTax[{i}].BSRCode",
                    message="Invalid BSR code format",
                    severity="BLOCKING"
                ))
        
        return errors
