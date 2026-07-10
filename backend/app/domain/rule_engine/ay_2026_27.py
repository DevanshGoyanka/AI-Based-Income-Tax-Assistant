"""AY 2026-27 - Income-tax Act, 1961 - LAST year of 1961 Act applicability.

Source: CBDT notification 30-Mar-2026 + corrigendum 10-Apr-2026.
Effective: For FY 2025-26 / AY 2026-27. From AY 2027-28, IT Act 2025 applies.
"""
from app.domain.money import Money
from app.domain.enums import TaxRegime, RuleLifecycle


class AY2026_27Rules:
    assessment_year = "2026-27"
    version = "1.0.0"
    lifecycle = RuleLifecycle.SUPPORTED
    governing_act = "1961"

    # New regime slabs (CBDT notification 30-Mar-2026 + corrigendum 10-Apr-2026)
    _NEW_LIMITS = [0, 300000, 700000, 1000000, 1200000, 1500000]
    _NEW_RATES = [0, 500, 1000, 1500, 2000, 3000]  # bps

    # Old regime slabs
    _OLD_LIMITS = [0, 250000, 500000, 1000000]
    _OLD_RATES = [0, 500, 2000, 3000]  # bps

    def slab_limits(self, regime: TaxRegime) -> list[int]:
        return self._NEW_LIMITS if regime == TaxRegime.NEW else self._OLD_LIMITS

    def slab_rates_bps(self, regime: TaxRegime) -> list[int]:
        return self._NEW_RATES if regime == TaxRegime.NEW else self._OLD_RATES

    def standard_deduction(self, regime: TaxRegime) -> Money:
        return Money.from_rupees(75000 if regime == TaxRegime.NEW else 50000)

    def rebate_87a_threshold(self, regime: TaxRegime) -> Money:
        return Money.from_rupees(1200000 if regime == TaxRegime.NEW else 500000)

    def rebate_87a_amount(self, regime: TaxRegime) -> Money:
        return Money.from_rupees(60000 if regime == TaxRegime.NEW else 12500)

    def sec_80c_limit(self, regime: TaxRegime) -> Money:
        return Money.ZERO if regime == TaxRegime.NEW else Money.from_rupees(150000)

    def sec_80ccd_1b_limit(self, regime: TaxRegime) -> Money:
        return Money.ZERO if regime == TaxRegime.NEW else Money.from_rupees(50000)

    def sec_80d_limit(self, regime: TaxRegime, senior: bool = False) -> Money:
        if regime == TaxRegime.NEW:
            return Money.ZERO
        return Money.from_rupees(50000 if senior else 25000)

    def sec_24b_cap(self) -> Money:
        return Money.from_rupees(200000)

    def sec_80tta_limit(self) -> Money:
        return Money.from_rupees(10000)

    def sec_111a_rate_bps(self) -> int:
        return 2000  # 20%

    def sec_112a_rate_bps(self) -> int:
        return 1250  # 12.5%

    def sec_112a_exemption(self) -> Money:
        return Money.from_rupees(125000)

    def cii_for(self, fy: str) -> int:
        return {"FY2025-26": 376, "FY2024-25": 363, "FY2023-24": 348}.get(fy, 376)

    def sec_44ad_turnover_limit(self) -> Money:
        return Money.from_rupees(30000000)

    def sec_44ad_rate_bps(self, is_cash: bool) -> int:
        return 600 if is_cash else 800  # 6% cash, 8% non-cash

    def sec_44ada_turnover_limit(self) -> Money:
        return Money.from_rupees(7500000)

    def sec_44ada_rate_bps(self) -> int:
        return 5000  # 50%

    def vda_rate_bps(self) -> int:
        return 3000  # 30% flat

    def vda_no_loss_setoff(self) -> bool:
        return True

    def allowed_itr_forms(self, profile: dict) -> list[str]:
        forms = []
        if profile.get("has_business"):
            return ["ITR-3", "ITR-4"]
        if profile.get("hp_count", 0) <= 2 and profile.get("total", 0) <= 5000000:
            forms.append("ITR-1")
        if profile.get("cg_present") or profile.get("hp_count", 0) > 2:
            forms.append("ITR-2")
        return forms or ["ITR-2"]

    def new_regime_allowed_deductions(self) -> set[str]:
        return {"80CCD_2", "80CCH_2", "80JJAA", "standard_deduction", "family_pension_25k"}

    def sec_10_exemptions(self) -> dict[str, str]:
        return {
            "10(13A)": "HRA",
            "10(5)": "LTA",
            "10(10)": "Gratuity",
            "10(10AA)": "VRS",
            "10(10C)": "Commuted pension",
            "10(1)": "Agri income",
            "10(14)": "Other allowances",
            "10(23C)": "Trust income",
        }


RULES = AY2026_27Rules()
