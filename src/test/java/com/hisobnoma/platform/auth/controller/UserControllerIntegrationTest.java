package com.hisobnoma.platform.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.dto.CreateUserRequest;
import com.hisobnoma.platform.auth.dto.UpdateUserRequest;
import com.hisobnoma.platform.auth.dto.UserDto;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.auth.service.UserService;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private static final Long CURRENT_USER_ID = 99L;
    private static final Long TENANT_ID = 1L;

    /**
     * Returns a RequestPostProcessor that authenticates as a UserPrincipal with the given authorities.
     * Needed because @WithMockUser creates a generic User, but SecurityContextHelper requires UserPrincipal.
     */
    private RequestPostProcessor userWithPermission(String... permissions) {
        UserPrincipal principal = new UserPrincipal(
                CURRENT_USER_ID, "admin", "pw", TENANT_ID, true, true,
                java.util.Arrays.stream(permissions)
                        .map(SimpleGrantedAuthority::new)
                        .toList());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private UserDto sampleDto() {
        return UserDto.builder()
                .id(10L)
                .username("john")
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .build();
    }

    private PageResponse<UserDto> samplePage() {
        return PageResponse.<UserDto>builder()
                .content(List.of(sampleDto()))
                .page(PageResponse.PageMetadata.builder()
                        .number(0).size(20).totalElements(1).totalPages(1)
                        .first(true).last(true).empty(false).build())
                .build();
    }

    // ---- GET /api/v1/users ----

    @Test
    void getUsers_adminAuthenticated_returns200Paginated() throws Exception {
        when(userService.getUsers(any(), any())).thenReturn(samplePage());

        mockMvc.perform(get("/api/v1/users").with(userWithPermission("ADMIN_USER_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    void getUsers_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(userWithPermission("SOMETHING_ELSE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsers_withSearchFilter_returnsFilteredList() throws Exception {
        when(userService.getUsers(eq("john"), any())).thenReturn(samplePage());

        mockMvc.perform(get("/api/v1/users").param("search", "john")
                        .with(userWithPermission("ADMIN_USER_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("john"));
    }

    // ---- GET /api/v1/users/{id} ----

    @Test
    void getUser_foundById_returns200() throws Exception {
        when(userService.getUser(10L)).thenReturn(sampleDto());

        mockMvc.perform(get("/api/v1/users/10").with(userWithPermission("ADMIN_USER_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.username").value("john"));
    }

    @Test
    void getUser_notFound_returns404() throws Exception {
        when(userService.getUser(999L)).thenThrow(new NotFoundException("User", 999L));

        mockMvc.perform(get("/api/v1/users/999").with(userWithPermission("ADMIN_USER_MANAGE")))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/v1/users ----

    @Test
    void createUser_validRequest_returns201() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .password("password123")
                .build();
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(sampleDto());

        mockMvc.perform(post("/api/v1/users")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void createUser_duplicateUsername_returns409() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("john")
                .password("password123")
                .build();
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new DuplicateResourceException("User", "username", "john"));

        mockMvc.perform(post("/api/v1/users")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_invalidRequest_returns400() throws Exception {
        // missing required fields
        mockMvc.perform(post("/api/v1/users")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/users/{id} ----

    @Test
    void updateUser_validRequest_returns200() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder().firstName("Jane").build();
        when(userService.updateUser(eq(10L), any(UpdateUserRequest.class))).thenReturn(sampleDto());

        mockMvc.perform(put("/api/v1/users/10")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateUser_notFound_returns404() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder().firstName("X").build();
        when(userService.updateUser(eq(999L), any())).thenThrow(new NotFoundException("User", 999L));

        mockMvc.perform(put("/api/v1/users/999")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /api/v1/users/{id} ----

    @Test
    void deleteUser_validId_returns200() throws Exception {
        // Controller returns 200 with success message, not 204
        doNothing().when(userService).deleteUser(10L);

        mockMvc.perform(delete("/api/v1/users/10").with(userWithPermission("ADMIN_USER_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteUser_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("User", 999L)).when(userService).deleteUser(999L);

        mockMvc.perform(delete("/api/v1/users/999").with(userWithPermission("ADMIN_USER_MANAGE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_selfDelete_returns400() throws Exception {
        // Service throws ValidationException, which maps to 400 in global handler
        doThrow(new ValidationException("Cannot delete your own account"))
                .when(userService).deleteUser(CURRENT_USER_ID);

        mockMvc.perform(delete("/api/v1/users/" + CURRENT_USER_ID)
                        .with(userWithPermission("ADMIN_USER_MANAGE")))
                .andExpect(status().is4xxClientError());
    }

    // ---- PUT /api/v1/users/{id}/roles ----

    @Test
    void assignRoles_validRequest_returns200() throws Exception {
        when(userService.assignRoles(eq(10L), any())).thenReturn(sampleDto());

        mockMvc.perform(put("/api/v1/users/10/roles")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Set.of("CASHIER", "MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    // ---- PUT /api/v1/users/{id}/lock ----

    @Test
    void lockUser_lockSuccess_returns200() throws Exception {
        when(userService.lockUser(eq(10L), eq(true))).thenReturn(sampleDto());

        mockMvc.perform(put("/api/v1/users/10/lock")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("locked", true))))
                .andExpect(status().isOk());
    }

    @Test
    void lockUser_notFound_returns404() throws Exception {
        when(userService.lockUser(eq(999L), anyBoolean()))
                .thenThrow(new NotFoundException("User", 999L));

        mockMvc.perform(put("/api/v1/users/999/lock")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("locked", true))))
                .andExpect(status().isNotFound());
    }

    // ---- PUT /api/v1/users/{id}/reset-password ----

    @Test
    void resetPasswordAdmin_success_returns200() throws Exception {
        doNothing().when(userService).resetPassword(eq(10L), anyString());

        mockMvc.perform(put("/api/v1/users/10/reset-password")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "newPass"))))
                .andExpect(status().isOk());
    }

    // ---- PUT /api/v1/users/{id}/set-pin ----

    @Test
    void setUserPin_validPin_returns200() throws Exception {
        doNothing().when(userService).setUserPin(eq(10L), eq("1234"));

        mockMvc.perform(put("/api/v1/users/10/set-pin")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "1234"))))
                .andExpect(status().isOk());
    }

    @Test
    void setUserPin_invalidFormat_returns400() throws Exception {
        doThrow(new ValidationException("PIN must be 4-6 digits"))
                .when(userService).setUserPin(eq(10L), eq("12"));

        mockMvc.perform(put("/api/v1/users/10/set-pin")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "12"))))
                .andExpect(status().is4xxClientError());
    }
}
