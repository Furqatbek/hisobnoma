-- Link coupon redemptions to online orders (POS transaction link stays for POS redemptions)
ALTER TABLE coupon_redemptions ADD COLUMN IF NOT EXISTS web_order_id BIGINT NULL REFERENCES web_orders(id);
CREATE INDEX IF NOT EXISTS idx_coupon_redemptions_web_order ON coupon_redemptions (web_order_id);

-- Coupon snapshot on online orders
ALTER TABLE web_orders ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(50);
ALTER TABLE web_orders ADD COLUMN IF NOT EXISTS coupon_discount NUMERIC(18,4) NOT NULL DEFAULT 0;
