package com.hisobnoma.platform.expense.controller;

import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.expense.entity.ExpenseRecord;
import com.hisobnoma.platform.expense.repository.ExpenseRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExpenseRecordControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/web/expenses";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ExpenseRecordRepository expenseRecordRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private ExpenseRecord existingExpense;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Expense FullFlow Tenant").code("FULLFLOW_EXP").active(true)
                .maxUsers(100).maxLocations(10).build());

        existingExpense = expenseRecordRepository.saveAndFlush(ExpenseRecord.builder()
                .createDate(LocalDate.of(2024, 1, 15))
                .category("Transport")
                .totalAmount(new BigDecimal("50000"))
                .currency("UZS")
                .generatedNotes("Taxi to office")
                .fullText("Daily commute expense")
                .tenantId(tenant.getId())
                .build());

        expenseRecordRepository.saveAndFlush(ExpenseRecord.builder()
                .createDate(LocalDate.of(2024, 1, 16))
                .category("Food")
                .totalAmount(new BigDecimal("25000"))
                .currency("UZS")
                .generatedNotes("Lunch")
                .fullText("Team lunch")
                .tenantId(tenant.getId())
                .build());

        entityManager.clear();
    }

    // ---- GET /api/v1/web/expenses ----

    @Test
    void getExpenses_returnsPaginatedResults() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    // ---- GET /api/v1/web/expenses/summary/total ----

    @Test
    void getTotal_returnsSummary() throws Exception {
        mockMvc.perform(get(BASE_URL + "/summary/total")
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(75000));
    }

    // ---- POST /api/v1/web/expenses ----

    @Test
    void createExpense_validRequest_returnsCreated() throws Exception {
        Map<String, Object> request = Map.of(
                "create_date", "2024-02-10",
                "category", "Office",
                "total_amount", 120000,
                "currency", "UZS",
                "generated_notes", "Office supplies",
                "full_text", "Purchased printer paper and pens"
        );

        mockMvc.perform(post(BASE_URL)
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("created"));
    }

    @Test
    void createExpense_missingAmount_returnsBadRequest() throws Exception {
        Map<String, Object> request = Map.of(
                "create_date", "2024-02-10",
                "category", "Office",
                "currency", "UZS"
        );

        mockMvc.perform(post(BASE_URL)
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("total_amount is required"));
    }

    @Test
    void createExpense_defaultsCategory() throws Exception {
        Map<String, Object> request = Map.of(
                "create_date", "2024-03-01",
                "total_amount", 10000,
                "currency", "UZS"
        );

        String responseBody = mockMvc.perform(post(BASE_URL)
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(responseBody).get("id").asLong();
        ExpenseRecord saved = expenseRecordRepository.findById(createdId).orElseThrow();
        assertThat(saved.getCategory()).isEqualTo("Boshqa");
    }

    // ---- DELETE /api/v1/web/expenses/{id} ----

    @Test
    void deleteExpense_returnsNoContent() throws Exception {
        Long expenseId = existingExpense.getId();

        mockMvc.perform(delete(BASE_URL + "/" + expenseId)
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isNoContent());

        assertThat(expenseRecordRepository.findById(expenseId)).isEmpty();
    }

    @Test
    void deleteExpense_nonExistent_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/999999")
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isNoContent());
    }
}
