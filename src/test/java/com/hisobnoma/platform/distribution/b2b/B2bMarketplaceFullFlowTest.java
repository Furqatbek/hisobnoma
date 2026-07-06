package com.hisobnoma.platform.distribution.b2b;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end for the public B2B marketplace: exercises the anonymous whitelist, tenant
 * resolution from X-Tenant-ID, the dedicated B2B token (issue + validate), server-side
 * pricing and DRAFT order creation — the full HTTP stack the service unit tests don't touch.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class B2bMarketplaceFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;

    private String tenantId;
    private Long productId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("B2B Tenant").code("B2B_FULLFLOW").active(true)
                .maxUsers(50).maxLocations(10).build());
        this.tenantId = String.valueOf(tenant.getId());

        Customer customer = new Customer();
        customer.setCode("WH-001");
        customer.setName("Osiyo Wholesale");
        customer.setPhone("+998 90 111-22-33");
        customer.setDefaultCurrency("UZS");
        customer.setTenantId(tenant.getId());
        customerRepository.saveAndFlush(customer);

        UnitOfMeasure uom = UnitOfMeasure.builder().code("PCS").name("Pieces").build();
        uom.setTenantId(tenant.getId());
        uom = uomRepository.saveAndFlush(uom);

        Product product = Product.builder()
                .sku("SKU-1").name("Cola 1L").sellingPrice(new BigDecimal("12000"))
                .baseUom(uom).active(true).sellable(true).build();
        product.setTenantId(tenant.getId());
        this.productId = productRepository.saveAndFlush(product).getId();
    }

    private String login(String code, String phone) throws Exception {
        String body = mockMvc.perform(post("/api/v1/b2b/auth/login")
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    @Test
    void fullFlow_loginBrowseCatalogPlaceOrder() throws Exception {
        String token = login("WH-001", "998901112233"); // different phone format, digits match

        // Catalog priced for the buyer
        mockMvc.perform(get("/api/v1/b2b/catalog")
                        .header("X-Tenant-ID", tenantId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value(productId))
                .andExpect(jsonPath("$.content[0].price").value(12000));

        // Place an order -> DRAFT distribution order, priced server-side
        mockMvc.perform(post("/api/v1/b2b/orders")
                        .header("X-Tenant-ID", tenantId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryAddress\":\"Chilonzor 5\",\"lines\":[{\"productId\":" + productId + ",\"quantity\":4}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.customerName").value("Osiyo Wholesale"))
                .andExpect(jsonPath("$.totalAmount").value(48000)) // 4 * 12000
                .andExpect(jsonPath("$.orderNumber", org.hamcrest.Matchers.startsWith("DO")));

        // The order is now listed under the buyer
        mockMvc.perform(get("/api/v1/b2b/orders")
                        .header("X-Tenant-ID", tenantId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].totalAmount").value(48000));
    }

    @Test
    void login_wrongPhone_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/b2b/auth/login")
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"WH-001\",\"phone\":\"998900000000\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void catalog_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/b2b/catalog")
                        .header("X-Tenant-ID", tenantId)
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }
}
