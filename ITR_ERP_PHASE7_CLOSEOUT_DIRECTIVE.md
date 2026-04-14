# ITR ERP — PHASE 7 CLOSE-OUT DIRECTIVE
## Closing the Final 7%: Orchestration Completion + SFT Full Coverage
**Date:** April 11, 2026  
**Prerequisite:** 93% compliant, 0 compile errors, all prior sections implemented  
**Scope:** Exactly two gaps. Do not touch anything else.

---

## GAP SUMMARY

| Gap | Current State | Required State | Section |
|---|---|---|---|
| **Orchestration** | Basic validation flow only (70%) | Full 11-step pipeline with AIS reconciliation, JSON export, loss persistence, audit trail | Section 1 |
| **SFT Categories** | 5 of 17 categories parsed | All 17 SFT categories (SFT-001 through SFT-018) | Section 2 |

**Execution contract:** Complete Section 1 first (it depends on existing services). Section 2 is independent — can run in parallel. Compile after each section. Zero new files needed for Section 2.

---

## SECTION 1 — COMPLETE `ITRFilingOrchestrationService.java`

### 1.1 What Exists vs What Is Missing

The current implementation has this skeleton:
```java
// EXISTING — keep these, build around them:
public FilingResult processITR2(Itr2FormData data)     // validates, sets status
public FilingResult processITR3(Itr3FormData data)     // validates, sets status
public FilingResult processITR4(Itr4FormData data)     // validates, sets status
```

What must be **added**:
- AIS reconciliation call (service already exists: `AISReconciliationService`)
- Loss ledger load from DB before validation (repository already exists: `LossLedgerRepository`)
- JSON export after validation passes (services already exist: `ITR2JSONExportService`, etc.)
- Loss ledger persistence after export (same `@Transactional` scope)
- Pre-submission checklist (service already exists: `PreSubmissionChecklistService`)
- Audit trail DB logging at each step (service and repository already exist)

### 1.2 Required `@Autowired` Injections

Open `ITRFilingOrchestrationService.java` and add these injections if not already present:

```java
@Autowired private AISReconciliationService aisReconciliation;
@Autowired private LossLedgerRepository lossLedgerRepo;
@Autowired private ITR2JSONExportService itr2JsonExporter;
@Autowired private ITR3JSONExportService itr3JsonExporter;
@Autowired private ITR4JSONExportService itr4JsonExporter;
@Autowired private PreSubmissionChecklistService preSubmissionChecklist;
@Autowired private AuditTrailRepository auditTrailRepo;
```

### 1.3 Replace `processITR2()` with the Full Pipeline

Replace the existing `processITR2` method body entirely. Do not change the method signature.

