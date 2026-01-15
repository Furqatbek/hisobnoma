-- Default Units of Measure for all tenants
-- These are common UOMs used in retail/inventory management

-- Insert default UOMs for tenant 1 (default tenant)
-- Base units (no conversion needed)
INSERT INTO units_of_measure (tenant_id, code, name, symbol, description, is_base_unit, conversion_factor, active, created_at, updated_at, version)
VALUES
    (1, 'PCS', 'Piece', 'pcs', 'Individual piece or unit', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'KG', 'Kilogram', 'kg', 'Weight in kilograms', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'G', 'Gram', 'g', 'Weight in grams', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'L', 'Liter', 'L', 'Volume in liters', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'ML', 'Milliliter', 'ml', 'Volume in milliliters', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'M', 'Meter', 'm', 'Length in meters', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'CM', 'Centimeter', 'cm', 'Length in centimeters', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'BOX', 'Box', 'box', 'Box packaging', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'PACK', 'Pack', 'pack', 'Pack or package', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'SET', 'Set', 'set', 'Set of items', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'PAIR', 'Pair', 'pair', 'Pair of items', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'DOZEN', 'Dozen', 'dz', '12 pieces', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'ROLL', 'Roll', 'roll', 'Roll packaging', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'BTL', 'Bottle', 'btl', 'Bottle packaging', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'CAN', 'Can', 'can', 'Can packaging', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'BAG', 'Bag', 'bag', 'Bag packaging', true, 1.000000, true, NOW(), NOW(), 0),
    (1, 'UNIT', 'Unit', 'unit', 'Generic unit', true, 1.000000, true, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;
