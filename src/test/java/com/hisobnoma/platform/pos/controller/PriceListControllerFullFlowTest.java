package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.inventory.entity.Brand;
import com.hisobnoma.platform.inventory.entity.Category;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.repository.BrandRepository;
import com.hisobnoma.platform.inventory.repository.CategoryRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import com.hisobnoma.platform.pos.dto.CreatePriceListItemRequest;
import com.hisobnoma.platform.pos.dto.CreatePriceListRequest;
import com.hisobnoma.platform.pos.entity.PriceList;
import com.hisobnoma.platform.pos.entity.PriceListItem;
import com.hisobnoma.platform.pos.enums.PriceListType;
import com.hisobnoma.platform.pos.repository.PriceListItemRepository;
import com.hisobnoma.platform.pos.repository.PriceListRepository;
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
class PriceListControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PriceListRepository priceListRepository;
    @Autowired private PriceListItemRepository priceListItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private UnitOfMeasureRepository unitOfMeasureRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/pos/price-lists";

    private Tenant tenant;
    private User user;
    private Product product;
    private Product product2;
    private Customer customer;
    private PriceList priceList;
    private PriceListItem priceListItem;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("PriceList Test Tenant").code("PL_FLOW_TEST").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("pricelist_test_user")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        Category category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-PL").name("PriceList Test Category")
                .active(true).tenantId(tenant.getId()).build());

        Brand brand = brandRepository.saveAndFlush(Brand.builder()
                .code("BRD-PL").name("PriceList Test Brand")
                .active(true).tenantId(tenant.getId()).build());

        UnitOfMeasure uom = unitOfMeasureRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS-PL").name("Pieces").symbol("pcs")
                .active(true).tenantId(tenant.getId()).build());

        product = productRepository.saveAndFlush(Product.builder()
                .sku("PROD-PL-001").name("Test Product One")
                .sellingPrice(new BigDecimal("100.0000"))
                .costPrice(new BigDecimal("50.0000"))
                .category(category).brand(brand).baseUom(uom)
                .active(true).tenantId(tenant.getId()).build());

        product2 = productRepository.saveAndFlush(Product.builder()
                .sku("PROD-PL-002").name("Test Product Two")
                .sellingPrice(new BigDecimal("200.0000"))
                .costPrice(new BigDecimal("80.0000"))
                .category(category).brand(brand).baseUom(uom)
                .active(true).tenantId(tenant.getId()).build());

        customer = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-PL-001").name("PriceList Test Customer")
                .active(true).tenantId(tenant.getId()).build());

        priceList = priceListRepository.saveAndFlush(PriceList.builder()
                .code("PL-STD-001").name("Standard Price List")
                .description("Standard pricing for tests")
                .type(PriceListType.STANDARD)
                .currency("UZS").priority(10)
                .defaultMarkupPercent(BigDecimal.ZERO)
                .active(true).defaultPriceList(false)
                .notes("Test notes")
                .tenantId(tenant.getId()).build());

        priceListItem = priceListItemRepository.saveAndFlush(PriceListItem.builder()
                .priceList(priceList)
                .product(product)
                .price(new BigDecimal("95.0000"))
                .minQuantity(BigDecimal.ONE)
                .active(true)
                .notes("Item note")
                .build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("POS_PRICELIST_READ"),
                        new SimpleGrantedAuthority("POS_PRICELIST_CREATE"),
                        new SimpleGrantedAuthority("POS_PRICELIST_UPDATE"),
                        new SimpleGrantedAuthority("POS_PRICELIST_DELETE"),
                        new SimpleGrantedAuthority("POS_PRICELIST_ITEMS_MANAGE"),
                        new SimpleGrantedAuthority("POS_PRICELIST_ASSIGN")));
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

    // ---- 1. GET / ----

    @Test
    void getAll_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("PL-STD-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---- 2. GET /active ----

    @Test
    void getActivePriceLists_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/active").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].code").value("PL-STD-001"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    // ---- 3. GET /{id} ----

    @Test
    void getById_returnsPriceList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + priceList.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PL-STD-001"))
                .andExpect(jsonPath("$.name").value("Standard Price List"))
                .andExpect(jsonPath("$.type").value("STANDARD"))
                .andExpect(jsonPath("$.description").value("Standard pricing for tests"))
                .andExpect(jsonPath("$.currency").value("UZS"))
                .andExpect(jsonPath("$.priority").value(10))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.notes").value("Test notes"));
    }

    // ---- 4. GET /{id} not found ----

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- 5. GET /code/{code} ----

    @Test
    void getByCode_returnsPriceList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/code/PL-STD-001").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PL-STD-001"))
                .andExpect(jsonPath("$.name").value("Standard Price List"))
                .andExpect(jsonPath("$.type").value("STANDARD"));
    }

    // ---- 6. POST / ----

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        CreatePriceListRequest request = CreatePriceListRequest.builder()
                .code("PL-NEW-001").name("New Price List")
                .description("Newly created price list")
                .type(PriceListType.STANDARD)
                .currency("USD").priority(5)
                .defaultMarkupPercent(new BigDecimal("10.00"))
                .defaultPriceList(false)
                .notes("New notes")
                .build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PL-NEW-001"))
                .andExpect(jsonPath("$.name").value("New Price List"))
                .andExpect(jsonPath("$.type").value("STANDARD"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.priority").value(5))
                .andExpect(jsonPath("$.notes").value("New notes"));
    }

    // ---- 7. PUT /{id} ----

    @Test
    void update_validRequest_returnsUpdated() throws Exception {
        CreatePriceListRequest request = CreatePriceListRequest.builder()
                .code("PL-STD-001").name("Updated Price List")
                .description("Updated description")
                .type(PriceListType.WHOLESALE)
                .currency("EUR").priority(20)
                .defaultMarkupPercent(new BigDecimal("5.00"))
                .defaultPriceList(false)
                .notes("Updated notes")
                .build();

        mockMvc.perform(put(BASE_URL + "/" + priceList.getId()).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(priceList.getId()))
                .andExpect(jsonPath("$.name").value("Updated Price List"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.type").value("WHOLESALE"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.priority").value(20))
                .andExpect(jsonPath("$.notes").value("Updated notes"));
    }

    // ---- 8. DELETE /{id} ----

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + priceList.getId()).with(fullAuth()))
                .andExpect(status().isNoContent());

        // Verify price list is deleted
        mockMvc.perform(get(BASE_URL + "/" + priceList.getId()).with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- 9. POST /{id}/activate ----

    @Test
    void activate_returnsActivated() throws Exception {
        // First deactivate the price list
        mockMvc.perform(post(BASE_URL + "/" + priceList.getId() + "/deactivate").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Now activate it
        mockMvc.perform(post(BASE_URL + "/" + priceList.getId() + "/activate").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.code").value("PL-STD-001"));
    }

    // ---- 10. POST /{id}/deactivate ----

    @Test
    void deactivate_returnsDeactivated() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + priceList.getId() + "/deactivate").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.code").value("PL-STD-001"));
    }

    // ---- 11. GET /{priceListId}/items ----

    @Test
    void getItems_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + priceList.getId() + "/items").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---- 12. GET /{priceListId}/items/{itemId} ----

    @Test
    void getItem_returnsSingleItem() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + priceList.getId() + "/items/" + priceListItem.getId())
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(priceListItem.getId()))
                .andExpect(jsonPath("$.priceListId").value(priceList.getId()))
                .andExpect(jsonPath("$.productId").value(product.getId()))
                .andExpect(jsonPath("$.price").value(closeTo(95.0, 0.01)))
                .andExpect(jsonPath("$.notes").value("Item note"));
    }

    // ---- 13. POST /{priceListId}/items ----

    @Test
    void addItem_validRequest_returnsCreated() throws Exception {
        CreatePriceListItemRequest request = CreatePriceListItemRequest.builder()
                .productId(product2.getId())
                .price(new BigDecimal("180.0000"))
                .minQuantity(BigDecimal.ONE)
                .notes("New item note")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + priceList.getId() + "/items").with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(product2.getId()))
                .andExpect(jsonPath("$.price").value(closeTo(180.0, 0.01)))
                .andExpect(jsonPath("$.notes").value("New item note"));
    }

    // ---- 14. PUT /{priceListId}/items/{itemId} ----

    @Test
    void updateItem_validRequest_returnsUpdated() throws Exception {
        CreatePriceListItemRequest request = CreatePriceListItemRequest.builder()
                .productId(product.getId())
                .price(new BigDecimal("85.0000"))
                .minQuantity(new BigDecimal("2"))
                .notes("Updated item note")
                .build();

        mockMvc.perform(put(BASE_URL + "/" + priceList.getId() + "/items/" + priceListItem.getId())
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(priceListItem.getId()))
                .andExpect(jsonPath("$.price").value(closeTo(85.0, 0.01)))
                .andExpect(jsonPath("$.notes").value("Updated item note"));
    }

    // ---- 15. DELETE /{priceListId}/items/{itemId} ----

    @Test
    void removeItem_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + priceList.getId() + "/items/" + priceListItem.getId())
                        .with(fullAuth()))
                .andExpect(status().isNoContent());

        // Verify item is deleted
        mockMvc.perform(get(BASE_URL + "/" + priceList.getId() + "/items/" + priceListItem.getId())
                        .with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- 16. POST /{priceListId}/assign-customer/{customerId} ----

    @Test
    void assignCustomer_returns200() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + priceList.getId() + "/assign-customer/" + customer.getId())
                        .param("priority", "5")
                        .with(fullAuth()))
                .andExpect(status().isOk());
    }

    // ---- 17. GET /customer/{customerId} ----

    @Test
    void getCustomerPriceLists_returnsList() throws Exception {
        // Assign first
        mockMvc.perform(post(BASE_URL + "/" + priceList.getId() + "/assign-customer/" + customer.getId())
                        .with(fullAuth()))
                .andExpect(status().isOk());

        // Then retrieve
        mockMvc.perform(get(BASE_URL + "/customer/" + customer.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("PL-STD-001"));
    }

    // ---- 18. DELETE /{priceListId}/unassign-customer/{customerId} ----

    @Test
    void unassignCustomer_returns204() throws Exception {
        // Assign first
        mockMvc.perform(post(BASE_URL + "/" + priceList.getId() + "/assign-customer/" + customer.getId())
                        .with(fullAuth()))
                .andExpect(status().isOk());

        // Then unassign
        mockMvc.perform(delete(BASE_URL + "/" + priceList.getId() + "/unassign-customer/" + customer.getId())
                        .with(fullAuth()))
                .andExpect(status().isNoContent());

        // Verify no longer assigned
        mockMvc.perform(get(BASE_URL + "/customer/" + customer.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- 19. Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        // GET /
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /active
        mockMvc.perform(get(BASE_URL + "/active").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /{id}
        mockMvc.perform(get(BASE_URL + "/" + priceList.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /code/{code}
        mockMvc.perform(get(BASE_URL + "/code/PL-STD-001").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /
        CreatePriceListRequest createReq = CreatePriceListRequest.builder()
                .code("NOPE").name("No Permission").type(PriceListType.STANDARD).build();
        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // PUT /{id}
        mockMvc.perform(put(BASE_URL + "/" + priceList.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // DELETE /{id}
        mockMvc.perform(delete(BASE_URL + "/" + priceList.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /{id}/activate
        mockMvc.perform(post(BASE_URL + "/" + priceList.getId() + "/activate").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /{id}/deactivate
        mockMvc.perform(post(BASE_URL + "/" + priceList.getId() + "/deactivate").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /{priceListId}/items
        mockMvc.perform(get(BASE_URL + "/" + priceList.getId() + "/items").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /{priceListId}/items
        CreatePriceListItemRequest itemReq = CreatePriceListItemRequest.builder()
                .productId(product.getId()).price(new BigDecimal("50")).build();
        mockMvc.perform(post(BASE_URL + "/" + priceList.getId() + "/items").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemReq)))
                .andExpect(status().isForbidden());

        // POST /{priceListId}/assign-customer/{customerId}
        mockMvc.perform(post(BASE_URL + "/" + priceList.getId() + "/assign-customer/" + customer.getId())
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /customer/{customerId}
        mockMvc.perform(get(BASE_URL + "/customer/" + customer.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