```java
@Transactional
public FilingResult processITR2(Itr2FormData data) {
    String pan = data.getPAN();
    String ay  = data.getAssessmentYear();
    FilingResult result = new FilingResult();

    // ── Step 1: Audit trail — filing started ──────────────────────────────────
    logAudit(pan, ay, "FILING_STARTED", "ITR-2 orchestration pipeline initiated", "SYSTEM");

    // ── Step 2: AIS Reconciliation ────────────────────────────────────────────
    // AISReconciliationService.reconcile() returns discrepancies > Rs 100 tolerance
    try {
        AISReconciliationResult aisResult = aisReconciliation.reconcile(data);
        if (aisResult.hasDiscrepancies()) {
            result.addWarnings(aisResult.getDiscrepancies()); // warnings only — not a hard stop
            logAudit(pan, ay, "AIS_DISCREPANCY",
                aisResult.getDiscrepancies().size() + " AIS discrepancies found (tolerance Rs 100)",
                "AIS_RECONCILIATION");
        }
    } catch (Exception e) {
        // AIS reconciliation failure is non-fatal — log and continue
        logAudit(pan, ay, "AIS_RECONCILIATION_SKIPPED", "AIS service unavailable: " + e.getMessage(), "AIS");
    }

    // ── Step 3: Load brought-forward losses from LossLedger ──────────────────
    List<LossLedger> bfLosses = lossLedgerRepo.findActive(pan, ay);
    if (!bfLosses.isEmpty()) {
        data.setBroughtForwardLosses(
            bfLosses.stream().map(this::toLossLedgerEntry).collect(java.util.stream.Collectors.toList())
        );
        logAudit(pan, ay, "BF_LOSSES_LOADED",
            bfLosses.size() + " brought-forward loss entries loaded from DB", "LOSS_LEDGER");
    }

    // ── Step 4: CBDT Validation (CAT-A = hard stop) ───────────────────────────
    ITR2CBDTValidationService.ValidationResult validation = itr2Validator.validate(data);
    logAudit(pan, ay, "VALIDATION_COMPLETE",
        "Errors: " + validation.getErrors().size() + ", Warnings: " + validation.getWarnings().size(),
        "ITR2_VALIDATOR");

    if (!validation.getErrors().isEmpty()) {
        result.setStatus("VALIDATION_FAILED");
        result.setErrors(validation.getErrors());
        result.setWarnings(validation.getWarnings());
        logAudit(pan, ay, "FILING_BLOCKED",
            validation.getErrors().size() + " CAT-A errors prevent filing", "SYSTEM");
        return result; // Hard stop — no JSON, no persistence
    }

    // ── Step 5: Pre-submission checklist ─────────────────────────────────────
    try {
        PreSubmissionResult checklistResult = preSubmissionChecklist.run(data);
        result.setChecklistResult(checklistResult);
        if (checklistResult.hasBlockingItems()) {
            result.setStatus("CHECKLIST_FAILED");
            result.setErrors(checklistResult.getBlockingItemMessages());
            logAudit(pan, ay, "CHECKLIST_BLOCKED",
                checklistResult.getBlockingItems().size() + " checklist items failed", "CHECKLIST");
            return result;
        }
    } catch (Exception e) {
        logAudit(pan, ay, "CHECKLIST_SKIPPED", "Checklist service error: " + e.getMessage(), "CHECKLIST");
    }

    // ── Step 6: Generate ITR-2 JSON ───────────────────────────────────────────
    String itdJson;
    try {
        itdJson = itr2JsonExporter.export(data);
        logAudit(pan, ay, "JSON_GENERATED", "ITR-2 JSON generated successfully", "ITR2_EXPORTER");
    } catch (Exception e) {
        result.setStatus("JSON_EXPORT_FAILED");
        result.addError("ITR-2 JSON generation failed: " + e.getMessage());
        logAudit(pan, ay, "JSON_EXPORT_FAILED", e.getMessage(), "ITR2_EXPORTER");
        return result; // Rollback — losses not persisted (same @Transactional)
    }

    // ── Step 7: Persist carry-forward losses ──────────────────────────────────
    // Runs in same @Transactional — if this fails, JSON step does NOT rollback
    // (JSON is already generated as a String; only DB state is transactional)
    persistCarryForwardLosses(pan, ay, data.getScheduleCFL());
    logAudit(pan, ay, "LOSSES_PERSISTED", "Carry-forward losses saved to LossLedger", "LOSS_LEDGER");

    // ── Step 8: Mark existing BF losses as utilized ───────────────────────────
    if (!bfLosses.isEmpty()) {
        applyBFLossUtilization(bfLosses, data.getScheduleBFLA());
    }

    // ── Step 9: Final result ──────────────────────────────────────────────────
    result.setStatus("SUCCESS");
    result.setItdJson(itdJson);
    result.setValidationResult(validation);
    logAudit(pan, ay, "FILING_COMPLETE", "ITR-2 filing pipeline completed successfully", "SYSTEM");

    return result;
}
```

### 1.4 Apply the Same Pattern to `processITR3()` and `processITR4()`

The logic is identical — only the form data type and exporter change. Replace those two methods with the same 9-step pattern, substituting:
- `Itr3FormData` / `itr3Validator` / `itr3JsonExporter` for ITR-3
- `Itr4FormData` / `itr4Validator` / `itr4JsonExporter` for ITR-4

ITR-4 does not have `scheduleCFL` (no loss carry-forward) — skip Steps 7 and 8 for ITR-4.

### 1.5 Add These Private Helper Methods

