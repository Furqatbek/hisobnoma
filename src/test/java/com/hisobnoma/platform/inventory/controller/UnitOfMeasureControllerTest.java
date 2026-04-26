package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.CreateUomRequest;
import com.hisobnoma.platform.inventory.dto.UnitOfMeasureDto;
import com.hisobnoma.platform.inventory.service.UnitOfMeasureService;
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
class UnitOfMeasureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UnitOfMeasureService uomService;

    // ==================== getAllUoms ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getAllUoms_authenticated_returns200() throws Exception {
        UnitOfMeasureDto dto = UnitOfMeasureDto.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .symbol("pcs")
                .active(true)
                .isBaseUnit(true)
                .build();
        when(uomService.getAllUoms()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/uom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PCS"));
    }

    @Test
    void getAllUoms_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAllUoms_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom"))
                .andExpect(status().isForbidden());
    }

    // ==================== getUomsPaginated ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getUomsPaginated_authenticated_returns200() throws Exception {
        UnitOfMeasureDto dto = UnitOfMeasureDto.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .build();
        PageResponse<UnitOfMeasureDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(uomService.getUoms(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/uom/paginated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("PCS"));
    }

    @Test
    void getUomsPaginated_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/paginated"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getUomsPaginated_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/paginated"))
                .andExpect(status().isForbidden());
    }

    // ==================== getActiveUoms ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getActiveUoms_authenticated_returns200() throws Exception {
        UnitOfMeasureDto dto = UnitOfMeasureDto.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .active(true)
                .build();
        when(uomService.getActiveUoms()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/uom/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getActiveUoms_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/active"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getActiveUoms_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/active"))
                .andExpect(status().isForbidden());
    }

    // ==================== getBaseUoms ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getBaseUoms_authenticated_returns200() throws Exception {
        UnitOfMeasureDto dto = UnitOfMeasureDto.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .isBaseUnit(true)
                .build();
        when(uomService.getBaseUoms()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/uom/base"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PCS"));
    }

    @Test
    void getBaseUoms_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/base"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getBaseUoms_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/base"))
                .andExpect(status().isForbidden());
    }

    // ==================== getUom ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getUom_found_returns200() throws Exception {
        UnitOfMeasureDto dto = UnitOfMeasureDto.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .build();
        when(uomService.getUom(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/uom/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PCS"));
    }

    @Test
    void getUom_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getUom_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getUomByCode ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getUomByCode_found_returns200() throws Exception {
        UnitOfMeasureDto dto = UnitOfMeasureDto.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .build();
        when(uomService.getUomByCode("PCS")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/uom/code/PCS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PCS"));
    }

    @Test
    void getUomByCode_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/code/PCS"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getUomByCode_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/code/PCS"))
                .andExpect(status().isForbidden());
    }

    // ==================== searchUoms ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void searchUoms_authenticated_returns200() throws Exception {
        UnitOfMeasureDto dto = UnitOfMeasureDto.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .build();
        PageResponse<UnitOfMeasureDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(uomService.searchUoms(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/uom/search").param("q", "Piece"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("PCS"));
    }

    @Test
    void searchUoms_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/search").param("q", "Piece"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void searchUoms_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/search").param("q", "Piece"))
                .andExpect(status().isForbidden());
    }

    // ==================== createUom ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_CREATE")
    void createUom_valid_returns201() throws Exception {
        CreateUomRequest request = CreateUomRequest.builder()
                .code("KG")
                .name("Kilograms")
                .symbol("kg")
                .active(true)
                .isBaseUnit(true)
                .build();

        UnitOfMeasureDto dto = UnitOfMeasureDto.builder()
                .id(2L)
                .code("KG")
                .name("Kilograms")
                .symbol("kg")
                .active(true)
                .isBaseUnit(true)
                .build();
        when(uomService.createUom(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/inventory/uom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("KG"));
    }

    @Test
    void createUom_unauthenticated_returns403() throws Exception {
        CreateUomRequest request = CreateUomRequest.builder()
                .code("KG")
                .name("Kilograms")
                .build();

        mockMvc.perform(post("/api/v1/inventory/uom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createUom_noPermission_returns403() throws Exception {
        CreateUomRequest request = CreateUomRequest.builder()
                .code("KG")
                .name("Kilograms")
                .build();

        mockMvc.perform(post("/api/v1/inventory/uom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== updateUom ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_UPDATE")
    void updateUom_valid_returns200() throws Exception {
        CreateUomRequest request = CreateUomRequest.builder()
                .code("PCS")
                .name("Updated Pieces")
                .build();

        UnitOfMeasureDto dto = UnitOfMeasureDto.builder()
                .id(1L)
                .code("PCS")
                .name("Updated Pieces")
                .build();
        when(uomService.updateUom(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/uom/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Pieces"));
    }

    @Test
    void updateUom_unauthenticated_returns403() throws Exception {
        CreateUomRequest request = CreateUomRequest.builder()
                .code("PCS")
                .name("Updated Pieces")
                .build();

        mockMvc.perform(put("/api/v1/inventory/uom/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void updateUom_noPermission_returns403() throws Exception {
        CreateUomRequest request = CreateUomRequest.builder()
                .code("PCS")
                .name("Updated Pieces")
                .build();

        mockMvc.perform(put("/api/v1/inventory/uom/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== deleteUom ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_DELETE")
    void deleteUom_returns204() throws Exception {
        doNothing().when(uomService).deleteUom(1L);

        mockMvc.perform(delete("/api/v1/inventory/uom/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUom_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/uom/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deleteUom_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/uom/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== convert ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void convert_authenticated_returns200() throws Exception {
        when(uomService.convert(new BigDecimal("10"), 1L, 2L)).thenReturn(new BigDecimal("10000"));

        mockMvc.perform(get("/api/v1/inventory/uom/convert")
                        .param("quantity", "10")
                        .param("fromUomId", "1")
                        .param("toUomId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(10000));
    }

    @Test
    void convert_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/convert")
                        .param("quantity", "10")
                        .param("fromUomId", "1")
                        .param("toUomId", "2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void convert_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/uom/convert")
                        .param("quantity", "10")
                        .param("fromUomId", "1")
                        .param("toUomId", "2"))
                .andExpect(status().isForbidden());
    }
}
