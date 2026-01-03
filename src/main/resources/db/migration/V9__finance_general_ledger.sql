-- V9__finance_general_ledger.sql
-- Finance Module - General Ledger (Block 6)

-- ===========================================
-- Currencies Table
-- ===========================================
CREATE TABLE currencies (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(3) NOT NULL,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10),
    decimal_places INTEGER NOT NULL DEFAULT 2,
    is_base_currency BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    format_pattern VARCHAR(50) DEFAULT '#,##0.00',
    symbol_position VARCHAR(10) DEFAULT 'BEFORE',
    thousands_separator VARCHAR(1) DEFAULT ',',
    decimal_separator VARCHAR(1) DEFAULT '.',
    sort_order INTEGER DEFAULT 0,
    notes VARCHAR(500),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_currency_tenant ON currencies(tenant_id);
CREATE INDEX idx_currency_code ON currencies(code);
CREATE INDEX idx_currency_active ON currencies(active);
CREATE UNIQUE INDEX idx_currency_code_tenant ON currencies(code, tenant_id) WHERE tenant_id IS NOT NULL;
CREATE UNIQUE INDEX idx_currency_code_system ON currencies(code) WHERE tenant_id IS NULL;

-- ===========================================
-- Exchange Rates Table
-- ===========================================
CREATE TABLE exchange_rates (
    id BIGSERIAL PRIMARY KEY,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(18,8) NOT NULL,
    inverse_rate DECIMAL(18,8),
    effective_date DATE NOT NULL,
    expiry_date DATE,
    source VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_exchange_rate_tenant ON exchange_rates(tenant_id);
CREATE INDEX idx_exchange_rate_from ON exchange_rates(from_currency);
CREATE INDEX idx_exchange_rate_to ON exchange_rates(to_currency);
CREATE INDEX idx_exchange_rate_date ON exchange_rates(effective_date);
CREATE INDEX idx_exchange_rate_lookup ON exchange_rates(from_currency, to_currency, effective_date);

-- ===========================================
-- Chart of Accounts Table
-- ===========================================
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    account_type VARCHAR(20) NOT NULL,
    normal_balance VARCHAR(10) NOT NULL,
    parent_account_id BIGINT REFERENCES accounts(id),
    account_level INTEGER NOT NULL DEFAULT 1,
    full_path VARCHAR(200),
    is_system_account BOOLEAN NOT NULL DEFAULT FALSE,
    is_control_account BOOLEAN NOT NULL DEFAULT FALSE,
    allows_direct_posting BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    current_balance DECIMAL(18,4) DEFAULT 0,
    ytd_debit DECIMAL(18,4) DEFAULT 0,
    ytd_credit DECIMAL(18,4) DEFAULT 0,
    opening_balance DECIMAL(18,4) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'UZS',
    tax_code VARCHAR(50),
    bank_account_number VARCHAR(50),
    bank_name VARCHAR(100),
    sort_order INTEGER DEFAULT 0,
    notes VARCHAR(1000),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_accounts_tenant ON accounts(tenant_id);
CREATE INDEX idx_accounts_code ON accounts(code);
CREATE INDEX idx_accounts_type ON accounts(account_type);
CREATE INDEX idx_accounts_parent ON accounts(parent_account_id);
CREATE INDEX idx_accounts_active ON accounts(active);
CREATE UNIQUE INDEX idx_accounts_code_tenant ON accounts(code, tenant_id) WHERE tenant_id IS NOT NULL;

-- ===========================================
-- Fiscal Years Table
-- ===========================================
CREATE TABLE fiscal_years (
    id BIGSERIAL PRIMARY KEY,
    year INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    closed_at TIMESTAMP,
    closed_by BIGINT,
    retained_earnings_account_id BIGINT REFERENCES accounts(id),
    year_end_closing_entry_id BIGINT,
    notes VARCHAR(500),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_fiscal_year_tenant ON fiscal_years(tenant_id);
CREATE INDEX idx_fiscal_year_year ON fiscal_years(year);
CREATE INDEX idx_fiscal_year_current ON fiscal_years(is_current);
CREATE UNIQUE INDEX idx_fiscal_year_tenant_year ON fiscal_years(year, tenant_id) WHERE tenant_id IS NOT NULL;

-- ===========================================
-- Fiscal Periods Table
-- ===========================================
CREATE TABLE fiscal_periods (
    id BIGSERIAL PRIMARY KEY,
    fiscal_year_id BIGINT NOT NULL REFERENCES fiscal_years(id) ON DELETE CASCADE,
    period_number INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    is_adjustment_period BOOLEAN NOT NULL DEFAULT FALSE,
    closed_at TIMESTAMP,
    closed_by BIGINT,
    reopened_at TIMESTAMP,
    reopened_by BIGINT,
    reopen_reason VARCHAR(500),
    notes VARCHAR(500),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_fiscal_period_tenant ON fiscal_periods(tenant_id);
CREATE INDEX idx_fiscal_period_year ON fiscal_periods(fiscal_year_id);
CREATE INDEX idx_fiscal_period_number ON fiscal_periods(period_number);
CREATE INDEX idx_fiscal_period_status ON fiscal_periods(status);
CREATE INDEX idx_fiscal_period_dates ON fiscal_periods(start_date, end_date);
CREATE UNIQUE INDEX idx_fiscal_period_year_number ON fiscal_periods(fiscal_year_id, period_number);

-- ===========================================
-- Journal Entries Table
-- ===========================================
CREATE TABLE journal_entries (
    id BIGSERIAL PRIMARY KEY,
    entry_number VARCHAR(50) NOT NULL UNIQUE,
    entry_date DATE NOT NULL,
    fiscal_period_id BIGINT NOT NULL REFERENCES fiscal_periods(id),
    description VARCHAR(500) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'DRAFT',
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    reference_type VARCHAR(50),
    reference_id BIGINT,
    reference_number VARCHAR(100),
    total_debit DECIMAL(18,4) DEFAULT 0,
    total_credit DECIMAL(18,4) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'UZS',
    exchange_rate DECIMAL(18,8) DEFAULT 1,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurring_template_id BIGINT,
    is_reversing BOOLEAN NOT NULL DEFAULT FALSE,
    reversal_date DATE,
    reversed_entry_id BIGINT REFERENCES journal_entries(id),
    reversing_entry_id BIGINT REFERENCES journal_entries(id),
    posted_at TIMESTAMP,
    posted_by BIGINT,
    reversed_at TIMESTAMP,
    reversed_by BIGINT,
    notes VARCHAR(1000),
    internal_notes VARCHAR(1000),
    attachments VARCHAR(500),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_journal_entry_tenant ON journal_entries(tenant_id);
CREATE INDEX idx_journal_entry_number ON journal_entries(entry_number);
CREATE INDEX idx_journal_entry_date ON journal_entries(entry_date);
CREATE INDEX idx_journal_entry_status ON journal_entries(status);
CREATE INDEX idx_journal_entry_source ON journal_entries(source);
CREATE INDEX idx_journal_entry_period ON journal_entries(fiscal_period_id);
CREATE INDEX idx_journal_entry_reference ON journal_entries(reference_type, reference_id);

-- ===========================================
-- Journal Lines Table
-- ===========================================
CREATE TABLE journal_lines (
    id BIGSERIAL PRIMARY KEY,
    journal_entry_id BIGINT NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    description VARCHAR(500),
    debit_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    credit_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    base_debit_amount DECIMAL(18,4) DEFAULT 0,
    base_credit_amount DECIMAL(18,4) DEFAULT 0,
    cost_center VARCHAR(50),
    project_code VARCHAR(50),
    department VARCHAR(50),
    reference_type VARCHAR(50),
    reference_id BIGINT,
    tax_code VARCHAR(50),
    tax_amount DECIMAL(18,4) DEFAULT 0,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_journal_line_tenant ON journal_lines(tenant_id);
CREATE INDEX idx_journal_line_entry ON journal_lines(journal_entry_id);
CREATE INDEX idx_journal_line_account ON journal_lines(account_id);

-- ===========================================
-- Add Finance Permissions
-- ===========================================
INSERT INTO permissions (name, code, description, module, action) VALUES
-- General Ledger permissions
('View Chart of Accounts', 'FINANCE_GL_VIEW', 'View chart of accounts and GL balances', 'FINANCE', 'VIEW'),
('Manage Chart of Accounts', 'FINANCE_GL_MANAGE', 'Create, update, delete accounts', 'FINANCE', 'MANAGE'),
('Post Journal Entries', 'FINANCE_GL_POST', 'Post journal entries to general ledger', 'FINANCE', 'CREATE'),
('Reverse Journal Entries', 'FINANCE_GL_REVERSE', 'Reverse posted journal entries', 'FINANCE', 'UPDATE'),
('View Journal Entries', 'FINANCE_JE_VIEW', 'View journal entries', 'FINANCE', 'VIEW'),
('Create Journal Entries', 'FINANCE_JE_CREATE', 'Create journal entries', 'FINANCE', 'CREATE'),

-- Fiscal Period permissions
('View Fiscal Periods', 'FINANCE_PERIOD_VIEW', 'View fiscal years and periods', 'FINANCE', 'VIEW'),
('Manage Fiscal Periods', 'FINANCE_PERIOD_MANAGE', 'Create and manage fiscal years/periods', 'FINANCE', 'MANAGE'),
('Close Fiscal Period', 'FINANCE_PERIOD_CLOSE', 'Close fiscal periods', 'FINANCE', 'UPDATE'),
('Reopen Fiscal Period', 'FINANCE_PERIOD_REOPEN', 'Reopen closed fiscal periods', 'FINANCE', 'UPDATE'),
('Close Fiscal Year', 'FINANCE_YEAR_CLOSE', 'Perform year-end closing', 'FINANCE', 'UPDATE'),

-- Currency permissions
('View Currencies', 'FINANCE_CURRENCY_VIEW', 'View currencies and exchange rates', 'FINANCE', 'VIEW'),
('Manage Currencies', 'FINANCE_CURRENCY_MANAGE', 'Manage currencies and exchange rates', 'FINANCE', 'MANAGE'),

-- Finance Reports permissions
('View Financial Reports', 'FINANCE_REPORTS_VIEW', 'View financial reports', 'FINANCE', 'VIEW'),
('Export Financial Reports', 'FINANCE_REPORTS_EXPORT', 'Export financial reports', 'FINANCE', 'EXPORT')
ON CONFLICT (code) DO NOTHING;

-- Assign GL permissions to FINANCE_MANAGER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'FINANCE_MANAGER'
AND p.code IN (
    'FINANCE_GL_VIEW', 'FINANCE_GL_MANAGE', 'FINANCE_GL_POST', 'FINANCE_GL_REVERSE',
    'FINANCE_JE_VIEW', 'FINANCE_JE_CREATE',
    'FINANCE_PERIOD_VIEW', 'FINANCE_PERIOD_MANAGE', 'FINANCE_PERIOD_CLOSE', 'FINANCE_PERIOD_REOPEN', 'FINANCE_YEAR_CLOSE',
    'FINANCE_CURRENCY_VIEW', 'FINANCE_CURRENCY_MANAGE',
    'FINANCE_REPORTS_VIEW', 'FINANCE_REPORTS_EXPORT'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- Assign limited GL permissions to ACCOUNTANT role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ACCOUNTANT'
AND p.code IN (
    'FINANCE_GL_VIEW', 'FINANCE_GL_POST',
    'FINANCE_JE_VIEW', 'FINANCE_JE_CREATE',
    'FINANCE_PERIOD_VIEW',
    'FINANCE_CURRENCY_VIEW',
    'FINANCE_REPORTS_VIEW'
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
    'FINANCE_GL_VIEW',
    'FINANCE_JE_VIEW',
    'FINANCE_PERIOD_VIEW',
    'FINANCE_CURRENCY_VIEW',
    'FINANCE_REPORTS_VIEW'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- ===========================================
-- Insert Default Currencies
-- ===========================================
INSERT INTO currencies (code, name, symbol, decimal_places, is_base_currency, sort_order) VALUES
('UZS', 'Uzbekistan Som', 'UZS', 2, TRUE, 1),
('USD', 'US Dollar', '$', 2, FALSE, 2),
('EUR', 'Euro', '€', 2, FALSE, 3),
('RUB', 'Russian Ruble', '₽', 2, FALSE, 4)
ON CONFLICT DO NOTHING;
