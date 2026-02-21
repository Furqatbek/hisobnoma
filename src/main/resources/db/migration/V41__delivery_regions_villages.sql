-- Delivery Regions
CREATE TABLE delivery_regions (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50),
    description     VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    version         BIGINT DEFAULT 0
);

CREATE INDEX idx_delivery_region_tenant ON delivery_regions(tenant_id);
CREATE INDEX idx_delivery_region_active ON delivery_regions(tenant_id, active);
CREATE UNIQUE INDEX uk_delivery_region_code ON delivery_regions(tenant_id, code) WHERE code IS NOT NULL;

-- Delivery Villages (areas within a region)
CREATE TABLE delivery_villages (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    region_id       BIGINT NOT NULL REFERENCES delivery_regions(id),
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50),
    description     VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    version         BIGINT DEFAULT 0
);

CREATE INDEX idx_delivery_village_tenant ON delivery_villages(tenant_id);
CREATE INDEX idx_delivery_village_region ON delivery_villages(region_id);
CREATE INDEX idx_delivery_village_active ON delivery_villages(tenant_id, active);
CREATE UNIQUE INDEX uk_delivery_village_code ON delivery_villages(tenant_id, code) WHERE code IS NOT NULL;
