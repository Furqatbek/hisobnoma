package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDto {
    private Long id;
    private Long productId;
    private String imageUrl;
    private String thumbnailUrl;
    private String altText;
    private String title;
    private Integer sortOrder;
    private boolean primary;
    private boolean active;
}
