-- Ensure SUPER_ADMIN role has ALL permissions
-- This fixes 403 errors for super admins on newer endpoints (Finance GL, Journal Entries, HR, etc.)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- Also ensure ADMIN role has ALL permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;