```java
// ── Persist carry-forward losses from ScheduleCFLData ────────────────────────
private void persistCarryForwardLosses(String pan, String ay, ScheduleCFLData cfl) {
    if (cfl == null) return;

    int ayStart = Integer.parseInt(ay.split("-")[0]);
    String expiryStd  = (ayStart + 8) + "-" + String.format("%02d", (ayStart + 9) % 100);
    String expirySpec = (ayStart + 4) + "-" + String.format("%02d", (ayStart + 5) % 100);

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

// ── Mark utilized BF losses as consumed ─────────────────────────────────────
private void applyBFLossUtilization(List<LossLedger> bfLosses, ScheduleBFLAData bfla) {
    if (bfla == null) return;
    // For each DB loss entry, call ll.utilize(amount) based on BFLA set-off amounts
    // The LossSetOffEngine already computed how much of each AY's loss was used
    bfla.getAllSetOffEntries().forEach(entry -> {
        bfLosses.stream()
            .filter(ll -> ll.getIncurredAY().equals(entry.incurredAY)
                       && ll.getLossType().equals(entry.lossType.name()))
            .findFirst()
            .ifPresent(ll -> {
                ll.utilize(entry.utilizedAmount);
                lossLedgerRepo.save(ll);
            });
    });
}

// ── Audit trail helper ───────────────────────────────────────────────────────
private void logAudit(String pan, String ay, String eventType, String description, String source) {
    try {
        AuditTrail entry = AuditTrail.of(pan, ay, eventType, description, source);
        auditTrailRepo.save(entry);
    } catch (Exception e) {
        // Audit logging failure must never block the main pipeline
    }
}

// ── Convert DB LossLedger entity to DTO entry ────────────────────────────────
private LossLedgerEntry toLossLedgerEntry(LossLedger ll) {
    LossLedgerEntry e = new LossLedgerEntry();
    e.incurredAY    = ll.getIncurredAY();
    e.lossType      = LossType.valueOf(ll.getLossType());
    e.originalAmount = ll.getOriginalAmount();
    e.utilizedSoFar  = ll.getUtilizedAmount();
    e.remaining      = ll.getRemainingAmount();
    e.expiryAY       = ll.getExpiryAY();
    return e;
}
```

### 1.6 Add Required Fields to `FilingResult` (if not present)

```java
public class FilingResult {
    private String status;           // SUCCESS | VALIDATION_FAILED | CHECKLIST_FAILED | JSON_EXPORT_FAILED
    private String itdJson;          // null if filing did not reach export step
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private ITR2CBDTValidationService.ValidationResult validationResult;
    private PreSubmissionResult checklistResult;

    public void addError(String msg) { this.errors.add(msg); }
    public void addWarnings(List<String> msgs) { this.warnings.addAll(msgs); }
    public boolean isSuccess() { return "SUCCESS".equals(status); }
    // Standard getters/setters
}
```

### 1.7 Compile Check After Section 1

```bash
mvn compile -q 2>&1 | grep "ERROR"
# Expected: zero output
```

---

## SECTION 2 — COMPLETE `SFTProcessingService.java` — All 17 Categories

### 2.1 What Exists vs What Is Missing

Existing (keep these — they work):
```
SFT-012: Property Purchase
SFT-013: Property Sale  
SFT-014: Foreign Remittance (LRS)
SFT-015: Cash Deposits
SFT-016: Cash Withdrawals
```

Missing (add these 12):
```
SFT-001: Savings account deposits > Rs 10L/year
SFT-002: Savings account withdrawals (cash)
SFT-003: Fixed/Recurring deposit opening > Rs 10L
SFT-004: Credit card bill payment (cash > Rs 1L; other > Rs 10L)
SFT-005: Share/debenture acquisition > Rs 10L
SFT-006: Share buyback proceeds
SFT-007: Mutual fund purchase > Rs 10L
SFT-008: Mutual fund redemption
SFT-009: Foreign exchange purchase > Rs 10L
SFT-010: Property purchase > Rs 30L (by sub-registrar)
SFT-011: Bond/debenture acquisition > Rs 10L
SFT-017: Dividend income
SFT-018: VDA / crypto (already handled by VDATransactionService — just route here)
```

### 2.2 Add the Missing SFT Category Methods

Open `SFTProcessingService.java`. Locate the `processSFTEntry()` or equivalent routing method. Add a case for each missing code below. If the method uses `if-else`, convert to a `switch` statement first:

