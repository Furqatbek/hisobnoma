package com.hisobnoma.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public (anonymous) view of a catalog item for the online shop.
 * Deliberately exposes only customer-safe fields — never cost prices
 * or stock quantities (only an in-stock flag).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCatalogProductDto {

    private Long id;
    private String name;
    private String shortDescription;
    private String description;
    private BigDecimal price;
    private String currency;
    private Long categoryId;
    private String categoryName;
    private String brandName;
    private String unitName;
    private boolean inStock;
    private String imageUrl;
    private List<String> images;
}
