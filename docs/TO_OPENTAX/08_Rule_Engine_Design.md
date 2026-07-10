# Rule Engine Design

**Version:** 1.0  
**Pattern:** Strategy + Repository  
**Versioning:** Assessment Year Based

---

## Overview

The Rule Engine manages tax computation rules that vary by Assessment Year. Rules are versioned, immutable once an AY is filed, and support backward compatibility for historical filings.

---

## Architecture

```
┌──────────────────────────────────────┐
│      RuleEngineService               │
│  get_rules(ay: str) -> Rules         │
└────────────┬─────────────────────────┘
             │
             ▼
┌──────────────────────────────────────┐
│      RuleRepository                  │
│  load_from_db(ay) -> RuleVersion     │
└────────────┬─────────────────────────┘
             │
             ▼
┌──────────────────────────────────────┐
│      rule_versions table             │
│  ay | slab_structure | deduction_... │
└──────────────────────────────────────┘
```

---

## Rule Structure

### Rule Version Model

```python
# core/domain/rule_version.py

from dataclasses import dataclass
from decimal import Decimal
from typing import List, Optional

@dataclass
class TaxSlab:
    from_amount: Decimal
    to_amount: Optional[Decimal]
    rate: Decimal

@dataclass
class DeductionLimit:
    section: str
    max_amount: Decimal
    combined_with: List[str]
    age_dependent: bool
    senior_amount: Optional[Decimal] = None

@dataclass
class SurchargeRule:
    threshold: Decimal
    rate: Decimal
    marginal_relief: bool

@dataclass
class RuleVersion:
    assessment_year: str
    
    # Slabs (regime-specific)
    old_regime_slabs: dict[int, List[TaxSlab]]  # By age: <60, 60-79, >=80
    new_regime_slabs: List[TaxSlab]
    
    # Standard deductions
    old_standard_deduction: Decimal
    new_standard_deduction: Decimal
    
    # Rebate 87A
    old_rebate_limit: Decimal
    old_rebate_income_threshold: Decimal
    new_rebate_limit: Decimal
    new_rebate_income_threshold: Decimal
    
    # Surcharge
    surcharge_rules: List[SurchargeRule]
    
    # Cess
    cess_rate: Decimal
    
    # Deduction limits
    deduction_limits: dict[str, DeductionLimit]
    
    # Special rates (capital gains, VDA, lottery)
    special_rates: dict[str, Decimal]
    
    # Loss carry forward
    loss_cf_years: int
    hp_loss_setoff_limit: Decimal
    
    # LTCG 112A exemption
    ltcg_112a_exemption: Decimal
    
    active: bool = True
```

---

## Rule Repository

```python
# infra/database/repositories/rule_repository.py

from typing import Optional
from sqlalchemy import select
from infra.database.models.rule_version_model import RuleVersionModel
from core.domain.rule_version import RuleVersion

class RuleRepository:
    def __init__(self, session):
        self.session = session
    
    async def get_by_ay(self, ay: str) -> Optional[RuleVersion]:
        stmt = select(RuleVersionModel).where(
            RuleVersionModel.assessment_year == ay
        )
        result = await self.session.execute(stmt)
        model = result.scalar_one_or_none()
        
        if not model:
            return None
        
        return self._to_domain(model)
    
    async def save(self, rule: RuleVersion) -> None:
        model = self._to_model(rule)
        self.session.add(model)
        await self.session.commit()
    
    def _to_domain(self, model: RuleVersionModel) -> RuleVersion:
        return RuleVersion(
            assessment_year=model.assessment_year,
            old_regime_slabs=model.slab_structure["old"],
            new_regime_slabs=model.slab_structure["new"],
            old_standard_deduction=Decimal(
                model.deduction_limits["standard_deduction"]["old"]
            ),
            # ... map all fields
        )
```

---

## Rule Service

```python
# core/services/rule_engine_service.py

from functools import lru_cache
from typing import Optional
from core.domain.rule_version import RuleVersion
from infra.database.repositories.rule_repository import RuleRepository

class RuleEngineService:
    def __init__(self, repository: RuleRepository):
        self.repository = repository
    
    @lru_cache(maxsize=10)
    async def get_rules(self, ay: str) -> RuleVersion:
        """Get rules for assessment year (cached)."""
        rules = await self.repository.get_by_ay(ay)
        
        if not rules:
            raise RuleVersionNotFoundError(
                f"No rules found for AY {ay}"
            )
        
        return rules
    
    def get_slab_for_income(
        self, 
        rules: RuleVersion, 
        income: Decimal, 
        regime: str, 
        age: int
    ) -> List[TaxSlab]:
        """Get applicable tax slabs."""
        if regime == "new":
            return rules.new_regime_slabs
        else:
            if age >= 80:
                return rules.old_regime_slabs[80]
            elif age >= 60:
                return rules.old_regime_slabs[60]
            else:
                return rules.old_regime_slabs[0]
    
    def get_deduction_limit(
        self, 
        rules: RuleVersion, 
        section: str, 
        age: int
    ) -> Decimal:
        """Get deduction limit for a section."""
        limit = rules.deduction_limits.get(section)
        
        if not limit:
            return Decimal("0")
        
        if limit.age_dependent and age >= 60 and limit.senior_amount:
            return limit.senior_amount
        
        return limit.max_amount
```

