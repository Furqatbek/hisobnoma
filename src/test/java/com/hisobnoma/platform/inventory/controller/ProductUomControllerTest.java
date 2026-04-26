package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.inventory.dto.CreateProductUomRequest;
import com.hisobnoma.platform.inventory.dto.ProductUomDto;
import com.hisobnoma.platform.inventory.service.ProductUomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductUomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductUomService productUomService;

    // ==================== getByProduct ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getByProduct_authenticated_returns200() throws Exception {
        ProductUomDto dto = ProductUomDto.builder()
                .id(1L)
                .productId(1L)
                .uomId(1L)
                .uomCode("BOX")
                .uomName("Box")
                .conversionFactor(new BigDecimal("12"))
                .active(true)
                .build();
        when(productUomService.getByProduct(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/products/1/uoms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uomCode").value("BOX"));
    }

    @Test
    void getByProduct_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/1/uoms"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getByProduct_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/1/uoms"))
                .andExpect(status().isForbidden());
    }

    // ==================== getActiveByProduct ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getActiveByProduct_authenticated_returns200() throws Exception {
        ProductUomDto dto = ProductUomDto.builder()
                .id(1L)
                .productId(1L)
                .uomCode("BOX")
                .active(true)
                .build();
        when(productUomService.getActiveByProduct(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/products/1/uoms/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getActiveByProduct_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/1/uoms/active"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getActiveByProduct_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/1/uoms/active"))
                .andExpect(status().isForbidden());
    }

    // ==================== create ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_UPDATE")
    void create_valid_returns201() throws Exception {
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("12"))
                .sellingPrice(new BigDecimal("120.00"))
                .active(true)
                .build();

        ProductUomDto dto = ProductUomDto.builder()
                .id(1L)
                .productId(1L)
                .uomId(2L)
                .uomCode("BOX")
                .uomName("Box")
                .conversionFactor(new BigDecimal("12"))
                .sellingPrice(new BigDecimal("120.00"))
                .active(true)
                .build();
        when(productUomService.create(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/inventory/products/1/uoms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uomCode").value("BOX"));
    }

    @Test
    void create_unauthenticated_returns403() throws Exception {
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("12"))
                .build();

        mockMvc.perform(post("/api/v1/inventory/products/1/uoms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void create_noPermission_returns403() throws Exception {
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("12"))
                .build();

        mockMvc.perform(post("/api/v1/inventory/products/1/uoms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== update ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_UPDATE")
    void update_valid_returns200() throws Exception {
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("24"))
                .sellingPrice(new BigDecimal("240.00"))
                .build();

        ProductUomDto dto = ProductUomDto.builder()
                .id(1L)
                .productId(1L)
                .uomId(2L)
                .uomCode("BOX")
                .conversionFactor(new BigDecimal("24"))
                .sellingPrice(new BigDecimal("240.00"))
                .build();
        when(productUomService.update(any(), any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/products/1/uoms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversionFactor").value(24));
    }

    @Test
    void update_unauthenticated_returns403() throws Exception {
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("24"))
                .build();

        mockMvc.perform(put("/api/v1/inventory/products/1/uoms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void update_noPermission_returns403() throws Exception {
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("24"))
                .build();

        mockMvc.perform(put("/api/v1/inventory/products/1/uoms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== delete ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_DELETE")
    void delete_returns204() throws Exception {
        doNothing().when(productUomService).delete(1L, 1L);

        mockMvc.perform(delete("/api/v1/inventory/products/1/uoms/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/products/1/uoms/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void delete_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/products/1/uoms/1"))
                .andExpect(status().isForbidden());
    }
}
