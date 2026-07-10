# Architecture Blueprint

**Document Version:** 1.0  
**Date:** 2026-07-10  
**Architecture Pattern:** Hexagonal Architecture + Clean Architecture + DDD

---

## 1. System Context

```
┌────────────────────────────────────────────────────────────┐
│                        External Actors                      │
├────────────────────────────────────────────────────────────┤
│  CA (Chartered Accountant)  │  Staff  │  Admin  │  Client  │
└────────────┬───────────────────────────────────────────────┘
             │
             ▼
┌────────────────────────────────────────────────────────────┐
│                     ITR Filing ERP System                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Presentation Layer (Next.js)            │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           Application Layer (Use Cases)              │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Domain Layer (Business Logic)                │  │
│  │         + OpenTax Tax Engine (Vendored)              │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │        Infrastructure Layer (Database, APIs)         │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────┬───────────────────────────────────────────────┘
             │
             ▼
┌────────────────────────────────────────────────────────────┐
│               External Systems                              │
├────────────────────────────────────────────────────────────┤
│  PostgreSQL  │  Redis  │  S3  │  Email  │  SMS  │  ITD API │
└────────────────────────────────────────────────────────────┘
```

---

## 2. Hexagonal Architecture

### 2.1 Core Domain (Center)

```
core/domain/
├── entities/
│   ├── Client              # Aggregate root
│   │   ├── id: UUID
│   │   ├── pan: PAN (VO)
│   │   ├── name: str
│   │   ├── dob: date
│   │   ├── assigned_user_id: UUID
│   │   └── methods: update_contact(), change_ay()
│   │
│   ├── Filing              # Aggregate root
│   │   ├── id: UUID
│   │   ├── client_id: UUID
│   │   ├── ay: AssessmentYear (VO)
│   │   ├── regime: TaxRegime (VO)
│   │   ├── status: FilingStatus
│   │   ├── schedules: List[Schedule]
│   │   └── methods: add_schedule(), compute_tax(), submit()
│   │
│   ├── Schedule            # Abstract base for ITR schedules
│   │   ├── ScheduleSalary  # Salary income (all ITRs)
│   │   ├── ScheduleHP      # House property (all ITRs)
│   │   ├── ScheduleCG      # Capital gains (ITR-2+)
│   │   ├── ScheduleOS      # Other sources (all ITRs)
│   │   ├── ScheduleBP      # Business/Profession (ITR-3+)
│   │   ├── ScheduleTDS     # TDS details (all ITRs)
│   │   ├── ScheduleIT      # Tax payments (all ITRs)
│   │   ├── ScheduleVIA     # Chapter VI-A deductions (all ITRs)
│   │   └── ScheduleBA      # Bank accounts (all ITRs)
│   │
│   └── ComputedReturn      # Immutable aggregate
│       ├── id: UUID
│       ├── filing_id: UUID
│       ├── snapshot_hash: str
│       ├── payload: JSONB (read-only)
│       ├── explanation: List[ComputationStep]
│       └── created_at: timestamp
│
└── value_objects/
    ├── PAN(value: str)
    │   └── validate() -> ensures [A-Z]{5}[0-9]{4}[A-Z]
    ├── Money(paise: int)
    │   └── add(), subtract(), multiply(), round_cbdt()
    ├── AssessmentYear(value: str)
    │   └── to_fy(), validate() -> ensures YYYY-YY
    └── TaxRegime(value: Literal["old", "new"])
```

### 2.2 Ports (Interfaces)

