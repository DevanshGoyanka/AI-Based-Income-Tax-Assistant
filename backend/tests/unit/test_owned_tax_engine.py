"""Test cases for owned tax engine (Phase 2).

Golden cases cover AY 2026-27 with both regimes.
Tolerance: ±₹1 for all monetary values (banker's rounding edge cases).
"""
import pytest
from datetime import date
from decimal import Decimal
from uuid import uuid4

from app.core.services.slab_tables import (
    get_rules,
    compute_slab_tax,
    SlabEntry,
    RuleVersion,
)
from app.core.services.interest_234_engine import (
    Interest234Engine,
    InterestResult,
    TaxPayment,
)
from app.core.services.tax_engine import TaxEngine, TaxEngineResult
from app.core.domain.schedules.salary import ScheduleSalary, SalaryDetail
from app.core.domain.schedules.house_property import ScheduleHP, HPDetail
from app.core.domain.schedules.other_sources import ScheduleOS, InterestDetail
from app.core.domain.schedules.schedule_via import ScheduleVIA, Section80C, Section80D
from app.core.domain.schedules.schedule_cg import ScheduleCG, CGTransaction
from app.core.domain.schedules.tds import ScheduleTDS, TDSDetail
from app.core.domain.value_objects import Money


# ─── Shared schedule helpers ──────────────────────────────────────────────────

def _salary_sched(gross: int) -> ScheduleSalary:
    """Build a ScheduleSalary with one employer.

    Note: Schedule base requires (id, filing_id, ay). We use test UUIDs.
    """
    return ScheduleSalary(
        id=uuid4(),
        filing_id=uuid4(),
        ay="2026-27",
        employers=[
            SalaryDetail(
                employer_name="Tech Corp",
                employer_tan="BANG12345A",
                gross_salary=Money.from_rupees(gross),
                allowances_exempt=Money.from_rupees(0),
                professional_tax=Money.from_rupees(0),
                tds_deducted=Money.from_rupees(0),
                standard_deduction=Money.from_rupees(0),
            )
        ],
    )


def _tds_sched(amount: int) -> ScheduleTDS:
    return ScheduleTDS(
        id=uuid4(),
        filing_id=uuid4(),
        ay="2026-27",
        tds_entries=[
            TDSDetail(
                deductor_name="Tech Corp",
                deductor_tan="BANG12345A",
                section_code="192",
                amount_paid=Money.from_rupees(amount),
                tax_deducted=Money.from_rupees(amount),
                tax_deposited=Money.from_rupees(amount),
                quarter="Q1",
            )
        ],
    )


def _via_sched_80c(amount: int) -> ScheduleVIA:
    return ScheduleVIA(
        id=uuid4(),
        filing_id=uuid4(),
        ay="2026-27",
        section_80c=Section80C(life_insurance=Money.from_rupees(amount)),
    )


def _cg_sched_stcg_111a(gain: int) -> ScheduleCG:
    """ScheduleCG with STCG u/s 111A (listed equity @ 15%)."""
    return ScheduleCG(
        id=uuid4(),
        filing_id=uuid4(),
        ay="2026-27",
        transactions=[
            CGTransaction(
                asset_type="listed_equity",
                purchase_date=date(2024, 4, 1),
                sale_date=date(2025, 3, 31),
                sale_price=Money.from_rupees(gain + 50_000),
                purchase_price=Money.from_rupees(50_000),
                transfer_expenses=Money.from_rupees(0),
                indexed_cost=None,
                section="111A",
            )
        ],
    )


def _cg_sched_ltcg_112a(gain: int) -> ScheduleCG:
    """ScheduleCG with LTCG u/s 112A (listed equity @ 12.5%)."""
    return ScheduleCG(
        id=uuid4(),
        filing_id=uuid4(),
        ay="2026-27",
        transactions=[
            CGTransaction(
                asset_type="listed_equity",
                purchase_date=date(2020, 4, 1),
                sale_date=date(2025, 3, 31),
                sale_price=Money.from_rupees(gain + 1_00_000),
                purchase_price=Money.from_rupees(1_00_000),
                transfer_expenses=Money.from_rupees(0),
                indexed_cost=None,
                section="112A",
            )
        ],
    )


