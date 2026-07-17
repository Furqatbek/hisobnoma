-- APNs (Apple Push) device tokens for the admin mobile app. Keyed by the opaque device token
-- (upsert target), routed to sandbox/production by the token's environment. Distinct from the
-- FCM device_tokens table used by the customer app.

CREATE TABLE device_push_tokens (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    user_id         BIGINT NOT NULL,
    token           VARCHAR(512) NOT NULL,
    platform        VARCHAR(20) NOT NULL DEFAULT 'ios',
    environment     VARCHAR(20) NOT NULL DEFAULT 'PRODUCTION',
    app_version     VARCHAR(40),
    last_seen_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    version         BIGINT DEFAULT 0,
    CONSTRAINT uk_device_push_tokens_token UNIQUE (tenant_id, token)
);

CREATE INDEX idx_device_push_tokens_tenant ON device_push_tokens(tenant_id);
CREATE INDEX idx_device_push_tokens_user ON device_push_tokens(user_id);

-- Permission to broadcast push notifications (staff send action).
INSERT INTO permissions (name, code, description, module, action) VALUES
('Send Push Notifications', 'MOBILE_PUSH_SEND', 'Broadcast push notifications to app users', 'MOBILE', 'MANAGE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN') AND p.code = 'MOBILE_PUSH_SEND'
ON CONFLICT DO NOTHING;
