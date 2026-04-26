package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.CategoryDto;
import com.hisobnoma.platform.inventory.dto.CreateCategoryRequest;
import com.hisobnoma.platform.inventory.dto.UpdateCategoryRequest;
import com.hisobnoma.platform.inventory.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    // ==================== getAllCategories ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getAllCategories_authenticated_returns200() throws Exception {
        CategoryDto dto = CategoryDto.builder()
                .id(1L)
                .code("CAT-001")
                .name("Electronics")
                .active(true)
                .build();
        when(categoryService.getAllCategories()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CAT-001"));
    }

    @Test
    void getAllCategories_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAllCategories_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/categories"))
                .andExpect(status().isForbidden());
    }

    // ==================== getCategoryTree ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getCategoryTree_authenticated_returns200() throws Exception {
        CategoryDto dto = CategoryDto.builder()
                .id(1L)
                .code("CAT-001")
                .name("Electronics")
                .build();
        when(categoryService.getCategoryTree()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    void getCategoryTree_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/categories/tree"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getCategoryTree_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/categories/tree"))
                .andExpect(status().isForbidden());
    }

    // ==================== getRootCategories ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getRootCategories_authenticated_returns200() throws Exception {
        CategoryDto dto = CategoryDto.builder()
                .id(1L)
                .code("CAT-001")
                .name("Root Category")
                .build();
        when(categoryService.getRootCategories()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/categories/roots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Root Category"));
    }

    @Test
    void getRootCategories_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/categories/roots"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getRootCategories_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/categories/roots"))
                .andExpect(status().isForbidden());
    }

    // ==================== getCategory ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getCategory_found_returns200() throws Exception {
        CategoryDto dto = CategoryDto.builder()
                .id(1L)
                .code("CAT-001")
                .name("Electronics")
                .build();
        when(categoryService.getCategory(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CAT-001"));
    }

    @Test
    void getCategory_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/categories/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getCategory_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/categories/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getChildCategories ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getChildCategories_authenticated_returns200() throws Exception {
        CategoryDto dto = CategoryDto.builder()
                .id(2L)
                .code("CAT-002")
                .name("Laptops")
                .parentId(1L)
                .build();
        when(categoryService.getChildCategories(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/categories/1/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CAT-002"));
    }

    @Test
    void getChildCategories_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/categories/1/children"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getChildCategories_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/categories/1/children"))
                .andExpect(status().isForbidden());
    }

    // ==================== searchCategories ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void searchCategories_authenticated_returns200() throws Exception {
        CategoryDto dto = CategoryDto.builder()
                .id(1L)
                .code("CAT-001")
                .name("Electronics")
                .build();
        PageResponse<CategoryDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(categoryService.searchCategories(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/categories/search").param("q", "Elec"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("CAT-001"));
    }

    @Test
    void searchCategories_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/categories/search").param("q", "Elec"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void searchCategories_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/categories/search").param("q", "Elec"))
                .andExpect(status().isForbidden());
    }

    // ==================== createCategory ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_CREATE")
    void createCategory_valid_returns201() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .code("CAT-002")
                .name("New Category")
                .active(true)
                .build();

        CategoryDto dto = CategoryDto.builder()
                .id(2L)
                .code("CAT-002")
                .name("New Category")
                .active(true)
                .build();
        when(categoryService.createCategory(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/inventory/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CAT-002"));
    }

    @Test
    void createCategory_unauthenticated_returns403() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .code("CAT-002")
                .name("New Category")
                .build();

        mockMvc.perform(post("/api/v1/inventory/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createCategory_noPermission_returns403() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .code("CAT-002")
                .name("New Category")
                .build();

        mockMvc.perform(post("/api/v1/inventory/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== updateCategory ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_UPDATE")
    void updateCategory_valid_returns200() throws Exception {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Updated Category")
                .build();

        CategoryDto dto = CategoryDto.builder()
                .id(1L)
                .code("CAT-001")
                .name("Updated Category")
                .build();
        when(categoryService.updateCategory(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Category"));
    }

    @Test
    void updateCategory_unauthenticated_returns403() throws Exception {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Updated Category")
                .build();

        mockMvc.perform(put("/api/v1/inventory/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void updateCategory_noPermission_returns403() throws Exception {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Updated Category")
                .build();

        mockMvc.perform(put("/api/v1/inventory/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== deleteCategory ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_DELETE")
    void deleteCategory_returns204() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/v1/inventory/categories/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCategory_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/categories/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deleteCategory_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/categories/1"))
                .andExpect(status().isForbidden());
    }
}
