# Testing Strategy

**Version:** 1.0  
**Coverage Target:** 80% minimum  
**Framework:** pytest, Playwright

---

## Testing Pyramid

```
              ┌─────────┐
             /  E2E (5%) \
            /─────────────\
           / Integration  \
          /    (25%)       \
         /─────────────────\
        /   Unit Tests     \
       /      (70%)         \
      /─────────────────────\
```

---

## Unit Tests (70% of tests)

**Target:** Core domain, use cases, adapters

### Domain Entity Tests
```python
# tests/unit/core/domain/test_filing.py

def test_filing_creation():
    filing = Filing.create(
        client_id=uuid4(),
        ay="2026-27",
        regime="new"
    )
    assert filing.status == FilingStatus.DRAFT
    assert filing.regime == "new"

def test_filing_add_salary_income():
    filing = Filing.create(uuid4(), "2026-27", "new")
    filing.add_salary_income("Test Corp", Money(1500000))
    assert len(filing.salary_income) == 1
    assert filing.salary_income[0].gross_salary == Money(1500000)
```

### OpenTax Adapter Tests
```python
# tests/unit/adapters/opentax/test_model_mapper.py

def test_map_filing_to_opentax():
    filing = create_sample_filing()
    mapper = ERPToOpenTaxMapper()
    
    opentax_model = mapper.map_filing(filing, "new")
    
    assert opentax_model.assessment_year == "2026-27"
    assert opentax_model.regime == "new"
    assert len(opentax_model.salary) == 1

def test_round_trip_mapping():
    """Ensure no data loss in ERP → OpenTax → ERP"""
    original_filing = create_sample_filing()
    
    # Map to OpenTax
    opentax_model = erp_to_opentax.map_filing(original_filing, "new")
    
    # Compute (mock OpenTax response)
    tax_result = mock_opentax_result()
    
    # Map back
    computed = opentax_to_erp.map_computation(
        original_filing.id, tax_result, "new"
    )
    
    # Verify
    assert computed.regime == "new"
    assert computed.payload["total_income"] > 0
```

### Computation Tests
```python
# tests/unit/services/test_salary_computer.py

def test_salary_computation_new_regime():
    records = [CanonicalSalaryIncome(
        gross_salary=Decimal("1500000"),
        allowances_exempt=Decimal("0"),
        professional_tax=Decimal("2400")
    )]
    
    result = SalaryIncomeComputer().compute(
        records, "new", rules_2026_27
    )
    
    # Gross - Std deduction (75K) - Prof tax
    expected = Decimal("1500000") - Decimal("75000") - Decimal("2400")
    assert result == expected
```

---

## Integration Tests (25% of tests)

**Target:** Database, API, adapters working together

### Database Integration
```python
# tests/integration/test_filing_repository.py

@pytest.mark.asyncio
async def test_save_and_retrieve_filing(db_session):
    repo = SQLAlchemyFilingRepository(db_session)
    
    filing = Filing.create(uuid4(), "2026-27", "new")
    await repo.save(filing)
    
    retrieved = await repo.get_by_id(filing.id)
    assert retrieved.id == filing.id
    assert retrieved.ay == "2026-27"
```

### API Integration
```python
# tests/integration/test_computation_api.py

@pytest.mark.asyncio
async def test_compute_tax_endpoint(client, db_session):
    # Setup: create client and filing
    client_id = await create_test_client(db_session)
    filing_id = await create_test_filing(db_session, client_id)
    
    # Execute
    response = await client.post(
        f"/api/v1/computation/calculate",
        json={"filing_id": str(filing_id), "regime": "new"}
    )
    
    # Verify
    assert response.status_code == 200
    data = response.json()
    assert data["total_income"] > 0
    assert data["total_tax_liability"] >= 0
```

### OpenTax Integration
```python
# tests/integration/test_opentax_integration.py

@pytest.mark.asyncio
async def test_full_computation_flow(db_session):
    # Create complete filing
    filing = create_complete_filing()
    await filing_repo.save(filing)
    
    # Execute via OpenTax adapter
    adapter = OpenTaxAdapter()
    computed = await adapter.compute_tax(filing, "new")
    
    # Verify result structure
    assert computed.payload["gross_total_income"] > 0
    assert computed.payload["total_tax_liability"] >= 0
    assert len(computed.payload["slab_breakdown"]) > 0
```

---

## Golden Tests (Reference Computations)

**Purpose:** Validate tax computation accuracy against known-good results.

```python
# tests/golden/test_golden_cases.py

GOLDEN_CASES = [
    {
        "name": "Simple salary 15L new regime",
        "input": {
            "ay": "2026-27",
            "regime": "new",
            "salary": {"gross": 1500000},
            "tds": 150000
        },
        "expected": {
            "gross_total_income": 1425000,  # After 75K std ded
            "total_income": 1425000,
            "tax_before_rebate": 112500,
            "rebate_87a": 0,
            "cess": 4500,
            "total_tax_liability": 117000,
            "refund": 33000
        }
    },
    # ... 20 more golden cases
]

@pytest.mark.parametrize("case", GOLDEN_CASES)
@pytest.mark.asyncio
async def test_golden_case(case):
    filing = create_filing_from_spec(case["input"])
    
    adapter = OpenTaxAdapter()
    computed = await adapter.compute_tax(
        filing, case["input"]["regime"]
    )
    
    for key, expected_value in case["expected"].items():
        actual = computed.payload[key]
        assert actual == expected_value, (
            f"{case['name']}: {key} mismatch\n"
            f"Expected: {expected_value}\n"
            f"Actual: {actual}"
        )
```

