# ITR ERP — PHASE 6 CONTINUATION DIRECTIVE
## DTO Extensions → Service Re-implementation → Full Wiring → Deployment
**Date:** April 11, 2026  
**Prepared For:** Agentic AI Implementation  
**Prerequisite:** Phase 5 complete — 140 files compiled, engines built

---

## CURRENT STATE AFTER PHASE 5

### ✅ Now Exists (DO NOT TOUCH):
- `AY202627TaxComputationService.java` — AY 2026-27 slabs, Rs 12L rebate, marginal relief ✅
- `LossSetOffEngine.java` — CYLA/BFLA matrix, 8-year limits ✅
- `DepreciationEngine.java` — DPM block-of-assets, half-rate rule, terminal depreciation ✅
- `ITR2Output.java` — Complete ITR-2 JSON structure ✅
- All Phase 1–4 services intact ✅

### ❌ Removed by Phase 5 (Must Re-implement Now):
- `ITR2CBDTValidationService.java` — removed due to missing DTO fields
- `ITR3CBDTValidationService.java` — removed due to missing DTO fields
- `ITR4CBDTValidationService.java` — removed due to missing DTO fields
- `ITR2JSONExportService.java` — removed due to missing DTO fields
- `ITR3JSONExportService.java` — removed due to missing DTO fields
- `ITR4JSONExportService.java` — removed due to missing DTO fields
- `VDATransactionService.java` — placeholder still not replaced
- `SFTProcessingService.java` — stubs still not replaced

### The Exact Problem Phase 5 Hit:
The existing `Itr2FormData.java`, `Itr3FormData.java`, and `Itr4FormData.java` DTOs did not have the fields the new validation services needed. The AI deleted the services instead of extending the DTOs. **This phase fixes the DTOs first, then re-implements the services on top of them.**

---

## EXECUTION ORDER — FOLLOW STRICTLY

```
Step 1: Extend all DTOs (this file, Sections 1–5)
Step 2: Re-implement ITR-2 validation + export (Section 6)
Step 3: Re-implement ITR-3 validation + export (Section 7)
Step 4: Re-implement ITR-4 validation + export (Section 8)
Step 5: Replace VDA + SFT placeholders (Section 9)
Step 6: Wire orchestration service (Section 10)
Step 7: Persistence — audit trail + loss ledger (Section 11)
Step 8: Compile and verify (Section 12)
```

**Do not skip ahead. Each step depends on the previous.**

---

## SECTION 1 — EXTEND `Itr2FormData.java`

Open the existing `Itr2FormData.java`. Add every field below that does not already exist. Do not remove existing fields.

```java
// ─── ADD TO Itr2FormData.java ───────────────────────────────────────────────

import java.time.LocalDate;
import java.util.*;

// --- House Property ---
private List<PropertyDetail> houseProperties = new ArrayList<>();
// PropertyDetail is defined in Section 3 of this document

// --- Capital Gains ---
private ScheduleCGData scheduleCG = new ScheduleCGData();
// ScheduleCGData is defined in Section 3

// --- VDA ---
private List<VDATransaction> vdaTransactions = new ArrayList<>();

// --- Loss Schedules ---
private ScheduleCYLAData scheduleCYLA;    // current year loss adjustment
private ScheduleBFLAData scheduleBFLA;    // brought forward loss adjustment
private ScheduleCFLData scheduleCFL;      // carry forward losses

// --- Chapter VI-A ---
private Schedule80GData schedule80G;      // donation-wise
private boolean amtApplicable = false;
private int adjustedTotalIncomeForAMT = 0;
private int amtTax = 0;                   // 18.5% of ATI if applicable
private int amtCreditBroughtForward = 0;
private int amtCreditUtilized = 0;
private int amtCreditCarriedForward = 0;

// --- Foreign ---
private boolean hasForeignAssets = false;
private String scheduleFA;                // JSON string of FA details (accept as-is)
private String scheduleFSI;              // foreign source income
private String scheduleTR;               // tax relief / foreign tax credit

// --- Assets & Liabilities ---
private ScheduleALData scheduleAL;        // mandatory if income > Rs 50L

// --- SPI / PTI ---
private boolean hasClubbing = false;       // minor child / spouse income clubbing

// --- Special Rate Income ---
// (post-July 23 rates are the new AY 2025-26 default)
private int stcg111A_preJul23 = 0;        // at 15%
private int stcg111A_postJul23 = 0;       // at 20%
private int ltcg112A_preJul23 = 0;        // at 10%, exemption Rs 1,00,000
private int ltcg112A_postJul23 = 0;       // at 12.5%, exemption Rs 1,25,000
private int vdaIncome = 0;                // at 30%, no set-off
private int lotteryIncome = 0;            // at 30%

// --- Prior Year Losses (from LossLedger) ---
private List<LossLedgerEntry> broughtForwardLosses = new ArrayList<>();

// --- Regime ---
private TaxRegime taxRegime;              // OLD or NEW
private String assessmentYear;            // "2025-26" or "2026-27"

// --- 89A relief ---
private int reliefUs89A = 0;
private boolean isHUF = false;            // HUF cannot claim 89A
```

---

## SECTION 2 — EXTEND `Itr3FormData.java`

Open the existing `Itr3FormData.java`. Add every field below that does not already exist.

```java
// ─── ADD TO Itr3FormData.java ────────────────────────────────────────────────

// --- Business Income ---
private ScheduleBPData scheduleBP = new ScheduleBPData();
// ScheduleBPData defined in Section 3

// --- Depreciation ---
private ScheduleDPMData scheduleDPM = new ScheduleDPMData();
private ScheduleDCGData scheduleDCG = new ScheduleDCGData();
// ScheduleDPMData and ScheduleDCGData defined in Section 3

// --- F&O ---
private FandODetail fandoDetail;
// FandODetail: { int fandoTurnover; int fandoProfit; boolean isSpeculative; }
// CRITICAL: fandoTurnover = sum of ABSOLUTE profit/loss per trade, NOT gross contract value

// --- MSME Payments (for 43B(h) disallowance) ---
private List<MSMEPayment> msmePayments = new ArrayList<>();
// MSMEPayment: { String supplierName; String supplierPAN; boolean isMSMERegistered;
//   LocalDate supplyDate; Integer paymentDate; int amount; }

// --- Section 43B items ---
private List<Section43BItem> section43BItems = new ArrayList<>();
// Section43BItem: { String description; String category; // TAX|PF|ESIC|BONUS|INTEREST|MSME
//   int amount; LocalDate paymentDate; boolean paidBeforeFilingDate; }

// --- Employee PF (36(1)(va)) ---
private List<EPFContribution> employeePFContributions = new ArrayList<>();
// EPFContribution: { int amount; LocalDate dueDate; LocalDate depositDate;
//   boolean depositedBeforeDueDate; }

// --- Partner Remuneration ---
private int bookProfitForPartnerRemuneration = 0;
private int partnerRemunerationClaimed = 0;
// Limit = max(150000, 90% of first 3L + 60% of balance book profit)

// --- Audit ---
private boolean isSubjectToTaxAudit = false;
private String auditReportFormNo;    // 3CA-3CD or 3CB-3CD
private LocalDate auditReportDate;

// --- Balance Sheet (full or abridged) ---
private BalanceSheetData balanceSheet = new BalanceSheetData();
private ProfitAndLossData profitAndLoss = new ProfitAndLossData();
// BalanceSheetData / ProfitAndLossData defined in Section 3

// --- GST ---
private List<GSTRegistration> gstRegistrations = new ArrayList<>();
// GSTRegistration: { String gstin; String businessName;
//   int turnoverGSTR1; int turnoverGSTR3B; int turnoverBooks; String reasonForDiff; }

// --- CG (ITR-3 also has capital gains) ---
private ScheduleCGData scheduleCG = new ScheduleCGData();
private List<VDATransaction> vdaTransactions = new ArrayList<>();
private List<PropertyDetail> houseProperties = new ArrayList<>();
private ScheduleCYLAData scheduleCYLA;
private ScheduleBFLAData scheduleBFLA;
private ScheduleCFLData scheduleCFL;
private Schedule80GData schedule80G;
private ScheduleALData scheduleAL;

// --- Regime ---
private TaxRegime taxRegime;
private String assessmentYear;
private List<LossLedgerEntry> broughtForwardLosses = new ArrayList<>();

// --- AMT ---
private boolean amtApplicable = false;
private int adjustedTotalIncomeForAMT = 0;
private int amtTax = 0;

// --- Speculative ---
private int speculativeBusinessIncome = 0;
private int speculativeBusinessLoss = 0;
// speculative = intraday equity; must be kept separate from non-speculative

// --- 80JJAA ---
private boolean sec80JJAAEligible = false;
private int newEmployeesCount = 0;
private int sec80JJAADeduction = 0;
// Condition: max emoluments Rs 25,000/month; must be under tax audit
```

---

## SECTION 3 — EXTEND `Itr4FormData.java`

Open the existing `Itr4FormData.java`. Add every field below that does not already exist.

