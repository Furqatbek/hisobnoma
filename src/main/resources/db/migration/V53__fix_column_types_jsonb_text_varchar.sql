-- V53: Fix column type mismatches between Flyway migrations and Hibernate entity definitions
-- JSONB -> TEXT for columns mapped as columnDefinition="TEXT" in entities
-- TEXT -> VARCHAR(N) for columns mapped with @Column(length=N) in entities

-- V20: mobile_alerts.data JSONB -> TEXT
ALTER TABLE mobile_alerts ALTER COLUMN data TYPE TEXT;

-- V21: report_definitions JSONB -> TEXT
ALTER TABLE report_definitions ALTER COLUMN parameter_schema TYPE TEXT;
ALTER TABLE report_definitions ALTER COLUMN default_parameters TYPE TEXT;

-- V21: report_schedules JSONB -> TEXT
ALTER TABLE report_schedules ALTER COLUMN parameters TYPE TEXT;

-- V21: report_executions JSONB -> TEXT
ALTER TABLE report_executions ALTER COLUMN parameters TYPE TEXT;

-- V16: price_lists TEXT -> VARCHAR to match entity @Column(length=N)
ALTER TABLE price_lists ALTER COLUMN description TYPE VARCHAR(500);
ALTER TABLE price_lists ALTER COLUMN notes TYPE VARCHAR(1000);

-- V16: price_list_items TEXT -> VARCHAR
ALTER TABLE price_list_items ALTER COLUMN notes TYPE VARCHAR(500);

-- V16: promotions TEXT -> VARCHAR
ALTER TABLE promotions ALTER COLUMN description TYPE VARCHAR(1000);
ALTER TABLE promotions ALTER COLUMN notes TYPE VARCHAR(1000);

-- V16: promotion_conditions TEXT -> VARCHAR
ALTER TABLE promotion_conditions ALTER COLUMN product_ids TYPE VARCHAR(2000);
ALTER TABLE promotion_conditions ALTER COLUMN category_ids TYPE VARCHAR(1000);
ALTER TABLE promotion_conditions ALTER COLUMN brand_ids TYPE VARCHAR(1000);
ALTER TABLE promotion_conditions ALTER COLUMN notes TYPE VARCHAR(500);

-- V16: promotion_actions TEXT -> VARCHAR
ALTER TABLE promotion_actions ALTER COLUMN target_product_ids TYPE VARCHAR(2000);
ALTER TABLE promotion_actions ALTER COLUMN target_category_ids TYPE VARCHAR(1000);
ALTER TABLE promotion_actions ALTER COLUMN notes TYPE VARCHAR(500);

-- V16: coupons TEXT -> VARCHAR
ALTER TABLE coupons ALTER COLUMN description TYPE VARCHAR(200);
ALTER TABLE coupons ALTER COLUMN notes TYPE VARCHAR(500);

-- V16: coupon_redemptions TEXT -> VARCHAR
ALTER TABLE coupon_redemptions ALTER COLUMN reversal_reason TYPE VARCHAR(500);
ALTER TABLE coupon_redemptions ALTER COLUMN notes TYPE VARCHAR(500);
