-- V8__add_rules_version_columns.sql
-- Add AY/rule tracking to client_year_data per Document 1 §5.2

ALTER TABLE client_year_data ADD COLUMN IF NOT EXISTS selected_regime VARCHAR(10) DEFAULT 'NEW';
ALTER TABLE client_year_data ADD COLUMN IF NOT EXISTS rules_version VARCHAR(20);
ALTER TABLE client_year_data ADD COLUMN IF NOT EXISTS selected_itr_form VARCHAR(10);

COMMENT ON COLUMN client_year_data.selected_regime IS 'OLD or NEW - user choice for this AY';
COMMENT ON COLUMN client_year_data.rules_version IS 'TaxYearRules version used for last computation';
