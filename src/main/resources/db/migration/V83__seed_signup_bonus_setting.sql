-- V83: Seed the one-time signup (welcome) bonus setting for every tenant. 0 = off.
-- Credited to a customer's loyalty wallet on their FIRST successful OTP sign-in when
-- loyalty.enabled is true. Idempotent per tenant.
INSERT INTO tenant_settings (tenant_id, setting_key, setting_value, description, category, created_at, updated_at)
SELECT t.id, 'loyalty.signup_bonus', '0',
       'Биринчи рўйхатдан ўтишда бериладиган бонус (балл, 0 = ўчирилган)',
       'loyalty', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM tenant_settings ts
    WHERE ts.tenant_id = t.id AND ts.setting_key = 'loyalty.signup_bonus'
);
