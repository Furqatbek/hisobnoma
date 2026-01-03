-- V8__inventory_operations.sql
-- Inventory Operations Module (Block 5)

-- ===========================================
-- Vendors Table
-- ===========================================
CREATE TABLE vendors (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    contact_person VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(50),
    alt_phone VARCHAR(50),
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    tax_id VARCHAR(50),
    payment_terms VARCHAR(100),
    payment_terms_days INTEGER,
    credit_limit DECIMAL(18,4),
    current_balance DECIMAL(18,4) DEFAULT 0,
    default_currency VARCHAR(3) DEFAULT 'UZS',
    bank_name VARCHAR(100),
    bank_account VARCHAR(50),
    bank_routing VARCHAR(50),
    website VARCHAR(255),
    notes VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_preferred BOOLEAN DEFAULT FALSE,
    lead_time_days INTEGER,
    min_order_amount DECIMAL(18,4),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_vendors_tenant ON vendors(tenant_id);
CREATE INDEX idx_vendors_code ON vendors(code);
CREATE INDEX idx_vendors_active ON vendors(active);
CREATE UNIQUE INDEX idx_vendors_code_tenant ON vendors(code, tenant_id) WHERE tenant_id IS NOT NULL;

-- ===========================================
-- Purchase Orders Table
-- ===========================================
CREATE TABLE purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    po_number VARCHAR(50) NOT NULL UNIQUE,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id),
    location_id BIGINT NOT NULL REFERENCES locations(id),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    order_date DATE NOT NULL,
    expected_date DATE,
    received_date DATE,
    subtotal DECIMAL(18,4) DEFAULT 0,
    discount_amount DECIMAL(18,4) DEFAULT 0,
    discount_percent DECIMAL(5,2),
    tax_amount DECIMAL(18,4) DEFAULT 0,
    shipping_amount DECIMAL(18,4) DEFAULT 0,
    total_amount DECIMAL(18,4) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'UZS',
    payment_terms VARCHAR(100),
    shipping_method VARCHAR(100),
    shipping_address VARCHAR(500),
    notes VARCHAR(1000),
    internal_notes VARCHAR(1000),
    vendor_reference VARCHAR(100),
    approved_by BIGINT,
    approved_at TIMESTAMP,
    cancelled_by BIGINT,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_po_tenant ON purchase_orders(tenant_id);
CREATE INDEX idx_po_number ON purchase_orders(po_number);
CREATE INDEX idx_po_vendor ON purchase_orders(vendor_id);
CREATE INDEX idx_po_status ON purchase_orders(status);
CREATE INDEX idx_po_order_date ON purchase_orders(order_date);

