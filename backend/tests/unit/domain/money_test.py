"""Money value object tests - integer rupees, immutable, exact arithmetic."""
import pytest
from decimal import Decimal
from app.domain.money import Money


class TestMoneyBasics:
    def test_from_rupees_int(self):
        m = Money.from_rupees(100)
        assert m.rupees == 100
        assert m == Money(100)

    def test_from_rupees_float_rounds(self):
        m = Money.from_rupees(100.4)
        assert m.rupees == 100
        m = Money.from_rupees(100.6)
        assert m.rupees == 101

    def test_from_rupees_string(self):
        m = Money.from_rupees("12345")
        assert m.rupees == 12345

    def test_from_rupees_decimal(self):
        m = Money.from_rupees(Decimal("99999.50"))
        assert m.rupees == 100000  # rounds up half

    def test_zero_constant(self):
        assert Money.ZERO.rupees == 0
        assert Money.ZERO.is_zero()

    def test_immutability(self):
        m = Money(100)
        with pytest.raises(Exception):  # FrozenInstanceError
            m.rupees = 200  # type: ignore


class TestMoneyArithmetic:
    def test_addition(self):
        a = Money(100)
        b = Money(50)
        assert (a + b).rupees == 150

    def test_subtraction(self):
        a = Money(100)
        b = Money(30)
        assert (a - b).rupees == 70

    def test_subtraction_can_be_negative(self):
        a = Money(30)
        b = Money(100)
        assert (a - b).rupees == -70
        assert (a - b).is_negative()

    def test_negation(self):
        a = Money(50)
        assert (-a).rupees == -50
        assert (-a).is_negative()

    def test_multiplication_by_int(self):
        a = Money(100)
        assert (a * 3).rupees == 300

    def test_zero_addition(self):
        a = Money(100)
        assert (a + Money.ZERO) == a

    def test_chained_operations_no_drift(self):
        """Critical: no precision drift across many operations."""
        a = Money(1234567)
        b = Money(9876543)
        result = a + b - a - b
        assert result.rupees == 0


class TestMoneyComparison:
    def test_lt(self):
        assert Money(100) < Money(200)
        assert not (Money(200) < Money(100))

    def test_le(self):
        assert Money(100) <= Money(100)
        assert Money(100) <= Money(200)

    def test_gt(self):
        assert Money(200) > Money(100)

    def test_ge(self):
        assert Money(100) >= Money(100)

    def test_hashable(self):
        m = Money(100)
        s = {m, Money(200), Money(100)}
        assert len(s) == 2


class TestMoneyPercent:
    def test_percent_basic(self):
        m = Money(1000)
        assert m.percent(5).rupees == 50
        assert m.percent(10).rupees == 100
        assert m.percent(12.5).rupees == 125  # half-up

    def test_percent_rounds_half_up(self):
        m = Money(100)
        assert m.percent(2.5).rupees == 3  # 2.5 -> 3

    def test_percent_zero(self):
        assert Money(1000).percent(0) == Money.ZERO


class TestMoneyAbs:
    def test_abs_negative(self):
        assert Money(-100).abs() == Money(100)

    def test_abs_positive(self):
        assert Money(100).abs() == Money(100)

    def test_abs_zero(self):
        assert Money.ZERO.abs() == Money.ZERO


class TestMoneyFormat:
    def test_format_small(self):
        assert Money(500).format_inr() == "₹500"

    def test_format_thousands(self):
        assert Money(1234).format_inr() == "₹1,234"

    def test_format_lakhs(self):
        assert Money(123456).format_inr() == "₹1,23,456"

    def test_format_crores(self):
        assert Money(12345678).format_inr() == "₹1,23,45,678"

    def test_format_negative(self):
        assert Money(-500).format_inr() == "₹-500"
