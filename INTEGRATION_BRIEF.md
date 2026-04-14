# IncomeTax ERP — Frontend ↔ Backend Integration Brief
> **For AI Agent Use Only.** This document instructs you to wire every form field, table, and action in `ITRDashboard.jsx` to the existing backend APIs. **You must NOT modify any backend files — controllers, services, repositories, DTOs, or entities.** Your work is frontend-only: add API call functions, replace all hardcoded/static data with live fetch calls, and handle loading/error states in the React component.

---

## 0. Ground Rules

| Rule | Detail |
|---|---|
| ❌ No backend changes | Never edit or create any `.java`, backend DTO, controller, service, or config file |
| ✅ Frontend only | All changes live inside `ITRDashboard.jsx` (or co-located utility files you create) |
| ✅ Preserve all UI | Do not change any CSS, layout, design tokens, or visual structure — pixel-perfect preservation is mandatory |
| ✅ Computed fields stay local | Fields marked `computed` in the `Field` component are derived by the `computeTax()` engine; never send them to the backend — compute client-side only |
| ✅ Error handling | Every API call must have a loading state and a visible error toast/inline message |
| ✅ Optimistic save | On "💾 Save", save computation to backend but keep local state as source of truth for the tax engine |

---

## 1. Base Configuration

Create a file `src/api/apiClient.js` (or inline at top of `ITRDashboard.jsx` if single-file deployment):

```js
const BASE_URL = process.env.REACT_APP_API_BASE_URL || "http://localhost:8080";

export async function apiFetch(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json", ...options.headers },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `HTTP ${res.status}`);
  }
  return res.json();
}
```

---

## 2. Client Master — `ClientsView`

### 2a. Data Source (replace hardcoded `clients` array)

**Current hardcoded:**
```js
const clients = [ { name:"Ramesh Verma", pan:"DEFPV1122K", ... }, ... ]
```

**Replace with API call:**

| Field in JSX | Backend DTO Field | Notes |
|---|---|---|
| `c.name` | `fullName` | String |
| `c.pan` | `pan` | String (10-char PAN) |
| `c.type` | `assesseeType` | Enum: `"Individual"`, `"HUF"`, `"Firm"` |
| `c.mobile` | `mobileNumber` | String |
| `c.itr` | `itrForm` | Enum: `"ITR-1"` … `"ITR-4"` |
| `c.sync` | `lastAisSync` | String / LocalDate formatted as `"Apr 2"` |
| `c.initials` | `initials` | Derived: first letters of name (compute FE if not in DTO) |
| `c.filingStatus` | `filingStatus` | Enum: `"Filed"`, `"In Progress"`, `"Doc Pending"`, `"Mismatch"`, `"Ready to File"` |
| `c.filingCls` | — | Derive FE-side from `filingStatus` (see mapping table below) |
| `c.ev` | `eVerifyStatus` | Enum: `"Verified"`, `"Pending"`, `"—"` |
| `c.evCls` | — | Derive FE-side from `eVerifyStatus` |
| `c.watch` | `watchFlag` | Boolean |
| `c.id` | `id` | Long — needed for navigation to computation |

**Status → Badge class mapping (FE-only, do not send to backend):**
```js
const filingCls = {
  "Filed": "badge-success",
  "In Progress": "badge-info",
  "Doc Pending": "badge-warning",
  "Mismatch": "badge-danger",
  "Ready to File": "badge-gold",
};
const evCls = {
  "Verified": "badge-success",
  "Pending": "badge-warning",
  "—": "badge-muted",
};
```

**API Endpoint:**
```
GET /api/clients?ay={assessmentYear}&page={page}&size={size}&status={filingStatus}
```
- Returns: `Page<ClientSummaryDTO>` or plain array `ClientSummaryDTO[]`
- Query params map to the topbar AY selector (`ay`), status filter dropdown, and search input
- Search input: append `&search={query}` (name/PAN/mobile)

**Add Client button:**
```
POST /api/clients
Body: { fullName, pan, assesseeType, mobileNumber, itrForm, assessmentYear }
```

---

