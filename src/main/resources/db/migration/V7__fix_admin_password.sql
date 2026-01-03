-- V7__fix_admin_password.sql
-- Fix admin password hash (password: admin123)
-- Using {noop} prefix for development (plaintext password)
-- In production, use proper BCrypt hash

UPDATE users
SET password_hash = '{noop}admin123'
WHERE username = 'admin';
