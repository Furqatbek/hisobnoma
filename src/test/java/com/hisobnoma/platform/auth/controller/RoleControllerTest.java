package com.hisobnoma.platform.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.dto.CreateRoleRequest;
import com.hisobnoma.platform.auth.dto.RoleDto;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.auth.service.RoleService;
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
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoleService roleService;

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
    void getRoles_withPermission_returns200() throws Exception {
        RoleDto dto = RoleDto.builder().id(1L).name("Admin").code("ADMIN").build();
        PageResponse<RoleDto> page = PageResponse.of(List.of(dto), 0, 20, 1);
        when(roleService.getRoles(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/roles").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].code").value("ADMIN"));
    }

    @Test
    void getRoles_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRoles_wrongPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/roles").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSystemRoles_returns200() throws Exception {
        RoleDto dto = RoleDto.builder().id(1L).name("Super Admin").code("SUPER_ADMIN").systemRole(true).build();
        when(roleService.getSystemRoles()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/roles/system").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].systemRole").value(true));
    }

    @Test
    void getRole_found_returns200() throws Exception {
        RoleDto dto = RoleDto.builder().id(1L).name("Admin").code("ADMIN").build();
        when(roleService.getRole(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/roles/1").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("ADMIN"));
    }

    @Test
    void createRole_valid_returns201() throws Exception {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("Manager").code("MANAGER").description("Manager role").build();
        RoleDto dto = RoleDto.builder().id(2L).name("Manager").code("MANAGER").build();
        when(roleService.createRole(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/roles")
                        .with(userWithPermission("ADMIN_ROLE_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("MANAGER"));
    }

    @Test
    void updateRole_valid_returns200() throws Exception {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("Updated Manager").code("MANAGER").build();
        RoleDto dto = RoleDto.builder().id(1L).name("Updated Manager").code("MANAGER").build();
        when(roleService.updateRole(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/roles/1")
                        .with(userWithPermission("ADMIN_ROLE_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Manager"));
    }

    @Test
    void deleteRole_returns200() throws Exception {
        doNothing().when(roleService).deleteRole(1L);

        mockMvc.perform(delete("/api/v1/roles/1").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().isOk());
    }

    @Test
    void assignPermissions_returns200() throws Exception {
        Set<String> permissions = Set.of("USER_READ", "USER_WRITE");
        RoleDto dto = RoleDto.builder().id(1L).name("Admin").code("ADMIN").permissions(permissions).build();
        when(roleService.assignPermissions(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/roles/1/permissions")
                        .with(userWithPermission("ADMIN_PERMISSION_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permissions)))
                .andExpect(status().isOk());
    }

    @Test
    void getAllPermissions_returns200() throws Exception {
        when(roleService.getAllPermissions()).thenReturn(List.of("USER_READ", "USER_WRITE"));

        mockMvc.perform(get("/api/v1/roles/permissions").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("USER_READ"));
    }
}
