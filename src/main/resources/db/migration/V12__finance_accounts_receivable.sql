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
    name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(200),
    tax_id VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(50),
    alt_phone VARCHAR(50),
    mobile_phone VARCHAR(50),
    website VARCHAR(255),

    -- Address (simplified)
    address VARCHAR(500),
    shipping_address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),

    -- Terms and Limits
    payment_terms VARCHAR(100),
    payment_terms_days INTEGER,
    credit_limit DECIMAL(18, 4),
    credit_hold BOOLEAN NOT NULL DEFAULT FALSE,

    -- Balances
    current_balance DECIMAL(18, 4) NOT NULL DEFAULT 0,
    total_invoiced DECIMAL(18, 4) NOT NULL DEFAULT 0,
    total_received DECIMAL(18, 4) NOT NULL DEFAULT 0,

    -- Currency and Accounts
    default_currency VARCHAR(3) DEFAULT 'UZS',
    ar_account_id BIGINT,
    revenue_account_id BIGINT,
    price_list_id BIGINT,
    discount_percent DECIMAL(5, 2),

    -- Classification
    customer_type VARCHAR(50),
    sales_rep_id BIGINT,

    -- Status
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(1000),
    internal_notes VARCHAR(1000),

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    CONSTRAINT uk_customers_code_tenant UNIQUE (code, tenant_id)
);

CREATE INDEX idx_customers_tenant ON customers(tenant_id);
CREATE INDEX idx_customers_code ON customers(code);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_active ON customers(tenant_id, active);
CREATE INDEX idx_customers_credit_hold ON customers(tenant_id, credit_hold);

-- ============================================
-- CUSTOMER CONTACTS TABLE
-- ============================================
CREATE TABLE customer_contacts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    title VARCHAR(100),
    department VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(50),
    mobile_phone VARCHAR(50),
    fax VARCHAR(50),

    -- Role Flags
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_billing_contact BOOLEAN NOT NULL DEFAULT FALSE,
    is_shipping_contact BOOLEAN NOT NULL DEFAULT FALSE,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_customer_contacts_customer FOREIGN KEY (customer_id)
        REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_contacts_customer ON customer_contacts(customer_id);
CREATE INDEX idx_customer_contacts_primary ON customer_contacts(customer_id, is_primary);

