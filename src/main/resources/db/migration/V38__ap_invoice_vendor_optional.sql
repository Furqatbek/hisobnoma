-- Make vendor_id optional on ap_invoices and ap_payments to support non-vendor expenses
-- (rent, utilities, misc expenses, etc.)
ALTER TABLE ap_invoices ALTER COLUMN vendor_id DROP NOT NULL;
ALTER TABLE ap_payments ALTER COLUMN vendor_id DROP NOT NULL;