```java
// ADD inside the SFT routing switch/if-else in SFTProcessingService.parseAIS()
// or wherever processSFTEntry() is called

case "SFT-001":
    // Savings account deposits > Rs 10L reported by banks
    // Impact: flag if sum > Rs 10L — may indicate unexplained income
    int sft001Amount = entry.path("transactionAmount").asInt(0);
    result.savingsDeposits.add(new SFTEntry("SFT-001", sft001Amount, pan, date));
    if (sft001Amount > 10_00_000) {
        result.highValueAlerts.add("SFT-001: Savings deposit Rs " + sft001Amount +
            " > Rs 10L. Reconcile with income declared.");
    }
    break;

case "SFT-002":
    // Savings/current account cash withdrawals > Rs 50L
    int sft002Amount = entry.path("transactionAmount").asInt(0);
    result.savingsWithdrawals.add(new SFTEntry("SFT-002", sft002Amount, pan, date));
    if (sft002Amount > 50_00_000) {
        result.highValueAlerts.add("SFT-002: Cash withdrawal Rs " + sft002Amount +
            " > Rs 50L. May trigger AIS scrutiny.");
    }
    break;

case "SFT-003":
    // Fixed/recurring deposit opening > Rs 10L (total in a year, any bank)
    int sft003Amount = entry.path("transactionAmount").asInt(0);
    result.fixedDeposits.add(new SFTEntry("SFT-003", sft003Amount, pan, date));
    if (sft003Amount > 10_00_000) {
        result.highValueAlerts.add("SFT-003: FD opening Rs " + sft003Amount +
            " > Rs 10L. Interest income must be declared under Other Sources.");
    }
    break;

case "SFT-004":
    // Credit card bill payments: cash > Rs 1L per transaction; other > Rs 10L/year
    int sft004Amount = entry.path("transactionAmount").asInt(0);
    String payMode = entry.path("paymentMode").asText("OTHER");
    result.creditCardPayments.add(new SFTEntry("SFT-004", sft004Amount, pan, date));
    boolean isCashPayment = "CASH".equalsIgnoreCase(payMode);
    int ccThreshold = isCashPayment ? 1_00_000 : 10_00_000;
    if (sft004Amount > ccThreshold) {
        result.highValueAlerts.add("SFT-004: Credit card payment Rs " + sft004Amount +
            " (" + payMode + ") exceeds threshold Rs " + ccThreshold);
    }
    break;

case "SFT-005":
    // Share/debenture acquisition (primary market) > Rs 10L
    int sft005Amount = entry.path("transactionAmount").asInt(0);
    result.shareAcquisitions.add(new SFTEntry("SFT-005", sft005Amount, pan, date));
    // Cost of acquisition — feed into Schedule CG if sold during year
    result.scheduleALCandidates.add(new SFTEntry("SFT-005", sft005Amount, pan, date));
    break;

case "SFT-006":
    // Share buyback proceeds — treated as capital gains (not dividend post-2019)
    int sft006Amount = entry.path("transactionAmount").asInt(0);
    result.shareBuybacks.add(new SFTEntry("SFT-006", sft006Amount, pan, date));
    result.highValueAlerts.add("SFT-006: Share buyback proceeds Rs " + sft006Amount +
        ". Verify capital gains declared in Schedule CG (Section 46A).");
    break;

case "SFT-007":
    // Mutual fund purchase (NAV-based) > Rs 10L/year
    int sft007Amount = entry.path("transactionAmount").asInt(0);
    result.mutualFundPurchases.add(new SFTEntry("SFT-007", sft007Amount, pan, date));
    result.scheduleALCandidates.add(new SFTEntry("SFT-007", sft007Amount, pan, date));
    break;

case "SFT-008":
    // Mutual fund redemption — capital gains trigger
    int sft008Amount = entry.path("transactionAmount").asInt(0);
    result.mutualFundRedemptions.add(new SFTEntry("SFT-008", sft008Amount, pan, date));
    result.highValueAlerts.add("SFT-008: MF redemption Rs " + sft008Amount +
        ". Verify STCG/LTCG declared in Schedule CG.");
    break;

case "SFT-009":
    // Foreign exchange purchase > Rs 10L (LRS / forex dealer reporting)
    int sft009Amount = entry.path("transactionAmount").asInt(0);
    result.forexTransactions.add(new SFTEntry("SFT-009", sft009Amount, pan, date));
    if (sft009Amount > 10_00_000) {
        result.highValueAlerts.add("SFT-009: Forex purchase Rs " + sft009Amount +
            " > Rs 10L. If remittance, verify Schedule FA/FSI.");
    }
    break;

case "SFT-010":
    // Property purchase > Rs 30L reported by sub-registrar / stamp duty authority
    // This is distinct from SFT-012 (your existing implementation may overlap)
    // SFT-010 = sub-registrar; SFT-012 = builder/developer
    int sft010Amount = entry.path("transactionAmount").asInt(0);
    result.propertyPurchases.add(new SFTEntry("SFT-010", sft010Amount, pan, date));
    result.scheduleALCandidates.add(new SFTEntry("SFT-010", sft010Amount, pan, date));
    if (sft010Amount > 30_00_000) {
        result.highValueAlerts.add("SFT-010: Property purchase Rs " + sft010Amount +
            " reported by sub-registrar. Verify Schedule AL (if income > Rs 50L).");
    }
    break;

case "SFT-011":
    // Bond/debenture acquisition > Rs 10L
    int sft011Amount = entry.path("transactionAmount").asInt(0);
    result.bondAcquisitions.add(new SFTEntry("SFT-011", sft011Amount, pan, date));
    result.scheduleALCandidates.add(new SFTEntry("SFT-011", sft011Amount, pan, date));
    break;

case "SFT-017":
    // Dividend income reported by companies/RTA (post Section 194 TDS)
    int sft017Amount = entry.path("transactionAmount").asInt(0);
    result.dividendIncome.add(new SFTEntry("SFT-017", sft017Amount, pan, date));
    result.highValueAlerts.add("SFT-017: Dividend Rs " + sft017Amount +
        " reported. Must be declared under Other Sources (taxable since FY 2020-21).");
    break;

case "SFT-018":
    // VDA/crypto — route to VDATransactionService, do not double-count here
    result.vdaTransactions.add(new SFTEntry("SFT-018",
        entry.path("transactionAmount").asInt(0), pan, date));
    // VDATransactionService.parseFromAIS() handles the detailed computation
    break;

default:
    result.unrecognizedEntries.add(type + ":" + entry.path("transactionAmount").asInt(0));
    break;
```

