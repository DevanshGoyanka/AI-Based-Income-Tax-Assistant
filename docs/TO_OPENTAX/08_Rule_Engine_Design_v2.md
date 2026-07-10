# Rule Engine Architecture (Refactored)

**Version:** 2.0 (Critical Refactor)  
**Date:** 2026-07-10  
**Pattern:** Separated Concerns - Tax / Schema / Validation

---

## Critical Change

Previous design conflated three independent concerns:
- Tax computation rules (slabs, rates)
- ITR schema definitions (JSON structure)
- Validation rules (CBDT checks)

These change at different frequencies and must be separate.

---

## Architecture Overview

```
┌─────────────────────────────────────────────┐
│            Rule System (3 subsystems)       │
├─────────────────────────────────────────────┤
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │   TaxRuleEngine                      │  │
│  │   - Slabs, rates, deduction limits   │  │
│  │   - Changes rarely (Act amendments)  │  │
│  └──────────────────────────────────────┘  │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │   SchemaRegistry                     │  │
│  │   - ITR JSON structure per AY        │  │
│  │   - Changes yearly (CBDT updates)    │  │
│  └──────────────────────────────────────┘  │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │   ValidationEngine                   │  │
│  │   - Business rules, CBDT checks      │  │
│  │   - Changes frequently (circulars)   │  │
│  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

---

## 1. Tax Rule Engine

**Purpose:** Tax computation logic only.

### Database Schema

```sql
CREATE TABLE tax_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_year VARCHAR(7) NOT NULL UNIQUE,
    
    -- Slab structures
    old_regime_slabs JSONB NOT NULL,
    new_regime_slabs JSONB NOT NULL,
    
    -- Standard deductions
    old_standard_deduction INTEGER NOT NULL,
    new_standard_deduction INTEGER NOT NULL,
    
    -- Rebate 87A
    old_rebate_limit INTEGER NOT NULL,
    old_rebate_threshold INTEGER NOT NULL,
    new_rebate_limit INTEGER NOT NULL,
    new_rebate_threshold INTEGER NOT NULL,
    
    -- Surcharge rules
    surcharge_rules JSONB NOT NULL,
    
    -- Cess
    cess_rate DECIMAL(5,4) NOT NULL DEFAULT 0.04,
    
    -- Deduction limits
    deduction_limits JSONB NOT NULL,
    
    -- Special rates (CG, VDA, lottery)
    special_rates JSONB NOT NULL,
    
    -- Loss carry forward
    loss_cf_years INTEGER NOT NULL DEFAULT 8,
    hp_loss_cap INTEGER NOT NULL DEFAULT 200000,
    
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### Domain Model

```python
# core/domain/tax_rules.py

@dataclass
class TaxRules:
    """Pure tax computation rules."""
    
    ay: str
    old_slabs: dict[int, List[TaxSlab]]  # By age
    new_slabs: List[TaxSlab]
    old_std_deduction: Money
    new_std_deduction: Money
    old_rebate: RebateRule
    new_rebate: RebateRule
    surcharge: List[SurchargeRule]
    cess_rate: Decimal
    deduction_limits: dict[str, Money]
    special_rates: dict[str, Decimal]
```

### Usage

```python
tax_rules = TaxRuleRepository().get(ay="2026-27")
tax = compute_tax(income, regime, tax_rules)
```

---

## 2. Schema Registry

**Purpose:** ITR JSON structure definitions.

### Database Schema

```sql
CREATE TABLE itr_schemas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    itr_type VARCHAR(10) NOT NULL,  -- ITR1, ITR2, ITR3, etc.
    assessment_year VARCHAR(7) NOT NULL,
    schema_version VARCHAR(20) NOT NULL,  -- v1.0, v1.1
    
    -- JSON schema definition
    json_schema JSONB NOT NULL,
    
    -- Field mappings (schedule → ITR JSON paths)
    field_mappings JSONB NOT NULL,
    
    -- Required schedules for this ITR type
    required_schedules JSONB NOT NULL,
    
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_schema UNIQUE (itr_type, assessment_year, schema_version)
);

CREATE INDEX idx_schemas_active ON itr_schemas(itr_type, assessment_year) 
WHERE active = TRUE;
```

### Domain Model

```python
# core/domain/itr_schema.py

@dataclass
class ITRSchema:
    """ITR JSON structure definition."""
    
    itr_type: str
    ay: str
    version: str
    json_schema: dict
    field_mappings: dict[str, str]  # schedule_field → json_path
    required_schedules: List[str]
    
    def map_schedule_to_json(
        self, 
        schedule: Schedule
    ) -> dict:
        """Map schedule to ITR JSON using field mappings."""
        pass
    
    def validate_schema(self, itr_json: dict) -> List[str]:
        """Validate against JSON schema."""
        pass
```

### Usage

```python
schema = SchemaRegistry().get(itr_type="ITR1", ay="2026-27")
itr_json = ITRBuilder().build(schedules, schema)
```

---

## 3. Validation Engine

**Purpose:** Business rule validation.

### Database Schema

