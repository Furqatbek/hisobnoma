-- Sales channel for promotions: POS (default, existing behaviour), WEB (online shop), ALL
ALTER TABLE promotions ADD COLUMN IF NOT EXISTS channel VARCHAR(10) NOT NULL DEFAULT 'POS';
CREATE INDEX IF NOT EXISTS idx_promotions_channel ON promotions (tenant_id, channel, is_active);

-- Promotion discount snapshot on online orders
ALTER TABLE web_orders ADD COLUMN IF NOT EXISTS discount_total NUMERIC(18,4) NOT NULL DEFAULT 0;
ALTER TABLE web_orders ADD COLUMN IF NOT EXISTS applied_promotions VARCHAR(500);
