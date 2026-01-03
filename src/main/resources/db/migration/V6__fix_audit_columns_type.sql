-- V6__fix_audit_columns_type.sql
-- Fix type mismatch: created_by and updated_by should be BIGINT (user ID) not VARCHAR

-- Fix users table
ALTER TABLE users ALTER COLUMN created_by TYPE BIGINT USING created_by::BIGINT;
ALTER TABLE users ALTER COLUMN updated_by TYPE BIGINT USING updated_by::BIGINT;

-- Fix roles table
ALTER TABLE roles ALTER COLUMN created_by TYPE BIGINT USING created_by::BIGINT;
ALTER TABLE roles ALTER COLUMN updated_by TYPE BIGINT USING updated_by::BIGINT;

-- Add foreign key constraints (optional, for referential integrity)
-- These are commented out as they may not be needed for all use cases
-- ALTER TABLE users ADD CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users(id);
-- ALTER TABLE users ADD CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);
-- ALTER TABLE roles ADD CONSTRAINT fk_roles_created_by FOREIGN KEY (created_by) REFERENCES users(id);
-- ALTER TABLE roles ADD CONSTRAINT fk_roles_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);
