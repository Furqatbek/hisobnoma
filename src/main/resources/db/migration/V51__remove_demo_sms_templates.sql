-- Remove demo SMS templates seeded in V49
DELETE FROM sms_templates WHERE code IN ('DEBT_REMINDER', 'PAYMENT_CONFIRM', 'ORDER_READY') AND tenant_id = 1;
