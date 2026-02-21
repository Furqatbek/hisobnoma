-- Product-Vendor mapping table for multi-vendor support
-- Allows each product to have multiple vendors with vendor-specific pricing and lead times

CREATE TABLE product_vendors (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    vendor_sku VARCHAR(100),
    vendor_product_name VARCHAR(200),
    unit_cost DECIMAL(18, 4),
    min_order_quantity DECIMAL(18, 4),
    lead_time_days INT,
    is_preferred BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,

    CONSTRAINT fk_pv_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_pv_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    CONSTRAINT uk_pv_tenant_product_vendor UNIQUE (tenant_id, product_id, vendor_id)
);

CREATE INDEX idx_pv_tenant ON product_vendors (tenant_id);
CREATE INDEX idx_pv_product ON product_vendors (product_id);
CREATE INDEX idx_pv_vendor ON product_vendors (vendor_id);
CREATE INDEX idx_pv_preferred ON product_vendors (tenant_id, product_id, is_preferred);
