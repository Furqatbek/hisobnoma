package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Journal Entry entity representing a general ledger entry.
 * Each entry must have balanced debit and credit lines.
 */
@Entity
@Table(name = "journal_entries", indexes = {
    @Index(name = "idx_journal_entry_tenant", columnList = "tenant_id"),
    @Index(name = "idx_journal_entry_number", columnList = "entry_number"),
    @Index(name = "idx_journal_entry_date", columnList = "entry_date"),
    @Index(name = "idx_journal_entry_status", columnList = "status"),
    @Index(name = "idx_journal_entry_source", columnList = "source"),
    @Index(name = "idx_journal_entry_period", columnList = "fiscal_period_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class JournalEntry extends TenantAwareEntity {

    @Column(name = "entry_number", nullable = false, unique = true, length = 50)
    private String entryNumber;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_period_id", nullable = false)
    private FiscalPeriod fiscalPeriod;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private JournalStatus status = JournalStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private JournalSource source = JournalSource.MANUAL;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "total_debit", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Column(name = "total_credit", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(length = 3)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "exchange_rate", precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private boolean recurring = false;

    @Column(name = "recurring_template_id")
    private Long recurringTemplateId;

    @Column(name = "is_reversing", nullable = false)
    @Builder.Default
    private boolean reversing = false;

    @Column(name = "reversal_date")
    private LocalDate reversalDate;

    @Column(name = "reversed_entry_id")
    private Long reversedEntryId;

    @Column(name = "reversing_entry_id")
    private Long reversingEntryId;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "posted_by")
    private Long postedBy;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversed_by")
    private Long reversedBy;

    @Column(length = 1000)
    private String notes;

    @Column(name = "internal_notes", length = 1000)
    private String internalNotes;

    @Column(length = 500)
    private String attachments;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();

    /**
     * Adds a line to this journal entry.
     */
    public void addLine(JournalLine line) {
        lines.add(line);
        line.setJournalEntry(this);
        line.setLineNumber(lines.size());
        recalculateTotals();
    }

    /**
     * Removes a line from this journal entry.
     */
    public void removeLine(JournalLine line) {
        lines.remove(line);
        line.setJournalEntry(null);
        renumberLines();
        recalculateTotals();
    }

    /**
     * Recalculates the total debit and credit amounts.
     */
    public void recalculateTotals() {
        this.totalDebit = lines.stream()
                .map(JournalLine::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalCredit = lines.stream()
                .map(JournalLine::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Renumbers the lines after a removal.
     */
    private void renumberLines() {
        int lineNum = 1;
        for (JournalLine line : lines) {
            line.setLineNumber(lineNum++);
        }
    }

    /**
     * Checks if the entry is balanced (debits equal credits).
     */
    public boolean isBalanced() {
        recalculateTotals();
        return totalDebit.compareTo(totalCredit) == 0;
    }

    /**
     * Gets the difference between debit and credit.
     */
    public BigDecimal getImbalance() {
        recalculateTotals();
        return totalDebit.subtract(totalCredit);
    }

    /**
     * Checks if this entry can be posted.
     */
    public boolean canPost() {
        return status == JournalStatus.DRAFT && isBalanced() && !lines.isEmpty();
    }

    /**
     * Checks if this entry can be reversed.
     */
    public boolean canReverse() {
        return status == JournalStatus.POSTED && reversingEntryId == null;
    }
}
