-- SMS OTP login codes for the agent mobile app (see DistributionAgentOtp).
CREATE TABLE distribution_agent_otps (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL,
    phone       VARCHAR(20) NOT NULL,
    code_hash   VARCHAR(64) NOT NULL,
    salt        VARCHAR(32) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    attempts    INTEGER NOT NULL DEFAULT 0,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT,
    version     BIGINT DEFAULT 0
);

CREATE INDEX idx_dist_agent_otp_phone ON distribution_agent_otps (tenant_id, phone, created_at);
