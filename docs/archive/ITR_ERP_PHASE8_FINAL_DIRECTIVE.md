# ITR ERP — PHASE 8: DEFINITIVE FINAL DIRECTIVE
## End-to-End Production Deployment — Zero Gaps Remaining
**Date:** April 11, 2026
**System State Going In:** Validation 100% ✅ | Persistence 100% ✅ | Orchestration 100% ✅ | VDA 100% ✅ | SFT 100% ✅ | **JSON Export 0% ❌ | AY 2026-27 integration 0% ❌ | Depreciation computation 50% ❌ | Auto-population 0% ❌**
**Target:** BUILD SUCCESS, all four ITR forms generate valid ITD-uploadable JSON, no manual fallbacks.

---

## AUDIT FINDINGS — EXACTLY WHAT IS MISSING

The previous seven phases achieved full validation coverage but left the **output layer** entirely unimplemented. The system can verify a return but cannot produce one. Here is the precise gap inventory from the Phase 7 audit:

| Component | Status | Blocker Level |
|-----------|--------|---------------|
| `ITR2JSONExportService.java` | ❌ Does not exist | **FILING BLOCKER** |
| `ITR3JSONExportService.java` | ❌ Does not exist | **FILING BLOCKER** |
| `ITR4JSONExportService.java` | ❌ Does not exist | **FILING BLOCKER** |
| AY 2026-27 integration in `ITRFilingOrchestrationService` | ❌ `AY202627TaxComputationService` built but never wired | Filing wrong tax |
| `DepreciationEngine.computeTotal()` | ⚠️ Validates only; does not produce computed figures | ITR-3 P&L manual |
| `ScheduleALService.autoPopulate()` | ❌ Manual entry only | UX gap |
| `HRAComputationService` metro detection | ❌ Manual city input | UX gap |
| `MultiEmployerConsolidationService` cross-validation | ❌ Aggregation only, no cross-checks | Duplicate deduction risk |

**The single most important task in this entire directive is creating the three JSON export services.** Everything else is wiring and polish. Do those three first.

---

## EXECUTION ORDER — MANDATORY

```
1. ITR2JSONExportService.java          ← Start here. ~600 lines. Creates uploadable JSON.
2. ITR3JSONExportService.java          ← ~700 lines. Inherits ITR-2 + adds BP/DPM/DCG.
3. ITR4JSONExportService.java          ← ~400 lines. Simpler — presumptive schema.
4. DepreciationEngine — add compute()  ← ~50 lines. Completes the existing skeleton.
5. AY 2026-27 — wire in Orchestration  ← ~30 lines. Service exists; just wire it.
6. ScheduleAL auto-population          ← ~60 lines. Extend existing SFTProcessingService.
7. HRA metro auto-detection            ← ~20 lines. Add lookup to HRAComputationService.
8. MultiEmployer cross-validation      ← ~80 lines. Extend existing service.
9. Final compile + smoke test          ← Must pass before this directive is complete.
```

**Do not jump to step 4 before step 3 compiles.** The export services are the gate.

---

## SECTION 1 — `ITR2JSONExportService.java` (CREATE FROM SCRATCH)

This is the most critical file in this directive. It transforms `Itr2FormData` into a JSON string conforming exactly to the ITD upload schema. The existing `ITDJSONExportService.java` (ITR-1) is your reference — replicate its pattern, extend it for ITR-2 schedules.

### 1.1 Class skeleton and dependencies

```java
package com.yourpackage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.yourpackage.dto.*;
import com.yourpackage.dto.ITRSharedDtos.*;
import com.yourpackage.util.ITDDateFormatter;
import com.yourpackage.util.SHA256DigestUtil;
import org.springframework.stereotype.Service;

@Service
public class ITR2JSONExportService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SHA256DigestUtil digestUtil;      // already exists — inject it
    private final ITDDateFormatter dateFormatter;  // already exists — inject it

    public ITR2JSONExportService(SHA256DigestUtil digestUtil, ITDDateFormatter dateFormatter) {
        this.digestUtil = digestUtil;
        this.dateFormatter = dateFormatter;
    }

    /**
     * Produces ITD-uploadable JSON for ITR-2.
     * Output format: { "ITR": { "ITR2": { "CreationInfo": {...}, "Form_ITR2": {...} } } }
     * All amounts: integers. All dates: DD/MM/YYYY. No null fields in output.
     */
    public String export(Itr2FormData data) {
        try {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode itr  = mapper.createObjectNode();
            ObjectNode itr2 = mapper.createObjectNode();

            itr2.set("CreationInfo",  buildCreationInfo(data));
            itr2.set("Form_ITR2",     buildFormITR2(data));

            itr.set("ITR2", itr2);
            root.set("ITR", itr);

            // Compute and inject SHA-256 digest AFTER building full JSON
            String jsonWithoutDigest = mapper.writeValueAsString(root);
            String digest = digestUtil.computeSHA256(jsonWithoutDigest);
            ((ObjectNode) root.path("ITR").path("ITR2").path("CreationInfo"))
                .put("Digest", digest);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("ITR-2 JSON export failed: " + e.getMessage(), e);
        }
    }
```

### 1.2 CreationInfo block

```java
    private ObjectNode buildCreationInfo(Itr2FormData data) {
        ObjectNode ci = mapper.createObjectNode();
        ci.put("SWVersionNo",     "1.1");
        ci.put("SWCreatedBy",     "ITR_ERP_v1");
        ci.put("JSONCreatedBy",   "ITR_ERP_v1");
        ci.put("JSONCreatedDate", dateFormatter.todayDDMMYYYY());
        ci.put("InterfaceType",   "JSON");
        // CRITICAL: IntermediaryCity must be <= 25 characters — truncate if longer
        String city = data.getIntermediaryCity() != null ? data.getIntermediaryCity() : "SYSTEM";
        ci.put("IntermediaryCity", city.length() > 25 ? city.substring(0, 25) : city);
        ci.put("Digest",          ""); // placeholder — replaced after full JSON built
        return ci;
    }
```

### 1.3 PersonalInfo block

```java
    private ObjectNode buildPersonalInfo(Itr2FormData data) {
        ObjectNode pi = mapper.createObjectNode();

        // Assessee name
        ObjectNode name = mapper.createObjectNode();
        name.put("FirstName",        nvl(data.getFirstName()));
        name.put("MiddleName",       nvl(data.getMiddleName()));
        name.put("SurNameOrOrgName", nvl(data.getSurName()));
        pi.set("AssesseeName", name);

        pi.put("PAN",    data.getPAN());
        pi.put("DOB",    dateFormatter.format(data.getDateOfBirth()));
        pi.put("AadhaarCardNo",       nvl(data.getAadhaarNumber()));
        pi.put("AadhaarEnrolmentID",  nvl(data.getAadhaarEnrolmentId()));
        pi.put("PrimaryMobileNo",     nvl(data.getMobileNumber()));
        pi.put("EmailID",             nvl(data.getEmail()));

        ObjectNode addr = mapper.createObjectNode();
        addr.put("ResidenceName",        nvl(data.getFlatDoorBlock()));
        addr.put("ResidenceNo",          nvl(data.getPremisesName()));
        addr.put("RoadOrStreet",         nvl(data.getRoadStreet()));
        addr.put("LocalityOrArea",       nvl(data.getLocality()));
        addr.put("CityOrTownOrDistrict", nvl(data.getCity()));
        addr.put("StateCode",            nvl(data.getStateCode()));
        addr.put("PinCode",              nvl(data.getPinCode()));
        addr.put("CountryCode",          "91");
        pi.set("Address", addr);

        pi.put("EmployerCategory",   nvl(data.getEmployerCategory()));
        pi.put("ResidentialStatus",  "RES");  // ITR-2 for residents only in most cases
        pi.put("Status",             "I");    // Individual
        pi.put("AssesseeType",       computeAssesseeType(data)); // 05/06/07/08
        pi.put("SeventhProvisotoSec139i", "N");

        return pi;
    }

    private String computeAssesseeType(Itr2FormData data) {
        // Age computed as on April 1 of the Financial Year
        if (data.getDateOfBirth() == null) return "05";
        java.time.LocalDate refDate = java.time.LocalDate.of(
            Integer.parseInt(data.getAssessmentYear().split("-")[0]) - 1, 4, 1);
        int age = java.time.Period.between(data.getDateOfBirth(), refDate).getYears();
        if (age >= 80) return "07";
        if (age >= 60) return "06";
        return "05";
    }
```

### 1.4 FilingStatus block

```java
    private ObjectNode buildFilingStatus(Itr2FormData data) {
        ObjectNode fs = mapper.createObjectNode();
        fs.put("ReturnFileSec",         "11"); // 139(1) voluntary; override if belated/revised
        fs.put("ReturnType",            "O");  // Original
        fs.put("IsRevised",             "N");
        fs.put("IsDefective",           "N");
        fs.put("TaxRegime",             data.getTaxRegime() == TaxRegime.OLD ? "O" : "N");
        fs.put("OptOutNewTaxRegime",    data.getTaxRegime() == TaxRegime.OLD ? "Y" : "N");
        fs.put("ModeOfFiling",          "O"); // Online
        // Regime-specific fields
        if (data.getForm10IEAAckNo() != null) {
            fs.put("Form10IEAFiled",    "Y");
            fs.put("Form10IEAAckNo",    data.getForm10IEAAckNo());
        } else {
            fs.put("Form10IEAFiled",    "N");
        }
        return fs;
    }
```

### 1.5 ScheduleS — Salary (same as ITR-1; reuse pattern)

```java
    private ObjectNode buildScheduleS(Itr2FormData data) {
        ObjectNode s = mapper.createObjectNode();
        // Gross salary components
        s.put("Salary",               safeInt(data.getBasicSalary()));
        s.put("PerquisitesValue",     safeInt(data.getPerquisites()));
        s.put("ProfitsInLieuSalary",  safeInt(data.getProfitsInLieu()));
        s.put("GrossSalary",          safeInt(data.getGrossSalary()));

        // Exemptions under Section 10
        ObjectNode allwExempt = mapper.createObjectNode();
        allwExempt.put("Sec10_5_LTA",         safeInt(data.getLtaExemption()));
        allwExempt.put("Sec10_10_Gratuity",    safeInt(data.getGratuityExemption()));
        allwExempt.put("Sec10_10A_CommutedPension", safeInt(data.getCommutedPensionExemption()));
        allwExempt.put("Sec10_10AA_LeaveEncashment", safeInt(data.getLeaveEncashmentExemption()));
        allwExempt.put("Sec10_13A_HRA",        safeInt(data.getHraExemption()));
        allwExempt.put("TotalExemptAllowances", safeInt(data.getTotalExemptAllowances()));
        s.set("AllwncExemptUs10", allwExempt);

        s.put("NetSalary",            safeInt(data.getNetSalary()));

        // Section 16 deductions
        // New regime: standard deduction = 75,000; old regime = 50,000
        int stdDed = data.getTaxRegime() == TaxRegime.NEW ? 75000 : 50000;
        stdDed = Math.min(stdDed, safeInt(data.getNetSalary())); // cannot exceed net salary
        s.put("DeductionUs16ia",      stdDed);
        s.put("DeductionUs16ii",      safeInt(data.getEntertainmentAllowance())); // Govt only
        // New regime: professional tax = 0
        int profTax = data.getTaxRegime() == TaxRegime.NEW ? 0 : Math.min(2500, safeInt(data.getProfessionalTax()));
        s.put("DeductionUs16iii",     profTax);
        s.put("DeductionUs16Total",   stdDed + safeInt(data.getEntertainmentAllowance()) + profTax);

        s.put("IncomeFromSalaries",   safeInt(data.getIncomeFromSalary()));
        return s;
    }
```

### 1.6 ScheduleHP — House Property (ITR-2 supports multiple)

