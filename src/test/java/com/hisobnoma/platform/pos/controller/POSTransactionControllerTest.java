package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.service.POSPaymentService;
import com.hisobnoma.platform.pos.service.POSTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class POSTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private POSTransactionService transactionService;

    @MockBean
    private POSPaymentService paymentService;

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

    // ==================== GET /api/v1/pos/transactions ====================

    @Test
    void getAll_authenticated_returns200() throws Exception {
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.findAll(any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/pos/transactions").with(userWithPermission("POS_SALE_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/transactions/{id} ====================

    @Test
    void getById_authenticated_returns200() throws Exception {
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/transactions/1").with(userWithPermission("POS_SALE_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionNumber").value("TXN-001"));
    }

    @Test
    void getById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/1").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/transactions/number/{transactionNumber} ====================

    @Test
    void getByNumber_authenticated_returns200() throws Exception {
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.findByNumber("TXN-001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/transactions/number/TXN-001").with(userWithPermission("POS_SALE_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionNumber").value("TXN-001"));
    }

    @Test
    void getByNumber_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/number/TXN-001"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByNumber_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/number/TXN-001").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/transactions/shift/{shiftId} ====================

    @Test
    void getByShift_authenticated_returns200() throws Exception {
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.findByShift(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/pos/transactions/shift/1").with(userWithPermission("POS_SALE_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].transactionNumber").value("TXN-001"));
    }

    @Test
    void getByShift_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/shift/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByShift_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/shift/1").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/transactions/held ====================

    @Test
    void getHeldTransactions_authenticated_returns200() throws Exception {
        when(transactionService.findHeldTransactions(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/transactions/held").param("shiftId", "1")
                        .with(userWithPermission("POS_SALE_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getHeldTransactions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/held").param("shiftId", "1"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions ====================

    @Test
    void create_authenticated_returns201() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest();
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.createTransaction(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions")
                        .with(userWithPermission("POS_SALE_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.transactionNumber").value("TXN-001"));
    }

    @Test
    void create_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/discount ====================

    @Test
    void applyDiscount_authenticated_returns200() throws Exception {
        ApplyDiscountRequest request = new ApplyDiscountRequest();
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.applyDiscount(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/discount")
                        .with(userWithPermission("POS_DISCOUNT_APPLY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void applyDiscount_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/discount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void applyDiscount_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/discount")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/hold ====================

    @Test
    void holdTransaction_authenticated_returns200() throws Exception {
        HoldTransactionRequest request = new HoldTransactionRequest();
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.holdTransaction(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/hold")
                        .with(userWithPermission("POS_SALE_HOLD"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void holdTransaction_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void holdTransaction_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/hold")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/void ====================

    @Test
    void voidTransaction_authenticated_returns200() throws Exception {
        VoidTransactionRequest request = new VoidTransactionRequest();
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.voidTransaction(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/void")
                        .with(userWithPermission("POS_SALE_VOID"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void voidTransaction_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/void")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void voidTransaction_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/void")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/complete ====================

    @Test
    void completeTransaction_authenticated_returns200() throws Exception {
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.completeTransaction(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/complete").with(userWithPermission("POS_SALE_CREATE")))
                .andExpect(status().isOk());
    }

    @Test
    void completeTransaction_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/complete"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/transactions/{id}/payments ====================

    @Test
    void getPayments_authenticated_returns200() throws Exception {
        when(paymentService.findByTransaction(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/transactions/1/payments").with(userWithPermission("POS_SALE_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getPayments_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/1/payments"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPayments_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/1/payments").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/payments ====================

    @Test
    void addPayment_authenticated_returns201() throws Exception {
        AddPaymentRequest request = new AddPaymentRequest();
        POSPaymentDto dto = POSPaymentDto.builder().id(1L).build();
        when(paymentService.addPayment(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/payments")
                        .with(userWithPermission("POS_PAYMENT_PROCESS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void addPayment_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addPayment_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/payments")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/payments/{paymentId}/void ====================

    @Test
    void voidPayment_authenticated_returns200() throws Exception {
        POSPaymentDto dto = POSPaymentDto.builder().id(1L).build();
        when(paymentService.voidPayment(eq(1L), eq(2L), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/payments/2/void")
                        .with(userWithPermission("POS_PAYMENT_VOID"))
                        .param("reason", "Mistake"))
                .andExpect(status().isOk());
    }

    @Test
    void voidPayment_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/payments/2/void")
                        .param("reason", "Mistake"))
                .andExpect(status().isForbidden());
    }

    @Test
    void voidPayment_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/payments/2/void")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .param("reason", "Mistake"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/returns ====================

    @Test
    void createReturn_authenticated_returns201() throws Exception {
        CreateReturnRequest request = new CreateReturnRequest();
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("RET-001").build();
        when(transactionService.createReturn(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/returns")
                        .with(userWithPermission("POS_RETURN_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createReturn_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReturn_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/returns")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/transactions/failed-gl ====================

    @Test
    void getFailedGlPostings_authenticated_returns200() throws Exception {
        when(transactionService.findFailedGlPostings()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/transactions/failed-gl").with(userWithPermission("POS_REPORTS_VIEW")))
                .andExpect(status().isOk());
    }

    @Test
    void getFailedGlPostings_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/failed-gl"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFailedGlPostings_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/failed-gl").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/retry-gl ====================

    @Test
    void retryGlPosting_authenticated_returns200() throws Exception {
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.retryGlPosting(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/retry-gl").with(userWithPermission("POS_REPORTS_VIEW")))
                .andExpect(status().isOk());
    }

    @Test
    void retryGlPosting_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/retry-gl"))
                .andExpect(status().isForbidden());
    }

    @Test
    void retryGlPosting_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/retry-gl").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }
}
