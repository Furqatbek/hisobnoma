-- V7__inventory_stock_management.sql
-- Stock Management Module (Block 4)

-- ===========================================
-- Locations Table
-- ===========================================
CREATE TABLE locations (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    location_type VARCHAR(20) NOT NULL,
    parent_location_id BIGINT REFERENCES locations(id),
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    phone VARCHAR(50),
    email VARCHAR(100),
    manager_name VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    allow_negative_stock BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER DEFAULT 0,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_locations_tenant ON locations(tenant_id);
CREATE INDEX idx_locations_code ON locations(code);
CREATE INDEX idx_locations_type ON locations(location_type);
CREATE INDEX idx_locations_parent ON locations(parent_location_id);
CREATE INDEX idx_locations_active ON locations(active);
CREATE UNIQUE INDEX idx_locations_code_tenant ON locations(code, tenant_id) WHERE tenant_id IS NOT NULL;

-- ===========================================
-- Stock Table (Inventory Levels)
-- ===========================================
CREATE TABLE stock (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    location_id BIGINT NOT NULL REFERENCES locations(id),
    quantity_on_hand DECIMAL(18,4) NOT NULL DEFAULT 0,
    quantity_reserved DECIMAL(18,4) NOT NULL DEFAULT 0,
    quantity_in_transit DECIMAL(18,4) NOT NULL DEFAULT 0,
    quantity_on_order DECIMAL(18,4) NOT NULL DEFAULT 0,
    average_cost DECIMAL(18,4) DEFAULT 0,
    last_cost DECIMAL(18,4),
    reorder_point DECIMAL(18,4),
    reorder_quantity DECIMAL(18,4),
    min_stock_level DECIMAL(18,4),
    max_stock_level DECIMAL(18,4),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_stock_product_location UNIQUE (product_id, location_id, tenant_id)
);

CREATE INDEX idx_stock_tenant ON stock(tenant_id);
CREATE INDEX idx_stock_product ON stock(product_id);
CREATE INDEX idx_stock_location ON stock(location_id);
CREATE INDEX idx_stock_product_location ON stock(product_id, location_id);

-- ===========================================
-- Stock Movements Table
-- ===========================================
CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    product_variant_id BIGINT REFERENCES product_variants(id),
    from_location_id BIGINT REFERENCES locations(id),
    to_location_id BIGINT REFERENCES locations(id),
    movement_type VARCHAR(30) NOT NULL,
    quantity DECIMAL(18,4) NOT NULL,
    unit_cost DECIMAL(18,4),
    total_cost DECIMAL(18,4),
    reference_type VARCHAR(30),
    reference_id BIGINT,
    reference_number VARCHAR(50),
    movement_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(200),
    notes VARCHAR(1000),
    batch_number VARCHAR(100),
    serial_number VARCHAR(100),
    quantity_before DECIMAL(18,4),
    quantity_after DECIMAL(18,4),
    reversed BOOLEAN NOT NULL DEFAULT FALSE,
    reversal_movement_id BIGINT,
    original_movement_id BIGINT,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_stock_movements_tenant ON stock_movements(tenant_id);
CREATE INDEX idx_stock_movements_product ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_from_location ON stock_movements(from_location_id);
CREATE INDEX idx_stock_movements_to_location ON stock_movements(to_location_id);
CREATE INDEX idx_stock_movements_type ON stock_movements(movement_type);
CREATE INDEX idx_stock_movements_reference ON stock_movements(reference_type, reference_id);
CREATE INDEX idx_stock_movements_date ON stock_movements(movement_date);

-- ===========================================
-- Stock Batches Table
-- ===========================================
CREATE TABLE stock_batches (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    location_id BIGINT NOT NULL REFERENCES locations(id),
    batch_number VARCHAR(100) NOT NULL,
    quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
    initial_quantity DECIMAL(18,4),
    quantity_reserved DECIMAL(18,4) NOT NULL DEFAULT 0,
    manufacture_date DATE,
    expiry_date DATE,
    received_date DATE,
    supplier_batch_number VARCHAR(100),
    unit_cost DECIMAL(18,4),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(500),
    supplier_id BIGINT,
    receiving_order_id BIGINT,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_stock_batch_product_location_number UNIQUE (product_id, location_id, batch_number, tenant_id)
);

CREATE INDEX idx_stock_batches_tenant ON stock_batches(tenant_id);
CREATE INDEX idx_stock_batches_product ON stock_batches(product_id);
CREATE INDEX idx_stock_batches_location ON stock_batches(location_id);
CREATE INDEX idx_stock_batches_number ON stock_batches(batch_number);
CREATE INDEX idx_stock_batches_expiry ON stock_batches(expiry_date);
CREATE INDEX idx_stock_batches_status ON stock_batches(status);

-- ===========================================
-- Serial Numbers Table
-- ===========================================
CREATE TABLE serial_numbers (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    product_variant_id BIGINT REFERENCES product_variants(id),
    location_id BIGINT REFERENCES locations(id),
    serial_number VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    batch_id BIGINT REFERENCES stock_batches(id),
    unit_cost DECIMAL(18,4),
    manufacture_date DATE,
    expiry_date DATE,
    warranty_start_date DATE,
    warranty_end_date DATE,
    received_date TIMESTAMP,
    sold_date TIMESTAMP,
    supplier_id BIGINT,
    customer_id BIGINT,
    receiving_order_id BIGINT,
    sales_transaction_id BIGINT,
    manufacturer_serial VARCHAR(100),
    imei VARCHAR(50),
    mac_address VARCHAR(50),
    notes VARCHAR(500),
    reserved BOOLEAN NOT NULL DEFAULT FALSE,
    reserved_for_order_id BIGINT,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_serial_number_product UNIQUE (product_id, serial_number, tenant_id)
);

CREATE INDEX idx_serial_numbers_tenant ON serial_numbers(tenant_id);
CREATE INDEX idx_serial_numbers_product ON serial_numbers(product_id);
CREATE INDEX idx_serial_numbers_location ON serial_numbers(location_id);
CREATE INDEX idx_serial_numbers_serial ON serial_numbers(serial_number);
CREATE INDEX idx_serial_numbers_status ON serial_numbers(status);
CREATE INDEX idx_serial_numbers_batch ON serial_numbers(batch_id);
CREATE INDEX idx_serial_numbers_imei ON serial_numbers(imei) WHERE imei IS NOT NULL;

-- ===========================================
-- Stock Reservations Table (for tracking pending reservations)
-- ===========================================
CREATE TABLE stock_reservations (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    product_variant_id BIGINT REFERENCES product_variants(id),
    location_id BIGINT NOT NULL REFERENCES locations(id),
    quantity DECIMAL(18,4) NOT NULL,
    reserved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    reference_type VARCHAR(30) NOT NULL,
    reference_id BIGINT NOT NULL,
    reference_number VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(500),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_stock_reservations_tenant ON stock_reservations(tenant_id);
CREATE INDEX idx_stock_reservations_product ON stock_reservations(product_id);
CREATE INDEX idx_stock_reservations_location ON stock_reservations(location_id);
CREATE INDEX idx_stock_reservations_reference ON stock_reservations(reference_type, reference_id);
CREATE INDEX idx_stock_reservations_status ON stock_reservations(status);
CREATE INDEX idx_stock_reservations_expires ON stock_reservations(expires_at) WHERE expires_at IS NOT NULL;

-- ===========================================
-- Add stock management permissions
-- ===========================================
INSERT INTO permissions (name, code, description, module, action) VALUES
('View Stock Levels', 'INVENTORY_STOCK_VIEW', 'View stock levels and inventory', 'INVENTORY', 'VIEW'),
('Adjust Stock', 'INVENTORY_STOCK_ADJUST', 'Adjust stock quantities', 'INVENTORY', 'UPDATE'),
('Transfer Stock', 'INVENTORY_STOCK_TRANSFER', 'Transfer stock between locations', 'INVENTORY', 'UPDATE'),
('Receive Stock', 'INVENTORY_STOCK_RECEIVE', 'Receive stock from purchase orders', 'INVENTORY', 'CREATE'),
('Issue Stock', 'INVENTORY_STOCK_ISSUE', 'Issue stock from inventory', 'INVENTORY', 'UPDATE'),
('Manage Locations', 'INVENTORY_LOCATION_MANAGE', 'Create, update, delete locations', 'INVENTORY', 'MANAGE'),
('View Batches', 'INVENTORY_BATCH_VIEW', 'View batch information', 'INVENTORY', 'VIEW'),
('Manage Batches', 'INVENTORY_BATCH_MANAGE', 'Manage batch tracking', 'INVENTORY', 'MANAGE'),
('View Serials', 'INVENTORY_SERIAL_VIEW', 'View serial number information', 'INVENTORY', 'VIEW'),
('Manage Serials', 'INVENTORY_SERIAL_MANAGE', 'Manage serial number tracking', 'INVENTORY', 'MANAGE')
ON CONFLICT (code) DO NOTHING;

-- Assign stock permissions to INVENTORY_MANAGER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'INVENTORY_MANAGER'
AND p.code IN (
    'INVENTORY_STOCK_VIEW', 'INVENTORY_STOCK_ADJUST', 'INVENTORY_STOCK_TRANSFER',
    'INVENTORY_STOCK_RECEIVE', 'INVENTORY_STOCK_ISSUE', 'INVENTORY_LOCATION_MANAGE',
    'INVENTORY_BATCH_VIEW', 'INVENTORY_BATCH_MANAGE', 'INVENTORY_SERIAL_VIEW', 'INVENTORY_SERIAL_MANAGE'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- Assign view permissions to WAREHOUSE_STAFF role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'WAREHOUSE_STAFF'
AND p.code IN (
    'INVENTORY_STOCK_VIEW', 'INVENTORY_STOCK_RECEIVE', 'INVENTORY_STOCK_ISSUE',
    'INVENTORY_BATCH_VIEW', 'INVENTORY_SERIAL_VIEW'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- Assign view permissions to STORE_MANAGER and CASHIER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('STORE_MANAGER', 'CASHIER')
AND p.code = 'INVENTORY_STOCK_VIEW'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
