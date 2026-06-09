# ITR Filing System — Phase 1 Implementation SPEC for Agentic AI

> **Application:** ITR Filing Assistant — AY 2026-27 (FY 2025-26)
> **Target:** ITR-1 (Sahaj), ITR-2, ITR-3, ITR-4 (Sugam) — Filing Ready
> **Branch:** `restructure/cleanup-v1`
> **Current State:** Backend compiles (0 errors), frontend builds (0 errors). Login works. All pages return 500 — NO REST controllers exist except AuthController.
> **Goal:** Working application where user can manage clients, import AIS/26AS/TIS via ITD ERI-2 API, import Form16 PDF, auto-populate, compute taxes, reconcile, download computation PDF + ITD-schema JSON.

---

## 1. RULES — DO NOT VIOLATE

### 1.1 File Modification Rules

| Action | Allowed? |
|---|---|
| **CREATE new files** | ✅ YES — all new controller/service/frontend files |
| **EDIT existing backend files** | ✅ YES — see "Can Edit" list |
| **EDIT existing frontend files** | ✅ YES — to connect to real endpoints |
| **DELETE files** | ❌ NO — never delete anything |
| **RENAME files** | ❌ NO — never rename anything |
| **Modify pom.xml** | ❌ NO — dependencies are sufficient |
| **Modify application.properties** | ❌ NO |
| **Modify SecurityConfig.java** | ⚠️ Only to add new public endpoint routes |
| **Touch infrastructure/security/** | ❌ NO — JwtTokenProvider works |
| **Touch model/ package** | ❌ NO — dead code, leave alone |
| **Modify any `_stubs.ts` file** | ❌ NO |

### 1.2 Golden Rules for ALL Code Written

1. **EVERY backend controller MUST have `@RequestMapping("/api/v1/...")`** — the frontend `axiosInstance.ts` points to `http://localhost:8080/api/v1/` as base URL.
2. **EVERY controller method MUST extract userId from SecurityContextHolder** — use this helper:
   ```java
   private String getUserId() {
       return SecurityContextHolder.getContext().getAuthentication().getName();
   }
   ```
3. **EVERY DTO must use Lombok** (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).
4. **EVERY endpoint that returns a List must return `List<T>` wrapped in `ResponseEntity<List<T>>`** — never an array.
5. **EVERY service method must accept `Long userId`** — to scope data per authenticated user.
6. **NEVER use `@Autowired`** — always constructor injection via `@RequiredArgsConstructor`.
7. **NEVER truncate code** — always write the complete, runnable implementation.
8. **ALWAYS add Google-style docstrings** to every class and public method.
9. **NEVER skip error handling** — every catch block must log and return appropriate HTTP status.
10. **Frontend API calls must use the existing `api/*.ts` pattern** — never create a new axios instance.

---

## 2. CURRENT FILE INVENTORY

### 2.1 Backend — Existing Files (DO NOT DELETE)

```
com/itr/
├── ItrFilingAssistantApplication.java           // Entry point — NO TOUCH
├── config/
│   ├── AsyncConfig.java                          // Thread pool — NO TOUCH
│   ├── AuthController.java                       // Auth REST — in use, OK
│   ├── CacheConfig.java                          // Cache — NO TOUCH
│   ├── DataCleanupRunner.java                    // Startup cleanup — NO TOUCH
│   ├── DataInitializer.java                      // Test users — NO TOUCH
│   ├── JwtAuthenticationFilter.java              // JWT filter — NO TOUCH
│   ├── OpenAPIConfig.java                        // Swagger — NO TOUCH
│   ├── SecurityConfig.java                       // CAN EDIT — add new public routes
│   └── SessionTimeoutHandler.java                // Session mgmt — NO TOUCH
├── domain/
│   ├── advancetax/
│   │   ├── Section234AComputer.java              // CAN USE — static utilities
│   │   ├── Section234BComputer.java
│   │   ├── Section234CComputer.java
│   │   └── Section234FComputer.java
│   ├── businessincome/
│   │   ├── AMTComputer.java
│   │   ├── BusinessType.java
│   │   ├── DepreciationRates.java
│   │   └── PresumptiveIncomeComputer.java
│   ├── capitalgains/
│   │   ├── AssetClass.java
│   │   ├── HoldingPeriodClassifier.java
│   │   ├── IndexedCostCalculator.java
│   │   ├── LTCG112AComputer.java
│   │   ├── STCG111AComputer.java
│   │   └── VDAComputer.java
│   ├── common/
│   │   ├── AgeCategory.java
│   │   ├── AssessmentYear.java                   // SINGLE SOURCE OF TRUTH — all AY 2026-27 constants
│   │   ├── Gender.java
│   │   ├── IndianAmount.java
│   │   ├── PAN.java
│   │   ├── ResidentialStatus.java
│   │   ├── TaxComputationResult.java
│   │   └── TaxRegime.java
│   ├── deductions/
│   │   ├── Chapter6ADeductionEngine.java
│   │   ├── NewRegimeAllowedDeductions.java
│   │   └── Section80G.java
│   ├── houseproperty/
│   │   ├── HPIncomeComputer.java
│   │   ├── HPScheduleComputer.java
│   │   └── PropertyType.java
│   ├── itrselection/
│   │   ├── ITRFormSelectorEngine.java
│   │   └── ITRFormType.java
│   ├── losses/
│   │   ├── LossEntry.java
│   │   ├── LossHead.java
│   │   ├── LossLedgerAggregator.java
│   │   └── SetOffOrderEngine.java
│   └── salary/
│       ├── EmployerEntry.java
│       ├── GratuityExemption.java
│       ├── HRAExemption.java
│       ├── HRAMetroCity.java
│       ├── LeaveEncashmentExemption.java
│       ├── MultiEmployerConsolidator.java
│       ├── SalaryComputationResult.java
│       └── SalaryScheduleComputer.java
├── dto/                                         // CAN EDIT — add new DTOs as needed
│   ├── AISData.java
│   ├── AuthRequest.java
│   ├── AuthResponse.java
│   ├── BusinessIncomeRequest.java
│   ├── BusinessIncomeResponse.java
│   ├── ClientRequest.java
│   ├── ClientResponse.java
│   ├── DocumentMetadata.java                    // DTO version
│   ├── ErrorResponse.java
│   ├── FilingRequest.java
│   ├── FilingResponse.java
│   ├── Form16Data.java
│   ├── Form16PartAData.java
│   ├── Form16PartBData.java
│   ├── Form26ASData.java
│   ├── ITDPrefillData.java
│   ├── PrefillResponse.java
│   ├── ReconciliationReport.java
│   ├── ReconciliationRequest.java
│   ├── ScheduleBFLA.java
│   ├── ScheduleCFL.java
│   ├── ScheduleCYLA.java
│   ├── TaxCalculationDtos.java
│   └── TISData.java
├── entity/                                      // NO TOUCH — JPA entities work
│   ├── AuditTrail.java
│   ├── Client.java
│   ├── ClientYearData.java
│   ├── ITRFiling.java
│   ├── ITRFormData.java
│   ├── LossLedger.java
│   ├── PANValidationCache.java
│   ├── TDSDeductor.java
│   └── User.java
├── exception/                                   // NO TOUCH — GlobalExceptionHandler works
│   ├── BusinessLogicException.java
│   ├── ConflictException.java
│   ├── ForbiddenException.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── infrastructure/security/JwtTokenProvider.java // NO TOUCH
├── model/                                        // NO TOUCH — dead code, leave it
├── repository/                                   // NO TOUCH — all repositories work
│   ├── AuditTrailRepository.java
│   ├── ClientRepository.java
│   ├── ClientYearDataRepository.java
│   ├── DocumentRepository.java
│   ├── ITRFilingRepository.java
│   ├── ITRFormDataRepository.java
│   ├── LossLedgerRepository.java
│   ├── PANValidationRepository.java
│   ├── TDSDeductorRepository.java
│   └── UserRepository.java
├── service/                                      // CAN EDIT + CREATE NEW
│   ├── AISReconciliationService.java            // STUB — CAN EDIT to implement
│   ├── AuditLogHashChainService.java
│   ├── AuthService.java                         // Works
│   ├── CapitalGainsExemptionService.java
│   ├── ClientService.java                       // Works — needs controller
│   ├── DeductionCalculatorService.java
│   ├── DocumentManagementService.java           // Works — needs controller
│   ├── DocumentStorageService.java
│   ├── EncryptionService.java
│   ├── Form16PartAParser.java
│   ├── Form16PartBParser.java
│   ├── ITRFilingService.java                    // Works — needs controller
│   ├── LossCarryForwardService.java
│   ├── LossSetOffEngine.java
│   ├── LTCG112AGrandfatheringService.java
│   ├── PrefillService.java                      // Works — needs controller
│   ├── SalaryExemptionService.java
│   ├── integration/
│   │   ├── AISImportService.java
│   │   ├── AISJsonImportService.java
│   │   ├── Form26ASImportService.java
│   │   └── TISImportService.java
│   └── taxengine/
│       └── InterestCalculator.java
└── util/
    ├── AISJsonDecryptor.java
    ├── DigestCalculator.java
    ├── ITDDateFormatter.java
    ├── ITDPdfDecryptor.java
    ├── PIIMaskingUtil.java
    └── SHA256DigestUtil.java
```

### 2.2 Frontend — Existing Files (DO NOT DELETE)

```
src/
├── main.tsx                              // Bootstrap — NO TOUCH
├── App.tsx                               // Router — CAN EDIT to add routes
├── App.css                               // NO TOUCH
├── index.css                             // NO TOUCH
├── api/                                  // CAN EDIT to connect to real endpoints
│   ├── _stubs.ts                         // NO TOUCH
│   ├── advancedTax.ts
│   ├── apiError.ts
│   ├── auth.ts                           // Works
│   ├── axiosInstance.ts                  // Works
│   ├── billing.ts → uses _stubs          // EDIT to connect
│   ├── clients.ts                        // Already correct
│   ├── communication.ts → uses _stubs    // Phase 2
│   ├── dashboard.ts                      // Already correct
│   ├── documents.ts                      // Already correct
│   ├── filing.ts                         // Already correct
│   ├── integration.ts                    // Already correct
│   ├── itr.ts                            // Already correct
│   ├── jobs.ts → uses _stubs             // Phase 2
│   ├── notices.ts → uses _stubs          // Phase 2
│   ├── pan.ts                            // Already correct
│   ├── reconciliation.ts → uses _stubs   // EDIT
│   └── sync.ts → uses _stubs             // Phase 2
├── components/                           // Works — NO TOUCH
├── contexts/AYContext.tsx                // Works — NO TOUCH
├── pages/
│   ├── AccountingPage.tsx                // Phase 2 — stub
│   ├── AdvancedTaxPage.tsx               // Works (uses advancedTax.ts)
│   ├── BillingPage.tsx                   // Phase 2 — stub
│   ├── CalendarPage.tsx                  // Phase 2 — stub
│   ├── ClientsPage.tsx                   // Works (uses clients.ts, pan.ts) — backend needs controller
│   ├── CommunicationPage.tsx             // Phase 2 — stub
│   ├── DashboardPage.tsx                 // Works (uses dashboard.ts, clients.ts) — backend needs controller
│   ├── FilingPage.tsx                    // Works (uses filing.ts) — backend needs controller
│   ├── ITRComputationPage.tsx            // Works (uses itr.ts, clients.ts, integration.ts) — backend needs controller
│   ├── ITRComputationTabs.tsx            // Works — backend needs controller
│   ├── JobsPage.tsx                      // Phase 2 — stub
│   ├── LoginPage.tsx                     // Works
│   ├── NoticesPage.tsx                   // Phase 2 — stub
│   ├── ReconciliationPage.tsx            // Phase 2 — stub
│   ├── RegisterPage.tsx                  // Works
│   ├── ReportsPage.tsx                   // Phase 2 — stub
│   ├── SyncPage.tsx                      // Phase 2 — stub
│   └── TasksPage.tsx                     // Phase 2 — stub
├── services/                             // Works — NO TOUCH
├── types/                                // Works — NO TOUCH
└── utils/                                // Works — NO TOUCH
```

---

## 3. ARCHITECTURE OVERVIEW

```
┌─────────────────────────────────────────────────────────────────────┐
│                          FRONTEND (React + Vite)                    │
│  port 3000                                                          │
│  src/api/*.ts → axiosInstance → http://localhost:8080/api/v1/       │
├─────────────────────────────────────────────────────────────────────┤
│                          BACKEND (Spring Boot 3.2.3)                │
│  port 8080                                                          │
│                                                                     │
│  SecurityConfig                                                     │
│  ├── Public: /api/v1/auth/**, /swagger-ui/**, /v3/api-docs/**       │
│  └── Authenticated: Everything else (JWT Bearer token required)     │
│                                                                     │
│  REST Controllers (config/*Controller.java)                         │
│  ├── AuthController       ✅ Works                                  │
│  ├── ClientController     ❌ NEEDS CREATION                         │
│  ├── DashboardController  ❌ NEEDS CREATION                         │
│  ├── FilingController     ❌ NEEDS CREATION                         │
│  ├── DocumentController   ❌ NEEDS CREATION                         │
│  ├── PrefillController    ❌ NEEDS CREATION                         │
│  ├── IntegrationController❌ NEEDS CREATION                         │
│  ├── TaxController        ❌ NEEDS CREATION                         │
│  ├── PANController        ❌ NEEDS CREATION                         │
│  └── AdvancedTaxController❌ NEEDS CREATION                         │
│                                                                     │
│  Services → Domain Engines → JPA Repositories → PostgreSQL          │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. PHASE 1 IMPLEMENTATION PLAN (10 STEPS)

### STEP 1: Create REST Controllers (BACKEND)

Create these files in `backend/src/main/java/com/itr/config/` (all follow existing `AuthController.java` pattern):

#### 1a. `ClientController.java`
- **Endpoints:**
  - `GET /api/v1/clients` — list all clients for authenticated user
  - `GET /api/v1/clients/{id}` — get single client with year data
  - `POST /api/v1/clients` — create client (PAN verification)
  - `PUT /api/v1/clients/{id}` — update client
  - `DELETE /api/v1/clients/{id}` — delete client
  - `GET /api/v1/clients/{id}/years` — list assessment years for client
  - `PUT /api/v1/clients/{id}/years/{year}/itr-type` — update ITR type for an AY
- **Uses existing:** `ClientService`, `ClientRequest`, `ClientResponse`
- **Pattern:** `getUserId()` from SecurityContext, convert to Long via `Long.parseLong(getUserId())`

#### 1b. `DashboardController.java`
- **Endpoints:**
  - `GET /api/v1/dashboard/stats` — returns counts: totalClients, filed, inProgress, docPending, watchList, totalMismatches, totalNotices
- **Logic:** Query ClientRepository + ITRFilingRepository to compute stats

#### 1c. `FilingController.java`
- **Endpoints:**
  - `GET /api/v1/filing` — list filings (query params: status, year)
  - `POST /api/v1/filing` — create filing
  - `PUT /api/v1/filing/{id}` — update filing
  - `DELETE /api/v1/filing/{id}` — delete filing
- **Uses existing:** `ITRFilingService`, `FilingRequest`, `FilingResponse`

#### 1d. `DocumentController.java`
- **Endpoints:**
  - `POST /api/v1/documents/upload` — multipart file upload (form: file, clientId, assessmentYear, documentType)
  - `GET /api/v1/documents/list` — list documents (params: clientId, assessmentYear)
  - `GET /api/v1/documents/download/{documentId}` — download document
  - `DELETE /api/v1/documents/{documentId}` — delete document
- **Uses existing:** `DocumentManagementService`

#### 1e. `PrefillController.java`
- **Endpoints:**
  - `POST /api/v1/prefill/26as/upload` — upload Form26AS PDF → parse → save
  - `POST /api/v1/prefill/ais` — upload AIS PDF → parse → save
  - `POST /api/v1/prefill/tis` — upload TIS PDF → parse → save
  - `POST /api/v1/prefill/form16` — upload Form16 PDF → parse → auto-populate
  - `POST /api/v1/prefill/autoPopulateAll` — auto-populate all ITR fields from available data
- **Uses existing:** `AISImportService`, `Form26ASImportService`, `TISImportService`, `Form16PartAParser`, `Form16PartBParser`, `PrefillService`

#### 1f. `IntegrationController.java`
- **Endpoints:**
  - `POST /api/v1/integration/autopopulate/form16` — JSON body with extracted form16 data → auto-populate fields
  - `POST /api/v1/integration/autopopulate/ais` — JSON body with AIS data → auto-populate fields
  - `POST /api/v1/integration/reconciliation` — reconcile AIS vs 26AS vs TIS → return discrepancies
- **Uses existing:** `AISReconciliationService`, `PrefillService`

#### 1g. `TaxController.java`
- **Endpoints:**
  - `GET /api/v1/clients/{clientId}/itr/{year}` — get saved form data
  - `PUT /api/v1/clients/{clientId}/itr/{year}` — save form data
  - `POST /api/v1/clients/{clientId}/itr/{year}/compute` — compute tax for the form data → return full computation result
  - `POST /api/v1/clients/{clientId}/itr/{year}/validate` — validate form data against CBDT rules
  - `GET /api/v1/clients/{clientId}/itr/{year}/download` — download as JSON (ITD schema)
  - `GET /api/v1/clients/{clientId}/itr/{year}/download-pdf` — download computation PDF
  - `POST /api/v1/tax-summary/compute` — compute tax summary (regime comparison)
- **Uses existing:** `ITRFormDataRepository`, `PrefillService`, domain calculators

#### 1h. `PANController.java`
- **Endpoints:**
  - `GET /api/v1/pan/{pan}/validate` — validate PAN format, check against PANValidationCache
  - `GET /api/v1/pan/{pan}/analyze` — analyze PAN (entity type, eligible ITR forms)
- **Uses existing:** `PANValidationRepository`

#### 1i. `AdvancedTaxController.java`
- **Endpoints:**
  - `POST /api/v1/advanced-tax/hra` — compute HRA exemption
  - `POST /api/v1/advanced-tax/section14a` — compute Section 14A disallowance
  - `POST /api/v1/advanced-tax/section50c` — compute Section 50C deemed sale consideration
  - `POST /api/v1/advanced-tax/relief89` — compute relief under Section 89
  - `POST /api/v1/advanced-tax/depreciation` — compute depreciation
  - `GET /api/v1/advanced-tax/depreciation/rates` — get depreciation rates
  - `POST /api/v1/advanced-tax/multi-employer` — compute multi-employer tax
  - `POST /api/v1/advanced-tax/ltcg-grandfathering` — compute LTCG grandfathering
  - `POST /api/v1/advanced-tax/epf-taxation` — compute EPF tax on contributions > ₹2.5L
  - `POST /api/v1/advanced-tax/clubbing/minor-child` — compute clubbing of minor child income
  - `POST /api/v1/advanced-tax/clubbing/spouse` — compute clubbing of spouse income
  - `POST /api/v1/advanced-tax/fo-trading` — compute F&O trading tax
  - `POST /api/v1/advanced-tax/break-even` — compute break-even analysis
- **Uses existing:** `Section234AComputer`, `Section234BComputer`, `Section234CComputer`, `Section234FComputer`, domain calculators
- **Note:** Implement proper business logic in each endpoint — not stubs. Each calculator must match CBDT rules exactly.

### STEP 2: Create ITD Schema JSON Export Service

**Create:** `backend/src/main/java/com/itr/service/ITRJsonExportService.java`

- **Purpose:** Generates ITR-1, ITR-2, ITR-3, ITR-4 JSON files strictly per ITD published schema for AY 2026-27
- **Methods:**
  - `String generateITR1Json(Itr1FormData data)` — returns JSON string matching ITR-1 schema
  - `String generateITR2Json(...)` — ITR-2 schema
  - `String generateITR3Json(...)` — ITR-3 schema
  - `String generateITR4Json(...)` — ITR-4 schema
- **IMPORTANT:** The ITD JSON schema for AY 2026-27 is defined in the CBDT "ITR Schema for AY 2026-27" document. The JSON structure must match:
  - Root: `{ "ITRForm": "ITR-1", "AssessmentYear": "2026-27", "SchemaVersion": "1.0", ... }`
  - All monetary values in paise (Integer)
  - Date format: `DD-MM-YYYY`
  - PAN format: `AAAAA9999A`
  - All mandatory fields must be present even if zero
- **Strategy:** Since Itr1-4FormData.java was deleted in cleanup, create new lightweight form DTOs at `dto/itr/` package OR re-create minimal form data DTOs. Name them as ITD schema expects:
  - `dto/ITR1SchemaData.java` — complete ITR-1 schema mapping
  - `dto/ITR2SchemaData.java` — complete ITR-2 schema mapping
  - `dto/ITR3SchemaData.java` — complete ITR-3 schema mapping
  - `dto/ITR4SchemaData.java` — complete ITR-4 schema mapping

### STEP 3: Create Computation PDF Generation Service

**Create:** `backend/src/main/java/com/itr/service/ComputationPDFService.java`

- **Purpose:** Generate a downloadable PDF showing full tax computation breakdown
- **Uses existing:** iText libraries already in pom.xml (`com.itextpdf:kernel:7.2.5`, `com.itextpdf:layout:7.2.5`)
- **Sections in PDF:**
  1. Header: Assessee Name, PAN, AY, ITR Type, Date
  2. Income Summary: Salary, House Property, Capital Gains, Business/Profession, Other Sources
  3. Gross Total Income
  4. Deductions under Chapter VI-A
  5. Net Taxable Income
  6. Tax Computation:
     - Tax on normal income (slab-wise)
     - Tax on special rate income
     - Surcharge
     - Health & Education Cess (4%)
     - Rebate under Section 87A
     - Total Tax Payable
  7. Relief under Section 89 (if applicable)
  8. TDS/TCS/Advance Tax/Self Assessment Tax summary
  9. Interest under Sections 234A/234B/234C/234F
  10. Net Tax Payable / Refund
- **Method:** `byte[] generateComputationPDF(ITR1SchemaData data, TaxComputationResult result)`

### STEP 4: Create ITD ERI-2 API Integration Service

**Create:** `backend/src/main/java/com/itr/service/integration/ITDERI2Service.java`

- **Purpose:** Integrate with Income Tax Department's ERI-2 (e-Return Intermediary 2.0) API for:
  - Prefill data download (26AS, AIS, TIS)
  - JSON upload for e-filing
  - Verify JSON against ITD schema
- **API Base URL (Sandbox):** `https://eportal.incometax.gov.in/iec/foservices/`
- **API Base URL (Production):** `https://eportal.incometax.gov.in/iec/foservices/`
- **Authentication:**
  - ERI-2 requires API key/secrete (issued by ITD on registration as ERI)
  - API keys stored in application.properties: `itd.api.key`, `itd.api.secret`, `itd.api.base-url`
  - Each request must be signed with HMAC-SHA256 using the secret
- **Endpoints to implement:**
  - `getPrefillData(pan, ay)` → download prefill JSON → decrypt → parse → return `ITDPrefillData`
  - `getForm26AS(pan, ay)` → download 26AS data
  - `getAISData(pan, ay)` → download AIS data
  - `getTISData(pan, ay)` → download TIS data
  - `submitITR(itrJson)` → submit ITR JSON to ITD portal
  - `getSubmissionStatus(acknowledgementNumber)` → check filing status
- **Design:** Create a configuration DTO:
  ```java
  @ConfigurationProperties(prefix = "itd.api")
  public record ITDAPIConfig(String key, String secret, String baseUrl) {}
  ```
- **RETRY LOGIC:** Use Spring Retry with exponential backoff for transient failures
- **RATE LIMITING:** Max 5 requests per minute per PAN (ITD requirement)

### STEP 5: Create Browser Automation for One-Click Import

**Create:** `backend/src/main/java/com/itr/service/integration/ITDPortalAutomationService.java`

- **Purpose:** Automate login to incometax.gov.in → navigate to AIS/26AS/TIS → download on single click
- **Technology:** Use Playwright for Java (`com.microsoft.playwright:playwright:1.40.0`) — add to pom.xml
- **Flow:**
  1. User provides their ITD portal credentials (PAN + Password + DOB)
  2. Service launches headless Playwright browser
  3. Navigates to `https://eportal.incometax.gov.in/`
  4. Logs in with provided credentials
  5. Navigates to "AIS" section
  6. Downloads AIS PDF/JSON
  7. Navigates to "Form 26AS" section
  8. Downloads Form 26AS PDF
  9. Navigates to "TIS" section
  10. Downloads TIS PDF
  11. Returns all downloaded files → feeds into existing import services
- **Security:** Credentials encrypted in memory, destroyed after session
- **IMPORTANT:** This feature must have a clear warning: "Credentials are used only for this session and are NOT stored"

### STEP 6: Create Complete Tax Computation Engine

**Create:** `backend/src/main/java/com/itr/service/TaxComputationOrchestrator.java`

- **Purpose:** Master orchestrator that takes form data → runs all domain calculators → produces `TaxComputationResult`
- **Flow:**
  1. Extract salary data → run `SalaryScheduleComputer` → get net salary income
  2. Extract HP data → run `HPScheduleComputer` → get HP income/loss
  3. Extract CG data → run capital gains calculators → get CG income
  4. Extract business data → run business income calculators → get business income
  5. Extract other sources → sum up
  6. Run `SetOffOrderEngine` → apply CYLA (current year loss adjustment)
  7. Run `Chapter6ADeductionEngine` → compute deductions
  8. Calculate net taxable income
  9. Apply tax slabs per regime (OLD vs NEW)
  10. Apply surcharge per income bracket
  11. Apply Health & Education Cess (4%)
  12. Apply Rebate under Section 87A
  13. Run `LossCarryForwardService` → compute BFLA + CFL
  14. Calculate interest under Sections 234A/234B/234C/234F
  15. Consider TDS/TCS/Advance Tax → compute refund or balance payable
  16. Return `TaxComputationResult`

### STEP 7: Create ITR-1 through ITR-4 Specific Calculation Services

**Create:** `backend/src/main/java/com/itr/service/ITR1ComputationService.java`
**Create:** `backend/src/main/java/com/itr/service/ITR2ComputationService.java`
**Create:** `backend/src/main/java/com/itr/service/ITR3ComputationService.java`
**Create:** `backend/src/main/java/com/itr/service/ITR4ComputationService.java`

- Each service validates form data specific to that ITR type:
  - **ITR-1:** Only salary, one HP, other sources, deductions (NO CG, NO business income)
  - **ITR-2:** Salary, multiple HP, CG, other sources, deductions (NO business income)
  - **ITR-3:** Everything in ITR-2 + business/profession income, depreciation, balance sheet
  - **ITR-4:** Presumptive income under 44AD/44ADA/44AE, simplified balance sheet

**DO NOT DELETE** the existing `domain/` engines — call them from these services.

### STEP 8: Connect Frontend to Real Backend

**EDIT these frontend files to replace stub usage with real API calls:**

| File | Action |
|---|---|
| `pages/ClientsPage.tsx` | Already calls `clientsApi` and `panApi` — ✅ Works once controllers exist |
| `pages/DashboardPage.tsx` | Already calls `dashboardApi` and `clientsApi` — ✅ Works once controllers exist |
| `pages/FilingPage.tsx` | Already calls `filingApi` — ✅ Works once controllers exist |
| `pages/ITRComputationPage.tsx` | Already calls `itrApi`, `clientsApi`, `import()` for integration — ✅ Works once controllers exist |
| `pages/ITRComputationTabs.tsx` | Already calls entry managers + `itrApi` — ✅ Works once controllers exist |
| `api/documents.ts` | Already correct — just needs backend controller |
| `api/billing.ts` | **Phase 2** — leave as stub |
| `api/reconciliation.ts` | **Phase 2** — leave as stub |
| `api/communication.ts` | **Phase 2** — leave as stub |
| `api/jobs.ts` | **Phase 2** — leave as stub |
| `api/notices.ts` | **Phase 2** — leave as stub |
| `api/sync.ts` | **Phase 2** — leave as stub |

**NOTE:** The frontend's `pages/` stubs (`AccountingPage`, `BillingPage`, `CalendarPage`, `CommunicationPage`, `JobsPage`, `NoticesPage`, `ReportsPage`, `SyncPage`, `TasksPage`) — these are Phase 2. They currently show warning banners. Do NOT delete them, do NOT modify them.

### STEP 9: Update SecurityConfig for New Public Endpoints

**EDIT:** `config/SecurityConfig.java`

Add to the `.requestMatchers(...).permitAll()` chain:
```java
.requestMatchers("/api/v1/auth/**", "/api/v1/pan/**", "/api/v1/prefill/**",
    "/api/v1/documents/share/**", "/api/v1/webhook/**",
    "/swagger-ui/**", "/v3/api-docs/**").permitAll()
```

**IMPORTANT:** Do NOT remove any existing permitAll entries. Only add new ones.

### STEP 10: Application Configuration

**EDIT:** `backend/src/main/resources/application.properties`

Add:
```properties
# ITD ERI-2 API Configuration
itd.api.base-url=https://eportal.incometax.gov.in/iec/foservices/
itd.api.key=
itd.api.secret=
itd.api.timeout-ms=30000
itd.api.max-retries=3

# File Upload
app.upload.dir=${user.home}/itr-filing-uploads

# Computation
tax.regime.default=NEW
```

**Note:** Keep the existing properties. Only append.

---

## 5. CBDT TAX RULES — ITR 1-4 (AY 2026-27)

### 5.1 New Tax Regime (Default — Section 115BAC)

| Income Slab (₹) | Rate |
|---|---|
| 0 – 4,00,000 | NIL |
| 4,00,001 – 8,00,000 | 5% |
| 8,00,001 – 12,00,000 | 10% |
| 12,00,001 – 16,00,000 | 15% |
| 16,00,001 – 20,00,000 | 20% |
| 20,00,001 – 24,00,000 | 25% |
| Above 24,00,000 | 30% |

**Rebate u/s 87A:** Full rebate if taxable income ≤ ₹7,00,000 (tax becomes NIL)

### 5.2 Old Tax Regime

| Income Slab (₹) | Rate |
|---|---|
| 0 – 2,50,000 | NIL |
| 2,50,001 – 5,00,000 | 5% |
| 5,00,001 – 10,00,000 | 20% |
| Above 10,00,000 | 30% |

**Senior Citizen (60-79):** Basic exemption ₹3,00,000
**Super Senior (80+):** Basic exemption ₹5,00,000

**Rebate u/s 87A:** Full rebate if taxable income ≤ ₹5,00,000 (₹7,00,000 for new regime)

### 5.3 Surcharge (Applies to both regimes)

| Income Range (₹) | Surcharge Rate |
|---|---|
| Above 50L – 1Cr | 10% |
| Above 1Cr – 2Cr | 15% |
| Above 2Cr – 5Cr | 25% |
| Above 5Cr | 37% |

**Marginal Relief:** Must be computed where surcharge pushes income into higher bracket.

### 5.4 Health & Education Cess

**Flat 4%** on (Income Tax + Surcharge) — applies to both regimes.

### 5.5 House Property Rules

| Item | Old Regime | New Regime |
|---|---|---|
| Self-occupied interest deduction | Max ₹2,00,000 | NIL |
| Let-out property interest | Actual without cap | Actual without cap |
| Standard deduction (30% of NAV) | ✅ Allowed | ✅ Allowed |
| Pre-construction interest (5 year limit) | 1/5th per year for 5 years | 1/5th per year for 5 years |
| HP loss set-off against other heads | Max ₹2,00,000 per year | NOT ALLOWED |
| Unrealized rent deduction | ✅ Allowed | ✅ Allowed |
| Municipal taxes (paid) | ✅ Allowed | ✅ Allowed |

### 5.6 Capital Gains Rules

| Type | Holding Period | Rate (Old Regime) | Rate (New Regime) |
|---|---|---|---|
| Listed Equity STCG | ≤12 months | 15% (was 15%, now 20% from Jul 23 2024) | Same |
| Listed Equity LTCG | >12 months | 10% over ₹1 Lakh | Same |
| Unlisted Shares STCG | ≤24 months | Taxed as per slab | Taxed as per slab |
| Unlisted Shares LTCG | >24 months | 20% with indexation | 20% with indexation |
| Property STCG | ≤24 months | Taxed as per slab | Taxed as per slab |
| Property LTCG | >24 months | 20% with indexation | 20% with indexation |
| VDA (Crypto/NFT) | Any | 30% flat (Section 115BBH) | 30% flat (Section 115BBH) |

### 5.7 Deductions — New Regime (115BAC)

**Only these deductions are allowed:**
- **80CCD(2):** Employer's NPS contribution up to 14% of salary (Central Govt) / 10% (others)
- **80CCH(2):** Employer's contribution to Agnipath scheme
- **80JJAA:** Additional employee cost (manufacturing sector)

### 5.8 Deductions — Old Regime (Full list allowed)

| Section | Max Deduction | Notes |
|---|---|---|
| 80C | ₹1,50,000 | PF, PPF, LIC, ELSS, etc. |
| 80CCC | ₹1,50,000 | Combined with 80C |
| 80CCD(1) | ₹1,50,000 | NPS (employee) — within 80C limit |
| 80CCD(1B) | ₹50,000 | NPS additional (over 80C) |
| 80CCD(2) | Actual | Employer NPS |
| 80D | ₹25,000/₹50,000 | Health insurance (self/senior citizen) |
| 80E | Actual | Education loan interest (max 8 years) |
| 80G | Actual | Donations (varying limits) |
| 80GG | ₹60,000 per year | Rent paid (no HRA) |
| 80TTA | ₹10,000 | Savings account interest |
| 80TTB | ₹50,000 | Senior citizen savings interest |
| 80U | ₹1,25,000 | Disability |
| 24(b) | ₹2,00,000 | Home loan interest (self-occupied) |

---

## 6. ITD JSON SCHEMA — OUTPUT STRUCTURE

### 6.1 ITR-1 Schema (Simplified)

```json
{
  "ITRForm": "ITR-1",
  "AssessmentYear": "2026-27",
  "SchemaVersion": "1.0",
  "PersonalInfo": {
    "PAN": "AAAAA9999A",
    "FirstName": "Test",
    "LastName": "User",
    "FatherName": "Father Name",
    "DOB": "01-01-1990",
    "Aadhaar": "XXXXXXXXXXXX",
    "Mobile": "9876543210",
    "Email": "test@test.com",
    "ResidentialStatus": "RES",
    "IsIndividualOrHUF": true
  },
  "FilingStatus": {
    "ReturnType": "ORIGINAL",
    "BelatedReturn": false,
    "RevisedReturn": false
  },
  "IncomeDetails": {
    "Salary": {
      "GrossSalary": 120000000,
      "ExemptAllowances": 6000000,
      "NetSalary": 114000000,
      "StandardDeduction": 75000,
      "ProfessionalTax": 2500
    },
    "HouseProperty": {
      "TotalRentalIncome": 0,
      "TotalInterestPaid": 200000,
      "IncomeFromHP": -200000
    },
    "OtherSources": {
      "InterestIncome": 50000,
      "TotalOtherSources": 50000
    },
    "GrossTotalIncome": 112000000,
    "Deductions": {
      "ChapterVIA": 150000
    },
    "TotalIncome": 110500000
  },
  "TaxComputation": {
    "TaxOnTotalIncome": 0,
    "Rebate87A": 0,
    "Surcharge": 0,
    "HealthAndEducationCess": 0,
    "TotalTaxPayable": 0,
    "Interest234A": 0,
    "Interest234B": 0,
    "Interest234C": 0,
    "Interest234F": 0,
    "TotalInterest": 0,
    "GrossTaxLiability": 0,
    "TDS": 0,
    "TCS": 0,
    "AdvanceTax": 0,
    "SelfAssessmentTax": 0,
    "NetTaxPayable": 0,
    "RefundAmount": 0
  }
}
```

**All monetary values in paise (Integer).** The existing `IndianAmount` value object (paise-based) should be used for all computations.

### 6.2 ITR-2 Schema (Additional fields over ITR-1)
- Multiple house properties
- Capital Gains (all asset types)
- Schedule AL (Assets & Liabilities)
- Schedule FA (Foreign Assets)
- Schedule FSI (Foreign Source Income)
- Schedule CYLA/BFLA/CFL
- Schedule AMT
- Schedule 80G (with carry forward)

### 6.3 ITR-3 Schema (Additional over ITR-2)
- Schedule BP (Business/Profession)
- Schedule DPM (Depreciation)
- Schedule GST
- Schedule PL (Profit & Loss)
- Schedule BS (Balance Sheet)
- Audit details
- MSME payments
- Section 43B items
- 80JJAA additional employee cost

### 6.4 ITR-4 Schema (Different structure)
- Schedule Presumptive (44AD/44ADA/44AE)
- Simplified Balance Sheet
- GST Registrations
- No Capital Gains
- No Foreign Assets

---

## 7. FRONTEND ROUTE MAP

Current routes in `App.tsx` — these work once backend controllers are created:

| Route | Page Component | Backend API | Status |
|---|---|---|---|
| `/login` | LoginPage | POST /auth/login | ✅ Works |
| `/register` | RegisterPage | POST /auth/register | ✅ Works |
| `/dashboard` | DashboardPage | GET /dashboard/stats | ❌ Needs controller |
| `/clients` | ClientsPage | GET/POST /clients, GET /pan | ❌ Needs controller |
| `/clients/:id` | ClientsPage (edit) | GET/PUT/DELETE /clients/:id | ❌ Needs controller |
| `/filing` | FilingPage | GET/POST/PUT/DELETE /filing | ❌ Needs controller |
| `/itr/:clientId/:year` | ITRComputationPage | GET/PUT /clients/:id/itr/:year | ❌ Needs controller |
| `/advanced-tax` | AdvancedTaxPage | POST /advanced-tax/* | ❌ Needs controller |
| /stub pages | Various stubs | — | Phase 2 |

---

## 8. IMPLEMENTATION ORDER

```
Step 1: REST Controllers (9 files)
  ├── ClientController.java        → Unlocks Client Management
  ├── DashboardController.java     → Unlocks Dashboard
  ├── FilingController.java        → Unlocks Filing
  ├── DocumentController.java      → Unlocks Document Upload
  ├── PrefillController.java       → Unlocks AIS/26AS/TIS Import
  ├── IntegrationController.java   → Unlocks Auto-Populate
  ├── TaxController.java           → Unlocks Tax Computation
  ├── PANController.java           → Unlocks PAN Validation
  └── AdvancedTaxController.java   → Unlocks Advanced Tax

Step 2: ITR JSON Export Service (1 file)
  └── ITRJsonExportService.java    → Enables ITD schema JSON download

Step 3: Computation PDF Generation (1 file)
  └── ComputationPDFService.java   → Enables PDF download

Step 4: ITD ERI-2 API Integration (1 file)
  └── ITDERI2Service.java          → Enables live ITD portal integration

Step 5: Browser Automation (1 file + pom.xml update)
  └── ITDPortalAutomationService.java  → One-click AIS/26AS/TIS import

Step 6: Tax Computation Orchestrator (1 file)
  └── TaxComputationOrchestrator.java  → Central computation engine

Step 7: ITR 1-4 Specific Services (4 files)
  ├── ITR1ComputationService.java
  ├── ITR2ComputationService.java
  ├── ITR3ComputationService.java
  └── ITR4ComputationService.java

Step 8: Frontend verification (connect to real APIs)
  ├── No code changes needed — pages already call API files
  └── Only verify once backend controllers exist

Step 9: SecurityConfig update (edit 1 file)
  └── SecurityConfig.java — add new public routes

Step 10: Application.properties update (edit 1 file)
  └── application.properties — add ITD API config
```

---

## 9. TESTING CHECKLIST

After implementation, verify each scenario:

| # | Scenario | Expected Result |
|---|---|---|
| 1 | Login with test1@test.com / pokemon123 | ✅ 200 with JWT token |
| 2 | GET /dashboard/stats with valid JWT | ✅ 200 with stats JSON |
| 3 | POST /clients with valid client data | ✅ 201 with client object |
| 4 | GET /clients | ✅ 200 with client list |
| 5 | POST /documents/upload with PDF | ✅ 201 with document metadata |
| 6 | POST /prefill/26as/upload with 26AS PDF | ✅ 200 with parsed data |
| 7 | POST /prefill/form16 with Form16 PDF | ✅ 200 with auto-populated data |
| 8 | POST /clients/:id/itr/:year/compute | ✅ 200 with full tax computation |
| 9 | GET /clients/:id/itr/:year/download | ✅ 200 with ITD-schema JSON file |
| 10 | GET /clients/:id/itr/:year/download-pdf | ✅ 200 with PDF file |
| 11 | POST /tax-summary/compute | ✅ 200 with regime comparison |
| 12 | GET /api/v1/pan/AAAAA9999A/validate | ✅ 200 with validation result |
| 13 | Login with bad password | ✅ 401 Unauthorized |
| 14 | Access protected endpoint without token | ✅ 403 Forbidden |

---

## 10. COMMANDS TO RUN AFTER ALL CHANGES

```powershell
# Backend: compile and verify
cd backend
mvn clean compile
# Expected: BUILD SUCCESS (0 errors)

# Backend: run tests
mvn test
# Expected: All tests pass

# Backend: package
mvn clean package -DskipTests
# Expected: JAR at backend/target/itr-filing-assistant-0.0.1-SNAPSHOT.jar

# Backend: run
java -jar target/itr-filing-assistant-0.0.1-SNAPSHOT.jar
# Expected: Starts on port 8080

# Frontend: install and build
cd frontend
npm install
npm run build
# Expected: BUILD SUCCESS (0 errors)

# Frontend: run dev
npm run dev
# Expected: Starts on port 3000
```

---

## 11. FILES SUMMARY — WHAT TO CREATE/EDIT

### CREATE (18 new backend files)

| File | Package | Step |
|---|---|---|
| `config/ClientController.java` | config | Step 1a |
| `config/DashboardController.java` | config | Step 1b |
| `config/FilingController.java` | config | Step 1c |
| `config/DocumentController.java` | config | Step 1d |
| `config/PrefillController.java` | config | Step 1e |
| `config/IntegrationController.java` | config | Step 1f |
| `config/TaxController.java` | config | Step 1g |
| `config/PANController.java` | config | Step 1h |
| `config/AdvancedTaxController.java` | config | Step 1i |
| `service/ITRJsonExportService.java` | service | Step 2 |
| `service/ComputationPDFService.java` | service | Step 3 |
| `service/integration/ITDERI2Service.java` | service.integration | Step 4 |
| `service/integration/ITDPortalAutomationService.java` | service.integration | Step 5 |
| `service/TaxComputationOrchestrator.java` | service | Step 6 |
| `service/ITR1ComputationService.java` | service | Step 7 |
| `service/ITR2ComputationService.java` | service | Step 7 |
| `service/ITR3ComputationService.java` | service | Step 7 |
| `service/ITR4ComputationService.java` | service | Step 7 |

### EDIT (2 existing backend files)

| File | Change | Step |
|---|---|---|
| `config/SecurityConfig.java` | Add new public endpoint patterns | Step 9 |
| `application.properties` | Append ITD API configuration | Step 10 |

### NO TOUCH (do NOT create, edit, or delete)

**All domain/ files** — they contain correct CBDT rules
**All entity/ files** — JPA mappings work correctly
**All repository/ files** — database access works
**All service/ files** — existing services like ClientService, PrefillService, AuthService work
**All dto/ files** — existing DTOs work (you may ADD new DTOs, never change existing)
**All exception/ files** — GlobalExceptionHandler works
**infrastructure/security/** — JwtTokenProvider works
**All util/ files** — utility classes work
**model/** — dead code, leave alone
**backend/pom.xml** — dependencies are sufficient (add Playwright only in Step 5)
**All frontend files under src/api/** — they are already correct for the backend endpoints
**All frontend pages under src/pages/** — they already call the correct API functions
**All frontend components** — they work and render correctly
**frontend/node_modules/** — do not touch

---

## 12. IMPORTANT WARNINGS FOR THE AGENT

1. **DO NOT** modify `application.properties` jwt.secret — it shows as redacted in display but contains a valid key. Leave it unchanged.
2. **DO NOT** modify `DataInitializer.java` — test user accounts must remain.
3. **DO NOT** delete any stub page — they are part of Phase 2 scope.
4. **DO NOT** modify `SecurityConfig.java` except to add new `.requestMatchers()` entries to `permitAll()`.
5. **ALL controllers must go in `config/` package** — the original `interfaces/rest/` package was deleted. This is intentional.
6. **ALL new services must go in `service/` package** — `service/integration/` for integration services.
7. **Frontend axiosInstance has base URL `http://localhost:8080/api/v1/`** — so controller paths must be relative to that. e.g., frontend calls `POST /clients` → backend controller must be `@RequestMapping("/api/v1/clients")`.
8. **The domain layer is complete and CORRECT** — do not rewrite any domain logic. Only use it.
9. **userId extraction:** `SecurityContextHolder.getContext().getAuthentication().getName()` returns email string. Convert to Long via `Long.parseLong()` when passing to services that expect Long userId.
10. **Playwright dependency** (for Step 5) is NOT in pom.xml. Add it when implementing that step.