```java
// ─── ADD TO Itr4FormData.java ────────────────────────────────────────────────

// --- Presumptive Income ---
private Sec44ADData sec44AD;
private Sec44ADAData sec44ADA;
private Sec44AEData sec44AE;

// Sec44ADData fields:
//   int grossTurnover; int turnoverCash; int turnoverDigital;
//   int grossIncomeCash8pct;    // = 8% of turnoverCash — computed, not user-entered
//   int grossIncomeDigital6pct; // = 6% of turnoverDigital — computed
//   int totalPresumptiveIncome; // = grossIncomeCash8pct + grossIncomeDigital6pct
//   String businessCode;        // ITD business code from dropdown
//   boolean priorYear44ADClaimed; // triggers opt-out lock-in check

// Sec44ADAData fields:
//   int grossReceipts; int receiptsCash; int receiptsDigital;
//   int presumptiveIncome;  // = 50% of grossReceipts — minimum; taxpayer can declare higher
//   String professionCode;  // ITD profession code

// Sec44AEData fields:
//   List<VehicleDetail> vehicles;
//   int totalPresumptiveIncome;
//   // VehicleDetail: { String registrationNumber; boolean isHeavyVehicle;
//   //   double gvwInTonnes; int monthsOwned; int declaredIncome; }

// --- Simplified Balance Sheet (MANDATORY) ---
private SimplifiedBalanceSheet simplifiedBalanceSheet = new SimplifiedBalanceSheet();
// Fields: int grossReceipts; int netProfit; int sundryDebtors; int sundryCreditors;
//   int stockInTrade; int cashBalance; int openingCapital; int drawings;
//   int additionsToCapital; int closingCapital;
//   int securedLoans; int unsecuredLoans; int totalFixedAssetsWDV;

// --- One House Property ---
private PropertyDetail houseProperty;   // single, not list — ITR-4 max 1 HP

// --- LTCG 112A only (ITR-4 allows <= Rs 1.25L) ---
private int ltcg112A = 0;  // if > 125000: validation error, must file ITR-3

// --- GST ---
private List<GSTRegistration> gstRegistrations = new ArrayList<>();

// --- Schedule AL ---
private ScheduleALData scheduleAL;      // if totalIncome > Rs 50L

// --- Regime ---
private TaxRegime taxRegime;            // ITR-4 new regime: only 80CCD(2), 80CCH allowed
private String assessmentYear;

// --- Prior year ---
private boolean priorYear44ADClaimed = false;
private boolean businessLossCarryForwardAttempted = false; // must be false — error if true

// --- Audit status (if opting out of 44AD) ---
private String auditStatus;  // "AUDIT_REQUIRED" | "NOT_REQUIRED" | "UNDER_AUDIT"
```

---

## SECTION 4 — CREATE SHARED INNER DTO CLASSES

Create a new file `ITRSharedDtos.java` in the DTO package. These classes are referenced by Itr2/3/4FormData.

```java
// ─── ITRSharedDtos.java ──────────────────────────────────────────────────────
// All fields are public for simplicity; add getters/setters per project convention

public class ITRSharedDtos {

    // ── PropertyDetail ────────────────────────────────────────────────────────
    public static class PropertyDetail {
        public String address;
        public String pinCode;
        public PropertyType type;        // SELF_OCCUPIED, LET_OUT, DEEMED_LET_OUT
        public LocalDate dateOfAcquisition;
        public int ownershipSharePct;    // 100 if sole owner
        public boolean isCoOwned;
        public String coOwnerPAN;
        public int coOwnerSharePct;
        public int grossAnnualValue;     // for LO/DLO: annual rent received
        public int municipalTax;         // paid by owner during year
        public int netAnnualValue;       // = grossAnnualValue - municipalTax
        public int standardDeduction;   // = 30% of netAnnualValue (computed)
        public int interestOnBorrowedCapital;
        public int preConstructionInterestInstallment; // 1/5th of pre-construction interest
        public int arrearUnrealisedRent;
        public int hpIncome;             // NAV - 30% - interest + arrear (computed)
        public boolean hasPassThroughIncome;
        public int passThroughIncome;
        public String loanAccountNumber;
        public String lenderPAN;
        public String lenderName;
    }

    // ── ScheduleCGData ────────────────────────────────────────────────────────
    public static class ScheduleCGData {
        // STCG
        public int stcg111A_preJul23 = 0;       // at 15%
        public int stcg111A_postJul23 = 0;      // at 20%
        public int stcgAtSlab = 0;              // at normal slab rate
        public int totalSTCG = 0;

        // LTCG
        public List<Schedule112ARow> schedule112ARows = new ArrayList<>();
        public int ltcg112A_preJul23 = 0;       // at 10%
        public int ltcg112A_postJul23 = 0;      // at 12.5%
        public int ltcgPropertyWithIndexation = 0;   // at 20%
        public int ltcgPropertyWithoutIndexation = 0; // at 12.5%
        public int totalLTCG = 0;

        // Exemptions u/s 54/54EC/54F
        public int exemptionUs54 = 0;
        public int exemptionUs54EC = 0;
        public int exemptionUs54F = 0;
        public boolean cgasDepositConfirmed = false;

        // Section 50C — property transactions
        public int actualSalePrice = 0;
        public int stampDutyValue = 0;
        public int fullValueOfConsideration = 0; // max(actual, SDV) subject to 110% rule

        // Totals
        public int totalCGIncome = 0;            // totalSTCG + totalLTCG
    }

    // ── Schedule112ARow ───────────────────────────────────────────────────────
    public static class Schedule112ARow {
        public String isinCode;
        public String nameOfScrip;
        public int units;
        public int salePricePerUnit;
        public int totalSaleValue;               // Col6 = units * salePricePerUnit
        public LocalDate dateOfAcquisition;
        public LocalDate dateOfTransfer;
        public int actualCostOfAcquisition;      // Col8
        public int fmvPerShareOn31Jan2018;       // Col10 — only if acquired before 1 Feb 2018
        public int totalFMV55_2_ac;              // Col11 = units * fmvPerShareOn31Jan2018
        public int fmvOn31Jan2018;               // Col9 = max(totalFMV, saleValue constraint)
        public int costWithoutIndexation;        // Col7 = max(Col8, Col9)
        public int expenditureOnTransfer;        // Col12
        public int totalDeductions;              // Col13 = Col7 + Col12
        public int balanceLTCG;                  // Col14 = Col6 - Col13
        public int exemptionUs54EC;
        public int exemptionUs54F;
        public int netTaxableLTCG;
        public boolean preJuly23Sale;            // determines 10% vs 12.5% rate
    }

    // ── ScheduleCYLAData ──────────────────────────────────────────────────────
    public static class ScheduleCYLAData {
        public int hpLossAvailable = 0;
        public int hpLossSetOffAgainstSalary = 0;
        public int hpLossSetOffAgainstOS = 0;
        public int hpLossSetOffAgainstCG = 0;
        public int hpLossUnabsorbed = 0;        // goes to CFL

        public int businessNSLossAvail = 0;     // non-speculative
        public int businessNSLossSetOff = 0;
        public int businessNSLossUnabsorbed = 0;

        public int speculativeLossAvail = 0;
        public int speculativeLossSetOff = 0;   // only vs speculative income
        public int speculativeLossUnabsorbed = 0;

        public int stcgLossAvail = 0;
        public int stcgLossSetOffAgainstSTCG = 0;
        public int stcgLossSetOffAgainstLTCG = 0;
        public int stcgLossUnabsorbed = 0;

        public int ltcgLossAvail = 0;
        public int ltcgLossSetOffAgainstLTCG = 0;
        public int ltcgLossUnabsorbed = 0;

        // VDA loss: NEVER set off — always 0
        public int vdaLoss = 0; // declared for audit trail; no set-off allowed
    }

    // ── ScheduleBFLAData ──────────────────────────────────────────────────────
    public static class ScheduleBFLAData {
        public List<LossLedgerEntry> hpLossesAvailable = new ArrayList<>();
        public List<LossLedgerEntry> businessLossesAvailable = new ArrayList<>();
        public List<LossLedgerEntry> speculativeLossesAvailable = new ArrayList<>();
        public List<LossLedgerEntry> stcgLossesAvailable = new ArrayList<>();
        public List<LossLedgerEntry> ltcgLossesAvailable = new ArrayList<>();
        public List<LossLedgerEntry> unabsorbedDepreciationAvailable = new ArrayList<>();

        public int totalBFLossSetOff = 0;
    }

    // ── ScheduleCFLData ───────────────────────────────────────────────────────
    public static class ScheduleCFLData {
        public int hpLossToCarryForward = 0;
        public int businessLossToCarryForward = 0;
        public int speculativeLossToCarryForward = 0;
        public int stcgLossToCarryForward = 0;
        public int ltcgLossToCarryForward = 0;
        public int unabsorbedDepToCarryForward = 0;
        // All these are persisted to LossLedger after filing
    }

    // ── Schedule80GData ───────────────────────────────────────────────────────
    public static class Schedule80GData {
        public List<DonationEntry> donations = new ArrayList<>();
        public int totalEligibleDeduction = 0;

        public static class DonationEntry {
            public String doneeName;
            public String doneePAN;
            public String doneeAddress;
            public int donationAmount;
            public PaymentMode paymentMode; // CASH, CHEQUE, ONLINE, DD
            public boolean isQualified100Pct; // 100% deduction or 50%
            public int eligibleAmount;       // 0 if cash > 2000; else proportionate
        }
    }

    // ── ScheduleALData ────────────────────────────────────────────────────────
    public static class ScheduleALData {
        public List<ImmovablePropertyEntry> immovableProperties = new ArrayList<>();
        public int jewellery = 0;
        public int artworkPaintings = 0;
        public int vehicles = 0;
        public int bullionGold = 0;
        public int otherMovable = 0;
        public int sharesDebentures = 0;
        public int insurancePolicies = 0;
        public int loansGiven = 0;
        public int cashInHandAbove500K = 0;
        public int bankDeposits = 0;
        public int otherFinancial = 0;
        public int totalAssets = 0;         // computed: sum of all above
        public int securedLoans = 0;
        public int unsecuredLoans = 0;
        public int otherLiabilities = 0;
        public int totalLiabilities = 0;    // computed
        public boolean requiresVerification = false; // true if auto-populated from AIS

        public static class ImmovablePropertyEntry {
            public String address;
            public int costOfAcquisition;
            public String yearOfAcquisition;
        }
    }

    // ── ScheduleBPData ────────────────────────────────────────────────────────
    public static class ScheduleBPData {
        public int netProfitFromPL = 0;

        // Add-backs (inadmissible debits)
        public int debitPersonalExpenses = 0;
        public int debitCapitalExpenditure = 0;
        public int disallowance40A3_cash = 0;    // cash > 10K per day per person
        public int disallowance36_1_va_epf = 0;  // employee PF deposited late
        public int disallowance43B = 0;           // taxes/PF/bonus not paid by filing
        public int disallowance43Bh_msme = 0;     // MSME > 45 days
        public int disallowance14A = 0;           // expenses for exempt income
        public int excessBookDepreciation = 0;    // book dep > IT Act dep
        public int totalAddBack = 0;              // sum of above

        // Deductions (admissible credits)
        public int dividendIncome = 0;            // removed to avoid double counting
        public int rentalIncome = 0;              // removed — taxed under HP
        public int itActDepreciation = 0;         // from ScheduleDPM
        public int additionalDepreciation = 0;    // u/s 32(1)(iia)
        public int deduction35AD = 0;             // specified business
        public int otherPermissibleDeductions = 0;
        public int totalDeductions = 0;           // sum of above

        // Results
        public int profitFromBusiness = 0;        // netProfit + addBack - deductions
        public int speculativeBusinessIncome = 0; // intraday equity — kept separate
        public int totalBusinessIncome = 0;

        // For partner remuneration validation
        public int bookProfit = 0;
        public int partnerRemunerationClaimed = 0;
    }

    // ── ScheduleDPMData ───────────────────────────────────────────────────────
    public static class ScheduleDPMData {
        public DepreciationBlock buildings_5pct = new DepreciationBlock("BUILDING_RESIDENTIAL", 5);
        public DepreciationBlock buildings_10pct = new DepreciationBlock("BUILDING_NON_RESIDENTIAL", 10);
        public DepreciationBlock buildings_40pct = new DepreciationBlock("BUILDING_TEMPORARY", 40);
        public DepreciationBlock furniture_10pct = new DepreciationBlock("FURNITURE", 10);
        public DepreciationBlock pm_15pct = new DepreciationBlock("PLANT_MACHINERY", 15);
        public DepreciationBlock computers_40pct = new DepreciationBlock("COMPUTERS", 40);
        public DepreciationBlock vehicles_15pct = new DepreciationBlock("VEHICLES", 15);
        public DepreciationBlock intangibles_25pct = new DepreciationBlock("INTANGIBLES", 25);
        public int totalDepreciation = 0;
        public int stcgOnBlocks = 0;             // when block disposal > (opening + additions)

        public static class DepreciationBlock {
            public String assetType;
            public int rate;
            public int openingWDV = 0;
            public int additions = 0;
            public int additionsAfterOct1 = 0;  // half-rate rule
            public int disposals = 0;
            public int closingWDV = 0;
            public int depreciation = 0;
            public int additionalDepreciation = 0;  // 32(1)(iia) — new P&M only
            public int stcgOnBlock = 0;          // if disposals > (opening + additions)
            public boolean hasGoodwill = false;  // if true, depreciation = 0

            public DepreciationBlock(String assetType, int rate) {
                this.assetType = assetType;
                this.rate = rate;
            }
        }
    }

    // ── ScheduleDCGData ───────────────────────────────────────────────────────
    public static class ScheduleDCGData {
        // Deemed Capital Gains when block is wiped out by disposal
        public int row1a = 0;  // opening WDV of buildings
        public int row1b = 0;  // opening WDV of P&M
        public int row1c = 0;  // opening WDV of furniture
        public int row1d = 0;  // opening WDV of intangibles
        public int row1_total = 0; // = 1a+1b+1c+1d

        public int row2a = 0;  // sale proceeds buildings
        public int row2b = 0;  // sale proceeds P&M
        public int row2c = 0;  // sale proceeds other
        public int row2d = 0;  // = 2a+2b+2c

        public int totalDeemedCapitalGains = 0; // = row2d - row1_total (if > 0)
    }

    // ── VDATransaction ────────────────────────────────────────────────────────
    public static class VDATransaction {
        public LocalDate dateOfAcquisition;
        public LocalDate dateOfTransfer;
        public int costOfAcquisition;       // only allowed deduction u/s 115BBH
        public int considerationReceived;
        public int tdsDeductedUnder194S;
        public boolean requiresManualCost;  // flag if cost not in AIS
        public String assetName;            // BTC, ETH, NFT description, etc.
        public String exchangeName;
    }

    // ── LossLedgerEntry ───────────────────────────────────────────────────────
    public static class LossLedgerEntry {
        public String incurredAY;           // "2023-24", "2024-25", etc.
        public LossType lossType;
        public int originalAmount;
        public int utilizedSoFar;
        public int remaining;
        public String expiryAY;             // AY after which loss lapses
        // HP/Business/CG: expiryAY = incurredAY + 8 years
        // Speculative: incurredAY + 4 years
        // Unabsorbed depreciation: never expires
    }

    public enum LossType {
        HP, BUSINESS_NON_SPECULATIVE, SPECULATIVE, STCG, LTCG, UNABSORBED_DEPRECIATION
    }

    public enum TaxRegime { OLD, NEW }
    public enum PropertyType { SELF_OCCUPIED, LET_OUT, DEEMED_LET_OUT }
    public enum PaymentMode { CASH, CHEQUE, ONLINE, DD, NEFT, RTGS }
}
```

