-- =============================================
-- HR-Finance Integration: Salary payments tracked in GL
-- =============================================

-- Add GL journal entry reference to salary_records
ALTER TABLE salary_records ADD COLUMN gl_journal_entry_id BIGINT;

-- Create Salary Expense account (6100) for each tenant that has a chart of accounts
INSERT INTO accounts (code, name, description, account_type, normal_balance, account_level, full_path, is_system_account, is_control_account, allows_direct_posting, active, current_balance, opening_balance, ytd_debit, ytd_credit, tenant_id, created_at, updated_at, version)
SELECT '6100', 'Ish haqi xarajatlari', 'Salary and wages expense', 'EXPENSE', 'DEBIT', 1, '6100', FALSE, FALSE, TRUE, TRUE, 0, 0, 0, 0, t.id, NOW(), NOW(), 0
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.code = '6100' AND a.tenant_id = t.id
);