---

## Seeding Rules

```python
# scripts/seed_rules.py

from decimal import Decimal
from core.domain.rule_version import RuleVersion, TaxSlab
from infra.database.repositories.rule_repository import RuleRepository

async def seed_ay_2026_27():
    rules = RuleVersion(
        assessment_year="2026-27",
        
        # Old regime slabs
        old_regime_slabs={
            0: [  # Age <60
                TaxSlab(Decimal("0"), Decimal("250000"), Decimal("0")),
                TaxSlab(Decimal("250000"), Decimal("500000"), Decimal("5")),
                TaxSlab(Decimal("500000"), Decimal("1000000"), Decimal("20")),
                TaxSlab(Decimal("1000000"), None, Decimal("30")),
            ],
            60: [  # Age 60-79
                TaxSlab(Decimal("0"), Decimal("300000"), Decimal("0")),
                TaxSlab(Decimal("300000"), Decimal("500000"), Decimal("5")),
                TaxSlab(Decimal("500000"), Decimal("1000000"), Decimal("20")),
                TaxSlab(Decimal("1000000"), None, Decimal("30")),
            ],
            80: [  # Age >=80
                TaxSlab(Decimal("0"), Decimal("500000"), Decimal("0")),
                TaxSlab(Decimal("500000"), Decimal("1000000"), Decimal("20")),
                TaxSlab(Decimal("1000000"), None, Decimal("30")),
            ],
        },
        
        # New regime slabs (AY 2026-27)
        new_regime_slabs=[
            TaxSlab(Decimal("0"), Decimal("400000"), Decimal("0")),
            TaxSlab(Decimal("400000"), Decimal("800000"), Decimal("5")),
            TaxSlab(Decimal("800000"), Decimal("1200000"), Decimal("10")),
            TaxSlab(Decimal("1200000"), Decimal("1600000"), Decimal("15")),
            TaxSlab(Decimal("1600000"), Decimal("2000000"), Decimal("20")),
            TaxSlab(Decimal("2000000"), Decimal("2400000"), Decimal("25")),
            TaxSlab(Decimal("2400000"), None, Decimal("30")),
        ],
        
        old_standard_deduction=Decimal("50000"),
        new_standard_deduction=Decimal("75000"),
        
        old_rebate_limit=Decimal("12500"),
        old_rebate_income_threshold=Decimal("500000"),
        new_rebate_limit=Decimal("60000"),
        new_rebate_income_threshold=Decimal("1200000"),
        
        surcharge_rules=[
            SurchargeRule(Decimal("5000000"), Decimal("0.10"), True),
            SurchargeRule(Decimal("10000000"), Decimal("0.15"), True),
            SurchargeRule(Decimal("20000000"), Decimal("0.25"), True),
            SurchargeRule(Decimal("50000000"), Decimal("0.37"), True),
        ],
        
        cess_rate=Decimal("0.04"),
        
        deduction_limits={
            "80C": DeductionLimit("80C", Decimal("150000"), ["80CCC", "80CCD1"], False),
            "80CCD1B": DeductionLimit("80CCD1B", Decimal("50000"), [], False),
            "80D": DeductionLimit("80D", Decimal("25000"), [], True, Decimal("50000")),
            "80TTA": DeductionLimit("80TTA", Decimal("10000"), [], False),
            "80TTB": DeductionLimit("80TTB", Decimal("50000"), [], False),
            # ... all other sections
        },
        
        special_rates={
            "STCG_111A_PRE": Decimal("15"),
            "STCG_111A_POST": Decimal("20"),
            "LTCG_112A_PRE": Decimal("10"),
            "LTCG_112A_POST": Decimal("12.5"),
            "LTCG_112": Decimal("20"),
            "LTCG_112_POST": Decimal("12.5"),
            "LOTTERY": Decimal("30"),
            "VDA": Decimal("30"),
            "GAMING": Decimal("30"),
            "UNEXPLAINED": Decimal("60"),
        },
        
        loss_cf_years=8,
        hp_loss_setoff_limit=Decimal("200000"),
        ltcg_112a_exemption=Decimal("125000"),
    )
    
    repo = RuleRepository(session)
    await repo.save(rules)
```

---

## Rule Validation

