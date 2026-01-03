-- Finance Module - Accounts Receivable
-- Migration: V12__finance_accounts_receivable.sql
-- Purpose: Create tables for Customer management, AR Invoices, Credit Notes, and Payments

-- ============================================
-- CUSTOMERS TABLE
-- ============================================
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    tax_id VARCHAR(50),
    email VARCHAR(255),
    phone VARCHAR(50),
    website VARCHAR(255),

    -- Billing Address
    billing_address_line1 VARCHAR(255),
    billing_address_line2 VARCHAR(255),
    billing_city VARCHAR(100),
    billing_state VARCHAR(100),
    billing_postal_code VARCHAR(20),
    billing_country VARCHAR(100),

    -- Shipping Address
    shipping_address_line1 VARCHAR(255),
    shipping_address_line2 VARCHAR(255),
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(100),
    shipping_postal_code VARCHAR(20),
    shipping_country VARCHAR(100),

    -- Terms and Limits
    payment_terms INTEGER DEFAULT 30,
    credit_limit DECIMAL(15, 2),
    credit_hold BOOLEAN NOT NULL DEFAULT FALSE,

    -- Balances
    current_balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_invoiced DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_received DECIMAL(15, 2) NOT NULL DEFAULT 0,

    -- Price List Reference
    price_list_id BIGINT,

    -- Status
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    CONSTRAINT uk_customers_code_tenant UNIQUE (code, tenant_id)
);

CREATE INDEX idx_customers_tenant ON customers(tenant_id);
CREATE INDEX idx_customers_active ON customers(tenant_id, active);
CREATE INDEX idx_customers_credit_hold ON customers(tenant_id, credit_hold);

-- ============================================
-- CUSTOMER CONTACTS TABLE
-- ============================================
CREATE TABLE customer_contacts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    title VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(50),
    mobile VARCHAR(50),

    -- Role Flags
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_billing_contact BOOLEAN NOT NULL DEFAULT FALSE,
    is_shipping_contact BOOLEAN NOT NULL DEFAULT FALSE,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_customer_contacts_customer FOREIGN KEY (customer_id)
        REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_contacts_customer ON customer_contacts(customer_id);
CREATE INDEX idx_customer_contacts_email ON customer_contacts(customer_id, email);
CREATE INDEX idx_customer_contacts_primary ON customer_contacts(customer_id, is_primary);

-- ============================================
-- AR INVOICES TABLE
-- ============================================
CREATE TABLE ar_invoices (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    invoice_number VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,

    -- Dates
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,

    -- Amounts
    subtotal DECIMAL(15, 2) NOT NULL DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    discount_amount DECIMAL(15, 2) DEFAULT 0,
    tax_amount DECIMAL(15, 2) DEFAULT 0,
    total_amount DECIMAL(15, 2) NOT NULL,
    paid_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    balance_due DECIMAL(15, 2) NOT NULL,

    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',

    -- Payment Terms
    payment_terms INTEGER,

    -- POS Integration
    pos_transaction_id BIGINT,
    pos_transaction_number VARCHAR(50),

    -- References
    purchase_order_number VARCHAR(100),
    sales_order_id BIGINT,
    sales_order_number VARCHAR(50),

    -- GL Reference
    gl_journal_entry_id BIGINT,

    notes TEXT,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    CONSTRAINT fk_ar_invoices_customer FOREIGN KEY (customer_id)
        REFERENCES customers(id),
    CONSTRAINT fk_ar_invoices_gl_entry FOREIGN KEY (gl_journal_entry_id)
        REFERENCES journal_entries(id),
    CONSTRAINT uk_ar_invoices_number_tenant UNIQUE (invoice_number, tenant_id)
);

CREATE INDEX idx_ar_invoices_tenant ON ar_invoices(tenant_id);
CREATE INDEX idx_ar_invoices_customer ON ar_invoices(tenant_id, customer_id);
CREATE INDEX idx_ar_invoices_status ON ar_invoices(tenant_id, status);
CREATE INDEX idx_ar_invoices_date ON ar_invoices(tenant_id, invoice_date);
CREATE INDEX idx_ar_invoices_due_date ON ar_invoices(tenant_id, due_date);
CREATE INDEX idx_ar_invoices_pos ON ar_invoices(pos_transaction_id);

