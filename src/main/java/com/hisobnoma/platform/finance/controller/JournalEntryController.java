package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.CreateJournalEntryRequest;
import com.hisobnoma.platform.finance.dto.JournalEntryDto;
import com.hisobnoma.platform.finance.entity.JournalStatus;
import com.hisobnoma.platform.finance.service.JournalEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/journal-entries")
@RequiredArgsConstructor
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_JE_VIEW')")
    public ResponseEntity<PageResponse<JournalEntryDto>> getJournalEntries(
            @PageableDefault(sort = "entryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(journalEntryService.getJournalEntries(pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('FINANCE_JE_VIEW')")
    public ResponseEntity<PageResponse<JournalEntryDto>> getJournalEntriesByStatus(
            @PathVariable JournalStatus status,
            @PageableDefault(sort = "entryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(journalEntryService.getJournalEntriesByStatus(status, pageable));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAuthority('FINANCE_JE_VIEW')")
    public ResponseEntity<List<JournalEntryDto>> getJournalEntriesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(journalEntryService.getJournalEntriesByDateRange(startDate, endDate));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('FINANCE_JE_VIEW')")
    public ResponseEntity<PageResponse<JournalEntryDto>> searchJournalEntries(
            @RequestParam String q,
            @PageableDefault(sort = "entryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(journalEntryService.searchJournalEntries(q, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_JE_VIEW')")
    public ResponseEntity<JournalEntryDto> getJournalEntry(@PathVariable Long id) {
        return ResponseEntity.ok(journalEntryService.getJournalEntry(id));
    }

    @GetMapping("/{id}/lines")
    @PreAuthorize("hasAuthority('FINANCE_JE_VIEW')")
    public ResponseEntity<JournalEntryDto> getJournalEntryWithLines(@PathVariable Long id) {
        return ResponseEntity.ok(journalEntryService.getJournalEntryWithLines(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_JE_CREATE')")
    public ResponseEntity<JournalEntryDto> createJournalEntry(@Valid @RequestBody CreateJournalEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(journalEntryService.createJournalEntry(request));
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('FINANCE_GL_POST')")
    public ResponseEntity<JournalEntryDto> postJournalEntry(@PathVariable Long id) {
        return ResponseEntity.ok(journalEntryService.postJournalEntry(id));
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAuthority('FINANCE_GL_REVERSE')")
    public ResponseEntity<JournalEntryDto> reverseJournalEntry(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reversalDate) {
        return ResponseEntity.ok(journalEntryService.reverseJournalEntry(id, reversalDate));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_JE_CREATE')")
    public ResponseEntity<Void> deleteJournalEntry(@PathVariable Long id) {
        journalEntryService.deleteJournalEntry(id);
        return ResponseEntity.noContent().build();
    }
}
