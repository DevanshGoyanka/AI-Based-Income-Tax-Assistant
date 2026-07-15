# Phase 3: ScheduleCG - Capital Gains

**Status:** PHASE 3 INTEGRATION COMPLETE ✅  
**Started:** 2026-07-15  
**Completed:** 2026-07-15  
**Reference:** `docs/TO_OPENTAX/04a_Schedule_Architecture.md`

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
- [x] CGTransaction data class
- [x] ScheduleCG base class
- [x] STCG/LTCG categorization
- [x] Section 111A (15% STCG)
- [x] Section 112A (10%/12.5% LTCG)
- [x] Section 112 (20% LTCG)
- [ ] Section 115BB (lottery)
- [ ] Section 115BBH (VDA)
- [ ] Section 115BBE (unexplained)
- [ ] Real estate capital gains
- [ ] Indexation benefit calculation

---

**Next:** Phase 4 (ITR JSON Generation)
