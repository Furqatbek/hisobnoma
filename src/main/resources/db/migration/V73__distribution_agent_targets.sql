-- Distribution module — Slice 5: agent KPI targets (actuals are computed live).

CREATE TABLE distribution_agent_targets (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT,
    agent_id                BIGINT NOT NULL REFERENCES distribution_agents(id),
    period_type             VARCHAR(10) NOT NULL DEFAULT 'MONTHLY',
    period_start            DATE NOT NULL,
    period_end              DATE NOT NULL,
    target_revenue          NUMERIC(18,4) DEFAULT 0,
    target_orders           INT DEFAULT 0,
    target_visits           INT DEFAULT 0,
    target_new_customers    INT DEFAULT 0,
    target_collection       NUMERIC(18,4) DEFAULT 0,
    notes                   VARCHAR(500),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    created_by              BIGINT,
    updated_by              BIGINT,
    version                 BIGINT DEFAULT 0,
    CONSTRAINT uk_agent_targets_period UNIQUE (tenant_id, agent_id, period_start, period_end)
);

CREATE INDEX idx_agent_targets_tenant ON distribution_agent_targets(tenant_id);
CREATE INDEX idx_agent_targets_agent ON distribution_agent_targets(agent_id);
CREATE INDEX idx_agent_targets_period ON distribution_agent_targets(tenant_id, period_start, period_end);

-- KPI permissions
INSERT INTO permissions (name, code, description, module, action) VALUES
('View Distribution KPIs', 'DISTRIBUTION_KPI_VIEW', 'View agent KPI dashboards and leaderboards', 'DISTRIBUTION', 'VIEW'),
('Manage Distribution Targets', 'DISTRIBUTION_KPI_MANAGE', 'Set and edit agent performance targets', 'DISTRIBUTION', 'MANAGE')
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
    'DISTRIBUTION_KPI_VIEW', 'DISTRIBUTION_KPI_MANAGE'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VIEWER' AND p.code IN (
    'DISTRIBUTION_KPI_VIEW'
)
ON CONFLICT DO NOTHING;