---

## SECTION 5 — CREATE ADDITIONAL SUPPORTING DTOS

Create `ITRBusinessDtos.java` in the DTO package:

```java
// ─── ITRBusinessDtos.java ─────────────────────────────────────────────────────

public class ITRBusinessDtos {

    public static class MSMEPayment {
        public String supplierName;
        public String supplierGSTIN;
        public boolean isMSMERegistered;
        public LocalDate supplyDate;
        public LocalDate paymentDate;     // null if not yet paid
        public int amount;
        public boolean disallowed;        // computed: true if > 45 days unpaid
    }

    public static class Section43BItem {
        public String description;
        public String category;           // TAX | PF | ESIC | BONUS | INTEREST | MSME
        public int amount;
        public LocalDate paymentDate;     // null if unpaid
        public boolean paidBeforeFilingDate;
        public boolean disallowed;        // computed
    }

    public static class EPFContribution {
        public String employeeUAN;
        public int amount;
        public LocalDate dueDate;         // per EPF rules
        public LocalDate depositDate;
        public boolean depositedOnTime;   // computed: depositDate <= due date
    }

    public static class FandODetail {
        public int grossContractValue;    // NOT used as turnover
        public int absoluteProfitSum;     // sum of all winning trades
        public int absoluteLossSum;       // sum of |all losing trades|
        public int fandoTurnover;         // = absoluteProfitSum + absoluteLossSum
        public int netProfitOrLoss;       // actual net P&L
        public boolean isSpeculative;     // F&O is NON-speculative; intraday IS speculative
    }

    public static class GSTRegistration {
        public String gstin;
        public String businessName;
        public int turnoverGSTR1;
        public int turnoverGSTR3B;
        public int turnoverBooks;
        public int differenceGSTvsBooks; // computed
        public String reasonForDifference;
    }

    public static class BalanceSheetData {
        // Assets
        public int fixedAssetsGross = 0;
        public int accumulatedDepreciation = 0;
        public int fixedAssetsNet = 0;
        public int sundryDebtors = 0;
        public int cashAndBankBalance = 0;
        public int stockInTrade = 0;
        public int loansAndAdvances = 0;
        public int otherCurrentAssets = 0;
        public int totalAssets = 0;
        // Liabilities
        public int capitalAccount = 0;
        public int reservesAndSurplus = 0;
        public int securedLoans = 0;
        public int unsecuredLoans = 0;
        public int sundryCreditors = 0;
        public int otherLiabilities = 0;
        public int totalLiabilities = 0;
    }

    public static class ProfitAndLossData {
        public int grossReceipts = 0;
        public int openingStock = 0;
        public int purchases = 0;
        public int directExpenses = 0;
        public int closingStock = 0;
        public int grossProfit = 0;
        public int otherIncome = 0;
        public int salariesAndWages = 0;
        public int rentRatesAndTaxes = 0;
        public int repairsAndMaintenance = 0;
        public int depreciation = 0;       // book depreciation
        public int otherExpenses = 0;
        public int netProfitAsPerPL = 0;
    }

    public static class SimplifiedBalanceSheet {
        // MANDATORY for ITR-4
        public int grossReceipts = 0;
        public int netProfit = 0;
        public int sundryDebtors = 0;
        public int sundryCreditors = 0;
        public int stockInTrade = 0;
        public int cashBalance = 0;
        public int openingCapital = 0;
        public int drawings = 0;
        public int additionsToCapital = 0;
        public int closingCapital = 0;     // = openingCapital + additions - drawings + netProfit
        public int securedLoans = 0;
        public int unsecuredLoans = 0;
        public int totalFixedAssetsWDV = 0;
    }
}
```

---

## SECTION 6 — RE-IMPLEMENT `ITR2CBDTValidationService.java`

Now that the DTOs exist, create this service. It must:
1. Accept `Itr2FormData` as input
2. Return `ValidationResult` (reuse the existing class from Phase 1)
3. Run all rules; collect ALL errors before returning (do not fail-fast)
4. CAT-A failures → `errors` list; warnings → `warnings` list

