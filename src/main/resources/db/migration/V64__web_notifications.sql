CREATE TABLE web_notifications (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES tenants(id),
    web_customer_id BIGINT       NOT NULL REFERENCES web_customers(id),
    title           VARCHAR(200) NOT NULL,
    body            VARCHAR(1000) NOT NULL,
    type            VARCHAR(30)  NOT NULL DEFAULT 'GENERAL',
    reference_type  VARCHAR(30),
    reference_id    VARCHAR(50),
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP,
    version         BIGINT       DEFAULT 0
);

CREATE INDEX idx_web_notifications_customer ON web_notifications (tenant_id, web_customer_id, is_read);
CREATE INDEX idx_web_notifications_created  ON web_notifications (tenant_id, web_customer_id, created_at DESC);
