-- Phase 7: fractional / weight-based products.
-- Lets a product be ordered in decimal quantities (e.g. 0.5 kg) with a defined
-- step; the shop app reads these to render a decimal quantity stepper.

ALTER TABLE products ADD COLUMN fractional BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE products ADD COLUMN order_step NUMERIC(18,4) NOT NULL DEFAULT 1;
