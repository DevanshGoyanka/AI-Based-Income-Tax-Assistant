# ITR ERP — FINAL COMPLETION DIRECTIVE
## Closing the Remaining 58% — Audit-Driven, Zero-Ambiguity Implementation Guide
**Date:** April 11, 2026  
**Prerequisite State:** Phase 6 partial — 42% CBDT compliant, 145 files compiled  
**Target:** 100% CBDT compliant, fully deployable, all ITR forms end-to-end  

---

## READ THIS FIRST — WHY PREVIOUS ATTEMPTS STALLED

The audit identified **five root causes** of the 58% gap. Every section of this directive maps to one of them.

| Root Cause | Audit Finding | This Directive Section |
|---|---|---|
| **Itr2FormData under-extended** | Only 5 of 30+ directive fields added (15% compliance) | Section 1 |
| **Validation logic too shallow** | Generic `addWarning()` instead of arithmetic checks | Sections 2–4 |
| **VDA/SFT placeholders untouched** | Original stubs still in place | Section 5 |
| **Orchestration service missing** | No end-to-end pipeline | Section 6 |
| **Persistence layer missing** | No LossLedger DB, no AuditTrail DB | Section 7 |

**Execution contract:** Complete each section in order. Compile after every section. If a section fails to compile, fix the missing dependency before moving forward — **do not delete any service**. The delete-instead-of-fix pattern is the cause of all prior failures.

---

## CURRENT STATE INVENTORY (as of audit)

### ✅ DO NOT TOUCH — Working:
- `AY202627TaxComputationService.java` — AY 2026-27 slabs, Rs 12L rebate, marginal relief
- `LossSetOffEngine.java` — CYLA/BFLA matrix, 8-year limits
- `DepreciationEngine.java` — DPM block-of-assets, half-rate rule
- `ITR2Output.java` — ITR-2 JSON structure
- `ITRSharedDtos.java` — All 12 shared DTOs (PropertyDetail, ScheduleCGData, etc.)
- `ITRBusinessDtos.java` — All business DTOs (MSMEPayment, FandODetail, etc.)
- `Itr3FormData.java` — 20+ Phase 6 fields added (95% compliant)
- `Itr4FormData.java` — 90% compliant
- `ITR1CBDTValidationService.java` — 99 rules, production-ready, untouched
- All Phase 1–4 services

### ❌ INCOMPLETE — Fix in This Directive:
- `Itr2FormData.java` — Only 5 fields added; 25+ fields missing (Section 1)
- `ITR2CBDTValidationService.java` — Generic warnings only; no arithmetic checks (Section 2)
- `ITR3CBDTValidationService.java` — Missing detailed BP/DPM/DCG checks (Section 3)
- `ITR4CBDTValidationService.java` — Warning-based; should be error-enforcement (Section 4)
- `VDATransactionService.java` — Original placeholder stub (Section 5.1)
- `SFTProcessingService.java` — Stub methods not implemented (Section 5.2)
- `ITRFilingOrchestrationService.java` — Does not exist (Section 6)
- `LossLedger.java` + `LossLedgerRepository.java` + migration — Do not exist (Section 7)

---

## SECTION 1 — COMPLETE `Itr2FormData.java` EXTENSION

**Current compliance: 15%. Target: 100%.**

Open `Itr2FormData.java`. The audit confirms only these 5 fields exist:
`taxRegime`, `assessmentYear`, `isHUF`, `reliefUs89A`, `broughtForwardLosses`.

Add **all fields below** that are absent. Do not remove anything.

```java
// ─── MISSING FIELDS — ADD TO Itr2FormData.java ─────────────────────────────
// All DTOs referenced below exist in ITRSharedDtos.java (already created)

import com.yourpackage.dto.ITRSharedDtos.*;
import java.util.*;

// ── House Property (MISSING) ─────────────────────────────────────────────────
private List<PropertyDetail> houseProperties = new ArrayList<>();

// ── Capital Gains (MISSING) ──────────────────────────────────────────────────
private ScheduleCGData scheduleCG = new ScheduleCGData();

// ── VDA (MISSING) ────────────────────────────────────────────────────────────
private List<VDATransaction> vdaTransactions = new ArrayList<>();

// ── Loss Schedules (MISSING) ─────────────────────────────────────────────────
private ScheduleCYLAData scheduleCYLA;
private ScheduleBFLAData scheduleBFLA;
private ScheduleCFLData scheduleCFL;

// ── Chapter VI-A Donations (MISSING) ─────────────────────────────────────────
private Schedule80GData schedule80G;

// ── AMT Fields (MISSING) ─────────────────────────────────────────────────────
private boolean amtApplicable = false;
private int adjustedTotalIncomeForAMT = 0;
private int amtTax = 0;
private int amtCreditBroughtForward = 0;
private int amtCreditUtilized = 0;
private int amtCreditCarriedForward = 0;
private int regularTaxBeforeCess = 0; // needed by validateAMT()

// ── Foreign Assets (MISSING) ─────────────────────────────────────────────────
private boolean hasForeignAssets = false;
private String scheduleFA;   // FA JSON as string — accept verbatim
private String scheduleFSI;  // FSI JSON as string
private String scheduleTR;   // TR JSON as string

// ── Schedule AL (MISSING) ────────────────────────────────────────────────────
private ScheduleALData scheduleAL;  // mandatory if total income > Rs 50L

// ── Clubbing (MISSING) ───────────────────────────────────────────────────────
private boolean hasClubbing = false;

// ── Special Rate Income — separate pre/post-July 2023 buckets (MISSING) ──────
private int stcg111A_preJul23 = 0;   // at 15%
private int stcg111A_postJul23 = 0;  // at 20%
private int ltcg112A_preJul23 = 0;   // at 10%, exemption Rs 1,00,000
private int ltcg112A_postJul23 = 0;  // at 12.5%, exemption Rs 1,25,000
private int vdaIncome = 0;           // at 30%, no set-off allowed
private int lotteryIncome = 0;       // at 30%
```

After adding these fields, generate getters and setters following your existing project convention. Compile now:
```bash
mvn compile -pl . 2>&1 | grep "ERROR"
# Must produce zero lines of output
```

---

## SECTION 2 — STRENGTHEN `ITR2CBDTValidationService.java`

**Current compliance: 40%. The service exists but uses generic warnings instead of field-level arithmetic checks. Replace every `addWarning()` with the precise rule check below.**

This service was partially implemented. Do not delete it. Open it and replace the body of each validation method per the code below.

### 2.1 Full HP Validation (replaces simplified version)

