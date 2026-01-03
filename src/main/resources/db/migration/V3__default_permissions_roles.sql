-- V3__default_permissions_roles.sql
-- Default permissions and roles

-- Insert all permissions
INSERT INTO permissions (name, code, description, module, action) VALUES
-- Inventory Permissions
('Create Product', 'INVENTORY_PRODUCT_CREATE', 'Create new products', 'INVENTORY', 'CREATE'),
('View Product', 'INVENTORY_PRODUCT_READ', 'View products', 'INVENTORY', 'READ'),
('Update Product', 'INVENTORY_PRODUCT_UPDATE', 'Update product details', 'INVENTORY', 'UPDATE'),
('Delete Product', 'INVENTORY_PRODUCT_DELETE', 'Delete products', 'INVENTORY', 'DELETE'),
('View Stock', 'INVENTORY_STOCK_READ', 'View stock levels', 'INVENTORY', 'READ'),
('Adjust Stock', 'INVENTORY_STOCK_ADJUST', 'Adjust stock quantities', 'INVENTORY', 'UPDATE'),
('Transfer Stock', 'INVENTORY_STOCK_TRANSFER', 'Transfer stock between locations', 'INVENTORY', 'UPDATE'),
('Create Receiving', 'INVENTORY_RECEIVING_CREATE', 'Create receiving orders', 'INVENTORY', 'CREATE'),
('Approve Receiving', 'INVENTORY_RECEIVING_APPROVE', 'Approve receiving orders', 'INVENTORY', 'APPROVE'),
('Create Stock Count', 'INVENTORY_COUNT_CREATE', 'Create stock counts', 'INVENTORY', 'CREATE'),
('Approve Stock Count', 'INVENTORY_COUNT_APPROVE', 'Approve stock counts', 'INVENTORY', 'APPROVE'),

-- Finance Permissions
('View General Ledger', 'FINANCE_GL_READ', 'View general ledger', 'FINANCE', 'READ'),
('Post to GL', 'FINANCE_GL_POST', 'Post entries to general ledger', 'FINANCE', 'CREATE'),
('Close Period', 'FINANCE_GL_CLOSE_PERIOD', 'Close accounting periods', 'FINANCE', 'APPROVE'),
('Create AP', 'FINANCE_AP_CREATE', 'Create accounts payable', 'FINANCE', 'CREATE'),
('Approve AP', 'FINANCE_AP_APPROVE', 'Approve accounts payable', 'FINANCE', 'APPROVE'),
('Pay AP', 'FINANCE_AP_PAY', 'Process payments', 'FINANCE', 'UPDATE'),
('Create AR', 'FINANCE_AR_CREATE', 'Create accounts receivable', 'FINANCE', 'CREATE'),
('Receive Payment', 'FINANCE_AR_RECEIVE_PAYMENT', 'Record payment receipts', 'FINANCE', 'UPDATE'),
('View Finance Reports', 'FINANCE_REPORTS_VIEW', 'View financial reports', 'FINANCE', 'VIEW'),
('Export Finance Reports', 'FINANCE_REPORTS_EXPORT', 'Export financial reports', 'FINANCE', 'EXPORT'),

-- POS Permissions
('Create Sale', 'POS_SALE_CREATE', 'Create POS sales', 'POS', 'CREATE'),
('Void Sale', 'POS_SALE_VOID', 'Void POS sales', 'POS', 'DELETE'),
('Refund Sale', 'POS_SALE_REFUND', 'Process refunds', 'POS', 'UPDATE'),
('Apply Discount', 'POS_DISCOUNT_APPLY', 'Apply standard discounts', 'POS', 'UPDATE'),
('Override Discount', 'POS_DISCOUNT_OVERRIDE', 'Override discount limits', 'POS', 'APPROVE'),
('Open Drawer', 'POS_DRAWER_OPEN', 'Open cash drawer', 'POS', 'UPDATE'),
('Close Drawer', 'POS_DRAWER_CLOSE', 'Close cash drawer', 'POS', 'UPDATE'),
('Reconcile Drawer', 'POS_DRAWER_RECONCILE', 'Reconcile cash drawer', 'POS', 'APPROVE'),
('View POS Reports', 'POS_REPORTS_VIEW', 'View POS reports', 'POS', 'VIEW'),

-- Admin Permissions
('Manage Users', 'ADMIN_USER_MANAGE', 'Create, update, delete users', 'ADMIN', 'MANAGE'),
('Manage Roles', 'ADMIN_ROLE_MANAGE', 'Create, update, delete roles', 'ADMIN', 'MANAGE'),
('Manage Permissions', 'ADMIN_PERMISSION_MANAGE', 'Assign permissions to roles', 'ADMIN', 'MANAGE'),
('Manage Tenants', 'ADMIN_TENANT_MANAGE', 'Manage tenant settings', 'ADMIN', 'MANAGE'),
('Manage Settings', 'ADMIN_SETTINGS_MANAGE', 'Manage system settings', 'ADMIN', 'MANAGE'),
('View Audit Logs', 'ADMIN_AUDIT_VIEW', 'View audit logs', 'ADMIN', 'VIEW'),
('Monitor System', 'ADMIN_SYSTEM_MONITOR', 'Monitor system health', 'ADMIN', 'VIEW'),