```sql
CREATE TABLE validation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    itr_type VARCHAR(10) NOT NULL,
    assessment_year VARCHAR(7),  -- NULL = applies to all AYs
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_type VARCHAR(20) NOT NULL,  -- 'blocking', 'warning', 'info'
    
    -- Rule definition
    field_path VARCHAR(200) NOT NULL,
    condition JSONB NOT NULL,  -- Validation logic
    error_message TEXT NOT NULL,
    itd_error_code VARCHAR(20),
    
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_validation_itr ON validation_rules(itr_type, assessment_year) 
WHERE active = TRUE;
```

### Domain Model

```python
# core/domain/validation_rules.py

@dataclass
class ValidationRule:
    """Single validation rule."""
    
    code: str
    field_path: str
    rule_type: str  # 'blocking', 'warning', 'info'
    condition: dict
    error_message: str
    itd_error_code: str | None
    
    def validate(self, itr_json: dict) -> ValidationError | None:
        """Execute validation rule."""
        pass

@dataclass
class ValidationEngine:
    """Collection of validation rules."""
    
    rules: List[ValidationRule]
    
    def validate_all(self, itr_json: dict) -> List[ValidationError]:
        """Run all validation rules."""
        errors = []
        for rule in self.rules:
            error = rule.validate(itr_json)
            if error:
                errors.append(error)
        return errors
```

### Example Rules

```python
# Validation rules for ITR-1

RULE_PAN_FORMAT = ValidationRule(
    code="PAN_001",
    field_path="PersonalInfo.PAN",
    rule_type="blocking",
    condition={"regex": "^[A-Z]{5}[0-9]{4}[A-Z]$"},
    error_message="Invalid PAN format",
    itd_error_code="PAN001"
)

RULE_SALARY_LIMIT = ValidationRule(
    code="ITR1_SAL_001",
    field_path="Schedules.ScheduleS.TotalSalary",
    rule_type="warning",
    condition={"max": 5000000000},  # ₹50Cr
    error_message="Salary exceeds ₹50Cr, consider ITR-2",
    itd_error_code=None
)

RULE_SINGLE_HP = ValidationRule(
    code="ITR1_HP_001",
    field_path="Schedules.ScheduleHP",
    rule_type="blocking",
    condition={"max_properties": 1},
    error_message="ITR-1 allows only 1 house property",
    itd_error_code="HP001"
)
```

### Usage

```python
validator = ValidationEngine.load(itr_type="ITR1", ay="2026-27")
errors = validator.validate_all(itr_json)
```

---

## Independent Versioning

### Tax Rules
- Version: By Assessment Year
- Change Frequency: Rarely (Finance Act amendments)
- Example: AY 2026-27 new regime slabs changed

### Schema Definitions
- Version: By ITR Type + AY + Version (v1.0, v1.1)
- Change Frequency: Yearly (CBDT schema updates)
- Example: ITR1 AY 2026-27 v1.1 added new field

### Validation Rules
- Version: By Rule Code + Effective Date
- Change Frequency: Frequently (circulars, notifications)
- Example: New validation added mid-year via notification

---

## Migration from Unified Rule Engine

### Step 1: Extract Tax Rules
```sql
INSERT INTO tax_rules (assessment_year, old_regime_slabs, ...)
SELECT assessment_year, slab_structure->>'old', ...
FROM rule_versions;
```

### Step 2: Create Schema Definitions
```sql
INSERT INTO itr_schemas (itr_type, assessment_year, json_schema, ...)
VALUES ('ITR1', '2026-27', '{"type": "object", ...}', ...);
```

### Step 3: Migrate Validation Rules
```sql
INSERT INTO validation_rules (rule_code, field_path, condition, ...)
VALUES ('PAN_001', 'PersonalInfo.PAN', '{"regex": "..."}', ...);
```

### Step 4: Update Code
```python
# OLD
rules = RuleEngine().get_rules(ay)
tax = compute_tax(income, rules)
itr_json = build_itr(filing, rules)
errors = validate(filing, rules)

# NEW
tax_rules = TaxRuleRepository().get(ay)
schema = SchemaRegistry().get(itr_type, ay)
validator = ValidationEngine.load(itr_type, ay)

tax = compute_tax(income, tax_rules)
itr_json = build_itr(schedules, schema)
errors = validator.validate_all(itr_json)
```

---

## Benefits

### 1. Independent Evolution
Tax rules stable while schema/validation evolve.

### 2. Clear Ownership
- Tax team owns tax_rules
- Schema team owns itr_schemas
- Compliance team owns validation_rules

### 3. Granular Updates
Update validation rules mid-year without touching tax logic.

### 4. Better Testing
```python
# Test tax computation
def test_tax_2026():
    rules = load_tax_rules("2026-27")
    assert compute_tax(1500000, "new", rules) == 117000

# Test schema mapping
def test_itr1_schema():
    schema = load_schema("ITR1", "2026-27")
    assert schema.map_field("salary.gross") == "Schedules.ScheduleS.GrossSalary"

# Test validation
def test_pan_validation():
    rule = RULE_PAN_FORMAT
    assert rule.validate({"PersonalInfo": {"PAN": "ABCDE1234F"}}) is None
    assert rule.validate({"PersonalInfo": {"PAN": "INVALID"}}) is not None
```

---

**Status:** CRITICAL ARCHITECTURE REFACTOR  
**Impact:** Enables independent evolution of tax/schema/validation  
**Implementation:** Phase 1 recommended (split before production)
