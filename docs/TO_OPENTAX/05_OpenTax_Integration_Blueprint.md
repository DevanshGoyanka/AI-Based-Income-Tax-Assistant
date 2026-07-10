# OpenTax Integration Blueprint

**Document Version:** 2.0 (Critical Refactor)  
**Date:** 2026-07-10  
**Integration Pattern:** Compatibility Layer (Not Core)

---

## CRITICAL ARCHITECTURAL CHANGE

OpenTax is now a **compatibility layer**, NOT the core tax engine.

### Before (Wrong):
```
ERP Domain
    ↓
OpenTax (Core)
    ↓
Tax Computation
```

### After (Correct):
```
ERP Domain (Core Tax Logic)
    ↓
ITaxEngine Interface
    ↓
OpenTaxAdapter | CustomAdapter | CommercialAdapter
```

OpenTax is ONE implementation of ITaxEngine, easily swappable.

---

## 1. Integration Strategy

### 1.1 Vendor Approach

**What to Vendor:**
- `api/filing/tax_calculation/` (entire module)
- `api/filing/itr/itr1/` (ITR-1 builder)
- `api/filing/itr/validations/` (validation engine)
- `api/filing/models/` (Pydantic schemas)

**How to Vendor:**
```bash
# 1. Fork OpenTax at specific commit
git clone https://github.com/opentax/opentax.git
cd opentax
git checkout <commit-sha>  # Lock to stable version

# 2. Copy modules to ERP
cp -r api/filing/tax_calculation backend/app/adapters/opentax/vendor/
cp -r api/filing/itr backend/app/adapters/opentax/vendor/
cp -r api/filing/models backend/app/adapters/opentax/vendor/

# 3. Create __init__.py marker
echo "# Vendored from OpenTax <commit-sha>" > backend/app/adapters/opentax/vendor/__init__.py

# 4. Lock in requirements.txt
echo "# OpenTax vendored dependencies" >> requirements.txt
echo "pydantic==2.12.4" >> requirements.txt
```

### 1.2 Versioning Strategy

**Lock to Commit SHA:**
```python
# backend/app/adapters/opentax/vendor/__version__.py
OPENTAX_COMMIT = "abc123def456"
OPENTAX_VERSION = "0.1.0"
VENDOR_DATE = "2026-07-10"
```

**Update Process:**
1. Test new OpenTax version in staging
2. Update vendored code
3. Run full regression test suite
4. Update __version__.py
5. Deploy to production

---

## 2. Adapter Layer Design

### 2.1 Tax Engine Adapter (Compatibility Layer)

```python
# backend/app/adapters/opentax/tax_engine_adapter.py

from typing import List
from uuid import UUID
from core.interfaces.tax_engine import ITaxEngine
from core.domain.schedule import Schedule
from core.domain.computed_return import ComputedReturn
from .model_mapper import ScheduleToOpenTaxMapper
from .response_mapper import OpenTaxToERPMapper
from .vendor.tax_calculation.tax_calculation_service import TaxCalculationService

class OpenTaxAdapter(ITaxEngine):
    """
    OpenTax compatibility layer.
    
    OpenTax is NOT the core - it's ONE implementation of ITaxEngine.
    Can be swapped for CustomTaxEngine or CommercialTaxEngine.
    """
    
    def __init__(self):
        self.tax_service = TaxCalculationService()  # Vendored OpenTax
        self.mapper = ScheduleToOpenTaxMapper()
        self.response_mapper = OpenTaxToERPMapper()
    
    async def compute_tax(
        self, 
        schedules: List[Schedule],
        regime: str,
        ay: str
    ) -> ComputedReturn:
        """
        Compute tax using OpenTax engine.
        
        IMPORTANT: OpenTax never accesses ERP database or models.
        All data passed as pure schedules.
        """
        
        # 1. Map ERP Schedules to OpenTax FilingModel
        opentax_filing = self.mapper.map_schedules(schedules, regime, ay)
        
        # 2. Call vendored OpenTax (pure computation, no side effects)
        result = await self.tax_service.calculate(opentax_filing)
        
        # 3. Map OpenTax result back to ERP ComputedReturn
        computed = self.response_mapper.map_computation(result)
        
        return computed
    
    async def generate_itr_json(self, filing: Filing) -> dict:
        """Generate ITR JSON using OpenTax builder."""
        
        # 1. Map to OpenTax
        opentax_filing = self.to_opentax.map_filing(filing, filing.regime)
        
        # 2. Build ITR via orchestrator
        result = await self.itr_orchestrator.build_itr(opentax_filing)
        
        # 3. Return ITR1 JSON
        if result.itr_type == "ITR1":
            return result.itr1.model_dump(by_alias=True)
        else:
            raise NotImplementedError("ITR-2 not yet supported")
    
    async def validate_filing(
        self, 
        filing: Filing
    ) -> List[ValidationError]:
        """Validate filing using OpenTax rules."""
        
        opentax_filing = self.to_opentax.map_filing(filing, filing.regime)
        result = await self.itr_orchestrator.build_itr(opentax_filing)
        
        return [
            ValidationError(
                field=err.field,
                message=err.message,
                severity=err.severity
            )
            for err in result.filingSummary.user_validation_errors or []
        ]
```