## 3. Filing View — `FilingView`

The `FilingView` re-uses the `clients` array filtered by `filingStatus !== "Filed"`. No separate endpoint needed unless the backend exposes a dedicated filings list. If it does:

```
GET /api/filings?ay={assessmentYear}&stage={stage}
```

| Field in JSX | Backend DTO Field | Notes |
|---|---|---|
| `c.name` | `clientName` | String |
| `c.pan` | `pan` | String |
| `c.itr` | `itrForm` | String |
| `c.sync` | `lastAisSync` | String |
| Filing recon badge | `reconStatus` | `"Clean"`, `"Mismatch"`, `"Pending"`, `"In Progress"` |
| Filing stage badge | `filingStage` | `"Mismatch Review"`, `"Reconciliation"`, `"Ready to File"`, `"Doc Pending"` |
| Tax payable cell | `taxPayable` | Long (render as `₹X,XX,XXX` or `"—"` if null) |

**"Open ITR" button** — calls `onOpenComputation(client)` with the full client object including `id`.

---

## 4. ITR Computation View — `ITRComputationView`

This is the most critical section. The `d` state object holds all input fields. On mount, fetch the saved computation for the selected client + AY. On "💾 Save", PATCH/PUT the computation back.

### 4a. Load Computation

```
GET /api/computations/{clientId}?ay={assessmentYear}
```
Returns: `ITRComputationDTO` (see full field mapping below)

On success: call `setD(responseData)` to populate all form fields.

### 4b. Save Computation

```
PUT /api/computations/{clientId}
Body: ITRComputationDTO (see below)
```
Trigger: "💾 Save" button click. Also auto-save on `activeSection` tab change (debounced, 2s).

### 4c. Import Endpoints

| Button | Endpoint | Body |
|---|---|---|
| 📥 AIS / TIS JSON | `POST /api/computations/{clientId}/import/ais?ay={ay}` | `{ jsonData: <file content string> }` |
| 📄 Form 16 JSON | `POST /api/computations/{clientId}/import/form16?ay={ay}` | `{ jsonData: <file content string> }` |
| 🔍 Form 16 PDF (OCR) | `POST /api/computations/{clientId}/import/form16-pdf?ay={ay}` | `multipart/form-data` with `file` field |

On success of any import: merge returned partial DTO into `d` state using `setD(prev => ({ ...prev, ...importedFields }))`.

Show `importBanner` with the appropriate message as the existing code already does.

### 4d. Validate ITR

```
POST /api/computations/{clientId}/validate?ay={ay}
Body: ITRComputationDTO
```
Returns: `{ valid: boolean, errors: string[] }`

On click of "✓ Validate ITR": show errors inline or a success toast.

### 4e. Export ITR JSON

```
GET /api/computations/{clientId}/export?ay={ay}&form={itrForm}
```
Returns: file download (JSON). Trigger browser download.

### 4f. Download Computation PDF

```
GET /api/computations/{clientId}/pdf?ay={ay}
```
Returns: PDF file. Trigger browser download.

---

## 5. Full `ITRComputationDTO` Field Mapping

> Every field in the `d` state object must be mapped. Fields marked **computed** are NEVER sent to or expected from the backend — they are derived by `computeTax()`.

### 5.1 — Employer / Metadata (top bar selectors)

| `d` State Key | DTO Field | Type | Notes |
|---|---|---|---|
| `empName` | `employerName` | String | SalarySection employer name |
| `empTAN` | `employerTan` | String | Employer TAN |
| `empType` | `employerType` | Enum: `"government"`, `"psu"`, `"private"` | |
| `ay` (component state) | `assessmentYear` | String: `"2025-26"` or `"2026-27"` | Sent as query param |
| `regime` (component state) | `taxRegime` | Enum: `"new"`, `"old"` | |
| `itrForm` (component state) | `itrForm` | String: `"ITR-1"` … `"ITR-4"` | |

### 5.2 — Salary Section (`SalarySection`)

