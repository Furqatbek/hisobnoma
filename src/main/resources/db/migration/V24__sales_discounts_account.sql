-- =====================================================
-- V24: Create Sales Discounts GL account (4200)
-- Contra-revenue account to track discounts given to customers
-- =====================================================

INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT
    t.id,
    '4200',
    'Sales Discounts',
    'Contra-revenue account for discounts given to customers (POS and AR)',
    'EXPENSE',
    'DEBIT',
    1,
    '4200',
    TRUE,
    TRUE,
    TRUE,
    'UZS',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '4200'
);
