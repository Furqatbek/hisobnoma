-- Seed fiscal year 2026 with monthly periods for all tenants
-- Required for GL journal entry posting (salary payments, advances, etc.)

-- Create fiscal year 2026 for each tenant that doesn't have one
INSERT INTO fiscal_years (year, name, start_date, end_date, status, is_current, is_closed, tenant_id, created_at, updated_at, version)
SELECT 2026, '2026 Moliyaviy yil', '2026-01-01', '2026-12-31', 'OPEN', TRUE, FALSE, t.id, NOW(), NOW(), 0
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM fiscal_years fy WHERE fy.year = 2026 AND fy.tenant_id = t.id
);

-- Generate 12 monthly periods for the newly created fiscal year
INSERT INTO fiscal_periods (fiscal_year_id, period_number, name, start_date, end_date, status, is_adjustment_period, tenant_id, created_at, updated_at, version)
SELECT fy.id, 1, 'January', '2026-01-01', '2026-01-31', 'OPEN', FALSE, fy.tenant_id, NOW(), NOW(), 0
FROM fiscal_years fy WHERE fy.year = 2026 AND NOT EXISTS (
    SELECT 1 FROM fiscal_periods fp WHERE fp.fiscal_year_id = fy.id AND fp.period_number = 1
);

INSERT INTO fiscal_periods (fiscal_year_id, period_number, name, start_date, end_date, status, is_adjustment_period, tenant_id, created_at, updated_at, version)
SELECT fy.id, 2, 'February', '2026-02-01', '2026-02-28', 'OPEN', FALSE, fy.tenant_id, NOW(), NOW(), 0
FROM fiscal_years fy WHERE fy.year = 2026 AND NOT EXISTS (
    SELECT 1 FROM fiscal_periods fp WHERE fp.fiscal_year_id = fy.id AND fp.period_number = 2
);

INSERT INTO fiscal_periods (fiscal_year_id, period_number, name, start_date, end_date, status, is_adjustment_period, tenant_id, created_at, updated_at, version)
SELECT fy.id, 3, 'March', '2026-03-01', '2026-03-31', 'OPEN', FALSE, fy.tenant_id, NOW(), NOW(), 0
FROM fiscal_years fy WHERE fy.year = 2026 AND NOT EXISTS (
    SELECT 1 FROM fiscal_periods fp WHERE fp.fiscal_year_id = fy.id AND fp.period_number = 3
);

INSERT INTO fiscal_periods (fiscal_year_id, period_number, name, start_date, end_date, status, is_adjustment_period, tenant_id, created_at, updated_at, version)
SELECT fy.id, 4, 'April', '2026-04-01', '2026-04-30', 'OPEN', FALSE, fy.tenant_id, NOW(), NOW(), 0
FROM fiscal_years fy WHERE fy.year = 2026 AND NOT EXISTS (
    SELECT 1 FROM fiscal_periods fp WHERE fp.fiscal_year_id = fy.id AND fp.period_number = 4
);

INSERT INTO fiscal_periods (fiscal_year_id, period_number, name, start_date, end_date, status, is_adjustment_period, tenant_id, created_at, updated_at, version)
SELECT fy.id, 5, 'May', '2026-05-01', '2026-05-31', 'OPEN', FALSE, fy.tenant_id, NOW(), NOW(), 0
FROM fiscal_years fy WHERE fy.year = 2026 AND NOT EXISTS (
    SELECT 1 FROM fiscal_periods fp WHERE fp.fiscal_year_id = fy.id AND fp.period_number = 5
);

INSERT INTO fiscal_periods (fiscal_year_id, period_number, name, start_date, end_date, status, is_adjustment_period, tenant_id, created_at, updated_at, version)
SELECT fy.id, 6, 'June', '2026-06-01', '2026-06-30', 'OPEN', FALSE, fy.tenant_id, NOW(), NOW(), 0
FROM fiscal_years fy WHERE fy.year = 2026 AND NOT EXISTS (
    SELECT 1 FROM fiscal_periods fp WHERE fp.fiscal_year_id = fy.id AND fp.period_number = 6
);

INSERT INTO fiscal_periods (fiscal_year_id, period_number, name, start_date, end_date, status, is_adjustment_period, tenant_id, created_at, updated_at, version)
SELECT fy.id, 7, 'July', '2026-07-01', '2026-07-31', 'OPEN', FALSE, fy.tenant_id, NOW(), NOW(), 0
FROM fiscal_years fy WHERE fy.year = 2026 AND NOT EXISTS (
    SELECT 1 FROM fiscal_periods fp WHERE fp.fiscal_year_id = fy.id AND fp.period_number = 7
);

INSERT INTO fiscal_periods (fiscal_year_id, period_number, name, start_date, end_date, status, is_adjustment_period, tenant_id, created_at, updated_at, version)
SELECT fy.id, 8, 'August', '2026-08-01', '2026-08-31', 'OPEN', FALSE, fy.tenant_id, NOW(), NOW(), 0
FROM fiscal_years fy WHERE fy.year = 2026 AND NOT EXISTS (
    SELECT 1 FROM fiscal_periods fp WHERE fp.fiscal_year_id = fy.id AND fp.period_number = 8
);

INSERT INTO fiscal_periods (fiscal_year_id, period_number, name, start_date, end_date, status, is_adjustment_period, tenant_id, created_at, updated_at, version)
SELECT fy.id, 9, 'September', '2026-09-01', '2026-09-30', 'OPEN', FALSE, fy.tenant_id, NOW(), NOW(), 0
FROM fiscal_years fy WHERE fy.year = 2026 AND NOT EXISTS (
    SELECT 1 FROM fiscal_periods fp WHERE fp.fiscal_year_id = fy.id AND fp.period_number = 9
);

INSERT INTO fiscal_periods (fiscal_year_id, period_number, name, start_date, end_date, status, is_adjustment_period, tenant_id, created_at, updated_at, version)
SELECT fy.id, 10, 'October', '2026-10-01', '2026-10-31', 'OPEN', FALSE, fy.tenant_id, NOW(), NOW(), 0
FROM fiscal_years fy WHERE fy.year = 2026 AND NOT EXISTS (
    SELECT 1 FROM fiscal_periods fp WHERE fp.fiscal_year_id = fy.id AND fp.period_number = 10
);

INSERT INTO fiscal_periods (fiscal_year_id, period_number, name, start_date, end_date, status, is_adjustment_period, tenant_id, created_at, updated_at, version)
SELECT fy.id, 11, 'November', '2026-11-01', '2026-11-30', 'OPEN', FALSE, fy.tenant_id, NOW(), NOW(), 0
FROM fiscal_years fy WHERE fy.year = 2026 AND NOT EXISTS (
    SELECT 1 FROM fiscal_periods fp WHERE fp.fiscal_year_id = fy.id AND fp.period_number = 11
);

INSERT INTO fiscal_periods (fiscal_year_id, period_number, name, start_date, end_date, status, is_adjustment_period, tenant_id, created_at, updated_at, version)
SELECT fy.id, 12, 'December', '2026-12-01', '2026-12-31', 'OPEN', FALSE, fy.tenant_id, NOW(), NOW(), 0
FROM fiscal_years fy WHERE fy.year = 2026 AND NOT EXISTS (
    SELECT 1 FROM fiscal_periods fp WHERE fp.fiscal_year_id = fy.id AND fp.period_number = 12
);
