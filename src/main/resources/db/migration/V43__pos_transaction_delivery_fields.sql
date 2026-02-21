-- Add delivery address fields to POS transactions
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS delivery_region_id BIGINT;
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS delivery_region_name VARCHAR(100);
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS delivery_village_id BIGINT;
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS delivery_village_name VARCHAR(100);
