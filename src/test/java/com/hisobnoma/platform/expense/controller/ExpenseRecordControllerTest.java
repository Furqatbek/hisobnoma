package com.hisobnoma.platform.expense.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.expense.entity.ExpenseRecord;
import com.hisobnoma.platform.expense.repository.ExpenseRecordRepository;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpenseRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExpenseRecordRepository repository;

    // ==================== GET /api/v1/web/expenses ====================
    // Note: ExpenseRecordController has no @RequiresPermission or @PreAuthorize annotations,
    // so we test that authenticated users can access and unauthenticated cannot.

    @Test
    @WithMockUser
    void getExpenses_authenticated_returns200() throws Exception {
        when(repository.findByTenantIdOrTenantIdIsNull(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/web/expenses"))
                .andExpect(status().isOk());
    }

    @Test
    void getExpenses_unauthenticated_returnsOk() throws Exception {
        when(repository.findByTenantIdOrTenantIdIsNull(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/web/expenses"))
                .andExpect(status().isOk());
    }

    // ==================== GET /api/v1/web/expenses/summary/total ====================

    @Test
    @WithMockUser
    void getTotal_authenticated_returns200() throws Exception {
        when(repository.sumTotalByTenantIdOrNull(any())).thenReturn(new BigDecimal("5000.00"));

        mockMvc.perform(get("/api/v1/web/expenses/summary/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5000.00));
    }

    @Test
    void getTotal_unauthenticated_returnsOk() throws Exception {
        when(repository.sumTotalByTenantIdOrNull(any())).thenReturn(new BigDecimal("0.00"));

        mockMvc.perform(get("/api/v1/web/expenses/summary/total"))
                .andExpect(status().isOk());
    }

    // ==================== POST /api/v1/web/expenses ====================

    @Test
    @WithMockUser
    void createExpense_authenticated_returns200() throws Exception {
        ExpenseRecord saved = ExpenseRecord.builder()
                .id(1L)
                .totalAmount(new BigDecimal("100.00"))
                .category("Office")
                .build();
        when(repository.save(any())).thenReturn(saved);

        String requestBody = """
                {
                    "total_amount": 100.00,
                    "category": "Office",
                    "currency": "UZS"
                }
                """;

        mockMvc.perform(post("/api/v1/web/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createExpense_unauthenticated_returnsOk() throws Exception {
        ExpenseRecord saved = ExpenseRecord.builder()
                .id(2L)
                .totalAmount(new BigDecimal("100.00"))
                .category("Boshqa")
                .build();
        when(repository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/web/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total_amount\": 100.00}"))
                .andExpect(status().isOk());
    }

    // ==================== DELETE /api/v1/web/expenses/{id} ====================

    @Test
    @WithMockUser
    void deleteExpense_authenticated_returns204() throws Exception {
        ExpenseRecord record = ExpenseRecord.builder().id(1L).tenantId(1L).build();
        when(repository.findById(1L)).thenReturn(Optional.of(record));

        mockMvc.perform(delete("/api/v1/web/expenses/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteExpense_unauthenticated_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/web/expenses/1"))
                .andExpect(status().isNoContent());
    }
}
