"""Schedule SI (Special Income), IF (Income from Firm), PTI, TPSA.
Used in: ITR-2, ITR-3.
"""
from dataclasses import dataclass, field
from decimal import Decimal
from typing import List, Optional
from uuid import UUID, uuid4

from ..value_objects import Money
from .base import Schedule, ValidationError


@dataclass
class SIEntry:
    """Single Special Income entry."""
    income_code: str = ""   # Section code: 115A, 115AB, 115AC, 115AD, 115B, 115BB, 115BBA, 115BBC, 115E
    description: str = ""
    amount: int = 0
    tax_rate: float = 0.0   # e.g. 0.10, 0.20, 0.30
    tax_payable: int = 0

    def compute_tax(self) -> int:
        return int(self.amount * self.tax_rate)


@dataclass
class ScheduleSI(Schedule[dict]):
    """Schedule SI — Special Income.
    
    Income chargeable at special rates under:
    - 115A: Dividends, interest, lottery (from outside India)
    - 115AB: FII unit trust / equity-oriented MF
    - 115AC: Foreign bonds / GDR
    - 115AD: FII securities (30% STCG, 10% LTCG)
    - 115B: Insurance company life insurance profits
    - 115BB: Lottery / horse racing / puzzle (30%)
    - 115BBA: Non-resident sports (10% for non-residents)
    - 115BBC: Specified entertainment (20%)
    - 115E: Foreign equity / specified assets for NRI (20% LTCG, 10% FII)
    
    Used in: ITR-2, ITR-3.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    entries: List[SIEntry] = field(default_factory=list)

    def total_income(self) -> int:
        return sum(e.amount for e in self.entries)

    def total_tax(self) -> int:
        return sum(e.compute_tax() for e in self.entries)

    def compute_income(self) -> Decimal:
        return Decimal(self.total_income())

    def to_itr_json(self) -> dict:
        return {
            "ScheduleSI": {
                "SplCodeRateTax": [
                    {
                        "SplIncCode": e.income_code,
                        "Description": e.description,
                        "Amount": e.amount,
                        "Tax": e.tax_payable,
                    }
                    for e in self.entries
                ],
                "TotSplRateInc": self.total_income(),
                "TotSplRateIncTax": self.total_tax(),
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        for e in self.entries:
            if e.amount < 0:
                errors.append(ValidationError(
                    field="ScheduleSI.entries",
                    message=f"Special income amount cannot be negative for {e.income_code}",
                    severity="BLOCKING"
                ))
        return errors


@dataclass
class PartnerFirmDetail:
    """Partner in firm details for ScheduleIF."""
    firm_name: str = ""
    firm_pan: str = ""
    firm_address: str = ""
    profit_share_percentage: float = 0.0
    share_of_profit: int = 0
    capital_balance: int = 0
    interest_on_capital: int = 0
    interest_on_drawing: int = 0
    remuneration_received: int = 0
    commission_received: int = 0


@dataclass
class ScheduleIF(Schedule[dict]):
    """Schedule IF — Income from Firm where assessee is a partner.
    
    Used in: ITR-3 only.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    partners: List[PartnerFirmDetail] = field(default_factory=list)

    def total_profit_share(self) -> int:
        return sum(p.share_of_profit for p in self.partners)

    def total_interest(self) -> int:
        return sum(p.interest_on_capital for p in self.partners)

    def total_remuneration(self) -> int:
        return sum(p.remuneration_received + p.commission_received for p in self.partners)

    def total_capital_balance(self) -> int:
        return sum(p.capital_balance for p in self.partners)

    def compute_income(self) -> Decimal:
        return Decimal(self.total_profit_share())

    def to_itr_json(self) -> dict:
        return {
            "ScheduleIF": {
                "PartnerFirmDetails": [
                    {
                        "FirmName": p.firm_name,
                        "FirmPAN": p.firm_pan,
                        "FirmAddress": p.firm_address,
                        "ProfitSharePercentage": int(p.profit_share_percentage * 100),
                        "ShareOfProfit": p.share_of_profit,
                        "CapitalBalance": p.capital_balance,
                        "InterestOnCapital": p.interest_on_capital,
                        "InterestOnDrawing": p.interest_on_drawing,
                        "Remuneration": p.remuneration_received,
                        "Commission": p.commission_received,
                    }
                    for p in self.partners
                ],
                "TotalProfitShareAmt": self.total_profit_share(),
                "TotalIntrstAmtDueOrRecv": self.total_interest(),
                "TotalRemunernAmtDueOrRecv": self.total_remuneration(),
                "TotalFirmCapBalOn31Mar": self.total_capital_balance(),
            }
        }

    def validate(self) -> List[ValidationError]:
        return []


