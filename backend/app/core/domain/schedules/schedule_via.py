"""Schedule VIA - Chapter VI-A Deductions."""
from dataclasses import dataclass
from decimal import Decimal
from typing import List
from uuid import UUID

from ..value_objects import Money
from .base import Schedule, ValidationError


@dataclass
class Section80C:
    """Section 80C/CCC/CCD(1) deductions."""
    life_insurance: Money = Money.from_rupees(0)
    ppf: Money = Money.from_rupees(0)
    elss: Money = Money.from_rupees(0)
    nsc: Money = Money.from_rupees(0)
    epf: Money = Money.from_rupees(0)
    vpf: Money = Money.from_rupees(0)
    nps: Money = Money.from_rupees(0)
    tuition_fees: Money = Money.from_rupees(0)
    home_loan_principal: Money = Money.from_rupees(0)
    sukanya_samriddhi: Money = Money.from_rupees(0)
    
    def total(self) -> Decimal:
        """Total 80C deductions (capped at ₹1.5L)."""
        total = sum([
            self.life_insurance.to_rupees(),
            self.ppf.to_rupees(),
            self.elss.to_rupees(),
            self.nsc.to_rupees(),
            self.epf.to_rupees(),
            self.vpf.to_rupees(),
            self.nps.to_rupees(),
            self.tuition_fees.to_rupees(),
            self.home_loan_principal.to_rupees(),
            self.sukanya_samriddhi.to_rupees()
        ])
        return min(total, Decimal("150000"))


@dataclass
class Section80D:
    """Section 80D - Health insurance premium."""
    self_family: Money = Money.from_rupees(0)
    parents: Money = Money.from_rupees(0)
    preventive_checkup: Money = Money.from_rupees(0)
    is_senior_citizen: bool = False
    parents_senior_citizen: bool = False
    
    def total(self) -> Decimal:
        """Total 80D deductions."""
        self_limit = Decimal("50000") if self.is_senior_citizen else Decimal("25000")
        parent_limit = Decimal("50000") if self.parents_senior_citizen else Decimal("25000")
        
        self_actual = min(self.self_family.to_rupees(), self_limit)
        parent_actual = min(self.parents.to_rupees(), parent_limit)
        checkup = min(self.preventive_checkup.to_rupees(), Decimal("5000"))
        
        return self_actual + parent_actual + checkup


@dataclass
class ScheduleVIA(Schedule[dict]):
    """Schedule VIA - Chapter VI-A Deductions (Old Regime Only)."""
    
    section_80c: Section80C
    section_80ccd_1b: Money = Money.from_rupees(0)  # Additional NPS (₹50K)
    section_80ccd_2: Money = Money.from_rupees(0)   # Employer NPS (no limit)
    section_80d: Section80D = None
    section_80e: Money = Money.from_rupees(0)       # Education loan interest
    section_80g: Money = Money.from_rupees(0)       # Donations
    section_80tta: Money = Money.from_rupees(0)     # Savings interest (₹10K)
    section_80ttb: Money = Money.from_rupees(0)     # Senior savings interest (₹50K)
    
    def __post_init__(self):
        if self.section_80d is None:
            self.section_80d = Section80D()
    
    def compute_income(self) -> Decimal:
        """Deductions reduce income (negative value)."""
        return -self.total_deductions()
    
    def total_deductions(self) -> Decimal:
        """Total Chapter VI-A deductions."""
        total = Decimal("0")
        
        # 80C/CCC/CCD(1) - ₹1.5L cap
        total += self.section_80c.total()
        
        # 80CCD(1B) - Additional NPS ₹50K
        total += min(self.section_80ccd_1b.to_rupees(), Decimal("50000"))
        
        # 80CCD(2) - Employer NPS (no limit)
        total += self.section_80ccd_2.to_rupees()
        
        # 80D - Health insurance
        total += self.section_80d.total()
        
        # 80E - Education loan (no limit)
        total += self.section_80e.to_rupees()
        
        # 80G - Donations (50%/100% eligible)
        total += self.section_80g.to_rupees()
        
        # 80TTA - Savings interest (₹10K)
        total += min(self.section_80tta.to_rupees(), Decimal("10000"))
        
        # 80TTB - Senior savings interest (₹50K)
        total += min(self.section_80ttb.to_rupees(), Decimal("50000"))
        
        return total
    
    def to_itr_json(self) -> dict:
        """Export to ITR JSON format."""
        return {
            "ScheduleVIA": {
                "Section80C": int(self.section_80c.total()),
                "Section80CCD1B": int(min(self.section_80ccd_1b.to_rupees(), Decimal("50000"))),
                "Section80CCD2": int(self.section_80ccd_2.to_rupees()),
                "Section80D": int(self.section_80d.total()),
                "Section80E": int(self.section_80e.to_rupees()),
                "Section80G": int(self.section_80g.to_rupees()),
                "Section80TTA": int(min(self.section_80tta.to_rupees(), Decimal("10000"))),
                "Section80TTB": int(min(self.section_80ttb.to_rupees(), Decimal("50000"))),
                "TotalDeductions": int(self.total_deductions())
            }
        }
    
    def validate(self) -> List[ValidationError]:
        """Validate VI-A deductions."""
        errors = []
        
        # Validate 80C limit
        if self.section_80c.total() > Decimal("150000"):
            errors.append(ValidationError(
                field="ScheduleVIA.Section80C",
                message="Section 80C limit exceeded (max ₹1,50,000)",
                severity="WARNING"
            ))
        
        # Validate 80CCD(1B) limit
        if self.section_80ccd_1b.to_rupees() > Decimal("50000"):
            errors.append(ValidationError(
                field="ScheduleVIA.Section80CCD1B",
                message="Section 80CCD(1B) limit exceeded (max ₹50,000)",
                severity="WARNING"
            ))
        
        # Validate 80TTA/80TTB mutual exclusivity
        if self.section_80tta.to_rupees() > 0 and self.section_80ttb.to_rupees() > 0:
            errors.append(ValidationError(
                field="ScheduleVIA",
                message="Cannot claim both 80TTA and 80TTB",
                severity="BLOCKING"
            ))
        
        return errors
