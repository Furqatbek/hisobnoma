-- Distribution module — Slice 3: van-sales loadouts + reconciliation.

CREATE TABLE distribution_van_loadouts (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             BIGINT,
    loadout_number        VARCHAR(50) NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    agent_id              BIGINT NOT NULL REFERENCES distribution_agents(id),
    vehicle_location_id   BIGINT NOT NULL REFERENCES locations(id),
    source_location_id    BIGINT NOT NULL REFERENCES locations(id),
    loadout_date          DATE NOT NULL,
    reconciled_at         TIMESTAMP,
    reconciled_by         BIGINT,
    total_loaded_value    NUMERIC(18,4) NOT NULL DEFAULT 0,
    total_sold_value      NUMERIC(18,4) NOT NULL DEFAULT 0,
    total_returned_value  NUMERIC(18,4) NOT NULL DEFAULT 0,
    total_damaged_value   NUMERIC(18,4) NOT NULL DEFAULT 0,
    expected_cash         NUMERIC(18,4) NOT NULL DEFAULT 0,
    actual_cash           NUMERIC(18,4) NOT NULL DEFAULT 0,
    cash_difference       NUMERIC(18,4) NOT NULL DEFAULT 0,
    currency              VARCHAR(3) NOT NULL DEFAULT 'UZS',
    notes                 VARCHAR(1000),
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP,
    created_by            BIGINT,
    updated_by            BIGINT,
    version               BIGINT DEFAULT 0,
    CONSTRAINT uk_van_loadouts_number UNIQUE (tenant_id, loadout_number)
);

CREATE INDEX idx_van_loadouts_tenant ON distribution_van_loadouts(tenant_id);
CREATE INDEX idx_van_loadouts_status ON distribution_van_loadouts(tenant_id, status);
CREATE INDEX idx_van_loadouts_agent ON distribution_van_loadouts(agent_id);
CREATE INDEX idx_van_loadouts_created ON distribution_van_loadouts(tenant_id, created_at);

CREATE TABLE distribution_van_loadout_lines (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT,
    loadout_id          BIGINT NOT NULL REFERENCES distribution_van_loadouts(id),
    product_id          BIGINT NOT NULL REFERENCES products(id),
    product_name        VARCHAR(200),
    product_sku         VARCHAR(50),
    unit_name           VARCHAR(50),
    quantity_loaded     NUMERIC(18,4) NOT NULL,
    quantity_returned   NUMERIC(18,4) NOT NULL DEFAULT 0,
    quantity_damaged    NUMERIC(18,4) NOT NULL DEFAULT 0,
    quantity_sold       NUMERIC(18,4) NOT NULL DEFAULT 0,
    unit_cost           NUMERIC(18,4) DEFAULT 0,
    unit_price          NUMERIC(18,4) DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    version             BIGINT DEFAULT 0
);

CREATE INDEX idx_van_lines_tenant ON distribution_van_loadout_lines(tenant_id);
CREATE INDEX idx_van_lines_loadout ON distribution_van_loadout_lines(loadout_id);
CREATE INDEX idx_van_lines_product ON distribution_van_loadout_lines(product_id);

-- Van loadout permissions
INSERT INTO permissions (name, code, description, module, action) VALUES
('View Van Loadouts', 'DISTRIBUTION_VAN_VIEW', 'View van-sales loadouts', 'DISTRIBUTION', 'VIEW'),
('Manage Van Loadouts', 'DISTRIBUTION_VAN_MANAGE', 'Create, load, reconcile and cancel van loadouts', 'DISTRIBUTION', 'MANAGE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND p.code LIKE 'DISTRIBUTION_%'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code LIKE 'DISTRIBUTION_%'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'STORE_MANAGER' AND p.code IN (
    'DISTRIBUTION_VAN_VIEW', 'DISTRIBUTION_VAN_MANAGE'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VIEWER' AND p.code IN (
    'DISTRIBUTION_VAN_VIEW'
)
ON CONFLICT DO NOTHING;
