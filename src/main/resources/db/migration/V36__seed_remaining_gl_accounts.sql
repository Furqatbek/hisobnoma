-- =====================================================
-- V36: Seed remaining GL integration accounts
-- Ensures all accounts used by GLIntegrationService exist for all tenants
-- =====================================================

-- 1110 - Cash
INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT t.id, '1110', 'Cash', 'Cash and cash equivalents',
       'ASSET', 'DEBIT', 1, '1110', TRUE, TRUE, TRUE, 'UZS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '1110');

-- 1400 - Salary Advances
INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT t.id, '1400', 'Salary Advances', 'Employee salary advance payments',
       'ASSET', 'DEBIT', 1, '1400', TRUE, TRUE, TRUE, 'UZS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '1400');

-- 4100 - Sales Revenue
INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT t.id, '4100', 'Sales Revenue', 'Revenue from product and service sales',
       'REVENUE', 'CREDIT', 1, '4100', TRUE, TRUE, TRUE, 'UZS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '4100');

-- 4200 - Sales Discounts
INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT t.id, '4200', 'Sales Discounts', 'Contra-revenue account for discounts given to customers',
       'EXPENSE', 'DEBIT', 1, '4200', TRUE, TRUE, TRUE, 'UZS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '4200');

-- 5100 - Cost of Goods Sold
INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT t.id, '5100', 'Cost of Goods Sold', 'Direct costs of products sold',
       'EXPENSE', 'DEBIT', 1, '5100', TRUE, TRUE, TRUE, 'UZS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '5100');

-- 5200 - Purchase Expenses
INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT t.id, '5200', 'Purchase Expenses', 'Operating and purchase expenses',
       'EXPENSE', 'DEBIT', 1, '5200', TRUE, TRUE, TRUE, 'UZS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '5200');

-- 6100 - Salary Expense
INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT t.id, '6100', 'Salary Expense', 'Employee salary and wages expense',
       'EXPENSE', 'DEBIT', 1, '6100', TRUE, TRUE, TRUE, 'UZS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '6100');
