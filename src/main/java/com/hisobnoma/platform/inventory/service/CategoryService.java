package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.Category;
import com.hisobnoma.platform.inventory.mapper.CategoryMapper;
import com.hisobnoma.platform.inventory.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Category> categories = categoryRepository.findAllByTenantId(tenantId);
        return categoryMapper.toDtoList(categories);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getRootCategories() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Category> rootCategories = categoryRepository.findRootCategoriesByTenantId(tenantId);
        return rootCategories.stream()
                .map(categoryMapper::toDtoWithChildren)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoryTree() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Category> rootCategories = categoryRepository.findRootCategoriesByTenantId(tenantId);
        return buildCategoryTree(rootCategories, tenantId);
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategory(Long id) {
        Category category = categoryRepository.findByIdWithChildren(id)
                .orElseThrow(() -> new NotFoundException("Category", id));
        return categoryMapper.toDtoWithChildren(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getChildCategories(Long parentId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Category> children = categoryRepository.findChildrenByParentId(tenantId, parentId);
        return categoryMapper.toDtoList(children);
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryDto> searchCategories(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Category> page = categoryRepository.searchByTenantId(tenantId, search, pageable);
        return PageResponse.of(page.map(categoryMapper::toDto));
    }

    @Transactional
    public CategoryDto createCategory(CreateCategoryRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Check for duplicate code
        if (categoryRepository.existsByCodeAndTenantId(request.getCode(), tenantId) ||
            (tenantId == null && categoryRepository.existsByCodeAndTenantIdIsNull(request.getCode()))) {
            throw new DuplicateResourceException("Category", "code", request.getCode());
        }

        Category category = categoryMapper.toEntity(request);
        category.setTenantId(tenantId);

        // Set parent if specified
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("Parent Category", request.getParentId()));
            category.setParent(parent);
            category.setLevel(parent.getLevel() + 1);
        }

        if (request.getSortOrder() == null) {
            category.setSortOrder(0);
        }

        category = categoryRepository.save(category);

        // Update path after save (needs ID)
        category.setPath(buildPath(category));
        category = categoryRepository.save(category);

        log.info("Created category: {} ({})", category.getName(), category.getCode());
        return categoryMapper.toDto(category);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category", id));

        if (request.getName() != null) {
            category.setName(request.getName());
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }

        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new ValidationException("Category cannot be its own parent");
            }

            Category newParent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("Parent Category", request.getParentId()));

            // Check for circular reference
            if (isDescendant(newParent, category)) {
                throw new ValidationException("Cannot set descendant as parent (circular reference)");
            }

            category.setParent(newParent);
            category.setLevel(newParent.getLevel() + 1);
            category.setPath(buildPath(category));
            updateChildrenPaths(category);
        }

        category = categoryRepository.save(category);
        log.info("Updated category: {}", category.getId());
        return categoryMapper.toDto(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findByIdWithChildren(id)
                .orElseThrow(() -> new NotFoundException("Category", id));

        if (category.hasChildren()) {
            throw new ValidationException("Cannot delete category with children. Delete children first.");
        }

        categoryRepository.delete(category);
        log.info("Deleted category: {}", id);
    }

    private List<CategoryDto> buildCategoryTree(List<Category> categories, Long tenantId) {
        return categories.stream()
                .map(category -> {
                    CategoryDto dto = categoryMapper.toDto(category);
                    List<Category> children = categoryRepository.findChildrenByParentId(tenantId, category.getId());
                    if (!children.isEmpty()) {
                        dto.setChildren(buildCategoryTree(children, tenantId));
                    }
                    return dto;
                })
                .toList();
    }

    private String buildPath(Category category) {
        if (category.getParent() == null) {
            return "/" + category.getId();
        }
        return category.getParent().getPath() + "/" + category.getId();
    }

    private boolean isDescendant(Category potentialDescendant, Category ancestor) {
        if (potentialDescendant == null || ancestor == null) {
            return false;
        }

        Category current = potentialDescendant;
        while (current != null) {
            if (current.getId().equals(ancestor.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void updateChildrenPaths(Category parent) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Category> children = categoryRepository.findChildrenByParentId(tenantId, parent.getId());
        for (Category child : children) {
            child.setLevel(parent.getLevel() + 1);
            child.setPath(buildPath(child));
            categoryRepository.save(child);
            updateChildrenPaths(child);
        }
    }
}
