# CRITICAL TAX SLAB FIX - AY 2025-26

## Issue Found
Tax slabs for AY 2025-26 New Regime were using AY 2026-27 slabs (4L-8L-12L...).

## Fixed
**AY 2025-26 New Regime** (corrected):
- 0-3L: 0%
- 3L-7L: 5%
- 7L-10L: 10%
- 10L-12L: 15%
- 12L-15L: 20%
- >15L: 30%

**Your income ₹7,86,730 - Correct calculation**:
- 0-3L: ₹0
- 3L-7L: ₹4L × 5% = ₹20,000
- 7L-7.87L: ₹86,730 × 10% = ₹8,673
- **Tax: ₹28,673**
- Rebate: ₹0 (income > ₹7L)
- Cess: ₹1,147
- **Total: ₹29,820**

## Frontend Integration Required
Call `/api/integration/autopopulate/prefill` after importing JSON to apply employer/TDS data.
