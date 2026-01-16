-- V23__fix_admin_role_permissions.sql
-- Fix ADMIN role to have all permissions except system settings
-- The ADMIN role was missing permissions added in later migrations

-- First, remove existing ADMIN permissions to avoid duplicates
DELETE FROM role_permissions
WHERE role_id = (SELECT id FROM roles WHERE code = 'ADMIN');

-- Re-assign all permissions to ADMIN except system-level ones
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
  AND p.code NOT IN (
    'ADMIN_SETTINGS_MANAGE',
    'ADMIN_TENANT_MANAGE'
  );