```java
private void validateScheduleHP(Itr2FormData data, ValidationResult result) {
    List<PropertyDetail> props = data.getHouseProperties();
    if (props == null || props.isEmpty()) return;

    long sopCount = props.stream()
        .filter(p -> p.type == PropertyType.SELF_OCCUPIED).count();
    if (sopCount > 2) {
        result.addError("VR2-HP-011",
            "Only 2 properties can be Self-Occupied. Found " + sopCount +
            ". Additional properties must be Deemed Let-Out.");
    }

    for (PropertyDetail p : props) {
        // VR2-HP-001: Standard deduction = exactly 30% of NAV
        int expectedSD = (int) Math.round(0.30 * p.netAnnualValue);
        if (p.standardDeduction != expectedSD) {
            result.addError("VR2-HP-001",
                "HP [" + p.address + "]: Standard deduction must be 30% of NAV = Rs " +
                expectedSD + ". Declared: Rs " + p.standardDeduction);
        }

        // VR2-HP-002: Co-owner shares must total 100%
        if (p.isCoOwned && (p.ownershipSharePct + p.coOwnerSharePct) != 100) {
            result.addError("VR2-HP-002",
                "HP [" + p.address + "]: Ownership shares total " +
                (p.ownershipSharePct + p.coOwnerSharePct) + "%. Must be 100%.");
        }

        // VR2-HP-004: No interest claim with 0% ownership
        if (p.ownershipSharePct == 0 && p.interestOnBorrowedCapital > 0) {
            result.addError("VR2-HP-004",
                "HP [" + p.address + "]: Cannot claim interest on borrowed capital with 0% ownership.");
        }

        // VR2-HP-005: No municipal tax if GAV = 0
        if (p.grossAnnualValue == 0 && p.municipalTax > 0) {
            result.addError("VR2-HP-005",
                "HP [" + p.address + "]: Municipal tax Rs " + p.municipalTax +
                " cannot be claimed when Gross Annual Value is zero.");
        }

        // VR2-HP-006: Old regime SOP interest cap = Rs 2,00,000
        if (data.getTaxRegime() == TaxRegime.OLD &&
            p.type == PropertyType.SELF_OCCUPIED &&
            p.interestOnBorrowedCapital > 200000) {
            result.addError("VR2-HP-006",
                "HP [" + p.address + "]: SOP interest deduction capped at Rs 2,00,000 (Old Regime). " +
                "Declared: Rs " + p.interestOnBorrowedCapital);
        }

        // VR2-HP-007: New regime SOP interest = 0
        if (data.getTaxRegime() == TaxRegime.NEW &&
            p.type == PropertyType.SELF_OCCUPIED &&
            p.interestOnBorrowedCapital > 0) {
            result.addError("VR2-HP-007",
                "HP [" + p.address + "]: No interest deduction allowed for Self-Occupied property under New Regime.");
        }

        // VR2-HP-008: Let-out/Deemed Let-out must have GAV > 0
        if ((p.type == PropertyType.LET_OUT || p.type == PropertyType.DEEMED_LET_OUT)
            && p.grossAnnualValue <= 0) {
            result.addError("VR2-HP-008",
                "HP [" + p.address + "]: Let-out property must have Gross Annual Value > 0.");
        }

        // VR2-HP-009: HP income arithmetic
        int expectedHPIncome = p.netAnnualValue - p.standardDeduction
            - p.interestOnBorrowedCapital + p.arrearUnrealisedRent;
        if (p.hpIncome != expectedHPIncome) {
            result.addError("VR2-HP-009",
                "HP [" + p.address + "]: Income = NAV - 30% - Interest + Arrear = Rs " +
                expectedHPIncome + ". Declared: Rs " + p.hpIncome);
        }

        // VR2-HP-010: NAV = GAV - municipal tax
        int expectedNAV = p.grossAnnualValue - p.municipalTax;
        if (p.netAnnualValue != expectedNAV) {
            result.addError("VR2-HP-010",
                "HP [" + p.address + "]: NAV must be GAV - Municipal Tax = Rs " + expectedNAV);
        }

        // VR2-HP-012: Co-owner PAN != assessee PAN
        if (p.isCoOwned && p.coOwnerPAN != null && p.coOwnerPAN.equals(data.getPAN())) {
            result.addError("VR2-HP-012",
                "HP [" + p.address + "]: Co-owner PAN cannot be same as assessee PAN.");
        }
    }
}
```

### 2.2 Full Schedule 112A Validation (63 rules reduced to 8 mandatory — all 8 must be implemented)

```java
private void validateSchedule112A(Itr2FormData data, ValidationResult result) {
    ScheduleCGData cg = data.getScheduleCG();
    if (cg == null || cg.schedule112ARows == null || cg.schedule112ARows.isEmpty()) return;

    int runningTotalPreJul23 = 0;
    int runningTotalPostJul23 = 0;

    for (Schedule112ARow row : cg.schedule112ARows) {
        boolean isPreFeb2018 = row.dateOfAcquisition != null &&
            row.dateOfAcquisition.isBefore(java.time.LocalDate.of(2018, 2, 1));

        // VR2-CG-112A-001: Col6 = units × sale price per unit
        int expectedSaleValue = row.units * row.salePricePerUnit;
        if (row.totalSaleValue != expectedSaleValue) {
            result.addError("VR2-CG-112A-001",
                "112A [" + row.nameOfScrip + "]: Total Sale Value must be " +
                row.units + " × Rs " + row.salePricePerUnit + " = Rs " + expectedSaleValue);
        }

        // VR2-CG-112A-002: Cost without indexation = max(actual cost, FMV Jan31 2018)
        int expectedCost;
        if (isPreFeb2018) {
            expectedCost = Math.max(row.actualCostOfAcquisition, row.fmvOn31Jan2018);
        } else {
            expectedCost = row.actualCostOfAcquisition;
            // VR2-CG-112A-003: FMV only applicable for pre-Feb-2018 assets
            if (row.fmvOn31Jan2018 > 0) {
                result.addError("VR2-CG-112A-003",
                    "112A [" + row.nameOfScrip + "]: FMV Jan 31, 2018 only applicable for assets acquired before Feb 1, 2018.");
            }
        }
        if (row.costWithoutIndexation != expectedCost) {
            result.addError("VR2-CG-112A-002",
                "112A [" + row.nameOfScrip + "]: Cost without indexation must be Rs " + expectedCost);
        }

        // VR2-CG-112A-004: Col11 = units × FMV per share
        if (isPreFeb2018) {
            int expectedTotalFMV = row.units * row.fmvPerShareOn31Jan2018;
            if (row.totalFMV55_2_ac != expectedTotalFMV) {
                result.addError("VR2-CG-112A-004",
                    "112A [" + row.nameOfScrip + "]: Total FMV = units × FMV/share = Rs " + expectedTotalFMV);
            }
            // FMV capped at min(total FMV, sale value)
            if (row.fmvOn31Jan2018 > Math.min(row.totalSaleValue, row.totalFMV55_2_ac)) {
                result.addError("VR2-CG-112A-003b",
                    "112A [" + row.nameOfScrip + "]: FMV cannot exceed min(sale value, total FMV per Col11).");
            }
        }

        // VR2-CG-112A-005: Col13 = Col7 + Col12
        int expectedDeductions = row.costWithoutIndexation + row.expenditureOnTransfer;
        if (row.totalDeductions != expectedDeductions) {
            result.addError("VR2-CG-112A-005",
                "112A [" + row.nameOfScrip + "]: Total deductions = cost + transfer expenses = Rs " + expectedDeductions);
        }

        // VR2-CG-112A-006: Col14 = Col6 - Col13
        int expectedBalance = row.totalSaleValue - row.totalDeductions;
        if (row.balanceLTCG != expectedBalance) {
            result.addError("VR2-CG-112A-006",
                "112A [" + row.nameOfScrip + "]: Balance LTCG = sale - deductions = Rs " + expectedBalance);
        }

        // Accumulate totals by rate period
        if (row.preJuly23Sale) {
            runningTotalPreJul23 += row.balanceLTCG;
        } else {
            runningTotalPostJul23 += row.balanceLTCG;
        }
    }

    // VR2-CG-112A-007: Declared totals must match row sums
    if (cg.ltcg112A_preJul23 != runningTotalPreJul23) {
        result.addError("VR2-CG-112A-007",
            "Schedule 112A: Pre-July 2023 LTCG total Rs " + cg.ltcg112A_preJul23 +
            " does not match row sum Rs " + runningTotalPreJul23);
    }
    if (cg.ltcg112A_postJul23 != runningTotalPostJul23) {
        result.addError("VR2-CG-112A-007b",
            "Schedule 112A: Post-July 2023 LTCG total Rs " + cg.ltcg112A_postJul23 +
            " does not match row sum Rs " + runningTotalPostJul23);
    }

    // VR2-CG-112A-008: LTCG exemption thresholds
    // Pre-July 2023: first Rs 1,00,000 exempt (old threshold)
    // Post-July 2023: first Rs 1,25,000 exempt (Finance Act 2023)
    if (cg.ltcg112A_postJul23 > 0 && cg.ltcg112A_postJul23 <= 125000) {
        // Tax = 0. Flag if taxable amount > 0 in Schedule SI
        result.addInfo("VR2-CG-112A-008",
            "Post-July 2023 LTCG Rs " + cg.ltcg112A_postJul23 + " is within Rs 1,25,000 exemption. Tax = 0.");
    }
}
```

### 2.3 Full CYLA Validation

```java
private void validateScheduleCYLA(Itr2FormData data, ValidationResult result) {
    ScheduleCYLAData cyla = data.getScheduleCYLA();
    if (cyla == null) return;

    // VR2-CYLA-001: HP loss cannot exceed HP income available (already computed by LossSetOffEngine)
    if (cyla.hpLossSetOffAgainstSalary < 0 || cyla.hpLossSetOffAgainstOS < 0) {
        result.addError("VR2-CYLA-001", "HP loss set-off amounts cannot be negative.");
    }

    // VR2-CYLA-002: HP loss set-off must not exceed HP loss available
    int totalHPSetOff = cyla.hpLossSetOffAgainstSalary + cyla.hpLossSetOffAgainstOS + cyla.hpLossSetOffAgainstCG;
    if (totalHPSetOff > cyla.hpLossAvailable) {
        result.addError("VR2-CYLA-002",
            "HP loss set-off Rs " + totalHPSetOff + " exceeds HP loss available Rs " + cyla.hpLossAvailable);
    }

    // VR2-CYLA-003: Speculative loss can ONLY be set off against speculative income
    if (cyla.speculativeLossSetOff > 0) {
        // Enforce this in LossSetOffEngine — flag if engine output violates it
        result.addInfo("VR2-CYLA-003",
            "Speculative loss set-off Rs " + cyla.speculativeLossSetOff +
            " verified — must only be against speculative income.");
    }

    // VR2-CYLA-004: LTCG loss only against LTCG income
    if (cyla.ltcgLossSetOffAgainstLTCG > cyla.ltcgLossAvail) {
        result.addError("VR2-CYLA-004",
            "LTCG loss set-off Rs " + cyla.ltcgLossSetOffAgainstLTCG +
            " exceeds available LTCG loss Rs " + cyla.ltcgLossAvail);
    }

    // VR2-CYLA-005: VDA loss cannot be set off — always zero
    if (cyla.vdaLoss != 0) {
        result.addError("VR2-CYLA-005",
            "VDA loss must be Rs 0 in Schedule CYLA. Section 115BBH prohibits VDA loss set-off.");
    }

    // Unabsorbed validation: remaining = available - setOff
    int hpUnabsorbed = cyla.hpLossAvailable - totalHPSetOff;
    if (cyla.hpLossUnabsorbed != hpUnabsorbed) {
        result.addError("VR2-CYLA-002b",
            "HP unabsorbed loss must be Rs " + hpUnabsorbed + ". Declared: Rs " + cyla.hpLossUnabsorbed);
    }
}
```