### 2.2 Model Mapper (ERP to OpenTax)

```python
# backend/app/adapters/opentax/model_mapper.py

from decimal import Decimal
from core.domain.filing import Filing
from .vendor.models.filing_model import FilingModel
from .vendor.models.person_model import PersonModel
from .vendor.models.salary_model import SalaryModel

class ERPToOpenTaxMapper:
    """Maps ERP domain models to OpenTax Pydantic models."""
    
    def map_filing(self, filing: Filing, regime: str) -> FilingModel:
        """Convert ERP Filing aggregate to OpenTax FilingModel."""
        
        return FilingModel(
            filing_id=str(filing.id),
            assessment_year=filing.ay.value,
            regime=regime,
            person=self._map_person(filing.client),
            salary=self._map_salary(filing.salary_income),
            house_property=self._map_house_property(filing.hp_income),
            capital_gains_securities=self._map_cg_securities(filing.cg_income),
            interest_income=self._map_interest(filing.interest_income),
            dividend_income=self._map_dividend(filing.dividend_income),
            tds=self._map_tds(filing.tds_records),
            tcs=self._map_tcs(filing.tcs_records),
            advance_tax=self._map_advance_tax(filing.advance_tax),
            section_80c=self._map_80c(filing.deductions.section_80c),
            section_80d=self._map_80d(filing.deductions.section_80d),
            # ... map all other sections
        )
    
    def _map_person(self, client) -> PersonModel:
        return PersonModel(
            first_name=client.first_name,
            last_name=client.last_name,
            pan_number=client.pan.value,
            date_of_birth=client.dob,
            residential_status=client.residential_status or "RESIDENT",
            mobile_number=client.mobile,
            email=client.email,
            address=self._map_address(client.address),
        )
    
    def _map_salary(self, salary_income) -> List[SalaryModel]:
        if not salary_income:
            return []
        
        return [
            SalaryModel(
                employer=self._map_employer(sal.employer),
                gross_salary=self._to_decimal(sal.gross_salary),
                # ... map all salary fields
            )
            for sal in salary_income
        ]
    
    @staticmethod
    def _to_decimal(value) -> Decimal:
        """Convert ERP Money to Decimal rupees."""
        if value is None:
            return Decimal("0")
        # ERP stores paise, OpenTax expects rupees
        return Decimal(value) / Decimal("100")
```

### 2.3 Response Mapper (OpenTax to ERP)

