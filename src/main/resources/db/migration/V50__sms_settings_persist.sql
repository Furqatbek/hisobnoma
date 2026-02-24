-- Seed SMS configuration into system_settings
INSERT INTO system_settings (setting_key, setting_value, default_value, description, category, value_type, is_sensitive, sort_order)
VALUES
    ('sms.enabled', 'false', 'false', 'SMS xizmatini yoqish/o''chirish', 'SMS', 'BOOLEAN', false, 1),
    ('sms.api_token', NULL, NULL, 'DevSMS.uz API tokeni', 'SMS', 'STRING', true, 2),
    ('sms.sender_id', '4546', '4546', 'SMS yuboruvchi identifikatori', 'SMS', 'STRING', false, 3),
    ('sms.base_url', 'https://devsms.uz/api', 'https://devsms.uz/api', 'DevSMS API bazaviy URL', 'SMS', 'STRING', false, 4)
ON CONFLICT (setting_key) DO NOTHING;
