# Computation Engine Design

**Version:** 2.0  
**Pattern:** Pipeline + Strategy  
**Core Principle:** Single Canonical Computation Path  
**Updated:** 2026-07-15 — Owned engine replaces OpenTax runtime dependency

---

## Overview

The Computation Engine is the heart of the tax system. It takes domain schedules
(`ScheduleSalary`, `ScheduleHP`, etc.) and produces an immutable `ComputedReturn`
snapshot using our OWNED tax logic. OpenTax is used as a test oracle only.

Per ADR-022, OpenTax is NOT called at runtime for tax computation.

---

## Computation Pipeline

```
Input: Filing (with income heads, deductions, TDS) + Client (dob, pan)
  │
  ▼
┌─────────────────────────────────────┐
│ 1. Extract Schedules from Filing     │
│    (ScheduleSalary, ScheduleHP, etc.)│
└────────────┬────────────────────────┘
             ▼
┌─────────────────────────────────────┐
│ 2. Income Head Computation          │
│    ├─ Salary (Gross - Exemptions)   │
│    ├─ House Property (NAV - Int)    │
│    ├─ Capital Gains (Sale - Cost)   │
│    └─ Other Sources (Interest/Div)  │
└────────────┬────────────────────────┘
             ▼
┌─────────────────────────────────────┐
│ 3. Gross Total Income (GTI)         │
│    Sum all income heads             │
└────────────┬────────────────────────┘
             ▼
┌─────────────────────────────────────┐
│ 4. Chapter VI-A Deductions          │
│    Apply limits per section         │
│    (old regime only)                │
└────────────┬────────────────────────┘
             ▼
┌─────────────────────────────────────┐
│ 5. Total Income                     │
│    GTI - Deductions                 │
└────────────┬────────────────────────┘
             ▼
┌─────────────────────────────────────┐
│ 6. Tax Computation (OpenTax)        │
│    ├─ Slab tax                      │
│    ├─ Special rate tax (CG/VDA)     │
│    ├─ Surcharge                     │
│    ├─ Cess                          │
│    └─ Rebate 87A                    │
└────────────┬────────────────────────┘
             ▼
┌─────────────────────────────────────┐
│ 7. Interest & Late Fees             │
│    234A, 234B, 234C, 234F           │
└────────────┬────────────────────────┘
             ▼
┌─────────────────────────────────────┐
│ 8. Net Tax Payable                  │
│    Total Tax - TDS - TCS - Advance  │
└────────────┬────────────────────────┘
             ▼
┌─────────────────────────────────────┐
│ 9. Create Immutable Snapshot        │
│    Signed with snapshot_hash        │
└─────────────────────────────────────┘
```

---

## Core Service

```python
# core/services/computation_engine.py

from uuid import UUID
from decimal import Decimal
from core.domain.filing import Filing
from core.domain.computed_return import ComputedReturn
from core.interfaces.tax_engine import ITaxEngine
from core.services.canonical_normalizer import CanonicalNormalizer
from core.services.income_computer import IncomeComputer

class ComputationEngine:
    """
    Single source of truth for tax computation.
    All tax calculations flow through this engine.
    """
    
    def __init__(
        self,
        tax_engine: ITaxEngine,  # OpenTax adapter
        normalizer: CanonicalNormalizer,
        income_computer: IncomeComputer,
        rule_engine: RuleEngineService
    ):
        self.tax_engine = tax_engine
        self.normalizer = normalizer
        self.income_computer = income_computer
        self.rule_engine = rule_engine
    
    async def compute(
        self, 
        filing: Filing, 
        regime: str
    ) -> ComputedReturn:
        """
        Main computation entry point.
        Returns immutable snapshot of tax computation.
        """
        
        # Step 1: Get rules for this AY
        rules = await self.rule_engine.get_rules(filing.ay)
        
        # Step 2: Normalize all input data to canonical model
        canonical = self.normalizer.normalize_filing(filing)
        
        # Step 3: Compute income heads
        income_breakdown = self.income_computer.compute_all_heads(
            canonical, rules, regime
        )
        
        # Step 4: Compute total income
        gti = income_breakdown.total_gross_income()
        deductions = income_breakdown.total_deductions()
        total_income = gti - deductions
        
        # Step 5: Delegate to OpenTax for tax computation
        tax_result = await self.tax_engine.compute_tax(
            filing=filing,
            regime=regime,
            income=int(gti),
            deductions=int(deductions),
            income_breakdown=income_breakdown
        )
        
        # Step 6: Create immutable snapshot
        snapshot = ComputedReturn.create(
            filing_id=filing.id,
            regime=regime,
            payload=tax_result.to_dict(),
            rule_version=rules.assessment_year
        )
        
        return snapshot
```