### 2.3 Add Missing Fields to `SFTParseResult`

The new categories reference list fields that may not exist in `SFTParseResult`. Add these if absent:

```java
public class SFTParseResult {
    // EXISTING (keep):
    public List<SFTEntry> propertyPurchases   = new ArrayList<>();
    public List<SFTEntry> propertySales        = new ArrayList<>();
    public List<SFTEntry> cashDeposits         = new ArrayList<>();
    public List<SFTEntry> cashWithdrawals      = new ArrayList<>();
    public List<SFTEntry> vdaTransactions      = new ArrayList<>();
    public List<String>   highValueAlerts      = new ArrayList<>();
    public List<String>   parseErrors          = new ArrayList<>();
    public List<String>   unrecognizedEntries  = new ArrayList<>();

    // ADD (missing from prior implementation):
    public List<SFTEntry> savingsDeposits      = new ArrayList<>(); // SFT-001
    public List<SFTEntry> savingsWithdrawals   = new ArrayList<>(); // SFT-002
    public List<SFTEntry> fixedDeposits        = new ArrayList<>(); // SFT-003
    public List<SFTEntry> creditCardPayments   = new ArrayList<>(); // SFT-004
    public List<SFTEntry> shareAcquisitions    = new ArrayList<>(); // SFT-005
    public List<SFTEntry> shareBuybacks        = new ArrayList<>(); // SFT-006
    public List<SFTEntry> mutualFundPurchases  = new ArrayList<>(); // SFT-007
    public List<SFTEntry> mutualFundRedemptions= new ArrayList<>(); // SFT-008
    public List<SFTEntry> forexTransactions    = new ArrayList<>(); // SFT-009
    public List<SFTEntry> bondAcquisitions     = new ArrayList<>(); // SFT-011
    public List<SFTEntry> dividendIncome       = new ArrayList<>(); // SFT-017
    public List<SFTEntry> scheduleALCandidates = new ArrayList<>(); // all asset acquisitions
    public List<SFTEntry> hotelBills           = new ArrayList<>(); // SFT-013 (if named differently)
    public List<SFTEntry> ddPurchases          = new ArrayList<>(); // SFT-014 (if named differently)

    // Utility
    public int totalEntryCount() {
        return savingsDeposits.size() + savingsWithdrawals.size() + fixedDeposits.size() +
               creditCardPayments.size() + shareAcquisitions.size() + shareBuybacks.size() +
               mutualFundPurchases.size() + mutualFundRedemptions.size() + forexTransactions.size() +
               propertyPurchases.size() + propertySales.size() + bondAcquisitions.size() +
               cashDeposits.size() + cashWithdrawals.size() + dividendIncome.size() +
               vdaTransactions.size();
    }
}
```

