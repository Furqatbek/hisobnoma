-- Distribution module — Slice 4: routes, route stops, and field visits.

CREATE TABLE distribution_routes (
    id                          BIGSERIAL PRIMARY KEY,
    tenant_id                   BIGINT,
    code                        VARCHAR(50) NOT NULL,
    name                        VARCHAR(200) NOT NULL,
    agent_id                    BIGINT REFERENCES distribution_agents(id),
    territory_region_id         BIGINT REFERENCES delivery_regions(id),
    day_of_week                 VARCHAR(10),
    estimated_duration_minutes  INT,
    distance_km                 NUMERIC(10,2),
    status                      VARCHAR(10) NOT NULL DEFAULT 'DRAFT',
    notes                       VARCHAR(1000),
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP,
    created_by                  BIGINT,
    updated_by                  BIGINT,
    version                     BIGINT DEFAULT 0,
    CONSTRAINT uk_dist_routes_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_dist_routes_tenant ON distribution_routes(tenant_id);
CREATE INDEX idx_dist_routes_agent ON distribution_routes(agent_id);
CREATE INDEX idx_dist_routes_status ON distribution_routes(tenant_id, status);

CREATE TABLE distribution_route_stops (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT,
    route_id            BIGINT NOT NULL REFERENCES distribution_routes(id),
    customer_id         BIGINT NOT NULL REFERENCES customers(id),
    customer_name       VARCHAR(200),
    sort_order          INT NOT NULL DEFAULT 0,
    visit_window_start  TIME,
    visit_window_end    TIME,
    latitude            NUMERIC(10,7),
    longitude           NUMERIC(10,7),
    address             VARCHAR(500),
    notes               VARCHAR(500),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    version             BIGINT DEFAULT 0
);

CREATE INDEX idx_route_stops_tenant ON distribution_route_stops(tenant_id);
CREATE INDEX idx_route_stops_route ON distribution_route_stops(route_id);
CREATE INDEX idx_route_stops_customer ON distribution_route_stops(customer_id);

CREATE TABLE distribution_visits (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT,
    agent_id                BIGINT NOT NULL REFERENCES distribution_agents(id),
    customer_id             BIGINT NOT NULL REFERENCES customers(id),
    customer_name           VARCHAR(200),
    route_id                BIGINT REFERENCES distribution_routes(id),
    route_stop_id           BIGINT,
    check_in_at             TIMESTAMP NOT NULL,
    check_out_at            TIMESTAMP,
    check_in_lat            NUMERIC(10,7),
    check_in_lng            NUMERIC(10,7),
    check_out_lat           NUMERIC(10,7),
    check_out_lng           NUMERIC(10,7),
    visit_type              VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    outcome                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    distribution_order_id   BIGINT,
    notes                   VARCHAR(1000),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    created_by              BIGINT,
    updated_by              BIGINT,
    version                 BIGINT DEFAULT 0
);

CREATE INDEX idx_dist_visits_tenant ON distribution_visits(tenant_id);
CREATE INDEX idx_dist_visits_agent ON distribution_visits(tenant_id, agent_id);
CREATE INDEX idx_dist_visits_customer ON distribution_visits(customer_id);
CREATE INDEX idx_dist_visits_checkin ON distribution_visits(tenant_id, check_in_at);

-- Route + visit permissions
INSERT INTO permissions (name, code, description, module, action) VALUES
('View Distribution Routes', 'DISTRIBUTION_ROUTE_VIEW', 'View distribution routes', 'DISTRIBUTION', 'VIEW'),
('Manage Distribution Routes', 'DISTRIBUTION_ROUTE_MANAGE', 'Create, edit and delete distribution routes', 'DISTRIBUTION', 'MANAGE'),
('View Distribution Visits', 'DISTRIBUTION_VISIT_VIEW', 'View field visits', 'DISTRIBUTION', 'VIEW'),
('Manage Distribution Visits', 'DISTRIBUTION_VISIT_MANAGE', 'Record field visit check-ins and check-outs', 'DISTRIBUTION', 'MANAGE')
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
    'DISTRIBUTION_ROUTE_VIEW', 'DISTRIBUTION_ROUTE_MANAGE',
    'DISTRIBUTION_VISIT_VIEW', 'DISTRIBUTION_VISIT_MANAGE'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VIEWER' AND p.code IN (
    'DISTRIBUTION_ROUTE_VIEW', 'DISTRIBUTION_VISIT_VIEW'
)
ON CONFLICT DO NOTHING;