```python
# core/interfaces/repositories.py
class IClientRepository(ABC):
    @abstractmethod
    async def get_by_id(self, client_id: UUID) -> Client | None: ...
    
    @abstractmethod
    async def get_by_pan(self, pan: PAN) -> Client | None: ...
    
    @abstractmethod
    async def save(self, client: Client) -> None: ...
    
    @abstractmethod
    async def delete(self, client_id: UUID) -> None: ...
    
    @abstractmethod
    async def list_all(self, user_id: UUID) -> List[Client]: ...

# core/interfaces/tax_engine.py
class ITaxEngine(ABC):
    """Tax computation engine interface - OpenTax is ONE implementation."""
    
    @abstractmethod
    async def compute_tax(
        self, 
        schedules: List[Schedule],
        regime: TaxRegime,
        ay: AssessmentYear
    ) -> ComputedReturn: ...
    
    @abstractmethod
    def explain_computation(
        self, 
        computed: ComputedReturn
    ) -> List[ComputationStep]: ...

# core/interfaces/itr_builder.py
class IITRBuilder(ABC):
    """ITR JSON generation interface - separate from tax computation."""
    
    @abstractmethod
    async def build(
        self, 
        schedules: List[Schedule],
        computed: ComputedReturn
    ) -> dict: ...
    
    @abstractmethod
    async def validate(
        self, 
        itr_json: dict
    ) -> List[ValidationError]: ...

# core/interfaces/document_parser.py
class IDocumentParser(ABC):
    @abstractmethod
    async def parse(self, file_path: Path) -> ParsedDocument: ...
    
    @abstractmethod
    def supports(self, file_type: str) -> bool: ...
```

### 2.3 Adapters (Implementations)

```python
# adapters/opentax/tax_engine_adapter.py
class OpenTaxAdapter(ITaxEngine):
    def __init__(self):
        self.tax_service = TaxCalculationService()  # Vendored
        self.itr_builder = ItrBuildingOrchestrator()  # Vendored
    
    async def compute_tax(
        self, 
        filing: Filing, 
        regime: TaxRegime
    ) -> TaxComputation:
        # Map ERP Filing → OpenTax FilingModel
        opentax_filing = self.mapper.to_opentax(filing)
        
        # Call vendored OpenTax
        result = await self.tax_service.calculate(opentax_filing)
        
        # Map OpenTax result → ERP TaxComputation
        return self.mapper.from_opentax(result)

# adapters/repositories/sqlalchemy_client_repo.py
class SQLAlchemyClientRepository(IClientRepository):
    def __init__(self, session: AsyncSession):
        self.session = session
    
    async def get_by_id(self, client_id: UUID) -> Client | None:
        stmt = select(ClientModel).where(ClientModel.id == client_id)
        result = await self.session.execute(stmt)
        model = result.scalar_one_or_none()
        return self._to_entity(model) if model else None
```

---

## 3. Use Case Layer

### 3.1 Filing Use Cases

```python
# core/use_cases/filing/compute_tax.py
class ComputeTaxUseCase:
    def __init__(
        self,
        filing_repo: IFilingRepository,
        tax_engine: ITaxEngine,
        snapshot_repo: ISnapshotRepository
    ):
        self.filing_repo = filing_repo
        self.tax_engine = tax_engine
        self.snapshot_repo = snapshot_repo
    
    async def execute(
        self, 
        filing_id: UUID, 
        regime: TaxRegime
    ) -> ComputedReturn:
        # 1. Load filing
        filing = await self.filing_repo.get_by_id(filing_id)
        if not filing:
            raise FilingNotFoundError(filing_id)
        
        # 2. Compute tax (via OpenTax adapter)
        computation = await self.tax_engine.compute_tax(filing, regime)
        
        # 3. Create immutable snapshot
        snapshot = ComputedReturn.create(
            filing_id=filing_id,
            payload=computation.to_dict(),
            rule_version=computation.rule_version
        )
        
        # 4. Persist snapshot
        await self.snapshot_repo.save(snapshot)
        
        # 5. Update filing status
        filing.mark_computed()
        await self.filing_repo.save(filing)
        
        return snapshot
```

### 3.2 Import Use Cases

