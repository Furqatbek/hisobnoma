-- V14__finance_banking_tax.sql
-- Finance Module - Banking & Tax (Block 9)

-- ===========================================
-- Bank Accounts Table
-- ===========================================
CREATE TABLE bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_code VARCHAR(50) NOT NULL,
    account_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    account_type VARCHAR(20) NOT NULL,
    bank_name VARCHAR(200),
    bank_branch VARCHAR(200),
    bank_code VARCHAR(50),
    account_number VARCHAR(50),
    iban VARCHAR(50),
    swift_code VARCHAR(20),
    currency VARCHAR(3) DEFAULT 'UZS',
    current_balance DECIMAL(18,4) DEFAULT 0,
    available_balance DECIMAL(18,4) DEFAULT 0,
    opening_balance DECIMAL(18,4) DEFAULT 0,
    opening_balance_date DATE,
    last_reconciled_date DATE,
    last_reconciled_balance DECIMAL(18,4),
    gl_account_id BIGINT REFERENCES accounts(id),
    contact_name VARCHAR(100),
    contact_phone VARCHAR(50),
    contact_email VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    allow_payments BOOLEAN NOT NULL DEFAULT TRUE,
    allow_receipts BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0,
    notes VARCHAR(1000),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_bank_account_tenant ON bank_accounts(tenant_id);
CREATE INDEX idx_bank_account_code ON bank_accounts(account_code);
CREATE INDEX idx_bank_account_type ON bank_accounts(account_type);
CREATE INDEX idx_bank_account_gl ON bank_accounts(gl_account_id);
CREATE INDEX idx_bank_account_active ON bank_accounts(active);
CREATE UNIQUE INDEX idx_bank_account_code_tenant ON bank_accounts(account_code, tenant_id) WHERE tenant_id IS NOT NULL;

