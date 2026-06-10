-- Delivery fee per region for online shop orders
ALTER TABLE delivery_regions ADD COLUMN IF NOT EXISTS delivery_fee NUMERIC(18,4) NOT NULL DEFAULT 0;

-- Snapshot of the applied delivery fee on each order
ALTER TABLE web_orders ADD COLUMN IF NOT EXISTS delivery_fee NUMERIC(18,4) NOT NULL DEFAULT 0;
