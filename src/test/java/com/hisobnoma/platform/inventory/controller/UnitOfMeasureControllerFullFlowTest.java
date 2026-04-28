package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.dto.CreateUomRequest;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UnitOfMeasureControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/inventory/uom";

    private Tenant tenant;
    private User user;
    private UnitOfMeasure pieceUom;
    private UnitOfMeasure boxUom;
    private UnitOfMeasure inactiveUom;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("UOM Test Tenant").code("FULLFLOW_UOM").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("uomtestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        pieceUom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS").name("Pieces").symbol("pcs")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(true).tenantId(tenant.getId()).build());

        boxUom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("BOX").name("Box of 12").symbol("box")
                .isBaseUnit(false).baseUom(pieceUom)
                .conversionFactor(new BigDecimal("12"))
                .active(true).tenantId(tenant.getId()).build());

        inactiveUom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("OLD").name("Old Unit")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(false).tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor readAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("INVENTORY_PRODUCT_READ"),
                        new SimpleGrantedAuthority("INVENTORY_PRODUCT_CREATE"),
                        new SimpleGrantedAuthority("INVENTORY_PRODUCT_UPDATE"),
                        new SimpleGrantedAuthority("INVENTORY_PRODUCT_DELETE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("SOME_OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET / ----

    @Test
    void getAllUoms_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[?(@.code == 'PCS')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'BOX')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'OLD')]").exists());
    }

    // ---- GET /paginated ----

    @Test
    void getUomsPaginated_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/paginated").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(3)));
    }

    // ---- GET /active ----

    @Test
    void getActiveUoms_returnsOnlyActive() throws Exception {
        mockMvc.perform(get(BASE_URL + "/active").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.code == 'PCS')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'BOX')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'OLD')]").doesNotExist());
    }

    // ---- GET /base ----

    @Test
    void getBaseUoms_returnsOnlyBaseUnits() throws Exception {
        mockMvc.perform(get(BASE_URL + "/base").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.code == 'PCS')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'BOX')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.code == 'OLD')]").doesNotExist());
    }

    // ---- GET /{id} ----

    @Test
    void getUomById_returnsUom() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + pieceUom.getId()).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pieceUom.getId()))
                .andExpect(jsonPath("$.code").value("PCS"))
                .andExpect(jsonPath("$.name").value("Pieces"))
                .andExpect(jsonPath("$.symbol").value("pcs"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.baseUnit").value(true))
                .andExpect(jsonPath("$.conversionFactor").value(1));
    }

    @Test
    void getUomById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(readAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /code/{code} ----

    @Test
    void getUomByCode_returnsUom() throws Exception {
        mockMvc.perform(get(BASE_URL + "/code/PCS").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PCS"))
                .andExpect(jsonPath("$.name").value("Pieces"));
    }

    // ---- GET /search ----

    @Test
    void searchUoms_returnsPaginatedResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("q", "Piece").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].code").value("PCS"))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ---- POST / ----

    @Test
    void createUom_baseUnit_returnsCreated() throws Exception {
        CreateUomRequest request = CreateUomRequest.builder()
                .code("KG").name("Kilograms").symbol("kg")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(true).build();

        mockMvc.perform(post(BASE_URL).with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("KG"))
                .andExpect(jsonPath("$.name").value("Kilograms"))
                .andExpect(jsonPath("$.symbol").value("kg"))
                .andExpect(jsonPath("$.baseUnit").value(true))
                .andExpect(jsonPath("$.conversionFactor").value(1))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createUom_derivedUnit_withBaseUom_returnsCreated() throws Exception {
        CreateUomRequest request = CreateUomRequest.builder()
                .code("DZN").name("Dozen").symbol("dzn")
                .isBaseUnit(false).baseUomId(pieceUom.getId())
                .conversionFactor(new BigDecimal("12"))
                .active(true).build();

        mockMvc.perform(post(BASE_URL).with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DZN"))
                .andExpect(jsonPath("$.name").value("Dozen"))
                .andExpect(jsonPath("$.baseUnit").value(false))
                .andExpect(jsonPath("$.baseUomId").value(pieceUom.getId()))
                .andExpect(jsonPath("$.baseUomCode").value("PCS"))
                .andExpect(jsonPath("$.baseUomName").value("Pieces"))
                .andExpect(jsonPath("$.conversionFactor").value(12));
    }

    @Test
    void createUom_duplicateCode_returnsBadRequest() throws Exception {
        CreateUomRequest request = CreateUomRequest.builder()
                .code("PCS").name("Duplicate Pieces").symbol("pcs")
                .isBaseUnit(true).active(true).build();

        mockMvc.perform(post(BASE_URL).with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ---- PUT /{id} ----

    @Test
    void updateUom_validRequest_returnsUpdated() throws Exception {
        CreateUomRequest request = CreateUomRequest.builder()
                .code("PCS").name("Updated Pieces").symbol("pc")
                .description("Updated description")
                .conversionFactor(BigDecimal.ONE)
                .active(true).isBaseUnit(true).build();

        mockMvc.perform(put(BASE_URL + "/" + pieceUom.getId()).with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pieceUom.getId()))
                .andExpect(jsonPath("$.name").value("Updated Pieces"))
                .andExpect(jsonPath("$.symbol").value("pc"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.code").value("PCS"));
    }

    // ---- DELETE /{id} ----

    @Test
    void deleteUom_noDependent_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + inactiveUom.getId()).with(readAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUom_withDependent_returnsBadRequest() throws Exception {
        // pieceUom is used as baseUom by boxUom, so deletion should fail
        mockMvc.perform(delete(BASE_URL + "/" + pieceUom.getId()).with(readAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /convert ----

    @Test
    void convertUnits_returnsResult() throws Exception {
        // Convert 100 pieces to boxes: 100 / 12 = 8.333333
        mockMvc.perform(get(BASE_URL + "/convert")
                        .param("quantity", "100")
                        .param("fromUomId", String.valueOf(pieceUom.getId()))
                        .param("toUomId", String.valueOf(boxUom.getId()))
                        .with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isNumber());
    }

    // ---- Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/paginated").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/active").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/base").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + pieceUom.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/code/PCS").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/search").param("q", "PCS").with(noPermAuth()))
                .andExpect(status().isForbidden());

        CreateUomRequest request = CreateUomRequest.builder()
                .code("NOPE").name("No Permission").build();

        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + pieceUom.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/" + pieceUom.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/convert")
                        .param("quantity", "10")
                        .param("fromUomId", String.valueOf(pieceUom.getId()))
                        .param("toUomId", String.valueOf(boxUom.getId()))
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
