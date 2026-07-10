"""ComputedReturn - the single canonical computation object. NO parallel paths."""
from __future__ import annotations
from dataclasses import dataclass
from app.domain.money import Money
from app.domain.enums import TaxRegime, ITRFormType


@dataclass(frozen=True)
class SalaryHead:
    gross: Money
    exemptions_10: Money
    profits_in_lieu: Money
    perquisites: Money
    deduction_16: Money  # Standard + Prof Tax
    net: Money


@dataclass(frozen=True)
class HousePropertyHead:
    annual_value: Money
    municipal_taxes: Money
    standard_deduction_30pct: Money
    interest_24b: Money
    net: Money
    loss_capped: Money


@dataclass(frozen=True)
class BusinessHead:
    net_profit: Money
    depreciation: Money
    net_business_income: Money


@dataclass(frozen=True)
class CapitalGainHead:
    stcg_111a: Money
    ltcg_112a: Money
    stcg_other: Money
    ltcg_other: Money
    sec_54_exempt: Money
    sec_54f_exempt: Money
    sec_54ec_exempt: Money
    net: Money


@dataclass(frozen=True)
class OtherSourcesHead:
    interest: Money
    dividends: Money
    winnings_115bb: Money
    family_pension: Money
    gifts_56_2: Money
    vda: Money
    rental_machinery: Money
    net: Money


@dataclass(frozen=True)
class DeductionsChapterVIA:
    total_80c: Money
    total_80ccd: Money
    total_80d: Money
    total_80g: Money
    other_80: Money
    total: Money


@dataclass(frozen=True)
class LossesAfterSetOff:
    cyla: dict[str, Money]
    bfla: dict[str, Money]
    cfl: dict[str, Money]


@dataclass(frozen=True)
class TaxComputation:
    gti: Money
    total_deductions: Money
    total_income: Money
    tax_on_total_income: Money
    surcharge: Money
    h_and_ec_cess: Money
    rebate_87a: Money
    marginal_relief: Money
    tax_after_rebate: Money
    interest_234a: Money
    interest_234b: Money
    interest_234c: Money
    late_fee_234f: Money
    total_tax_liability: Money
    tds_tcs_total: Money
    advance_tax: Money
    self_assessment_tax: Money
    refund_or_demand: Money


@dataclass(frozen=True)
class ComputedReturn:
    """Single canonical computation object."""
    ay: str
    itr_form: ITRFormType
    regime: TaxRegime
    pan: str
    rule_version: str
    computed_at: str
    salary: SalaryHead
    house_property: HousePropertyHead
    business: BusinessHead
    capital_gains: CapitalGainHead
    other_sources: OtherSourcesHead
    deductions: DeductionsChapterVIA
    losses: LossesAfterSetOff
    tax: TaxComputation
    snapshot_hash: str = ""
