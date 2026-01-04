-- V17__pos_pricing_promotions_permissions.sql
-- Permissions for POS Pricing & Promotions module

-- Insert pricing and promotions permissions
INSERT INTO permissions (name, code, description, module, action) VALUES
-- Price List Permissions
('View Price Lists', 'POS_PRICELIST_READ', 'View price lists', 'POS', 'READ'),
('Create Price List', 'POS_PRICELIST_CREATE', 'Create price lists', 'POS', 'CREATE'),
('Update Price List', 'POS_PRICELIST_UPDATE', 'Update price lists', 'POS', 'UPDATE'),
('Delete Price List', 'POS_PRICELIST_DELETE', 'Delete price lists', 'POS', 'DELETE'),
('Manage Price List Items', 'POS_PRICELIST_ITEMS_MANAGE', 'Manage price list items', 'POS', 'MANAGE'),
('Assign Customer Price Lists', 'POS_PRICELIST_ASSIGN', 'Assign price lists to customers', 'POS', 'UPDATE'),

-- Promotion Permissions
('View Promotions', 'POS_PROMOTION_READ', 'View promotions', 'POS', 'READ'),
('Create Promotion', 'POS_PROMOTION_CREATE', 'Create promotions', 'POS', 'CREATE'),
('Update Promotion', 'POS_PROMOTION_UPDATE', 'Update promotions', 'POS', 'UPDATE'),
('Delete Promotion', 'POS_PROMOTION_DELETE', 'Delete promotions', 'POS', 'DELETE'),
('Manage Promotion Conditions', 'POS_PROMOTION_CONDITIONS_MANAGE', 'Manage promotion conditions', 'POS', 'MANAGE'),
('Manage Promotion Actions', 'POS_PROMOTION_ACTIONS_MANAGE', 'Manage promotion actions', 'POS', 'MANAGE'),

-- Coupon Permissions
('View Coupons', 'POS_COUPON_READ', 'View coupons', 'POS', 'READ'),
('Create Coupon', 'POS_COUPON_CREATE', 'Create coupons', 'POS', 'CREATE'),
('Update Coupon', 'POS_COUPON_UPDATE', 'Update coupons', 'POS', 'UPDATE'),
('Delete Coupon', 'POS_COUPON_DELETE', 'Delete coupons', 'POS', 'DELETE'),
('Generate Bulk Coupons', 'POS_COUPON_GENERATE', 'Generate bulk coupons', 'POS', 'CREATE'),
('View Coupon Redemptions', 'POS_COUPON_REDEMPTIONS_VIEW', 'View coupon redemptions', 'POS', 'VIEW'),

-- Pricing Engine Permissions
('Calculate Prices', 'POS_PRICING_CALCULATE', 'Calculate prices for items', 'POS', 'READ'),
('Apply Coupons', 'POS_COUPON_APPLY', 'Apply coupons to orders', 'POS', 'UPDATE'),
('Record Coupon Redemption', 'POS_COUPON_REDEEM', 'Record coupon redemption', 'POS', 'CREATE')
ON CONFLICT (code) DO NOTHING;

-- Assign pricing permissions to SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN'
AND p.code LIKE 'POS_PRICELIST%' OR p.code LIKE 'POS_PROMOTION%' OR p.code LIKE 'POS_COUPON%' OR p.code LIKE 'POS_PRICING%'
ON CONFLICT DO NOTHING;

-- Assign pricing permissions to ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN'
AND (p.code LIKE 'POS_PRICELIST%' OR p.code LIKE 'POS_PROMOTION%' OR p.code LIKE 'POS_COUPON%' OR p.code LIKE 'POS_PRICING%')
ON CONFLICT DO NOTHING;

-- Assign read and apply permissions to STORE_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'STORE_MANAGER'
AND p.code IN (
    'POS_PRICELIST_READ', 'POS_PRICELIST_CREATE', 'POS_PRICELIST_UPDATE',
    'POS_PROMOTION_READ', 'POS_PROMOTION_CREATE', 'POS_PROMOTION_UPDATE',
    'POS_COUPON_READ', 'POS_COUPON_CREATE', 'POS_COUPON_UPDATE',
    'POS_PRICING_CALCULATE', 'POS_COUPON_APPLY', 'POS_COUPON_REDEEM',
    'POS_COUPON_REDEMPTIONS_VIEW'
)
ON CONFLICT DO NOTHING;

-- Assign basic pricing permissions to CASHIER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CASHIER'
AND p.code IN (
    'POS_PRICELIST_READ',
    'POS_PROMOTION_READ',
    'POS_COUPON_READ',
    'POS_PRICING_CALCULATE',
    'POS_COUPON_APPLY',
    'POS_COUPON_REDEEM'
)
ON CONFLICT DO NOTHING;
