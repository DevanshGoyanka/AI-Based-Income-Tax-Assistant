"""OUR OWNED tax computation engine — TaxEngine.

This is the single source of truth for all tax computation.
It is NOT OpenTax. OpenTax vendored code is reference/test-oracle only.

Data flows: Filing → Schedules → TaxEngine.compute() → ComputedReturn (immutable snapshot)

The engine computes BOTH regimes always and returns:
  - `tax_breakdown`: breakdown for the requested regime
  - `old_regime_breakdown`: breakdown for comparison
  - `new_regime_breakdown`: breakdown for comparison
"""
from dataclasses import dataclass, field
from datetime import date
from decimal import Decimal
from typing import Dict, List, Optional
from uuid import UUID

from app.core.domain.computed_return import (
    TaxBreakdown,
    SlabBreakdown,
    ComputationStep,
    InterestBreakdown,
    CGRateBucketBreakdown,
    CapitalGainsBreakdown,
)
from app.core.domain.schedules.base import Schedule
from app.core.domain.schedules.salary import ScheduleSalary, SalaryDetail
from app.core.domain.schedules.house_property import ScheduleHP, HPDetail
from app.core.domain.schedules.other_sources import ScheduleOS, InterestDetail, DividendDetail
from app.core.domain.schedules.schedule_via import ScheduleVIA
from app.core.domain.schedules.tds import ScheduleTDS
from app.core.domain.schedules.schedule_it import ScheduleIT
from app.core.domain.schedules.schedule_cg import ScheduleCG, CGTransaction, CGExemptionThreshold
from app.core.services.slab_tables import (
    get_rules,
    compute_slab_tax,
    RuleVersion,
)
from app.core.services.interest_234_engine import (
    Interest234Engine,
    InterestResult,
    TaxPayment,
)


# ─── Context ─────────────────────────────────────────────────────────────────

@dataclass
class ComputationContext:
    """Immutable context for one tax computation."""
    filing_id: UUID
    client_id: UUID
    pan: str
    ay: str
    regime: str          # "old" or "new"
    dob: Optional[date] = None

    def age(self, fy_end: Optional[date] = None) -> int:
        """Compute age as of FY end (Mar 31 of AY)."""
        if self.dob is None:
            return 0
        ref = fy_end or self._default_fy_end()
        if ref <= self.dob:
            return 0
        age = ref.year - self.dob.year
        if (ref.month, ref.day) < (self.dob.month, self.dob.day):
            age -= 1
        return age

    def _default_fy_end(self) -> date:
        ay_start = int(self.ay.split("-")[0])
        return date(ay_start, 3, 31)


# ─── Income Heads ────────────────────────────────────────────────────────────

@dataclass
class IncomeHeads:
    """All income heads before deductions."""
    salary_income: int = 0
    hp_income: int = 0
    os_income: int = 0
    cg_detail: Optional[CapitalGainsBreakdown] = None
    cg_income: int = 0        # Legacy: total CG (sum of detail)
    bp_income: int = 0

    @property
    def gross_total_income(self) -> int:
        cg = self.cg_detail.cg_total if self.cg_detail else self.cg_income
        return self.salary_income + self.hp_income + self.os_income + cg + self.bp_income
    
    @property
    def normal_income(self) -> int:
        """Income taxed at slab rates (excludes CG special rate income)."""
        if not self.cg_detail:
            return self.salary_income + self.hp_income + self.os_income + self.bp_income
        
        cg_at_slab = (
            self.cg_detail.stcg_applicable_rate +
            self.cg_detail.ltcg_112_proviso_credit
        )
        return self.salary_income + self.hp_income + self.os_income + cg_at_slab + self.bp_income


# ─── Tax Engine ───────────────────────────────────────────────────────────────