---

## Income Head Computation

### Salary Income

```python
# core/services/income_heads/salary_computer.py

from decimal import Decimal
from core.domain.canonical_income import CanonicalSalaryIncome

class SalaryIncomeComputer:
    """Compute net salary income per ITR rules."""
    
    def compute(
        self, 
        salary_records: List[CanonicalSalaryIncome],
        regime: str,
        rules: RuleVersion
    ) -> Decimal:
        """
        Compute net salary income.
        
        Formula:
          Gross Salary
          - Allowances Exempt (10(13A), 10(5), etc.)
          - Standard Deduction (regime-dependent)
          - Professional Tax
          = Net Salary Income
        """
        total_gross = Decimal("0")
        total_exempt = Decimal("0")
        total_prof_tax = Decimal("0")
        
        for sal in salary_records:
            total_gross += sal.gross_salary
            total_exempt += sal.allowances_exempt
            total_prof_tax += sal.professional_tax
        
        # Standard deduction
        std_deduction = (
            rules.new_standard_deduction if regime == "new" 
            else rules.old_standard_deduction
        )
        
        net = total_gross - total_exempt - std_deduction - total_prof_tax
        return max(Decimal("0"), net)
```

### House Property Income

```python
# core/services/income_heads/house_property_computer.py

class HousePropertyComputer:
    """Compute house property income per Sec 22-27."""
    
    def compute(
        self, 
        properties: List[CanonicalHouseProperty],
        rules: RuleVersion
    ) -> Decimal:
        """
        Compute HP income.
        
        Formula per property:
          Gross Annual Value (rent received)
          - Municipal Taxes Paid
          = Net Annual Value (NAV)
          - 30% of NAV (standard deduction)
          - Interest on Borrowed Capital
          = Income from HP
        
        Loss set-off cap: ₹2L u/s 71(3A)
        """
        total_income = Decimal("0")
        
        for prop in properties:
            # NAV
            nav = prop.annual_rent - prop.municipal_taxes
            
            # Standard 30% deduction (only if NAV > 0)
            std_ded = nav * Decimal("0.30") if nav > 0 else Decimal("0")
            
            # Interest
            interest = prop.interest_paid
            
            # Income (can be negative)
            income = nav - std_ded - interest
            
            # Apply ownership share
            income *= (prop.ownership_share / Decimal("100"))
            
            total_income += income
        
        # Loss set-off cap
        if total_income < 0:
            return max(total_income, -rules.hp_loss_setoff_limit)
        
        return total_income
```

### Capital Gains

