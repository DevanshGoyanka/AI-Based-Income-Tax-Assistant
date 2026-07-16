"""Schedule AMT (Alternate Minimum Tax) — Sections 115JC / 115JD.
Used in: ITR-2, ITR-3.
"""
from dataclasses import dataclass, field
from decimal import Decimal
from typing import List
from uuid import UUID, uuid4

from .base import Schedule, ValidationError


@dataclass
class ScheduleAMT(Schedule[dict]):
    """Schedule AMT — Alternate Minimum Tax computation.
    
    When adjusted total income (before deductions 80C-80GGC except 80P) 
    exceeds ₹20L and regular tax is less than AMT, AMT applies.
    
    AMT rate: 18.5% of adjusted income (0% for individuals below 20L income).
    AMT credit can be carried forward for 10 years under Section 115JD.
    
    Used in: ITR-2, ITR-3.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    # Income from regular computation
    total_income_item_11: int = 0  # Total income from regular computation

    # Adjustments under Section 115JC
    adjustment_deferred_tax: int = 0         # Deferred tax liability added back
    adjustment_exempt_income: int = 0        # Exempt income adjustments
    adjustment_ind_as: int = 0               # Ind-AS adjustments
    adjustment_other: int = 0                # Other 115JC adjustments
    total_adjustments: int = 0

    # Adjusted income
    adjusted_under_115jc: int = 0           # Total income + adjustments
    adjusted_ifsc_115jcf: int = 0            # Adjusted for IFSC units (10%)
    adjusted_other: int = 0                  # Other adjusted income

    # AMT computation
    amt_rate: float = 0.0185                 # 18.5%
    amt_payable: int = 0                     # max(adjusted_115jc, adjusted_ifsc) * rate

    # Credit
    amt_credit_available: int = 0             # Brought forward AMT credit
    amt_credit_utilized: int = 0             # AMT credit used this year
    amt_credit_remaining: int = 0            # AMT credit carried forward

    def compute_adjusted_income(self) -> int:
        self.total_adjustments = (
            self.adjustment_deferred_tax +
            self.adjustment_exempt_income +
            self.adjustment_ind_as +
            self.adjustment_other
        )
        self.adjusted_under_115jc = self.total_income_item_11 + self.total_adjustments
        return self.adjusted_under_115jc

    def compute_amt(self) -> int:
        base = max(self.adjusted_under_115jc, self.adjusted_ifsc_115jcf)
        self.amt_payable = int(base * self.amt_rate)
        return self.amt_payable

    def compute_income(self) -> Decimal:
        # AMT itself is a tax, not income
        return Decimal(0)

    def to_itr_json(self) -> dict:
        return {
            "ScheduleAMT": {
                "TotalIncItem11": self.total_income_item_11,
                "AdjustmentSec115JC": {
                    "DeferredTaxLiability": self.adjustment_deferred_tax,
                    "ExemptIncomeAdjustment": self.adjustment_exempt_income,
                    "IndASAdjustment": self.adjustment_ind_as,
                    "OtherAdjustment": self.adjustment_other,
                    "TotalAdjustment": self.total_adjustments,
                },
                "AdjustedUnderSec115JC": self.adjusted_under_115jc,
                "AdjustedUnderSec115JCIFSC": self.adjusted_ifsc_115jcf,
                "AdjustedUnderSec115JCOther": self.adjusted_other,
                "TaxPayableUnderSec115JC": self.amt_payable,
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        if self.adjusted_under_115jc < 0:
            errors.append(ValidationError(
                field="ScheduleAMT.adjusted_under_115jc",
                message="Adjusted income under 115JC cannot be negative",
                severity="BLOCKING"
            ))
        if self.amt_credit_utilized > self.amt_credit_available:
            errors.append(ValidationError(
                field="ScheduleAMT.amt_credit_utilized",
                message="AMT credit utilized cannot exceed available credit",
                severity="BLOCKING"
            ))
        return errors


@dataclass
class ScheduleAMTC(Schedule[dict]):
    """Schedule AMTC — AMT Credit.
    
    AMT credit is generated when AMT paid > regular tax.
    Can be carried forward for 10 assessment years.
    
    Used in: ITR-2, ITR-3.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    # AMT credit brought forward
    amt_credit_bf_from_prev_year: int = 0

    # Current year AMT credit
    current_year_amt_paid: int = 0
    current_year_regular_tax: int = 0
    amt_credit_generated: int = 0  # max(0, AMT_paid - Regular_tax)

    # Utilization
    amt_credit_used_this_year: int = 0

    # Totals
    total_amt_credit_available: int = 0
    total_amt_credit_used: int = 0
    amt_credit_carried_forward: int = 0

    def compute_credit(self) -> int:
        self.amt_credit_generated = max(0, self.current_year_amt_paid - self.current_year_regular_tax)
        self.total_amt_credit_available = self.amt_credit_bf_from_prev_year + self.amt_credit_generated
        self.total_amt_credit_used = min(self.amt_credit_used_this_year, self.total_amt_credit_available)
        self.amt_credit_carried_forward = self.total_amt_credit_available - self.total_amt_credit_used
        return self.amt_credit_carried_forward

    def compute_income(self) -> Decimal:
        return Decimal(0)

    def to_itr_json(self) -> dict:
        return {
            "ScheduleAMTC": {
                "BroughtForwardAMTCredit": self.amt_credit_bf_from_prev_year,
                "AMTPaidInCurrentAY": self.current_year_amt_paid,
                "RegularTax": self.current_year_regular_tax,
                "AMTCreditGenerated": self.amt_credit_generated,
                "TotalAMTCreditAvailable": self.total_amt_credit_available,
                "AMTCreditUtilizedInCurrentAY": self.total_amt_credit_used,
                "AMTCreditCarriedForward": self.amt_credit_carried_forward,
            }
        }

    def validate(self) -> List[ValidationError]:
        return []