```python
# core/use_cases/filing/import_ais_26as.py
class ImportAIS26ASUseCase:
    def __init__(
        self,
        parser: IDocumentParser,
        filing_repo: IFilingRepository,
        normalizer: CanonicalNormalizer
    ):
        self.parser = parser
        self.filing_repo = filing_repo
        self.normalizer = normalizer
    
    async def execute(
        self, 
        client_id: UUID, 
        ay: AssessmentYear,
        file_path: Path
    ) -> None:
        # 1. Parse document
        parsed = await self.parser.parse(file_path)
        
        # 2. Normalize to canonical model
        canonical = self.normalizer.normalize_ais(parsed)
        
        # 3. Load or create filing
        filing = await self.filing_repo.get_or_create(
            client_id, ay
        )
        
        # 4. Merge canonical data into filing
        filing.merge_imported_data(canonical)
        
        # 5. Persist
        await self.filing_repo.save(filing)
```

---

## 4. Data Flow Architecture

### 4.1 Tax Computation Flow

```
User Request
    │
    ▼
┌─────────────────────────────────────┐
│  API Controller (FastAPI)           │
│  POST /api/v1/filings/{id}/compute  │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Use Case: ComputeTaxUseCase        │
│  (Application Layer)                │
└────────────┬────────────────────────┘
             │
             ├──► Load Filing from DB
             │
             ▼
┌─────────────────────────────────────┐
│  Adapter: OpenTaxAdapter             │
│  (Hexagonal Port)                   │
└────────────┬────────────────────────┘
             │
             ├──► Map Filing → FilingModel
             │
             ▼
┌─────────────────────────────────────┐
│  OpenTax Tax Engine (Vendored)      │
│  TaxCalculationService.calculate()  │
└────────────┬────────────────────────┘
             │
             ├──► Slab computation
             ├──► Surcharge + cess
             ├──► Rebate 87A
             ├──► Interest 234A/B/C
             │
             ▼
┌─────────────────────────────────────┐
│  Response: TaxRegimeBreakdownModel  │
└────────────┬────────────────────────┘
             │
             ├──► Map → ComputedReturn
             │
             ▼
┌─────────────────────────────────────┐
│  Create Immutable Snapshot          │
│  (Aggregate: ComputedReturn)        │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Persist to computed_return_        │
│  snapshots table                    │
└────────────┬────────────────────────┘
             │
             ▼
API Response (JSON)
```

### 4.2 Document Import Flow

```
User Uploads AIS PDF
    │
    ▼
┌─────────────────────────────────────┐
│  API: POST /api/v1/imports/ais      │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Store file (S3 or local)           │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Parser: AISPDFParser               │
│  (decrypt + extract tables)         │
└────────────┬────────────────────────┘
             │
             ├──► Raw AIS data
             │
             ▼
┌─────────────────────────────────────┐
│  Normalizer: CanonicalNormalizer    │
│  (AIS → Canonical Income Model)     │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Canonical Income:                  │
│  - Salary: {employer, gross, hra}   │
│  - TDS: {tan, section, amount}      │
│  - Interest: {bank, amount}         │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Load Filing (or create if new)     │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Merge canonical → Filing aggregate │
│  (intelligent merge, detect dups)   │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Persist updated Filing to DB       │
└────────────┬────────────────────────┘
             │
             ▼
API Response (success + merge report)
```

---

## 5. State Management

### 5.1 Filing State Machine

```
DRAFT ──────┐
    │       │
    ▼       │
IMPORTED    │
    │       │
    ▼       │
COMPUTED ◄──┘
    │
    ├──► VALIDATING
    │       │
    │       ├──► VALIDATION_FAILED ──► DRAFT
    │       │
    │       └──► VALIDATED
    │               │
    │               ▼
    │           SUBMITTED
    │               │
    │               ├──► ACKNOWLEDGED
    │               │
    │               └──► REJECTED ──► DRAFT
    │
    └──► FILED (terminal)

Transitions:
- DRAFT → IMPORTED: AIS/26AS upload
- IMPORTED → COMPUTED: Tax computation
- COMPUTED → VALIDATING: User triggers validation
- VALIDATING → VALIDATED: Passes all checks
- VALIDATED → SUBMITTED: User submits to ITD
- SUBMITTED → ACKNOWLEDGED: ITD confirms receipt
- ACKNOWLEDGED → FILED: User marks complete
```

