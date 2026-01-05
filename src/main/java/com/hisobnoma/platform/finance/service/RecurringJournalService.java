package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.RecurringJournalTemplateDto;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.RecurringJournalTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing recurring journal templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringJournalService {

    private final RecurringJournalTemplateRepository templateRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryService journalEntryService;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<RecurringJournalTemplateDto> getTemplates(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<RecurringJournalTemplate> page = templateRepository.findByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public RecurringJournalTemplateDto getTemplate(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        RecurringJournalTemplate template = templateRepository.findByIdWithLines(id, tenantId)
                .orElseThrow(() -> new NotFoundException("RecurringJournalTemplate", id));
        return toDto(template);
    }

    @Transactional
    public RecurringJournalTemplateDto createTemplate(RecurringJournalTemplateDto dto) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        if (templateRepository.existsByNameAndTenantId(dto.getName(), tenantId)) {
            throw new BusinessException("Template with name '" + dto.getName() + "' already exists");
        }

        // Validate lines balance
        validateLinesBalance(dto.getLines());

        RecurringJournalTemplate template = RecurringJournalTemplate.builder()
                .tenantId(tenantId)
                .name(dto.getName())
                .description(dto.getDescription())
                .frequency(dto.getFrequency())
                .frequencyDay(dto.getFrequencyDay())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .active(dto.isActive())
                .autoPost(dto.isAutoPost())
                .runCount(0)
                .build();

        // Set next run date
        template.setNextRunDate(dto.getStartDate());

        // Add lines
        for (RecurringJournalTemplateDto.LineDto lineDto : dto.getLines()) {
            Account account = accountRepository.findByIdAndTenantId(lineDto.getAccountId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Account", lineDto.getAccountId()));

            RecurringJournalLine line = RecurringJournalLine.builder()
                    .tenantId(tenantId)
                    .account(account)
                    .description(lineDto.getDescription())
                    .debitAmount(lineDto.getDebitAmount() != null ? lineDto.getDebitAmount() : BigDecimal.ZERO)
                    .creditAmount(lineDto.getCreditAmount() != null ? lineDto.getCreditAmount() : BigDecimal.ZERO)
                    .sortOrder(lineDto.getSortOrder() != null ? lineDto.getSortOrder() : 0)
                    .build();

            template.addLine(line);
        }

        template = templateRepository.save(template);
        log.info("Created recurring journal template: {}", template.getName());

        return toDto(template);
    }

    @Transactional
    public RecurringJournalTemplateDto updateTemplate(Long id, RecurringJournalTemplateDto dto) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        RecurringJournalTemplate template = templateRepository.findByIdWithLines(id, tenantId)
                .orElseThrow(() -> new NotFoundException("RecurringJournalTemplate", id));

        // Validate lines balance
        validateLinesBalance(dto.getLines());

        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setFrequency(dto.getFrequency());
        template.setFrequencyDay(dto.getFrequencyDay());
        template.setStartDate(dto.getStartDate());
        template.setEndDate(dto.getEndDate());
        template.setActive(dto.isActive());
        template.setAutoPost(dto.isAutoPost());

        // Update lines - clear and re-add
        template.getLines().clear();
        for (RecurringJournalTemplateDto.LineDto lineDto : dto.getLines()) {
            Account account = accountRepository.findByIdAndTenantId(lineDto.getAccountId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Account", lineDto.getAccountId()));

            RecurringJournalLine line = RecurringJournalLine.builder()
                    .tenantId(tenantId)
                    .account(account)
                    .description(lineDto.getDescription())
                    .debitAmount(lineDto.getDebitAmount() != null ? lineDto.getDebitAmount() : BigDecimal.ZERO)
                    .creditAmount(lineDto.getCreditAmount() != null ? lineDto.getCreditAmount() : BigDecimal.ZERO)
                    .sortOrder(lineDto.getSortOrder() != null ? lineDto.getSortOrder() : 0)
                    .build();

            template.addLine(line);
        }

        template = templateRepository.save(template);
        log.info("Updated recurring journal template: {}", template.getName());

        return toDto(template);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        RecurringJournalTemplate template = templateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("RecurringJournalTemplate", id));

        templateRepository.delete(template);
        log.info("Deleted recurring journal template: {}", template.getName());
    }

    @Transactional
    public RecurringJournalTemplateDto activateTemplate(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        RecurringJournalTemplate template = templateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("RecurringJournalTemplate", id));

        template.setActive(true);
        template = templateRepository.save(template);

        return toDto(template);
    }

    @Transactional
    public RecurringJournalTemplateDto deactivateTemplate(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        RecurringJournalTemplate template = templateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("RecurringJournalTemplate", id));

        template.setActive(false);
        template = templateRepository.save(template);

        return toDto(template);
    }

    /**
     * Execute a template manually to create a journal entry.
     */
    @Transactional
    public Long executeTemplate(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        RecurringJournalTemplate template = templateRepository.findByIdWithLines(id, tenantId)
                .orElseThrow(() -> new NotFoundException("RecurringJournalTemplate", id));

        return createJournalFromTemplate(template);
    }

    /**
     * Process due recurring templates.
     * Called by scheduler or manually.
     */
    @Transactional
    public int processDueTemplates(Long tenantId) {
        LocalDate today = LocalDate.now();
        List<RecurringJournalTemplate> dueTemplates = templateRepository.findDueTemplates(tenantId, today);

        int processed = 0;
        for (RecurringJournalTemplate template : dueTemplates) {
            try {
                createJournalFromTemplate(template);
                processed++;
            } catch (Exception e) {
                log.error("Failed to process recurring template {}: {}", template.getId(), e.getMessage());
            }
        }

        log.info("Processed {} recurring journal templates for tenant {}", processed, tenantId);
        return processed;
    }

    private Long createJournalFromTemplate(RecurringJournalTemplate template) {
        // Create journal entry from template
        List<JournalEntryService.JournalLineRequest> lines = new ArrayList<>();

        for (RecurringJournalLine line : template.getLines()) {
            lines.add(JournalEntryService.JournalLineRequest.builder()
                    .accountId(line.getAccount().getId())
                    .description(line.getDescription())
                    .debitAmount(line.getDebitAmount())
                    .creditAmount(line.getCreditAmount())
                    .build());
        }

        JournalEntryService.CreateJournalRequest request = JournalEntryService.CreateJournalRequest.builder()
                .entryDate(LocalDate.now())
                .description("Auto-generated from template: " + template.getName())
                .reference("REC-" + template.getId())
                .lines(lines)
                .build();

        Long journalId = journalEntryService.createJournalEntry(request);

        // Auto-post if configured
        if (template.isAutoPost()) {
            journalEntryService.postJournalEntry(journalId);
        }

        // Update template
        template.setLastRunDate(LocalDate.now());
        template.setNextRunDate(template.calculateNextRunDate());
        template.incrementRunCount();
        templateRepository.save(template);

        log.info("Created journal entry {} from recurring template {}", journalId, template.getName());
        return journalId;
    }

    private void validateLinesBalance(List<RecurringJournalTemplateDto.LineDto> lines) {
        BigDecimal totalDebits = lines.stream()
                .map(l -> l.getDebitAmount() != null ? l.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = lines.stream()
                .map(l -> l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BusinessException("Total debits must equal total credits");
        }
    }

    private RecurringJournalTemplateDto toDto(RecurringJournalTemplate template) {
        return RecurringJournalTemplateDto.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .frequency(template.getFrequency())
                .frequencyDay(template.getFrequencyDay())
                .startDate(template.getStartDate())
                .endDate(template.getEndDate())
                .nextRunDate(template.getNextRunDate())
                .lastRunDate(template.getLastRunDate())
                .runCount(template.getRunCount())
                .active(template.isActive())
                .autoPost(template.isAutoPost())
                .lines(template.getLines().stream()
                        .map(this::toLineDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private RecurringJournalTemplateDto.LineDto toLineDto(RecurringJournalLine line) {
        return RecurringJournalTemplateDto.LineDto.builder()
                .id(line.getId())
                .accountId(line.getAccount().getId())
                .accountCode(line.getAccount().getCode())
                .accountName(line.getAccount().getName())
                .description(line.getDescription())
                .debitAmount(line.getDebitAmount())
                .creditAmount(line.getCreditAmount())
                .sortOrder(line.getSortOrder())
                .build();
    }
}
