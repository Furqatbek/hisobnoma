-- Phase 7: online card payments (Payme / Click / Uzum).
-- A payment row tracks one redirect-and-confirm attempt against a web order.

CREATE TABLE web_payments (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT       NOT NULL REFERENCES tenants(id),
    web_order_id       BIGINT       NOT NULL REFERENCES web_orders(id),
    order_number       VARCHAR(50)  NOT NULL,
    public_id          VARCHAR(40)  NOT NULL,
    provider           VARCHAR(10)  NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    amount             NUMERIC(18,4) NOT NULL,
    currency           VARCHAR(3)   NOT NULL DEFAULT 'UZS',
    provider_reference VARCHAR(100),
    payment_url        VARCHAR(1000),
    failure_reason     VARCHAR(500),
    paid_at            TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP WITH TIME ZONE,
    created_by         BIGINT,
    updated_by         BIGINT,
    version            BIGINT DEFAULT 0,
    CONSTRAINT uk_web_payments_public_id UNIQUE (public_id)
);

CREATE INDEX idx_web_payments_order        ON web_payments (tenant_id, web_order_id);
CREATE INDEX idx_web_payments_provider_ref ON web_payments (provider, provider_reference);
