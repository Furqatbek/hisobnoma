-- Agents collecting cash against outstanding AR invoices during a visit.
ALTER TABLE distribution_visits ADD COLUMN collected_amount NUMERIC(19,4);
ALTER TABLE distribution_visits ADD COLUMN ar_payment_id BIGINT;
