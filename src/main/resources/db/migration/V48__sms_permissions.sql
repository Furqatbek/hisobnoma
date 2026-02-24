-- SMS module permissions
INSERT INTO permissions (code, name, description, module, created_at, updated_at)
VALUES
    ('SMS_SEND', 'SMS yuborish', 'SMS xabar yuborish', 'SMS', NOW(), NOW()),
    ('SMS_VIEW', 'SMS ko''rish', 'SMS tarix va balansni ko''rish', 'SMS', NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- Grant SMS permissions to ADMIN and SUPER_ADMIN roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code IN ('SMS_SEND', 'SMS_VIEW')
ON CONFLICT DO NOTHING;
