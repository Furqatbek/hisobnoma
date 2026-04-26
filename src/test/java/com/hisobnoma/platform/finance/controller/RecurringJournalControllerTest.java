package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.RecurringJournalTemplateDto;
import com.hisobnoma.platform.finance.entity.RecurringJournalTemplate.RecurrenceFrequency;
import com.hisobnoma.platform.finance.service.RecurringJournalService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecurringJournalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecurringJournalService recurringJournalService;

    private static final String BASE_URL = "/api/v1/finance/recurring-journals";

    private RecurringJournalTemplateDto buildTemplateDto() {
        return RecurringJournalTemplateDto.builder()
                .id(1L)
                .name("Monthly Rent")
                .description("Monthly office rent payment")
                .frequency(RecurrenceFrequency.MONTHLY)
                .frequencyDay(1)
                .startDate(LocalDate.of(2024, 1, 1))
                .nextRunDate(LocalDate.of(2024, 2, 1))
                .lastRunDate(LocalDate.of(2024, 1, 1))
                .runCount(1)
                .active(true)
                .autoPost(false)
                .lines(List.of(
                        RecurringJournalTemplateDto.LineDto.builder()
                                .id(1L)
                                .accountId(2L)
                                .accountCode("5210")
                                .accountName("Rent Expense")
                                .description("Rent expense")
                                .debitAmount(new BigDecimal("5000.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .sortOrder(1)
                                .build(),
                        RecurringJournalTemplateDto.LineDto.builder()
                                .id(2L)
                                .accountId(1L)
                                .accountCode("1110")
                                .accountName("Cash")
                                .description("Cash payment")
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("5000.00"))
                                .sortOrder(2)
                                .build()
                ))
                .build();
    }

    // ==================== GET /api/v1/finance/recurring-journals ====================

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_VIEW")
    void getTemplates_returns200() throws Exception {
        // Given
        RecurringJournalTemplateDto dto = buildTemplateDto();
        PageResponse<RecurringJournalTemplateDto> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(dto))
        );
        when(recurringJournalService.getTemplates(any())).thenReturn(pageResponse);

        // When / Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Monthly Rent"))
                .andExpect(jsonPath("$.content[0].frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    void getTemplates_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getTemplates_noPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/recurring-journals/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_VIEW")
    void getTemplate_returns200() throws Exception {
        // Given
        RecurringJournalTemplateDto dto = buildTemplateDto();
        when(recurringJournalService.getTemplate(1L)).thenReturn(dto);

        // When / Then
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Monthly Rent"))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines[0].accountCode").value("5210"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_VIEW")
    void getTemplate_notFound_returns404() throws Exception {
        // Given
        when(recurringJournalService.getTemplate(999L))
                .thenThrow(new NotFoundException("RecurringJournalTemplate", 999L));

        // When / Then
        mockMvc.perform(get(BASE_URL + "/999"))
                .andExpect(status().isNotFound());
    }

    // ==================== POST /api/v1/finance/recurring-journals ====================

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_MANAGE")
    void createTemplate_returns201() throws Exception {
        // Given
        RecurringJournalTemplateDto request = buildTemplateDto();
        request.setId(null);
        RecurringJournalTemplateDto created = buildTemplateDto();
        when(recurringJournalService.createTemplate(any(RecurringJournalTemplateDto.class))).thenReturn(created);

        // When / Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Monthly Rent"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_MANAGE")
    void createTemplate_duplicateName_returns400() throws Exception {
        // Given
        RecurringJournalTemplateDto request = buildTemplateDto();
        when(recurringJournalService.createTemplate(any(RecurringJournalTemplateDto.class)))
                .thenThrow(new BusinessException("Template with name 'Monthly Rent' already exists"));

        // When / Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_MANAGE")
    void createTemplate_unbalancedLines_returns400() throws Exception {
        // Given
        RecurringJournalTemplateDto request = buildTemplateDto();
        when(recurringJournalService.createTemplate(any(RecurringJournalTemplateDto.class)))
                .thenThrow(new BusinessException("Total debits must equal total credits"));

        // When / Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTemplate_unauthenticated_returns403() throws Exception {
        // Given
        RecurringJournalTemplateDto request = buildTemplateDto();

        // When / Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/finance/recurring-journals/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_MANAGE")
    void updateTemplate_returns200() throws Exception {
        // Given
        RecurringJournalTemplateDto request = buildTemplateDto();
        RecurringJournalTemplateDto updated = buildTemplateDto();
        updated.setName("Updated Monthly Rent");
        when(recurringJournalService.updateTemplate(eq(1L), any(RecurringJournalTemplateDto.class)))
                .thenReturn(updated);

        // When / Then
        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Monthly Rent"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_MANAGE")
    void updateTemplate_notFound_returns404() throws Exception {
        // Given
        RecurringJournalTemplateDto request = buildTemplateDto();
        when(recurringJournalService.updateTemplate(eq(999L), any(RecurringJournalTemplateDto.class)))
                .thenThrow(new NotFoundException("RecurringJournalTemplate", 999L));

        // When / Then
        mockMvc.perform(put(BASE_URL + "/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE /api/v1/finance/recurring-journals/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_MANAGE")
    void deleteTemplate_returns204() throws Exception {
        // Given
        doNothing().when(recurringJournalService).deleteTemplate(1L);

        // When / Then
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());

        verify(recurringJournalService).deleteTemplate(1L);
    }

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_MANAGE")
    void deleteTemplate_notFound_returns404() throws Exception {
        // Given
        doThrow(new NotFoundException("RecurringJournalTemplate", 999L))
                .when(recurringJournalService).deleteTemplate(999L);

        // When / Then
        mockMvc.perform(delete(BASE_URL + "/999"))
                .andExpect(status().isNotFound());
    }

    // ==================== PUT /api/v1/finance/recurring-journals/{id}/activate ====================

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_MANAGE")
    void activateTemplate_returns200() throws Exception {
        // Given
        RecurringJournalTemplateDto dto = buildTemplateDto();
        dto.setActive(true);
        when(recurringJournalService.activateTemplate(1L)).thenReturn(dto);

        // When / Then
        mockMvc.perform(put(BASE_URL + "/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ==================== PUT /api/v1/finance/recurring-journals/{id}/deactivate ====================

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_MANAGE")
    void deactivateTemplate_returns200() throws Exception {
        // Given
        RecurringJournalTemplateDto dto = buildTemplateDto();
        dto.setActive(false);
        when(recurringJournalService.deactivateTemplate(1L)).thenReturn(dto);

        // When / Then
        mockMvc.perform(put(BASE_URL + "/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ==================== POST /api/v1/finance/recurring-journals/{id}/execute ====================

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_MANAGE")
    void executeTemplate_returns200() throws Exception {
        // Given
        when(recurringJournalService.executeTemplate(1L)).thenReturn(10L);

        // When / Then
        mockMvc.perform(post(BASE_URL + "/1/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journalEntryId").value(10));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_RECURRING_MANAGE")
    void executeTemplate_notFound_returns404() throws Exception {
        // Given
        when(recurringJournalService.executeTemplate(999L))
                .thenThrow(new NotFoundException("RecurringJournalTemplate", 999L));

        // When / Then
        mockMvc.perform(post(BASE_URL + "/999/execute"))
                .andExpect(status().isNotFound());
    }
}