```python
# core/services/income_heads/capital_gains_computer.py

class CapitalGainsComputer:
    """Compute capital gains with special rate handling."""
    
    def compute(
        self, 
        transactions: List[CanonicalCGTransaction],
        rules: RuleVersion
    ) -> CapitalGainsBreakdown:
        """
        Compute CG, categorized by holding period and rate.
        
        Categories:
          - STCG Slab (< 12 months, non-listed)
          - STCG 111A (@15%/20%, listed equity STT paid)
          - LTCG 112A (@10%/12.5%, listed equity STT paid)
          - LTCG 112 (@20%/12.5%, other assets)
          - VDA (@30%)
          - Lottery/Gaming (@30%)
        """
        breakdown = CapitalGainsBreakdown()
        
        for txn in transactions:
            # Holding period
            months_held = self._months_between(
                txn.purchase_date, txn.sale_date
            )
            
            # Compute gain
            sale_value = txn.sale_price
            cost = self._compute_cost(txn, rules)
            gain = sale_value - cost
            
            if gain <= 0:
                # Loss (handle separately)
                continue
            
            # Categorize
            if txn.asset_type == "listed_equity":
                if months_held <= 12:
                    # STCG 111A
                    if txn.sale_date < date(2024, 7, 23):
                        breakdown.stcg_111a_15 += gain
                    else:
                        breakdown.stcg_111a_20 += gain
                else:
                    # LTCG 112A
                    if txn.sale_date < date(2024, 7, 23):
                        breakdown.ltcg_112a_10 += gain
                    else:
                        breakdown.ltcg_112a_12_5 += gain
            
            elif txn.asset_type == "vda":
                breakdown.vda_30 += gain
            
            elif txn.asset_type == "lottery":
                breakdown.lottery_30 += gain
            
            # ... other categories
        
        return breakdown
    
    def _compute_cost(
        self, 
        txn: CanonicalCGTransaction, 
        rules: RuleVersion
    ) -> Decimal:
        """
        Compute indexed/non-indexed cost.
        
        Post Jul 23, 2024: No indexation for new transactions
        Pre Jul 23, 2024: Indexation allowed for non-equity
        """
        base_cost = txn.purchase_price + txn.transfer_expenses
        
        if txn.sale_date < date(2024, 7, 23):
            # Indexation allowed
            if txn.asset_type in ["real_estate", "bonds"]:
                cii_purchase = self._get_cii(txn.purchase_date.year)
                cii_sale = self._get_cii(txn.sale_date.year)
                indexed_cost = base_cost * (cii_sale / cii_purchase)
                return indexed_cost
        
        return base_cost
```

---

## Deduction Computer

```python
# core/services/deduction_computer.py

class DeductionComputer:
    """Compute Chapter VI-A deductions with limits."""
    
    def compute(
        self, 
        deductions: CanonicalDeductions,
        regime: str,
        rules: RuleVersion,
        age: int
    ) -> DeductionBreakdown:
        """
        Compute all deductions.
        
        New regime: ONLY 80CCD(2) allowed
        Old regime: All sections apply with limits
        """
        if regime == "new":
            return DeductionBreakdown(
                section_80ccd2=deductions.employer_nps
            )
        
        # Old regime: apply all limits
        breakdown = DeductionBreakdown()
        
        # 80CCE combined limit (80C + 80CCC + 80CCD1)
        total_80cce = (
            deductions.section_80c +
            deductions.section_80ccc +
            deductions.section_80ccd1
        )
        breakdown.section_80cce = min(
            total_80cce,
            rules.get_deduction_limit("80C", age)
        )
        
        # 80CCD(1B) additional ₹50K
        breakdown.section_80ccd1b = min(
            deductions.section_80ccd1b,
            rules.get_deduction_limit("80CCD1B", age)
        )
        
        # 80CCD(2) no limit
        breakdown.section_80ccd2 = deductions.section_80ccd2
        
        # 80D (age-dependent)
        breakdown.section_80d = self._compute_80d(
            deductions.section_80d, age, rules
        )
        
        # ... all other sections
        
        return breakdown
```

---

## Rounding Rules

```python
# core/services/rounding_service.py

from decimal import Decimal, ROUND_HALF_EVEN

class RoundingService:
    """CBDT-compliant rounding rules."""
    
    @staticmethod
    def round_to_nearest_rupee(amount: Decimal) -> int:
        """
        Round to nearest rupee using banker's rounding.
        
        Examples:
          1234.50 → 1234 (round to even)
          1234.51 → 1235
          1235.50 → 1236 (round to even)
        """
        return int(amount.quantize(Decimal("1"), rounding=ROUND_HALF_EVEN))
    
    @staticmethod
    def round_to_nearest_ten(amount: Decimal) -> int:
        """
        Round to nearest ₹10 (ITR filing requirement).
        
        Examples:
          1234 → 1230
          1235 → 1240 (round to even ten)
        """
        rounded = (amount / Decimal("10")).quantize(
            Decimal("1"), rounding=ROUND_HALF_EVEN
        )
        return int(rounded) * 10
    
    @staticmethod
    def round_percentage(rate: Decimal, precision: int = 2) -> Decimal:
        """Round percentage rates."""
        return rate.quantize(Decimal(f"0.{'0' * precision}"))
```