```python
# backend/app/adapters/opentax/response_mapper.py

from uuid import UUID
from decimal import Decimal
from core.domain.computed_return import ComputedReturn, TaxBreakdown
from .vendor.tax_calculation.models.tax_calculation_response import (
    TaxCalculationResponseModel
)

class OpenTaxToERPMapper:
    """Maps OpenTax results to ERP domain models."""
    
    def map_computation(
        self,
        filing_id: UUID,
        opentax_result: TaxCalculationResponseModel,
        regime: str
    ) -> ComputedReturn:
        """Convert OpenTax tax computation to ERP ComputedReturn."""
        
        current = opentax_result.current_regime
        
        return ComputedReturn.create(
            filing_id=filing_id,
            regime=regime,
            payload={
                "gross_total_income": int(current.gross_total_income),
                "total_deductions": int(current.total_deductions),
                "total_income": int(current.total_income),
                "tax_before_rebate": int(current.tax_before_rebate),
                "rebate_87a": int(current.rebate_87a),
                "surcharge": int(current.surcharge),
                "cess": int(current.health_education_cess),
                "total_tax_liability": int(current.total_tax_liability),
                "tds": int(current.tds),
                "tcs": int(current.tcs),
                "advance_tax": int(current.advance_tax),
                "tax_payable": int(current.tax_payable),
                "refund": int(current.refund),
                "interest_234a": int(current.tax_intrest_breakdown.interest_234a),
                "interest_234b": int(current.tax_intrest_breakdown.interest_234b),
                "interest_234c": int(current.tax_intrest_breakdown.interest_234c),
                "late_fee_234f": int(current.tax_intrest_breakdown.late_fee_234f),
                "slab_breakdown": [
                    {
                        "from": int(slab.from_amount),
                        "to": int(slab.to_amount) if slab.to_amount else None,
                        "rate": float(slab.rate),
                        "taxable": int(slab.taxable_amount),
                        "tax": int(slab.tax)
                    }
                    for slab in current.slab_breakdown or []
                ]
            },
            rule_version=f"AY-{opentax_result.current_regime.regime}"
        )
```

---

## 3. Canonical Data Model

### 3.1 Canonical Income Model

```python
# backend/app/services/canonical/canonical_income.py

from dataclasses import dataclass
from typing import List, Optional
from decimal import Decimal

@dataclass
class CanonicalSalaryIncome:
    """Normalized salary income from all sources."""
    
    employer_name: str
    employer_tan: Optional[str]
    gross_salary: Decimal
    allowances_exempt: Decimal
    standard_deduction: Decimal
    professional_tax: Decimal
    tds_deducted: Decimal
    
    # HRA details
    hra_received: Optional[Decimal] = None
    hra_exempt: Optional[Decimal] = None
    rent_paid: Optional[Decimal] = None
    
    # Source metadata
    source: str = "manual"  # "ais", "form16", "26as", "manual"
    source_id: Optional[str] = None

@dataclass
class CanonicalTDSRecord:
    """Normalized TDS record."""
    
    deductor_tan: str
    deductor_name: str
    section_code: str
    amount_paid: Decimal
    tax_deducted: Decimal
    quarter: Optional[str]
    receipt_number: Optional[str]
    date_of_deduction: Optional[str]
    
    source: str = "26as"

@dataclass
class CanonicalIncomeData:
    """Complete normalized income data."""
    
    salary: List[CanonicalSalaryIncome]
    house_property: List  # TODO: Define structure
    capital_gains: List   # TODO: Define structure
    other_sources: dict
    tds_records: List[CanonicalTDSRecord]
    tcs_records: List
```

### 3.2 Normalizer Service