-- ============================================
-- AR INVOICES TABLE
-- ============================================
CREATE TABLE ar_invoices (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    invoice_number VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(200),
    customer_email VARCHAR(100),

    -- Dates
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    -- POS Integration
    pos_transaction_id BIGINT,
    pos_transaction_number VARCHAR(50),

    -- Sales Order Reference
    sales_order_id BIGINT,
    sales_order_number VARCHAR(50),

    -- Amounts
    subtotal DECIMAL(18, 4) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(18, 4) DEFAULT 0,
    discount_percent DECIMAL(5, 2),
    tax_amount DECIMAL(18, 4) DEFAULT 0,
    shipping_amount DECIMAL(18, 4) DEFAULT 0,
    total_amount DECIMAL(18, 4) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(18, 4) NOT NULL DEFAULT 0,
    credit_applied DECIMAL(18, 4) DEFAULT 0,
    balance_due DECIMAL(18, 4) NOT NULL DEFAULT 0,

    -- Currency
    currency VARCHAR(3) DEFAULT 'UZS',
    exchange_rate DECIMAL(18, 8) DEFAULT 1,

    -- Terms
    payment_terms VARCHAR(100),
    payment_terms_days INTEGER,

    -- Account References
    ar_account_id BIGINT,
    revenue_account_id BIGINT,

    -- Description and Notes
    description VARCHAR(1000),
    notes VARCHAR(1000),
    internal_notes VARCHAR(1000),

    -- Addresses
    billing_address VARCHAR(500),
    shipping_address VARCHAR(500),

    -- GL References
    gl_posted BOOLEAN NOT NULL DEFAULT FALSE,
    gl_journal_entry_id BIGINT,
    gl_posted_at TIMESTAMP,

    -- Sent tracking
    sent_at TIMESTAMP,
    sent_by BIGINT,

    -- Cancellation
    cancelled_by BIGINT,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),

    -- Write-off
    written_off_by BIGINT,
    written_off_at TIMESTAMP,
    write_off_reason VARCHAR(500),

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
CREATE INDEX idx_ar_invoices_number ON ar_invoices(invoice_number);
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
    quantity DECIMAL(18, 4) NOT NULL,
    unit_price DECIMAL(18, 4) NOT NULL,
    unit_cost DECIMAL(18, 4),

    -- Calculations
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    tax_amount DECIMAL(18, 4) DEFAULT 0,
    line_total DECIMAL(18, 4) NOT NULL,

    -- Margin
    profit_margin DECIMAL(18, 2),

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
    customer_name VARCHAR(200),

    -- Original Invoice Reference
    original_invoice_id BIGINT,
    original_invoice_number VARCHAR(50),

    -- Dates
    credit_note_date DATE NOT NULL,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    -- Reason
    reason_code VARCHAR(50),
    reason VARCHAR(500),

    -- Amounts
    credit_amount DECIMAL(18, 4) NOT NULL,
    subtotal DECIMAL(18, 4),
    tax_amount DECIMAL(18, 4),
    total_amount DECIMAL(18, 4),
    applied_amount DECIMAL(18, 4) NOT NULL DEFAULT 0,
    balance DECIMAL(18, 4) NOT NULL,
    refunded_amount DECIMAL(18, 4) DEFAULT 0,

    -- Currency
    currency VARCHAR(3) DEFAULT 'UZS',
    exchange_rate DECIMAL(18, 8) DEFAULT 1,

    -- Account References
    ar_account_id BIGINT,
    revenue_account_id BIGINT,

    description VARCHAR(1000),
    notes VARCHAR(1000),

    -- GL References
    gl_posted BOOLEAN NOT NULL DEFAULT FALSE,
    gl_journal_entry_id BIGINT,
    gl_posted_at TIMESTAMP,

    -- Approval
    approved_by BIGINT,
    approved_at TIMESTAMP,

    -- Cancellation
    cancelled_by BIGINT,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),

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
CREATE INDEX idx_credit_notes_number ON credit_notes(credit_note_number);
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
    customer_name VARCHAR(200),

    -- Payment Details
    payment_date DATE NOT NULL,
    payment_amount DECIMAL(18, 4) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,

    -- Allocation Tracking
    allocated_amount DECIMAL(18, 4) NOT NULL DEFAULT 0,
    unallocated_amount DECIMAL(18, 4) NOT NULL,

    -- Currency
    currency VARCHAR(3) DEFAULT 'UZS',
    exchange_rate DECIMAL(18, 8) DEFAULT 1,

    -- Account References
    bank_account_id BIGINT,
    bank_account_name VARCHAR(200),
    cash_account_id BIGINT,
    ar_account_id BIGINT,

    -- Reference Numbers
    reference_number VARCHAR(50),
    check_number VARCHAR(50),
    check_date DATE,
    card_last_four VARCHAR(4),
    card_type VARCHAR(20),
    transaction_id VARCHAR(100),

    memo VARCHAR(200),
    notes VARCHAR(1000),

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- GL References
    gl_posted BOOLEAN NOT NULL DEFAULT FALSE,
    gl_journal_entry_id BIGINT,
    gl_posted_at TIMESTAMP,

    -- Deposit
    deposited BOOLEAN NOT NULL DEFAULT FALSE,
    deposited_at TIMESTAMP,
    deposited_by BIGINT,
    deposit_reference VARCHAR(100),

    -- Processing
    processed_by BIGINT,
    processed_at TIMESTAMP,

    -- Refund
    refunded_by BIGINT,
    refunded_at TIMESTAMP,
    refund_reason VARCHAR(500),

    -- Cancellation
    cancelled_by BIGINT,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),

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
CREATE INDEX idx_ar_payments_number ON ar_payments(payment_number);
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

    -- Invoice Reference
    invoice_number VARCHAR(50),
    invoice_amount DECIMAL(18, 4),
    invoice_balance_before DECIMAL(18, 4),

    -- Amounts
    allocated_amount DECIMAL(18, 4) NOT NULL,
    discount_taken DECIMAL(18, 4) DEFAULT 0,
    write_off_amount DECIMAL(18, 4) DEFAULT 0,

    -- Balance After
    invoice_balance_after DECIMAL(18, 4),
    notes VARCHAR(500),

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
INSERT INTO permissions (name, code, description, module, action, created_at, updated_at) VALUES
-- Customer permissions
('View Customers', 'FINANCE_AR_CUSTOMER_VIEW', 'Can view customer master records', 'FINANCE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Manage Customers', 'FINANCE_AR_CUSTOMER_MANAGE', 'Can create, update, delete customers', 'FINANCE', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- AR Invoice permissions
('View AR Invoices', 'FINANCE_AR_READ', 'Can view accounts receivable invoices', 'FINANCE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Create AR Invoices', 'FINANCE_AR_WRITE', 'Can create and update accounts receivable invoices', 'FINANCE', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Approve AR Invoices', 'FINANCE_AR_APPROVE', 'Can post and cancel accounts receivable invoices', 'FINANCE', 'APPROVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- AR Payment permissions
('View AR Payments', 'FINANCE_AR_PAY_VIEW', 'Can view accounts receivable payments', 'FINANCE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Create AR Payments', 'FINANCE_AR_PAY', 'Can create and process accounts receivable payments', 'FINANCE', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Complete AR Payments', 'FINANCE_AR_PAY_COMPLETE', 'Can complete accounts receivable payments', 'FINANCE', 'APPROVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Credit Note permissions
('View Credit Notes', 'FINANCE_AR_CREDIT_VIEW', 'Can view credit notes', 'FINANCE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Create Credit Notes', 'FINANCE_AR_CREDIT_CREATE', 'Can create credit notes', 'FINANCE', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Approve Credit Notes', 'FINANCE_AR_CREDIT_APPROVE', 'Can approve and apply credit notes', 'FINANCE', 'APPROVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- AR Reports
('View Customer Statements', 'FINANCE_AR_STATEMENT', 'Can view customer statements and balance', 'FINANCE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Assign AR permissions to roles
-- SUPER_ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN'
AND p.code IN ('FINANCE_AR_CUSTOMER_VIEW', 'FINANCE_AR_CUSTOMER_MANAGE',
               'FINANCE_AR_READ', 'FINANCE_AR_WRITE', 'FINANCE_AR_APPROVE',
               'FINANCE_AR_PAY_VIEW', 'FINANCE_AR_PAY', 'FINANCE_AR_PAY_COMPLETE',
               'FINANCE_AR_CREDIT_VIEW', 'FINANCE_AR_CREDIT_CREATE', 'FINANCE_AR_CREDIT_APPROVE',
               'FINANCE_AR_STATEMENT')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN'
AND p.code IN ('FINANCE_AR_CUSTOMER_VIEW', 'FINANCE_AR_CUSTOMER_MANAGE',
               'FINANCE_AR_READ', 'FINANCE_AR_WRITE', 'FINANCE_AR_APPROVE',
               'FINANCE_AR_PAY_VIEW', 'FINANCE_AR_PAY', 'FINANCE_AR_PAY_COMPLETE',
               'FINANCE_AR_CREDIT_VIEW', 'FINANCE_AR_CREDIT_CREATE', 'FINANCE_AR_CREDIT_APPROVE',
               'FINANCE_AR_STATEMENT')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- FINANCE_MANAGER gets all AR permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'FINANCE_MANAGER'
AND p.code IN ('FINANCE_AR_CUSTOMER_VIEW', 'FINANCE_AR_CUSTOMER_MANAGE',
               'FINANCE_AR_READ', 'FINANCE_AR_WRITE', 'FINANCE_AR_APPROVE',
               'FINANCE_AR_PAY_VIEW', 'FINANCE_AR_PAY', 'FINANCE_AR_PAY_COMPLETE',
               'FINANCE_AR_CREDIT_VIEW', 'FINANCE_AR_CREDIT_CREATE', 'FINANCE_AR_CREDIT_APPROVE',
               'FINANCE_AR_STATEMENT')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ACCOUNTANT gets view, create (no approve)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ACCOUNTANT'
AND p.code IN ('FINANCE_AR_CUSTOMER_VIEW',
               'FINANCE_AR_READ', 'FINANCE_AR_WRITE',
               'FINANCE_AR_PAY_VIEW', 'FINANCE_AR_PAY',
               'FINANCE_AR_CREDIT_VIEW', 'FINANCE_AR_CREDIT_CREATE',
               'FINANCE_AR_STATEMENT')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- VIEWER gets read-only access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VIEWER'
AND p.code IN ('FINANCE_AR_CUSTOMER_VIEW', 'FINANCE_AR_READ', 'FINANCE_AR_PAY_VIEW',
               'FINANCE_AR_CREDIT_VIEW', 'FINANCE_AR_STATEMENT')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ============================================
-- TABLE COMMENTS
-- ============================================
COMMENT ON TABLE customers IS 'Customer master records for accounts receivable';
COMMENT ON TABLE customer_contacts IS 'Contact persons for each customer';
COMMENT ON TABLE ar_invoices IS 'Accounts receivable invoices';
COMMENT ON TABLE ar_invoice_lines IS 'Line items for AR invoices';
COMMENT ON TABLE credit_notes IS 'Credit notes for refunds and adjustments';
COMMENT ON TABLE ar_payments IS 'Customer payment receipts';
COMMENT ON TABLE ar_payment_allocations IS 'Allocation of payments to invoices';
