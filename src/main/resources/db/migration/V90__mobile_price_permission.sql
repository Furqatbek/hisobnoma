-- Mobile permission for changing product prices from the admin app.
-- The endpoint accepts EITHER this mobile permission OR the underlying
-- INVENTORY_PRODUCT_UPDATE module permission (mirrors the other mobile endpoints).
INSERT INTO permissions (code, name, description, module, action, created_at, updated_at) VALUES
    ('MOBILE_PRICE_WRITE', 'Mobile Change Price', 'Change a product selling price from the mobile app', 'MOBILE', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Grant to the roles that manage pricing.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')
AND p.code = 'MOBILE_PRICE_WRITE'
ON CONFLICT DO NOTHING;
