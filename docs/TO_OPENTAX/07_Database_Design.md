# Database Design

**Version:** 1.0  
**Database:** PostgreSQL 15+  
**ORM:** SQLAlchemy 2.0  
**Migrations:** Alembic

---

## Schema Overview

```
┌─────────────┐
│   users     │──┐
└─────────────┘  │
                 │ created_by
┌─────────────┐  │
│   clients   │◄─┘
└──────┬──────┘
       │ 1:N
       ▼
┌─────────────┐      ┌──────────────────┐
│itr_filings  │──┬──►│computed_return_  │
└─────────────┘  │   │  snapshots       │
                 │   └──────────────────┘
                 │
                 ├──►│itr_form_data     │
                 │   └──────────────────┘
                 │
                 ├──►│tds_deductors     │
                 │   └──────────────────┘
                 │
                 └──►│documents         │
                     └──────────────────┘
```

---

## Core Tables

### users
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'staff',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    mfa_secret VARCHAR(64),
    last_login TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT users_role_check CHECK (role IN ('admin', 'ca', 'staff', 'client'))
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role_active ON users(role, is_active) WHERE is_active = TRUE;
```

### clients
```sql
CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pan VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    dob DATE,
    father_name VARCHAR(255),
    mobile VARCHAR(15),
    email VARCHAR(255),
    address JSONB,
    aadhaar_masked VARCHAR(14),
    category VARCHAR(20) NOT NULL DEFAULT 'individual',
    tags JSONB,
    family_group_id UUID,
    extra_data JSONB,
    assigned_user_id UUID,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT clients_pan_format CHECK (pan ~ '^[A-Z]{5}[0-9]{4}[A-Z]$'),
    CONSTRAINT clients_category_check CHECK (category IN ('individual', 'huf', 'firm', 'company')),
    CONSTRAINT fk_clients_assigned_user FOREIGN KEY (assigned_user_id) 
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_clients_created_by FOREIGN KEY (created_by) 
        REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_clients_pan ON clients(pan);
CREATE INDEX idx_clients_assigned_user ON clients(assigned_user_id);
CREATE INDEX idx_clients_created_by ON clients(created_by);
CREATE INDEX idx_clients_name_trgm ON clients USING gin(name gin_trgm_ops);
```

### itr_filings
```sql
CREATE TABLE itr_filings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL,
    ay VARCHAR(7) NOT NULL,
    itr_form VARCHAR(10) NOT NULL,
    regime VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'draft',
    json_hash VARCHAR(64),
    json_path VARCHAR(500),
    pdf_path VARCHAR(500),
    filed_at TIMESTAMPTZ,
    ack_no VARCHAR(20),
    extra_data JSONB,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_filings_client FOREIGN KEY (client_id) 
        REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_filings_created_by FOREIGN KEY (created_by) 
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_filing_client_ay UNIQUE (client_id, ay),
    CONSTRAINT filings_ay_format CHECK (ay ~ '^[0-9]{4}-[0-9]{2}$'),
    CONSTRAINT filings_regime_check CHECK (regime IN ('old', 'new')),
    CONSTRAINT filings_status_check CHECK (status IN (
        'draft', 'imported', 'computed', 'validating', 
        'validation_failed', 'validated', 'submitted', 
        'acknowledged', 'rejected', 'filed'
    ))
);

CREATE INDEX idx_filings_client_ay ON itr_filings(client_id, ay);
CREATE INDEX idx_filings_status ON itr_filings(status);
CREATE INDEX idx_filings_created_by ON itr_filings(created_by);
CREATE INDEX idx_filings_dashboard ON itr_filings(created_by, status, created_at) 
    INCLUDE (ay, regime);
```

---

## Computation Tables

### computed_return_snapshots
```sql
CREATE TABLE computed_return_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL,
    pan VARCHAR(10) NOT NULL,
    ay VARCHAR(7) NOT NULL,
    itr_form VARCHAR(10) NOT NULL,
    regime VARCHAR(10) NOT NULL,
    rule_version VARCHAR(20) NOT NULL,
    payload JSONB NOT NULL,
    snapshot_hash VARCHAR(64) NOT NULL UNIQUE,
    is_locked BOOLEAN NOT NULL DEFAULT TRUE,
    filed_at TIMESTAMPTZ,
    ack_no VARCHAR(20),
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_snapshots_client FOREIGN KEY (client_id) 
        REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_snapshots_created_by FOREIGN KEY (created_by) 
        REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_snapshots_client_ay ON computed_return_snapshots(client_id, ay);
