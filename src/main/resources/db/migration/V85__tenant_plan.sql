-- Subscription tier on the tenant (self-service upgrade/downgrade; billing later).
-- Existing tenants keep their current maxUsers/maxLocations until they change plan.
ALTER TABLE tenants ADD COLUMN plan VARCHAR(20) NOT NULL DEFAULT 'STARTER';