---

## Computation Validation

```python
# core/services/computation_validator.py

class ComputationValidator:
    """Validate computed results for sanity."""
    
    def validate(self, computed: ComputedReturn) -> List[str]:
        """Run sanity checks on computation."""
        errors = []
        
        payload = computed.payload
        
        # Basic sanity
        if payload["total_income"] < 0:
            errors.append("Total income cannot be negative")
        
        if payload["total_tax_liability"] < 0:
            errors.append("Tax liability cannot be negative")
        
        # Cess must be 4% of (tax + surcharge)
        base = payload["tax_after_rebate"] + payload["surcharge"]
        expected_cess = int(base * Decimal("0.04"))
        actual_cess = payload["cess"]
        
        if abs(expected_cess - actual_cess) > 1:
            errors.append(
                f"Cess mismatch: expected {expected_cess}, got {actual_cess}"
            )
        
        # Rebate cannot exceed tax
        if payload["rebate_87a"] > payload["tax_before_rebate"]:
            errors.append("Rebate exceeds tax before rebate")
        
        # Net balance check
        total_paid = (
            payload["tds"] + 
            payload["tcs"] + 
            payload["advance_tax"]
        )
        net = payload["total_tax_liability"] - total_paid
        
        if net > 0 and payload["tax_payable"] != net:
            errors.append("Tax payable mismatch")
        
        if net < 0 and payload["refund"] != -net:
            errors.append("Refund mismatch")
        
        return errors
```

---

## Dual Regime Comparison

```python
# core/services/regime_comparator.py

class RegimeComparator:
    """Compare old vs new regime."""
    
    async def compare(
        self, 
        filing: Filing
    ) -> RegimeComparison:
        """
        Compute both regimes and compare.
        Returns which regime is better.
        """
        # Compute both
        old_result = await self.engine.compute(filing, "old")
        new_result = await self.engine.compute(filing, "new")
        
        # Compare
        old_tax = old_result.payload["total_tax_liability"]
        new_tax = new_result.payload["total_tax_liability"]
        
        savings = old_tax - new_tax
        better_regime = "new" if savings > 0 else "old"
        
        return RegimeComparison(
            old_regime=old_result,
            new_regime=new_result,
            better_regime=better_regime,
            savings=abs(savings),
            comparison_items=[
                ComparisonItem(
                    "Gross Total Income",
                    old_result.payload["gross_total_income"],
                    new_result.payload["gross_total_income"]
                ),
                ComparisonItem(
                    "Deductions",
                    old_result.payload["total_deductions"],
                    new_result.payload["total_deductions"]
                ),
                # ... all other fields
            ]
        )
```

---

## Performance Optimization

### Caching

```python
# Use LRU cache for rule lookups
@lru_cache(maxsize=10)
async def get_rules(ay: str) -> RuleVersion:
    return await rule_repo.get_by_ay(ay)

# Cache canonical normalization within request
@lru_cache(maxsize=100)
def normalize_filing(filing_id: UUID) -> CanonicalModel:
    return normalizer.normalize(filing)
```

### Parallel Computation

```python
# Compute both regimes in parallel
import asyncio

async def compute_both_regimes(filing: Filing):
    old_task = compute_engine.compute(filing, "old")
    new_task = compute_engine.compute(filing, "new")
    
    old_result, new_result = await asyncio.gather(old_task, new_task)
    return old_result, new_result
```

---

## Error Handling

```python
class ComputationError(Exception):
    """Base for computation errors."""

class IncomeComputationError(ComputationError):
    """Error computing income head."""

class DeductionLimitExceededError(ComputationError):
    """Deduction exceeds allowed limit."""

class NegativeTaxError(ComputationError):
    """Computed negative tax (logic error)."""
```

---

**Status:** DESIGN COMPLETE  
**Integration:** OpenTax vendored for core tax logic  
**Performance:** < 500ms per computation
