"""AY Rule Engine tests - verify AY 2026-27 / 2027-28 / registry behaviour."""
import pytest
from app.domain.enums import TaxRegime, RuleLifecycle
from app.domain.rule_engine.registry import get_rules, list_known_ays, latest_supported
from app.domain.rule_engine.ay_2026_27 import AY2026_27Rules


class TestAY2026_27Basics:
    def test_ay_string(self):
        r = get_rules("2026-27")
        assert r.assessment_year == "2026-27"
        assert r.governing_act == "1961"
        assert r.lifecycle == RuleLifecycle.SUPPORTED

    def test_new_regime_slabs(self):
        r = get_rules("2026-27")
        limits = r.slab_limits(TaxRegime.NEW)
        rates = r.slab_rates_bps(TaxRegime.NEW)
        # 0, 3L, 7L, 10L, 12L, 15L
        assert limits == [0, 300000, 700000, 1000000, 1200000, 1500000]
        # 0%, 5%, 10%, 15%, 20%, 30%
        assert rates == [0, 500, 1000, 1500, 2000, 3000]

    def test_old_regime_slabs(self):
        r = get_rules("2026-27")
        limits = r.slab_limits(TaxRegime.OLD)
        rates = r.slab_rates_bps(TaxRegime.OLD)
        # 0, 2.5L, 5L, 10L
        assert limits == [0, 250000, 500000, 1000000]
        # 0%, 5%, 20%, 30%
        assert rates == [0, 500, 2000, 3000]

    def test_standard_deduction(self):
        r = get_rules("2026-27")
        assert r.standard_deduction(TaxRegime.NEW).rupees == 75000
        assert r.standard_deduction(TaxRegime.OLD).rupees == 50000

    def test_rebate_87a(self):
        r = get_rules("2026-27")
        # New regime: ₹12L threshold, ₹60,000 rebate (last AY of 1961 Act, with enhancements)
        assert r.rebate_87a_threshold(TaxRegime.NEW).rupees == 1200000
        assert r.rebate_87a_amount(TaxRegime.NEW).rupees == 60000
        # Old regime: ₹5L threshold, ₹12,500 rebate
        assert r.rebate_87a_threshold(TaxRegime.OLD).rupees == 500000
        assert r.rebate_87a_amount(TaxRegime.OLD).rupees == 12500

    def test_80c_limit(self):
        r = get_rules("2026-27")
        assert r.sec_80c_limit(TaxRegime.NEW).rupees == 0  # Not allowed
        assert r.sec_80c_limit(TaxRegime.OLD).rupees == 150000

    def test_capital_gains_rates(self):
        r = get_rules("2026-27")
        assert r.sec_111a_rate_bps() == 2000  # 20%
        assert r.sec_112a_rate_bps() == 1250  # 12.5%
        assert r.sec_112a_exemption().rupees == 125000

    def test_cii_for_fy(self):
        r = get_rules("2026-27")
        assert r.cii_for("FY2025-26") == 376
        assert r.cii_for("FY2024-25") == 363
        assert r.cii_for("FY2023-24") == 348


class TestAYRegistry:
    def test_discovers_both_ays(self):
        known = list_known_ays()
        ay_codes = {d["ay"] for d in known}
        assert "2026-27" in ay_codes
        assert "2027-28" in ay_codes

    def test_2027_28_is_draft(self):
        r = get_rules("2027-28")
        assert r.lifecycle == RuleLifecycle.DRAFT
        assert r.governing_act == "2025"

    def test_latest_supported(self):
        latest = latest_supported()
        # 2026-27 should be the latest SUPPORTED (2027-28 is DRAFT)
        assert latest.assessment_year == "2026-27"
        assert latest.lifecycle == RuleLifecycle.SUPPORTED

    def test_ay_metadata_complete(self):
        for entry in list_known_ays():
            assert "ay" in entry
            assert "act" in entry
            assert "lifecycle" in entry
            assert "version" in entry


class TestAYIsolations:
    """Critical: switching AY does NOT mutate prior cached rules."""

    def test_switching_ay_keeps_cache_separate(self):
        r1 = get_rules("2026-27")
        r2 = get_rules("2027-28")
        assert r1.assessment_year != r2.assessment_year
        assert r1.governing_act != r2.governing_act
        assert r1 is not r2