| `d` Key | DTO Field | Type | Computed? |
|---|---|---|---|
| `d.basic` | `basicSalary` | Long | ❌ Input |
| `d.da` | `dearnessAllowance` | Long | ❌ Input |
| `d.hra` | `hraReceived` | Long | ❌ Input |
| `d.bonus` | `bonusIncentives` | Long | ❌ Input |
| `d.allowances` | `specialAllowances` | Long | ❌ Input |
| `d.perquisites` | `perquisites` | Long | ❌ Input |
| `d.hraRent` | `annualRentPaid` | Long | ❌ Input |
| `d.hraMetro` | `hraMetroCity` | Boolean | ❌ Input |
| `d.profTax` | `professionalTax` | Long | ❌ Input |
| Gross Salary | — | — | ✅ Computed FE: basic+da+hra+bonus+allowances+perquisites |
| HRA Exempt | — | — | ✅ Computed FE: min of 3 conditions |
| Net Taxable Salary | — | — | ✅ Computed FE: gross−hraExempt−75000−min(profTax,2500) |
| Standard Deduction | — | — | ✅ Hardcoded ₹75,000 |

### 5.3 — House Property Section (`HousePropSection`)

| `d` Key | DTO Field | Type | Computed? |
|---|---|---|---|
| `d.hpType` | `housePropertyType` | Enum: `"sop"`, `"letout"`, `"deemed"` | ❌ Input |
| `d.grossRent` | `grossAnnualRent` | Long | ❌ Input (letout only) |
| `d.munTax` | `municipalTaxPaid` | Long | ❌ Input (letout only) |
| `d.homeLoanInt` | `homeLoanInterestLetOut` | Long | ❌ Input (letout only) |
| `d.sopLoanInt` | `homeLoanInterestSop` | Long | ❌ Input (SOP only) |
| `d.preConInt` | `preConstructionInterest` | Long | ❌ Input (SOP, optional) |
| Net Annual Value | — | — | ✅ Computed FE: max(0, grossRent−munTax) |
| Std. Deduction 30% | — | — | ✅ Computed FE: NAV × 0.30 |
| Net HP Income | — | — | ✅ Computed FE |

### 5.4 — Capital Gains Section (`CapGainsSection`)

| `d` Key | DTO Field | Type | Computed? |
|---|---|---|---|
| `d.stcgPre` | `stcgPre23Jul2024` | Long | ❌ Input |
| `d.stcgPost` | `stcgPost23Jul2024` | Long | ❌ Input |
| `d.stcgOther` | `stcgOtherAssets` | Long | ❌ Input |
| `d.ltcgPre` | `ltcgPre23Jul2024` | Long | ❌ Input |
| `d.ltcgPost` | `ltcgPost23Jul2024` | Long | ❌ Input |
| `d.ltcgOther` | `ltcgOtherAssets` | Long | ❌ Input |
| STCG Tax (auto) | — | — | ✅ Computed FE: stcgPre×0.15 + stcgPost×0.20 |
| LTCG exempt threshold | — | — | ✅ Computed FE: AY-based (₹1L/₹1.25L) |
| LTCG Tax §112A | — | — | ✅ Computed FE |

### 5.5 — Business Section (`BusinessSection`)

| `d` Key | DTO Field | Type | Computed? |
|---|---|---|---|
| `d.bizType` | `presumptiveScheme` | Enum: `"44AD"`, `"44ADA"`, `"regular"` | ❌ Input |
| `d.bizTurnover` | `grossTurnoverReceipts` | Long | ❌ Input |
| `d.bizDigital` | `digitalReceipts` | Long | ❌ Input (optional) |
| `d.bizDeclared` | `declaredIncome` | Long | ❌ Input |
| `d.bpNetProfit` | `regularBooksNetProfit` | Long | ❌ Input (regular only) |

### 5.6 — Other Sources Section (`OtherSourcesSection`)

