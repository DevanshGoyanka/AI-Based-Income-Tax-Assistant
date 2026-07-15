"""Interest u/s 234A, 234B, 234C, 234F — OUR OWN implementation.

Referenced OpenTax's Interest234Service (vendor/filing/tax_calculation/) as oracle.
Reimplemented from scratch using our own data structures and Decimal precision.

Key rules:
  234A — 1% per month (or part) on unpaid tax from due date to filing date.
  234B — 1% per month on advance-tax shortfall (April AY → filing date),
         with monthly SAT adjustments reducing outstanding principal.
  234C — 1% × 3 months per quarter on quarterly advance-tax shortfall
         (Q1=15%, Q2=45%, Q3=75%, Q4=100%); Q4 shortfall charged 1 month.
  234F — Flat late-filing fee (₹1,000 or ₹5,000) if return filed after due date.

Exemptions:
  • Senior citizens (age > 59) are exempt from 234B and 234C.
  • 234C does not apply if (NetTaxLiability − TDS − TCS) < ₹10,000.
"""
from dataclasses import dataclass
from datetime import date
from decimal import Decimal, ROUND_HALF_EVEN
from typing import List, Optional

# ─── Dataclasses ──────────────────────────────────────────────────────────────


@dataclass
class TaxPayment:
    """Advance or self-assessment tax payment."""
    amount: int          # Amount in rupees
    payment_date: date   # Date of payment


@dataclass
class InterestResult:
    """Result of interest calculation under sections 234A/B/C/F."""
    interest_234a: int = 0
    interest_234b: int = 0
    interest_234c: int = 0
    late_fee_234f: int = 0

    @property
    def total(self) -> int:
        return self.interest_234a + self.interest_234b + self.interest_234c + self.late_fee_234f


# ─── Constants ────────────────────────────────────────────────────────────────

NET_TAX_THRESHOLD = 10_000     # 234B/C minimum threshold
ADVANCE_TAX_PCT = 90            # 90% of assessed tax required by year-end
Q1_PCT = Decimal("0.15")        # 15% required by Jun 15
Q2_PCT = Decimal("0.45")        # 45% required by Sep 15
Q3_PCT = Decimal("0.75")         # 75% required by Dec 15
Q4_PCT = Decimal("1.00")         # 100% required by Mar 15
INTEREST_RATE = Decimal("0.01")  # 1% per month


# ─── Core Engine ──────────────────────────────────────────────────────────────


