# Schedule Architecture Design

**Version:** 1.1 (Critical Update)  
**Date:** 2026-07-10  
**Pattern:** Domain-Driven Schedule Abstraction

---

## Overview

ITR forms are composed of **Schedules** - standardized sections that are reusable across ITR-1 through ITR-7. This architecture treats Schedules as first-class domain objects.

---

## Schedule Hierarchy

```
Schedule (Abstract Base)
├── ScheduleSalary      # All ITRs
├── ScheduleHP          # All ITRs
├── ScheduleCG          # ITR-2+
├── ScheduleOS          # All ITRs
├── ScheduleBP          # ITR-3+ (Business/Profession)
├── ScheduleESR         # ITR-3+ (Presumptive)
├── ScheduleTDS         # All ITRs
├── ScheduleIT          # All ITRs (Tax payments)
├── ScheduleVIA         # All ITRs (Deductions)
├── ScheduleBA          # All ITRs (Bank accounts)
├── ScheduleAL          # ITR-2+ (Assets/Liabilities)
└── ScheduleFA          # ITR-2+ (Foreign assets)
```

---

## Core Schedule Entity

```python
# core/domain/schedule.py

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Generic, TypeVar
from uuid import UUID
from decimal import Decimal

T = TypeVar('T')

@dataclass
class Schedule(ABC, Generic[T]):
    """Abstract base for all ITR schedules."""
    
    id: UUID
    filing_id: UUID
    ay: str
    
    @abstractmethod
    def compute_income(self) -> Decimal:
        """Compute total income from this schedule."""
        pass
    
    @abstractmethod
    def to_itr_json(self) -> dict:
        """Export to ITR JSON format."""
        pass
    
    @abstractmethod
    def validate(self) -> List[ValidationError]:
        """Validate schedule data."""
        pass
```

---

## Concrete Schedules

### ScheduleSalary

```python
# core/domain/schedules/schedule_salary.py

@dataclass
class SalaryDetail:
    employer_name: str
    employer_tan: str
    gross_salary: Money
    allowances_exempt: Money
    professional_tax: Money
    tds_deducted: Money

@dataclass
class ScheduleSalary(Schedule[List[SalaryDetail]]):
    """Schedule - Salary Income (Section 17)"""
    
    employers: List[SalaryDetail]
    
    def compute_income(self) -> Decimal:
        """
        Net salary = Gross - Allowances - Std Deduction - Prof Tax
        """
        total_gross = sum(e.gross_salary.to_rupees() for e in self.employers)
        total_exempt = sum(e.allowances_exempt.to_rupees() for e in self.employers)
        total_prof_tax = sum(e.professional_tax.to_rupees() for e in self.employers)
        
        # Standard deduction applied at computation time (regime-dependent)
        return total_gross - total_exempt - total_prof_tax
    
    def to_itr_json(self) -> dict:
        """ITR1/ITR2 Schedule Salary format."""
        return {
            "Schedules": {
                "ScheduleS": {
                    "SalaryDetails": [
                        {
                            "EmployerName": emp.employer_name,
                            "TAN": emp.employer_tan,
                            "GrossSalary": int(emp.gross_salary.to_rupees()),
                            "AllowancesExempt": int(emp.allowances_exempt.to_rupees()),
                            # ... full ITR schema mapping
                        }
                        for emp in self.employers
                    ]
                }
            }
        }
```

### ScheduleHP

```python
# core/domain/schedules/schedule_hp.py

@dataclass
class HPDetail:
    address: str
    ownership_share: Decimal
    annual_rent: Money
    municipal_taxes: Money
    interest_paid: Money
    co_owners: List[str]

@dataclass
class ScheduleHP(Schedule[List[HPDetail]]):
    """Schedule - House Property Income (Section 22-27)"""
    
    properties: List[HPDetail]
    
    def compute_income(self) -> Decimal:
        """
        Per property:
          NAV = Rent - Municipal Tax
          Income = NAV - 30% of NAV - Interest
        Total capped at -₹2L loss (Sec 71(3A))
        """
        total = Decimal("0")
        
        for prop in self.properties:
            nav = prop.annual_rent.to_rupees() - prop.municipal_taxes.to_rupees()
            std_ded = nav * Decimal("0.30") if nav > 0 else Decimal("0")
            interest = prop.interest_paid.to_rupees()
            income = (nav - std_ded - interest) * prop.ownership_share
            total += income
        
        # Loss set-off cap
        return max(total, Decimal("-200000"))
    
    def to_itr_json(self) -> dict:
        return {
            "Schedules": {
                "ScheduleHP": {
                    "PropertyDetails": [
                        {
                            "Address": prop.address,
                            "OwnershipShare": float(prop.ownership_share),
                            "AnnualRent": int(prop.annual_rent.to_rupees()),
                            # ... full ITR schema
                        }
                        for prop in self.properties
                    ]
                }
            }
        }
```

### ScheduleCG

