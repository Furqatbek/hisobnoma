package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateJournalEntryRequest;
import com.hisobnoma.platform.finance.dto.CreateJournalLineRequest;
import com.hisobnoma.platform.finance.dto.JournalEntryDto;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.mapper.JournalEntryMapper;
import com.hisobnoma.platform.finance.mapper.JournalLineMapper;
import com.hisobnoma.platform.finance.repository.JournalEntryRepository;
import com.hisobnoma.platform.finance.repository.JournalLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final JournalEntryMapper journalEntryMapper;
    private final JournalLineMapper journalLineMapper;
    private final AccountService accountService;
    private final FiscalPeriodService fiscalPeriodService;
    private final SecurityContextHelper securityContextHelper;

    private static final DateTimeFormatter ENTRY_NUMBER_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    @Transactional(readOnly = true)
    public PageResponse<JournalEntryDto> getJournalEntries(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<JournalEntry> page = journalEntryRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(journalEntryMapper::toDtoWithoutLines));
    }

    @Transactional(readOnly = true)
    public PageResponse<JournalEntryDto> getJournalEntriesByStatus(JournalStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<JournalEntry> page = journalEntryRepository.findByStatusAndTenantId(tenantId, status, pageable);
        return PageResponse.of(page.map(journalEntryMapper::toDtoWithoutLines));
    }

    @Transactional(readOnly = true)
    public PageResponse<JournalEntryDto> getJournalEntriesBySource(JournalSource source, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<JournalEntry> page = journalEntryRepository.findBySourceAndTenantId(tenantId, source, pageable);
        return PageResponse.of(page.map(journalEntryMapper::toDtoWithoutLines));
    }

    @Transactional(readOnly = true)
    public JournalEntryDto getJournalEntry(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        JournalEntry entry = journalEntryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Journal entry not found with id: " + id));
        return journalEntryMapper.toDto(entry);
    }

    @Transactional(readOnly = true)
    public JournalEntryDto getJournalEntryWithLines(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        JournalEntry entry = journalEntryRepository.findByIdWithLines(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Journal entry not found with id: " + id));
        return journalEntryMapper.toDto(entry);
    }

    @Transactional(readOnly = true)
    public List<JournalEntryDto> getJournalEntriesByDateRange(LocalDate startDate, LocalDate endDate) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<JournalEntry> entries = journalEntryRepository.findByDateRangeAndTenantId(tenantId, startDate, endDate);
        return journalEntryMapper.toDtoList(entries);
    }

    @Transactional(readOnly = true)
    public PageResponse<JournalEntryDto> searchJournalEntries(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<JournalEntry> page = journalEntryRepository.searchByTenantId(tenantId, search, pageable);
        return PageResponse.of(page.map(journalEntryMapper::toDtoWithoutLines));
    }

    @Transactional
    public JournalEntryDto createJournalEntry(CreateJournalEntryRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Validate that posting is allowed for the entry date
        fiscalPeriodService.validatePostingAllowed(request.getEntryDate());

        // Get the fiscal period for the entry date
        FiscalPeriod fiscalPeriod = fiscalPeriodService.getPeriodEntityByDate(request.getEntryDate());

        // Generate entry number
        String entryNumber = generateEntryNumber(request.getEntryDate(), tenantId);

        JournalEntry entry = journalEntryMapper.toEntity(request);
        entry.setTenantId(tenantId);
        entry.setEntryNumber(entryNumber);
        entry.setFiscalPeriod(fiscalPeriod);
        entry.setStatus(JournalStatus.DRAFT);

        if (request.getSource() == null) {
            entry.setSource(JournalSource.MANUAL);
        }

        if (request.getCurrency() == null || request.getCurrency().isEmpty()) {
            entry.setCurrency("UZS");
        }

        if (request.getExchangeRate() == null) {
            entry.setExchangeRate(BigDecimal.ONE);
        }

        // Create lines
        for (CreateJournalLineRequest lineRequest : request.getLines()) {
            JournalLine line = journalLineMapper.toEntity(lineRequest);
            line.setTenantId(tenantId);

            // Set the account
            Account account = accountService.getAccountEntity(lineRequest.getAccountId());
            if (!account.isActive()) {
                throw new BusinessException("Account '" + account.getCode() + "' is not active");
            }
            if (!account.isAllowsDirectPosting()) {
                throw new BusinessException("Account '" + account.getCode() + "' does not allow direct posting");
            }
            line.setAccount(account);

            // Set default amounts
            if (line.getDebitAmount() == null) {
                line.setDebitAmount(BigDecimal.ZERO);
            }
            if (line.getCreditAmount() == null) {
                line.setCreditAmount(BigDecimal.ZERO);
            }

            // Calculate base amounts
            line.setBaseDebitAmount(line.getDebitAmount().multiply(entry.getExchangeRate()));
            line.setBaseCreditAmount(line.getCreditAmount().multiply(entry.getExchangeRate()));

            entry.addLine(line);
        }

        // Validate balance
        if (!entry.isBalanced()) {
            throw new BusinessException("Journal entry is not balanced. Difference: " + entry.getImbalance());
        }

        entry = journalEntryRepository.save(entry);
        return journalEntryMapper.toDto(entry);
    }

    @Transactional
    public JournalEntryDto postJournalEntry(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        JournalEntry entry = journalEntryRepository.findByIdWithLines(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Journal entry not found with id: " + id));

        if (!entry.canPost()) {
            throw new BusinessException("Journal entry cannot be posted. Status: " + entry.getStatus() +
                    ", Balanced: " + entry.isBalanced() + ", Lines: " + entry.getLines().size());
        }

        // Validate that posting is still allowed
        fiscalPeriodService.validatePostingAllowed(entry.getEntryDate());

        // Post the entry
        entry.setStatus(JournalStatus.POSTED);
        entry.setPostedAt(Instant.now());
        entry.setPostedBy(userId);

        // Update account balances
        for (JournalLine line : entry.getLines()) {
            accountService.updateAccountBalance(
                    line.getAccount(),
                    line.getBaseDebitAmount(),
                    line.getBaseCreditAmount()
            );
        }

        entry = journalEntryRepository.save(entry);
        return journalEntryMapper.toDto(entry);
    }

    /**
     * Reverses a journal entry using today's date.
     * Convenience method for GL integration.
     */
    @Transactional
    public JournalEntry reverseEntry(Long id) {
        JournalEntryDto dto = reverseJournalEntry(id, LocalDate.now());
        return journalEntryRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Reversing entry not found"));
    }

    @Transactional
    public JournalEntryDto reverseJournalEntry(Long id, LocalDate reversalDate) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        JournalEntry originalEntry = journalEntryRepository.findByIdWithLines(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Journal entry not found with id: " + id));

        if (!originalEntry.canReverse()) {
            throw new BusinessException("Journal entry cannot be reversed. Status: " + originalEntry.getStatus());
        }

        // Validate that posting is allowed for the reversal date
        fiscalPeriodService.validatePostingAllowed(reversalDate);

        // Get the fiscal period for the reversal date
        FiscalPeriod fiscalPeriod = fiscalPeriodService.getPeriodEntityByDate(reversalDate);

        // Generate entry number for the reversing entry
        String entryNumber = generateEntryNumber(reversalDate, tenantId);

        // Create the reversing entry
        JournalEntry reversingEntry = JournalEntry.builder()
                .tenantId(tenantId)
                .entryNumber(entryNumber)
                .entryDate(reversalDate)
                .fiscalPeriod(fiscalPeriod)
                .description("Reversal of " + originalEntry.getEntryNumber() + ": " + originalEntry.getDescription())
                .status(JournalStatus.DRAFT)
                .source(originalEntry.getSource())
                .referenceType(originalEntry.getReferenceType())
                .referenceId(originalEntry.getReferenceId())
                .referenceNumber(originalEntry.getReferenceNumber())
                .currency(originalEntry.getCurrency())
                .exchangeRate(originalEntry.getExchangeRate())
                .reversedEntryId(originalEntry.getId())
                .build();

        // Create reversed lines (swap debit and credit)
        for (JournalLine originalLine : originalEntry.getLines()) {
            JournalLine reversedLine = JournalLine.builder()
                    .tenantId(tenantId)
                    .account(originalLine.getAccount())
                    .description(originalLine.getDescription())
                    .debitAmount(originalLine.getCreditAmount())
                    .creditAmount(originalLine.getDebitAmount())
                    .baseDebitAmount(originalLine.getBaseCreditAmount())
                    .baseCreditAmount(originalLine.getBaseDebitAmount())
                    .costCenter(originalLine.getCostCenter())
                    .projectCode(originalLine.getProjectCode())
                    .department(originalLine.getDepartment())
                    .build();
            reversingEntry.addLine(reversedLine);
        }

        // Save the reversing entry
        reversingEntry = journalEntryRepository.save(reversingEntry);

        // Post the reversing entry
        reversingEntry.setStatus(JournalStatus.POSTED);
        reversingEntry.setPostedAt(Instant.now());
        reversingEntry.setPostedBy(userId);

        // Update account balances
        for (JournalLine line : reversingEntry.getLines()) {
            accountService.updateAccountBalance(
                    line.getAccount(),
                    line.getBaseDebitAmount(),
                    line.getBaseCreditAmount()
            );
        }

        reversingEntry = journalEntryRepository.save(reversingEntry);

        // Update the original entry
        originalEntry.setStatus(JournalStatus.REVERSED);
        originalEntry.setReversedAt(Instant.now());
        originalEntry.setReversedBy(userId);
        originalEntry.setReversingEntryId(reversingEntry.getId());
        journalEntryRepository.save(originalEntry);

        return journalEntryMapper.toDto(reversingEntry);
    }

    @Transactional
    public void deleteJournalEntry(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        JournalEntry entry = journalEntryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Journal entry not found with id: " + id));

        if (entry.getStatus() != JournalStatus.DRAFT) {
            throw new BusinessException("Only draft entries can be deleted");
        }

        journalEntryRepository.delete(entry);
    }

    private String generateEntryNumber(LocalDate date, Long tenantId) {
        String prefix = "JE-" + date.format(ENTRY_NUMBER_FORMAT) + "-";
        Integer maxNumber = journalEntryRepository.findMaxEntryNumberForPrefix(prefix, tenantId);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return prefix + String.format("%04d", nextNumber);
    }

    /**
     * Creates a journal entry from an integration request (used by GLIntegrationService).
     */
    @Transactional
    public JournalEntry createAndPostEntry(CreateJournalEntryRequest request, Long tenantId) {
        // Get the fiscal period for the entry date, and refuse to post into a closed period —
        // AR/AP/POS posting funnels through here and must respect period-close like the manual path.
        FiscalPeriod fiscalPeriod = fiscalPeriodService.getPeriodEntityByDate(request.getEntryDate());
        if (!fiscalPeriod.allowsPosting()) {
            throw new BusinessException("Posting is not allowed for period: " + fiscalPeriod.getDisplayName()
                    + ". Period status: " + fiscalPeriod.getStatus());
        }

        // Generate entry number
        String entryNumber = generateEntryNumber(request.getEntryDate(), tenantId);

        JournalEntry entry = journalEntryMapper.toEntity(request);
        entry.setTenantId(tenantId);
        entry.setEntryNumber(entryNumber);
        entry.setFiscalPeriod(fiscalPeriod);
        entry.setStatus(JournalStatus.DRAFT);

        if (request.getCurrency() == null || request.getCurrency().isEmpty()) {
            entry.setCurrency("UZS");
        }

        if (request.getExchangeRate() == null) {
            entry.setExchangeRate(BigDecimal.ONE);
        }

        // Create lines
        for (CreateJournalLineRequest lineRequest : request.getLines()) {
            JournalLine line = journalLineMapper.toEntity(lineRequest);
            line.setTenantId(tenantId);

            Account account = accountService.getAccountEntity(lineRequest.getAccountId());
            if (!account.isActive()) {
                throw new BusinessException("Account '" + account.getCode() + "' is not active");
            }
            if (!account.isAllowsDirectPosting()) {
                throw new BusinessException("Account '" + account.getCode() + "' does not allow direct posting");
            }
            line.setAccount(account);

            if (line.getDebitAmount() == null) {
                line.setDebitAmount(BigDecimal.ZERO);
            }
            if (line.getCreditAmount() == null) {
                line.setCreditAmount(BigDecimal.ZERO);
            }

            line.setBaseDebitAmount(line.getDebitAmount().multiply(entry.getExchangeRate()));
            line.setBaseCreditAmount(line.getCreditAmount().multiply(entry.getExchangeRate()));

            entry.addLine(line);
        }

        // Validate balance
        if (!entry.isBalanced()) {
            throw new BusinessException("Journal entry is not balanced. Difference: " + entry.getImbalance());
        }

        // Save and post
        entry = journalEntryRepository.save(entry);
        entry.setStatus(JournalStatus.POSTED);
        entry.setPostedAt(Instant.now());

        // Update account balances
        for (JournalLine line : entry.getLines()) {
            accountService.updateAccountBalance(
                    line.getAccount(),
                    line.getBaseDebitAmount(),
                    line.getBaseCreditAmount()
            );
        }

        return journalEntryRepository.save(entry);
    }
}
