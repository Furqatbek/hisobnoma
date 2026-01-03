-- Initialize database for Hisobnoma Platform

-- Create additional databases for testing if needed
-- CREATE DATABASE hisobnoma_test;

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Grant privileges (for development)
GRANT ALL PRIVILEGES ON DATABASE hisobnoma_dev TO postgres;

-- Print success message
DO $$
BEGIN
    RAISE NOTICE 'Database initialization completed successfully!';
END $$;
