-- Idempotency for /mobile/pos/quick-sale: a client-generated clientRequestId (UUID) maps to the
-- POS transaction it created, so a retried request returns the original sale instead of a duplicate.

CREATE TABLE mobile_quick_sale_idempotency (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT,
    client_request_id   VARCHAR(100) NOT NULL,
    transaction_id      BIGINT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    version             BIGINT DEFAULT 0,
    CONSTRAINT uk_mobile_quicksale_client_request UNIQUE (tenant_id, client_request_id)
);

CREATE INDEX idx_mobile_quicksale_tenant ON mobile_quick_sale_idempotency(tenant_id);
