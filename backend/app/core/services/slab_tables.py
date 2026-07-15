"""CBDT slab rates, deduction limits, and rule constants for AY 2026-27.

All values are hardcoded per the Finance Act 2025 (Finance (No.2) Act 2025 for AY 2025-26
continues with AY 2026-27 slabs per Finance Act 2025). Values are indexed by assessment year
for future AY switching via RuleVersion.

AY 2026-27 (Finance Act 2025):
  New Regime: 0-4L(0%), 4-8L(5%), 8-12L(10%), 12-16L(15%), 16-20L(20%), 20-24L(25%), 24L+(30%)
  Old Regime: 0-2.5L(0%), 2.5-5L(5%), 5-10L(20%), 10L+(30%)
  Rebate 87A: New(≤₹7L tax ≤₹25K), Old(≤₹5L income ≤₹12.5K)
  Std Deduction: New(₹75,000), Old(₹50,000)
  Surcharge: 10% (>50L), 15% (>1Cr), 25% (>2Cr), 37% old / 25% new (>5Cr)
  Cess: 4% on (tax after rebate + surcharge)
"""
from dataclasses import dataclass
from decimal import Decimal
from typing import Dict, List, Tuple


@dataclass(frozen=True)
class SlabEntry:
    """Single slab bracket."""
    lower: int          # Inclusive lower bound in rupees
    upper: int | None   # None = no upper limit
    rate: Decimal       # Rate as decimal e.g. Decimal("0.05")


@dataclass(frozen=True)
class DeductionLimits:
    """Chapter VI-A deduction limits."""
    section_80c: int = 150_000
    section_80ccc: int = 150_000
    combined_80cce: int = 150_000   # 80C + 80CCC + 80CCD(1) combined cap
    section_80ccd_1b: int = 50_000
    section_80ccd_2: int | None = None   # No limit
    section_80d_self: int = 25_000
    section_80d_self_senior: int = 50_000
    section_80d_parent: int = 25_000
    section_80d_parent_senior: int = 50_000
    section_80d_preventive_cap: int = 5_000
    section_80e: int | None = None    # No limit
    section_80g: int | None = None    # Capped at 50%/100% of adjusted GTI
    section_80tta: int = 10_000
    section_80ttb: int = 50_000       # Senior citizens
    section_80u: int = 75_000
    section_80u_severe: int = 125_000
    hp_loss_setoff: int = -200_000    # Section 24B cap


@dataclass(frozen=True)
class RuleVersion:
    """Complete tax rules for one AY."""
    ay: str
    new_regime_slabs: Tuple[SlabEntry, ...]
    old_regime_slabs_under_60: Tuple[SlabEntry, ...]
    old_regime_slabs_60_to_79: Tuple[SlabEntry, ...]
    old_regime_slabs_80_plus: Tuple[SlabEntry, ...]
    new_std_deduction: int
    old_std_deduction: int
    rebate_87a_new_max_income: int     # Max income for rebate 87A new regime
    rebate_87a_new_max_tax: int        # Max rebate amount new regime
    rebate_87a_old_max_income: int     # Max income for rebate 87A old regime
    rebate_87a_old_max_tax: int        # Max rebate amount old regime
    surcharge_thresholds: Tuple[Tuple[int, Decimal], ...]  # (threshold, rate)
    surcharge_new_thresholds: Tuple[Tuple[int, Decimal], ...]
    cess_rate: Decimal
    deductions: DeductionLimits


# ─── AY 2026-27 Rules ────────────────────────────────────────────────────────

RULES_AY_2026_27 = RuleVersion(
    ay="2026-27",

    # NEW REGIME (all ages) — Finance Act 2025
    new_regime_slabs=(
        SlabEntry(0, 4_00_000, Decimal("0")),
        SlabEntry(4_00_000, 8_00_000, Decimal("0.05")),
        SlabEntry(8_00_000, 12_00_000, Decimal("0.10")),
        SlabEntry(12_00_000, 16_00_000, Decimal("0.15")),
        SlabEntry(16_00_000, 20_00_000, Decimal("0.20")),
        SlabEntry(20_00_000, 24_00_000, Decimal("0.25")),
        SlabEntry(24_00_000, None, Decimal("0.30")),
    ),

    # OLD REGIME - Age < 60
    old_regime_slabs_under_60=(
        SlabEntry(0, 2_50_000, Decimal("0")),
        SlabEntry(2_50_000, 5_00_000, Decimal("0.05")),
        SlabEntry(5_00_000, 10_00_000, Decimal("0.20")),
        SlabEntry(10_00_000, None, Decimal("0.30")),
    ),

    # OLD REGIME - Age 60 to < 80
    old_regime_slabs_60_to_79=(
        SlabEntry(0, 3_00_000, Decimal("0")),
        SlabEntry(3_00_000, 5_00_000, Decimal("0.05")),
        SlabEntry(5_00_000, 10_00_000, Decimal("0.20")),
        SlabEntry(10_00_000, None, Decimal("0.30")),
    ),

    # OLD REGIME - Age 80+
    old_regime_slabs_80_plus=(
        SlabEntry(0, 5_00_000, Decimal("0")),
        SlabEntry(5_00_000, 10_00_000, Decimal("0.20")),
        SlabEntry(10_00_000, None, Decimal("0.30")),
    ),

    new_std_deduction=75_000,
    old_std_deduction=50_000,

    # Rebate 87A
    rebate_87a_new_max_income=7_00_000,   # Section 87A: income ≤ ₹7L → rebate ≤ ₹25K
    rebate_87a_new_max_tax=25_000,
    rebate_87a_old_max_income=5_00_000,    # Old regime: income ≤ ₹5L → rebate ≤ ₹12,500
    rebate_87a_old_max_tax=12_500,

    # Surcharge (old regime)
    surcharge_thresholds=(
        (50_00_000, Decimal("0.10")),
        (1_00_00_000, Decimal("0.15")),
        (2_00_00_000, Decimal("0.25")),
        (5_00_00_000, Decimal("0.37")),
    ),
    # Surcharge (new regime — no surcharge above 5Cr)
    surcharge_new_thresholds=(
        (50_00_000, Decimal("0.10")),
        (1_00_00_000, Decimal("0.15")),
        (2_00_00_000, Decimal("0.25")),
        (5_00_00_000, Decimal("0.25")),
    ),

    cess_rate=Decimal("0.04"),

    deductions=DeductionLimits(),
)