```python
# core/services/rule_validator.py

class RuleValidator:
    """Validate rule consistency."""
    
    def validate(self, rules: RuleVersion) -> List[str]:
        errors = []
        
        # Validate slabs are sequential
        for slabs in rules.old_regime_slabs.values():
            errors.extend(self._validate_slabs(slabs))
        
        errors.extend(self._validate_slabs(rules.new_regime_slabs))
        
        # Validate deduction limits
        if rules.deduction_limits["80C"].max_amount < Decimal("100000"):
            errors.append("80C limit too low")
        
        # Validate special rates
        if rules.special_rates["LOTTERY"] < Decimal("30"):
            errors.append("Lottery rate must be >= 30%")
        
        return errors
    
    def _validate_slabs(self, slabs: List[TaxSlab]) -> List[str]:
        errors = []
        
        for i, slab in enumerate(slabs):
            if slab.from_amount < 0:
                errors.append(f"Slab {i}: negative from_amount")
            
            if slab.to_amount and slab.to_amount <= slab.from_amount:
                errors.append(f"Slab {i}: to_amount <= from_amount")
            
            if i > 0 and slabs[i-1].to_amount != slab.from_amount:
                errors.append(f"Slab {i}: gap in slab ranges")
        
        return errors
```

---

## Rule Change Management

### Backward Compatibility

Rules are immutable once an AY is "locked" (at least one filing with status >= SUBMITTED).

```python
# core/services/rule_change_service.py

class RuleChangeService:
    async def can_modify_rules(self, ay: str) -> bool:
        """Check if rules for this AY can be modified."""
        filed_count = await self.filing_repo.count_submitted(ay)
        return filed_count == 0
    
    async def update_rules(
        self, 
        ay: str, 
        new_rules: RuleVersion
    ) -> None:
        """Update rules (only if no filings submitted)."""
        if not await self.can_modify_rules(ay):
            raise RulesLockedError(
                f"Cannot modify rules for AY {ay}: filings exist"
            )
        
        validator = RuleValidator()
        errors = validator.validate(new_rules)
        
        if errors:
            raise InvalidRulesError(errors)
        
        await self.rule_repo.save(new_rules)
```

### Rule Diff

```python
# core/services/rule_diff_service.py

class RuleDiffService:
    def diff(self, old: RuleVersion, new: RuleVersion) -> dict:
        """Compare two rule versions."""
        changes = []
        
        if old.new_regime_slabs != new.new_regime_slabs:
            changes.append({
                "field": "new_regime_slabs",
                "old": old.new_regime_slabs,
                "new": new.new_regime_slabs
            })
        
        if old.new_standard_deduction != new.new_standard_deduction:
            changes.append({
                "field": "new_standard_deduction",
                "old": int(old.new_standard_deduction),
                "new": int(new.new_standard_deduction)
            })
        
        # ... compare all fields
        
        return {"changes": changes}
```

---

## Usage Example

```python
# In TaxCalculationService

class TaxCalculationService:
    def __init__(self, rule_engine: RuleEngineService):
        self.rule_engine = rule_engine
    
    async def compute_tax(
        self, 
        filing: Filing, 
        regime: str
    ) -> ComputedReturn:
        # Get rules for this AY
        rules = await self.rule_engine.get_rules(filing.ay)
        
        # Get slabs
        slabs = self.rule_engine.get_slab_for_income(
            rules, filing.total_income, regime, filing.age
        )
        
        # Compute slab tax
        tax = self._apply_slabs(filing.total_income, slabs)
        
        # Get deduction limit
        limit_80c = self.rule_engine.get_deduction_limit(
            rules, "80C", filing.age
        )
        
        # Apply limit
        deduction = min(filing.deduction_80c, limit_80c)
        
        # ... rest of computation
```

---

## Database Schema

```sql
CREATE TABLE rule_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_year VARCHAR(7) NOT NULL UNIQUE,
    slab_structure JSONB NOT NULL,
    deduction_limits JSONB NOT NULL,
    special_rate_rules JSONB NOT NULL,
    surcharge_rules JSONB NOT NULL,
    rebate_rules JSONB NOT NULL,
    cess_rate DECIMAL(5,4) NOT NULL,
    loss_cf_years INTEGER NOT NULL DEFAULT 8,
    hp_loss_setoff_limit INTEGER NOT NULL DEFAULT 200000,
    ltcg_112a_exemption INTEGER NOT NULL DEFAULT 125000,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Example JSONB structure
{
  "old": {
    "0": [
      {"from": 0, "to": 250000, "rate": 0},
      {"from": 250000, "to": 500000, "rate": 5},
      {"from": 500000, "to": 1000000, "rate": 20},
      {"from": 1000000, "to": null, "rate": 30}
    ],
    "60": [...],
    "80": [...]
  },
  "new": [
    {"from": 0, "to": 400000, "rate": 0},
    ...
  ]
}
```

---

**Status:** DESIGN COMPLETE  
**Implementation:** Ready