-- Report Permissions
('View Inventory Reports', 'REPORTS_INVENTORY_VIEW', 'View inventory reports', 'REPORTS', 'VIEW'),
('View Finance Reports', 'REPORTS_FINANCE_VIEW', 'View finance reports', 'REPORTS', 'VIEW'),
('View Sales Reports', 'REPORTS_SALES_VIEW', 'View sales reports', 'REPORTS', 'VIEW'),
('Export Reports', 'REPORTS_EXPORT', 'Export reports', 'REPORTS', 'EXPORT'),
('Schedule Reports', 'REPORTS_SCHEDULE', 'Schedule automated reports', 'REPORTS', 'MANAGE');

-- Insert default system roles (tenant_id = NULL for system roles)
INSERT INTO roles (name, code, description, system_role, tenant_id) VALUES
('Super Admin', 'SUPER_ADMIN', 'Full system access', TRUE, NULL),
('Admin', 'ADMIN', 'Administrative access except system settings', TRUE, NULL),
('Inventory Manager', 'INVENTORY_MANAGER', 'Full inventory management access', TRUE, NULL),
('Finance Manager', 'FINANCE_MANAGER', 'Full finance management access', TRUE, NULL),
('Accountant', 'ACCOUNTANT', 'Finance read and create access', TRUE, NULL),
('Store Manager', 'STORE_MANAGER', 'POS and inventory read access', TRUE, NULL),
('Cashier', 'CASHIER', 'Basic POS access', TRUE, NULL),
('Warehouse Staff', 'WAREHOUSE_STAFF', 'Inventory operations access', TRUE, NULL),
('Viewer', 'VIEWER', 'Read-only access', TRUE, NULL);

-- Assign permissions to SUPER_ADMIN (all permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.code = 'SUPER_ADMIN';

-- Assign permissions to ADMIN (all except system settings)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code NOT IN ('ADMIN_SETTINGS_MANAGE', 'ADMIN_TENANT_MANAGE');

-- Assign permissions to INVENTORY_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'INVENTORY_MANAGER' AND (p.module = 'INVENTORY' OR p.code = 'REPORTS_INVENTORY_VIEW');

-- Assign permissions to FINANCE_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'FINANCE_MANAGER' AND (p.module = 'FINANCE' OR p.code = 'REPORTS_FINANCE_VIEW');

-- Assign permissions to ACCOUNTANT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ACCOUNTANT' AND p.code IN (
    'FINANCE_GL_READ', 'FINANCE_GL_POST',
    'FINANCE_AP_CREATE', 'FINANCE_AR_CREATE', 'FINANCE_AR_RECEIVE_PAYMENT',
    'FINANCE_REPORTS_VIEW', 'REPORTS_FINANCE_VIEW'
);

-- Assign permissions to STORE_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'STORE_MANAGER' AND p.code IN (
    'POS_SALE_CREATE', 'POS_SALE_VOID', 'POS_SALE_REFUND',
    'POS_DISCOUNT_APPLY', 'POS_DISCOUNT_OVERRIDE',
    'POS_DRAWER_OPEN', 'POS_DRAWER_CLOSE', 'POS_DRAWER_RECONCILE',
    'POS_REPORTS_VIEW', 'REPORTS_SALES_VIEW',
    'INVENTORY_PRODUCT_READ', 'INVENTORY_STOCK_READ'
);

-- Assign permissions to CASHIER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CASHIER' AND p.code IN (
    'POS_SALE_CREATE', 'POS_DISCOUNT_APPLY',
    'POS_DRAWER_OPEN', 'POS_DRAWER_CLOSE',
    'INVENTORY_PRODUCT_READ', 'INVENTORY_STOCK_READ'
);

-- Assign permissions to WAREHOUSE_STAFF
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'WAREHOUSE_STAFF' AND p.code IN (
    'INVENTORY_PRODUCT_READ', 'INVENTORY_STOCK_READ',
    'INVENTORY_STOCK_ADJUST', 'INVENTORY_STOCK_TRANSFER',
    'INVENTORY_RECEIVING_CREATE', 'INVENTORY_COUNT_CREATE'
);

-- Assign permissions to VIEWER (all READ/VIEW permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VIEWER' AND p.action IN ('READ', 'VIEW');

-- Create default super admin user (password: admin123)
INSERT INTO users (username, phone, password_hash, first_name, last_name, enabled, phone_verified, tenant_id)
VALUES ('admin', '+998900000000', '{noop}admin123', 'System', 'Admin', TRUE, TRUE, NULL);

-- Assign SUPER_ADMIN role to default admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.code = 'SUPER_ADMIN';