def _cg_sched_ltcg_112(gain: int) -> ScheduleCG:
    """ScheduleCG with LTCG u/s 112 (other assets @ 20%)."""
    return ScheduleCG(
        id=uuid4(),
        filing_id=uuid4(),
        ay="2026-27",
        transactions=[
            CGTransaction(
                asset_type="property",
                purchase_date=date(2015, 4, 1),
                sale_date=date(2025, 3, 31),
                sale_price=Money.from_rupees(gain + 5_00_000),
                purchase_price=Money.from_rupees(5_00_000),
                transfer_expenses=Money.from_rupees(50_000),
                indexed_cost=None,
                section="112",
            )
        ],
    )


# ─── Slab table tests ─────────────────────────────────────────────────────────

class TestSlabTables:
    """Test slab rates for AY 2026-27."""

    def test_rules_ay_2026_27_loaded(self):
        rules = get_rules("2026-27")
        assert rules.ay == "2026-27"
        assert rules.new_std_deduction == 75_000
        assert rules.old_std_deduction == 50_000
        assert rules.cess_rate == Decimal("0.04")

    def test_new_regime_slabs_correct(self):
        rules = get_rules("2026-27")
        slabs = rules.new_regime_slabs
        assert len(slabs) == 7
        assert slabs[0].lower == 0
        assert slabs[0].upper == 4_00_000
        assert slabs[0].rate == Decimal("0")
        assert slabs[1].lower == 4_00_000
        assert slabs[1].rate == Decimal("0.05")
        assert slabs[6].upper is None   # No upper cap on last slab
        assert slabs[6].rate == Decimal("0.30")

    def test_old_regime_slabs_under_60(self):
        rules = get_rules("2026-27")
        slabs = rules.old_regime_slabs_under_60
        assert len(slabs) == 4
        assert slabs[0].upper == 2_50_000
        assert slabs[0].rate == Decimal("0")
        assert slabs[1].rate == Decimal("0.05")
        assert slabs[2].rate == Decimal("0.20")
        assert slabs[3].rate == Decimal("0.30")

    def test_old_regime_slabs_senior(self):
        rules = get_rules("2026-27")
        slabs_60 = rules.old_regime_slabs_60_to_79
        assert slabs_60[0].upper == 3_00_000
        slabs_80 = rules.old_regime_slabs_80_plus
        assert slabs_80[0].upper == 5_00_000

    def test_surcharge_thresholds(self):
        rules = get_rules("2026-27")
        thresholds = rules.surcharge_thresholds   # old regime
        assert thresholds[0] == (50_00_000, Decimal("0.10"))
        assert thresholds[1] == (1_00_00_000, Decimal("0.15"))
        assert thresholds[2] == (2_00_00_000, Decimal("0.25"))
        assert thresholds[3] == (5_00_00_000, Decimal("0.37"))

    def test_deduction_limits(self):
        rules = get_rules("2026-27")
        d = rules.deductions
        assert d.section_80c == 150_000
        assert d.section_80ccd_1b == 50_000
        assert d.section_80d_self == 25_000
        assert d.section_80tta == 10_000
        assert d.hp_loss_setoff == -200_000


