"""Schedule EI — Exempt Income u/s 10.
Income not chargeable to tax (HUF, agriculture, PPF, etc.).
Used in: ITR-2, ITR-3.
"""
from dataclasses import dataclass, field
from decimal import Decimal
from typing import List, Optional
from uuid import UUID, uuid4

from .base import Schedule, ValidationError


@dataclass
class ScheduleEI(Schedule[dict]):
    """Schedule EI — Exempt Income u/s 10.
    
    Includes: Agriculture income (sec 10(1)), share from HUF (sec 10(2)),
    interest from specified sources (sec 10(4), 10(10)), PPF (sec 10(11)),
    pension (sec 10(10A)), gratuity (sec 10(10)), leave encashment (sec 10(10AA)),
    retrenchment compensation (sec 10(10B)), VRS (sec 10(10C)), etc.
    
    Agriculture income >₹5000 is exempt u/s 10(1).
    Note: Agriculture income is included in GTI for rate calculation but exempt from tax.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    # Interest income
    interest_income: int = 0

    # Agriculture
    gross_agriculture_receipts: int = 0
    expenses_on_agriculture: int = 0
    unabated_agriculture_loss_prev_8_years: int = 0
    agri_income_rule_7_and_7a: int = 0
    net_agriculture_or_other_income_rule_7: int = 0  # Rule 7/7A income

    # Exempt agriculture income (sec 10(1))
    exempt_agriculture_income: int = 0  # Calculated: gross_agri - expenses (if +ve, else 0)

    # Other exempt income
    exempt_share_income_huf: int = 0          # sec 10(2)
    exempt_interest_income: int = 0           # sec 10(4), 10(10), etc.
    exempt_dividend_income: int = 0           # sec 10(34), 10(35)
    exempt_ppf_income: int = 0               # sec 10(11)
    exempt_pension_income: int = 0            # sec 10(10A)
    exempt_gratuity_income: int = 0           # sec 10(10)
    exempt_leave_encashment_income: int = 0  # sec 10(10AA)
    exempt_retrenchment_comp: int = 0         # sec 10(10B)
    exempt_vrs_income: int = 0                # sec 10(10C)
    exempt_other: int = 0                     # Other exempt income

    # DTAA
    exempt_dtaa_income: int = 0              # Income not chargeable per DTAA

    # Total
    total_exempt_income: int = 0

    def compute_income(self) -> Decimal:
        # Exempt income is not added to taxable income
        return Decimal(0)

    def compute_net_agriculture(self) -> int:
        """Net agriculture income = receipts - expenses."""
        return max(0, self.gross_agriculture_receipts - self.expenses_on_agriculture)

    def to_itr_json(self) -> dict:
        self.exempt_agriculture_income = self.compute_net_agriculture()
        self.total_exempt_income = (
            self.exempt_agriculture_income +
            self.exempt_share_income_huf +
            self.exempt_interest_income +
            self.exempt_dividend_income +
            self.exempt_ppf_income +
            self.exempt_pension_income +
            self.exempt_gratuity_income +
            self.exempt_leave_encashment_income +
            self.exempt_retrenchment_comp +
            self.exempt_vrs_income +
            self.exempt_other +
            self.exempt_dtaa_income
        )
        return {
            "ScheduleEI": {
                "InterestInc": self.interest_income,
                "GrossAgriRecpt": self.gross_agriculture_receipts,
                "ExpIncAgri": self.expenses_on_agriculture,
                "UnabAgriLossPrev8": self.unabated_agriculture_loss_prev_8_years,
                "AgriIncRule7and8": self.agri_income_rule_7_and_7a,
                "NetAgriIncOrOthrIncRule7": self.net_agriculture_or_other_income_rule_7,
                "ExcNetAgriInc": {"NetAgriInc": self.exempt_agriculture_income},
                "OthersInc": {
                    "ShareIncExemptUs10": self.exempt_share_income_huf,
                    "InterestIncExemptUs10": self.exempt_interest_income,
                    "DividendIncExemptUs10": self.exempt_dividend_income,
                    "PPFIncExemptUs10": self.exempt_ppf_income,
                    "PensionExemptUs10": self.exempt_pension_income,
                    "GratuityExemptUs10": self.exempt_gratuity_income,
                    "LeaveEncashExemptUs10": self.exempt_leave_encashment_income,
                    "RetrenchCompExemptUs10": self.exempt_retrenchment_comp,
                    "VRSExemptUs10": self.exempt_vrs_income,
                    "OtherExemptInc": self.exempt_other,
                },
                "Others": self.exempt_other,
                "IncNotChrgblAsPerDTAA": {
                    "NatureDesc": "Income not chargeable per DTAA",
                    "IncNotChrgblAsPerDTAA": self.exempt_dtaa_income,
                },
                "IncChrgblAsPerDTAA": 0,
                "PassThrIncNotChrgblTax": 0,
                "TotalExemptInc": self.total_exempt_income,
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        if self.gross_agriculture_receipts < self.expenses_on_agriculture:
            # This is normal - agriculture can have a loss
            pass
        if self.total_exempt_income < 0:
            errors.append(ValidationError(
                field="ScheduleEI.total_exempt_income",
                message="Total exempt income cannot be negative",
                severity="BLOCKING"
            ))
        return errors


@dataclass
class ScheduleUs24B(Schedule[dict]):
    """Schedule Us 24B — Loss from House Property (to be set off).
    
    Tracks HP loss after all deductions (interest, municipal tax, etc.)
    so it can be set off against other income heads under Section 71.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    property_address: str = ""
    annual_value: int = 0
    municipal_tax_paid: int = 0
    standard_deduction: int = 0  # 30% of NAV
    interest_on_loan: int = 0
    total_deductions: int = 0
    loss_from_hp: int = 0

    # Set off
    set_off_against_salary: int = 0
    set_off_against_os: int = 0
    set_off_against_bp: int = 0
    remaining_loss: int = 0

    def compute_income(self) -> Decimal:
        # HP loss is negative
        return Decimal(-self.loss_from_hp)

    def to_itr_json(self) -> dict:
        return {
            "ScheduleHP": {
                "PropertyDetails": [{
                    "TypeOfHP": "HP",
                    "AddrDetail": {"AddrDetail": self.property_address},
                    "AnnualValue": self.annual_value,
                    "MunicipalTax": self.municipal_tax_paid,
                    "StandardDeduction": self.standard_deduction,
                    "InterestPayable": self.interest_on_loan,
                    "TotalLoss": self.loss_from_hp,
                    "SetOffUnder71": {
                        "SetOffSal": self.set_off_against_salary,
                        "SetOffHP": 0,
                        "SetOffOS": self.set_off_against_os,
                        "SetOffBP": self.set_off_against_bp,
                        "RemainingLoss": self.remaining_loss,
                    }
                }],
                "TotalLossOfHP": self.loss_from_hp,
            }
        }

    def validate(self) -> List[ValidationError]:
        return []