-- ===========================================
-- Purchase Order Lines Table
-- ===========================================
CREATE TABLE purchase_order_lines (
    id BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    product_variant_id BIGINT REFERENCES product_variants(id),
    line_number INTEGER NOT NULL,
    description VARCHAR(500),
    quantity DECIMAL(18,4) NOT NULL,
    received_quantity DECIMAL(18,4) DEFAULT 0,
    cancelled_quantity DECIMAL(18,4) DEFAULT 0,
    uom_id BIGINT NOT NULL REFERENCES units_of_measure(id),
    unit_price DECIMAL(18,4) NOT NULL,
    discount_percent DECIMAL(5,2),
    discount_amount DECIMAL(18,4) DEFAULT 0,
    tax_percent DECIMAL(5,2),
    tax_amount DECIMAL(18,4) DEFAULT 0,
    line_total DECIMAL(18,4) DEFAULT 0,
    notes VARCHAR(500),
    expected_date DATE,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_po_line_tenant ON purchase_order_lines(tenant_id);
CREATE INDEX idx_po_line_po ON purchase_order_lines(purchase_order_id);
CREATE INDEX idx_po_line_product ON purchase_order_lines(product_id);

-- ===========================================
-- Receiving Orders Table
-- ===========================================
CREATE TABLE receiving_orders (
    id BIGSERIAL PRIMARY KEY,
    receiving_number VARCHAR(50) NOT NULL UNIQUE,
    purchase_order_id BIGINT REFERENCES purchase_orders(id),
    vendor_id BIGINT NOT NULL REFERENCES vendors(id),
    location_id BIGINT NOT NULL REFERENCES locations(id),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    receiving_date DATE NOT NULL,
    vendor_delivery_note VARCHAR(100),
    vendor_invoice_number VARCHAR(100),
    vendor_invoice_date DATE,
    subtotal DECIMAL(18,4) DEFAULT 0,
    discount_amount DECIMAL(18,4) DEFAULT 0,
    tax_amount DECIMAL(18,4) DEFAULT 0,
    shipping_amount DECIMAL(18,4) DEFAULT 0,
    total_amount DECIMAL(18,4) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'UZS',
    notes VARCHAR(1000),
    internal_notes VARCHAR(1000),
    received_by BIGINT,
    received_at TIMESTAMP,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    cancelled_by BIGINT,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),
    stock_updated BOOLEAN DEFAULT FALSE,
    ap_invoice_created BOOLEAN DEFAULT FALSE,
    ap_invoice_id BIGINT,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_receiving_tenant ON receiving_orders(tenant_id);
CREATE INDEX idx_receiving_number ON receiving_orders(receiving_number);
CREATE INDEX idx_receiving_po ON receiving_orders(purchase_order_id);
CREATE INDEX idx_receiving_vendor ON receiving_orders(vendor_id);
CREATE INDEX idx_receiving_status ON receiving_orders(status);
CREATE INDEX idx_receiving_date ON receiving_orders(receiving_date);

-- ===========================================
-- Receiving Lines Table
-- ===========================================
CREATE TABLE receiving_lines (
    id BIGSERIAL PRIMARY KEY,
    receiving_order_id BIGINT NOT NULL REFERENCES receiving_orders(id) ON DELETE CASCADE,
    po_line_id BIGINT REFERENCES purchase_order_lines(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    product_variant_id BIGINT REFERENCES product_variants(id),
    line_number INTEGER NOT NULL,
    description VARCHAR(500),
    expected_quantity DECIMAL(18,4),
    received_quantity DECIMAL(18,4) NOT NULL,
    accepted_quantity DECIMAL(18,4),
    rejected_quantity DECIMAL(18,4) DEFAULT 0,
    rejection_reason VARCHAR(500),
    uom_id BIGINT NOT NULL REFERENCES units_of_measure(id),
    unit_cost DECIMAL(18,4) NOT NULL,
    tax_percent DECIMAL(5,2),
    tax_amount DECIMAL(18,4) DEFAULT 0,
    line_total DECIMAL(18,4) DEFAULT 0,
    batch_number VARCHAR(100),
    serial_numbers VARCHAR(2000),
    manufacture_date DATE,
    expiry_date DATE,
    target_location_id BIGINT REFERENCES locations(id),
    notes VARCHAR(500),
    stock_movement_id BIGINT,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_receiving_line_tenant ON receiving_lines(tenant_id);
CREATE INDEX idx_receiving_line_receiving ON receiving_lines(receiving_order_id);
CREATE INDEX idx_receiving_line_product ON receiving_lines(product_id);
CREATE INDEX idx_receiving_line_po_line ON receiving_lines(po_line_id);

-- ===========================================
-- Inventory Counts Table
-- ===========================================
CREATE TABLE inventory_counts (
    id BIGSERIAL PRIMARY KEY,
    count_number VARCHAR(50) NOT NULL UNIQUE,
    location_id BIGINT NOT NULL REFERENCES locations(id),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    count_type VARCHAR(20) NOT NULL DEFAULT 'PARTIAL',
    count_date DATE NOT NULL,
    description VARCHAR(200),
    notes VARCHAR(1000),
    total_items INTEGER DEFAULT 0,
    counted_items INTEGER DEFAULT 0,
    variance_items INTEGER DEFAULT 0,
    total_variance_value DECIMAL(18,4) DEFAULT 0,
    positive_variance_value DECIMAL(18,4) DEFAULT 0,
    negative_variance_value DECIMAL(18,4) DEFAULT 0,
    freeze_stock BOOLEAN DEFAULT FALSE,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    cancelled_by BIGINT,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),
    adjustments_posted BOOLEAN DEFAULT FALSE,
    category_id BIGINT REFERENCES categories(id),
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_inv_count_tenant ON inventory_counts(tenant_id);
CREATE INDEX idx_inv_count_number ON inventory_counts(count_number);
CREATE INDEX idx_inv_count_location ON inventory_counts(location_id);
CREATE INDEX idx_inv_count_status ON inventory_counts(status);
CREATE INDEX idx_inv_count_date ON inventory_counts(count_date);

-- ===========================================
-- Inventory Count Lines Table
-- ===========================================
CREATE TABLE inventory_count_lines (
    id BIGSERIAL PRIMARY KEY,
    inventory_count_id BIGINT NOT NULL REFERENCES inventory_counts(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    product_variant_id BIGINT REFERENCES product_variants(id),
    line_number INTEGER NOT NULL,
    system_quantity DECIMAL(18,4),
    counted_quantity DECIMAL(18,4),
    variance_quantity DECIMAL(18,4),
    unit_cost DECIMAL(18,4),
    variance_value DECIMAL(18,4) DEFAULT 0,
    uom_id BIGINT NOT NULL REFERENCES units_of_measure(id),
    batch_number VARCHAR(100),
    serial_number VARCHAR(100),
    counted BOOLEAN NOT NULL DEFAULT FALSE,
    counted_by BIGINT,
    counted_at TIMESTAMP,
    recount_required BOOLEAN DEFAULT FALSE,
    recount_quantity DECIMAL(18,4),
    recounted_by BIGINT,
    recounted_at TIMESTAMP,
    notes VARCHAR(500),
    adjustment_reason VARCHAR(200),
    stock_movement_id BIGINT,
    adjustment_approved BOOLEAN DEFAULT FALSE,
    tenant_id BIGINT REFERENCES tenants(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_inv_count_line_tenant ON inventory_count_lines(tenant_id);
CREATE INDEX idx_inv_count_line_count ON inventory_count_lines(inventory_count_id);
CREATE INDEX idx_inv_count_line_product ON inventory_count_lines(product_id);

-- ===========================================
-- Add operations permissions
-- ===========================================
INSERT INTO permissions (name, code, description, module, action) VALUES
-- Vendor permissions
('View Vendors', 'INVENTORY_VENDOR_VIEW', 'View vendor/supplier information', 'INVENTORY', 'VIEW'),
('Manage Vendors', 'INVENTORY_VENDOR_MANAGE', 'Create, update, delete vendors', 'INVENTORY', 'MANAGE'),

-- Purchase Order permissions
('View Purchase Orders', 'INVENTORY_PO_VIEW', 'View purchase orders', 'INVENTORY', 'VIEW'),
('Create Purchase Orders', 'INVENTORY_PO_CREATE', 'Create purchase orders', 'INVENTORY', 'CREATE'),
('Update Purchase Orders', 'INVENTORY_PO_UPDATE', 'Update purchase orders', 'INVENTORY', 'UPDATE'),
('Approve Purchase Orders', 'INVENTORY_PO_APPROVE', 'Approve purchase orders', 'INVENTORY', 'APPROVE'),
('Cancel Purchase Orders', 'INVENTORY_PO_CANCEL', 'Cancel purchase orders', 'INVENTORY', 'DELETE'),

-- Receiving permissions
('View Receiving', 'INVENTORY_RECEIVING_VIEW', 'View receiving orders', 'INVENTORY', 'VIEW'),
('Create Receiving', 'INVENTORY_RECEIVING_CREATE', 'Create receiving orders', 'INVENTORY', 'CREATE'),
('Confirm Receiving', 'INVENTORY_RECEIVING_CONFIRM', 'Confirm and complete receiving', 'INVENTORY', 'UPDATE'),

-- Inventory Count permissions
('View Inventory Counts', 'INVENTORY_COUNT_VIEW', 'View inventory counts', 'INVENTORY', 'VIEW'),
('Create Inventory Counts', 'INVENTORY_COUNT_CREATE', 'Create inventory counts', 'INVENTORY', 'CREATE'),
('Perform Inventory Counts', 'INVENTORY_COUNT_PERFORM', 'Perform/record counts', 'INVENTORY', 'UPDATE'),
('Approve Inventory Counts', 'INVENTORY_COUNT_APPROVE', 'Approve counts and post adjustments', 'INVENTORY', 'APPROVE'),

-- Planning permissions
('View Inventory Planning', 'INVENTORY_PLANNING_VIEW', 'View reorder suggestions and analysis', 'INVENTORY', 'VIEW')
ON CONFLICT (code) DO NOTHING;

-- Assign operations permissions to INVENTORY_MANAGER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'INVENTORY_MANAGER'
AND p.code IN (
    'INVENTORY_VENDOR_VIEW', 'INVENTORY_VENDOR_MANAGE',
    'INVENTORY_PO_VIEW', 'INVENTORY_PO_CREATE', 'INVENTORY_PO_UPDATE', 'INVENTORY_PO_APPROVE', 'INVENTORY_PO_CANCEL',
    'INVENTORY_RECEIVING_VIEW', 'INVENTORY_RECEIVING_CREATE', 'INVENTORY_RECEIVING_CONFIRM',
    'INVENTORY_COUNT_VIEW', 'INVENTORY_COUNT_CREATE', 'INVENTORY_COUNT_PERFORM', 'INVENTORY_COUNT_APPROVE',
    'INVENTORY_PLANNING_VIEW'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- Assign limited permissions to WAREHOUSE_STAFF role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'WAREHOUSE_STAFF'
AND p.code IN (
    'INVENTORY_VENDOR_VIEW',
    'INVENTORY_PO_VIEW',
    'INVENTORY_RECEIVING_VIEW', 'INVENTORY_RECEIVING_CREATE', 'INVENTORY_RECEIVING_CONFIRM',
    'INVENTORY_COUNT_VIEW', 'INVENTORY_COUNT_CREATE', 'INVENTORY_COUNT_PERFORM'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- Assign view permissions to STORE_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'STORE_MANAGER'
AND p.code IN (
    'INVENTORY_VENDOR_VIEW',
    'INVENTORY_PO_VIEW',
    'INVENTORY_RECEIVING_VIEW',
    'INVENTORY_COUNT_VIEW',
    'INVENTORY_PLANNING_VIEW'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