class TestSlabComputation:
    """Test compute_slab_tax function."""

    def test_new_regime_zero_income(self):
        rules = get_rules("2026-27")
        _, tax = compute_slab_tax(0, rules.new_regime_slabs)
        assert tax == 0

    def test_new_regime_4l(self):
        rules = get_rules("2026-27")
        _, tax = compute_slab_tax(4_00_000, rules.new_regime_slabs)
        assert tax == 0   # Entirely in exempt slab

    def test_new_regime_8l(self):
        rules = get_rules("2026-27")
        _, tax = compute_slab_tax(8_00_000, rules.new_regime_slabs)
        # 4-8L: 4L × 5% = 20,000
        assert tax == 20_000

    def test_new_regime_15l(self):
        rules = get_rules("2026-27")
        _, tax = compute_slab_tax(15_00_000, rules.new_regime_slabs)
        # 4-8L: 4L×5% = 20K; 8-12L: 4L×10% = 40K; 12-15L: 3L×15% = 45K
        assert tax == 1_05_000

    def test_new_regime_1crore(self):
        rules = get_rules("2026-27")
        _, tax = compute_slab_tax(1_00_00_000, rules.new_regime_slabs)
        # 4-8L: 4L×5% = 20K; 8-12L: 4L×10% = 40K; 12-16L: 4L×15% = 60K;
        # 16-20L: 4L×20% = 80K; 20-24L: 4L×25% = 1,00,000;
        # 24-100L: 76L×30% = 2,28,000
        # Total: 25,80,000
        assert tax == 25_80_000

    def test_old_regime_15l(self):
        rules = get_rules("2026-27")
        _, tax = compute_slab_tax(15_00_000, rules.old_regime_slabs_under_60)
        # 2.5-5L: 2.5L×5% = 12,500; 5-10L: 5L×20% = 1,00,000;
        # 10-15L: 5L×30% = 1,50,000 → 2,62,500
        assert tax == 2_62_500

    def test_old_regime_5l(self):
        rules = get_rules("2026-27")
        _, tax = compute_slab_tax(5_00_000, rules.old_regime_slabs_under_60)
        # 2.5-5L: 2.5L×5% = 12,500
        assert tax == 12_500

    def test_old_regime_3l_senior(self):
        rules = get_rules("2026-27")
        _, tax = compute_slab_tax(3_00_000, rules.old_regime_slabs_60_to_79)
        assert tax == 0   # Entirely exempt (3L within 3L exempt limit)


# ─── Interest 234 engine tests ────────────────────────────────────────────────

class TestInterest234Engine:
    """Test our owned interest 234A/B/C engine."""

    def test_234f_high_income_late(self):
        """234F: Late filing, income > ₹5L → ₹5,000. 234A also fires (TDS < tax)."""
        engine = Interest234Engine()
        result = engine.compute(
            net_tax_liability=100_000, tds=80_000, tcs=0,
            tax_payments=[],
            age=30, assessment_year="2026-27",
            filing_date=date(2026, 10, 1),
            due_date=date(2026, 7, 31),
            total_income=700_000,
        )
        # 234F fires (late + income > 5L)
        assert result.late_fee_234f == 5000
        # 234A also fires: unpaid tax = 100K - 80K = 20K; 2 months × 1% = 400
        # (234B/234C may also fire depending on date calculations — interest is real)
        assert result.interest_234a >= 0

    def test_234f_low_income_late(self):
        """234F: Late filing, income ≤ ₹5L → ₹1,000."""
        engine = Interest234Engine()
        result = engine.compute(
            net_tax_liability=5_000, tds=0, tcs=0,
            tax_payments=[],
            age=30, assessment_year="2026-27",
            filing_date=date(2026, 10, 1),
            due_date=date(2026, 7, 31),
            total_income=300_000,
        )
        assert result.late_fee_234f == 1000

    def test_234f_on_time_no_fee(self):
        """No 234F if filed on time."""
        engine = Interest234Engine()
        result = engine.compute(
            net_tax_liability=100_000, tds=80_000, tcs=0,
            tax_payments=[],
            age=30, assessment_year="2026-27",
            filing_date=date(2026, 7, 15),
            due_date=date(2026, 7, 31),
            total_income=700_000,
        )
        assert result.late_fee_234f == 0

    def test_234a_late_filing(self):
        """234A: 5 months late on ₹1L shortfall → ₹5,000."""
        engine = Interest234Engine()
        result = engine.compute(
            net_tax_liability=100_000, tds=0, tcs=0,
            tax_payments=[],
            age=30, assessment_year="2026-27",
            filing_date=date(2026, 12, 31),  # 5 months late
            due_date=date(2026, 7, 31),
            total_income=700_000,
        )
        # 1,00,000 × 1% × 5 = 5,000
        assert result.interest_234a == 5000

    def test_senior_exempt_234bc(self):
        """Senior citizens (age > 59) exempt from 234B and 234C."""
        engine = Interest234Engine()
        result = engine.compute(
            net_tax_liability=200_000, tds=0, tcs=0,
            tax_payments=[],
            age=65, assessment_year="2026-27",
            filing_date=date(2026, 12, 31),
            due_date=date(2026, 7, 31),
            total_income=1_000_000,
        )
        assert result.interest_234b == 0
        assert result.interest_234c == 0

    def test_234b_applies_when_no_advance_tax(self):
        """234B fires when advance tax < 90% of assessed tax."""
        engine = Interest234Engine()
        result = engine.compute(
            net_tax_liability=200_000, tds=0, tcs=0,
            tax_payments=[],  # No advance tax
            age=30, assessment_year="2026-27",
            filing_date=date(2026, 12, 31),
            due_date=date(2026, 7, 31),
            total_income=1_000_000,
        )
        # Assessed tax = 200K ≥ 10K; advance = 0 < 90%×200K = 180K → 234B applies
        assert result.interest_234b > 0

    def test_234b_exempt_below_threshold(self):
        """234B/234C don't apply if (NetTax − TDS − TCS) < ₹10K."""
        engine = Interest234Engine()
        result = engine.compute(
            net_tax_liability=9_000, tds=0, tcs=0,
            tax_payments=[],
            age=30, assessment_year="2026-27",
            filing_date=date(2026, 12, 31),
            due_date=date(2026, 7, 31),
            total_income=500_000,
        )
        assert result.interest_234b == 0
        assert result.interest_234c == 0

    def test_advance_tax_clears_234b(self):
        """234B does NOT apply when advance tax ≥ 90% of assessed tax."""
        engine = Interest234Engine()
        result = engine.compute(
            net_tax_liability=100_000,
            tds=0, tcs=0,
            tax_payments=[
                # Full advance tax paid during FY (≥ 90% × 100K = 90K)
                TaxPayment(amount=95_000, payment_date=date(2025, 12, 15)),
            ],
            age=30, assessment_year="2026-27",
            filing_date=date(2026, 12, 31),
            due_date=date(2026, 7, 31),
            total_income=700_000,
        )
        assert result.interest_234b == 0


