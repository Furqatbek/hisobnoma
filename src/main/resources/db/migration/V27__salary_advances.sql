-- =============================================
-- Salary Advances (Avans) - Track advance payments to employees
-- =============================================

CREATE TABLE salary_advances (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    amount DECIMAL(15,2) NOT NULL,
    advance_date DATE NOT NULL,
    period_year INT NOT NULL,
    period_month INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'GIVEN',  -- GIVEN, DEDUCTED, CANCELLED
    salary_record_id BIGINT REFERENCES salary_records(id),
    gl_journal_entry_id BIGINT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_salary_advances_tenant ON salary_advances(tenant_id);
CREATE INDEX idx_salary_advances_employee ON salary_advances(employee_id);
CREATE INDEX idx_salary_advances_period ON salary_advances(tenant_id, period_year, period_month);
CREATE INDEX idx_salary_advances_status ON salary_advances(tenant_id, status);

-- Add advance tracking columns to salary_records
ALTER TABLE salary_records ADD COLUMN advance_amount DECIMAL(15,2) DEFAULT 0;
ALTER TABLE salary_records ADD COLUMN pay_amount DECIMAL(15,2);

-- Create Salary Advance asset account (1400) for tenants that don't have it
INSERT INTO accounts (code, name, description, account_type, normal_balance, account_level, full_path, is_system_account, is_control_account, allows_direct_posting, active, current_balance, opening_balance, ytd_debit, ytd_credit, tenant_id, created_at, updated_at, version)
SELECT '1400', 'Xodimlarga avans', 'Employee salary advances', 'ASSET', 'DEBIT', 1, '1400', FALSE, FALSE, TRUE, TRUE, 0, 0, 0, 0, t.id, NOW(), NOW(), 0
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.code = '1400' AND a.tenant_id = t.id
);
