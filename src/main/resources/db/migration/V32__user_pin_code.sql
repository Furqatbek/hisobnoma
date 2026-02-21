-- Add PIN code support for POS touch-based login
ALTER TABLE users ADD COLUMN pin_hash VARCHAR(255);
