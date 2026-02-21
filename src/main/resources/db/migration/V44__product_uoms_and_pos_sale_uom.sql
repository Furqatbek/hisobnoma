-- Product alternate UOMs: maps products to alternate units with product-specific conversion factors
-- Example: Qum (Sand) baseUom=TONNA
--   product_uoms: (uom=SAMOSVOL, conversionFactor=12.0)  → 1 samosvol = 12 tonna
--   product_uoms: (uom=CHELAK,   conversionFactor=0.5)   → 1 chelak  = 0.5 tonna
CREATE TABLE product_uoms (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    uom_id          BIGINT NOT NULL REFERENCES units_of_measure(id),
    conversion_factor DECIMAL(18,6) NOT NULL,
    selling_price   DECIMAL(18,4),
    is_default_sale BOOLEAN NOT NULL DEFAULT FALSE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INTEGER DEFAULT 0,
    tenant_id       BIGINT REFERENCES tenants(id),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    version         BIGINT DEFAULT 0,
    CONSTRAINT uk_product_uom UNIQUE (product_id, uom_id, tenant_id)
);

CREATE INDEX idx_product_uoms_product ON product_uoms(product_id);
CREATE INDEX idx_product_uoms_tenant ON product_uoms(tenant_id);

-- POS transaction lines: track which UOM the cashier sold in
ALTER TABLE pos_transaction_lines ADD COLUMN sale_uom_id   BIGINT REFERENCES units_of_measure(id);
ALTER TABLE pos_transaction_lines ADD COLUMN sale_quantity  DECIMAL(18,4);
ALTER TABLE pos_transaction_lines ADD COLUMN sale_uom_code VARCHAR(20);
ALTER TABLE pos_transaction_lines ADD COLUMN sale_uom_name VARCHAR(50);