### 5.2 Snapshot Versioning

```
Filing (mutable)
    │
    ├──► Snapshot v1 (after AIS import)
    │
    ├──► Snapshot v2 (after manual edits)
    │
    ├──► Snapshot v3 (after tax computation)
    │
    └──► Snapshot v4 (pre-submit, immutable)
         └──► This becomes the "filed" version
              (locked, cannot be deleted)
```

---

## 6. Security Architecture

### 6.1 Authentication Flow

```
1. User Login
   ├──► POST /api/v1/auth/login {email, password}
   │
   ├──► Verify password (bcrypt)
   │
   ├──► Generate JWT (30 min expiry)
   │
   └──► Return {token, refresh_token}

2. Authenticated Request
   ├──► Authorization: Bearer <token>
   │
   ├──► JWT validation (middleware)
   │
   ├──► Extract user_id + role from claims
   │
   └──► Inject into request context

3. Token Refresh
   ├──► POST /api/v1/auth/refresh {refresh_token}
   │
   ├──► Validate refresh token (longer expiry)
   │
   └──► Issue new JWT
```

### 6.2 Authorization (RBAC)

```
Roles:
├── ADMIN
│   └── All permissions
├── CA (Chartered Accountant)
│   ├── Manage own clients
│   ├── Create/edit/delete filings
│   ├── Compute tax
│   └── Submit returns
├── STAFF
│   ├── View clients (assigned only)
│   ├── Edit filings (assigned only)
│   └── Cannot submit
└── CLIENT
    ├── View own filings (read-only)
    └── Upload documents

Permission Check (Middleware):
@require_permission("filing:edit")
async def update_filing(filing_id: UUID):
    # Check user has access to this client
    if not user.can_access_filing(filing_id):
        raise ForbiddenError()
```

---

## 7. Performance Architecture

### 7.1 Caching Strategy

```
┌────────────────────────────────────┐
│  Application Cache (Redis)         │
├────────────────────────────────────┤
│  client:{id}          TTL: 5 min   │
│  filing:{id}          TTL: 2 min   │
│  computation:{id}     TTL: 10 min  │
│  master_data:ay:{ay}  TTL: 1 day   │
└────────────────────────────────────┘

Cache Invalidation:
- client:{id} → invalidated on update
- filing:{id} → invalidated on edit/compute
- computation:{id} → never invalidated (immutable)
- master_data → invalidated on rule change
```

### 7.2 Database Optimization

```sql
-- Most critical indexes
CREATE INDEX idx_filings_client_ay 
    ON itr_filings(client_id, ay);

CREATE INDEX idx_snapshots_filing_created 
    ON computed_return_snapshots(filing_id, created_at DESC);

CREATE INDEX idx_tds_client_ay 
    ON tds_deductors(client_id, ay);

-- Partial indexes for performance
CREATE INDEX idx_filings_status_draft 
    ON itr_filings(status) 
    WHERE status = 'draft';

-- Covering index for dashboard query
CREATE INDEX idx_filings_dashboard 
    ON itr_filings(created_by, status, created_at) 
    INCLUDE (ay, regime);
```

### 7.3 Async Processing

```
Synchronous (< 2s response):
- Client CRUD
- Filing CRUD
- Document upload (store only)
- Validation

Asynchronous (background tasks):
- Document parsing (via Celery)
- Tax computation (if > 5s)
- ITR JSON generation
- PDF generation
- Bulk import
- Email notifications

Task Queue (Celery + Redis):
┌──────────────────────────────┐
│  Task Producer (FastAPI)     │
└────────────┬─────────────────┘
             │
             ▼
┌──────────────────────────────┐
│  Redis (Broker)              │
└────────────┬─────────────────┘
             │
             ▼
┌──────────────────────────────┐
│  Celery Workers (4x)         │
│  - parse_ais_task            │
│  - compute_tax_task          │
│  - generate_json_task        │
└──────────────────────────────┘
```

