package com.hisobnoma.platform.mobile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.expense.entity.ExpenseRecord;
import com.hisobnoma.platform.finance.dto.ARPaymentDto;
import com.hisobnoma.platform.hr.dto.SalaryAdvanceDto;
import com.hisobnoma.platform.hr.dto.SalaryRecordDto;
import com.hisobnoma.platform.mobile.service.MobileAdminActionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobileAdminActionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private MobileAdminActionService service;

    // ---- expense ----

    @Test
    @WithMockUser(authorities = "MOBILE_EXPENSE_WRITE")
    void recordExpense_withPermission_returns200() throws Exception {
        ExpenseRecord rec = ExpenseRecord.builder().totalAmount(new BigDecimal("50000")).build();
        rec.setId(7L);
        when(service.recordExpense(any())).thenReturn(rec);

        mockMvc.perform(post("/api/v1/mobile/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalAmount\":50000,\"category\":\"Transport\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void recordExpense_wrongPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalAmount\":50000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "MOBILE_EXPENSE_WRITE")
    void recordExpense_missingAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Transport\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---- debtor payment ----

    @Test
    @WithMockUser(authorities = "MOBILE_AR_COLLECT")
    void collectDebtorPayment_withMobilePerm_returns200() throws Exception {
        when(service.collectDebtorPayment(any())).thenReturn(new ARPaymentDto());
        mockMvc.perform(post("/api/v1/mobile/finance/debtor-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":100,\"amount\":60000,\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void collectDebtorPayment_withModulePerm_returns200() throws Exception {
        when(service.collectDebtorPayment(any())).thenReturn(new ARPaymentDto());
        mockMvc.perform(post("/api/v1/mobile/finance/debtor-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":100,\"amount\":60000}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void collectDebtorPayment_wrongPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/finance/debtor-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":100,\"amount\":60000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "MOBILE_AR_COLLECT")
    void collectDebtorPayment_missingCustomer_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/finance/debtor-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":60000}"))
                .andExpect(status().isBadRequest());
    }

    // ---- salary / advance ----

    @Test
    @WithMockUser(authorities = "MOBILE_SALARY_PAY")
    void paySalary_withMobilePerm_returns200() throws Exception {
        when(service.paySalary(any())).thenReturn(new SalaryRecordDto());
        mockMvc.perform(post("/api/v1/mobile/hr/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":5,\"periodYear\":2026,\"periodMonth\":7,\"baseAmount\":2000000}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "HR_SALARY_WRITE")
    void recordAdvance_withModulePerm_returns200() throws Exception {
        when(service.recordAdvance(any())).thenReturn(new SalaryAdvanceDto());
        mockMvc.perform(post("/api/v1/mobile/hr/advance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":5,\"amount\":500000,\"periodYear\":2026,\"periodMonth\":7}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void hrActions_wrongPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/hr/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":5,\"periodYear\":2026,\"periodMonth\":7,\"baseAmount\":2000000}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/mobile/hr/advance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":5,\"amount\":500000,\"periodYear\":2026,\"periodMonth\":7}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "MOBILE_SALARY_PAY")
    void recordAdvance_missingAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/hr/advance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":5,\"periodYear\":2026,\"periodMonth\":7}"))
                .andExpect(status().isBadRequest());
    }
}