@dataclass
class PTIEntry:
    """Transfer of immovable property — SchedulePTI."""
    transfer_type: str = ""  # Sale, Gift, Inheritance
    property_address: str = ""
    share_percentage: float = 0.0
    total_consideration: int = 0
    cost_of_acquisition: int = 0
    improvement_cost: int = 0
    transfer_expenses: int = 0
    taxable_gain: int = 0
    buyer_name: str = ""
    buyer_pan: str = ""


@dataclass
class SchedulePTI(Schedule[dict]):
    """Schedule PTI — Income from Transfer of Immovable Property.
    
    Income from transfer of share in property under Section 56(2)(x) / 64.
    Used in: ITR-3 only.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    entries: List[PTIEntry] = field(default_factory=list)

    def total_taxable_gain(self) -> int:
        return sum(e.taxable_gain for e in self.entries if e.taxable_gain > 0)

    def compute_income(self) -> Decimal:
        return Decimal(self.total_taxable_gain())

    def to_itr_json(self) -> dict:
        return {
            "SchedulePTI": {
                "SchedulePTIDtls": [
                    {
                        "TransferType": e.transfer_type,
                        "PropertyAddress": e.property_address,
                        "SharePercentage": int(e.share_percentage * 100),
                        "TotalConsideration": e.total_consideration,
                        "CostOfAcquisition": e.cost_of_acquisition,
                        "ImprovementCost": e.improvement_cost,
                        "TransferExpenses": e.transfer_expenses,
                        "TaxableGain": e.taxable_gain,
                        "BuyerName": e.buyer_name,
                        "BuyerPAN": e.buyer_pan,
                    }
                    for e in self.entries
                ],
                "TotalTaxableGain": self.total_taxable_gain(),
            }
        }

    def validate(self) -> List[ValidationError]:
        return []


@dataclass
class TPSATaxPayment:
    """Tax payment detail for ScheduleTPSA."""
    bsr_code: str = ""
    date_of_deposit: str = ""
    challan_serial: str = ""
    amount: int = 0


@dataclass
class ScheduleTPSA(Schedule[dict]):
    """Schedule TPSA — Tax on Presumptive Special Assets.
    
    Additional tax on deemed income from specified assets under Section 115BBE.
    Used in: ITR-3 only.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    # Section 92CE adjustment
    primary_adjustment: int = 0
    additional_income_tax_18_pct: int = 0
    surcharge_12_pct: int = 0
    health_education_cess: int = 0
    total_additional_tax: int = 0
    taxes_paid: int = 0
    net_tax_payable: int = 0
    payments: List[TPSATaxPayment] = field(default_factory=list)

    def compute_income(self) -> Decimal:
        return Decimal(self.primary_adjustment)

    def to_itr_json(self) -> dict:
        return {
            "ScheduleTPSA": {
                "AmtPrimaryAdjUs92CE_2A": self.primary_adjustment,
                "AdditionalIncTax18PercAbove": self.additional_income_tax_18_pct,
                "Surcharge12Perc": self.surcharge_12_pct,
                "HealthEducationCess": self.health_education_cess,
                "TotalAdditionalTax": self.total_additional_tax,
                "TaxesPaid": self.taxes_paid,
                "NetTaxPayable": self.net_tax_payable,
                "DtlsTaxesPaid": [
                    {
                        "BSRCode": p.bsr_code,
                        "DateOfDeposit": p.date_of_deposit,
                        "ChallanSerialNo": p.challan_serial,
                        "Amount": p.amount,
                    }
                    for p in self.payments
                ],
                "TotalAmountDeposited": sum(p.amount for p in self.payments),
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        if self.net_tax_payable < 0:
            errors.append(ValidationError(
                field="ScheduleTPSA.net_tax_payable",
                message="Net tax payable cannot be negative",
                severity="BLOCKING"
            ))
        return errors
