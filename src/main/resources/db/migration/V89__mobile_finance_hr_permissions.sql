-- Mobile permissions for the admin app's finance/HR write actions:
-- record an expense, accept a debtor (AR) payment, and pay a salary/advance.
-- Endpoints accept EITHER the mobile permission OR the underlying module permission
-- (FINANCE_AR_WRITE / HR_SALARY_WRITE), mirroring the other mobile endpoints.
INSERT INTO permissions (code, name, description, module, action, created_at, updated_at) VALUES
    ('MOBILE_EXPENSE_WRITE', 'Mobile Write Expense', 'Record an expense from the mobile app', 'MOBILE', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MOBILE_AR_COLLECT', 'Mobile Collect Debtor Payment', 'Accept and register a customer (AR) payment from the mobile app', 'MOBILE', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MOBILE_SALARY_PAY', 'Mobile Pay Salary/Advance', 'Record a paid salary or advance from the mobile app', 'MOBILE', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Grant to the finance/admin-capable roles (not cashier/warehouse — these post to the GL).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT')
AND p.code IN ('MOBILE_EXPENSE_WRITE', 'MOBILE_AR_COLLECT', 'MOBILE_SALARY_PAY')
ON CONFLICT DO NOTHING;