-- ===========================================
-- Bank Reconciliations Table
-- ===========================================
CREATE TABLE bank_reconciliations (
    id BIGSERIAL PRIMARY KEY,
    bank_account_id BIGINT NOT NULL REFERENCES bank_accounts(id) ON DELETE CASCADE,
    reconciliation_number VARCHAR(50) NOT NULL UNIQUE,
    statement_date DATE NOT NULL,
    statement_start_date DATE NOT NULL,
    statement_end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    opening_balance DECIMAL(18,4) NOT NULL,
    statement_ending_balance DECIMAL(18,4) NOT NULL,
    book_balance DECIMAL(18,4),
    cleared_deposits DECIMAL(18,4) DEFAULT 0,
    cleared_withdrawals DECIMAL(18,4) DEFAULT 0,
    outstanding_deposits DECIMAL(18,4) DEFAULT 0,
    outstanding_withdrawals DECIMAL(18,4) DEFAULT 0,
    adjusted_bank_balance DECIMAL(18,4),
    adjusted_book_balance DECIMAL(18,4),
    difference DECIMAL(18,4) DEFAULT 0,
    transactions_cleared INTEGER DEFAULT 0,
    transactions_outstanding INTEGER DEFAULT 0,
    completed_at TIMESTAMP,
    completed_by BIGINT,
    cancelled_at TIMESTAMP,
    cancelled_by BIGINT,
    cancellation_reason VARCHAR(500),
    notes VARCHAR(1000),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_bank_recon_tenant ON bank_reconciliations(tenant_id);
CREATE INDEX idx_bank_recon_account ON bank_reconciliations(bank_account_id);
CREATE INDEX idx_bank_recon_date ON bank_reconciliations(statement_date);
CREATE INDEX idx_bank_recon_status ON bank_reconciliations(status);

-- ===========================================
-- Bank Transactions Table
-- ===========================================
CREATE TABLE bank_transactions (
    id BIGSERIAL PRIMARY KEY,
    bank_account_id BIGINT NOT NULL REFERENCES bank_accounts(id) ON DELETE CASCADE,
    transaction_number VARCHAR(50) NOT NULL UNIQUE,
    transaction_date DATE NOT NULL,
    value_date DATE,
    transaction_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description VARCHAR(500) NOT NULL,
    debit_amount DECIMAL(18,4) DEFAULT 0,
    credit_amount DECIMAL(18,4) DEFAULT 0,
    running_balance DECIMAL(18,4),
    currency VARCHAR(3) DEFAULT 'UZS',
    exchange_rate DECIMAL(18,8) DEFAULT 1,
    base_amount DECIMAL(18,4),
    reference_number VARCHAR(100),
    check_number VARCHAR(50),
    payee_payer VARCHAR(200),
    reference_type VARCHAR(50),
    reference_id BIGINT,
    counter_account_id BIGINT REFERENCES accounts(id),
    transfer_to_account_id BIGINT REFERENCES bank_accounts(id),
    gl_posted BOOLEAN NOT NULL DEFAULT FALSE,
    gl_journal_entry_id BIGINT,
    reconciliation_id BIGINT REFERENCES bank_reconciliations(id),
    reconciled_date DATE,
    bank_statement_line VARCHAR(200),
    notes VARCHAR(1000),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_bank_tx_tenant ON bank_transactions(tenant_id);
CREATE INDEX idx_bank_tx_account ON bank_transactions(bank_account_id);
CREATE INDEX idx_bank_tx_date ON bank_transactions(transaction_date);
CREATE INDEX idx_bank_tx_type ON bank_transactions(transaction_type);
CREATE INDEX idx_bank_tx_status ON bank_transactions(status);
CREATE INDEX idx_bank_tx_reference ON bank_transactions(reference_number);
CREATE INDEX idx_bank_tx_reconciliation ON bank_transactions(reconciliation_id);

-- ===========================================
-- Tax Codes Table
-- ===========================================
CREATE TABLE tax_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    tax_type VARCHAR(20) NOT NULL,
    is_inclusive BOOLEAN NOT NULL DEFAULT FALSE,
    is_compound BOOLEAN NOT NULL DEFAULT FALSE,
    applies_to_sales BOOLEAN NOT NULL DEFAULT TRUE,
    applies_to_purchases BOOLEAN NOT NULL DEFAULT TRUE,
    sales_account_id BIGINT REFERENCES accounts(id),
    purchase_account_id BIGINT REFERENCES accounts(id),
    tax_authority VARCHAR(200),
    tax_registration_number VARCHAR(100),
    report_code VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system_code BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER DEFAULT 0,
    notes VARCHAR(1000),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_tax_code_tenant ON tax_codes(tenant_id);
CREATE INDEX idx_tax_code_code ON tax_codes(code);
CREATE INDEX idx_tax_code_type ON tax_codes(tax_type);
CREATE INDEX idx_tax_code_active ON tax_codes(active);
CREATE UNIQUE INDEX idx_tax_code_code_tenant ON tax_codes(code, tenant_id) WHERE tenant_id IS NOT NULL;

-- ===========================================
-- Tax Rates Table
-- ===========================================
CREATE TABLE tax_rates (
    id BIGSERIAL PRIMARY KEY,
    tax_code_id BIGINT NOT NULL REFERENCES tax_codes(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    rate DECIMAL(10,4) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    min_amount DECIMAL(18,4),
    max_amount DECIMAL(18,4),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_tax_rate_tenant ON tax_rates(tenant_id);
CREATE INDEX idx_tax_rate_code ON tax_rates(tax_code_id);
CREATE INDEX idx_tax_rate_effective ON tax_rates(effective_from);
CREATE INDEX idx_tax_rate_active ON tax_rates(active);

-- ===========================================
-- Add Banking & Tax Permissions
-- ===========================================
INSERT INTO permissions (name, code, description, module, action) VALUES
-- Bank Account permissions
('View Bank Accounts', 'FINANCE_BANK_VIEW', 'View bank accounts and transactions', 'FINANCE', 'VIEW'),
('Manage Bank Accounts', 'FINANCE_BANK_MANAGE', 'Create, update, delete bank accounts', 'FINANCE', 'MANAGE'),
('Record Bank Transactions', 'FINANCE_BANK_TRANSACT', 'Record bank transactions', 'FINANCE', 'CREATE'),
('Reconcile Bank Accounts', 'FINANCE_BANK_RECONCILE', 'Perform bank reconciliation', 'FINANCE', 'UPDATE'),
('Transfer Between Accounts', 'FINANCE_BANK_TRANSFER', 'Transfer funds between bank accounts', 'FINANCE', 'CREATE'),

-- Tax permissions
('View Tax Codes', 'FINANCE_TAX_VIEW', 'View tax codes and rates', 'FINANCE', 'VIEW'),
('Manage Tax Codes', 'FINANCE_TAX_MANAGE', 'Create, update, delete tax codes and rates', 'FINANCE', 'MANAGE'),
('View Tax Reports', 'FINANCE_TAX_REPORTS', 'View tax reports and summaries', 'FINANCE', 'VIEW'),
('Export Tax Reports', 'FINANCE_TAX_EXPORT', 'Export tax reports', 'FINANCE', 'EXPORT')
ON CONFLICT (code) DO NOTHING;

-- Assign Banking & Tax permissions to FINANCE_MANAGER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'FINANCE_MANAGER'
AND p.code IN (
    'FINANCE_BANK_VIEW', 'FINANCE_BANK_MANAGE', 'FINANCE_BANK_TRANSACT',
    'FINANCE_BANK_RECONCILE', 'FINANCE_BANK_TRANSFER',
    'FINANCE_TAX_VIEW', 'FINANCE_TAX_MANAGE', 'FINANCE_TAX_REPORTS', 'FINANCE_TAX_EXPORT'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- Assign limited Banking & Tax permissions to ACCOUNTANT role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ACCOUNTANT'
AND p.code IN (
    'FINANCE_BANK_VIEW', 'FINANCE_BANK_TRANSACT', 'FINANCE_BANK_RECONCILE',
    'FINANCE_TAX_VIEW', 'FINANCE_TAX_REPORTS'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- Assign view permissions to VIEWER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VIEWER'
AND p.code IN (
    'FINANCE_BANK_VIEW',
    'FINANCE_TAX_VIEW', 'FINANCE_TAX_REPORTS'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- ===========================================
-- Insert Default Tax Codes for Uzbekistan
-- ===========================================
INSERT INTO tax_codes (code, name, description, tax_type, is_inclusive, applies_to_sales, applies_to_purchases, is_system_code, sort_order) VALUES
('VAT12', 'VAT 12%', 'Standard VAT rate for Uzbekistan', 'VAT', FALSE, TRUE, TRUE, TRUE, 1),
('VAT0', 'VAT 0%', 'Zero-rated VAT', 'VAT', FALSE, TRUE, TRUE, TRUE, 2),
('NO_TAX', 'No Tax', 'Tax exempt transactions', 'OTHER', FALSE, TRUE, TRUE, TRUE, 3)
ON CONFLICT DO NOTHING;

-- Insert default tax rates
INSERT INTO tax_rates (tax_code_id, name, rate, effective_from, active)
SELECT tc.id, 'Standard Rate', 12.0000, '2020-01-01', TRUE
FROM tax_codes tc WHERE tc.code = 'VAT12' AND tc.tenant_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO tax_rates (tax_code_id, name, rate, effective_from, active)
SELECT tc.id, 'Zero Rate', 0.0000, '2020-01-01', TRUE
FROM tax_codes tc WHERE tc.code = 'VAT0' AND tc.tenant_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO tax_rates (tax_code_id, name, rate, effective_from, active)
SELECT tc.id, 'No Tax Rate', 0.0000, '2020-01-01', TRUE
FROM tax_codes tc WHERE tc.code = 'NO_TAX' AND tc.tenant_id IS NULL
ON CONFLICT DO NOTHING;
