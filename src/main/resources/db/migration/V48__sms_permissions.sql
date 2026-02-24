-- SMS module permissions
INSERT INTO permissions (name, code, description, module, action)
VALUES
    ('SMS yuborish', 'SMS_SEND', 'SMS xabar yuborish', 'SMS', 'CREATE'),
    ('SMS ko''rish', 'SMS_VIEW', 'SMS tarix va balansni ko''rish', 'SMS', 'READ')
ON CONFLICT (code) DO NOTHING;

-- Grant SMS permissions to ADMIN and SUPER_ADMIN roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code IN ('SMS_SEND', 'SMS_VIEW')
ON CONFLICT DO NOTHING;