### 2.4 Full VIA Deductions Validation

```java
private void validateScheduleVIA(Itr2FormData data, ValidationResult result) {
    // VR2-VIA-001: 80C cap = Rs 1,50,000
    if (data.getDeduction80C() > 150000) {
        result.addError("VR2-VIA-001",
            "80C deduction capped at Rs 1,50,000. Claimed: Rs " + data.getDeduction80C());
    }

    // VR2-VIA-002: 80D self = Rs 25,000 (< 60); Rs 50,000 (>= 60)
    int maxSelf80D = (data.getTaxpayerAge() >= 60) ? 50000 : 25000;
    if (data.getDeduction80D_self() > maxSelf80D) {
        result.addError("VR2-VIA-002",
            "80D self/family cap = Rs " + maxSelf80D + ". Claimed: Rs " + data.getDeduction80D_self());
    }

    // VR2-VIA-002b: 80D parents
    int maxParent80D = (data.isParentSeniorCitizen()) ? 50000 : 25000;
    if (data.getDeduction80D_parents() > maxParent80D) {
        result.addError("VR2-VIA-002b",
            "80D parents cap = Rs " + maxParent80D + ". Claimed: Rs " + data.getDeduction80D_parents());
    }

    // VR2-VIA-003: 80G cash donation limit (2% rule)
    if (data.getSchedule80G() != null) {
        data.getSchedule80G().getDonations().forEach(don -> {
            if (don.paymentMode == PaymentMode.CASH && don.amount > 2000) {
                result.addError("VR2-VIA-003",
                    "80G: Cash donation to " + don.donatee + " Rs " + don.amount +
                    " exceeds Rs 2,000 cash limit. Eligible amount = 0.");
            }
        });
    }

    // VR2-VIA-004: 80CCD(1) cap = 10% of salary (employee)
    if (data.getDeduction80CCD1() > 0) {
        int limit80CCD1 = (int) Math.round(0.10 * data.getGrossSalary());
        if (data.getDeduction80CCD1() > limit80CCD1) {
            result.addError("VR2-VIA-004",
                "80CCD(1) cap = 10% of salary = Rs " + limit80CCD1 +
                ". Claimed: Rs " + data.getDeduction80CCD1());
        }
    }

    // VR2-VIA-005: 80CCD(1) + 80CCD(1B) total cannot exceed Rs 2,00,000
    int total80CCD = data.getDeduction80CCD1() + data.getDeduction80CCD1B();
    if (total80CCD > 200000) {
        result.addError("VR2-VIA-005",
            "80CCD(1) + 80CCD(1B) combined cap = Rs 2,00,000. Total: Rs " + total80CCD);
    }

    // VR2-VIA-006: New regime — block all disallowed deductions
    if (data.getTaxRegime() == TaxRegime.NEW) {
        int disallowed = data.getDeduction80C() + data.getDeduction80D_self() +
            data.getDeduction80D_parents() + data.getDeduction80E() +
            data.getDeduction80TTA() + data.getDeduction80TTB();
        if (disallowed > 0) {
            result.addError("VR2-VIA-006",
                "New Regime does not allow Chapter VI-A deductions (80C/D/E/TTA/TTB). " +
                "Only 80CCD(2) employer NPS, 80JJAA, 80CCH(2) permitted. " +
                "Total disallowed deductions declared: Rs " + disallowed);
        }
    }

    // VR2-VIA-007: AMT check
    if (data.getAdjustedTotalIncomeForAMT() > 2000000) {
        int computedAMT = (int) Math.round(0.185 * data.getAdjustedTotalIncomeForAMT());
        if (computedAMT > data.getRegularTaxBeforeCess() && !data.isAmtApplicable()) {
            result.addError("VR2-VIA-007",
                "AMT applies: ATI Rs " + data.getAdjustedTotalIncomeForAMT() +
                " > Rs 20L, computed AMT Rs " + computedAMT +
                " > regular tax Rs " + data.getRegularTaxBeforeCess() +
                ". Mark amtApplicable = true and complete Schedule AMT.");
        }
    }

    // VR2-VIA-008: HUF cannot claim 89A relief
    if (data.isHUF() && data.getReliefUs89A() > 0) {
        result.addError("VR2-VIA-008", "HUF cannot claim relief u/s 89A.");
    }

    // VR2-VIA-009: Schedule AL mandatory if income > Rs 50L
    int totalIncome = data.getTotalIncome(); // gross total income field
    if (totalIncome > 5000000 && data.getScheduleAL() == null) {
        result.addError("VR2-VIA-009",
            "Schedule AL (Assets and Liabilities) is mandatory when total income > Rs 50,00,000.");
    }
}
```

### 2.5 VDA Validation (hard enforcement, not info)

```java
private void validateScheduleVDA(Itr2FormData data, ValidationResult result) {
    int computedVDAIncome = 0;
    for (VDATransaction vda : data.getVdaTransactions()) {
        int gain = vda.considerationReceived - vda.costOfAcquisition;
        if (gain < 0) gain = 0; // Section 115BBH: loss not allowed
        computedVDAIncome += gain;
    }
    // VDA income must match declared vdaIncome
    if (data.getVdaIncome() != computedVDAIncome) {
        result.addError("VR2-SI-004",
            "VDA income declared Rs " + data.getVdaIncome() +
            " does not match transaction sum Rs " + computedVDAIncome +
            " (losses floored to 0 per Section 115BBH).");
    }
    if (data.getVdaIncome() < 0) {
        result.addError("VR2-SI-008", "VDA income cannot be negative. Set to 0.");
    }
}
```

---

## SECTION 3 — STRENGTHEN `ITR3CBDTValidationService.java`

**Current compliance: 50%. Add arithmetic checks to every stub method.**

### 3.1 Section 43B(h) — MSME Payment Disallowance

Replace the stub body:

```java
private void validateMSMEPayments(Itr3FormData data, ValidationResult result) {
    List<MSMEPayment> payments = data.getMsmePayments();
    if (payments == null) return;

    int totalDisallowance = 0;
    for (MSMEPayment p : payments) {
        if (p.isMSMERegistered && p.paymentDate == null) {
            // Unpaid at filing date — disallow entire amount
            totalDisallowance += p.amount;
            result.addError("VR3-BP-008",
                "MSME supplier [" + p.supplierName + "]: Rs " + p.amount +
                " unpaid. Disallowed u/s 43B(h). Add back to income in Schedule BP.");
            p.disallowed = true;
        } else if (p.isMSMERegistered && p.paymentDate != null && p.supplyDate != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(p.supplyDate, p.paymentDate);
            if (days > 45) {
                totalDisallowance += p.amount;
                result.addError("VR3-BP-008",
                    "MSME supplier [" + p.supplierName + "]: Payment after " + days +
                    " days (limit 45). Rs " + p.amount + " disallowed u/s 43B(h).");
                p.disallowed = true;
            }
        }
    }
    // Cross-check: totalDisallowance must be added back in ScheduleBP.msmeDisallowance
    if (data.getScheduleBP() != null && data.getScheduleBP().msmeDisallowance != totalDisallowance) {
        result.addError("VR3-BP-008b",
            "Schedule BP MSME disallowance Rs " + data.getScheduleBP().msmeDisallowance +
            " does not match computed Rs " + totalDisallowance);
    }
}
```

### 3.2 Partner Remuneration — Full 40(b) Limit Check