```java
    private ObjectNode buildScheduleHP(Itr2FormData data) {
        ObjectNode hp = mapper.createObjectNode();
        ArrayNode props = mapper.createArrayNode();

        if (data.getHouseProperties() != null) {
            for (PropertyDetail p : data.getHouseProperties()) {
                ObjectNode prop = mapper.createObjectNode();
                prop.put("PropertyAddress",       nvl(p.address));
                prop.put("TypeOfHP",              mapPropertyType(p.type)); // S/L/D
                prop.put("GrossRentReceived",     safeInt(p.grossAnnualValue));
                prop.put("TaxPaidLocalAuthority", safeInt(p.municipalTax));
                prop.put("AnnualValue",           safeInt(p.netAnnualValue));
                // Standard deduction: 30% of Annual Value (only for let-out/DLO)
                int stdDed = (p.type == PropertyType.SELF_OCCUPIED) ? 0 : safeInt(p.standardDeduction);
                prop.put("StandardDeduction30Pct",stdDed);
                // Interest on housing loan
                int interest = p.interestOnBorrowedCapital;
                // Cap for old regime SOP
                if (data.getTaxRegime() == TaxRegime.OLD && p.type == PropertyType.SELF_OCCUPIED) {
                    interest = Math.min(interest, 200000);
                }
                if (data.getTaxRegime() == TaxRegime.NEW && p.type == PropertyType.SELF_OCCUPIED) {
                    interest = 0; // No deduction under new regime for SOP
                }
                prop.put("InterestPayable",       interest);
                prop.put("ArrearUnrealizedRent",  safeInt(p.arrearUnrealisedRent));
                prop.put("IncomeFromHP",
                    safeInt(p.netAnnualValue) - stdDed - interest + safeInt(p.arrearUnrealisedRent));

                // Co-ownership details
                if (p.isCoOwned) {
                    prop.put("CoOwnerPAN",        nvl(p.coOwnerPAN));
                    prop.put("OwnerSharePct",     p.ownershipSharePct);
                }

                props.add(prop);
            }
        }

        hp.set("PropertyDetails", props);

        // Aggregate HP income across all properties
        int aggregateHP = data.getHouseProperties() == null ? 0 :
            data.getHouseProperties().stream().mapToInt(p -> {
                int nav = p.netAnnualValue;
                int sd  = p.type == PropertyType.SELF_OCCUPIED ? 0 : p.standardDeduction;
                int int_ = data.getTaxRegime() == TaxRegime.NEW && p.type == PropertyType.SELF_OCCUPIED
                    ? 0 : Math.min(p.interestOnBorrowedCapital,
                        (data.getTaxRegime() == TaxRegime.OLD && p.type == PropertyType.SELF_OCCUPIED) ? 200000 : Integer.MAX_VALUE);
                return nav - sd - int_ + p.arrearUnrealisedRent;
            }).sum();

        // HP loss capped at Rs 2,00,000 for set-off; excess carried forward
        int hpLossSetOff = 0;
        int hpLossCarriedForward = 0;
        if (aggregateHP < 0) {
            hpLossSetOff = Math.min(Math.abs(aggregateHP), 200000);
            hpLossCarriedForward = Math.abs(aggregateHP) - hpLossSetOff;
        }

        hp.put("TotalHPIncome",          aggregateHP);
        hp.put("HPLossSetOffLimit",       hpLossSetOff);
        hp.put("HPLossCarriedForward",    hpLossCarriedForward);
        return hp;
    }

    private String mapPropertyType(PropertyType type) {
        return switch (type) {
            case SELF_OCCUPIED  -> "S";
            case LET_OUT        -> "L";
            case DEEMED_LET_OUT -> "D";
        };
    }
```

### 1.7 ScheduleCG — Capital Gains

```java
    private ObjectNode buildScheduleCG(Itr2FormData data) {
        ScheduleCGData cg = data.getScheduleCG();
        ObjectNode cgNode = mapper.createObjectNode();
        if (cg == null) return cgNode;

        // ── STCG Section ──────────────────────────────────────────────────────
        ObjectNode stcg = mapper.createObjectNode();

        // Pre-July 23, 2024: STCG 111A at 15%
        ObjectNode stcgPre = mapper.createObjectNode();
        stcgPre.put("TaxableRateSec111A_15Pct", safeInt(cg.stcg111A_preJul23));
        stcg.set("SaleBeforeJuly2024_111A", stcgPre);

        // Post-July 23, 2024: STCG 111A at 20%
        ObjectNode stcgPost = mapper.createObjectNode();
        stcgPost.put("TaxableRateSec111A_20Pct", safeInt(cg.stcg111A_postJul23));
        stcg.set("SaleOnOrAfterJuly2024_111A", stcgPost);

        // STCG at slab rate (debt MFs, unlisted shares, etc.)
        stcg.put("STCGOnOtherAssets_SlabRate", safeInt(cg.stcgAtSlab));
        stcg.put("TotalSTCG", safeInt(cg.totalSTCG));
        cgNode.set("ShortTermCapGain", stcg);

        // ── Schedule 112A — LTCG on Listed Equity ─────────────────────────────
        if (cg.schedule112ARows != null && !cg.schedule112ARows.isEmpty()) {
            ArrayNode rows112A = mapper.createArrayNode();
            for (Schedule112ARow row : cg.schedule112ARows) {
                ObjectNode r = mapper.createObjectNode();
                r.put("ISINCode",             nvl(row.isinCode));
                r.put("NameOfScrip",          nvl(row.nameOfScrip));
                r.put("SharesOrUnits",        safeInt(row.units));
                r.put("SalePricePerUnit",     safeInt(row.salePricePerUnit));
                r.put("TotalSaleValue",       safeInt(row.totalSaleValue));
                r.put("ActualCostOfAcq",      safeInt(row.actualCostOfAcquisition));
                // FMV grandfathering — only if acquired before Feb 1, 2018
                boolean preGrandfathering = row.dateOfAcquisition != null &&
                    row.dateOfAcquisition.isBefore(java.time.LocalDate.of(2018, 2, 1));
                r.put("FMVPerShareOn31Jan2018", preGrandfathering ? safeInt(row.fmvPerShareOn31Jan2018) : 0);
                r.put("TotalFMV_55_2_ac",      preGrandfathering ? safeInt(row.totalFMV55_2_ac) : 0);
                // Col7 = max(actual cost, FMV Jan31) if pre-Feb2018; else actual cost
                int col7 = preGrandfathering
                    ? Math.max(row.actualCostOfAcquisition, row.fmvOn31Jan2018)
                    : row.actualCostOfAcquisition;
                r.put("CostWithoutIndexation", col7);
                r.put("ExpenditureOnTransfer", safeInt(row.expenditureOnTransfer));
                r.put("TotalDeductions",       col7 + safeInt(row.expenditureOnTransfer));
                r.put("BalanceLTCG",           safeInt(row.totalSaleValue) - col7 - safeInt(row.expenditureOnTransfer));
                r.put("TransferDate",          row.dateOfTransfer != null ? dateFormatter.format(row.dateOfTransfer) : "");
                r.put("ExemptionUs54EC",       safeInt(row.exemptionUs54EC));
                r.put("ExemptionUs54F",        safeInt(row.exemptionUs54F));
                r.put("PreJuly23Sale",         row.preJuly23Sale ? "Y" : "N");
                rows112A.add(r);
            }
            cgNode.set("Schedule112A", rows112A);
        }

        // ── LTCG Summary ──────────────────────────────────────────────────────
        ObjectNode ltcg = mapper.createObjectNode();

        // Pre-July 23 pool: 10% rate, Rs 1,00,000 exemption
        int ltcgPre = safeInt(cg.ltcg112A_preJul23);
        int taxablePre = Math.max(0, ltcgPre - 100000);
        ObjectNode ltcgPreNode = mapper.createObjectNode();
        ltcgPreNode.put("TotalLTCG112A_PreJul23",     ltcgPre);
        ltcgPreNode.put("ExemptionThreshold_100000",  Math.min(ltcgPre, 100000));
        ltcgPreNode.put("TaxableLTCG_10Pct",          taxablePre);
        ltcg.set("LTCGEquity_PreJuly2024_112A", ltcgPreNode);

        // Post-July 23 pool: 12.5% rate, Rs 1,25,000 exemption
        int ltcgPost = safeInt(cg.ltcg112A_postJul23);
        int taxablePost = Math.max(0, ltcgPost - 125000);
        ObjectNode ltcgPostNode = mapper.createObjectNode();
        ltcgPostNode.put("TotalLTCG112A_PostJul23",    ltcgPost);
        ltcgPostNode.put("ExemptionThreshold_125000",  Math.min(ltcgPost, 125000));
        ltcgPostNode.put("TaxableLTCG_12_5Pct",        taxablePost);
        ltcg.set("LTCGEquity_PostJuly2024_112A", ltcgPostNode);

        // Property LTCG
        ObjectNode ltcgProp = mapper.createObjectNode();
        ltcgProp.put("SaleConsideration",       safeInt(cg.fullValueOfConsideration)); // 50C adjusted
        ltcgProp.put("IndexedCostOfAcquisition",safeInt(cg.ltcgPropertyWithIndexation > 0 ?
            data.getIndexedCostOfAcquisition() : 0));
        ltcgProp.put("ExemptionUs54",           safeInt(cg.exemptionUs54));
        ltcgProp.put("ExemptionUs54EC",         safeInt(cg.exemptionUs54EC));
        ltcgProp.put("ExemptionUs54F",          safeInt(cg.exemptionUs54F));
        ltcgProp.put("TaxableLTCG_20Pct",       safeInt(cg.ltcgPropertyWithIndexation));
        ltcgProp.put("TaxableLTCG_12_5Pct",     safeInt(cg.ltcgPropertyWithoutIndexation));
        ltcg.set("LTCG_Property", ltcgProp);

        ltcg.put("TotalLTCG", safeInt(cg.totalLTCG));
        cgNode.set("LongTermCapGain", ltcg);

        cgNode.put("TotalCGIncome", safeInt(cg.totalCGIncome));
        return cgNode;
    }
```

### 1.8 ScheduleVDA

```java
    private ObjectNode buildScheduleVDA(Itr2FormData data) {
        ObjectNode vda = mapper.createObjectNode();
        ArrayNode details = mapper.createArrayNode();

        if (data.getVdaTransactions() != null) {
            for (VDATransaction t : data.getVdaTransactions()) {
                ObjectNode row = mapper.createObjectNode();
                row.put("DateOfAcquisition",            t.dateOfAcquisition != null ? dateFormatter.format(t.dateOfAcquisition) : "");
                row.put("DateOfTransfer",               t.dateOfTransfer    != null ? dateFormatter.format(t.dateOfTransfer)    : "");
                row.put("HeadUnderWhichIncomeTaxable",  "CG");
                row.put("CostOfAcquisition",            safeInt(t.costOfAcquisition));
                row.put("ConsiderationReceived",        safeInt(t.considerationReceived));
                row.put("IncomeFromVDA",                Math.max(0, t.considerationReceived - t.costOfAcquisition));
                details.add(row);
            }
        }

        vda.set("VDADetails", details);
        vda.put("TotalIncomeFromVDA", safeInt(data.getVdaIncome()));
        vda.put("TDSUs194S", data.getVdaTransactions() == null ? 0 :
            data.getVdaTransactions().stream().mapToInt(t -> t.tdsDeductedUnder194S).sum());
        return vda;
    }
```

### 1.9 ScheduleVIA — Chapter VI-A Deductions

```java
    private ObjectNode buildScheduleVIA(Itr2FormData data) {
        ObjectNode via = mapper.createObjectNode();

        boolean isNew = data.getTaxRegime() == TaxRegime.NEW;

        // New regime: block all deductions except 80CCD(2), 80JJAA, 80CCH(2)
        via.put("DeductionUs80C",      isNew ? 0 : safeInt(data.getDeduction80C()));
        via.put("DeductionUs80CCC",    isNew ? 0 : safeInt(data.getDeduction80CCC()));
        via.put("DeductionUs80CCD1",   isNew ? 0 : safeInt(data.getDeduction80CCD1()));
        via.put("DeductionUs80CCD1B",  isNew ? 0 : safeInt(data.getDeduction80CCD1B()));
        // 80CCD(2) employer NPS — allowed in both regimes
        via.put("DeductionUs80CCD2",   safeInt(data.getDeduction80CCD2()));
        // 80CCH Agniveer corpus — allowed in both regimes
        via.put("DeductionUs80CCH",    safeInt(data.getDeduction80CCH()));
        via.put("DeductionUs80D",      isNew ? 0 : safeInt(data.getDeduction80D()));
        via.put("DeductionUs80DD",     isNew ? 0 : safeInt(data.getDeduction80DD()));
        via.put("DeductionUs80DDB",    isNew ? 0 : safeInt(data.getDeduction80DDB()));
        via.put("DeductionUs80E",      isNew ? 0 : safeInt(data.getDeduction80E()));
        via.put("DeductionUs80EEA",    isNew ? 0 : safeInt(data.getDeduction80EEA()));
        via.put("DeductionUs80EEB",    isNew ? 0 : safeInt(data.getDeduction80EEB()));
        via.put("DeductionUs80G",      isNew ? 0 : safeInt(data.getDeduction80G()));
        via.put("DeductionUs80GG",     isNew ? 0 : safeInt(data.getDeduction80GG()));
        via.put("DeductionUs80GGA",    isNew ? 0 : safeInt(data.getDeduction80GGA()));
        via.put("DeductionUs80TTA",    isNew ? 0 : safeInt(data.getDeduction80TTA()));
        via.put("DeductionUs80TTB",    isNew ? 0 : safeInt(data.getDeduction80TTB()));
        via.put("DeductionUs80U",      isNew ? 0 : safeInt(data.getDeduction80U()));
        // 80JJAA allowed in both regimes
        via.put("DeductionUs80JJAA",   safeInt(data.getDeduction80JJAA()));

        // Total must match sum of above
        int total = safeInt(data.getTotalChapterVIADeductions());
        via.put("TotalChapVIADeductions", total);

        return via;
    }
```

