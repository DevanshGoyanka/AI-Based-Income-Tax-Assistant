-- V7__add_filing_snapshot_columns.sql
-- Add columns to itr_filing for snapshot support per Document 1 §8

ALTER TABLE itr_filing ADD COLUMN IF NOT EXISTS computed_return_json JSONB;
ALTER TABLE itr_filing ADD COLUMN IF NOT EXISTS rules_version VARCHAR(20);
ALTER TABLE itr_filing ADD COLUMN IF NOT EXISTS itr_form_version VARCHAR(20);
ALTER TABLE itr_filing ADD COLUMN IF NOT EXISTS json_schema_version VARCHAR(20);
ALTER TABLE itr_filing ADD COLUMN IF NOT EXISTS trigger_action VARCHAR(50);
ALTER TABLE itr_filing ADD COLUMN IF NOT EXISTS computation_hash VARCHAR(64);
ALTER TABLE itr_filing ADD COLUMN IF NOT EXISTS previous_snapshot_id BIGINT REFERENCES itr_filing(id);

COMMENT ON COLUMN itr_filing.computed_return_json IS 'Full ComputedReturn serialized - immutable once written';
COMMENT ON COLUMN itr_filing.computation_hash IS 'SHA-256 hash for audit chain per Document 1 §8.2';
COMMENT ON COLUMN itr_filing.trigger_action IS 'DRAFT_SAVE, JSON_EXPORT, PDF_EXPORT, FILED, REVISED';