```java
private void validatePartnerRemuneration(Itr3FormData data, ValidationResult result) {
    int bookProfit = data.getBookProfitForPartnerRemuneration();
    int claimed = data.getPartnerRemunerationClaimed();
    if (claimed <= 0) return;

    // Section 40(b) formula:
    // If book profit <= 3,00,000: limit = max(1,50,000, 90% of book profit)
    // If book profit >  3,00,000: limit = (90% of first 3L) + (60% of balance)
    int limit;
    if (bookProfit <= 300000) {
        limit = Math.max(150000, (int) Math.round(0.90 * bookProfit));
    } else {
        int part1 = (int) Math.round(0.90 * 300000);
        int part2 = (int) Math.round(0.60 * (bookProfit - 300000));
        limit = Math.max(150000, part1 + part2);
    }

    if (claimed > limit) {
        result.addError("VR3-BP-010",
            "Partner remuneration claimed Rs " + claimed +
            " exceeds Section 40(b) limit Rs " + limit +
            " (based on book profit Rs " + bookProfit + "). Excess Rs " + (claimed - limit) + " disallowed.");
    }
}
```

### 3.3 F&O Turnover Exact Check

```java
private void validateFandOTurnover(Itr3FormData data, ValidationResult result) {
    FandODetail fando = data.getFandoDetail();
    if (fando == null) return;

    // CRITICAL: F&O turnover = absolute profit sum + absolute loss sum (not gross contract value)
    int expectedTurnover = fando.absoluteProfitSum + fando.absoluteLossSum;
    if (fando.fandoTurnover != expectedTurnover) {
        result.addError("VR3-BP-011",
            "F&O turnover must be sum of |profit trades| + |loss trades| = Rs " + expectedTurnover +
            ". Declared Rs " + fando.fandoTurnover + ". Gross contract value is NOT turnover.");
    }

    // Tax audit threshold: F&O turnover > Rs 10 Cr requires audit (> Rs 1 Cr if cash > 5%)
    if (fando.fandoTurnover > 10_00_00_000 && !data.isSubjectToTaxAudit()) {
        result.addError("VR3-BP-011b",
            "F&O turnover Rs " + fando.fandoTurnover + " exceeds Rs 10 Cr. Tax audit mandatory.");
    }
}
```

### 3.4 Depreciation Cross-Check (DPM vs DEP totals)

```java
private void validateScheduleDPM(Itr3FormData data, ValidationResult result) {
    ScheduleDPMData dpm = data.getScheduleDPM();
    if (dpm == null) return;

    // Re-compute depreciation using DepreciationEngine
    int computedTotal = depreciationEngine.computeTotal(dpm);
    if (Math.abs(dpm.totalDepreciation - computedTotal) > 0) {
        result.addError("VR3-DPM-002",
            "Schedule DPM: Declared depreciation Rs " + dpm.totalDepreciation +
            " does not match recomputed Rs " + computedTotal +
            ". Check individual block computations.");
    }

    // VR3-DPM-005: Goodwill — 0% depreciation from AY 2021-22
    if (dpm.intangibles_25pct != null && dpm.intangibles_25pct.hasGoodwill &&
        dpm.intangibles_25pct.depreciation > 0) {
        result.addError("VR3-DPM-005",
            "Goodwill is not eligible for depreciation from AY 2021-22 (Finance Act 2021). " +
            "Set goodwill depreciation to 0.");
    }

    // VR3-DPM-006: Additional depreciation (32(1)(iia)) — P&M used < 180 days → 10% only
    dpm.getBlocks().forEach(block -> {
        if (block.additionalDepreciation > 0 && block.additionsAfterOct1 > 0) {
            int maxAddlDep = (int) Math.round(0.10 * block.additionsAfterOct1);
            if (block.additionalDepreciation > maxAddlDep) {
                result.addError("VR3-DPM-006",
                    "Asset block [" + block.assetType + "]: P&M put to use after Oct 1 " +
                    "qualifies for only 10% additional depreciation = Rs " + maxAddlDep);
            }
        }
    });
}
```

### 3.5 DCG Arithmetic Enforcement

```java
private void validateScheduleDCG(Itr3FormData data, ValidationResult result) {
    ScheduleDCGData dcg = data.getScheduleDCG();
    if (dcg == null) return;

    // VR3-DCG-001: row1_total arithmetic
    int expectedRow1 = dcg.row1a + dcg.row1b + dcg.row1c + dcg.row1d;
    if (dcg.row1_total != expectedRow1) {
        result.addError("VR3-DCG-001",
            "DCG: Opening WDV total must be Rs " + expectedRow1 + ". Declared Rs " + dcg.row1_total);
    }

    // VR3-DCG-002: row2d arithmetic
    int expectedRow2d = dcg.row2a + dcg.row2b + dcg.row2c;
    if (dcg.row2d != expectedRow2d) {
        result.addError("VR3-DCG-002",
            "DCG: Sale proceeds total must be Rs " + expectedRow2d + ". Declared Rs " + dcg.row2d);
    }

    // VR3-DCG-003: deemed CG = row2d - row1_total (only if positive)
    int expectedDCG = Math.max(0, dcg.row2d - dcg.row1_total);
    if (dcg.totalDeemedCapitalGains != expectedDCG) {
        result.addError("VR3-DCG-003",
            "DCG: Deemed Capital Gains must be max(0, sale - WDV) = Rs " + expectedDCG +
            ". Declared Rs " + dcg.totalDeemedCapitalGains);
    }

    // VR3-DCG-004: DCG must also appear in Schedule CG
    if (expectedDCG > 0 && data.getScheduleCG() != null) {
        // Validate that scheduleCG.stcgAtSlab includes this amount
        if (data.getScheduleCG().stcgAtSlab < expectedDCG) {
            result.addError("VR3-DCG-004",
                "Deemed Capital Gains Rs " + expectedDCG +
                " from Schedule DCG must be included in Schedule CG as STCG at slab rate.");
        }
    }
}
```

---

## SECTION 4 — STRENGTHEN `ITR4CBDTValidationService.java`

**Current compliance: 45%. Replace warning-based enforcement with hard errors for all CAT-A rules.**

### 4.1 Presumptive Income — Full Enforcement

