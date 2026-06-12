-- Phase 5: push notifications + referrals

CREATE TABLE web_device_tokens (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES tenants(id),
    web_customer_id BIGINT       NOT NULL REFERENCES web_customers(id),
    token           VARCHAR(500) NOT NULL,
    platform        VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE,
    last_seen_at    TIMESTAMP WITH TIME ZONE DEFAULT now(),
    version         BIGINT DEFAULT 0,
    CONSTRAINT uk_web_device_tokens_token_tenant UNIQUE (tenant_id, token)
);

CREATE INDEX idx_web_device_tokens_customer ON web_device_tokens (tenant_id, web_customer_id);

-- Referral fields on web_customers
ALTER TABLE web_customers ADD COLUMN referral_code VARCHAR(12);
ALTER TABLE web_customers ADD COLUMN referred_by  BIGINT NULL REFERENCES web_customers(id);

CREATE UNIQUE INDEX uk_web_customers_referral_code ON web_customers (referral_code) WHERE referral_code IS NOT NULL;

-- Campaign channel (SMS / PUSH / PUSH_WITH_SMS_FALLBACK)
ALTER TABLE web_campaigns ADD COLUMN channel VARCHAR(30) NOT NULL DEFAULT 'SMS';