### 1.10 Schedule80G — Donation details

```java
    private ObjectNode buildSchedule80G(Itr2FormData data) {
        ObjectNode g80 = mapper.createObjectNode();
        ArrayNode donations = mapper.createArrayNode();

        if (data.getSchedule80G() != null) {
            for (Schedule80GData.DonationEntry d : data.getSchedule80G().getDonations()) {
                // Cash > Rs 2,000: automatically excluded
                if (d.paymentMode == PaymentMode.CASH && d.donationAmount > 2000) continue;
                ObjectNode don = mapper.createObjectNode();
                don.put("DoneeName",        nvl(d.doneeName));
                don.put("DoneePAN",         nvl(d.doneePAN));
                don.put("DonationAmount",   safeInt(d.donationAmount));
                don.put("EligibleAmount",   safeInt(d.eligibleAmount));
                don.put("PaymentMode",      d.paymentMode.name());
                don.put("Is100PctDeduction", d.isQualified100Pct ? "Y" : "N");
                donations.add(don);
            }
        }

        g80.set("DonationDetails", donations);
        g80.put("TotalEligibleDeduction",
            data.getSchedule80G() == null ? 0 : safeInt(data.getSchedule80G().totalEligibleDeduction));
        return g80;
    }
```

### 1.11 TaxComputation block

```java
    private ObjectNode buildTaxComputation(Itr2FormData data) {
        ObjectNode tc = mapper.createObjectNode();
        tc.put("TaxPayableOnTI",       safeInt(data.getTaxOnTotalIncome()));
        tc.put("RebateUs87A",          safeInt(data.getRebate87A()));
        tc.put("TaxAfterRebate",       safeInt(data.getTaxAfterRebate()));
        tc.put("Surcharge",            safeInt(data.getSurcharge()));
        tc.put("SurchargeOnSpecialInc", safeInt(data.getSurchargeOnSpecialIncome()));
        tc.put("TotalSurcharge",       safeInt(data.getTotalSurcharge()));
        tc.put("HealthEduCess",        safeInt(data.getHealthEducationCess()));
        tc.put("TotalTaxAndCess",      safeInt(data.getTotalTaxAndCess()));
        tc.put("ReliefUs89",           safeInt(data.getReliefUs89()));
        tc.put("NetTaxLiability",      safeInt(data.getNetTaxLiability()));
        tc.put("IntrstUs234A",         safeInt(data.getInterest234A()));
        tc.put("IntrstUs234B",         safeInt(data.getInterest234B()));
        tc.put("IntrstUs234C",         safeInt(data.getInterest234C()));
        tc.put("LateFilingFeeUs234F",  safeInt(data.getLateFilingFee234F()));
        tc.put("TotalIntrstPnltyFee",
            safeInt(data.getInterest234A()) + safeInt(data.getInterest234B()) +
            safeInt(data.getInterest234C()) + safeInt(data.getLateFilingFee234F()));
        tc.put("GrossTaxLiability",    safeInt(data.getNetTaxLiability()) +
            safeInt(data.getInterest234A()) + safeInt(data.getInterest234B()) +
            safeInt(data.getInterest234C()) + safeInt(data.getLateFilingFee234F()));
        return tc;
    }
```

### 1.12 TaxPaid — TDS1/TDS2/TCS/AdvanceTax (reuse ITR-1 pattern exactly)

```java
    private ObjectNode buildTaxPaid(Itr2FormData data) {
        ObjectNode tp = mapper.createObjectNode();

        // TDS1 — Salary TDS
        ObjectNode tds1 = mapper.createObjectNode();
        ArrayNode tds1List = mapper.createArrayNode();
        if (data.getTdsSalaryEntries() != null) {
            for (TDSSalaryEntry e : data.getTdsSalaryEntries()) {
                ObjectNode t = mapper.createObjectNode();
                t.put("EmployerOrDeductorTAN",  nvl(e.tan));
                t.put("EmployerName",           nvl(e.employerName));
                t.put("TotalTDSSal",            safeInt(e.tdsDeducted));
                t.put("ClaimOutOfTotTDSSal",    safeInt(e.tdsClaimedThisYear));
                tds1List.add(t);
            }
        }
        tds1.set("TDSSal", tds1List);
        tds1.put("TotalTDS1TaxDeducted", data.getTdsSalaryEntries() == null ? 0 :
            data.getTdsSalaryEntries().stream().mapToInt(e -> e.tdsDeducted).sum());
        tds1.put("TotalTDS1TaxClaimed",  data.getTdsSalaryEntries() == null ? 0 :
            data.getTdsSalaryEntries().stream().mapToInt(e -> e.tdsClaimedThisYear).sum());
        tp.set("TDS1", tds1);

        // TDS2 — TDS on other income
        ObjectNode tds2 = mapper.createObjectNode();
        ArrayNode tds2List = mapper.createArrayNode();
        if (data.getTdsOtherEntries() != null) {
            for (TDSOtherEntry e : data.getTdsOtherEntries()) {
                ObjectNode t = mapper.createObjectNode();
                t.put("DeductorTAN",               nvl(e.tan));
                t.put("DeductorName",              nvl(e.deductorName));
                t.put("AmtOnWhichTDSDeducted",     safeInt(e.grossAmount));
                t.put("TaxDeducted",               safeInt(e.tdsDeducted));
                t.put("YrOfTaxDeduction",          nvl(e.yearOfDeduction));
                t.put("ClaimOutOfTotTDSOthThanSals", safeInt(e.tdsClaimedThisYear));
                tds2List.add(t);
            }
        }
        tds2.set("TDSOthThanSals", tds2List);
        tds2.put("TotalTDSOthThanSalClaimed", data.getTdsOtherEntries() == null ? 0 :
            data.getTdsOtherEntries().stream().mapToInt(e -> e.tdsClaimedThisYear).sum());
        tp.set("TDS2", tds2);

        // TCS
        ObjectNode tcs = mapper.createObjectNode();
        ArrayNode tcsList = mapper.createArrayNode();
        if (data.getTcsEntries() != null) {
            for (TCSEntry e : data.getTcsEntries()) {
                ObjectNode t = mapper.createObjectNode();
                t.put("CollectorTAN",          nvl(e.tan));
                t.put("CollectorName",         nvl(e.collectorName));
                t.put("AmtOnWhichTCSCollected",safeInt(e.grossAmount));
                t.put("TaxCollected",          safeInt(e.tcsCollected));
                t.put("ClaimOutOfTotTCS",      safeInt(e.tcsClaimedThisYear));
                tcsList.add(t);
            }
        }
        tcs.set("TCS", tcsList);
        tcs.put("TotalTCSClaimedThisYear", data.getTcsEntries() == null ? 0 :
            data.getTcsEntries().stream().mapToInt(e -> e.tcsClaimedThisYear).sum());
        tp.set("TCS", tcs);

        // Advance Tax / Self-Assessment Tax
        ObjectNode at = mapper.createObjectNode();
        ArrayNode atList = mapper.createArrayNode();
        if (data.getAdvanceTaxEntries() != null) {
            for (AdvanceTaxEntry e : data.getAdvanceTaxEntries()) {
                ObjectNode t = mapper.createObjectNode();
                t.put("BSRCode",      nvl(e.bsrCode));
                t.put("DateDep",      dateFormatter.format(e.dateOfDeposit));
                t.put("SrlNoOfChaln",nvl(e.challanSerialNumber));
                t.put("Amt",         safeInt(e.amount));
                t.put("Type",        nvl(e.paymentType)); // 200/300/400
                atList.add(t);
            }
        }
        at.set("AdvTaxDetails", atList);
        at.put("TotalAdvTaxPaid", data.getAdvanceTaxEntries() == null ? 0 :
            data.getAdvanceTaxEntries().stream().mapToInt(e -> e.amount).sum());
        tp.set("AdvanceTax", at);

        return tp;
    }
```

### 1.13 ScheduleAL — Assets and Liabilities

```java
    private ObjectNode buildScheduleAL(Itr2FormData data) {
        ScheduleALData al = data.getScheduleAL();
        if (al == null) return mapper.createObjectNode(); // not mandatory if income <= 50L

        ObjectNode alNode = mapper.createObjectNode();

        ArrayNode props = mapper.createArrayNode();
        if (al.immovableProperties != null) {
            for (ScheduleALData.ImmovablePropertyEntry p : al.immovableProperties) {
                ObjectNode pr = mapper.createObjectNode();
                pr.put("AddressOfProperty",  nvl(p.address));
                pr.put("CostOfAcquisition",  safeInt(p.costOfAcquisition));
                pr.put("YearOfAcquisition",  nvl(p.yearOfAcquisition));
                props.add(pr);
            }
        }
        alNode.set("ImmovablePropertyDetails", props);

        ObjectNode movable = mapper.createObjectNode();
        movable.put("Jewellery",          safeInt(al.jewellery));
        movable.put("ArtworkPaintings",   safeInt(al.artworkPaintings));
        movable.put("VehicleOtherComm",   safeInt(al.vehicles));
        movable.put("BullionGold",        safeInt(al.bullionGold));
        movable.put("OtherMovable",       safeInt(al.otherMovable));
        alNode.set("MovableAssets", movable);

        ObjectNode financial = mapper.createObjectNode();
        financial.put("SharesDebentures", safeInt(al.sharesDebentures));
        financial.put("InsurancePolicies",safeInt(al.insurancePolicies));
        financial.put("LoansGiven",       safeInt(al.loansGiven));
        financial.put("CashInHand",       safeInt(al.cashInHandAbove500K));
        financial.put("BankDeposits",     safeInt(al.bankDeposits));
        financial.put("OtherFinancial",   safeInt(al.otherFinancial));
        alNode.set("FinancialAssets", financial);

        int totalAssets = safeInt(al.jewellery) + safeInt(al.artworkPaintings) +
            safeInt(al.vehicles) + safeInt(al.bullionGold) + safeInt(al.otherMovable) +
            safeInt(al.sharesDebentures) + safeInt(al.insurancePolicies) +
            safeInt(al.loansGiven) + safeInt(al.cashInHandAbove500K) +
            safeInt(al.bankDeposits) + safeInt(al.otherFinancial) +
            (al.immovableProperties == null ? 0 :
             al.immovableProperties.stream().mapToInt(p -> p.costOfAcquisition).sum());
        alNode.put("TotalAssets", totalAssets);

        ObjectNode liab = mapper.createObjectNode();
        liab.put("SecuredLoans",     safeInt(al.securedLoans));
        liab.put("UnsecuredLoans",   safeInt(al.unsecuredLoans));
        liab.put("OtherLiabilities", safeInt(al.otherLiabilities));
        int totalLiab = safeInt(al.securedLoans) + safeInt(al.unsecuredLoans) + safeInt(al.otherLiabilities);
        liab.put("TotalLiabilities", totalLiab);
        alNode.set("Liabilities", liab);

        return alNode;
    }
```

### 1.14 Verification block and Bank Account

```java
    private ObjectNode buildVerification(Itr2FormData data) {
        ObjectNode v = mapper.createObjectNode();
        v.put("Capacity",        "S"); // S = Self
        v.put("AssesseeVerName", nvl(data.getFirstName() + " " + data.getSurName()));
        v.put("FatherName",      nvl(data.getFatherName()));
        v.put("Place",           nvl(data.getPlaceOfFiling()));
        v.put("Date",            dateFormatter.todayDDMMYYYY());
        v.put("Declaration",
            "I solemnly declare that the information given in this return and the schedules " +
            "thereto is correct and complete and that the amount of total income and other " +
            "particulars shown therein are truly stated.");
        return v;
    }

    private ObjectNode buildBankAccount(Itr2FormData data) {
        ObjectNode bank = mapper.createObjectNode();
        bank.put("IFSCCode",     nvl(data.getBankIFSC()));
        bank.put("BankName",     nvl(data.getBankName()));
        bank.put("AccountNo",    nvl(data.getBankAccountNumber()));
        bank.put("AccountType",  nvl(data.getBankAccountType())); // SB/CA/CC
        bank.put("IsPreValidated", "Y");
        return bank;
    }
```

