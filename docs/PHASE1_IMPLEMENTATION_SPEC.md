# ITR Filing System — Phase 1 Implementation SPEC for Agentic AI

> **Application:** ITR Filing Assistant — AY 2026-27 (FY 2025-26)
> **Target:** ITR-1 (Sahaj), ITR-2, ITR-3, ITR-4 (Sugam) — Filing Ready
> **Branch:** `restructure/cleanup-v1`
> **Current State:** Backend compiles (0 errors), frontend builds (0 errors). Login works. All other pages return 500 — NO REST controllers exist except AuthController.
> **Testing Priority:** CORE TAX CALCULATION must be verified 100% correct before integrating ITD API keys and browser automation. Those are Phase 1 last steps.

---

## 1. RULES — DO NOT VIOLATE

### 1.1 File Modification Rules

| Action | Allowed? |
|---|---|
| **CREATE new files** | ✅ YES — all new controller/service/frontend files |
| **EDIT existing backend files** | ✅ YES — see "Can Edit" list below |
| **EDIT existing frontend files** | ✅ YES — to connect to real endpoints |
| **DELETE files** | ❌ NO — never delete anything |
| **RENAME files** | ❌ NO — never rename anything |
| **Modify pom.xml** | ⚠️ YES — only for Playwright (Step 11) |
| **Modify application.properties** | ⚠️ YES — only for ITD API config (Step 12) |
| **Modify SecurityConfig.java** | ⚠️ YES — only to add new public endpoint routes |
| **Touch infrastructure/security/** | ❌ NO — JwtTokenProvider works |
| **Touch model/ package** | ❌ NO — dead code, leave alone |
| **Modify any `_stubs.ts` file** | ❌ NO |

### 1.2 Golden Rules for ALL Code Written

1. **EVERY backend controller MUST have `@RequestMapping("/api/v1/...")`** — frontend `axiosInstance.ts` has base URL `http://localhost:8080/api/v1/`.
2. **EVERY controller method MUST extract userId from SecurityContextHolder:**
   ```java
   private Long getUserId() {
       String email = SecurityContextHolder.getContext().getAuthentication().getName();
       return Long.parseLong(email); // User ID stored as numeric string in token
   }
   ```
3. **EVERY DTO must use Lombok** (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).
4. **EVERY endpoint that returns a List must return `ResponseEntity<List<T>>`** — never an array.
5. **EVERY service method must accept `Long userId`** — to scope data per authenticated user.
6. **NEVER use `@Autowired`** — always constructor injection via `@RequiredArgsConstructor`.
7. **NEVER truncate code** — always write complete, runnable implementation.
8. **ALWAYS add Google-style docstrings** to every class and public method.
9. **NEVER skip error handling** — every catch block must log and return appropriate HTTP status.
10. **Frontend API calls must use existing `api/*.ts` pattern** — never create a new axios instance.

---

## 2. CURRENT FILE INVENTORY

### 2.1 Backend — Existing Files (DO NOT DELETE)

```
com/itr/
├── ItrFilingAssistantApplication.java           // Entry point — NO TOUCH
├── config/
│   ├── AsyncConfig.java                          // Thread pool — NO TOUCH
│   ├── AuthController.java                       // Auth REST — WORKS
│   ├── CacheConfig.java                          // Cache — NO TOUCH
│   ├── DataCleanupRunner.java                    // Startup cleanup — NO TOUCH
│   ├── DataInitializer.java                      // Test users — NO TOUCH
│   ├── JwtAuthenticationFilter.java             // JWT filter — NO TOUCH
│   ├── OpenAPIConfig.java                        // Swagger — NO TOUCH
│   ├── SecurityConfig.java                       // CAN EDIT — add public routes
│   └── SessionTimeoutHandler.java                // Session mgmt — NO TOUCH
├── domain/                                       // CAN USE — all domain engines work
│   ├── advancetax/
│   │   ├── Section234AComputer.java
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
├── dto/                                         // CAN ADD NEW + EDIT ClientRequest.java — add itdPassword field
│   ├── AISData.java
│   ├── AuthRequest.java
│   ├── AuthResponse.java
│   ├── BusinessIncomeRequest.java
│   ├── BusinessIncomeResponse.java
│   ├── ClientRequest.java                       // CAN EDIT — add ITD password field
│   ├── ClientResponse.java
│   ├── DocumentMetadata.java
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
├── entity/                                      // CAN EDIT — add ITD password fields (Client.java only)
│   ├── AuditTrail.java
│   ├── Client.java                             // CAN EDIT — add ITD password fields
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
├── model/                                        // NO TOUCH — dead code
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
│   ├── AISReconciliationService.java             // STUB — CAN EDIT
│   ├── AuditLogHashChainService.java
│   ├── AuthService.java                        // WORKS
│   ├── CapitalGainsExemptionService.java
│   ├── ClientService.java                      // WORKS — needs controller
│   ├── DeductionCalculatorService.java
│   ├── DocumentManagementService.java           // WORKS — needs controller
│   ├── DocumentStorageService.java
│   ├── EncryptionService.java
│   ├── Form16PartAParser.java
│   ├── Form16PartBParser.java
│   ├── ITRFilingService.java                   // WORKS — needs controller
│   ├── LossCarryForwardService.java
│   ├── LossSetOffEngine.java
│   ├── LTCG112AGrandfatheringService.java
│   ├── PrefillService.java                     // WORKS — needs controller
│   ├── SalaryExemptionService.java
│   ├── integration/
│   │   ├── AISImportService.java               // WORKS — PDF parsing
│   │   ├── AISJsonImportService.java           // WORKS — JSON decrypt
│   │   ├── Form26ASImportService.java          // WORKS — PDF parsing
│   │   └── TISImportService.java               // WORKS — PDF parsing
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
├── api/
│   ├── _stubs.ts                         // NO TOUCH
│   ├── advancedTax.ts                     // Works (needs backend controller)
│   ├── apiError.ts                       // NO TOUCH
│   ├── auth.ts                           // WORKS
│   ├── axiosInstance.ts                  // WORKS
│   ├── billing.ts                        // Phase 2 — leave stub
│   ├── clients.ts                        // Works (needs backend controller)
│   ├── communication.ts                  // Phase 2 — leave stub
│   ├── dashboard.ts                      // Works (needs backend controller)
│   ├── documents.ts                      // Works (needs backend controller)
│   ├── filing.ts                         // Works (needs backend controller)
│   ├── integration.ts                    // Works (needs backend controller)
│   ├── itr.ts                            // Works (needs backend controller)
│   ├── jobs.ts                           // Phase 2 — leave stub
│   ├── notices.ts                        // Phase 2 — leave stub
│   ├── pan.ts                            // Works (needs backend controller)
│   ├── reconciliation.ts                 // Phase 2 — leave stub
│   ├── sync.ts                           // Phase 2 — leave stub
│   └── tokenManager.ts                   // NO TOUCH
├── components/
│   ├── BankInterestEntryManager.tsx       // Works
│   ├── CapitalGainsEntryManager.tsx      // Works
│   ├── DividendEntryManager.tsx           // Works
│   ├── DonationEntryManager.tsx           // Works
│   ├── EmployerEntryManager.tsx           // Works
│   ├── EmployerReconciliationModal.tsx   // Works
│   ├── HousePropertyEntryManager.tsx      // Works
│   ├── IndianNumberInput.tsx              // Works
│   ├── ProtectedRoute.tsx                 // Works
│   ├── ReconciliationModal.tsx           // Works
│   ├── TDSEntryManager.tsx                // Works
│   ├── layout/AppLayout.tsx              // Works
│   ├── layout/Sidebar.tsx                 // Works
│   ├── layout/Topbar.tsx                  // Works
│   └── ui/Badge.tsx, EmptyState.tsx, SkeletonRow.tsx, Spinner.tsx
├── contexts/AYContext.tsx                 // Works — NO TOUCH
├── pages/
│   ├── ClientsPage.tsx                   // Works — EDIT: add ITD password field with Caps Lock warning
│   ├── DashboardPage.tsx                  // Works — backend needs controller
│   ├── FilingPage.tsx                     // Works — backend needs controller
│   ├── ITRComputationPage.tsx            // Works — backend needs controller
│   ├── ITRComputationTabs.tsx            // Works — backend needs controller
│   ├── AdvancedTaxPage.tsx                // Works — backend needs controller
│   ├── LoginPage.tsx                      // WORKS
│   ├── RegisterPage.tsx                  // WORKS
│   ├── AccountingPage.tsx                // Phase 2 — stub
│   ├── BillingPage.tsx                    // Phase 2 — stub
│   ├── CalendarPage.tsx                  // Phase 2 — stub
│   ├── CommunicationPage.tsx             // Phase 2 — stub
│   ├── JobsPage.tsx                      // Phase 2 — stub
│   ├── NoticesPage.tsx                   // Phase 2 — stub
│   ├── ReconciliationPage.tsx            // Phase 2 — stub
│   ├── ReportsPage.tsx                   // Phase 2 — stub
│   ├── SyncPage.tsx                      // Phase 2 — stub
│   └── TasksPage.tsx                     // Phase 2 — stub
├── services/
│   ├── capitalGainsCalculationService.ts  // Works
│   ├── housePropertyCalculationService.ts // Works
│   └── salaryCalculationService.ts       // Works
├── types/
│   ├── api.types.ts                      // Works
│   └── import.types.ts                   // Works
└── utils/formatters.ts                   // Works
```

---

## 3. RESTRUCTURED IMPLEMENTATION PLAN

### ⚠️ CRITICAL: Implementation Order

The plan is structured in **4 sub-phases** to enable testing at each stage. Browser automation and ITD API integration are **LAST STEPS ONLY** — after core tax calculation is verified 100% correct.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          SUB-PHASE A: Foundation                           │
│  Step 1-2: REST Controllers + PAN Controller → Unlocks ALL pages          │
│  TEST HERE: Login → Dashboard → Clients → Filing all working             │
├──────────────────────────────────────────────────────────────────────────────┤
│                          SUB-PHASE B: Core Computation                      │
│  Step 3-4: Tax Computation Orchestrator + ITR 1-4 Services                │
│  TEST HERE: Compute button returns correct tax for all 4 forms            │
├──────────────────────────────────────────────────────────────────────────────┤
│                          SUB-PHASE C: Output Generation                    │
│  Step 5-6: Computation PDF + ITD Schema JSON Export                       │
│  TEST HERE: Download PDF and JSON match manual computation                │
├──────────────────────────────────────────────────────────────────────────────┤
│                          SUB-PHASE D: Manual Import                        │
│  Step 7-8: Prefill/Integration Controllers + Reconciliation               │
│  TEST HERE: Upload 26AS/AIS/TIS PDF → auto-populate → reconcile            │
├──────────────────────────────────────────────────────────────────────────────┤
│                          SUB-PHASE E: ITD Integration (LAST)               │
│  Step 9-10: Browser Automation + ERI-2 API Integration                    │
│  TEST HERE: One-click import + API key upload works                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                          Step 11-12: Config Updates                         │
│  SecurityConfig + application.properties                                  │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. SUB-PHASE A: REST CONTROLLERS (Foundation)

### STEP 1: Create All REST Controllers

**CREATE these files in `backend/src/main/java/com/itr/config/`**

#### 1a. `ClientController.java`
```
Endpoints:
  GET    /api/v1/clients                          → list all clients
  GET    /api/v1/clients/{id}                     → get single client
  POST   /api/v1/clients                          → create client
  PUT    /api/v1/clients/{id}                     → update client
  DELETE /api/v1/clients/{id}                     → delete client
  GET    /api/v1/clients/{id}/years               → list AY years
  PUT    /api/v1/clients/{id}/years/{year}/itr-type → update ITR type for AY
  PUT    /api/v1/clients/{id}/itd-password        → set/update ITD portal password (encrypted)
  DELETE /api/v1/clients/{id}/itd-password        → remove ITD portal password
  GET    /api/v1/clients/{id}/itd-password        → check if password exists (returns boolean, never the password)
  POST   /api/v1/clients/{id}/itd-validate        → validate password by attempting ITD portal login

Uses: ClientService, ClientRequest, ClientResponse, EncryptionService
UserId: get from SecurityContextHolder, convert to Long
```

**ITD Portal Password Management:**
```
Password is encrypted using EncryptionService (AES-256-GCM) before storage.
NEVER stored in plain text. Only encrypted bytes + IV stored in database.

Database fields added to Client entity:
  itdPasswordEncrypted (byte[]) — AES-256-GCM encrypted password
  itdPasswordIv (byte[])         — 12-byte GCM IV for this client's password
  itdPasswordSetAt (OffsetDateTime) — timestamp when password was set

Flow:
  1. User enters password in frontend (with Caps Lock warning)
  2. Frontend sends plain password to PUT /clients/{id}/itd-password
  3. Backend: EncryptionService.encrypt(plainPassword.getBytes(), ITD_MASTER_KEY)
  4. Store: client.setItdPasswordEncrypted(encryptedBytes)
  5. Store: client.setItdPasswordIv(iv)
  6. ClientService.save(client)
  7. GET /clients/{id}/itd-password → returns { "exists": true/false }

CRITICAL SECURITY RULES:
  - Password stored ONLY for the specific PAN of this client
  - Master encryption key stored in application.properties (never in code)
  - GET endpoint NEVER returns the actual password — only boolean exists
  - Encrypted password can only be decrypted using the master key + client's IV
  - DELETE removes both encrypted bytes and IV from database
  - Password is used ONLY by ITDPortalAutomationService for browser login
```

#### 1b. `DashboardController.java`
```
Endpoints:
  GET /api/v1/dashboard/stats?ay=2026-27         → returns stats JSON

Returns:
{
  "totalClients": 5,
  "filed": 2,
  "inProgress": 1,
  "docPending": 1,
  "watchList": 1,
  "totalMismatches": 0,
  "totalNotices": 0,
  "recentClients": [...]
}

Query: ClientRepository.findByUserId() + ITRFilingRepository
```

#### 1c. `FilingController.java`
```
Endpoints:
  GET    /api/v1/filing                          → list filings (?status=filed&year=2026-27)
  POST   /api/v1/filing                          → create filing
  PUT    /api/v1/filing/{id}                     → update filing (status, ackNo, filingDate)
  DELETE /api/v1/filing/{id}                     → delete filing

Uses: ITRFilingService, FilingRequest, FilingResponse
```

#### 1d. `DocumentController.java`
```
Endpoints:
  POST   /api/v1/documents/upload                 → multipart (file, clientId, ay, documentType)
  GET    /api/v1/documents/list?clientId=&ay=    → list documents
  GET    /api/v1/documents/{id}                   → get document metadata
  GET    /api/v1/documents/download/{id}          → download file
  DELETE /api/v1/documents/{id}                   → delete document

Document types: FORM16_PDF, FORM16_JSON, FORM26AS_PDF, AIS_PDF, AIS_JSON, TIS_PDF, OTHER
Uses: DocumentManagementService, DocumentStorageService
```

#### 1e. `PANController.java`
```
Endpoints:
  GET /api/v1/pan/{pan}/validate                  → validate PAN format
  GET /api/v1/pan/{pan}/analyze                    → analyze PAN (entity type, eligible ITR forms)

Returns:
{
  "pan": "AAAAA9999A",
  "valid": true,
  "entityType": "INDIVIDUAL",
  "name": "TEST USER",
  "eligibleITRForms": ["ITR-1", "ITR-2", "ITR-3", "ITR-4"],
  "warnings": []
}

Uses: PANValidationRepository, PAN value object for regex validation
```

#### 1f. `PrefillController.java`
```
Endpoints:
  POST /api/v1/prefill/26as/upload                 → upload 26AS PDF → parse → return parsed data
  POST /api/v1/prefill/ais/upload                  → upload AIS PDF → parse → return parsed data
  POST /api/v1/prefill/tis/upload                  → upload TIS PDF → parse → return parsed data
  POST /api/v1/prefill/form16/upload               → upload Form16 PDF → parse Part A + B → return data
  POST /api/v1/prefill/autoPopulateAll              → populate all fields from available docs
  GET  /api/v1/prefill/status/{clientId}/{ay}      → get prefill status (which docs uploaded, which fields populated)

Multipart: file (PDF), clientId, assessmentYear
Returns: PrefillResponse with status, recommended ITR type, income summary

Uses: AISImportService, Form26ASImportService, TISImportService,
      Form16PartAParser, Form16PartBParser, PrefillService
```

#### 1g. `IntegrationController.java`
```
Endpoints:
  POST /api/v1/integration/autopopulate/form16   → body: Form16Data JSON → populate ITR fields
  POST /api/v1/integration/autopopulate/ais      → body: AISData JSON → populate ITR fields
  POST /api/v1/integration/reconciliation          → body: { form26as, ais, tis } → return discrepancies

Reconciliation returns:
{
  "hasDiscrepancies": true,
  "discrepancies": [
    {
      "field": "salaryTDS",
      "source1": "AIS",
      "value1": 50000,
      "source2": "26AS",
      "value2": 48000,
      "difference": 2000,
      "severity": "HIGH"
    }
  ],
  "summary": { "total": 5, "high": 2, "medium": 1, "low": 2 }
}

Uses: AISReconciliationService, PrefillService
```

#### 1h. `TaxController.java`
```
Endpoints:
  GET  /api/v1/clients/{clientId}/itr/{ay}        → get saved ITR form data
  PUT  /api/v1/clients/{clientId}/itr/{ay}        → save ITR form data
  POST /api/v1/clients/{clientId}/itr/{ay}/compute → compute tax → return TaxComputationResult
  POST /api/v1/clients/{clientId}/itr/{ay}/validate → validate → return validation errors
  GET  /api/v1/clients/{clientId}/itr/{ay}/download → download ITD-schema JSON
  GET  /api/v1/clients/{clientId}/itr/{ay}/download-pdf → download computation PDF
  POST /api/v1/tax-summary/compute                 → regime comparison (OLD vs NEW)

Validation endpoint returns:
{
  "valid": false,
  "errors": [
    { "field": "salary.grossSalary", "code": "MANDATORY_FIELD", "message": "Gross salary is mandatory" },
    { "field": "houseProperty[0].interest", "code": "INVALID_AMOUNT", "message": "Interest cannot be negative" }
  ],
  "warnings": [
    { "field": "deductions.80C", "code": "LIMIT_EXCEEDED", "message": "80C limit is Rs.1,50,000" }
  ]
}

Uses: ITRFormDataRepository, TaxComputationOrchestrator (Step 4),
      ITRJsonExportService (Step 5), ComputationPDFService (Step 6)
```

#### 1i. `AdvancedTaxController.java`
```
Endpoints:
  POST /api/v1/advanced-tax/hra                  → HRA exemption calculation
  POST /api/v1/advanced-tax/section14a            → Section 14A disallowance
  POST /api/v1/advanced-tax/section50c            → Section 50C deemed sale
  POST /api/v1/advanced-tax/relief89              → Section 89 relief
  POST /api/v1/advanced-tax/depreciation          → depreciation computation
  GET  /api/v1/advanced-tax/depreciation/rates    → IT Act Section 32 rates
  POST /api/v1/advanced-tax/multi-employer        → multi-employer consolidation
  POST /api/v1/advanced-tax/ltcg-grandfathering   → LTCG 112A grandfathering
  POST /api/v1/advanced-tax/epf-taxation          → EPF tax on contributions > 2.5L
  POST /api/v1/advanced-tax/clubbing/minor-child  → minor child income clubbing
  POST /api/v1/advanced-tax/clubbing/spouse       → spouse income clubbing
  POST /api/v1/advanced-tax/fo-trading            → F&O trading tax
  POST /api/v1/advanced-tax/break-even             → break-even analysis

Every endpoint must implement FULL CBDT rules — not stubs.
```

---

## 5A. ITD PORTAL PASSWORD — ENCRYPTED STORAGE

### Overview

When managing clients, an **ITD Portal Password** can be stored for each client (PAN-specific). This password is used exclusively for the browser automation service (`ITDPortalAutomationService`) to log into `incometax.gov.in` on behalf of the client and download AIS/26AS/TIS data.

**Security Requirements:**
- Password is NEVER stored in plain text
- Password is encrypted using **AES-256-GCM** (via existing `EncryptionService`)
- Encryption key is stored in `application.properties` as `itd.master.key`
- Each client's password uses a unique IV (Initialization Vector)
- Password is scoped to the **specific PAN** of the client — cannot be used for any other PAN
- Password is **never exposed** via any API endpoint — only existence is checkable
- Password is **deleted** when client is deleted

### Database Changes

**EDIT:** `backend/src/main/java/com/itr/entity/Client.java`

Add the following fields to the Client entity:

```java
@Column(name = "itd_password_encrypted")
private byte[] itdPasswordEncrypted; // AES-256-GCM encrypted password bytes

@Column(name = "itd_password_iv")
private byte[] itdPasswordIv; // 12-byte GCM IV for this client's password

@Column(name = "itd_password_set_at")
private OffsetDateTime itdPasswordSetAt; // When password was last set
```

**EDIT:** `backend/src/main/java/com/itr/dto/ClientRequest.java`

Add optional ITD password field (used in create and update):

```java
private String itdPassword; // Plain text from frontend — encrypt before storing
```

**EDIT:** `backend/src/main/java/com/itr/service/ClientService.java`

Add methods for password management:

```java
/**
 * Sets the ITD portal password for a client.
 * Password is encrypted using AES-256-GCM before storage.
 * @param clientId   the client database ID
 * @param password   plain text ITD portal password
 * @param userId     the authenticated user ID (for authorization)
 * @throws ResourceNotFoundException if client not found
 * @throws ForbiddenException if client belongs to different user
 */
