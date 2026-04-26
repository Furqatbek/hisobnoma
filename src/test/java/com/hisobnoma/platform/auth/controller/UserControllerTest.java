package com.hisobnoma.platform.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.dto.CreateUserRequest;
import com.hisobnoma.platform.auth.dto.UpdateUserRequest;
import com.hisobnoma.platform.auth.dto.UserDto;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.auth.service.UserService;
import com.hisobnoma.platform.common.dto.PageResponse;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private RequestPostProcessor userWithPermission(String... permissions) {
        UserPrincipal principal = new UserPrincipal(
                1L, "admin", "pw", 1L, true, true,
                Arrays.stream(permissions)
                        .map(SimpleGrantedAuthority::new)
                        .toList());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    @Test
    void getUsers_withPermission_returns200() throws Exception {
        UserDto dto = UserDto.builder().id(1L).username("admin").firstName("Admin").build();
        PageResponse<UserDto> page = PageResponse.of(List.of(dto), 0, 20, 1);
        when(userService.getUsers(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/users").with(userWithPermission("ADMIN_USER_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("admin"));
    }

    @Test
    void getUsers_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsers_wrongPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsers_withSearch_returns200() throws Exception {
        UserDto dto = UserDto.builder().id(1L).username("admin").build();
        PageResponse<UserDto> page = PageResponse.of(List.of(dto), 0, 20, 1);
        when(userService.getUsers(eq("admin"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/users").param("search", "admin")
                        .with(userWithPermission("ADMIN_USER_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("admin"));
    }

    @Test
    void getUser_found_returns200() throws Exception {
        UserDto dto = UserDto.builder().id(1L).username("admin").build();
        when(userService.getUser(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/users/1").with(userWithPermission("ADMIN_USER_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void createUser_valid_returns201() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser").password("password123").firstName("New").lastName("User").build();
        UserDto dto = UserDto.builder().id(2L).username("newuser").firstName("New").lastName("User").build();
        when(userService.createUser(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/users")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    void updateUser_valid_returns200() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Updated").lastName("User").build();
        UserDto dto = UserDto.builder().id(1L).username("admin").firstName("Updated").build();
        when(userService.updateUser(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/users/1")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Updated"));
    }

    @Test
    void deleteUser_returns200() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/v1/users/1").with(userWithPermission("ADMIN_USER_MANAGE")))
                .andExpect(status().isOk());
    }

    @Test
    void assignRoles_returns200() throws Exception {
        Set<String> roles = Set.of("ADMIN", "MANAGER");
        UserDto dto = UserDto.builder().id(1L).username("admin").roles(roles).build();
        when(userService.assignRoles(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/users/1/roles")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roles)))
                .andExpect(status().isOk());
    }

    @Test
    void lockUser_returns200() throws Exception {
        Map<String, Boolean> request = Map.of("locked", true);
        UserDto dto = UserDto.builder().id(1L).username("admin").locked(true).build();
        when(userService.lockUser(eq(1L), eq(true))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/users/1/lock")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locked").value(true));
    }

    @Test
    void resetPassword_returns200() throws Exception {
        Map<String, String> request = Map.of("password", "newpassword123");
        doNothing().when(userService).resetPassword(eq(1L), eq("newpassword123"));

        mockMvc.perform(put("/api/v1/users/1/reset-password")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void setUserPin_returns200() throws Exception {
        Map<String, String> request = Map.of("pin", "1234");
        doNothing().when(userService).setUserPin(eq(1L), eq("1234"));

        mockMvc.perform(put("/api/v1/users/1/set-pin")
                        .with(userWithPermission("ADMIN_USER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
