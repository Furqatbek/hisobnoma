package com.hisobnoma.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemDto {

    private Long catalogItemId;
    private String name;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String promotionLabel;
    private boolean inStock;
    private boolean available;
    private String imageUrl;
    private Instant addedAt;
    private boolean priceDrop;
}