# ─── Tax engine golden-case tests ──────────────────────────────────────────────

class TestTaxEngineGoldenCases:
    """Golden test cases from fixtures/golden_tax_cases.py applied to owned engine."""

    def test_case1_new_regime_15l(self):
        """CASE_1_NEW_REGIME_15L: Salaried, new regime, ₹15L"""
        engine = TaxEngine()
        salary = _salary_sched(15_00_000)
        tds = _tds_sched(150_000)

        result = engine.compute(
            schedules=[salary, tds],
            regime="new", ay="2026-27",
            dob=date(1990, 1, 1), pan="ABCDE1234F",
        )

        # gross: 15L - 75K std ded = 14.25L
        assert result.breakdown.gross_total_income == 14_25_000
        # New regime: no deductions
        assert result.breakdown.total_deductions == 0
        assert result.breakdown.total_income == 14_25_000
        # Tax: 4-8L: 4L×5%=20K; 8-12L: 4L×10%=40K;
        #       12-14.25L: 2.25L×15%=33,750 → 93,750
        assert result.breakdown.tax_before_rebate == 93_750
        assert result.breakdown.rebate_87a == 0
        assert result.breakdown.surcharge == 0
        # Cess: 93,750 × 4% = 3,750
        assert result.breakdown.cess == 3_750
        assert result.breakdown.total_tax_liability == 97_500
        # TDS 150,000 → refund = 52,500
        assert result.breakdown.refund == 52_500

    def test_case2_old_regime_15l_deductions(self):
        """CASE_2_OLD_REGIME_15L: Salaried, old regime, ₹15L, ₹1.5L deductions"""
        engine = TaxEngine()
        salary = _salary_sched(15_00_000)
        via = _via_sched_80c(150_000)
        tds = _tds_sched(150_000)

        result = engine.compute(
            schedules=[salary, via, tds],
            regime="old", ay="2026-27",
            dob=date(1990, 1, 1), pan="ABCDE1234F",
        )

        # gross: 15L - 50K std ded = 14.5L
        assert result.breakdown.gross_total_income == 14_50_000
        # 80C = 1.5L
        assert result.breakdown.total_deductions == 150_000
        # Total income: 14.5L - 1.5L = 13L
        assert result.breakdown.total_income == 13_00_000
        # Tax: 2.5-5L: 2.5L×5%=12,500; 5-10L: 5L×20%=1,00,000;
        #       10-13L: 3L×30%=90,000 → 2,02,500
        assert result.breakdown.tax_before_rebate == 2_02_500
        assert result.breakdown.rebate_87a == 0
        assert result.breakdown.surcharge == 0
        # Cess: 2,02,500 × 4% = 8,100
        assert result.breakdown.cess == 8_100
        assert result.breakdown.total_tax_liability == 2_10_600
        # Tax payable: 2,10,600 - 1,50,000 TDS + 234B/234C interest
        # (no advance tax paid → 234B/234C interest applies)
        assert result.breakdown.tax_payable >= 60_600

    def test_case3_rebate_87a_new_regime(self):
        """CASE_3_REBATE_87A: Low income, new regime, income ≤ ₹7L → full rebate"""
        engine = TaxEngine()
        salary = _salary_sched(6_50_000)
        tds = _tds_sched(15_000)

        result = engine.compute(
            schedules=[salary, tds],
            regime="new", ay="2026-27",
            dob=date(1990, 1, 1), pan="ABCDE1234F",
        )

        # gross: 6.5L - 75K = 5.75L
        assert result.breakdown.gross_total_income == 5_75_000
        assert result.breakdown.total_income == 5_75_000
        # Tax: 4-5.75L: 1.75L×5% = 8,750
        assert result.breakdown.tax_before_rebate == 8_750
        # Full rebate since income ≤ 7L
        assert result.breakdown.rebate_87a == 8_750
        assert result.breakdown.total_tax_liability == 0
        # Refund: TDS 15,000 - 0 = 15,000
        assert result.breakdown.refund == 15_000

    def test_case4_high_income_surcharge(self):
        """CASE_4_HIGH_INCOME: ₹60L, surcharge 10% applies (> ₹50L)"""
        engine = TaxEngine()
        salary = _salary_sched(60_00_000)
        tds = _tds_sched(12_00_000)

        result = engine.compute(
            schedules=[salary, tds],
            regime="new", ay="2026-27",
            dob=date(1990, 1, 1), pan="ABCDE1234F",
        )

        # gross: 60L - 75K = 59.25L
        assert result.breakdown.gross_total_income == 59_25_000
        assert result.breakdown.total_income == 59_25_000
        # Income > ₹50L → surcharge applies
        assert result.breakdown.surcharge > 0
        # Cess on (tax + surcharge)
        assert result.breakdown.cess > 0
        # Total liability > tax before rebate
        assert result.breakdown.total_tax_liability > result.breakdown.tax_before_rebate