| `d` Key | DTO Field | Type | Computed? |
|---|---|---|---|
| `d.interestSB` | `savingsBankInterest` | Long | ❌ Input |
| `d.interestFD` | `fixedDepositInterest` | Long | ❌ Input |
| `d.interestRefund` | `itRefundInterest` | Long | ❌ Input (optional) |
| `d.dividends` | `dividendIncome` | Long | ❌ Input |
| `d.familyPension` | `familyPension` | Long | ❌ Input |
| `d.otherMisc` | `otherMiscIncome` | Long | ❌ Input (optional) |
| Total Other Sources | — | — | ✅ Computed FE |

### 5.7 — VDA / Crypto Section (`VDASection`)

| `d` Key | DTO Field | Type | Computed? |
|---|---|---|---|
| `d.vdaSale` | `vdaSaleConsideration` | Long | ❌ Input |
| `d.vdaCOA` | `vdaCostOfAcquisition` | Long | ❌ Input |
| `d.vdaGains` | `vdaNetGains` | Long | ❌ Input |
| `d.vdaTDS` | `vdaTds194S` | Long | ❌ Input |
| VDA Tax @ 30% | — | — | ✅ Computed FE: max(0, vdaGains) × 0.30 |

### 5.8 — Deductions Section (`DeductionsSection`)

| `d` Key | DTO Field | Type | Computed? |
|---|---|---|---|
| `d.s80C_epf` | `dedn80CEpf` | Long | ❌ Input |
| `d.s80C_ppf` | `dedn80CPpf` | Long | ❌ Input |
| `d.s80C_elss` | `dedn80CElss` | Long | ❌ Input |
| `d.s80C_lic` | `dedn80CLic` | Long | ❌ Input |
| `d.s80C_home` | `dedn80CLoanPrincipal` | Long | ❌ Input |
| `d.s80CCD1B` | `dedn80Ccd1b` | Long | ❌ Input (max ₹50,000) |
| `d.s80CCD2` | `dedn80Ccd2` | Long | ❌ Input (optional) |
| `d.s80D_self` | `dedn80DSelf` | Long | ❌ Input (old regime only) |
| `d.s80D_parent` | `dedn80DParent` | Long | ❌ Input (old regime only) |
| `d.s80E` | `dedn80E` | Long | ❌ Input (old regime only) |
| `d.s80TTA` | `dedn80Tta` | Long | ❌ Input (old regime only) |
| `d.s80G` | `dedn80G` | Long | ❌ Input (old regime only, optional) |
| Total §80C (capped ₹1.5L) | — | — | ✅ Computed FE |

### 5.9 — Losses Section (`LossesSection`)

| `d` Key | DTO Field | Type | Computed? |
|---|---|---|---|
| `d.bfBizLoss` | `bfBusinessLoss` | Long | ❌ Input |
| `d.bfSTCG` | `bfStcgLoss` | Long | ❌ Input |
| `d.bfLTCG` | `bfLtcgLoss` | Long | ❌ Input |
| `d.bfSpecLoss` | `bfSpeculativeLoss` | Long | ❌ Input |
| `d.bfBizSetOff` | `bfBizLossSetOff` | Long | ❌ Input |
| `d.bfLoss` | `totalBfLossUtilised` | Long | ❌ Input (or computed sum — align with backend) |
| Total BF Loss | — | — | ✅ Computed FE: bfBizSetOff + bfSTCG + bfLTCG |

### 5.10 — TDS & Advance Tax Section (`TDSSection`)

| `d` Key | DTO Field | Type | Computed? |
|---|---|---|---|
| `d.tdsS192` | `tdsS192Salary` | Long | ❌ Input |
| `d.tds194A` | `tdsS194AInterest` | Long | ❌ Input |
| `d.tds194B` | `tdsS194BLottery` | Long | ❌ Input (optional) |
| `d.tds194IA` | `tdsS194IAProperty` | Long | ❌ Input (optional) |
| `d.tds194S` | `tdsS194SVda` | Long | ❌ Input (optional) |
| `d.tdsOther` | `tdsOther` | Long | ❌ Input (optional) |
| `d.adv15Jun` | `advTax15Jun` | Long | ❌ Input |
| `d.adv15Sep` | `advTax15Sep` | Long | ❌ Input |
| `d.adv15Dec` | `advTax15Dec` | Long | ❌ Input |
| `d.adv15Mar` | `advTax15Mar` | Long | ❌ Input |
| `d.selfTax` | `selfAssessmentTax` | Long | ❌ Input |
| Total TDS | — | — | ✅ Computed FE |
| Total Advance Tax | — | — | ✅ Computed FE |

