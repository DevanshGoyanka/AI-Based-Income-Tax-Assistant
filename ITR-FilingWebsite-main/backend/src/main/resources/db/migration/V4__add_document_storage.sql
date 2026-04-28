-- Migration: Add imported document storage columns to client_year_data
-- Section 10 of FIXES_PART2_PDF_IMPORT_CLEANUP.md

ALTER TABLE client_year_data
  ADD COLUMN IF NOT EXISTS ais_data JSONB,
  ADD COLUMN IF NOT EXISTS form26as_data JSONB,
  ADD COLUMN IF NOT EXISTS tis_data JSONB,
  ADD COLUMN IF NOT EXISTS import_timestamp TIMESTAMP,
  ADD COLUMN IF NOT EXISTS reconciliation_report JSONB;

-- Indexes for faster JSON queries
CREATE INDEX IF NOT EXISTS idx_cyd_ais ON client_year_data USING GIN (ais_data);
CREATE INDEX IF NOT EXISTS idx_cyd_26as ON client_year_data USING GIN (form26as_data);
