-- V20: Mobile Module Tables
-- Device tokens for push notifications, alerts, and preferences

-- =====================================================
-- Device Tokens Table (Push Notifications)
-- =====================================================
CREATE TABLE device_tokens (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    fcm_token VARCHAR(500) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    device_name VARCHAR(100),
    device_model VARCHAR(100),
    os_version VARCHAR(50),
    app_version VARCHAR(20),
    last_active_at TIMESTAMP WITH TIME ZONE,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_device_token_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_device_token_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_device_token_user_device UNIQUE (user_id, device_id, tenant_id)
);

CREATE INDEX idx_device_token_user ON device_tokens(user_id);
CREATE INDEX idx_device_token_tenant ON device_tokens(tenant_id);
CREATE INDEX idx_device_token_platform ON device_tokens(platform);
CREATE INDEX idx_device_token_active ON device_tokens(active);

-- =====================================================
-- Mobile Alerts Table
-- =====================================================
CREATE TABLE mobile_alerts (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT,
    alert_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    entity_type VARCHAR(50),
    entity_id BIGINT,
    action_url VARCHAR(500),
    data JSONB,
    is_read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    sent_via_push BOOLEAN NOT NULL DEFAULT false,
    push_sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_mobile_alert_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_mobile_alert_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_mobile_alert_user ON mobile_alerts(user_id);
CREATE INDEX idx_mobile_alert_tenant ON mobile_alerts(tenant_id);
CREATE INDEX idx_mobile_alert_type ON mobile_alerts(alert_type);
CREATE INDEX idx_mobile_alert_read ON mobile_alerts(is_read);
CREATE INDEX idx_mobile_alert_created ON mobile_alerts(created_at DESC);
CREATE INDEX idx_mobile_alert_expires ON mobile_alerts(expires_at) WHERE expires_at IS NOT NULL;

-- =====================================================
-- Alert Preferences Table
-- =====================================================
CREATE TABLE alert_preferences (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    push_enabled BOOLEAN NOT NULL DEFAULT true,
    in_app_enabled BOOLEAN NOT NULL DEFAULT true,
    email_enabled BOOLEAN NOT NULL DEFAULT false,
    sms_enabled BOOLEAN NOT NULL DEFAULT false,
    threshold_value INTEGER,
    quiet_hours_start INTEGER,
    quiet_hours_end INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_alert_pref_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_alert_pref_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_alert_pref_user_type UNIQUE (user_id, alert_type, tenant_id)
);

CREATE INDEX idx_alert_pref_user ON alert_preferences(user_id);
CREATE INDEX idx_alert_pref_tenant ON alert_preferences(tenant_id);

-- =====================================================
-- Mobile Permissions
-- =====================================================
INSERT INTO permissions (code, name, description, module, action, created_at, updated_at) VALUES
    ('MOBILE_DASHBOARD_VIEW', 'View Mobile Dashboard', 'View mobile dashboard and summaries', 'MOBILE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MOBILE_INVENTORY_VIEW', 'View Mobile Inventory', 'View inventory on mobile', 'MOBILE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MOBILE_ALERTS_VIEW', 'View Mobile Alerts', 'View and manage alerts on mobile', 'MOBILE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MOBILE_ALERTS_MANAGE', 'Manage Mobile Alerts', 'Configure alert preferences', 'MOBILE', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MOBILE_SYNC_ACCESS', 'Mobile Sync Access', 'Access offline sync data', 'MOBILE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MOBILE_QUICK_SALE', 'Mobile Quick Sale', 'Create quick sales from mobile', 'MOBILE', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MOBILE_QUICK_COUNT', 'Mobile Quick Count', 'Perform quick stock counts from mobile', 'MOBILE', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Assign mobile permissions to relevant roles
-- Admin role gets all mobile permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
AND p.code IN ('MOBILE_DASHBOARD_VIEW', 'MOBILE_INVENTORY_VIEW', 'MOBILE_ALERTS_VIEW',
               'MOBILE_ALERTS_MANAGE', 'MOBILE_SYNC_ACCESS', 'MOBILE_QUICK_SALE', 'MOBILE_QUICK_COUNT')
ON CONFLICT DO NOTHING;

-- Store Manager gets mobile dashboard and alerts
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'STORE_MANAGER'
AND p.code IN ('MOBILE_DASHBOARD_VIEW', 'MOBILE_INVENTORY_VIEW', 'MOBILE_ALERTS_VIEW',
               'MOBILE_ALERTS_MANAGE', 'MOBILE_SYNC_ACCESS', 'MOBILE_QUICK_SALE')
ON CONFLICT DO NOTHING;

-- Inventory Manager gets inventory view and quick count
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'INVENTORY_MANAGER'
AND p.code IN ('MOBILE_INVENTORY_VIEW', 'MOBILE_ALERTS_VIEW', 'MOBILE_SYNC_ACCESS', 'MOBILE_QUICK_COUNT')
ON CONFLICT DO NOTHING;

-- Cashier gets sync and quick sale
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'CASHIER'
AND p.code IN ('MOBILE_SYNC_ACCESS', 'MOBILE_QUICK_SALE')
ON CONFLICT DO NOTHING;

-- Warehouse Staff gets inventory view and quick count
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'WAREHOUSE_STAFF'
AND p.code IN ('MOBILE_INVENTORY_VIEW', 'MOBILE_SYNC_ACCESS', 'MOBILE_QUICK_COUNT')
ON CONFLICT DO NOTHING;

COMMENT ON TABLE device_tokens IS 'Mobile device registration for push notifications (FCM)';
COMMENT ON TABLE mobile_alerts IS 'In-app alerts and notifications for mobile users';
COMMENT ON TABLE alert_preferences IS 'User preferences for alert notification channels';
