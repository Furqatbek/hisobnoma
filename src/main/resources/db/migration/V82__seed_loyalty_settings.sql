-- V82: Seed loyalty & referral program settings for every tenant (idempotent).
--
-- The loyalty/referral engines read these tenant-setting keys; nothing seeded them, so the
-- programs were silently OFF and invisible until an admin hand-typed the exact keys into the
-- generic settings editor. Seed them disabled with sensible values so the new typed
-- "Loyalty settings" form shows real state and enabling is a toggle. Behavior-neutral:
-- enabled=false keeps both programs off until a staff decision.

INSERT INTO tenant_settings (tenant_id, setting_key, setting_value, description, category, created_at, updated_at)
SELECT t.id, s.key, s.value, s.descr, 'loyalty', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
CROSS JOIN (VALUES
    ('loyalty.enabled',                    'false', 'Кешбэк дастури ёқилганми'),
    ('loyalty.earn_percent',               '1',     'Харид суммасидан неча % балл берилади'),
    ('loyalty.expiry_days',                '180',   'Балл амал қилиш муддати (кун, 0 = муддатсиз)'),
    ('loyalty.min_redeem',                 '5000',  'Сарфлаш учун минимал балл'),
    ('loyalty.max_redeem_percent_of_order','50',    'Буюртманинг неча % игача балл билан тўлаш мумкин'),
    ('referral.enabled',                   'false', 'Реферал дастури ёқилганми'),
    ('referral.reward_referrer',           '10000', 'Таклиф қилган мижоз мукофоти (балл)'),
    ('referral.reward_referred',           '5000',  'Янги мижоз мукофоти (балл)'),
    ('referral.monthly_cap',               '0',     'Ойлик мукофотлар чегараси (0 = чексиз)')
) AS s(key, value, descr)
WHERE NOT EXISTS (
    SELECT 1 FROM tenant_settings ts
    WHERE ts.tenant_id = t.id AND ts.setting_key = s.key
);
