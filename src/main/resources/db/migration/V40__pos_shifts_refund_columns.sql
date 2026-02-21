-- V40: Add missing refund tracking columns to pos_shifts
-- These columns track refund amounts by payment type during a shift

ALTER TABLE pos_shifts ADD COLUMN IF NOT EXISTS cash_refunds NUMERIC(18, 4) DEFAULT 0;
ALTER TABLE pos_shifts ADD COLUMN IF NOT EXISTS card_refunds NUMERIC(18, 4) DEFAULT 0;
ALTER TABLE pos_shifts ADD COLUMN IF NOT EXISTS other_refunds NUMERIC(18, 4) DEFAULT 0;