### 1.15 Assemble Form_ITR2

```java
    private ObjectNode buildFormITR2(Itr2FormData data) {
        ObjectNode form = mapper.createObjectNode();

        form.set("PersonalInfo",    buildPersonalInfo(data));
        form.set("FilingStatus",    buildFilingStatus(data));
        form.set("ScheduleS",       buildScheduleS(data));
        form.set("ScheduleHP",      buildScheduleHP(data));
        form.set("ScheduleCG",      buildScheduleCG(data));

        // Schedule VDA only if VDA income exists
        if (data.getVdaIncome() > 0 || (data.getVdaTransactions() != null && !data.getVdaTransactions().isEmpty())) {
            form.set("ScheduleVDA", buildScheduleVDA(data));
        }

        // Schedule CYLA/BFLA only if losses exist
        if (data.getScheduleCYLA() != null) form.set("ScheduleCYLA", buildScheduleCYLA(data));
        if (data.getScheduleBFLA() != null) form.set("ScheduleBFLA", buildScheduleBFLA(data));
        if (data.getScheduleCFL()  != null) form.set("ScheduleCFL",  buildScheduleCFL(data));

        form.set("ScheduleOS",      buildScheduleOS(data));   // Other Sources
        form.set("ScheduleVIA",     buildScheduleVIA(data));
        form.set("Schedule80G",     buildSchedule80G(data));

        // Schedule AL: mandatory only if total income > Rs 50L
        if (data.getTotalIncome() > 5000000 || data.getScheduleAL() != null) {
            form.set("ScheduleAL",  buildScheduleAL(data));
        }

        // Special rate income schedule
        form.set("ScheduleSI",      buildScheduleSI(data));

        // Exempt income
        form.set("ScheduleEI",      buildScheduleEI(data));

        // AMT only if applicable
        if (data.isAmtApplicable()) form.set("ScheduleAMT", buildScheduleAMT(data));

        form.set("TaxComputation",  buildTaxComputation(data));
        form.set("TaxPaid",         buildTaxPaid(data));
        form.set("Verification",    buildVerification(data));
        form.set("BankAccountDetail", buildBankAccount(data));

        return form;
    }
```

### 1.16 Remaining schedule builders (ScheduleOS, ScheduleSI, ScheduleEI, ScheduleAMT, CYLA, BFLA, CFL)

```java
    // Other Sources — interest, dividends, family pension
    private ObjectNode buildScheduleOS(Itr2FormData data) {
        ObjectNode os = mapper.createObjectNode();
        os.put("InterestFromSavingsBankAcc",    safeInt(data.getInterestSavingsBank()));
        os.put("InterestFromDeposits",          safeInt(data.getInterestFD()));
        os.put("InterestFromITRefund",          safeInt(data.getInterestITRefund()));
        os.put("DividendGross",                 safeInt(data.getDividendIncome()));
        os.put("FamilyPension",                 safeInt(data.getFamilyPension()));
        int familyPensionDed = data.getTaxRegime() == TaxRegime.OLD
            ? Math.min(15000, (int) Math.round(safeInt(data.getFamilyPension()) / 3.0))
            : Math.min(25000, (int) Math.round(safeInt(data.getFamilyPension()) / 3.0));
        os.put("FamilyPensionDedUs57iia",       familyPensionDed);
        os.put("OtherIncomeTotal",              safeInt(data.getOtherSourcesIncomeTotal()));
        return os;
    }

    // Special rate income — tax at flat rates
    private ObjectNode buildScheduleSI(Itr2FormData data) {
        ObjectNode si = mapper.createObjectNode();
        ScheduleCGData cg = data.getScheduleCG();
        // STCG 111A split
        si.put("TaxableSTCG_111A_15Pct",  cg != null ? safeInt(cg.stcg111A_preJul23) : 0);
        si.put("TaxableSTCG_111A_20Pct",  cg != null ? safeInt(cg.stcg111A_postJul23) : 0);
        // LTCG 112A (net of exemptions)
        int netLTCGPre  = cg != null ? Math.max(0, safeInt(cg.ltcg112A_preJul23)  - 100000) : 0;
        int netLTCGPost = cg != null ? Math.max(0, safeInt(cg.ltcg112A_postJul23) - 125000) : 0;
        si.put("TaxableLTCG_112A_10Pct",  netLTCGPre);
        si.put("TaxableLTCG_112A_12_5Pct",netLTCGPost);
        // VDA
        si.put("TaxableVDA_115BBH_30Pct", safeInt(data.getVdaIncome()));
        // Lottery
        si.put("TaxableLottery_115BB_30Pct", safeInt(data.getLotteryIncome()));

        // Tax on each — note: no 87A rebate on special rate income
        int taxOnSpecial =
            (int) Math.round(0.15 * netLTCGPre  == 0 ? 0 : safeInt(cg.stcg111A_preJul23)  * 0.15) +
            (int) Math.round(safeInt(cg != null ? cg.stcg111A_postJul23 : 0) * 0.20) +
            (int) Math.round(netLTCGPre  * 0.10) +
            (int) Math.round(netLTCGPost * 0.125) +
            (int) Math.round(safeInt(data.getVdaIncome())     * 0.30) +
            (int) Math.round(safeInt(data.getLotteryIncome()) * 0.30);
        si.put("TotalTaxOnSpecialRateIncome", taxOnSpecial);
        return si;
    }

    private ObjectNode buildScheduleEI(Itr2FormData data) {
        ObjectNode ei = mapper.createObjectNode();
        ei.put("AgricultureIncome",    safeInt(data.getAgricultureIncome()));
        ei.put("ExemptPPFInterest",    safeInt(data.getPpfInterest()));
        ei.put("ExemptLICMaturity",    safeInt(data.getLicMaturityExempt()));
        ei.put("ExemptGratuity",       safeInt(data.getGratuityExemption()));
        ei.put("ExemptLeaveEncashment",safeInt(data.getLeaveEncashmentExemption()));
        ei.put("ShareInFirmProfit",    safeInt(data.getShareInFirmProfit()));
        ei.put("TotalExemptIncome",    safeInt(data.getTotalExemptIncome()));
        return ei;
    }

    private ObjectNode buildScheduleAMT(Itr2FormData data) {
        ObjectNode amt = mapper.createObjectNode();
        amt.put("AdjustedTotalIncome",  safeInt(data.getAdjustedTotalIncomeForAMT()));
        amt.put("AMTTaxAt18_5Pct",      safeInt(data.getAmtTax()));
        amt.put("RegularIncomeTax",     safeInt(data.getRegularTaxBeforeCess()));
        amt.put("AMTApplicable",        data.isAmtApplicable() ? "Y" : "N");
        amt.put("AMTCreditBF",          safeInt(data.getAmtCreditBroughtForward()));
        amt.put("AMTCreditUtilized",    safeInt(data.getAmtCreditUtilized()));
        amt.put("AMTCreditCF",          safeInt(data.getAmtCreditCarriedForward()));
        return amt;
    }

    private ObjectNode buildScheduleCYLA(Itr2FormData data) {
        ScheduleCYLAData cy = data.getScheduleCYLA();
        ObjectNode n = mapper.createObjectNode();
        if (cy == null) return n;
        n.put("HPLossAvailable",             safeInt(cy.hpLossAvailable));
        n.put("HPLossSetOffAgainstSalary",   safeInt(cy.hpLossSetOffAgainstSalary));
        n.put("HPLossSetOffAgainstOS",       safeInt(cy.hpLossSetOffAgainstOS));
        n.put("HPLossSetOffAgainstCG",       safeInt(cy.hpLossSetOffAgainstCG));
        n.put("HPLossUnabsorbed",            safeInt(cy.hpLossUnabsorbed));
        n.put("BusinessNSLossAvail",         safeInt(cy.businessNSLossAvail));
        n.put("BusinessNSLossSetOff",        safeInt(cy.businessNSLossSetOff));
        n.put("SpeculativeLossAvail",        safeInt(cy.speculativeLossAvail));
        n.put("SpeculativeLossSetOff",       safeInt(cy.speculativeLossSetOff));
        n.put("STCGLossAvail",               safeInt(cy.stcgLossAvail));
        n.put("LTCGLossAvail",               safeInt(cy.ltcgLossAvail));
        n.put("LTCGLossSetOffAgainstLTCG",   safeInt(cy.ltcgLossSetOffAgainstLTCG));
        n.put("VDALoss",                     0); // Always 0 — cannot be set off
        return n;
    }

    private ObjectNode buildScheduleBFLA(Itr2FormData data) {
        ScheduleBFLAData bf = data.getScheduleBFLA();
        ObjectNode n = mapper.createObjectNode();
        if (bf == null) return n;
        n.put("TotalBFLossSetOff", safeInt(bf.totalBFLossSetOff));
        return n;
    }

    private ObjectNode buildScheduleCFL(Itr2FormData data) {
        ScheduleCFLData cfl = data.getScheduleCFL();
        ObjectNode n = mapper.createObjectNode();
        if (cfl == null) return n;
        n.put("HPLossCarriedForward",          safeInt(cfl.hpLossToCarryForward));
        n.put("BusinessLossCarriedForward",    safeInt(cfl.businessLossToCarryForward));
        n.put("SpeculativeLossCarriedForward", safeInt(cfl.speculativeLossToCarryForward));
        n.put("STCGLossCarriedForward",        safeInt(cfl.stcgLossToCarryForward));
        n.put("LTCGLossCarriedForward",        safeInt(cfl.ltcgLossToCarryForward));
        n.put("UnabsorbedDepCarriedForward",   safeInt(cfl.unabsorbedDepToCarryForward));
        return n;
    }

    // ── Utility methods ───────────────────────────────────────────────────────
    /** Convert null to 0 safely */
    private int safeInt(Integer val) { return val == null ? 0 : val; }
    /** Convert null to empty string — ITD rejects null text fields */
    private String nvl(String s) { return s == null ? "" : s.replaceAll("[~@#$%^&*()_+{}|:<>?]", ""); }
} // end ITR2JSONExportService
```

---

## SECTION 2 — `ITR3JSONExportService.java` (CREATE FROM SCRATCH)

ITR-3 inherits everything from ITR-2 and adds business income schedules. Do not duplicate — compose.