### 5.11 — Tax Computation Panel (`TaxComputationSection`)

All values in this section are **fully computed by `computeTax(d, ay, regime)`** client-side. Nothing in this view is fetched from or persisted to the backend separately. The final computed values are displayed for reference only.

---

## 6. Dashboard View — `DashboardView`

Replace all hardcoded stat values and activity feed items.

### 6a. Stats Cards

```
GET /api/dashboard/stats?ay={assessmentYear}
```

| UI Label | DTO Field | Type |
|---|---|---|
| "Revenue This Month" | `revenueThisMonth` | Long (render as `₹X.XL`) |
| "Pending E-Verify" | `pendingEVerify` | Integer |
| "Active Notices" | `activeNotices` | Integer |
| "Outstanding Dues" | `outstandingDues` | Long (render as `₹X.XL`) |

### 6b. Filing Progress Pipeline

```
GET /api/dashboard/filing-pipeline?ay={assessmentYear}
```

| UI Stage | DTO Field | Type |
|---|---|---|
| "Total" | `totalClients` | Integer |
| "Filed ✓" | `filedCount` | Integer |
| "In Progress" | `inProgressCount` | Integer |
| "Doc Pending" | `docPendingCount` | Integer |
| "Ready" | `readyToFileCount` | Integer |
| "Mismatch" | `mismatchCount` | Integer |
| Progress % | — | Computed FE: `(filedCount / totalClients) * 100` |

### 6c. Recent Activity Feed

```
GET /api/dashboard/activity?limit=10
```

Response: `ActivityItemDTO[]`

| DTO Field | Type | Notes |
|---|---|---|
| `clientName` | String | Used in bold |
| `activityText` | String | Full text message |
| `timestamp` | String | Display as "2 minutes ago" etc. |
| `badge` | String | `"Filed"`, `"Notice"`, `"Partial"`, `"Mismatch"`, `"E-Verified"` |
| `badgeCls` | String | `"badge-success"`, `"badge-danger"`, `"badge-warning"` |
| `dotColor` | String | Hex or CSS var name |

### 6d. ITR Type Distribution

```
GET /api/dashboard/itr-types?ay={assessmentYear}
```

| DTO Field | Type |
|---|---|
| `itrForm` | String: `"ITR-1"` … `"ITR-4"` |
| `count` | Integer |

### 6e. Today's Deadlines

```
GET /api/dashboard/deadlines?limit=5
```

| DTO Field | Type |
|---|---|
| `title` | String |
| `subtitle` | String |
| `severity` | Enum: `"urgent"`, `"warning"`, `"info"` |
| `dotColor` | Derived FE from severity |

---

## 7. AY Selector (Top Bar)

The `<select>` in the topbar renders options `"AY 2025–26"`, `"AY 2026–27"`, `"Both AYs"`. When the user changes it, re-fetch all data-driven views with the new `ay` query parameter:
- `GET /api/clients?ay=...`
- `GET /api/dashboard/stats?ay=...`
- etc.

Map the display value to the API parameter:
```js
const ayParam = { "AY 2025–26": "2025-26", "AY 2026–27": "2026-27", "Both AYs": null };
```

---

## 8. State Management Pattern

In `ITRComputationView`, wire the lifecycle as follows:

```js
// 1. On mount — load saved computation
useEffect(() => {
  if (!client?.id) return;
  setLoading(true);
  apiFetch(`/api/computations/${client.id}?ay=${ay}`)
    .then(data => setD(prev => ({ ...prev, ...data })))
    .catch(err => setError(err.message))
    .finally(() => setLoading(false));
}, [client?.id, ay]);

// 2. On regime change — re-run tax engine only (no API call needed)
// computeTax(d, ay, regime) already handles this via useMemo

// 3. Save button
const handleSave = async () => {
  setSaving(true);
  try {
    await apiFetch(`/api/computations/${client.id}`, {
      method: "PUT",
      body: JSON.stringify({ ...d, assessmentYear: ay, taxRegime: regime, itrForm }),
    });
    // show success toast
  } catch (err) {
    // show error toast
  } finally {
    setSaving(false));
  }
};
```

