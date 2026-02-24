package com.hisobnoma.platform.expense.controller;

import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.expense.entity.ExpenseRecord;
import com.hisobnoma.platform.expense.repository.ExpenseRecordRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Public (non-secured) REST controller for accepting expense data from external services.
 * Expects X-Tenant-ID header for multi-tenancy.
 */
@RestController
@RequestMapping("/api/v1/web/expenses")
@RequiredArgsConstructor
public class ExpenseRecordController {

    private final ExpenseRecordRepository repository;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createExpense(@RequestBody ExpenseRequest request) {
        LocalDate createDate = parseDate(request.getCreateDate());
        if (createDate == null) {
            createDate = LocalDate.now();
        }

        ExpenseRecord record = ExpenseRecord.builder()
                .createDate(createDate)
                .category(request.getCategory())
                .totalAmount(request.getTotalAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "UZS")
                .generatedNotes(request.getGeneratedNotes())
                .fullText(request.getFullText())
                .tenantId(TenantContext.getCurrentTenant())
                .build();

        ExpenseRecord saved = repository.save(record);

        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "status", "created"
        ));
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
