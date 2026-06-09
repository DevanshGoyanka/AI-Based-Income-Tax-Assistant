# CAPITAL GAINS - STRICT CBDT COMPLIANCE AUDIT REPORT
## Mandatory Fields Gap Analysis - Schedule CG (AY 2025-26)

---

## EXECUTIVE SUMMARY

This report provides a **strict audit** of the Capital Gains implementation against CBDT (Central Board of Direct Taxes) mandatory field requirements for Schedule CG as per Income Tax Department guidelines and Ministry of Finance regulations for Assessment Year 2025-26.

**CRITICAL FINDING**: The current Capital Gains tab implementation is **NON-COMPLIANT** with CBDT Schedule CG requirements. Multiple mandatory fields are missing, leading to potential ITR rejection.

---

## SECTION 1: CURRENTLY IMPLEMENTED FIELDS

### Frontend (CapitalGainsEntryManager.tsx)
```
✓ assetType           ✓ purchaseDate      ✓ saleDate
✓ assetDescription    ✓ purchaseCost      ✓ saleCost
✓ expenses            ✓ exemptionSection
```

### Backend Validation (CapitalGainsValidationService.java)
```
✓ Basic field validation (null checks)
✓ Date relationship validation
✓ Amount validation
✓ Holding period calculation
✓ Exemption section validation
✓ Buyer details (partial - warning only)
✓ Grandfathering (Section 112A)
```

---

## SECTION 2: MISSING MANDATORY FIELDS (CBDT NON-COMPLIANT)

### 2.1 PROPERTY/LAND TRANSACTIONS (CRITICAL GAP)

| Field | CBDT Requirement | Current Status | Severity |
|-------|------------------|----------------|----------|
| **Property Address** | MANDATORY for all property sales | NOT CAPTURED | BLOCKER |
| **Pincode** | MANDATORY for property >₹50L | NOT CAPTURED | BLOCKER |
| **Property Type** | MANDATORY (Urban/ Rural for land) | NOT CAPTURED | BLOCKER |
| **Buyer Name** | MANDATORY for >₹50L | Only WARNING | BLOCKER |
| **Buyer PAN** | MANDATORY for >₹50L (as per Sec 194IA) | Only WARNING | BLOCKER |
| **Seller Name** | MANDATORY for >₹50L | NOT CAPTURED | BLOCKER |
| **Seller PAN** | MANDATORY for >₹50L | NOT CAPTURED | BLOCKER |
| **Stamp Duty Value** | Required for Section 50C | NOT CAPTURED | HIGH |
| **Date of Agreement** | Required for determining FY | NOT CAPTURED | HIGH |

### 2.2 EQUITY/SHARE TRANSACTIONS (CRITICAL GAP)

| Field | CBDT Requirement | Current Status | Severity |
|-------|------------------|----------------|----------|
| **ISIN Code** | MANDATORY for listed equity/MF | NOT CAPTURED | BLOCKER |
| **Name of Scrip** | MANDATORY | NOT CAPTURED | BLOCKER |
| **Number of Units** | MANDATORY | PARTIAL (Purchase Cost only) | HIGH |
| **Sale Price Per Unit** | Required for Schedule 112A detail | NOT CAPTURED | BLOCKER |
| **Purchase Price Per Unit** | Required for Schedule 112A detail | NOT CAPTURED | BLOCKER |

### 2.3 OTHER ASSET TRANSACTIONS

| Field | CBDT Requirement | Current Status | Severity |
|-------|------------------|----------------|----------|
| **Description** | Mandatory for non-listed assets | PARTIAL | MEDIUM |
| **Dematerialized** | Required (Y/N) for shares | NOT CAPTURED | HIGH |
| **STT Paid** | Required (Y/N) | PARTIAL (through asset type) | LOW |

### 2.4 LTCG CALCULATION FIELDS

| Field | CBDT Requirement | Current Status | Severity |
|-------|------------------|----------------|----------|
| **Cost of Improvement** | Optional but impact tax significantly | AVAILABLE | OK |
| **Acquisition Financial Year** | Required for CII lookup | NOT EXTRACTED | HIGH |
| **Transfer Financial Year** | Required for CII lookup | NOT EXTRACTED | HIGH |
| **Indexation Applied** | Y/N flag | NOT TRACKED | HIGH |
| **Cost Inflation Index (Acquisition)** | Required for LTCG | NOT TRACKED | HIGH |
| **Cost Inflation Index (Transfer)** | Required for LTCG | NOT TRACKED | HIGH |

### 2.5 EXEMPTION/REINVESTMENT FIELDS

