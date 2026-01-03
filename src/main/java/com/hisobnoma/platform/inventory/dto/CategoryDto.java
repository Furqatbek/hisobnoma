package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String imageUrl;
    private Long parentId;
    private String parentName;
    private Integer sortOrder;
    private boolean active;
    private Integer level;
    private String path;
    private List<CategoryDto> children;
}
