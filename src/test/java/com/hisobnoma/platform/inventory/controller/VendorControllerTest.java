package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.CreateVendorContactRequest;
import com.hisobnoma.platform.inventory.dto.CreateVendorRequest;
import com.hisobnoma.platform.inventory.dto.VendorContactDto;
import com.hisobnoma.platform.inventory.dto.VendorDto;
import com.hisobnoma.platform.inventory.service.VendorContactService;
import com.hisobnoma.platform.inventory.service.VendorService;
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
class VendorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VendorService vendorService;

    @MockBean
    private VendorContactService vendorContactService;

    // ==================== getVendors ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_VIEW")
    void getVendors_authenticated_returns200() throws Exception {
        VendorDto dto = VendorDto.builder()
                .id(1L)
                .code("VND-001")
                .name("Test Vendor")
                .active(true)
                .build();
        PageResponse<VendorDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(vendorService.getVendors(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("VND-001"));
    }

    @Test
    void getVendors_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getVendors_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors"))
                .andExpect(status().isForbidden());
    }

    // ==================== getActiveVendors ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_VIEW")
    void getActiveVendors_authenticated_returns200() throws Exception {
        VendorDto dto = VendorDto.builder()
                .id(1L)
                .code("VND-001")
                .name("Test Vendor")
                .active(true)
                .build();
        when(vendorService.getActiveVendors()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/vendors/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getActiveVendors_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/active"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getActiveVendors_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/active"))
                .andExpect(status().isForbidden());
    }

    // ==================== getPreferredVendors ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_VIEW")
    void getPreferredVendors_authenticated_returns200() throws Exception {
        VendorDto dto = VendorDto.builder()
                .id(1L)
                .code("VND-001")
                .name("Preferred Vendor")
                .preferred(true)
                .build();
        when(vendorService.getPreferredVendors()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/vendors/preferred"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].preferred").value(true));
    }

    @Test
    void getPreferredVendors_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/preferred"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getPreferredVendors_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/preferred"))
                .andExpect(status().isForbidden());
    }

    // ==================== getVendor ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_VIEW")
    void getVendor_found_returns200() throws Exception {
        VendorDto dto = VendorDto.builder()
                .id(1L)
                .code("VND-001")
                .name("Test Vendor")
                .build();
        when(vendorService.getVendor(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/vendors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("VND-001"));
    }

    @Test
    void getVendor_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getVendor_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getVendorByCode ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_VIEW")
    void getVendorByCode_found_returns200() throws Exception {
        VendorDto dto = VendorDto.builder()
                .id(1L)
                .code("VND-001")
                .name("Test Vendor")
                .build();
        when(vendorService.getVendorByCode("VND-001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/vendors/code/VND-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("VND-001"));
    }

    @Test
    void getVendorByCode_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/code/VND-001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getVendorByCode_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/code/VND-001"))
                .andExpect(status().isForbidden());
    }

    // ==================== searchVendors ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_VIEW")
    void searchVendors_authenticated_returns200() throws Exception {
        VendorDto dto = VendorDto.builder()
                .id(1L)
                .code("VND-001")
                .name("Test Vendor")
                .build();
        PageResponse<VendorDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(vendorService.searchVendors(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/vendors/search").param("q", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("VND-001"));
    }

    @Test
    void searchVendors_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/search").param("q", "Test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void searchVendors_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/search").param("q", "Test"))
                .andExpect(status().isForbidden());
    }

    // ==================== createVendor ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_MANAGE")
    void createVendor_valid_returns201() throws Exception {
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-002")
                .name("New Vendor")
                .build();

        VendorDto dto = VendorDto.builder()
                .id(2L)
                .code("VND-002")
                .name("New Vendor")
                .active(true)
                .build();
        when(vendorService.createVendor(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/inventory/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("VND-002"));
    }

    @Test
    void createVendor_unauthenticated_returns403() throws Exception {
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-002")
                .name("New Vendor")
                .build();

        mockMvc.perform(post("/api/v1/inventory/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createVendor_noPermission_returns403() throws Exception {
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-002")
                .name("New Vendor")
                .build();

        mockMvc.perform(post("/api/v1/inventory/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== updateVendor ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_MANAGE")
    void updateVendor_valid_returns200() throws Exception {
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-001")
                .name("Updated Vendor")
                .build();

        VendorDto dto = VendorDto.builder()
                .id(1L)
                .code("VND-001")
                .name("Updated Vendor")
                .build();
        when(vendorService.updateVendor(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/vendors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Vendor"));
    }

    @Test
    void updateVendor_unauthenticated_returns403() throws Exception {
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-001")
                .name("Updated Vendor")
                .build();

        mockMvc.perform(put("/api/v1/inventory/vendors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void updateVendor_noPermission_returns403() throws Exception {
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-001")
                .name("Updated Vendor")
                .build();

        mockMvc.perform(put("/api/v1/inventory/vendors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== deleteVendor ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_MANAGE")
    void deleteVendor_returns204() throws Exception {
        doNothing().when(vendorService).deleteVendor(1L);

        mockMvc.perform(delete("/api/v1/inventory/vendors/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteVendor_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/vendors/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deleteVendor_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/vendors/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== activateVendor ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_MANAGE")
    void activateVendor_returns200() throws Exception {
        VendorDto dto = VendorDto.builder()
                .id(1L)
                .code("VND-001")
                .active(true)
                .build();
        when(vendorService.activateVendor(1L)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/vendors/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void activateVendor_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/vendors/1/activate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void activateVendor_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/vendors/1/activate"))
                .andExpect(status().isForbidden());
    }

    // ==================== getVendorContacts ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_VIEW")
    void getVendorContacts_authenticated_returns200() throws Exception {
        VendorContactDto dto = VendorContactDto.builder()
                .id(1L)
                .vendorId(1L)
                .name("John Doe")
                .email("john@vendor.com")
                .build();
        when(vendorContactService.getContactsByVendor(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/vendors/1/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("John Doe"));
    }

    @Test
    void getVendorContacts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/1/contacts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getVendorContacts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/1/contacts"))
                .andExpect(status().isForbidden());
    }

    // ==================== getPrimaryContact ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_VIEW")
    void getPrimaryContact_authenticated_returns200() throws Exception {
        VendorContactDto dto = VendorContactDto.builder()
                .id(1L)
                .vendorId(1L)
                .name("Primary Contact")
                .primary(true)
                .build();
        when(vendorContactService.getPrimaryContact(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/vendors/1/contacts/primary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Primary Contact"));
    }

    @Test
    void getPrimaryContact_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/1/contacts/primary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getPrimaryContact_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/1/contacts/primary"))
                .andExpect(status().isForbidden());
    }

    // ==================== getBillingContact ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_VIEW")
    void getBillingContact_authenticated_returns200() throws Exception {
        VendorContactDto dto = VendorContactDto.builder()
                .id(2L)
                .vendorId(1L)
                .name("Billing Contact")
                .billingContact(true)
                .build();
        when(vendorContactService.getBillingContact(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/vendors/1/contacts/billing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Billing Contact"));
    }

    @Test
    void getBillingContact_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/1/contacts/billing"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getBillingContact_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/1/contacts/billing"))
                .andExpect(status().isForbidden());
    }

    // ==================== getOrderingContact ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_VIEW")
    void getOrderingContact_authenticated_returns200() throws Exception {
        VendorContactDto dto = VendorContactDto.builder()
                .id(3L)
                .vendorId(1L)
                .name("Ordering Contact")
                .orderingContact(true)
                .build();
        when(vendorContactService.getOrderingContact(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/vendors/1/contacts/ordering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Ordering Contact"));
    }

    @Test
    void getOrderingContact_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/1/contacts/ordering"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getOrderingContact_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/1/contacts/ordering"))
                .andExpect(status().isForbidden());
    }

    // ==================== getContact ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_VIEW")
    void getContact_found_returns200() throws Exception {
        VendorContactDto dto = VendorContactDto.builder()
                .id(1L)
                .vendorId(1L)
                .name("John Doe")
                .build();
        when(vendorContactService.getContact(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/vendors/contacts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("John Doe"));
    }

    @Test
    void getContact_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/contacts/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getContact_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/vendors/contacts/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== createContact ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_MANAGE")
    void createContact_valid_returns201() throws Exception {
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(1L)
                .name("New Contact")
                .email("new@vendor.com")
                .build();

        VendorContactDto dto = VendorContactDto.builder()
                .id(2L)
                .vendorId(1L)
                .name("New Contact")
                .email("new@vendor.com")
                .build();
        when(vendorContactService.createContact(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/inventory/vendors/1/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("New Contact"));
    }

    @Test
    void createContact_unauthenticated_returns403() throws Exception {
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(1L)
                .name("New Contact")
                .build();

        mockMvc.perform(post("/api/v1/inventory/vendors/1/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createContact_noPermission_returns403() throws Exception {
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(1L)
                .name("New Contact")
                .build();

        mockMvc.perform(post("/api/v1/inventory/vendors/1/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== updateContact ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_MANAGE")
    void updateContact_valid_returns200() throws Exception {
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(1L)
                .name("Updated Contact")
                .build();

        VendorContactDto dto = VendorContactDto.builder()
                .id(1L)
                .vendorId(1L)
                .name("Updated Contact")
                .build();
        when(vendorContactService.updateContact(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/vendors/contacts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Contact"));
    }

    @Test
    void updateContact_unauthenticated_returns403() throws Exception {
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(1L)
                .name("Updated Contact")
                .build();

        mockMvc.perform(put("/api/v1/inventory/vendors/contacts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void updateContact_noPermission_returns403() throws Exception {
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(1L)
                .name("Updated Contact")
                .build();

        mockMvc.perform(put("/api/v1/inventory/vendors/contacts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== deleteContact ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_MANAGE")
    void deleteContact_returns204() throws Exception {
        doNothing().when(vendorContactService).deleteContact(1L);

        mockMvc.perform(delete("/api/v1/inventory/vendors/contacts/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteContact_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/vendors/contacts/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deleteContact_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/vendors/contacts/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== setPrimaryContact ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_VENDOR_MANAGE")
    void setPrimaryContact_returns200() throws Exception {
        VendorContactDto dto = VendorContactDto.builder()
                .id(1L)
                .vendorId(1L)
                .name("John Doe")
                .primary(true)
                .build();
        when(vendorContactService.setPrimaryContact(1L)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/vendors/contacts/1/set-primary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primary").value(true));
    }

    @Test
    void setPrimaryContact_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/vendors/contacts/1/set-primary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void setPrimaryContact_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/vendors/contacts/1/set-primary"))
                .andExpect(status().isForbidden());
    }
}