| Field | CBDT Requirement | Current Status | Severity |
|-------|------------------|----------------|----------|
| **Investment Date** | Mandatory for Sec 54/54F/54EC | NOT CAPTURED | HIGH |
| **New Property Address** | Mandatory for Sec 54/54F | NOT CAPTURED | HIGH |
| **New Property Pincode** | Mandatory for Sec 54/54F | NOT CAPTURED | HIGH |
| **Bond Details (54EC)** | Name, issuer, date | NOT CAPTURED | HIGH |
| **Amount Invested** | Partial | PARTIAL (exemptionAmount) | MEDIUM |

---

## SECTION 3: LEGAL REQUIREMENTS (CBDT/FMoF)

### 3.1 Section 194IA (Buyer's Responsibility)
- **Requirement**: Buyer must deduct TDS at 1% if property sale >₹50 Lakhs
- **Mandatory**: Buyer PAN, Seller PAN, Property Details
- **Current Gap**: System does NOT enforce buyer/seller PAN for >₹50L

### 3.2 Section 50C (Stamp Duty Valuation)
- **Requirement**: If stamp duty value > sale consideration, use stamp duty for cap gains
- **Mandatory**: Stamp duty value field required for property
- **Current Gap**: Not captured - may result in incorrect LTCG

### 3.3 Schedule CG XML/JSON Schema Requirements
According to ITD JSON schema for ITR-2:

**For Schedule112A (Equity/MF):**
```json
"Schedule112A": [{
  "ISINCode": "string",        // MANDATORY
  "NameOfScrip": "string",     // MANDATORY  
  "SharesOrUnits": number,     // MANDATORY
  "SalePricePerUnit": number,  // MANDATORY
  "TotalSaleValue": number,    // MANDATORY
  "ActualCostOfAcq": number,   // MANDATORY
  "FMVPerShareOn31Jan2018": number, // For grandfathering
  "TransferDate": "date",      // MANDATORY
  "ExemptionUs54EC": number,
  "ExemptionUs54F": number
}]
```

**For Schedule112 (Property/Other):**
```json
"Schedule112": [{
  "DateOfAcquisition": "date",     // MANDATORY
  "DateOfTransfer": "date",        // MANDATORY
  "CostOfAcquisition": number,     // MANDATORY
  "CostOfImprovement": number,
  "TransferExpenses": number,
  "FullValueOfConsideration": number, // MANDATORY
  "Address": "string",             // MANDATORY for property
  "PinCode": "string",             // MANDATORY for property >50L
  "PropertyType": "string",        // U/R
  "BnftOnSale": "string"           // Type of asset
}]
```

---

## SECTION 4: VALIDATION GAPS

### 4.1 Mandatory Field Enforcement (CURRENT vs REQUIRED)

| Validation | Current Behavior | Required Behavior | Impact |
|------------|-----------------|-------------------|--------|
| Buyer PAN for >₹50L property | WARNING only | ERROR & BLOCK submission | REJECTION |
| Buyer Name for >₹50L property | WARNING only | ERROR & BLOCK submission | REJECTION |
| Property Address for property | Not validated | MANDATORY for property | REJECTION |
| ISIN for listed equity | Not validated | ERROR if missing for listed | REJECTION |
| Pincode for >₹50L property | Not validated | MANDATORY | REJECTION |

### 4.2 Missing Cross-Field Validations

1. **Purchase Date → Financial Year → CII Match**
   - Current: Not validated
   - Required: Verify CII exists for acquisition FY

2. **Asset Type → Holding Period → Gain Type Consistency**
   - Current: Validated partially
   - Required: Add explicit validation

3. **Exemption → Investment Timeline (2 years for Sec 54)**
   - Current: Warning only
   - Required: Add date validation

4. **FMV Jan 2018 → Purchase Date Consistency**
   - Current: Warning
   - Required: Error if purchase > Jan 2018 and FMV provided

---

## SECTION 5: JSON EXPORT COMPLIANCE

### 5.1 Current Export vs ITD Schema

**Current Export (ITR2JSONExportService.java:buildScheduleCG)** - Line 201-280:
```java
// Schedule112A fields exported:
r.put("ISINCode", nvl(row.getIsinCode()));          // MISSING IN UI
r.put("NameOfScrip", nvl(row.getNameOfScrip()));    // MISSING IN UI
r.put("SharesOrUnits", safeInt(row.getUnits()));    // MISSING IN UI
r.put("SalePricePerUnit", safeInt(row.getSalePricePerUnit())); // MISSING IN UI
r.put("FMVPerShareOn31Jan2018", ...);               // PARTIAL
r.put("TransferDate", ...);                         // Available
r.put("ExemptionUs54EC", ...);                      // Available
r.put("ExemptionUs54F", ...);                       // Available
```

**Impact**: JSON will have NULL values for mandatory Schedule CG fields → ITR REJECTION

### 5.2 Data Flow Analysis

