-- Delivery module permissions
INSERT INTO permissions (name, code, description, module, action) VALUES
('View Delivery Region', 'DELIVERY_REGION_READ', 'View delivery regions', 'DELIVERY', 'READ'),
('Create Delivery Region', 'DELIVERY_REGION_CREATE', 'Create new delivery regions', 'DELIVERY', 'CREATE'),
('Update Delivery Region', 'DELIVERY_REGION_UPDATE', 'Update delivery regions', 'DELIVERY', 'UPDATE'),
('Delete Delivery Region', 'DELIVERY_REGION_DELETE', 'Delete delivery regions', 'DELIVERY', 'DELETE'),
('View Delivery Village', 'DELIVERY_VILLAGE_READ', 'View delivery villages', 'DELIVERY', 'READ'),
('Create Delivery Village', 'DELIVERY_VILLAGE_CREATE', 'Create new delivery villages', 'DELIVERY', 'CREATE'),
('Update Delivery Village', 'DELIVERY_VILLAGE_UPDATE', 'Update delivery villages', 'DELIVERY', 'UPDATE'),
('Delete Delivery Village', 'DELIVERY_VILLAGE_DELETE', 'Delete delivery villages', 'DELIVERY', 'DELETE')
ON CONFLICT (code) DO NOTHING;

-- SUPER_ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND p.code LIKE 'DELIVERY_%'
ON CONFLICT DO NOTHING;

-- ADMIN gets all delivery permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code LIKE 'DELIVERY_%'
ON CONFLICT DO NOTHING;

-- STORE_MANAGER gets read, create, update (no delete)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'STORE_MANAGER' AND p.code IN (
    'DELIVERY_REGION_READ', 'DELIVERY_REGION_CREATE', 'DELIVERY_REGION_UPDATE',
    'DELIVERY_VILLAGE_READ', 'DELIVERY_VILLAGE_CREATE', 'DELIVERY_VILLAGE_UPDATE'
)
ON CONFLICT DO NOTHING;

-- CASHIER gets read-only access for POS delivery address selection
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CASHIER' AND p.code IN (
    'DELIVERY_REGION_READ', 'DELIVERY_VILLAGE_READ'
)
ON CONFLICT DO NOTHING;

-- VIEWER gets read-only access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VIEWER' AND p.code IN (
    'DELIVERY_REGION_READ', 'DELIVERY_VILLAGE_READ'
)
ON CONFLICT DO NOTHING;
