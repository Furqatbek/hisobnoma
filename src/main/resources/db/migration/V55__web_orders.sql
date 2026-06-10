-- Online shop orders placed from the customer mobile app
CREATE TABLE web_orders (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             BIGINT,
    order_number          VARCHAR(50) NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'NEW',
    customer_name         VARCHAR(200) NOT NULL,
    phone                 VARCHAR(50) NOT NULL,
    delivery_region_id    BIGINT,
    delivery_region_name  VARCHAR(100),
    delivery_village_id   BIGINT,
    delivery_village_name VARCHAR(100),
    customer_note         VARCHAR(500),
    total_amount          NUMERIC(18,4) NOT NULL DEFAULT 0,
    currency              VARCHAR(3) NOT NULL DEFAULT 'UZS',
    source_ip             VARCHAR(45),
    user_agent            VARCHAR(500),
    customer_id           BIGINT,
    ar_invoice_id         BIGINT,
    ar_invoice_number     VARCHAR(50),
    cancellation_reason   VARCHAR(500),
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP,
    created_by            BIGINT,
    updated_by            BIGINT,
    version               BIGINT DEFAULT 0
);

CREATE INDEX idx_web_orders_tenant ON web_orders(tenant_id);
CREATE INDEX idx_web_orders_status ON web_orders(tenant_id, status);
CREATE INDEX idx_web_orders_created ON web_orders(tenant_id, created_at);
CREATE UNIQUE INDEX uk_web_orders_number ON web_orders(tenant_id, order_number);

CREATE TABLE web_order_lines (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT,
    web_order_id  BIGINT NOT NULL REFERENCES web_orders(id),
    product_id    BIGINT NOT NULL,
    catalog_item_id BIGINT,
    product_name  VARCHAR(200) NOT NULL,
    unit_name     VARCHAR(50),
    quantity      NUMERIC(18,4) NOT NULL,
    unit_price    NUMERIC(18,4) NOT NULL,
    line_total    NUMERIC(18,4) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    created_by    BIGINT,
    updated_by    BIGINT,
    version       BIGINT DEFAULT 0
);

CREATE INDEX idx_web_order_lines_order ON web_order_lines(web_order_id);
CREATE INDEX idx_web_order_lines_tenant ON web_order_lines(tenant_id);

-- Online order permissions
INSERT INTO permissions (name, code, description, module, action)
VALUES
    ('Onlayn buyurtmalarni ko''rish', 'WEB_ORDER_VIEW', 'Onlayn buyurtmalar ro''yxatini ko''rish', 'WEB', 'READ'),
    ('Onlayn buyurtmalarni boshqarish', 'WEB_ORDER_MANAGE', 'Onlayn buyurtmalarni tasdiqlash va boshqarish', 'WEB', 'UPDATE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code IN ('WEB_ORDER_VIEW', 'WEB_ORDER_MANAGE')
ON CONFLICT DO NOTHING;