CREATE INDEX idx_snapshots_pan ON computed_return_snapshots(pan);
CREATE INDEX idx_snapshots_hash ON computed_return_snapshots(snapshot_hash);
CREATE INDEX idx_snapshots_created ON computed_return_snapshots(created_at DESC);
```

### filing_snapshots_metadata
```sql
CREATE TABLE filing_snapshots_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filing_id UUID NOT NULL,
    snapshot_id UUID NOT NULL,
    snapshot_type VARCHAR(20) NOT NULL,
    note TEXT,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_snapshot_meta_filing FOREIGN KEY (filing_id) 
        REFERENCES itr_filings(id) ON DELETE CASCADE,
    CONSTRAINT fk_snapshot_meta_snapshot FOREIGN KEY (snapshot_id) 
        REFERENCES computed_return_snapshots(id) ON DELETE CASCADE,
    CONSTRAINT fk_snapshot_meta_created_by FOREIGN KEY (created_by) 
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT snapshot_type_check CHECK (snapshot_type IN (
        'auto', 'manual', 'pre_import', 'pre_compute', 'pre_submit'
    ))
);

CREATE INDEX idx_snapshot_meta_filing ON filing_snapshots_metadata(filing_id, created_at DESC);
CREATE INDEX idx_snapshot_meta_snapshot ON filing_snapshots_metadata(snapshot_id);
```

### itr_form_data
```sql
CREATE TABLE itr_form_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filing_id UUID NOT NULL,
    head_type VARCHAR(30) NOT NULL,
    payload JSONB NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_form_data_filing FOREIGN KEY (filing_id) 
        REFERENCES itr_filings(id) ON DELETE CASCADE,
    CONSTRAINT uq_form_data_filing_head UNIQUE (filing_id, head_type)
);

CREATE INDEX idx_form_data_filing ON itr_form_data(filing_id);
```

---

## Import Tables

### form_26as_data
```sql
CREATE TABLE form_26as_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL,
    ay VARCHAR(7) NOT NULL,
    pan VARCHAR(10) NOT NULL,
    raw_json JSONB NOT NULL,
    parsed_summary JSONB,
    imported_by UUID,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_26as_client FOREIGN KEY (client_id) 
        REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_26as_imported_by FOREIGN KEY (imported_by) 
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_26as_client_ay UNIQUE (client_id, ay)
);

CREATE INDEX idx_26as_client_ay ON form_26as_data(client_id, ay);
```

### ais_data
```sql
CREATE TABLE ais_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL,
    ay VARCHAR(7) NOT NULL,
    pan VARCHAR(10) NOT NULL,
    source VARCHAR(50) NOT NULL,
    raw_json JSONB NOT NULL,
    parsed_summary JSONB,
    imported_by UUID,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_ais_client FOREIGN KEY (client_id) 
        REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_ais_imported_by FOREIGN KEY (imported_by) 
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_ais_client_ay_source UNIQUE (client_id, ay, source)
);

CREATE INDEX idx_ais_client_ay ON ais_data(client_id, ay);
```

### tds_deductors
```sql
CREATE TABLE tds_deductors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL,
    ay VARCHAR(7) NOT NULL,
    deductor_tan VARCHAR(10) NOT NULL,
    deductor_name VARCHAR(255) NOT NULL,
    section_code VARCHAR(10) NOT NULL,
    amount_paid INTEGER NOT NULL DEFAULT 0,
    tax_deducted INTEGER NOT NULL DEFAULT 0,
    tax_deposited INTEGER NOT NULL DEFAULT 0,
    quarter VARCHAR(5),
    receipt_no VARCHAR(50),
    extra_data JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_tds_client FOREIGN KEY (client_id) 
        REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT tds_amounts_positive CHECK (
        amount_paid >= 0 AND tax_deducted >= 0 AND tax_deposited >= 0
    )
);

CREATE INDEX idx_tds_client_ay ON tds_deductors(client_id, ay);
CREATE INDEX idx_tds_tan ON tds_deductors(deductor_tan);
```

---

## Document Management

### documents
```sql
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID,
    filing_id UUID,
    name VARCHAR(255) NOT NULL,
    doc_type VARCHAR(50) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    sha256 VARCHAR(64) NOT NULL,
    uploaded_by UUID,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_docs_client FOREIGN KEY (client_id) 
        REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_docs_filing FOREIGN KEY (filing_id) 
        REFERENCES itr_filings(id) ON DELETE CASCADE,
    CONSTRAINT fk_docs_uploaded_by FOREIGN KEY (uploaded_by) 
        REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_docs_client ON documents(client_id);
CREATE INDEX idx_docs_filing ON documents(filing_id);
CREATE INDEX idx_docs_sha256 ON documents(sha256);
```

---

## Rule Engine

### rule_versions
```sql
CREATE TABLE rule_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_year VARCHAR(7) NOT NULL UNIQUE,
    slab_structure JSONB NOT NULL,
    deduction_limits JSONB NOT NULL,
    special_rate_rules JSONB NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT rule_ay_format CHECK (assessment_year ~ '^[0-9]{4}-[0-9]{2}$')
);

