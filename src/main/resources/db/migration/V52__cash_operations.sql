-- Individual cash in/out operation records for audit trail
CREATE TABLE pos_cash_operations (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    shift_id        BIGINT NOT NULL REFERENCES pos_shifts(id),
    operation_type  VARCHAR(20) NOT NULL,
    amount          NUMERIC(18, 4) NOT NULL,
    reason          VARCHAR(500),
    cashier_id      BIGINT NOT NULL,
    cashier_name    VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      BIGINT,
    updated_by      BIGINT,
    version         BIGINT DEFAULT 0
);

CREATE INDEX idx_cash_ops_tenant ON pos_cash_operations(tenant_id);
CREATE INDEX idx_cash_ops_shift ON pos_cash_operations(shift_id);
CREATE INDEX idx_cash_ops_created_at ON pos_cash_operations(created_at);