-- ============================================
-- AR INVOICE LINES TABLE
-- ============================================
CREATE TABLE ar_invoice_lines (
    id BIGSERIAL PRIMARY KEY,
    ar_invoice_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,

    -- Item Reference
    item_id BIGINT,
    description VARCHAR(500) NOT NULL,

    -- Quantities and Prices
    quantity DECIMAL(15, 4) NOT NULL,
    unit_price DECIMAL(15, 4) NOT NULL,
    unit_cost DECIMAL(15, 4),

    -- Calculations
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    tax_amount DECIMAL(15, 2) DEFAULT 0,
    line_total DECIMAL(15, 2) NOT NULL,

    -- Margin
    profit_margin DECIMAL(15, 2),

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ar_invoice_lines_invoice FOREIGN KEY (ar_invoice_id)
        REFERENCES ar_invoices(id) ON DELETE CASCADE
);

CREATE INDEX idx_ar_invoice_lines_invoice ON ar_invoice_lines(ar_invoice_id);
CREATE INDEX idx_ar_invoice_lines_item ON ar_invoice_lines(item_id);

-- ============================================
-- CREDIT NOTES TABLE
-- ============================================
CREATE TABLE credit_notes (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    credit_note_number VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,

    -- Original Invoice Reference
    original_invoice_id BIGINT,

    -- Dates
    credit_note_date DATE NOT NULL,

    -- Amounts
    credit_amount DECIMAL(15, 2) NOT NULL,
    subtotal DECIMAL(15, 2),
    tax_amount DECIMAL(15, 2),
    total_amount DECIMAL(15, 2),
    applied_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    balance DECIMAL(15, 2) NOT NULL,

    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',

    -- Reason
    reason TEXT NOT NULL,

    -- GL Reference
    gl_journal_entry_id BIGINT,

    notes TEXT,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    CONSTRAINT fk_credit_notes_customer FOREIGN KEY (customer_id)
        REFERENCES customers(id),
    CONSTRAINT fk_credit_notes_invoice FOREIGN KEY (original_invoice_id)
        REFERENCES ar_invoices(id),
    CONSTRAINT fk_credit_notes_gl_entry FOREIGN KEY (gl_journal_entry_id)
        REFERENCES journal_entries(id),
    CONSTRAINT uk_credit_notes_number_tenant UNIQUE (credit_note_number, tenant_id)
);

CREATE INDEX idx_credit_notes_tenant ON credit_notes(tenant_id);
CREATE INDEX idx_credit_notes_customer ON credit_notes(tenant_id, customer_id);
CREATE INDEX idx_credit_notes_status ON credit_notes(tenant_id, status);
CREATE INDEX idx_credit_notes_original_invoice ON credit_notes(original_invoice_id);

-- ============================================
-- AR PAYMENTS TABLE
-- ============================================
CREATE TABLE ar_payments (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    payment_number VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,

    -- Payment Details
    payment_date DATE NOT NULL,
    payment_amount DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,

    -- Bank Details
    bank_reference VARCHAR(100),
    check_number VARCHAR(50),
    deposited BOOLEAN NOT NULL DEFAULT FALSE,
    deposit_date DATE,

    -- Allocation Tracking
    allocated_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    unallocated_amount DECIMAL(15, 2) NOT NULL,

    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    -- GL Reference
    gl_journal_entry_id BIGINT,

    notes TEXT,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    CONSTRAINT fk_ar_payments_customer FOREIGN KEY (customer_id)
        REFERENCES customers(id),
    CONSTRAINT fk_ar_payments_gl_entry FOREIGN KEY (gl_journal_entry_id)
        REFERENCES journal_entries(id),
    CONSTRAINT uk_ar_payments_number_tenant UNIQUE (payment_number, tenant_id)
);

