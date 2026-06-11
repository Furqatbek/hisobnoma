package com.hisobnoma.platform.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.dto.RequestOtpRequest;
import com.hisobnoma.platform.web.dto.VerifyOtpRequest;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebWishlistItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebWishlistFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private WebCustomerRepository customerRepository;
    @Autowired private WebCatalogItemRepository catalogRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private WebWishlistItemRepository wishlistRepository;

    @MockBean private SmsService smsService;

    private static final AtomicLong SEQ = new AtomicLong(8000000);
    private static final String H = "X-Tenant-ID";

    private Tenant tenant;
    private User adminUser;
    private WebCatalogItem catalogItem;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Wishlist Tenant").code("FULLFLOW_WISH").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("wishadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .name("шт").code("PCS").tenantId(tenant.getId()).build());

        Product product = productRepository.saveAndFlush(Product.builder()
                .name("Coca-Cola 1.5л")
                .sku("COKE-15")
                .sellingPrice(BigDecimal.valueOf(12000))
                .costPrice(BigDecimal.valueOf(8000))
                .active(true)
                .sellable(true)
                .trackInventory(false)
                .tenantId(tenant.getId())
                .baseUom(uom)
                .build());

        catalogItem = catalogRepository.saveAndFlush(WebCatalogItem.builder()
                .product(product)
                .tenantId(tenant.getId())
                .status(WebCatalogStatus.LIVE)
                .sortOrder(1)
                .build());
    }

    private String uniquePhone() { return "+99890" + SEQ.incrementAndGet(); }
    private String uniqueIp() { return "10.9." + (SEQ.incrementAndGet() % 250) + "." + (SEQ.incrementAndGet() % 250); }

    private String loginAndGetToken(String phone, String name) throws Exception {
        mockMvc.perform(post("/api/v1/web/auth/request-otp")
                        .header(H, tenant.getId())
                        .header("X-Forwarded-For", uniqueIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestOtpRequest(phone))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(smsService, atLeastOnce()).sendSmsAsync(anyString(), cap.capture());
        String code = cap.getValue().replaceAll("[^0-9]", "");

        VerifyOtpRequest req = VerifyOtpRequest.builder()
                .phone(phone).code(code).name(name).build();

        MvcResult result = mockMvc.perform(post("/api/v1/web/auth/verify")
                        .header(H, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/token").asText();
    }

    private RequestPostProcessor staffAuth(String... permissions) {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true,
                java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new)
                        .map(a -> (org.springframework.security.core.GrantedAuthority) a).toList());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    @Test
    void wishlist_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/web/me/wishlist")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/web/me/wishlist/" + catalogItem.getId())
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void likeAndUnlike_fullFlow() throws Exception {
        String phone = uniquePhone();
        String token = loginAndGetToken(phone, "Wishlist User");

        // Like
        mockMvc.perform(put("/api/v1/web/me/wishlist/" + catalogItem.getId())
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Wishlist ids contains the item
        mockMvc.perform(get("/api/v1/web/me/wishlist/ids")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasItem(catalogItem.getId().intValue())));

        // Wishlist page contains the item
        mockMvc.perform(get("/api/v1/web/me/wishlist")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].catalogItemId").value(catalogItem.getId()))
                .andExpect(jsonPath("$.content[0].name").value("Coca-Cola 1.5л"))
                .andExpect(jsonPath("$.content[0].available").value(true));

        // Unlike
        mockMvc.perform(delete("/api/v1/web/me/wishlist/" + catalogItem.getId())
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Wishlist ids is now empty
        mockMvc.perform(get("/api/v1/web/me/wishlist/ids")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void like_idempotent() throws Exception {
        String phone = uniquePhone();
        String token = loginAndGetToken(phone, "Idempotent");

        // Like twice
        mockMvc.perform(put("/api/v1/web/me/wishlist/" + catalogItem.getId())
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/web/me/wishlist/" + catalogItem.getId())
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Only one entry in ids
        mockMvc.perform(get("/api/v1/web/me/wishlist/ids")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void staffSees_likeCount_andMostWished() throws Exception {
        String phone = uniquePhone();
        String token = loginAndGetToken(phone, "Liker");

        // Like an item
        mockMvc.perform(put("/api/v1/web/me/wishlist/" + catalogItem.getId())
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Staff sees like count on catalog items
        mockMvc.perform(get("/api/v1/web-catalog")
                        .with(staffAuth("WEB_CATALOG_VIEW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].likeCount").value(1));

        // Staff sees most-wished endpoint
        mockMvc.perform(get("/api/v1/web-catalog/most-wished")
                        .with(staffAuth("WEB_CATALOG_VIEW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productName").value("Coca-Cola 1.5л"))
                .andExpect(jsonPath("$.data[0].likeCount").value(1));
    }

    @Test
    void staffSees_customerWishlistCount() throws Exception {
        String phone = uniquePhone();
        String token = loginAndGetToken(phone, "Counter");

        mockMvc.perform(put("/api/v1/web/me/wishlist/" + catalogItem.getId())
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        WebCustomer customer = customerRepository.findByTenantIdAndPhone(
                tenant.getId(), phone.replace("+", "")).orElseThrow();

        mockMvc.perform(get("/api/v1/web-customers/" + customer.getId())
                        .with(staffAuth("WEB_CUSTOMER_VIEW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wishlistCount").value(1));
    }

    @Test
    void mostWished_requiresPermission() throws Exception {
        mockMvc.perform(get("/api/v1/web-catalog/most-wished"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerScoping_wishlistsAreIsolated() throws Exception {
        String phoneA = uniquePhone();
        String phoneB = uniquePhone();
        String tokenA = loginAndGetToken(phoneA, "UserA");
        String tokenB = loginAndGetToken(phoneB, "UserB");

        // A likes the item
        mockMvc.perform(put("/api/v1/web/me/wishlist/" + catalogItem.getId())
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // B's wishlist is empty
        mockMvc.perform(get("/api/v1/web/me/wishlist/ids")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // A's wishlist has the item
        mockMvc.perform(get("/api/v1/web/me/wishlist/ids")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }
}
