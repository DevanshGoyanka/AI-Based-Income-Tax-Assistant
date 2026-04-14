-- Loss Ledger Table - Section 7 ITR_ERP_FINAL_COMPLETION_DIRECTIVE.md
-- Tracks carry-forward losses with 8-year expiry (4-year for speculative)
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

-- Audit Trail Table - Section 7 ITR_ERP_FINAL_COMPLETION_DIRECTIVE.md
-- Persistent audit log for all ITR operations
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
