-- SMS templates table
CREATE TABLE sms_templates (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    code            VARCHAR(50) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    template        VARCHAR(500) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    version         BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX idx_sms_template_code ON sms_templates (tenant_id, code);
CREATE INDEX idx_sms_template_tenant ON sms_templates (tenant_id);

-- Seed default templates
INSERT INTO sms_templates (tenant_id, code, name, template) VALUES
    (1, 'DEBT_REMINDER', 'Qarz eslatmasi', 'Hurmatli {name}, sizda {amount} so''m qarz mavjud. Iltimos, to''lovni amalga oshiring.'),
    (1, 'PAYMENT_CONFIRM', 'To''lov tasdiqi', 'Hurmatli {name}, {amount} so''m to''lovingiz qabul qilindi. Rahmat!'),
    (1, 'ORDER_READY', 'Buyurtma tayyor', 'Hurmatli {name}, #{orderId} raqamli buyurtmangiz tayyor. Qabul qilib olishingiz mumkin.');

-- Add SMS template management permission
INSERT INTO permissions (name, code, description, module, action)
VALUES ('SMS shablonlarni boshqarish', 'SMS_TEMPLATES_MANAGE', 'SMS shablonlarni yaratish va tahrirlash', 'SMS', 'UPDATE')
ON CONFLICT (code) DO NOTHING;

-- Grant to ADMIN and SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code = 'SMS_TEMPLATES_MANAGE'
ON CONFLICT DO NOTHING;
