package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateJournalEntryRequest;
import com.hisobnoma.platform.finance.dto.CreateJournalLineRequest;
import com.hisobnoma.platform.finance.dto.JournalEntryDto;
import com.hisobnoma.platform.finance.dto.JournalLineDto;
import com.hisobnoma.platform.finance.entity.JournalSource;
import com.hisobnoma.platform.finance.entity.JournalStatus;
import com.hisobnoma.platform.finance.service.JournalEntryService;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JournalEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JournalEntryService journalEntryService;

    private static final String BASE_URL = "/api/v1/finance/journal-entries";

    private JournalEntryDto buildJournalEntryDto() {
        return JournalEntryDto.builder()
                .id(1L)
                .entryNumber("JE-202401-0001")
                .entryDate(LocalDate.of(2024, 1, 15))
                .fiscalPeriodId(1L)
                .fiscalPeriodName("January 2024")
                .description("Test journal entry")
                .status(JournalStatus.DRAFT)
                .source(JournalSource.MANUAL)
                .totalDebit(new BigDecimal("1000.00"))
                .totalCredit(new BigDecimal("1000.00"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .balanced(true)
                .lines(List.of(
                        JournalLineDto.builder()
                                .id(1L)
                                .accountId(1L)
                                .accountCode("1110")
                                .accountName("Cash")
                                .debitAmount(new BigDecimal("1000.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .build(),
                        JournalLineDto.builder()
                                .id(2L)
                                .accountId(2L)
                                .accountCode("4100")
                                .accountName("Sales Revenue")
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("1000.00"))
                                .build()
                ))
                .build();
    }

    private CreateJournalEntryRequest buildCreateRequest() {
        return CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("Test journal entry")
                .lines(List.of(
                        CreateJournalLineRequest.builder()
                                .accountId(1L)
                                .debitAmount(new BigDecimal("1000.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .description("Cash debit")
                                .build(),
                        CreateJournalLineRequest.builder()
                                .accountId(2L)
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("1000.00"))
                                .description("Revenue credit")
                                .build()
                ))
                .build();
    }

    // ==================== GET /api/v1/finance/journal-entries ====================

    @Test
    @WithMockUser(authorities = "FINANCE_JE_VIEW")
    void getJournalEntries_returns200() throws Exception {
        // Given
        JournalEntryDto dto = buildJournalEntryDto();
        PageResponse<JournalEntryDto> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(dto))
        );
        when(journalEntryService.getJournalEntries(any())).thenReturn(pageResponse);

        // When / Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entryNumber").value("JE-202401-0001"))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.content[0].balanced").value(true));
    }

    @Test
    void getJournalEntries_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getJournalEntries_noPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/journal-entries/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_JE_VIEW")
    void getJournalEntry_returns200() throws Exception {
        // Given
        JournalEntryDto dto = buildJournalEntryDto();
        when(journalEntryService.getJournalEntry(1L)).thenReturn(dto);

        // When / Then
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.entryNumber").value("JE-202401-0001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_JE_VIEW")
    void getJournalEntry_notFound_returns404() throws Exception {
        // Given
        when(journalEntryService.getJournalEntry(999L))
                .thenThrow(new NotFoundException("Journal entry not found with id: 999"));

        // When / Then
        mockMvc.perform(get(BASE_URL + "/999"))
                .andExpect(status().isNotFound());
    }

    // ==================== POST /api/v1/finance/journal-entries ====================

    @Test
    @WithMockUser(authorities = "FINANCE_JE_CREATE")
    void createJournalEntry_returns201_balanced() throws Exception {
        // Given
        CreateJournalEntryRequest request = buildCreateRequest();
        JournalEntryDto dto = buildJournalEntryDto();
        when(journalEntryService.createJournalEntry(any(CreateJournalEntryRequest.class))).thenReturn(dto);

        // When / Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.balanced").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_JE_CREATE")
    void createJournalEntry_returns400_unbalanced() throws Exception {
        // Given
        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("Unbalanced entry")
                .lines(List.of(
                        CreateJournalLineRequest.builder()
                                .accountId(1L)
                                .debitAmount(new BigDecimal("1000.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .build(),
                        CreateJournalLineRequest.builder()
                                .accountId(2L)
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("500.00"))
                                .build()
                ))
                .build();

        when(journalEntryService.createJournalEntry(any(CreateJournalEntryRequest.class)))
                .thenThrow(new BusinessException("Journal entry is not balanced"));

        // When / Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_JE_CREATE")
    void createJournalEntry_closedPeriod_returns400() throws Exception {
        // Given
        CreateJournalEntryRequest request = buildCreateRequest();
        when(journalEntryService.createJournalEntry(any(CreateJournalEntryRequest.class)))
                .thenThrow(new BusinessException("Fiscal period is closed"));

        // When / Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_JE_CREATE")
    void createJournalEntry_emptyLines_returns400() throws Exception {
        // Given
        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("Entry with no lines")
                .lines(Collections.emptyList())
                .build();

        // When / Then - validation will reject empty lines
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJournalEntry_unauthenticated_returns403() throws Exception {
        // Given
        CreateJournalEntryRequest request = buildCreateRequest();

        // When / Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/finance/journal-entries/{id}/post ====================

    @Test
    @WithMockUser(authorities = "FINANCE_GL_POST")
    void postJournalEntry_returns200() throws Exception {
        // Given
        JournalEntryDto postedDto = JournalEntryDto.builder()
                .id(1L)
                .entryNumber("JE-202401-0001")
                .status(JournalStatus.POSTED)
                .balanced(true)
                .build();
        when(journalEntryService.postJournalEntry(1L)).thenReturn(postedDto);

        // When / Then
        mockMvc.perform(post(BASE_URL + "/1/post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_GL_POST")
    void postJournalEntry_alreadyPosted_returns400() throws Exception {
        // Given
        when(journalEntryService.postJournalEntry(1L))
                .thenThrow(new BusinessException("Journal entry cannot be posted. Status: POSTED"));

        // When / Then
        mockMvc.perform(post(BASE_URL + "/1/post"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_GL_POST")
    void postJournalEntry_notFound_returns404() throws Exception {
        // Given
        when(journalEntryService.postJournalEntry(999L))
                .thenThrow(new NotFoundException("Journal entry not found with id: 999"));

        // When / Then
        mockMvc.perform(post(BASE_URL + "/999/post"))
                .andExpect(status().isNotFound());
    }

    // ==================== POST /api/v1/finance/journal-entries/{id}/reverse ====================

    @Test
    @WithMockUser(authorities = "FINANCE_GL_REVERSE")
    void reverseJournalEntry_returns200_reversalCreated() throws Exception {
        // Given
        JournalEntryDto reversalDto = JournalEntryDto.builder()
                .id(2L)
                .entryNumber("JE-202401-0002")
                .status(JournalStatus.POSTED)
                .reversedEntryId(1L)
                .balanced(true)
                .build();
        when(journalEntryService.reverseJournalEntry(eq(1L), any(LocalDate.class))).thenReturn(reversalDto);

        // When / Then
        mockMvc.perform(post(BASE_URL + "/1/reverse")
                        .param("reversalDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.reversedEntryId").value(1));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_GL_REVERSE")
    void reverseJournalEntry_notPosted_returns400() throws Exception {
        // Given
        when(journalEntryService.reverseJournalEntry(eq(1L), any(LocalDate.class)))
                .thenThrow(new BusinessException("Journal entry cannot be reversed. Status: DRAFT"));

        // When / Then
        mockMvc.perform(post(BASE_URL + "/1/reverse")
                        .param("reversalDate", "2024-01-31"))
                .andExpect(status().isBadRequest());
    }

    // ==================== DELETE /api/v1/finance/journal-entries/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_JE_CREATE")
    void deleteJournalEntry_returns204() throws Exception {
        // Given
        doNothing().when(journalEntryService).deleteJournalEntry(1L);

        // When / Then
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());

        verify(journalEntryService).deleteJournalEntry(1L);
    }

    @Test
    @WithMockUser(authorities = "FINANCE_JE_CREATE")
    void deleteJournalEntry_posted_returns400() throws Exception {
        // Given
        doThrow(new BusinessException("Only draft entries can be deleted"))
                .when(journalEntryService).deleteJournalEntry(1L);

        // When / Then
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /api/v1/finance/journal-entries/status/{status} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_JE_VIEW")
    void getJournalEntriesByStatus_returns200() throws Exception {
        // Given
        JournalEntryDto dto = buildJournalEntryDto();
        PageResponse<JournalEntryDto> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(dto))
        );
        when(journalEntryService.getJournalEntriesByStatus(eq(JournalStatus.DRAFT), any())).thenReturn(pageResponse);

        // When / Then
        mockMvc.perform(get(BASE_URL + "/status/DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    // ==================== GET /api/v1/finance/journal-entries/date-range ====================

    @Test
    @WithMockUser(authorities = "FINANCE_JE_VIEW")
    void getJournalEntriesByDateRange_returns200() throws Exception {
        // Given
        JournalEntryDto dto = buildJournalEntryDto();
        when(journalEntryService.getJournalEntriesByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(dto));

        // When / Then
        mockMvc.perform(get(BASE_URL + "/date-range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entryNumber").value("JE-202401-0001"));
    }

    // ==================== GET /api/v1/finance/journal-entries/{id}/lines ====================

    @Test
    @WithMockUser(authorities = "FINANCE_JE_VIEW")
    void getJournalEntryWithLines_returns200() throws Exception {
        // Given
        JournalEntryDto dto = buildJournalEntryDto();
        when(journalEntryService.getJournalEntryWithLines(1L)).thenReturn(dto);

        // When / Then
        mockMvc.perform(get(BASE_URL + "/1/lines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].accountCode").value("1110"))
                .andExpect(jsonPath("$.lines[1].accountCode").value("4100"));
    }

    // ==================== GET /api/v1/finance/journal-entries/search ====================

    @Test
    @WithMockUser(authorities = "FINANCE_JE_VIEW")
    void searchJournalEntries_returns200() throws Exception {
        // Given
        JournalEntryDto dto = buildJournalEntryDto();
        PageResponse<JournalEntryDto> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(dto))
        );
        when(journalEntryService.searchJournalEntries(eq("test"), any())).thenReturn(pageResponse);

        // When / Then
        mockMvc.perform(get(BASE_URL + "/search")
                        .param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Test journal entry"));
    }
}
