package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.service.PriceListService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
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
class PriceListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PriceListService priceListService;

    // ==================== GET /api/v1/pos/price-lists ====================

    @Test
    @WithMockUser(authorities = "POS_PRICELIST_READ")
    void getAllPriceLists_authenticated_returns200() throws Exception {
        PriceListDto dto = PriceListDto.builder().id(1L).code("PL-001").name("Default").build();
        when(priceListService.findAllPriceLists(any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/pos/price-lists"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllPriceLists_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/price-lists"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAllPriceLists_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/price-lists"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/price-lists/active ====================

    @Test
    @WithMockUser(authorities = "POS_PRICELIST_READ")
    void getActivePriceLists_authenticated_returns200() throws Exception {
        when(priceListService.findActivePriceLists()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/price-lists/active"))
                .andExpect(status().isOk());
    }

    @Test
    void getActivePriceLists_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/price-lists/active"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/price-lists/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_PRICELIST_READ")
    void getPriceListById_authenticated_returns200() throws Exception {
        PriceListDto dto = PriceListDto.builder().id(1L).code("PL-001").name("Default").build();
        when(priceListService.findPriceListById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/price-lists/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PL-001"));
    }

    @Test
    void getPriceListById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/price-lists/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getPriceListById_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/price-lists/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/price-lists/code/{code} ====================

    @Test
    @WithMockUser(authorities = "POS_PRICELIST_READ")
    void getPriceListByCode_authenticated_returns200() throws Exception {
        PriceListDto dto = PriceListDto.builder().id(1L).code("PL-001").name("Default").build();
        when(priceListService.findPriceListByCode("PL-001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/price-lists/code/PL-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PL-001"));
    }

    @Test
    void getPriceListByCode_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/price-lists/code/PL-001"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/price-lists ====================

    @Test
    @WithMockUser(authorities = "POS_PRICELIST_CREATE")
    void createPriceList_authenticated_returns201() throws Exception {
        CreatePriceListRequest request = new CreatePriceListRequest();
        PriceListDto dto = PriceListDto.builder().id(1L).code("PL-001").name("Default").build();
        when(priceListService.createPriceList(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/price-lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PL-001"));
    }

    @Test
    void createPriceList_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/price-lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createPriceList_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/price-lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/pos/price-lists/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_PRICELIST_UPDATE")
    void updatePriceList_authenticated_returns200() throws Exception {
        CreatePriceListRequest request = new CreatePriceListRequest();
        PriceListDto dto = PriceListDto.builder().id(1L).code("PL-001").name("Updated").build();
        when(priceListService.updatePriceList(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/pos/price-lists/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updatePriceList_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/price-lists/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void updatePriceList_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/price-lists/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/pos/price-lists/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_PRICELIST_DELETE")
    void deletePriceList_authenticated_returns204() throws Exception {
        doNothing().when(priceListService).deletePriceList(1L);

        mockMvc.perform(delete("/api/v1/pos/price-lists/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePriceList_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/price-lists/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deletePriceList_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/price-lists/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/price-lists/{id}/activate ====================

    @Test
    @WithMockUser(authorities = "POS_PRICELIST_UPDATE")
    void activatePriceList_authenticated_returns200() throws Exception {
        PriceListDto dto = PriceListDto.builder().id(1L).code("PL-001").build();
        when(priceListService.activatePriceList(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/price-lists/1/activate"))
                .andExpect(status().isOk());
    }

    @Test
    void activatePriceList_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/price-lists/1/activate"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/price-lists/{priceListId}/items ====================

    @Test
    @WithMockUser(authorities = "POS_PRICELIST_ITEMS_MANAGE")
    void addPriceListItem_authenticated_returns201() throws Exception {
        CreatePriceListItemRequest request = new CreatePriceListItemRequest();
        PriceListItemDto dto = PriceListItemDto.builder().id(1L).build();
        when(priceListService.addPriceListItem(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/price-lists/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void addPriceListItem_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/price-lists/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void addPriceListItem_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/price-lists/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/pos/price-lists/{priceListId}/items/{itemId} ====================

    @Test
    @WithMockUser(authorities = "POS_PRICELIST_ITEMS_MANAGE")
    void removePriceListItem_authenticated_returns204() throws Exception {
        doNothing().when(priceListService).removePriceListItem(1L, 2L);

        mockMvc.perform(delete("/api/v1/pos/price-lists/1/items/2"))
                .andExpect(status().isNoContent());
    }

    @Test
    void removePriceListItem_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/price-lists/1/items/2"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/price-lists/{priceListId}/assign-customer/{customerId} ====================

    @Test
    @WithMockUser(authorities = "POS_PRICELIST_ASSIGN")
    void assignPriceListToCustomer_authenticated_returns200() throws Exception {
        doNothing().when(priceListService).assignPriceListToCustomer(eq(1L), eq(2L), any());

        mockMvc.perform(post("/api/v1/pos/price-lists/1/assign-customer/2"))
                .andExpect(status().isOk());
    }

    @Test
    void assignPriceListToCustomer_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/price-lists/1/assign-customer/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void assignPriceListToCustomer_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/price-lists/1/assign-customer/2"))
                .andExpect(status().isForbidden());
    }
}
