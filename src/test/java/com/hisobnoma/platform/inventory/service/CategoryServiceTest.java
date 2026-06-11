package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.inventory.dto.CategoryDto;
import com.hisobnoma.platform.inventory.dto.CreateCategoryRequest;
import com.hisobnoma.platform.inventory.dto.UpdateCategoryRequest;
import com.hisobnoma.platform.inventory.entity.Category;
import com.hisobnoma.platform.inventory.mapper.CategoryMapper;
import com.hisobnoma.platform.inventory.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private CategoryService categoryService;

    private static final Long TENANT_ID = 1L;
    private Category rootCategory;
    private Category childCategory;
    private CategoryDto rootCategoryDto;
    private CategoryDto childCategoryDto;

    @BeforeEach
    void setUp() {
        lenient().when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        rootCategory = Category.builder()
                .id(1L)
                .code("ELECTRONICS")
                .name("Electronics")
                .level(0)
                .path("/1")
                .active(true)
                .children(new ArrayList<>())
                .tenantId(TENANT_ID)
                .build();

        childCategory = Category.builder()
                .id(2L)
                .code("PHONES")
                .name("Phones")
                .parent(rootCategory)
                .level(1)
                .path("/1/2")
                .active(true)
                .children(new ArrayList<>())
                .tenantId(TENANT_ID)
                .build();

        rootCategory.getChildren().add(childCategory);

        rootCategoryDto = CategoryDto.builder()
                .id(1L)
                .code("ELECTRONICS")
                .name("Electronics")
                .level(0)
                .active(true)
                .build();

        childCategoryDto = CategoryDto.builder()
                .id(2L)
                .code("PHONES")
                .name("Phones")
                .parentId(1L)
                .level(1)
                .active(true)
                .build();
    }

    @Test
    void getAllCategories_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(rootCategory, childCategory));
        when(categoryMapper.toDtoList(anyList())).thenReturn(List.of(rootCategoryDto, childCategoryDto));

        // When
        List<CategoryDto> result = categoryService.getAllCategories();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getCategoryTree_returnsHierarchicalList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.findRootCategoriesByTenantId(TENANT_ID)).thenReturn(List.of(rootCategory));
        when(categoryMapper.toDto(rootCategory)).thenReturn(rootCategoryDto);
        when(categoryRepository.findChildrenByParentId(TENANT_ID, 1L)).thenReturn(List.of(childCategory));
        when(categoryMapper.toDto(childCategory)).thenReturn(childCategoryDto);
        when(categoryRepository.findChildrenByParentId(TENANT_ID, 2L)).thenReturn(Collections.emptyList());

        // When
        List<CategoryDto> result = categoryService.getCategoryTree();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getCategory_found_returnsDto() {
        // Given
        when(categoryRepository.findByIdWithChildren(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryMapper.toDtoWithChildren(rootCategory)).thenReturn(rootCategoryDto);

        // When
        CategoryDto result = categoryService.getCategory(1L);

        // Then
        assertNotNull(result);
        assertEquals("Electronics", result.getName());
    }

    @Test
    void getCategory_notFound_throwsNotFoundException() {
        // Given
        when(categoryRepository.findByIdWithChildren(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> categoryService.getCategory(999L));
    }

    @Test
    void createCategory_root_success() {
        // Given
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .code("FOOD")
                .name("Food")
                .build();

        Category newCategory = Category.builder()
                .id(3L)
                .code("FOOD")
                .name("Food")
                .level(0)
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.existsByCodeAndTenantId("FOOD", TENANT_ID)).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(newCategory);
        when(categoryRepository.save(any(Category.class))).thenReturn(newCategory);
        when(categoryMapper.toDto(newCategory)).thenReturn(CategoryDto.builder().id(3L).name("Food").build());

        // When
        CategoryDto result = categoryService.createCategory(request);

        // Then
        assertNotNull(result);
        verify(categoryRepository, atLeast(1)).save(any(Category.class));
    }

    @Test
    void createCategory_withParent_success() {
        // Given
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .code("TABLETS")
                .name("Tablets")
                .parentId(1L)
                .build();

        Category newCategory = Category.builder()
                .id(3L)
                .code("TABLETS")
                .name("Tablets")
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.existsByCodeAndTenantId("TABLETS", TENANT_ID)).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(newCategory);
        when(categoryRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(newCategory);
        when(categoryMapper.toDto(newCategory)).thenReturn(CategoryDto.builder().id(3L).name("Tablets").parentId(1L).build());

        // When
        CategoryDto result = categoryService.createCategory(request);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getParentId());
    }

    @Test
    void createCategory_parentNotFound_throwsNotFoundException() {
        // Given
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .code("CHILD")
                .name("Child")
                .parentId(999L)
                .build();

        Category newCategory = Category.builder()
                .id(3L)
                .code("CHILD")
                .name("Child")
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.existsByCodeAndTenantId("CHILD", TENANT_ID)).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(newCategory);
        when(categoryRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> categoryService.createCategory(request));
    }

    @Test
    void createCategory_duplicateCode_throwsDuplicateResourceException() {
        // Given
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .code("ELECTRONICS")
                .name("Electronics Dup")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.existsByCodeAndTenantId("ELECTRONICS", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(DuplicateResourceException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategory_success() {
        // Given
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Updated Electronics")
                .build();

        when(categoryRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(rootCategory);
        when(categoryMapper.toDto(rootCategory)).thenReturn(rootCategoryDto);

        // When
        CategoryDto result = categoryService.updateCategory(1L, request);

        // Then
        assertNotNull(result);
        verify(categoryRepository).save(rootCategory);
    }

    @Test
    void updateCategory_selfParent_throwsValidationException() {
        // Given
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .parentId(1L)
                .build();

        when(categoryRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(rootCategory));

        // When/Then
        assertThrows(ValidationException.class, () -> categoryService.updateCategory(1L, request));
    }

    @Test
    void deleteCategory_success_noChildren_noProducts() {
        // Given
        Category leafCategory = Category.builder()
                .id(3L)
                .code("LEAF")
                .name("Leaf")
                .children(new ArrayList<>())
                .tenantId(TENANT_ID)
                .build();

        when(categoryRepository.findByIdWithChildren(3L)).thenReturn(Optional.of(leafCategory));

        // When
        categoryService.deleteCategory(3L);

        // Then
        verify(categoryRepository).delete(leafCategory);
    }

    @Test
    void deleteCategory_hasChildren_throwsValidationException() {
        // Given
        when(categoryRepository.findByIdWithChildren(1L)).thenReturn(Optional.of(rootCategory));

        // When/Then
        assertThrows(ValidationException.class, () -> categoryService.deleteCategory(1L));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_notFound_throwsNotFoundException() {
        // Given
        when(categoryRepository.findByIdWithChildren(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> categoryService.deleteCategory(999L));
    }
}