```java
private void validatePresumptiveIncome(Itr4FormData data, ValidationResult result) {

    // ── Section 44AD ─────────────────────────────────────────────────────────
    Sec44ADData ad = data.getSec44AD();
    if (ad != null) {
        // VR4-001: Total turnover limit = Rs 3 Cr (if digital > 95%) or Rs 2 Cr
        int limit = 3_00_00_000; // Rs 3 Cr default (if digital receipts >= 95%)
        int digitalPct = (ad.grossTurnover > 0)
            ? (int) Math.round(100.0 * ad.turnoverDigital / ad.grossTurnover) : 0;
        if (digitalPct < 95) limit = 2_00_00_000;
        if (ad.grossTurnover > limit) {
            result.addError("VR4-001",
                "44AD: Turnover Rs " + ad.grossTurnover + " exceeds limit Rs " + limit +
                " (digital receipts " + digitalPct + "%). Must file ITR-3.");
        }

        // VR4-002: Minimum declared income — cash: 8%, digital: 6%
        int minCashIncome = (int) Math.round(0.08 * ad.turnoverCash);
        int minDigitalIncome = (int) Math.round(0.06 * ad.turnoverDigital);
        int minTotal = minCashIncome + minDigitalIncome;
        if (ad.totalPresumptiveIncome < minTotal) {
            result.addError("VR4-002",
                "44AD: Declared income Rs " + ad.totalPresumptiveIncome +
                " is below minimum required Rs " + minTotal +
                " (8% of cash Rs " + ad.turnoverCash + " + 6% of digital Rs " + ad.turnoverDigital + ").");
        }

        // VR4-003: LTCG 112A > Rs 1,25,000 → must file ITR-3
        if (data.getLtcg112A() > 125000) {
            result.addError("VR4-003",
                "44AD: LTCG 112A of Rs " + data.getLtcg112A() +
                " exceeds Rs 1,25,000. ITR-4 cannot be used. File ITR-3.");
        }

        // VR4-004: Cash receipt > 5% of turnover (post Finance Act 2023 — triggers audit lock)
        if (ad.turnoverCash > 0 && ad.grossTurnover > 0) {
            int cashPct = (int) Math.round(100.0 * ad.turnoverCash / ad.grossTurnover);
            if (cashPct > 5 && ad.grossTurnover > 1_00_00_000) {
                result.addError("VR4-004",
                    "44AD: Cash receipts " + cashPct + "% of turnover exceeds 5% limit. " +
                    "When turnover > Rs 1 Cr and cash > 5%, audit is required. Cannot use ITR-4.");
            }
        }

        // VR4-005: Opt-out lock-in (if 44AD claimed in prior year and now opting out, 5-year ban)
        if (ad.priorYear44ADClaimed && data.isBusinessLossCarryForwardAttempted()) {
            result.addError("VR4-005",
                "44AD: Prior year had 44AD. Opting out triggers 5-year lock-in from regular scheme. " +
                "Business loss cannot be carried forward under 44AD.");
        }
    }

    // ── Section 44ADA ─────────────────────────────────────────────────────────
    Sec44ADAData ada = data.getSec44ADA();
    if (ada != null) {
        // VR4-006: Gross receipts limit = Rs 75 Lakh (if digital >= 95%) or Rs 50 Lakh
        int adaLimit = (ada.grossReceipts > 0 && ada.receiptsDigital * 100 / ada.grossReceipts >= 95)
            ? 75_00_000 : 50_00_000;
        if (ada.grossReceipts > adaLimit) {
            result.addError("VR4-006",
                "44ADA: Gross receipts Rs " + ada.grossReceipts + " exceeds limit Rs " + adaLimit +
                ". Must file ITR-3.");
        }

        // VR4-007: Profession code must be eligible (medicine, law, architecture, etc.)
        if (ada.professionCode == null || ada.professionCode.isBlank()) {
            result.addError("VR4-007", "44ADA: Profession code is mandatory.");
        }

        // VR4-008: Declared income >= 50% of gross receipts
        int min50Pct = (int) Math.round(0.50 * ada.grossReceipts);
        if (ada.presumptiveIncome < min50Pct) {
            result.addError("VR4-008",
                "44ADA: Declared income Rs " + ada.presumptiveIncome +
                " is below 50% minimum Rs " + min50Pct + " of gross receipts Rs " + ada.grossReceipts);
        }
    }

    // ── Section 44AE ──────────────────────────────────────────────────────────
    Sec44AEData ae = data.getSec44AE();
    if (ae != null && ae.vehicles != null) {
        int computedTotal = 0;
        for (VehicleDetail v : ae.vehicles) {
            // VR4-009: Heavy goods vehicle (GVW > 12 tonnes): Rs 1,000/month/tonne
            // Light vehicle: Rs 7,500/month flat
            int monthlyIncome;
            if (v.isHeavyVehicle && v.gvwInTonnes > 12) {
                monthlyIncome = (int) Math.round(1000 * v.gvwInTonnes);
            } else {
                monthlyIncome = 7500;
            }
            int vehicleIncome = monthlyIncome * v.monthsOwned;
            if (v.declaredIncome < vehicleIncome) {
                result.addError("VR4-009",
                    "44AE: Vehicle [" + v.registrationNumber + "]: Minimum income for " +
                    v.monthsOwned + " months = Rs " + vehicleIncome +
                    ". Declared Rs " + v.declaredIncome);
            }
            computedTotal += Math.max(v.declaredIncome, vehicleIncome);
        }
        // VR4-010: Vehicle count <= 10
        if (ae.vehicles.size() > 10) {
            result.addError("VR4-010",
                "44AE: Maximum 10 vehicles allowed. Found " + ae.vehicles.size() + ". Must file ITR-3.");
        }
        if (ae.totalPresumptiveIncome != computedTotal) {
            result.addError("VR4-009b",
                "44AE: Total income Rs " + ae.totalPresumptiveIncome +
                " does not match vehicle sum Rs " + computedTotal);
        }
    }
}
```

### 4.2 Simplified Balance Sheet Validation

```java
private void validateSimplifiedBalanceSheet(Itr4FormData data, ValidationResult result) {
    SimplifiedBalanceSheet bs = data.getSimplifiedBalanceSheet();
    if (bs == null) {
        result.addError("VR4-015", "Simplified Balance Sheet is mandatory for ITR-4.");
        return;
    }

    // VR4-016: Closing capital arithmetic
    int expectedClosing = bs.openingCapital + bs.additionsToCapital - bs.drawings + bs.netProfit;
    if (bs.closingCapital != expectedClosing) {
        result.addError("VR4-016",
            "Balance Sheet: Closing capital = Opening + Additions - Drawings + Net Profit = Rs " +
            expectedClosing + ". Declared Rs " + bs.closingCapital);
    }

    // VR4-017: Net profit must match declared presumptive income
    int declaredIncome = 0;
    if (data.getSec44AD() != null) declaredIncome += data.getSec44AD().totalPresumptiveIncome;
    if (data.getSec44ADA() != null) declaredIncome += data.getSec44ADA().presumptiveIncome;
    if (data.getSec44AE() != null) declaredIncome += data.getSec44AE().totalPresumptiveIncome;
    if (bs.netProfit != declaredIncome) {
        result.addError("VR4-017",
            "Balance Sheet: Net profit Rs " + bs.netProfit +
            " must match total presumptive income Rs " + declaredIncome);
    }

    // VR4-018: Cash balance must not be negative
    if (bs.cashBalance < 0) {
        result.addError("VR4-018", "Balance Sheet: Cash balance cannot be negative.");
    }
}
```

---

## SECTION 5 — REPLACE VDA AND SFT PLACEHOLDER STUBS

**Current compliance: 0%. Both services exist as stubs. Replace the implementation bodies completely.**

### 5.1 Replace `VDATransactionService.java`

Open the existing file. Replace the entire class body with:

```java
@Service
public class VDATransactionService {

    /**
     * Computes VDA income per Section 115BBH.
     * Only allowable deduction: cost of acquisition.
     * No other deductions (brokerage, mining cost if not original acquirer, etc.)
     * Losses: automatically floored to 0. Cannot be set off.
     * Tax rate: 30% flat + applicable surcharge + 4% cess. No 87A rebate.
     * TDS: if consideration > Rs 10,000 from exchange, 1% TDS u/s 194S applies.
     */
    public VDAComputationResult compute(List<VDATransaction> transactions, String assessmentYear) {
        if (transactions == null || transactions.isEmpty()) {
            return VDAComputationResult.empty();
        }

        int totalConsideration = 0;
        int totalCost = 0;
        int totalTDS194S = 0;
        List<VDATransactionResult> details = new ArrayList<>();

        for (VDATransaction t : transactions) {
            int gain = t.considerationReceived - t.costOfAcquisition;
            int taxableGain = Math.max(0, gain); // Loss floored to 0 per 115BBH(2)

            totalConsideration += t.considerationReceived;
            totalCost += t.costOfAcquisition;
            totalTDS194S += t.tdsDeductedUnder194S;

            details.add(new VDATransactionResult(
                t.assetName,
                t.dateOfTransfer,
                t.considerationReceived,
                t.costOfAcquisition,
                gain,            // raw (may be negative for audit trail)
                taxableGain,     // tax-effective (always >= 0)
                t.tdsDeductedUnder194S
            ));
        }

        int netVDAIncome = Math.max(0, totalConsideration - totalCost);

        // Flag AIS reconciliation: TDS u/s 194S from exchange must match AIS
        boolean tdsReconciled = (totalTDS194S > 0); // flag for AIS cross-check in orchestration

        return new VDAComputationResult(netVDAIncome, totalTDS194S, tdsReconciled, details);
    }

    /**
     * Parses VDA transactions from AIS JSON.
     * AIS code: SFT-018 (crypto exchange reporting).
     * Each SFT-018 entry has: isin = "VDA", name, consideration, tds194S amount.
     */
    public List<VDATransaction> parseFromAIS(String aisJsonString) {
        List<VDATransaction> result = new ArrayList<>();
        if (aisJsonString == null || aisJsonString.isBlank()) return result;

        try {
            // Parse AIS JSON — structure per ITD AIS schema
            com.fasterxml.jackson.databind.JsonNode root =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(aisJsonString);
            com.fasterxml.jackson.databind.JsonNode sftList = root.path("sftTransactionList");

            for (com.fasterxml.jackson.databind.JsonNode entry : sftList) {
                String type = entry.path("informationType").asText("");
                if (!"SFT-018".equals(type)) continue;

                VDATransaction vda = new VDATransaction();
                vda.assetName = entry.path("assetDescription").asText("VDA");
                vda.considerationReceived = entry.path("saleAmount").asInt(0);
                vda.tdsDeductedUnder194S = entry.path("tdsAmount").asInt(0);
                vda.dateOfTransfer = parseDate(entry.path("transactionDate").asText(""));
                vda.requiresManualCost = true; // cost of acquisition always requires manual entry
                result.add(vda);
            }
        } catch (Exception e) {
            // Log and return empty — AIS parse failure is non-fatal; user can enter manually
        }
        return result;
    }

    private java.time.LocalDate parseDate(String ddMmYyyy) {
        if (ddMmYyyy == null || ddMmYyyy.length() < 8) return null;
        try {
            return java.time.LocalDate.parse(ddMmYyyy,
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) { return null; }
    }
}
```