```java
@Service
public class ITR2CBDTValidationService {

    private final ITR1CBDTValidationService itr1Validator; // inject — reuse common rules
    private final LossSetOffEngine lossSetOffEngine;

    public ValidationResult validate(Itr2FormData data) {
        ValidationResult result = new ValidationResult();

        // ── Step 1: Run all ITR-1 common rules that apply to ITR-2 ──────────
        // (salary, OS, TDS, TCS, advance tax, PAN, Aadhaar, deductions)
        ValidationResult commonResult = itr1Validator.validateCommonFields(data.toCommonFormData());
        result.merge(commonResult);

        // ── Step 2: HP Rules ─────────────────────────────────────────────────
        validateScheduleHP(data, result);

        // ── Step 3: Capital Gains ────────────────────────────────────────────
        validateScheduleCG(data, result);

        // ── Step 4: Schedule 112A Grandfathering ────────────────────────────
        validateSchedule112A(data, result);

        // ── Step 5: CYLA ─────────────────────────────────────────────────────
        validateScheduleCYLA(data, result);

        // ── Step 6: BFLA ─────────────────────────────────────────────────────
        validateScheduleBFLA(data, result);

        // ── Step 7: VIA Deductions ───────────────────────────────────────────
        validateScheduleVIA(data, result);

        // ── Step 8: Special Rate Income ──────────────────────────────────────
        validateScheduleSI(data, result);

        // ── Step 9: VDA ──────────────────────────────────────────────────────
        validateScheduleVDA(data, result);

        // ── Step 10: AMT ─────────────────────────────────────────────────────
        validateAMT(data, result);

        return result;
    }

    // ── HP Validation ─────────────────────────────────────────────────────────
    private void validateScheduleHP(Itr2FormData data, ValidationResult result) {
        long sopCount = data.getHouseProperties().stream()
            .filter(p -> p.type == SELF_OCCUPIED).count();
        if (sopCount > 2) {
            result.addError("VR2-HP-011", "Only 2 properties can be Self-Occupied. Additional must be Deemed Let-Out.");
        }

        for (PropertyDetail p : data.getHouseProperties()) {
            // VR2-HP-001: stdDeduction = 30% of NAV exactly
            int expectedSD = (int) Math.round(0.30 * p.netAnnualValue);
            if (p.standardDeduction != expectedSD) {
                result.addError("VR2-HP-001", "Property " + p.address + ": standard deduction must be exactly 30% of NAV = Rs " + expectedSD);
            }
            // VR2-HP-002: co-owner shares = 100%
            if (p.isCoOwned && (p.ownershipSharePct + p.coOwnerSharePct) != 100) {
                result.addError("VR2-HP-002", "Property " + p.address + ": ownership shares must total 100%");
            }
            // VR2-HP-003: assessee NAV = own% × total NAV
            if (p.isCoOwned) {
                int expectedNAV = (int) Math.round(p.netAnnualValue * 100.0 / p.ownershipSharePct);
                // (validate proportionate NAV logic per convention)
            }
            // VR2-HP-004: no interest if share = 0
            if (p.ownershipSharePct == 0 && p.interestOnBorrowedCapital > 0) {
                result.addError("VR2-HP-004", "Cannot claim interest on borrowed capital with 0% ownership share.");
            }
            // VR2-HP-005: no municipal tax if GAV = 0
            if (p.grossAnnualValue == 0 && p.municipalTax > 0) {
                result.addError("VR2-HP-005", "Municipal tax cannot be claimed when Gross Annual Value is zero.");
            }
            // VR2-HP-006: old regime SOP interest <= 2L
            if (data.getTaxRegime() == OLD && p.type == SELF_OCCUPIED && p.interestOnBorrowedCapital > 200000) {
                result.addError("VR2-HP-006", "SOP interest deduction capped at Rs 2,00,000 under old regime. Claimed: Rs " + p.interestOnBorrowedCapital);
            }
            // VR2-HP-007: new regime SOP interest = 0
            if (data.getTaxRegime() == NEW && p.type == SELF_OCCUPIED && p.interestOnBorrowedCapital > 0) {
                result.addError("VR2-HP-007", "No interest deduction for Self-Occupied property under new regime.");
            }
            // VR2-HP-008: let-out gross rent > 0
            if ((p.type == LET_OUT || p.type == DEEMED_LET_OUT) && p.grossAnnualValue <= 0) {
                result.addError("VR2-HP-008", "Let-out/Deemed Let-out property must have Gross Annual Value > 0.");
            }
            // VR2-HP-009: HP income formula
            int expectedHPIncome = p.netAnnualValue - p.standardDeduction - p.interestOnBorrowedCapital + p.arrearUnrealisedRent;
            if (p.hpIncome != expectedHPIncome) {
                result.addError("VR2-HP-009", "HP income computation error. Expected Rs " + expectedHPIncome + ", found Rs " + p.hpIncome);
            }
            // VR2-HP-012: co-owner PAN != assessee PAN
            if (p.isCoOwned && p.coOwnerPAN != null && p.coOwnerPAN.equals(data.getPAN())) {
                result.addError("VR2-HP-012", "Co-owner PAN cannot be same as assessee PAN.");
            }
        }
    }

    // ── Schedule 112A Validation ──────────────────────────────────────────────
    private void validateSchedule112A(Itr2FormData data, ValidationResult result) {
        ScheduleCGData cg = data.getScheduleCG();
        int runningTotal = 0;

        for (Schedule112ARow row : cg.getSchedule112ARows()) {
            // VR2-CG-112A-001: Col6 = units * sale price per unit
            int expectedSaleValue = row.units * row.salePricePerUnit;
            if (row.totalSaleValue != expectedSaleValue) {
                result.addError("VR2-CG-112A-001", row.nameOfScrip + ": Total Sale Value must be units × price = Rs " + expectedSaleValue);
            }
            // VR2-CG-112A-002: costWithoutIndexation = max(actual, FMV Jan31 2018)
            boolean isPreFeb2018 = row.dateOfAcquisition != null && row.dateOfAcquisition.isBefore(LocalDate.of(2018, 2, 1));
            int expectedCost;
            if (isPreFeb2018) {
                expectedCost = Math.max(row.actualCostOfAcquisition, row.fmvOn31Jan2018);
            } else {
                expectedCost = row.actualCostOfAcquisition;
                if (row.fmvOn31Jan2018 > 0) {
                    result.addError("VR2-CG-112A-003", row.nameOfScrip + ": FMV Jan 31, 2018 only applicable for assets acquired before Feb 1, 2018.");
                }
            }
            if (row.costWithoutIndexation != expectedCost) {
                result.addError("VR2-CG-112A-002", row.nameOfScrip + ": Cost without indexation must be Rs " + expectedCost);
            }
            // VR2-CG-112A-003: FMV <= min(sale value, total FMV)
            if (isPreFeb2018 && row.fmvOn31Jan2018 > Math.min(row.totalSaleValue, row.totalFMV55_2_ac)) {
                result.addError("VR2-CG-112A-003", row.nameOfScrip + ": FMV Jan 31, 2018 cannot exceed sale value or total FMV.");
            }
            // VR2-CG-112A-004: Col11 = units * Col10
            if (isPreFeb2018) {
                int expectedTotalFMV = row.units * row.fmvPerShareOn31Jan2018;
                if (row.totalFMV55_2_ac != expectedTotalFMV) {
                    result.addError("VR2-CG-112A-004", row.nameOfScrip + ": Total FMV must be units × FMV per share = Rs " + expectedTotalFMV);
                }
            }
            // VR2-CG-112A-005: Col13 = Col7 + Col12
            int expectedDeductions = row.costWithoutIndexation + row.expenditureOnTransfer;
            if (row.totalDeductions != expectedDeductions) {
                result.addError("VR2-CG-112A-005", row.nameOfScrip + ": Total deductions must be Rs " + expectedDeductions);
            }
            // VR2-CG-112A-006: Col14 = Col6 - Col13
            int expectedBalance = row.totalSaleValue - row.totalDeductions;
            if (row.balanceLTCG != expectedBalance) {
                result.addError("VR2-CG-112A-006", row.nameOfScrip + ": Balance LTCG must be Rs " + expectedBalance);
            }
            runningTotal += row.balanceLTCG;
        }

        // VR2-CG-112A-007: totals match sum of rows
        int declaredTotal = cg.ltcg112A_preJul23 + cg.ltcg112A_postJul23;
        if (declaredTotal != runningTotal) {
            result.addError("VR2-CG-112A-007", "Schedule 112A total Rs " + declaredTotal + " does not match sum of rows Rs " + runningTotal);
        }

        // VR2-CG-112A-008: Schedule CG B4a = total of Col14
        // (This consistency check is done against scheduleCG.ltcg112A total)
    }

    // ── Schedule CG Validation ────────────────────────────────────────────────
    private void validateScheduleCG(Itr2FormData data, ValidationResult result) {
        ScheduleCGData cg = data.getScheduleCG();

        // VR2-CG-001: totalSTCG = sum of components
        int expectedSTCG = cg.stcg111A_preJul23 + cg.stcg111A_postJul23 + cg.stcgAtSlab;
        if (cg.totalSTCG != expectedSTCG) {
            result.addError("VR2-CG-001", "Total STCG Rs " + cg.totalSTCG + " does not match component sum Rs " + expectedSTCG);
        }
        // VR2-CG-002: totalLTCG = sum of components
        int expectedLTCG = cg.ltcg112A_preJul23 + cg.ltcg112A_postJul23 + cg.ltcgPropertyWithIndexation + cg.ltcgPropertyWithoutIndexation;
        if (cg.totalLTCG != expectedLTCG) {
            result.addError("VR2-CG-002", "Total LTCG Rs " + cg.totalLTCG + " does not match component sum Rs " + expectedLTCG);
        }
        // VR2-CG-003: totalCGIncome = totalSTCG + totalLTCG
        if (cg.totalCGIncome != (cg.totalSTCG + cg.totalLTCG)) {
            result.addError("VR2-CG-003", "Capital Gains total must equal STCG + LTCG.");
        }
        // VR2-CG-007: Section 50C — property stamp duty rule
        if (cg.stampDutyValue > 0 && cg.actualSalePrice > 0) {
            if (cg.stampDutyValue > cg.actualSalePrice * 1.10) {
                // Must use SDV as full consideration
                int expectedFVC = cg.stampDutyValue;
                if (cg.fullValueOfConsideration != expectedFVC) {
                    result.addError("VR2-CG-007", "SDV > 110% of sale price — full value of consideration must be SDV Rs " + expectedFVC);
                }
            }
        }
        // VR2-CG-011: LTCG 112A B4c = B4a - B4b (exemption)
        int totalLTCG112A = cg.ltcg112A_preJul23 + cg.ltcg112A_postJul23;
        // This is validated against 125000 (or 100000 for pre-Jul23) threshold — done in export

        // VR2-CG-013: CGAS deposit check
        boolean hasExemptionClaimed = cg.exemptionUs54 > 0 || cg.exemptionUs54F > 0 || cg.exemptionUs54EC > 0;
        if (hasExemptionClaimed && !cg.cgasDepositConfirmed) {
            result.addWarning("VR2-CG-013", "Exemption u/s 54/54F/54EC claimed but CGAS deposit not confirmed. Ensure deposit made before filing date.");
        }

        // VR2-CG-009: Split rate enforcement
        if (cg.stcg111A_preJul23 != 0 || cg.stcg111A_postJul23 != 0) {
            // Both pools must be tracked separately — cannot merge
            // Validated if both fields are set independently
        }
    }

    // ── CYLA Validation ───────────────────────────────────────────────────────
    private void validateScheduleCYLA(Itr2FormData data, ValidationResult result) {
        ScheduleCYLAData cyla = data.getScheduleCYLA();
        if (cyla == null) return;

        // VR2-CYLA-001: HP loss set-off <= Rs 2,00,000
        int totalHPSetOff = cyla.hpLossSetOffAgainstSalary + cyla.hpLossSetOffAgainstOS + cyla.hpLossSetOffAgainstCG;
        if (totalHPSetOff > 200000) {
            result.addError("VR2-CYLA-001", "HP loss set-off against other heads cannot exceed Rs 2,00,000. Total claimed: Rs " + totalHPSetOff);
        }
        // VR2-CYLA-002: Speculative loss only vs speculative income
        if (cyla.speculativeLossSetOff > 0) {
            // Must verify it's being set off against speculative income only — validated in LossSetOffEngine
        }
        // VR2-CYLA-003: LTCG loss only vs LTCG
        if (cyla.ltcgLossSetOffAgainstLTCG > cyla.ltcgLossAvail) {
            result.addError("VR2-CYLA-003", "LTCG loss set-off cannot exceed LTCG loss available.");
        }
        // VR2-CYLA-004: Income after set-off >= 0 per head (enforced by LossSetOffEngine)
        // VR2-CYLA-005: STCG at slab in CYLA = CG schedule figure (cross-reference)
    }

    // ── Schedule VIA Validation ───────────────────────────────────────────────
    private void validateScheduleVIA(Itr2FormData data, ValidationResult result) {
        // VR2-VIA-001: New regime — only 80CCD(2), 80JJAA, 80CCH(2)
        if (data.getTaxRegime() == NEW) {
            String[] disallowedInNew = {"80C","80CCC","80CCD1","80D","80DD","80DDB","80E","80EE","80EEA","80G","80GG","80TTA","80TTB","80U"};
            for (String ded : disallowedInNew) {
                if (data.getDeductionAmount(ded) > 0) {
                    result.addError("VR2-VIA-001", ded + " deduction is NOT available under new regime. Amount must be zero.");
                }
            }
        }
        // VR2-VIA-002/003/004: 80G validation
        if (data.getSchedule80G() != null) {
            for (Schedule80GData.DonationEntry d : data.getSchedule80G().getDonations()) {
                if (d.paymentMode == CASH && d.donationAmount > 2000) {
                    if (d.eligibleAmount != 0) {
                        result.addError("VR2-VIA-004", "Cash donation > Rs 2,000 to " + d.doneeName + " must have eligible amount = 0.");
                    }
                }
                if (d.doneePAN != null && d.doneePAN.equals(data.getPAN())) {
                    result.addError("VR2-VIA-005", "Donee PAN cannot be same as assessee PAN.");
                }
            }
        }
        // VR2-VIA-008: HUF cannot claim 89A
        if (data.isHUF() && data.getReliefUs89A() > 0) {
            result.addError("VR2-VIA-008", "HUF cannot claim relief u/s 89A.");
        }
    }

    // ── Special Rate Income Validation ────────────────────────────────────────
    private void validateScheduleSI(Itr2FormData data, ValidationResult result) {
        // VR2-SI-004: VDA income rate = 30%
        // (enforced in VDATransactionService compute — income cannot be negative)
        if (data.getVdaIncome() < 0) {
            result.addError("VR2-SI-008", "VDA income cannot be negative. Loss not allowed for set-off u/s 115BBH.");
            // Auto-correct: data.setVdaIncome(0);
        }
        // VR2-SI-007: Surcharge on 111A/112A capped at 15%
        // (enforced in TaxComputationEngine — not a field check here)
        // VR2-SI-006: 87A rebate not applicable to special rate income
        // (enforced in TaxComputationEngine)
    }

    // ── AMT Validation ────────────────────────────────────────────────────────
    private void validateAMT(Itr2FormData data, ValidationResult result) {
        // VR2-VIA-007: AMT applicable if ATI > Rs 20L and AMT > regular tax
        if (data.getAdjustedTotalIncomeForAMT() > 2000000) {
            int amt = (int) Math.round(0.185 * data.getAdjustedTotalIncomeForAMT());
            if (amt > data.getRegularTaxBeforeCess()) {
                if (!data.isAmtApplicable()) {
                    result.addError("VR2-VIA-007", "AMT applies: ATI Rs " + data.getAdjustedTotalIncomeForAMT() + " > Rs 20L and AMT Rs " + amt + " > regular tax. Fill Schedule AMT.");
                }
            }
        }
    }

    private void validateScheduleVDA(Itr2FormData data, ValidationResult result) {
        for (VDATransaction vda : data.getVdaTransactions()) {
            if (vda.considerationReceived < vda.costOfAcquisition) {
                result.addInfo("VDA-LOSS", "VDA transaction in " + vda.assetName + " shows a loss of Rs " + (vda.costOfAcquisition - vda.considerationReceived) + ". This loss CANNOT be set off against any income per Section 115BBH. Declaring as Rs 0.");
            }
        }
    }

    private void validateScheduleBFLA(Itr2FormData data, ValidationResult result) {
        ScheduleBFLAData bfla = data.getScheduleBFLA();
        if (bfla == null) return;
        // VR2-BFLA-005: BF HP loss can only be set off against HP income
        // VR2-BFLA-003: remaining = available - setOff
        // (detailed cross-checks performed by LossSetOffEngine — results fed back here)
    }
}
```

