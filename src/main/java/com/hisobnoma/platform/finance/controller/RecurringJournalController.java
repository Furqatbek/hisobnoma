package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.RecurringJournalTemplateDto;
import com.hisobnoma.platform.finance.service.RecurringJournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for recurring journal templates.
 */
@RestController
@RequestMapping("/api/v1/finance/recurring-journals")
@RequiredArgsConstructor
public class RecurringJournalController {

    private final RecurringJournalService recurringJournalService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_GL_VIEW')")
    public ResponseEntity<PageResponse<RecurringJournalTemplateDto>> getTemplates(
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(recurringJournalService.getTemplates(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_GL_VIEW')")
    public ResponseEntity<RecurringJournalTemplateDto> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(recurringJournalService.getTemplate(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_GL_POST')")
    public ResponseEntity<RecurringJournalTemplateDto> createTemplate(
            @Valid @RequestBody RecurringJournalTemplateDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recurringJournalService.createTemplate(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_GL_POST')")
    public ResponseEntity<RecurringJournalTemplateDto> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody RecurringJournalTemplateDto request) {
        return ResponseEntity.ok(recurringJournalService.updateTemplate(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_GL_POST')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        recurringJournalService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FINANCE_GL_POST')")
    public ResponseEntity<RecurringJournalTemplateDto> activateTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(recurringJournalService.activateTemplate(id));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('FINANCE_GL_POST')")
    public ResponseEntity<RecurringJournalTemplateDto> deactivateTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(recurringJournalService.deactivateTemplate(id));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('FINANCE_GL_POST')")
    public ResponseEntity<Map<String, Long>> executeTemplate(@PathVariable Long id) {
        Long journalId = recurringJournalService.executeTemplate(id);
        return ResponseEntity.ok(Map.of("journalEntryId", journalId));
    }
}
