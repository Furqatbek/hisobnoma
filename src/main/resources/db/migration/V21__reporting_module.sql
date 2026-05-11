-- V21: Reporting Module
-- Creates tables for report definitions, schedules, and execution history

-- Report Definitions Table
CREATE TABLE report_definitions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(30) NOT NULL,
    query_class VARCHAR(255),
    template_path VARCHAR(255),
    parameter_schema TEXT,
    default_parameters TEXT,
    is_system_report BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_report_def_category CHECK (category IN ('INVENTORY', 'FINANCIAL', 'SALES', 'PURCHASING', 'CUSTOM'))
);

CREATE INDEX idx_report_def_tenant ON report_definitions(tenant_id);
CREATE INDEX idx_report_def_code ON report_definitions(code);
CREATE INDEX idx_report_def_category ON report_definitions(category);
CREATE UNIQUE INDEX idx_report_def_code_tenant ON report_definitions(code, tenant_id) WHERE tenant_id IS NOT NULL;

-- Report Export Formats Table (ElementCollection)
CREATE TABLE report_export_formats (
    report_definition_id BIGINT NOT NULL REFERENCES report_definitions(id) ON DELETE CASCADE,
    format VARCHAR(10) NOT NULL,
    CONSTRAINT chk_export_format CHECK (format IN ('PDF', 'EXCEL', 'CSV', 'JSON'))
);

CREATE INDEX idx_report_export_formats_def ON report_export_formats(report_definition_id);

-- Report Schedules Table
CREATE TABLE report_schedules (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    report_definition_id BIGINT NOT NULL REFERENCES report_definitions(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    cron_expression VARCHAR(50),
    run_time TIME,
    day_of_week INTEGER,
    day_of_month INTEGER,
    parameters TEXT,
    export_format VARCHAR(10) NOT NULL,
    delivery_method VARCHAR(20) NOT NULL DEFAULT 'STORE_ONLY',
    email_recipients VARCHAR(500),
    webhook_url VARCHAR(500),
    last_run_at TIMESTAMP WITH TIME ZONE,
    next_run_at TIMESTAMP WITH TIME ZONE,
    last_run_status VARCHAR(20),
    active BOOLEAN DEFAULT TRUE,
    created_by_user_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_schedule_frequency CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY', 'CUSTOM_CRON')),
    CONSTRAINT chk_schedule_export_format CHECK (export_format IN ('PDF', 'EXCEL', 'CSV', 'JSON')),
    CONSTRAINT chk_schedule_delivery_method CHECK (delivery_method IN ('EMAIL', 'STORE_ONLY', 'WEBHOOK'))
);

CREATE INDEX idx_report_schedule_tenant ON report_schedules(tenant_id);
CREATE INDEX idx_report_schedule_active ON report_schedules(active);
CREATE INDEX idx_report_schedule_next_run ON report_schedules(next_run_at);

-- Report Executions Table
CREATE TABLE report_executions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    report_definition_id BIGINT NOT NULL REFERENCES report_definitions(id) ON DELETE CASCADE,
    report_schedule_id BIGINT REFERENCES report_schedules(id) ON DELETE SET NULL,
    executed_by_user_id BIGINT,
    source VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    parameters TEXT,
    export_format VARCHAR(10),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    row_count INTEGER,
    file_path VARCHAR(500),
    file_size BIGINT,
    error_message TEXT,
    error_stack_trace TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_exec_source CHECK (source IN ('MANUAL', 'SCHEDULED', 'API')),
    CONSTRAINT chk_exec_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_exec_export_format CHECK (export_format IN ('PDF', 'EXCEL', 'CSV', 'JSON'))
);

CREATE INDEX idx_report_exec_tenant ON report_executions(tenant_id);
CREATE INDEX idx_report_exec_report ON report_executions(report_definition_id);
CREATE INDEX idx_report_exec_user ON report_executions(executed_by_user_id);
CREATE INDEX idx_report_exec_status ON report_executions(status);
CREATE INDEX idx_report_exec_started ON report_executions(started_at);

-- Insert default system reports
INSERT INTO report_definitions (tenant_id, code, name, description, category, is_system_report, active, sort_order)
VALUES
    (1, 'STOCK_ON_HAND', 'Stock on Hand Report', 'Shows current stock levels for all products by location', 'INVENTORY', TRUE, TRUE, 1),
    (1, 'INVENTORY_VALUATION', 'Inventory Valuation Report', 'Shows inventory value by product with cost and retail pricing', 'INVENTORY', TRUE, TRUE, 2),
    (1, 'SALES_SUMMARY', 'Sales Summary Report', 'Summary of sales by date range with daily breakdown', 'SALES', TRUE, TRUE, 1),
    (1, 'TRIAL_BALANCE', 'Trial Balance', 'Trial balance showing all account balances', 'FINANCIAL', TRUE, TRUE, 1),
    (1, 'AR_AGING', 'AR Aging Report', 'Accounts receivable aging by customer', 'FINANCIAL', TRUE, TRUE, 2),
    (1, 'AP_AGING', 'AP Aging Report', 'Accounts payable aging by vendor', 'FINANCIAL', TRUE, TRUE, 3);

