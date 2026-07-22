-- Permission for the one-call tenant provisioning endpoint (platform operators only).
INSERT INTO permissions (name, code, description, module, action) VALUES
('Provision Tenants', 'TENANT_PROVISION', 'Onboard new tenants (tenant + accounts + admin + fiscal year)', 'ADMIN', 'MANAGE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND p.code = 'TENANT_PROVISION'
ON CONFLICT DO NOTHING;
