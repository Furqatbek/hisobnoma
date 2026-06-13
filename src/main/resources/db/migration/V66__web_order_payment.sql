-- Phase 7: online order payment metadata + delivery address.
-- Foundation for the card-payment flow (Payme/Click/Uzum); also fixes the
-- mobile app's required address field having nowhere to be stored.

ALTER TABLE web_orders ADD COLUMN delivery_address VARCHAR(500);
ALTER TABLE web_orders ADD COLUMN payment_method   VARCHAR(10)  NOT NULL DEFAULT 'CASH';
ALTER TABLE web_orders ADD COLUMN payment_status   VARCHAR(20)  NOT NULL DEFAULT 'NONE';