class Interest234Engine:
    """Our owned interest 234A/B/C/F computation engine."""

    def compute(
        self,
        net_tax_liability: int,
        tds: int,
        tcs: int,
        tax_payments: List[TaxPayment],
        age: int,
        assessment_year: str,
        filing_date: Optional[date] = None,
        due_date: Optional[date] = None,
        total_income: int = 0,
        is_resident: bool = True,
    ) -> InterestResult:
        """Compute interest u/s 234A, 234B, 234C and late fee 234F.

        Args:
            net_tax_liability: Total tax + cess liability.
            tds: Total TDS available as credit.
            tcs: Total TCS available as credit.
            tax_payments: List of advance tax + self-assessment tax payments.
            age: Taxpayer's age as of FY end (Mar 31 of AY start year).
            assessment_year: e.g. "2026-27".
            filing_date: Date return was filed/verified.
            due_date: Due date for filing (defaults to Jul 31 of AY start year).
            total_income: Taxable income (used for 234F fee tier).
            is_resident: Residential status (residents get senior exemptions).
        """
        ay_start = _ay_start_year(assessment_year)
        due_date = due_date or date(ay_start, 7, 31)
        filing_date = filing_date or date.today()

        fy_start = date(ay_start - 1, 4, 1)
        fy_end = date(ay_start, 3, 31)

        # Separate advance tax (paid during FY) from self-assessment tax (paid after FY)
        advance_tax_total = 0
        sat_before_due = 0
        sat_by_month: dict[int, int] = {}   # month_offset → amount

        for pay in tax_payments:
            if fy_start <= pay.payment_date <= fy_end:
                advance_tax_total += pay.amount
            elif pay.payment_date > fy_end:
                if pay.payment_date <= due_date:
                    sat_before_due += pay.amount
                # Track SAT by month offset from April of AY for 234B
                month_offset = (
                    (pay.payment_date.year - ay_start) * 12
                    + (pay.payment_date.month - 4)
                    + 1
                )
                if month_offset >= 1:
                    sat_by_month[month_offset] = sat_by_month.get(month_offset, 0) + pay.amount

        result = InterestResult()

        # 234F — Late filing fee (computed before 234B)
        result.late_fee_234f = self._calc_234f(filing_date, due_date, total_income)

        # 234A — Interest for late filing
        result.interest_234a = self._calc_234a(
            net_tax_liability, advance_tax_total, tds, tcs,
            sat_before_due, filing_date, due_date,
        )

        # Senior citizens (age > 59) who are resident exempt from 234B and 234C
        exempt_234bc = age > 59 and is_resident

        # 234C — Deferment of advance tax (computed before 234B)
        if not exempt_234bc:
            result.interest_234c = self._calc_234c(
                net_tax_liability, tds, tcs, tax_payments, assessment_year,
            )

        # 234B — Default in advance tax
        if not exempt_234bc:
            result.interest_234b = self._calc_234b(
                net_tax_liability, advance_tax_total, tds, tcs,
                sat_by_month, filing_date, due_date, assessment_year,
                result.interest_234c, result.late_fee_234f,
            )

        return result

    # ── 234A ──────────────────────────────────────────────────────────────────

    def _calc_234a(
        self,
        net_tax_liability: int,
        advance_tax: int,
        tds: int,
        tcs: int,
        sat_before_due: int,
        filing_date: date,
        due_date: date,
    ) -> int:
        """234A: 1% per month (or part) on outstanding tax from due date to filing.

        Principal = MAX(0, FLOOR(NetTax − AdvanceTax − TDS − TCS − SAT_before_due, 100))
        Interest  = Principal × 0.01 × months_late
        """
        if filing_date <= due_date:
            return 0

        principal = net_tax_liability - advance_tax - tds - tcs - sat_before_due
        if principal <= 0:
            return 0

        principal = _floor_100(principal)
        if principal <= 0:
            return 0

        months = _months_between(due_date, filing_date)
        interest = int(Decimal(principal) * INTEREST_RATE * months)
        return max(0, interest)

    # ── 234B ───────────────────────────────────────────────────────────────────

    def _calc_234b(
        self,
        net_tax_liability: int,
        advance_tax: int,
        tds: int,
        tcs: int,
        sat_by_month: dict[int, int],
        filing_date: date,
        due_date: date,
        assessment_year: str,
        interest_234c: int,
        late_fee_234f: int,
    ) -> int:
        """234B: 1% per month on advance-tax shortfall.

        Applies when:
          (NetTax − TDS − TCS) ≥ ₹10,000  AND
          AdvanceTax < 90% of (NetTax − TDS − TCS)
        """
        assessed_tax = net_tax_liability - tds - tcs
        if assessed_tax < NET_TAX_THRESHOLD:
            return 0

        required = int(assessed_tax * Decimal(ADVANCE_TAX_PCT) / 100)
        if advance_tax >= required:
            return 0

        shortfall = _floor_100(max(0, net_tax_liability - advance_tax - tds - tcs))
        if shortfall <= 0:
            return 0

        ay_start = _ay_start_year(assessment_year)
        if filing_date < date(ay_start, 4, 1):
            return 0

        # Total months from April of AY to filing date
        total_months = (
            (filing_date.month - 4)
            + (filing_date.year - ay_start) * 12
            + 1
        )
        if total_months <= 0:
            return 0

        due_month_offset = (
            (due_date.year - ay_start) * 12
            + (due_date.month - 4)
            + 1
        )

        # Month-by-month loop: SAT first reduces interest, then principal
        carry_principal = Decimal(shortfall)
        carry_interest = Decimal("0")
        total_interest = Decimal("0")

        for month_i in range(1, total_months + 1):
            balance_principal = carry_principal
            balance_interest = carry_interest + INTEREST_RATE * Decimal(_floor_100(int(balance_principal)))
            if month_i == 1:
                balance_interest += Decimal(interest_234c)
            if month_i == due_month_offset:
                balance_interest += Decimal(late_fee_234f)

            sat_payment = Decimal(sat_by_month.get(month_i, 0))
            if sat_payment > 0:
                adj_interest = min(sat_payment, balance_interest)
                remaining = sat_payment - adj_interest
                adj_principal = min(remaining, balance_principal)
                carry_principal = max(Decimal("0"), balance_principal - adj_principal)
                carry_interest = max(Decimal("0"), balance_interest - adj_interest)
            else:
                carry_principal = balance_principal
                carry_interest = balance_interest

            # Accumulate interest for this month
            monthly = INTEREST_RATE * Decimal(_floor_100(int(balance_principal)))
            total_interest += monthly

        return max(0, int(_vba_round(total_interest)))

    # ── 234C ──────────────────────────────────────────────────────────────────

    def _calc_234c(
        self,
        net_tax_liability: int,
        tds: int,
        tcs: int,
        tax_payments: List[TaxPayment],
        assessment_year: str,
    ) -> int:
        """234C: Interest on quarterly advance-tax shortfall.

        Q1 (Jun 15): 15% required  — shortfall × 1% × 3 months
        Q2 (Sep 15): 45% required  — shortfall × 1% × 3 months
        Q3 (Dec 15): 75% required  — shortfall × 1% × 3 months
        Q4 (Mar 15): 100% required — shortfall × 1% × 1 month
        """
        assessed_tax = net_tax_liability - tds - tcs
        if assessed_tax < NET_TAX_THRESHOLD:
            return 0

        ay_start = _ay_start_year(assessment_year)
        fy_start_year = ay_start - 1

        q1_end = date(fy_start_year, 6, 15)
        q2_end = date(fy_start_year, 9, 15)
        q3_end = date(fy_start_year, 12, 15)
        q4_end = date(ay_start, 3, 15)

        # Bucket payments by quarter
        quarterly: dict[int, int] = {0: 0, 1: 0, 2: 0, 3: 0, 4: 0}
        for pay in tax_payments:
            if pay.payment_date <= q1_end:
                quarterly[0] += pay.amount
            elif pay.payment_date <= q2_end:
                quarterly[1] += pay.amount
            elif pay.payment_date <= q3_end:
                quarterly[2] += pay.amount
            elif pay.payment_date <= q4_end:
                quarterly[3] += pay.amount
            else:
                quarterly[4] += pay.amount

        net_base = assessed_tax
        if net_base <= 0:
            return 0

        interest_total = 0

        # Q1
        required = int(Decimal(net_base) * Q1_PCT)
        cum_paid = quarterly[0]
        threshold_12 = _floor_100(int(Decimal("0.12") * Decimal(net_base)))
        shortfall = max(0, required - cum_paid)
        if cum_paid < threshold_12:
            shortfall = max(0, shortfall)
        else:
            shortfall = 0
        if shortfall > 100:
            shortfall = _round_down_100(shortfall)
        interest_total += int(Decimal(shortfall) * Decimal("0.01") * 3)

        # Q2
        required = int(Decimal(net_base) * Q2_PCT)
        cum_paid += quarterly[1]
        threshold_36 = _floor_100(int(Decimal("0.36") * Decimal(net_base)))
        shortfall = max(0, required - cum_paid)
        if cum_paid >= threshold_36:
            shortfall = 0
        if shortfall > 100:
            shortfall = _round_down_100(shortfall)
        interest_total += int(Decimal(shortfall) * Decimal("0.01") * 3)

        # Q3
        required = int(Decimal(net_base) * Q3_PCT)
        cum_paid += quarterly[2]
        shortfall = max(0, required - cum_paid)
        if shortfall > 100:
            shortfall = _round_down_100(shortfall)
        interest_total += int(Decimal(shortfall) * Decimal("0.01") * 3)

        # Q4
        required = int(Decimal(net_base) * Q4_PCT)
        cum_paid += quarterly[3]
        shortfall = max(0, required - cum_paid)
        if shortfall > 100:
            shortfall = _round_down_100(shortfall)
        interest_total += int(Decimal(shortfall) * Decimal("0.01") * 1)

        return max(0, interest_total)

    # ── 234F ───────────────────────────────────────────────────────────────────

    def _calc_234f(
        self,
        filing_date: date,
        due_date: date,
        total_income: int,
    ) -> int:
        """234F: Late filing fee.

        If total income > ₹5L → ₹5,000
        If total income ≤ ₹5L → ₹1,000
        """
        if filing_date <= due_date:
            return 0
        return 1000 if total_income <= 500_000 else 5000


# ─── Module helpers ──────────────────────────────────────────────────────────

def _ay_start_year(assessment_year: str) -> int:
    """Extract start year from AY string e.g. '2026-27' → 2026."""
    return int(assessment_year.split("-")[0])


def _floor_100(value: int) -> int:
    """Floor to nearest 100."""
    return (value // 100) * 100


def _round_down_100(value: int) -> int:
    """Round down to nearest 100."""
    return (value // 100) * 100


def _months_between(start: date, end: date) -> int:
    """Count months (including partial months as full months)."""
    if end <= start:
        return 0
    months = (end.year - start.year) * 12 + (end.month - start.month)
    if end.day > start.day:
        months += 1
        # Adjust for months without day 31
        if start.day == 30 and start.month in (4, 6, 9, 11):
            months -= 1
    elif start.day == 7 and end.day <= start.day:
        months += 1
    return max(0, months)


def _vba_round(val: Decimal) -> Decimal:
    """Banker's rounding to nearest integer (VBA default)."""
    return val.quantize(Decimal("1"), rounding=ROUND_HALF_EVEN)
