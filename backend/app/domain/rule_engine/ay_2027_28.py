"""AY 2027-28 - Income-tax Act, 2025 - DRAFT.

Effective: 1-Apr-2027. Major overhaul: new slab structure, renamed sections.
Currently skeleton - to be fleshed out when CBDT notifies forms in early 2027.
"""
from app.domain.money import Money
from app.domain.enums import TaxRegime, RuleLifecycle


class AY2027_28Rules:
    assessment_year = "2027-28"
    version = "0.1.0-draft"
    lifecycle = RuleLifecycle.DRAFT
    governing_act = "2025"

    def slab_limits(self, regime: TaxRegime) -> list[int]:
        # Placeholder - awaiting CBDT notification
        return [0, 400000, 800000, 1200000, 1600000, 2000000, 2400000]

    def slab_rates_bps(self, regime: TaxRegime) -> list[int]:
        return [0, 500, 1000, 1500, 2000, 2500, 3000]

    def standard_deduction(self, regime: TaxRegime) -> Money:
        return Money.from_rupees(75000)

    def rebate_87a_threshold(self, regime: TaxRegime) -> Money:
        return Money.from_rupees(1200000)

    def rebate_87a_amount(self, regime: TaxRegime) -> Money:
        return Money.from_rupees(60000)

    def sec_80c_limit(self, regime: TaxRegime) -> Money:
        return Money.ZERO

    def sec_80ccd_1b_limit(self, regime: TaxRegime) -> Money:
        return Money.ZERO

    def sec_80d_limit(self, regime: TaxRegime, senior: bool = False) -> Money:
        return Money.ZERO

    def sec_24b_cap(self) -> Money:
        return Money.from_rupees(200000)

    def sec_80tta_limit(self) -> Money:
        return Money.from_rupees(10000)

    def sec_111a_rate_bps(self) -> int:
        return 2000

    def sec_112a_rate_bps(self) -> int:
        return 1250

    def sec_112a_exemption(self) -> Money:
        return Money.from_rupees(125000)

    def cii_for(self, fy: str) -> int:
        return {"FY2026-27": 389, "FY2025-26": 376}.get(fy, 389)

    def sec_44ad_turnover_limit(self) -> Money:
        return Money.from_rupees(30000000)

    def sec_44ad_rate_bps(self, is_cash: bool) -> int:
        return 600 if is_cash else 800

    def sec_44ada_turnover_limit(self) -> Money:
        return Money.from_rupees(7500000)

    def sec_44ada_rate_bps(self) -> int:
        return 5000

    def vda_rate_bps(self) -> int:
        return 3000

    def vda_no_loss_setoff(self) -> bool:
        return True

    def allowed_itr_forms(self, profile: dict) -> list[str]:
        return ["ITR-1", "ITR-2", "ITR-3", "ITR-4"]

    def new_regime_allowed_deductions(self) -> set[str]:
        return {"80CCD_2", "80CCH_2", "80JJAA", "standard_deduction", "family_pension_25k"}

    def sec_10_exemptions(self) -> dict[str, str]:
        return {}


RULES = AY2027_28Rules()
