-- Online shop customer accounts (phone + SMS OTP login)
CREATE TABLE web_customers (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT,
    phone         VARCHAR(20) NOT NULL,
    name          VARCHAR(200),
    verified_at   TIMESTAMP,
    last_login_at TIMESTAMP,
    customer_id   BIGINT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    created_by    BIGINT,
    updated_by    BIGINT,
    version       BIGINT DEFAULT 0
);

CREATE INDEX idx_web_customers_tenant ON web_customers(tenant_id);
CREATE UNIQUE INDEX uk_web_customers_phone ON web_customers(tenant_id, phone);

-- One-time codes (hashed, short-lived)
CREATE TABLE web_otp_codes (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT,
    phone       VARCHAR(20) NOT NULL,
    code_hash   VARCHAR(64) NOT NULL,
    salt        VARCHAR(32) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    attempts    INT NOT NULL DEFAULT 0,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT,
    version     BIGINT DEFAULT 0
);

CREATE INDEX idx_web_otp_phone ON web_otp_codes(tenant_id, phone, created_at);

-- Normalized phone on orders so verified customers can list their own orders
ALTER TABLE web_orders ADD COLUMN IF NOT EXISTS phone_normalized VARCHAR(20);
UPDATE web_orders SET phone_normalized = regexp_replace(phone, '[^0-9]', '', 'g')
WHERE phone_normalized IS NULL;
CREATE INDEX idx_web_orders_phone_norm ON web_orders(tenant_id, phone_normalized);

-- Online customer permissions
INSERT INTO permissions (name, code, description, module, action)
VALUES
    ('Onlayn mijozlarni ko''rish', 'WEB_CUSTOMER_VIEW', 'Onlayn mijozlar ro''yxatini ko''rish', 'WEB', 'READ'),
    ('Onlayn mijozlarni boshqarish', 'WEB_CUSTOMER_MANAGE', 'Onlayn mijozlarni AR mijozga bog''lash', 'WEB', 'UPDATE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code IN ('WEB_CUSTOMER_VIEW', 'WEB_CUSTOMER_MANAGE')
ON CONFLICT DO NOTHING;