---

## 9. Sidebar Search

The global search input (`placeholder="Search clients, PAN…"`) should call:
```
GET /api/clients/search?q={query}&ay={ay}
```
Debounce: 300ms. On result click, navigate to that client's computation view.

---

## 10. "Run Bulk Sync" Button

```
POST /api/sync/bulk?ay={assessmentYear}
```
Returns: `{ jobId: string, status: "QUEUED" }`
Show a toast: "Bulk sync job queued. Job ID: {jobId}"

---

## 11. Loading & Error States

| Situation | UI Behaviour |
|---|---|
| Fetching clients list | Show skeleton rows in the table |
| Fetching computation | Show a spinner overlay on the form area |
| Save in progress | Disable "💾 Save" button, show spinner inside it |
| API error | Show a red inline error bar below the action bar with the error message |
| Import in progress | Show spinner on the clicked import button |

---

## 12. AY-Specific Tax Rule Notes (for validation — FE-side only)

These are enforced by `computeTax()` and require no backend change. Listed for context:

- AY 2026-27: LTCG exempt threshold = ₹1,25,000 (AY 2025-26: ₹1,00,000)
- AY 2026-27: LTCG §112A rate = 12.5% (AY 2025-26: 10%)
- AY 2026-27: New regime rebate nil-tax threshold = ₹12,00,000
- Standard deduction = ₹75,000 (hardcoded, not from backend)
- Professional Tax max cap = ₹2,500 (enforced FE)
- SOP home loan interest max cap = ₹2,00,000 (enforced FE)
- §80C overall cap = ₹1,50,000 (enforced FE)
- §80CCD(1B) cap = ₹50,000 (enforced FE)

---

## 13. Summary Checklist for the Agent

- [ ] Create `apiClient.js` with base URL and `apiFetch` wrapper
- [ ] Replace `const clients = [...]` with `useEffect` + `GET /api/clients`
- [ ] Wire client search input to `GET /api/clients/search`
- [ ] Replace `DashboardView` hardcoded stats with `GET /api/dashboard/stats`
- [ ] Replace `DashboardView` activity feed with `GET /api/dashboard/activity`
- [ ] Replace `DashboardView` ITR type chart with `GET /api/dashboard/itr-types`
- [ ] Replace `DashboardView` deadlines with `GET /api/dashboard/deadlines`
- [ ] Replace `DashboardView` pipeline numbers with `GET /api/dashboard/filing-pipeline`
- [ ] Wire `ITRComputationView` `useEffect` to load `GET /api/computations/{id}?ay=` on mount
- [ ] Map all 50+ `d` state fields to DTO fields per Section 5 above
- [ ] Wire "💾 Save" to `PUT /api/computations/{id}`
- [ ] Wire AIS/TIS import button to `POST /api/computations/{id}/import/ais`
- [ ] Wire Form 16 JSON import to `POST /api/computations/{id}/import/form16`
- [ ] Wire Form 16 PDF import to `POST /api/computations/{id}/import/form16-pdf`
- [ ] Wire "✓ Validate ITR" to `POST /api/computations/{id}/validate`
- [ ] Wire "Export ITR JSON" to `GET /api/computations/{id}/export`
- [ ] Wire "📊 Download PDF" to `GET /api/computations/{id}/pdf`
- [ ] Wire "Run Bulk Sync" to `POST /api/sync/bulk`
- [ ] Wire AY topbar selector to refetch all views
- [ ] Add loading skeletons/spinners for all async operations
- [ ] Add error toast/inline error bar for all failed calls
- [ ] Confirm `computeTax()` engine is never replaced — only fed from `d` state
- [ ] Confirm all `computed` `Field` components remain read-only (no backend write)
- [ ] **Confirm zero backend file modifications**
