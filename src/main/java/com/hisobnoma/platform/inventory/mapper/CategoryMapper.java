package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CategoryDto;
import com.hisobnoma.platform.inventory.dto.CreateCategoryRequest;
import com.hisobnoma.platform.inventory.entity.Category;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Named("toDto")
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    @Mapping(target = "children", ignore = true)
    CategoryDto toDto(Category category);

    @Named("toDtoWithChildren")
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    @Mapping(target = "children", qualifiedByName = "mapChildrenRecursive")
    CategoryDto toDtoWithChildren(Category category);

    @IterableMapping(qualifiedByName = "toDto")
    List<CategoryDto> toDtoList(List<Category> categories);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "level", ignore = true)
    @Mapping(target = "path", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Category toEntity(CreateCategoryRequest request);

    @Named("mapChildrenRecursive")
    default List<CategoryDto> mapChildren(List<Category> children) {
        if (children == null || children.isEmpty()) {
            return new ArrayList<>();
        }
        return children.stream()
                .map(this::toDtoWithChildren)
                .toList();
    }
}
