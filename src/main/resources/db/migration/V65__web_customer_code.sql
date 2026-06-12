-- Phase 7: stable public identity code for online customers.
-- Encoded in the in-app wallet QR as {base}/{tenantSlug}/{customerCode}
-- and used for in-store/POS lookup. Globally unique, derived from the PK.

ALTER TABLE web_customers ADD COLUMN customer_code VARCHAR(20);

-- Backfill existing accounts: WC-00001, WC-00042, ...
UPDATE web_customers
SET customer_code = 'WC-' || LPAD(id::text, 5, '0')
WHERE customer_code IS NULL;

CREATE UNIQUE INDEX uk_web_customers_code
    ON web_customers (customer_code)
    WHERE customer_code IS NOT NULL;
