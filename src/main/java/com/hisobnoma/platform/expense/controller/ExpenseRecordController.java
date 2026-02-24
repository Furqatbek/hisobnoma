package com.hisobnoma.platform.expense.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.expense.entity.ExpenseRecord;
import com.hisobnoma.platform.expense.repository.ExpenseRecordRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * REST controller for expense records.
 * Supports both authenticated (JWT) and external (X-Tenant-ID header) access.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/web/expenses")
@RequiredArgsConstructor
public class ExpenseRecordController {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private final ExpenseRecordRepository repository;

    @GetMapping
    public ResponseEntity<PageResponse<ExpenseRecord>> getExpenses(
            @PageableDefault(size = 20, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Long tenantId = resolveTenantId();
        Page<ExpenseRecord> page = repository.findByTenantIdOrTenantIdIsNull(tenantId, pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/summary/total")
    public ResponseEntity<Map<String, Object>> getTotal() {
        Long tenantId = resolveTenantId();
        BigDecimal total = repository.sumTotalByTenantIdOrNull(tenantId);
        return ResponseEntity.ok(Map.of("total", total));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createExpense(@RequestBody ExpenseRequest request) {
        LocalDate createDate = parseDate(request.getCreateDate());
        if (createDate == null) {
            createDate = LocalDate.now();
        }

        if (request.getTotalAmount() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "total_amount is required"
            ));
        }

        Long tenantId = resolveTenantId();

        ExpenseRecord record = ExpenseRecord.builder()
                .createDate(createDate)
                .category(request.getCategory() != null ? request.getCategory() : "Boshqa")
                .totalAmount(request.getTotalAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "UZS")
                .generatedNotes(request.getGeneratedNotes())
                .fullText(request.getFullText())
                .tenantId(tenantId)
                .build();

        ExpenseRecord saved = repository.save(record);
        log.info("Created expense record id={} tenant={} amount={}", saved.getId(), tenantId, saved.getTotalAmount());

        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "status", "created"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        Long tenantId = resolveTenantId();
        repository.findById(id).ifPresent(record -> {
            if (tenantId.equals(record.getTenantId()) || record.getTenantId() == null) {
                repository.delete(record);
            }
        });
        return ResponseEntity.noContent().build();
    }

    /**
     * Resolves tenant ID from context, defaulting to 1 if not available.
     */
    private Long resolveTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            log.debug("No tenant in context, defaulting to tenant {}", DEFAULT_TENANT_ID);
            return DEFAULT_TENANT_ID;
        }
        return tenantId;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    @Data
    public static class ExpenseRequest {
        @JsonProperty("create_date")
        private String createDate;
        private String category;
        @JsonProperty("total_amount")
        private BigDecimal totalAmount;
        private String currency;
        @JsonProperty("generated_notes")
        private String generatedNotes;
        @JsonProperty("full_text")
        private String fullText;
    }
}