```python
# core/domain/schedules/schedule_cg.py

@dataclass
class CGTransaction:
    asset_type: str  # "listed_equity", "real_estate", "bonds"
    purchase_date: date
    sale_date: date
    sale_price: Money
    purchase_price: Money
    transfer_expenses: Money
    indexed_cost: Money | None
    
@dataclass
class ScheduleCG(Schedule[List[CGTransaction]]):
    """Schedule - Capital Gains (Section 45-55A)"""
    
    transactions: List[CGTransaction]
    
    def compute_income(self) -> Decimal:
        """Compute total CG (categorized by rate)."""
        # Delegate to CG calculator (rate-specific logic)
        return sum(
            self._compute_gain(txn) 
            for txn in self.transactions
        )
    
    def categorize_by_rate(self) -> dict[str, Decimal]:
        """
        Returns:
          {"stcg_111a_15": amount, "ltcg_112a_10": amount, ...}
        """
        # Complex categorization logic
        pass
```

---

## ITR Builder Uses Schedules

```python
# core/services/itr_builder.py

class ITRBuilder:
    """Builds ITR JSON from schedules (not from Filing directly)."""
    
    def build_itr1(
        self, 
        schedules: List[Schedule],
        computed: ComputedReturn
    ) -> dict:
        """
        ITR-1 uses:
          - ScheduleSalary
          - ScheduleHP (max 1 property)
          - ScheduleOS
          - ScheduleTDS
          - ScheduleVIA
          - ScheduleBA
        """
        itr = {
            "ITR": {
                "ITR1": {
                    "PartA_GEN1": self._build_personal_info(),
                    "Schedules": {}
                }
            }
        }
        
        for schedule in schedules:
            # Each schedule knows how to export itself
            schedule_json = schedule.to_itr_json()
            itr["ITR"]["ITR1"]["Schedules"].update(schedule_json["Schedules"])
        
        # Add computation results
        itr["ITR"]["ITR1"]["TaxComputation"] = computed.to_itr_json()
        
        return itr
    
    def build_itr2(
        self, 
        schedules: List[Schedule],
        computed: ComputedReturn
    ) -> dict:
        """
        ITR-2 uses same schedules + ScheduleCG, ScheduleAL
        """
        # Same pattern, more schedules allowed
        pass
```

---

## Benefits of Schedule Architecture

### 1. Reusability
```python
# Same ScheduleSalary used in ITR-1, ITR-2, ITR-3
salary_schedule = ScheduleSalary(...)
itr1_builder.build(schedules=[salary_schedule, ...])
itr2_builder.build(schedules=[salary_schedule, cg_schedule, ...])
```

### 2. Independent Testing
```python
def test_schedule_salary_computation():
    schedule = ScheduleSalary(
        employers=[SalaryDetail(gross=Money(1500000), ...)]
    )
    assert schedule.compute_income() == Decimal("1422600")
```

### 3. Clear Validation
```python
def test_itr1_cannot_have_multiple_hp():
    schedules = [
        ScheduleHP(properties=[prop1]),
        ScheduleHP(properties=[prop2]),  # Invalid for ITR-1
    ]
    
    errors = ITR1Validator().validate(schedules)
    assert "ITR-1 allows max 1 house property" in errors
```

### 4. Easy ITR Form Extension
```python
# To add ITR-3 support:
class ITR3Builder:
    def build(self, schedules: List[Schedule]) -> dict:
        # Accepts all ITR-1/2 schedules + ScheduleBP
        pass
```

---

## Filing Aggregate Refactored

```python
# core/domain/filing.py

@dataclass
class Filing:
    """Filing is now a container for Schedules."""
    
    id: UUID
    client_id: UUID
    ay: str
    regime: str
    status: FilingStatus
    schedules: dict[str, Schedule]  # Key: schedule type
    
    def add_schedule(self, schedule: Schedule) -> None:
        """Add/replace schedule."""
        schedule_type = type(schedule).__name__
        self.schedules[schedule_type] = schedule
    
    def get_schedule(self, schedule_type: type[T]) -> T | None:
        """Type-safe schedule retrieval."""
        return self.schedules.get(schedule_type.__name__)
    
    def compute_gti(self) -> Decimal:
        """Gross Total Income = sum of all income schedules."""
        income_schedules = [
            "ScheduleSalary", "ScheduleHP", 
            "ScheduleCG", "ScheduleOS", "ScheduleBP"
        ]
        
        total = Decimal("0")
        for sched_type in income_schedules:
            if sched_type in self.schedules:
                total += self.schedules[sched_type].compute_income()
        
        return total
```

---

## Migration Path

### Phase 1: Add Schedule Classes
Create all schedule domain entities (backward compatible).

### Phase 2: Dual Write
Filing still has `salary_income`, `house_property` fields, but ALSO builds schedules internally.

### Phase 3: Adapter Layer
OpenTax adapter maps Schedules → OpenTax FilingModel (not Filing → FilingModel).

### Phase 4: Remove Old Fields
Delete `salary_income`, `house_property` from Filing. Pure schedule-based.

---

**Status:** CRITICAL ARCHITECTURE UPDATE  
**Impact:** Foundation for ITR-2/3/4/5/6/7 support  
**Implementation:** Phase 1 recommended before production