class TaxEngine:
    """OUR OWNED tax computation engine.

    Pure functions — no side effects, no DB calls.
    Takes domain schedules + context → returns TaxBreakdown.

    CRITICAL: This does NOT call OpenTax at runtime.
    All computation is done from scratch using our own rules.
    """

    def __init__(self):
        self.interest_engine = Interest234Engine()

    # ── Main entry ────────────────────────────────────────────────────────────

    def compute(
        self,
        schedules: List[Schedule],
        regime: str,
        ay: str,
        dob: Optional[date] = None,
        filing_id: Optional[UUID] = None,
        client_id: Optional[UUID] = None,
        pan: str = "",
        filing_date: Optional[date] = None,
        due_date: Optional[date] = None,
    ) -> "TaxEngineResult":
        """Compute tax for a complete filing.

        Args:
            schedules: List of domain schedules from the filing.
            regime: "old" or "new" — primary regime for this computation.
            ay: Assessment year e.g. "2026-27".
            dob: Date of birth for age-based slabs/surcharge.
            filing_id: Filing UUID (for result metadata).
            client_id: Client UUID (for result metadata).
            pan: PAN number (for result metadata).
            filing_date: Date of filing (for 234A/234F interest).
            due_date: Due date for filing (defaults to Jul 31 AY).

        Returns:
            TaxEngineResult with breakdown for both regimes.
        """
        rules = get_rules(ay)
        ctx = ComputationContext(
            filing_id=filing_id or UUID("00000000-0000-0000-0000-000000000000"),
            client_id=client_id or UUID("00000000-0000-0000-0000-000000000001"),
            pan=pan,
            ay=ay,
            regime=regime,
            dob=dob,
        )

        # Extract schedules
        salary_sched = _first_of(schedules, ScheduleSalary)
        hp_sched = _first_of(schedules, ScheduleHP)
        os_sched = _first_of(schedules, ScheduleOS)
        cg_sched = _first_of(schedules, ScheduleCG)
        via_sched = _first_of(schedules, ScheduleVIA)
        tds_sched = _first_of(schedules, ScheduleTDS)
        it_sched = _first_of(schedules, ScheduleIT)

        # Compute income heads
        age = ctx.age()
        income_heads = IncomeHeads()
        steps: List[ComputationStep] = []

        if salary_sched:
            income_heads.salary_income, sal_steps = self._compute_salary(
                salary_sched, regime, rules, age
            )
            steps.extend(sal_steps)

        if hp_sched:
            income_heads.hp_income, hp_steps = self._compute_hp(hp_sched, rules)
            steps.extend(hp_steps)

        if os_sched:
            income_heads.os_income, os_steps = self._compute_other_sources(os_sched)
            steps.extend(os_steps)

        if cg_sched:
            cg_detail, cg_steps = self._compute_capital_gains(cg_sched, rules)
            income_heads.cg_detail = cg_detail
            income_heads.cg_income = cg_detail.cg_total
            steps.extend(cg_steps)

        # Gross total income
        gti = income_heads.gross_total_income
        steps.append(_step(
            "gross_total_income", "Gross Total Income",
            "", _inr(gti),
            "Sum of all income heads"
        ))

        # CG special rate tax (adds to tax_before_rebate)
        cg_special_tax = income_heads.cg_detail.cg_tax if income_heads.cg_detail else 0

        # Deductions (old regime only)
        total_deductions = 0
        if via_sched and regime == "old":
            total_deductions, ded_steps = self._compute_deductions(via_sched, rules, age)
            steps.extend(ded_steps)

        # Total income
        total_income = max(0, gti - total_deductions)
        steps.append(_step(
            "total_income", "Total Income",
            _inr(gti) + " - " + _inr(total_deductions), _inr(total_income),
            f"GTI {f'- deductions {_inr(total_deductions)}' if regime == 'old' else '(no deductions in new regime)'}"
        ))

        # Tax before rebate (slab tax + CG special rate tax)
        slabs = self._get_slabs(regime, age, rules)
        slab_breakdown, slab_tax = compute_slab_tax(total_income, slabs)
        tax_before_rebate = slab_tax + cg_special_tax
        steps.append(_step(
            "slab_tax", "Tax on Total Income",
            f"Slab: {_inr(slab_tax)} + CG special: {_inr(cg_special_tax)}", _inr(tax_before_rebate),
            f"Slab rates for {regime} regime, age group {age_group_label(age)}"
        ))

        # BEL (for surcharge calculation): normal income + CG special rate income
        # Section 112A/112 LTCG with 12.5%/20% rates counts toward BEL for surcharge
        cg_ltcg_for_bel = income_heads.cg_detail.ltcg_total if income_heads.cg_detail else 0
        cg_stcg_for_bel = income_heads.cg_detail.stcg_total if income_heads.cg_detail else 0
        bel = total_income + cg_ltcg_for_bel  # LTCG (special rate) counts in BEL
        bel_shortfall = max(0, rules.deductions.bel_threshold - bel)
        
        # Rebate 87A (on slab tax, NOT on CG special rate tax)
        rebate = self._compute_rebate_87a(slab_tax, total_income, regime, rules)
        tax_after_rebate = max(0, tax_before_rebate - rebate)
        if rebate > 0:
            steps.append(_step(
                "rebate_87a", "Rebate u/s 87A",
                f"{_inr(tax_before_rebate)} - {_inr(rebate)}", _inr(tax_after_rebate),
                f"Rebate 87A: {f'income ≤ {_inr(rules.rebate_87a_new_max_income)} → full rebate' if regime == 'new' else f'income ≤ {_inr(rules.rebate_87a_old_max_income)} → rebate ≤ {_inr(rules.rebate_87a_old_max_tax)}'}"
            ))

        # Surcharge (uses BEL + CG LTCG for threshold)
        surcharge_amount, surcharge_rate = self._compute_surcharge(
            tax_after_rebate=tax_after_rebate,
            bel=bel,
            regime=regime,
            rules=rules,
            cg_ltcg=cg_ltcg_for_bel,
        )
        if surcharge_amount > 0:
            steps.append(_step(
                "surcharge", "Surcharge",
                _inr(tax_after_rebate), _inr(surcharge_amount),
                f"Surcharge {surcharge_rate*100:.0f}% applied (BEL: {_inr(bel)}, CG LTCG: {_inr(cg_ltcg_for_bel)})"
            ))

        # Cess
        cess = int(Decimal(tax_after_rebate + surcharge_amount) * rules.cess_rate)
        steps.append(_step(
            "cess", "Health & Education Cess",
            f"({_inr(tax_after_rebate)} + {_inr(surcharge_amount)}) × 4%", _inr(cess),
            "4% cess on (tax after rebate + surcharge)"
        ))

        # Total tax liability
        total_tax_liability = tax_after_rebate + surcharge_amount + cess
        steps.append(_step(
            "total_tax_liability", "Total Tax Liability",
            f"{_inr(tax_after_rebate)} + {_inr(surcharge_amount)} + {_inr(cess)}", _inr(total_tax_liability),
            "Tax + surcharge + cess"
        ))

        # Tax credits
        total_tds = tds_sched.total_tds_credit() if tds_sched else 0
        total_tcs = 0    # Phase 3
        advance_tax = int(it_sched.total_advance_tax()) if it_sched else 0
        sat = int(it_sched.total_self_assessment_tax()) if it_sched else 0
        total_taxes_paid = total_tds + total_tcs + advance_tax + sat

        tax_payable = max(0, total_tax_liability - total_taxes_paid)
        refund = max(0, total_taxes_paid - total_tax_liability)

        steps.append(_step(
            "tax_credits", "Tax Credits",
            f"TDS {_inr(total_tds)} + Advance Tax {_inr(advance_tax)}", f"TDS: {_inr(total_tds)}",
            f"Total taxes paid: {_inr(total_taxes_paid)}"
        ))
        steps.append(_step(
            "tax_payable", "Tax Payable / (Refund)",
            f"{_inr(total_tax_liability)} - {_inr(total_taxes_paid)}",
            _inr(tax_payable) if tax_payable > 0 else f"Refund: {_inr(refund)}",
            f"Net tax: {_inr(tax_payable)}" if tax_payable > 0 else f"Refund: {_inr(refund)}"
        ))

        # Interest 234A/B/C
        interest_result = self._compute_interest(
            net_tax_liability=total_tax_liability,
            tds=total_tds,
            tcs=total_tcs,
            it_sched=it_sched,
            age=age,
            ay=ay,
            filing_date=filing_date,
            due_date=due_date,
            total_income=total_income,
        )

        net_tax_payable = tax_payable + interest_result.total
        net_refund = refund - interest_result.total if refund > interest_result.total else 0

        # Build TaxBreakdown for this regime
        breakdown = TaxBreakdown(
            gross_total_income=gti,
            total_deductions=total_deductions,
            total_income=total_income,
            normal_income=income_heads.normal_income,
            normal_tax=slab_tax,
            cg_total=income_heads.cg_detail.cg_total if income_heads.cg_detail else 0,
            cg_stcg=income_heads.cg_detail.stcg_total if income_heads.cg_detail else 0,
            cg_ltcg=income_heads.cg_detail.ltcg_total if income_heads.cg_detail else 0,
            cg_special_rate_tax=cg_special_tax,
            cg_rate_buckets=income_heads.cg_detail.rate_buckets if income_heads.cg_detail else [],
            tax_before_rebate=tax_before_rebate,
            rebate_87a=rebate,
            surcharge=surcharge_amount,
            cess=cess,
            total_tax_liability=total_tax_liability,
            tds=total_tds,
            tcs=total_tcs,
            advance_tax=advance_tax,
            tax_payable=net_tax_payable,
            refund=net_refund if net_refund > 0 else 0,
            interest_234a=interest_result.interest_234a,
            interest_234b=interest_result.interest_234b,
            interest_234c=interest_result.interest_234c,
            late_fee_234f=interest_result.late_fee_234f,
            bel=bel,
            bel_shortfall=bel_shortfall,
        )

        # Also compute other regime for comparison
        other_regime = "old" if regime == "new" else "new"
        other_breakdown = self._compute_single_regime(
            income_heads, gti, rules, regime, age, total_tds, total_tcs,
            advance_tax, sat, ay, dob, filing_date, due_date,
        )

        # Build slab breakdown
        slab_entries = [
            SlabBreakdown(
                from_amount=lower,
                to_amount=upper,
                rate=float(rate),
                taxable_amount=min(total_income, (upper or total_income) if upper else total_income) - lower,
                tax=stax,
            )
            for lower, upper, rate, stax in slab_breakdown
        ]

        result = TaxEngineResult(
            regime=regime,
            breakdown=breakdown,
            other_regime=other_regime,
            other_regime_breakdown=other_breakdown,
            slab_breakdown=slab_entries,
            explanation=steps,
            rule_version=f"owned-engine-v1.0-{ay}",
        )
        return result

    def _compute_single_regime(
        self,
        income_heads: IncomeHeads,
        gti: int,
        rules: RuleVersion,
        regime: str,
        age: int,
        total_tds: int,
        total_tcs: int,
        advance_tax: int,
        sat: int,
        ay: str,
        dob: Optional[date],
        filing_date: Optional[date],
        due_date: Optional[date],
    ) -> TaxBreakdown:
        """Compute tax breakdown for a single regime (used for comparison)."""
        # CG special rate tax
        cg_special_tax = income_heads.cg_detail.cg_tax if income_heads.cg_detail else 0
        cg_ltcg_for_bel = income_heads.cg_detail.ltcg_total if income_heads.cg_detail else 0
        
        # Deductions
        total_deductions = 0
        if regime == "old":
            # Approximate — full ScheduleVIA deductions
            total_deductions = 0   # Will be refined with actual via_sched

        total_income = max(0, gti - total_deductions)

        slabs = self._get_slabs(regime, age, rules)
        slab_breakdown_list, slab_tax = compute_slab_tax(total_income, slabs)
        tax_before_rebate = slab_tax + cg_special_tax

        # BEL for surcharge
        bel = total_income + cg_ltcg_for_bel
        bel_shortfall = max(0, rules.deductions.bel_threshold - bel)

        # Rebate 87A (on slab tax only)
        rebate = self._compute_rebate_87a(slab_tax, total_income, regime, rules)
        tax_after_rebate = max(0, tax_before_rebate - rebate)

        surcharge_amount, _ = self._compute_surcharge(
            tax_after_rebate=tax_after_rebate,
            bel=bel,
            regime=regime,
            rules=rules,
            cg_ltcg=cg_ltcg_for_bel,
        )
        cess = int(Decimal(tax_after_rebate + surcharge_amount) * rules.cess_rate)
        total_tax_liability = tax_after_rebate + surcharge_amount + cess

        total_taxes_paid = total_tds + total_tcs + advance_tax + sat
        tax_payable = max(0, total_tax_liability - total_taxes_paid)
        refund = max(0, total_taxes_paid - total_tax_liability)

        return TaxBreakdown(
            gross_total_income=gti,
            total_deductions=total_deductions,
            total_income=total_income,
            normal_income=income_heads.normal_income,
            normal_tax=slab_tax,
            cg_total=income_heads.cg_detail.cg_total if income_heads.cg_detail else 0,
            cg_stcg=income_heads.cg_detail.stcg_total if income_heads.cg_detail else 0,
            cg_ltcg=income_heads.cg_detail.ltcg_total if income_heads.cg_detail else 0,
            cg_special_rate_tax=cg_special_tax,
            cg_rate_buckets=income_heads.cg_detail.rate_buckets if income_heads.cg_detail else [],
            tax_before_rebate=tax_before_rebate,
            rebate_87a=rebate,
            surcharge=surcharge_amount,
            cess=cess,
            total_tax_liability=total_tax_liability,
            tds=total_tds,
            tcs=total_tcs,
            advance_tax=advance_tax,
            tax_payable=tax_payable,
            refund=refund,
        )

    # ── Income head methods ───────────────────────────────────────────────────

    def _compute_salary(
        self,
        schedule: ScheduleSalary,
        regime: str,
        rules: RuleVersion,
        age: int,
    ) -> tuple[int, List[ComputationStep]]:
        """Compute salary income after standard deduction and allowances."""
        steps: List[ComputationStep] = []
        total_gross = 0
        total_exempt = 0
        total_prof_tax = 0

        for emp in schedule.employers:
            gross = int(emp.gross_salary.to_rupees())
            exempt = int(emp.allowances_exempt.to_rupees())
            prof_tax = int(emp.professional_tax.to_rupees())
            total_gross += gross
            total_exempt += exempt
            total_prof_tax += prof_tax

        # Standard deduction
        std_ded = rules.new_std_deduction if regime == "new" else rules.old_std_deduction
        std_ded_label = "new regime" if regime == "new" else "old regime"

        net_salary = max(0, total_gross - total_exempt - total_prof_tax - std_ded)

        steps.append(_step(
            "salary.gross", "Gross Salary",
            f"₹{total_gross:,}", _inr(total_gross),
            f"Total from {len(schedule.employers)} employer(s)"
        ))
        if total_exempt > 0:
            steps.append(_step(
                "salary.exempt", "Allowances Exempt",
                f"₹{total_exempt:,}", f"-₹{total_exempt:,}",
                "HRA, LTA, etc."
            ))
        if total_prof_tax > 0:
            steps.append(_step(
                "salary.prof_tax", "Professional Tax",
                f"₹{total_prof_tax:,}", f"-₹{total_prof_tax:,}",
                "Paid during FY"
            ))
        steps.append(_step(
            "salary.std_ded", f"Standard Deduction ({std_ded_label})",
            f"₹{std_ded:,}", f"-₹{std_ded:,}",
            f"₹75,000 (new) / ₹50,000 (old)"
        ))
        steps.append(_step(
            "salary.net", "Net Salary Income",
            f"{_inr(total_gross)} - {_inr(total_exempt)} - {_inr(total_prof_tax)} - {_inr(std_ded)}",
            _inr(net_salary),
            f"Salary income after deductions"
        ))

        return net_salary, steps

    def _compute_hp(
        self,
        schedule: ScheduleHP,
        rules,
    ) -> tuple[int, List[ComputationStep]]:
        """Compute house property income (Section 22-27)."""
        steps: List[ComputationStep] = []
        total = 0

        for prop in schedule.properties:
            rent = int(prop.annual_rent.to_rupees())
            municipal = int(prop.municipal_taxes.to_rupees())
            interest = int(prop.interest_paid.to_rupees())
            share = float(prop.ownership_share)

            nav = rent - municipal
            std_ded = int(nav * Decimal("0.30")) if nav > 0 else 0
            income = (nav - std_ded - interest) * Decimal(str(share))

            total += int(income)

        total = max(total, rules.deductions.hp_loss_setoff)

        steps.append(_step(
            "hp.income", "Income from House Property",
            "", _inr(total),
            f"NAV - 30% - interest (loss capped at ₹{rules.deductions.hp_loss_setoff:,})"
        ))

        return total, steps

    def _compute_other_sources(
        self,
        schedule: ScheduleOS,
    ) -> tuple[int, List[ComputationStep]]:
        """Compute other sources income (interest + dividend + other)."""
        steps: List[ComputationStep] = []
        interest = sum(int(d.interest_amount.to_rupees()) for d in schedule.interest_income)
        dividend = sum(int(d.dividend_amount.to_rupees()) for d in schedule.dividend_income)
        other = int(schedule.other_income.to_rupees())
        total = interest + dividend + other

        if interest > 0:
            steps.append(_step("os.interest", "Interest Income", f"₹{interest:,}", f"₹{interest:,}", "Bank FDs, bonds"))
        if dividend > 0:
            steps.append(_step("os.dividend", "Dividend Income", f"₹{dividend:,}", f"₹{dividend:,}", "Mutual funds, shares"))
        if other > 0:
            steps.append(_step("os.other", "Other Income", f"₹{other:,}", f"₹{other:,}", "Miscellaneous"))

        steps.append(_step("os.total", "Total Other Sources Income", "", _inr(total), "Interest + Dividend + Other"))

        return total, steps

    def _compute_capital_gains(
        self,
        schedule: ScheduleCG,
        rules,
    ) -> tuple[CapitalGainsBreakdown, List[ComputationStep]]:
        """Compute capital gains with special rate tax computation.
        
        CG is taxed at SPECIAL RATES, separate from slab income:
        - 111A: STCG listed equity @ 15% (STT paid)
        - 112A: LTCG listed equity @ 12.5% (>₹1.25L exempt)
        - 112: LTCG other assets @ 20% (with/without indexation)
        - 115BB: Lottery/winnings @ 30%
        - 115BBH: VDA/Crypto @ 30%
        - 115BBE: Unexplained @ 60%
        - STCG at slab rates (non-listed equity, no STT)
        
        Returns:
            CapitalGainsBreakdown with all rate buckets populated
        """
        steps: List[ComputationStep] = []
        
        # Get CG totals from ScheduleCG
        totals = schedule.get_totals()
        stcg = totals["short_term"]
        ltcg = totals["long_term"]
        special_rate_income = totals["special_rate"]
        total_cg = totals["total"]
        
        # Compute rate buckets (with ₹1.25L exemption for 112A)
        rate_buckets = schedule.compute_rate_buckets(
            exempt_threshold=CGExemptionThreshold.SECTION_112A_EXEMPT
        )
        
        # CG tax at special rates
        cg_special_tax, _ = schedule.compute_special_rate_tax(
            exempt_threshold=CGExemptionThreshold.SECTION_112A_EXEMPT
        )
        
        # Get detailed bucket-level categorisation
        bucket_map = schedule.categorize_by_rate_bucket()
        
        # Build breakdown from actual bucket map
        breakdown = CapitalGainsBreakdown(
            stcg_total=stcg,
            stcg_111a_15_pct=bucket_map["stcg_111a_15_pct"],
            stcg_111a_20_pct=bucket_map["stcg_20_pct"],
            stcg_applicable_rate=bucket_map["stcg_applicable_rate"],
            ltcg_total=ltcg,
            ltcg_112a_12_5_pct=max(0, bucket_map["ltcg_112a_12_5_pct"] - CGExemptionThreshold.SECTION_112A_EXEMPT),
            ltcg_112a_exempt=min(bucket_map["ltcg_112a_12_5_pct"], CGExemptionThreshold.SECTION_112A_EXEMPT),
            ltcg_112_20_pct=bucket_map["ltcg_20_pct"],
            ltcg_112_12_5_pct=bucket_map["ltcg_12_5_pct_other"],
            lottery_30_pct=bucket_map["lottery_30_pct"],
            online_gaming_30_pct=bucket_map["online_gaming_30_pct"],
            vda_30_pct=bucket_map["vda_30_pct"],
            unexplained_60_pct=bucket_map["unexplained_60_pct"],
            cg_total=total_cg,
            cg_tax=cg_special_tax,
            rate_buckets=[
                CGRateBucketBreakdown(
                    section=b.section,
                    description=b.description,
                    rate=float(b.rate),
                    income=b.income,
                    taxable_income=b.taxable_income,
                    tax=b.tax,
                )
                for b in rate_buckets if b.income > 0
            ],
        )
        
        # Add section-specific steps
        for bucket in rate_buckets:
            if bucket.income > 0:
                steps.append(_step(
                    f"cg.{bucket.section.lower()}",
                    f"CG {bucket.section} @ {bucket.rate*100:.1f}%",
                    _inr(bucket.income) + (f" (exempt ₹{bucket.income - bucket.taxable_income:,})" if bucket.taxable_income < bucket.income else ""),
                    f"Tax: {_inr(bucket.tax)}",
                    bucket.description
                ))
        
        # Add aggregate steps
        if stcg > 0:
            steps.append(_step("cg.stcg", "Short-Term Capital Gains", "", _inr(stcg), "All short-term gains"))
        if ltcg > 0:
            steps.append(_step("cg.ltcg", "Long-Term Capital Gains", "", _inr(ltcg), "All long-term gains"))
        if special_rate_income > 0:
            steps.append(_step("cg.special", "Special Rate Income (Lottery/VDA/Other)", "", _inr(special_rate_income), "Taxed at 30%/60%"))
        if cg_special_tax > 0:
            steps.append(_step("cg.tax", "Capital Gains Tax (Special Rates)", "", _inr(cg_special_tax), "Total CG tax at special rates"))
        
        if total_cg > 0:
            steps.append(_step("cg.total", "Total Capital Gains",
                f"STCG {_inr(stcg)} + LTCG {_inr(ltcg)} + Special {_inr(special_rate_income)}",
                _inr(total_cg),
                f"CG tax: {_inr(cg_special_tax)} (at special rates)"
            ))
        else:
            steps.append(_step("cg.total", "Total Capital Gains", "", "₹0", "No capital gains reported"))

        return breakdown, steps

    def _compute_deductions(
        self,
        schedule: ScheduleVIA,
        rules,
        age: int,
    ) -> tuple[int, List[ComputationStep]]:
        """Compute Chapter VI-A deductions (old regime only)."""
        steps: List[ComputationStep] = []
        total = 0

        ded = rules.deductions

        # 80C (capped at ₹1.5L)
        s80c = int(schedule.section_80c.total())
        s80c_allowed = min(s80c, ded.section_80c)
        total += s80c_allowed
        if s80c > 0:
            steps.append(_step("80c", "Section 80C",
                f"₹{s80c:,}", f"₹{s80c_allowed:,}",
                f"Max ₹{ded.section_80c:,}"))

        # 80CCD(1B) additional NPS ₹50K
        s80ccd1b = int(min(schedule.section_80ccd_1b.to_rupees(), Decimal(ded.section_80ccd_1b)))
        total += s80ccd1b
        if s80ccd1b > 0:
            steps.append(_step("80ccd1b", "Section 80CCD(1B)",
                f"₹{s80ccd1b:,}", f"₹{s80ccd1b:,}",
                f"Additional NPS, max ₹{ded.section_80ccd_1b:,}"))

        # 80CCD(2) employer NPS (no limit)
        s80ccd2 = int(schedule.section_80ccd_2.to_rupees())
        total += s80ccd2
        if s80ccd2 > 0:
            steps.append(_step("80ccd2", "Section 80CCD(2)",
                f"₹{s80ccd2:,}", f"₹{s80ccd2:,}",
                "Employer NPS, no limit"))

        # 80D health insurance
        if schedule.section_80d:
            s80d = int(schedule.section_80d.total())
            total += s80d
            if s80d > 0:
                steps.append(_step("80d", "Section 80D",
                    f"₹{s80d:,}", f"₹{s80d:,}",
                    f"Health insurance premium"))

        # 80E education loan (no limit)
        s80e = int(schedule.section_80e.to_rupees())
        total += s80e
        if s80e > 0:
            steps.append(_step("80e", "Section 80E",
                f"₹{s80e:,}", f"₹{s80e:,}",
                "Education loan interest, no limit"))

        # 80G donations
        s80g = int(schedule.section_80g.to_rupees())
        total += s80g
        if s80g > 0:
            steps.append(_step("80g", "Section 80G",
                f"₹{s80g:,}", f"₹{s80g:,}",
                "Donations (50%/100% eligible)"))

        # 80TTA/80TTB
        s80tta = int(min(schedule.section_80tta.to_rupees(), Decimal(ded.section_80tta)))
        total += s80tta
        if s80tta > 0:
            steps.append(_step("80tta", "Section 80TTA",
                f"₹{s80tta:,}", f"₹{s80tta:,}",
                f"Savings interest, max ₹{ded.section_80tta:,}"))

        s80ttb = int(min(schedule.section_80ttb.to_rupees(), Decimal(ded.section_80ttb)))
        total += s80ttb
        if s80ttb > 0:
            steps.append(_step("80ttb", "Section 80TTB",
                f"₹{s80ttb:,}", f"₹{s80ttb:,}",
                f"Senior savings interest, max ₹{ded.section_80ttb:,}"))

        steps.append(_step("via.total", "Total Chapter VI-A Deductions",
            "", _inr(total), "Old regime only"))

        return total, steps

    # ── Tax computation ───────────────────────────────────────────────────────

    def _get_slabs(self, regime: str, age: int, rules: RuleVersion):
        """Get slab definitions for regime and age group."""
        if regime == "new":
            return rules.new_regime_slabs
        if age < 60:
            return rules.old_regime_slabs_under_60
        elif age < 80:
            return rules.old_regime_slabs_60_to_79
        else:
            return rules.old_regime_slabs_80_plus

    def _compute_rebate_87a(
        self,
        tax_before_rebate: int,
        total_income: int,
        regime: str,
        rules: RuleVersion,
    ) -> int:
        """Rebate 87A — full rebate if income ≤ threshold, else partial."""
        if regime == "new":
            if total_income <= rules.rebate_87a_new_max_income:
                return min(tax_before_rebate, rules.rebate_87a_new_max_tax)
        else:
            if total_income <= rules.rebate_87a_old_max_income:
                return min(tax_before_rebate, rules.rebate_87a_old_max_tax)
        return 0

    def _compute_surcharge(
        self,
        tax_after_rebate: int,
        bel: int,
        regime: str,
        rules: RuleVersion,
        cg_ltcg: int = 0,
    ) -> tuple[int, Decimal]:
        """Surcharge with marginal relief calculation.
        
        Uses BEL (Basic Exemption Limit) + CG LTCG for surcharge threshold check.
        BEL shortfall means CG LTCG pushes total_income above threshold even if
        normal income is below.
        """
        # BEL thresholds for surcharge (not the rebate thresholds)
        thresholds = (
            rules.surcharge_new_thresholds if regime == "new"
            else rules.surcharge_thresholds
        )
        surcharge = Decimal("0")
        applied_rate = Decimal("0")

        # Use BEL + CG LTCG for surcharge calculation
        surcharge_base = bel + cg_ltcg
        
        for threshold, rate in thresholds:
            if surcharge_base > threshold:
                # Calculate surcharge with marginal relief
                base_amount = surcharge_base - threshold
                surcharge += int(Decimal(base_amount) * rate)
                applied_rate = rate

        # Marginal relief: surcharge cannot exceed (tax × rate)
        max_surcharge = int(Decimal(tax_after_rebate) * applied_rate)
        surcharge = min(surcharge, max_surcharge)

        return int(surcharge), applied_rate

    def _compute_interest(
        self,
        net_tax_liability: int,
        tds: int,
        tcs: int,
        it_sched: Optional[ScheduleIT],
        age: int,
        ay: str,
        filing_date: Optional[date],
        due_date: Optional[date],
        total_income: int,
    ) -> InterestResult:
        """Compute interest u/s 234A/B/C using our own Interest234Engine."""
        payments: List[TaxPayment] = []
        if it_sched:
            for adv in it_sched.advance_tax:
                from datetime import datetime
                pay_date = datetime.strptime(adv.date_of_deposit, "%d/%m/%Y").date()
                payments.append(TaxPayment(int(adv.amount.to_rupees()), pay_date))
            for sat in it_sched.self_assessment_tax:
                from datetime import datetime
                pay_date = datetime.strptime(sat.date_of_deposit, "%d/%m/%Y").date()
                payments.append(TaxPayment(int(sat.amount.to_rupees()), pay_date))

        return self.interest_engine.compute(
            net_tax_liability=net_tax_liability,
            tds=tds,
            tcs=tcs,
            tax_payments=payments,
            age=age,
            assessment_year=ay,
            filing_date=filing_date,
            due_date=due_date,
            total_income=total_income,
            is_resident=True,
        )


