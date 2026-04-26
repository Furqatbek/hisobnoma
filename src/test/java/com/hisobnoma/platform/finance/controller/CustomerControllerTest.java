package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.CreateCustomerRequest;
import com.hisobnoma.platform.finance.dto.CustomerDto;
import com.hisobnoma.platform.finance.service.CustomerService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    private static final String BASE_URL = "/api/v1/finance/customers";

    private CustomerDto createSampleCustomerDto() {
        return CustomerDto.builder()
                .id(1L)
                .code("CUST-000001")
                .name("Test Customer")
                .email("test@example.com")
                .phone("+998901234567")
                .active(true)
                .creditHold(false)
                .currentBalance(BigDecimal.ZERO)
                .creditLimit(new BigDecimal("50000.00"))
                .paymentTerms("Net 30")
                .paymentTermsDays(30)
                .defaultCurrency("UZS")
                .build();
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getCustomers_authenticated_returns200() throws Exception {
        // Given
        CustomerDto dto = createSampleCustomerDto();
        PageResponse<CustomerDto> pageResponse = PageResponse.of(List.of(dto), 0, 10, 1);
        when(customerService.getCustomers(any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getCustomers_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getCustomers_wrongPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getCustomer_authenticated_returns200() throws Exception {
        // Given
        CustomerDto dto = createSampleCustomerDto();
        when(customerService.getCustomer(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("CUST-000001"))
                .andExpect(jsonPath("$.data.name").value("Test Customer"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getCustomerByCode_authenticated_returns200() throws Exception {
        // Given
        CustomerDto dto = createSampleCustomerDto();
        when(customerService.getCustomerByCode("CUST-000001")).thenReturn(dto);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/code/CUST-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("CUST-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getActiveCustomers_authenticated_returns200() throws Exception {
        // Given
        CustomerDto dto = createSampleCustomerDto();
        when(customerService.getActiveCustomers()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void searchCustomers_authenticated_returns200() throws Exception {
        // Given
        CustomerDto dto = createSampleCustomerDto();
        PageResponse<CustomerDto> pageResponse = PageResponse.of(List.of(dto), 0, 10, 1);
        when(customerService.searchCustomers(eq("Test"), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/search")
                        .param("query", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getNextCustomerCode_authenticated_returns200() throws Exception {
        // Given
        when(customerService.getNextCustomerCode()).thenReturn("CUST-000011");

        // When/Then
        mockMvc.perform(get(BASE_URL + "/next-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void createCustomer_authenticated_returns201() throws Exception {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .name("New Customer")
                .email("new@example.com")
                .phone("+998909999999")
                .paymentTerms("Net 30")
                .paymentTermsDays(30)
                .build();

        CustomerDto dto = createSampleCustomerDto();
        when(customerService.createCustomer(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("CUST-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void updateCustomer_authenticated_returns200() throws Exception {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .code("CUST-000001")
                .name("Updated Customer")
                .email("updated@example.com")
                .build();

        CustomerDto dto = createSampleCustomerDto();
        dto.setName("Updated Customer");
        when(customerService.updateCustomer(eq(1L), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void deleteCustomer_authenticated_returns204() throws Exception {
        // Given
        doNothing().when(customerService).deleteCustomer(1L);

        // When/Then
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void activateCustomer_authenticated_returns200() throws Exception {
        // Given
        CustomerDto dto = createSampleCustomerDto();
        dto.setActive(true);
        when(customerService.activateCustomer(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(patch(BASE_URL + "/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void setCreditHold_authenticated_returns200() throws Exception {
        // Given
        CustomerDto dto = createSampleCustomerDto();
        dto.setCreditHold(true);
        when(customerService.setCreditHold(1L, true)).thenReturn(dto);

        // When/Then
        mockMvc.perform(patch(BASE_URL + "/1/credit-hold")
                        .param("hold", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void updateCreditLimit_authenticated_returns200() throws Exception {
        // Given
        CustomerDto dto = createSampleCustomerDto();
        dto.setCreditLimit(new BigDecimal("100000.00"));
        when(customerService.updateCreditLimit(eq(1L), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(patch(BASE_URL + "/1/credit-limit")
                        .param("creditLimit", "100000.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getCustomersOnCreditHold_authenticated_returns200() throws Exception {
        // Given
        CustomerDto dto = createSampleCustomerDto();
        dto.setCreditHold(true);
        when(customerService.getCustomersOnCreditHold()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/credit-hold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getCustomersOverCreditLimit_authenticated_returns200() throws Exception {
        // Given
        CustomerDto dto = createSampleCustomerDto();
        when(customerService.getCustomersOverCreditLimit()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/over-credit-limit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void canBeInvoiced_authenticated_returns200() throws Exception {
        // Given
        when(customerService.canBeInvoiced(eq(1L), any())).thenReturn(true);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/1/can-invoice")
                        .param("amount", "5000.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void createCustomer_withReadOnlyPermission_returns403() throws Exception {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .name("Test")
                .build();

        // When/Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
