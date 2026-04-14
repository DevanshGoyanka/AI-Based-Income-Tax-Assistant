-- Migration V3: Add ITR Forms Support
-- Adds tables for ITR form data storage, PAN validation cache, and TDS deductor details

-- ═══════════════════════════════════════════════════════════════
-- Table: itr_form_data
-- Stores complete ITR form data as JSON for all ITR types (1-4)
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS itr_form_data (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    assessment_year VARCHAR(10) NOT NULL,
    itr_form_type VARCHAR(10) NOT NULL,
    form_data JSONB NOT NULL,
    computation_data JSONB,
    validation_status VARCHAR(20) DEFAULT 'PENDING',
    validation_errors JSONB,
    validation_warnings JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(client_id, assessment_year, itr_form_type)
);

CREATE INDEX idx_itr_form_data_client_year ON itr_form_data(client_id, assessment_year);
CREATE INDEX idx_itr_form_data_form_type ON itr_form_data(itr_form_type);
CREATE INDEX idx_itr_form_data_validation_status ON itr_form_data(validation_status);

COMMENT ON TABLE itr_form_data IS 'Stores complete ITR form data (1-4) as JSON with validation status';
COMMENT ON COLUMN itr_form_data.form_data IS 'Complete ITR form data as JSON (Itr1FormData, Itr2FormData, etc.)';
COMMENT ON COLUMN itr_form_data.computation_data IS 'Tax computation results as JSON';
COMMENT ON COLUMN itr_form_data.validation_status IS 'PENDING, VALID, INVALID, WARNING';

-- ═══════════════════════════════════════════════════════════════
-- Table: pan_validation_cache
-- Caches PAN validation results and entity type detection
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS pan_validation_cache (
    pan VARCHAR(10) PRIMARY KEY,
    entity_type CHAR(1) NOT NULL,
    entity_description VARCHAR(100),
    is_valid BOOLEAN DEFAULT true,
    eligible_itr_forms VARCHAR(50),
    validated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_accessed TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_pan_validation_entity_type ON pan_validation_cache(entity_type);

COMMENT ON TABLE pan_validation_cache IS 'Caches PAN validation and entity type detection results';
COMMENT ON COLUMN pan_validation_cache.entity_type IS 'P=Individual, H=HUF, F=Firm, C=Company, A=AOP, T=Trust, etc.';
COMMENT ON COLUMN pan_validation_cache.eligible_itr_forms IS 'Comma-separated list of eligible ITR forms';

-- ═══════════════════════════════════════════════════════════════
-- Table: tds_deductors
-- Stores TDS deductor details (TAN, name, address)
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS tds_deductors (
    tan VARCHAR(10) PRIMARY KEY,
    deductor_name VARCHAR(200) NOT NULL,
    deductor_address TEXT,
    deductor_type VARCHAR(50),
    deductor_pan VARCHAR(10),
    city VARCHAR(100),
    state VARCHAR(50),
    pin_code VARCHAR(6),
    is_verified BOOLEAN DEFAULT false,
    verification_source VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_tds_deductors_name ON tds_deductors(deductor_name);
CREATE INDEX idx_tds_deductors_type ON tds_deductors(deductor_type);

COMMENT ON TABLE tds_deductors IS 'Master data for TDS deductors (employers, banks, companies)';
COMMENT ON COLUMN tds_deductors.deductor_type IS 'Employer, Bank, Company, Government, Other';
COMMENT ON COLUMN tds_deductors.verification_source IS 'TRACES, Manual, Form16, Form26AS, AIS';

-- ═══════════════════════════════════════════════════════════════
-- Table: itr_classification_history
-- Tracks ITR form classification decisions and changes
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS itr_classification_history (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    assessment_year VARCHAR(10) NOT NULL,
    pan VARCHAR(10) NOT NULL,
    entity_type CHAR(1),
    recommended_form VARCHAR(10),
    selected_form VARCHAR(10),
    classification_reason TEXT,
    income_profile JSONB,
    classified_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_itr_classification_client ON itr_classification_history(client_id, assessment_year);
CREATE INDEX idx_itr_classification_pan ON itr_classification_history(pan);

COMMENT ON TABLE itr_classification_history IS 'Audit trail of ITR form classification decisions';
COMMENT ON COLUMN itr_classification_history.income_profile IS 'IncomeProfile JSON used for classification';

-- ═══════════════════════════════════════════════════════════════
-- Update trigger for itr_form_data.updated_at
-- ═══════════════════════════════════════════════════════════════
CREATE OR REPLACE FUNCTION update_itr_form_data_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_itr_form_data_timestamp
    BEFORE UPDATE ON itr_form_data
    FOR EACH ROW
    EXECUTE FUNCTION update_itr_form_data_timestamp();

-- ═══════════════════════════════════════════════════════════════
-- Update trigger for tds_deductors.last_updated
-- ═══════════════════════════════════════════════════════════════
CREATE OR REPLACE FUNCTION update_tds_deductors_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_tds_deductors_timestamp
    BEFORE UPDATE ON tds_deductors
    FOR EACH ROW
    EXECUTE FUNCTION update_tds_deductors_timestamp();
