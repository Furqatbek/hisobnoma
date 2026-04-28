package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.repository.CategoryRepository;
import com.hisobnoma.platform.inventory.repository.LocationRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.repository.POSTerminalRepository;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import com.hisobnoma.platform.pos.repository.ShiftRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class POSTransactionControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UnitOfMeasureRepository unitOfMeasureRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private POSTerminalRepository terminalRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private POSTransactionRepository transactionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/pos/transactions";

    private Tenant tenant;
    private User user;
    private Location location;
    private Category category;
    private UnitOfMeasure uom;
    private Product product;
    private Product product2;
    private POSTerminal terminal;
    private Shift shift;
    private POSTransaction existingTransaction;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("POS Tx Test Tenant").code("POSTX_FULL").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("postxfulluser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        location = locationRepository.saveAndFlush(Location.builder()
                .code("LOC-TX-001").name("POS Store")
                .locationType(LocationType.STORE).active(true)
                .allowNegativeStock(true)
                .tenantId(tenant.getId()).build());

        category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-TX-001").name("Test Category")
                .tenantId(tenant.getId()).build());

        uom = unitOfMeasureRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS").name("Pieces").symbol("pcs")
                .active(true).isBaseUnit(true)
                .tenantId(tenant.getId()).build());

        product = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-TX-001").name("Test Product A").barcode("1234567890")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("50000"))
                .costPrice(new BigDecimal("30000"))
                .trackInventory(false)
                .active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        product2 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-TX-002").name("Test Product B").barcode("1234567891")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("25000"))
                .costPrice(new BigDecimal("15000"))
                .trackInventory(false)
                .active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        terminal = terminalRepository.saveAndFlush(POSTerminal.builder()
                .terminalCode("T-TX-001").name("Test Terminal")
                .location(location).active(true)
                .tenantId(tenant.getId()).build());

        shift = shiftRepository.saveAndFlush(Shift.builder()
                .shiftNumber("SH-TX-001")
                .terminal(terminal)
                .cashierId(user.getId())
                .cashierName("postxfulluser")
                .status(ShiftStatus.OPEN)
                .openedAt(Instant.now())
                .openingCash(BigDecimal.ZERO)
                .tenantId(tenant.getId()).build());

        terminal.setCurrentShiftId(shift.getId());
        terminalRepository.saveAndFlush(terminal);

        // Create an existing PENDING transaction for query/modification tests
        existingTransaction = transactionRepository.saveAndFlush(POSTransaction.builder()
                .transactionNumber("TX-EXISTING-001")
                .terminal(terminal)
                .shift(shift)
                .cashierId(user.getId())
                .cashierName("postxfulluser")
                .transactionType(TransactionType.SALE)
                .status(TransactionStatus.PENDING)
                .subtotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("POS_SALE_READ"),
                        new SimpleGrantedAuthority("POS_SALE_CREATE"),
                        new SimpleGrantedAuthority("POS_SALE_HOLD"),
                        new SimpleGrantedAuthority("POS_SALE_VOID"),
                        new SimpleGrantedAuthority("POS_DISCOUNT_APPLY"),
                        new SimpleGrantedAuthority("POS_PAYMENT_PROCESS"),
                        new SimpleGrantedAuthority("POS_REPORTS_VIEW")));
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
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ---- 2. GET /{id} ----

    @Test
    void getById_returnsTransaction() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + existingTransaction.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(existingTransaction.getId()))
                .andExpect(jsonPath("$.data.transactionNumber").value("TX-EXISTING-001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.transactionType").value("SALE"))
                .andExpect(jsonPath("$.data.terminalId").value(terminal.getId()))
                .andExpect(jsonPath("$.data.shiftId").value(shift.getId()));
    }

    // ---- 3. GET /{id} not found ----

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- 4. GET /shift/{shiftId} ----

    @Test
    void getByShift_returnsTransactions() throws Exception {
        mockMvc.perform(get(BASE_URL + "/shift/" + shift.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].shiftId").value(shift.getId()));
    }

    // ---- 5. POST / create transaction ----

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .transactionType(TransactionType.SALE)
                .items(List.of(
                        CreateTransactionRequest.LineItem.builder()
                                .productId(product.getId())
                                .quantity(new BigDecimal("2"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.transactionNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.transactionType").value("SALE"))
                .andExpect(jsonPath("$.data.terminalId").value(terminal.getId()))
                .andExpect(jsonPath("$.data.shiftId").value(shift.getId()))
                .andExpect(jsonPath("$.data.lines").isArray())
                .andExpect(jsonPath("$.data.lines.length()").value(1))
                .andExpect(jsonPath("$.data.lines[0].productName").value("Test Product A"))
                .andExpect(jsonPath("$.data.totalAmount").value(100000));
    }

    // ---- 6. POST /{id}/lines ----

    @Test
    void addLine_returnsUpdatedTransaction() throws Exception {
        AddLineRequest request = AddLineRequest.builder()
                .productId(product.getId())
                .quantity(new BigDecimal("3"))
                .build();

        mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/lines")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(existingTransaction.getId()))
                .andExpect(jsonPath("$.data.lines").isArray())
                .andExpect(jsonPath("$.data.lines.length()").value(1))
                .andExpect(jsonPath("$.data.lines[0].productName").value("Test Product A"))
                .andExpect(jsonPath("$.data.lines[0].quantity").value(3))
                .andExpect(jsonPath("$.data.totalAmount").value(150000));
    }

    // ---- 7. PUT /{id}/lines/{lineId} ----

    @Test
    void updateLine_returnsUpdatedTransaction() throws Exception {
        // First add a line
        AddLineRequest addReq = AddLineRequest.builder()
                .productId(product.getId())
                .quantity(new BigDecimal("2"))
                .build();

        MvcResult addResult = mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/lines")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andReturn();

        Long lineId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("data").path("lines").get(0).path("id").asLong();

        // Now update the line
        UpdateLineRequest updateReq = UpdateLineRequest.builder()
                .quantity(new BigDecimal("5"))
                .build();

        mockMvc.perform(put(BASE_URL + "/" + existingTransaction.getId() + "/lines/" + lineId)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lines[0].quantity").value(5))
                .andExpect(jsonPath("$.data.totalAmount").value(250000));
    }

    // ---- 8. DELETE /{id}/lines/{lineId} ----

    @Test
    void removeLine_returnsUpdatedTransaction() throws Exception {
        // First add a line
        AddLineRequest addReq = AddLineRequest.builder()
                .productId(product.getId())
                .quantity(new BigDecimal("1"))
                .build();

        MvcResult addResult = mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/lines")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andReturn();

        Long lineId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("data").path("lines").get(0).path("id").asLong();

        // Now remove the line
        mockMvc.perform(delete(BASE_URL + "/" + existingTransaction.getId() + "/lines/" + lineId)
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lines").isArray())
                .andExpect(jsonPath("$.data.lines.length()").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(0));
    }

    // ---- 9. POST /{id}/discount ----

    @Test
    void applyDiscount_returnsDiscounted() throws Exception {
        // First add a line to have a non-zero subtotal
        AddLineRequest addReq = AddLineRequest.builder()
                .productId(product.getId())
                .quantity(new BigDecimal("2"))
                .build();

        mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/lines")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        // Apply a fixed discount
        ApplyDiscountRequest discountReq = ApplyDiscountRequest.builder()
                .amount(new BigDecimal("10000"))
                .reason("Loyal customer")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/discount")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(discountReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountAmount").value(10000))
                .andExpect(jsonPath("$.data.discountReason").value("Loyal customer"))
                .andExpect(jsonPath("$.data.totalAmount").value(90000));
    }

    // ---- 10. POST /{id}/hold ----

    @Test
    void holdTransaction_returnsHeld() throws Exception {
        HoldTransactionRequest holdReq = HoldTransactionRequest.builder()
                .reason("Customer went to get cash")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/hold")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HELD"))
                .andExpect(jsonPath("$.data.heldReason").value("Customer went to get cash"))
                .andExpect(jsonPath("$.data.heldBy").value(user.getId()));
    }

    // ---- 11. POST /{id}/recall ----

    @Test
    void recallTransaction_returnsPending() throws Exception {
        // First hold the transaction
        HoldTransactionRequest holdReq = HoldTransactionRequest.builder()
                .reason("Temporary hold")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/hold")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andExpect(status().isOk());

        // Recall it
        mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/recall")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.heldReason").doesNotExist());
    }

    // ---- 12. GET /held ----

    @Test
    void getHeldTransactions_returnsList() throws Exception {
        // Hold the existing transaction first
        HoldTransactionRequest holdReq = HoldTransactionRequest.builder()
                .reason("Hold for test")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/hold")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andExpect(status().isOk());

        // Query held transactions
        mockMvc.perform(get(BASE_URL + "/held").param("shiftId", shift.getId().toString())
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("HELD"));
    }

    // ---- 13. POST /{id}/void ----

    @Test
    void voidTransaction_returnsVoided() throws Exception {
        VoidTransactionRequest voidReq = VoidTransactionRequest.builder()
                .reason("Customer cancelled")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/void")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VOIDED"))
                .andExpect(jsonPath("$.data.voidReason").value("Customer cancelled"))
                .andExpect(jsonPath("$.data.voidedBy").value(user.getId()));
    }

    // ---- 14. GET /{id}/payments ----

    @Test
    void getPayments_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + existingTransaction.getId() + "/payments")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ---- 15. POST /{id}/payments ----

    @Test
    void addPayment_returnsPayment() throws Exception {
        // First add a line so transaction has a balance
        AddLineRequest addReq = AddLineRequest.builder()
                .productId(product.getId())
                .quantity(new BigDecimal("1"))
                .build();

        mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/lines")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        // Add payment
        AddPaymentRequest payReq = AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("50000"))
                .tenderedAmount(new BigDecimal("60000"))
                .build();

        mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/payments")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentType").value("CASH"))
                .andExpect(jsonPath("$.data.amount").value(50000))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    // ---- 16. Full sale flow: create -> add line -> pay -> complete ----

    @Test
    void fullSaleFlow_createAddLinePayComplete() throws Exception {
        // Step 1: Create transaction with an item
        CreateTransactionRequest createReq = CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .transactionType(TransactionType.SALE)
                .items(List.of(
                        CreateTransactionRequest.LineItem.builder()
                                .productId(product.getId())
                                .quantity(new BigDecimal("1"))
                                .build()
                ))
                .build();

        MvcResult createResult = mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        Long txId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Step 2: Add another line
        AddLineRequest addLineReq = AddLineRequest.builder()
                .productId(product2.getId())
                .quantity(new BigDecimal("2"))
                .build();

        mockMvc.perform(post(BASE_URL + "/" + txId + "/lines")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addLineReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lines.length()").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(100000));

        // Step 3: Add payment covering total
        AddPaymentRequest payReq = AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("100000"))
                .tenderedAmount(new BigDecimal("100000"))
                .build();

        mockMvc.perform(post(BASE_URL + "/" + txId + "/payments")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Step 4: Complete the transaction
        mockMvc.perform(post(BASE_URL + "/" + txId + "/complete")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedBy").value(user.getId()))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());
    }

    // ---- 17. GET /failed-gl ----

    @Test
    void getFailedGlPostings_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/failed-gl").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ---- 18. Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        // GET /
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /{id}
        mockMvc.perform(get(BASE_URL + "/" + existingTransaction.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /
        CreateTransactionRequest createReq = CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .transactionType(TransactionType.SALE)
                .build();
        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // POST /{id}/hold
        HoldTransactionRequest holdReq = HoldTransactionRequest.builder().reason("test").build();
        mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/hold")
                        .with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andExpect(status().isForbidden());

        // POST /{id}/void
        VoidTransactionRequest voidReq = VoidTransactionRequest.builder().reason("test").build();
        mockMvc.perform(post(BASE_URL + "/" + existingTransaction.getId() + "/void")
                        .with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidReq)))
                .andExpect(status().isForbidden());

        // GET /failed-gl
        mockMvc.perform(get(BASE_URL + "/failed-gl").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    // ---- 3b. GET /number/{transactionNumber} ----

    @Test
    void getByNumber_returnsTransaction() throws Exception {
        mockMvc.perform(get(BASE_URL + "/number/TX-EXISTING-001").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionNumber").value("TX-EXISTING-001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }
}