-- Insert default export formats for system reports
INSERT INTO report_export_formats (report_definition_id, format)
SELECT id, 'EXCEL' FROM report_definitions WHERE code = 'STOCK_ON_HAND';
INSERT INTO report_export_formats (report_definition_id, format)
SELECT id, 'CSV' FROM report_definitions WHERE code = 'STOCK_ON_HAND';
INSERT INTO report_export_formats (report_definition_id, format)
SELECT id, 'JSON' FROM report_definitions WHERE code = 'STOCK_ON_HAND';

INSERT INTO report_export_formats (report_definition_id, format)
SELECT id, 'EXCEL' FROM report_definitions WHERE code = 'INVENTORY_VALUATION';
INSERT INTO report_export_formats (report_definition_id, format)
SELECT id, 'CSV' FROM report_definitions WHERE code = 'INVENTORY_VALUATION';

INSERT INTO report_export_formats (report_definition_id, format)
SELECT id, 'EXCEL' FROM report_definitions WHERE code = 'SALES_SUMMARY';
INSERT INTO report_export_formats (report_definition_id, format)
SELECT id, 'CSV' FROM report_definitions WHERE code = 'SALES_SUMMARY';

INSERT INTO report_export_formats (report_definition_id, format)
SELECT id, 'EXCEL' FROM report_definitions WHERE code = 'TRIAL_BALANCE';
INSERT INTO report_export_formats (report_definition_id, format)
SELECT id, 'CSV' FROM report_definitions WHERE code = 'TRIAL_BALANCE';

INSERT INTO report_export_formats (report_definition_id, format)
SELECT id, 'EXCEL' FROM report_definitions WHERE code = 'AR_AGING';
INSERT INTO report_export_formats (report_definition_id, format)
SELECT id, 'CSV' FROM report_definitions WHERE code = 'AR_AGING';

INSERT INTO report_export_formats (report_definition_id, format)
SELECT id, 'EXCEL' FROM report_definitions WHERE code = 'AP_AGING';
INSERT INTO report_export_formats (report_definition_id, format)
SELECT id, 'CSV' FROM report_definitions WHERE code = 'AP_AGING';

-- Insert reporting permissions
INSERT INTO permissions (name, code, description, module, action) VALUES
    ('REPORT_VIEW', 'REPORT_VIEW', 'View available reports', 'REPORTS', 'VIEW'),
    ('REPORT_GENERATE', 'REPORT_GENERATE', 'Generate reports', 'REPORTS', 'GENERATE'),
    ('REPORT_EXPORT', 'REPORT_EXPORT', 'Export reports', 'REPORTS', 'EXPORT'),
    ('REPORT_SCHEDULE_VIEW', 'REPORT_SCHEDULE_VIEW', 'View report schedules', 'REPORTS', 'VIEW'),
    ('REPORT_SCHEDULE_MANAGE', 'REPORT_SCHEDULE_MANAGE', 'Manage report schedules', 'REPORTS', 'MANAGE'),
    ('REPORT_INVENTORY_VIEW', 'REPORT_INVENTORY_VIEW', 'View inventory reports', 'REPORTS', 'VIEW'),
    ('REPORT_SALES_VIEW', 'REPORT_SALES_VIEW', 'View sales reports', 'REPORTS', 'VIEW'),
    ('REPORT_FINANCIAL_VIEW', 'REPORT_FINANCIAL_VIEW', 'View financial reports', 'REPORTS', 'VIEW')
ON CONFLICT (code) DO NOTHING;

-- Grant report permissions to admin role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name IN ('REPORT_VIEW', 'REPORT_GENERATE', 'REPORT_EXPORT', 'REPORT_SCHEDULE_VIEW',
                 'REPORT_SCHEDULE_MANAGE', 'REPORT_INVENTORY_VIEW', 'REPORT_SALES_VIEW', 'REPORT_FINANCIAL_VIEW')
ON CONFLICT DO NOTHING;

-- Grant view permissions to manager role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.name IN ('REPORT_VIEW', 'REPORT_GENERATE', 'REPORT_EXPORT', 'REPORT_SCHEDULE_VIEW',
                 'REPORT_INVENTORY_VIEW', 'REPORT_SALES_VIEW')
ON CONFLICT DO NOTHING;
