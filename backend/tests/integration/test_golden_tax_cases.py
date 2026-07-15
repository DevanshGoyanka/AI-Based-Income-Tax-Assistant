"""Test OpenTax adapter against golden test cases."""
import pytest
from uuid import uuid4

from app.core.domain.schedules.salary import ScheduleSalary, SalaryDetail
from app.core.domain.schedules.schedule_via import ScheduleVIA, Section80C
from app.core.domain.value_objects import Money, TaxRegime, AssessmentYear
from app.adapters.opentax.tax_engine_adapter import OpenTaxAdapter
from tests.fixtures.golden_tax_cases import GOLDEN_TEST_CASES


@pytest.mark.parametrize("test_case", GOLDEN_TEST_CASES, ids=lambda tc: tc["name"])
@pytest.mark.asyncio
async def test_golden_tax_computation(test_case):
    """Test OpenTax adapter against known-good tax computations."""
    
    input_data = test_case["input"]
    expected = test_case["expected"]
    
    filing_id = uuid4()
    
    # Build schedules from input
    salary_data = input_data.get("salary", {})
    salary_schedule = ScheduleSalary(
        id=uuid4(),
        filing_id=filing_id,
        ay=input_data["ay"],
        employers=[
            SalaryDetail(
                employer_name=salary_data.get("employer_name", "Employer"),
                employer_tan=salary_data.get("employer_tan", "AAAAA0000A"),
                gross_salary=Money.from_rupees(salary_data["gross"]),
                allowances_exempt=Money.from_rupees(0),
                professional_tax=Money.from_rupees(0),
                tds_deducted=Money.from_rupees(input_data.get("tds", 0)),
            )
        ]
    )
    
    schedules = [salary_schedule]
    
    # Add deductions if old regime
    if input_data["regime"] == "old" and "deductions" in input_data:
        ded = input_data["deductions"]
        section_80c = Section80C(
            life_insurance=Money.from_rupees(ded.get("section_80c", 0))
        )
        via_schedule = ScheduleVIA(
            id=uuid4(),
            filing_id=filing_id,
            ay=input_data["ay"],
            section_80c=section_80c
        )
        schedules.append(via_schedule)
    
    # Compute tax
    adapter = OpenTaxAdapter()
    computed = await adapter.compute_tax(
        schedules=schedules,
        regime=TaxRegime(input_data["regime"]),
        ay=AssessmentYear(input_data["ay"])
    )
    
    # Validate results
    tolerance = 100  # ₹100 tolerance for rounding differences
    
    for key, expected_value in expected.items():
        actual_value = computed.payload.get(key)
        
        if actual_value is None:
            pytest.fail(f"{key} missing in computed result")
        
        diff = abs(actual_value - expected_value)
        
        assert diff <= tolerance, (
            f"{key}: expected {expected_value:,}, got {actual_value:,}, "
            f"diff {diff:,} exceeds tolerance {tolerance}"
        )