---

## 8. Error Handling

### 8.1 Error Hierarchy

```python
class DomainError(Exception):
    """Base for all domain errors"""

class ClientNotFoundError(DomainError): ...
class FilingNotFoundError(DomainError): ...
class InvalidPANError(DomainError): ...
class InvalidAssessmentYearError(DomainError): ...

class ApplicationError(Exception):
    """Base for application layer errors"""

class ComputationFailedError(ApplicationError): ...
class ValidationFailedError(ApplicationError): ...
class ImportFailedError(ApplicationError): ...

class InfrastructureError(Exception):
    """Base for infrastructure errors"""

class DatabaseError(InfrastructureError): ...
class StorageError(InfrastructureError): ...
```

### 8.2 Global Exception Handler

```python
@app.exception_handler(DomainError)
async def domain_error_handler(request, exc):
    return JSONResponse(
        status_code=400,
        content={
            "error": type(exc).__name__,
            "message": str(exc),
            "type": "domain_error"
        }
    )

@app.exception_handler(ApplicationError)
async def application_error_handler(request, exc):
    return JSONResponse(
        status_code=422,
        content={
            "error": type(exc).__name__,
            "message": str(exc),
            "type": "application_error"
        }
    )
```

---

## 9. Monitoring & Observability

### 9.1 Logging Strategy

```python
# Structured logging (JSON)
logger.info(
    "tax_computation_completed",
    extra={
        "filing_id": str(filing_id),
        "client_id": str(client_id),
        "ay": ay,
        "regime": regime,
        "duration_ms": duration,
        "total_tax": total_tax
    }
)

# Log levels:
# ERROR: Unhandled exceptions, data corruption
# WARNING: Validation failures, retries
# INFO: Business events (filing created, tax computed)
# DEBUG: Detailed flow (disabled in prod)
```

### 9.2 Metrics (Prometheus)

```python
# Counter
filings_created_total = Counter(
    "filings_created_total",
    "Total filings created",
    ["ay", "regime"]
)

# Histogram
tax_computation_duration = Histogram(
    "tax_computation_duration_seconds",
    "Tax computation duration",
    buckets=[0.1, 0.5, 1.0, 2.0, 5.0]
)

# Gauge
active_users = Gauge(
    "active_users",
    "Currently active users"
)
```

---

## 10. Deployment Architecture

```
┌────────────────────────────────────────────────────┐
│                  Load Balancer (Nginx)             │
└──────────────┬─────────────────────────────────────┘
               │
      ┌────────┴────────┐
      ▼                 ▼
┌───────────┐     ┌───────────┐
│  Web 1    │     │  Web 2    │
│  Next.js  │     │  Next.js  │
└───────────┘     └───────────┘
      │                 │
      └────────┬────────┘
               ▼
┌────────────────────────────────────┐
│       API (FastAPI)                │
│       - 4x Uvicorn workers         │
└──────────────┬─────────────────────┘
               │
      ┌────────┴──────────┐
      ▼                   ▼
┌───────────┐     ┌──────────────┐
│PostgreSQL │     │ Redis        │
│(Primary)  │     │(Cache+Queue) │
└───────────┘     └──────────────┘
      │
      ▼
┌───────────┐
│PostgreSQL │
│(Replica)  │
│(Read-only)│
└───────────┘

Celery Workers (Background):
- Worker 1: Document parsing
- Worker 2: Tax computation
- Worker 3: JSON/PDF generation
- Worker 4: Notifications
```

---

**Architecture Status:** APPROVED  
**Implementation Ready:** Yes  
**Next:** OpenTax Integration Blueprint
