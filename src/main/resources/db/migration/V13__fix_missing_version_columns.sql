-- V13__fix_missing_columns.sql
-- Fix all missing columns and type mismatches between V11/V12 migrations and entities
--
-- Entity hierarchy:
-- BaseEntity: id, created_at, updated_at, version
-- AuditableEntity extends BaseEntity: + created_by (BIGINT), updated_by (BIGINT)
-- TenantAwareEntity extends AuditableEntity: + tenant_id

-- =====================================================
-- AP MODULE TABLES (V11)
-- =====================================================

-- ap_invoices (extends TenantAwareEntity)
ALTER TABLE ap_invoices ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE ap_invoices ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE ap_invoices ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- ap_payments (extends TenantAwareEntity)
ALTER TABLE ap_payments ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE ap_payments ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE ap_payments ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- ap_invoice_lines (extends BaseEntity)
ALTER TABLE ap_invoice_lines ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ap_payment_allocations (extends BaseEntity)
ALTER TABLE ap_payment_allocations ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- vendor_contacts (extends BaseEntity)
ALTER TABLE vendor_contacts ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- =====================================================
-- AR MODULE - CUSTOMERS TABLE
-- =====================================================
ALTER TABLE customers ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
-- Fix created_by/updated_by type (V12 has VARCHAR, entity needs BIGINT)
ALTER TABLE customers ALTER COLUMN created_by TYPE BIGINT USING NULL;
ALTER TABLE customers ALTER COLUMN updated_by TYPE BIGINT USING NULL;

-- =====================================================
-- AR MODULE - CUSTOMER_CONTACTS TABLE
-- =====================================================
ALTER TABLE customer_contacts ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- =====================================================
-- AR MODULE - AR_INVOICES TABLE
-- =====================================================
ALTER TABLE ar_invoices ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
-- Fix created_by/updated_by type (V12 has VARCHAR, entity needs BIGINT)
ALTER TABLE ar_invoices ALTER COLUMN created_by TYPE BIGINT USING NULL;
ALTER TABLE ar_invoices ALTER COLUMN updated_by TYPE BIGINT USING NULL;

-- =====================================================
-- AR MODULE - AR_INVOICE_LINES TABLE (many missing columns)
-- =====================================================
ALTER TABLE ar_invoice_lines ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE ar_invoice_lines ADD COLUMN IF NOT EXISTS product_id BIGINT;
ALTER TABLE ar_invoice_lines ADD COLUMN IF NOT EXISTS product_sku VARCHAR(50);
ALTER TABLE ar_invoice_lines ADD COLUMN IF NOT EXISTS product_name VARCHAR(200);
ALTER TABLE ar_invoice_lines ADD COLUMN IF NOT EXISTS revenue_account_id BIGINT;
ALTER TABLE ar_invoice_lines ADD COLUMN IF NOT EXISTS revenue_account_code VARCHAR(20);
ALTER TABLE ar_invoice_lines ADD COLUMN IF NOT EXISTS unit_of_measure VARCHAR(20);
ALTER TABLE ar_invoice_lines ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(18, 4) DEFAULT 0;
ALTER TABLE ar_invoice_lines ADD COLUMN IF NOT EXISTS tax_code VARCHAR(50);
ALTER TABLE ar_invoice_lines ADD COLUMN IF NOT EXISTS tax_rate DECIMAL(5, 2);
ALTER TABLE ar_invoice_lines ADD COLUMN IF NOT EXISTS pos_line_id BIGINT;
ALTER TABLE ar_invoice_lines ADD COLUMN IF NOT EXISTS sales_order_line_id BIGINT;
ALTER TABLE ar_invoice_lines ADD COLUMN IF NOT EXISTS notes VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_ar_invoice_lines_product ON ar_invoice_lines(product_id);

-- =====================================================
-- AR MODULE - CREDIT_NOTES TABLE
-- =====================================================
ALTER TABLE credit_notes ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
-- Fix created_by/updated_by type (V12 has VARCHAR, entity needs BIGINT)
ALTER TABLE credit_notes ALTER COLUMN created_by TYPE BIGINT USING NULL;
ALTER TABLE credit_notes ALTER COLUMN updated_by TYPE BIGINT USING NULL;

-- =====================================================
-- AR MODULE - AR_PAYMENTS TABLE
-- =====================================================
ALTER TABLE ar_payments ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
-- Fix created_by/updated_by type (V12 has VARCHAR, entity needs BIGINT)
ALTER TABLE ar_payments ALTER COLUMN created_by TYPE BIGINT USING NULL;
ALTER TABLE ar_payments ALTER COLUMN updated_by TYPE BIGINT USING NULL;

-- =====================================================
-- AR MODULE - AR_PAYMENT_ALLOCATIONS TABLE
-- =====================================================
ALTER TABLE ar_payment_allocations ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
