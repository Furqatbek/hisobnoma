package com.hisobnoma.platform.web.dto;

import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Staff-facing view of a web catalog item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebCatalogItemDto {

    private Long id;
    private Long productId;
    private String sku;
    private String productName;
    private String displayName;
    private BigDecimal basePrice;
    private BigDecimal priceOverride;
    private BigDecimal effectivePrice;
    private Integer sortOrder;
    private WebCatalogStatus status;
    private String imageUrl;
    private String categoryName;
    private boolean productActive;
    private boolean productSellable;
}
