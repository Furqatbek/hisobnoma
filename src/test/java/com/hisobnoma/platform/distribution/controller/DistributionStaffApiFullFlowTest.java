package com.hisobnoma.platform.distribution.controller;

import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack security + serialization coverage for the remaining staff distribution controllers
 * (orders, van loadouts, routes, visits, agent-targets, KPI). Verifies each controller's
 * {@code @PreAuthorize} wiring — VIEW can read, VIEW-only is forbidden from mutating — plus a
 * data-seeded happy path for the core order-create flow over HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DistributionStaffApiFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;

    private Tenant tenant;
    private Long customerId;
    private Long productId;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Staff Dist Tenant").code("DIST_STAFF_FF").active(true)
                .maxUsers(50).maxLocations(10).build());

        Customer c = new Customer();
        c.setCode("C-1");
        c.setName("Osiyo");
        c.setTenantId(tenant.getId());
        customerId = customerRepository.saveAndFlush(c).getId();

        UnitOfMeasure uom = UnitOfMeasure.builder().code("PCS").name("Pieces").build();
        uom.setTenantId(tenant.getId());
        uom = uomRepository.saveAndFlush(uom);

        Product p = Product.builder().sku("SKU-1").name("Cola").sellingPrice(new BigDecimal("10000"))
                .baseUom(uom).active(true).sellable(true).build();
        p.setTenantId(tenant.getId());
        productId = productRepository.saveAndFlush(p).getId();
    }

    private RequestPostProcessor auth(String... authorities) {
        List<GrantedAuthority> granted = Arrays.stream(authorities)
                .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a)).toList();
        UserPrincipal principal = new UserPrincipal(1L, "staff", "x", tenant.getId(), true, true, granted);
        Authentication a = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(a);
    }

    // ---- core happy path over HTTP: create an order, then read it back ----

    @Test
    void orderCreate_withCreateAuthority_returnsDraftPricedServerSide() throws Exception {
        String body = "{\"customerId\":" + customerId + ",\"lines\":[{\"productId\":" + productId + ",\"quantity\":2}]}";
        mockMvc.perform(post("/api/v1/distribution/orders")
                        .with(auth("DISTRIBUTION_ORDER_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(20000)) // 2 * 10000, priced server-side
                .andExpect(jsonPath("$.orderNumber", org.hamcrest.Matchers.startsWith("DO")));

        mockMvc.perform(get("/api/v1/distribution/orders").with(auth("DISTRIBUTION_ORDER_VIEW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].totalAmount").value(20000));
    }

    // ---- security matrix: VIEW can read, VIEW-only cannot mutate ----

    @Test
    void orders_viewReadsManageGuardsMutation() throws Exception {
        mockMvc.perform(get("/api/v1/distribution/orders").with(auth("DISTRIBUTION_ORDER_VIEW")))
                .andExpect(status().isOk());
        // valid body so @Valid passes and the @PreAuthorize(CREATE) guard is what rejects
        mockMvc.perform(post("/api/v1/distribution/orders")
                        .with(auth("DISTRIBUTION_ORDER_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"lines\":[{\"productId\":1,\"quantity\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void vanLoadouts_viewReadsManageGuardsMutation() throws Exception {
        mockMvc.perform(get("/api/v1/distribution/van-loadouts").with(auth("DISTRIBUTION_VAN_VIEW")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/distribution/van-loadouts")
                        .with(auth("DISTRIBUTION_VAN_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":1,\"vehicleLocationId\":1,\"sourceLocationId\":1,\"lines\":[{\"productId\":1,\"quantityLoaded\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void routes_viewReadsManageGuardsMutation() throws Exception {
        mockMvc.perform(get("/api/v1/distribution/routes").with(auth("DISTRIBUTION_ROUTE_VIEW")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/distribution/routes")
                        .with(auth("DISTRIBUTION_ROUTE_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"R1\",\"name\":\"Route 1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void visits_viewReadsManageGuardsMutation() throws Exception {
        mockMvc.perform(get("/api/v1/distribution/visits").with(auth("DISTRIBUTION_VISIT_VIEW")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/distribution/visits/check-in")
                        .with(auth("DISTRIBUTION_VISIT_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":1,\"customerId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentTargets_viewReadsManageGuardsMutation() throws Exception {
        mockMvc.perform(get("/api/v1/distribution/agent-targets/by-agent/1").with(auth("DISTRIBUTION_KPI_VIEW")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/distribution/agent-targets")
                        .with(auth("DISTRIBUTION_KPI_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":1,\"periodStart\":\"2026-07-01\",\"periodEnd\":\"2026-07-31\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void kpiDashboard_readableWithViewAuthority() throws Exception {
        mockMvc.perform(get("/api/v1/distribution/kpi/dashboard")
                        .param("from", "2026-07-01").param("to", "2026-07-31")
                        .with(auth("DISTRIBUTION_KPI_VIEW")))
                .andExpect(status().isOk());
    }

    @Test
    void anyStaffEndpoint_unauthenticated_isDenied() throws Exception {
        mockMvc.perform(get("/api/v1/distribution/orders")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/distribution/kpi/dashboard")
                .param("from", "2026-07-01").param("to", "2026-07-31")).andExpect(status().isForbidden());
    }
}
