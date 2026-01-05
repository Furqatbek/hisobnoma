-- V18: Recurring Journal Templates and POS Returns Enhancement

-- Recurring Journal Templates
CREATE TABLE IF NOT EXISTS recurring_journal_templates (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES tenants(id),
    name VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    frequency VARCHAR(20) NOT NULL,
    frequency_day INTEGER,
    start_date DATE NOT NULL,
    end_date DATE,
    next_run_date DATE,
    last_run_date DATE,
    run_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    auto_post BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_recurring_templates_tenant ON recurring_journal_templates(tenant_id);
CREATE INDEX idx_recurring_templates_next_run ON recurring_journal_templates(next_run_date);
CREATE INDEX idx_recurring_templates_active ON recurring_journal_templates(is_active);

-- Recurring Journal Lines
CREATE TABLE IF NOT EXISTS recurring_journal_lines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES tenants(id),
    template_id BIGINT NOT NULL REFERENCES recurring_journal_templates(id) ON DELETE CASCADE,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    description VARCHAR(500),
    debit_amount DECIMAL(18, 4) DEFAULT 0,
    credit_amount DECIMAL(18, 4) DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_recurring_lines_template ON recurring_journal_lines(template_id);

-- Add return-related fields to POS transactions
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS original_transaction_id BIGINT REFERENCES pos_transactions(id);
ALTER TABLE pos_transactions ADD COLUMN IF NOT EXISTS return_reason VARCHAR(500);

-- Add index for finding returns by original transaction
CREATE INDEX IF NOT EXISTS idx_pos_transactions_original ON pos_transactions(original_transaction_id);

-- Insert new permissions for recurring journals and returns
INSERT INTO permissions (code, name, description) VALUES
    ('FINANCE_RECURRING_VIEW', 'View Recurring Journals', 'Can view recurring journal templates'),
    ('FINANCE_RECURRING_MANAGE', 'Manage Recurring Journals', 'Can create/edit/delete recurring journal templates'),
    ('POS_RETURN_CREATE', 'Create POS Returns', 'Can create return transactions'),
    ('POS_RETURN_APPROVE', 'Approve POS Returns', 'Can approve return transactions')
ON CONFLICT (code) DO NOTHING;

-- Add permissions to STORE_MANAGER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'STORE_MANAGER'
AND p.code IN ('FINANCE_RECURRING_VIEW', 'FINANCE_RECURRING_MANAGE', 'POS_RETURN_CREATE', 'POS_RETURN_APPROVE')
ON CONFLICT DO NOTHING;

-- Add return permission to CASHIER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'CASHIER'
AND p.code = 'POS_RETURN_CREATE'
ON CONFLICT DO NOTHING;
