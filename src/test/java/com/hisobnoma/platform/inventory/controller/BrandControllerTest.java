package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.BrandDto;
import com.hisobnoma.platform.inventory.dto.CreateBrandRequest;
import com.hisobnoma.platform.inventory.dto.UpdateBrandRequest;
import com.hisobnoma.platform.inventory.service.BrandService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BrandService brandService;

    // ==================== getAllBrands ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getAllBrands_authenticated_returns200() throws Exception {
        BrandDto dto = BrandDto.builder()
                .id(1L)
                .code("BRD-001")
                .name("Test Brand")
                .active(true)
                .build();
        when(brandService.getAllBrands()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BRD-001"));
    }

    @Test
    void getAllBrands_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/brands"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAllBrands_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/brands"))
                .andExpect(status().isForbidden());
    }

    // ==================== getBrandsPaginated ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getBrandsPaginated_authenticated_returns200() throws Exception {
        BrandDto dto = BrandDto.builder()
                .id(1L)
                .code("BRD-001")
                .name("Test Brand")
                .active(true)
                .build();
        PageResponse<BrandDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(brandService.getBrands(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/brands/paginated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("BRD-001"));
    }

    @Test
    void getBrandsPaginated_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/brands/paginated"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getBrandsPaginated_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/brands/paginated"))
                .andExpect(status().isForbidden());
    }

    // ==================== getActiveBrands ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getActiveBrands_authenticated_returns200() throws Exception {
        BrandDto dto = BrandDto.builder()
                .id(1L)
                .code("BRD-001")
                .name("Test Brand")
                .active(true)
                .build();
        when(brandService.getActiveBrands()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/brands/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getActiveBrands_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/brands/active"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getActiveBrands_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/brands/active"))
                .andExpect(status().isForbidden());
    }

    // ==================== getBrand ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getBrand_found_returns200() throws Exception {
        BrandDto dto = BrandDto.builder()
                .id(1L)
                .code("BRD-001")
                .name("Test Brand")
                .build();
        when(brandService.getBrand(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/brands/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BRD-001"));
    }

    @Test
    void getBrand_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/brands/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getBrand_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/brands/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== searchBrands ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void searchBrands_authenticated_returns200() throws Exception {
        BrandDto dto = BrandDto.builder()
                .id(1L)
                .code("BRD-001")
                .name("Test Brand")
                .build();
        PageResponse<BrandDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(brandService.searchBrands(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/brands/search").param("q", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("BRD-001"));
    }

    @Test
    void searchBrands_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/brands/search").param("q", "Test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void searchBrands_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/brands/search").param("q", "Test"))
                .andExpect(status().isForbidden());
    }

    // ==================== createBrand ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_CREATE")
    void createBrand_valid_returns201() throws Exception {
        CreateBrandRequest request = CreateBrandRequest.builder()
                .code("BRD-002")
                .name("New Brand")
                .active(true)
                .build();

        BrandDto dto = BrandDto.builder()
                .id(2L)
                .code("BRD-002")
                .name("New Brand")
                .active(true)
                .build();
        when(brandService.createBrand(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/inventory/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BRD-002"));
    }

    @Test
    void createBrand_unauthenticated_returns403() throws Exception {
        CreateBrandRequest request = CreateBrandRequest.builder()
                .code("BRD-002")
                .name("New Brand")
                .build();

        mockMvc.perform(post("/api/v1/inventory/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createBrand_noPermission_returns403() throws Exception {
        CreateBrandRequest request = CreateBrandRequest.builder()
                .code("BRD-002")
                .name("New Brand")
                .build();

        mockMvc.perform(post("/api/v1/inventory/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== updateBrand ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_UPDATE")
    void updateBrand_valid_returns200() throws Exception {
        UpdateBrandRequest request = UpdateBrandRequest.builder()
                .name("Updated Brand")
                .build();

        BrandDto dto = BrandDto.builder()
                .id(1L)
                .code("BRD-001")
                .name("Updated Brand")
                .build();
        when(brandService.updateBrand(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/brands/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Brand"));
    }

    @Test
    void updateBrand_unauthenticated_returns403() throws Exception {
        UpdateBrandRequest request = UpdateBrandRequest.builder()
                .name("Updated Brand")
                .build();

        mockMvc.perform(put("/api/v1/inventory/brands/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void updateBrand_noPermission_returns403() throws Exception {
        UpdateBrandRequest request = UpdateBrandRequest.builder()
                .name("Updated Brand")
                .build();

        mockMvc.perform(put("/api/v1/inventory/brands/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== deleteBrand ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_DELETE")
    void deleteBrand_returns204() throws Exception {
        doNothing().when(brandService).deleteBrand(1L);

        mockMvc.perform(delete("/api/v1/inventory/brands/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBrand_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/brands/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deleteBrand_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/brands/1"))
                .andExpect(status().isForbidden());
    }
}
