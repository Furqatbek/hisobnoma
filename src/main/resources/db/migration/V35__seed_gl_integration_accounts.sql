-- =====================================================
-- V35: Seed essential GL integration accounts
-- Ensures accounts used by GLIntegrationService exist for all tenants
-- Fixes mismatch between GL constants and chart of accounts
-- =====================================================

-- 1130 - Accounts Receivable
INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT t.id, '1130', 'Accounts Receivable', 'Customer receivables from AR invoices',
       'ASSET', 'DEBIT', 1, '1130', TRUE, TRUE, TRUE, 'UZS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '1130');

-- 1140 - Inventory
INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT t.id, '1140', 'Inventory', 'Product inventory asset account',
       'ASSET', 'DEBIT', 1, '1140', TRUE, TRUE, TRUE, 'UZS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '1140');

-- 2110 - Accounts Payable
INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT t.id, '2110', 'Accounts Payable', 'Vendor payables',
       'LIABILITY', 'CREDIT', 1, '2110', TRUE, TRUE, TRUE, 'UZS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '2110');
