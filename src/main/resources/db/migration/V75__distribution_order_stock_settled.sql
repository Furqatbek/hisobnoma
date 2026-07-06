-- Distribution orders: track whether stock movements (reserve/release/deduct) actually
-- succeeded. Best-effort stock ops on confirm/deliver no longer fail silently — a failure
-- flips this to FALSE so staff can query and reconcile (billed-but-not-decremented) orders.

ALTER TABLE distribution_orders ADD COLUMN stock_settled BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_dist_orders_stock_unsettled
    ON distribution_orders(tenant_id) WHERE stock_settled = FALSE;
