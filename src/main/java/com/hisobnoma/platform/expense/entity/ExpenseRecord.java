package com.hisobnoma.platform.expense.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expense_records", indexes = {
    @Index(name = "idx_expense_record_tenant", columnList = "tenant_id"),
    @Index(name = "idx_expense_record_category", columnList = "category"),
    @Index(name = "idx_expense_record_date", columnList = "create_date")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ExpenseRecord extends TenantAwareEntity {

    @Column(name = "create_date", nullable = false)
    private LocalDate createDate;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalAmount;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "generated_notes", length = 1000)
    private String generatedNotes;

    @Column(name = "full_text", columnDefinition = "TEXT")
    private String fullText;
}