```java
@Service
public class ITR3JSONExportService {

    private final ITR2JSONExportService itr2Exporter;
    private final DepreciationEngine depreciationEngine;
    private final SHA256DigestUtil digestUtil;
    private final ITDDateFormatter dateFormatter;
    private final ObjectMapper mapper = new ObjectMapper();

    // constructor injection of all above

    public String export(Itr3FormData data) {
        try {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode itr  = mapper.createObjectNode();
            ObjectNode itr3 = mapper.createObjectNode();

            itr3.set("CreationInfo", buildCreationInfo(data));
            itr3.set("Form_ITR3",   buildFormITR3(data));

            itr.set("ITR3", itr3);
            root.set("ITR", itr);

            String jsonWithoutDigest = mapper.writeValueAsString(root);
            String digest = digestUtil.computeSHA256(jsonWithoutDigest);
            ((ObjectNode) root.path("ITR").path("ITR3").path("CreationInfo"))
                .put("Digest", digest);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("ITR-3 JSON export failed: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildFormITR3(Itr3FormData data) {
        ObjectNode form = mapper.createObjectNode();

        // All common sections — delegate to ITR-2 builder methods via shared helper
        // ITR3FormData must extend or wrap ITR2FormData fields for personal, salary, HP, CG, VDA, CYLA, VIA
        form.set("PersonalInfo",    itr2Exporter.buildPersonalInfoFromCommon(data.toCommonData()));
        form.set("FilingStatus",    itr2Exporter.buildFilingStatusFromCommon(data.toCommonData()));
        form.set("ScheduleHP",      itr2Exporter.buildScheduleHPFromList(data.getHouseProperties(), data.getTaxRegime()));
        form.set("ScheduleCG",      itr2Exporter.buildScheduleCGFromData(data.getScheduleCG()));
        if (data.getVdaIncome() > 0) form.set("ScheduleVDA", itr2Exporter.buildScheduleVDAFromList(data.getVdaTransactions(), data.getVdaIncome()));
        form.set("ScheduleVIA",     itr2Exporter.buildScheduleVIAFromCommon(data.toCommonData()));
        form.set("ScheduleOS",      itr2Exporter.buildScheduleOSFromCommon(data.toCommonData()));

        // ITR-3-specific schedules
        form.set("ScheduleBP",      buildScheduleBP(data));
        form.set("ScheduleDPM",     buildScheduleDPM(data));
        form.set("ScheduleDCG",     buildScheduleDCG(data));
        form.set("ScheduleGST",     buildScheduleGST(data));
        form.set("BalanceSheet",    buildBalanceSheet(data));
        form.set("ProfitAndLoss",   buildProfitAndLoss(data));

        if (data.getScheduleCYLA() != null) form.set("ScheduleCYLA", itr2Exporter.buildScheduleCYLAFromData(data.getScheduleCYLA()));
        if (data.getScheduleBFLA() != null) form.set("ScheduleBFLA", itr2Exporter.buildScheduleBFLAFromData(data.getScheduleBFLA()));
        if (data.getScheduleCFL()  != null) form.set("ScheduleCFL",  itr2Exporter.buildScheduleCFLFromData(data.getScheduleCFL()));

        if (data.getTotalIncome() > 5000000) form.set("ScheduleAL",  itr2Exporter.buildScheduleALFromData(data.getScheduleAL()));

        form.set("TaxComputation",  itr2Exporter.buildTaxComputationFromCommon(data.toCommonData()));
        form.set("TaxPaid",         itr2Exporter.buildTaxPaidFromCommon(data.toCommonData()));
        form.set("Verification",    itr2Exporter.buildVerificationFromCommon(data.toCommonData()));
        form.set("BankAccountDetail", itr2Exporter.buildBankAccountFromCommon(data.toCommonData()));

        return form;
    }

    // ── Schedule BP ───────────────────────────────────────────────────────────
    private ObjectNode buildScheduleBP(Itr3FormData data) {
        ScheduleBPData bp = data.getScheduleBP();
        ObjectNode n = mapper.createObjectNode();
        if (bp == null) return n;

        n.put("NetProfitFromPL",              safeInt(bp.netProfitFromPL));

        // Add-backs
        ObjectNode addBack = mapper.createObjectNode();
        addBack.put("PersonalExpenses",       safeInt(bp.debitPersonalExpenses));
        addBack.put("CapitalExpenditure",     safeInt(bp.debitCapitalExpenditure));
        addBack.put("Disallowance40A3Cash",   safeInt(bp.disallowance40A3_cash));
        addBack.put("Disallowance36_1_va",    safeInt(bp.disallowance36_1_va_epf));
        addBack.put("Disallowance43B",        safeInt(bp.disallowance43B));
        addBack.put("Disallowance43Bh_MSME",  safeInt(bp.disallowance43Bh_msme));
        addBack.put("Disallowance14A",        safeInt(bp.disallowance14A));
        addBack.put("ExcessBookDepreciation", safeInt(bp.excessBookDepreciation));
        addBack.put("TotalAddBack",           safeInt(bp.totalAddBack));
        n.set("AddAdmissibleDebits", addBack);

        // Deductions
        ObjectNode deduct = mapper.createObjectNode();
        deduct.put("DividendIncome",          safeInt(bp.dividendIncome));
        deduct.put("RentalIncome",            safeInt(bp.rentalIncome));
        deduct.put("ITActDepreciation",       safeInt(bp.itActDepreciation));
        deduct.put("AdditionalDepreciation",  safeInt(bp.additionalDepreciation));
        deduct.put("Deduction35AD",           safeInt(bp.deduction35AD));
        deduct.put("OtherPermissible",        safeInt(bp.otherPermissibleDeductions));
        deduct.put("TotalDeductions",         safeInt(bp.totalDeductions));
        n.set("LessAdmissibleCredits", deduct);

        n.put("ProfitFromBusiness",           safeInt(bp.profitFromBusiness));
        n.put("SpeculativeBusinessIncome",    safeInt(bp.speculativeBusinessIncome));
        n.put("TotalBusinessIncome",          safeInt(bp.totalBusinessIncome));
        n.put("BookProfit",                   safeInt(bp.bookProfit));
        n.put("PartnerRemunerationClaimed",   safeInt(bp.partnerRemunerationClaimed));

        return n;
    }

    // ── Schedule DPM ──────────────────────────────────────────────────────────
    private ObjectNode buildScheduleDPM(Itr3FormData data) {
        ScheduleDPMData dpm = data.getScheduleDPM();
        ObjectNode n = mapper.createObjectNode();
        if (dpm == null) return n;

        // Recompute actual depreciation using DepreciationEngine before export
        int computedTotal = depreciationEngine.computeTotal(dpm);

        addDepBlock(n, "Buildings_5pct",   dpm.buildings_5pct);
        addDepBlock(n, "Buildings_10pct",  dpm.buildings_10pct);
        addDepBlock(n, "Buildings_40pct",  dpm.buildings_40pct);
        addDepBlock(n, "Furniture_10pct",  dpm.furniture_10pct);
        addDepBlock(n, "PM_15pct",         dpm.pm_15pct);
        addDepBlock(n, "Computers_40pct",  dpm.computers_40pct);
        addDepBlock(n, "Vehicles_15pct",   dpm.vehicles_15pct);
        addDepBlock(n, "Intangibles_25pct",dpm.intangibles_25pct);
        n.put("TotalDepreciation",         computedTotal);
        n.put("STCGOnBlocks",              safeInt(dpm.stcgOnBlocks));
        return n;
    }

    private void addDepBlock(ObjectNode parent, String key, ScheduleDPMData.DepreciationBlock block) {
        if (block == null) return;
        ObjectNode b = mapper.createObjectNode();
        b.put("OpeningWDV",             safeInt(block.openingWDV));
        b.put("Additions",              safeInt(block.additions));
        b.put("AdditionsAfterOct1",     safeInt(block.additionsAfterOct1));
        b.put("Disposals",              safeInt(block.disposals));
        b.put("ClosingWDV",             safeInt(block.closingWDV));
        b.put("Depreciation",           safeInt(block.depreciation));
        b.put("AdditionalDepreciation", safeInt(block.additionalDepreciation));
        b.put("STCGOnBlock",            safeInt(block.stcgOnBlock));
        parent.set(key, b);
    }

    // ── Schedule DCG ──────────────────────────────────────────────────────────
    private ObjectNode buildScheduleDCG(Itr3FormData data) {
        ScheduleDCGData dcg = data.getScheduleDCG();
        ObjectNode n = mapper.createObjectNode();
        if (dcg == null) return n;

        n.put("Row1a", safeInt(dcg.row1a));
        n.put("Row1b", safeInt(dcg.row1b));
        n.put("Row1c", safeInt(dcg.row1c));
        n.put("Row1d", safeInt(dcg.row1d));
        n.put("Row1Total", safeInt(dcg.row1a) + safeInt(dcg.row1b) + safeInt(dcg.row1c) + safeInt(dcg.row1d));
        n.put("Row2a", safeInt(dcg.row2a));
        n.put("Row2b", safeInt(dcg.row2b));
        n.put("Row2c", safeInt(dcg.row2c));
        n.put("Row2d", safeInt(dcg.row2a) + safeInt(dcg.row2b) + safeInt(dcg.row2c));
        int dcgTotal = Math.max(0, (safeInt(dcg.row2a)+safeInt(dcg.row2b)+safeInt(dcg.row2c)) -
            (safeInt(dcg.row1a)+safeInt(dcg.row1b)+safeInt(dcg.row1c)+safeInt(dcg.row1d)));
        n.put("TotalDeemedCapitalGains", dcgTotal);
        return n;
    }

    // ── Schedule GST ──────────────────────────────────────────────────────────
    private ObjectNode buildScheduleGST(Itr3FormData data) {
        ObjectNode n = mapper.createObjectNode();
        ArrayNode list = mapper.createArrayNode();
        if (data.getGstRegistrations() != null) {
            for (GSTRegistration g : data.getGstRegistrations()) {
                ObjectNode e = mapper.createObjectNode();
                e.put("GSTIN",              nvl(g.gstin));
                e.put("NameOfBusiness",     nvl(g.businessName));
                e.put("TurnoverGSTR1",      safeInt(g.turnoverGSTR1));
                e.put("TurnoverGSTR3B",     safeInt(g.turnoverGSTR3B));
                e.put("TurnoverBooks",      safeInt(g.turnoverBooks));
                e.put("DifferenceGSTvsBooks", Math.abs(safeInt(g.turnoverGSTR1) - safeInt(g.turnoverBooks)));
                e.put("ReasonForDifference",nvl(g.reasonForDifference));
                list.add(e);
            }
        }
        n.set("GSTINList", list);
        return n;
    }

    private ObjectNode buildBalanceSheet(Itr3FormData data) {
        BalanceSheetData bs = data.getBalanceSheet();
        ObjectNode n = mapper.createObjectNode();
        if (bs == null) return n;
        n.put("GrossFixedAssets",  safeInt(bs.fixedAssetsGross));
        n.put("AccumDepreciation", safeInt(bs.accumulatedDepreciation));
        n.put("NetFixedAssets",    safeInt(bs.fixedAssetsNet));
        n.put("SundryDebtors",     safeInt(bs.sundryDebtors));
        n.put("CashBank",          safeInt(bs.cashAndBankBalance));
        n.put("StockInTrade",      safeInt(bs.stockInTrade));
        n.put("LoansAdvances",     safeInt(bs.loansAndAdvances));
        n.put("TotalAssets",       safeInt(bs.totalAssets));
        n.put("CapitalAccount",    safeInt(bs.capitalAccount));
        n.put("SecuredLoans",      safeInt(bs.securedLoans));
        n.put("UnsecuredLoans",    safeInt(bs.unsecuredLoans));
        n.put("SundryCreditors",   safeInt(bs.sundryCreditors));
        n.put("OtherLiabilities",  safeInt(bs.otherLiabilities));
        n.put("TotalLiabilities",  safeInt(bs.totalLiabilities));
        return n;
    }

    private ObjectNode buildProfitAndLoss(Itr3FormData data) {
        ProfitAndLossData pl = data.getProfitAndLoss();
        ObjectNode n = mapper.createObjectNode();
        if (pl == null) return n;
        n.put("GrossReceipts",     safeInt(pl.grossReceipts));
        n.put("OpeningStock",      safeInt(pl.openingStock));
        n.put("Purchases",         safeInt(pl.purchases));
        n.put("DirectExpenses",    safeInt(pl.directExpenses));
        n.put("ClosingStock",      safeInt(pl.closingStock));
        n.put("GrossProfit",       safeInt(pl.grossProfit));
        n.put("OtherIncome",       safeInt(pl.otherIncome));
        n.put("SalariesWages",     safeInt(pl.salariesAndWages));
        n.put("Depreciation",      safeInt(pl.depreciation));
        n.put("OtherExpenses",     safeInt(pl.otherExpenses));
        n.put("NetProfit",         safeInt(pl.netProfitAsPerPL));
        return n;
    }

    private ObjectNode buildCreationInfo(Itr3FormData data) {
        ObjectNode ci = mapper.createObjectNode();
        ci.put("SWVersionNo",     "1.0");
        ci.put("SWCreatedBy",     "ITR_ERP_v1");
        ci.put("JSONCreatedDate", dateFormatter.todayDDMMYYYY());
        ci.put("InterfaceType",   "JSON");
        String city = data.getIntermediaryCity() != null ? data.getIntermediaryCity() : "SYSTEM";
        ci.put("IntermediaryCity", city.length() > 25 ? city.substring(0, 25) : city);
        ci.put("Digest", "");
        return ci;
    }

    private int safeInt(Integer v) { return v == null ? 0 : v; }
    private String nvl(String s) { return s == null ? "" : s.replaceAll("[~@#$%^&*()_+{}|:<>?]", ""); }
}
```

**IMPORTANT — package-private builder methods in ITR2JSONExportService:**
The ITR3 exporter calls methods like `itr2Exporter.buildPersonalInfoFromCommon(...)`. To make this work, you must change the private methods in `ITR2JSONExportService` that are reused by ITR-3 to **package-private** (remove the `private` keyword). These are:
- `buildPersonalInfoFromCommon`, `buildFilingStatusFromCommon`, `buildScheduleHPFromList`, `buildScheduleCGFromData`, `buildScheduleVDAFromList`, `buildScheduleVIAFromCommon`, `buildScheduleOSFromCommon`, `buildScheduleCYLAFromData`, `buildScheduleBFLAFromData`, `buildScheduleCFLFromData`, `buildScheduleALFromData`, `buildTaxComputationFromCommon`, `buildTaxPaidFromCommon`, `buildVerificationFromCommon`, `buildBankAccountFromCommon`

