# Phase 3: ScheduleCG - Capital Gains

**Status:** PHASE 3 COMPLETE ✅  
**Started:** 2026-07-15  
**Completed:** 2026-07-15  
**Tests:** 41/41 passing (8 CG tests)  
**Reference:** `docs/SCHEDULE_PLANNING_MASTER.md`, `docs/TO_OPENTAX/04a_Schedule_Architecture.md`

---

## Overview

ScheduleCG handles Capital Gains from:
- Listed equities (stocks, mutual funds)
- Unlisted shares
- Bonds and debentures
- Real estate
- RSUs and stock options
- Other capital assets

## Capital Gains Categories

### By Duration
| Type | Holding Period | Notes |
|------|----------------|-------|
| STCG | < 1 year | Most assets |
| LTCG | >= 1 year | Listed equity, equity MF |
| STCG | < 2 years | Immovable property |
| LTCG | >= 2 years | Immovable property |

### By Tax Rate
| Section | Rate | Asset Type | Condition |
|---------|------|------------|-----------|
| 111A | 15% | Listed equity/MF (STCG) | STT paid |
| 112A | 10% | Listed equity (LTCG) | > ₹1.25L exempt |
| 112 | 20% | Other LTCG | With indexation |
| 112 | 20% | Listed securities | Without indexation |
| 115BB | 30% | Lottery/winnings | N/A |
| 115BBH | 30% | VDA/Crypto | N/A |
| 115BBE | 60% | Unexplained | N/A |

---

## Domain Models

### CGTransaction
Represents a single capital asset transaction.

### ScheduleCG
Aggregate for all capital gains in a filing.

---

## Implementation Files

```
backend/app/core/domain/schedules/
├── __init__.py
└── schedule_cg.py
```

---

## Status

**Implemented:**
- [x] CGTransaction data class with STT, FMV, broker fields
- [x] ScheduleCG base class with full rate bucket categorization
- [x] STCG/LTCG categorization with auto section detection
- [x] Section 111A (15% STCG listed equity)
- [x] Section 112A (12.5% LTCG listed equity, ₹1.25L exempt)
- [x] Section 112 (20% LTCG with/without indexation)
- [x] Section 115BB (lottery @ 30%)
- [x] Section 115BBH (VDA/crypto @ 30%)
- [x] Section 115BBE (unexplained income @ 60%)
- [x] Section 115BBJ (online gaming @ 30%)
- [x] CG rate bucket computation with exemption thresholds
- [x] CG special rate tax computation in tax engine
- [x] BEL (Basic Exemption Limit) for surcharge calculation
- [x] surcharge now uses BEL + CG LTCG for threshold
- [x] TaxBreakdown with CG fields (cg_total, cg_stcg, cg_ltcg, cg_special_rate_tax)
- [x] CGRateBucketBreakdown in ComputedReturn
- [x] 8 comprehensive CG test cases
- [x] ITR-1/4 LTCG112A correctly documented in planning doc
- [ ] Full ScheduleCGFor23 ITR-2 JSON generation (Phase 7)
- [ ] Schedule112A detailed schedule (Phase 7)
- [ ] ScheduleVDA/115AD domain models (Phase 7)
- [ ] DCG (Deemed CG) for ITR-3 (Phase 7)

---

**Next:** Phase 4 (ITR JSON Generation)