---

## SECTION 7 — RE-IMPLEMENT `ITR3CBDTValidationService.java`

```java
@Service
public class ITR3CBDTValidationService {

    private final ITR2CBDTValidationService itr2Validator;
    private final DepreciationEngine depreciationEngine;

    public ValidationResult validate(Itr3FormData data) {
        ValidationResult result = new ValidationResult();

        // Inherit all ITR-2 rules (HP, CG, CYLA, BFLA, VIA, SI, VDA)
        result.merge(itr2Validator.validate(data.toItr2FormData()));

        // ── Schedule BP ──────────────────────────────────────────────────────
        validateScheduleBP(data, result);

        // ── Schedule DPM ─────────────────────────────────────────────────────
        validateScheduleDPM(data, result);

        // ── Schedule DCG ─────────────────────────────────────────────────────
        validateScheduleDCG(data, result);

        return result;
    }

    private void validateScheduleBP(Itr3FormData data, ValidationResult result) {
        ScheduleBPData bp = data.getScheduleBP();

        // VR3-BP-004: A6 arithmetic
        int expectedBPIncome = bp.netProfitFromPL + bp.totalAddBack - bp.totalDeductions;
        if (bp.profitFromBusiness != expectedBPIncome) {
            result.addError("VR3-BP-004", "Business income computation error. Expected Rs " + expectedBPIncome + ", found Rs " + bp.profitFromBusiness);
        }
        // VR3-BP-005/006: 80JJAA
        if (data.getSec80JJAADeduction() > 0) {
            if (!data.isSubjectToTaxAudit()) {
                result.addError("VR3-BP-006", "80JJAA deduction requires tax audit.");
            }
        }
        // VR3-BP-007: 40A(3) disallowance must be added back
        // (enforced during data entry — MSMEPayment / expense tracking in ScheduleBP)

        // VR3-BP-010: Partner remuneration limit
        if (bp.partnerRemunerationClaimed > 0 && bp.bookProfit > 0) {
            int limitPart1 = (int) Math.round(Math.min(bp.bookProfit, 300000) * 0.90);
            int limitPart2 = (int) Math.round(Math.max(0, bp.bookProfit - 300000) * 0.60);
            int limit = Math.max(150000, limitPart1 + limitPart2);
            if (bp.partnerRemunerationClaimed > limit) {
                result.addError("VR3-BP-010", "Partner remuneration Rs " + bp.partnerRemunerationClaimed + " exceeds Section 40(b) limit Rs " + limit);
            }
        }
        // VR3-BP-011: F&O turnover
        if (data.getFandoDetail() != null) {
            FandODetail fando = data.getFandoDetail();
            if (fando.fandoTurnover != (fando.absoluteProfitSum + fando.absoluteLossSum)) {
                result.addError("VR3-BP-011", "F&O turnover must be sum of absolute profits + absolute losses. Not gross contract value.");
            }
        }
        // VR3-BP-012: Speculative income kept separate
        if (bp.speculativeBusinessIncome < 0) {
            result.addError("VR3-BP-012", "Speculative income cannot be negative in Schedule BP. Losses go to CYLA separately.");
        }
    }

    private void validateScheduleDPM(Itr3FormData data, ValidationResult result) {
        ScheduleDPMData dpm = data.getScheduleDPM();

        // Recompute depreciation using DepreciationEngine and compare
        int computedTotal = depreciationEngine.computeTotal(dpm);
        if (dpm.totalDepreciation != computedTotal) {
            result.addError("VR3-DPM-002", "Total depreciation Rs " + dpm.totalDepreciation + " does not match computed Rs " + computedTotal + ". Check block computations.");
        }
        // VR3-DPM-005: Goodwill = 0% depreciation
        if (dpm.intangibles_25pct.hasGoodwill && dpm.intangibles_25pct.depreciation > 0) {
            result.addError("VR3-DPM-005", "Goodwill has 0% depreciation from AY 2021-22. Remove goodwill block depreciation.");
        }
        // VR3-DPM-003: DPM total matches Schedule DEP
        // (cross-reference enforced in ITR3JSONExportService — both use same computed value)
    }

    private void validateScheduleDCG(Itr3FormData data, ValidationResult result) {
        ScheduleDCGData dcg = data.getScheduleDCG();

        // VR3-DCG-001: row1_total = 1a+1b+1c+1d
        int expectedRow1Total = dcg.row1a + dcg.row1b + dcg.row1c + dcg.row1d;
        if (dcg.row1_total != expectedRow1Total) {
            result.addError("VR3-DCG-001", "DCG total opening WDV must equal sum of components = Rs " + expectedRow1Total);
        }
        // VR3-DCG-002: row2d = 2a+2b+2c
        int expectedRow2d = dcg.row2a + dcg.row2b + dcg.row2c;
        if (dcg.row2d != expectedRow2d) {
            result.addError("VR3-DCG-002", "DCG disposal proceeds total must equal component sum = Rs " + expectedRow2d);
        }
        // VR3-DCG-003/004: cross-reference to DPM
        ScheduleDPMData dpm = data.getScheduleDPM();
        if (dpm != null) {
            if (dcg.row1a != dpm.buildings_5pct.openingWDV + dpm.buildings_10pct.openingWDV + dpm.buildings_40pct.openingWDV) {
                result.addError("VR3-DCG-003", "DCG row1a must match DPM buildings opening WDV.");
            }
        }
    }
}
```

