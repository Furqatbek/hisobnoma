-- V78: Ensure the "2130 VAT Payable" liability account exists for every tenant.
--
-- GLIntegrationService now segregates output VAT/QQS on AR invoices into this account instead of
-- folding it into Sales Revenue (revenue was overstated and no VAT liability was booked). The
-- code-based default chart (AccountService) already creates 2130, but tenants provisioned only via
-- the seed migrations did not have it; without this account a taxed AR invoice would fail to post.
-- Idempotent: only inserts where the account is missing.

INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT
    t.id,
    '2130',
    'VAT Payable',
    'Output VAT/QQS collected on sales, owed to the tax authority',
    'LIABILITY',
    'CREDIT',
    1,
    '2130',
    TRUE,
    TRUE,
    TRUE,
    'UZS',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '2130'
);