# ─── Lookup table ────────────────────────────────────────────────────────────

_AY_RULES: Dict[str, RuleVersion] = {
    "2026-27": RULES_AY_2026_27,
    "2025-26": RuleVersion(
        ay="2025-26",
        new_regime_slabs=(
            SlabEntry(0, 4_00_000, Decimal("0")),
            SlabEntry(4_00_000, 8_00_000, Decimal("0.05")),
            SlabEntry(8_00_000, 12_00_000, Decimal("0.10")),
            SlabEntry(12_00_000, 16_00_000, Decimal("0.15")),
            SlabEntry(16_00_000, 20_00_000, Decimal("0.20")),
            SlabEntry(20_00_000, 24_00_000, Decimal("0.25")),
            SlabEntry(24_00_000, None, Decimal("0.30")),
        ),
        old_regime_slabs_under_60=(
            SlabEntry(0, 2_50_000, Decimal("0")),
            SlabEntry(2_50_000, 5_00_000, Decimal("0.05")),
            SlabEntry(5_00_000, 10_00_000, Decimal("0.20")),
            SlabEntry(10_00_000, None, Decimal("0.30")),
        ),
        old_regime_slabs_60_to_79=(
            SlabEntry(0, 3_00_000, Decimal("0")),
            SlabEntry(3_00_000, 5_00_000, Decimal("0.05")),
            SlabEntry(5_00_000, 10_00_000, Decimal("0.20")),
            SlabEntry(10_00_000, None, Decimal("0.30")),
        ),
        old_regime_slabs_80_plus=(
            SlabEntry(0, 5_00_000, Decimal("0")),
            SlabEntry(5_00_000, 10_00_000, Decimal("0.20")),
            SlabEntry(10_00_000, None, Decimal("0.30")),
        ),
        new_std_deduction=75_000,
        old_std_deduction=75_000,
        rebate_87a_new_max_income=7_00_000,
        rebate_87a_new_max_tax=25_000,
        rebate_87a_old_max_income=5_00_000,
        rebate_87a_old_max_tax=12_500,
        surcharge_thresholds=(
            (50_00_000, Decimal("0.10")),
            (1_00_00_000, Decimal("0.15")),
            (2_00_00_000, Decimal("0.25")),
            (5_00_00_000, Decimal("0.37")),
        ),
        surcharge_new_thresholds=(
            (50_00_000, Decimal("0.10")),
            (1_00_00_000, Decimal("0.15")),
            (2_00_00_000, Decimal("0.25")),
            (5_00_00_000, Decimal("0.25")),
        ),
        cess_rate=Decimal("0.04"),
        deductions=DeductionLimits(),
    ),
}


def get_rules(ay: str) -> RuleVersion:
    """Get tax rules for an assessment year. Defaults to AY 2026-27."""
    return _AY_RULES.get(ay, RULES_AY_2026_27)


def compute_slab_tax(income: int, slabs: Tuple[SlabEntry, ...]) -> Tuple[List[Tuple[int, int, Decimal, int]], int]:
    """Compute slab tax and return per-slab breakdown.

    Args:
        income: Taxable income in rupees
        slabs: Slab definitions

    Returns:
        (slab_breakdown, total_tax)
        slab_breakdown: list of (lower, upper, rate, tax) per active slab
        total_tax: total tax in rupees (integer)
    """
    breakdown: List[Tuple[int, int, Decimal, int]] = []
    total_tax = 0
    remaining = income

    for slab in slabs:
        if remaining <= 0:
            break
        slab_width = (slab.upper - slab.lower) if slab.upper is not None else remaining
        taxable_in_slab = min(remaining, slab_width)
        slab_tax = int(taxable_in_slab * slab.rate)
        total_tax += slab_tax
        breakdown.append((
            slab.lower,
            slab.upper,
            slab.rate,
            slab_tax,
        ))
        remaining -= taxable_in_slab

    return breakdown, total_tax
