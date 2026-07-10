"""ITD Rounding Rules - Centralized per ITD & Indian-tax practice.

Source: Income-tax Rules, 1962 + CBDT circulars + ITD schema clarifications.
Reference: ITR-1/ITR-2 JSON schema requires all amounts at schedule level to be
multiples of ₹10, except TDS/TCS/advance tax/self-assessment which stay at ₹1.
"""
from app.domain.money import Money


class RoundingRules:
    """Centralized ITD rounding. All money goes through these methods."""

    @staticmethod
    def round_to_nearest_ten_rupees(m: Money) -> Money:
        """SECTION 288: All ITR schedule amounts to nearest ₹10.
        Used for: all heads, deductions, GTI, tax, surcharge, cess, refund/demand.
        Per ITD ITR-1/ITR-2 JSON schema, every field is a multiple of ₹10.
        """
        r = m.rupees
        if r >= 0:
            return Money((r + 5) // 10 * 10)
        return Money(-(((-r) + 5) // 10 * 10))

    @staticmethod
    def round_advance_tax_installment(m: Money) -> Money:
        """Advance-tax installments: nearest ₹1, never negative."""
        return Money(max(0, m.rupees))

    @staticmethod
    def round_tds_tcs(m: Money) -> Money:
        """TDS/TCS as per Form 26AS: no rounding, already integer rupees."""
        return m

    @staticmethod
    def round_indexed_cost(c: Money) -> Money:
        """CII-indexed cost: no rounding, already integer rupees."""
        return c

    @staticmethod
    def force_ten_multiple(m: Money) -> Money:
        """Hard-asserts ITR-1/ITR-2 JSON contract: every field is ₹10 multiple."""
        rounded = RoundingRules.round_to_nearest_ten_rupees(m)
        if rounded.rupees % 10 != 0:
            raise ValueError(f"ITD rounding invariant violated: {m} -> {rounded}")
        return rounded
