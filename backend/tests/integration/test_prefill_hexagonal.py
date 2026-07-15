"""Test prefill import with new hexagonal architecture."""
import pytest
import json
from uuid import uuid4
from decimal import Decimal

from app.core.use_cases.import_prefill import ImportPrefillUseCase
from app.core.normalizers.prefill_normalizer import PrefillNormalizer
from app.core.normalizers.canonical_mapper import CanonicalToScheduleMapper
from app.core.domain.value_objects import AssessmentYear


@pytest.fixture
def sample_prefill_data():
    """Load sample prefill JSON."""
    with open("tests/fixtures/sample_prefill.json") as f:
        return json.load(f)


@pytest.fixture
def normalizer():
    return PrefillNormalizer()


@pytest.fixture
def mapper():
    return CanonicalToScheduleMapper()


class TestPrefillNormalizer:
    """Test prefill JSON normalization."""
    
    def test_normalize_extracts_salaries(self, normalizer, sample_prefill_data):
        canonical = normalizer.normalize(sample_prefill_data)
        
        assert len(canonical.salaries) == 1
        assert canonical.salaries[0].employer_name == "ABC Corp Pvt Ltd"
        assert canonical.salaries[0].employer_tan == "BANG12345A"
        assert canonical.salaries[0].gross_salary == Decimal("1500000")
        assert canonical.salaries[0].tds_deducted == Decimal("130000")
    
    def test_normalize_extracts_house_property(self, normalizer, sample_prefill_data):
        canonical = normalizer.normalize(sample_prefill_data)
        
        assert len(canonical.house_properties) == 1
        prop = canonical.house_properties[0]
        assert "Mumbai" in prop.address
        assert prop.annual_rent == Decimal("120000")
        assert prop.interest_paid == Decimal("200000")
    
    def test_normalize_extracts_interest(self, normalizer, sample_prefill_data):
        canonical = normalizer.normalize(sample_prefill_data)
        
        assert len(canonical.interest_income) == 2
        total_interest = sum(i.interest_amount for i in canonical.interest_income)
        assert total_interest == Decimal("25000")
    
    def test_normalize_extracts_tds(self, normalizer, sample_prefill_data):
        canonical = normalizer.normalize(sample_prefill_data)
        
        assert len(canonical.tds_entries) == 2
        assert canonical.tds_entries[0].section_code == "192"
        assert canonical.tds_entries[1].section_code == "194A"


class TestCanonicalMapper:
    """Test canonical to schedule mapping."""
    
    def test_maps_salary_schedule(self, normalizer, mapper, sample_prefill_data):
        canonical = normalizer.normalize(sample_prefill_data)
        filing_id = uuid4()
        
        schedules = mapper.map_to_schedules(canonical, filing_id, "2026-27")
        
        salary_schedule = next(s for s in schedules if type(s).__name__ == "ScheduleSalary")
        assert len(salary_schedule.employers) == 1
        assert salary_schedule.employers[0].employer_tan == "BANG12345A"
    
    def test_maps_house_property_schedule(self, normalizer, mapper, sample_prefill_data):
        canonical = normalizer.normalize(sample_prefill_data)
        filing_id = uuid4()
        
        schedules = mapper.map_to_schedules(canonical, filing_id, "2026-27")
        
        hp_schedule = next(s for s in schedules if type(s).__name__ == "ScheduleHP")
        assert len(hp_schedule.properties) == 1
        assert hp_schedule.properties[0].annual_rent.to_rupees() == Decimal("120000")
    
    def test_schedule_income_computation(self, normalizer, mapper, sample_prefill_data):
        canonical = normalizer.normalize(sample_prefill_data)
        filing_id = uuid4()
        
        schedules = mapper.map_to_schedules(canonical, filing_id, "2026-27")
        
        salary_schedule = next(s for s in schedules if type(s).__name__ == "ScheduleSalary")
        income = salary_schedule.compute_income()
        assert income > Decimal("1300000")


print("\n✓ Created test file: tests/integration/test_prefill_hexagonal.py")
print("\nTo test with real prefill JSON:")
print("1. Start backend: cd backend && python -m uvicorn app.main:app --reload")
print("2. Use curl or Postman:")
print("   curl -X POST http://localhost:8000/api/v1/imports/prefill/<client-id> \\")
print("        -F 'file=@tests/fixtures/sample_prefill.json'")
print("\n3. Or run unit tests: pytest tests/integration/test_prefill_hexagonal.py -v")
