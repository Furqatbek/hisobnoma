-- Distribution module — Slice 2: B2B wholesale orders + lines.

CREATE TABLE distribution_orders (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT,
    order_number            VARCHAR(50) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    agent_id                BIGINT REFERENCES distribution_agents(id),
    customer_id             BIGINT NOT NULL REFERENCES customers(id),
    customer_name           VARCHAR(200),
    source_location_id      BIGINT REFERENCES locations(id),
    order_date              DATE NOT NULL,
    expected_delivery_date  DATE,
    delivered_at            TIMESTAMP,
    payment_method          VARCHAR(10) NOT NULL DEFAULT 'CREDIT',
    subtotal                NUMERIC(18,4) NOT NULL DEFAULT 0,
    discount_amount         NUMERIC(18,4) NOT NULL DEFAULT 0,
    tax_amount              NUMERIC(18,4) NOT NULL DEFAULT 0,
    delivery_fee            NUMERIC(18,4) NOT NULL DEFAULT 0,
    total_amount            NUMERIC(18,4) NOT NULL DEFAULT 0,
    cash_collected          NUMERIC(18,4) NOT NULL DEFAULT 0,
    credit_amount           NUMERIC(18,4) NOT NULL DEFAULT 0,
    payment_terms_days      INT,
    due_date                DATE,
    ar_invoice_id           BIGINT,
    ar_invoice_number       VARCHAR(50),
    delivery_address        VARCHAR(500),
    currency                VARCHAR(3) NOT NULL DEFAULT 'UZS',
    notes                   VARCHAR(1000),
    internal_notes          VARCHAR(1000),
    cancellation_reason     VARCHAR(500),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    created_by              BIGINT,
    updated_by              BIGINT,
    version                 BIGINT DEFAULT 0,
    CONSTRAINT uk_dist_orders_number UNIQUE (tenant_id, order_number)
);

CREATE INDEX idx_dist_orders_tenant ON distribution_orders(tenant_id);
CREATE INDEX idx_dist_orders_status ON distribution_orders(tenant_id, status);
CREATE INDEX idx_dist_orders_agent ON distribution_orders(agent_id);
CREATE INDEX idx_dist_orders_customer ON distribution_orders(customer_id);
CREATE INDEX idx_dist_orders_created ON distribution_orders(tenant_id, created_at);

CREATE TABLE distribution_order_lines (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT,
    order_id            BIGINT NOT NULL REFERENCES distribution_orders(id),
    product_id          BIGINT NOT NULL REFERENCES products(id),
    product_name        VARCHAR(200),
    product_sku         VARCHAR(50),
    unit_name           VARCHAR(50),
    quantity            NUMERIC(18,4) NOT NULL,
    unit_price          NUMERIC(18,4) NOT NULL,
    discount_percent    NUMERIC(5,2) DEFAULT 0,
    line_total          NUMERIC(18,4) NOT NULL DEFAULT 0,
    fulfilled_quantity  NUMERIC(18,4) DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    version             BIGINT DEFAULT 0
);

CREATE INDEX idx_dist_order_lines_tenant ON distribution_order_lines(tenant_id);
CREATE INDEX idx_dist_order_lines_order ON distribution_order_lines(order_id);
CREATE INDEX idx_dist_order_lines_product ON distribution_order_lines(product_id);

-- Distribution order permissions
INSERT INTO permissions (name, code, description, module, action) VALUES
('View Distribution Orders', 'DISTRIBUTION_ORDER_VIEW', 'View distribution orders', 'DISTRIBUTION', 'VIEW'),
('Create Distribution Orders', 'DISTRIBUTION_ORDER_CREATE', 'Create distribution orders', 'DISTRIBUTION', 'CREATE'),
('Manage Distribution Orders', 'DISTRIBUTION_ORDER_MANAGE', 'Edit, progress, invoice and cancel distribution orders', 'DISTRIBUTION', 'MANAGE')
ON CONFLICT (code) DO NOTHING;

-- SUPER_ADMIN and ADMIN get all distribution permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND p.code LIKE 'DISTRIBUTION_%'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code LIKE 'DISTRIBUTION_%'
ON CONFLICT DO NOTHING;

-- STORE_MANAGER runs the full order lifecycle
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'STORE_MANAGER' AND p.code IN (
    'DISTRIBUTION_ORDER_VIEW', 'DISTRIBUTION_ORDER_CREATE', 'DISTRIBUTION_ORDER_MANAGE'
)
ON CONFLICT DO NOTHING;

-- VIEWER gets read-only access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VIEWER' AND p.code IN (
    'DISTRIBUTION_ORDER_VIEW'
)
ON CONFLICT DO NOTHING;
