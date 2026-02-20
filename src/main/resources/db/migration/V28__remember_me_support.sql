-- Add remembered flag to refresh_tokens for "remember me" functionality
ALTER TABLE refresh_tokens ADD COLUMN remembered BOOLEAN NOT NULL DEFAULT FALSE;
