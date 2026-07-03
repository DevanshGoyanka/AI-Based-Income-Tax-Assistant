-- V9: Create validation_report table for storing validation results
-- Document 3 Phase 3

CREATE TABLE validation_report (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES clients(id),
    assessment_year VARCHAR(10) NOT NULL,
    validated_at TIMESTAMP NOT NULL,
    blocking_count INTEGER NOT NULL DEFAULT 0,
    warning_count INTEGER NOT NULL DEFAULT 0,
    report_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_validation_client_ay (client_id, assessment_year)
);
