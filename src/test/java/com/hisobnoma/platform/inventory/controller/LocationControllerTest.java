package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.CreateLocationRequest;
import com.hisobnoma.platform.inventory.dto.LocationDto;
import com.hisobnoma.platform.inventory.entity.LocationType;
import com.hisobnoma.platform.inventory.service.LocationService;
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
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LocationService locationService;

    // ==================== getAllLocations ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getAllLocations_authenticated_returns200() throws Exception {
        LocationDto dto = LocationDto.builder()
                .id(1L)
                .code("LOC-001")
                .name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .active(true)
                .build();
        when(locationService.getAllLocations()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("LOC-001"));
    }

    @Test
    void getAllLocations_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAllLocations_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations"))
                .andExpect(status().isForbidden());
    }

    // ==================== getLocationsPaginated ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getLocationsPaginated_authenticated_returns200() throws Exception {
        LocationDto dto = LocationDto.builder()
                .id(1L)
                .code("LOC-001")
                .name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .build();
        PageResponse<LocationDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(locationService.getLocations(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/locations/paginated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("LOC-001"));
    }

    @Test
    void getLocationsPaginated_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/paginated"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getLocationsPaginated_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/paginated"))
                .andExpect(status().isForbidden());
    }

    // ==================== getActiveLocations ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getActiveLocations_authenticated_returns200() throws Exception {
        LocationDto dto = LocationDto.builder()
                .id(1L)
                .code("LOC-001")
                .name("Main Warehouse")
                .active(true)
                .build();
        when(locationService.getActiveLocations()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/locations/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getActiveLocations_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/active"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getActiveLocations_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/active"))
                .andExpect(status().isForbidden());
    }

    // ==================== getLocationTree ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getLocationTree_authenticated_returns200() throws Exception {
        LocationDto dto = LocationDto.builder()
                .id(1L)
                .code("LOC-001")
                .name("Main Warehouse")
                .build();
        when(locationService.getLocationTree()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/locations/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Main Warehouse"));
    }

    @Test
    void getLocationTree_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/tree"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getLocationTree_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/tree"))
                .andExpect(status().isForbidden());
    }

    // ==================== getRootLocations ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getRootLocations_authenticated_returns200() throws Exception {
        LocationDto dto = LocationDto.builder()
                .id(1L)
                .code("LOC-001")
                .name("Root Location")
                .build();
        when(locationService.getRootLocations()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/locations/roots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Root Location"));
    }

    @Test
    void getRootLocations_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/roots"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getRootLocations_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/roots"))
                .andExpect(status().isForbidden());
    }

    // ==================== getLocationsByType ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getLocationsByType_authenticated_returns200() throws Exception {
        LocationDto dto = LocationDto.builder()
                .id(1L)
                .code("LOC-001")
                .name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .build();
        when(locationService.getLocationsByType(LocationType.WAREHOUSE)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/locations/type/WAREHOUSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("LOC-001"));
    }

    @Test
    void getLocationsByType_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/type/WAREHOUSE"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getLocationsByType_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/type/WAREHOUSE"))
                .andExpect(status().isForbidden());
    }

    // ==================== getWarehouseAndStoreLocations ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getWarehouseAndStoreLocations_authenticated_returns200() throws Exception {
        LocationDto dto = LocationDto.builder()
                .id(1L)
                .code("LOC-001")
                .name("Main Warehouse")
                .build();
        when(locationService.getWarehouseAndStoreLocations()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/locations/warehouse-store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Main Warehouse"));
    }

    @Test
    void getWarehouseAndStoreLocations_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/warehouse-store"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getWarehouseAndStoreLocations_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/warehouse-store"))
                .andExpect(status().isForbidden());
    }

    // ==================== getDefaultLocation ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getDefaultLocation_authenticated_returns200() throws Exception {
        LocationDto dto = LocationDto.builder()
                .id(1L)
                .code("LOC-001")
                .name("Default Location")
                .isDefault(true)
                .build();
        when(locationService.getDefaultLocation()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/locations/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Default Location"));
    }

    @Test
    void getDefaultLocation_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/default"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getDefaultLocation_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/default"))
                .andExpect(status().isForbidden());
    }

    // ==================== getLocation ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getLocation_found_returns200() throws Exception {
        LocationDto dto = LocationDto.builder()
                .id(1L)
                .code("LOC-001")
                .name("Main Warehouse")
                .build();
        when(locationService.getLocation(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOC-001"));
    }

    @Test
    void getLocation_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getLocation_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getLocationByCode ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getLocationByCode_found_returns200() throws Exception {
        LocationDto dto = LocationDto.builder()
                .id(1L)
                .code("LOC-001")
                .name("Main Warehouse")
                .build();
        when(locationService.getLocationByCode("LOC-001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/locations/code/LOC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOC-001"));
    }

    @Test
    void getLocationByCode_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/code/LOC-001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getLocationByCode_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/code/LOC-001"))
                .andExpect(status().isForbidden());
    }

    // ==================== getChildLocations ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getChildLocations_authenticated_returns200() throws Exception {
        LocationDto dto = LocationDto.builder()
                .id(2L)
                .code("LOC-002")
                .name("Zone A")
                .parentLocationId(1L)
                .build();
        when(locationService.getChildLocations(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/locations/1/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("LOC-002"));
    }

    @Test
    void getChildLocations_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/1/children"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getChildLocations_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/1/children"))
                .andExpect(status().isForbidden());
    }

    // ==================== searchLocations ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void searchLocations_authenticated_returns200() throws Exception {
        LocationDto dto = LocationDto.builder()
                .id(1L)
                .code("LOC-001")
                .name("Main Warehouse")
                .build();
        PageResponse<LocationDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(locationService.searchLocations(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/locations/search").param("q", "Main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("LOC-001"));
    }

    @Test
    void searchLocations_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/search").param("q", "Main"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void searchLocations_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations/search").param("q", "Main"))
                .andExpect(status().isForbidden());
    }

    // ==================== createLocation ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_LOCATION_MANAGE")
    void createLocation_valid_returns201() throws Exception {
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("LOC-002")
                .name("New Location")
                .locationType(LocationType.WAREHOUSE)
                .active(true)
                .build();

        LocationDto dto = LocationDto.builder()
                .id(2L)
                .code("LOC-002")
                .name("New Location")
                .locationType(LocationType.WAREHOUSE)
                .active(true)
                .build();
        when(locationService.createLocation(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/inventory/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("LOC-002"));
    }

    @Test
    void createLocation_unauthenticated_returns403() throws Exception {
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("LOC-002")
                .name("New Location")
                .locationType(LocationType.WAREHOUSE)
                .build();

        mockMvc.perform(post("/api/v1/inventory/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createLocation_noPermission_returns403() throws Exception {
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("LOC-002")
                .name("New Location")
                .locationType(LocationType.WAREHOUSE)
                .build();

        mockMvc.perform(post("/api/v1/inventory/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== updateLocation ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_LOCATION_MANAGE")
    void updateLocation_valid_returns200() throws Exception {
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("LOC-001")
                .name("Updated Location")
                .locationType(LocationType.WAREHOUSE)
                .build();

        LocationDto dto = LocationDto.builder()
                .id(1L)
                .code("LOC-001")
                .name("Updated Location")
                .build();
        when(locationService.updateLocation(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Location"));
    }

    @Test
    void updateLocation_unauthenticated_returns403() throws Exception {
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("LOC-001")
                .name("Updated Location")
                .locationType(LocationType.WAREHOUSE)
                .build();

        mockMvc.perform(put("/api/v1/inventory/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void updateLocation_noPermission_returns403() throws Exception {
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("LOC-001")
                .name("Updated Location")
                .locationType(LocationType.WAREHOUSE)
                .build();

        mockMvc.perform(put("/api/v1/inventory/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== deleteLocation ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_LOCATION_MANAGE")
    void deleteLocation_returns204() throws Exception {
        doNothing().when(locationService).deleteLocation(1L);

        mockMvc.perform(delete("/api/v1/inventory/locations/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteLocation_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/locations/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deleteLocation_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/locations/1"))
                .andExpect(status().isForbidden());
    }
}