# ─── Result ──────────────────────────────────────────────────────────────────

@dataclass
class TaxEngineResult:
    """Result of TaxEngine.compute() — breakdown for both regimes."""
    regime: str
    breakdown: TaxBreakdown
    other_regime: str
    other_regime_breakdown: TaxBreakdown
    slab_breakdown: List[SlabBreakdown]
    explanation: List[ComputationStep]
    rule_version: str

    def to_payload(self) -> dict:
        """Serialize breakdown to dict for ComputedReturn.payload."""
        b = self.breakdown
        return {
            "regime": self.regime,
            "gross_total_income": b.gross_total_income,
            "total_deductions": b.total_deductions,
            "total_income": b.total_income,
            "tax_before_rebate": b.tax_before_rebate,
            "rebate_87a": b.rebate_87a,
            "surcharge": b.surcharge,
            "cess": b.cess,
            "total_tax_liability": b.total_tax_liability,
            "tds": b.tds,
            "tcs": b.tcs,
            "advance_tax": b.advance_tax,
            "tax_payable": b.tax_payable,
            "refund": b.refund,
            "interest_234a": b.interest_234a,
            "interest_234b": b.interest_234b,
            "interest_234c": b.interest_234c,
            "late_fee_234f": b.late_fee_234f,
        }


# ─── Helpers ─────────────────────────────────────────────────────────────────

def _first_of(schedules: List[Schedule], typ: type) -> Optional[Schedule]:
    for s in schedules:
        if isinstance(s, typ):
            return s
    return None


def _step(step_id: str, label: str, input_val: str, output_val: str, rule: str) -> ComputationStep:
    return ComputationStep(
        step=step_id,
        description=label,
        input_value=input_val,
        output_value=output_val,
        rule_applied=rule,
    )


def _inr(amount: int) -> str:
    """Format as Indian number system string."""
    return f"₹{amount:,}"


def age_group_label(age: int) -> str:
    if age < 60:
        return "<60"
    elif age < 80:
        return "60-79"
    else:
        return "80+"