Each of these must accept a generic interface or common data object. The simplest approach: create an interface `CommonITRData` that `Itr2FormData`, `Itr3FormData`, and `Itr4FormData` all implement, and change the method signatures accordingly.

---

## SECTION 3 — `ITR4JSONExportService.java` (CREATE FROM SCRATCH)

Simpler than ITR-2/3. Presumptive income only. No Capital Gains (except LTCG 112A ≤ 1.25L). No balance sheet beyond the simplified version.

```java
@Service
public class ITR4JSONExportService {

    private final ITR2JSONExportService itr2Exporter;
    private final SHA256DigestUtil digestUtil;
    private final ITDDateFormatter dateFormatter;
    private final ObjectMapper mapper = new ObjectMapper();

    public String export(Itr4FormData data) {
        try {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode itr  = mapper.createObjectNode();
            ObjectNode itr4 = mapper.createObjectNode();

            itr4.set("CreationInfo", buildCreationInfo(data));
            itr4.set("Form_ITR4",   buildFormITR4(data));

            itr.set("ITR4", itr4);
            root.set("ITR", itr);

            String jsonWithoutDigest = mapper.writeValueAsString(root);
            String digest = digestUtil.computeSHA256(jsonWithoutDigest);
            ((ObjectNode) root.path("ITR").path("ITR4").path("CreationInfo"))
                .put("Digest", digest);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("ITR-4 JSON export failed: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildFormITR4(Itr4FormData data) {
        ObjectNode form = mapper.createObjectNode();

        form.set("PersonalInfo",    itr2Exporter.buildPersonalInfoFromCommon(data.toCommonData()));
        form.set("FilingStatus",    itr2Exporter.buildFilingStatusFromCommon(data.toCommonData()));

        // Presumptive business income
        form.set("BusinessProfessionIncome", buildPresumptiveIncome(data));

        // Single HP (max 1 allowed in ITR-4)
        if (data.getHouseProperty() != null) {
            ObjectNode hp = mapper.createObjectNode();
            ArrayNode props = mapper.createArrayNode();
            props.add(buildSinglePropertyNode(data.getHouseProperty(), data.getTaxRegime()));
            hp.set("PropertyDetails", props);
            form.set("ScheduleHP", hp);
        }

        // Other Sources
        form.set("ScheduleOS",  itr2Exporter.buildScheduleOSFromCommon(data.toCommonData()));

        // Chapter VI-A
        form.set("ScheduleVIA", itr2Exporter.buildScheduleVIAFromCommon(data.toCommonData()));

        // LTCG 112A only if <= Rs 1,25,000
        if (data.getLtcg112A() > 0 && data.getLtcg112A() <= 125000) {
            ObjectNode cg = mapper.createObjectNode();
            cg.put("LTCG112A_Amount",          safeInt(data.getLtcg112A()));
            cg.put("LTCG112A_Exemption",       safeInt(Math.min(data.getLtcg112A(), 125000)));
            cg.put("LTCG112A_Taxable",         Math.max(0, safeInt(data.getLtcg112A()) - 125000));
            form.set("ScheduleCG_LTCG112A", cg);
        }

        // Simplified Balance Sheet (MANDATORY for ITR-4)
        form.set("SimplifiedBalanceSheet", buildSimplifiedBS(data));

        // Schedule AL if income > 50L
        if (data.getTotalIncome() > 5000000 && data.getScheduleAL() != null) {
            form.set("ScheduleAL", itr2Exporter.buildScheduleALFromData(data.getScheduleAL()));
        }

        // GST reconciliation if GSTIN exists
        if (data.getGstRegistrations() != null && !data.getGstRegistrations().isEmpty()) {
            form.set("ScheduleGST", buildGSTSchedule(data));
        }

        form.set("TaxComputation",  itr2Exporter.buildTaxComputationFromCommon(data.toCommonData()));
        form.set("TaxPaid",         itr2Exporter.buildTaxPaidFromCommon(data.toCommonData()));
        form.set("Verification",    itr2Exporter.buildVerificationFromCommon(data.toCommonData()));
        form.set("BankAccountDetail", itr2Exporter.buildBankAccountFromCommon(data.toCommonData()));

        return form;
    }

    private ObjectNode buildPresumptiveIncome(Itr4FormData data) {
        ObjectNode bp = mapper.createObjectNode();

        if (data.getSec44AD() != null) {
            Sec44ADData ad = data.getSec44AD();
            ObjectNode n44ad = mapper.createObjectNode();
            n44ad.put("GrossTurnover",        safeInt(ad.grossTurnover));
            n44ad.put("TurnoverCash",         safeInt(ad.turnoverCash));
            n44ad.put("TurnoverDigital",      safeInt(ad.turnoverDigital));
            // Compute minimum income — system enforces; taxpayer can declare higher
            int minCash    = (int) Math.round(0.08 * safeInt(ad.turnoverCash));
            int minDigital = (int) Math.round(0.06 * safeInt(ad.turnoverDigital));
            n44ad.put("GrossIncomeCash_8Pct",    minCash);
            n44ad.put("GrossIncomeDigital_6Pct",  minDigital);
            // Use declared income if >= minimum; otherwise floor to minimum
            int declared = safeInt(ad.totalPresumptiveIncome);
            n44ad.put("TotalPresumptiveIncome44AD", Math.max(declared, minCash + minDigital));
            n44ad.put("BusinessCode",         nvl(ad.businessCode));
            bp.set("Sec44AD", n44ad);
        }

        if (data.getSec44ADA() != null) {
            Sec44ADAData ada = data.getSec44ADA();
            ObjectNode n44ada = mapper.createObjectNode();
            n44ada.put("GrossProfessionalReceipts", safeInt(ada.grossReceipts));
            n44ada.put("ReceiptsCash",              safeInt(ada.receiptsCash));
            n44ada.put("ReceiptsDigital",           safeInt(ada.receiptsDigital));
            int min50 = (int) Math.round(0.50 * safeInt(ada.grossReceipts));
            n44ada.put("PresumptiveIncome50Pct", Math.max(safeInt(ada.presumptiveIncome), min50));
            n44ada.put("ProfessionCode",         nvl(ada.professionCode));
            bp.set("Sec44ADA", n44ada);
        }

        if (data.getSec44AE() != null) {
            Sec44AEData ae = data.getSec44AE();
            ObjectNode n44ae = mapper.createObjectNode();
            ArrayNode vehicles = mapper.createArrayNode();
            int totalAE = 0;
            for (VehicleDetail v : ae.vehicles) {
                ObjectNode vn = mapper.createObjectNode();
                vn.put("RegistrationNumber",   nvl(v.registrationNumber));
                vn.put("IsHeavyVehicle",       v.isHeavyVehicle ? "Y" : "N");
                vn.put("GVW_Tonnes",           v.gvwInTonnes);
                vn.put("MonthsOwned",          v.monthsOwned);
                int minIncome = v.isHeavyVehicle && v.gvwInTonnes > 12
                    ? (int) Math.round(1000 * v.gvwInTonnes) * v.monthsOwned
                    : 7500 * v.monthsOwned;
                int vehicleIncome = Math.max(safeInt(v.declaredIncome), minIncome);
                vn.put("PresumptiveIncomePerVehicle", vehicleIncome);
                vehicles.add(vn);
                totalAE += vehicleIncome;
            }
            n44ae.set("VehicleDetails", vehicles);
            n44ae.put("TotalPresumptiveIncome44AE", totalAE);
            bp.set("Sec44AE", n44ae);
        }

        // Total across all sections
        int total = 0;
        if (data.getSec44AD()  != null) total += safeInt(data.getSec44AD().totalPresumptiveIncome);
        if (data.getSec44ADA() != null) total += safeInt(data.getSec44ADA().presumptiveIncome);
        if (data.getSec44AE()  != null) total += safeInt(data.getSec44AE().totalPresumptiveIncome);
        bp.put("TotalPresumptiveBusinessIncome", total);

        return bp;
    }

    private ObjectNode buildSimplifiedBS(Itr4FormData data) {
        SimplifiedBalanceSheet bs = data.getSimplifiedBalanceSheet();
        ObjectNode n = mapper.createObjectNode();
        if (bs == null) return n;
        n.put("GrossReceipts",       safeInt(bs.grossReceipts));
        n.put("NetProfit",           safeInt(bs.netProfit));
        n.put("TotalSundryDebtors",  safeInt(bs.sundryDebtors));
        n.put("TotalSundryCreditors",safeInt(bs.sundryCreditors));
        n.put("TotalStockInTrade",   safeInt(bs.stockInTrade));
        n.put("CashBalance",         safeInt(bs.cashBalance));
        n.put("OpeningCapital",      safeInt(bs.openingCapital));
        n.put("Drawings",            safeInt(bs.drawings));
        n.put("AdditionsToCapital",  safeInt(bs.additionsToCapital));
        // Closing capital computed: opening + additions - drawings + net profit
        int closingCap = safeInt(bs.openingCapital) + safeInt(bs.additionsToCapital)
            - safeInt(bs.drawings) + safeInt(bs.netProfit);
        n.put("ClosingCapital",      closingCap);
        n.put("TotalSecuredLoans",   safeInt(bs.securedLoans));
        n.put("TotalUnsecuredLoans", safeInt(bs.unsecuredLoans));
        n.put("TotalFixedAssetsWDV", safeInt(bs.totalFixedAssetsWDV));
        return n;
    }

    private ObjectNode buildGSTSchedule(Itr4FormData data) {
        ObjectNode n = mapper.createObjectNode();
        ArrayNode list = mapper.createArrayNode();
        for (GSTRegistration g : data.getGstRegistrations()) {
            ObjectNode e = mapper.createObjectNode();
            e.put("GSTIN",             nvl(g.gstin));
            e.put("NameOfBusiness",    nvl(g.businessName));
            e.put("TurnoverGSTR1",     safeInt(g.turnoverGSTR1));
            e.put("TurnoverGSTR3B",    safeInt(g.turnoverGSTR3B));
            e.put("TurnoverBooks",     safeInt(g.turnoverBooks));
            list.add(e);
        }
        n.set("GSTINList", list);
        return n;
    }

    private ObjectNode buildSinglePropertyNode(PropertyDetail p, TaxRegime regime) {
        ObjectNode pr = mapper.createObjectNode();
        pr.put("PropertyAddress",       nvl(p.address));
        pr.put("TypeOfHP",              p.type == PropertyType.SELF_OCCUPIED ? "S" : p.type == PropertyType.LET_OUT ? "L" : "D");
        pr.put("GrossRentReceived",     safeInt(p.grossAnnualValue));
        pr.put("TaxPaidLocalAuthority", safeInt(p.municipalTax));
        pr.put("AnnualValue",           safeInt(p.netAnnualValue));
        int stdDed = p.type == PropertyType.SELF_OCCUPIED ? 0 : safeInt(p.standardDeduction);
        pr.put("StandardDeduction30Pct",stdDed);
        int interest = regime == TaxRegime.NEW && p.type == PropertyType.SELF_OCCUPIED ? 0
            : (regime == TaxRegime.OLD && p.type == PropertyType.SELF_OCCUPIED
               ? Math.min(p.interestOnBorrowedCapital, 200000)
               : p.interestOnBorrowedCapital);
        pr.put("InterestPayable",       interest);
        pr.put("IncomeFromHP",          safeInt(p.netAnnualValue) - stdDed - interest + safeInt(p.arrearUnrealisedRent));
        return pr;
    }

    private ObjectNode buildCreationInfo(Itr4FormData data) {
        ObjectNode ci = mapper.createObjectNode();
        ci.put("SWVersionNo",     "1.1");
        ci.put("SWCreatedBy",     "ITR_ERP_v1");
        ci.put("JSONCreatedDate", dateFormatter.todayDDMMYYYY());
        ci.put("InterfaceType",   "JSON");
        String city = data.getIntermediaryCity() != null ? data.getIntermediaryCity() : "SYSTEM";
        ci.put("IntermediaryCity", city.length() > 25 ? city.substring(0, 25) : city);
        ci.put("Digest", "");
        return ci;
    }

    private int safeInt(Integer v) { return v == null ? 0 : v; }
    private String nvl(String s) { return s == null ? "" : s.replaceAll("[~@#$%^&*()_+{}|:<>?]", ""); }
}
```

