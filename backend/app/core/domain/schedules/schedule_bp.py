"""Schedule BP — Business / Profession income domain models.
ITR-4: Presumptive (44AD/44ADA/44AE).
ITR-3: Non-presumptive (detailed P&L, Manufacturing, Trading accounts).
"""
from dataclasses import dataclass, field
from datetime import date
from decimal import Decimal
from typing import List, Optional
from uuid import UUID, uuid4

from ..value_objects import Money
from .base import Schedule, ValidationError


# ─── ITR-4 Presumptive Schedules ─────────────────────────────────────────────

@dataclass
class BusinessPresumptiveDetail:
    """44AD Presumptive Business Income — General businesses.
    
    Turnover ≤ ₹3Cr (₹2Cr if cash > 5%): 6% of receipts as profit.
    Turnover > ₹3Cr: 8% of receipts as profit.
    """
    id: UUID = field(default_factory=uuid4)
    nature_of_business: str = ""
    business_code: str = ""
    gross_receipts: int = 0          # Total receipts/turnover
    gross_profit: int = 0            # As per books (if maintained)
    presumptive_rate: float = 0.06    # 6% or 8%
    presumptive_income: int = 0       # gross_receipts * presumptive_rate
    disallowed_expenses: int = 0      # Expenses not allowed as deduction
    depreciation: int = 0             # Depreciation claimed separately
    net_profit_per_books: int = 0     # If books are maintained
    taxable_profit: int = 0          # Higher of presumptive or book profit
    is_cash_receipts_above_5_pct: bool = False

    def compute_presumptive_income(self) -> int:
        if self.gross_receipts > 30000000:  # >₹3Cr
            self.presumptive_rate = 0.08
        else:
            self.presumptive_rate = 0.06
        self.presumptive_income = int(self.gross_receipts * self.presumptive_rate)
        self.taxable_profit = max(self.presumptive_income, self.net_profit_per_books)
        return self.taxable_profit


@dataclass
class ProfessionPresumptiveDetail:
    """44ADA Presumptive Profession Income — Specified professions.
    
    Advocates, tax consultants, etc. — 50% of gross receipts as profit.
    Turnover ≤ ₹75L: Presumptive allowed.
    """
    id: UUID = field(default_factory=uuid4)
    nature_of_profession: str = ""
    profession_code: str = ""
    gross_receipts: int = 0
    presumptive_income: int = 0       # 50% of gross receipts
    expenses: int = 0                 # If books maintained
    taxable_profit: int = 0

    def compute_presumptive_income(self) -> int:
        self.presumptive_income = int(self.gross_receipts * 0.5)
        self.taxable_profit = max(self.presumptive_income, self.expenses)
        return self.taxable_profit


@dataclass
class TransportPresumptiveDetail:
    """44AE Presumptive Transport Business.
    
    Goods carriage: ₹1,000 per ton per month (or ₹7,500 if goods carriage).
    Plying, hiring, or leasing of goods carriages.
    """
    id: UUID = field(default_factory=uuid4)
    vehicle_type: str = ""   # "Heavy Goods", "Light Goods", "Other"
    number_of_vehicles: int = 0
    capacity_per_vehicle_ton: float = 0.0
    months_operated: int = 12
    presumptive_rate_per_ton: int = 1000  # ₹1,000 per ton/month
    presumptive_income: int = 0

    def compute_presumptive_income(self) -> int:
        self.presumptive_income = int(
            self.number_of_vehicles *
            self.capacity_per_vehicle_ton *
            self.months_operated *
            self.presumptive_rate_per_ton
        )
        return self.presumptive_income


# ─── ITR-3 Non-Presumptive Schedules ─────────────────────────────────────────

@dataclass
class ManufacturingAccount:
    """Manufacturing Account — Cost of production computation."""
    opening_stock: int = 0
    raw_materials_purchased: int = 0
    custom_duty_on_materials: int = 0
    freight_on_materials: int = 0
    direct_expenses: int = 0
    wages_direct: int = 0
    factory_overheads: int = 0
    closing_stock_raw: int = 0
    cost_of_production: int = 0


