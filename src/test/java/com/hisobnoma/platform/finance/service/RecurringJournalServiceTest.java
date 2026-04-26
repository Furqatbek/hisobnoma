package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.JournalEntryDto;
import com.hisobnoma.platform.finance.dto.RecurringJournalTemplateDto;
import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.AccountType;
import com.hisobnoma.platform.finance.entity.NormalBalance;
import com.hisobnoma.platform.finance.entity.RecurringJournalLine;
import com.hisobnoma.platform.finance.entity.RecurringJournalTemplate;
import com.hisobnoma.platform.finance.entity.RecurringJournalTemplate.RecurrenceFrequency;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.RecurringJournalTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringJournalServiceTest {

    @Mock
    private RecurringJournalTemplateRepository templateRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JournalEntryService journalEntryService;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private RecurringJournalService recurringJournalService;

    private RecurringJournalTemplate template;
    private RecurringJournalTemplateDto templateDto;
    private Account cashAccount;
    private Account expenseAccount;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        cashAccount = Account.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .code("1110")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .active(true)
                .build();

        expenseAccount = Account.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .code("5210")
                .name("Rent Expense")
                .accountType(AccountType.EXPENSE)
                .normalBalance(NormalBalance.DEBIT)
                .active(true)
                .build();

        template = RecurringJournalTemplate.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .name("Monthly Rent")
                .description("Monthly office rent payment")
                .frequency(RecurrenceFrequency.MONTHLY)
                .frequencyDay(1)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(null)
                .nextRunDate(LocalDate.of(2024, 2, 1))
                .lastRunDate(LocalDate.of(2024, 1, 1))
                .runCount(1)
                .active(true)
                .autoPost(false)
                .lines(new ArrayList<>())
                .build();

        RecurringJournalLine debitLine = RecurringJournalLine.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .template(template)
                .account(expenseAccount)
                .description("Rent expense")
                .debitAmount(new BigDecimal("5000.00"))
                .creditAmount(BigDecimal.ZERO)
                .sortOrder(1)
                .build();

        RecurringJournalLine creditLine = RecurringJournalLine.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .template(template)
                .account(cashAccount)
                .description("Cash payment")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("5000.00"))
                .sortOrder(2)
                .build();

        template.getLines().add(debitLine);
        template.getLines().add(creditLine);

        templateDto = RecurringJournalTemplateDto.builder()
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

    // ==================== getTemplates ====================

    @Test
    void getTemplates_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<RecurringJournalTemplate> page = new PageImpl<>(List.of(template), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

        // When
        PageResponse<RecurringJournalTemplateDto> result = recurringJournalService.getTemplates(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Monthly Rent", result.getContent().get(0).getName());
    }

    // ==================== getTemplate ====================

    @Test
    void getTemplate_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.findByIdWithLines(1L, TENANT_ID)).thenReturn(Optional.of(template));

        // When
        RecurringJournalTemplateDto result = recurringJournalService.getTemplate(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Monthly Rent", result.getName());
        assertEquals(2, result.getLines().size());
    }

    @Test
    void getTemplate_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.findByIdWithLines(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> recurringJournalService.getTemplate(999L));
    }

    // ==================== createTemplate ====================

    @Test
    void createTemplate_success_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.existsByNameAndTenantId("Monthly Rent", TENANT_ID)).thenReturn(false);
        when(accountRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(expenseAccount));
        when(accountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(cashAccount));
        when(templateRepository.save(any(RecurringJournalTemplate.class))).thenReturn(template);

        // When
        RecurringJournalTemplateDto result = recurringJournalService.createTemplate(templateDto);

        // Then
        assertNotNull(result);
        assertEquals("Monthly Rent", result.getName());
        verify(templateRepository).save(any(RecurringJournalTemplate.class));
    }

    @Test
    void createTemplate_duplicateName_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.existsByNameAndTenantId("Monthly Rent", TENANT_ID)).thenReturn(true);

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> recurringJournalService.createTemplate(templateDto));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void createTemplate_unbalancedLines_throwsBusinessException() {
        // Given
        RecurringJournalTemplateDto unbalancedDto = RecurringJournalTemplateDto.builder()
                .name("Unbalanced Template")
                .frequency(RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.of(2024, 1, 1))
                .active(true)
                .lines(List.of(
                        RecurringJournalTemplateDto.LineDto.builder()
                                .accountId(2L)
                                .debitAmount(new BigDecimal("5000.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .build(),
                        RecurringJournalTemplateDto.LineDto.builder()
                                .accountId(1L)
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("3000.00"))
                                .build()
                ))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.existsByNameAndTenantId("Unbalanced Template", TENANT_ID)).thenReturn(false);

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> recurringJournalService.createTemplate(unbalancedDto));
        assertTrue(ex.getMessage().contains("Total debits must equal total credits"));
    }

    // ==================== updateTemplate ====================

    @Test
    void updateTemplate_success_returnsDto() {
        // Given
        RecurringJournalTemplateDto updateDto = RecurringJournalTemplateDto.builder()
                .name("Updated Monthly Rent")
                .description("Updated description")
                .frequency(RecurrenceFrequency.MONTHLY)
                .frequencyDay(15)
                .startDate(LocalDate.of(2024, 1, 1))
                .active(true)
                .autoPost(true)
                .lines(List.of(
                        RecurringJournalTemplateDto.LineDto.builder()
                                .accountId(2L)
                                .description("Rent")
                                .debitAmount(new BigDecimal("6000.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .build(),
                        RecurringJournalTemplateDto.LineDto.builder()
                                .accountId(1L)
                                .description("Cash")
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("6000.00"))
                                .build()
                ))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.findByIdWithLines(1L, TENANT_ID)).thenReturn(Optional.of(template));
        when(accountRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(expenseAccount));
        when(accountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(cashAccount));
        when(templateRepository.save(any(RecurringJournalTemplate.class))).thenReturn(template);

        // When
        RecurringJournalTemplateDto result = recurringJournalService.updateTemplate(1L, updateDto);

        // Then
        assertNotNull(result);
        verify(templateRepository).save(any(RecurringJournalTemplate.class));
    }

    @Test
    void updateTemplate_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.findByIdWithLines(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> recurringJournalService.updateTemplate(999L, templateDto));
    }

    // ==================== deleteTemplate ====================

    @Test
    void deleteTemplate_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(template));

        // When
        recurringJournalService.deleteTemplate(1L);

        // Then
        verify(templateRepository).delete(template);
    }

    @Test
    void deleteTemplate_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> recurringJournalService.deleteTemplate(999L));
    }

    // ==================== activateTemplate ====================

    @Test
    void activateTemplate_success() {
        // Given
        template.setActive(false);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(RecurringJournalTemplate.class))).thenReturn(template);

        // When
        RecurringJournalTemplateDto result = recurringJournalService.activateTemplate(1L);

        // Then
        assertNotNull(result);
        assertTrue(template.isActive());
        verify(templateRepository).save(template);
    }

    // ==================== deactivateTemplate ====================

    @Test
    void deactivateTemplate_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(RecurringJournalTemplate.class))).thenReturn(template);

        // When
        RecurringJournalTemplateDto result = recurringJournalService.deactivateTemplate(1L);

        // Then
        assertNotNull(result);
        assertFalse(template.isActive());
        verify(templateRepository).save(template);
    }

    // ==================== executeTemplate (generateEntries) ====================

    @Test
    void executeTemplate_success_createsJournalEntry() {
        // Given
        JournalEntryDto createdEntry = JournalEntryDto.builder()
                .id(10L)
                .entryNumber("JE-202402-0001")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.findByIdWithLines(1L, TENANT_ID)).thenReturn(Optional.of(template));
        when(journalEntryService.createJournalEntry(any())).thenReturn(createdEntry);
        when(templateRepository.save(any(RecurringJournalTemplate.class))).thenReturn(template);

        // When
        Long journalId = recurringJournalService.executeTemplate(1L);

        // Then
        assertEquals(10L, journalId);
        verify(journalEntryService).createJournalEntry(any());
        verify(templateRepository).save(template);
    }

    @Test
    void executeTemplate_withAutoPost_postsEntry() {
        // Given
        template.setAutoPost(true);

        JournalEntryDto createdEntry = JournalEntryDto.builder()
                .id(10L)
                .entryNumber("JE-202402-0001")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.findByIdWithLines(1L, TENANT_ID)).thenReturn(Optional.of(template));
        when(journalEntryService.createJournalEntry(any())).thenReturn(createdEntry);
        when(templateRepository.save(any(RecurringJournalTemplate.class))).thenReturn(template);

        // When
        Long journalId = recurringJournalService.executeTemplate(1L);

        // Then
        assertEquals(10L, journalId);
        verify(journalEntryService).createJournalEntry(any());
        verify(journalEntryService).postJournalEntry(10L);
    }

    @Test
    void executeTemplate_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(templateRepository.findByIdWithLines(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> recurringJournalService.executeTemplate(999L));
    }

    // ==================== processDueTemplates ====================

    @Test
    void processDueTemplates_processesAllDueTemplates() {
        // Given
        JournalEntryDto createdEntry = JournalEntryDto.builder()
                .id(10L)
                .entryNumber("JE-202402-0001")
                .build();

        when(templateRepository.findDueTemplates(eq(TENANT_ID), any(LocalDate.class)))
                .thenReturn(List.of(template));
        when(journalEntryService.createJournalEntry(any())).thenReturn(createdEntry);
        when(templateRepository.save(any(RecurringJournalTemplate.class))).thenReturn(template);

        // When
        int processed = recurringJournalService.processDueTemplates(TENANT_ID);

        // Then
        assertEquals(1, processed);
        verify(journalEntryService).createJournalEntry(any());
    }

    @Test
    void processDueTemplates_noDueTemplates_returnsZero() {
        // Given
        when(templateRepository.findDueTemplates(eq(TENANT_ID), any(LocalDate.class)))
                .thenReturn(List.of());

        // When
        int processed = recurringJournalService.processDueTemplates(TENANT_ID);

        // Then
        assertEquals(0, processed);
        verify(journalEntryService, never()).createJournalEntry(any());
    }
}