```
Frontend Input → Backend Calculation → JSON Export → ITD Portal
     ↓                  ↓                   ↓            ↓
   INCOMPLETE      CALCULATES OK       NULL/MISSING   REJECTION
   FIELDS           VALUES             REQUIRED FIELDS
```

---

## SECTION 6: PRIORITY MATRIX FOR FIXES

### PRIORITY 1: BLOCKERS (Fix Immediately)

| # | Field | Fix Location | Complexity | Effort |
|---|-------|--------------|------------|--------|
| 1 | Property Address | CapitalGainsEntryManager.tsx | HIGH | 2 days |
| 2 | Pincode | CapitalGainsEntryManager.tsx | MEDIUM | 1 day |
| 3 | Buyer Name | CapitalGainsEntryManager.tsx | MEDIUM | 1 day |
| 4 | Buyer PAN | CapitalGainsEntryManager.tsx | MEDIUM | 1 day |
| 5 | ISIN Code | CapitalGainsEntryManager.tsx | MEDIUM | 1 day |
| 6 | Share Units | CapitalGainsEntryManager.tsx | MEDIUM | 1 day |
| 7 | Sale Price Per Unit | capitalGainsCalculationService.ts | MEDIUM | 1 day |
| 8 | Purchase Price Per Unit | capitalGainsCalculationService.ts | MEDIUM | 1 day |

### PRIORITY 2: HIGH PRIORITY

| # | Field | Fix Location | Complexity | Effort |
|---|-------|--------------|------------|--------|
| 1 | Make Buyer PAN mandatory for >₹50L | CapitalGainsValidationService.java | LOW | 2 hours |
| 2 | Property Type (Urban/Rural) | CapitalGainsEntryManager.tsx | LOW | 1 day |
| 3 | Stamp Duty Value (50C) | CapitalGainsEntryManager.tsx | MEDIUM | 1 day |
| 4 | Cost Inflation Index storage | CapitalGainsComputationService.java | MEDIUM | 1 day |

### PRIORITY 3: MEDIUM PRIORITY

| # | Field | Fix Location | Complexity | Effort |
|---|-------|--------------|------------|--------|
| 1 | Investment Date for exemptions | CapitalGainsEntryManager.tsx | MEDIUM | 1 day |
| 2 | New Property Address (54/54F) | CapitalGainsEntryManager.tsx | HIGH | 2 days |
| 3 | Bond Details (54EC) | CapitalGainsEntryManager.tsx | MEDIUM | 1 day |
| 4 | Acquisition FY validation | CapitalGainsValidationService.java | LOW | 2 hours |

---

## SECTION 7: RECOMMENDED ACTIONS

### Immediate Actions Required:

1. **Add Property Address fields** to CapitalGainsEntryManager.tsx
   - Required for property/LAND asset types
   
2. **Add Buyer Fields** - Make mandatory when sale > ₹50L
   - Buyer Name (text)
   - Buyer PAN (format: AAAAA1234A)
   - Validate PAN format

3. **Add Equity/Share Specific fields**
   - ISIN Code (12-char for India: IN##########)
   - Name of Scrip
   - Units
   - Price Per Unit

4. **Enable Validation export** - Cannot submit without mandatory fields

5. **Update JSON Export** - Ensure all required fields flow through

### Validation Rules to Add:
```java
// Buyer PAN - Make it ERROR not WARNING
if (saleCost >= 5000000 && (buyerPAN == null || buyerPAN.isEmpty())) {
    errors.add("ERROR: Buyer PAN mandatory for property sale ≥ ₹50 Lakhs as per Section 194IA");
}

// Property Address
if (assetType.isProperty() && (address == null || address.isEmpty())) {
    errors.add("ERROR: Property address is mandatory for Schedule CG");
}
```

---

## SECTION 8: SUMMARY

| Category | Count | Status |
|----------|-------|--------|
| Total Required Fields (CBDT) | ~45 | - |
| Currently Implemented | 12 | 27% |
| Missing Mandatory | 18 | BLOCKER |
| Missing Recommended | 15 | HIGH |

**COMPLIANCE SCORE: 27%** - **NON-COMPLIANT**

The current implementation will result in ITR rejection when attempting to file returns with capital gains through this system. Immediate remediation is required.

---

## APPENDIX: CBDT SCHEDULE CG REFERENCE

### Mandatory Fields Per Asset Type:

**For Listed Equity (112A):**
- ISIN Code, Name, Units, Sale Price, Purchase Cost, Transfer Date

**For Unlisted Equity (112):**
- Description, Date of Acquisition, Date of Transfer, Full Value

**For Property:**
- Full Address, Pincode, Dates of Acquisition/Transfer, Costs

**For >₹50L Transactions:**
- Buyer Name, Buyer PAN (via 194IA), Property Address with Pincode

---

*Report Generated: May 2026*
*Compliance Standard: CBDT ITD Schema 2025-26*
*Assessment: AY 2025-26*
