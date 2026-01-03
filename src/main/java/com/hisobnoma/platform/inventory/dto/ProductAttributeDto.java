package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeDto {
    private Long id;
    private Long productId;
    private String attributeName;
    private String attributeValue;
    private String attributeType;
    private String attributeGroup;
    private Integer sortOrder;
    private boolean visible;
    private boolean searchable;
    private boolean filterable;
}
