-- Ensure Cash account (1110) exists for all tenants
-- This account is needed by GL integration for salary payments and advances
INSERT INTO accounts (code, name, description, account_type, normal_balance, account_level, full_path,
                     is_system_account, is_control_account, allows_direct_posting, active,
                     current_balance, opening_balance, ytd_debit, ytd_credit,
                     tenant_id, created_at, updated_at, version)
SELECT '1110', 'Naqd pul', 'Cash on hand', 'ASSET', 'DEBIT', 1, '1110',
       FALSE, FALSE, TRUE, TRUE, 0, 0, 0, 0,
       t.id, NOW(), NOW(), 0
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.code = '1110' AND a.tenant_id = t.id
);
