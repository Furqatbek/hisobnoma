-- V5__tenants_table.sql
-- Tenants table for multi-tenancy support

CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    contact_email VARCHAR(100),
    contact_phone VARCHAR(20),
    address VARCHAR(255),
    timezone VARCHAR(100) DEFAULT 'UTC',
    currency VARCHAR(10) DEFAULT 'USD',
    locale VARCHAR(10) DEFAULT 'en',
    settings TEXT,
    subscription_expires_at TIMESTAMP WITH TIME ZONE,
    max_users INT NOT NULL DEFAULT 10,
    max_locations INT NOT NULL DEFAULT 5,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_tenants_code ON tenants(code);
CREATE INDEX idx_tenants_active ON tenants(active);

-- Add foreign key constraints for tenant_id
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE roles ADD CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- Insert default tenant
INSERT INTO tenants (name, code, description, active, timezone, currency, locale, max_users, max_locations)
VALUES ('Default Tenant', 'DEFAULT', 'Default system tenant', TRUE, 'UTC', 'USD', 'en', 100, 50);

-- Update existing records to use default tenant
UPDATE users SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE roles SET tenant_id = 1 WHERE tenant_id IS NULL AND system_role = FALSE;