CREATE INDEX idx_ar_payments_tenant ON ar_payments(tenant_id);
CREATE INDEX idx_ar_payments_customer ON ar_payments(tenant_id, customer_id);
CREATE INDEX idx_ar_payments_status ON ar_payments(tenant_id, status);
CREATE INDEX idx_ar_payments_date ON ar_payments(tenant_id, payment_date);
CREATE INDEX idx_ar_payments_deposited ON ar_payments(tenant_id, deposited);

-- ============================================
-- AR PAYMENT ALLOCATIONS TABLE
-- ============================================
CREATE TABLE ar_payment_allocations (
    id BIGSERIAL PRIMARY KEY,
    ar_payment_id BIGINT NOT NULL,
    ar_invoice_id BIGINT,
    credit_note_id BIGINT,

    -- Amounts
    allocated_amount DECIMAL(15, 2) NOT NULL,
    discount_taken DECIMAL(15, 2) DEFAULT 0,
    write_off_amount DECIMAL(15, 2) DEFAULT 0,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ar_payment_alloc_payment FOREIGN KEY (ar_payment_id)
        REFERENCES ar_payments(id) ON DELETE CASCADE,
    CONSTRAINT fk_ar_payment_alloc_invoice FOREIGN KEY (ar_invoice_id)
        REFERENCES ar_invoices(id),
    CONSTRAINT fk_ar_payment_alloc_credit_note FOREIGN KEY (credit_note_id)
        REFERENCES credit_notes(id)
);

CREATE INDEX idx_ar_payment_alloc_payment ON ar_payment_allocations(ar_payment_id);
CREATE INDEX idx_ar_payment_alloc_invoice ON ar_payment_allocations(ar_invoice_id);
CREATE INDEX idx_ar_payment_alloc_credit_note ON ar_payment_allocations(credit_note_id);

-- ============================================
-- ADD PERMISSIONS FOR AR MODULE
-- ============================================
INSERT INTO permissions (name, description, resource, action, created_at)
VALUES
    ('FINANCE_AR_READ', 'View AR invoices, payments, customers', 'FINANCE_AR', 'READ', CURRENT_TIMESTAMP),
    ('FINANCE_AR_WRITE', 'Create and edit AR invoices, payments, customers', 'FINANCE_AR', 'WRITE', CURRENT_TIMESTAMP),
    ('FINANCE_AR_APPROVE', 'Post AR invoices and complete payments', 'FINANCE_AR', 'APPROVE', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Add AR permissions to finance roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name IN ('FINANCE_AR_READ', 'FINANCE_AR_WRITE', 'FINANCE_AR_APPROVE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'FINANCE_MANAGER' AND p.name IN ('FINANCE_AR_READ', 'FINANCE_AR_WRITE', 'FINANCE_AR_APPROVE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ACCOUNTANT' AND p.name IN ('FINANCE_AR_READ', 'FINANCE_AR_WRITE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ============================================
-- ADD AR-RELATED ACCOUNTS TO DEFAULT CHART OF ACCOUNTS
-- ============================================
-- Note: These accounts should be added in a tenant-specific setup
-- This is just a reference for the account codes used in GL integration

-- 4200 - Sales Discounts (contra-revenue for discounts given)
-- 4300 - Sales Returns & Allowances (contra-revenue for credit notes)
-- 6200 - Bad Debt Expense (for write-offs)

COMMENT ON TABLE customers IS 'Customer master records for accounts receivable';
COMMENT ON TABLE customer_contacts IS 'Contact persons for each customer';
COMMENT ON TABLE ar_invoices IS 'Accounts receivable invoices';
COMMENT ON TABLE ar_invoice_lines IS 'Line items for AR invoices';
COMMENT ON TABLE credit_notes IS 'Credit notes for refunds and adjustments';
COMMENT ON TABLE ar_payments IS 'Customer payment receipts';
COMMENT ON TABLE ar_payment_allocations IS 'Allocation of payments to invoices';
