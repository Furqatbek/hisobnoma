-- Distribution module — align distribution_orders with the planned data model:
-- originating visit/route linkage, pricing-basis snapshot, and delivery GPS.

ALTER TABLE distribution_orders ADD COLUMN visit_id BIGINT;
ALTER TABLE distribution_orders ADD COLUMN route_id BIGINT REFERENCES distribution_routes(id);
ALTER TABLE distribution_orders ADD COLUMN price_list_id BIGINT;
ALTER TABLE distribution_orders ADD COLUMN delivery_lat NUMERIC(10,7);
ALTER TABLE distribution_orders ADD COLUMN delivery_lng NUMERIC(10,7);

CREATE INDEX idx_dist_orders_visit ON distribution_orders(visit_id);
CREATE INDEX idx_dist_orders_route ON distribution_orders(route_id);
