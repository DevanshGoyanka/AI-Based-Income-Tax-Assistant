"""End-to-end test: Complete tax computation flow."""
import pytest
from decimal import Decimal
from uuid import uuid4

from app.core.domain.schedules.salary import ScheduleSalary, SalaryDetail
from app.core.domain.schedules.schedule_via import ScheduleVIA, Section80C, Section80D
from app.core.domain.value_objects import Money, TaxRegime, AssessmentYear
from app.adapters.opentax.tax_engine_adapter import OpenTaxAdapter


@pytest.mark.asyncio
async def test_end_to_end_tax_computation():
    """Test complete flow: Schedules -> OpenTax -> ComputedReturn."""
    
    # Setup
    filing_id = uuid4()
    ay = AssessmentYear("2026-27")
    
    # Create salary schedule
    salary_schedule = ScheduleSalary(
        id=uuid4(),
        filing_id=filing_id,
        ay="2026-27",
        employers=[
            SalaryDetail(
                employer_name="Tech Corp",
                employer_tan="BANG12345A",
                gross_salary=Money.from_rupees(1500000),
                allowances_exempt=Money.from_rupees(50000),
                professional_tax=Money.from_rupees(2400),
                tds_deducted=Money.from_rupees(150000),
                standard_deduction=Money.from_rupees(75000)
            )
        ]
    )
    
    # Create deduction schedule (old regime only)
    section_80c = Section80C(
        life_insurance=Money.from_rupees(50000),
        ppf=Money.from_rupees(50000),
        nps=Money.from_rupees(50000)
    )
    
    via_schedule = ScheduleVIA(
        id=uuid4(),
        filing_id=filing_id,
        ay="2026-27",
        section_80c=section_80c
    )
    
    schedules = [salary_schedule]
    
    # Test new regime
    adapter = OpenTaxAdapter()
    computed_new = await adapter.compute_tax(
        schedules=schedules,
        regime=TaxRegime("new"),
        ay=ay
    )
    
    assert computed_new.regime == "new"
    assert computed_new.payload["gross_total_income"] > 0
    assert computed_new.payload["total_tax_liability"] > 0
    assert len(computed_new.slab_breakdown) > 0
    
    # Test old regime with deductions
    schedules_old = [salary_schedule, via_schedule]
    computed_old = await adapter.compute_tax(
        schedules=schedules_old,
        regime=TaxRegime("old"),
        ay=ay
    )
    
    assert computed_old.regime == "old"
    assert computed_old.payload["total_deductions"] == 150000
    
    # Old regime should have lower tax due to deductions
    assert computed_old.payload["total_income"] < computed_new.payload["total_income"]
    
    print(f"New regime tax: ₹{computed_new.payload['total_tax_liability']:,}")
    print(f"Old regime tax: ₹{computed_old.payload['total_tax_liability']:,}")
