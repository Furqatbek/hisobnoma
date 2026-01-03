-- =====================================================
-- Block 7: Finance Module - Accounts Payable
-- =====================================================

-- Add default expense account to vendors
ALTER TABLE vendors ADD COLUMN IF NOT EXISTS default_expense_account_id BIGINT;
ALTER TABLE vendors ADD CONSTRAINT fk_vendors_expense_account
    FOREIGN KEY (default_expense_account_id) REFERENCES accounts(id);

-- =====================================================
-- Vendor Contacts Table
-- =====================================================
CREATE TABLE IF NOT EXISTS vendor_contacts (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id),
    name VARCHAR(100) NOT NULL,
    title VARCHAR(100),
    department VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(50),
    mobile_phone VARCHAR(50),
    fax VARCHAR(50),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_billing_contact BOOLEAN NOT NULL DEFAULT FALSE,
    is_ordering_contact BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vendor_contacts_vendor ON vendor_contacts(vendor_id);
CREATE INDEX idx_vendor_contacts_primary ON vendor_contacts(is_primary);

-- =====================================================
-- AP Invoices Table
-- =====================================================
CREATE TABLE IF NOT EXISTS ap_invoices (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    invoice_number VARCHAR(50) NOT NULL,
    vendor_invoice_number VARCHAR(100),
    vendor_id BIGINT NOT NULL REFERENCES vendors(id),
    vendor_name VARCHAR(200),
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    received_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    purchase_order_id BIGINT REFERENCES purchase_orders(id),
    purchase_order_number VARCHAR(50),
    receiving_order_id BIGINT REFERENCES receiving_orders(id),
    receiving_order_number VARCHAR(50),
    subtotal DECIMAL(18,4) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(18,4) DEFAULT 0,
    tax_amount DECIMAL(18,4) DEFAULT 0,
    shipping_amount DECIMAL(18,4) DEFAULT 0,
    total_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(18,4) DEFAULT 0,
    balance_due DECIMAL(18,4) DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'UZS',
    exchange_rate DECIMAL(18,8) DEFAULT 1,
    payment_terms VARCHAR(100),
    payment_terms_days INTEGER,
    expense_account_id BIGINT REFERENCES accounts(id),
    ap_account_id BIGINT REFERENCES accounts(id),
    description VARCHAR(1000),
    notes VARCHAR(1000),
    internal_notes VARCHAR(1000),
    matching_status VARCHAR(20),
    matching_notes VARCHAR(500),
    gl_posted BOOLEAN NOT NULL DEFAULT FALSE,
    gl_journal_entry_id BIGINT REFERENCES journal_entries(id),
    gl_posted_at TIMESTAMP,
    submitted_by BIGINT REFERENCES users(id),
    submitted_at TIMESTAMP,
    approved_by BIGINT REFERENCES users(id),
    approved_at TIMESTAMP,
    rejected_by BIGINT REFERENCES users(id),
    rejected_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    cancelled_by BIGINT REFERENCES users(id),
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ap_invoices_tenant ON ap_invoices(tenant_id);
CREATE INDEX idx_ap_invoices_number ON ap_invoices(invoice_number);
CREATE INDEX idx_ap_invoices_vendor ON ap_invoices(vendor_id);
CREATE INDEX idx_ap_invoices_status ON ap_invoices(status);
CREATE INDEX idx_ap_invoices_due_date ON ap_invoices(due_date);
CREATE INDEX idx_ap_invoices_po ON ap_invoices(purchase_order_id);
CREATE INDEX idx_ap_invoices_receiving ON ap_invoices(receiving_order_id);
CREATE UNIQUE INDEX idx_ap_invoices_tenant_number ON ap_invoices(tenant_id, invoice_number);

-- =====================================================
-- AP Invoice Lines Table
-- =====================================================
CREATE TABLE IF NOT EXISTS ap_invoice_lines (
    id BIGSERIAL PRIMARY KEY,
    ap_invoice_id BIGINT NOT NULL REFERENCES ap_invoices(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    product_id BIGINT REFERENCES products(id),
    product_sku VARCHAR(50),
    product_name VARCHAR(200),
    description VARCHAR(500) NOT NULL,
    expense_account_id BIGINT REFERENCES accounts(id),
    expense_account_code VARCHAR(20),
    quantity DECIMAL(18,4) NOT NULL,
    unit_of_measure VARCHAR(20),
    unit_price DECIMAL(18,4) NOT NULL,
    discount_percent DECIMAL(5,2),
    discount_amount DECIMAL(18,4) DEFAULT 0,
    line_total DECIMAL(18,4) NOT NULL DEFAULT 0,
    tax_code VARCHAR(50),
    tax_rate DECIMAL(5,2),
    tax_amount DECIMAL(18,4) DEFAULT 0,
    purchase_order_line_id BIGINT,
    receiving_line_id BIGINT,
    ordered_quantity DECIMAL(18,4),
    received_quantity DECIMAL(18,4),
    variance_quantity DECIMAL(18,4),
    variance_amount DECIMAL(18,4),
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ap_invoice_lines_invoice ON ap_invoice_lines(ap_invoice_id);
CREATE INDEX idx_ap_invoice_lines_product ON ap_invoice_lines(product_id);
CREATE INDEX idx_ap_invoice_lines_account ON ap_invoice_lines(expense_account_id);

-- =====================================================
-- AP Payments Table
-- =====================================================
CREATE TABLE IF NOT EXISTS ap_payments (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    payment_number VARCHAR(50) NOT NULL,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id),
    vendor_name VARCHAR(200),
    payment_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    payment_method VARCHAR(20) NOT NULL,
    payment_amount DECIMAL(18,4) NOT NULL,
    allocated_amount DECIMAL(18,4) DEFAULT 0,
    unallocated_amount DECIMAL(18,4) DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'UZS',
    exchange_rate DECIMAL(18,8) DEFAULT 1,
    bank_account_id BIGINT,
    bank_account_name VARCHAR(200),
    cash_account_id BIGINT REFERENCES accounts(id),
    ap_account_id BIGINT REFERENCES accounts(id),
    reference_number VARCHAR(50),
    check_number VARCHAR(50),
    check_date DATE,
    memo VARCHAR(200),
    notes VARCHAR(1000),
    gl_posted BOOLEAN NOT NULL DEFAULT FALSE,
    gl_journal_entry_id BIGINT REFERENCES journal_entries(id),
    gl_posted_at TIMESTAMP,
    reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    reconciled_at TIMESTAMP,
    reconciled_by BIGINT REFERENCES users(id),
    submitted_by BIGINT REFERENCES users(id),
    submitted_at TIMESTAMP,
    approved_by BIGINT REFERENCES users(id),
    approved_at TIMESTAMP,
    completed_by BIGINT REFERENCES users(id),
    completed_at TIMESTAMP,
    voided_by BIGINT REFERENCES users(id),
    voided_at TIMESTAMP,
    void_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ap_payments_tenant ON ap_payments(tenant_id);
CREATE INDEX idx_ap_payments_number ON ap_payments(payment_number);
CREATE INDEX idx_ap_payments_vendor ON ap_payments(vendor_id);
CREATE INDEX idx_ap_payments_status ON ap_payments(status);
CREATE INDEX idx_ap_payments_date ON ap_payments(payment_date);
CREATE INDEX idx_ap_payments_bank ON ap_payments(bank_account_id);
CREATE UNIQUE INDEX idx_ap_payments_tenant_number ON ap_payments(tenant_id, payment_number);

-- =====================================================
-- AP Payment Allocations Table
-- =====================================================
CREATE TABLE IF NOT EXISTS ap_payment_allocations (
    id BIGSERIAL PRIMARY KEY,
    ap_payment_id BIGINT NOT NULL REFERENCES ap_payments(id) ON DELETE CASCADE,
    ap_invoice_id BIGINT NOT NULL REFERENCES ap_invoices(id),
    invoice_number VARCHAR(50),
    invoice_amount DECIMAL(18,4),
    invoice_balance_before DECIMAL(18,4),
    allocated_amount DECIMAL(18,4) NOT NULL,
    discount_taken DECIMAL(18,4) DEFAULT 0,
    write_off_amount DECIMAL(18,4) DEFAULT 0,
    invoice_balance_after DECIMAL(18,4),
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ap_alloc_payment ON ap_payment_allocations(ap_payment_id);
CREATE INDEX idx_ap_alloc_invoice ON ap_payment_allocations(ap_invoice_id);

-- =====================================================
-- AP Module Permissions
-- =====================================================
INSERT INTO permissions (name, code, description, module, action, created_at, updated_at) VALUES
-- AP Invoice permissions
('View AP Invoices', 'FINANCE_AP_VIEW', 'Can view accounts payable invoices', 'FINANCE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Create AP Invoices', 'FINANCE_AP_CREATE', 'Can create accounts payable invoices', 'FINANCE', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Update AP Invoices', 'FINANCE_AP_UPDATE', 'Can update accounts payable invoices', 'FINANCE', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Approve AP Invoices', 'FINANCE_AP_APPROVE', 'Can approve accounts payable invoices', 'FINANCE', 'APPROVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Cancel AP Invoices', 'FINANCE_AP_CANCEL', 'Can cancel accounts payable invoices', 'FINANCE', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- AP Payment permissions
('View AP Payments', 'FINANCE_AP_PAY_VIEW', 'Can view accounts payable payments', 'FINANCE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Create AP Payments', 'FINANCE_AP_PAY', 'Can create and process accounts payable payments', 'FINANCE', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Approve AP Payments', 'FINANCE_AP_PAY_APPROVE', 'Can approve accounts payable payments', 'FINANCE', 'APPROVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Void AP Payments', 'FINANCE_AP_PAY_VOID', 'Can void accounts payable payments', 'FINANCE', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Vendor permissions (extending inventory vendor permissions)
('View Vendor Statements', 'FINANCE_VENDOR_STATEMENT', 'Can view vendor statements and balance', 'FINANCE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Assign AP permissions to roles
-- SUPER_ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN'
AND p.code IN ('FINANCE_AP_VIEW', 'FINANCE_AP_CREATE', 'FINANCE_AP_UPDATE', 'FINANCE_AP_APPROVE', 'FINANCE_AP_CANCEL',
               'FINANCE_AP_PAY_VIEW', 'FINANCE_AP_PAY', 'FINANCE_AP_PAY_APPROVE', 'FINANCE_AP_PAY_VOID', 'FINANCE_VENDOR_STATEMENT')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN'
AND p.code IN ('FINANCE_AP_VIEW', 'FINANCE_AP_CREATE', 'FINANCE_AP_UPDATE', 'FINANCE_AP_APPROVE', 'FINANCE_AP_CANCEL',
               'FINANCE_AP_PAY_VIEW', 'FINANCE_AP_PAY', 'FINANCE_AP_PAY_APPROVE', 'FINANCE_AP_PAY_VOID', 'FINANCE_VENDOR_STATEMENT')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- FINANCE_MANAGER gets all AP permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'FINANCE_MANAGER'
AND p.code IN ('FINANCE_AP_VIEW', 'FINANCE_AP_CREATE', 'FINANCE_AP_UPDATE', 'FINANCE_AP_APPROVE', 'FINANCE_AP_CANCEL',
               'FINANCE_AP_PAY_VIEW', 'FINANCE_AP_PAY', 'FINANCE_AP_PAY_APPROVE', 'FINANCE_AP_PAY_VOID', 'FINANCE_VENDOR_STATEMENT')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ACCOUNTANT gets view, create, update (no approve/void)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ACCOUNTANT'
AND p.code IN ('FINANCE_AP_VIEW', 'FINANCE_AP_CREATE', 'FINANCE_AP_UPDATE',
               'FINANCE_AP_PAY_VIEW', 'FINANCE_AP_PAY', 'FINANCE_VENDOR_STATEMENT')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- VIEWER gets read-only access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VIEWER'
AND p.code IN ('FINANCE_AP_VIEW', 'FINANCE_AP_PAY_VIEW', 'FINANCE_VENDOR_STATEMENT')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- =====================================================
-- Create AP control account in default chart of accounts
-- =====================================================
INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, is_control_account,
                     allows_direct_posting, active, currency, created_at, updated_at)
SELECT
    t.id,
    '2100',
    'Accounts Payable',
    'Control account for vendor payables',
    'LIABILITY',
    'CREDIT',
    1,
    '2100',
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    'UZS',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '2100'
);

-- Create Purchase Discount account
INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT
    t.id,
    '5200',
    'Purchase Discounts',
    'Discounts received from vendors',
    'REVENUE',
    'CREDIT',
    1,
    '5200',
    TRUE,
    TRUE,
    TRUE,
    'UZS',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '5200'
);