@dataclass
class TradingAccount:
    """Trading Account — Gross profit computation."""
    opening_stock_traded: int = 0
    purchases: int = 0
    direct_expenses: int = 0
    closing_stock_traded: int = 0
    sales: int = 0
    duty_and_taxes_on_sales: int = 0
    cost_of_goods_sold: int = 0
    gross_profit: int = 0

    def compute(self) -> int:
        self.cost_of_goods_sold = (
            self.opening_stock_traded + self.purchases +
            self.direct_expenses - self.closing_stock_traded
        )
        self.gross_profit = self.sales - self.duty_and_taxes_on_sales - self.cost_of_goods_sold
        return self.gross_profit


@dataclass
class DepreciationItem:
    """Single depreciation item — Plant & Machinery / Other Assets."""
    id: UUID = field(default_factory=uuid4)
    description: str = ""
    asset_code: str = ""
    date_put_to_use: Optional[date] = None
    rate: float = 0.15          # 15%, 30%, 40%, 60%, 80%, 100%
    actual_cost: int = 0
    normal_depreciation: int = 0
    additional_depreciation: int = 0
    total_depreciation: int = 0
    wdv_carried_forward: int = 0


@dataclass
class ScheduleDPM:
    """Depreciation on Plant & Machinery — Schedule DPM."""
    items: List[DepreciationItem] = field(default_factory=list)
    total_normal_depreciation: int = 0
    total_additional_depreciation: int = 0
    total_depreciation: int = 0

    def compute(self) -> int:
        self.total_normal_depreciation = sum(i.normal_depreciation for i in self.items)
        self.total_additional_depreciation = sum(i.additional_depreciation for i in self.items)
        self.total_depreciation = self.total_normal_depreciation + self.total_additional_depreciation
        return self.total_depreciation


@dataclass
class ScheduleDOA:
    """Depreciation on Other Assets — Furniture, Building, etc."""
    building: int = 0
    furniture_fittings: int = 0
    vehicles: int = 0
    computers: int = 0
    intangible_assets: int = 0
    ships: int = 0
    other_assets: int = 0
    total_depreciation: int = 0

    def compute(self) -> int:
        return self.total_depreciation


@dataclass
class ScheduleDCG:
    """Deemed Capital Gains on depreciable assets u/s 50.
    
    When a depreciable asset is sold, WDV is compared with
    sale proceeds to compute deemed CG.
    Used in: ITR-3 only.
    """
    id: UUID = field(default_factory=uuid4)
    asset_description: str = ""
    date_of_transfer: Optional[date] = None
    sale_proceeds: int = 0
    wdv_before_transfer: int = 0
    deemed_capital_gain: int = 0  # max(0, proceeds - WDV)
    depreciation_claimed: int = 0  # Depreciation already claimed
    taxable_gain: int = 0           # gain - depreciation_claimed (if recaptured)

    def compute(self) -> int:
        self.deemed_capital_gain = max(0, self.sale_proceeds - self.wdv_before_transfer)
        self.taxable_gain = max(0, self.deemed_capital_gain - self.depreciation_claimed)
        return self.taxable_gain


@dataclass
class DeductUs35:
    """Deduction under Section 35 — Scientific Research."""
    capital_expenditure: int = 0
    revenue_expenditure: int = 0
    approved_institution_name: str = ""


@dataclass
class ScheduleESR:
    """Expenditure on Scientific Research — Schedule ESR."""
    items: List[DeductUs35] = field(default_factory=list)
    total_capital: int = 0
    total_revenue: int = 0


# ─── Main ScheduleBP ───────────────────────────────────────────────────────────

