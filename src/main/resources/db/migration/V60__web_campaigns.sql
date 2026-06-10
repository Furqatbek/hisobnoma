-- SMS opt-out for online customers (consent withdrawal — required by advertising law)
ALTER TABLE web_customers ADD COLUMN IF NOT EXISTS sms_opt_out BOOLEAN NOT NULL DEFAULT FALSE;

-- Marketing campaigns: a segment + SMS template (+ optional personal coupons)
CREATE TABLE web_campaigns (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    name            VARCHAR(200) NOT NULL,
    segment_type    VARCHAR(40) NOT NULL,
    segment_param   NUMERIC(18,4),
    sms_template_id BIGINT NOT NULL,
    promotion_id    BIGINT,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    recipient_count INT NOT NULL DEFAULT 0,
    sent_count      INT NOT NULL DEFAULT 0,
    failed_count    INT NOT NULL DEFAULT 0,
    cost_estimate   NUMERIC(18,4) NOT NULL DEFAULT 0,
    failure_reason  VARCHAR(500),
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    version         BIGINT DEFAULT 0
);

CREATE INDEX idx_web_campaigns_tenant ON web_campaigns(tenant_id, created_at);

-- Campaign permissions
INSERT INTO permissions (name, code, description, module, action)
VALUES
    ('SMS kampaniyalarni ko''rish', 'WEB_CAMPAIGN_VIEW', 'Onlayn do''kon SMS kampaniyalarini ko''rish', 'WEB', 'READ'),
    ('SMS kampaniyalarni boshqarish', 'WEB_CAMPAIGN_MANAGE', 'SMS kampaniyalar yaratish va yuborish', 'WEB', 'UPDATE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code IN ('WEB_CAMPAIGN_VIEW', 'WEB_CAMPAIGN_MANAGE')
ON CONFLICT DO NOTHING;
