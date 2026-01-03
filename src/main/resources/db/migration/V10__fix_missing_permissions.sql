-- V10__fix_missing_permissions.sql
-- Add missing permissions used by controllers but not defined in previous migrations

-- ===========================================
-- Insert Missing Inventory Permissions
-- ===========================================
INSERT INTO permissions (name, code, description, module, action) VALUES
-- Stock Permissions (missing from V3)
('View Stock', 'INVENTORY_STOCK_VIEW', 'View stock levels and inventory', 'INVENTORY', 'VIEW'),
('Receive Stock', 'INVENTORY_STOCK_RECEIVE', 'Receive stock into inventory', 'INVENTORY', 'CREATE'),
('Issue Stock', 'INVENTORY_STOCK_ISSUE', 'Issue stock from inventory', 'INVENTORY', 'UPDATE'),

-- Purchase Order Permissions (missing from V3)
('View Purchase Orders', 'INVENTORY_PO_VIEW', 'View purchase orders', 'INVENTORY', 'VIEW'),
('Create Purchase Orders', 'INVENTORY_PO_CREATE', 'Create new purchase orders', 'INVENTORY', 'CREATE'),
('Update Purchase Orders', 'INVENTORY_PO_UPDATE', 'Update existing purchase orders', 'INVENTORY', 'UPDATE'),
('Approve Purchase Orders', 'INVENTORY_PO_APPROVE', 'Approve purchase orders', 'INVENTORY', 'APPROVE'),
('Cancel Purchase Orders', 'INVENTORY_PO_CANCEL', 'Cancel purchase orders', 'INVENTORY', 'DELETE'),

-- Receiving Permissions (missing from V3)
('View Receiving Orders', 'INVENTORY_RECEIVING_VIEW', 'View receiving orders', 'INVENTORY', 'VIEW'),
('Confirm Receiving', 'INVENTORY_RECEIVING_CONFIRM', 'Confirm goods received', 'INVENTORY', 'UPDATE'),

-- Inventory Count Permissions (missing from V3)
('View Inventory Counts', 'INVENTORY_COUNT_VIEW', 'View inventory count sessions', 'INVENTORY', 'VIEW'),
('Perform Inventory Count', 'INVENTORY_COUNT_PERFORM', 'Perform physical inventory counts', 'INVENTORY', 'UPDATE'),

-- Location Permissions (missing from V3)
('Manage Locations', 'INVENTORY_LOCATION_MANAGE', 'Create, update, delete warehouse locations', 'INVENTORY', 'MANAGE'),

-- Vendor Permissions (missing from V3)
('View Vendors', 'INVENTORY_VENDOR_VIEW', 'View vendor information', 'INVENTORY', 'VIEW'),
('Manage Vendors', 'INVENTORY_VENDOR_MANAGE', 'Create, update, delete vendors', 'INVENTORY', 'MANAGE')
ON CONFLICT (code) DO NOTHING;

-- ===========================================
-- Update SUPER_ADMIN Role (gets all new permissions)
-- ===========================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN'
AND p.code IN (
    'INVENTORY_STOCK_VIEW', 'INVENTORY_STOCK_RECEIVE', 'INVENTORY_STOCK_ISSUE',
    'INVENTORY_PO_VIEW', 'INVENTORY_PO_CREATE', 'INVENTORY_PO_UPDATE', 'INVENTORY_PO_APPROVE', 'INVENTORY_PO_CANCEL',
    'INVENTORY_RECEIVING_VIEW', 'INVENTORY_RECEIVING_CONFIRM',
    'INVENTORY_COUNT_VIEW', 'INVENTORY_COUNT_PERFORM',
    'INVENTORY_LOCATION_MANAGE',
    'INVENTORY_VENDOR_VIEW', 'INVENTORY_VENDOR_MANAGE'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- ===========================================
-- Update ADMIN Role (gets all new permissions except system-critical)
-- ===========================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN'
AND p.code IN (
    'INVENTORY_STOCK_VIEW', 'INVENTORY_STOCK_RECEIVE', 'INVENTORY_STOCK_ISSUE',
    'INVENTORY_PO_VIEW', 'INVENTORY_PO_CREATE', 'INVENTORY_PO_UPDATE', 'INVENTORY_PO_APPROVE', 'INVENTORY_PO_CANCEL',
    'INVENTORY_RECEIVING_VIEW', 'INVENTORY_RECEIVING_CONFIRM',
    'INVENTORY_COUNT_VIEW', 'INVENTORY_COUNT_PERFORM',
    'INVENTORY_LOCATION_MANAGE',
    'INVENTORY_VENDOR_VIEW', 'INVENTORY_VENDOR_MANAGE'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- ===========================================
-- Update INVENTORY_MANAGER Role (full inventory access)
-- ===========================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'INVENTORY_MANAGER'
AND p.code IN (
    'INVENTORY_STOCK_VIEW', 'INVENTORY_STOCK_RECEIVE', 'INVENTORY_STOCK_ISSUE',
    'INVENTORY_PO_VIEW', 'INVENTORY_PO_CREATE', 'INVENTORY_PO_UPDATE', 'INVENTORY_PO_APPROVE', 'INVENTORY_PO_CANCEL',
    'INVENTORY_RECEIVING_VIEW', 'INVENTORY_RECEIVING_CONFIRM',
    'INVENTORY_COUNT_VIEW', 'INVENTORY_COUNT_PERFORM',
    'INVENTORY_LOCATION_MANAGE',
    'INVENTORY_VENDOR_VIEW', 'INVENTORY_VENDOR_MANAGE'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- ===========================================
-- Update STORE_MANAGER Role (POS + inventory view + stock operations)
-- ===========================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'STORE_MANAGER'
AND p.code IN (
    'INVENTORY_STOCK_VIEW', 'INVENTORY_STOCK_RECEIVE', 'INVENTORY_STOCK_ISSUE',
    'INVENTORY_PO_VIEW',
    'INVENTORY_RECEIVING_VIEW', 'INVENTORY_RECEIVING_CONFIRM',
    'INVENTORY_COUNT_VIEW', 'INVENTORY_COUNT_PERFORM',
    'INVENTORY_VENDOR_VIEW'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- ===========================================
-- Update WAREHOUSE_STAFF Role (inventory operations)
-- ===========================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'WAREHOUSE_STAFF'
AND p.code IN (
    'INVENTORY_STOCK_VIEW', 'INVENTORY_STOCK_RECEIVE', 'INVENTORY_STOCK_ISSUE',
    'INVENTORY_PO_VIEW',
    'INVENTORY_RECEIVING_VIEW', 'INVENTORY_RECEIVING_CONFIRM',
    'INVENTORY_COUNT_VIEW', 'INVENTORY_COUNT_PERFORM',
    'INVENTORY_VENDOR_VIEW'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- ===========================================
-- Update VIEWER Role (all VIEW permissions)
-- ===========================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VIEWER'
AND p.code IN (
    'INVENTORY_STOCK_VIEW',
    'INVENTORY_PO_VIEW',
    'INVENTORY_RECEIVING_VIEW',
    'INVENTORY_COUNT_VIEW',
    'INVENTORY_VENDOR_VIEW'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- ===========================================
-- Update CASHIER Role (basic stock view)
-- ===========================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CASHIER'
AND p.code IN (
    'INVENTORY_STOCK_VIEW'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