---

## SECTION 4 — COMPLETE `DepreciationEngine.computeTotal()`

The existing `DepreciationEngine` validates depreciation fields but Phase 5 built it without the actual computation method. Add this method to the existing class:

```java
// ADD to existing DepreciationEngine.java

/**
 * Recomputes total IT Act depreciation from all blocks per Section 32.
 * Called by ITR3JSONExportService and ITR3CBDTValidationService.
 * Uses block-of-assets method; half-rate rule for assets added after Oct 1.
 */
public int computeTotal(ScheduleDPMData dpm) {
    if (dpm == null) return 0;

    int total = 0;
    total += computeBlock(dpm.buildings_5pct);
    total += computeBlock(dpm.buildings_10pct);
    total += computeBlock(dpm.buildings_40pct);
    total += computeBlock(dpm.furniture_10pct);
    total += computeBlock(dpm.pm_15pct);
    total += computeBlock(dpm.computers_40pct);
    total += computeBlock(dpm.vehicles_15pct);
    // Intangibles: 25% rate; 0% if goodwill present
    if (dpm.intangibles_25pct != null && !dpm.intangibles_25pct.hasGoodwill) {
        total += computeBlock(dpm.intangibles_25pct);
    }
    return total;
}

private int computeBlock(ScheduleDPMData.DepreciationBlock block) {
    if (block == null) return 0;

    int opening  = block.openingWDV;
    int additions = block.additions;
    int addAfterOct1 = block.additionsAfterOct1; // half-rate applies
    int disposals = block.disposals;

    // Closing WDV before depreciation
    int closingWDV = opening + additions - disposals;

    if (closingWDV <= 0) {
        // Block entirely wiped by disposals — Short-Term Capital Gain
        block.stcgOnBlock = Math.abs(closingWDV);
        block.closingWDV  = 0;
        block.depreciation = 0;
        return 0;
    }

    block.stcgOnBlock = 0;
    block.closingWDV  = closingWDV;

    // Separate additions added before and after Oct 1
    int addBeforeOct1 = additions - addAfterOct1;

    // Full-rate base: opening WDV + additions before Oct 1 - disposals
    int fullRateBase = Math.max(0, opening + addBeforeOct1 - disposals);
    int fullRateDep  = (int) Math.round(fullRateBase * block.rate / 100.0);

    // Half-rate: additions after Oct 1 get 50% of normal rate
    int halfRateDep  = (int) Math.round(addAfterOct1 * block.rate / 200.0);

    int normalDep = fullRateDep + halfRateDep;

    // Additional depreciation u/s 32(1)(iia) — new P&M only
    // Full 20% if used >= 180 days; only 10% if < 180 days
    // additionsAfterOct1 by definition = installed < 180 days (after Oct 1 = < 182 days before Mar 31)
    int addlDep = 0;
    if (block.additionalDepreciation > 0) {
        // Verify: full 20% for additions before Oct 1; only 10% for additions after Oct 1
        int addlFullRate = (int) Math.round(addBeforeOct1 * 0.20);
        int addlHalfRate = (int) Math.round(addAfterOct1 * 0.10); // other 10% in next year
        addlDep = addlFullRate + addlHalfRate;
        block.additionalDepreciation = addlDep; // update with computed value
    }

    block.depreciation = normalDep + addlDep;
    return block.depreciation;
}
```

---

## SECTION 5 — WIRE AY 2026-27 TAX ENGINE INTO ORCHESTRATION

The `AY202627TaxComputationService` was built in Phase 5 but never called by the orchestration service. Open `ITRFilingOrchestrationService.java` and update Step 7:

```java
// REPLACE the existing Step 7 in ITRFilingOrchestrationService.file():

// Step 7: Compute tax — both regimes, correct AY
TaxRegimeComparisonResult regimeResult;
if ("2026-27".equals(ay)) {
    // Use the new AY 2026-27 engine (Rs 12L rebate, 6-slab structure)
    regimeResult = ay2627TaxService.compare(request.toTaxInput());
} else {
    // AY 2025-26 and earlier: use existing regime comparison service
    regimeResult = regimeComparison.compare(request.toTaxInput());
}
auditTrail.logEvent(pan, ay, "TAX_COMPUTED",
    "AY: " + ay + " | Optimal regime: " + regimeResult.getRecommendedRegime() +
    " | Tax: Rs " + regimeResult.getOptimalTax(), "TAX_ENGINE");
```

Also ensure `AY202627TaxComputationService` has a `compare()` method that returns `TaxRegimeComparisonResult`. If it only has `computeNewRegime()` and `computeOldRegime()`, add:

```java
// ADD to AY202627TaxComputationService.java if missing:
public TaxRegimeComparisonResult compare(TaxInput input) {
    TaxResult oldRegime = computeOldRegime(input);  // same slabs as AY 2025-26
    TaxResult newRegime = computeNewRegime(input);  // AY 2026-27 6-slab + Rs 12L rebate
    return TaxRegimeComparisonResult.of(oldRegime, newRegime);
}
```

---

## SECTION 6 — SCHEDULE AL AUTO-POPULATION FROM SFT

Open `SFTProcessingService.java`. The `autoPopulateScheduleAL()` method exists but returns an incomplete object. Replace its body:

```java
// REPLACE body of autoPopulateScheduleAL() in SFTProcessingService.java:

public ScheduleALData autoPopulateScheduleAL(SFTParseResult sft, int totalIncome) {
    if (totalIncome <= 5000000) return null; // Schedule AL not mandatory if income <= 50L

    ScheduleALData al = new ScheduleALData();
    al.requiresVerification = true; // Always flag — SFT values are cost, not current market value

    // Immovable property from SFT-010 (property purchases)
    if (sft.propertyPurchases != null) {
        for (SFTEntry e : sft.propertyPurchases) {
            ScheduleALData.ImmovablePropertyEntry prop = new ScheduleALData.ImmovablePropertyEntry();
            prop.address = "Auto-populated from AIS SFT-010 — verify address";
            prop.costOfAcquisition = e.amount;
            // Extract year from transaction date
            prop.yearOfAcquisition = (e.transactionDate != null && e.transactionDate.length() >= 4)
                ? e.transactionDate.substring(e.transactionDate.length() - 4)
                : String.valueOf(java.time.LocalDate.now().getYear());
            al.immovableProperties.add(prop);
        }
    }

    // Financial assets
    // Shares and debentures from SFT-005 (shares acquired)
    al.sharesDebentures = sft.shareAcquisitions == null ? 0 :
        sft.shareAcquisitions.stream().mapToInt(e -> e.amount).sum();

    // Mutual funds from SFT-007 (MF purchases net of SFT-008 redemptions)
    int mfPurchased = sft.mutualFundPurchases == null ? 0 :
        sft.mutualFundPurchases.stream().mapToInt(e -> e.amount).sum();
    int mfRedeemed = sft.mutualFundRedemptions == null ? 0 :
        sft.mutualFundRedemptions.stream().mapToInt(e -> e.amount).sum();
    al.sharesDebentures += Math.max(0, mfPurchased - mfRedeemed);

    // Bank deposits from SFT-003 (fixed deposits)
    al.bankDeposits = sft.fixedDeposits == null ? 0 :
        sft.fixedDeposits.stream().mapToInt(e -> e.amount).sum();

    // Recompute total assets
    int propCost = al.immovableProperties.stream().mapToInt(p -> p.costOfAcquisition).sum();
    al.totalAssets = propCost + al.sharesDebentures + al.bankDeposits +
        al.jewellery + al.artworkPaintings + al.vehicles + al.bullionGold +
        al.otherMovable + al.insurancePolicies + al.loansGiven +
        al.cashInHandAbove500K + al.otherFinancial;

    return al;
}
```

---

## SECTION 7 — HRA METRO CITY AUTO-DETECTION

Open `HRAComputationService.java`. The metro city set needs to be hardcoded correctly (only 4 cities qualify as metro for HRA). Add/replace the `isMetroCity` logic:

```java
// ADD to HRAComputationService.java (or replace existing if partial):

// ONLY these 4 are "metro" for HRA u/s 10(13A):
// Mumbai (including Bombay / Mumbai Suburban), Delhi (including New Delhi),
// Kolkata (including Calcutta), Chennai (including Madras)
// Bengaluru, Hyderabad, Pune, Ahmedabad etc. are NOT metro → 40% rule
private static final Set<String> METRO_CITY_NAMES = Set.of(
    "MUMBAI", "BOMBAY", "MUMBAI SUBURBAN", "NAVI MUMBAI",
    "DELHI", "NEW DELHI", "NORTH DELHI", "SOUTH DELHI", "EAST DELHI", "WEST DELHI",
    "KOLKATA", "CALCUTTA",
    "CHENNAI", "MADRAS"
);

public boolean isMetroCity(String city) {
    if (city == null || city.isBlank()) return false;
    return METRO_CITY_NAMES.contains(city.trim().toUpperCase());
}

public HRAExemption compute(HRAInput input) {
    boolean isMetro = isMetroCity(input.cityOfResidence);
    double basicDA = input.basicSalary + input.dearessAllowance;

    int component1 = safeInt(input.hraReceived);                              // Actual HRA received
    int component2 = (int) Math.round(basicDA * (isMetro ? 0.50 : 0.40));    // 50%/40% of Basic+DA
    int component3 = Math.max(0, (int) Math.round(input.rentPaid - 0.10 * basicDA)); // Rent - 10% of Basic+DA

    // If taxpayer did not pay rent or rent <= 10% of salary: component3 = 0
    if (input.rentPaid <= 0) component3 = 0;

    int exemption = Math.min(Math.min(component1, component2), component3);

    return HRAExemption.builder()
        .exemptAmount(exemption)
        .taxableHRA(component1 - exemption)
        .isMetroCity(isMetro)
        .component1_hraReceived(component1)
        .component2_percentOfSalary(component2)
        .component3_rentMinus10Pct(component3)
        .metroPercentage(isMetro ? 50 : 40)
        .build();
}

private int safeInt(Integer v) { return v == null ? 0 : v; }
```

---

## SECTION 8 — MULTI-EMPLOYER CROSS-VALIDATION

Open `MultiEmployerConsolidationService.java`. The service aggregates salary but does not cross-validate. Add the following method:

```java
// ADD to MultiEmployerConsolidationService.java:

public MultiEmployerValidationResult crossValidate(List<Form16Data> form16List, String taxpayerPAN) {
    MultiEmployerValidationResult result = new MultiEmployerValidationResult();

    // VAL-ME-001: Each Form 16 must have unique TAN (same TAN = duplicate upload)
    Set<String> seenTANs = new HashSet<>();
    for (Form16Data f16 : form16List) {
        if (!seenTANs.add(f16.getPartA().getTanOfEmployer())) {
            result.addError("VAL-ME-001",
                "Duplicate TAN " + f16.getPartA().getTanOfEmployer() +
                " — same employer Form 16 uploaded twice. Remove the duplicate.");
        }
    }

    // VAL-ME-002: PAN on every Form 16 must match the taxpayer's PAN
    for (Form16Data f16 : form16List) {
        String f16PAN = f16.getPartA().getPanOfEmployee();
        if (!taxpayerPAN.equals(f16PAN)) {
            result.addError("VAL-ME-002",
                "Form 16 from " + f16.getPartA().getEmployerName() +
                " has PAN " + f16PAN + " — does not match taxpayer PAN " + taxpayerPAN +
                ". This Form 16 belongs to a different taxpayer.");
        }
    }

    // VAL-ME-003: Flag overlapping employment periods (same-time employment is legal but unusual)
    for (int i = 0; i < form16List.size(); i++) {
        for (int j = i + 1; j < form16List.size(); j++) {
            if (periodsOverlap(form16List.get(i), form16List.get(j))) {
                result.addWarning("VAL-ME-003",
                    "Overlapping employment periods between " +
                    form16List.get(i).getPartA().getEmployerName() + " and " +
                    form16List.get(j).getPartA().getEmployerName() +
                    ". Verify if this was concurrent employment.");
            }
        }
    }

    // VAL-ME-004: Standard deduction must be claimed ONCE total across all employers
    int totalStdDed = form16List.stream()
        .mapToInt(f -> f.getPartB().getStandardDeductionClaimed())
        .sum();
    int maxAllowed = 75000; // AY 2025-26 new regime; for old regime it's 50000
    // Use whichever is applicable based on regime
    if (totalStdDed > maxAllowed) {
        int excess = totalStdDed - maxAllowed;
        result.addError("VAL-ME-004",
            "Standard deduction overclaimed: Total Rs " + totalStdDed +
            " across " + form16List.size() + " employers. Maximum allowed Rs " + maxAllowed +
            ". Excess Rs " + excess + " will be added back to income.");
        result.setStandardDeductionCorrection(excess);
    }

    // VAL-ME-005: Professional tax aggregate cap = Rs 2,500
    int totalProfTax = form16List.stream()
        .mapToInt(f -> f.getPartB().getProfessionalTaxDeducted())
        .sum();
    if (totalProfTax > 2500) {
        result.addWarning("VAL-ME-005",
            "Professional tax total Rs " + totalProfTax +
            " across all employers exceeds Rs 2,500 cap. " +
            "Will be capped at Rs 2,500 in ITR.");
        result.setProfessionalTaxCap(2500);
    }

    // VAL-ME-006: Gratuity exemption from only ONE employer
    long gratuityClaimCount = form16List.stream()
        .filter(f -> f.getPartB().getGratuityExemptAmount() > 0)
        .count();
    if (gratuityClaimCount > 1) {
        result.addError("VAL-ME-006",
            "Gratuity exemption claimed from " + gratuityClaimCount + " employers. " +
            "Gratuity exemption is available from ONE employer only in a financial year.");
    }

    return result;
}

private boolean periodsOverlap(Form16Data f1, Form16Data f2) {
    java.time.LocalDate start1 = f1.getPartA().getEmploymentFrom();
    java.time.LocalDate end1   = f1.getPartA().getEmploymentTo();
    java.time.LocalDate start2 = f2.getPartA().getEmploymentFrom();
    java.time.LocalDate end2   = f2.getPartA().getEmploymentTo();
    if (start1 == null || end1 == null || start2 == null || end2 == null) return false;
    return !end1.isBefore(start2) && !end2.isBefore(start1);
}
```