---

## SECTION 8 — RE-IMPLEMENT `ITR4CBDTValidationService.java`

```java
@Service
public class ITR4CBDTValidationService {

    private final ITR1CBDTValidationService itr1Validator;

    public ValidationResult validate(Itr4FormData data) {
        ValidationResult result = new ValidationResult();

        // Common rules from ITR-1 (salary, OS, PAN, Aadhaar, TDS, advance tax)
        result.merge(itr1Validator.validateCommonFields(data.toCommonFormData()));

        // ── ITR-4 Specific ────────────────────────────────────────────────────

        // VR4-001: Total income (excluding 112A) <= Rs 50L
        int incomeExcluding112A = data.getTotalIncome() - data.getLtcg112A();
        if (incomeExcluding112A > 5000000) {
            result.addError("VR4-001", "Total income (excluding LTCG 112A) Rs " + incomeExcluding112A + " exceeds Rs 50L. Must file ITR-3.");
        }

        // VR4-002: No CG other than LTCG 112A
        if (data.hasSTCGOrOtherCG()) {
            result.addError("VR4-002", "Capital gains other than LTCG u/s 112A not allowed in ITR-4. File ITR-3.");
        }

        // VR4-003: LTCG 112A > Rs 1.25L → must file ITR-2/3
        if (data.getLtcg112A() > 125000) {
            result.addError("VR4-003", "LTCG u/s 112A of Rs " + data.getLtcg112A() + " exceeds Rs 1,25,000. Must file ITR-2 or ITR-3.");
        }

        // VR4-004: 44AD turnover limit
        if (data.getSec44AD() != null) {
            Sec44ADData d = data.getSec44AD();
            double cashPct = (d.grossTurnover > 0) ? (double) d.turnoverCash / d.grossTurnover : 0;
            int limit = (cashPct <= 0.05) ? 30000000 : 20000000;
            if (d.grossTurnover > limit) {
                result.addError("VR4-004", "44AD turnover Rs " + d.grossTurnover + " exceeds limit Rs " + limit + ". Must maintain books and file ITR-3.");
            }
        }

        // VR4-005: 44ADA receipts limit
        if (data.getSec44ADA() != null) {
            Sec44ADAData d = data.getSec44ADA();
            double cashPct = (d.grossReceipts > 0) ? (double) d.receiptsCash / d.grossReceipts : 0;
            int limit = (cashPct <= 0.05) ? 7500000 : 5000000;
            if (d.grossReceipts > limit) {
                result.addError("VR4-005", "44ADA receipts Rs " + d.grossReceipts + " exceed limit Rs " + limit + ". Must file ITR-3.");
            }
        }

        // VR4-006: 44AE max 10 vehicles
        if (data.getSec44AE() != null) {
            for (VehicleDetail v : data.getSec44AE().getVehicles()) {
                if (v.maxVehiclesAtAnyTime > 10) {
                    result.addError("VR4-006", "Cannot own > 10 goods vehicles for 44AE eligibility. File ITR-3.");
                }
            }
        }

        // VR4-007: Minimum 44AD income
        if (data.getSec44AD() != null) {
            Sec44ADData d = data.getSec44AD();
            int minIncome8pct = (int) Math.round(0.08 * d.turnoverCash);
            int minIncome6pct = (int) Math.round(0.06 * d.turnoverDigital);
            int minRequired = minIncome8pct + minIncome6pct;
            if (d.totalPresumptiveIncome < minRequired) {
                result.addError("VR4-007", "44AD: Declared income Rs " + d.totalPresumptiveIncome + " is below minimum Rs " + minRequired + " (8% of cash + 6% of digital turnover).");
            }
        }

        // VR4-008: Minimum 44ADA income
        if (data.getSec44ADA() != null) {
            Sec44ADAData d = data.getSec44ADA();
            int min50pct = (int) Math.round(0.50 * d.grossReceipts);
            if (d.presumptiveIncome < min50pct) {
                result.addError("VR4-008", "44ADA: Declared income Rs " + d.presumptiveIncome + " below 50% minimum Rs " + min50pct + ".");
            }
        }

        // VR4-009: 44AE vehicle income computation
        if (data.getSec44AE() != null) {
            for (VehicleDetail v : data.getSec44AE().getVehicles()) {
                int expectedIncome = v.isHeavyVehicle
                    ? (int) v.gvwInTonnes * 1000 * v.monthsOwned
                    : 7500 * v.monthsOwned;
                if (v.declaredIncome < expectedIncome) {
                    result.addWarning("VR4-009", "Vehicle " + v.registrationNumber + ": declared Rs " + v.declaredIncome + ", statutory minimum Rs " + expectedIncome + ".");
                }
            }
        }

        // VR4-010: LLP cannot file ITR-4
        if (data.isLLP()) {
            result.addError("VR4-010", "LLP must file ITR-5, not ITR-4.");
        }

        // VR4-011: 44AD opt-out lock-in
        if (data.isPriorYear44ADClaimed() && (data.getSec44AD() == null || data.getSec44AD().grossTurnover == 0)) {
            result.addError("VR4-011", "Opting out of 44AD requires maintaining books of accounts and tax audit for the next 5 years.");
            result.addAlert("ALERT-011", "44AD OPT-OUT detected. Confirm with taxpayer.");
        }

        // VR4-013: Only one HP in ITR-4
        if (data.getHouseProperty() == null && data.hasMultipleHouseProperties()) {
            result.addError("VR4-013", "ITR-4 allows only one house property. Multiple properties detected — file ITR-3.");
        }

        // VR4-014: Balance sheet MANDATORY fields
        SimplifiedBalanceSheet bs = data.getSimplifiedBalanceSheet();
        if (bs == null) {
            result.addError("VR4-014", "Simplified Balance Sheet is mandatory for ITR-4. All fields must be filled.");
        } else {
            if (bs.sundryDebtors < 0 || bs.sundryCreditors < 0 || bs.stockInTrade < 0 || bs.cashBalance < 0) {
                result.addError("VR4-014", "Balance sheet fields cannot be negative.");
            }
        }

        // VR4-016: GST reconciliation mandatory if GSTIN declared
        if (data.getGstRegistrations() != null && !data.getGstRegistrations().isEmpty()) {
            // Must have schedule GST filled
            boolean hasGSTSchedule = data.getGstRegistrations().stream().allMatch(g -> g.turnoverGSTR1 > 0 || g.turnoverBooks > 0);
            if (!hasGSTSchedule) {
                result.addError("VR4-016", "GST turnover reconciliation mandatory when GSTIN is declared.");
            }
        }

        // VR4-018: Cannot carry forward business losses in ITR-4
        if (data.isBusinessLossCarryForwardAttempted()) {
            result.addError("VR4-018", "Business losses cannot be carried forward in ITR-4. Consider filing ITR-3 to preserve loss carry-forward.");
        }

        // VR4-019: New regime deductions check
        if (data.getTaxRegime() == NEW) {
            // Only 80CCD(2), 80CCH, standard deduction allowed
            if (data.getDeduction80C() > 0 || data.getDeduction80D() > 0 || data.getDeduction80G() > 0) {
                result.addError("VR4-019", "New regime: Only 80CCD(2), 80CCH, and standard deduction allowed. All other deductions must be zero.");
            }
        }

        // VR4-020: 44AD and 44ADA for same business
        // (Different businesses allowed; same business code + same GSTIN = error)
        if (data.getSec44AD() != null && data.getSec44ADA() != null
            && data.getSec44AD().grossTurnover > 0 && data.getSec44ADA().grossReceipts > 0) {
            // Check if same GSTIN / business code — if so, error
            // If different businesses: allowed — just log info
        }

        return result;
    }
}
```

---

## SECTION 9 — REPLACE PLACEHOLDER SERVICES

### 9.1 Replace `VDATransactionService.java` (complete rewrite)

