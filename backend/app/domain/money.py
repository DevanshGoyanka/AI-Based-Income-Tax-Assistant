"""Money value object - immutable integer rupees representation."""
from __future__ import annotations
from dataclasses import dataclass
from decimal import Decimal, ROUND_HALF_UP
from typing import Union


@dataclass(frozen=True, slots=True)
class Money:
    """Immutable money in rupees (integer). ITD requires amounts rounded to ₹10."""
    rupees: int

    @classmethod
    def from_rupees(cls, rupees: Union[int, float, str, Decimal]) -> Money:
        if isinstance(rupees, int):
            return cls(rupees)
        rupees_d = Decimal(str(rupees)).quantize(Decimal("1"), rounding=ROUND_HALF_UP)
        return cls(int(rupees_d))

    def __add__(self, other: Money) -> Money:
        return Money(self.rupees + other.rupees)

    def __sub__(self, other: Money) -> Money:
        return Money(self.rupees - other.rupees)

    def __neg__(self) -> Money:
        return Money(-self.rupees)

    def __mul__(self, n: int) -> Money:
        return Money(self.rupees * n)

    def __lt__(self, other: Money) -> bool:
        return self.rupees < other.rupees

    def __le__(self, other: Money) -> bool:
        return self.rupees <= other.rupees

    def __gt__(self, other: Money) -> bool:
        return self.rupees > other.rupees

    def __ge__(self, other: Money) -> bool:
        return self.rupees >= other.rupees

    def __eq__(self, other: object) -> bool:
        return isinstance(other, Money) and self.rupees == other.rupees

    def __hash__(self) -> int:
        return hash(self.rupees)

    def is_zero(self) -> bool:
        return self.rupees == 0

    def is_negative(self) -> bool:
        return self.rupees < 0

    def abs(self) -> Money:
        return Money(abs(self.rupees))

    def percent(self, percent: Union[int, float, Decimal]) -> Money:
        """Apply percentage. 5% -> 5."""
        pct = Decimal(str(percent))
        result = (Decimal(self.rupees) * pct / 100).quantize(Decimal("1"), rounding=ROUND_HALF_UP)
        return Money(int(result))

    def format_inr(self) -> str:
        """Indian numbering: 1,23,45,678"""
        val = abs(self.rupees)
        s = str(val)
        if len(s) <= 3:
            int_part = s
        else:
            last_three = s[-3:]
            rest = s[:-3]
            if rest:
                groups = [rest[max(0, i-2):i] for i in range(len(rest), 0, -2)]
                int_part = ','.join(reversed(groups)) + ',' + last_three
            else:
                int_part = last_three
        sign = "-" if self.rupees < 0 else ""
        return f"₹{sign}{int_part}"


Money.ZERO = Money(0)