### 5.2 Replace `SFTProcessingService.java`

Open the existing file. Replace stub methods with real implementations:

```java
@Service
public class SFTProcessingService {

    /**
     * Parses AIS JSON and extracts all SFT transactions by category.
     * ITD AIS JSON structure: root → sftTransactionList → [{informationType, ...}]
     */
    public SFTParseResult parseAIS(String aisJsonString) {
        SFTParseResult result = new SFTParseResult();
        if (aisJsonString == null || aisJsonString.isBlank()) return result;

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(aisJsonString);
            com.fasterxml.jackson.databind.JsonNode list = root.path("sftTransactionList");

            for (com.fasterxml.jackson.databind.JsonNode entry : list) {
                String type = entry.path("informationType").asText("");
                processSFTEntry(type, entry, result);
            }
        } catch (Exception e) {
            result.parseErrors.add("AIS JSON parse error: " + e.getMessage());
        }
        return result;
    }

    private void processSFTEntry(String type,
                                  com.fasterxml.jackson.databind.JsonNode e,
                                  SFTParseResult r) {
        int amount = e.path("transactionAmount").asInt(0);
        String pan   = e.path("partyPAN").asText("");
        String date  = e.path("transactionDate").asText("");

        switch (type) {
            case "SFT-001" -> // Savings account deposits > Rs 10L
                r.savingsDeposits.add(new SFTEntry(type, amount, pan, date));
            case "SFT-002" -> // Savings account withdrawals
                r.savingsWithdrawals.add(new SFTEntry(type, amount, pan, date));
            case "SFT-003" -> // Fixed deposit > Rs 10L
                r.fixedDeposits.add(new SFTEntry(type, amount, pan, date));
            case "SFT-004" -> // Credit card bill payment > Rs 1L (cash) or Rs 10L (other)
                r.creditCardPayments.add(new SFTEntry(type, amount, pan, date));
            case "SFT-005" -> // Shares / debentures acquisition > Rs 10L
                r.shareAcquisitions.add(new SFTEntry(type, amount, pan, date));
            case "SFT-006" -> // Share buyback proceeds
                r.shareBuybacks.add(new SFTEntry(type, amount, pan, date));
            case "SFT-007" -> // Mutual fund purchase > Rs 10L
                r.mutualFundPurchases.add(new SFTEntry(type, amount, pan, date));
            case "SFT-008" -> // Mutual fund redemption
                r.mutualFundRedemptions.add(new SFTEntry(type, amount, pan, date));
            case "SFT-009" -> // Foreign exchange > Rs 10L
                r.forexTransactions.add(new SFTEntry(type, amount, pan, date));
            case "SFT-010" -> // Property purchase > Rs 30L
                r.propertyPurchases.add(new SFTEntry(type, amount, pan, date));
            case "SFT-011" -> // Property sale > Rs 30L
                r.propertySales.add(new SFTEntry(type, amount, pan, date));
            case "SFT-012" -> // Cash deposit (demonetization era / post)
                r.cashDeposits.add(new SFTEntry(type, amount, pan, date));
            case "SFT-013" -> // Cash payments for hotel bills
                r.hotelBills.add(new SFTEntry(type, amount, pan, date));
            case "SFT-014" -> // Cash purchase of DD/PO > Rs 10L
                r.ddPurchases.add(new SFTEntry(type, amount, pan, date));
            case "SFT-015" -> // TDS on salary
                r.tdsSalary.add(new SFTEntry(type, amount, pan, date));
            case "SFT-016" -> // TDS on professional fees
                r.tdsProfessional.add(new SFTEntry(type, amount, pan, date));
            case "SFT-018" -> // VDA / crypto transactions
                r.vdaTransactions.add(new SFTEntry(type, amount, pan, date));
            default -> r.unrecognizedEntries.add(type + ":" + amount);
        }
    }

    /**
     * Auto-populates Schedule AL from SFT data.
     * - SFT-010/011: property values → immovable property
     * - SFT-005/006: shares → financial assets
     * - SFT-007/008: mutual funds → financial assets
     * - SFT-003: FDs → financial assets
     */
    public ScheduleALData autoPopulateScheduleAL(SFTParseResult sft) {
        ScheduleALData al = new ScheduleALData();

        // Immovable property from SFT-010 (purchase price as proxy for cost)
        int propertyValue = sft.propertyPurchases.stream()
            .mapToInt(e -> e.amount).sum();
        al.immovablePropertyCost = propertyValue;

        // Financial assets from shares, MFs, FDs
        int financialAssets = sft.shareAcquisitions.stream().mapToInt(e -> e.amount).sum()
            + sft.mutualFundPurchases.stream().mapToInt(e -> e.amount).sum()
            + sft.fixedDeposits.stream().mapToInt(e -> e.amount).sum();
        al.financialAssets = financialAssets;

        al.autoPopulated = true;
        al.requiresManualVerification = true; // Always flag — SFT is cost, not current value
        return al;
    }
}
```

---

## SECTION 6 — CREATE `ITRFilingOrchestrationService.java`

**This file does not exist. Create it from scratch.**

