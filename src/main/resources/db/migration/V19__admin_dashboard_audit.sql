-- =====================================================
-- V19: Admin Dashboard and Audit Trail Tables
-- =====================================================

-- System Settings table (global settings across all tenants)
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    default_value TEXT,
    description VARCHAR(500),
    category VARCHAR(50),
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    is_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    is_readonly BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    validation_regex VARCHAR(500),
    min_value BIGINT,
    max_value BIGINT,
    allowed_values TEXT,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

-- Indexes for system_settings
CREATE INDEX IF NOT EXISTS idx_system_settings_key ON system_settings(setting_key);
CREATE INDEX IF NOT EXISTS idx_system_settings_category ON system_settings(category);

-- Tenant Settings table (tenant-specific settings that override system defaults)
CREATE TABLE IF NOT EXISTS tenant_settings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    description VARCHAR(500),
    category VARCHAR(50),
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    is_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_setting_id BIGINT REFERENCES system_settings(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_tenant_settings_tenant_key UNIQUE (tenant_id, setting_key)
);

-- Indexes for tenant_settings
CREATE INDEX IF NOT EXISTS idx_tenant_settings_tenant ON tenant_settings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_settings_key ON tenant_settings(setting_key);
CREATE INDEX IF NOT EXISTS idx_tenant_settings_category ON tenant_settings(category);

-- Audit Logs table (comprehensive audit trail)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    user_id BIGINT,
    username VARCHAR(100),
    user_full_name VARCHAR(200),
    action VARCHAR(20) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    entity_name VARCHAR(200),
    module VARCHAR(50),
    description TEXT,
    old_values TEXT,
    new_values TEXT,
    changed_fields TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    request_url VARCHAR(500),
    request_method VARCHAR(10),
    action_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time_ms BIGINT,
    is_success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    correlation_id VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Indexes for audit_logs (optimized for common queries)
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant ON audit_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(action_timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_module ON audit_logs(module);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_timestamp ON audit_logs(tenant_id, action_timestamp DESC);

-- Insert default system settings
INSERT INTO system_settings (setting_key, default_value, description, category, value_type, is_readonly) VALUES
    ('app.name', 'Hisobnoma', 'Application name', 'GENERAL', 'STRING', true),
    ('app.version', '1.0.0', 'Application version', 'GENERAL', 'STRING', true),
    ('session.timeout.minutes', '30', 'Session timeout in minutes', 'SECURITY', 'INTEGER', false),
    ('password.min.length', '8', 'Minimum password length', 'SECURITY', 'INTEGER', false),
    ('password.require.special', 'true', 'Require special characters in password', 'SECURITY', 'BOOLEAN', false),
    ('login.max.attempts', '5', 'Maximum login attempts before lockout', 'SECURITY', 'INTEGER', false),
    ('login.lockout.minutes', '15', 'Account lockout duration in minutes', 'SECURITY', 'INTEGER', false),
    ('inventory.low.stock.threshold', '10', 'Default low stock threshold', 'INVENTORY', 'INTEGER', false),
    ('inventory.negative.stock.allowed', 'false', 'Allow negative stock quantities', 'INVENTORY', 'BOOLEAN', false),
    ('pos.receipt.footer', 'Thank you for shopping with us!', 'Default POS receipt footer text', 'POS', 'STRING', false),
    ('pos.allow.returns', 'true', 'Allow POS returns', 'POS', 'BOOLEAN', false),
    ('finance.fiscal.year.start.month', '1', 'Fiscal year start month (1-12)', 'FINANCE', 'INTEGER', false),
    ('finance.default.currency', 'UZS', 'Default currency code', 'FINANCE', 'STRING', false),
    ('audit.retention.days', '365', 'Audit log retention period in days', 'AUDIT', 'INTEGER', false)
ON CONFLICT (setting_key) DO NOTHING;

-- Insert admin permissions
INSERT INTO permissions (name, code, description, module, action) VALUES
    ('View Admin Dashboard', 'ADMIN_DASHBOARD_VIEW', 'Can view admin dashboard and statistics', 'ADMIN', 'VIEW'),
    ('View System Settings', 'ADMIN_SETTINGS_VIEW', 'Can view system settings', 'ADMIN', 'VIEW'),
    ('Manage System Settings', 'ADMIN_SETTINGS_MANAGE', 'Can create/edit/delete system settings', 'ADMIN', 'MANAGE'),
    ('View Tenant Settings', 'TENANT_SETTINGS_VIEW', 'Can view tenant settings', 'ADMIN', 'VIEW'),
    ('Manage Tenant Settings', 'TENANT_SETTINGS_MANAGE', 'Can create/edit/delete tenant settings', 'ADMIN', 'MANAGE'),
    ('View Audit Logs', 'ADMIN_AUDIT_VIEW', 'Can view audit logs', 'ADMIN', 'VIEW'),
    ('Export Audit Logs', 'ADMIN_AUDIT_EXPORT', 'Can export audit logs', 'ADMIN', 'EXPORT'),
    ('View System Health', 'ADMIN_HEALTH_VIEW', 'Can view system health and monitoring', 'ADMIN', 'VIEW')
ON CONFLICT (code) DO NOTHING;

-- Grant admin permissions to SUPER_ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN'
AND p.code IN (
    'ADMIN_DASHBOARD_VIEW',
    'ADMIN_SETTINGS_VIEW',
    'ADMIN_SETTINGS_MANAGE',
    'TENANT_SETTINGS_VIEW',
    'TENANT_SETTINGS_MANAGE',
    'ADMIN_AUDIT_VIEW',
    'ADMIN_AUDIT_EXPORT',
    'ADMIN_HEALTH_VIEW'
)
ON CONFLICT DO NOTHING;

-- Grant limited admin permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN'
AND p.code IN (
    'ADMIN_DASHBOARD_VIEW',
    'TENANT_SETTINGS_VIEW',
    'TENANT_SETTINGS_MANAGE',
    'ADMIN_AUDIT_VIEW'
)
ON CONFLICT DO NOTHING;
