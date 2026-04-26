package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateJournalEntryRequest;
import com.hisobnoma.platform.finance.dto.CreateJournalLineRequest;
import com.hisobnoma.platform.finance.dto.JournalEntryDto;
import com.hisobnoma.platform.finance.dto.JournalLineDto;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.mapper.JournalEntryMapper;
import com.hisobnoma.platform.finance.mapper.JournalLineMapper;
import com.hisobnoma.platform.finance.repository.JournalEntryRepository;
import com.hisobnoma.platform.finance.repository.JournalLineRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private JournalLineRepository journalLineRepository;

    @Mock
    private JournalEntryMapper journalEntryMapper;

    @Mock
    private JournalLineMapper journalLineMapper;

    @Mock
    private AccountService accountService;

    @Mock
    private FiscalPeriodService fiscalPeriodService;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private JournalEntryService journalEntryService;

    private JournalEntry journalEntry;
    private JournalEntryDto journalEntryDto;
    private Account cashAccount;
    private Account revenueAccount;
    private FiscalPeriod fiscalPeriod;
    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        fiscalPeriod = FiscalPeriod.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .name("January 2024")
                .periodNumber(1)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(PeriodStatus.OPEN)
                .build();

        cashAccount = Account.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .code("1110")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .active(true)
                .allowsDirectPosting(true)
                .ytdDebit(BigDecimal.ZERO)
                .ytdCredit(BigDecimal.ZERO)
                .openingBalance(BigDecimal.ZERO)
                .currentBalance(BigDecimal.ZERO)
                .build();

        revenueAccount = Account.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .code("4100")
                .name("Sales Revenue")
                .accountType(AccountType.REVENUE)
                .normalBalance(NormalBalance.CREDIT)
                .active(true)
                .allowsDirectPosting(true)
                .ytdDebit(BigDecimal.ZERO)
                .ytdCredit(BigDecimal.ZERO)
                .openingBalance(BigDecimal.ZERO)
                .currentBalance(BigDecimal.ZERO)
                .build();

        journalEntry = JournalEntry.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .entryNumber("JE-202401-0001")
                .entryDate(LocalDate.of(2024, 1, 15))
                .fiscalPeriod(fiscalPeriod)
                .description("Test journal entry")
                .status(JournalStatus.DRAFT)
                .source(JournalSource.MANUAL)
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .totalDebit(new BigDecimal("1000.00"))
                .totalCredit(new BigDecimal("1000.00"))
                .lines(new ArrayList<>())
                .build();

        // Add balanced lines
        JournalLine debitLine = JournalLine.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .journalEntry(journalEntry)
                .lineNumber(1)
                .account(cashAccount)
                .description("Cash received")
                .debitAmount(new BigDecimal("1000.00"))
                .creditAmount(BigDecimal.ZERO)
                .baseDebitAmount(new BigDecimal("1000.00"))
                .baseCreditAmount(BigDecimal.ZERO)
                .build();

        JournalLine creditLine = JournalLine.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .journalEntry(journalEntry)
                .lineNumber(2)
                .account(revenueAccount)
                .description("Sales revenue")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("1000.00"))
                .baseDebitAmount(BigDecimal.ZERO)
                .baseCreditAmount(new BigDecimal("1000.00"))
                .build();

        journalEntry.getLines().add(debitLine);
        journalEntry.getLines().add(creditLine);

        journalEntryDto = JournalEntryDto.builder()
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

    // ==================== getJournalEntries ====================

    @Test
    void getJournalEntries_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<JournalEntry> page = new PageImpl<>(List.of(journalEntry), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(journalEntryMapper.toDtoWithoutLines(journalEntry)).thenReturn(journalEntryDto);

        // When
        PageResponse<JournalEntryDto> result = journalEntryService.getJournalEntries(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("JE-202401-0001", result.getContent().get(0).getEntryNumber());
    }

    @Test
    void getJournalEntries_returnsEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<JournalEntry> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(emptyPage);

        // When
        PageResponse<JournalEntryDto> result = journalEntryService.getJournalEntries(pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // ==================== getJournalEntry ====================

    @Test
    void getJournalEntry_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(journalEntry));
        when(journalEntryMapper.toDto(journalEntry)).thenReturn(journalEntryDto);

        // When
        JournalEntryDto result = journalEntryService.getJournalEntry(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("JE-202401-0001", result.getEntryNumber());
        assertEquals(JournalStatus.DRAFT, result.getStatus());
    }

    @Test
    void getJournalEntry_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> journalEntryService.getJournalEntry(999L));
    }

    // ==================== createJournalEntry ====================

    @Test
    void createJournalEntry_balanced_returnsDraftDto() {
        // Given
        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("Test entry")
                .lines(List.of(
                        CreateJournalLineRequest.builder()
                                .accountId(1L)
                                .debitAmount(new BigDecimal("1000.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .description("Cash received")
                                .build(),
                        CreateJournalLineRequest.builder()
                                .accountId(2L)
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("1000.00"))
                                .description("Sales revenue")
                                .build()
                ))
                .build();

        JournalEntry mappedEntry = JournalEntry.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("Test entry")
                .lines(new ArrayList<>())
                .totalDebit(BigDecimal.ZERO)
                .totalCredit(BigDecimal.ZERO)
                .build();

        JournalLine mappedDebitLine = JournalLine.builder()
                .debitAmount(new BigDecimal("1000.00"))
                .creditAmount(BigDecimal.ZERO)
                .build();

        JournalLine mappedCreditLine = JournalLine.builder()
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("1000.00"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        doNothing().when(fiscalPeriodService).validatePostingAllowed(any(LocalDate.class));
        when(fiscalPeriodService.getPeriodEntityByDate(any(LocalDate.class))).thenReturn(fiscalPeriod);
        when(journalEntryRepository.findMaxEntryNumberForPrefix(any(), eq(TENANT_ID))).thenReturn(null);
        when(journalEntryMapper.toEntity(request)).thenReturn(mappedEntry);
        when(journalLineMapper.toEntity(request.getLines().get(0))).thenReturn(mappedDebitLine);
        when(journalLineMapper.toEntity(request.getLines().get(1))).thenReturn(mappedCreditLine);
        when(accountService.getAccountEntity(1L)).thenReturn(cashAccount);
        when(accountService.getAccountEntity(2L)).thenReturn(revenueAccount);
        when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(journalEntry);
        when(journalEntryMapper.toDto(journalEntry)).thenReturn(journalEntryDto);

        // When
        JournalEntryDto result = journalEntryService.createJournalEntry(request);

        // Then
        assertNotNull(result);
        assertEquals(JournalStatus.DRAFT, result.getStatus());
        assertTrue(result.isBalanced());
        verify(journalEntryRepository).save(any(JournalEntry.class));
    }

    @Test
    void createJournalEntry_unbalanced_throwsBusinessException() {
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

        JournalEntry mappedEntry = JournalEntry.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("Unbalanced entry")
                .lines(new ArrayList<>())
                .totalDebit(BigDecimal.ZERO)
                .totalCredit(BigDecimal.ZERO)
                .exchangeRate(BigDecimal.ONE)
                .build();

        JournalLine mappedDebitLine = JournalLine.builder()
                .debitAmount(new BigDecimal("1000.00"))
                .creditAmount(BigDecimal.ZERO)
                .build();

        JournalLine mappedCreditLine = JournalLine.builder()
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("500.00"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        doNothing().when(fiscalPeriodService).validatePostingAllowed(any(LocalDate.class));
        when(fiscalPeriodService.getPeriodEntityByDate(any(LocalDate.class))).thenReturn(fiscalPeriod);
        when(journalEntryRepository.findMaxEntryNumberForPrefix(any(), eq(TENANT_ID))).thenReturn(null);
        when(journalEntryMapper.toEntity(request)).thenReturn(mappedEntry);
        when(journalLineMapper.toEntity(request.getLines().get(0))).thenReturn(mappedDebitLine);
        when(journalLineMapper.toEntity(request.getLines().get(1))).thenReturn(mappedCreditLine);
        when(accountService.getAccountEntity(1L)).thenReturn(cashAccount);
        when(accountService.getAccountEntity(2L)).thenReturn(revenueAccount);

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> journalEntryService.createJournalEntry(request));
        assertTrue(ex.getMessage().contains("not balanced"));
    }

    @Test
    void createJournalEntry_accountNotFound_throwsNotFoundException() {
        // Given
        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("Entry with missing account")
                .lines(List.of(
                        CreateJournalLineRequest.builder()
                                .accountId(999L)
                                .debitAmount(new BigDecimal("1000.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .build(),
                        CreateJournalLineRequest.builder()
                                .accountId(2L)
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("1000.00"))
                                .build()
                ))
                .build();

        JournalEntry mappedEntry = JournalEntry.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("Entry with missing account")
                .lines(new ArrayList<>())
                .totalDebit(BigDecimal.ZERO)
                .totalCredit(BigDecimal.ZERO)
                .exchangeRate(BigDecimal.ONE)
                .build();

        JournalLine mappedDebitLine = JournalLine.builder()
                .debitAmount(new BigDecimal("1000.00"))
                .creditAmount(BigDecimal.ZERO)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        doNothing().when(fiscalPeriodService).validatePostingAllowed(any(LocalDate.class));
        when(fiscalPeriodService.getPeriodEntityByDate(any(LocalDate.class))).thenReturn(fiscalPeriod);
        when(journalEntryRepository.findMaxEntryNumberForPrefix(any(), eq(TENANT_ID))).thenReturn(null);
        when(journalEntryMapper.toEntity(request)).thenReturn(mappedEntry);
        when(journalLineMapper.toEntity(request.getLines().get(0))).thenReturn(mappedDebitLine);
        when(accountService.getAccountEntity(999L))
                .thenThrow(new NotFoundException("Account not found with id: 999"));

        // When/Then
        assertThrows(NotFoundException.class,
                () -> journalEntryService.createJournalEntry(request));
    }

    @Test
    void createJournalEntry_closedPeriod_throwsBusinessException() {
        // Given
        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("Entry in closed period")
                .lines(List.of(
                        CreateJournalLineRequest.builder()
                                .accountId(1L)
                                .debitAmount(new BigDecimal("1000.00"))
                                .build(),
                        CreateJournalLineRequest.builder()
                                .accountId(2L)
                                .creditAmount(new BigDecimal("1000.00"))
                                .build()
                ))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        doThrow(new BusinessException("Fiscal period is closed"))
                .when(fiscalPeriodService).validatePostingAllowed(any(LocalDate.class));

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> journalEntryService.createJournalEntry(request));
        assertTrue(ex.getMessage().contains("Fiscal period is closed"));
    }

    @Test
    void createJournalEntry_inactiveAccount_throwsBusinessException() {
        // Given
        Account inactiveAccount = Account.builder()
                .id(3L)
                .code("1200")
                .name("Inactive Account")
                .active(false)
                .allowsDirectPosting(true)
                .build();

        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("Entry with inactive account")
                .lines(List.of(
                        CreateJournalLineRequest.builder()
                                .accountId(3L)
                                .debitAmount(new BigDecimal("1000.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .build(),
                        CreateJournalLineRequest.builder()
                                .accountId(2L)
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("1000.00"))
                                .build()
                ))
                .build();

        JournalEntry mappedEntry = JournalEntry.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("Entry with inactive account")
                .lines(new ArrayList<>())
                .totalDebit(BigDecimal.ZERO)
                .totalCredit(BigDecimal.ZERO)
                .exchangeRate(BigDecimal.ONE)
                .build();

        JournalLine mappedDebitLine = JournalLine.builder()
                .debitAmount(new BigDecimal("1000.00"))
                .creditAmount(BigDecimal.ZERO)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        doNothing().when(fiscalPeriodService).validatePostingAllowed(any(LocalDate.class));
        when(fiscalPeriodService.getPeriodEntityByDate(any(LocalDate.class))).thenReturn(fiscalPeriod);
        when(journalEntryRepository.findMaxEntryNumberForPrefix(any(), eq(TENANT_ID))).thenReturn(null);
        when(journalEntryMapper.toEntity(request)).thenReturn(mappedEntry);
        when(journalLineMapper.toEntity(request.getLines().get(0))).thenReturn(mappedDebitLine);
        when(accountService.getAccountEntity(3L)).thenReturn(inactiveAccount);

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> journalEntryService.createJournalEntry(request));
        assertTrue(ex.getMessage().contains("not active"));
    }

    // ==================== postJournalEntry ====================

    @Test
    void postJournalEntry_draft_updatesLedgerBalances() {
        // Given
        JournalEntryDto postedDto = JournalEntryDto.builder()
                .id(1L)
                .entryNumber("JE-202401-0001")
                .status(JournalStatus.POSTED)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(journalEntryRepository.findByIdWithLines(1L)).thenReturn(Optional.of(journalEntry));
        doNothing().when(fiscalPeriodService).validatePostingAllowed(any(LocalDate.class));
        doNothing().when(accountService).updateAccountBalance(any(), any(), any());
        when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(journalEntry);
        when(journalEntryMapper.toDto(journalEntry)).thenReturn(postedDto);

        // When
        JournalEntryDto result = journalEntryService.postJournalEntry(1L);

        // Then
        assertNotNull(result);
        assertEquals(JournalStatus.POSTED, result.getStatus());
        verify(accountService, times(2)).updateAccountBalance(any(), any(), any());
        verify(journalEntryRepository).save(any(JournalEntry.class));
    }

    @Test
    void postJournalEntry_alreadyPosted_throwsBusinessException() {
        // Given
        journalEntry.setStatus(JournalStatus.POSTED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(journalEntryRepository.findByIdWithLines(1L)).thenReturn(Optional.of(journalEntry));

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> journalEntryService.postJournalEntry(1L));
        assertTrue(ex.getMessage().contains("cannot be posted"));
    }

    @Test
    void postJournalEntry_reversed_throwsBusinessException() {
        // Given
        journalEntry.setStatus(JournalStatus.REVERSED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(journalEntryRepository.findByIdWithLines(1L)).thenReturn(Optional.of(journalEntry));

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> journalEntryService.postJournalEntry(1L));
        assertTrue(ex.getMessage().contains("cannot be posted"));
    }

    @Test
    void postJournalEntry_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(journalEntryRepository.findByIdWithLines(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> journalEntryService.postJournalEntry(999L));
    }

    // ==================== reverseJournalEntry ====================

    @Test
    void reverseJournalEntry_posted_createsReversalEntry() {
        // Given
        journalEntry.setStatus(JournalStatus.POSTED);
        journalEntry.setPostedAt(Instant.now());
        journalEntry.setPostedBy(USER_ID);

        JournalEntry reversingEntry = JournalEntry.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .entryNumber("JE-202401-0002")
                .status(JournalStatus.POSTED)
                .lines(new ArrayList<>())
                .build();

        JournalEntryDto reversalDto = JournalEntryDto.builder()
                .id(2L)
                .entryNumber("JE-202401-0002")
                .status(JournalStatus.POSTED)
                .reversedEntryId(1L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(journalEntryRepository.findByIdWithLines(1L)).thenReturn(Optional.of(journalEntry));
        doNothing().when(fiscalPeriodService).validatePostingAllowed(any(LocalDate.class));
        when(fiscalPeriodService.getPeriodEntityByDate(any(LocalDate.class))).thenReturn(fiscalPeriod);
        when(journalEntryRepository.findMaxEntryNumberForPrefix(any(), eq(TENANT_ID))).thenReturn(1);
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(inv -> {
            JournalEntry entry = inv.getArgument(0);
            if (entry.getId() == null) {
                entry.setId(2L);
            }
            return entry;
        });
        doNothing().when(accountService).updateAccountBalance(any(), any(), any());
        when(journalEntryMapper.toDto(any(JournalEntry.class))).thenReturn(reversalDto);

        // When
        JournalEntryDto result = journalEntryService.reverseJournalEntry(1L, LocalDate.of(2024, 1, 31));

        // Then
        assertNotNull(result);
        assertEquals(JournalStatus.POSTED, result.getStatus());
        assertEquals(1L, result.getReversedEntryId());
        // Verify the original entry was saved as REVERSED
        verify(journalEntryRepository, atLeast(2)).save(any(JournalEntry.class));
    }

    @Test
    void reverseJournalEntry_draft_throwsBusinessException() {
        // Given
        journalEntry.setStatus(JournalStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(journalEntryRepository.findByIdWithLines(1L)).thenReturn(Optional.of(journalEntry));

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> journalEntryService.reverseJournalEntry(1L, LocalDate.of(2024, 1, 31)));
        assertTrue(ex.getMessage().contains("cannot be reversed"));
    }

    @Test
    void reverseJournalEntry_alreadyReversed_throwsBusinessException() {
        // Given
        journalEntry.setStatus(JournalStatus.POSTED);
        journalEntry.setReversingEntryId(99L); // already has a reversing entry
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(journalEntryRepository.findByIdWithLines(1L)).thenReturn(Optional.of(journalEntry));

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> journalEntryService.reverseJournalEntry(1L, LocalDate.of(2024, 1, 31)));
        assertTrue(ex.getMessage().contains("cannot be reversed"));
    }

    // ==================== getJournalEntriesByDateRange ====================

    @Test
    void getJournalEntriesByDateRange_returnsEntriesInRange() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryRepository.findByDateRangeAndTenantId(TENANT_ID, startDate, endDate))
                .thenReturn(List.of(journalEntry));
        when(journalEntryMapper.toDtoList(List.of(journalEntry))).thenReturn(List.of(journalEntryDto));

        // When
        List<JournalEntryDto> result = journalEntryService.getJournalEntriesByDateRange(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(journalEntryRepository).findByDateRangeAndTenantId(TENANT_ID, startDate, endDate);
    }

    // ==================== getJournalEntriesByStatus ====================

    @Test
    void getJournalEntriesByStatus_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<JournalEntry> page = new PageImpl<>(List.of(journalEntry), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryRepository.findByStatusAndTenantId(TENANT_ID, JournalStatus.DRAFT, pageable))
                .thenReturn(page);
        when(journalEntryMapper.toDtoWithoutLines(journalEntry)).thenReturn(journalEntryDto);

        // When
        PageResponse<JournalEntryDto> result = journalEntryService.getJournalEntriesByStatus(JournalStatus.DRAFT, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ==================== deleteJournalEntry ====================

    @Test
    void deleteJournalEntry_draft_succeeds() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(journalEntry));

        // When
        journalEntryService.deleteJournalEntry(1L);

        // Then
        verify(journalEntryRepository).delete(journalEntry);
    }

    @Test
    void deleteJournalEntry_posted_throwsBusinessException() {
        // Given
        journalEntry.setStatus(JournalStatus.POSTED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(journalEntry));

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> journalEntryService.deleteJournalEntry(1L));
        assertTrue(ex.getMessage().contains("Only draft entries"));
        verify(journalEntryRepository, never()).delete(any());
    }

    @Test
    void deleteJournalEntry_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> journalEntryService.deleteJournalEntry(999L));
    }

    // ==================== getJournalEntryWithLines ====================

    @Test
    void getJournalEntryWithLines_found_returnsDtoWithLines() {
        // Given
        when(journalEntryRepository.findByIdWithLines(1L)).thenReturn(Optional.of(journalEntry));
        when(journalEntryMapper.toDto(journalEntry)).thenReturn(journalEntryDto);

        // When
        JournalEntryDto result = journalEntryService.getJournalEntryWithLines(1L);

        // Then
        assertNotNull(result);
        assertNotNull(result.getLines());
        assertEquals(2, result.getLines().size());
    }

    @Test
    void getJournalEntryWithLines_notFound_throwsNotFoundException() {
        // Given
        when(journalEntryRepository.findByIdWithLines(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> journalEntryService.getJournalEntryWithLines(999L));
    }
}
