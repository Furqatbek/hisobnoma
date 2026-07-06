-- Distribution module — Slice 1: agents and their territory coverage.

-- Distribution agents (field sales / route / van-sales reps)
CREATE TABLE distribution_agents (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT,
    code                VARCHAR(50) NOT NULL,
    name                VARCHAR(200) NOT NULL,
    phone               VARCHAR(50),
    email               VARCHAR(100),
    photo_url           VARCHAR(500),
    user_id             BIGINT,
    employee_id         BIGINT,
    vehicle_plate       VARCHAR(50),
    vehicle_name        VARCHAR(200),
    commission_percent  NUMERIC(5,2),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    hired_at            DATE,
    terminated_at       DATE,
    notes               VARCHAR(1000),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    version             BIGINT DEFAULT 0
);

CREATE INDEX idx_dist_agents_tenant ON distribution_agents(tenant_id);
CREATE INDEX idx_dist_agents_user ON distribution_agents(user_id);
CREATE INDEX idx_dist_agents_status ON distribution_agents(tenant_id, status);
CREATE UNIQUE INDEX uk_dist_agent_code ON distribution_agents(tenant_id, code);

-- Territory coverage: agent -> delivery region (optionally narrowed to a village)
CREATE TABLE distribution_territories (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    agent_id        BIGINT NOT NULL REFERENCES distribution_agents(id),
    region_id       BIGINT NOT NULL REFERENCES delivery_regions(id),
    village_id      BIGINT REFERENCES delivery_villages(id),
    priority        INT NOT NULL DEFAULT 0,
    exclusive       BOOLEAN NOT NULL DEFAULT FALSE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    version         BIGINT DEFAULT 0
);

CREATE INDEX idx_dist_territories_tenant ON distribution_territories(tenant_id);
CREATE INDEX idx_dist_territories_agent ON distribution_territories(agent_id);
CREATE INDEX idx_dist_territories_region ON distribution_territories(tenant_id, region_id);

-- Distribution agent permissions
INSERT INTO permissions (name, code, description, module, action) VALUES
('View Distribution Agents', 'DISTRIBUTION_AGENT_VIEW', 'View distribution agents and their territories', 'DISTRIBUTION', 'VIEW'),
('Manage Distribution Agents', 'DISTRIBUTION_AGENT_MANAGE', 'Create, update and delete distribution agents', 'DISTRIBUTION', 'MANAGE')
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

-- STORE_MANAGER manages agents
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'STORE_MANAGER' AND p.code IN (
    'DISTRIBUTION_AGENT_VIEW', 'DISTRIBUTION_AGENT_MANAGE'
)
ON CONFLICT DO NOTHING;

-- VIEWER gets read-only access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VIEWER' AND p.code IN (
    'DISTRIBUTION_AGENT_VIEW'
)
ON CONFLICT DO NOTHING;