```java
@Service
@Transactional
public class ITRFilingOrchestrationService {

    @Autowired private ITRFormSelectionService formSelector;
    @Autowired private AISReconciliationService aisService;
    @Autowired private SFTProcessingService sftService;
    @Autowired private VDATransactionService vdaService;
    @Autowired private AY202627TaxComputationService taxEngine;
    @Autowired private LossSetOffEngine lossEngine;
    @Autowired private TaxRegimeComparisonService regimeComparison;
    @Autowired private ITR2CBDTValidationService itr2Validator;
    @Autowired private ITR3CBDTValidationService itr3Validator;
    @Autowired private ITR4CBDTValidationService itr4Validator;
    @Autowired private ITR2JSONExportService itr2Exporter;
    @Autowired private ITR3JSONExportService itr3Exporter;
    @Autowired private ITR4JSONExportService itr4Exporter;
    @Autowired private PreSubmissionChecklistService checklist;
    @Autowired private AuditTrailService auditTrail;
    @Autowired private LossLedgerRepository lossLedgerRepo;

    /**
     * Full end-to-end ITR filing pipeline.
     * Throws ITRFilingException if any CAT-A validation rule fails.
     * Loss ledger persistence runs in same transaction — rolled back on failure.
     */
    public ITRFilingResult file(FilingRequest request) {
        String pan = request.getPAN();
        String ay  = request.getAssessmentYear();

        auditTrail.logEvent(pan, ay, "FILING_STARTED", "Orchestration pipeline initiated", "SYSTEM");

        // Step 1: Determine ITR form
        String itrForm = formSelector.selectForm(request.toFormSelectionInput());
        auditTrail.logEvent(pan, ay, "FORM_SELECTED", itrForm, "FORM_SELECTOR");

        // Step 2: Parse AIS and SFT data
        SFTParseResult sft = sftService.parseAIS(request.getAisJson());
        auditTrail.logEvent(pan, ay, "AIS_PARSED",
            "SFT entries: " + sft.totalEntryCount(), "AIS");

        // Step 3: Reconcile AIS with declared income
        AISReconciliationResult aisRec = aisService.reconcile(request.toAISInput(), request.getAisJson());
        if (!aisRec.getDiscrepancies().isEmpty()) {
            auditTrail.logEvent(pan, ay, "AIS_DISCREPANCY",
                aisRec.getDiscrepancies().size() + " discrepancies found", "AIS");
        }

        // Step 4: Compute VDA income (if applicable)
        if (request.hasVDATransactions()) {
            VDAComputationResult vdaResult = vdaService.compute(request.getVdaTransactions(), ay);
            request.setComputedVDAIncome(vdaResult.getNetVDAIncome());
            auditTrail.logEvent(pan, ay, "VDA_COMPUTED",
                "Net VDA income: Rs " + vdaResult.getNetVDAIncome(), "VDA_SERVICE");
        }

        // Step 5: Load brought-forward losses from LossLedger
        List<LossLedger> bfLosses = lossLedgerRepo.findActive(pan, ay);
        request.setBroughtForwardLosses(bfLosses);

        // Step 6: Run loss set-off engine
        if ("ITR2".equals(itrForm) || "ITR3".equals(itrForm)) {
            lossEngine.compute(request);
        }

        // Step 7: Compute tax — both regimes
        TaxRegimeComparisonResult regimeResult = regimeComparison.compare(request.toTaxInput());
        auditTrail.logEvent(pan, ay, "TAX_COMPUTED",
            "Optimal regime: " + regimeResult.getRecommendedRegime(), "TAX_ENGINE");

        // Step 8: Run CBDT validation — CAT-A failures are hard stops
        ValidationResult validation = switch (itrForm) {
            case "ITR2" -> itr2Validator.validate(request.getItr2FormData());
            case "ITR3" -> itr3Validator.validate(request.getItr3FormData());
            case "ITR4" -> itr4Validator.validate(request.getItr4FormData());
            default     -> itr2Validator.validate(request.getItr2FormData()); // fallback
        };

        // CRITICAL: CAT-A failures block JSON generation
        if (!validation.getErrors().isEmpty()) {
            auditTrail.logEvent(pan, ay, "VALIDATION_FAILED",
                validation.getErrors().size() + " CAT-A errors", "VALIDATOR");
            throw new ITRFilingException(
                "ITR filing blocked: " + validation.getErrors().size() +
                " CBDT Category-A validation errors must be resolved before filing.",
                validation.getErrors()
            );
        }

        // Step 9: Run pre-submission checklist
        PreSubmissionResult checklistResult = checklist.run(request);

        // Step 10: Generate ITR JSON
        String itdJson = switch (itrForm) {
            case "ITR2" -> itr2Exporter.export(request.getItr2FormData());
            case "ITR3" -> itr3Exporter.export(request.getItr3FormData());
            case "ITR4" -> itr4Exporter.export(request.getItr4FormData());
            default     -> throw new ITRFilingException("Unknown form: " + itrForm);
        };
        auditTrail.logEvent(pan, ay, "JSON_GENERATED", itrForm + " JSON ready", "EXPORTER");

        // Step 11: Persist carry-forward losses (same @Transactional — rolled back on failure)
        persistCarryForwardLosses(pan, ay, request);

        return ITRFilingResult.success(itdJson, validation, regimeResult, checklistResult, sft);
    }

    private void persistCarryForwardLosses(String pan, String ay, FilingRequest req) {
        ScheduleCFLData cfl = req.getCFLData();
        if (cfl == null) return;

        int ayStart = Integer.parseInt(ay.split("-")[0]);
        String expiryStd  = (ayStart + 8)  + "-" + String.format("%02d", (ayStart + 9) % 100);
        String expirySpec = (ayStart + 4)  + "-" + String.format("%02d", (ayStart + 5) % 100);

        if (cfl.hpLossToCarryForward > 0)
            lossLedgerRepo.save(LossLedger.of(pan, ay, "HP", cfl.hpLossToCarryForward, expiryStd));
        if (cfl.businessLossToCarryForward > 0)
            lossLedgerRepo.save(LossLedger.of(pan, ay, "BUSINESS_NON_SPECULATIVE",
                cfl.businessLossToCarryForward, expiryStd));
        if (cfl.speculativeLossToCarryForward > 0)
            lossLedgerRepo.save(LossLedger.of(pan, ay, "SPECULATIVE",
                cfl.speculativeLossToCarryForward, expirySpec));
        if (cfl.stcgLossToCarryForward > 0)
            lossLedgerRepo.save(LossLedger.of(pan, ay, "STCG", cfl.stcgLossToCarryForward, expiryStd));
        if (cfl.ltcgLossToCarryForward > 0)
            lossLedgerRepo.save(LossLedger.of(pan, ay, "LTCG", cfl.ltcgLossToCarryForward, expiryStd));
        if (cfl.unabsorbedDepToCarryForward > 0)
            lossLedgerRepo.save(LossLedger.of(pan, ay, "UNABSORBED_DEPRECIATION",
                cfl.unabsorbedDepToCarryForward, "9999-00")); // never expires
    }
}
```

Also create these companion classes if they don't exist:

```java
// ITRFilingException.java
public class ITRFilingException extends RuntimeException {
    private final List<ValidationError> errors;
    public ITRFilingException(String message, List<ValidationError> errors) {
        super(message);
        this.errors = errors;
    }
    public ITRFilingException(String message) { super(message); this.errors = List.of(); }
    public List<ValidationError> getErrors() { return errors; }
}

// ITRFilingResult.java
public class ITRFilingResult {
    public final String itdJson;
    public final ValidationResult validation;
    public final TaxRegimeComparisonResult regimeComparison;
    public final PreSubmissionResult checklist;
    public final SFTParseResult sft;
    public final boolean success;

    public static ITRFilingResult success(String json, ValidationResult v,
        TaxRegimeComparisonResult r, PreSubmissionResult c, SFTParseResult s) {
        return new ITRFilingResult(json, v, r, c, s, true);
    }
    // constructor, getters
}
```

---

## SECTION 7 — IMPLEMENT PERSISTENCE LAYER

**Current compliance: 0%. Create all three components.**

### 7.1 Create `LossLedger.java` (JPA Entity)

```java
@Entity
@Table(name = "loss_ledger", indexes = {
    @Index(name = "idx_ll_pan_ay",     columnList = "taxpayer_pan, incurred_ay"),
    @Index(name = "idx_ll_pan_active", columnList = "taxpayer_pan, is_expired, remaining_amount")
})
public class LossLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "taxpayer_pan",    nullable = false, length = 10) private String taxpayerPAN;
    @Column(name = "incurred_ay",     nullable = false, length = 7)  private String incurredAY;
    @Column(name = "loss_type",       nullable = false, length = 30) private String lossType;
    @Column(name = "original_amount", nullable = false)              private int originalAmount;
    @Column(name = "utilized_amount", nullable = false)              private int utilizedAmount = 0;
    @Column(name = "remaining_amount",nullable = false)              private int remainingAmount;
    @Column(name = "expiry_ay",       nullable = false, length = 7)  private String expiryAY;
    @Column(name = "is_expired",      nullable = false)              private boolean isExpired = false;
    @Column(name = "created_at")      private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    @Column(name = "updated_at")      private java.time.LocalDateTime updatedAt = java.time.LocalDateTime.now();

    public static LossLedger of(String pan, String ay, String type, int amount, String expiryAY) {
        LossLedger ll = new LossLedger();
        ll.taxpayerPAN   = pan;
        ll.incurredAY    = ay;
        ll.lossType      = type;
        ll.originalAmount  = amount;
        ll.remainingAmount = amount;
        ll.expiryAY      = expiryAY;
        return ll;
    }

    public void utilize(int amount) {
        this.utilizedAmount  += amount;
        this.remainingAmount -= amount;
        if (this.remainingAmount <= 0) {
            this.remainingAmount = 0;
            this.isExpired = true;
        }
        this.updatedAt = java.time.LocalDateTime.now();
    }

    // Standard getters/setters for all fields
}
```

### 7.2 Create `LossLedgerRepository.java`

```java
@Repository
public interface LossLedgerRepository extends JpaRepository<LossLedger, Long> {

    @Query("SELECT l FROM LossLedger l " +
           "WHERE l.taxpayerPAN = :pan " +
           "  AND l.isExpired = false " +
           "  AND l.remainingAmount > 0 " +
           "  AND l.expiryAY >= :currentAY " +
           "ORDER BY l.incurredAY ASC") // oldest losses first (FIFO set-off)
    List<LossLedger> findActive(@Param("pan") String pan, @Param("currentAY") String currentAY);

    List<LossLedger> findByTaxpayerPANAndIncurredAY(String pan, String incurredAY);

    @Query("SELECT l FROM LossLedger l " +
           "WHERE l.taxpayerPAN = :pan " +
           "  AND l.lossType = :lossType " +
           "  AND l.isExpired = false " +
           "  AND l.remainingAmount > 0 " +
           "ORDER BY l.incurredAY ASC")
    List<LossLedger> findActiveByType(@Param("pan") String pan, @Param("lossType") String lossType);
}
```

### 7.3 Database Migration Script

Create `src/main/resources/db/migration/V3__create_loss_ledger_and_audit_trail.sql`:

