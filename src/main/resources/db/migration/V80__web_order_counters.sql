-- V80: Per-tenant order-number counter for the online shop.
--
-- Checkout previously derived the next "WO-%06d" from count(*)+1 inside the open transaction:
-- two concurrent checkouts on one tenant computed the SAME number, the unique index
-- uk_web_orders_number killed one at commit, and that customer got a 500. Numbers are now
-- allocated from this counter row under a pessimistic lock in a short separate transaction,
-- which serializes allocation per tenant (gaps on rollback are acceptable).
--
-- Seeded from the highest existing numeric suffix so new allocations continue the sequence.

CREATE TABLE web_order_counters (
    tenant_id   BIGINT PRIMARY KEY,
    next_number BIGINT NOT NULL
);

INSERT INTO web_order_counters (tenant_id, next_number)
SELECT tenant_id,
       COALESCE(MAX(CAST(SUBSTRING(order_number FROM 4) AS BIGINT)), 0) + 1
FROM web_orders
WHERE order_number ~ '^WO-[0-9]+$'
GROUP BY tenant_id;