```python
# backend/app/services/canonical/normalizer.py

from typing import Dict, Any
from .canonical_income import CanonicalIncomeData, CanonicalSalaryIncome

class CanonicalNormalizer:
    """Normalize data from different sources to canonical model."""
    
    def normalize_ais(self, ais_data: Dict[str, Any]) -> CanonicalIncomeData:
        """Normalize AIS JSON to canonical model."""
        
        salary = []
        tds_records = []
        
        # Extract salary from AIS Part B
        for emp in ais_data.get("part_b", {}).get("salary", []):
            salary.append(CanonicalSalaryIncome(
                employer_name=emp.get("employer_name", ""),
                employer_tan=emp.get("tan"),
                gross_salary=Decimal(emp.get("gross_salary", 0)),
                allowances_exempt=Decimal(emp.get("allowances_exempt", 0)),
                standard_deduction=Decimal(0),  # Computed later
                professional_tax=Decimal(emp.get("professional_tax", 0)),
                tds_deducted=Decimal(emp.get("tds", 0)),
                source="ais",
                source_id=ais_data.get("ais_id")
            ))
        
        # Extract TDS records
        for tds in ais_data.get("part_b", {}).get("tds", []):
            tds_records.append(CanonicalTDSRecord(
                deductor_tan=tds.get("tan"),
                deductor_name=tds.get("deductor_name"),
                section_code=tds.get("section"),
                amount_paid=Decimal(tds.get("amount_paid", 0)),
                tax_deducted=Decimal(tds.get("tax_deducted", 0)),
                quarter=tds.get("quarter"),
                receipt_number=tds.get("receipt"),
                date_of_deduction=tds.get("date"),
                source="ais"
            ))
        
        return CanonicalIncomeData(
            salary=salary,
            house_property=[],
            capital_gains=[],
            other_sources={},
            tds_records=tds_records,
            tcs_records=[]
        )
    
    def normalize_form26as(self, form26as_data: Dict[str, Any]) -> CanonicalIncomeData:
        """Normalize Form 26AS to canonical model."""
        # Similar structure
        pass
    
    def merge(
        self, 
        existing: CanonicalIncomeData, 
        new: CanonicalIncomeData
    ) -> CanonicalIncomeData:
        """Intelligently merge canonical data."""
        
        # Merge salary (detect duplicates by TAN)
        merged_salary = self._merge_salary(existing.salary, new.salary)
        
        # Merge TDS (detect duplicates by TAN + section + amount)
        merged_tds = self._merge_tds(existing.tds_records, new.tds_records)
        
        return CanonicalIncomeData(
            salary=merged_salary,
            house_property=existing.house_property + new.house_property,
            capital_gains=existing.capital_gains + new.capital_gains,
            other_sources={**existing.other_sources, **new.other_sources},
            tds_records=merged_tds,
            tcs_records=existing.tcs_records + new.tcs_records
        )
    
    def _merge_salary(self, existing, new):
        """Merge salary records, preferring newer data."""
        by_tan = {s.employer_tan: s for s in existing}
        
        for sal in new:
            if sal.employer_tan in by_tan:
                # Update existing
                by_tan[sal.employer_tan] = sal
            else:
                # Add new
                by_tan[sal.employer_tan] = sal
        
        return list(by_tan.values())
    
    def _merge_tds(self, existing, new):
        """Merge TDS records, avoiding duplicates."""
        seen = {
            (t.deductor_tan, t.section_code, int(t.amount_paid))
            for t in existing
        }
        
        merged = existing.copy()
        
        for tds in new:
            key = (tds.deductor_tan, tds.section_code, int(tds.amount_paid))
            if key not in seen:
                merged.append(tds)
                seen.add(key)
        
        return merged
```

---

## 4. Integration Points

### 4.1 Tax Computation Flow

```python
# core/use_cases/filing/compute_tax.py

class ComputeTaxUseCase:
    def __init__(
        self,
        filing_repo: IFilingRepository,
        tax_engine: ITaxEngine,  # <- OpenTaxAdapter injected here
        snapshot_repo: ISnapshotRepository
    ):
        self.filing_repo = filing_repo
        self.tax_engine = tax_engine
        self.snapshot_repo = snapshot_repo
    
    async def execute(self, filing_id: UUID) -> ComputedReturn:
        filing = await self.filing_repo.get_by_id(filing_id)
        
        # OpenTax adapter handles all tax logic
        computed = await self.tax_engine.compute_tax(
            filing, 
            filing.regime
        )
        
        await self.snapshot_repo.save(computed)
        return computed
```

### 4.2 ITR JSON Generation Flow

```python
# core/use_cases/filing/generate_itr_json.py

class GenerateITRJsonUseCase:
    def __init__(
        self,
        filing_repo: IFilingRepository,
        tax_engine: ITaxEngine,
        storage: IStorage
    ):
        self.filing_repo = filing_repo
        self.tax_engine = tax_engine
        self.storage = storage
    
    async def execute(self, filing_id: UUID) -> str:
        filing = await self.filing_repo.get_by_id(filing_id)
        
        # OpenTax adapter builds ITR JSON
        itr_json = await self.tax_engine.generate_itr_json(filing)
        
        # Store JSON
        json_path = f"filings/{filing_id}/itr.json"
        await self.storage.save_json(json_path, itr_json)
        
        return json_path
```

---

## 5. Testing Strategy

### 5.1 Adapter Unit Tests