### 2.4 Update `autoPopulateScheduleAL()` to Include New Asset Categories

The existing method only used property + financial assets. Extend it:

```java
public ScheduleALData autoPopulateScheduleAL(SFTParseResult sft) {
    ScheduleALData al = new ScheduleALData();

    // Immovable property (SFT-010, SFT-012 — purchase cost)
    al.immovablePropertyCost = sft.propertyPurchases.stream().mapToInt(e -> e.amount).sum();

    // Financial assets (SFT-005, SFT-007, SFT-003, SFT-011)
    al.financialAssets =
        sft.shareAcquisitions.stream().mapToInt(e -> e.amount).sum()
        + sft.mutualFundPurchases.stream().mapToInt(e -> e.amount).sum()
        + sft.fixedDeposits.stream().mapToInt(e -> e.amount).sum()
        + sft.bondAcquisitions.stream().mapToInt(e -> e.amount).sum();

    // Net of disposals (subtract sold assets — SFT-006 buybacks, SFT-008 MF redemptions)
    int disposals =
        sft.shareBuybacks.stream().mapToInt(e -> e.amount).sum()
        + sft.mutualFundRedemptions.stream().mapToInt(e -> e.amount).sum();
    al.financialAssets = Math.max(0, al.financialAssets - disposals);

    al.autoPopulated = true;
    al.requiresManualVerification = true; // SFT reports cost, not current market value
    al.verificationNote = "Schedule AL auto-populated from AIS/SFT data. " +
        "Amounts reflect transaction cost, not current market value. " +
        "Verify and update current market values before filing.";

    return al;
}
```

---

## SECTION 3 — COMPILE AND VERIFY

```bash
# After Section 1 and Section 2:
mvn clean compile -q
# Expected: BUILD SUCCESS, 0 errors

mvn test
# Expected: all tests pass

# Verify file count increased from 145 to ~157
find src/main/java -name "*.java" | wc -l
# Expected: 155-160
```

### Smoke Tests

```bash
# Test 1: ITR-2 validation pipeline with loss carry-forward
POST /api/itr/file/itr2
{
  "pan": "TESTPA0001T",
  "assessmentYear": "2025-26",
  "taxRegime": "NEW",
  "grossSalary": 1500000,
  "houseProperties": [],
  "scheduleCG": { "schedule112ARows": [], "totalCGIncome": 0 }
}
# Expected: 200 OK, status = "SUCCESS" or specific validation errors (not NPE/500)

# Test 2: SFT parsing — all 17 types
POST /api/sft/parse
Body: AIS JSON with one entry of each SFT type (SFT-001 through SFT-018)
# Expected: SFTParseResult with 17 entries, 0 unrecognizedEntries, 0 parseErrors

# Test 3: Loss carry-forward persistence
# File ITR-2 with ScheduleCFLData.hpLossToCarryForward = 150000
# Then query: SELECT * FROM loss_ledger WHERE taxpayer_pan = 'TESTPA0001T'
# Expected: 1 row, loss_type = 'HP', remaining_amount = 150000, is_expired = false
```

---

## FINAL STATE AFTER THIS DIRECTIVE

| Component | Before | After |
|---|---|---|
| Orchestration | 70% | 100% |
| SFT Coverage | 80% (5/17) | 100% (17/17) |
| **Overall CBDT Compliance** | **93%** | **100%** |

---

## NON-NEGOTIABLE RULES (repeat from prior directive — still apply)

1. Audit trail logging failures (`logAudit()`) must **never** throw exceptions that block the main pipeline. Wrap in try-catch.
2. AIS reconciliation failure is **non-fatal** — log and continue. The taxpayer may not have AIS data.
3. Loss ledger persistence and JSON export run in the **same `@Transactional` scope** in `processITR2/3`. If JSON export throws, the transaction rolls back and losses are not persisted.
4. SFT-018 (VDA) entries must **only be added to `result.vdaTransactions`** in `SFTProcessingService`. The actual VDA income computation stays exclusively in `VDATransactionService`. Do not compute VDA income in SFT service.
5. All `SFTEntry` amounts are transaction amounts, not income. A savings deposit of Rs 10L is not Rs 10L of income — it is a flag for reconciliation only.
6. `ITR-1` — untouched.
