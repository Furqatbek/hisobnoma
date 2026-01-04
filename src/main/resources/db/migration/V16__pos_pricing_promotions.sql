-- POS Module: Pricing & Promotions
-- This migration creates tables for price lists, promotions, and coupons

-- =====================================================
-- PRICE LISTS
-- =====================================================

-- Price Lists table
CREATE TABLE IF NOT EXISTS price_lists (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    priority INTEGER NOT NULL DEFAULT 0,
    default_markup_percent DECIMAL(10, 4),
    start_date DATE,
    end_date DATE,
    location_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_price_lists_code_tenant UNIQUE (code, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_price_lists_tenant ON price_lists(tenant_id);
CREATE INDEX IF NOT EXISTS idx_price_lists_type ON price_lists(type);
CREATE INDEX IF NOT EXISTS idx_price_lists_active ON price_lists(active);
CREATE INDEX IF NOT EXISTS idx_price_lists_dates ON price_lists(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_price_lists_location ON price_lists(location_id);

-- Price List Items table
CREATE TABLE IF NOT EXISTS price_list_items (
    id BIGSERIAL PRIMARY KEY,
    price_list_id BIGINT NOT NULL REFERENCES price_lists(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    variant_id BIGINT REFERENCES product_variants(id),
    price DECIMAL(19, 4),
    markup_percent DECIMAL(10, 4),
    min_quantity DECIMAL(19, 4),
    max_quantity DECIMAL(19, 4),
    start_date DATE,
    end_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_price_list_items_price_list ON price_list_items(price_list_id);
CREATE INDEX IF NOT EXISTS idx_price_list_items_product ON price_list_items(product_id);
CREATE INDEX IF NOT EXISTS idx_price_list_items_variant ON price_list_items(variant_id);
CREATE INDEX IF NOT EXISTS idx_price_list_items_dates ON price_list_items(start_date, end_date);

-- Customer Price Lists (customer-to-price-list assignments)
CREATE TABLE IF NOT EXISTS customer_price_lists (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    price_list_id BIGINT NOT NULL REFERENCES price_lists(id) ON DELETE CASCADE,
    priority INTEGER NOT NULL DEFAULT 0,
    start_date DATE,
    end_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_customer_price_lists UNIQUE (customer_id, price_list_id)
);

CREATE INDEX IF NOT EXISTS idx_customer_price_lists_customer ON customer_price_lists(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_price_lists_price_list ON customer_price_lists(price_list_id);
CREATE INDEX IF NOT EXISTS idx_customer_price_lists_dates ON customer_price_lists(start_date, end_date);

-- =====================================================
-- PROMOTIONS
-- =====================================================

-- Promotions table
CREATE TABLE IF NOT EXISTS promotions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    scope VARCHAR(50) NOT NULL DEFAULT 'ORDER',
    discount_value DECIMAL(19, 4),
    max_discount_amount DECIMAL(19, 4),

    -- BOGO fields
    buy_quantity INTEGER,
    get_quantity INTEGER,
    get_product_id BIGINT,

    -- Time restrictions
    start_date DATE,
    end_date DATE,
    start_time TIME,
    end_time TIME,
    valid_days_of_week VARCHAR(50),

    -- Location restrictions
    location_id BIGINT,

    -- Usage limits
    max_uses INTEGER,
    max_uses_per_customer INTEGER,
    current_uses INTEGER NOT NULL DEFAULT 0,

    -- Stacking rules
    stackable BOOLEAN NOT NULL DEFAULT FALSE,
    priority INTEGER NOT NULL DEFAULT 0,
    exclusive_group VARCHAR(100),

    -- Customer restrictions
    customer_group VARCHAR(100),
    minimum_customer_orders INTEGER,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_promotions_code_tenant UNIQUE (code, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_promotions_tenant ON promotions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_promotions_type ON promotions(type);
CREATE INDEX IF NOT EXISTS idx_promotions_active ON promotions(active);
CREATE INDEX IF NOT EXISTS idx_promotions_dates ON promotions(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_promotions_location ON promotions(location_id);

-- Promotion Conditions table
CREATE TABLE IF NOT EXISTS promotion_conditions (
    id BIGSERIAL PRIMARY KEY,
    promotion_id BIGINT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    condition_type VARCHAR(50) NOT NULL,
    operator VARCHAR(20),
    value VARCHAR(500),
    product_ids TEXT,
    category_ids TEXT,
    brand_ids TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_promotion_conditions_promotion ON promotion_conditions(promotion_id);
CREATE INDEX IF NOT EXISTS idx_promotion_conditions_type ON promotion_conditions(condition_type);

-- Promotion Actions table
CREATE TABLE IF NOT EXISTS promotion_actions (
    id BIGSERIAL PRIMARY KEY,
    promotion_id BIGINT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL,
    discount_type VARCHAR(50),
    discount_value DECIMAL(19, 4),
    max_discount_amount DECIMAL(19, 4),
    target_product_ids TEXT,
    target_category_ids TEXT,
    free_product_id BIGINT,
    free_quantity INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_promotion_actions_promotion ON promotion_actions(promotion_id);
CREATE INDEX IF NOT EXISTS idx_promotion_actions_type ON promotion_actions(action_type);

-- =====================================================
-- COUPONS
-- =====================================================

-- Coupons table
CREATE TABLE IF NOT EXISTS coupons (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    code VARCHAR(50) NOT NULL,
    promotion_id BIGINT NOT NULL REFERENCES promotions(id),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date DATE,
    end_date DATE,
    max_uses INTEGER,
    max_uses_per_customer INTEGER,
    current_uses INTEGER NOT NULL DEFAULT 0,
    minimum_order_amount DECIMAL(19, 4),
    restricted_to_customer_id BIGINT,
    first_used_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_coupons_code_tenant UNIQUE (code, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_coupons_tenant ON coupons(tenant_id);
CREATE INDEX IF NOT EXISTS idx_coupons_promotion ON coupons(promotion_id);
CREATE INDEX IF NOT EXISTS idx_coupons_status ON coupons(status);
CREATE INDEX IF NOT EXISTS idx_coupons_dates ON coupons(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_coupons_customer ON coupons(restricted_to_customer_id);

-- Coupon Redemptions table
CREATE TABLE IF NOT EXISTS coupon_redemptions (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL REFERENCES coupons(id),
    customer_id BIGINT,
    order_id BIGINT,
    discount_applied DECIMAL(19, 4) NOT NULL,
    redeemed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_coupon_redemptions_coupon ON coupon_redemptions(coupon_id);
CREATE INDEX IF NOT EXISTS idx_coupon_redemptions_customer ON coupon_redemptions(customer_id);
CREATE INDEX IF NOT EXISTS idx_coupon_redemptions_order ON coupon_redemptions(order_id);
CREATE INDEX IF NOT EXISTS idx_coupon_redemptions_date ON coupon_redemptions(redeemed_at);

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON TABLE price_lists IS 'Price lists for tiered and customer-specific pricing';
COMMENT ON TABLE price_list_items IS 'Individual product pricing within a price list';
COMMENT ON TABLE customer_price_lists IS 'Customer-to-price-list assignments';
COMMENT ON TABLE promotions IS 'Promotional campaigns and discount rules';
COMMENT ON TABLE promotion_conditions IS 'Conditions that must be met for promotion to apply';
COMMENT ON TABLE promotion_actions IS 'Actions/discounts applied when promotion conditions are met';
COMMENT ON TABLE coupons IS 'Coupon codes linked to promotions';
COMMENT ON TABLE coupon_redemptions IS 'History of coupon usage';

COMMENT ON COLUMN promotions.type IS 'PERCENTAGE_OFF, FIXED_AMOUNT_OFF, BUY_X_GET_Y, BUNDLE, FREE_ITEM, TIERED_DISCOUNT, SPEND_X_GET_Y';
COMMENT ON COLUMN promotions.scope IS 'ORDER, LINE_ITEM, SHIPPING, CATEGORY, PRODUCT';
COMMENT ON COLUMN promotion_conditions.condition_type IS 'MINIMUM_PURCHASE, MINIMUM_QUANTITY, SPECIFIC_PRODUCTS, CATEGORY, BRAND, CUSTOMER_GROUP, etc.';
COMMENT ON COLUMN coupons.status IS 'ACTIVE, INACTIVE, EXPIRED, DEPLETED, CANCELLED';