@dataclass
class BusinessDetailITR3:
    """ITR-3: Non-presumptive business detail entry."""
    id: UUID = field(default_factory=uuid4)
    nature_of_business: str = ""
    business_code: str = ""
    method_of_accounting: str = ""  # Cash, Mercantile, Hybrid

    # Manufacturing
    manufacturing_account: ManufacturingAccount = None

    # Trading
    trading_account: TradingAccount = None

    # Profit & Loss
    gross_profit: int = 0
    other_income: int = 0
    closing_stock: int = 0
    opening_stock: int = 0

    # Expenses (from P&L)
    administrative_expenses: int = 0
    other_expenses: int = 0
    disallowed_expenses: int = 0
    depreciation: int = 0

    # Net result
    net_profit: int = 0

    # Depreciation schedules
    depreciation_pm: ScheduleDPM = None
    depreciation_oa: ScheduleDOA = None

    # Deemed CG
    deemed_cg: List[ScheduleDCG] = field(default_factory=list)


@dataclass
class ScheduleBP(Schedule[dict]):
    """Schedule BP — Business / Profession Income.
    
    ITR-4 (Presumptive):
    - 44AD: General businesses (6%/8% of gross receipts)
    - 44ADA: Specified professions (50% of gross receipts)
    - 44AE: Goods carriage (per ton per month)
    
    ITR-3 (Non-Presumptive):
    - Detailed P&L with Manufacturing Account, Trading Account, Depreciation
    
    Used in: ITR-3, ITR-4.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    # ITR-4 Presumptive
    presumptive_44ad: List[BusinessPresumptiveDetail] = field(default_factory=list)
    presumptive_44ada: List[ProfessionPresumptiveDetail] = field(default_factory=list)
    presumptive_44ae: List[TransportPresumptiveDetail] = field(default_factory=list)

    # ITR-3 Non-Presumptive
    business_details: List[BusinessDetailITR3] = field(default_factory=list)

    # Totals
    total_presumptive_income: int = 0
    total_non_presumptive_income: int = 0
    speculative_income: int = 0
    specified_business_income: int = 0

    def total_business_income(self) -> int:
        pres = sum(b.compute_presumptive_income() for b in self.presumptive_44ad)
        pres += sum(p.compute_presumptive_income() for p in self.presumptive_44ada)
        pres += sum(t.compute_presumptive_income() for t in self.presumptive_44ae)
        self.total_presumptive_income = pres
        non_pres = sum(b.net_profit for b in self.business_details)
        self.total_non_presumptive_income = non_pres
        return self.total_presumptive_income + self.total_non_presumptive_income + self.speculative_income + self.specified_business_income

    def compute_income(self) -> Decimal:
        return Decimal(self.total_business_income())

    def to_itr_json(self) -> dict:
        # ITR-4 format
        pres_44ad = [
            {
                "NatureOfBusiness": b.nature_of_business,
                "BusinessCode": b.business_code,
                "GrossReceipts": b.gross_receipts,
                "Profit": b.taxable_profit,
                "PresumptiveRate": b.presumptive_rate,
            }
            for b in self.presumptive_44ad
        ]
        pres_44ada = [
            {
                "NatureOfProfession": p.nature_of_profession,
                "ProfessionCode": p.profession_code,
                "GrossReceipts": p.gross_receipts,
                "Profit": p.taxable_profit,
            }
            for p in self.presumptive_44ada
        ]
        pres_44ae = [
            {
                "VehicleType": t.vehicle_type,
                "NumberOfVehicles": t.number_of_vehicles,
                "Capacity": int(t.capacity_per_vehicle_ton * 1000),
                "Months": t.months_operated,
                "Profit": t.presumptive_income,
            }
            for t in self.presumptive_44ae
        ]

        return {
            "ScheduleBP": {
                "Business44AD": pres_44ad,
                "Business44ADA": pres_44ada,
                "Business44AE": pres_44ae,
                "TotalBPIncome": self.total_business_income(),
                "SpeculativeIncome": self.speculative_income,
                "SpecifiedBusinessIncome": self.specified_business_income,
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        for b in self.presumptive_44ad:
            if b.gross_receipts < 0:
                errors.append(ValidationError("ScheduleBP.44AD", "Receipts cannot be negative", "BLOCKING"))
        return errors
