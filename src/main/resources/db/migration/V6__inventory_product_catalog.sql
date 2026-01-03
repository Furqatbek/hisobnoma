-- V4__inventory_product_catalog.sql
-- Inventory Module - Product Catalog tables

-- Categories table (hierarchical)
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(500),
    parent_id BIGINT REFERENCES categories(id),
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    level INTEGER NOT NULL DEFAULT 0,
    path VARCHAR(500),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_categories_tenant ON categories(tenant_id);
CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_code ON categories(code);
CREATE INDEX idx_categories_active ON categories(active);

-- Brands table
CREATE TABLE brands (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    logo_url VARCHAR(500),
    website VARCHAR(255),
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_brands_tenant ON brands(tenant_id);
CREATE INDEX idx_brands_code ON brands(code);
CREATE INDEX idx_brands_active ON brands(active);

-- Units of Measure table
CREATE TABLE units_of_measure (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(50) NOT NULL,
    symbol VARCHAR(10),
    description VARCHAR(200),
    base_uom_id BIGINT REFERENCES units_of_measure(id),
    conversion_factor DECIMAL(18, 6) NOT NULL DEFAULT 1.0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_base_unit BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_uom_tenant ON units_of_measure(tenant_id);
CREATE INDEX idx_uom_code ON units_of_measure(code);
CREATE INDEX idx_uom_base ON units_of_measure(base_uom_id);

-- Products table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    barcode VARCHAR(50),
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    short_description VARCHAR(500),
    category_id BIGINT REFERENCES categories(id),
    brand_id BIGINT REFERENCES brands(id),
    base_uom_id BIGINT NOT NULL REFERENCES units_of_measure(id),
    cost_price DECIMAL(18, 4) DEFAULT 0,
    selling_price DECIMAL(18, 4) NOT NULL,
    min_selling_price DECIMAL(18, 4),
    wholesale_price DECIMAL(18, 4),
    track_inventory BOOLEAN NOT NULL DEFAULT TRUE,
    allow_negative_stock BOOLEAN NOT NULL DEFAULT FALSE,
    min_stock_level DECIMAL(18, 4) DEFAULT 0,
    reorder_point DECIMAL(18, 4) DEFAULT 0,
    reorder_quantity DECIMAL(18, 4) DEFAULT 0,
    max_stock_level DECIMAL(18, 4),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_service BOOLEAN NOT NULL DEFAULT FALSE,
    is_sellable BOOLEAN NOT NULL DEFAULT TRUE,
    is_purchasable BOOLEAN NOT NULL DEFAULT TRUE,
    has_variants BOOLEAN NOT NULL DEFAULT FALSE,
    track_batch BOOLEAN NOT NULL DEFAULT FALSE,
    track_serial BOOLEAN NOT NULL DEFAULT FALSE,
    weight DECIMAL(10, 3),
    weight_unit VARCHAR(20),
    length DECIMAL(10, 2),
    width DECIMAL(10, 2),
    height DECIMAL(10, 2),
    dimension_unit VARCHAR(20),
    primary_image_url VARCHAR(500),
    manufacturer VARCHAR(200),
    manufacturer_part_number VARCHAR(100),
    tax_code VARCHAR(100),
    notes VARCHAR(2000),
    tags VARCHAR(500),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_products_tenant ON products(tenant_id);
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_barcode ON products(barcode);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_brand ON products(brand_id);
CREATE INDEX idx_products_active ON products(active);
CREATE INDEX idx_products_name ON products(name);

-- Product Variants table
CREATE TABLE product_variants (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(50) NOT NULL UNIQUE,
    barcode VARCHAR(50),
    name VARCHAR(200) NOT NULL,
    option1_name VARCHAR(100),
    option1_value VARCHAR(100),
    option2_name VARCHAR(100),
    option2_value VARCHAR(100),
    option3_name VARCHAR(100),
    option3_value VARCHAR(100),
    cost_price DECIMAL(18, 4),
    selling_price DECIMAL(18, 4),
    price_difference DECIMAL(18, 4) DEFAULT 0,
    weight DECIMAL(10, 3),
    image_url VARCHAR(500),
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_variants_product ON product_variants(product_id);
CREATE INDEX idx_variants_sku ON product_variants(sku);
CREATE INDEX idx_variants_barcode ON product_variants(barcode);
CREATE INDEX idx_variants_tenant ON product_variants(tenant_id);

-- Product Images table
CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    alt_text VARCHAR(200),
    title VARCHAR(200),
    sort_order INTEGER NOT NULL DEFAULT 0,
    "primary" BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_product_images_product ON product_images(product_id);
CREATE INDEX idx_product_images_tenant ON product_images(tenant_id);

-- Product Attributes table
CREATE TABLE product_attributes (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    attribute_name VARCHAR(100) NOT NULL,
    attribute_value VARCHAR(500),
    attribute_type VARCHAR(50) DEFAULT 'TEXT',
    attribute_group VARCHAR(100),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    is_searchable BOOLEAN NOT NULL DEFAULT FALSE,
    is_filterable BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_product_attrs_product ON product_attributes(product_id);
CREATE INDEX idx_product_attrs_name ON product_attributes(attribute_name);
CREATE INDEX idx_product_attrs_tenant ON product_attributes(tenant_id);
CREATE INDEX idx_product_attrs_group ON product_attributes(attribute_group);

-- Insert default units of measure
INSERT INTO units_of_measure (code, name, symbol, description, is_base_unit) VALUES
('PCS', 'Pieces', 'pcs', 'Individual pieces/units', TRUE),
('BOX', 'Box', 'box', 'Box containing multiple units', TRUE),
('KG', 'Kilogram', 'kg', 'Weight in kilograms', TRUE),
('G', 'Gram', 'g', 'Weight in grams', FALSE),
('L', 'Liter', 'L', 'Volume in liters', TRUE),
('ML', 'Milliliter', 'mL', 'Volume in milliliters', FALSE),
('M', 'Meter', 'm', 'Length in meters', TRUE),
('CM', 'Centimeter', 'cm', 'Length in centimeters', FALSE),
('SET', 'Set', 'set', 'Set of items', TRUE),
('PACK', 'Pack', 'pack', 'Package/Pack of items', TRUE),
('DOZEN', 'Dozen', 'dz', '12 units', FALSE),
('PAIR', 'Pair', 'pr', '2 units', FALSE);

-- Set up unit conversions
UPDATE units_of_measure SET base_uom_id = (SELECT id FROM units_of_measure WHERE code = 'KG'), conversion_factor = 0.001 WHERE code = 'G';
UPDATE units_of_measure SET base_uom_id = (SELECT id FROM units_of_measure WHERE code = 'L'), conversion_factor = 0.001 WHERE code = 'ML';
UPDATE units_of_measure SET base_uom_id = (SELECT id FROM units_of_measure WHERE code = 'M'), conversion_factor = 0.01 WHERE code = 'CM';
UPDATE units_of_measure SET base_uom_id = (SELECT id FROM units_of_measure WHERE code = 'PCS'), conversion_factor = 12 WHERE code = 'DOZEN';
UPDATE units_of_measure SET base_uom_id = (SELECT id FROM units_of_measure WHERE code = 'PCS'), conversion_factor = 2 WHERE code = 'PAIR';

-- Insert sample categories
INSERT INTO categories (code, name, description, level, path, active) VALUES
('ELECTRONICS', 'Electronics', 'Electronic devices and accessories', 0, '/1', TRUE),
('CLOTHING', 'Clothing', 'Apparel and fashion items', 0, '/2', TRUE),
('FOOD', 'Food & Beverages', 'Food items and drinks', 0, '/3', TRUE),
('HOME', 'Home & Living', 'Home improvement and furniture', 0, '/4', TRUE),
('OFFICE', 'Office Supplies', 'Office and stationery items', 0, '/5', TRUE);
