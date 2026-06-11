-- Phase 6: wishlists ("like") + price/restock alerts

CREATE TABLE web_wishlist_items (
    id                       BIGSERIAL PRIMARY KEY,
    tenant_id                BIGINT NOT NULL REFERENCES tenants(id),
    web_customer_id          BIGINT NOT NULL REFERENCES web_customers(id),
    catalog_item_id          BIGINT NOT NULL REFERENCES web_catalog_items(id),
    last_known_available     BOOLEAN NOT NULL DEFAULT true,
    last_notified_sale_price NUMERIC(18,4),
    notified_at              TIMESTAMP WITH TIME ZONE,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP WITH TIME ZONE,
    version                  BIGINT DEFAULT 0,
    CONSTRAINT uk_web_wishlist_item UNIQUE (web_customer_id, catalog_item_id)
);

CREATE INDEX idx_web_wishlist_tenant_item ON web_wishlist_items (tenant_id, catalog_item_id);
CREATE INDEX idx_web_wishlist_tenant_customer ON web_wishlist_items (tenant_id, web_customer_id);
