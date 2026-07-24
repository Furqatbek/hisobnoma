package com.hisobnoma.platform.expense.service;

import com.hisobnoma.platform.expense.entity.ExpenseRecord;
import com.hisobnoma.platform.expense.repository.ExpenseRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Shared expense-record creation used by both the web controller and the mobile
 * app. Tenant is always passed in explicitly by the caller (resolved from the
 * staff JWT), so this method carries no security-context dependency.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRecordRepository repository;

    @Transactional
    public ExpenseRecord create(Long tenantId, LocalDate createDate, String category,
                                BigDecimal totalAmount, String currency,
                                String generatedNotes, String fullText) {
        ExpenseRecord record = ExpenseRecord.builder()
                .createDate(createDate != null ? createDate : LocalDate.now())
                .category(category != null && !category.isBlank() ? category : "Boshqa")
                .totalAmount(totalAmount)
                .currency(currency != null && !currency.isBlank() ? currency : "UZS")
                .generatedNotes(generatedNotes)
                .fullText(fullText)
                .tenantId(tenantId)
                .build();
        ExpenseRecord saved = repository.save(record);
        log.info("Created expense record id={} tenant={} amount={}",
                saved.getId(), tenantId, saved.getTotalAmount());
        return saved;
    }
}