---

## SECTION 9 — COMPILE AND VERIFICATION

Run this exact sequence after completing all sections:

```bash
# Step 1: Compile — zero errors required
mvn clean compile -q
echo "Exit code: $?"    # Must be 0

# Step 2: Count compiled files — should be >= 155
find target/classes -name "*.class" | wc -l

# Step 3: Run all tests
mvn test

# Step 4: Start application
mvn spring-boot:run &
sleep 15   # wait for startup

# Step 5: Smoke test ITR-4 (simplest form — validates the full pipeline)
curl -s -X POST http://localhost:8080/api/itr/file \
  -H "Content-Type: application/json" \
  -d '{
    "pan": "TESTPA0001T",
    "assessmentYear": "2025-26",
    "itrForm": "ITR4",
    "sec44AD": {
      "grossTurnover": 5000000,
      "turnoverCash": 500000,
      "turnoverDigital": 4500000,
      "totalPresumptiveIncome": 390000,
      "businessCode": "0101"
    },
    "simplifiedBalanceSheet": {
      "grossReceipts": 5000000,
      "netProfit": 390000,
      "sundryDebtors": 100000,
      "sundryCreditors": 50000,
      "stockInTrade": 0,
      "cashBalance": 25000,
      "openingCapital": 200000,
      "drawings": 150000,
      "additionsToCapital": 0,
      "closingCapital": 440000,
      "securedLoans": 0,
      "unsecuredLoans": 0,
      "totalFixedAssetsWDV": 50000
    },
    "taxRegime": "NEW",
    "bankIFSC": "SBIN0000001",
    "bankAccountNumber": "12345678901",
    "bankAccountType": "SB"
  }' | python3 -m json.tool | grep -E '"itdJson"|"errors"|"success"'
# Expected: "success": true, "errors": [], itdJson contains valid JSON string

# Step 6: Smoke test ITR-2 (more complex)
curl -s -X POST http://localhost:8080/api/itr/file \
  -H "Content-Type: application/json" \
  -d '{
    "pan": "TESTPA0002T",
    "assessmentYear": "2025-26",
    "itrForm": "ITR2",
    "grossSalary": 1200000,
    "netSalary": 1000000,
    "incomeFromSalary": 950000,
    "houseProperties": [],
    "scheduleCG": { "totalSTCG": 0, "totalLTCG": 0, "totalCGIncome": 0 },
    "taxRegime": "NEW",
    "bankIFSC": "HDFC0000001",
    "bankAccountNumber": "98765432101",
    "bankAccountType": "SB"
  }' | python3 -m json.tool | grep -E '"itdJson"|"errors"|"success"'
# Expected: "success": true, "errors": [], itdJson contains {"ITR":{"ITR2":...}}
```

---

## SECTION 10 — CRITICAL NON-NEGOTIABLE RULES

These rules caused failures in prior phases. Violating any one of them will break the ITD upload.

**Rule 1 — ALL monetary amounts in exported JSON must be integers.**
Use `safeInt()` on every field. No `double`, no `.0`, no `null`. The ITD portal rejects JSON with decimal amounts immediately.

**Rule 2 — All dates must be `DD/MM/YYYY` format.**
Use `ITDDateFormatter.format(localDate)` for every date field. Never `toString()` on a `LocalDate` (that produces `YYYY-MM-DD` which ITD rejects).

**Rule 3 — IntermediaryCity in CreationInfo must be <= 25 characters.**
This is the single most common reason ITR-1 uploads fail at the portal. Enforce the `substring(0, 25)` truncation.

**Rule 4 — No special characters in any text field.**
The `nvl()` utility method strips `~ @ # $ % ^ & * ( ) _ + { } | : < > ?`. Apply it to every string field before putting it in JSON.

**Rule 5 — SHA-256 Digest must be computed AFTER the full JSON is assembled, then injected.**
The current approach in ITR-1 (and templated above for ITR-2/3/4) is: build full JSON → compute digest of full JSON string → inject digest into CreationInfo → re-serialize. Do not change this order.

**Rule 6 — VDA income minimum is 0. Never negative.**
`Math.max(0, consideration - cost)` on every VDA transaction. Section 115BBH prohibits loss set-off.

**Rule 7 — New regime: only three Chapter VI-A deductions allowed.**
80CCD(2), 80JJAA, 80CCH(2). All others must be output as `0` in the JSON regardless of what the taxpayer entered. The `buildScheduleVIA()` method already implements this — do not bypass it.

**Rule 8 — F&O turnover = |profit trades| + |loss trades|, never gross contract value.**
This is enforced in validation. The export service should use `fandoDetail.fandoTurnover` which the validation already corrected.

**Rule 9 — ITR-1 must not be modified.**
It is 100% production-ready. No refactoring of ITR-1 for "consistency" is allowed.

**Rule 10 — Never delete a compiling service to fix a compilation error.**
If a service fails to compile because a DTO field is missing, add the field to the DTO. This is the root cause of all prior failures.

---

## FINAL COMPLETION CHECKLIST

Mark ✅ when code is written AND compiles AND has been smoke-tested:

### Section 1 — ITR-2 JSON Export
- [ ] `ITR2JSONExportService.java` created, compiles with 0 errors
- [ ] `buildCreationInfo()` — city truncation at 25 chars ✓
- [ ] `buildPersonalInfo()` — all fields, age → assesseeType mapping ✓
- [ ] `buildFilingStatus()` — regime, 10-IEA, return type ✓
- [ ] `buildScheduleS()` — gross salary, exemptions, standard deduction correct for regime ✓
- [ ] `buildScheduleHP()` — multiple properties, HP loss capping at Rs 2L ✓
- [ ] `buildScheduleCG()` — pre/post July23 split, 112A grandfathering ✓
- [ ] `buildScheduleVDA()` — income floored to 0, 194S TDS summed ✓
- [ ] `buildScheduleVIA()` — new regime deductions blocked ✓
- [ ] `buildSchedule80G()` — cash > Rs 2,000 excluded ✓
- [ ] `buildTaxComputation()` — all 234A/B/C/F fields ✓
- [ ] `buildTaxPaid()` — TDS1, TDS2, TCS, AdvanceTax ✓
- [ ] `buildScheduleAL()` — only when totalIncome > Rs 50L ✓
- [ ] SHA-256 digest computed and injected last ✓
- [ ] All amounts: integers; all dates: DD/MM/YYYY; no special chars ✓
- [ ] Package-private builder methods extracted for ITR-3 reuse ✓

### Section 2 — ITR-3 JSON Export
- [ ] `ITR3JSONExportService.java` created, compiles
- [ ] Reuses ITR-2 builder methods (not duplicated) ✓
- [ ] `buildScheduleBP()` — all add-backs and deductions ✓
- [ ] `buildScheduleDPM()` — calls `DepreciationEngine.computeTotal()` ✓
- [ ] `buildScheduleDCG()` — row arithmetic enforced ✓
- [ ] `buildScheduleGST()` — per GSTIN ✓
- [ ] `buildBalanceSheet()` and `buildProfitAndLoss()` ✓
- [ ] SHA-256 digest computed and injected ✓

### Section 3 — ITR-4 JSON Export
- [ ] `ITR4JSONExportService.java` created, compiles
- [ ] `buildPresumptiveIncome()` — 44AD (8%/6%), 44ADA (50%), 44AE (vehicle formula) ✓
- [ ] `buildSimplifiedBS()` — closing capital computed, not accepted from input ✓
- [ ] GST schedule conditional on GSTIN ✓
- [ ] SHA-256 digest computed and injected ✓

### Section 4 — Depreciation Engine
- [ ] `computeTotal(ScheduleDPMData)` added to `DepreciationEngine` ✓
- [ ] `computeBlock()` implements half-rate rule (additions after Oct 1) ✓
- [ ] Goodwill block returns 0 depreciation ✓
- [ ] STCG on block computed when disposals > (opening + additions) ✓
- [ ] Additional depreciation: 20% full-rate additions; 10% for post-Oct-1 additions ✓

### Section 5 — AY 2026-27 Wiring
- [ ] `ITRFilingOrchestrationService` Step 7 uses `ay2627TaxService` when `ay = "2026-27"` ✓
- [ ] `AY202627TaxComputationService.compare()` method exists and returns `TaxRegimeComparisonResult` ✓

### Section 6 — Schedule AL Auto-population
- [ ] `SFTProcessingService.autoPopulateScheduleAL()` fully implemented ✓
- [ ] SFT-010 → immovable property entries ✓
- [ ] SFT-005/007/003 → financial assets ✓
- [ ] `requiresVerification = true` always set ✓
- [ ] Returns `null` when `totalIncome <= Rs 50L` ✓

### Section 7 — HRA Metro Detection
- [ ] `METRO_CITY_NAMES` set contains exactly the 4 metro city groups ✓
- [ ] Does NOT include Bengaluru, Hyderabad, Pune, Ahmedabad ✓
- [ ] `compute()` uses `isMetroCity()` to select 50%/40% ✓

### Section 8 — Multi-employer Cross-validation
- [ ] `crossValidate()` method added to `MultiEmployerConsolidationService` ✓
- [ ] VAL-ME-001: duplicate TAN detection ✓
- [ ] VAL-ME-002: PAN mismatch detection ✓
- [ ] VAL-ME-004: standard deduction overclaim with correction amount ✓
- [ ] VAL-ME-005: professional tax cap ✓
- [ ] VAL-ME-006: gratuity from multiple employers blocked ✓

### Section 9 — Final Verification
- [ ] `mvn clean compile` → BUILD SUCCESS, exit code 0
- [ ] `mvn test` → all tests pass
- [ ] ITR-4 smoke test: `"success": true`, `"errors": []`, `itdJson` non-null
- [ ] ITR-2 smoke test: `"success": true`, `itdJson` contains `{"ITR":{"ITR2":`
- [ ] No `NullPointerException` in any test payload
- [ ] `grep -c "ERROR" $(mvn compile 2>&1)` → 0

---

*This directive closes all remaining gaps identified in the Phase 7 audit. The three JSON export services are the singular critical path. Everything else (depreciation computation, AY 2026-27 wiring, auto-population, metro detection, multi-employer) is wiring of already-built logic. Complete the export services first — in order: ITR-2, ITR-3, ITR-4.*

*Legal basis: Income-tax Act 1961, Finance Act 2025 (AY 2026-27), CBDT e-Filing Validation Rules V1.0/V1.1 (July 2025), ITD JSON Schema AY 2025-26, Section 32 (depreciation), Section 115BBH (VDA), Section 44AD/44ADA/44AE (presumptive income), Section 10(13A) (HRA).*
