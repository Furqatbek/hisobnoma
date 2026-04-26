package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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

    // ==================== GET /api/v1/pos/transactions ====================

    @Test
    @WithMockUser(authorities = "POS_SALE_READ")
    void getAll_authenticated_returns200() throws Exception {
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.findAll(any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/pos/transactions"))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAll_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/transactions/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_SALE_READ")
    void getById_authenticated_returns200() throws Exception {
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionNumber").value("TXN-001"));
    }

    @Test
    void getById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getById_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/transactions/number/{transactionNumber} ====================

    @Test
    @WithMockUser(authorities = "POS_SALE_READ")
    void getByNumber_authenticated_returns200() throws Exception {
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.findByNumber("TXN-001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/transactions/number/TXN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionNumber").value("TXN-001"));
    }

    @Test
    void getByNumber_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/number/TXN-001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getByNumber_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/number/TXN-001"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/transactions/shift/{shiftId} ====================

    @Test
    @WithMockUser(authorities = "POS_SALE_READ")
    void getByShift_authenticated_returns200() throws Exception {
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.findByShift(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/pos/transactions/shift/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].transactionNumber").value("TXN-001"));
    }

    @Test
    void getByShift_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/shift/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getByShift_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/shift/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/transactions/held ====================

    @Test
    @WithMockUser(authorities = "POS_SALE_READ")
    void getHeldTransactions_authenticated_returns200() throws Exception {
        when(transactionService.findHeldTransactions(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/transactions/held").param("shiftId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void getHeldTransactions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/held").param("shiftId", "1"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions ====================

    @Test
    @WithMockUser(authorities = "POS_SALE_CREATE")
    void create_authenticated_returns201() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest();
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.createTransaction(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions")
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
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void create_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/discount ====================

    @Test
    @WithMockUser(authorities = "POS_DISCOUNT_APPLY")
    void applyDiscount_authenticated_returns200() throws Exception {
        ApplyDiscountRequest request = new ApplyDiscountRequest();
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.applyDiscount(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/discount")
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
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void applyDiscount_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/discount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/hold ====================

    @Test
    @WithMockUser(authorities = "POS_SALE_HOLD")
    void holdTransaction_authenticated_returns200() throws Exception {
        HoldTransactionRequest request = new HoldTransactionRequest();
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.holdTransaction(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/hold")
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
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void holdTransaction_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/void ====================

    @Test
    @WithMockUser(authorities = "POS_SALE_VOID")
    void voidTransaction_authenticated_returns200() throws Exception {
        VoidTransactionRequest request = new VoidTransactionRequest();
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.voidTransaction(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/void")
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
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void voidTransaction_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/void")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/complete ====================

    @Test
    @WithMockUser(authorities = "POS_SALE_CREATE")
    void completeTransaction_authenticated_returns200() throws Exception {
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.completeTransaction(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/complete"))
                .andExpect(status().isOk());
    }

    @Test
    void completeTransaction_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/complete"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/transactions/{id}/payments ====================

    @Test
    @WithMockUser(authorities = "POS_SALE_READ")
    void getPayments_authenticated_returns200() throws Exception {
        when(paymentService.findByTransaction(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/transactions/1/payments"))
                .andExpect(status().isOk());
    }

    @Test
    void getPayments_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/1/payments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getPayments_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/1/payments"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/payments ====================

    @Test
    @WithMockUser(authorities = "POS_PAYMENT_PROCESS")
    void addPayment_authenticated_returns201() throws Exception {
        AddPaymentRequest request = new AddPaymentRequest();
        POSPaymentDto dto = POSPaymentDto.builder().id(1L).build();
        when(paymentService.addPayment(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/payments")
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
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void addPayment_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/payments/{paymentId}/void ====================

    @Test
    @WithMockUser(authorities = "POS_PAYMENT_VOID")
    void voidPayment_authenticated_returns200() throws Exception {
        POSPaymentDto dto = POSPaymentDto.builder().id(1L).build();
        when(paymentService.voidPayment(eq(1L), eq(2L), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/payments/2/void")
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
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void voidPayment_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/payments/2/void")
                        .param("reason", "Mistake"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/returns ====================

    @Test
    @WithMockUser(authorities = "POS_RETURN_CREATE")
    void createReturn_authenticated_returns201() throws Exception {
        CreateReturnRequest request = new CreateReturnRequest();
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("RET-001").build();
        when(transactionService.createReturn(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/returns")
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
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createReturn_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/transactions/failed-gl ====================

    @Test
    @WithMockUser(authorities = "POS_REPORTS_VIEW")
    void getFailedGlPostings_authenticated_returns200() throws Exception {
        when(transactionService.findFailedGlPostings()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/transactions/failed-gl"))
                .andExpect(status().isOk());
    }

    @Test
    void getFailedGlPostings_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/failed-gl"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getFailedGlPostings_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/transactions/failed-gl"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/transactions/{id}/retry-gl ====================

    @Test
    @WithMockUser(authorities = "POS_REPORTS_VIEW")
    void retryGlPosting_authenticated_returns200() throws Exception {
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("TXN-001").build();
        when(transactionService.retryGlPosting(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/transactions/1/retry-gl"))
                .andExpect(status().isOk());
    }

    @Test
    void retryGlPosting_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/retry-gl"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void retryGlPosting_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/transactions/1/retry-gl"))
                .andExpect(status().isForbidden());
    }
}