```python
# tests/unit/adapters/test_opentax_adapter.py

import pytest
from adapters.opentax.tax_engine_adapter import OpenTaxAdapter
from core.domain.filing import Filing

@pytest.fixture
def adapter():
    return OpenTaxAdapter()

@pytest.fixture
def sample_filing():
    return Filing.create(
        client_id=uuid4(),
        ay="2026-27",
        regime="new"
    )

async def test_compute_tax_new_regime(adapter, sample_filing):
    result = await adapter.compute_tax(sample_filing, "new")
    
    assert result.regime == "new"
    assert result.payload["total_income"] >= 0
    assert result.payload["total_tax_liability"] >= 0

async def test_mapper_preserves_data(adapter, sample_filing):
    sample_filing.add_salary_income(
        employer="Test Corp",
        gross_salary=1500000
    )
    
    opentax_model = adapter.to_opentax.map_filing(sample_filing, "new")
    
    assert opentax_model.assessment_year == "2026-27"
    assert len(opentax_model.salary) == 1
    assert opentax_model.salary[0].gross_salary == Decimal("1500000")
```

### 5.2 Integration Tests

```python
# tests/integration/test_opentax_integration.py

async def test_end_to_end_computation(db_session):
    # 1. Create client
    client_repo = SQLAlchemyClientRepository(db_session)
    client = Client.create(pan="ABCDE1234F", name="Test User")
    await client_repo.save(client)
    
    # 2. Create filing
    filing_repo = SQLAlchemyFilingRepository(db_session)
    filing = Filing.create(client.id, "2026-27", "new")
    filing.add_salary_income("Test Corp", 1500000)
    await filing_repo.save(filing)
    
    # 3. Compute tax via OpenTax adapter
    tax_engine = OpenTaxAdapter()
    computed = await tax_engine.compute_tax(filing, "new")
    
    # 4. Verify result
    assert computed.payload["total_income"] > 0
    assert computed.payload["tax_payable"] > 0
```

### 5.3 Golden Test Cases

```python
# tests/fixtures/golden_computations/test_case_1.py

GOLDEN_TEST_CASE_1 = {
    "name": "Salaried individual, new regime, ₹15L",
    "input": {
        "ay": "2026-27",
        "regime": "new",
        "salary": {
            "gross": 1500000,
            "employer": "Tech Corp",
        },
        "tds": 150000,
    },
    "expected": {
        "gross_total_income": 1425000,  # After ₹75K std deduction
        "total_income": 1425000,
        "tax_before_rebate": 112500,
        "rebate_87a": 0,
        "surcharge": 0,
        "cess": 4500,
        "total_tax_liability": 117000,
        "tax_payable": 0,  # TDS covers it
        "refund": 33000,
    }
}

async def test_golden_case_1(adapter):
    filing = create_filing_from_dict(GOLDEN_TEST_CASE_1["input"])
    result = await adapter.compute_tax(filing, "new")
    
    for key, expected_value in GOLDEN_TEST_CASE_1["expected"].items():
        actual_value = result.payload[key]
        assert actual_value == expected_value, (
            f"{key}: expected {expected_value}, got {actual_value}"
        )
```

---

## 6. Rollout Plan

### Phase 1: Adapter Setup (Week 2)
- Vendor OpenTax code
- Create adapter interfaces
- Implement basic mapper

### Phase 2: Salary Integration (Week 3)
- Map salary income
- Test against Form 16 data
- Verify standard deduction

### Phase 3: House Property (Week 4)
- Map HP income
- Test interest calculations
- Verify loss set-off

### Phase 4: Deductions (Week 5)
- Map all 80C-80U sections
- Verify limits
- Test old vs new regime

### Phase 5: Full Computation (Week 6)
- Complete tax computation
- Test surcharge + cess
- Verify rebate 87A

### Phase 6: ITR JSON (Week 7)
- Generate ITR-1 JSON
- Validate against ITD schema
- Test with real data

### Phase 7: Production (Week 8)
- Pilot with 10 clients
- Monitor for errors
- Full rollout

---

**Integration Status:** DESIGN COMPLETE  
**Implementation Ready:** Yes  
**Risk Level:** Low (vendored code is stable)