public void setItdPassword(Long clientId, String password, Long userId) {
    Client client = clientRepository.findByIdAndUserId(clientId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

    // Encrypt password using master key
    byte[] plainBytes = password.getBytes(StandardCharsets.UTF_8);
    EncryptionService.EncryptedData encrypted =
        encryptionService.encrypt(plainBytes, masterEncryptionKey);

    client.setItdPasswordEncrypted(encrypted.getEncryptedBytes());
    client.setItdPasswordIv(encrypted.getIv());
    client.setItdPasswordSetAt(OffsetDateTime.now());
    clientRepository.save(client);
}

/**
 * Removes the ITD portal password for a client.
 */
public void removeItdPassword(Long clientId, Long userId) {
    Client client = clientRepository.findByIdAndUserId(clientId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
    client.setItdPasswordEncrypted(null);
    client.setItdPasswordIv(null);
    client.setItdPasswordSetAt(null);
    clientRepository.save(client);
}

/**
 * Checks if ITD password exists for a client. Returns boolean only.
 * NEVER returns the actual password.
 */
public boolean hasItdPassword(Long clientId, Long userId) {
    Client client = clientRepository.findByIdAndUserId(clientId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
    return client.getItdPasswordEncrypted() != null;
}

/**
 * Retrieves the decrypted ITD password for a client.
 * INTERNAL USE ONLY — called only by ITDPortalAutomationService.
 * @return the decrypted plain text password
 */
public String getDecryptedItdPassword(Long clientId, Long userId) {
    Client client = clientRepository.findByIdAndUserId(clientId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

    if (client.getItdPasswordEncrypted() == null) {
        return null;
    }

    byte[] decrypted = encryptionService.decrypt(
        client.getItdPasswordEncrypted(),
        masterEncryptionKey,
        client.getItdPasswordIv()
    );
    return new String(decrypted, StandardCharsets.UTF_8);
}
```

**application.properties update:**

```properties
# ITD Portal Password Encryption
itd.master.key=<base64-encoded-256-bit-key>  # Generate: openssl rand -base64 32
```

### ClientController Endpoints (already in Step 1a)

```
PUT    /api/v1/clients/{id}/itd-password        → set/update password (body: { "password": "xxx" })
DELETE /api/v1/clients/{id}/itd-password        → remove password
GET    /api/v1/clients/{id}/itd-password        → check exists (returns { "exists": true/false })
POST   /api/v1/clients/{id}/itd-validate        → validate password (optional, tests ITD portal login)
```

**PUT /clients/{id}/itd-password body:**
```json
{
  "password": "UserPlainTextPassword123"
}
```

**Response:**
```json
{ "success": true, "message": "ITD portal password saved securely", "setAt": "2026-06-10T12:00:00+05:30" }
```

**GET /clients/{id}/itd-password response:**
```json
{ "exists": true }
```

### Frontend: Add Client Form — ITD Password Field

**EDIT:** `frontend/src/pages/ClientsPage.tsx`

Add an **optional collapsible section** in the "Add Client" / "Edit Client" modal/form:

```
┌─────────────────────────────────────────────────────┐
│ ☐ Save ITD Portal Password (Optional)                 │
│                                                     │
│   ⚠️  This password is used only for the           │
│       browser automation to import AIS/26AS/TIS     │
│       on your behalf. It is stored encrypted        │
│       and never shared with anyone.                 │
│                                                     │
│   Password: [••••••••••••••••]  ← show Caps Lock warning if ON
│   Confirm:  [••••••••••••••••]                       │
│                                                     │
│   🔒 Stored encrypted — AES-256-GCM                 │
│   🔑 Scoped to PAN: AAAAA9999A                      │
│   🗑️  Can be removed anytime                        │
└─────────────────────────────────────────────────────┘
```

**Caps Lock Detection (React):**
```tsx
const [capsLockWarning, setCapsLockWarning] = useState(false);

// On password input change
const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const isCapsLockOn = e.getModifierState('CapsLock');
    setCapsLockWarning(isCapsLockOn);
};

// In the form field
{capsLockWarning && (
    <div style={{color: '#ef4444', fontSize: '12px', fontWeight: 500}}>
        ⚠️ Caps Lock is ON — password may be entered in uppercase
    </div>
)}
```

**API call:**
```tsx
// When saving client
await clientsApi.updateClient(clientId, {
    ...formData,
    itdPassword: passwordField.value || undefined  // only send if provided
});
```

**Password field in form:**
```tsx
// Collapsible section
const [saveItdPassword, setSaveItdPassword] = useState(false);
const [itdPassword, setItdPassword] = useState('');
const [itdPasswordConfirm, setItdPasswordConfirm] = useState('');

{ saveItdPassword && (
    <div className="itd-password-section" style={{marginTop: '12px', padding: '12px', background: '#fef3c7', borderRadius: '8px'}}>
        <label>
            <input type="checkbox" checked={saveItdPassword}
                onChange={(e) => setSaveItdPassword(e.target.checked)} />
            Save ITD Portal Password (Optional)
        </label>
        {saveItdPassword && (
            <>
                <p style={{fontSize: '11px', color: '#92400e', margin: '4px 0'}}>
                    🔒 This password is encrypted and stored only for PAN: {pan}
                </p>
                <input
                    type="password"
                    placeholder="ITD Portal Password"
                    value={itdPassword}
                    onChange={(e) => {
                        setItdPassword(e.target.value);
                        setCapsLockWarning(e.target.getModifierState('CapsLock'));
                    }}
                    style={{width: '100%', marginTop: '8px'}}
                />
                {capsLockWarning && (
                    <div style={{color: '#dc2626', fontSize: '12px', fontWeight: 600, marginTop: '4px'}}>
                        ⚠️ CAPS LOCK IS ON — password may be incorrect
                    </div>
                )}
                <input
                    type="password"
                    placeholder="Confirm Password"
                    value={itdPasswordConfirm}
                    onChange={(e) => setItdPasswordConfirm(e.target.value)}
                    style={{width: '100%', marginTop: '8px'}}
                />
                {itdPassword !== itdPasswordConfirm && itdPasswordConfirm && (
                    <div style={{color: '#dc2626', fontSize: '12px', marginTop: '4px'}}>
                        Passwords do not match
                    </div>
                )}
            </>
        )}
    </div>
)}
```

### Password Usage in Browser Automation

When `ITDPortalAutomationService` needs to log in to ITD portal for a specific client:

```java
// Inside ITDPortalAutomationService
String plainPassword = clientService.getDecryptedItdPassword(clientId, userId);
// Use plainPassword for Playwright login
// After session ends, plainPassword reference is nulled
```

### Security Notes

1. **Master Key:** `itd.master.key` in `application.properties` must be a 256-bit random key. Never commit this to git. Add to `.gitignore`.
2. **IV per client:** Each client has a unique IV. Compromising one client's IV does not reveal another client's password.
3. **No plain text logs:** Password field in logs must be masked: `password=******`.
4. **Session-only decryption:** Password is decrypted only during the browser automation session and immediately used, then the reference is nulled.
5. **User consent required:** User must explicitly check the checkbox and enter password. Not auto-saved.
6. **No cross-PAN use:** The encrypted password is tied to the specific PAN in the Client entity. It cannot be used for any other PAN.
7. **Encryption standard:** AES-256-GCM with 12-byte IV and 128-bit authentication tag — same as the existing document encryption service.

---

## 5. SUB-PHASE B: Core Tax Computation

### STEP 2: Create Tax Computation Orchestrator

**CREATE:** `backend/src/main/java/com/itr/service/TaxComputationOrchestrator.java`

This is the **heart of the application**. It must implement the complete tax computation flow for ALL four ITR forms.

```java
@Service
@RequiredArgsConstructor
public class TaxComputationOrchestrator {

    private final SalaryScheduleComputer salaryScheduleComputer;
    private final HPScheduleComputer hpScheduleComputer;
    private final CapitalGainsExemptionService capitalGainsExemptionService;
    private final BusinessIncomeService businessIncomeService;
    private final SetOffOrderEngine setOffOrderEngine;
    private final Chapter6ADeductionEngine chapter6ADeductionEngine;
    private final LossCarryForwardService lossCarryForwardService;
    private final InterestCalculator interestCalculator;
    private final PrefillService prefillService;

    /**
     * Computes complete tax for a given client and assessment year.
     * Called from TaxController.compute() endpoint.
     *
     * @param clientId      the client database ID
     * @param assessmentYear e.g. "2026-27"
     * @param regime        OLD or NEW tax regime
     * @return TaxComputationResult with all income heads, deductions, tax, surcharge, cess
     */
    public TaxComputationResult computeTax(Long clientId, String assessmentYear, TaxRegime regime) {
        // Step 1: Get form data from ITRFormDataRepository
        // Step 2: Run SalaryScheduleComputer → netSalaryIncome
        // Step 3: Run HPScheduleComputer → hpIncome/loss
        // Step 4: Run CG calculators (LTCG, STCG, VDA) → cgIncome
        // Step 5: Run business income calculators → businessIncome
        // Step 6: Sum other sources → otherSourcesIncome
        // Step 7: Run SetOffOrderEngine (CYLA) → adjusted income per head
        // Step 8: Run Chapter6ADeductionEngine → totalDeductions
        // Step 9: Calculate gross total income
        // Step 10: Apply tax slabs per regime
        // Step 11: Apply rebate 87A
        // Step 12: Apply surcharge
        // Step 13: Apply H&E Cess (4%)
        // Step 14: Run LossCarryForwardService (BFLA + CFL) if loss remains
        // Step 15: Calculate interest 234A/234B/234C/234F
        // Step 16: Compute TDS/TCS/AdvanceTax/SelfAssessmentTax → refund or balance
        // Step 17: Return TaxComputationResult
    }
}
```

### STEP 3: Create ITR 1-4 Specific Computation Services

**CREATE:** `backend/src/main/java/com/itr/service/ITR1ComputationService.java`
```
Purpose: ITR-1 (Sahaj) specific computation
Eligibility: Salary + at most ONE house property + other sources. NO CG, NO business.
Validates:
  - No capital gains entries
  - At most one house property
  - ITR type can only be ITR-1
Calls: TaxComputationOrchestrator
Returns: TaxComputationResult
```

**CREATE:** `backend/src/main/java/com/itr/service/ITR2ComputationService.java`
```
Purpose: ITR-2 specific computation
Eligibility: Salary + multiple house properties + capital gains + other sources. NO business.
Validates:
  - Business income must be zero
  - If CG present, must be eligible ITR-2
Calls: TaxComputationOrchestrator
Returns: TaxComputationResult
Also handles: Schedule AL, FA, FSI, AMT, 80G
```

**CREATE:** `backend/src/main/java/com/itr/service/ITR3ComputationService.java`
```
Purpose: ITR-3 specific computation
Eligibility: Everything in ITR-2 + business/profession income
Validates:
  - Business income > 0 allowed
  - Schedule BP, DPM, GST, PL, BS required
Calls: TaxComputationOrchestrator
Returns: TaxComputationResult
Also handles: Depreciation (Section 32), Section 43B, MSME payments, Audit details
```

**CREATE:** `backend/src/main/java/com/itr/service/ITR4ComputationService.java`
```
Purpose: ITR-4 (Sugam) specific computation
Eligibility: Presumptive income under 44AD/44ADA/44AE
Validates:
  - No capital gains
  - No foreign assets
  - Schedule Presumptive must be filled
Calls: TaxComputationOrchestrator
Returns: TaxComputationResult
Also handles: Simplified Balance Sheet, GST Registrations
```

---

## 6. SUB-PHASE C: Output Generation

### STEP 4: Create ITD Schema JSON Export Service

**CREATE:** `backend/src/main/java/com/itr/service/ITRJsonExportService.java`

**Critical:** The JSON must strictly match the ITD published schema for AY 2026-27. Download the official schema from `https://www.incometax.gov.in/iec/foservices/` and map every field.

```
Methods:
  String generateITR1Json(ITR1FormData data)   // ITR-1 schema
  String generateITR2Json(ITR2FormData data)   // ITR-2 schema
  String generateITR3Json(ITR3FormData data)   // ITR-3 schema
  String generateITR4Json(ITR4FormData data)   // ITR-4 schema

All monetary values in PAISE (Integer).
Date format: DD-MM-YYYY
PAN format: AAAAA9999A
```

**CREATE form data DTOs** at `dto/itr/` package:

```
dto/itr/ITR1FormData.java    — complete ITR-1 schema (Sahaj)
dto/itr/ITR2FormData.java    — complete ITR-2 schema
dto/itr/ITR3FormData.java    — complete ITR-3 schema
dto/itr/ITR4FormData.java    — complete ITR-4 schema (Sugam)

Each must have:
  - All mandatory fields (even if zero/null)
  - All optional fields (populate if available)
  - Tax computation section
  - TDS/TCS section
  - Tax paid section
  - Verification info
  - CreationInfo (digest, timestamp)
```

### STEP 5: Create Computation PDF Generation Service

**CREATE:** `backend/src/main/java/com/itr/service/ComputationPDFService.java`

Uses existing iText libraries in pom.xml (`com.itextpdf:kernel:7.2.5`, `com.itextpdf:layout:7.2.5`).

```
Methods:
  byte[] generateComputationPDF(ITRFormData data, TaxComputationResult result)

PDF Structure:
  1. HEADER: Assessee Name, PAN, AY, ITR Type, Date, Acknowledgement
  2. INCOME SUMMARY TABLE:
     | Head             | Amount (Rs.) |
     |------------------|-------------|
     | Salary           | 12,00,000   |
     | House Property   | -2,00,000   |
     | Capital Gains    | 50,000      |
     | Business         | 0           |
     | Other Sources    | 5,000       |
     | Gross Total     | 10,55,000   |
  3. DEDUCTIONS TABLE:
     | Section | Amount |
     | 80C     | 1,50,000 |
     | 80D     | 25,000 |
     | 80G     | 10,000 |
     | Chapter VI-A Total | 1,85,000 |
  4. TAX COMPUTATION:
     - Net Taxable Income
     - Tax slabs breakdown (with rate and amount)
     - Rebate 87A
     - Surcharge
     - H&E Cess (4%)
     - Total Tax Payable
  5. TAX PAID:
     - TDS (employer-wise)
     - TCS
     - Advance Tax
     - Self Assessment Tax
  6. INTEREST:
     - 234A / 234B / 234C / 234F with calculation
  7. NET TAX PAYABLE / REFUND
  8. BANK DETAILS (if refund)
  9. SIGNATURE + VERIFICATION
```

---

## 7. SUB-PHASE D: Manual PDF/JSON Import

### STEP 6: Enhance IntegrationController with Full Reconciliation

The IntegrationController (from Step 1g) already has the reconciliation endpoint. Ensure it implements:

```
POST /api/v1/integration/reconciliation
  Input: { form26as: Form26ASData, ais: AISData, tis: TISData }
  Output: ReconciliationReport with severity levels

Severity mapping:
  HIGH   — TDS mismatch > Rs.500, Salary mismatch > Rs.1000, missing major income
  MEDIUM — Minor field mismatches, missing supporting documents
  LOW    — Formatting differences, non-material variations

Each discrepancy must show:
  - field: full JSON path (e.g., "salary.tds.section194A[0].amount")
  - aisValue: value from AIS
  - form26asValue: value from Form 26AS
  - difference: absolute difference
  - severity: HIGH/MEDIUM/LOW
  - suggestion: "Verify with employer TDS certificate"
```

### STEP 7: Create Manual Import Flow

For each document type, the flow is:

```
User uploads PDF → PrefillController → Service parses PDF → Data stored in ClientYearData.formData JSON → IntegrationController auto-populates fields → User reviews and corrects → TaxController computes
```

**Key flow for Form16:**
```
1. User uploads Form16 PDF
2. PrefillController.post("/form16/upload")
3. Form16PartAParser extracts: employer TAN, name, address, salary, TDS, quarterly details
4. Form16PartBParser extracts: all Section 17(1) components, deductions u/s 16
5. Form16Data object created
6. IntegrationController.post("/autopopulate/form16") → maps to ITR form fields
7. User sees pre-populated salary section with employer name, TAN, all allowances
8. User corrects if needed → TaxController computes
```

**Key flow for 26AS/AIS/TIS:**
```
1. User uploads AIS PDF → AISImportService decrypts + parses → AISData
2. User uploads 26AS PDF → Form26ASImportService decrypts + parses → Form26ASData
3. User uploads TIS PDF → TISImportService decrypts + parses → TISData
4. IntegrationController.post("/reconciliation") → compare all three
5. Show discrepancies with severity
6. User resolves discrepancies
7. Auto-populate from AIS as primary source, 26AS for TDS verification
```

---

## 8. SUB-PHASE E: ITD Integration (LAST — After Core Verified)

### STEP 8: Create ITD ERI-2 API Integration Service

**CREATE:** `backend/src/main/java/com/itr/service/integration/ITDERI2Service.java`

**DO NOT implement this until Steps 1-7 are tested and working.**

```
Purpose: Integrate with ITD ERI-2 (e-Return Intermediary 2.0) API

API Base URL: https://eportal.incometax.gov.in/iec/foservices/
Authentication: API key + HMAC-SHA256 signature per ITD spec

Configuration (application.properties):
  itd.api.key=<your-api-key>
  itd.api.secret=<your-secret>
  itd.api.base-url=https://eportal.incometax.gov.in/iec/foservices/
  itd.api.timeout-ms=30000
  itd.api.max-retries=3

Methods to implement:
  1. getPrefillData(pan: String, ay: String): ITDPrefillData
     → GET /prefill/data?pan=XXX&ay=2026-27
     → Decrypt response → parse JSON → return ITDPrefillData

  2. getForm26AS(pan: String, ay: String): Form26ASData
     → GET /form26as/download?pan=XXX&ay=2026-27
     → Return parsed Form26ASData

  3. getAISData(pan: String, ay: String): AISData
     → GET /ais/download?pan=XXX&ay=2026-27
     → Return parsed AISData

  4. getTISData(pan: String, ay: String): TISData
     → GET /tis/download?pan=XXX&ay=2026-27
     → Return parsed TISData

  5. submitITR(itrJson: String, itrType: String): SubmissionResult
     → POST /itr/submit with signed JSON
     → Return acknowledgement number + status

  6. getSubmissionStatus(ackNo: String): FilingStatus
     → GET /itr/status?ackNo=XXX
     → Return filed/pending/rejected status

Design:
  @ConfigurationProperties(prefix = "itd.api")
  public record ITDAPIConfig(String key, String secret, String baseUrl, int timeoutMs, int maxRetries) {}

  Retry with exponential backoff (Spring Retry)
  Rate limiting: max 5 requests/minute per PAN
  All responses logged for debugging
```

### STEP 9: Create Browser Automation Service

**CREATE:** `backend/src/main/java/com/itr/service/integration/ITDPortalAutomationService.java`

**DO NOT implement this until Steps 1-8 are tested and working.**

```
Purpose: One-click import of AIS/26AS/TIS via browser automation

Requires pom.xml update (Step 12):
  <dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.40.0</version>
  </dependency>

Flow:
  1. User provides ITD portal credentials (PAN + Password + DOB)
     ⚠️ WARNING: "Credentials used only for this session, NOT stored"
  2. Launch headless Playwright browser (Chromium)
  3. Navigate to https://eportal.incometax.gov.in/
  4. Login with provided credentials
  5. Navigate to "AIS" → Download AIS PDF/JSON
  6. Navigate to "Form 26AS" → Download 26AS PDF
  7. Navigate to "TIS" → Download TIS PDF
  8. Close browser, destroy credentials from memory
  9. Return downloaded files → feed into existing import services

Security:
  - Credentials passed as method parameters, not stored anywhere
  - Browser runs headless, no UI shown
  - Session destroyed immediately after download
  - All navigation steps logged for debugging
```

---

## 9. ITD VALIDATIONS AND MANDATORY FIELDS

This section is the **AUTHORITATIVE SOURCE** for all field-level validations that must be enforced when generating the ITD JSON and validating form data. These are derived from CBDT's "ITR Schema Validation Rules for AY 2026-27" and ITD's e-filing portal requirements.

### 9.1 Personal Information Validations

| Field | Rule | Error Code | Error Message |
|---|---|---|---|
| PAN | Must match `^[A-Z]{5}[0-9]{4}[A-Z]$` | `INVALID_PAN` | PAN format is invalid |
| PAN | Must exist in user's client record | `PAN_MISMATCH` | PAN does not match registered client |
| FirstName | Max 30 chars, no special chars except space | `INVALID_NAME` | First name contains invalid characters |
| LastName | Max 30 chars, no special chars except space | `INVALID_NAME` | Last name contains invalid characters |
| FatherName | Max 30 chars | `MANDATORY_FIELD` | Father's name is mandatory |
| DOB | Must be valid date, age ≥ 18 | `INVALID_DOB` | Date of birth is invalid or assessee is minor |
| Aadhaar | Must be 12 digits or null | `INVALID_AADHAAR` | Aadhaar number is invalid |
| Aadhaar | If provided, last 4 digits must match | `AADHAAR_MISMATCH` | Aadhaar does not match |
| Mobile | Must be 10 digits, starting with 6-9 | `INVALID_MOBILE` | Mobile number is invalid |
| Email | Must be valid email format | `INVALID_EMAIL` | Email address is invalid |
| ResidentialStatus | Must be RES/NOR/NRI | `INVALID_RES_STATUS` | Residential status is invalid |
| Aadhaar Enrollment No | Required if Aadhaar not linked | `MANDATORY_FIELD` | Aadhaar enrollment number required |
| Bank Account | IFSCCode valid 11-char format | `INVALID_IFSC` | IFSC code is invalid |

### 9.2 Filing Status Validations

| Field | Rule | Error Code |
|---|---|---|
| ReturnType | ORIGINAL / REVISED / DEFECTIVE | `INVALID_RETURN_TYPE` |
| Acknowledgement No | Required if revised, 15-digit format | `INVALID_ACK_NO` |
| Date of Filing | Must be within AY (Apr 1 2026 to Mar 31 2027) | `INVALID_DATE` |
| Section | Required if belated return | `MANDATORY_FIELD` |
| Notice No | Required if under scrutiny | `MANDATORY_FIELD` |

### 9.3 Salary Income Validations

| Field | Rule | Error Code | Error Message |
|---|---|---|---|
| GrossSalary | ≥ 0, in paise | `INVALID_AMOUNT` | Gross salary cannot be negative |
| ExemptAllowances | ≤ GrossSalary | `EXCEEDS_LIMIT` | Exempt allowances cannot exceed gross salary |
| StandardDeduction | ≤ 75,000 (FY 2025-26) | `EXCEEDS_LIMIT` | Standard deduction exceeds limit of Rs.75,000 |
| ProfessionalTax | ≤ 2,500 | `EXCEEDS_LIMIT` | Professional tax exceeds limit of Rs.2,500 |
| Employer TAN | Required if salary > 0, 10-char format | `MANDATORY_FIELD` | Employer TAN is mandatory |
| Employer Name | Required if salary > 0 | `MANDATORY_FIELD` | Employer name is mandatory |
| Nature of Employment | Required if salary > 0 | `MANDATORY_FIELD` | Nature of employment is mandatory |
| Salary from previous employer | Must have TAN, income ≤ 0 if no previous | `INVALID_ENTRY` | Previous employer salary entry is invalid |

### 9.4 House Property Validations

| Field | Rule | Error Code | Error Message |
|---|---|---|---|
| PropertyType | SELF_OCCUPIED / LET_OUT / DEEMED | `INVALID_PROPERTY_TYPE` | Property type is invalid |
| AnnualValue | ≥ 0 | `INVALID_AMOUNT` | Annual value cannot be negative |
| MunicipalTaxesPaid | ≤ AnnualValue, ≤ Actual Paid | `EXCEEDS_LIMIT` | Municipal taxes exceed annual value |
| InterestOnBorrowedCapital | ≥ 0 | `INVALID_AMOUNT` | Interest cannot be negative |
| SO Interest (Old Regime) | ≤ 2,00,000 | `EXCEEDS_LIMIT` | Self-occupied interest exceeds Rs.2,00,000 |
| SO Interest (New Regime) | Must be 0 | `NEW_REGIME_NOT_ALLOWED` | Self-occupied interest not allowed in new regime |
| Arrears/UnrealizedRent | ≥ 0, in paise | `INVALID_AMOUNT` | Arrears of rent cannot be negative |
| HP Loss Set-off (Old) | ≤ 2,00,000 per year | `EXCEEDS_LIMIT` | HP loss set-off exceeds Rs.2,00,000 |
| Pre-construction interest | Only first 5 years of loan, 1/5th per year | `INVALID_PERIOD` | Pre-construction interest period is invalid |

### 9.5 Capital Gains Validations

| Field | Rule | Error Code | Error Message |
|---|---|---|---|
| PurchaseDate | Must be before sale date | `INVALID_DATE` | Purchase date cannot be after sale date |
| SaleDate | Must be within AY | `INVALID_DATE` | Sale date must be within AY 2026-27 |
| CostOfImprovement | ≥ 0 | `INVALID_AMOUNT` | Cost of improvement cannot be negative |
| IndexedCostOfAcquisition | ≥ ActualCost (after CII indexation) | `INVALID_COST` | Indexed cost must be ≥ actual cost |
| STCG Rate (Equity) | 20% (Finance Act 2024, from Jul 23 2024) | `INVALID_RATE` | STCG rate for listed equity is 20% |
| LTCG Rate (Equity) | 12.5% over ₹1,25,000 exemption | `INVALID_RATE` | LTCG rate for listed equity is 12.5% |
| LTCG Exemption (Sec 112A) | ≤ 1,25,000 | `EXCEEDS_LIMIT` | Exemption under Section 112A cannot exceed Rs.1,25,000 |
| VDA Gains | 30% flat, no indexation, no loss set-off | `INVALID_TREATMENT` | VDA gains cannot be indexed or set off |
| STCG on property | Taxed as per slab (no special rate) | `INVALID_RATE` | Property STCG is taxed as per slab |
| LTCG on property | 20% with CII indexation | `INVALID_RATE` | Property LTCG rate is 20% with indexation |

### 9.6 Business/Profession Income Validations (ITR-3, ITR-4)

| Field | Rule | Error Code | Error Message |
|---|---|---|---|
| Business Code | Must be valid 4-digit NIC code | `INVALID_CODE` | Business code is invalid |
| 44AD Turnover | ≥ GrossReceipts if 44AD opted | `INVALID_AMOUNT` | Turnover cannot be less than gross receipts |
| 44AD Presumptive Rate | 6% (digital receipts), 8% (cash ≤ 5%) | `INVALID_RATE` | Presumptive rate is invalid |
| 44ADA Professional | 50% of gross receipts | `INVALID_RATE` | Presumptive rate for 44ADA is 50% |
| 44AE Per Vehicle | As per ITD notified rates for AY 2026-27 | `INVALID_AMOUNT` | Per vehicle income is invalid |
| Depreciation | Block-of-assets method, Section 32 rates | `INVALID_RATE` | Depreciation rate is invalid |
| Section 43B | Must be actually paid before filing date | `NOT_PAID` | Business income reduced by unpaid 43B items |
| MSME Payments | ≥ 45 days overdue to be disallowed | `INVALID_PERIOD` | MSME payment period is invalid |
| Audit Required | If turnover > ₹10L (44AD) or turnover > ₹50L (others) | `AUDIT_REQUIRED` | Tax audit is mandatory |
| Book Profit (Partners) | Required if partnership firm, Section 92E | `MANDATORY_FIELD` | Book profit is mandatory for Section 92E |

### 9.7 Other Sources Validations

| Field | Rule | Error Code | Error Message |
|---|---|---|---|
| Interest Income | ≥ 0, in paise | `INVALID_AMOUNT` | Interest cannot be negative |
| Dividend Income | ≥ 0, in paise | `INVALID_AMOUNT` | Dividend cannot be negative |
| Family Pension | ≤ 1/3 of salary received | `EXCEEDS_LIMIT` | Family pension exceeds permissible limit |
| 80TTA | ≤ 10,000 (savings interest) | `EXCEEDS_LIMIT` | 80TTA deduction exceeds Rs.10,000 |
| 80TTB | ≤ 50,000 (senior citizen) | `EXCEEDS_LIMIT` | 80TTB deduction exceeds Rs.50,000 |

### 9.8 Deductions Validations

| Section | Rule | Error Code | Error Message |
|---|---|---|---|
| 80C | ≤ 1,50,000 (PF, PPF, LIC, ELSS, NSC, etc.) | `EXCEEDS_LIMIT` | 80C exceeds limit of Rs.1,50,000 |
| 80CCC | Combined with 80C ≤ 1,50,000 | `EXCEEDS_LIMIT` | 80C+80CCC exceeds limit |
| 80CCD(1) | Within 80C limit of 1,50,000 | `EXCEEDS_LIMIT` | 80CCD(1) exceeds 80C limit |
| 80CCD(1B) | ≤ 50,000 | `EXCEEDS_LIMIT` | 80CCD(1B) exceeds Rs.50,000 |
| 80CCD(2) | ≤ 14% of salary (CG) / 10% (others) — New regime allowed | `EXCEEDS_LIMIT` | 80CCD(2) exceeds permissible limit |
| 80D | ≤ 25,000 (self) / 50,000 (senior) / 1,00,000 (super senior) | `EXCEEDS_LIMIT` | 80D exceeds limit |
| 80DD | Fixed 50,000 (normal) / 1,25,000 (severe disability) | `EXCEEDS_LIMIT` | 80DD amount is invalid |
| 80DDB | ≤ 40,000 (normal) / 1,00,000 (senior citizen) | `EXCEEDS_LIMIT` | 80DDB amount is invalid |
| 80E | Actual interest, max 8 years from start | `INVALID_PERIOD` | 80E period exceeds 8 years |
| 80EE | ≤ 50,000, only for first home loan ≤ 50L | `EXCEEDS_LIMIT` | 80EE conditions not met |
| 80EEA | ≤ 1,50,000, only for stamp duty ≤ 45L | `EXCEEDS_LIMIT` | 80EEA conditions not met |
| 80G | Varies by category (100%/50% with/without 10% GTI limit) | `INVALID_CATEGORY` | 80G donation category is invalid |
| 80G | Cash donation > 2,000 disallowed | `EXCEEDS_LIMIT` | Cash donation exceeds Rs.2,000 limit |
| 80GG | ≤ 60,000, no HRA claimed | `EXCEEDS_LIMIT` | 80GG exceeds Rs.60,000 |
| 80GGC | Only for political parties, in cash disallowed | `INVALID_MODE` | 80GGC cash donation not allowed |
| 80U | Fixed 1,25,000 (normal) / 1,25,000 (severe disability) | `EXCEEDS_LIMIT` | 80U amount is invalid |
| 80CCH(2) | New regime only, Agnipath scheme | `NEW_REGIME_ONLY` | 80CCH(2) only in new regime |
| 80JJAA | Only manufacturing, additional employee cost | `INVALID_CATEGORY` | 80JJAA not applicable |

### 9.9 Tax Computation Validations

| Rule | Error Code | Error Message |
|---|---|---|
| New regime only allows 80CCD(2), 80CCH(2), 80JJAA | `NEW_REGIME_VIOLATION` | Deduction not allowed in new regime |
| Rebate 87A: taxable income ≤ 7L → tax = 0 (new), ≤ 5L → tax = 0 (old) | `REBATE_LIMIT` | Rebate 87A not applicable |
| Surcharge: 50L-1Cr = 10%, 1Cr-2Cr = 15%, 2Cr-5Cr = 25%, >5Cr = 37% | `SURCHARGE_APPLICABLE` | Surcharge calculation |
| Marginal relief: surcharge cannot push total tax above income | `MARGINAL_RELIEF` | Marginal relief applied |
| H&E Cess: 4% on (tax + surcharge) | `CESS_APPLICABLE` | Cess calculation |
| Self-assessment tax: must equal (tax - TDS - advance tax - TCS) | `TAX_MISMATCH` | Self-assessment tax does not match |
| Interest 234F: 1,000 (≤5L income) / 5,000 (before Dec 31, >5L) / 10,000 (after Dec 31) | `LATE_FEE_APPLICABLE` | Late filing fee applicable |

### 9.10 TDS Validations

| Rule | Error Code | Error Message |
|---|---|---|
| TAN must be 10-char format if provided | `INVALID_TAN` | TAN format is invalid |
| TDS rate must match applicable rate | `INVALID_RATE` | TDS rate is incorrect |
| TDS deducted must ≤ income amount | `EXCEEDS_INCOME` | TDS deducted exceeds income |
| Quarterly TDS detail must match annual TDS | `TDS_MISMATCH` | Quarterly TDS does not match annual total |

### 9.11 ITD JSON Upload Validations (Final Pre-submission)

These are the validation rules ITD applies when JSON is uploaded to e-filing portal:

| Error Code | Field | Description |
|---|---|---|
| `ITR_101` | PAN | PAN does not match the filer |
| `ITR_102` | AssessmentYear | AY mismatch |
| `ITR_103` | ITRForm | ITR type does not match income heads |
| `ITR_104` | TotalIncome | Total income is negative |
| `ITR_105` | TaxPayable | Tax computation error |
| `ITR_106` | TDS | TDS details do not match Form 26AS |
| `ITR_107` | ScheduleMismatch | Income head mismatch across schedules |
| `ITR_108` | MandatoryField | Required field is missing |
| `ITR_109` | SchemaVersion | JSON schema version mismatch |
| `ITR_110` | CreationInfo | JSON digest does not match |
| `ITR_111` | VerificationInfo | Digital signature invalid |
| `ITR_112` | BankAccount | Bank account not validated |
| `ITR_113` | Aadhaar | Aadhaar not linked to PAN |
| `ITR_114` | Form26ASMismatch | Salary in JSON does not match 26AS |
| `ITR_115` | AdvanceTax | Advance tax detail is incomplete |

### 9.12 Validation Error Response Format

Every validation error returned to the frontend must follow this structure:

```json
{
  "valid": false,
  "errors": [
    {
      "field": "salary.grossSalary",
      "code": "INVALID_AMOUNT",
      "message": "Gross salary cannot be negative",
      "value": -50000,
      "severity": "ERROR"
    },
    {
      "field": "houseProperty[0].selfOccupiedInterest",
      "code": "NEW_REGIME_NOT_ALLOWED",
      "message": "Self-occupied house property interest is not allowed in new tax regime",
      "value": 200000,
      "severity": "ERROR"
    }
  ],
  "warnings": [
    {
      "field": "deductions.80C",
      "code": "LIMIT_EXCEEDED",
      "message": "80C deduction claimed is Rs.1,60,000 but maximum allowed is Rs.1,50,000. Excess amount of Rs.10,000 will be disallowed.",
      "value": 160000,
      "severity": "WARNING"
    }
  ],
  "info": [
    {
      "field": "taxComputation.regime",
      "code": "REGIME_RECOMMENDATION",
      "message": "New regime results in tax savings of Rs.12,500 compared to old regime.",
      "severity": "INFO"
    }
  ]
}
```

---

## 10. SUB-PHASE E: Config Updates (After All Code Written)

### STEP 10: Update SecurityConfig.java

**EDIT:** `backend/src/main/java/com/itr/config/SecurityConfig.java`

Add to the `.requestMatchers(...).permitAll()` chain. DO NOT remove existing entries.

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/api/v1/auth/**",
        "/api/v1/pan/**",
        "/api/v1/prefill/**",
        "/api/v1/documents/share/**",
        "/api/v1/webhook/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/error"
    ).permitAll()
    .anyRequest().authenticated()
)
```

### STEP 11: Update application.properties

**EDIT:** `backend/src/main/resources/application.properties`

Append these properties at the end. DO NOT modify existing properties.

```properties
# ITD ERI-2 API Configuration (configure after API keys received from ITD)
itd.api.base-url=https://eportal.incometax.gov.in/iec/foservices/
itd.api.key=
itd.api.secret=
itd.api.timeout-ms=30000
itd.api.max-retries=3

# ITD Portal Password Encryption (AES-256-GCM)
# Generate key: openssl rand -base64 32
# WARNING: Never commit this key to version control
itd.master.key=

# File Upload
app.upload.dir=${user.home}/itr-filing-uploads

# Tax Computation
tax.regime.default=NEW

# Playwright (for browser automation)
playwright.enabled=false
```

### STEP 12: Update pom.xml (for Browser Automation)

**EDIT:** `backend/pom.xml`

Add Playwright dependency. Only do this when implementing Step 9.

```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.40.0</version>
</dependency>
```

---

## 11. CBDT TAX RULES — AY 2026-27

### 11.1 New Tax Regime (Default — Section 115BAC)

| Income Slab (₹) | Rate |
|---|---|
| 0 – 4,00,000 | NIL |
| 4,00,001 – 8,00,000 | 5% |
| 8,00,001 – 12,00,000 | 10% |
| 12,00,001 – 16,00,000 | 15% |
| 16,00,001 – 20,00,000 | 20% |
| 20,00,001 – 24,00,000 | 25% |
| Above 24,00,000 | 30% |

**Rebate u/s 87A:** Full rebate if taxable income ≤ ₹7,00,000 → tax becomes NIL

### 11.2 Old Tax Regime

| Income Slab (₹) | Rate |
|---|---|
| 0 – 2,50,000 | NIL |
| 2,50,001 – 5,00,000 | 5% |
| 5,00,001 – 10,00,000 | 20% |
| Above 10,00,000 | 30% |

**Senior Citizen (60-79):** Basic exemption ₹3,00,000
**Super Senior (80+):** Basic exemption ₹5,00,000

### 11.3 Surcharge

| Income | Rate |
|---|---|
| 50L – 1Cr | 10% |
| 1Cr – 2Cr | 15% |
| 2Cr – 5Cr | 25% |
| Above 5Cr | 37% |

**Marginal Relief:** Must compute where surcharge pushes income into higher bracket.

### 11.4 Health & Education Cess

**4% flat** on (Income Tax + Surcharge) — both regimes.

### 11.5 Interest Rules

| Section | Trigger | Rate | Period |
|---|---|---|---|
| 234A | Tax payable but return not filed by due date | 1%/month | From due date to filing date |
| 234B | Advance tax paid < 90% of assessed tax | 1%/month | From Apr 1 to date of tax payment |
| 234C | Quarterly installment shortfall | 1%/month | From each quarterly due date |
| 234F | Late filing fee | ₹1,000/5,000/10,000 | Based on income and delay |

---

## 12. TESTING CHECKLIST

### Sub-Phase A Tests (after Step 1)

| # | Test | Expected |
|---|---|---|
| A1 | Login with test1@test.com / pokemon123 | ✅ 200 + JWT |
| A2 | GET /dashboard/stats | ✅ 200 + stats JSON |
| A3 | POST /clients (create) | ✅ 201 + client object |
| A4 | GET /clients (list) | ✅ 200 + client list |
| A5 | GET /clients/{id} (single) | ✅ 200 + client with years |
| A6 | PUT /clients/{id} (update) | ✅ 200 + updated client |
| A7 | DELETE /clients/{id} | ✅ 204 |
| A8 | POST /filing (create) | ✅ 201 + filing object |
| A9 | GET /filing (list) | ✅ 200 + filing list |
| A10 | POST /documents/upload (PDF) | ✅ 201 + document metadata |
| A11 | GET /documents/list | ✅ 200 + document list |
| A12 | GET /pan/AAAAA9999A/validate | ✅ 200 + validation result |
| A13 | Bad password login | ✅ 401 |
| A14 | Protected endpoint without JWT | ✅ 403 |

### ITD Portal Password Tests

| # | Test | Expected |
|---|---|---|
| A15 | PUT /clients/{id}/itd-password (set password) | ✅ 200 + encrypted stored |
| A16 | GET /clients/{id}/itd-password (check exists) | ✅ 200 + { "exists": true } |
| A17 | DELETE /clients/{id}/itd-password (remove) | ✅ 200 + password deleted |
| A18 | GET /clients/{id}/itd-password after delete | ✅ 200 + { "exists": false } |
| A19 | Verify password NOT returned in GET /clients/{id} | ✅ Response has no password field |
| A20 | PUT /clients/{id}/itd-password with wrong client JWT | ✅ 403 Forbidden |
| A21 | GET decrypted password via internal method | ✅ Returns correct plain text password |

### Sub-Phase B Tests (after Steps 2-3)

| # | Test | Expected |
|---|---|---|
| B1 | POST /clients/1/itr/2026-27/compute (ITR-1, salary only) | ✅ 200 + correct tax |
| B2 | POST /clients/1/itr/2026-27/compute (ITR-2, with CG) | ✅ 200 + correct tax |
| B3 | POST /clients/1/itr/2026-27/compute (ITR-3, with business) | ✅ 200 + correct tax |
| B4 | POST /clients/1/itr/2026-27/compute (ITR-4, presumptive) | ✅ 200 + correct tax |
| B5 | Verify ITR-1 rejects CG entries | ✅ 400 + validation error |
| B6 | Verify ITR-4 rejects CG entries | ✅ 400 + validation error |
| B7 | Verify HP interest = 0 in new regime | ✅ Computed correctly |
| B8 | Verify HP interest capped at 2L in old regime | ✅ Computed correctly |
| B9 | Verify Rebate 87A applied when income ≤ 7L (new) | ✅ Tax = 0 |
| B10 | Verify Surcharge computed correctly at each bracket | ✅ Correct percentage |
| B11 | Verify H&E Cess = 4% on (tax + surcharge) | ✅ Correct |
| B12 | Verify LTCG 112A: 12.5% over 1.25L exemption | ✅ Correct |
| B13 | Verify STCG 111A: 20% (Finance Act 2024) | ✅ Correct |
| B14 | Verify VDA: 30% flat, no indexation | ✅ Correct |

### Sub-Phase C Tests (after Steps 4-5)

| # | Test | Expected |
|---|---|---|
| C1 | GET /clients/1/itr/2026-27/download (JSON) | ✅ 200 + valid ITD-schema JSON |
| C2 | Verify JSON monetary values in paise | ✅ All amounts × 100 |
| C3 | Verify JSON date format DD-MM-YYYY | ✅ Correct format |
| C4 | Verify JSON PAN format AAAAA9999A | ✅ Correct format |
| C5 | GET /clients/1/itr/2026-27/download-pdf | ✅ 200 + PDF file |
| C6 | Verify PDF contains all income heads | ✅ Salary, HP, CG, OS |
| C7 | Verify PDF tax computation matches compute endpoint | ✅ Consistent |
| C8 | Verify JSON validates against ITD schema | ✅ No ITR_1xx errors |
| C9 | Verify all mandatory fields present in JSON | ✅ Complete |
| C10 | Verify ITR-2 JSON has CG, AL, FA, FSI sections | ✅ Complete |

### Sub-Phase D Tests (after Steps 6-7)

| # | Test | Expected |
|---|---|---|
| D1 | POST /prefill/26as/upload with 26AS PDF | ✅ 200 + parsed data |
| D2 | POST /prefill/ais/upload with AIS PDF | ✅ 200 + parsed data |
| D3 | POST /prefill/tis/upload with TIS PDF | ✅ 200 + parsed data |
| D4 | POST /prefill/form16/upload with Form16 PDF | ✅ 200 + PartA + PartB data |
| D5 | POST /integration/autopopulate/form16 | ✅ 200 + populated fields |
| D6 | POST /integration/reconciliation | ✅ 200 + discrepancy report |
| D7 | Verify HIGH severity for TDS mismatch > 500 | ✅ Correct |
| D8 | Verify prefill status shows all uploaded docs | ✅ Complete |
| D9 | Upload 26AS → auto-populate → compute → verify | ✅ End-to-end works |

### Sub-Phase E Tests (after Steps 8-9) — LAST

| # | Test | Expected |
|---|---|---|
| E1 | Configure ITD API keys in application.properties | ✅ Keys set |
| E2 | Test ERI-2 API connectivity (sandbox) | ✅ Connected |
| E3 | GET /itd/prefill/{pan}/{ay} (API) vs manual upload | ✅ Same data |
| E4 | Submit ITR JSON via API | ✅ Acknowledgement received |
| E5 | Browser automation: one-click AIS download | ✅ PDF downloaded |
| E6 | Browser automation: one-click 26AS download | ✅ PDF downloaded |
| E7 | End-to-end: API prefill → compute → PDF → JSON → submit | ✅ Filing complete |

---

## 13. FILES SUMMARY

### CREATE (16 new files)

| File | Package | Sub-Phase |
|---|---|---|
| `config/ClientController.java` | config | A |
| `config/DashboardController.java` | config | A |
| `config/FilingController.java` | config | A |
| `config/DocumentController.java` | config | A |
| `config/PANController.java` | config | A |
| `config/PrefillController.java` | config | A |
| `config/IntegrationController.java` | config | A |
| `config/TaxController.java` | config | A |
| `config/AdvancedTaxController.java` | config | A |
| `service/TaxComputationOrchestrator.java` | service | B |
| `service/ITR1ComputationService.java` | service | B |
| `service/ITR2ComputationService.java` | service | B |
| `service/ITR3ComputationService.java` | service | B |
| `service/ITR4ComputationService.java` | service | B |
| `service/ITRJsonExportService.java` | service | C |
| `service/ComputationPDFService.java` | service | C |

### CREATE DTOs (4 new files)

| File | Package |
|---|---|
| `dto/itr/ITR1FormData.java` | dto.itr |
| `dto/itr/ITR2FormData.java` | dto.itr |
| `dto/itr/ITR3FormData.java` | dto.itr |
| `dto/itr/ITR4FormData.java` | dto.itr |

### CREATE (Sub-Phase E — after core verified)

| File | Package | Sub-Phase |
|---|---|---|
| `service/integration/ITDERI2Service.java` | service.integration | E |
| `service/integration/ITDPortalAutomationService.java` | service.integration | E |

### EDIT (6 files)

| File | Change | When |
|---|---|---|
| `entity/Client.java` | Add itdPasswordEncrypted, itdPasswordIv, itdPasswordSetAt fields | Sub-Phase A |
| `dto/ClientRequest.java` | Add optional itdPassword field | Sub-Phase A |
| `service/ClientService.java` | Add setItdPassword, removeItdPassword, hasItdPassword, getDecryptedItdPassword methods | Sub-Phase A |
| `config/SecurityConfig.java` | Add public routes | After Step 1 |
| `application.properties` | Append ITD master key + API config | After Step 10 |
| `pom.xml` | Add Playwright | When implementing Step 9 |

### NO TOUCH

All domain/ files
All repository/ files  
All exception/ files  
All util/ files
All infrastructure/ files (including security/JwtTokenProvider.java)
All model/ files
All other entity/ files (AuditTrail, ClientYearData, ITRFiling, etc.) — only Client.java may be edited
All other dto/ files — only ClientRequest.java may be edited
All frontend files (already correct — just need backend)
backend/pom.xml (except Step 12)

---

## 14. COMMANDS TO RUN

```powershell
# Verify backend compiles
cd backend
mvn clean compile
# Expected: BUILD SUCCESS

# Run tests
mvn test
# Expected: All pass

# Package
mvn clean package -DskipTests

# Run
java -jar target/itr-filing-assistant-0.0.1-SNAPSHOT.jar

# Frontend
cd frontend
npm install
npm run build
npm run dev
```

---

## 15. IMPORTANT WARNINGS FOR THE AGENT

1. **DO NOT implement Steps 8-9 (ITD API + Browser Automation) until ALL other steps are tested and working.** The core tax calculation must be 100% verified before connecting to external systems.
2. **DO NOT** modify `application.properties` jwt.secret — it contains a valid key (redacted in display).
3. **DO NOT** modify `DataInitializer.java` — test user accounts must remain.
4. **DO NOT** delete any stub page — Phase 2 scope.
5. **All controllers go in `config/` package** — `interfaces/rest/` was deleted.
6. **userId extraction:** `SecurityContextHolder.getContext().getAuthentication().getName()` returns the user ID (numeric string). Convert to Long via `Long.parseLong()`.
7. **ALL monetary values stored in PAISE** — multiply by 100 before storing, divide by 100 when displaying.
8. **ALL dates in DD-MM-YYYY format** — use `ITDDateFormatter.java`.
9. **ALL JSON output must match ITD schema** — download the official schema JSON from incometax.gov.in and map every field.
10. **Validation errors must be returned with codes from Section 9** — not generic messages.
11. **ITR-1 can only contain Salary + at most ONE house property + Other Sources** — validate and reject CG/business entries.
12. **ITR-4 can only contain Salary + at most ONE house property + Other Sources + Presumptive business** — validate and reject CG/business.
13. **New regime (115BAC):** Self-occupied HP interest = NIL. Only 80CCD(2), 80CCH(2), 80JJAA deductions allowed.
14. **The existing domain/ engines are CORRECT** — do not rewrite them. Only call them from new services.
15. **When in doubt about CBDT rules, refer to `AssessmentYear.java`** — it is the single source of truth for all AY 2026-27 constants.