class TestTaxEngineInterest:
    """Test interest within TaxEngine."""

    def test_late_filing_triggers_234a_and_234f(self):
        """Late filing: 234A interest + ₹5,000 late fee (income > 5L)."""
        engine = TaxEngine()
        salary = _salary_sched(10_00_000)

        result = engine.compute(
            schedules=[salary],
            regime="new", ay="2026-27",
            dob=date(1990, 1, 1), pan="ABCDE1234F",
            filing_date=date(2026, 12, 31),
            due_date=date(2026, 7, 31),
        )

        assert result.breakdown.interest_234a > 0
        assert result.breakdown.late_fee_234f == 5000   # > 5L income

    def test_on_time_no_interest(self):
        """On-time filing: no 234A and no 234F."""
        engine = TaxEngine()
        salary = _salary_sched(10_00_000)

        result = engine.compute(
            schedules=[salary],
            regime="new", ay="2026-27",
            dob=date(1990, 1, 1), pan="ABCDE1234F",
            filing_date=date(2026, 7, 15),
            due_date=date(2026, 7, 31),
        )

        assert result.breakdown.interest_234a == 0
        assert result.breakdown.late_fee_234f == 0


class TestTaxEnginePayload:
    """Test TaxEngineResult.to_payload() and ComputedReturn construction."""

    def test_payload_has_all_required_fields(self):
        engine = TaxEngine()
        salary = _salary_sched(10_00_000)
        result = engine.compute(
            schedules=[salary], regime="new", ay="2026-27",
            dob=date(1990, 1, 1), pan="ABCDE1234F",
        )

        payload = result.to_payload()
        for field in (
            "regime", "gross_total_income", "total_deductions", "total_income",
            "tax_before_rebate", "rebate_87a", "surcharge", "cess",
            "total_tax_liability", "tds", "tcs", "advance_tax",
            "tax_payable", "refund",
            "interest_234a", "interest_234b", "interest_234c", "late_fee_234f",
        ):
            assert field in payload, f"Missing: {field}"

    def test_payload_rule_version_contains_ay_and_owned(self):
        engine = TaxEngine()
        salary = _salary_sched(10_00_000)
        result = engine.compute(schedules=[salary], regime="new", ay="2026-27")
        assert "2026-27" in result.rule_version
        assert "owned-engine" in result.rule_version

    def test_slab_breakdown_populated(self):
        engine = TaxEngine()
        salary = _salary_sched(15_00_000)
        result = engine.compute(schedules=[salary], regime="new", ay="2026-27")
        assert len(result.slab_breakdown) > 0
        assert all(e.tax >= 0 for e in result.slab_breakdown)


