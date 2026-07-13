package com.hisobnoma.platform.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Canonical "product for POS / sale cart" shape. Returned by the mobile product-search
 * endpoints so a product looked up via search carries the same cart-relevant fields as one
 * loaded from the inventory list — no partial-object mapping on the client. Being a typed DTO
 * (not an ad-hoc Map), fields can't silently drift.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductLookupDto {
    /** Product id — used as the sale-line productId and the cart key. */
    private Long id;
    private String name;
    private String sku;
    private String barcode;
    private BigDecimal sellingPrice;
    /** Price floor for discount enforcement. */
    private BigDecimal minSellingPrice;
    private String categoryName;
    /** Total on-hand across the tenant's locations. */
    private BigDecimal stockQuantity;
    private boolean active;
    private boolean trackInventory;
    private String baseUomName;
    private String baseUomCode;
}