```sql
-- Loss carry-forward ledger
CREATE TABLE IF NOT EXISTS loss_ledger (
    id               BIGSERIAL    PRIMARY KEY,
    taxpayer_pan     VARCHAR(10)  NOT NULL,
    incurred_ay      VARCHAR(7)   NOT NULL,
    loss_type        VARCHAR(30)  NOT NULL,
    original_amount  INTEGER      NOT NULL,
    utilized_amount  INTEGER      NOT NULL DEFAULT 0,
    remaining_amount INTEGER      NOT NULL,
    expiry_ay        VARCHAR(7)   NOT NULL,
    is_expired       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ll_pan_ay
    ON loss_ledger(taxpayer_pan, incurred_ay);

CREATE INDEX IF NOT EXISTS idx_ll_pan_active
    ON loss_ledger(taxpayer_pan, is_expired, remaining_amount);

-- Audit trail (persistent replacement for in-memory AuditTrailService)
CREATE TABLE IF NOT EXISTS audit_trail (
    id                 BIGSERIAL     PRIMARY KEY,
    taxpayer_pan       VARCHAR(10)   NOT NULL,
    assessment_year    VARCHAR(7)    NOT NULL,
    event_type         VARCHAR(50)   NOT NULL,
    event_description  TEXT          NOT NULL,
    data_source        VARCHAR(100),
    field_name         VARCHAR(200),
    old_value          TEXT,
    new_value          TEXT,
    validation_rule_id VARCHAR(30),
    validation_result  VARCHAR(10),
    created_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_audit_pan_ay
    ON audit_trail(taxpayer_pan, assessment_year);

CREATE INDEX IF NOT EXISTS idx_audit_event_type
    ON audit_trail(event_type, created_at DESC);
```

### 7.4 Upgrade `AuditTrailService.java` to DB Persistence

The existing service is in-memory. Add a `@Autowired AuditTrailRepository` (create this JPA repository) and replace the in-memory list with DB saves:

```java
// AuditTrail.java (JPA Entity)
@Entity
@Table(name = "audit_trail")
public class AuditTrail {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "taxpayer_pan")       private String taxpayerPAN;
    @Column(name = "assessment_year")    private String assessmentYear;
    @Column(name = "event_type")         private String eventType;
    @Column(name = "event_description",  columnDefinition = "TEXT") private String eventDescription;
    @Column(name = "data_source")        private String dataSource;
    @Column(name = "validation_rule_id") private String validationRuleId;
    @Column(name = "validation_result")  private String validationResult;
    @Column(name = "created_at")         private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    @Column(name = "created_by")         private String createdBy;
    // getters/setters
}

// AuditTrailRepository.java
@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {
    List<AuditTrail> findByTaxpayerPANAndAssessmentYear(String pan, String ay);
}
```

---

## SECTION 8 — COMPILE, VERIFY, SMOKE TEST

Run after each section (not just at the end):

```bash
# After Section 1 (DTOs)
mvn compile -q 2>&1 | grep -c "ERROR"
# Expected: 0

# After all sections
mvn clean compile -q
# Expected: BUILD SUCCESS, 0 errors, ~155 Java files

mvn test
# Expected: All tests pass

# Smoke test — start app and POST minimal payloads:
# POST /api/itr/file
# Body: { "pan": "TESTPA0001T", "assessmentYear": "2025-26", 
#         "itrForm": "ITR4", "sec44AD": { "grossTurnover": 5000000, 
#         "turnoverCash": 500000, "turnoverDigital": 4500000, 
#         "totalPresumptiveIncome": 390000 } }
# Expected: 200 OK, validation.errors = [], itdJson non-null
```

---

## SECTION 9 — COMPLETION CHECKLIST

Mark each item ✅ only when: (a) code is written, (b) it compiles clean, (c) unit test exists.

### Section 1 — Itr2FormData Extension
- [ ] `houseProperties: List<PropertyDetail>` added
- [ ] `scheduleCG: ScheduleCGData` added
- [ ] `vdaTransactions: List<VDATransaction>` added
- [ ] `scheduleCYLA`, `scheduleBFLA`, `scheduleCFL` added
- [ ] `schedule80G: Schedule80GData` added
- [ ] `amtApplicable`, `adjustedTotalIncomeForAMT`, `amtTax`, `regularTaxBeforeCess` added
- [ ] `hasForeignAssets`, `scheduleFA`, `scheduleFSI`, `scheduleTR` added
- [ ] `scheduleAL: ScheduleALData` added
- [ ] `stcg111A_preJul23`, `stcg111A_postJul23`, `ltcg112A_preJul23`, `ltcg112A_postJul23` added
- [ ] `vdaIncome`, `lotteryIncome` added
- [ ] All getters and setters generated

### Section 2 — ITR2CBDTValidationService
- [ ] `validateScheduleHP()` — all 12 rules with arithmetic checks
- [ ] `validateSchedule112A()` — all 8 rules with per-row column validation
- [ ] `validateScheduleCYLA()` — all 5 rules with balance checks
- [ ] `validateScheduleVIA()` — all 9 rules with caps and new regime block
- [ ] `validateScheduleVDA()` — income flooring and cross-check
- [ ] `validateAMT()` — 18.5% threshold enforcement
- [ ] CAT-A failures use `result.addError()`, not `addWarning()`

### Section 3 — ITR3CBDTValidationService
- [ ] MSME 43B(h) — day-count check and disallowance cross-check
- [ ] Partner remuneration — 40(b) two-tier formula
- [ ] F&O turnover — absolute value enforcement
- [ ] Schedule DPM — DepreciationEngine recompute cross-check
- [ ] Goodwill → 0% depreciation enforced
- [ ] Additional depreciation half-rate for post-Oct-1 assets
- [ ] Schedule DCG — row arithmetic enforcement, CG cross-reference

### Section 4 — ITR4CBDTValidationService
- [ ] 44AD turnover limit (Rs 2 Cr / Rs 3 Cr) enforced as error
- [ ] 44AD minimum income (8%/6%) enforced as error
- [ ] 44ADA 50% minimum enforced as error
- [ ] 44AE per-vehicle income minimum enforced
- [ ] 10-vehicle limit enforced
- [ ] LTCG 112A > Rs 1.25L → error
- [ ] Simplified balance sheet arithmetic enforced
- [ ] New regime deductions blocked

### Section 5 — VDA and SFT
- [ ] `VDATransactionService.compute()` — real 115BBH implementation
- [ ] `VDATransactionService.parseFromAIS()` — SFT-018 JSON parsing
- [ ] `SFTProcessingService.parseAIS()` — all 17 SFT codes handled
- [ ] `SFTProcessingService.autoPopulateScheduleAL()` — property + financial assets

### Section 6 — Orchestration
- [ ] `ITRFilingOrchestrationService.java` created
- [ ] 11-step pipeline in correct order
- [ ] CAT-A failures throw `ITRFilingException` (hard stop)
- [ ] Loss persistence runs inside same `@Transactional`
- [ ] `ITRFilingException.java` created
- [ ] `ITRFilingResult.java` created

### Section 7 — Persistence
- [ ] `LossLedger.java` (JPA entity) with all columns
- [ ] `LossLedgerRepository.java` with `findActive()` and `findActiveByType()`
- [ ] `AuditTrail.java` (JPA entity) created
- [ ] `AuditTrailRepository.java` created
- [ ] `V3__create_loss_ledger_and_audit_trail.sql` in migrations folder
- [ ] `AuditTrailService.java` upgraded to use DB repository

---

## NON-NEGOTIABLE RULES FOR ALL CODE IN THIS DIRECTIVE

1. **Do not delete any existing service.** If a new file fails to compile, add the missing DTO field or import. Deletion is never the fix.

2. **All monetary values in ITD JSON output = integers.** Use `Math.round()` before casting to int. No decimals anywhere in exported JSON.

3. **All dates in ITD JSON = `DD/MM/YYYY` format.** Use the existing `ITDDateFormatter` utility.

4. **CAT-A validation errors = `result.addError()`.** Warnings = `result.addWarning()`. These are different lists. The orchestration service checks `getErrors()` for hard stops — warnings do not block filing.

5. **VDA loss is always ≥ 0.** `Math.max(0, consideration - cost)`. Section 115BBH(2) prohibits VDA losses from being set off.

6. **F&O turnover = sum of |profit per trade| + sum of |loss per trade|.** Not gross contract value. Not net P&L. This is the most common F&O error.

7. **New regime: only three deductions allowed.** 80CCD(2) employer NPS, 80JJAA, 80CCH(2). All others = 0 and must be blocked with CAT-A error.

8. **ITR-1 is production-ready.** Do not touch it.

9. **Sections must be completed in order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8.** Services depend on DTOs. DTOs must compile before services are written.

10. **Loss ledger persistence is `@Transactional`.** If JSON export fails, losses must not be saved. Wire the orchestration service so both run in the same transaction boundary.

---

*This directive covers all six identified root causes of the 58% compliance gap. All validation rule IDs, tax algorithms, and depreciation formulas are embedded above. No external reference files are required for implementation.*

*Legal basis: Income-tax Act 1961, Finance Act 2025 (AY 2026-27), CBDT e-Filing Validation Rules V1.0/V1.1 (July 2025), Section 115BBH (VDA), Section 43B(h) (MSME), Section 40(b) (Partner remuneration), Section 44AD/44ADA/44AE (Presumptive income).*
