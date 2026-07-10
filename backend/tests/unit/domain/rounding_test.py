"""ITD Rounding Rules tests - all schedule amounts must be ₹10 multiples."""
import pytest
from app.domain.money import Money
from app.domain.rule_engine.rounding import RoundingRules


class TestRoundToTen:
    def test_already_multiple(self):
        assert RoundingRules.round_to_nearest_ten_rupees(Money(100)) == Money(100)
        assert RoundingRules.round_to_nearest_ten_rupees(Money(123450)) == Money(123450)

    def test_round_up(self):
        assert RoundingRules.round_to_nearest_ten_rupees(Money(105)) == Money(110)
        assert RoundingRules.round_to_nearest_ten_rupees(Money(123456)) == Money(123460)

    def test_round_down(self):
        assert RoundingRules.round_to_nearest_ten_rupees(Money(104)) == Money(100)
        assert RoundingRules.round_to_nearest_ten_rupees(Money(123451)) == Money(123450)

    def test_round_half_up(self):
        assert RoundingRules.round_to_nearest_ten_rupees(Money(105)) == Money(110)
        assert RoundingRules.round_to_nearest_ten_rupees(Money(115)) == Money(120)

    def test_zero(self):
        assert RoundingRules.round_to_nearest_ten_rupees(Money.ZERO) == Money.ZERO

    def test_negative_rounds_correctly(self):
        assert RoundingRules.round_to_nearest_ten_rupees(Money(-104)) == Money(-100)
        assert RoundingRules.round_to_nearest_ten_rupees(Money(-105)) == Money(-110)
        assert RoundingRules.round_to_nearest_ten_rupees(Money(-123456)) == Money(-123460)


class TestAdvanceTaxInstallment:
    def test_non_negative(self):
        assert RoundingRules.round_advance_tax_installment(Money(-500)) == Money.ZERO
        assert RoundingRules.round_advance_tax_installment(Money(1000)) == Money(1000)


class TestTdsTcs:
    def test_no_rounding_rupees(self):
        """TDS/TCS already integer rupees, no rounding needed."""
        assert RoundingRules.round_tds_tcs(Money(12345)) == Money(12345)


class TestForceTenMultiple:
    def test_passes_for_clean(self):
        assert RoundingRules.force_ten_multiple(Money(1000)) == Money(1000)

    def test_invariant_holds(self):
        result = RoundingRules.force_ten_multiple(Money(123456))
        assert result.rupees % 10 == 0


class TestIdempotence:
    def test_round_of_round_is_same(self):
        """Critical: rounding must be idempotent."""
        for x in [0, 1, 5, 9, 10, 11, 99, 100, 101, 999, 1234567]:
            m = Money(x)
            once = RoundingRules.round_to_nearest_ten_rupees(m)
            twice = RoundingRules.round_to_nearest_ten_rupees(once)
            assert once == twice, f"Not idempotent at {x}: {once} -> {twice}"
