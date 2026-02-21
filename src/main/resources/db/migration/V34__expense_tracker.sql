-- V34__expense_tracker.sql
-- Expense tracker table for external integrations

CREATE TABLE expense_records (
    id BIGSERIAL PRIMARY KEY,
    create_date DATE NOT NULL DEFAULT CURRENT_DATE,
    category VARCHAR(100) NOT NULL,
    total_amount DECIMAL(18,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'UZS',
    generated_notes VARCHAR(1000),
    full_text TEXT,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_expense_record_tenant ON expense_records(tenant_id);
CREATE INDEX idx_expense_record_category ON expense_records(category);
CREATE INDEX idx_expense_record_date ON expense_records(create_date);
