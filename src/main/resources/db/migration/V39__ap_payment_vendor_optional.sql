-- Make vendor_id optional on ap_payments to support non-vendor expenses
ALTER TABLE ap_payments ALTER COLUMN vendor_id DROP NOT NULL;
