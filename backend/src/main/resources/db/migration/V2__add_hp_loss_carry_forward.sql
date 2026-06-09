-- V2__add_hp_loss_carry_forward.sql
-- Add column to store House Property loss carry forward

ALTER TABLE client_year_data
ADD COLUMN hp_loss_carry_forward BIGINT DEFAULT 0;

COMMENT ON COLUMN client_year_data.hp_loss_carry_forward IS 'House Property loss exceeding ₹2L set-off cap, to be carried forward to next year (Section 71)';
