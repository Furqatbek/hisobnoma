-- Web catalog items: curated draft/live product list for the online shop (mobile app)
CREATE TABLE web_catalog_items (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    product_id      BIGINT NOT NULL REFERENCES products(id),
    display_name    VARCHAR(200),
    price_override  NUMERIC(18,4),
    sort_order      INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    version         BIGINT DEFAULT 0
);

CREATE INDEX idx_web_catalog_tenant ON web_catalog_items(tenant_id);
CREATE INDEX idx_web_catalog_status ON web_catalog_items(tenant_id, status);
CREATE INDEX idx_web_catalog_sort ON web_catalog_items(tenant_id, sort_order);
CREATE UNIQUE INDEX uk_web_catalog_product ON web_catalog_items(tenant_id, product_id);

-- Web catalog permissions
INSERT INTO permissions (name, code, description, module, action)
VALUES
    ('Veb-katalogni ko''rish', 'WEB_CATALOG_VIEW', 'Onlayn katalog ro''yxatini ko''rish', 'WEB', 'READ'),
    ('Veb-katalogni boshqarish', 'WEB_CATALOG_MANAGE', 'Onlayn katalog elementlarini boshqarish', 'WEB', 'UPDATE')
ON CONFLICT (code) DO NOTHING;

-- Grant web catalog permissions to ADMIN and SUPER_ADMIN roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code IN ('WEB_CATALOG_VIEW', 'WEB_CATALOG_MANAGE')
ON CONFLICT DO NOTHING;
