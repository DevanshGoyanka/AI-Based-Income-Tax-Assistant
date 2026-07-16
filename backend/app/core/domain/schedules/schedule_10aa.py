"""Schedule10AA — SEZ Deduction u/s 10AA.
Used in: ITR-3 only.
"""
from dataclasses import dataclass, field
from decimal import Decimal
from typing import List
from uuid import UUID, uuid4

from .base import Schedule, ValidationError


@dataclass
class Schedule10AA(Schedule[dict]):
    """Schedule 10AA — Deduction in respect of units in SEZ / STPZ / EHTP.
    
    Deduction = 100% of export profit for first 5 years,
    50% for next 5 years, 0% thereafter.
    Available to SEZ units that began operations after 1-Apr-2000.
    
    Condition: DTA sales ≤ 25% of total turnover + exports.
    
    Used in: ITR-3 only.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    # Unit details
    unit_name: str = ""
    ie_code: str = ""  # Import-Export code
    line_of_business: str = ""

    # Turnover figures
    export_revenue: int = 0
    net_foreign_exchange: int = 0    # Export - Import
    domestic_sales: int = 0          # DTA sales

    # Income computation
    total_turnover: int = 0
    dta_limit_applicable: bool = True  # DTA ≤ 25% of turnover?
    eligible_turnover: int = 0
    profit_from_export: int = 0        # Profit attributable to eligible turnover
    deduction_available: int = 0        # Min of export profit and eligible deduction

    def compute_deduction(self) -> int:
        if self.dta_limit_applicable:
            self.total_turnover = self.export_revenue + self.domestic_sales
            # DTA sales must be ≤ 25% of total
            if self.domestic_sales > 0:
                dta_ratio = self.domestic_sales / self.total_turnover
                if dta_ratio > 0.25:
                    return 0  # DTA exceeds limit, no deduction
            self.eligible_turnover = self.total_turnover
        else:
            self.eligible_turnover = self.export_revenue

        self.deduction_available = min(self.profit_from_export, self.eligible_turnover)
        return self.deduction_available

    def compute_income(self) -> Decimal:
        return Decimal(0)  # Deduction reduces income

    def to_itr_json(self) -> dict:
        return {
            "Schedule10AA": {
                "DeductSEZ": {
                    "UndertakingName": self.unit_name,
                    "IECode": self.ie_code,
                    "LineOfBusiness": self.line_of_business,
                    "ExportRevenue": self.export_revenue,
                    "NetForeignExchangeEarning": self.net_foreign_exchange,
                    "DTASales": self.domestic_sales,
                    "TotalTurnover": self.total_turnover,
                    "EligibleTurnover": self.eligible_turnover,
                    "ProfitAttributable": self.profit_from_export,
                    "Deduction": self.deduction_available,
                }
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        if self.export_revenue < 0 or self.domestic_sales < 0:
            errors.append(ValidationError(
                field="Schedule10AA.turnover",
                message="Turnover values cannot be negative",
                severity="BLOCKING"
            ))
        return errors
