"""Schedule Salary - Salary Income."""
from dataclasses import dataclass
from decimal import Decimal
from typing import List
from uuid import UUID

from ..value_objects import Money
from .base import Schedule, ValidationError


@dataclass
class SalaryDetail:
    """Salary from one employer."""
    employer_name: str
    employer_tan: str
    gross_salary: Money
    allowances_exempt: Money
    professional_tax: Money
    tds_deducted: Money
    standard_deduction: Money = Money.from_rupees(0)
    
    def compute_net_salary(self) -> Decimal:
        """Compute net salary after deductions."""
        net = (
            self.gross_salary.to_rupees()
            - self.allowances_exempt.to_rupees()
            - self.professional_tax.to_rupees()
            - self.standard_deduction.to_rupees()
        )
        return max(net, Decimal("0"))


@dataclass
class ScheduleSalary(Schedule[List[SalaryDetail]]):
    """Schedule S - Salary Income (Section 17)."""
    
    employers: List[SalaryDetail]
    
    def compute_income(self) -> Decimal:
        """Sum of net salary from all employers."""
        return sum(emp.compute_net_salary() for emp in self.employers)
    
    def to_itr_json(self) -> dict:
        """Export to ITR JSON format."""
        return {
            "ScheduleS": {
                "SalaryDetails": [
                    {
                        "EmployerName": emp.employer_name,
                        "TAN": emp.employer_tan,
                        "GrossSalary": int(emp.gross_salary.to_rupees()),
                        "AllowancesExempt": int(emp.allowances_exempt.to_rupees()),
                        "ProfessionalTax": int(emp.professional_tax.to_rupees()),
                        "StandardDeduction": int(emp.standard_deduction.to_rupees()),
                        "TDSDeducted": int(emp.tds_deducted.to_rupees()),
                    }
                    for emp in self.employers
                ]
            }
        }
    
    def validate(self) -> List[ValidationError]:
        """Validate salary schedule."""
        errors = []
        
        if not self.employers:
            errors.append(ValidationError(
                field="ScheduleS",
                message="At least one employer required",
                severity="BLOCKING"
            ))
        
        for i, emp in enumerate(self.employers):
            if not emp.employer_tan or len(emp.employer_tan) != 10:
                errors.append(ValidationError(
                    field=f"ScheduleS.SalaryDetails[{i}].TAN",
                    message="Invalid TAN format",
                    severity="BLOCKING"
                ))
            
            if emp.gross_salary.paise < 0:
                errors.append(ValidationError(
                    field=f"ScheduleS.SalaryDetails[{i}].GrossSalary",
                    message="Gross salary cannot be negative",
                    severity="BLOCKING"
                ))
        
        return errors