```java
@Service
public class VDATransactionService {

    public VDAComputationResult compute(List<VDATransaction> transactions) {
        int totalTaxableIncome = 0;
        int totalTDS194S = 0;

        for (VDATransaction txn : transactions) {
            // u/s 115BBH: only cost of acquisition deductible — no other expense
            int gain = txn.considerationReceived - txn.costOfAcquisition;
            int taxableGain = Math.max(0, gain); // loss = 0; cannot set off
            totalTaxableIncome += taxableGain;
            totalTDS194S += txn.tdsDeductedUnder194S;
        }

        // Tax = 30% flat — no basic exemption; no 87A rebate; no VI-A deductions
        int tax = (int) Math.round(0.30 * totalTaxableIncome);

        return VDAComputationResult.builder()
            .totalIncomeFromVDA(totalTaxableIncome)
            .taxOnVDA(tax)
            .tdsUnder194S(totalTDS194S)
            .build();
    }

    public List<VDATransaction> parseFromAIS(AISData aisData) {
        // Match TDS section = "194S" entries in AIS
        return aisData.getTdsEntries().stream()
            .filter(e -> "194S".equals(e.getSection()))
            .map(e -> {
                VDATransaction vda = new VDATransaction();
                vda.considerationReceived = e.getTransactionAmount();
                vda.tdsDeductedUnder194S = e.getTaxDeducted();
                vda.dateOfTransfer = e.getTransactionDate();
                vda.requiresManualCost = true; // cost must come from taxpayer
                vda.exchangeName = e.getDeductorName();
                return vda;
            })
            .collect(Collectors.toList());
    }

    // Build Schedule VDA JSON fields
    public Map<String, Object> buildScheduleVDAJson(List<VDATransaction> transactions, int totalIncome, int tds) {
        List<Map<String, Object>> details = transactions.stream().map(t -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("DateOfAcquisition", ITDDateFormatter.format(t.dateOfAcquisition));
            row.put("DateOfTransfer", ITDDateFormatter.format(t.dateOfTransfer));
            row.put("HeadUnderWhichIncomeTaxable", "CG");
            row.put("CostOfAcquisition", t.costOfAcquisition);
            row.put("ConsiderationReceived", t.considerationReceived);
            row.put("IncomeFromVDA", Math.max(0, t.considerationReceived - t.costOfAcquisition));
            return row;
        }).collect(Collectors.toList());

        Map<String, Object> scheduleVDA = new LinkedHashMap<>();
        scheduleVDA.put("VDADetails", details);
        scheduleVDA.put("TotalIncomeFromVDA", totalIncome);
        scheduleVDA.put("TDSUs194S", tds);
        return scheduleVDA;
    }
}
```

### 9.2 Replace stub methods in `SFTProcessingService.java`

Replace the 17 stub methods with real logic:

```java
@Service
public class SFTProcessingService {

    public SFTSummary processSFTData(AISData aisData) {
        SFTSummary summary = new SFTSummary();
        if (aisData == null || aisData.getSftTransactions() == null) return summary;

        Map<String, List<SFTTransaction>> byCode = aisData.getSftTransactions()
            .stream()
            .collect(Collectors.groupingBy(SFTTransaction::getSftCode));

        // SFT-001: Savings account deposits > Rs 10L → alert
        processSFT("SFT-001", byCode, 1000000, "Savings account deposits", summary);
        // SFT-002: Fixed deposits > Rs 10L
        processSFT("SFT-002", byCode, 1000000, "Fixed deposit transactions", summary);
        // SFT-003: Credit card payments > Rs 1L (cash) or Rs 10L (other modes)
        processSFT("SFT-003", byCode, 100000, "Credit card payments (cash limit)", summary);
        // SFT-005: Mutual fund purchases > Rs 10L → map to Schedule CG / ScheduleAL
        List<SFTTransaction> sft005 = byCode.getOrDefault("SFT-005", List.of());
        summary.setNetMFInvestment(sft005.stream().mapToInt(t -> t.transactionAmount).sum());

        // SFT-006: Share transactions > Rs 10L → must cross-check with Schedule CG
        List<SFTTransaction> sft006 = byCode.getOrDefault("SFT-006", List.of());
        if (!sft006.isEmpty()) {
            summary.addFlag("SFT-006", "Share transactions Rs " + sft006.stream().mapToInt(t -> t.transactionAmount).sum() + " in AIS — verify all are reflected in Schedule CG.");
        }

        // SFT-010/011: Dividends from MF / shares → must match OS declared income
        List<SFTTransaction> dividends = new ArrayList<>();
        dividends.addAll(byCode.getOrDefault("SFT-010", List.of()));
        dividends.addAll(byCode.getOrDefault("SFT-011", List.of()));
        summary.setAISDividendTotal(dividends.stream().mapToInt(t -> t.transactionAmount).sum());

        // SFT-012: Property purchase → Schedule AL immovable property
        List<SFTTransaction> sft012 = byCode.getOrDefault("SFT-012", List.of());
        sft012.forEach(t -> summary.addPropertyPurchase(t.reportingEntity, t.transactionAmount, t.transactionDate));

        // SFT-013: Property sale > Rs 30L → MANDATORY CG disclosure
        List<SFTTransaction> sft013 = byCode.getOrDefault("SFT-013", List.of());
        if (!sft013.isEmpty()) {
            summary.addFlag("SFT-013", "Property sale(s) of Rs " + sft013.stream().mapToInt(t -> t.transactionAmount).sum() + " found in AIS. Must be declared in Schedule CG.");
        }

        // SFT-014: Foreign remittance LRS → Schedule FA
        List<SFTTransaction> sft014 = byCode.getOrDefault("SFT-014", List.of());
        if (!sft014.isEmpty()) {
            summary.setHasForeignRemittance(true);
            summary.addFlag("SFT-014", "Foreign remittance under LRS detected. Schedule FA disclosure required.");
        }

        // SFT-015: Cash deposits > Rs 10L aggregate → scrutiny risk
        processSFT("SFT-015", byCode, 1000000, "Cash deposits", summary);

        // SFT-017: Foreign assets → Schedule FA mandatory
        List<SFTTransaction> sft017 = byCode.getOrDefault("SFT-017", List.of());
        if (!sft017.isEmpty()) {
            summary.setHasForeignAssets(true);
        }

        return summary;
    }

    private void processSFT(String code, Map<String, List<SFTTransaction>> byCode, int threshold, String label, SFTSummary summary) {
        int total = byCode.getOrDefault(code, List.of()).stream().mapToInt(t -> t.transactionAmount).sum();
        if (total > threshold) {
            summary.addFlag(code, label + " Rs " + total + " exceeds threshold Rs " + threshold + " — verify source and ensure income is declared.");
        }
    }

    public ScheduleALData autoPopulateScheduleAL(SFTSummary sft, int totalIncome) {
        if (totalIncome <= 5000000) return null; // only mandatory if > Rs 50L

        ScheduleALData al = new ScheduleALData();
        al.requiresVerification = true;

        // Property purchases → immovable assets
        sft.getPropertyPurchases().forEach(p -> {
            ScheduleALData.ImmovablePropertyEntry entry = new ScheduleALData.ImmovablePropertyEntry();
            entry.address = p.address;
            entry.costOfAcquisition = p.amount;
            entry.yearOfAcquisition = String.valueOf(p.date.getYear());
            al.immovableProperties.add(entry);
        });

        // Investments → financial assets
        al.sharesDebentures = sft.getNetMFInvestment();
        al.bankDeposits = sft.getTotalFDBalance();

        // Recompute totals
        al.totalAssets = al.immovableProperties.stream().mapToInt(p -> p.costOfAcquisition).sum()
            + al.sharesDebentures + al.bankDeposits;

        return al;
    }
}
```

---

## SECTION 10 — CREATE `ITRFilingOrchestrationService.java`