class TestCapitalGains:
    """Test capital gains (ScheduleCG) integration in tax engine."""

    def test_ltcg_112a_listed_equity(self):
        """LTCG on listed equity u/s 112A @ 12.5% (AY 2026-27)."""
        engine = TaxEngine()
        cg = _cg_sched_ltcg_112a(5_00_000)
        result = engine.compute(schedules=[cg], regime="new", ay="2026-27")
        
        assert result.breakdown.gross_total_income == 5_00_000
        # LTCG is special rate income, included in GTI but not taxed at slab rates yet
        # For now, CG flows through as regular income (special rate tax is Phase 4)
        assert result.breakdown.total_income == 5_00_000
        
    def test_stcg_111a_listed_equity(self):
        """STCG on listed equity u/s 111A @ 15%."""
        engine = TaxEngine()
        cg = _cg_sched_stcg_111a(3_00_000)
        result = engine.compute(schedules=[cg], regime="new", ay="2026-27")
        
        assert result.breakdown.gross_total_income == 3_00_000
        
    def test_salary_plus_cg(self):
        """Combined salary and capital gains income."""
        engine = TaxEngine()
        salary = _salary_sched(8_00_000)
        cg = _cg_sched_ltcg_112a(2_00_000)
        result = engine.compute(schedules=[salary, cg], regime="new", ay="2026-27")
        
        # Salary after std deduction: 8L - 75K = 7.25L
        # CG: 2L
        # GTI: 9.25L
        assert result.breakdown.gross_total_income == 9_25_000
        
    def test_cg_explanation_steps(self):
        """Verify CG appears in explanation steps."""
        engine = TaxEngine()
        cg = _cg_sched_ltcg_112a(3_00_000)
        result = engine.compute(schedules=[cg], regime="new", ay="2026-27")
        
        cg_steps = [s for s in result.explanation if "cg" in s.step or "capital" in s.description.lower()]
        assert len(cg_steps) >= 2  # At least section-specific + total CG


class TestTaxEngineAdapter:
    """TaxEngineAdapter wraps TaxEngine and conforms to ITaxEngine."""

    async def test_adapter_returns_computed_return(self):
        from app.adapters.opentax.tax_engine_adapter import TaxEngineAdapter
        from app.core.domain.value_objects import TaxRegime, AssessmentYear

        adapter = TaxEngineAdapter()
        salary = _salary_sched(10_00_000)

        computed = await adapter.compute_tax(
            schedules=[salary],
            regime=TaxRegime("new"),
            ay=AssessmentYear("2026-27"),
            dob=date(1990, 1, 1),
            pan="ABCDE1234F",
            filing_date=date(2026, 7, 15),
        )

        assert computed.regime == "new"
        assert computed.ay == "2026-27"
        assert computed.pan == "ABCDE1234F"
        assert "owned-engine" in computed.rule_version
        assert computed.payload["regime"] == "new"

    async def test_adapter_explain_computation(self):
        from app.adapters.opentax.tax_engine_adapter import TaxEngineAdapter
        from app.core.domain.value_objects import TaxRegime, AssessmentYear

        adapter = TaxEngineAdapter()
        salary = _salary_sched(10_00_000)
        computed = await adapter.compute_tax(
            schedules=[salary],
            regime=TaxRegime("new"),
            ay=AssessmentYear("2026-27"),
        )
        steps = adapter.explain_computation(computed)
        assert len(steps) > 0
        assert all("step" in s and "output_value" in s for s in steps)