CREATE INDEX idx_rules_ay ON rule_versions(assessment_year);
CREATE INDEX idx_rules_active ON rule_versions(active) WHERE active = TRUE;
```

---

## Audit & Tracking

### audit_trail
```sql
CREATE TABLE audit_trail (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    action VARCHAR(50) NOT NULL,
    entity VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100),
    before JSONB,
    after JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_action ON audit_trail(action);
CREATE INDEX idx_audit_entity ON audit_trail(entity, entity_id);
CREATE INDEX idx_audit_created ON audit_trail(created_at DESC);
CREATE INDEX idx_audit_user ON audit_trail(user_id, created_at DESC);
```

### loss_ledger
```sql
CREATE TABLE loss_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL,
    pan VARCHAR(10) NOT NULL,
    head VARCHAR(30) NOT NULL,
    source_ay VARCHAR(7) NOT NULL,
    original_amount INTEGER NOT NULL DEFAULT 0,
    set_off_against INTEGER NOT NULL DEFAULT 0,
    cf_years_remaining INTEGER NOT NULL DEFAULT 8,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_loss_client FOREIGN KEY (client_id) 
        REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT uq_loss_client_head_ay UNIQUE (client_id, head, source_ay)
);

CREATE INDEX idx_loss_client_pan ON loss_ledger(client_id, pan);
CREATE INDEX idx_loss_active ON loss_ledger(is_active) WHERE is_active = TRUE;
```

---

## Bulk Operations

### bulk_import_jobs
```sql
CREATE TABLE bulk_import_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(255) NOT NULL,
    total_rows INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    errors JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    committed_at TIMESTAMPTZ,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_bulk_created_by FOREIGN KEY (created_by) 
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT bulk_status_check CHECK (status IN (
        'pending', 'processing', 'completed', 'failed'
    ))
);

CREATE INDEX idx_bulk_status ON bulk_import_jobs(status, created_at DESC);
CREATE INDEX idx_bulk_created_by ON bulk_import_jobs(created_by);
```

### backup_records
```sql
CREATE TABLE backup_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(255) NOT NULL UNIQUE,
    storage_path VARCHAR(500) NOT NULL,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    sha256 VARCHAR(64) NOT NULL,
    backup_type VARCHAR(20) NOT NULL DEFAULT 'manual',
    target VARCHAR(50) NOT NULL DEFAULT 'local',
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_backup_created_by FOREIGN KEY (created_by) 
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT backup_type_check CHECK (backup_type IN (
        'manual', 'auto', 'scheduled'
    ))
);

CREATE INDEX idx_backup_created ON backup_records(created_at DESC);
```

---

## Migration Plan

### Phase 1: Fix Existing Schema
```sql
-- 0003_add_foreign_keys.py
ALTER TABLE clients 
    ADD CONSTRAINT fk_clients_assigned_user 
    FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE itr_filings 
    ADD CONSTRAINT fk_filings_client 
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE;

-- etc.
```

### Phase 2: Add Missing Constraints
```sql
-- 0004_add_unique_constraints.py
ALTER TABLE form_26as_data 
    ADD CONSTRAINT uq_26as_client_ay UNIQUE (client_id, ay);

ALTER TABLE ais_data 
    ADD CONSTRAINT uq_ais_client_ay_source UNIQUE (client_id, ay, source);
```

### Phase 3: Add Composite Indexes
```sql
-- 0005_add_composite_indexes.py
CREATE INDEX idx_filings_client_ay ON itr_filings(client_id, ay);
CREATE INDEX idx_snapshots_client_ay ON computed_return_snapshots(client_id, ay);
```

### Phase 4: New Tables
```sql
-- 0006_create_rule_versions.py
CREATE TABLE rule_versions (...);

-- 0007_create_filing_snapshots_metadata.py
CREATE TABLE filing_snapshots_metadata (...);
```

---

## Performance Tuning

### Connection Pool
```python
# settings.py
SQLALCHEMY_POOL_SIZE = 20
SQLALCHEMY_MAX_OVERFLOW = 10
SQLALCHEMY_POOL_TIMEOUT = 30
SQLALCHEMY_POOL_RECYCLE = 3600
```

### Query Optimization
```sql
-- Analyze query plans
EXPLAIN ANALYZE 
SELECT * FROM itr_filings 
WHERE client_id = 'uuid' AND ay = '2026-27';

-- Create partial indexes for common queries
CREATE INDEX idx_filings_draft 
    ON itr_filings(client_id, created_at DESC) 
    WHERE status = 'draft';
```

---

**Schema Version:** 1.0  
**Last Updated:** 2026-07-10