```java
@Service
public class ITRFilingOrchestrationService {

    // Inject all services
    private final MultiEmployerConsolidationService multiEmployerService;
    private final AISReconciliationService aisReconciliationService;
    private final SFTProcessingService sftProcessingService;
    private final ITRFormSelectionService formSelectionService;
    private final TaxRegimeComparisonService regimeComparisonService;
    private final AY202627TaxComputationService ay2627TaxService;
    private final ITR1CBDTValidationService itr1Validator;
    private final ITR2CBDTValidationService itr2Validator;
    private final ITR3CBDTValidationService itr3Validator;
    private final ITR4CBDTValidationService itr4Validator;
    private final PreSubmissionChecklistService checklistService;
    private final ITDJSONExportService itr1Exporter;
    private final ITR2JSONExportService itr2Exporter;
    private final ITR3JSONExportService itr3Exporter;
    private final ITR4JSONExportService itr4Exporter;
    private final AuditTrailService auditTrailService;
    private final LossLedgerRepository lossLedgerRepository;

    @Transactional
    public ITRFilingResult process(FilingRequest request) {
        String pan = request.getTaxpayerPAN();
        String ay = request.getAssessmentYear();

        // Step 1: Consolidate Form 16 (multi-employer safe)
        Form16ConsolidatedData form16 = multiEmployerService.consolidate(request.getForm16List());
        auditTrailService.logComputation(pan, ay, "Form 16 consolidated: " + request.getForm16List().size() + " employer(s)", "FORM_16", null, null, null);

        // Step 2: Parse AIS + SFT
        AISData aisData = aisReconciliationService.parseAIS(request.getAisJson());
        SFTSummary sft = sftProcessingService.processSFTData(aisData);

        // Step 3: Reconcile AIS vs declared income
        ReconciliationReport recon = aisReconciliationService.reconcile(form16, aisData, request.getTaxpayerData());
        auditTrailService.logComputation(pan, ay, "AIS reconciliation: " + recon.getGapCount() + " gap(s)", "AIS", null, null, null);

        // Step 4: Load prior-year losses from LossLedger
        List<LossLedgerEntry> priorLosses = lossLedgerRepository.findActive(pan, ay);
        request.getTaxpayerData().setBroughtForwardLosses(priorLosses);

        // Step 5: Select ITR form
        String itrForm = formSelectionService.selectForm(request.getTaxpayerData());
        auditTrailService.logComputation(pan, ay, "ITR form selected: " + itrForm, "SYSTEM", "ITR_FORM", null, itrForm);

        // Step 6: Tax computation (both regimes, both AYs)
        RegimeComparison regimeComparison = regimeComparisonService.compare(request.getTaxpayerData());
        auditTrailService.logComputation(pan, ay, "Regime comparison: Old Rs " + regimeComparison.getOldRegimeTax() + " vs New Rs " + regimeComparison.getNewRegimeTax(), "SYSTEM", "TAX_COMPUTATION", null, null);

        // Step 7: Validate
        ValidationResult validation = switch (itrForm) {
            case "ITR1" -> itr1Validator.validate(request.getItr1FormData());
            case "ITR2" -> itr2Validator.validate(request.getItr2FormData());
            case "ITR3" -> itr3Validator.validate(request.getItr3FormData());
            case "ITR4" -> itr4Validator.validate(request.getItr4FormData());
            default -> throw new IllegalStateException("Unknown form: " + itrForm);
        };

        if (validation.hasErrors()) {
            auditTrailService.logValidation(pan, ay, "BATCH", "ALL_RULES", validation);
            return ITRFilingResult.blockedByValidation(validation);
        }

        // Step 8: Pre-submission checklist
        ChecklistResult checklist = checklistService.runChecklist(request.getTaxpayerData(), itrForm);
        if (!checklist.allMandatoryPassed()) {
            return ITRFilingResult.blockedByChecklist(checklist.getFailedChecks());
        }

        // Step 9: Generate JSON
        String itdJson = switch (itrForm) {
            case "ITR1" -> itr1Exporter.export(request.getItr1FormData());
            case "ITR2" -> itr2Exporter.export(request.getItr2FormData());
            case "ITR3" -> itr3Exporter.export(request.getItr3FormData());
            case "ITR4" -> itr4Exporter.export(request.getItr4FormData());
            default -> throw new IllegalStateException("Unknown form: " + itrForm);
        };
        auditTrailService.logComputation(pan, ay, "ITR JSON generated for " + itrForm, "SYSTEM", "JSON_OUTPUT", null, "SUCCESS");

        // Step 10: Persist carry-forward losses
        persistCarryForwardLosses(pan, ay, request);

        return ITRFilingResult.success(itdJson, validation, regimeComparison, checklist, sft);
    }

    private void persistCarryForwardLosses(String pan, String ay, FilingRequest request) {
        // Read CFL from the relevant form data
        ScheduleCFLData cfl = request.getCFLData();
        if (cfl == null) return;

        int ayStart = Integer.parseInt(ay.split("-")[0]);

        if (cfl.hpLossToCarryForward > 0) {
            lossLedgerRepository.save(LossLedger.of(pan, ay, "HP", cfl.hpLossToCarryForward, (ayStart + 8) + "-" + (ayStart + 9)));
        }
        if (cfl.businessLossToCarryForward > 0) {
            lossLedgerRepository.save(LossLedger.of(pan, ay, "BUSINESS_NON_SPECULATIVE", cfl.businessLossToCarryForward, (ayStart + 8) + "-" + (ayStart + 9)));
        }
        if (cfl.speculativeLossToCarryForward > 0) {
            lossLedgerRepository.save(LossLedger.of(pan, ay, "SPECULATIVE", cfl.speculativeLossToCarryForward, (ayStart + 4) + "-" + (ayStart + 5)));
        }
        if (cfl.stcgLossToCarryForward > 0) {
            lossLedgerRepository.save(LossLedger.of(pan, ay, "STCG", cfl.stcgLossToCarryForward, (ayStart + 8) + "-" + (ayStart + 9)));
        }
        if (cfl.ltcgLossToCarryForward > 0) {
            lossLedgerRepository.save(LossLedger.of(pan, ay, "LTCG", cfl.ltcgLossToCarryForward, (ayStart + 8) + "-" + (ayStart + 9)));
        }
        if (cfl.unabsorbedDepToCarryForward > 0) {
            // Unabsorbed depreciation: UNLIMITED — set expiry to 9999
            lossLedgerRepository.save(LossLedger.of(pan, ay, "UNABSORBED_DEPRECIATION", cfl.unabsorbedDepToCarryForward, "9999-00"));
        }
    }
}
```

---

## SECTION 11 — PERSISTENCE LAYER

### 11.1 Create `LossLedger.java` (JPA Entity)

```java
@Entity
@Table(name = "loss_ledger")
public class LossLedger {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "taxpayer_pan", nullable = false, length = 10)
    private String taxpayerPAN;

    @Column(name = "incurred_ay", nullable = false, length = 7)
    private String incurredAY;

    @Column(name = "loss_type", nullable = false, length = 30)
    private String lossType;

    @Column(name = "original_amount", nullable = false)
    private int originalAmount;

    @Column(name = "utilized_amount", nullable = false)
    private int utilizedAmount = 0;

    @Column(name = "remaining_amount", nullable = false)
    private int remainingAmount;

    @Column(name = "expiry_ay", nullable = false, length = 7)
    private String expiryAY;

    @Column(name = "is_expired", nullable = false)
    private boolean isExpired = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public static LossLedger of(String pan, String ay, String type, int amount, String expiryAY) {
        LossLedger ll = new LossLedger();
        ll.taxpayerPAN = pan;
        ll.incurredAY = ay;
        ll.lossType = type;
        ll.originalAmount = amount;
        ll.remainingAmount = amount;
        ll.expiryAY = expiryAY;
        return ll;
    }
    // getters/setters
}
```

### 11.2 Create `LossLedgerRepository.java`

```java
@Repository
public interface LossLedgerRepository extends JpaRepository<LossLedger, Long> {

    @Query("SELECT l FROM LossLedger l WHERE l.taxpayerPAN = :pan AND l.isExpired = false AND l.remainingAmount > 0 AND l.expiryAY >= :currentAY")
    List<LossLedger> findActive(@Param("pan") String pan, @Param("currentAY") String currentAY);

    List<LossLedger> findByTaxpayerPANAndIncurredAY(String pan, String incurredAY);
}
```

### 11.3 Database Migration Script

Create `V3__create_loss_ledger.sql`:

```sql
CREATE TABLE IF NOT EXISTS loss_ledger (
    id BIGSERIAL PRIMARY KEY,
    taxpayer_pan VARCHAR(10) NOT NULL,
    incurred_ay VARCHAR(7) NOT NULL,
    loss_type VARCHAR(30) NOT NULL,
    original_amount INTEGER NOT NULL,
    utilized_amount INTEGER NOT NULL DEFAULT 0,
    remaining_amount INTEGER NOT NULL,
    expiry_ay VARCHAR(7) NOT NULL,
    is_expired BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_loss_ledger_pan_ay ON loss_ledger(taxpayer_pan, incurred_ay);
CREATE INDEX IF NOT EXISTS idx_loss_ledger_active ON loss_ledger(taxpayer_pan, is_expired, remaining_amount);

-- Existing audit_trail table (if not already created in a prior migration):
CREATE TABLE IF NOT EXISTS audit_trail (
    id BIGSERIAL PRIMARY KEY,
    taxpayer_pan VARCHAR(10) NOT NULL,
    assessment_year VARCHAR(7) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_description TEXT NOT NULL,
    data_source VARCHAR(100),
    field_name VARCHAR(200),
    old_value TEXT,
    new_value TEXT,
    validation_rule_id VARCHAR(30),
    validation_result VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_audit_pan_ay ON audit_trail(taxpayer_pan, assessment_year);
```

---

## SECTION 12 — COMPILE VERIFICATION AND SMOKE TEST

After completing all sections above, run:

```bash
# Step 1: Compile — must produce 0 errors, 0 warnings about missing symbols
mvn clean compile -q

# Step 2: Run all tests
mvn test

# Step 3: Build fat JAR
mvn package -DskipTests

# Step 4: Smoke test — verify all services wire correctly
# Start the application and POST to:
# POST /api/itr/validate/ITR2  with a minimal valid Itr2FormData JSON
# POST /api/itr/validate/ITR4  with a minimal valid Itr4FormData JSON
# Expect: 200 OK with validationResult.errors = []
```

### Expected Final File Count After Phase 6

| Category | Phase 5 | Phase 6 Additions | Total |
|----------|---------|-------------------|-------|
| Core Services | 18 | +6 (ITR2/3/4 val + export) | 24 |
| Supporting Services | 8 | +3 (VDA, SFT, Orchestrator) | 11 |
| DTO Classes | 10 | +4 (shared, business, loss, output) | 14 |
| Repository / Entity | 0 | +2 (LossLedger, AuditTrail) | 2 |
| **Total Java Files** | **140** | **+15** | **~155** |

---

## CRITICAL RULES — RE-READ BEFORE ANY CODE

1. **Sections 1–5 (DTOs) must be completed and compiled before Sections 6–9 (services)**. The services import from the DTOs. Out-of-order execution causes the same DTO-not-found failure that caused Phase 5 to delete the services.

2. **Never remove a working service.** If a new service fails to compile, fix the missing dependency (add the DTO field) — do not delete the service.

3. **All monetary amounts in ITD JSON = integers.** Use `Math.round()` before any integer cast.

4. **VDA loss is always zero.** `Math.max(0, consideration - cost)` — never allow negative.

5. **F&O turnover = sum of absolute values** of each trade's P&L. Not gross contract value.

6. **Speculative income (intraday equity) is a separate field** from non-speculative. They must never be summed.

7. **New regime ITR-4**: only standard deduction + 80CCD(2) + 80CCH. Block all others in validation.

8. **Loss carry-forward persistence** (Section 11) must run inside the same `@Transactional` scope as JSON generation. If JSON generation fails, losses must not be persisted.

9. **ITR-1 is production-ready — do not touch it.**
