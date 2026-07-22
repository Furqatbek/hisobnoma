-- Telegram bot credentials are PLATFORM-level: one bot serves every tenant, so
-- the token lives in system_settings (previously written to tenant_settings,
-- where one tenant's save clobbered the shared bot for everyone).
INSERT INTO system_settings (setting_key, setting_value, default_value, description, category, value_type, is_sensitive, sort_order)
VALUES
    ('telegram.enabled', 'false', 'false', 'Telegram botni yoqish/o''chirish', 'TELEGRAM', 'BOOLEAN', false, 1),
    ('telegram.bot_token', NULL, NULL, 'Telegram bot tokeni (BotFather)', 'TELEGRAM', 'STRING', true, 2),
    ('telegram.bot_username', 'hisobnoma_bot', 'hisobnoma_bot', 'Telegram bot username', 'TELEGRAM', 'STRING', false, 3)
ON CONFLICT (setting_key) DO NOTHING;
