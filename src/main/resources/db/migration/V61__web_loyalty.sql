-- Phase 4: Loyalty cashback — append-only points ledger

CREATE TABLE web_loyalty_transactions (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    web_customer_id BIGINT       NOT NULL REFERENCES web_customers(id),
    web_order_id    BIGINT       NULL     REFERENCES web_orders(id),
    type            VARCHAR(10)  NOT NULL,  -- EARN, SPEND, EXPIRE, ADJUST
    amount          NUMERIC(18,4) NOT NULL, -- positive = credit, negative = debit
    expires_at      TIMESTAMP    NULL,
    note            VARCHAR(500) NULL,
    created_by      BIGINT       NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wlt_tenant_customer ON web_loyalty_transactions (tenant_id, web_customer_id);
CREATE INDEX idx_wlt_order           ON web_loyalty_transactions (web_order_id) WHERE web_order_id IS NOT NULL;

-- Idempotent earn: at most one EARN row per order
CREATE UNIQUE INDEX uk_wlt_earn_per_order
    ON web_loyalty_transactions (tenant_id, web_order_id, type)
    WHERE type = 'EARN' AND web_order_id IS NOT NULL;

-- Points spent on a web order (reduces the amount to pay)
ALTER TABLE web_orders ADD COLUMN points_spent NUMERIC(18,4) NOT NULL DEFAULT 0;

-- Permissions
INSERT INTO permissions (name, code, description, module, action, created_at, updated_at)
VALUES
    ('View Web Loyalty', 'WEB_LOYALTY_VIEW', 'View online customer loyalty balances and ledger', 'WEB', 'VIEW', NOW(), NOW()),
    ('Manage Web Loyalty', 'WEB_LOYALTY_MANAGE', 'Manually adjust loyalty points', 'WEB', 'MANAGE', NOW(), NOW());

-- Grant to ADMIN and SUPER_ADMIN roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code IN ('WEB_LOYALTY_VIEW', 'WEB_LOYALTY_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
