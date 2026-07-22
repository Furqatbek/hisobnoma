-- V84: Bind coupons directly to online-shop (web) customers.
--
-- Coupons could only be bound to an AR customer_id, but web customers are phone-first and often
-- have no AR link — staff had no way to issue a personal coupon to an app user. web_customer_id
-- lets a coupon target one web customer; /me/coupons matches it alongside the AR binding.

ALTER TABLE coupons ADD COLUMN IF NOT EXISTS web_customer_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_coupons_web_customer ON coupons(web_customer_id);
