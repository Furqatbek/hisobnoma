package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CategoryDto;
import com.hisobnoma.platform.inventory.dto.CreateCategoryRequest;
import com.hisobnoma.platform.inventory.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CategoryMapperTest {

    @Autowired
    private CategoryMapper categoryMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Category parent = Category.builder()
                .id(10L)
                .code("CAT-ROOT")
                .name("Electronics")
                .active(true)
                .children(new ArrayList<>())
                .build();

        Category category = Category.builder()
                .id(1L)
                .tenantId(1L)
                .code("CAT-001")
                .name("Smartphones")
                .description("Mobile phones")
                .imageUrl("https://example.com/phones.png")
                .parent(parent)
                .sortOrder(1)
                .active(true)
                .level(1)
                .path("/10/1")
                .children(new ArrayList<>())
                .build();

        // When
        CategoryDto dto = categoryMapper.toDto(category);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("CAT-001", dto.getCode());
        assertEquals("Smartphones", dto.getName());
        assertEquals("Mobile phones", dto.getDescription());
        assertEquals("https://example.com/phones.png", dto.getImageUrl());
        assertEquals(10L, dto.getParentId());
        assertEquals("Electronics", dto.getParentName());
        assertEquals(1, dto.getSortOrder());
        assertTrue(dto.isActive());
        assertEquals(1, dto.getLevel());
        assertEquals("/10/1", dto.getPath());
        // children should be ignored by toDto
        assertNull(dto.getChildren());
    }

    @Test
    void toDto_withNullParent_mapsParentFieldsToNull() {
        // Given
        Category category = Category.builder()
                .id(1L)
                .code("CAT-ROOT")
                .name("Root Category")
                .parent(null)
                .level(0)
                .active(true)
                .children(new ArrayList<>())
                .build();

        // When
        CategoryDto dto = categoryMapper.toDto(category);

        // Then
        assertNotNull(dto);
        assertNull(dto.getParentId());
        assertNull(dto.getParentName());
    }

    @Test
    void toDtoWithChildren_mapsChildrenRecursively() {
        // Given
        Category grandchild = Category.builder()
                .id(3L)
                .code("CAT-003")
                .name("Android Phones")
                .parent(null) // will be set below
                .level(2)
                .active(true)
                .children(new ArrayList<>())
                .build();

        Category child = Category.builder()
                .id(2L)
                .code("CAT-002")
                .name("Smartphones")
                .parent(null) // will be set below
                .level(1)
                .active(true)
                .children(new ArrayList<>(List.of(grandchild)))
                .build();
        grandchild.setParent(child);

        Category root = Category.builder()
                .id(1L)
                .code("CAT-001")
                .name("Electronics")
                .parent(null)
                .level(0)
                .active(true)
                .children(new ArrayList<>(List.of(child)))
                .build();
        child.setParent(root);

        // When
        CategoryDto dto = categoryMapper.toDtoWithChildren(root);

        // Then
        assertNotNull(dto);
        assertEquals("Electronics", dto.getName());
        assertNotNull(dto.getChildren());
        assertEquals(1, dto.getChildren().size());
        assertEquals("Smartphones", dto.getChildren().get(0).getName());
        assertNotNull(dto.getChildren().get(0).getChildren());
        assertEquals(1, dto.getChildren().get(0).getChildren().size());
        assertEquals("Android Phones", dto.getChildren().get(0).getChildren().get(0).getName());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        Category cat1 = Category.builder()
                .id(1L)
                .code("CAT-001")
                .name("Electronics")
                .active(true)
                .children(new ArrayList<>())
                .build();

        Category cat2 = Category.builder()
                .id(2L)
                .code("CAT-002")
                .name("Clothing")
                .active(true)
                .children(new ArrayList<>())
                .build();

        // When
        List<CategoryDto> dtos = categoryMapper.toDtoList(List.of(cat1, cat2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Electronics", dtos.get(0).getName());
        assertEquals("Clothing", dtos.get(1).getName());
    }

    @Test
    void toEntity_mapsFromCreateRequest() {
        // Given
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .code("CAT-010")
                .name("Appliances")
                .description("Home appliances")
                .imageUrl("https://example.com/appliances.png")
                .sortOrder(5)
                .active(true)
                .build();

        // When
        Category entity = categoryMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("CAT-010", entity.getCode());
        assertEquals("Appliances", entity.getName());
        assertEquals("Home appliances", entity.getDescription());
        assertEquals("https://example.com/appliances.png", entity.getImageUrl());
        assertEquals(5, entity.getSortOrder());
        assertTrue(entity.isActive());
        // Ignored fields should not be set
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getParent());
    }
}