---

## E2E Tests (5% of tests)

**Target:** User workflows end-to-end

```python
# tests/e2e/test_filing_workflow.py

@pytest.mark.e2e
async def test_complete_filing_workflow(browser):
    page = await browser.new_page()
    
    # 1. Login
    await page.goto("http://localhost:3000/login")
    await page.fill('input[name="email"]', "test@example.com")
    await page.fill('input[name="password"]', "password")
    await page.click('button[type="submit"]')
    
    # 2. Create client
    await page.goto("/clients/new")
    await page.fill('input[name="pan"]', "ABCDE1234F")
    await page.fill('input[name="name"]', "Test Client")
    await page.click('button:has-text("Save")')
    
    # 3. Create filing
    await page.click('text=Create Filing')
    await page.select_option('select[name="ay"]', "2026-27")
    await page.click('text=New Regime')
    
    # 4. Add salary income
    await page.fill('input[name="gross_salary"]', "1500000")
    await page.fill('input[name="employer"]', "Test Corp")
    
    # 5. Compute tax
    await page.click('button:has-text("Compute Tax")')
    await page.wait_for_selector('text=Tax Computed Successfully')
    
    # 6. Verify result
    refund = await page.text_content('[data-testid="refund-amount"]')
    assert "33,000" in refund
```

---

## Performance Tests

**Target:** API response times, computation speed

```python
# tests/performance/test_computation_performance.py

@pytest.mark.performance
async def test_computation_under_2_seconds():
    filing = create_complex_filing()  # All income heads
    
    start = time.time()
    computed = await compute_engine.compute(filing, "new")
    duration = time.time() - start
    
    assert duration < 2.0, f"Computation took {duration:.2f}s"

@pytest.mark.performance
async def test_api_response_under_200ms():
    responses = []
    
    for _ in range(100):
        start = time.time()
        response = await client.get("/api/v1/clients")
        responses.append(time.time() - start)
    
    p95 = sorted(responses)[94]  # 95th percentile
    assert p95 < 0.2, f"P95 response time: {p95:.3f}s"
```

---

## Load Tests

**Tool:** Locust

```python
# tests/load/locustfile.py

from locust import HttpUser, task, between

class ITRUser(HttpUser):
    wait_time = between(1, 3)
    
    def on_start(self):
        # Login
        response = self.client.post("/api/v1/auth/login", json={
            "email": "test@example.com",
            "password": "password"
        })
        self.token = response.json()["token"]
        self.client.headers["Authorization"] = f"Bearer {self.token}"
    
    @task(3)
    def list_clients(self):
        self.client.get("/api/v1/clients")
    
    @task(1)
    def compute_tax(self):
        self.client.post("/api/v1/computation/calculate", json={
            "filing_id": "uuid",
            "regime": "new"
        })
```

**Run:**
```bash
locust -f tests/load/locustfile.py --host http://localhost:8000
# Target: 100 concurrent users, <200ms p95
```

---

## Regression Tests

**Purpose:** Prevent old bugs from reappearing

```python
# tests/regression/test_fixed_bugs.py

@pytest.mark.regression
async def test_bug_123_duplicate_tds_on_merge():
    """Bug: AIS import was duplicating TDS records"""
    filing = Filing.create(uuid4(), "2026-27", "new")
    
    # Import AIS twice with same data
    await import_ais_use_case.execute(filing.id, ais_file)
    await import_ais_use_case.execute(filing.id, ais_file)
    
    # Should have unique TDS records only
    tds_count = len(filing.tds_records)
    assert tds_count == 5, f"Expected 5 TDS records, got {tds_count}"
```

---

## Test Data Management

### Fixtures
```python
# tests/fixtures/filing_fixtures.py

@pytest.fixture
def sample_filing():
    return Filing.create(
        client_id=uuid4(),
        ay="2026-27",
        regime="new"
    )

@pytest.fixture
def sample_client():
    return Client.create(
        pan="ABCDE1234F",
        name="Test Client",
        dob=date(1990, 1, 15)
    )

@pytest.fixture
async def db_session():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    
    async with AsyncSession(engine) as session:
        yield session
    
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
```

### Test Data Files
```
tests/fixtures/
├── ais_samples/
│   ├── simple_salary.json
│   ├── salary_hp_cg.json
│   └── complex_all_heads.json
├── form26as_samples/
│   ├── part1_salary.txt
│   └── all_parts.zip
└── golden_computations/
    ├── case_01_salary_15L.json
    ├── case_02_salary_old_regime.json
    └── case_20_complex.json
```

---

## CI/CD Integration

### GitHub Actions
```yaml
# .github/workflows/test.yml

name: Tests

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-python@v4
      - run: pip install -r requirements-dev.txt
      - run: pytest tests/unit/ --cov=app --cov-report=xml
      - uses: codecov/codecov-action@v3
  
  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: test
    steps:
      - uses: actions/checkout@v3
      - run: pytest tests/integration/
  
  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - run: npm install
      - run: playwright install
      - run: pytest tests/e2e/
```

---

## Coverage Requirements

**Minimum Coverage:**
- Core domain: 90%
- Use cases: 85%
- Adapters: 80%
- API: 75%
- Overall: 80%

**Exclude from coverage:**
- Test files
- Migrations
- Config files
- `__init__.py`

---

## Testing Checklist

Before each release:
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] All golden tests pass
- [ ] E2E tests pass
- [ ] Coverage ≥80%
- [ ] Load test passed (100 users)
- [ ] No security vulnerabilities
- [ ] Performance targets met

---

**Status:** READY FOR IMPLEMENTATION
